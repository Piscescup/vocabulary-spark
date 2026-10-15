package io.github.piscescup.vocabularyspark.gui.workspace.view.editor;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import com.thecsdev.commonmc.api.client.gui.widget.TScrollBarWidget;
import io.github.piscescup.vocabularyspark.gui.widget.WidgetTheme;
import io.github.piscescup.vocabularyspark.gui.workspace.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.workspace.controller.WorkspaceTab;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/**
 * A closable document tab strip with horizontal scrolling for many imported books.
 */
public final class TabStripPanel extends TElement {
    private final VocabularyWorkspace workspace;

    public TabStripPanel(VocabularyWorkspace workspace) {
        this.workspace = workspace;
    }

    @Override
    protected void initCallback() {
        var bb = getBounds();
        var panel = new TPanelElement.Transparent() {
            @Override
            protected void initCallback() {
                int x = getBounds().x;
                for (var tab : workspace.tabs()) {
                    var button = new TabButton(tab);
                    int width = Math.min(150, Math.max(
                            60, button.getLabel()
                                .getTextWidth() + 14
                        )
                    ) + button.closeWidth();
                    button.setBounds(x, getBounds().y, width, 16);
                    add(button);
                    x += width + 2;
                }
            }
        };
        panel.setBounds(bb.x, bb.y, bb.width, 16);
        add(panel);
        var scrollbar = new TScrollBarWidget.Flat(panel, TScrollBarWidget.ScrollDirection.HORIZONTAL);
        scrollbar.setBounds(bb.x, bb.y + 16, bb.width, 3);
        add(scrollbar);
    }

    /** The label and close action share one tab background and outline. */
    private final class TabButton extends TButtonWidget.Paintable {
        private final WorkspaceTab tab;

        private TabButton(WorkspaceTab tab) {
            super(0x33000000, 0x66888888, WidgetTheme.ACCENT);
            this.tab = tab;
            getLabel().setText(tab.title());
            getLabel().textColorProperty().set(
                tab.equals(workspace.currentTab()) ? WidgetTheme.ACCENT : 0xFFFFFFFF,
                TabStripPanel.class
            );
            tooltipProperty().set(ignored -> TTooltip.of(tab.title()), TabStripPanel.class);
            eClicked.addListener(ignored -> workspace.open(tab));
        }

        private int closeWidth() {
            return tab == WorkspaceTab.Page.HOME ? 0 : 14;
        }

        @Override
        protected void initCallback() {
            super.initCallback();
            var bb = getBounds();
            getLabel().setBounds(bb.x + 5, bb.y + 3, bb.width - closeWidth() - 10, bb.height - 6);
            if (closeWidth() == 0) return;

            var close = new TButtonWidget.Transparent() {
                @Override
                protected void initCallback() {
                    getLabel().setBounds(getBounds());
                    add(getLabel());
                }

                @Override
                public void renderCallback(TGuiGraphics pencil) {
                    if (!isHoveredOrFocused()) return;
                    var bounds = getBounds();
                    pencil.fillColor(bounds.x, bounds.y, bounds.width, bounds.height, 0x33FFFFFF);
                }
            };
            close.getLabel().setText(Component.literal("×"));
            close.getLabel().textScaleProperty().set(0.75, TabStripPanel.class);
            close.tooltipProperty().set(ignored -> TTooltip.of(text("close")), TabStripPanel.class);
            close.setBounds(bb.endX - closeWidth() - 2, bb.y + 2, closeWidth(), bb.height - 4);
            close.eClicked.addListener(ignored -> workspace.closeTab(tab));
            add(close);
        }
    }
}
