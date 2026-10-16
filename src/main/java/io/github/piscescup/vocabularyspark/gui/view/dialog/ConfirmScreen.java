package io.github.piscescup.vocabularyspark.gui.view.dialog;

import com.thecsdev.common.math.UDim2;
import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.panel.window.TWindowElement;
import com.thecsdev.commonmc.api.client.gui.screen.promise.TCompletableScreen;
import io.github.piscescup.vocabularyspark.gui.widget.Buttons;
import io.github.piscescup.vocabularyspark.gui.widget.Labels;
import io.github.piscescup.vocabularyspark.gui.widget.ScrollPanelWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/**
 * TCDCommons modal; Escape and the window close button cancel without writing.
 */
public final class ConfirmScreen extends TCompletableScreen<Boolean> {
    private final Component message;

    public ConfirmScreen(Screen previous, Component title, Component message) {
        super(previous);
        titleProperty().set(title, ConfirmScreen.class);
        this.message = message;
    }

    @Override
    protected void initCallback() {
        var window = new TWindowElement() {
            @Override
            protected void initBodyCallback(TElement body) {
                var bounds = body.getBounds();
                body.add(new ScrollPanelWidget(bounds.x + 10, bounds.y + 8,
                    bounds.width - 20, bounds.height - 43, panel -> {
                        var bb = panel.getBounds();
                        panel.add(Labels.paragraph(message, bb.x + 10, bb.y + 10,
                            bb.width - 20, 0xFFFFFFFF));
                    }
                ));
                int width = (bounds.width - 30) / 2;
                body.add(Buttons.button(text("replace"), bounds.x + 10, bounds.endY - 26,
                    width, 20, true, () -> resolve(true)
                ));
                body.add(Buttons.button(text("cancel"), bounds.x + 20 + width, bounds.endY - 26,
                    width, 20, true, () -> resolve(false)
                ));
            }
        };
        window.titleProperty()
            .set(titleProperty().get(), ConfirmScreen.class);
        window.backgroundColorProperty()
            .set(0xFF2B2B2B, ConfirmScreen.class);
        window.closeOperationProperty()
            .set(TWindowElement.CloseOperation.CLOSE_SCREEN, ConfirmScreen.class);
        add(window);
        window.setBounds(new UDim2(0.1, 0, 0.15, 0), new UDim2(0.8, 0, 0.7, 0));
    }

    private void resolve(boolean answer) {
        getResult().complete(answer);
        close();
    }
}
