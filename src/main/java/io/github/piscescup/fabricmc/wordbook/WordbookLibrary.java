package io.github.piscescup.fabricmc.wordbook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Local managed copies; imported source files are never modified. */
public final class WordbookLibrary {
    private final Path directory;

    public WordbookLibrary(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    public Path directory() { return directory; }

    public LoadResult load() throws IOException {
        Files.createDirectories(directory);
        var books = new ArrayList<Wordbook>();
        var failures = new ArrayList<String>();
        try (var files = Files.list(directory)) {
            for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                try {
                    Wordbook book = WordbookFiles.read(path);
                    if (books.stream().anyMatch(existing -> existing.id().equals(book.id()))) {
                        throw new IOException("Duplicate wordbook id: " + book.id());
                    }
                    books.add(book);
                } catch (IOException | IllegalArgumentException exception) {
                    failures.add(path.getFileName() + ": " + exception.getMessage());
                }
            }
        }
        return new LoadResult(List.copyOf(books), List.copyOf(failures));
    }

    public void save(Wordbook book, boolean replace) throws IOException {
        Files.createDirectories(directory);
        WordbookFiles.write(storagePath(book.id()), book, replace);
    }

    /** User-supplied IDs are never used as file paths. */
    private Path storagePath(String id) {
        String filename = UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)) + ".json";
        return directory.resolve(filename);
    }

    public record LoadResult(List<Wordbook> books, List<String> failures) {}
}
