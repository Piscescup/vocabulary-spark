package io.github.piscescup.vocabularyspark.gui.workspace.controller;

import io.github.piscescup.vocabularyspark.vocabulary.Word;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DictionarySearchTest {
    /** Represents a response that can arrive despite cancellation of the UI request. */
    private static final class LateResponse extends CompletableFuture<Optional<Word>> {
        @Override public boolean cancel(boolean interrupt) { return false; }
    }

    private static Word word() {
        return (Word) Proxy.newProxyInstance(Word.class.getClassLoader(), new Class<?>[]{Word.class},
            (proxy, method, args) -> { throw new AssertionError("Lookup navigation must not render the word"); });
    }

    @Test void foundCallbackWaitsForClientExecutorAndSeesReadyState() {
        var response = new LateResponse();
        var clientTasks = new ArrayDeque<Runnable>();
        var search = new DictionarySearch(query -> response, () -> {}, clientTasks::add);
        Word vocabulary = word();
        var opened = new ArrayList<Word>();
        search.setInput(" ice cream ");
        search.submit(found -> {
            assertEquals(DictionarySearch.State.READY, search.state());
            assertSame(vocabulary, search.result().orElseThrow());
            opened.add(found);
        });
        response.complete(Optional.of(vocabulary));
        assertEquals(DictionarySearch.State.LOADING, search.state());
        assertTrue(opened.isEmpty());
        clientTasks.remove().run();
        assertSame(vocabulary, opened.getFirst());
    }

    @Test void olderResponseCannotOpenDetailsOrReplaceTheLatestResult() {
        var first = new LateResponse();
        var second = new LateResponse();
        var responses = new ArrayDeque<>(List.of(first, second));
        var search = new DictionarySearch(query -> responses.remove(), () -> {}, Runnable::run);
        var opened = new ArrayList<Word>();
        search.setInput("first");
        search.submit(opened::add);
        search.setInput("second");
        search.submit(opened::add);
        Word latest = word();
        second.complete(Optional.of(latest));
        first.complete(Optional.of(word()));
        assertEquals(1, opened.size());
        assertSame(latest, opened.getFirst());
        assertSame(latest, search.result().orElseThrow());
    }

    @Test void closingDropsLateNavigationAndClosesTheDictionaryClient() {
        var response = new LateResponse();
        var closed = new AtomicBoolean();
        var search = new DictionarySearch(query -> response, () -> closed.set(true), Runnable::run);
        var opened = new ArrayList<Word>();
        search.setInput("spark");
        search.submit(opened::add);
        search.close();
        response.complete(Optional.of(word()));
        assertTrue(closed.get());
        assertTrue(opened.isEmpty());
        assertTrue(search.result().isEmpty());
    }

    @Test void notFoundDoesNotNavigateAndCanBeRetried() {
        var responses = new ArrayDeque<>(List.of(new LateResponse(), new LateResponse()));
        var first = responses.getFirst();
        var second = responses.getLast();
        var search = new DictionarySearch(query -> responses.remove(), () -> {}, Runnable::run);
        var opened = new ArrayList<Word>();
        search.setInput("spark");
        search.submit(opened::add);
        first.complete(Optional.empty());
        assertEquals(DictionarySearch.State.NOT_FOUND, search.state());
        assertTrue(opened.isEmpty());
        search.submit(opened::add);
        Word vocabulary = word();
        second.complete(Optional.of(vocabulary));
        assertSame(vocabulary, opened.getFirst());
    }

    @Test void invalidInputDoesNotSendARequestOrOpenDetails() {
        var search = new DictionarySearch(query -> { throw new AssertionError("Unexpected query"); },
            () -> {}, Runnable::run);
        for (String invalid : List.of(" ", "two\nlines", "x".repeat(201))) {
            search.setInput(invalid);
            search.submit(found -> fail("Unexpected navigation"));
            assertEquals(DictionarySearch.State.INVALID_INPUT, search.state());
            assertTrue(search.result().isEmpty());
        }
    }
}
