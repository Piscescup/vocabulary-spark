package io.github.piscescup.fabricmc.gui.workspace.view.tab;

import com.thecsdev.common.math.Point2d;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import io.github.piscescup.fabricmc.gui.widget.TextFieldWidget;
import io.github.piscescup.fabricmc.gui.widget.ButtonWidget;
import io.github.piscescup.fabricmc.gui.widget.LabelWidget;
import io.github.piscescup.fabricmc.gui.widget.ParagraphWidget;
import io.github.piscescup.fabricmc.gui.widget.ScrollPanelWidget;
import io.github.piscescup.fabricmc.gui.workspace.controller.VocabularyWorkspace;
import io.github.piscescup.fabricmc.gui.workspace.controller.WordbookActions;
import io.github.piscescup.fabricmc.gui.workspace.controller.WorkspaceTab;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.fabricmc.gui.widget.WidgetTheme.*;
import static io.github.piscescup.fabricmc.utils.ComponentUtils.text;

/**
 * Paginated word browser; even very large imported books create only one page of GUI rows.
 */
public final class WordbookTabGUI extends WorkspaceTabGUI {
    private static final int PAGE_SIZE = 100;
    private final WorkspaceTab.Book tab;
    private List<String> allWords = List.of();
    private List<String> matches = List.of();
    private int page;
    private TPanelElement list;
    private LabelWidget pageLabel;
    private TButtonWidget previous, next;

    public WordbookTabGUI(VocabularyWorkspace workspace, WordbookActions actions, WorkspaceTab.Book tab) {
        super(workspace, actions);
        this.tab = tab;
    }

    @Override
    protected void initCallback() {
        var book = workspace.book(tab.bookId())
            .orElseThrow();
        allWords = book.words();
        var bb = getBounds();
        new LabelWidget(Component.literal(book.name()), bb.x + 10, bb.y + 8, bb.width - 120, 20, ACCENT).addTo(this);
        new ButtonWidget(text("export"), bb.endX - 105, bb.y + 8, 95, 22, !workspace.busy(),
            () -> actions.exportBook(book)
        ).addTo(this);

        var search = new TextFieldWidget();
        search.placeholderProperty()
            .set(text("filter_words"), WordbookTabGUI.class);
        search.textProperty()
            .set(workspace.filter(tab), WordbookTabGUI.class);
        search.setBounds(bb.x + 10, bb.y + 37, bb.width - 20, 22);
        add(search);

        list = new ScrollPanelWidget(bb.x + 10, bb.y + 67, bb.width - 20, Math.max(1, bb.height - 104), panel -> {
                var pb = panel.getBounds();
                int y = pb.y + 10;
                if (matches.isEmpty()) {
                    new ParagraphWidget(text("no_matching_words"), pb.x + 10, y, pb.width - 20, MUTED).addTo(panel);
                }
                int start = page * PAGE_SIZE, end = Math.min(matches.size(), start + PAGE_SIZE);
                for (int index = start; index < end; index++) {
                    new LabelWidget(Component.literal(Integer.toString(index + 1)), pb.x + 10, y, 35, 20, MUTED).addTo(panel);
                    new LabelWidget(Component.literal(matches.get(index)), pb.x + 50, y, pb.width - 60, 20, 0xFFFFFFFF).addTo(panel);
                    y += 24;
                }
            }
        ).addTo(this).getPanel();
        previous = new ButtonWidget(Component.literal("<"), bb.x + 10, bb.endY - 29, 28, 22, true, () -> changePage(-1)).addTo(this);
        next = new ButtonWidget(Component.literal(">"), bb.endX - 38, bb.endY - 29, 28, 22, true, () -> changePage(1)).addTo(this);
        pageLabel = new LabelWidget(Component.empty(), bb.x + 44, bb.endY - 29, bb.width - 88, 22, MUTED).addTo(this);
        pageLabel.textAlignmentProperty()
            .set(com.thecsdev.common.util.enumerations.CompassDirection.CENTER, WordbookTabGUI.class);

        updateMatches(search.textProperty()
            .get());
        search.textProperty()
            .addChangeListener((property, oldValue, newValue) -> {
                workspace.setFilter(tab, newValue);
                page = 0;
                updateMatches(newValue);
                refreshList();
            });
    }

    private void updateMatches(String input) {
        String query = input.strip()
            .toLowerCase(Locale.ROOT);
        matches = query.isEmpty() ? allWords : allWords.stream()
            .filter(word -> word.toLowerCase(Locale.ROOT)
                .contains(query))
            .toList();
        page = Math.min(page, pageCount() - 1);
        pageLabel.setText(text("page_count", page + 1, pageCount(), matches.size()));
        previous.enabledProperty()
            .set(page > 0, WordbookTabGUI.class);
        next.enabledProperty()
            .set(page + 1 < pageCount(), WordbookTabGUI.class);
    }

    private int pageCount() {
        return Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void changePage(int delta) {
        page = Math.clamp(page + delta, 0, pageCount() - 1);
        updateMatches(workspace.filter(tab));
        refreshList();
    }

    private void refreshList() {
        list.scrollAmountProperty()
            .set(Point2d.ZERO, WordbookTabGUI.class);
        list.clearAndInit();
    }
}
