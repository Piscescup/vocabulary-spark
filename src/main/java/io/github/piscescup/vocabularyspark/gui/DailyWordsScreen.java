package io.github.piscescup.vocabularyspark.gui;

import com.thecsdev.common.math.UDim2;
import com.thecsdev.commonmc.api.client.gui.screen.ILastScreenProvider;
import com.thecsdev.commonmc.api.client.gui.screen.TScreenPlus;
import io.github.piscescup.vocabularyspark.gui.widget.DailyWordsWidget;
import io.github.piscescup.vocabularyspark.gui.controller.VocabularyWorkspace;
import net.minecraft.client.gui.screens.Screen;

import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/** A separate daily-plan overview; Back or Escape returns to daily study. */
public final class DailyWordsScreen extends TScreenPlus implements ILastScreenProvider {
    private final Screen previous;
    private final DailyWordsWidget overview;

    public DailyWordsScreen(Screen previous, VocabularyWorkspace workspace) {
        super(text("today_words"));
        this.previous = previous;
        overview = new DailyWordsWidget(workspace, this::close);
    }

    @Override
    public Screen getLastScreen() { return previous; }

    @Override
    public boolean isAllowingInGameHud() { return false; }

    @Override
    protected void initCallback() {
        add(overview);
        overview.setBounds(new UDim2(0.05, 0, 0.025, 0), new UDim2(0.9, 0, 0.95, 0));
    }
}
