package io.github.piscescup.vocabularyspark.gui.widget;

import com.thecsdev.commonmc.api.client.gui.TElement;
import io.github.piscescup.vocabularyspark.vocabulary.pos.GrammarLabel;
import io.github.piscescup.vocabularyspark.vocabulary.translation.TranslationEntry;

import java.util.stream.Collectors;

import static io.github.piscescup.vocabularyspark.gui.widget.DictionaryTextStyles.*;

/**
 * One complete bilingual sense, followed by its indented bilingual examples.
 */
public final class VocabularySenseWidget extends TElement {
    public VocabularySenseWidget(
        TranslationEntry sense, int number, boolean numbered,
        int x, int y, int width
    ) {
        String grammar = sense.grammarLabels()
            .stream()
            .map(GrammarLabel::code)
            .sorted()
            .map(code -> "[" + code + "]")
            .collect(Collectors.joining(" "));
        var definitionLabel = Labels.paragraph(
            dictionarySenseFormat(
                number, numbered, grammar, sense.englishDefinition(), sense.translation()),
            x, y, width, DICTIONARY_TEXT_COLOR, DICTIONARY_BODY_SCALE, false
        );
        add(definitionLabel);
        int bottom = definitionLabel.getBounds().endY;
        var examples = sense.examples()
            .stream()
            .filter(example -> !example.getString()
                .isBlank())
            .toList();
        for (var example : examples) {
            var exampleLabel = Labels.paragraph(
                dictionaryExampleFormat(example), x + 5, bottom + 5,
                Math.max(1, width - 5), DICTIONARY_TEXT_COLOR, DICTIONARY_BODY_SCALE, false
            );
            add(exampleLabel);
            bottom = exampleLabel.getBounds().endY;
        }
        setBounds(x, y, width, Math.max(1, bottom - y));
    }
}
