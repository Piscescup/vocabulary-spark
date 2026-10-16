package io.github.piscescup.vocabularyspark.study;

import io.github.piscescup.vocabularyspark.wordbook.WordbookDatabase;
import io.github.piscescup.vocabularyspark.wordbook.Wordbook;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Indexed daily queues and incremental per-word progress; every operation runs off the client thread.
 */
public final class StudyStore {
    public enum Grade {
        FAMILIAR,
        FUZZY,
        UNFAMILIAR
    }

    public static final int MAX_GOAL = 500;

    private StudyStore() {
    }

    /** Loads a complete plan only when its overview is opened or refreshed. */
    public static StudyPlan plan(Path database, Wordbook book, LocalDate day) throws IOException {
        try (var connection = WordbookDatabase.open(database)) {
            connection.setAutoCommit(false);
            try {
                ensurePlan(connection, day);
                var entries = new ArrayList<StudyPlan.Entry>();
                try (var query = connection.prepareStatement("""
                    SELECT word_key,word,kind,done,last_grade,review_required
                    FROM daily_items WHERE day=? ORDER BY done,slot
                    """)) {
                    query.setString(1, day.toString());
                    try (var rows = query.executeQuery()) {
                        while (rows.next()) {
                            var status = switch (rows.getString(5)) {
                                case "FAMILIAR" -> StudyPlan.Status.FAMILIAR;
                                case "FUZZY" -> StudyPlan.Status.FUZZY;
                                case "UNFAMILIAR" -> StudyPlan.Status.UNFAMILIAR;
                                default -> StudyPlan.Status.UNSEEN;
                            };
                            entries.add(new StudyPlan.Entry(rows.getString(1), rows.getString(2),
                                rows.getString(3).equals("REVIEW"), rows.getInt(4) != 0, status, rows.getInt(6) != 0));
                        }
                    }
                }
                connection.commit();
                return new StudyPlan(day, book, entries);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IOException("Cannot read today's word list", exception);
        }
    }

    public static StudySnapshot load(Path database, LocalDate day, Integer goal) throws IOException {
        try (var connection = WordbookDatabase.open(database)) {
            connection.setAutoCommit(false);
            try {
                if (goal != null) {
                    if (goal < 1 || goal > MAX_GOAL) throw new IllegalArgumentException("Daily goal must be 1–500");
                    try (
                        var update = connection.prepareStatement("UPDATE settings SET daily_goal=? WHERE singleton=1")
                    ) {
                        update.setInt(1, goal);
                        update.executeUpdate();
                    }
                }
                ensurePlan(connection, day);
                var result = snapshot(connection, day);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IOException("Cannot load today's study plan", exception);
        }
    }

    public static StudySnapshot grade(Path database, LocalDate day, String key, Grade grade) throws IOException {
        try (var connection = WordbookDatabase.open(database)) {
            connection.setAutoCommit(false);
            try {
                if (count(
                    connection, "SELECT COUNT(*) FROM daily_items WHERE day=? AND word_key=? AND done=0",
                    day.toString(), key
                ) != 1) {
                    throw new SQLException("This study card is no longer pending");
                }
                boolean uncertain = grade != Grade.FAMILIAR;
                String markedOn = null;
                try (
                    var query = connection.prepareStatement("SELECT review_marked_on FROM progress WHERE word_key=?")
                ) {
                    query.setString(1, key);
                    try (var row = query.executeQuery()) {
                        if (row.next()) markedOn = row.getString(1);
                    }
                }
                if (uncertain) markedOn = day.toString();
                // Familiar later today completes today's card, but must not cancel tomorrow's review.
                String due = day.toString()
                    .equals(markedOn) ? day.plusDays(1)
                    .toString() : "9999-12-31";
                try (
                    var update = connection.prepareStatement("""
                        INSERT INTO progress(word_key,introduced_on,next_due,streak,attempts,review_marked_on,last_grade)
                        VALUES(?,?,?,?,1,?,?)
                        ON CONFLICT(word_key) DO UPDATE SET next_due=excluded.next_due,streak=excluded.streak,
                          attempts=progress.attempts+1,review_marked_on=excluded.review_marked_on,last_grade=excluded.last_grade
                        """)
                ) {
                    update.setString(1, key);
                    update.setString(2, day.toString());
                    update.setString(3, due);
                    update.setInt(4, uncertain ? 0 : 1);
                    update.setString(5, markedOn);
                    update.setString(6, grade.name());
                    update.executeUpdate();
                }
                int slot = count(
                    connection, "SELECT COALESCE(MAX(slot),0)+1 FROM daily_items WHERE day=?", day.toString());
                try (
                    var update = connection.prepareStatement("""
                        UPDATE daily_items SET done=?,slot=?,last_grade=?,review_required=MAX(review_required,?)
                        WHERE day=? AND word_key=?
                        """)
                ) {
                    update.setInt(1, uncertain ? 0 : 1);
                    update.setInt(2, slot);
                    update.setString(3, grade.name());
                    update.setInt(4, uncertain ? 1 : 0);
                    update.setString(5, day.toString());
                    update.setString(6, key);
                    update.executeUpdate();
                }
                var result = snapshot(connection, day);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IOException("Cannot save study progress", exception);
        }
    }

    private static void ensurePlan(Connection connection, LocalDate day) throws SQLException {
        int goal = count(connection, "SELECT daily_goal FROM settings WHERE singleton=1");
        int completed = count(connection, "SELECT COUNT(*) FROM daily_items WHERE day=? AND done=1", day.toString());
        int total = count(connection, "SELECT COUNT(*) FROM daily_items WHERE day=?", day.toString());
        int target = Math.max(goal, completed);
        if (total > target) {
            try (
                var trim = connection.prepareStatement("""
                    DELETE FROM daily_items WHERE day=? AND word_key IN
                      (SELECT word_key FROM daily_items WHERE day=? AND done=0 ORDER BY slot DESC LIMIT ?)
                    """)
            ) {
                trim.setString(1, day.toString());
                trim.setString(2, day.toString());
                trim.setInt(3, total - target);
                trim.executeUpdate();
            }
        }
        addCards(connection, day, true, target);
        addCards(connection, day, false, target);
    }

    private static void addCards(Connection connection, LocalDate day, boolean reviews, int target)
        throws SQLException {
        int remaining = target - count(connection, "SELECT COUNT(*) FROM daily_items WHERE day=?", day.toString());
        if (remaining <= 0) return;
        String condition = reviews ? "p.word_key IS NOT NULL AND p.next_due<=?" : "p.word_key IS NULL";
        String order = reviews ? "p.next_due,w.position" : "w.position";
        var cards = new ArrayList<StudySnapshot.Card>();
        try (
            var query = connection.prepareStatement("""
                SELECT w.word_key,w.word FROM words w LEFT JOIN progress p ON p.word_key=w.word_key
                LEFT JOIN daily_items d ON d.word_key=w.word_key AND d.day=?
                WHERE d.word_key IS NULL AND %s ORDER BY %s LIMIT ?
                """.formatted(condition, order))
        ) {
            int parameter = 1;
            query.setString(parameter++, day.toString());
            if (reviews) query.setString(parameter++, day.toString());
            query.setInt(parameter, remaining);
            try (var rows = query.executeQuery()) {
                while (rows.next()) cards.add(new StudySnapshot.Card(rows.getString(1), rows.getString(2), reviews));
            }
        }
        int slot = count(connection, "SELECT COALESCE(MAX(slot),0) FROM daily_items WHERE day=?", day.toString());
        try (
            var insert = connection.prepareStatement(
                "INSERT INTO daily_items(day,slot,word_key,word,kind) VALUES(?,?,?,?,?)")
        ) {
            for (var card : cards) {
                insert.setString(1, day.toString());
                insert.setInt(2, ++slot);
                insert.setString(3, card.key());
                insert.setString(4, card.word());
                insert.setString(5, reviews ? "REVIEW" : "NEW");
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static StudySnapshot snapshot(Connection connection, LocalDate day) throws SQLException {
        int goal = count(connection, "SELECT daily_goal FROM settings WHERE singleton=1");
        int total = count(connection, "SELECT COUNT(*) FROM daily_items WHERE day=?", day.toString());
        int completed = count(connection, "SELECT COUNT(*) FROM daily_items WHERE day=? AND done=1", day.toString());
        int newCompleted = count(
            connection, "SELECT COUNT(*) FROM daily_items WHERE day=? AND done=1 AND kind='NEW'", day.toString());
        int learned = count(connection, "SELECT COUNT(*) FROM progress");
        StudySnapshot.Card current = null;
        try (
            var query = connection.prepareStatement(
                "SELECT word_key,word,kind FROM daily_items WHERE day=? AND done=0 ORDER BY slot LIMIT 1")
        ) {
            query.setString(1, day.toString());
            try (var row = query.executeQuery()) {
                if (row.next()) {
                    current = new StudySnapshot.Card(
                        row.getString(1), row.getString(2), row.getString(3)
                        .equals("REVIEW")
                    );
                }
            }
        }
        return new StudySnapshot(day, goal, total, completed, newCompleted, completed - newCompleted, learned, current);
    }

    private static int count(Connection connection, String sql, String... parameters) throws SQLException {
        try (var query = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) query.setString(index + 1, parameters[index]);
            try (var rows = query.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        }
    }
}
