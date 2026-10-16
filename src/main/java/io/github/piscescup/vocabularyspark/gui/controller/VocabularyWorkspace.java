package io.github.piscescup.vocabularyspark.gui.controller;

import io.github.piscescup.vocabularyspark.VocabularySparkModReferences;
import io.github.piscescup.vocabularyspark.client.audio.PronunciationPlayer;
import io.github.piscescup.vocabularyspark.dictionary.DictionaryClient;
import io.github.piscescup.vocabularyspark.study.StudyFiles;
import io.github.piscescup.vocabularyspark.gui.widget.WordbookTitles;
import io.github.piscescup.vocabularyspark.wordbook.Wordbook;
import io.github.piscescup.vocabularyspark.wordbook.WordbookFiles;
import io.github.piscescup.vocabularyspark.wordbook.WordbookLibrary;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;
import static io.github.piscescup.vocabularyspark.gui.controller.WorkspaceTab.Page.HOME;

/**
 * MVC controller, following Better Statistics' editor/tab/revision organization.
 */
public final class VocabularyWorkspace implements AutoCloseable {
    private final WordbookLibrary library;
    private final Path dailyDirectory;
    private final Executor clientExecutor;
    private final DictionarySearch dictionarySearch;
    private final DailyStudy dailyStudy;
    private final PronunciationPlayer pronunciationPlayer;
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Vocabulary Spark wordbooks");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, Wordbook> books = new LinkedHashMap<>();
    private final List<WorkspaceTab> tabs = new ArrayList<>(List.of(HOME));
    private final Map<WorkspaceTab, String> filters = new LinkedHashMap<>();
    private final Map<WorkspaceTab.Book, Integer> wordbookPages = new LinkedHashMap<>();
    private WorkspaceTab currentTab = HOME;
    private String selectedBookId;
    private long revision;
    private boolean busy;
    private Component status = text("ready");
    private Consumer<Throwable> errorHandler = failure -> {
    };

    public VocabularyWorkspace(
        WordbookLibrary library, Executor clientExecutor, PronunciationPlayer pronunciationPlayer,
        StudyFiles studyFiles
    ) {
        this.library = library;
        this.dailyDirectory = studyFiles.directory()
            .getParent();
        this.clientExecutor = clientExecutor;
        this.pronunciationPlayer = pronunciationPlayer;
        var dictionary = new DictionaryClient(library.directory()
            .getParent()
            .resolve("dictionary.json"));
        this.dictionarySearch = new DictionarySearch(dictionary, clientExecutor);
        this.dailyStudy = new DailyStudy(studyFiles, dictionary::lookup, clientExecutor);
    }

    public DictionarySearch dictionarySearch() {
        return dictionarySearch;
    }

    public DailyStudy dailyStudy() {
        return dailyStudy;
    }

    public PronunciationPlayer pronunciationPlayer() {
        return pronunciationPlayer;
    }

    public WordbookLibrary library() {
        return library;
    }

    public List<Wordbook> books() {
        return List.copyOf(books.values());
    }

    public List<WorkspaceTab> tabs() {
        return List.copyOf(tabs);
    }

    public WorkspaceTab currentTab() {
        return currentTab;
    }

    public long revision() {
        return revision;
    }

    public boolean busy() {
        return busy;
    }

    public Component status() {
        return status;
    }

    public Optional<Wordbook> book(String id) {
        return Optional.ofNullable(books.get(id));
    }

    public Optional<Wordbook> selectedBook() {
        return book(selectedBookId);
    }

    public String filter(WorkspaceTab tab) {
        return filters.getOrDefault(tab, "");
    }

    public void setFilter(WorkspaceTab tab, String query) {
        filters.put(tab, query);
    }

    /**
     * Keeps the current wordbook page when a dictionary-detail screen is opened and closed.
     */
    public int wordbookPage(WorkspaceTab.Book tab) {
        return wordbookPages.getOrDefault(tab, 0);
    }

    public void setWordbookPage(WorkspaceTab.Book tab, int page) {
        wordbookPages.put(tab, Math.max(0, page));
    }

    public void setErrorHandler(Consumer<Throwable> handler) {
        errorHandler = handler;
    }

