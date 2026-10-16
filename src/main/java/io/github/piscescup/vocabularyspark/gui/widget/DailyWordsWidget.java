package io.github.piscescup.vocabularyspark.gui.widget;

import com.thecsdev.common.math.Point2d;
import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.misc.TFillColorElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import com.thecsdev.commonmc.api.client.gui.widget.TDropdownWidget;
import io.github.piscescup.vocabularyspark.gui.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.controller.DictionarySearch;
import io.github.piscescup.vocabularyspark.gui.VocabularyDetailScreen;
import io.github.piscescup.vocabularyspark.study.StudyPlan;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionException;

import static io.github.piscescup.vocabularyspark.gui.widget.DictionaryTextStyles.*;
import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/** Compact, paginated daily-plan browser; filtering never changes a word's learning status. */
public final class DailyWordsWidget extends TElement {
    private static final int PAGE_SIZE = 30;
    private final VocabularyWorkspace workspace;
    private final Runnable back;
    private StudyPlan plan;
    private List<StudyPlan.Entry> matches = List.of();
    private Filter filter = Filter.ALL;
    private String query = "", loadError = "";
    private int page;
    private boolean requested, loading, lookupStarted;
    private long revision, drawnRevision, loadSequence, lookupRevision;
    private LocalDate automaticReloadDay;
    private TPanelElement rows;
    private final Map<Filter, TButtonWidget.Paintable> sidebarButtons = new EnumMap<>(Filter.class);
    private TLabelElement pageLabel, status;
    private TButtonWidget previous, next;

    private enum Filter implements TDropdownWidget.Entry {
        ALL("today_words_all"), PENDING("today_words_pending"), FAMILIAR("study_familiar"),
        FUZZY("study_fuzzy"), UNFAMILIAR("study_unfamiliar"), NEW("study_new_card"),
        REVIEW("study_review_card"), TOMORROW("today_words_tomorrow");

        private final String key;
        Filter(String key) { this.key = key; }
        @Override public Component getDisplayName() { return text(key); }
        boolean accepts(StudyPlan.Entry entry) {
            return switch (this) {
                case ALL -> true;
                case PENDING -> !entry.completed();
                case FAMILIAR -> entry.status() == StudyPlan.Status.FAMILIAR;
                case FUZZY -> entry.status() == StudyPlan.Status.FUZZY;
                case UNFAMILIAR -> entry.status() == StudyPlan.Status.UNFAMILIAR;
                case NEW -> !entry.review();
                case REVIEW -> entry.review();
                case TOMORROW -> entry.reviewTomorrow();
            };
        }
    }

    public DailyWordsWidget(VocabularyWorkspace workspace, Runnable back) {
        this.workspace = workspace;
        this.back = back;
    }

    @Override
    protected void initCallback() {
        if (!requested) reload();
        drawnRevision = revision;
        var bb = getBounds();
        int x = bb.x + 10, width = Math.max(1, bb.width - 20), y = bb.y + 8;
        var background = new TFillColorElement.Flat(DICTIONARY_BACKGROUND, DICTIONARY_LINE_COLOR);
        background.setBounds(bb); add(background);
        add(button(text("today_words_back"), x, y, 84, 21, true, back));
        add(label(text("today_words").withStyle(ChatFormatting.BOLD), x + 92, y,
            width - 168, 21, DICTIONARY_WORD_COLOR));
        add(button(text("study_refresh"), x + width - 68, y, 68, 21, !loading, this::reload));
        y += 29;
        var book = plan == null ? workspace.book(workspace.dailyStudy().selectedBookId()).orElse(null) : plan.book();
        if (book != null) {
            y = paragraph(this, text("today_words_book_date", WordbookTitles.title(book),
                (plan == null ? LocalDate.now() : plan.day()).toString()), x, y, width, DICTIONARY_WORD_COLOR) + 4;
        }
        if (plan != null) {
            long reviews = plan.entries().stream().filter(StudyPlan.Entry::review).count();
            long completed = plan.entries().stream().filter(StudyPlan.Entry::completed).count();
            y = paragraph(this, text("today_words_summary", plan.entries().size(), plan.entries().size() - reviews,
                reviews, completed, plan.entries().size() - completed), x, y, width, DICTIONARY_TEXT_COLOR) + 5;
            var track = new TFillColorElement.Flat(DICTIONARY_LINE_COLOR, 0);
            track.setBounds(x, y, width, 3); add(track);
            if (!plan.entries().isEmpty() && completed > 0) {
                var fill = new TFillColorElement.Flat(DICTIONARY_TAB_COLOR, 0);
                fill.setBounds(x, y, Math.max(1, (int) (width * completed / plan.entries().size())), 3); add(fill);
            }
            y += 9;
        }
        boolean compact = bb.width < 480;
        int searchWidth = Math.max(1, width - (compact ? 152 : 26));
        var search = new TextFieldWidget();
        search.placeholderProperty().set(text("today_words_search"), DailyWordsWidget.class);
        search.textProperty().set(query, DailyWordsWidget.class);
        search.setBounds(x, y, searchWidth, 22); add(search);
        search.textProperty().addChangeListener((property, before, after) -> {
            query = after; page = 0; refreshMatches();
        });
        add(button(Component.literal("×"), x + searchWidth + 4, y, 22, 22, true,
            () -> search.textProperty().set("", DailyWordsWidget.class)));
        if (compact) {
            var dropdown = new TDropdownWidget<Filter>(filter);
            dropdown.getEntries().addAll(List.of(Filter.values()));
            dropdown.getLabel().textScaleProperty().set(0.85, DailyWordsWidget.class);
            dropdown.getLabel().dropShadowProperty().set(false, DailyWordsWidget.class);
            dropdown.selectedEntryProperty().addChangeListener((property, before, after) -> {
                if (after != null) selectFilter(after);
            });
            dropdown.setBounds(x + width - 118, y, 118, 22); add(dropdown);
        }
        y += 30;
        int listX = compact ? x : x + 140;
        int listWidth = Math.max(1, width - (compact ? 0 : 140));
        int footerY = bb.endY - 49;
        int listHeight = Math.max(1, footerY - y - 6);
        sidebarButtons.clear();
        if (!compact) {
            var filterPanel = new ScrollPanelWidget(x, y, 132, listHeight, this::filterButtons);
            add(filterPanel);
        }
        var list = new ScrollPanelWidget(listX, y, listWidth, listHeight, this::wordRows);
        add(list); rows = list.getPanel();
        previous = button(Component.literal("<"), listX, footerY, 28, 21, false, () -> changePage(-1));
        next = button(Component.literal(">"), listX + listWidth - 28, footerY, 28, 21, false, () -> changePage(1));
        add(previous); add(next);
        pageLabel = label(Component.empty(), listX + 32, footerY, listWidth - 64, 21, DICTIONARY_MUTED_COLOR);
        pageLabel.textAlignmentProperty().set(CompassDirection.CENTER, DailyWordsWidget.class); add(pageLabel);
        status = label(Component.empty(), x, bb.endY - 23, width, 18, DICTIONARY_MUTED_COLOR);
        status.hoverableProperty().set(true, DailyWordsWidget.class);
        status.tooltipProperty().set(ignored -> TTooltip.of(status.getText()), DailyWordsWidget.class); add(status);
        refreshMatches(); updateStatus();
    }

