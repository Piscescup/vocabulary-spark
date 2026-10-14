package io.github.piscescup.fabricmc.vocabulary.translation;

import com.google.gson.annotations.SerializedName;
import io.github.piscescup.fabricmc.utils.ComponentFormatUtils;
import io.github.piscescup.fabricmc.vocabulary.VocabularyExample;
import io.github.piscescup.fabricmc.vocabulary.pos.GrammarLabel;
import io.github.piscescup.fabricmc.vocabulary.pos.PartOfSpeech;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;

/**
 * A bilingual dictionary sense retaining its source texts and examples.
 * Gson field annotations map the record components to the response fields.
 *
 * <p>Null strings become empty strings and supplied strings are stripped of
 * surrounding whitespace. Grammar labels and examples are stored as unmodifiable
 * copies; null collections become empty collections and null elements are rejected.
 * Grammar labels are used only when supplied, rather than inferred from the text.
 *
 * <p>Display methods create formatted components on request. Unrecognized
 * part-of-speech text remains available through {@link #partOfSpeechLabel()}.
 *
 * @param id the source definition ID, used for ascending ordering
 * @param rawPartOfSpeech the source part-of-speech label, or {@code null}
 * @param englishText the English definition, or {@code null}
 * @param chineseText the Chinese translation, or {@code null}
 * @param grammarLabels the supplied grammar labels, or {@code null} for none
 * @param rawExamples the examples in source order, or {@code null} for none
 * @see TranslationEntry
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public record VocabularyTranslationEntry(
    @SerializedName("id")
    int id,

    @SerializedName("pos")
    String rawPartOfSpeech,

    @SerializedName("text")
    String englishText,

    @SerializedName("translation")
    String chineseText,

    @SerializedName("grammarLabels")
    Set<GrammarLabel> grammarLabels,

    @SerializedName("example")
    List<VocabularyExample> rawExamples
)
    implements TranslationEntry
{
    /**
     * Creates a sense with normalized text and unmodifiable collection snapshots.
     *
     * @param id the source definition ID, used for ascending ordering
     * @param rawPartOfSpeech the source part-of-speech label, or {@code null}
     * @param englishText the English definition, or {@code null}
     * @param chineseText the Chinese translation, or {@code null}
     * @param grammarLabels the supplied grammar labels, or {@code null}
     * @param rawExamples the source examples, or {@code null}
     * @throws NullPointerException if a supplied collection contains a null element
     */
    public VocabularyTranslationEntry {
        rawPartOfSpeech = rawPartOfSpeech == null ? "" : rawPartOfSpeech.strip();
        englishText = englishText == null ? "" : englishText.strip();
        chineseText = chineseText == null ? "" : chineseText.strip();
        grammarLabels = grammarLabels == null ? Set.of() : Set.copyOf(grammarLabels);
        rawExamples = rawExamples == null ? List.of() : List.copyOf(rawExamples);
    }

    /**
     * Resolves the source label to a recognized part of speech.
     *
     * @return the matching constant, or {@link PartOfSpeech#UNKNOWN} for an
     *         empty or unrecognized source label
     */
    @Override
    public PartOfSpeech pos() {
        return rawPartOfSpeech.isEmpty() ? PartOfSpeech.UNKNOWN : PartOfSpeech.fromLabel(rawPartOfSpeech);
    }

    /**
     * Returns the recognized display name, retaining non-empty source text
     * when the part of speech is unrecognized.
     *
     * @return the recognized name, the unrecognized source label, or
     *         {@link PartOfSpeech#UNKNOWN}'s display name if no label was supplied
     */
    @Override
    public String partOfSpeechLabel() {
        PartOfSpeech pos = pos();
        return pos == PartOfSpeech.UNKNOWN && !rawPartOfSpeech.isEmpty()
            ? rawPartOfSpeech : pos.getName();
    }

    /**
     * Formats the stored English definition as a new component.
     *
     * @return a new definition component, empty when the source text is empty
     */
    @Override
    public Component englishDefinition() {
        return ComponentFormatUtils.englishDefinitionFormat(englishText);
    }

    /**
     * Formats the stored Chinese translation as a new component.
     *
     * @return a new translation component, empty when the source text is empty
     */
    @Override
    public Component translation() {
        return ComponentFormatUtils.translationFormat(chineseText);
    }

    /**
     * Formats examples in source order, omitting entries whose English and
     * Chinese texts are both empty. The source list remains unchanged.
     *
     * @return an unmodifiable list of newly created example components
     */
    @Override
    public List<Component> examples() {
        return rawExamples.stream()
            .filter(example -> !example.englishText().isEmpty() || !example.chineseText().isEmpty())
            .<Component>map(VocabularyExample::component)
            .toList();
    }
}
