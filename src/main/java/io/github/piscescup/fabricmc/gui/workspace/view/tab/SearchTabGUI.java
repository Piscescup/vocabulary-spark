package io.github.piscescup.fabricmc.gui.workspace.view.tab;

import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;
import io.github.piscescup.fabricmc.gui.widget.TextFieldWidget;
import io.github.piscescup.fabricmc.gui.widget.ButtonWidget;
import io.github.piscescup.fabricmc.gui.widget.LabelWidget;
import io.github.piscescup.fabricmc.gui.widget.ParagraphWidget;
import io.github.piscescup.fabricmc.gui.widget.ScrollPanelWidget;
import io.github.piscescup.fabricmc.gui.widget.VocabularyEntryElement;
import io.github.piscescup.fabricmc.gui.workspace.controller.DictionarySearch;
import io.github.piscescup.fabricmc.gui.workspace.controller.VocabularyWorkspace;
import io.github.piscescup.fabricmc.gui.workspace.controller.WordbookActions;
import io.github.piscescup.fabricmc.gui.workspace.view.VocabularyDetailScreen;

import static io.github.piscescup.fabricmc.gui.widget.WidgetTheme.ACCENT;
import static io.github.piscescup.fabricmc.gui.widget.WidgetTheme.MUTED;
import static io.github.piscescup.fabricmc.utils.ComponentUtils.text;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_ENTER;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RETURN;

/** Query controls and one clickable word summary; all senses are shown in the detail GUI. */
public final class SearchTabGUI extends WorkspaceTabGUI {
    private final DictionarySearch search;
    private TextFieldWidget input;
    private TPanelElement results;
    private long revision;

    public SearchTabGUI(VocabularyWorkspace workspace, WordbookActions actions) {
        super(workspace, actions);
        search = workspace.dictionarySearch();
    }

    @Override
    protected void initCallback() {
        revision = search.revision();
        var bb = getBounds();
        new LabelWidget(text("search"), bb.x + 10, bb.y + 8, bb.width - 20, 20, ACCENT).addTo(this);

        input = new TextFieldWidget();
        input.placeholderProperty().set(text("search_placeholder"), SearchTabGUI.class);
        input.textProperty().set(search.input(), SearchTabGUI.class);
        input.textProperty().addChangeListener((property, oldValue, newValue) -> search.setInput(newValue));
        input.setBounds(bb.x + 10, bb.y + 35, Math.max(1, bb.width - 100), 22);
        add(input);
        new ButtonWidget(text("search_submit"), bb.endX - 80, bb.y + 35, 70, 22, true, search::submit).addTo(this);

        results = new ScrollPanelWidget(bb.x + 10, bb.y + 65, bb.width - 20,
            Math.max(1, bb.height - 75), this::populateResults).addTo(this).getPanel();
    }

    @Override
    protected void tickCallback() {
        if (results != null && revision != search.revision()) {
            revision = search.revision();
            results.scrollAmountProperty().set(com.thecsdev.common.math.Point2d.ZERO, SearchTabGUI.class);
            results.clearAndInit();
        }
    }

    @Override
    public boolean inputCallback(TInputContext.InputDiscoveryPhase phase, TInputContext context) {
        // The text field consumes Enter in MAIN, so submit it in the ancestor's PREEMPT phase.
        if (phase == TInputContext.InputDiscoveryPhase.PREEMPT
            && context.getInputType() == TInputContext.InputType.KEY_PRESS
            && input != null && input.isFocused()
            && (Integer.valueOf(SDL_SCANCODE_RETURN).equals(context.getScanCode())
                || Integer.valueOf(SDL_SCANCODE_KP_ENTER).equals(context.getScanCode()))) {
            search.submit();
            return true;
        }
        return false;
    }

    private void populateResults(TPanelElement panel) {
        var bb = panel.getBounds();
        int x = bb.x + 10, y = bb.y + 10, width = bb.width - 20;
        int color = search.state() == DictionarySearch.State.FAILED
            || search.state() == DictionarySearch.State.INVALID_INPUT ? 0xFFFF9999 : MUTED;
        y = new ParagraphWidget(search.message(), x, y, width, color).addTo(panel).getBounds().endY + 12;
        var vocabulary = search.result().orElse(null);
        if (vocabulary == null) return;

        int wordWidth = VocabularyEntryElement.wordWidth(width);
        int posWidth = VocabularyEntryElement.posWidth(width);
        new LabelWidget(text("search_word"), x + 8, y, wordWidth - 12, 18, ACCENT).addTo(panel);
        new LabelWidget(text("search_pos"), x + wordWidth, y, posWidth - 8, 18, ACCENT).addTo(panel);
        new LabelWidget(text("search_translation"), x + wordWidth + posWidth, y,
            width - wordWidth - posWidth - 8, 18, ACCENT).addTo(panel);
        y += 23;
        var summary = vocabulary.translations().stream()
            .filter(sense -> !sense.translation().getString().isBlank() || !sense.englishDefinition().getString().isBlank())
            .findFirst().orElse(null);
        if (summary == null) return;
        var row = new VocabularyEntryElement(vocabulary, summary, () -> {
            var client = getClient();
            if (client == null) return;
            var detail = new VocabularyDetailScreen(client.gui.screen(), vocabulary);
            client.gui.setScreen(detail.getAsScreen());
        });
        row.setBoundsToFitContent(x, y, width);
        panel.add(row);
    }
}