    private void reload() {
        if (loading) return;
        var client = getClient();
        if (client == null) return;
        requested = true; loading = true; loadError = "";
        long request = ++loadSequence;
        revision++;
        workspace.dailyStudy().todayWords().whenCompleteAsync((result, failure) -> {
            if (request != loadSequence) return;
            loading = false;
            if (failure == null) {
                if (plan == null || !plan.day().equals(result.day()) || !plan.book().id().equals(result.book().id())) page = 0;
                plan = result;
            } else {
                while (failure instanceof CompletionException && failure.getCause() != null) failure = failure.getCause();
                loadError = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            }
            revision++;
        }, client::schedule);
    }

    @Override
    protected void tickCallback() {
        if (!requested) reload();
        workspace.dailyStudy().tick();
        LocalDate today = LocalDate.now();
        if (plan != null && !plan.day().equals(today) && !loading && !today.equals(automaticReloadDay)) {
            automaticReloadDay = today; reload();
        }
        if (drawnRevision != revision) clearAndInit();
        if (lookupStarted && lookupRevision != workspace.dictionarySearch().revision()) updateStatus();
    }

    private void filterButtons(TPanelElement parent) {
        var bb = parent.getBounds();
        int y = bb.y + 6;
        for (var choice : Filter.values()) {
            long count = plan == null ? 0 : plan.entries().stream().filter(choice::accepts).count();
            var button = Buttons.dictionaryButton(text("today_words_filter_count", choice.getDisplayName(), count),
                bb.x + 5, y, bb.width - 10, 22, filter == choice, () -> selectFilter(choice));
            button.enabledProperty().set(!loading && plan != null, DailyWordsWidget.class);
            sidebarButtons.put(choice, button);
            parent.add(button); y += 26;
        }
    }

    private void selectFilter(Filter choice) {
        if (filter == choice) return;
        filter = choice; page = 0; refreshMatches();
        // Retain the sidebar controls so their scroll position and focus stay in place.
        sidebarButtons.forEach((category, button) -> {
            boolean selected = category == filter;
            button.backgroundColorProperty().set(selected ? DICTIONARY_BADGE_BACKGROUND : 0x44000000, DailyWordsWidget.class);
            button.outlineColorProperty().set(selected ? DICTIONARY_TAB_COLOR : DICTIONARY_LINE_COLOR, DailyWordsWidget.class);
            button.getLabel().setText(dictionaryNavigationFormat(button.getLabel().getText(), selected));
        });
    }

