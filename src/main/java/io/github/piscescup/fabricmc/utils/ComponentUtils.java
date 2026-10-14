package io.github.piscescup.fabricmc.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ComponentUtils {
    private ComponentUtils() {}

    public static MutableComponent text(String key, Object... arguments) {
        return Component.translatable("gui.vocabulary-spark." + key, arguments);
    }
}
