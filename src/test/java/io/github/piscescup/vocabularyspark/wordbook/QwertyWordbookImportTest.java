package io.github.piscescup.vocabularyspark.wordbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QwertyWordbookImportTest {
    @TempDir Path directory;

    @Test void importsNamesWithoutRequiringDictionaryFieldsAndPreservesPhrasesAndUnicode() throws Exception {
        Path source = directory.resolve("my-qwerty.JSON");
        Files.writeString(source, """
            ﻿[
              {"name":" Spark ","trans":["火花"],"usphone":"spɑrk","ukphone":"spɑːk"},
              {"name":"spark"},
              {"name":"ice cream","notation":"a phrase"},
              {"name":"café"}
            ]
            """, StandardCharsets.UTF_8);

        Wordbook book = WordbookFiles.read(source);
        assertEquals(List.of("Spark", "ice cream", "café"), book.words());
        assertEquals("my-qwerty · Qwerty", book.name());
        assertTrue(book.id().startsWith("qwerty-"));
    }

    @Test void recognizesTheFiveBasicExamFilenames() throws Exception {
        for (var entry : Map.of(
            "CET4_T.json", "CET-4", "CET6_T.json", "CET-6",
            "KaoYan_3_T.json", "考研英语", "IELTS_3_T.json", "IELTS", "TOEFL_3_T.json", "TOEFL"
        ).entrySet()) {
            Path source = directory.resolve(entry.getKey());
            Files.writeString(source, "[{\"name\":\"spark\"}]");
            assertEquals(entry.getValue() + " · Qwerty", WordbookFiles.read(source).name());
        }
    }

    @Test void identitySurvivesMovingUpdatingAndChangingFilenameCaseButSeparatesBooks() throws Exception {
        Path first = directory.resolve("CET4_T.json");
        Files.writeString(first, "[{\"name\":\"spark\"}]");
        Wordbook original = WordbookFiles.read(first);
        Path moved = Files.createDirectory(directory.resolve("downloads")).resolve("cet4_t.JSON");
        Files.writeString(moved, "[{\"name\":\"ability\"}]");
        Wordbook updated = WordbookFiles.read(moved);

        assertEquals(original.id(), updated.id());
        assertEquals(original.name(), updated.name());
        assertNotEquals(original.words(), updated.words());
        Path other = directory.resolve("CET6_T.json");
        Files.writeString(other, Files.readString(first));
        assertNotEquals(original.id(), WordbookFiles.read(other).id());
    }

    @Test void invalidEntriesRejectTheWholeImportAndIdentifyTheSourceRow() throws Exception {
        Path source = directory.resolve("invalid.json");
        for (String invalid : List.of("null", "42", "\"word\"", "{}", "{\"name\":null}",
            "{\"name\":false}", "{\"name\":42}", "{\"name\":[]}", "{\"name\":\"  \"}")) {
            Files.writeString(source, "[{\"name\":\"valid\"}," + invalid + "]");
            IOException exception = assertThrows(IOException.class, () -> WordbookFiles.read(source), invalid);
            assertTrue(exception.getMessage().contains("Qwerty entry 2"), exception.getMessage());
        }
        for (String invalid : List.of("[]", "[{\"name\":\"two\\nlines\"}]",
            "[{\"name\":\"" + "x".repeat(201) + "\"}]")) {
            Files.writeString(source, invalid);
            assertThrows(IOException.class, () -> WordbookFiles.read(source));
        }
    }

    @Test void enforcesSourceEntryLimitBeforeDeduplication() {
        var entries = new JsonArray();
        var entry = JsonParser.parseString("{\"name\":\"spark\"}");
        for (int index = 0; index <= Wordbook.MAX_WORDS; index++) entries.add(entry);
        assertThrows(IllegalArgumentException.class, () -> QwertyWordbookAdapter.read(entries, "large"));
    }

    @Test void managedCopyAndNativeExportsRetainIdentityWithoutChangingTheSource() throws Exception {
        Path source = directory.resolve("CET4_T.json");
        Files.writeString(source, "[{\"name\":\"spark\",\"trans\":[\"火花\"]}]");
        byte[] original = Files.readAllBytes(source);
        Wordbook book = WordbookFiles.read(source);
        var library = new WordbookLibrary(directory.resolve("library"));
        library.save(book, false);
        assertEquals(List.of(book), new WordbookLibrary(library.directory()).load().books());

        Path exported = directory.resolve("different-export-name.json");
        WordbookFiles.write(exported, book, false);
        assertTrue(JsonParser.parseString(Files.readString(exported)).isJsonObject());
        assertEquals(book, WordbookFiles.read(exported));
        assertArrayEquals(original, Files.readAllBytes(source));

        Files.writeString(source, "[{\"name\":\"ability\"}]");
        Wordbook updated = WordbookFiles.read(source);
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> library.save(updated, false));
        assertEquals(List.of(book), library.load().books());
        library.save(updated, true);
        assertEquals(List.of(updated), new WordbookLibrary(library.directory()).load().books());
    }
}