    public void open(WorkspaceTab tab) {
        if (!tabs.contains(tab)) tabs.add(tab);
        currentTab = tab;
        if (tab instanceof WorkspaceTab.Book bookTab) selectedBookId = bookTab.bookId();
        revision++;
    }

    public void openBook(Wordbook book) {
        open(new WorkspaceTab.Book(book));
    }

    public void closeTab(WorkspaceTab tab) {
        if (tab == HOME) return;
        int index = tabs.indexOf(tab);
        if (index < 0) return;
        tabs.remove(index);
        filters.remove(tab);
        if (tab instanceof WorkspaceTab.Book book) wordbookPages.remove(book);
        if (currentTab.equals(tab)) currentTab = tabs.get(Math.max(0, index - 1));
        if (currentTab instanceof WorkspaceTab.Book bookTab) selectedBookId = bookTab.bookId();
        revision++;
    }

    public void load() {
        runIo(
            "loading", library::load, result -> {
                result.books()
                    .forEach(book -> books.put(book.id(), book));
                dailyStudy.initialize(result.books());
                status = text("loaded", books.size());
                if (!result.failures()
                    .isEmpty()) {
                    errorHandler.accept(new java.io.IOException(String.join("\n", result.failures())));
                    status = text(
                        "load_partial", result.failures()
                            .size()
                    );
                }
            }
        );
    }

    public void readImport(Path source, Consumer<Wordbook> confirmReplacement) {
        runIo(
            "importing", () -> WordbookFiles.read(source), imported -> {
                Wordbook previous = books.get(imported.id());
                if (imported.equals(previous)) {
                    openBook(previous);
                    status = text("already_imported", WordbookTitles.title(imported));
                } else if (previous != null) {
                    status = text("ready");
                    confirmReplacement.accept(imported);
                } else {
                    saveImport(imported, false);
                }
            }
        );
    }

    public void saveImport(Wordbook book, boolean replace) {
        runIo(
            "importing", () -> {
                library.save(book, replace);
                return book;
            }, saved -> {
                // Refresh an already-open tab when a replacement changes the book's display name.
                for (int index = 0; index < tabs.size(); index++) {
                    if (tabs.get(index) instanceof WorkspaceTab.Book tab && tab.bookId()
                        .equals(saved.id())) {
                        filters.remove(tabs.set(index, new WorkspaceTab.Book(saved)));
                        wordbookPages.remove(tab);
                    }
                }
                books.put(saved.id(), saved);
                dailyStudy.updateBook(saved);
                openBook(saved);
                status = text(
                    "imported", WordbookTitles.title(saved), saved.words()
                        .size()
                );
            }
        );
    }

    public void export(Path target, Wordbook book, boolean replace) {
        runIo(
            "exporting", () -> {
                Path destination = target.toAbsolutePath()
                    .normalize();
                if (destination.startsWith(library.directory()) || destination.startsWith(dailyDirectory)) {
                    throw new java.io.IOException(
                        "Choose an export location outside the managed wordbooks and daily directories");
                }
                WordbookFiles.write(target, book, replace);
                return target;
            },
            path -> status = text(
                "exported", path.getFileName()
                    .toString()
            )
        );
    }

    private <T> void runIo(String message, IoTask<T> task, Consumer<T> onSuccess) {
        if (busy) return;
        busy = true;
        status = text(message);
        revision++;
        CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return task.run();
                    } catch (Exception exception) {
                        throw new java.util.concurrent.CompletionException(exception);
                    }
                }, io
            )
            .whenCompleteAsync(
                (result, exception) -> {
                    busy = false;
                    if (exception == null) {
                        onSuccess.accept(result);
                    } else {
                        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                        VocabularySparkModReferences.MOD_LOGGER.warn("Wordbook operation failed", cause);
                        status = text("operation_failed", cause.getMessage());
                        errorHandler.accept(cause);
                    }
                    revision++;
                }, clientExecutor
            );
    }

    public void close() {
        dailyStudy.close();
        pronunciationPlayer.close();
        dictionarySearch.close();
        io.shutdown();
    }

    @FunctionalInterface
    private interface IoTask<T> {
        T run() throws Exception;
    }
}
