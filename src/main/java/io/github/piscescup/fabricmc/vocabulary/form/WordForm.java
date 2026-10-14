package io.github.piscescup.fabricmc.vocabulary.form;

import com.google.gson.annotations.SerializedName;
import io.github.piscescup.fabricmc.utils.ComponentFormatUtils;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Objects;

/**
 * A recognized word-form type with a title-case display name.
 *
 * <p>Gson annotations define the exact serialized names of the constants.
 * {@link #fromLabel(String)} additionally accepts differences in case and
 * whitespace, returning {@link #UNKNOWN} for unrecognized non-blank labels.
 * Gson does not automatically use that method when reading an enum value.
 *
 * <p>Declaration order determines the iteration order of enum-based form maps.
 * The formatted label is stored internally; {@link #getForm()} returns a copy
 * so that caller changes cannot modify the shared label.
 *
 * @see WordFormData
 * @author REN YuanTong
 * @since 1.0.0
 */
public enum WordForm {
    /** The plain or base form of a word. */
    @SerializedName("Plain form")
    PLAIN_FORM("Plain Form"),

    /** The third-person singular form of a verb. */
    @SerializedName("Third-person singular")
    THIRD_PERSON_SINGULAR("Third-Person Singular"),

    /** The past-tense form of a verb. */
    @SerializedName("Past tense")
    PAST_TENSE("Past Tense"),

    /** The past participle of a verb. */
    @SerializedName("Past participle")
    PAST_PARTICIPLE("Past Participle"),

    /** The present participle of a verb. */
    @SerializedName("Present participle")
    PRESENT_PARTICIPLE("Present Participle"),

    /** A singular form. */
    @SerializedName("Singular")
    SINGULAR("Singular"),

    /** A plural form. */
    @SerializedName("Plural")
    PLURAL("Plural"),

    /** The comparative form of an adjective or adverb. */
    @SerializedName("Comparative")
    COMPARATIVE("Comparative"),

    /** The superlative form of an adjective or adverb. */
    @SerializedName("Superlative")
    SUPERLATIVE("Superlative"),

    /** A missing or unrecognized word-form type. */
    @SerializedName("Unknown")
    UNKNOWN("Unknown");

    private final Component form;

    /**
     * Creates a word-form type with an internally stored formatted label.
     *
     * @param form the display name for this type
     */
    WordForm(String form) {
        this.form = ComponentFormatUtils.wordFormFormat(form);
    }

    /**
     * Returns a copy of this type's bold, dark-gray display label.
     *
     * @return a formatted label whose modification does not affect this constant
     */
    public Component getForm() {
        return form.copy();
    }

    /**
     * Returns the plain display name of this type.
     *
     * @return the title-case label, such as {@code "Past Participle"}
     */
    public String getName() {
        return form.getString();
    }

    /**
     * Reads an API word-form type, ignoring case and extra whitespace.
     * Unknown types return {@link #UNKNOWN}; retain the source label separately
     * when importing an unrecognized type. Gson annotations map exact wire names;
     * importers needing this fallback should call this method explicitly.
     *
     * @param label the source word-form type label
     * @return the matching constant, or {@link #UNKNOWN} if unrecognized
     * @throws NullPointerException if {@code label} is null
     * @throws IllegalArgumentException if {@code label} is blank
     */
    public static WordForm fromLabel(String label) {
        String normalized = Objects.requireNonNull(label, "label")
            .replaceAll("(?U)\\s+", " ")
            .strip()
            .toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        for (WordForm form : values()) {
            if (form.getName().toLowerCase(Locale.ROOT).equals(normalized)) {
                return form;
            }
        }
        return UNKNOWN;
    }
}
