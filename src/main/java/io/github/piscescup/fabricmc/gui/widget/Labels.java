package io.github.piscescup.fabricmc.gui.widget;

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import net.minecraft.network.chat.Component;

/**
 * Creates configured TCDCommons labels without an additional container element.
 *
 * <p>Factories set the display style and bounds before returning the label.
 * Callers attach it to its parent using the parent's {@code add} method.
 * Paragraphs wrap their text and measure their height before attachment.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class Labels {
    private Labels() {}

    /**
     * Creates a label with the default text shadow.
     *
     * @param text the text to display
     * @param x the horizontal position
     * @param y the vertical position
     * @param width the width, clamped to at least one pixel
     * @param height the height, clamped to at least one pixel
     * @param color the ARGB text color
     * @return a configured, unattached label
     */
    public static TLabelElement label(Component text, int x, int y, int width, int height, int color) {
        return label(text, x, y, width, height, color, true);
    }

    /**
     * Creates a label with an explicit shadow setting.
     *
     * @param text the text to display
     * @param x the horizontal position
     * @param y the vertical position
     * @param width the width, clamped to at least one pixel
     * @param height the height, clamped to at least one pixel
     * @param color the ARGB text color
     * @param shadow whether to draw a text shadow
     * @return a configured, unattached label
     */
    public static TLabelElement label(
        Component text, int x, int y, int width, int height, int color, boolean shadow
    ) {
        var label = new TLabelElement(text);
        label.textColorProperty().set(color, Labels.class);
        label.dropShadowProperty().set(shadow, Labels.class);
        label.hoverableProperty().set(false, Labels.class);
        label.focusableProperty().set(false, Labels.class);
        label.setBounds(x, y, Math.max(1, width), Math.max(1, height));
        return label;
    }

    /**
     * Creates a wrapped paragraph at normal scale with a text shadow.
     *
     * @param text the text to display
     * @param x the horizontal position
     * @param y the vertical position
     * @param width the wrapping width, clamped to at least one pixel
     * @param color the ARGB text color
     * @return an unattached paragraph with its height measured
     */
    public static TLabelElement paragraph(Component text, int x, int y, int width, int color) {
        return paragraph(text, x, y, width, color, 1.0);
    }

    /**
     * Creates a wrapped paragraph at the supplied scale with a text shadow.
     *
     * @param text the text to display
     * @param x the horizontal position
     * @param y the vertical position
     * @param width the wrapping width, clamped to at least one pixel
     * @param color the ARGB text color
     * @param scale the text scale
     * @return an unattached paragraph with its height measured
     */
    public static TLabelElement paragraph(Component text, int x, int y, int width, int color, double scale) {
        return paragraph(text, x, y, width, color, scale, true);
    }

    /**
     * Creates a wrapped paragraph aligned to its upper-left corner.
     *
     * @param text the text to display
     * @param x the horizontal position
     * @param y the vertical position
     * @param width the wrapping width, clamped to at least one pixel
     * @param color the ARGB text color
     * @param scale the text scale
     * @param shadow whether to draw a text shadow
     * @return an unattached paragraph with its height measured
     */
    public static TLabelElement paragraph(
        Component text, int x, int y, int width, int color, double scale, boolean shadow
    ) {
        var label = label(text, x, y, width, 1, color, shadow);
        label.textScaleProperty().set(scale, Labels.class);
        label.textAlignmentProperty().set(CompassDirection.NORTH_WEST, Labels.class);
        label.wrapTextProperty().set(true, Labels.class);
        label.setBoundsToFitText(x, y, Math.max(1, width));
        return label;
    }
}
