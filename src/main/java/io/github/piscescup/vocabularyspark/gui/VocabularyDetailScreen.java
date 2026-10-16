package io.github.piscescup.vocabularyspark.gui;

import com.thecsdev.common.math.UDim2;
import com.thecsdev.commonmc.api.client.gui.screen.ILastScreenProvider;
import com.thecsdev.commonmc.api.client.gui.screen.TScreenPlus;
import io.github.piscescup.vocabularyspark.client.audio.PronunciationPlayer;
import io.github.piscescup.vocabularyspark.gui.widget.VocabularyDetailWidget;
import io.github.piscescup.vocabularyspark.vocabulary.Word;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;

/**
 * Independent dictionary-detail GUI. Back or Escape returns to the originating workspace page.
 */
public final class VocabularyDetailScreen extends TScreenPlus implements ILastScreenProvider {
    private final Screen previous;
    private final Word vocabulary;
    private final PronunciationPlayer pronunciationPlayer;

    public VocabularyDetailScreen(Screen previous, Word vocabulary, PronunciationPlayer pronunciationPlayer) {
        super(vocabulary.word());
        this.previous = previous;
        this.vocabulary = Objects.requireNonNull(vocabulary);
        this.pronunciationPlayer = Objects.requireNonNull(pronunciationPlayer);
    }

    @Override
    public Screen getLastScreen() {
        return previous;
    }

    @Override
    public boolean isAllowingInGameHud() {
        return false;
    }

    @Override
    protected void initCallback() {
        var detail = new VocabularyDetailWidget(vocabulary, this::close, pronunciationPlayer);
        add(detail);
        detail.setBounds(new UDim2(0.05, 0, 0.025, 0), new UDim2(0.9, 0, 0.95, 0));
    }

    @Override
    protected void closeCallback() {
        pronunciationPlayer.stop();
        super.closeCallback();
    }
}
