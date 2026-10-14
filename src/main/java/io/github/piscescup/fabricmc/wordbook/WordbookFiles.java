package io.github.piscescup.fabricmc.wordbook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** UTF-8 JSON/TXT import and atomic export, independent of the Minecraft client. */
public final class WordbookFiles {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long MAX_BYTES = 16 * 1024 * 1024;

    private WordbookFiles() {}

    public static Wordbook read(Path path) throws IOException {
        if (Files.size(path) > MAX_BYTES) throw new IOException("Wordbook exceeds 16 MiB");
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        return switch (extension(path)) {
            case "txt" -> Wordbook.fromText(baseName(path), text.lines().toList());
            case "json" -> readJson(text);
            default -> throw new IOException("Choose a .json or .txt wordbook");
        };
    }

    private static Wordbook readJson(String text) throws IOException {
        try {
            JsonElement root = JsonParser.parseString(text);
            if (!root.isJsonObject()) throw new IllegalArgumentException("Expected a JSON object");
            JsonObject object = root.getAsJsonObject();
            JsonElement version = object.get("formatVersion");
            if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
                || !version.getAsBigDecimal().equals(java.math.BigDecimal.ONE)) {
                throw new IllegalArgumentException("Expected formatVersion: 1");
            }
            JsonElement array = object.get("words");
            if (array == null || !array.isJsonArray()) throw new IllegalArgumentException("Expected a words array");
            List<String> words = new ArrayList<>();
            for (JsonElement entry : array.getAsJsonArray()) {
                if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Every words entry must be a string");
                }
                words.add(entry.getAsString());
            }
            return new Wordbook(1, requiredString(object, "id"), requiredString(object, "name"), words);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid wordbook JSON: " + exception.getMessage(), exception);
        }
    }

    private static String requiredString(JsonObject object, String key) {
        JsonElement entry = object.get(key);
        if (entry == null || !entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Expected a string for " + key);
        }
        return entry.getAsString();
    }

    /** Existing files are only replaced after the caller has obtained explicit confirmation. */
    public static void write(Path destination, Wordbook book, boolean replace) throws IOException {
        String content = switch (extension(destination)) {
            case "json" -> JSON.toJson(book) + "\n";
            case "txt" -> String.join("\n", book.words()) + "\n";
            default -> throw new IOException("Export requires a .json or .txt extension");
        };
        Path target = destination.toAbsolutePath().normalize();
        if (!replace && Files.exists(target)) throw new java.nio.file.FileAlreadyExistsException(target.toString());
        Path temporary = Files.createTempFile(target.getParent(), ".vocabulary-spark-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            if (replace) {
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                // ATOMIC_MOVE may overwrite an existing target even without REPLACE_EXISTING.
                Files.move(temporary, target);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String extension(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String baseName(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
