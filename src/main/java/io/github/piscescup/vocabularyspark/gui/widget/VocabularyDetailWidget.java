package io.github.piscescup.vocabularyspark.gui.widget;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.misc.TFillColorElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import io.github.piscescup.vocabularyspark.client.audio.PronunciationPlayer;
import io.github.piscescup.vocabularyspark.vocabulary.Word;
import io.github.piscescup.vocabularyspark.vocabulary.pronunciation.Pronunciation;
import io.github.piscescup.vocabularyspark.vocabulary.translation.TranslationEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.network.chat.Component;

import static io.github.piscescup.vocabularyspark.gui.widget.DictionaryTextStyles.*;
import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/**
 * Dictionary page: contents sidebar, original headword, pronunciations, POS tabs and senses.
 */
public final class VocabularyDetailWidget extends TElement {
    private final Word vocabulary;
    private final Runnable back;
    private final PronunciationPlayer pronunciationPlayer;
    private final Map<String, List<TranslationEntry>> senses = new LinkedHashMap<>();
    private String selectedPos;
    private boolean showForms;

    public VocabularyDetailWidget(Word vocabulary, Runnable back, PronunciationPlayer pronunciationPlayer) {
        this.vocabulary = Objects.requireNonNull(vocabulary);
        this.back = Objects.requireNonNull(back);
        this.pronunciationPlayer = Objects.requireNonNull(pronunciationPlayer);
        for (var sense : vocabulary.translations()) {
            if (sense.englishDefinition()
                .getString()
                .isBlank() && sense.translation()
                .getString()
                .isBlank()) {
                continue;
            }
            String pos = sense.pos()
                .getName();
            senses.computeIfAbsent(pos, ignored -> new ArrayList<>())
                .add(sense);
        }
        selectedPos = senses.keySet()
            .stream()
            .findFirst()
            .orElse(null);
    }

    @Override
    protected void initCallback() {
        var bb = getBounds();
        int sidebar = bb.width >= 360 ? 86 : 0;
        add(new DictionaryNavigationWidget(
            text("detail_back"), bb.x + 6, bb.y + 3, 90, 19,
            true, false, back
        ));
        var title = Labels.label(
            dictionaryHeadingFormat(text("detail_title")), bb.x + 103, bb.y + 3,
            bb.width - 111, 19, DICTIONARY_WORD_COLOR, false
        );
        add(title);
        title.textScaleProperty()
            .set(DICTIONARY_BODY_SCALE, VocabularyDetailWidget.class);

        if (sidebar > 0) {
            add(Labels.paragraph(
                dictionaryHeadingFormat(text("detail_contents")), bb.x + 8, bb.y + 34,
                sidebar - 16, DICTIONARY_WORD_COLOR, DICTIONARY_BODY_SCALE, false
            ));
            add(Buttons.dictionaryButton(
                text("detail_definitions"), bb.x + 6, bb.y + 51,
                sidebar - 12, 20, !showForms, () -> selectSection(false)
            ));
            add(Buttons.dictionaryButton(
                text("detail_forms"), bb.x + 6, bb.y + 75,
                sidebar - 12, 20, showForms, () -> selectSection(true)
            ));
        }

        var content = new ScrollPanelWidget(
            bb.x + sidebar + 5, bb.y + 29,
            bb.width - sidebar - 10, bb.height - 34, panel -> populateContent(panel, sidebar == 0)
        );
        content.getPanel()
            .scrollPaddingProperty()
            .set(8, VocabularyDetailWidget.class);
        content.getPanel()
            .backgroundColorProperty()
            .set(0, VocabularyDetailWidget.class);
        content.getPanel()
            .outlineColorProperty()
            .set(0, VocabularyDetailWidget.class);
        content.getPanel()
            .focusedOutlineColorProperty()
            .set(0, VocabularyDetailWidget.class);
        add(content);
    }

