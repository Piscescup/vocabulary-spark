package io.github.piscescup.vocabularyspark.gui.controller;

import com.thecsdev.commonmc.api.client.gui.screen.TScreenWrapper;
import com.thecsdev.commonmc.api.client.gui.screen.TTextDialogScreen;
import com.thecsdev.commonmc.api.client.gui.screen.promise.TFileChooserScreen;
import io.github.piscescup.vocabularyspark.gui.VocabularyScreen;
import io.github.piscescup.vocabularyspark.gui.view.dialog.ConfirmScreen;
import io.github.piscescup.vocabularyspark.wordbook.Wordbook;
import io.github.piscescup.vocabularyspark.gui.widget.WordbookTitles;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;

import static io.github.piscescup.vocabularyspark.utils.ComponentUtils.text;

/**
 * File menu actions use TCDCommons' file chooser, promise screens and text dialogs.
 */
public final class WordbookActions {
    private final Minecraft client;
    private final VocabularyWorkspace workspace;
    private Path lastDirectory;

    public WordbookActions(Minecraft client, VocabularyWorkspace workspace) {
        this.client = client;
        this.workspace = workspace;
        this.lastDirectory = client.gameDirectory.toPath();
        workspace.setErrorHandler(this::showError);
    }

    public void importBook() {
        if (workspace.busy()) return;
        var chooser = new TFileChooserScreen.Builder(TFileChooserScreen.Mode.CHOOSE_FILE)
            .setLastScreen(client.gui.screen())
            .setCurrentDirectory(lastDirectory)
            .setPathFilter(
                TFileChooserScreen.PathFilter.extnames(text("supported_files"), "json", "txt", "sqlite", "JSON", "TXT", "SQLITE"))
            .build();
        chooser.getResult()
            .thenAccept(paths -> {
                if (paths.isEmpty()) return;
                Path path = paths.getFirst();
                lastDirectory = path.toAbsolutePath()
                    .getParent();
                workspace.readImport(path, this::confirmImportReplacement);
            })
            .exceptionally(failure -> {
                if (!chooser.getResult()
                    .isCancelled()) {
                    showError(failure);
                }
                return null;
            });
        client.gui.setScreen(chooser.getAsScreen());
    }

    public void exportSelected() {
        workspace.selectedBook()
            .ifPresent(this::exportBook);
    }

    public void exportBook(Wordbook book) {
        if (workspace.busy()) return;
        var chooser = new TFileChooserScreen.Builder(TFileChooserScreen.Mode.CREATE_FILE)
            .setLastScreen(client.gui.screen())
            .setCurrentDirectory(lastDirectory)
            .setPathFilter(TFileChooserScreen.PathFilter.extname("json"))
            .addPathFilter(TFileChooserScreen.PathFilter.extname("txt"))
            .addPathFilter(TFileChooserScreen.PathFilter.extname("sqlite"))
            .build();
        chooser.getResult()
            .thenAccept(paths -> {
                if (paths.isEmpty()) return;
                Path target = paths.getFirst();
                lastDirectory = target.toAbsolutePath()
                    .getParent();
                if (Files.exists(target)) {
                    confirm(
                        text("overwrite_title"), text("overwrite_message", target.toString()),
                        () -> workspace.export(target, book, true)
                    );
                } else {
                    workspace.export(target, book, false);
                }
            })
            .exceptionally(failure -> {
                if (!chooser.getResult()
                    .isCancelled()) {
                    showError(failure);
                }
                return null;
            });
        client.gui.setScreen(chooser.getAsScreen());
    }

    private void confirmImportReplacement(Wordbook book) {
        confirm(
            text("replace_book_title"), text("replace_book_message", WordbookTitles.title(book), book.id()),
            () -> workspace.saveImport(book, true)
        );
    }

    private void confirm(
        net.minecraft.network.chat.Component title, net.minecraft.network.chat.Component message,
        Runnable accepted
    ) {
        if (!isWorkspaceOpen()) return;
        var dialog = new ConfirmScreen(client.gui.screen(), title, message);
        dialog.getResult()
            .thenAccept(replace -> {
                if (replace) accepted.run();
            });
        client.gui.setScreen(dialog.getAsScreen());
    }

    public void showAbout() {
        var dialog = new TTextDialogScreen(client.gui.screen(), text("about"), text("about_message"));
        client.gui.setScreen(dialog.getAsScreen());
    }

    private void showError(Throwable failure) {
        while (failure.getCause() != null) failure = failure.getCause();
        if (failure instanceof CancellationException || !isWorkspaceOpen()) return;
        var dialog = new TTextDialogScreen(
            client.gui.screen(), text("error_title"),
            text("error_message", failure.getMessage() == null ? failure.toString() : failure.getMessage())
        );
        client.gui.setScreen(dialog.getAsScreen());
    }

    private boolean isWorkspaceOpen() {
        return client.gui.screen() instanceof TScreenWrapper<?> screen
            && screen.getTargetTScreen() instanceof VocabularyScreen;
    }
}
