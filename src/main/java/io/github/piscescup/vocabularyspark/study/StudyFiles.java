package io.github.piscescup.vocabularyspark.study;

import io.github.piscescup.vocabularyspark.wordbook.Wordbook;
import io.github.piscescup.vocabularyspark.wordbook.WordbookDatabase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

/** Player-specific study files. Queues are partitioned by date inside each book's database. */
public final class StudyFiles {
    private final Path directory;

    public StudyFiles(Path modDirectory, String playerName, UUID playerId) {
        String safeName = playerName.replaceAll("[^A-Za-z0-9_-]", "_");
        if (safeName.isBlank()) safeName = "player";
        directory = modDirectory.toAbsolutePath().normalize().resolve("daily")
            .resolve(safeName + "_" + playerId);
    }

    public Path directory() { return directory; }
    public Path settingsPath() { return directory.resolve("settings.json"); }

    public Path databasePath(Wordbook book) {
        String key = UUID.nameUUIDFromBytes(book.id().getBytes(StandardCharsets.UTF_8)).toString();
        return directory.resolve("books").resolve(folderPrefix(book.name()) + "_" + key).resolve("study.sqlite");
    }

    private static String folderPrefix(String bookName) {
        String name = bookName.replaceFirst("\\s*·\\s*Qwerty$", "").strip();
        name = switch (name) {
            case "CET-4", "CET4" -> "cet4";
            case "CET-6", "CET6" -> "cet6";
            case "考研", "考研英语" -> "cn_master";
            case "IELTS", "TOEFL" -> name.toLowerCase(Locale.ROOT);
            default -> name;
        };
        String safeName = name.replaceAll("[^\\p{L}\\p{N}_-]+", "_").replaceAll("^_+|_+$", "");
        return safeName.isBlank() ? "wordbook" : safeName;
    }

    /** Syncs book contents while keeping this player's settings, progress and dated queues. */
    public Path prepare(Wordbook book) throws IOException {
        Path database = databasePath(book);
        WordbookDatabase.write(database, book, true, false);
        return database;
    }
}
