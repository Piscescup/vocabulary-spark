package io.github.piscescup.vocabularyspark.study;

import java.time.LocalDate;

/**
 * Immutable state handed from the storage worker to the client thread.
 */
public record StudySnapshot(
    LocalDate day,
    int goal, int total,
    int completed, int newCompleted,
    int reviewCompleted, int learned, Card current
) {
    public int remaining() {
        return total - completed;
    }

    public record Card(String key, String word, boolean review) {
    }
}
