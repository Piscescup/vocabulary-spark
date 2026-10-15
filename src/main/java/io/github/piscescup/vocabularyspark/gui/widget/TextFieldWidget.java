package io.github.piscescup.vocabularyspark.gui.widget;

import com.thecsdev.common.properties.NotNullProperty;
import com.thecsdev.common.util.enumerations.CompassDirection;
import com.thecsdev.commonmc.api.client.gui.label.TLabelElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import com.thecsdev.commonmc.api.client.gui.util.CursorType;
import com.thecsdev.commonmc.api.client.gui.util.TGuiUtils;
import com.thecsdev.commonmc.api.client.gui.util.TInputContext;
import com.thecsdev.commonmc.api.client.gui.widget.TClickableWidget;
import net.minecraft.network.chat.Component;

import static org.lwjgl.sdl.SDLKeycode.SDL_KMOD_CTRL;
import static org.lwjgl.sdl.SDLKeycode.SDL_KMOD_SHIFT;
import static org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT;
import static org.lwjgl.sdl.SDLScancode.*;

/** Single-line TCDCommons editor with a caret, selection and clipboard shortcuts. */
public final class TextFieldWidget extends TClickableWidget {
    private final NotNullProperty<String> text = new NotNullProperty<>("");
    private final NotNullProperty<Component> placeholder = new NotNullProperty<>(Component.empty());
    private final TLabelElement textLabel = new TLabelElement();
    private final TLabelElement placeholderLabel = new TLabelElement();
    private int cursor, anchor, viewStart, viewEnd;
    private boolean selecting;
    private long lastEdit = System.currentTimeMillis();

    public TextFieldWidget() {
        eClicked.removeListener(ONCLICK_SOUND);
        for (var label : new TLabelElement[]{textLabel, placeholderLabel}) {
            label.textAlignmentProperty().set(CompassDirection.WEST, TextFieldWidget.class);
            label.dropShadowProperty().set(false, TextFieldWidget.class);
        }
        placeholderLabel.textColorProperty().set(0xFF8996A3, TextFieldWidget.class);
        text.addFilter(TextFieldWidget::singleLine, TextFieldWidget.class);
        text.addChangeListener((property, previous, current) -> {
            cursor = anchor = current.length();
            refreshView();
        });
        placeholder.addChangeListener((property, previous, current) -> refreshView());
        boundsProperty().addChangeListener((property, previous, current) -> refreshView());
    }

    public NotNullProperty<String> textProperty() { return text; }
    public NotNullProperty<Component> placeholderProperty() { return placeholder; }

    @Override
    protected void initCallback() {
        add(placeholderLabel);
        add(textLabel);
        refreshView();
    }

    @Override
    public CursorType getCursor() { return CursorType.IBEAM; }

    private static String singleLine(String value) {
        return value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').codePoints()
            .filter(codePoint -> !Character.isISOControl(codePoint)).limit(4096)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }

    private int width(String value) { return textLabel.fontProperty().get().width(value); }
    private int previous(int index) { return text.get().offsetByCodePoints(index, index > 0 ? -1 : 0); }
    private int next(int index) { return text.get().offsetByCodePoints(index, index < text.get().length() ? 1 : 0); }

    private void refreshView() {
        var bb = getBounds();
        String value = text.get();
        int available = Math.max(1, bb.width - 9);
        viewStart = Math.min(viewStart, cursor);
        while (viewStart < cursor && width(value.substring(viewStart, cursor)) > available) {
            viewStart = next(viewStart);
        }
        while (viewStart > 0 && width(value.substring(previous(viewStart), cursor)) <= available) {
            viewStart = previous(viewStart);
        }
        viewEnd = viewStart;
        while (viewEnd < value.length() && width(value.substring(viewStart, next(viewEnd))) <= available) {
            viewEnd = next(viewEnd);
        }
        textLabel.setText(Component.literal(value.substring(viewStart, viewEnd)));
        placeholderLabel.setText(placeholder.get());
        placeholderLabel.visibleProperty().set(value.isEmpty(), TextFieldWidget.class);
        textLabel.setBounds(bb.x + 4, bb.y + 2, Math.max(1, bb.width - 8), Math.max(1, bb.height - 4));
        placeholderLabel.setBounds(textLabel.getBounds());
    }

    private void moveCursor(int target, boolean extendSelection) {
        cursor = Math.clamp(target, 0, text.get().length());
        if (!extendSelection) anchor = cursor;
        lastEdit = System.currentTimeMillis();
        refreshView();
    }

    private void replaceSelection(String insertion) {
        insertion = singleLine(insertion);
        int start = Math.min(cursor, anchor), end = Math.max(cursor, anchor);
        String value = text.get();
        text.set(value.substring(0, start) + insertion + value.substring(end), TextFieldWidget.class);
        moveCursor(Math.min(start + insertion.length(), text.get().length()), false);
    }

