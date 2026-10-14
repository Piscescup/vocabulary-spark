package io.github.piscescup.fabricmc.dictionary;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.piscescup.fabricmc.vocabulary.Vocabulary;
import io.github.piscescup.fabricmc.vocabulary.Word;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Asynchronous Cambridge API queries returning the vocabulary model through its Word interface. */
public final class DictionaryClient implements AutoCloseable {
    public static final String DEFAULT_ENDPOINT =
        "https://dictionary-api.eliaschen.dev/api/dictionary/en-cn/";
    private static final Gson JSON = new Gson();
    private static final long CACHE_LIFETIME = Duration.ofMinutes(30).toNanos();
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private final Map<URI, CachedWord> cache = new LinkedHashMap<>(16, 0.75f, true);
    private final Path settings;

    public DictionaryClient(Path settings) {
        this.settings = settings;
    }

    public CompletableFuture<Optional<Word>> lookup(String word) {
        // Configuration reads and all HTTP work happen outside the render/client thread.
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encoded = URLEncoder.encode(word, StandardCharsets.UTF_8).replace("+", "%20");
                return URI.create(endpoint().toASCIIString() + encoded);
            } catch (IOException | RuntimeException exception) {
                throw new CompletionException(exception);
            }
        }).thenCompose(uri -> {
            synchronized (cache) {
                CachedWord cached = cache.get(uri);
                if (cached != null && System.nanoTime() - cached.savedAt() < CACHE_LIFETIME) {
                    return CompletableFuture.completedFuture(Optional.of(cached.word()));
                }
                cache.remove(uri);
            }
            var request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "Vocabulary-Spark/1.0")
                .GET().build();
            return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    try {
                        Optional<Word> result = parse(response);
                        result.ifPresent(vocabulary -> {
                            synchronized (cache) {
                                cache.put(uri, new CachedWord(vocabulary, System.nanoTime()));
                                if (cache.size() > 128) cache.remove(cache.keySet().iterator().next());
                            }
                        });
                        return result;
                    } catch (IOException exception) {
                        throw new CompletionException(exception);
                    }
                });
        });
    }

    private URI endpoint() throws IOException {
        String endpoint = DEFAULT_ENDPOINT;
        if (Files.exists(settings)) {
            try {
                JsonObject config = JsonParser.parseString(Files.readString(settings, StandardCharsets.UTF_8))
                    .getAsJsonObject();
                var value = config.get("endpoint");
                if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Expected an endpoint string");
                }
                endpoint = value.getAsString().strip();
            } catch (RuntimeException exception) {
                throw new IOException("Invalid dictionary.json: " + exception.getMessage(), exception);
            }
        }
        URI uri;
        try {
            uri = URI.create(endpoint.endsWith("/") ? endpoint : endpoint + "/");
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid dictionary endpoint", exception);
        }
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
            || uri.getHost() == null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IOException("Dictionary endpoint must be an HTTP(S) URL without a query or fragment");
        }
        return uri;
    }

    private Optional<Word> parse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() == 404) return Optional.empty();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Dictionary service returned HTTP " + response.statusCode());
        }
        if (response.body().length() > 1024 * 1024) throw new IOException("Dictionary response is too large");
        try {
            var root = JsonParser.parseString(response.body());
            if (!root.isJsonObject()) throw new IllegalArgumentException("Expected a dictionary entry");
            JsonObject object = root.getAsJsonObject();
            if (object.has("error")) {
                String error = object.get("error").getAsString();
                if ("word not found".equalsIgnoreCase(error)) return Optional.empty();
                throw new IOException("Dictionary service: " + error);
            }
            if (!object.has("word") || !object.get("word").isJsonPrimitive()
                || !object.get("word").getAsJsonPrimitive().isString()
                || !object.has("definition") || !object.get("definition").isJsonArray()) {
                throw new IllegalArgumentException("Missing word or definition array");
            }
            var definitions = object.getAsJsonArray("definition");
            for (var definition : definitions) {
                if (!definition.isJsonObject()) continue;
                var examples = definition.getAsJsonObject().get("example");
                if (examples != null && examples.isJsonArray()) {
                    definition.getAsJsonObject().add("example", orderedById(examples.getAsJsonArray()));
                }
            }
            Vocabulary vocabulary = JSON.fromJson(object, Vocabulary.class);
            boolean hasDefinition = vocabulary.rawTranslations().stream()
                .anyMatch(entry -> !entry.chineseText().isBlank() || !entry.englishText().isBlank());
            return hasDefinition ? Optional.of(vocabulary) : Optional.empty();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid dictionary response: " + exception.getMessage(), exception);
        }
    }

    /** Numeric IDs sort first; missing IDs and equal IDs retain their array order. */
    private static JsonArray orderedById(JsonArray source) {
        var ordered = new JsonArray();
        source.asList().stream()
            .sorted(Comparator.comparing(DictionaryClient::entryId, Comparator.nullsLast(Comparator.naturalOrder())))
            .forEachOrdered(ordered::add);
        return ordered;
    }

    private static BigDecimal entryId(JsonElement entry) {
        if (!entry.isJsonObject()) return null;
        var id = entry.getAsJsonObject().get("id");
        if (id == null || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isNumber()) return null;
        try {
            return id.getAsBigDecimal();
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public void close() {
        http.shutdownNow();
    }

    private record CachedWord(Word word, long savedAt) {}
}
