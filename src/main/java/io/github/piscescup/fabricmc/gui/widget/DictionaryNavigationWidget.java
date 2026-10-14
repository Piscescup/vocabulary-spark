package io.github.piscescup.fabricmc.gui.widget;

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import net.minecraft.network.chat.Component;

/** A text navigation control; selected part-of-speech tabs have an accent underline. */
public final class DictionaryNavigationWidget extends TButtonWidget.Transparent {
    private final boolean selected;
    private final boolean underline;

    public DictionaryNavigationWidget(Component label, int x, int y, int width, int height,
                                      boolean selected, boolean underline, Runnable action) {
        this.selected = selected;
        this.underline = underline;
        getLabel().setText(DictionaryTextStyles.dictionaryNavigationFormat(label, selected));
        getLabel().dropShadowProperty().set(false, DictionaryNavigationWidget.class);
        getLabel().textScaleProperty().set(DictionaryTextStyles.DICTIONARY_BODY_SCALE, DictionaryNavigationWidget.class);
        getLabel().textAlignmentProperty().set(CompassDirection.WEST, DictionaryNavigationWidget.class);
        setBounds(x, y, Math.max(1, width), Math.max(1, height));
        tooltipProperty().set(ignored -> TTooltip.of(label), DictionaryNavigationWidget.class);
        eClicked.addListener(ignored -> action.run());
    }

    public DictionaryNavigationWidget addTo(TElement parent) {
        parent.add(this);
        return this;
    }

    @Override
    public void renderCallback(TGuiGraphics pencil) {
        var bb = getBounds();
        if (isHoveredOrFocused()) {
            pencil.fillColor(bb.x, bb.y, bb.width, bb.height, DictionaryTextStyles.DICTIONARY_HOVER_BACKGROUND);
        }
        if (selected && underline) {
            pencil.fillColor(bb.x, bb.endY - 2, bb.width, 2, DictionaryTextStyles.DICTIONARY_TAB_COLOR);
        }
    }
}
