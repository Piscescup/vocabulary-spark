package io.github.piscescup.fabricmc.vocabulary;

import com.google.gson.annotations.SerializedName;
import io.github.piscescup.fabricmc.utils.ComponentFormatUtils;
import io.github.piscescup.fabricmc.vocabulary.form.WordForm;
import io.github.piscescup.fabricmc.vocabulary.form.WordFormData;
import io.github.piscescup.fabricmc.vocabulary.pronunciation.Pronunciation;
import io.github.piscescup.fabricmc.vocabulary.pronunciation.PronunciationData;
import io.github.piscescup.fabricmc.vocabulary.translation.TranslationEntry;
import io.github.piscescup.fabricmc.vocabulary.translation.VocabularyTranslationEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A dictionary word storing source data separately from its formatted views.
 * Gson field annotations map the components to the Cambridge API response.
 *
 * <p>The headword is stripped of surrounding whitespace. Null collections
 * become empty lists; other collections are copied into unmodifiable lists
 * that preserve source order and reject null elements. Examination categories
 * are retained only when supplied; no category is inferred from definitions.
 *
 * <p>The {@link Word} methods create display views on request. Changes to a
 * returned display component do not change the stored source data. The raw
 * translation list retains source order and duplicates, whereas
 * {@link #translations()} sorts senses and merges entries that compare equally.
 *
 * @param rawWord the non-blank headword, stripped before storage
 * @param rawPronunciations the source pronunciations, or {@code null} for none
 * @param rawTranslations the source senses, or {@code null} for none
 * @param rawForms the source word forms, or {@code null} for none
 * @param rawCategories the supplied learning categories, or {@code null} for none
 * @see Word
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public record Vocabulary(
    @SerializedName("word")
    String rawWord,

    @SerializedName("pronunciation")
    List<PronunciationData> rawPronunciations,

    @SerializedName("definition")
    List<VocabularyTranslationEntry> rawTranslations,

    @SerializedName("verbs")
    List<WordFormData> rawForms,

    @SerializedName("categories")
    List<String> rawCategories
)
    implements Word
{
    /**
     * Creates a word from source data, normalizing the headword and copying
     * each collection into an unmodifiable list.
     *
     * @param rawWord the headword to strip and store
     * @param rawPronunciations the source pronunciations, or {@code null}
     * @param rawTranslations the source senses, or {@code null}
     * @param rawForms the source word forms, or {@code null}
     * @param rawCategories the learning categories, or {@code null}
     * @throws IllegalArgumentException if {@code rawWord} is null or blank
     * @throws NullPointerException if a supplied collection contains a null element
     */
    public Vocabulary {
        if (rawWord == null || rawWord.isBlank()) {
            throw new IllegalArgumentException("Vocabulary must contain a non-blank word");
        }
        rawWord = rawWord.strip();
        rawPronunciations = rawPronunciations == null ? List.of() : List.copyOf(rawPronunciations);
        rawTranslations = rawTranslations == null ? List.of() : List.copyOf(rawTranslations);
        rawForms = rawForms == null ? List.of() : List.copyOf(rawForms);
        rawCategories = rawCategories == null ? List.of() : List.copyOf(rawCategories);
    }

    /**
     * Creates a word without any word-form data.
     *
     * @param word the non-blank headword
     * @param pronunciations the source pronunciations, or {@code null}
     * @param translations the source senses, or {@code null}
     * @param categories the learning categories, or {@code null}
     * @throws IllegalArgumentException if {@code word} is null or blank
     * @throws NullPointerException if a supplied collection contains a null element
     */
    public Vocabulary(
        String word,
        List<PronunciationData> pronunciations,
        List<VocabularyTranslationEntry> translations,
        List<String> categories
    ) {
        this(word, pronunciations, translations, List.of(), categories);
    }

    /**
     * Returns a new bold component containing the normalized headword.
     *
     * @return a newly created headword component
     */
    @Override
    public MutableComponent word() {
        return ComponentFormatUtils.wordFormat(rawWord);
    }

    /**
     * Converts complete source pronunciations into an unmodifiable display list.
     * Entries missing a region, phonetic symbol or audio URL are omitted; the
     * remaining entries retain their source order.
     *
     * @return the available pronunciations, or an empty unmodifiable list
     * @throws IllegalArgumentException if a complete entry has an invalid audio URL
     * @see PronunciationData#pronunciation()
     */
    @Override
    public List<Pronunciation> pronunciations() {
        return rawPronunciations.stream()
            .map(PronunciationData::pronunciation)
            .flatMap(Optional::stream)
            .toList();
    }

    /**
     * Groups non-empty spellings into an unmodifiable map ordered by the
     * declaration order of {@link WordForm} constants.
     *
     * <p>Spellings with the same type retain their source order and are joined
     * with {@code ", "}. Unrecognized types share the {@link WordForm#UNKNOWN}
     * key; their original labels remain available in {@link #rawForms()}.
     *
     * @return a new map of word-form types to formatted spellings
     */
    @Override
    public Map<WordForm, Component> forms() {
        Map<WordForm, Component> components = new EnumMap<>(WordForm.class);
        Map<WordForm, List<String>> groups = new EnumMap<>(WordForm.class);
        for (WordFormData form : rawForms) {
            if (!form.wordText().isEmpty()) {
                groups.computeIfAbsent(form.form(), ignored -> new ArrayList<>()).add(form.wordText());
            }
        }
        groups.forEach((form, words) -> components.put(
            form, ComponentFormatUtils.wordFormFormat(String.join(", ", words))
        ));
        return Collections.unmodifiableMap(components);
    }

    /**
     * Returns an unmodifiable set of senses in their natural order.
     * Entries for which {@link TranslationEntry#compareTo(TranslationEntry)}
     * returns zero occupy a single position in the set.
     *
     * @return a new sorted set of the source senses
     * @see #rawTranslations()
     */
    @Override
    public SortedSet<TranslationEntry> translations() {
        SortedSet<TranslationEntry> translations = new TreeSet<>(rawTranslations);
        return Collections.unmodifiableSortedSet(translations);
    }

    /**
     * Formats the supplied learning categories in source order.
     *
     * @return an unmodifiable list containing newly created category components
     */
    @Override
    public List<MutableComponent> categories() {
        return rawCategories.stream().map(Component::literal).toList();
    }
}
