package io.github.piscescup.vocabularyspark.gui.view.tab;

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.misc.TFillColorElement;
import com.thecsdev.commonmc.api.client.gui.panel.TPanelElement;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import io.github.piscescup.vocabularyspark.gui.widget.Buttons;
import io.github.piscescup.vocabularyspark.gui.widget.Labels;
import io.github.piscescup.vocabularyspark.gui.widget.PronunciationWidget;
import io.github.piscescup.vocabularyspark.gui.widget.ScrollPanelWidget;
import io.github.piscescup.vocabularyspark.gui.widget.TextFieldWidget;
import io.github.piscescup.vocabularyspark.gui.widget.WordbookTitles;
import io.github.piscescup.vocabularyspark.gui.controller.DailyStudy;
import io.github.piscescup.vocabularyspark.gui.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.controller.WordbookActions;
import io.github.piscescup.vocabularyspark.gui.VocabularyDetailScreen;
import io.github.piscescup.vocabularyspark.gui.DailyWordsScreen;
import io.github.piscescup.vocabularyspark.study.StudyStore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.HashSet;

import static io.github.piscescup.vocabularyspark.gui.widget.DictionaryTextStyles.*;
import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/** Responsive study dashboard using only TCDCommons controls and the existing dictionary display API. */
public final class DailyStudyTabGUI extends WorkspaceTabGUI {
    private final DailyStudy study;
    private long revision;

    public DailyStudyTabGUI(VocabularyWorkspace workspace, WordbookActions actions) {
        super(workspace, actions);
        study = workspace.dailyStudy();
    }

    @Override
    protected void tickCallback() {
        study.tick();
        if (revision != study.revision()) clearAndInit();
    }

    @Override
    protected void initCallback() {
        revision = study.revision();
        var bb = getBounds();
        add(Labels.label(text("study"), bb.x + 10, bb.y + 5, Math.max(1, bb.width - 206), 18, DICTIONARY_WORD_COLOR, false));
        add(button(text("today_words"), bb.endX - 178, bb.y + 5, 92, 18,
            !study.busy() && study.snapshot().isPresent(), () -> {
                var client = getClient();
                if (client != null) client.gui.setScreen(new DailyWordsScreen(client.gui.screen(), workspace).getAsScreen());
            }));
        add(button(text("study_refresh"), bb.endX - 78, bb.y + 5, 68, 18, !study.busy(), study::refresh));
        int top = bb.y + 28, height = Math.max(1, bb.height - 33);
        if (bb.width >= 480) {
            int sidebar = Math.clamp(bb.width / 4, 142, 190);
            settings(bb.x + 8, top, sidebar, height, false);
            add(new ScrollPanelWidget(bb.x + sidebar + 16, top, bb.width - sidebar - 24, height,
                panel -> card(panel, panel.getBounds().x + 10, panel.getBounds().y + 8, panel.getBounds().width - 20)));
        } else {
            int settingsHeight = Math.min(86, Math.max(1, height - 35));
            settings(bb.x + 8, top, bb.width - 16, settingsHeight, true);
            add(new ScrollPanelWidget(bb.x + 8, top + settingsHeight + 6, bb.width - 16,
                Math.max(1, height - settingsHeight - 6), panel ->
                    card(panel, panel.getBounds().x + 8, panel.getBounds().y + 8, panel.getBounds().width - 16)));
        }
    }

