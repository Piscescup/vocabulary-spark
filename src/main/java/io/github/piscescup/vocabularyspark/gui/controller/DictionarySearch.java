package io.github.piscescup.vocabularyspark.gui.controller;

import io.github.piscescup.vocabularyspark.VocabularySparkModReferences;
import io.github.piscescup.vocabularyspark.dictionary.DictionaryClient;
import io.github.piscescup.vocabularyspark.vocabulary.Word;
import net.minecraft.network.chat.Component;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/** Search state survives page changes. Only the latest submitted query may update it. */
public final class DictionarySearch implements AutoCloseable {
    public enum State { IDLE, LOADING, READY, NOT_FOUND, INVALID_INPUT, FAILED }

    private final Function<String, CompletableFuture<Optional<Word>>> lookup;
    private final Runnable closeDictionary;
    private final Executor clientExecutor;
    private String input = "";
    private String submitted = "";
    private String error = "";
    private State state = State.IDLE;
    private Word result;
    private CompletableFuture<Optional<Word>> pending;
    private long sequence, revision;

    public DictionarySearch(DictionaryClient dictionary, Executor clientExecutor) {
        this(dictionary::lookup, dictionary::close, clientExecutor);
    }

    DictionarySearch(Function<String, CompletableFuture<Optional<Word>>> lookup, Runnable closeDictionary,
        Executor clientExecutor) {
        this.lookup = Objects.requireNonNull(lookup);
        this.closeDictionary = Objects.requireNonNull(closeDictionary);
        this.clientExecutor = clientExecutor;
    }

    public String input() { return input; }
    public void setInput(String input) { this.input = input; }
    public State state() { return state; }
    public long revision() { return revision; }
    public Optional<Word> result() { return Optional.ofNullable(result); }

    public Component message() {
        return switch (state) {
            case IDLE -> text("search_hint");
            case LOADING -> text("search_loading", submitted);
            case READY -> text("search_found", result.word());
            case NOT_FOUND -> text("search_not_found", submitted);
            case INVALID_INPUT -> text("search_invalid_input");
            case FAILED -> text("search_failed", error);
        };
    }

    public void submit() {
        submit(word -> {});
    }

    /** Calls {@code onFound} on the client executor only for the latest successful query. */
    public void submit(Consumer<Word> onFound) {
        Objects.requireNonNull(onFound, "onFound");
        long request = ++sequence;
        if (pending != null) pending.cancel(true);
        submitted = input.strip();
        result = null;
        if (submitted.isEmpty() || submitted.length() > 200
            || submitted.codePoints().anyMatch(Character::isISOControl)) {
            state = State.INVALID_INPUT;
            revision++;
            return;
        }
        state = State.LOADING;
        revision++;
        pending = lookup.apply(submitted);
        pending.whenCompleteAsync((word, failure) -> {
            if (sequence != request) return;
            if (failure == null) {
                result = word.orElse(null);
                state = result == null ? State.NOT_FOUND : State.READY;
            } else {
                while (failure instanceof CompletionException && failure.getCause() != null) {
                    failure = failure.getCause();
                }
                VocabularySparkModReferences.MOD_LOGGER.warn("Dictionary query failed", failure);
                if (failure instanceof HttpTimeoutException || failure instanceof ConnectException) {
                    error = text("search_connection_failed").getString();
                } else {
                    error = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
                }
                state = State.FAILED;
            }
            revision++;
            if (state == State.READY) onFound.accept(result);
        }, clientExecutor);
    }

    @Override
    public void close() {
        sequence++;
        if (pending != null) pending.cancel(true);
        closeDictionary.run();
    }
}