    private void populateContent(TPanelElement panel, boolean compact) {
        var bb = panel.getBounds();
        int x = bb.x + 8, y = bb.y + 8, width = Math.max(1, bb.width - 16);
        if (compact) {
            int buttonWidth = Math.min(76, Math.max(1, (width - 5) / 2));
            panel.add(Buttons.dictionaryButton(
                text("detail_definitions"), x, y, buttonWidth, 19,
                !showForms, () -> selectSection(false)
            ));
            panel.add(Buttons.dictionaryButton(
                text("detail_forms"), x + buttonWidth + 5, y, buttonWidth, 19,
                showForms, () -> selectSection(true)
            ));
            y += 25;
        }

        var headword = Labels.paragraph(
            headwordFormat(vocabulary.word()),
            x, y, width, DICTIONARY_WORD_COLOR, DICTIONARY_HEADWORD_SCALE, false
        );
        panel.add(headword);
        y = headword.getBounds().endY + 5;
        var pronunciations = new LinkedHashMap<PronunciationKey, Pronunciation>();
        for (var pronunciation : vocabulary.pronunciations()) {
            var area = pronunciation.area();
            var symbol = pronunciation.phoneticSymbol();
            pronunciations.putIfAbsent(
                new PronunciationKey(
                    area.getString(), symbol.getString(),
                    pronunciation.pronunciationUrl()
                        .toExternalForm()
                ), pronunciation
            );
        }
        if (!pronunciations.isEmpty()) {
            int pronunciationX = x, pronunciationY = y, bottom = y;
            for (var pronunciation : pronunciations.values()) {
                var button = new PronunciationWidget(pronunciation, pronunciationPlayer);
                button.setBoundsToFitContent(pronunciationX, pronunciationY, width);
                if (pronunciationX > x && button.getBounds().endX > x + width) {
                    pronunciationX = x;
                    pronunciationY = bottom + 3;
                    button.setBoundsToFitContent(pronunciationX, pronunciationY, width);
                }
                panel.add(button);
                bottom = Math.max(bottom, button.getBounds().endY);
                pronunciationX = button.getBounds().endX + 8;
            }
            y = bottom + 6;
        }
        var categories = vocabulary.categories();
        if (!categories.isEmpty()) {
            var categoryText = Component.empty();
            for (var category : categories) {
                if (!categoryText.getString()
                    .isEmpty()) {
                    categoryText.append(" / ");
                }
                categoryText.append(category.copy());
            }
            var categoryLabel = Labels.paragraph(
                dictionaryNavigationFormat(categoryText, false), x, y, width,
                DICTIONARY_MUTED_COLOR, DICTIONARY_BODY_SCALE, false
            );
            panel.add(categoryLabel);
            y = categoryLabel.getBounds().endY + 6;
        }

        if (showForms) {
            populateForms(panel, x, y, width);
            return;
        }

        int tabX = x, tabY = y;
        for (String pos : senses.keySet()) {
            var tab = new DictionaryNavigationWidget(
                Component.literal(pos), tabX, tabY, 44, 19,
                pos.equals(selectedPos), true, () -> selectPos(pos)
            );
            int tabWidth = Math.min(width, Math.max(
                    34, tab.getLabel()
                        .getTextWidth() + 14
                )
            );
            if (tabX > x && tabX + tabWidth > x + width) {
                tabX = x;
                tabY += 23;
            }
            tab.setBounds(tabX, tabY, tabWidth, 19);
            panel.add(tab);
            tabX += tabWidth + 4;
        }
        var divider = new TFillColorElement.Flat(DICTIONARY_LINE_COLOR, 0);
        divider.setBounds(x, tabY + 19, width, 1);
        panel.add(divider);
        y = tabY + 27;
        if (selectedPos == null) {
            panel.add(Labels.paragraph(
                dictionaryNavigationFormat(text("detail_no_definitions"), false),
                x, y, width, DICTIONARY_MUTED_COLOR, DICTIONARY_BODY_SCALE, false
            ));
            return;
        }
        List<TranslationEntry> selected = senses.get(selectedPos);
        for (int index = 0; index < selected.size(); index++) {
            if (index > 0) {
                var separator = new TFillColorElement.Flat(DICTIONARY_LINE_COLOR, 0);
                separator.setBounds(x, y, width, 1);
                panel.add(separator);
                y += 7;
            }
            var entry = selected.get(index);
            var sense = new VocabularySenseWidget(
                entry, index + 1, selected.size() > 1,
                x, y, width
            );
            panel.add(sense);
            y = sense.getBounds().endY + 9;
        }
    }

    private void populateForms(TPanelElement panel, int x, int y, int width) {
        var heading = Labels.paragraph(
            dictionaryHeadingFormat(text("detail_forms")), x, y, width,
            DICTIONARY_WORD_COLOR, DICTIONARY_BODY_SCALE, false
        );
        panel.add(heading);
        y = heading.getBounds().endY + 8;
        var forms = vocabulary.forms();
        if (forms.isEmpty()) {
            panel.add(Labels.paragraph(
                text("detail_no_forms"), x, y, width, DICTIONARY_MUTED_COLOR,
                DICTIONARY_BODY_SCALE, false
            ));
            return;
        }
        for (var form : forms.entrySet()) {
            var formLabel = Labels.paragraph(
                dictionaryFormFormat(
                    form.getKey()
                        .getForm(), form.getValue()
                ), x, y,
                width, DICTIONARY_TEXT_COLOR, DICTIONARY_BODY_SCALE, false
            );
            panel.add(formLabel);
            y = formLabel.getBounds().endY + 6;
        }
    }

    private record PronunciationKey(String area, String symbol, String audioUrl) {
    }

    private void selectSection(boolean forms) {
        showForms = forms;
        clearAndInit();
    }

    private void selectPos(String pos) {
        selectedPos = pos;
        showForms = false;
        clearAndInit();
    }

    @Override
    public void renderCallback(TGuiGraphics pencil) {
        var bb = getBounds();
        pencil.fillColor(bb.x, bb.y, bb.width, bb.height, DICTIONARY_BACKGROUND);
        pencil.fillColor(bb.x, bb.y + 25, bb.width, 1, DICTIONARY_LINE_COLOR);
        if (bb.width >= 360) {
            pencil.fillColor(bb.x + 86, bb.y + 26, 1, bb.height - 26, DICTIONARY_LINE_COLOR);
        }
    }
}
