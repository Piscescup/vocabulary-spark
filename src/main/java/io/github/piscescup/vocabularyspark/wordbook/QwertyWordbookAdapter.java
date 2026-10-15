package io.github.piscescup.vocabularyspark.wordbook;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

/**
 * Converts Qwerty Learner's array of word objects into a portable wordbook.
 * Only {@code name} is imported; definitions and pronunciations remain separate
 * dictionary data. The source filename supplies the title and stable identity.
 *
 * @see <a href="https://github.com/RealKai42/qwerty-learner/tree/master/public/dicts">Qwerty Learner dictionaries</a>
 */
final class QwertyWordbookAdapter {
    private QwertyWordbookAdapter() {}

    /**
     * Reads an entire source array, preserving spelling, phrases and source order.
     * Revisions using the same filename retain the same ID so the existing import
     * workflow can request confirmation before replacing a book.
     *
     * @param entries Qwerty word objects, each containing a nonblank string {@code name}
     * @param sourceName source filename without its extension
     * @return the normalized wordbook, with case-insensitive duplicates removed
     * @throws IllegalArgumentException if an entry or the resulting wordbook is invalid
     */
    static Wordbook read(JsonArray entries, String sourceName) {
        if (entries.size() > Wordbook.MAX_WORDS) {
            throw new IllegalArgumentException("Qwerty wordbook exceeds " + Wordbook.MAX_WORDS + " entries");
        }
        var words = new ArrayList<String>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            JsonElement entry = entries.get(index);
            if (!entry.isJsonObject()) {
                throw new IllegalArgumentException("Qwerty entry " + (index + 1) + " must be an object");
            }
            JsonElement name = entry.getAsJsonObject().get("name");
            if (name == null || !name.isJsonPrimitive() || !name.getAsJsonPrimitive().isString()
                || name.getAsString().isBlank()) {
                throw new IllegalArgumentException("Qwerty entry " + (index + 1) + " requires a nonblank string name");
            }
            words.add(name.getAsString());
        }
        String normalizedName = sourceName.toLowerCase(Locale.ROOT);
        String title = switch (normalizedName) {
            case "cet4_t" -> "CET-4";
            case "cet6_t" -> "CET-6";
            case "kaoyan_3_t" -> "考研英语";
            case "ielts_3_t" -> "IELTS";
            case "toefl_3_t" -> "TOEFL";
            default -> sourceName;
        };
        String id = "qwerty-" + UUID.nameUUIDFromBytes(normalizedName.getBytes(StandardCharsets.UTF_8));
        return new Wordbook(Wordbook.CURRENT_VERSION, id, title + " · Qwerty", words);
    }
}
