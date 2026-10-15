package io.github.piscescup.vocabularyspark.wordbook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinWordbookLibraryTest {
    @TempDir Path directory;

    @Test void fiveBundledBooksLoadWithoutExtractingFiles() throws Exception {
        var library = WordbookLibrary.withBuiltins(directory.resolve("library"));
        var result = library.load();
        assertTrue(result.failures().isEmpty(), result.failures().toString());
        assertEquals(List.of("CET-4 · Qwerty", "CET-6 · Qwerty", "考研英语 · Qwerty",
            "IELTS · Qwerty", "TOEFL · Qwerty"), result.books().stream().map(Wordbook::name).toList());
        assertEquals(List.of(2607, 2345, 3727, 3575, 4264),
            result.books().stream().map(book -> book.words().size()).toList());
        assertEquals(5, result.books().stream().map(Wordbook::id).distinct().count());
        try (var files = Files.list(library.directory())) { assertEquals(0, files.count()); }
    }

    @Test void localOverrideAndAdditionalBookSurviveRestartWithoutDuplicateBuiltins() throws Exception {
        Path libraryPath = directory.resolve("library");
        var library = WordbookLibrary.withBuiltins(libraryPath);
        Wordbook builtin = library.load().books().getFirst();
        Wordbook override = new Wordbook(1, builtin.id(), "My CET4", List.of("spark"));
        Wordbook custom = new Wordbook(1, "my-book", "My book", List.of("ice cream"));
        library.save(override, true);
        library.save(custom, false);

        var result = WordbookLibrary.withBuiltins(libraryPath).load();
        assertTrue(result.failures().isEmpty());
        assertEquals(6, result.books().size());
        assertEquals(override, result.books().getFirst());
        assertTrue(result.books().contains(custom));
        assertEquals(1, result.books().stream().filter(book -> book.id().equals(builtin.id())).count());
        assertEquals(builtin, WordbookLibrary.withBuiltins(directory.resolve("fresh-library")).load().books().getFirst());
    }

    @Test void brokenLocalBookLeavesBundledBooksAvailable() throws Exception {
        var library = WordbookLibrary.withBuiltins(directory.resolve("library"));
        var originals = library.load().books();
        Files.writeString(library.directory().resolve("broken.json"), "{bad json");
        var result = library.load();
        assertEquals(originals, result.books());
        assertEquals(1, result.failures().size());
        assertTrue(result.failures().getFirst().startsWith("broken.json:"));
    }

    @Test void duplicateLocalIdsRemainErrorsEvenWhenTheyOverrideABuiltin() throws Exception {
        var library = WordbookLibrary.withBuiltins(directory.resolve("library"));
        Wordbook builtin = library.load().books().getFirst();
        library.save(builtin, false);
        WordbookFiles.write(library.directory().resolve("duplicate.json"), builtin, false);
        var result = library.load();
        assertEquals(5, result.books().size());
        assertEquals(1, result.failures().size());
        assertTrue(result.failures().getFirst().contains("Duplicate wordbook id"));
    }

    @Test void exportedBuiltinWorksInALocalOnlyLibrary() throws Exception {
        Wordbook builtin = WordbookLibrary.withBuiltins(directory.resolve("bundled")).load().books().getFirst();
        Path exported = directory.resolve("exported.json");
        WordbookFiles.write(exported, builtin, false);
        var library = new WordbookLibrary(directory.resolve("local"));
        assertTrue(library.load().books().isEmpty());
        library.save(WordbookFiles.read(exported), false);
        assertEquals(List.of(builtin), library.load().books());
    }
}
