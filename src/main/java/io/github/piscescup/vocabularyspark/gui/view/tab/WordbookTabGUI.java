package io.github.piscescup.vocabularyspark.gui.view.tab;

import com.thecsdev.common.math.Point2d;
import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import io.github.piscescup.vocabularyspark.gui.widget.Buttons;
import io.github.piscescup.vocabularyspark.gui.widget.Labels;
import io.github.piscescup.vocabularyspark.gui.widget.ScrollPanelWidget;
import io.github.piscescup.vocabularyspark.gui.widget.TextFieldWidget;
import io.github.piscescup.vocabularyspark.gui.widget.WordbookTitles;
import io.github.piscescup.vocabularyspark.gui.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.controller.DictionarySearch;
import io.github.piscescup.vocabularyspark.gui.controller.WordbookActions;
import io.github.piscescup.vocabularyspark.gui.controller.WorkspaceTab;
import io.github.piscescup.vocabularyspark.gui.VocabularyDetailScreen;

import java.util.List;
import java.util.Locale;

import net.minecraft.network.chat.Component;

import static io.github.piscescup.vocabularyspark.gui.widget.WidgetTheme.*;
import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/**
 * Paginated word browser with asynchronous lookup and direct dictionary-detail navigation.
 * Even very large books create only one page of GUI rows.
 */
public final class WordbookTabGUI extends WorkspaceTabGUI {
    private static final int PAGE_SIZE = 100;
    private final WorkspaceTab.Book tab;
    private List<String> allWords = List.of();
    private List<String> matches = List.of();
    private int page;
    private TPanelElement list;
    private TLabelElement pageLabel;
    private TLabelElement lookupStatus;
    private boolean lookupStarted;
    private long lookupRevision;
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
        page = workspace.wordbookPage(tab);
        var bb = getBounds();
        add(Labels.label(WordbookTitles.title(book), bb.x + 10, bb.y + 8, bb.width - 120, 20, ACCENT));
        add(Buttons.button(
            text("export"), bb.endX - 105, bb.y + 8, 95, 22, !workspace.busy(),
            () -> actions.exportBook(book)
        ));

        var search = new TextFieldWidget();
        search.placeholderProperty()
            .set(text("filter_words"), WordbookTabGUI.class);
        search.textProperty()
            .set(workspace.filter(tab), WordbookTabGUI.class);
        search.setBounds(bb.x + 10, bb.y + 37, bb.width - 20, 22);
        add(search);

        lookupStatus = Labels.label(text("wordbook_lookup_hint"), bb.x + 10, bb.y + 64, bb.width - 20, 18, MUTED);
        lookupStatus.hoverableProperty()
            .set(true, WordbookTabGUI.class);
        lookupStatus.tooltipProperty()
            .set(ignored -> TTooltip.of(lookupStatus.getText()), WordbookTabGUI.class);
        add(lookupStatus);
        lookupRevision = workspace.dictionarySearch()
            .revision();

        var wordPanel = new ScrollPanelWidget(
            bb.x + 10, bb.y + 88, bb.width - 20, Math.max(1, bb.height - 125), panel -> {
            var pb = panel.getBounds();
            int y = pb.y + 10;
            if (matches.isEmpty()) {
                panel.add(Labels.paragraph(text("no_matching_words"), pb.x + 10, y, pb.width - 20, MUTED));
            }
            int start = page * PAGE_SIZE, end = Math.min(matches.size(), start + PAGE_SIZE);
            for (int index = start; index < end; index++) {
                panel.add(Labels.label(Component.literal(Integer.toString(index + 1)), pb.x + 10, y, 35, 20, MUTED));
                String word = matches.get(index);
                var row = Buttons.button(
                    Component.literal(word), pb.x + 50, y, pb.width - 60, 20, true,
                    () -> lookupWord(word)
                );
                row.getLabel()
                    .textAlignmentProperty()
                    .set(CompassDirection.WEST, WordbookTabGUI.class);
                row.tooltipProperty()
                    .set(ignored -> TTooltip.of(text("wordbook_lookup_word", word)), WordbookTabGUI.class);
                panel.add(row);
                y += 24;
            }
        }
        );
        add(wordPanel);
        list = wordPanel.getPanel();
        previous = Buttons.button(Component.literal("<"), bb.x + 10, bb.endY - 29, 28, 22, true, () -> changePage(-1));
        add(previous);
        next = Buttons.button(Component.literal(">"), bb.endX - 38, bb.endY - 29, 28, 22, true, () -> changePage(1));
        add(next);
        pageLabel = Labels.label(Component.empty(), bb.x + 44, bb.endY - 29, bb.width - 88, 22, MUTED);
        add(pageLabel);
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

    private void lookupWord(String word) {
        var client = getClient();
        if (client == null) return;
        var origin = client.gui.screen();
        var dictionary = workspace.dictionarySearch();
        lookupStarted = true;
        dictionary.setInput(word);
        dictionary.submit(vocabulary -> {
            // Never reopen a screen after the player leaves this book or closes the workspace.
            if (client.gui.screen() != origin || !workspace.currentTab()
                .equals(tab)) {
                return;
            }
            var detail = new VocabularyDetailScreen(origin, vocabulary, workspace.pronunciationPlayer());
            client.gui.setScreen(detail.getAsScreen());
        });
        updateLookupStatus();
    }

    @Override
    protected void tickCallback() {
        if (lookupStarted && lookupStatus != null && lookupRevision != workspace.dictionarySearch()
            .revision()) {
            updateLookupStatus();
        }
    }

    private void updateLookupStatus() {
        var dictionary = workspace.dictionarySearch();
        lookupRevision = dictionary.revision();
        lookupStatus.setText(dictionary.message());
        boolean failed = dictionary.state() == DictionarySearch.State.FAILED
            || dictionary.state() == DictionarySearch.State.INVALID_INPUT;
        lookupStatus.textColorProperty()
            .set(failed ? 0xFFFF9999 : MUTED, WordbookTabGUI.class);
    }

    private void updateMatches(String input) {
        String query = input.strip()
            .toLowerCase(Locale.ROOT);
        matches = query.isEmpty() ? allWords : allWords.stream()
            .filter(word -> word.toLowerCase(Locale.ROOT)
                .contains(query))
            .toList();
        page = Math.min(page, pageCount() - 1);
        workspace.setWordbookPage(tab, page);
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
