package io.github.piscescup.vocabularyspark.gui.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.piscescup.vocabularyspark.study.StudySnapshot;
import io.github.piscescup.vocabularyspark.study.StudyFiles;
import io.github.piscescup.vocabularyspark.study.StudyStore;
import io.github.piscescup.vocabularyspark.study.StudyPlan;
import io.github.piscescup.vocabularyspark.vocabulary.Word;
import io.github.piscescup.vocabularyspark.wordbook.Wordbook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/** Client-thread study state with serialized storage work and guarded asynchronous dictionary results. */
public final class DailyStudy implements AutoCloseable {
    private final StudyFiles files;
    private final Function<String, CompletableFuture<Optional<Word>>> lookup;
    private final Executor clientExecutor;
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable, "Vocabulary Spark daily study");
        thread.setDaemon(true);
        return thread;
    });
    private String selectedBookId;
    private Wordbook selectedBook;
    private String goalDraft = "20";
    private StudySnapshot snapshot;
    private LocalDate automaticRefreshDay;
    private Word definition;
    private boolean busy, revealed, definitionLoading, closed, refreshRequested;
    private String error = "", lookupError = "";
    private long revision, operation, lookupOperation;
    private CompletableFuture<Optional<Word>> pendingLookup;

    public DailyStudy(StudyFiles files, Function<String, CompletableFuture<Optional<Word>>> lookup,
                      Executor clientExecutor) {
        this.files = files;
        this.lookup = lookup;
        this.clientExecutor = clientExecutor;
    }

    public String selectedBookId() { return selectedBookId; }
    public String goalDraft() { return goalDraft; }
    public void setGoalDraft(String value) { goalDraft = value; }
    public Optional<StudySnapshot> snapshot() { return Optional.ofNullable(snapshot); }
    public Optional<Word> definition() { return Optional.ofNullable(definition); }
    public boolean busy() { return busy; }
    public boolean revealed() { return revealed; }
    public boolean definitionLoading() { return definitionLoading; }
    public String error() { return error; }
    public String lookupError() { return lookupError; }
    public long revision() { return revision; }

    public CompletableFuture<StudyPlan> todayWords() {
        if (closed || selectedBook == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Choose a wordbook first"));
        }
        Wordbook book = selectedBook;
        LocalDate day = LocalDate.now();
        return CompletableFuture.supplyAsync(() -> {
            try { return StudyStore.plan(files.prepare(book), book, day); }
            catch (IOException exception) { throw new CompletionException(exception); }
        }, io);
    }

    public void initialize(List<Wordbook> books) {
        long request = ++operation;
        CompletableFuture.supplyAsync(() -> {
            try {
                Path settings = files.settingsPath();
                if (!Files.exists(settings)) return "";
                var root = JsonParser.parseString(Files.readString(settings, StandardCharsets.UTF_8)).getAsJsonObject();
                return root.has("bookId") ? root.get("bookId").getAsString() : "";
            } catch (IOException | RuntimeException exception) {
                throw new CompletionException(exception);
            }
        }, io).whenCompleteAsync((id, failure) -> {
            if (closed || request != operation) return;
            if (failure != null) { error = message(failure); revision++; return; }
            books.stream().filter(book -> book.id().equals(id)).findFirst().ifPresent(this::select);
        }, clientExecutor);
    }

    public void select(Wordbook book) {
        if (closed) return;
        selectedBookId = book.id();
        selectedBook = book;
        refreshRequested = false;
        snapshot = null;
        submit(() -> {
            saveSelection(book.id());
            return StudyStore.load(files.prepare(book), LocalDate.now(), null);
        }, true);
    }

    public void refresh() {
        if (closed || selectedBookId == null) return;
        if (busy) { refreshRequested = true; return; }
        Wordbook book = selectedBook;
        submit(() -> StudyStore.load(files.prepare(book), LocalDate.now(), null), false);
    }

    public void updateBook(Wordbook book) {
        if (!book.id().equals(selectedBookId)) return;
        selectedBook = book;
        refresh();
    }

    public void saveGoal() {
        if (busy || selectedBookId == null) return;
        int goal;
        try {
            goal = Integer.parseInt(goalDraft.strip());
            if (goal < 1 || goal > StudyStore.MAX_GOAL) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            error = "invalid_goal"; revision++; return;
        }
        Wordbook book = selectedBook;
        submit(() -> StudyStore.load(files.prepare(book), LocalDate.now(), goal), true);
    }

    public void reveal() { revealed = true; revision++; }

    public void grade(StudyStore.Grade grade) {
        if (closed || busy || snapshot == null || snapshot.current() == null) return;
        if (!snapshot.day().equals(LocalDate.now())) { refresh(); return; }
        Wordbook book = selectedBook;
        var card = snapshot.current();
        LocalDate day = snapshot.day();
        submit(() -> StudyStore.grade(files.databasePath(book), day, card.key(), grade), false);
    }

    public void tick() {
        LocalDate today = LocalDate.now();
        if (snapshot != null && !snapshot.day().equals(today) && !busy && !today.equals(automaticRefreshDay)) {
            automaticRefreshDay = today;
            refresh();
        }
    }

    public void lookupDefinition() {
        if (closed || snapshot == null || snapshot.current() == null) return;
        invalidateLookup();
        long request = lookupOperation;
        definitionLoading = true;
        lookupError = "";
        revision++;
        try {
            pendingLookup = lookup.apply(snapshot.current().word());
        } catch (RuntimeException exception) {
            definitionLoading = false; lookupError = message(exception); revision++; return;
        }
        pendingLookup.whenCompleteAsync((word, failure) -> {
            if (closed || request != lookupOperation) return;
            definitionLoading = false;
            if (failure == null) definition = word.orElse(null);
            else lookupError = message(failure);
            revision++;
        }, clientExecutor);
    }

    private void submit(IoTask task, boolean updateGoal) {
        long request = ++operation;
        busy = true; error = ""; revealed = false;
        invalidateLookup();
        revision++;
        CompletableFuture.supplyAsync(() -> {
            try { return task.run(); }
            catch (Exception exception) { throw new CompletionException(exception); }
        }, io).whenCompleteAsync((result, failure) -> {
            if (closed || request != operation) return;
            busy = false;
            if (failure != null) error = message(failure);
            else {
                snapshot = result;
                if (updateGoal) goalDraft = Integer.toString(result.goal());
            }
            revision++;
            if (refreshRequested) { refreshRequested = false; refresh(); return; }
            if (failure == null) lookupDefinition();
        }, clientExecutor);
    }

    private void invalidateLookup() {
        lookupOperation++;
        if (pendingLookup != null) pendingLookup.cancel(true);
        definition = null; definitionLoading = false; lookupError = "";
    }

    private void saveSelection(String id) throws IOException {
        Path settings = files.settingsPath();
        Files.createDirectories(settings.getParent());
        var root = new JsonObject(); root.addProperty("bookId", id);
        Path temporary = Files.createTempFile(settings.getParent(), ".study-", ".tmp");
        try {
            Files.writeString(temporary, root + "\n", StandardCharsets.UTF_8);
            try { Files.move(temporary, settings, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, settings, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static String message(Throwable failure) {
        while (failure instanceof CompletionException && failure.getCause() != null) failure = failure.getCause();
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    @Override
    public void close() {
        closed = true; operation++; invalidateLookup(); io.shutdown();
        try {
            io.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface IoTask { StudySnapshot run() throws Exception; }
}
