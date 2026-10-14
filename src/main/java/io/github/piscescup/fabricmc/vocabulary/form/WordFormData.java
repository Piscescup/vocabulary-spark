package io.github.piscescup.fabricmc.vocabulary.form;

import com.google.gson.annotations.SerializedName;

/**
 * One word-form spelling retaining the source type label.
 *
 * <p>Null strings become empty strings and supplied strings are stripped of
 * surrounding whitespace. {@link #form()} resolves a recognized type without
 * replacing an unrecognized label stored in this record.
 *
 * @param rawType the source word-form type, or {@code null}
 * @param wordText the spelling for this form, or {@code null}
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public record WordFormData(
    @SerializedName("type") String rawType,
    @SerializedName("text") String wordText
) {
    /**
     * Creates word-form data with stripped, non-null strings.
     *
     * @param rawType the source type label, or {@code null}
     * @param wordText the source spelling, or {@code null}
     */
    public WordFormData {
        rawType = rawType == null ? "" : rawType.strip();
        wordText = wordText == null ? "" : wordText.strip();
    }

    /**
     * Resolves the source type label, ignoring case and extra whitespace.
     *
     * @return the matching word form, or {@link WordForm#UNKNOWN} if the label
     *         is empty or unrecognized
     */
    public WordForm form() {
        return rawType.isEmpty() ? WordForm.UNKNOWN : WordForm.fromLabel(rawType);
    }
}
