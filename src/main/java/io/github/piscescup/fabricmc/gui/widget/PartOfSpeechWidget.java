package io.github.piscescup.fabricmc.gui.widget;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;

/** White italic part-of-speech text on a dark accent badge. */
public final class PartOfSpeechWidget extends TElement {
    private final TLabelElement label;

    public PartOfSpeechWidget(String partOfSpeech, int x, int y) {
        label = new TLabelElement(DictionaryTextStyles.dictionaryPartOfSpeechFormat(partOfSpeech));
        label.dropShadowProperty().set(false, PartOfSpeechWidget.class);
        setBounds(x, y, label.getTextWidth() + 12, 18);
    }

    @Override
    protected void initCallback() {
        var bb = getBounds();
        label.setBounds(bb.x + 6, bb.y + 3, bb.width - 12, bb.height - 6);
        add(label);
    }

    @Override
    public void renderCallback(TGuiGraphics pencil) {
        var bb = getBounds();
        pencil.fillColor(bb.x, bb.y, bb.width, bb.height, DictionaryTextStyles.DICTIONARY_BADGE_BACKGROUND);
    }
}
