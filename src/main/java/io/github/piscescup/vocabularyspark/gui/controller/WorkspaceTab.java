package io.github.piscescup.vocabularyspark.gui.controller;

import io.github.piscescup.vocabularyspark.wordbook.Wordbook;
import io.github.piscescup.vocabularyspark.gui.widget.WordbookTitles;
import net.minecraft.network.chat.Component;

import java.util.Objects;

import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/**
 * Tabs are controller state; each tab is rendered by a separate TCDCommons view.
 */
public sealed interface WorkspaceTab {
    Component title();

    enum Page implements WorkspaceTab {
        HOME("home"), SEARCH("search"), WORDBOOKS("wordbooks"), STUDY("study");

        private final String key;

        Page(String key) {
            this.key = key;
        }

        public Component title() {
            return text(key);
        }
    }

    record Book(String bookId, String name) implements WorkspaceTab {
        public Book {
            Objects.requireNonNull(bookId);
            Objects.requireNonNull(name);
        }

        public Book(Wordbook book) {
            this(book.id(), book.name());
        }

        public Component title() {
            return WordbookTitles.title(bookId, name);
        }
    }
}
