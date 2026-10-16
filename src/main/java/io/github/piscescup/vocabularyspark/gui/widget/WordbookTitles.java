package io.github.piscescup.vocabularyspark.gui.widget;

import io.github.piscescup.vocabularyspark.wordbook.Wordbook;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/** Localized GUI titles leave a wordbook's stored name and identity intact. */
public final class WordbookTitles {
    private WordbookTitles() {}

    public static MutableComponent title(Wordbook book) { return title(book.id(), book.name()); }

    public static MutableComponent title(String id, String name) {
        if (id.equals("qwerty-b907b12b-541d-3a65-95d6-ab94f888115e")
            && name.equals("考研英语 · Qwerty")) {
            return text("book_cn_master");
        }
        return Component.literal(name);
    }
}
