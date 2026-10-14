package io.github.piscescup.fabricmc.gui.workspace.view;

import com.thecsdev.common.math.UDim2;
import com.thecsdev.commonmc.api.client.gui.screen.ILastScreenProvider;
import com.thecsdev.commonmc.api.client.gui.screen.TScreenPlus;
import io.github.piscescup.fabricmc.gui.widget.VocabularyDetailWidget;
import io.github.piscescup.fabricmc.vocabulary.Word;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;

/** Independent dictionary-detail GUI. Back or Escape returns to the same search workspace. */
public final class VocabularyDetailScreen extends TScreenPlus implements ILastScreenProvider {
    private final Screen previous;
    private final Word vocabulary;

    public VocabularyDetailScreen(Screen previous, Word vocabulary) {
        super(vocabulary.word());
        this.previous = previous;
        this.vocabulary = Objects.requireNonNull(vocabulary);
    }

    @Override
    public Screen getLastScreen() { return previous; }

    @Override
    public boolean isAllowingInGameHud() { return false; }

    @Override
    protected void initCallback() {
        var detail = new VocabularyDetailWidget(vocabulary, this::close);
        add(detail);
        detail.setBounds(new UDim2(0.05, 0, 0.025, 0), new UDim2(0.9, 0, 0.95, 0));
    }
}