    /** Book rows scroll independently; the goal field and save action are direct, fixed children. */
    private void settings(int x, int y, int width, int height, boolean compact) {
        var background = new TFillColorElement.Flat(0x44000000, 0x99000000);
        background.setBounds(x, y, Math.max(1, width), Math.max(1, height));
        add(background);
        int innerX = x + 8, innerWidth = Math.max(1, width - 16);
        int listY = paragraph(this, text("study_choose_book"), innerX, y + 6, innerWidth, DICTIONARY_WORD_COLOR) + 4;
        int footerHeight = compact ? 29 : 65;
        int listHeight = Math.max(18, height - (listY - y) - footerHeight - 8);
        add(new ScrollPanelWidget(innerX, listY, innerWidth, listHeight, panel ->
            bookButtons(panel, panel.getBounds().x + 4, panel.getBounds().y + 4, panel.getBounds().width - 8)));
        int goalY = listY + listHeight + 6;
        int fieldX = innerX, fieldWidth;
        int saveWidth = compact ? 94 : Math.max(70, innerWidth - 44);
        if (compact) {
            int labelWidth = Math.max(70, innerWidth - saveWidth - 48);
            var label = Labels.label(text("study_daily_goal"), innerX, goalY, labelWidth, 21, DICTIONARY_WORD_COLOR, false);
            label.textScaleProperty().set(0.85, DailyStudyTabGUI.class);
            add(label);
            fieldX += labelWidth + 4;
            fieldWidth = Math.max(1, innerWidth - labelWidth - saveWidth - 8);
        } else {
            goalY = paragraph(this, text("study_daily_goal"), innerX, goalY, innerWidth, DICTIONARY_WORD_COLOR) + 4;
            fieldWidth = Math.max(1, innerWidth - saveWidth - 4);
        }
        var goal = new TextFieldWidget();
        goal.textProperty().set(study.goalDraft(), DailyStudyTabGUI.class);
        goal.textProperty().addChangeListener((property, previous, current) -> study.setGoalDraft(current));
        goal.enabledProperty().set(!study.busy(), DailyStudyTabGUI.class);
        goal.tooltipProperty().set(ignored -> studyRules(), DailyStudyTabGUI.class);
        goal.setBounds(fieldX, goalY, fieldWidth, 21);
        add(goal);
        var save = button(text("study_save_goal"), fieldX + fieldWidth + 4, goalY, saveWidth, 21,
            !study.busy() && study.selectedBookId() != null, study::saveGoal);
        save.tooltipProperty().set(ignored -> studyRules(), DailyStudyTabGUI.class);
        add(save);
        if (!compact) {
            var rules = Labels.paragraph(text("study_rules"), innerX, goalY + 27, innerWidth, DICTIONARY_MUTED_COLOR, 0.85, false);
            rules.hoverableProperty().set(true, DailyStudyTabGUI.class);
            rules.tooltipProperty().set(ignored -> studyRules(), DailyStudyTabGUI.class);
            add(rules);
        }
    }

    private static TTooltip studyRules() {
        return TTooltip.of(text("study_goal_hint").append("\n\n").append(text("study_schedule_hint")));
    }

    private void bookButtons(TPanelElement panel, int x, int y, int width) {
        width = Math.max(1, width);
        if (workspace.books().isEmpty()) {
            y = paragraph(panel, text(workspace.busy() ? "loading" : "no_books"), x, y, width, DICTIONARY_MUTED_COLOR) + 6;
            panel.add(button(text("import"), x, y, width, 21, !workspace.busy(), actions::importBook));
            return;
        }
        for (var book : workspace.books()) {
            boolean selected = book.id().equals(study.selectedBookId());
            var choose = button(WordbookTitles.title(book), x, y, width, 21,
                !study.busy(), () -> study.select(book));
            choose.getLabel().textAlignmentProperty().set(CompassDirection.WEST, DailyStudyTabGUI.class);
            if (selected) {
                choose.backgroundColorProperty().set(DICTIONARY_BADGE_BACKGROUND, DailyStudyTabGUI.class);
                choose.outlineColorProperty().set(DICTIONARY_TAB_COLOR, DailyStudyTabGUI.class);
                choose.getLabel().textColorProperty().set(DICTIONARY_WORD_COLOR, DailyStudyTabGUI.class);
            }
            panel.add(choose);
            y += 25;
        }
    }

