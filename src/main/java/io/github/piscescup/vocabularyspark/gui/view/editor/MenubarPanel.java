package io.github.piscescup.vocabularyspark.gui.view.editor;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import io.github.piscescup.vocabularyspark.gui.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.controller.WordbookActions;
import io.github.piscescup.vocabularyspark.gui.view.menubar.MenubarItem;

/**
 * TCDCommons context menus anchored to a transparent menu strip.
 */
public final class MenubarPanel extends TElement {
    private final VocabularyWorkspace workspace;
    private final WordbookActions actions;

    public MenubarPanel(VocabularyWorkspace workspace, WordbookActions actions) {
        this.workspace = workspace;
        this.actions = actions;
    }

    @Override
    public void renderCallback(TGuiGraphics pencil) {
        var bb = getBounds();
        pencil.fillColor(bb.x, bb.y, bb.width, bb.height, 0x44000000);
        pencil.drawOutlineIn(bb.x, bb.y, bb.width, bb.height, 0xAA000000);
    }

    @Override
    protected void initCallback() {
        var bb = getBounds();
        int x = bb.x + 2;
        for (var item : MenubarItem.values()) {
            var button = new TButtonWidget.Transparent();
            button.getLabel()
                .setText(item.title());
            int width = Math.max(
                34, button.getLabel()
                    .getTextWidth() + 12
            );
            button.setBounds(x, bb.y + 1, width, bb.height - 2);
            button.contextMenuProperty()
                .set(element -> item.createMenu(getClient(), workspace, actions), MenubarPanel.class);
            button.eClicked.addListener(TElement::showContextMenu);
            add(button);
            x += width;
        }
    }
}
