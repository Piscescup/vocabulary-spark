package io.github.piscescup.fabricmc.gui.widget;

import com.thecsdev.common.properties.NotNullProperty;
import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import net.minecraft.network.chat.Component;

/** A styled label component. TLabelElement is final, so it is composed as a child. */
public class LabelWidget extends TElement {
    private final TLabelElement label;

    public LabelWidget(Component text, int x, int y, int width, int height, int color) {
        label = new TLabelElement(text);
        label.textColorProperty().set(color, LabelWidget.class);
        hoverableProperty().set(false, LabelWidget.class);
        focusableProperty().set(false, LabelWidget.class);
        setBounds(x, y, Math.max(1, width), Math.max(1, height));
    }

    public final TLabelElement getLabel() { return label; }
    public final void setText(Component text) { label.setText(text); }
    public LabelWidget withoutShadow() {
        label.dropShadowProperty().set(false, LabelWidget.class);
        return this;
    }
    public final NotNullProperty<CompassDirection> textAlignmentProperty() {
        return label.textAlignmentProperty();
    }

    public LabelWidget addTo(TElement parent) {
        parent.add(this);
        return this;
    }

    @Override
    protected void initCallback() {
        label.setBounds(getBounds());
        add(label);
    }
}
