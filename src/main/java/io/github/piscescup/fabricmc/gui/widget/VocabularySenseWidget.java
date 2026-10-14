package io.github.piscescup.fabricmc.gui.widget;

import com.thecsdev.commonmc.api.client.gui.TElement;
import io.github.piscescup.fabricmc.vocabulary.pos.GrammarLabel;
import io.github.piscescup.fabricmc.vocabulary.translation.TranslationEntry;

import java.util.stream.Collectors;

import static io.github.piscescup.fabricmc.gui.widget.DictionaryTextStyles.*;

/** One complete bilingual sense, followed by its indented bilingual examples. */
public final class VocabularySenseWidget extends TElement {
    public VocabularySenseWidget(TranslationEntry sense, int number, boolean numbered,
                                 int x, int y, int width) {
        String grammar = sense.grammarLabels().stream().map(GrammarLabel::code).sorted()
            .map(code -> "[" + code + "]").collect(Collectors.joining(" "));
        int bottom = new ParagraphWidget(dictionarySenseFormat(
            number, numbered, grammar, sense.englishDefinition(), sense.translation()),
            x, y, width, DICTIONARY_TEXT_COLOR, DICTIONARY_BODY_SCALE)
            .withoutShadow().addTo(this).getBounds().endY;
        var examples = sense.examples().stream().filter(example -> !example.getString().isBlank()).toList();
        for (var example : examples) {
            bottom = new ParagraphWidget(dictionaryExampleFormat(example), x + 5, bottom + 5,
                Math.max(1, width - 5), DICTIONARY_TEXT_COLOR, DICTIONARY_BODY_SCALE)
                .withoutShadow().addTo(this).getBounds().endY;
        }
        setBounds(x, y, width, Math.max(1, bottom - y));
    }
}
