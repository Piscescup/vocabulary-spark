package io.github.piscescup.fabricmc.gui.workspace.view.tab;

import io.github.piscescup.fabricmc.gui.widget.ButtonWidget;
import io.github.piscescup.fabricmc.gui.widget.LabelWidget;
import io.github.piscescup.fabricmc.gui.widget.ParagraphWidget;
import io.github.piscescup.fabricmc.gui.widget.ScrollPanelWidget;
import io.github.piscescup.fabricmc.gui.workspace.controller.VocabularyWorkspace;
import io.github.piscescup.fabricmc.gui.workspace.controller.WordbookActions;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.fabricmc.gui.widget.WidgetTheme.*;
import static io.github.piscescup.fabricmc.utils.ComponentUtils.text;

/**
 * Library overview. Opening a book creates or selects its document tab.
 */
public final class WordbooksTabGUI extends WorkspaceTabGUI {
    public WordbooksTabGUI(VocabularyWorkspace workspace, WordbookActions actions) {
        super(workspace, actions);
    }

    @Override
    protected void initCallback() {
        var bb = getBounds();
        new ScrollPanelWidget(bb.x + 10, bb.y + 10, bb.width - 20, bb.height - 20, panel -> {
                var pb = panel.getBounds();
                int x = pb.x + 10, y = pb.y + 10, width = pb.width - 20;
                new LabelWidget(text("wordbooks"), x, y, width, 18, ACCENT).addTo(panel);
                y = new ParagraphWidget(text("library_hint"), x, y + 25, width, MUTED).addTo(panel).getBounds().endY + 12;
                new ButtonWidget(text("import"), x, y, Math.min(width, 150), 24, !workspace.busy(), actions::importBook).addTo(panel);
                y += 38;
                if (workspace.books()
                    .isEmpty()) {
                    new ParagraphWidget(text("no_books"), x, y, width, MUTED).addTo(panel);
                }
                for (var book : workspace.books()) {
                    new ButtonWidget(Component.literal(book.name()), x, y, width, 26, true, () -> workspace.openBook(book)).addTo(panel);
                    y = new ParagraphWidget(text(
                            "book_size", book.words()
                                .size()
                        ), x + 5, y + 30, width - 10, MUTED
                    ).addTo(panel).getBounds().endY + 14;
                }
            }
        ).addTo(this);
    }
}
