package io.github.piscescup.vocabularyspark.wordbook;

import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Locale;

/**
 * One transactional, indexed SQLite database per wordbook. Call only on an I/O worker.
 */
public final class WordbookDatabase {
    private static final int SCHEMA_VERSION = 2;

    private WordbookDatabase() {
    }

    public static Connection open(Path path) throws IOException, SQLException {
        Files.createDirectories(path.toAbsolutePath()
            .getParent());
        loadDriver();
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout=5000");
            int version = schemaVersion(connection);
            if (version != 0 && version != SCHEMA_VERSION) {
                throw new SQLException("Invalid Vocabulary Spark SQLite format");
            }
            if (version == 0) {
                try (
                    var tables = statement.executeQuery(
                        "SELECT COUNT(*) FROM sqlite_schema WHERE name NOT GLOB 'sqlite_*'")
                ) {
                    if (tables.next() && tables.getInt(1) > 0) {
                        throw new SQLException("Expected the current Vocabulary Spark database format");
                    }
                }
            }
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            if (version == 0) createSchema(connection);
            return connection;
        } catch (SQLException | RuntimeException exception) {
            connection.close();
            throw exception;
        }
    }

    /**
     * Initializes an empty database with the current tables and indexes.
     */
    private static void createSchema(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try (var statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE book (
                  singleton INTEGER PRIMARY KEY CHECK(singleton=1), id TEXT NOT NULL, name TEXT NOT NULL,
                  format_version INTEGER NOT NULL, origin TEXT NOT NULL, content_hash TEXT NOT NULL)
                """);
            statement.execute("""
                CREATE TABLE words (
                  position INTEGER PRIMARY KEY, word TEXT NOT NULL, word_key TEXT NOT NULL UNIQUE)
                """);
            statement.execute(
                "CREATE TABLE settings (singleton INTEGER PRIMARY KEY CHECK(singleton=1), daily_goal INTEGER NOT NULL)");
            statement.execute("INSERT INTO settings VALUES(1,20)");
            statement.execute("""
                CREATE TABLE progress (
                  word_key TEXT PRIMARY KEY, introduced_on TEXT NOT NULL, next_due TEXT NOT NULL,
                  streak INTEGER NOT NULL, attempts INTEGER NOT NULL, review_marked_on TEXT,
                  last_grade TEXT NOT NULL DEFAULT 'UNFAMILIAR')
                """);
            statement.execute("CREATE INDEX progress_due ON progress(next_due)");
            statement.execute("""
                CREATE TABLE daily_items (
                  day TEXT NOT NULL, slot INTEGER NOT NULL, word_key TEXT NOT NULL, word TEXT NOT NULL,
                  kind TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, last_grade TEXT NOT NULL DEFAULT '',
                  review_required INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(day,word_key))
                """);
            statement.execute("CREATE INDEX daily_queue ON daily_items(day,done,slot)");
            statement.execute("PRAGMA user_version=" + SCHEMA_VERSION);
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static int schemaVersion(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var row = statement.executeQuery("PRAGMA user_version")) {
            return row.next() ? row.getInt(1) : 0;
        }
    }

    public static Wordbook read(Path path) throws IOException {
        // Reads portable wordbook contents without changing the source.
        try (var connection = openReadOnly(path); var statement = connection.createStatement()) {
            int databaseVersion = schemaVersion(connection);
            if (databaseVersion != SCHEMA_VERSION) {
                throw new SQLException("Invalid Vocabulary Spark SQLite format");
            }
            String id, name;
            int version;
            try (var metadata = statement.executeQuery("SELECT id,name,format_version FROM book WHERE singleton=1")) {
                if (!metadata.next()) throw new IOException("Wordbook database has no metadata: " + path.getFileName());
                id = metadata.getString(1);
                name = metadata.getString(2);
                version = metadata.getInt(3);
            }
            var words = new ArrayList<String>();
            try (var rows = statement.executeQuery("SELECT word FROM words ORDER BY position")) {
                while (rows.next()) words.add(rows.getString(1));
            }
            return new Wordbook(version, id, name, words);
        } catch (SQLException | IllegalArgumentException exception) {
            throw new IOException("Cannot read " + path.getFileName() + ": " + exception.getMessage(), exception);
        }
    }

    /**
     * Finishes an isolated export as a single portable database, with no required WAL sidecar.
     */
    static void writePortable(Path path, Wordbook book) throws IOException {
        write(path, book, true, false);
        finishPortable(path);
    }

    private static void finishPortable(Path path) throws IOException {
        try (var connection = open(path); var statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(TRUNCATE)");
            statement.execute("PRAGMA journal_mode=DELETE");
        } catch (SQLException exception) {
            throw new IOException("Cannot finalize SQLite file", exception);
        }
    }

    private static Connection openReadOnly(Path path) throws IOException, SQLException {
        if (!Files.isRegularFile(path)) throw new IOException("Wordbook database does not exist: " + path);
        loadDriver();
        var config = new SQLiteConfig();
        config.setReadOnly(true);
        return config.createConnection("jdbc:sqlite:" + path.toAbsolutePath());
    }

    private static void loadDriver() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("SQLite driver is unavailable", exception);
        }
    }

    /**
     * Replacing a book keeps progress for words still present; the entire update is atomic.
     */
    public static void write(Path path, Wordbook book, boolean replace, boolean builtin) throws IOException {
        try (var connection = open(path)) {
            String origin = builtin ? "builtin" : "local";
            String hash = contentHash(book);
            try (
                var query = connection.createStatement();
                var rows = query.executeQuery("SELECT origin,content_hash FROM book WHERE singleton=1")
            ) {
                if (rows.next()) {
                    if (builtin && rows.getString(1)
                        .equals("local")) {
                        return;
                    }
                    if (!replace && rows.getString(1)
                        .equals("local")) {
                        throw new java.nio.file.FileAlreadyExistsException(path.toString());
                    }
                    if (origin.equals(rows.getString(1)) && hash.equals(rows.getString(2))) return;
                }
            }
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.createStatement()) {
                    statement.execute(
                        "CREATE TEMP TABLE incoming (position INTEGER, word TEXT, word_key TEXT PRIMARY KEY)");
                }
                try (var insert = connection.prepareStatement("INSERT INTO incoming VALUES(?,?,?)")) {
                    for (int index = 0; index < book.words()
                        .size(); index++) {
                        String word = book.words()
                            .get(index);
                        insert.setInt(1, index);
                        insert.setString(2, word);
                        insert.setString(3, word.toLowerCase(Locale.ROOT));
                        insert.addBatch();
                        if ((index + 1) % 500 == 0) {
                            insert.executeBatch();
                            insert.clearBatch();
                        }
                    }
                    insert.executeBatch();
                }
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate(
                        "DELETE FROM progress WHERE word_key NOT IN (SELECT word_key FROM incoming)");
                    statement.executeUpdate(
                        "DELETE FROM daily_items WHERE done=0 AND word_key NOT IN (SELECT word_key FROM incoming)");
                    statement.executeUpdate("DELETE FROM words");
                    statement.executeUpdate(
                        "INSERT INTO words SELECT position,word,word_key FROM incoming ORDER BY position");
                }
                try (var metadata = connection.prepareStatement("INSERT OR REPLACE INTO book VALUES(1,?,?,?,?,?)")) {
                    metadata.setString(1, book.id());
                    metadata.setString(2, book.name());
                    metadata.setInt(3, book.formatVersion());
                    metadata.setString(4, origin);
                    metadata.setString(5, hash);
                    metadata.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IOException("Cannot save " + book.name() + ": " + exception.getMessage(), exception);
        }
    }

    private static String contentHash(Wordbook book) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update((book.id() + "\0" + book.name()).getBytes(StandardCharsets.UTF_8));
            for (String word : book.words()) {
                digest.update((byte) 0);
                digest.update(word.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of()
                .formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
