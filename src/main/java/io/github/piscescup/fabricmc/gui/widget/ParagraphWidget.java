package io.github.piscescup.fabricmc.gui.widget;

import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.TElement;
import net.minecraft.network.chat.Component;

/** A wrapped paragraph whose measured height is available through getBounds(). */
public final class ParagraphWidget extends LabelWidget {
    public ParagraphWidget(Component text, int x, int y, int width, int color) {
        this(text, x, y, width, color, 1.0);
    }

    public ParagraphWidget(Component text, int x, int y, int width, int color, double scale) {
        super(text, x, y, width, 1, color);
        getLabel().textScaleProperty().set(scale, ParagraphWidget.class);
        getLabel().textAlignmentProperty().set(CompassDirection.NORTH_WEST, ParagraphWidget.class);
        getLabel().wrapTextProperty().set(true, ParagraphWidget.class);
        getLabel().setBoundsToFitText(x, y, Math.max(1, width));
        setBounds(getLabel().getBounds());
    }

    @Override
    public ParagraphWidget addTo(TElement parent) {
        super.addTo(parent);
        return this;
    }
}
