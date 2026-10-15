package io.github.piscescup.vocabularyspark.vocabulary;

import com.google.gson.annotations.SerializedName;
import io.github.piscescup.vocabularyspark.utils.ComponentFormatUtils;
import net.minecraft.network.chat.MutableComponent;

/**
 * The English text and Chinese translation of one example sentence.
 *
 * <p>Null text becomes an empty string, and surrounding whitespace is removed
 * from supplied text. Display formatting is applied by {@link #component()}
 * without changing either stored string.
 *
 * @param englishText the English example, or {@code null} for an empty string
 * @param chineseText the Chinese translation, or {@code null} for an empty string
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public record VocabularyExample(
    @SerializedName("text")
    String englishText,

    @SerializedName("translation")
    String chineseText
) {
    /**
     * Creates an example with stripped, non-null text components.
     *
     * @param englishText the English example, or {@code null}
     * @param chineseText the Chinese translation, or {@code null}
     */
    public VocabularyExample {
        englishText = englishText == null ? "" : englishText.strip();
        chineseText = chineseText == null ? "" : chineseText.strip();
    }

    /**
     * Formats this example with English first and Chinese second.
     * A newline separates the texts only when both are present.
     *
     * @return a new display component, empty if both texts are empty
     */
    public MutableComponent component() {
        return ComponentFormatUtils.exampleFormat(englishText, chineseText);
    }
}
