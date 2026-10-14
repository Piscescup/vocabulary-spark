package io.github.piscescup.fabricmc.gui.widget;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import net.minecraft.network.chat.Component;

/** Vocabulary Spark's flat button, with consistent text, tooltip and enabled styling. */
public final class ButtonWidget extends TButtonWidget.Paintable {
    public ButtonWidget(Component text, int x, int y, int width, int height, boolean enabled, Runnable action) {
        super(0x33000000, 0x66888888, WidgetTheme.ACCENT);
        getLabel().setText(text);
        enabledProperty().addChangeListener((property, previous, current) -> updateTextColor(current));
        enabledProperty().set(enabled, ButtonWidget.class);
        updateTextColor(enabled);
        setBounds(x, y, Math.max(1, width), Math.max(1, height));
        eClicked.addListener(ignored -> action.run());
        tooltipProperty().set(ignored -> TTooltip.of(text), ButtonWidget.class);
    }

    private void updateTextColor(boolean enabled) {
        getLabel().textColorProperty().set(enabled ? 0xFFFFFFFF : 0xFF777777, ButtonWidget.class);
    }

    public ButtonWidget addTo(TElement parent) {
        parent.add(this);
        return this;
    }
}
