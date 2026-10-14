package io.github.piscescup.fabricmc.wordbook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordbookFilesTest {
    @TempDir Path directory;

    private Wordbook example() {
        return new Wordbook(1, "cet4-sample", "四级示例", List.of("spark", "abandon", "ice cream"));
    }

    @Test void jsonRoundTripPreservesIdentityNameSpellingAndOrder() throws Exception {
        Path path = directory.resolve("词书.json");
        WordbookFiles.write(path, example(), false);
        assertEquals(example(), WordbookFiles.read(path));
        assertTrue(Files.readString(path).contains("四级示例"));
    }

    @Test void textImportHandlesBomCrlfWhitespaceDuplicatesAndPhrases() throws Exception {
        Path path = directory.resolve("我的单词.TXT");
        Files.writeString(path, "\uFEFF Spark \r\n\r\nspark\r\nabandon\r\nice cream \r\n", StandardCharsets.UTF_8);
        Wordbook book = WordbookFiles.read(path);
        assertEquals("我的单词", book.name());
        assertEquals(List.of("Spark", "abandon", "ice cream"), book.words());
        assertEquals(book, WordbookFiles.read(path));
    }

    @Test void textExportIsJustAnOrderedWordList() throws Exception {
        Path path = directory.resolve("share.txt");
        WordbookFiles.write(path, example(), false);
        assertEquals("spark\nabandon\nice cream\n", Files.readString(path));
        assertEquals(example().words(), WordbookFiles.read(path).words());
    }

    @Test void refusingOverwriteLeavesOriginalBytesAndNoTemporaryFiles() throws Exception {
        Path path = directory.resolve("existing.json");
        Files.writeString(path, "original content");
        assertThrows(FileAlreadyExistsException.class, () -> WordbookFiles.write(path, example(), false));
        assertEquals("original content", Files.readString(path));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test void confirmedOverwriteProducesReadableBookAndNoTemporaryFiles() throws Exception {
        Path path = directory.resolve("existing.json");
        Files.writeString(path, "original content");
        WordbookFiles.write(path, example(), true);
        assertEquals(example(), WordbookFiles.read(path));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test void unsupportedFormatAndNonStringWordsAreRejected() throws Exception {
        Path path = directory.resolve("bad.json");
        for (String json : List.of(
            "{\"formatVersion\":2,\"id\":\"x\",\"name\":\"x\",\"words\":[\"word\"]}",
            "{\"formatVersion\":1,\"id\":\"x\",\"name\":\"x\",\"words\":[42]}",
            "{\"formatVersion\":1,\"id\":\"x\",\"name\":\"x\",\"words\":[null]}",
            "{\"formatVersion\":1,\"id\":\"x\",\"name\":\"x\",\"words\":[\"\"]}",
            "{\"formatVersion\":\"1\",\"id\":\"x\",\"name\":\"x\",\"words\":[\"word\"]}",
            "{\"formatVersion\":1,\"id\":\"x\",\"words\":[\"word\"]}",
            "[\"word\"]", "{bad json")) {
            Files.writeString(path, json);
            assertThrows(IOException.class, () -> WordbookFiles.read(path), json);
        }
    }

    @Test void jsonBomAndUnicodeAreSupported() throws Exception {
        Path path = directory.resolve("book.json");
        Files.writeString(path, "\uFEFF{\"formatVersion\":1,\"id\":\"x\",\"name\":\"雅思示例\",\"words\":[\"café\"]}");
        assertEquals(List.of("café"), WordbookFiles.read(path).words());
    }

    @Test void emptyTextAndControlCharactersAreRejected() throws Exception {
        Path path = directory.resolve("empty.txt");
        Files.writeString(path, "\n \n");
        assertThrows(IllegalArgumentException.class, () -> WordbookFiles.read(path));
        assertThrows(IllegalArgumentException.class, () -> new Wordbook(1, "id", "name", List.of("two\nlines")));
    }

    @Test void inputSizeIsBoundedAndUnsupportedExtensionsAreRejected() throws Exception {
        Path path = directory.resolve("large.txt");
        try (var file = new java.io.RandomAccessFile(path.toFile(), "rw")) { file.setLength(16 * 1024 * 1024 + 1); }
        assertThrows(IOException.class, () -> WordbookFiles.read(path));
        assertThrows(IOException.class, () -> WordbookFiles.write(directory.resolve("book.csv"), example(), false));
    }

    @Test void libraryPersistsCopiesWithoutModifyingSource() throws Exception {
        Path source = directory.resolve("source.txt");
        Files.writeString(source, " spark \r\nspark\r\nabandon\r\n");
        byte[] original = Files.readAllBytes(source);
        Wordbook book = WordbookFiles.read(source);
        var library = new WordbookLibrary(directory.resolve("library"));
        library.save(book, false);
        assertEquals(List.of(book), new WordbookLibrary(library.directory()).load().books());
        assertArrayEquals(original, Files.readAllBytes(source));
    }

    @Test void libraryCannotWriteOutsideItsDirectoryThroughBookId() throws Exception {
        var library = new WordbookLibrary(directory.resolve("library"));
        Wordbook traversal = new Wordbook(1, "../../escaped", "Example", List.of("spark"));
        library.save(traversal, false);
        assertEquals(List.of(traversal), library.load().books());
        assertFalse(Files.exists(directory.resolve("escaped.json")));
        assertThrows(FileAlreadyExistsException.class, () -> library.save(traversal, false));
    }

    @Test void corruptStoredBookDoesNotPreventLoadingOtherBooks() throws Exception {
        var library = new WordbookLibrary(directory.resolve("library"));
        library.save(example(), false);
        Files.writeString(library.directory().resolve("broken.json"), "{bad json");
        var result = library.load();
        assertEquals(List.of(example()), result.books());
        assertEquals(1, result.failures().size());
        assertTrue(result.failures().getFirst().startsWith("broken.json:"));
    }

    @Test void libraryReplacementSurvivesRestart() throws Exception {
        var library = new WordbookLibrary(directory.resolve("library"));
        library.save(example(), false);
        var updated = new Wordbook(1, example().id(), "Updated name", List.of("ability"));
        library.save(updated, true);
        assertEquals(List.of(updated), new WordbookLibrary(library.directory()).load().books());
    }
}
