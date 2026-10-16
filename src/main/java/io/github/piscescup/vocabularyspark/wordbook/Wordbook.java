package io.github.piscescup.vocabularyspark.wordbook;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Portable wordbook contents. Dictionary entries and learning progress live separately.
 */
public record Wordbook(int formatVersion, String id, String name, List<String> words) {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_WORDS = 100_000;

    public Wordbook {
        if (formatVersion != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported wordbook formatVersion: " + formatVersion);
        }
        if (id == null || id.isBlank() || id.length() > 128) {
            throw new IllegalArgumentException("Wordbook id must contain 1–128 characters");
        }
        if (name == null || name.isBlank() || name.length() > 200) {
            throw new IllegalArgumentException("Wordbook name must contain 1–200 characters");
        }
        if (words == null || words.size() > MAX_WORDS) {
            throw new IllegalArgumentException("Wordbook must contain a words array (at most " + MAX_WORDS + ")");
        }
        id = id.strip();
        name = name.strip();
        var unique = new LinkedHashMap<String, String>();
        for (String word : words) {
            if (word == null) throw new IllegalArgumentException("A word cannot be null");
            word = word.strip();
            if (word.isEmpty()) continue;
            if (word.length() > 200 || word.codePoints()
                .anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Invalid word: " + word.substring(0, Math.min(word.length(), 40)));
            }
            unique.putIfAbsent(word.toLowerCase(Locale.ROOT), word);
        }
        if (unique.isEmpty()) throw new IllegalArgumentException("Wordbook contains no words");
        words = List.copyOf(unique.values());
    }

    /**
     * TXT has no metadata; derive a repeatable identity without changing spelling or order.
     */
    public static Wordbook fromText(String name, List<String> words) {
        var normalized = new Wordbook(CURRENT_VERSION, "temporary", name, words);
        var identity = name + "\n" + String.join("\n", normalized.words());
        return new Wordbook(
            CURRENT_VERSION,
            "txt-" + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)), name, normalized.words()
        );
    }
}
