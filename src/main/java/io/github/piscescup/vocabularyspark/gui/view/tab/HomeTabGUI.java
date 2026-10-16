package io.github.piscescup.vocabularyspark.gui.view.tab;

import com.thecsdev.commonmc.api.client.gui.misc.TTextureElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import io.github.piscescup.vocabularyspark.VocabularySparkModReferences;
import io.github.piscescup.vocabularyspark.gui.widget.Buttons;
import io.github.piscescup.vocabularyspark.gui.widget.Labels;
import io.github.piscescup.vocabularyspark.gui.widget.ScrollPanelWidget;
import io.github.piscescup.vocabularyspark.gui.widget.WordbookTitles;
import io.github.piscescup.vocabularyspark.gui.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.controller.WordbookActions;
import io.github.piscescup.vocabularyspark.gui.controller.WorkspaceTab;
import java.util.Locale;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.vocabularyspark.gui.widget.WidgetTheme.*;
import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/**
 * Home dashboard inspired by Better Statistics' personal-home and side panels.
 */
public final class HomeTabGUI extends WorkspaceTabGUI {
    public HomeTabGUI(VocabularyWorkspace workspace, WordbookActions actions) {
        super(workspace, actions);
    }

    @Override
    protected void initCallback() {
        var bb = getBounds();
        if (bb.width >= 480) {
            int primaryWidth = (bb.width - 30) * 2 / 3;
            add(new ScrollPanelWidget(bb.x + 10, bb.y + 10, primaryWidth, bb.height - 20, this::mainContent));
            add(new ScrollPanelWidget(bb.x + 20 + primaryWidth, bb.y + 10,
                bb.width - primaryWidth - 30, bb.height - 20, this::sideContent
            ));
        } else {
            // Small windows stack the sections instead of squeezing text into narrow columns.
            add(new ScrollPanelWidget(bb.x + 6, bb.y + 6, bb.width - 12, bb.height - 12, outer -> {
                    var ob = outer.getBounds();
                    int bottom = mainContent(outer);
                    var side = new TPanelElement.Transparent() {
                        @Override
                        protected void initCallback() {
                            sideContent(this);
                        }
                    };
                    side.setBounds(ob.x, bottom + 15, ob.width, Math.max(
                            150, workspace.books()
                                .size() * 27 + 90
                        )
                    );
                    outer.add(side);
                }
            ));
        }
    }

    private int mainContent(TPanelElement panel) {
        var bb = panel.getBounds();
        int x = bb.x + 10, y = bb.y + 10, width = bb.width - 20;
        var icon = new TTextureElement();
        icon.textureProperty()
            .set(VocabularySparkModReferences.fromPath("icon.png"), HomeTabGUI.class);
        icon.modeProperty()
            .set(TTextureElement.Mode.TEXTURE, HomeTabGUI.class);
        icon.setBounds(x, y, 32, 32);
        panel.add(icon);
        panel.add(Labels.label(Component.literal(VocabularySparkModReferences.MOD_NAME), x + 40, y, width - 40, 17, ACCENT));
        panel.add(Labels.label(Component.literal("v" + VocabularySparkModReferences.MOD_VERSION), x + 40, y + 18, width - 40, 14, MUTED));
        var intro = Labels.paragraph(text("home_intro"), x, y + 44, width, MUTED);
        panel.add(intro);
        y = intro.getBounds().endY + 14;

        panel.add(Labels.label(text("quick_access"), x, y, width, 16, ACCENT));
        y += 21;
        int buttonWidth = (width - 10) / 2;
        panel.add(Buttons.button(text("import"), x, y, buttonWidth, 24, !workspace.busy(), actions::importBook));
        panel.add(Buttons.button(text("wordbooks"), x + buttonWidth + 10, y, buttonWidth, 24, true,
            () -> workspace.open(WorkspaceTab.Page.WORDBOOKS)
        ));
        y += 29;
        panel.add(Buttons.button(text("export"), x, y, buttonWidth, 24,
            !workspace.busy() && workspace.selectedBook()
                .isPresent(), actions::exportSelected
        ));
        panel.add(Buttons.button(text("search"), x + buttonWidth + 10, y, buttonWidth, 24, true,
            () -> workspace.open(WorkspaceTab.Page.SEARCH)));
        y += 29;
        panel.add(Buttons.button(text("study"), x, y, width, 24, true,
            () -> workspace.open(WorkspaceTab.Page.STUDY)));
        y += 35;

        panel.add(Labels.label(text("overview"), x, y, width, 16, ACCENT));
        y += 21;
        y = stat(panel, text("book_count"), Component.literal(Integer.toString(workspace.books()
            .size())), x, y, width
        );
        long count = workspace.books()
            .stream()
            .flatMap(book -> book.words()
                .stream())
            .map(word -> word.toLowerCase(Locale.ROOT))
            .distinct()
            .count();
        y = stat(panel, text("word_count"), Component.literal(Long.toString(count)), x, y, width);
        y = stat(
            panel, text("selected_book"), workspace.selectedBook()
                .<Component>map(WordbookTitles::title)
                .orElse(text("none_selected")), x, y, width
        );
        if (workspace.books()
            .isEmpty()) {
            var emptyHint = Labels.paragraph(text("empty_hint"), x, y + 10, width, MUTED);
            panel.add(emptyHint);
            y = emptyHint.getBounds().endY;
        }
        return y;
    }

    private int stat(TPanelElement panel, Component name, Component value, int x, int y, int width) {
        var row = new com.thecsdev.commonmc.api.client.gui.misc.TFillColorElement.Flat(0x44000000, 0);
        row.setBounds(x, y, width, 22);
        panel.add(row);
        row.add(Labels.label(name, x + 5, y, width / 2 - 5, 22, 0xFFFFFFFF));
        var result = Labels.label(value, x + width / 2, y, width / 2 - 5, 22, 0xFFFFFFFF);
        row.add(result);
        result.textAlignmentProperty()
            .set(com.thecsdev.common.util.enumerations.CompassDirection.EAST, HomeTabGUI.class);
        return y + 26;
    }

    private void sideContent(TPanelElement panel) {
        var bb = panel.getBounds();
        int x = bb.x + 10, y = bb.y + 10, width = bb.width - 20;
        panel.add(Labels.label(text("wordbooks"), x, y, width, 16, ACCENT));
        y += 22;
        if (workspace.books()
            .isEmpty()) {
            var noBooks = Labels.paragraph(text("no_books"), x, y, width, MUTED);
            panel.add(noBooks);
            y = noBooks.getBounds().endY + 8;
            panel.add(Buttons.button(text("import_first"), x, y, width, 24, !workspace.busy(), actions::importBook));
            y += 36;
        } else {
            for (var book : workspace.books()) {
                panel.add(Buttons.button(text(
                        "book_entry", WordbookTitles.title(book), book.words()
                            .size()
                    ), x, y, width, 24,
                    true, () -> workspace.openBook(book)
                ));
                y += 29;
            }
            y += 10;
        }
        panel.add(Labels.label(text("getting_started"), x, y, width, 16, ACCENT));
        var importHelp = Labels.paragraph(text("import_help"), x, y + 22, width, MUTED);
        panel.add(importHelp);
        y = importHelp.getBounds().endY + 12;
        panel.add(Labels.paragraph(text("export_help"), x, y, width, MUTED));
    }
}
