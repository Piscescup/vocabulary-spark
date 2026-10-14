# Vocabulary Spark

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
methods in `ComponentFormatUtils`. Each sense returns its English definition
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
