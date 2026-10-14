package io.github.piscescup.fabricmc.gui.workspace.view.tab;

import com.thecsdev.commonmc.api.client.gui.misc.TTextureElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import io.github.piscescup.fabricmc.References;
import io.github.piscescup.fabricmc.gui.widget.ButtonWidget;
import io.github.piscescup.fabricmc.gui.widget.LabelWidget;
import io.github.piscescup.fabricmc.gui.widget.ParagraphWidget;
import io.github.piscescup.fabricmc.gui.widget.ScrollPanelWidget;
import io.github.piscescup.fabricmc.gui.workspace.controller.VocabularyWorkspace;
import io.github.piscescup.fabricmc.gui.workspace.controller.WordbookActions;
import io.github.piscescup.fabricmc.gui.workspace.controller.WorkspaceTab;
import java.util.Locale;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.fabricmc.gui.widget.WidgetTheme.*;
import static io.github.piscescup.fabricmc.utils.ComponentUtils.text;

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
            new ScrollPanelWidget(bb.x + 10, bb.y + 10, primaryWidth, bb.height - 20, this::mainContent).addTo(this);
            new ScrollPanelWidget(bb.x + 20 + primaryWidth, bb.y + 10,
                bb.width - primaryWidth - 30, bb.height - 20, this::sideContent
            ).addTo(this);
        } else {
            // Small windows stack the sections instead of squeezing text into narrow columns.
            new ScrollPanelWidget(bb.x + 6, bb.y + 6, bb.width - 12, bb.height - 12, outer -> {
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
            ).addTo(this);
        }
    }

    private int mainContent(TPanelElement panel) {
        var bb = panel.getBounds();
        int x = bb.x + 10, y = bb.y + 10, width = bb.width - 20;
        var icon = new TTextureElement();
        icon.textureProperty()
            .set(References.fromPath("icon.png"), HomeTabGUI.class);
        icon.modeProperty()
            .set(TTextureElement.Mode.TEXTURE, HomeTabGUI.class);
        icon.setBounds(x, y, 32, 32);
        panel.add(icon);
        new LabelWidget(Component.literal(References.MOD_NAME), x + 40, y, width - 40, 17, ACCENT).addTo(panel);
        new LabelWidget(Component.literal("v" + References.MOD_VERSION), x + 40, y + 18, width - 40, 14, MUTED).addTo(panel);
        y = new ParagraphWidget(text("home_intro"), x, y + 44, width, MUTED).addTo(panel).getBounds().endY + 14;

        new LabelWidget(text("quick_access"), x, y, width, 16, ACCENT).addTo(panel);
        y += 21;
        int buttonWidth = (width - 10) / 2;
        new ButtonWidget(text("import"), x, y, buttonWidth, 24, !workspace.busy(), actions::importBook).addTo(panel);
        new ButtonWidget(text("wordbooks"), x + buttonWidth + 10, y, buttonWidth, 24, true,
            () -> workspace.open(WorkspaceTab.Page.WORDBOOKS)
        ).addTo(panel);
        y += 29;
        new ButtonWidget(text("export"), x, y, buttonWidth, 24,
            !workspace.busy() && workspace.selectedBook()
                .isPresent(), actions::exportSelected
        ).addTo(panel);
        new ButtonWidget(text("search"), x + buttonWidth + 10, y, buttonWidth, 24, true,
            () -> workspace.open(WorkspaceTab.Page.SEARCH)).addTo(panel);
        y += 40;

        new LabelWidget(text("overview"), x, y, width, 16, ACCENT).addTo(panel);
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
                .<Component>map(book -> Component.literal(book.name()))
                .orElse(text("none_selected")), x, y, width
        );
        if (workspace.books()
            .isEmpty()) {
            y = new ParagraphWidget(text("empty_hint"), x, y + 10, width, MUTED).addTo(panel).getBounds().endY;
        }
        return y;
    }

    private int stat(TPanelElement panel, Component name, Component value, int x, int y, int width) {
        var row = new com.thecsdev.commonmc.api.client.gui.misc.TFillColorElement.Flat(0x44000000, 0);
        row.setBounds(x, y, width, 22);
        panel.add(row);
        new LabelWidget(name, x + 5, y, width / 2 - 5, 22, 0xFFFFFFFF).addTo(row);
        var result = new LabelWidget(value, x + width / 2, y, width / 2 - 5, 22, 0xFFFFFFFF).addTo(row);
        result.textAlignmentProperty()
            .set(com.thecsdev.common.util.enumerations.CompassDirection.EAST, HomeTabGUI.class);
        return y + 26;
    }

    private void sideContent(TPanelElement panel) {
        var bb = panel.getBounds();
        int x = bb.x + 10, y = bb.y + 10, width = bb.width - 20;
        new LabelWidget(text("wordbooks"), x, y, width, 16, ACCENT).addTo(panel);
        y += 22;
        if (workspace.books()
            .isEmpty()) {
            y = new ParagraphWidget(text("no_books"), x, y, width, MUTED).addTo(panel).getBounds().endY + 8;
            new ButtonWidget(text("import_first"), x, y, width, 24, !workspace.busy(), actions::importBook).addTo(panel);
            y += 36;
        } else {
            for (var book : workspace.books()) {
                new ButtonWidget(text(
                        "book_entry", book.name(), book.words()
                            .size()
                    ), x, y, width, 24,
                    true, () -> workspace.openBook(book)
                ).addTo(panel);
                y += 29;
            }
            y += 10;
        }
        new LabelWidget(text("getting_started"), x, y, width, 16, ACCENT).addTo(panel);
        y = new ParagraphWidget(text("import_help"), x, y + 22, width, MUTED).addTo(panel).getBounds().endY + 12;
        new ParagraphWidget(text("export_help"), x, y, width, MUTED).addTo(panel);
    }
}
