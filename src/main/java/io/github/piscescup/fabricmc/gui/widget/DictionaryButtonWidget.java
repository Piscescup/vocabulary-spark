package io.github.piscescup.fabricmc.gui.widget;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.fabricmc.gui.widget.DictionaryTextStyles.*;

/** Compact, bordered TCDCommons buttons for dictionary navigation. */
public final class DictionaryButtonWidget extends TButtonWidget.Paintable {
    public DictionaryButtonWidget(Component text, int x, int y, int width, int height,
                                  boolean selected, Runnable action) {
        super(selected ? DICTIONARY_BADGE_BACKGROUND : 0x44000000,
            selected ? DICTIONARY_TAB_COLOR : DICTIONARY_LINE_COLOR, DICTIONARY_TAB_COLOR);
        getLabel().setText(dictionaryNavigationFormat(text, selected));
        getLabel().textScaleProperty().set(DICTIONARY_BODY_SCALE, DictionaryButtonWidget.class);
        getLabel().dropShadowProperty().set(false, DictionaryButtonWidget.class);
        setBounds(x, y, Math.max(1, width), Math.max(1, height));
        eClicked.addListener(ignored -> action.run());
    }

    public DictionaryButtonWidget addTo(TElement parent) {
        parent.add(this);
        return this;
    }
}
