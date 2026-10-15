package io.github.piscescup.vocabularyspark.vocabulary;


import io.github.piscescup.vocabularyspark.vocabulary.form.WordForm;
import io.github.piscescup.vocabularyspark.vocabulary.pronunciation.Pronunciation;
import io.github.piscescup.vocabularyspark.vocabulary.translation.TranslationEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * A dictionary word with pronunciations, word forms, bilingual senses and
 * learning categories available as display data.
 *
 * <p>Each sense is represented by a {@link TranslationEntry}, which keeps its
 * part of speech, grammar labels, definition and examples together. Categories
 * describe learning groups, such as examination word lists; they are separate
 * from parts of speech and grammar labels.
 *
 * <p>The mutability of returned collections and components is defined by each
 * implementation. See {@link Vocabulary} for the raw-data implementation and
 * its formatted views.
 *
 * @see Vocabulary
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface Word {
    /**
     * Returns the headword formatted for display.
     *
     * @return the display component for this word
     */
    MutableComponent word();

    /**
     * Returns the available regional pronunciations.
     *
     * @return the pronunciations, or an empty list if none are available
     */
    List<Pronunciation> pronunciations();

    /**
     * Returns the available spellings grouped by word-form type.
     *
     * @return word-form types mapped to their display components, or an empty map
     */
    Map<WordForm, Component> forms();

    /**
     * Returns the bilingual senses in the order defined by the returned set.
     *
     * @return the ordered senses, or an empty set if none are available
     * @see TranslationEntry#compareTo(TranslationEntry)
     */
    SortedSet<TranslationEntry> translations();

    /**
     * Returns the learning categories supplied for this word.
     *
     * @return category display components, or an empty list if none were supplied
     */
    List<MutableComponent> categories();
}