    private void refreshMatches() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        matches = plan == null ? List.of() : plan.entries().stream().filter(filter::accepts)
            .filter(entry -> entry.word().toLowerCase(Locale.ROOT).contains(needle)).toList();
        page = Math.clamp(page, 0, pageCount() - 1);
        if (pageLabel != null) pageLabel.setText(text("page_count", page + 1, pageCount(), matches.size()));
        if (previous != null) previous.enabledProperty().set(!loading && page > 0, DailyWordsWidget.class);
        if (next != null) next.enabledProperty().set(!loading && page + 1 < pageCount(), DailyWordsWidget.class);
        if (rows != null) {
            rows.scrollAmountProperty().set(Point2d.ZERO, DailyWordsWidget.class);
            rows.clearAndInit();
        }
    }

    private int pageCount() { return Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE); }

    private void changePage(int delta) { page = Math.clamp(page + delta, 0, pageCount() - 1); refreshMatches(); }

    private void wordRows(TPanelElement parent) {
        var bb = parent.getBounds();
        int x = bb.x + 7, y = bb.y + 7, width = Math.max(1, bb.width - 14);
        if (loading || plan == null || matches.isEmpty()) {
            paragraph(parent, text(loading ? "today_words_loading" : loadError.isEmpty()
                ? "today_words_empty" : "today_words_load_failed", loadError), x, y, width, DICTIONARY_MUTED_COLOR);
            return;
        }
        int start = page * PAGE_SIZE, end = Math.min(matches.size(), start + PAGE_SIZE);
        for (int index = start; index < end; index++) {
            var entry = matches.get(index);
            var row = button(Component.literal((index + 1) + ". " + entry.word()), x, y, width, 21, true,
                () -> lookup(entry.word()));
            row.getLabel().textAlignmentProperty().set(CompassDirection.WEST, DailyWordsWidget.class);
            row.tooltipProperty().set(ignored -> TTooltip.of(text("wordbook_lookup_word", entry.word())), DailyWordsWidget.class);
            parent.add(row); y += 24;
            var metadata = text(entry.review() ? "study_review_card" : "study_new_card")
                .withStyle(style -> style.withColor(DICTIONARY_WORD_COLOR & 0xFFFFFF))
                .append(Component.literal(" · ").withStyle(style -> style.withColor(DICTIONARY_MUTED_COLOR & 0xFFFFFF)))
                .append(statusText(entry.status()));
            if (entry.reviewTomorrow()) metadata.append(text("today_words_next_review")
                .withStyle(style -> style.withColor(0xE6C67A)));
            y = paragraph(parent, metadata, x + 4, y, width - 8, DICTIONARY_MUTED_COLOR) + 8;
        }
    }

    private static Component statusText(StudyPlan.Status state) {
        String key = switch (state) {
            case UNSEEN -> "today_words_unseen";
            case FAMILIAR -> "study_familiar";
            case FUZZY -> "study_fuzzy";
            case UNFAMILIAR -> "study_unfamiliar";
        };
        int color = switch (state) {
            case UNSEEN -> DICTIONARY_MUTED_COLOR & 0xFFFFFF;
            case FAMILIAR -> 0x9ED6AB;
            case FUZZY -> 0xE6C67A;
            case UNFAMILIAR -> 0xFF9D9D;
        };
        return text(key).withStyle(style -> style.withColor(color));
    }

    private void lookup(String word) {
        var client = getClient();
        if (client == null) return;
        var origin = client.gui.screen();
        var dictionary = workspace.dictionarySearch();
        lookupStarted = true;
        dictionary.setInput(word);
        dictionary.submit(vocabulary -> {
            if (client.gui.screen() != origin) return;
            client.gui.setScreen(new VocabularyDetailScreen(origin, vocabulary, workspace.pronunciationPlayer()).getAsScreen());
        });
        updateStatus();
    }

    private void updateStatus() {
        if (status == null) return;
        lookupRevision = workspace.dictionarySearch().revision();
        status.setText(loading ? text("today_words_loading") : !loadError.isEmpty()
            ? text("today_words_load_failed", loadError) : lookupStarted
            ? workspace.dictionarySearch().message() : text("today_words_hint"));
        boolean failed = !loadError.isEmpty() || lookupStarted
            && (workspace.dictionarySearch().state() == DictionarySearch.State.FAILED
                || workspace.dictionarySearch().state() == DictionarySearch.State.INVALID_INPUT);
        status.textColorProperty().set(failed ? 0xFFFF9999 : DICTIONARY_MUTED_COLOR, DailyWordsWidget.class);
    }

    private static TLabelElement label(Component text, int x, int y, int width, int height, int color) {
        var label = Labels.label(text, x, y, width, height, color, false);
        label.textScaleProperty().set(0.85, DailyWordsWidget.class);
        return label;
    }

    private static int paragraph(TElement parent, Component text, int x, int y, int width, int color) {
        var label = Labels.paragraph(text, x, y, width, color, 0.85, false);
        parent.add(label); return label.getBounds().endY;
    }

    private static TButtonWidget.Paintable button(Component text, int x, int y, int width, int height,
                                                   boolean enabled, Runnable action) {
        var button = Buttons.button(text, x, y, width, height, enabled, action);
        button.getLabel().textScaleProperty().set(0.85, DailyWordsWidget.class);
        button.getLabel().dropShadowProperty().set(false, DailyWordsWidget.class);
        return button;
    }
}
