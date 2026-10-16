package io.github.piscescup.vocabularyspark.gui.widget;

import com.thecsdev.commonmc.api.client.gui.tooltip.TTooltip;
import com.thecsdev.commonmc.api.client.gui.widget.TButtonWidget;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.vocabularyspark.gui.widget.DictionaryTextStyles.*;

/**
 * Creates native TCDCommons buttons configured with Vocabulary Spark's styles.
 *
 * <p>Ordinary buttons update their text color when enabled state changes.
 * Dictionary section buttons use the selected appearance supplied at creation.
 * Callers attach the returned buttons using their parent's {@code add} method.
 * Controls with custom rendering remain separate widget implementations.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class Buttons {
    private Buttons() {
    }

    /**
     * Creates a flat action button with a tooltip and enabled-state text colors.
     *
     * @param text    the label and tooltip text
     * @param x       the horizontal position
     * @param y       the vertical position
     * @param width   the width, clamped to at least one pixel
     * @param height  the height, clamped to at least one pixel
     * @param enabled the initial enabled state
     * @param action  the action invoked when the button is clicked
     * @return a configured, unattached button
     */
    public static TButtonWidget.Paintable button(
        Component text, int x, int y, int width, int height, boolean enabled, Runnable action
    ) {
        var button = new TButtonWidget.Paintable(0x33000000, 0x66888888, WidgetTheme.ACCENT);
        button.getLabel()
            .setText(text);
        button.enabledProperty()
            .addChangeListener((property, previous, current) ->
                button.getLabel()
                    .textColorProperty()
                    .set(current ? 0xFFFFFFFF : 0xFF777777, Buttons.class));
        button.enabledProperty()
            .set(enabled, Buttons.class);
        button.getLabel()
            .textColorProperty()
            .set(enabled ? 0xFFFFFFFF : 0xFF777777, Buttons.class);
        button.setBounds(x, y, Math.max(1, width), Math.max(1, height));
        button.eClicked.addListener(ignored -> action.run());
        button.tooltipProperty()
            .set(ignored -> TTooltip.of(text), Buttons.class);
        return button;
    }

    /**
     * Creates a compact, bordered dictionary section button.
     *
     * @param text     the label text
     * @param x        the horizontal position
     * @param y        the vertical position
     * @param width    the width, clamped to at least one pixel
     * @param height   the height, clamped to at least one pixel
     * @param selected whether to use the selected section appearance
     * @param action   the action invoked when the button is clicked
     * @return a configured, unattached dictionary button
     */
    public static TButtonWidget.Paintable dictionaryButton(
        Component text, int x, int y, int width, int height, boolean selected, Runnable action
    ) {
        var button = new TButtonWidget.Paintable(
            selected ? DICTIONARY_BADGE_BACKGROUND : 0x44000000,
            selected ? DICTIONARY_TAB_COLOR : DICTIONARY_LINE_COLOR, DICTIONARY_TAB_COLOR
        );
        button.getLabel()
            .setText(dictionaryNavigationFormat(text, selected));
        button.getLabel()
            .textScaleProperty()
            .set(DICTIONARY_BODY_SCALE, Buttons.class);
        button.getLabel()
            .dropShadowProperty()
            .set(false, Buttons.class);
        button.setBounds(x, y, Math.max(1, width), Math.max(1, height));
        button.eClicked.addListener(ignored -> action.run());
        return button;
    }
}
