package io.github.piscescup.vocabularyspark.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

/** English translations for Vocabulary Spark. */
public final class ENUSLanguageProvider extends FabricLanguageProvider {
    public ENUSLanguageProvider(
        FabricPackOutput packOutput, CompletableFuture<HolderLookup.Provider> registryLookup
    ) {
        super(packOutput, "en_us", registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder translations) {
        translations.add("key.category.vocabulary-spark.main", "Vocabulary Spark");
        translations.add("key.vocabulary-spark.open", "Open Vocabulary Spark");
        translations.add("gui.vocabulary-spark.home", "Home");
        translations.add("gui.vocabulary-spark.wordbooks", "Wordbooks");
        translations.add("gui.vocabulary-spark.file", "File");
        translations.add("gui.vocabulary-spark.view", "View");
        translations.add("gui.vocabulary-spark.about", "About");
        translations.add("gui.vocabulary-spark.about_mod", "About Vocabulary Spark");
        translations.add("gui.vocabulary-spark.import", "Import wordbook…");
        translations.add("gui.vocabulary-spark.export", "Export wordbook…");
        translations.add("gui.vocabulary-spark.export_named", "Export %s…");
        translations.add("gui.vocabulary-spark.close", "Close");
        translations.add("gui.vocabulary-spark.replace", "Replace");
        translations.add("gui.vocabulary-spark.cancel", "Cancel");
        translations.add("gui.vocabulary-spark.ready", "Ready");
        translations.add("gui.vocabulary-spark.loading", "Loading wordbooks…");
        translations.add("gui.vocabulary-spark.loaded", "Loaded %s wordbooks");
        translations.add("gui.vocabulary-spark.load_partial", "%s wordbooks could not be loaded; valid books are available");
        translations.add("gui.vocabulary-spark.importing", "Importing wordbook…");
        translations.add("gui.vocabulary-spark.exporting", "Exporting wordbook…");
        translations.add("gui.vocabulary-spark.imported", "Imported %s · %s words");
        translations.add("gui.vocabulary-spark.already_imported", "%s is already in your library");
        translations.add("gui.vocabulary-spark.exported", "Exported %s");
        translations.add("gui.vocabulary-spark.operation_failed", "Operation failed: %s");
        translations.add("gui.vocabulary-spark.supported_files", "Wordbooks (*.json, *.txt)");
        translations.add("gui.vocabulary-spark.overwrite_title", "Replace file?");
        translations.add("gui.vocabulary-spark.overwrite_message", "This file already exists:\n%s\n\nReplace it with the exported wordbook?");
        translations.add("gui.vocabulary-spark.replace_book_title", "Replace wordbook?");
        translations.add("gui.vocabulary-spark.replace_book_message", "A wordbook with the same ID already exists.\n\nImport “%s” and replace its contents?\nID: %s");
        translations.add("gui.vocabulary-spark.error_title", "Wordbook operation failed");
        translations.add("gui.vocabulary-spark.error_message", "The operation could not be completed.\n\n%s");
        translations.add("gui.vocabulary-spark.about_message", "Vocabulary Spark\n\nYour vocabulary workspace in Minecraft.\n\nGUI and file dialogs: TCDCommons API.\nInterface architecture inspired by Better Statistics Screen.\n\nThis version provides the home dashboard, a local wordbook library, and JSON/TXT import and export.");
        translations.add("gui.vocabulary-spark.home_intro", "CET4, CET6, postgraduate, IELTS and TOEFL word lists are included. Open a book to browse, or import your own.");
        translations.add("gui.vocabulary-spark.quick_access", "Quick access");
        translations.add("gui.vocabulary-spark.overview", "Library overview");
        translations.add("gui.vocabulary-spark.book_count", "Wordbooks");
        translations.add("gui.vocabulary-spark.word_count", "Unique words");
        translations.add("gui.vocabulary-spark.selected_book", "Selected book");
        translations.add("gui.vocabulary-spark.none_selected", "None");
        translations.add("gui.vocabulary-spark.empty_hint", "Start by importing your CET4, CET6, IELTS or personal word list.");
        translations.add("gui.vocabulary-spark.no_books", "No wordbooks yet. Import a JSON or TXT file to get started.");
        translations.add("gui.vocabulary-spark.import_first", "Import your first book");
        translations.add("gui.vocabulary-spark.book_entry", "%s · %s words");
        translations.add("gui.vocabulary-spark.getting_started", "Getting started");
        translations.add("gui.vocabulary-spark.import_help", "File → Import wordbook\nSupports Vocabulary Spark and Qwerty Learner JSON.\nTXT: one word or phrase per line.\nImported books are kept locally and are available next time you launch the game.");
        translations.add("gui.vocabulary-spark.export_help", "Open a book to select it for export.\nJSON preserves its name and ID. TXT exports just the word list.");
        translations.add("gui.vocabulary-spark.library_hint", "CET4, CET6, postgraduate, IELTS and TOEFL word lists are included. You can also import your own. Select a book to browse or export, and switch between books using tabs.");
        translations.add("gui.vocabulary-spark.book_size", "%s words");
        translations.add("gui.vocabulary-spark.filter_words", "Filter words in this book…");
        translations.add("gui.vocabulary-spark.wordbook_lookup_hint", "Click a word or phrase to look it up and open its details.");
        translations.add("gui.vocabulary-spark.wordbook_lookup_word", "Look up %s");
        translations.add("gui.vocabulary-spark.no_matching_words", "No matching words.");
        translations.add("gui.vocabulary-spark.page_count", "%s / %s · %s words");
        translations.add("gui.vocabulary-spark.search", "Search");
        translations.add("gui.vocabulary-spark.search_submit", "Search");
        translations.add("gui.vocabulary-spark.search_placeholder", "Enter an English word or phrase…");
        translations.add("gui.vocabulary-spark.search_hint", "Enter a word, then press Enter or Search to look it up.");
        translations.add("gui.vocabulary-spark.search_loading", "Looking up %s…");
        translations.add("gui.vocabulary-spark.search_found", "Found %s. Click the word for its full entry.");
        translations.add("gui.vocabulary-spark.search_not_found", "No entry found for %s. Check the spelling and try again.");
        translations.add("gui.vocabulary-spark.search_invalid_input", "Enter a word or phrase of 1–200 characters.");
        translations.add("gui.vocabulary-spark.search_failed", "Lookup failed: %s");
        translations.add("gui.vocabulary-spark.search_connection_failed", "Could not connect to the dictionary service. Check your connection and try again.");
        translations.add("gui.vocabulary-spark.search_word", "Word");
        translations.add("gui.vocabulary-spark.search_pos", "POS");
        translations.add("gui.vocabulary-spark.search_translation", "Translation");
        translations.add("gui.vocabulary-spark.search_open_details", "Open word details");
        translations.add("gui.vocabulary-spark.detail_back", "← Back");
        translations.add("gui.vocabulary-spark.detail_title", "Word details");
        translations.add("gui.vocabulary-spark.detail_contents", "Contents");
        translations.add("gui.vocabulary-spark.detail_definitions", "Meanings");
        translations.add("gui.vocabulary-spark.detail_forms", "Word forms");
        translations.add("gui.vocabulary-spark.detail_no_definitions", "No definitions available.");
        translations.add("gui.vocabulary-spark.detail_no_forms", "No word forms available.");
        translations.add("gui.vocabulary-spark.pronunciation_play", "Click to play the %s pronunciation");
        translations.add("gui.vocabulary-spark.pronunciation_loading", "Loading the %s pronunciation…");
        translations.add("gui.vocabulary-spark.pronunciation_playing", "Playing %s; click to replay");
        translations.add("gui.vocabulary-spark.pronunciation_failed", "Could not load or play this pronunciation. Click to retry.");
        translations.add("gui.vocabulary-spark.pronunciation_muted", "Pronunciation is muted. Check Master and UI volume.");
    }
}
