package io.github.piscescup.vocabularyspark.vocabulary.pos;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Grammar information attached to an individual sense, separately from its
 * part of speech. One sense may have several labels, or no supplied labels.
 * Combined codes retain their meaning, for example {@code C or U}.
 *
 * <p>Construction collapses Unicode whitespace and strips surrounding
 * whitespace from both components. One enclosing pair of square brackets is
 * removed from the code. Empty text and null arguments are rejected.
 *
 * <p>{@link #fromCode(String)} resolves built-in codes without regard to case
 * and retains normalized text for unrecognized codes. The record constructor
 * can also be used directly to provide a custom display name.
 *
 * @param code the non-blank dictionary code, optionally enclosed in square brackets
 * @param name the non-blank human-readable display name
 * @see PartOfSpeech
 * @see <a href="https://dictionary.cambridge.org/help/codes.html">Cambridge labels and codes</a>
 * @author REN YuanTong
 * @since 1.0.0
 */
public record GrammarLabel(String code, String name) {
    /** A countable noun sense, represented by {@code [C]}. */
    public static final GrammarLabel COUNTABLE = new GrammarLabel("C", "Countable");
    /** An uncountable noun sense, represented by {@code [U]}. */
    public static final GrammarLabel UNCOUNTABLE = new GrammarLabel("U", "Uncountable");
    /** A noun sense that may be countable or uncountable, represented by {@code [C or U]}. */
    public static final GrammarLabel COUNTABLE_OR_UNCOUNTABLE =
        new GrammarLabel("C or U", "Countable or Uncountable");
    /** A singular noun use, represented by {@code [S]}. */
    public static final GrammarLabel SINGULAR = new GrammarLabel("S", "Singular");
    /** A plural noun use, represented by {@code [plural]}. */
    public static final GrammarLabel PLURAL = new GrammarLabel("plural", "Plural");
    /** A transitive verb sense, represented by {@code [T]}. */
    public static final GrammarLabel TRANSITIVE = new GrammarLabel("T", "Transitive");
    /** An intransitive verb sense, represented by {@code [I]}. */
    public static final GrammarLabel INTRANSITIVE = new GrammarLabel("I", "Intransitive");
    /** A verb sense allowing either use, represented by {@code [I or T]}. */
    public static final GrammarLabel TRANSITIVE_OR_INTRANSITIVE =
        new GrammarLabel("I or T", "Intransitive or Transitive");
    /** A linking verb sense, represented by {@code [L]}. */
    public static final GrammarLabel LINKING = new GrammarLabel("L", "Linking");
    /** A non-gradable use, represented by {@code [not gradable]}. */
    public static final GrammarLabel NOT_GRADABLE = new GrammarLabel("not gradable", "Not Gradable");
    /** A use that is usually passive, represented by {@code [usually passive]}. */
    public static final GrammarLabel USUALLY_PASSIVE = new GrammarLabel("usually passive", "Usually Passive");

    private static final Map<String, GrammarLabel> KNOWN_CODES = Stream.of(
        COUNTABLE, UNCOUNTABLE, COUNTABLE_OR_UNCOUNTABLE, SINGULAR, PLURAL,
        TRANSITIVE, INTRANSITIVE, TRANSITIVE_OR_INTRANSITIVE, LINKING,
        NOT_GRADABLE, USUALLY_PASSIVE
    ).collect(Collectors.toUnmodifiableMap(
        label -> label.code().toLowerCase(Locale.ROOT), Function.identity()
    ));

    /**
     * Creates a grammar label with normalized code and name text.
     *
     * @param code the code, optionally enclosed in one pair of square brackets
     * @param name the display name
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if either argument is blank, or the code
     *         is empty after removing its enclosing brackets
     */
    public GrammarLabel {
        code = requireText(code, "code");
        if (code.startsWith("[") && code.endsWith("]")) {
            code = requireText(code.substring(1, code.length() - 1), "code");
        }
        name = requireText(name, "name");
    }

    /**
     * Reads a dictionary code with optional square brackets. Unknown codes
     * retain their text instead of being discarded or guessed.
     *
     * <p>Built-in codes are matched without regard to case. For other codes,
     * both the code and display name use the normalized input code, preserving
     * its case and keeping combined codes as a single label.
     *
     * @param code the source code, optionally enclosed in square brackets
     * @return the matching built-in label, or a new label for an unknown code
     * @throws NullPointerException if {@code code} is null
     * @throws IllegalArgumentException if {@code code} is blank or empty after
     *         removing its enclosing brackets
     */
    public static GrammarLabel fromCode(String code) {
        GrammarLabel label = new GrammarLabel(code, code);
        GrammarLabel known = KNOWN_CODES.get(label.code().toLowerCase(Locale.ROOT));
        return known != null ? known : new GrammarLabel(label.code(), label.code());
    }

    /**
     * Returns the code enclosed in square brackets for dictionary-style display.
     *
     * @return the bracketed code, such as {@code "[C or U]"}
     */
    public String getDisplayLabel() {
        return "[" + code + "]";
    }

    /**
     * Collapses Unicode whitespace and requires a non-empty normalized value.
     *
     * @param value the source text
     * @param field the field name used in exception messages
     * @return the normalized, non-empty text
     * @throws NullPointerException if {@code value} is null
     * @throws IllegalArgumentException if the normalized value is empty
     */
    private static String requireText(String value, String field) {
        String text = Objects.requireNonNull(value, field)
            .replaceAll("(?U)\\s+", " ").strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return text;
    }
}
