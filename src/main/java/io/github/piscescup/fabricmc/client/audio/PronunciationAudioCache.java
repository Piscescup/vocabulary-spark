package io.github.piscescup.fabricmc.client.audio;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Asynchronously downloads and decodes pronunciation clips into a bounded
 * in-memory cache. Concurrent requests for the same URI share one load.
 *
 * <p>Failed loads are removed so a later click can retry. Completed clips use
 * least-recently-used eviction, with at most 32 clips and 16 MiB of PCM data.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
final class PronunciationAudioCache implements AutoCloseable {
    private static final int MAX_DOWNLOAD_BYTES = 2 * 1024 * 1024;
    private static final int MAX_CACHE_BYTES = 16 * 1024 * 1024;
    private static final int MAX_ENTRIES = 32;
    private final LinkedHashMap<URI, DecodedPronunciation> cache = new LinkedHashMap<>(16, 0.75f, true);
    private final Map<URI, CompletableFuture<DecodedPronunciation>> pending = new LinkedHashMap<>();
    private final Function<URI, CompletableFuture<DecodedPronunciation>> loader;
    private final HttpClient http;
    private final ExecutorService decoder;
    private int cachedBytes;
    private boolean closed;

    /** Creates a cache using HTTP(S) downloads and a background decoder. */
    PronunciationAudioCache() {
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
        decoder = Executors.newSingleThreadExecutor(runnable -> {
            var thread = new Thread(runnable, "Vocabulary Spark pronunciation decoder");
            thread.setDaemon(true);
            return thread;
        });
        loader = this::download;
    }

    /**
     * Creates a cache with a supplied asynchronous loader.
     *
     * @param loader the download and decode operation
     */
    PronunciationAudioCache(Function<URI, CompletableFuture<DecodedPronunciation>> loader) {
        this.loader = loader;
        http = null;
        decoder = null;
    }

    /**
     * Loads a clip or reuses its pending or completed load.
     *
     * @param uri the absolute HTTP(S) audio URI
     * @return an independent future that does not cancel shared work when cancelled
     */
    synchronized CompletableFuture<DecodedPronunciation> load(URI uri) {
        if (closed) return CompletableFuture.failedFuture(new IOException("Pronunciation cache is closed"));
        if (uri == null || uri.getHost() == null
            || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
            return CompletableFuture.failedFuture(new IOException("Expected an absolute HTTP(S) audio URL"));
        }
        var cached = cache.get(uri);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        var existing = pending.get(uri);
        if (existing != null) return existing.copy();
        if (pending.size() >= MAX_ENTRIES) {
            return CompletableFuture.failedFuture(new IOException("Too many pending pronunciation downloads"));
        }
        CompletableFuture<DecodedPronunciation> future;
        try {
            future = loader.apply(uri);
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        var shared = new CompletableFuture<DecodedPronunciation>();
        pending.put(uri, shared);
        future.whenComplete((audio, failure) -> {
            synchronized (this) {
                pending.remove(uri);
                if (!closed && failure == null && audio != null) {
                    cache.put(uri, audio);
                    cachedBytes += audio.byteSize();
                    while (cache.size() > MAX_ENTRIES || cachedBytes > MAX_CACHE_BYTES) {
                        var oldest = cache.pollFirstEntry();
                        cachedBytes -= oldest.getValue().byteSize();
                    }
                }
            }
            // Publish only after updating the cache, so an immediate retry is safe.
            if (failure != null) shared.completeExceptionally(failure);
            else if (audio == null) shared.completeExceptionally(new IOException("No decoded pronunciation"));
            else shared.complete(audio);
        });
        shared.whenComplete((audio, failure) -> {
            if (shared.isCancelled()) future.cancel(true);
        });
        return shared.copy();
    }

    private CompletableFuture<DecodedPronunciation> download(URI uri) {
        var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
            .header("Accept", "audio/mpeg, application/octet-stream;q=0.5")
            .header("User-Agent", "Vocabulary-Spark/1.0")
            .GET().build();
        var response = http.sendAsync(request,
            HttpResponse.BodyHandlers.limiting(HttpResponse.BodyHandlers.ofByteArray(), MAX_DOWNLOAD_BYTES));
        var decoded = response.thenApplyAsync(result -> {
            try {
                if (result.statusCode() < 200 || result.statusCode() >= 300) {
                    throw new IOException("Pronunciation service returned HTTP " + result.statusCode());
                }
                return DecodedPronunciation.decode(result.body());
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }, decoder).orTimeout(25, TimeUnit.SECONDS);
        decoded.whenComplete((audio, failure) -> {
            if (failure != null) response.cancel(true);
        });
        return decoded;
    }

    /** Cancels pending work and releases cached audio and owned executors. */
    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        // Cancellation callbacks remove entries, so iterate over a snapshot.
        for (var future : pending.values().toArray(CompletableFuture[]::new)) future.cancel(true);
        pending.clear();
        cache.clear();
        cachedBytes = 0;
        if (http != null) http.shutdownNow();
        if (decoder != null) decoder.shutdownNow();
    }
}
