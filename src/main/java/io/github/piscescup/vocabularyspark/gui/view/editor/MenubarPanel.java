package io.github.piscescup.vocabularyspark.gui.view.editor;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.ctxmenu.TContextMenu;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import io.github.piscescup.vocabularyspark.gui.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.controller.WordbookActions;
import io.github.piscescup.vocabularyspark.gui.controller.WorkspaceTab;
import io.github.piscescup.vocabularyspark.gui.widget.WordbookTitles;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

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

    /** Menu definitions belong to the only component that uses them. */
    private enum MenubarItem {
        FILE("file"), VIEW("view"), ABOUT("about");

        private final String key;

        MenubarItem(String key) {
            this.key = key;
        }

        Component title() {
            return text(key);
        }

        TContextMenu createMenu(Minecraft client, VocabularyWorkspace workspace, WordbookActions actions) {
            var menu = new TContextMenu.Builder(client);
            switch (this) {
                case FILE -> {
                    if (!workspace.busy()) {
                        menu.addButton(text("import"), ignored -> actions.importBook());
                        workspace.selectedBook().ifPresent(book -> menu.addButton(
                            text("export_named", WordbookTitles.title(book)), ignored -> actions.exportBook(book)));
                        menu.addSeparator();
                    }
                    menu.addButton(text("close"), ignored -> client.gui.screen().onClose());
                }
                case VIEW -> {
                    menu.addButton(text("home"), ignored -> workspace.open(WorkspaceTab.Page.HOME));
                    menu.addButton(text("search"), ignored -> workspace.open(WorkspaceTab.Page.SEARCH));
                    menu.addButton(text("wordbooks"), ignored -> workspace.open(WorkspaceTab.Page.WORDBOOKS));
                    menu.addButton(text("study"), ignored -> workspace.open(WorkspaceTab.Page.STUDY));
                    if (!workspace.books().isEmpty()) menu.addSeparator();
                    for (var book : workspace.books()) {
                        menu.addButton(WordbookTitles.title(book), ignored -> workspace.openBook(book));
                    }
                }
                case ABOUT -> menu.addButton(text("about_mod"), ignored -> actions.showAbout());
            }
            return menu.build();
        }
    }
}
