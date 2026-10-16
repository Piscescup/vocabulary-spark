package io.github.piscescup.vocabularyspark.study;

import io.github.piscescup.vocabularyspark.wordbook.Wordbook;

import java.time.LocalDate;
import java.util.List;

/**
 * Today's complete word list, including completed words and tomorrow's review marks.
 */
public record StudyPlan(LocalDate day, Wordbook book, List<Entry> entries) {
    public StudyPlan {
        entries = List.copyOf(entries);
    }

    public enum Status {
        UNSEEN,
        FAMILIAR,
        FUZZY,
        UNFAMILIAR
    }

    public record Entry(
        String key, String word,
        boolean review, boolean completed,
        Status status, boolean reviewTomorrow
    ) {
    }
}