    private int indexAt(double mouseX) {
        String value = text.get();
        double target = mouseX - getBounds().x - 4;
        for (int index = viewStart; index < viewEnd; index = next(index)) {
            int left = width(value.substring(viewStart, index));
            int right = width(value.substring(viewStart, next(index)));
            if (target < (left + right) / 2.0) return index;
        }
        return viewEnd;
    }

    @Override
    protected void focusLostCallback() {
        super.focusLostCallback();
        selecting = false;
    }

    @Override
    public boolean inputCallback(TInputContext.InputDiscoveryPhase phase, TInputContext context) {
        if (phase != TInputContext.InputDiscoveryPhase.MAIN || !isFocusable()) return false;
        if (context.getInputType() == TInputContext.InputType.MOUSE_PRESS
            && Integer.valueOf(SDL_BUTTON_LEFT).equals(context.getMouseButton())) {
            selecting = true;
            moveCursor(indexAt(context.getMouseX()), TGuiUtils.isShiftDown());
            return true;
        }
        if (context.getInputType() == TInputContext.InputType.MOUSE_RELEASE
            && Integer.valueOf(SDL_BUTTON_LEFT).equals(context.getMouseButton())) {
            selecting = false;
            return true;
        }
        if (!isFocused()) return false;
        if (context.getInputType() == TInputContext.InputType.MOUSE_DRAG && selecting) {
            moveCursor(indexAt(context.getMouseX()), true);
            return true;
        }
        int modifiers = context.getModifiers() == null ? 0 : context.getModifiers();
        boolean control = (modifiers & SDL_KMOD_CTRL) != 0;
        boolean shift = (modifiers & SDL_KMOD_SHIFT) != 0;
        if (context.getInputType() == TInputContext.InputType.CHAR_TYPE) {
            if (!control && context.getCharacter() != null && !Character.isISOControl(context.getCharacter())) {
                replaceSelection(context.getCharacter().toString());
            }
            return true;
        }
        if (context.getInputType() != TInputContext.InputType.KEY_PRESS || context.getScanCode() == null) return false;
        int key = context.getScanCode();
        if (control) {
            switch (key) {
                case SDL_SCANCODE_A -> { anchor = 0; moveCursor(text.get().length(), true); return true; }
                case SDL_SCANCODE_C, SDL_SCANCODE_X -> {
                    var client = getClient();
                    if (client != null && cursor != anchor) {
                        client.keyboardHandler.setClipboard(text.get().substring(Math.min(cursor, anchor), Math.max(cursor, anchor)));
                        if (key == SDL_SCANCODE_X) replaceSelection("");
                    }
                    return true;
                }
                case SDL_SCANCODE_V -> {
                    var client = getClient();
                    if (client != null) replaceSelection(client.keyboardHandler.getClipboard());
                    return true;
                }
                default -> { }
            }
        }
        switch (key) {
            case SDL_SCANCODE_LEFT -> moveCursor(!shift && cursor != anchor ? Math.min(cursor, anchor) : previous(cursor), shift);
            case SDL_SCANCODE_RIGHT -> moveCursor(!shift && cursor != anchor ? Math.max(cursor, anchor) : next(cursor), shift);
            case SDL_SCANCODE_HOME -> moveCursor(0, shift);
            case SDL_SCANCODE_END -> moveCursor(text.get().length(), shift);
            case SDL_SCANCODE_BACKSPACE -> {
                if (cursor == anchor) anchor = previous(cursor);
                replaceSelection("");
            }
            case SDL_SCANCODE_DELETE -> {
                if (cursor == anchor) anchor = next(cursor);
                replaceSelection("");
            }
            default -> { return false; }
        }
        return true;
    }

    @Override
    public void renderCallback(TGuiGraphics pencil) {
        var bb = getBounds();
        pencil.fillColor(bb.x, bb.y, bb.width, bb.height, 0xBB0C131D);
        if (isFocused() && cursor != anchor) {
            int start = Math.clamp(Math.min(cursor, anchor), viewStart, viewEnd);
            int end = Math.clamp(Math.max(cursor, anchor), viewStart, viewEnd);
            int x = bb.x + 4 + width(text.get().substring(viewStart, start));
            pencil.fillColor(x, bb.y + 3, width(text.get().substring(start, end)), Math.max(1, bb.height - 6), 0x885880AA);
        }
    }

    @Override
    public void postRenderCallback(TGuiGraphics pencil) {
        var bb = getBounds();
        pencil.drawOutlineIn(bb.x, bb.y, bb.width, bb.height, isHoveredOrFocused() ? 0xFF9CCBEE : 0x66888888);
        if (isFocused() && (System.currentTimeMillis() - lastEdit) % 1000 < 500) {
            int x = bb.x + 4 + width(text.get().substring(viewStart, cursor));
            pencil.fillColor(x, bb.y + 4, 1, Math.max(1, bb.height - 8), 0xFFE6EDF3);
        }
    }
}
