# Vocabulary Spark

## Client workspace

Requires **TCDCommons API 5.6.0 for Minecraft 26.3** (installed separately when
using the released mod). Gradle downloads it automatically for development.

Press **K** in-game, or run the client command **`/vocabulary`**, to open the
workspace. The key can be changed in Controls → Key Binds → Vocabulary Spark.
Both English and Simplified Chinese interface text are included.

The home dashboard has File / View / About menus, quick access buttons, library
counts and a list of imported wordbooks. Books open in closable tabs, with
filtering and pagination. The home tab stays available. Online dictionary
search is available from Home or View → Search. Daily study plans and spaced
repetition are not implemented yet.

### Dictionary search

Use **View → Search** (or the home page Search button), enter an English word or
phrase, then press Enter or click Search. The result is one clickable summary
row: word, part of speech and the first available translation. Click it (or
focus it and press Enter) to open an independent dictionary-detail GUI.

The compact detail page uses a translucent background, small text, blue accents,
and a sidebar of bordered navigation buttons. Pronunciations appear together
below the headword; identical region/transcription pairs are displayed once.
Each part of speech has its own tab labeled with `PartOfSpeech.getName()`.
`VocabularyTranslationEntry` stores the numeric definition `id`, exposed through
`TranslationEntry.id()`. Its natural ordering compares IDs first and uses the
existing text and grammar comparisons to break ties. `Vocabulary.translations()`
therefore returns senses in ID order directly. Examples are sorted by `id` within
each definition during response parsing, retaining array order for equal IDs.
Source-page whitespace inside English definitions is collapsed for display.
English definitions and cyan Chinese
translations appear on separate lines, followed by indented bilingual examples.
All examples returned by the display API are shown directly, left-aligned.
Text shadows are disabled through TCDCommons. The Word forms sidebar button
is always available and displays an empty-state message when no forms are supplied.
Narrow windows put the navigation
above the content. Back/Escape returns to the search workspace.

Search and wordbook-filter fields support Ctrl+A/C/X/V, selection replacement,
arrow/Home/End movement, Shift selection, and mouse selection. They are implemented
as a TCDCommons widget in the GUI package.

Headwords display their original spelling; no syllable separator dots are
inserted. Dictionary colors, text scale and component formatting are centralized
in the GUI package's `DictionaryTextStyles`, which styles copies of components
returned by `Word`, `Pronunciation` and `TranslationEntry`. Pronunciation labels
come from `Pronunciation.area()` (such as `UK_En`), and transcriptions come from
`phoneticSymbol()`. The model and its formatting API remain unchanged.
The data source does not supply Oxford's derivations,
CEFR badges or synonym/antonym cross-references; these are not inferred.

The query uses the Cambridge API documented below, with the default endpoint
`https://dictionary-api.eliaschen.dev/api/dictionary/en-cn/`. Queries run
asynchronously with a 20-second request timeout. The page retains its input and
results when switching tabs; a newer search supersedes an earlier one. Successful
entries are cached in memory for 30 minutes, with at most 128 cached entries.
Loading, not-found and error states are displayed in the results panel.

To use your own compatible API deployment, create
`<game directory>/config/vocabulary-spark/dictionary.json`:

```json
{
  "endpoint": "http://localhost:3000/api/dictionary/en-cn/"
}
```

The endpoint is read for each submitted query. The encoded search word is
appended to it. It must return the existing `Vocabulary` JSON format.

### Import and export

- **File → Import wordbook** opens TCDCommons' in-game file chooser.
- Import UTF-8 JSON (schema below), or TXT with one word/phrase per line.
- Blank lines and surrounding whitespace are removed. Duplicate words are
  compared without case; the first spelling and original order are preserved.
- Imports are saved as managed JSON copies in
  `<game directory>/config/vocabulary-spark/wordbooks/`. Original files are
  left untouched. The library reloads when the client starts.
- Open a book, then use **File → Export** or the book's Export button.
- JSON preserves the ID, display name and word list. TXT contains only the
  words; reimporting TXT derives a name from its filename and a stable ID from
  that name and the normalized contents.
- Reimporting identical JSON selects the existing book. Changing a book with
  the same ID requires confirmation before replacing its managed copy.
- Exporting over an existing file requires confirmation. Cancel/Escape does
  not write. Invalid files produce an in-game error dialog; a corrupt stored
  book does not prevent the remaining library from loading.

