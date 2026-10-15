package io.github.piscescup.vocabularyspark.wordbook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Local managed copies and optional bundled books; imported source files are never modified. */
public final class WordbookLibrary {
    private static final String BUILTIN_ROOT = "/assets/vocabulary-spark/wordbooks/";
    private static final List<String> BUILTIN_FILES = List.of(
        "cet4.json", "cet6.json", "kaoyan.json", "ielts.json", "toefl.json"
    );
    private final Path directory;
    private final boolean includeBuiltins;

    /** Creates a library containing only locally managed books. */
    public WordbookLibrary(Path directory) {
        this(directory, false);
    }

    private WordbookLibrary(Path directory, boolean includeBuiltins) {
        this.directory = directory.toAbsolutePath().normalize();
        this.includeBuiltins = includeBuiltins;
    }

    /**
     * Creates a library that reads bundled books directly from the classpath,
     * including when resources are inside a JAR. Local books with the same ID
     * override bundled books, allowing confirmed imports to survive restarts.
     *
     * @param directory directory for imported and overridden books
     * @return a library containing bundled and local books
     */
    public static WordbookLibrary withBuiltins(Path directory) {
        return new WordbookLibrary(directory, true);
    }

    public Path directory() { return directory; }

    public LoadResult load() throws IOException {
        Files.createDirectories(directory);
        var books = new LinkedHashMap<String, Wordbook>();
        var failures = new ArrayList<String>();
        if (includeBuiltins) loadBuiltins(books, failures);
        var localIds = new HashSet<String>();
        try (var files = Files.list(directory)) {
            for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                try {
                    Wordbook book = WordbookFiles.read(path);
                    if (!localIds.add(book.id())) {
                        throw new IOException("Duplicate wordbook id: " + book.id());
                    }
                    books.put(book.id(), book);
                } catch (IOException | IllegalArgumentException exception) {
                    failures.add(path.getFileName() + ": " + exception.getMessage());
                }
            }
        }
        return new LoadResult(List.copyOf(books.values()), List.copyOf(failures));
    }

    /** Bundled files are read as streams; no writable extracted copies are needed. */
    private static void loadBuiltins(Map<String, Wordbook> books, List<String> failures) {
        for (String filename : BUILTIN_FILES) {
            String resource = BUILTIN_ROOT + filename;
            try (var stream = WordbookLibrary.class.getResourceAsStream(resource)) {
                if (stream == null) throw new IOException("Missing bundled wordbook: " + resource);
                Wordbook book = WordbookFiles.readJson(stream, filename);
                if (books.putIfAbsent(book.id(), book) != null) {
                    throw new IOException("Duplicate bundled wordbook id: " + book.id());
                }
            } catch (IOException | IllegalArgumentException exception) {
                failures.add(resource + ": " + exception.getMessage());
            }
        }
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
