package io.github.piscescup.vocabularyspark.vocabulary.translation;


import io.github.piscescup.vocabularyspark.vocabulary.pos.GrammarLabel;
import io.github.piscescup.vocabularyspark.vocabulary.pos.PartOfSpeech;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One dictionary sense with a part of speech, an English definition, a Chinese
 * translation, grammar labels and examples.
 *
 * <p>Grammar labels describe the use of this sense, such as countability or
 * transitivity, independently of its part of speech. Implementations may retain
 * an unrecognized source part-of-speech label through {@link #partOfSpeechLabel()}.
 *
 * <p>The natural order compares source IDs first, followed by displayed texts
 * and grammatical information when IDs are equal.
 * Component styling does not distinguish entries in this order. Implementations
 * used in sorted collections must keep all comparison data stable and should
 * make their equality consistent with this ordering.
 *
 * @see #compareTo(TranslationEntry)
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface TranslationEntry
    extends Comparable<TranslationEntry>
{
    /**
     * Returns the source definition ID used as the primary sort key.
     *
     * @return the definition ID supplied by the dictionary
     */
    int id();

    /**
     * Returns the recognized part of speech for this sense.
     *
     * @return the part of speech, or {@link PartOfSpeech#UNKNOWN} if unrecognized
     */
    PartOfSpeech pos();

    /**
     * Returns the part-of-speech label to display for this sense.
     * The default implementation returns {@link PartOfSpeech#getName()} for
     * {@link #pos()}; implementations may return a retained source label instead.
     *
     * @return the display label for the part of speech
     */
    default String partOfSpeechLabel() {
        return pos().getName();
    }

    /**
     * Grammar labels for this sense, such as countability or transitivity.
     * An empty set means that no grammar information was supplied.
     * The default implementation returns an empty unmodifiable set.
     *
     * @return the supplied grammar labels, or an empty set
     */
    default Set<GrammarLabel> grammarLabels() {
        return Set.of();
    }

    /**
     * Returns the Chinese translation formatted for display.
     *
     * @return the translation component, empty if no translation is available
     */
    Component translation();

    /**
     * Returns the English definition separately from the Chinese translation.
     * The default implementation returns an empty component.
     *
     * @return the definition component, empty if no definition is available
     */
    default Component englishDefinition() {
        return Component.empty();
    }

    /**
     * Returns the examples for this sense in their intended display order.
     * An example may contain both its English text and Chinese translation.
     *
     * @return the example components, or an empty list if none are available
     */
    List<Component> examples();

    /**
     * Compares this sense with another sense in natural order.
     * The first difference in the following sequence determines the result:
     * <ol>
     *   <li>Source definition ID, in ascending numeric order.</li>
     *   <li>Part of speech, in enum declaration order.</li>
     *   <li>Part-of-speech display label.</li>
     *   <li>Chinese translation text.</li>
     *   <li>English definition text.</li>
     *   <li>Grammar labels, sorted by code and then name.</li>
     *   <li>Example texts, in list order.</li>
     * </ol>
     * <p>Strings use {@link String#compareTo(String)}. Sequences are compared
     * lexicographically, with a shorter matching prefix ordered first. Grammar
     * set iteration order and component formatting do not affect the result.
     *
     * <p>Implementations should align equality with this ordering when used
     * in sorted sets. Fields used for comparison must remain unchanged while
     * an entry is stored in a sorted collection.</p>
     *
     * @param other the sense to compare with this sense
     * @return a negative integer, zero, or a positive integer if this sense
     *         precedes, compares equally to, or follows {@code other}
     * @throws NullPointerException if {@code other} is null
     */
    @Override
    default int compareTo(@NonNull TranslationEntry other) {
        Objects.requireNonNull(other, "other");
        if (this == other) return 0;

        int result = Integer.compare(id(), other.id());
        if (result != 0) return result;

        result = pos().compareTo(other.pos());
        if (result != 0) return result;

        result = partOfSpeechLabel().compareTo(other.partOfSpeechLabel());
        if (result != 0) return result;

        result = translation().getString().compareTo(other.translation().getString());
        if (result != 0) return result;

        result = englishDefinition().getString().compareTo(other.englishDefinition().getString());
        if (result != 0) return result;

        Comparator<GrammarLabel> labelOrder = Comparator.comparing(GrammarLabel::code)
            .thenComparing(GrammarLabel::name);
        result = compareSequences(
            grammarLabels().stream().sorted(labelOrder).toList(),
            other.grammarLabels().stream().sorted(labelOrder).toList(),
            labelOrder
        );
        if (result != 0) return result;

        return compareSequences(
            examples(), other.examples(), Comparator.comparing(Component::getString)
        );
    }

    /**
     * Compares sequences lexicographically using the supplied element order.
     *
     * @param <T> the element type
     * @param left the first sequence
     * @param right the second sequence
     * @param order the comparator applied to corresponding elements
     * @return the first non-zero element comparison, or the relative sequence
     *         lengths when all corresponding elements compare equally
     */
    private static <T> int compareSequences(
        Iterable<? extends T> left,
        Iterable<? extends T> right,
        Comparator<? super T> order
    ) {
        var leftIterator = left.iterator();
        var rightIterator = right.iterator();
        while (leftIterator.hasNext() && rightIterator.hasNext()) {
            int result = order.compare(leftIterator.next(), rightIterator.next());
            if (result != 0) return result;
        }
        return Boolean.compare(leftIterator.hasNext(), rightIterator.hasNext());
    }
}