```json
{
  "formatVersion": 1,
  "id": "my-cet4",
  "name": "我的四级单词本",
  "words": ["abandon", "ability", "able"]
}
```

The `examples/` directory contains small JSON and TXT files for trying the
workflow. They are demonstrations, not complete exam vocabulary lists. Each
book needs a stable, unique ID. Current limits are 16 MiB per imported file,
100,000 source entries, 200 characters per word/name, and 128 characters per ID.
Wordbooks contain membership and ordering; dictionary details and future
learning progress are separate data.

### Interface architecture

The MVC/editor/tab layout follows the organization of
[Better Statistics' views](https://github.com/TheCSDev/betterstats/tree/main/src/main/java/com/thecsdev/betterstats/mcbs/view).
Better Statistics itself is not a runtime dependency and its assets are not bundled.

- `wordbook/`: portable wordbook model, JSON/TXT codec and local library.
- `dictionary/`: asynchronous HTTP queries and bounded in-memory word cache.
- `gui/widget/`: reusable controls, clickable word summaries and dictionary-detail
  components (navigation, POS badge, bilingual senses and page layout).
- `gui/workspace/controller/`: workspace state, tabs, search state and file actions.
- `gui/workspace/view/`: root GUI and tab-view factory.
- `view/editor/`, `view/menubar/`, `view/tab/`, `view/dialog/`: menu strip,
  tab strip, menu definitions, page views and confirmation dialog.

Views use TCDCommons `TScreenPlus`, `TElement`, panels, labels, buttons, scrollbars,
context menus, file chooser and promise dialogs. Minecraft screen references
only bridge to the engine; no vanilla widgets, file dialogs or GUI render
utilities are used in the workspace. File I/O runs on a dedicated worker, with
state changes scheduled back to the client thread. Reusable controls are actual
components under `gui.widget`, rather than static GUI factory methods.

Run `gradlew.bat test build` to verify the file workflows and build the mod.

## Dictionary JSON

`Vocabulary` is a record whose `@SerializedName` annotations map responses
from [chenelias/cambridge-dictionary-api](https://github.com/chenelias/cambridge-dictionary-api).
No custom JSON adapter is required.
In the mod, deserialize the response body with:

```java
Vocabulary vocabulary = References.GSON.fromJson(json, Vocabulary.class);
```

`VocabularyTranslationEntry` and `VocabularyExample` store strings and use
`@SerializedName` to map the API fields, without a custom sense adapter. An
individual object from the response's `definition` array can be read independently:

```java
VocabularyTranslationEntry entry = References.GSON.fromJson(
    definitionJson, VocabularyTranslationEntry.class
);
```

The sense's `englishText()`, `chineseText()`, `rawPartOfSpeech()` and
`rawExamples()` expose the original data. Serializing a sense with Gson writes
the API field names and raw strings, rather than Minecraft component internals.

Display methods build fresh Minecraft text components using the string-based
methods in the GUI package's `DictionaryTextStyles`, which calls the existing `ComponentFormatUtils` API without modifying it. Pronunciation regions retain the API's original labels. Each sense returns its English definition
from `englishDefinition()` and Chinese translation from `translation()`.
Example components contain the English text followed by the Chinese translation
on a new line. Pronunciations use one `AreaPronunciation` record for all regions,
storing the original region code, phonetic string and audio URL. Display labels
such as `UKEn` and `USEn` are computed by `area()` and are not written into `lang`.

`rawPronunciations()` contains `PronunciationData` records, and `rawForms()`
contains `WordFormData` records. The display accessors build area-based
pronunciations and group forms by `WordForm`. `forms()` preserves multiple spellings of
the same form by displaying them together, for example `dreamed, dreamt`.

Exam categories and grammar labels stay empty for ordinary upstream responses:
the API does not return those fields. Records can be serialized with Gson and
read back using the same annotations, without Minecraft component internals.

The query layer must check HTTP status and API error responses before parsing.
The vocabulary constructor rejects missing or blank headwords. Missing optional
arrays become immutable empty lists. Raw incomplete pronunciations are retained;
`pronunciations()` omits entries lacking a region, symbol or audio URL. Any
non-blank region is supported. Invalid URLs throw `IllegalArgumentException`
when a pronunciation view is requested. Record fields follow Gson's normal
conversion rules.

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
