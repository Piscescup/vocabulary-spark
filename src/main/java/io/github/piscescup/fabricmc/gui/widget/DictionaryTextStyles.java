package io.github.piscescup.fabricmc.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/** GUI-only colors applied to copies of components returned by the display API. */
public final class DictionaryTextStyles {
    public static final int DICTIONARY_BACKGROUND = 0x660C131D;
    public static final int DICTIONARY_HOVER_BACKGROUND = 0x335E9FC7;
    public static final int DICTIONARY_TEXT_COLOR = 0xFFE6EDF3;
    public static final int DICTIONARY_WORD_COLOR = 0xFF9CCBEE;
    public static final int DICTIONARY_TRANSLATION_COLOR = 0xFF85D6D2;
    public static final int DICTIONARY_MUTED_COLOR = 0xFFB5C1CD;
    public static final int DICTIONARY_LINE_COLOR = 0x446F91AB;
    public static final int DICTIONARY_TAB_COLOR = 0xFF9CCBEE;
    public static final int DICTIONARY_BADGE_BACKGROUND = 0x663B617B;
    public static final double DICTIONARY_HEADWORD_SCALE = 1.25;
    public static final double DICTIONARY_PRONUNCIATION_SCALE = 0.8;
    public static final double DICTIONARY_BODY_SCALE = 0.85;

    private DictionaryTextStyles() {}

    public static MutableComponent partOfSpeechFormat(String label) {
        return Component.literal(label).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
    }

    /** Applies the shared headword color to a copy, retaining its spelling and other styles. */
    public static MutableComponent headwordFormat(Component word) {
        return word.copy().withStyle(style -> style.withColor(DICTIONARY_WORD_COLOR & 0xFFFFFF));
    }

    public static MutableComponent dictionaryHeadingFormat(Component heading) {
        return heading.copy().withStyle(ChatFormatting.BOLD)
            .withStyle(style -> style.withColor(DICTIONARY_WORD_COLOR & 0xFFFFFF));
    }

    public static MutableComponent dictionaryNavigationFormat(Component label, boolean selected) {
        return label.copy().withStyle(style -> style.withColor(
            (selected ? DICTIONARY_WORD_COLOR : DICTIONARY_MUTED_COLOR) & 0xFFFFFF));
    }

    public static MutableComponent dictionaryPronunciationFormat(Component area, Component symbol) {
        return area.copy()
            .withStyle(style -> style.withColor(DICTIONARY_MUTED_COLOR & 0xFFFFFF))
            .append(" ").append(symbol.copy()
                .withStyle(style -> style.withColor(DICTIONARY_TEXT_COLOR & 0xFFFFFF)));
    }

    /** Definitions retain the API's components, with Chinese on a separate line. */
    public static MutableComponent dictionarySenseFormat(
        int number, boolean numbered, String grammar, Component english, Component chinese
    ) {
        MutableComponent result = Component.empty();
        if (numbered) {
            result.append(Component.literal(number + " ").withStyle(ChatFormatting.BOLD)
                .withStyle(style -> style.withColor(DICTIONARY_WORD_COLOR & 0xFFFFFF)));
        }
        if (!grammar.isBlank()) {
            result.append(Component.literal(grammar + " ").withStyle(ChatFormatting.ITALIC)
                .withStyle(style -> style.withColor(DICTIONARY_MUTED_COLOR & 0xFFFFFF)));
        }
        result.append(inlineDefinition(english));
        if (!english.getString().isBlank() && !chinese.getString().isBlank()) result.append("\n");
        result.append(chinese.copy()
            .withStyle(style -> style.withColor(DICTIONARY_TRANSLATION_COLOR & 0xFFFFFF)));
        return result;
    }

    /** Collapse source-page indentation in the display copy, retaining component styles. */
    private static MutableComponent inlineDefinition(Component source) {
        var result = Component.empty();
        boolean[] hasText = {false};
        boolean[] pendingSpace = {false};
        source.visit((style, value) -> {
            var normalized = new StringBuilder();
            for (int index = 0; index < value.length();) {
                int codePoint = value.codePointAt(index);
                index += Character.charCount(codePoint);
                if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                    pendingSpace[0] = hasText[0];
                    continue;
                }
                if (pendingSpace[0]) normalized.append(' ');
                normalized.appendCodePoint(codePoint);
                hasText[0] = true;
                pendingSpace[0] = false;
            }
            if (!normalized.isEmpty()) {
                result.append(Component.literal(normalized.toString())
                    .setStyle(style.withColor(DICTIONARY_TEXT_COLOR & 0xFFFFFF).withBold(true)));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    /** English examples are italic; the Chinese translation follows below in gray. */
    public static MutableComponent dictionaryExampleFormat(Component example) {
        return Component.literal("• ").append(example.copy());
    }

    public static MutableComponent dictionaryFormFormat(Component label, Component spelling) {
        return label.copy()
            .withStyle(style -> style.withColor(DICTIONARY_MUTED_COLOR & 0xFFFFFF))
            .append(": ").append(spelling.copy()
                .withStyle(style -> style.withColor(DICTIONARY_WORD_COLOR & 0xFFFFFF)));
    }
}