    private void card(TPanelElement panel, int x, int y, int width) {
        width = Math.max(1, width);
        if (!study.error().isEmpty()) {
            var message = study.error().equals("invalid_goal") ? text("study_invalid_goal") : text("study_error", study.error());
            y = paragraph(panel, message, x, y, width, 0xFFFF9999) + 8;
        }
        if (study.busy()) {
            paragraph(panel, text("study_saving"), x, y, width, DICTIONARY_MUTED_COLOR);
            return;
        }
        var snapshot = study.snapshot().orElse(null);
        if (snapshot == null) {
            paragraph(panel, text("study_start_hint"), x, y, width, DICTIONARY_MUTED_COLOR);
            return;
        }
        var book = workspace.book(study.selectedBookId()).orElse(null);
        if (book != null) y = paragraph(panel, WordbookTitles.title(book), x, y, width, DICTIONARY_WORD_COLOR) + 4;
        y = paragraph(panel, text("study_today", snapshot.day().toString(), snapshot.completed(), snapshot.total()),
            x, y, width, DICTIONARY_TEXT_COLOR) + 4;
        var track = new TFillColorElement.Flat(0x553B617B, 0);
        track.setBounds(x, y, width, 3); panel.add(track);
        if (snapshot.total() > 0 && snapshot.completed() > 0) {
            var fill = new TFillColorElement.Flat(DICTIONARY_TAB_COLOR, 0);
            fill.setBounds(x, y, Math.max(1, width * snapshot.completed() / snapshot.total()), 3); panel.add(fill);
        }
        y += 8;
        y = paragraph(panel, text("study_counts", snapshot.newCompleted(), snapshot.reviewCompleted(), snapshot.learned()),
            x, y, width, DICTIONARY_MUTED_COLOR) + 12;
        if (snapshot.current() == null) {
            y = paragraph(panel, text(snapshot.total() == 0 ? "study_nothing_due" : "study_complete"),
                x, y, width, DICTIONARY_WORD_COLOR) + 6;
            paragraph(panel, text("study_complete_hint"), x, y, width, DICTIONARY_MUTED_COLOR);
            return;
        }
        y = paragraph(panel, text(snapshot.current().review() ? "study_review_card" : "study_new_card"),
            x, y, width, DICTIONARY_MUTED_COLOR) + 6;
        var word = Labels.paragraph(Component.literal(snapshot.current().word()).withStyle(ChatFormatting.BOLD),
            x, y, width, DICTIONARY_WORD_COLOR, 1.4, false);
        panel.add(word); y = word.getBounds().endY + 8;
        int gradeWidth = Math.max(1, (width - 8) / 3);
        panel.add(button(text("study_familiar"), x, y, gradeWidth, 22, true, () -> study.grade(StudyStore.Grade.FAMILIAR)));
        panel.add(button(text("study_fuzzy"), x + gradeWidth + 4, y, gradeWidth, 22, true, () -> study.grade(StudyStore.Grade.FUZZY)));
        panel.add(button(text("study_unfamiliar"), x + (gradeWidth + 4) * 2, y, gradeWidth, 22, true, () -> study.grade(StudyStore.Grade.UNFAMILIAR)));
        y += 29;
        if (!study.revealed()) {
            y = paragraph(panel, text("study_recall_hint"), x, y, width, DICTIONARY_MUTED_COLOR) + 7;
            panel.add(button(text("study_reveal"), x, y, width, 22, true, study::reveal));
            return;
        }
        var definition = study.definition().orElse(null);
        if (study.definitionLoading()) {
            paragraph(panel, text("study_definition_loading"), x, y, width, DICTIONARY_MUTED_COLOR);
            return;
        }
        if (definition == null) {
            y = paragraph(panel, text(study.lookupError().isEmpty() ? "study_no_definition" : "study_lookup_failed"),
                x, y, width, DICTIONARY_MUTED_COLOR) + 6;
            panel.add(button(text("study_retry_lookup"), x, y, width, 20, true, study::lookupDefinition));
            return;
        }
        var pronunciations = new HashSet<String>();
        for (var pronunciation : definition.pronunciations()) {
            String identity = pronunciation.area().getString() + "\n" + pronunciation.phoneticSymbol().getString();
            if (!pronunciations.add(identity)) continue;
            var audio = new PronunciationWidget(pronunciation, workspace.pronunciationPlayer());
            audio.setBoundsToFitContent(x, y, width);
            panel.add(audio); y = audio.getBounds().endY + 3;
        }
        panel.add(button(text("study_full_entry"), x, y, width, 20, true, () -> {
            var client = getClient();
            if (client != null) client.gui.setScreen(new VocabularyDetailScreen(client.gui.screen(), definition,
                workspace.pronunciationPlayer()).getAsScreen());
        }));
        y += 27;
        int number = 0;
        for (var sense : definition.translations()) {
            var meaning = Component.literal((++number) + ". " + sense.pos().getName() + " ")
                .withStyle(style -> style.withColor(DICTIONARY_WORD_COLOR & 0xFFFFFF))
                .append(sense.translation().copy());
            y = paragraph(panel, meaning, x, y, width, DICTIONARY_TRANSLATION_COLOR) + 3;
            if (!sense.englishDefinition().getString().isBlank()) {
                y = paragraph(panel, sense.englishDefinition().copy(), x, y, width, DICTIONARY_MUTED_COLOR) + 6;
            }
        }
    }

    private static int paragraph(TElement parent, Component text, int x, int y, int width, int color) {
        var label = Labels.paragraph(text, x, y, width, color, 0.85, false);
        parent.add(label);
        return label.getBounds().endY;
    }

    private static com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget.Paintable button(
        Component text, int x, int y, int width, int height, boolean enabled, Runnable action
    ) {
        var button = Buttons.button(text, x, y, width, height, enabled, action);
        button.getLabel().textScaleProperty().set(0.85, DailyStudyTabGUI.class);
        button.getLabel().dropShadowProperty().set(false, DailyStudyTabGUI.class);
        return button;
    }
}
