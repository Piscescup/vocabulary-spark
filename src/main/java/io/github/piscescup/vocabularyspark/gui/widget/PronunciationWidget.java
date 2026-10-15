package io.github.piscescup.vocabularyspark.gui.widget;

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import io.github.piscescup.vocabularyspark.client.audio.PronunciationPlayer;
import io.github.piscescup.vocabularyspark.vocabulary.pronunciation.Pronunciation;
import net.minecraft.network.chat.Component;

import java.net.URI;

import static io.github.piscescup.vocabularyspark.gui.widget.DictionaryTextStyles.*;
import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/**
 * A clickable region and phonetic transcription bound to its pronunciation URL.
 *
 * <p>Each control keeps its audio identity independently of the displayed text.
 * Its tooltip reports loading, playback and retry states, and the underline
 * highlights an active request. Text wraps when the available width is narrow.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class PronunciationWidget extends TButtonWidget.Transparent {
    private final PronunciationPlayer player;
    private final URI audioUri;
    private final Component area;

    /**
     * Creates a pronunciation control without attaching it to a parent.
     *
     * @param pronunciation the regional transcription and audio URL
     * @param player the client-owned pronunciation player
     */
    public PronunciationWidget(Pronunciation pronunciation, PronunciationPlayer player) {
        this.player = player;
        audioUri = URI.create(pronunciation.pronunciationUrl().toExternalForm());
        area = pronunciation.area();
        getLabel().setText(dictionaryPronunciationFormat(area, pronunciation.phoneticSymbol()));
        getLabel().dropShadowProperty().set(false, PronunciationWidget.class);
        getLabel().textScaleProperty().set(DICTIONARY_PRONUNCIATION_SCALE, PronunciationWidget.class);
        getLabel().textAlignmentProperty().set(CompassDirection.NORTH_WEST, PronunciationWidget.class);
        getLabel().wrapTextProperty().set(true, PronunciationWidget.class);
        tooltipProperty().set(ignored -> TTooltip.of(tooltipText()), PronunciationWidget.class);
        eClicked.removeListener(ONCLICK_SOUND);
        eClicked.addListener(ignored -> player.play(audioUri));
    }

    /**
     * Measures the control, wrapping its label if needed.
     *
     * @param x the horizontal position
     * @param y the vertical position
     * @param availableWidth the maximum width available for the control
     */
    public void setBoundsToFitContent(int x, int y, int availableWidth) {
        var label = getLabel();
        // Wrapped text width depends on the current bounds, which are initially
        // zero. Measure the full component independently of those bounds.
        int textWidth = (int) Math.ceil(label.fontProperty().get().width(label.getText())
            * label.textScaleProperty().getD());
        int width = Math.max(1, Math.min(availableWidth, textWidth + 10));
        label.setBoundsToFitText(x + 5, y + 3, Math.max(1, width - 10));
        setBounds(x, y, width, Math.max(18, label.getBounds().height + 6));
    }

    private Component tooltipText() {
        return switch (player.state(audioUri)) {
            case IDLE -> text("pronunciation_play", area);
            case LOADING -> text("pronunciation_loading", area);
            case PLAYING -> text("pronunciation_playing", area);
            case FAILED -> text("pronunciation_failed");
            case MUTED -> text("pronunciation_muted");
        };
    }

    /** Draws hover feedback and the loading or playback indicator. */
    @Override
    public void renderCallback(TGuiGraphics pencil) {
        var bb = getBounds();
        if (isHoveredOrFocused()) {
            pencil.fillColor(bb.x, bb.y, bb.width, bb.height, DICTIONARY_HOVER_BACKGROUND);
        }
        var state = player.state(audioUri);
        if (state == PronunciationPlayer.State.LOADING || state == PronunciationPlayer.State.PLAYING) {
            pencil.fillColor(bb.x + 3, bb.endY - 1, Math.max(1, bb.width - 6), 1, DICTIONARY_TAB_COLOR);
        } else if (state == PronunciationPlayer.State.FAILED || state == PronunciationPlayer.State.MUTED) {
            pencil.fillColor(bb.x + 3, bb.endY - 1, Math.max(1, bb.width - 6), 1,
                state == PronunciationPlayer.State.FAILED ? 0xFFFF9999 : 0xFFFFD580);
        }
    }
}
