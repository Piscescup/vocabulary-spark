package io.github.piscescup.vocabularyspark.gui.workspace.view.menubar;

import com.thecsdev.commonmc.api.client.gui.ctxmenu.TContextMenu;
import io.github.piscescup.vocabularyspark.gui.workspace.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.workspace.controller.WordbookActions;
import io.github.piscescup.vocabularyspark.gui.workspace.controller.WorkspaceTab;
import net.minecraft.client.Minecraft;

import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/**
 * Menu definitions are separate from the menubar's layout, as in Better Statistics.
 */
public enum MenubarItem {
    FILE("file"), VIEW("view"), ABOUT("about");

    private final String key;

    MenubarItem(String key) {
        this.key = key;
    }

    public net.minecraft.network.chat.Component title() {
        return text(key);
    }

    public TContextMenu createMenu(
        Minecraft client,
        VocabularyWorkspace workspace,
        WordbookActions actions
    ) {
        var menu = new TContextMenu.Builder(client);
        switch (this) {
            case FILE -> {
                if (!workspace.busy()) {
                    menu.addButton(text("import"), ignored -> actions.importBook());
                    workspace.selectedBook()
                        .ifPresent(book -> menu.addButton(
                            text("export_named", book.name()), ignored -> actions.exportBook(book)));
                    menu.addSeparator();
                }
                menu.addButton(text("close"), ignored -> client.gui.screen()
                    .onClose()
                );
            }
            case VIEW -> {
                menu.addButton(text("home"), ignored -> workspace.open(WorkspaceTab.Page.HOME));
                menu.addButton(text("search"), ignored -> workspace.open(WorkspaceTab.Page.SEARCH));
                menu.addButton(text("wordbooks"), ignored -> workspace.open(WorkspaceTab.Page.WORDBOOKS));
                if (!workspace.books()
                    .isEmpty()) {
                    menu.addSeparator();
                }
                for (var book : workspace.books()) {
                    menu.addButton(
                        net.minecraft.network.chat.Component.literal(book.name()),
                        ignored -> workspace.openBook(book)
                    );
                }
            }
            case ABOUT -> menu.addButton(text("about_mod"), ignored -> actions.showAbout());
        }
        return menu.build();
    }
}
