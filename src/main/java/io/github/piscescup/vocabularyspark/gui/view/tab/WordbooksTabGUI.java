package io.github.piscescup.vocabularyspark.gui.view.tab;

import io.github.piscescup.vocabularyspark.gui.widget.Buttons;
import io.github.piscescup.vocabularyspark.gui.widget.Labels;
import io.github.piscescup.vocabularyspark.gui.widget.ScrollPanelWidget;
import io.github.piscescup.vocabularyspark.gui.widget.WordbookTitles;
import io.github.piscescup.vocabularyspark.gui.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.controller.WordbookActions;

import static io.github.piscescup.vocabularyspark.gui.widget.WidgetTheme.*;
import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

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
        add(new ScrollPanelWidget(
            bb.x + 10, bb.y + 10, bb.width - 20, bb.height - 20, panel -> {
            var pb = panel.getBounds();
            int x = pb.x + 10, y = pb.y + 10, width = pb.width - 20;
            panel.add(Labels.label(text("wordbooks"), x, y, width, 18, ACCENT));
            var libraryHint = Labels.paragraph(text("library_hint"), x, y + 25, width, MUTED);
            panel.add(libraryHint);
            y = libraryHint.getBounds().endY + 12;
            panel.add(
                Buttons.button(text("import"), x, y, Math.min(width, 150), 24, !workspace.busy(), actions::importBook));
            y += 38;
            if (workspace.books()
                .isEmpty()) {
                panel.add(Labels.paragraph(text("no_books"), x, y, width, MUTED));
            }
            for (var book : workspace.books()) {
                panel.add(
                    Buttons.button(WordbookTitles.title(book), x, y, width, 26, true, () -> workspace.openBook(book)));
                var bookSizeLabel = Labels.paragraph(
                    text(
                        "book_size", book.words()
                            .size()
                    ), x + 5, y + 30, width - 10, MUTED
                );
                panel.add(bookSizeLabel);
                y = bookSizeLabel.getBounds().endY + 14;
            }
        }
        ));
    }
}
