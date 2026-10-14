package io.github.piscescup.fabricmc.gui.widget;

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.widget.TClickableWidget;
import io.github.piscescup.fabricmc.vocabulary.Word;
import io.github.piscescup.fabricmc.vocabulary.translation.TranslationEntry;

import static io.github.piscescup.fabricmc.utils.ComponentUtils.text;

/**
 * Reusable word / part-of-speech / translation row drawn entirely with TCDCommons.
 */
public final class VocabularyEntryElement extends TClickableWidget {
    private final TLabelElement word;
    private final TLabelElement partOfSpeech;
    private final TLabelElement translation;

    public VocabularyEntryElement(Word vocabulary, TranslationEntry sense, Runnable openDetails) {
        word = new TLabelElement(DictionaryTextStyles.headwordFormat(vocabulary.word()));
        String pos = sense.pos().getName();
        partOfSpeech = new TLabelElement(DictionaryTextStyles.partOfSpeechFormat(pos));
        translation = new TLabelElement(sense.translation().getString().isBlank()
            ? sense.englishDefinition() :
            sense.translation()
        );
        for (var label : new TLabelElement[]{word, partOfSpeech, translation}) {
            label.wrapTextProperty()
                .set(true, VocabularyEntryElement.class);

            label.textAlignmentProperty()
                .set(CompassDirection.NORTH_WEST, VocabularyEntryElement.class);
        }
        tooltipProperty().set(ignored -> TTooltip.of(text("search_open_details")), VocabularyEntryElement.class);
        eClicked.addListener(ignored -> openDetails.run());
    }

    public static int wordWidth(int width) {
        return Math.max(55, width / 4);
    }

    public static int posWidth(int width) {
        return Math.max(42, width / 6);
    }

    /**
     * Measure wrapping before attaching the row, keeping subsequent rows below all three columns.
     */
    public void setBoundsToFitContent(int x, int y, int width) {
        int wordWidth = wordWidth(width), posWidth = posWidth(width);
        word.setBoundsToFitText(x + 8, y + 8, wordWidth - 12);
        partOfSpeech.setBoundsToFitText(x + wordWidth, y + 8, posWidth - 8);
        translation.setBoundsToFitText(
            x + wordWidth + posWidth, y + 8,
            Math.max(1, width - wordWidth - posWidth - 8)
        );
        int bottom = Math.max(
            word.getBounds().endY,
            Math.max(partOfSpeech.getBounds().endY, translation.getBounds().endY)
        );
        setBounds(x, y, width, Math.max(28, bottom - y + 8));
    }

    @Override
    protected void initCallback() {
        add(word);
        add(partOfSpeech);
        add(translation);
    }

    @Override
    public void renderCallback(TGuiGraphics pencil) {
        var bb = getBounds();
        pencil.fillColor(bb.x, bb.y, bb.width, bb.height, isHoveredOrFocused() ? 0x55444444 : 0x44000000);
        if (isHoveredOrFocused()) {
            pencil.drawOutlineIn(bb.x, bb.y, bb.width, bb.height, WidgetTheme.ACCENT);
        }
    }
}
