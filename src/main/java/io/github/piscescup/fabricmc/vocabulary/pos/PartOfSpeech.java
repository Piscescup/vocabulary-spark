package io.github.piscescup.fabricmc.vocabulary.pos;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A dictionary part of speech with a title-case display name.
 *
 * <p>Built-in names follow Cambridge Dictionary's labels. Abbreviations are
 * display conventions used by this mod, not values supplied by Cambridge.
 * Grammar codes such as {@code [C]}, {@code [U]} and {@code [T]} are separate
 * usage information, not additional parts of speech.</p>
 *
 * <p>Declaration order defines the part-of-speech order used when comparing
 * translation entries. Unrecognized source labels can be represented by
 * {@link #UNKNOWN} while retaining their original text separately.
 *
 * @see GrammarLabel
 * @see <a href="https://dictionary.cambridge.org/help/codes.html">Cambridge labels and codes</a>
 * @author REN YuanTong
 * @since 1.0.0
 */
public enum PartOfSpeech {
    /** A noun. */
    NOUN("Noun", "N."),
    /** A pronoun. */
    PRONOUN("Pronoun", "Pron."),
    /** A verb. */
    VERB("Verb", "V."),
    /** An adjective. */
    ADJECTIVE("Adjective", "Adj."),
    /** An adverb. */
    ADVERB("Adverb", "Adv."),
    /** A preposition. */
    PREPOSITION("Preposition", "Prep."),
    /** A conjunction. */
    CONJUNCTION("Conjunction", "Conj."),
    /** An exclamation, also referred to as an interjection. */
    EXCLAMATION("Exclamation", "Exclam."),
    /** A determiner. */
    DETERMINER("Determiner", "Det."),
    /** A predeterminer. */
    PREDETERMINER("Predeterminer", "Predet."),
    /** A number, also referred to as a numeral. */
    NUMBER("Number", "Num."),
    /** An ordinal number. */
    ORDINAL_NUMBER("Ordinal Number", "Ord. Num."),
    /** An auxiliary verb. */
    AUXILIARY_VERB("Auxiliary Verb", "Aux. V."),
    /** A modal verb. */
    MODAL_VERB("Modal Verb", "Modal V."),
    /** A phrasal verb. */
    PHRASAL_VERB("Phrasal Verb", "Phr. V."),
    /** A prefix. */
    PREFIX("Prefix", "Pref."),
    /** A suffix. */
    SUFFIX("Suffix", "Suff."),
    /** An article; retained for dictionary sources that use this label. */
    ARTICLE("Article", "Art."),
    /** A particle; retained for dictionary sources that use this label. */
    PARTICLE("Particle", "Part."),
    /** A missing or unrecognized part of speech. */
    UNKNOWN("Unknown", "Unknown");

    /**
     * A compatibility alias for {@link #EXCLAMATION}, not a separate enum constant.
     *
     * @deprecated Use Cambridge's {@link #EXCLAMATION} label.
     */
    @Deprecated
    public static final PartOfSpeech INTERJECTION = EXCLAMATION;

    /**
     * A compatibility alias for {@link #NUMBER}, not a separate enum constant.
     *
     * @deprecated Use Cambridge's {@link #NUMBER} label.
     */
    @Deprecated
    public static final PartOfSpeech NUMERAL = NUMBER;

    private static final Map<String, PartOfSpeech> KNOWN_LABELS = Stream.of(values())
        .collect(Collectors.toUnmodifiableMap(
            pos -> pos.getName().toLowerCase(Locale.ROOT), Function.identity()
        ));

    private final String name;
    private final String abbreviation;

    /**
     * Creates a part of speech with its display name and abbreviation.
     *
     * @param name the title-case display name
     * @param abbreviation the abbreviation used by this mod
     */
    PartOfSpeech(String name, String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }

    /**
     * Converts an API label to a known constant when possible, ignoring case
     * and extra whitespace. Unrecognized labels return {@link #UNKNOWN};
     * callers should retain the original label separately when importing data.
     *
     * <p>The aliases {@code "interjection"} and {@code "numeral"} resolve to
     * {@link #EXCLAMATION} and {@link #NUMBER}, respectively. Abbreviations such
     * as {@code "N."} are not recognized by this method.
     *
     * @param label the source part-of-speech label
     * @return the recognized constant, or {@link #UNKNOWN} if unrecognized
     * @throws NullPointerException if {@code label} is null
     * @throws IllegalArgumentException if {@code label} is blank
     */
    public static PartOfSpeech fromLabel(String label) {
        String normalized = Objects.requireNonNull(label, "label")
            .replaceAll("(?U)\\s+", " ").strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        String key = normalized.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "interjection" -> EXCLAMATION;
            case "numeral" -> NUMBER;
            default -> KNOWN_LABELS.getOrDefault(key, UNKNOWN);
        };
    }

    /**
     * Returns the full display name of this part of speech.
     *
     * @return the title-case name, such as {@code "Ordinal Number"}
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the abbreviation used by this mod for display.
     *
     * @return the display abbreviation, such as {@code "Ord. Num."}
     */
    public String getAbbreviation() {
        return abbreviation;
    }
}
