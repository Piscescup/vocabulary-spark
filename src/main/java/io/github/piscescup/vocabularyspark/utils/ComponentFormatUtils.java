package io.github.piscescup.vocabularyspark.utils;


import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.NonNull;

/**
 * Creates Minecraft text components for vocabulary display.
 *
 * <p>Each method creates a new component and applies the styles associated
 * with its display role. Input strings are used as literal text without
 * trimming, translation-key lookup or normalization; callers supply non-null
 * strings and handle missing source data before formatting.
 *
 * <p>Returned components may be modified without affecting stored vocabulary
 * strings or components created by earlier calls.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class ComponentFormatUtils {
    /** Prevents instantiation of this utility class. */
    private ComponentFormatUtils() {}

    /**
     * Formats a headword in bold.
     *
     * @param word the non-null headword text
     * @return a new bold headword component
     */
    @NonNull
    public static MutableComponent wordFormat(@NonNull String word) {
        return Component.literal(word).withStyle(ChatFormatting.BOLD);
    }

    /**
     * Formats an English definition in gray.
     *
     * @param definition the non-null definition text
     * @return a new gray definition component
     */
    @NonNull
    public static MutableComponent englishDefinitionFormat(@NonNull String definition) {
        return Component.literal(definition).withStyle(ChatFormatting.GRAY);
    }

    /**
     * Formats a Chinese translation in white.
     *
     * @param translation the non-null translation text
     * @return a new white translation component
     */
    @NonNull
    public static MutableComponent translationFormat(@NonNull String translation) {
        return Component.literal(translation).withStyle(ChatFormatting.WHITE);
    }

    /**
     * Formats an English example and its Chinese translation together.
     * English text is italic and gray; Chinese text is gray. A newline is
     * inserted between them only when both strings are non-empty.
     *
     * @param english the English text, or an empty string if unavailable
     * @param chinese the Chinese text, or an empty string if unavailable
     * @return a new example component, empty if both strings are empty
     * @throws NullPointerException if either argument is null
     */
    @NonNull
    public static MutableComponent exampleFormat(String english, String chinese) {
        MutableComponent example = Component.empty();
        if (!english.isEmpty()) {
            example.append(Component.literal(english).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
        if (!chinese.isEmpty()) {
            if (!english.isEmpty()) example.append("\n");
            example.append(Component.literal(chinese).withStyle(ChatFormatting.GRAY));
        }
        return example;
    }

    /**
     * Formats a word-form label or spelling in bold, dark-gray text.
     *
     * @param form the non-null label or spelling to display
     * @return a new bold, dark-gray component
     */
    public static Component wordFormFormat(String form) {
        return Component.literal(form)
            .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GRAY);
    }

    /**
     * Formats a pronunciation-region label in italic, gray text.
     * This method does not add a suffix or change the case of the supplied label.
     *
     * @param area the non-null region label ready for display
     * @return a new italic, gray region component
     */
    @NonNull
    public static MutableComponent pronunciationAreaFormat(
        @NonNull String area
    ) {
        return Component.literal(area).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
    }

    /**
     * Formats a phonetic transcription with bold, black text as its default style.
     * Additional styles are applied afterward and may override these defaults.
     *
     * @param symbol the non-null phonetic transcription
     * @param styles the additional formatting values to apply in order
     * @return a new phonetic-symbol component with the resulting style
     */
    @NonNull
    public static MutableComponent pronunciationSymbolFormat(
        @NonNull String symbol,
        ChatFormatting... styles
    ) {
        return Component.literal(symbol).withStyle(ChatFormatting.BOLD, ChatFormatting.BLACK, ChatFormatting.UNDERLINE)
            .withStyle(styles);
    }
}
