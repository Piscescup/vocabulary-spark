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

Five Qwerty Learner word lists (CET4, CET6, postgraduate English, IELTS and TOEFL)
are bundled with the mod and appear automatically in the library. Their word
lists are available offline; online dictionary lookup still needs a connection.

Click a word or phrase inside any wordbook to look it up. The status line shows
loading, not-found or error information; a successful lookup opens the existing
dictionary-detail screen automatically. Back/Escape returns to the original
wordbook with its filter and page preserved. Queries reuse the search page's
dictionary client and cache. Only the latest request can open details, and a
completed request does not reopen the book after you leave it or close the workspace.

### Dictionary search

Use **View → Search** (or the home page Search button), enter an English word or
phrase, then press Enter or click Search. The result is one clickable summary
row: word, part of speech and the first available translation. Click it (or
focus it and press Enter) to open an independent dictionary-detail GUI.

The compact detail page uses a translucent background, small text, blue accents,
and a sidebar of bordered navigation buttons. Pronunciations appear together
below the headword; identical region/transcription/audio-URL combinations are
displayed once. Click a regional label or phonetic transcription to play that
pronunciation. Hover for loading, playback, retry or muted-state information.
Audio uses Minecraft's Master and UI volume settings. Selecting another
pronunciation stops the previous clip; leaving the detail screen stops playback
and prevents an earlier download from starting afterward.

Pronunciation MP3 files are downloaded and decoded asynchronously. Decoded clips
are cached for the game session (up to 32 clips and 16 MiB), and failed downloads
can be retried. Playback uses Fabric's existing custom audio-stream API; the
bundled JLayer 1.0.1 dependency supplies MP3 decoding. No external player or
separately installed codec is required.

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
above the content. Back/Escape returns to the originating search or wordbook page.

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
- Import UTF-8 JSON (schema below), Qwerty Learner JSON, or TXT with one
  word/phrase per line.
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

### Qwerty Learner wordbooks

Download a JSON file from
[Qwerty Learner's public/dicts directory](https://github.com/RealKai42/qwerty-learner/tree/master/public/dicts)
using **Raw / Download raw file**, then select it through **File → Import wordbook**.
The importer automatically recognizes arrays of objects containing a string
`name`. It imports those names as words, preserves phrases and ordering, and
removes case-insensitive duplicates. Missing or invalid names reject the whole
import rather than silently losing entries. Optional translations and phonetic
fields are not required or stored in the wordbook.

The five basic files receive readable titles:

| Source file | Imported title |
| --- | --- |
| `CET4_T.json` | CET-4 · Qwerty |
| `CET6_T.json` | CET-6 · Qwerty |
| `KaoYan_3_T.json` | 考研英语 · Qwerty |
| `IELTS_3_T.json` | IELTS · Qwerty |
| `TOEFL_3_T.json` | TOEFL · Qwerty |

Other Qwerty dictionaries use their filename as the title, followed by `· Qwerty`.
The ID is derived from the filename without its extension (case-insensitive),
not its directory or contents. Keep filenames when updating a source book;
changed contents then use the existing replacement-confirmation workflow.
Different books should use different filenames. Exported JSON uses the native
Vocabulary Spark schema and preserves this ID, so no conversion is needed on
subsequent imports. Original source files are never modified.

`examples/sample-qwerty.json` is a small, locally authored format demonstration.
The five basic datasets are bundled under
`src/main/resources/assets/vocabulary-spark/wordbooks/` in native wordbook format.
They are read directly from the JAR using resource streams, without extraction
or network requests. Imported local books with the same ID override their bundled
versions after the existing replacement confirmation; other local books appear
alongside them. Updating the mod updates bundled books unless a local override
exists. `WordbookLibrary.withBuiltins(...)` enables this combined library, while
the ordinary constructor loads local books only.

Only the word lists are bundled; upstream translations and audio are not.
Source revision, original filenames and conversion details are recorded in
`META-INF/licenses/QwertyLearner-NOTICE.txt`, alongside the upstream license.
The word-list assets have their own upstream licensing notices and are not
relicensed under the project's CC0 declaration.

Qwerty's index lists
multiple versions of some exams; a book's exam label alone does not establish
that it contains a complete or current official syllabus. Its repository uses
[GPL-3.0](https://github.com/RealKai42/qwerty-learner/blob/master/LICENSE), and its
README attributes dictionary data to kajweb/dict and community contributions;
check the source and applicable data permissions before redistributing a dataset.

### Interface architecture

The MVC/editor/tab layout follows the organization of
[Better Statistics' views](https://github.com/TheCSDev/betterstats/tree/main/src/main/java/com/thecsdev/betterstats/mcbs/view).
Better Statistics itself is not a runtime dependency and its assets are not bundled.

- `wordbook/`: portable wordbook model, JSON/TXT codec and local library.
- `dictionary/`: asynchronous HTTP queries and bounded in-memory word cache.
- `gui/widget/`: reusable controls, clickable word summaries and dictionary-detail
  components (navigation, pronunciation controls, bilingual senses and page layout).
- `client/audio/`: MP3 decoding, bounded pronunciation cache and game sound playback.
- `gui/workspace/controller/`: workspace state, tabs, search state and file actions.
- `gui/workspace/view/`: root GUI and tab-view factory.
- `view/editor/`, `view/menubar/`, `view/tab/`, `view/dialog/`: menu strip,
  tab strip, menu definitions, page views and confirmation dialog.

Views use TCDCommons `TScreenPlus`, `TElement`, panels, labels, buttons, scrollbars,
context menus, file chooser and promise dialogs. Minecraft screen references
only bridge to the engine; no vanilla widgets, file dialogs or GUI render
utilities are used in the workspace. File I/O runs on a dedicated worker, with
state changes scheduled back to the client thread. `Labels` and `Buttons` create
configured native TCDCommons elements; controls with additional behavior remain
components under `gui.widget`.

Run `gradlew.bat test build` to verify file workflows, audio decoding and download
behavior, and build the mod. Audio download tests use a temporary local HTTP
server and generated silent MP3 frames, without contacting the dictionary service.

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

The bundled JLayer 1.0.1 library is licensed under LGPL-2.0-or-later. Its license
and source-download notice are included under `META-INF/licenses/` in the mod.
