package io.github.piscescup.vocabularyspark.gui.widget;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.widget.TScrollBarWidget;

import java.util.Objects;
import java.util.function.Consumer;

/** A scrollable content panel and its vertical scrollbar, owned by one GUI component. */
public final class ScrollPanelWidget extends TElement {
    private final TPanelElement.Paintable panel;
    private final TScrollBarWidget.Flat scrollbar;

    public ScrollPanelWidget(int x, int y, int width, int height, Consumer<TPanelElement> content) {
        Objects.requireNonNull(content);
        panel = new TPanelElement.Paintable(0x44000000, 0x99000000, 0x55888888) {
            @Override
            protected void initCallback() { content.accept(this); }
        };
        panel.scrollPaddingProperty().set(10, ScrollPanelWidget.class);
        scrollbar = new TScrollBarWidget.Flat(panel, TScrollBarWidget.ScrollDirection.VERTICAL);
        setBounds(x, y, Math.max(9, width), Math.max(1, height));
    }

    public TPanelElement.Paintable getPanel() { return panel; }

    @Override
    protected void initCallback() {
        var bb = getBounds();
        panel.setBounds(bb.x, bb.y, bb.width - 8, bb.height);
        add(panel);
        scrollbar.setBounds(bb.endX - 8, bb.y, 8, bb.height);
        add(scrollbar);
    }
}
