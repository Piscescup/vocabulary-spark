package io.github.piscescup.vocabularyspark.client.audio;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PronunciationAudioCacheTest {
    private static final URI AUDIO = URI.create("https://audio.example.test/word.mp3");

    @Test
    void sharesPendingLoadsWithoutSharingCancellation() throws Exception {
        var calls = new AtomicInteger();
        var download = new CompletableFuture<DecodedPronunciation>();
        var clip = DecodedPronunciation.decode(DecodedPronunciationTest.silentMp3());
        try (var cache = new PronunciationAudioCache(uri -> {
            calls.incrementAndGet();
            return download;
        })) {
            var first = cache.load(AUDIO);
            var second = cache.load(AUDIO);
            first.cancel(true);
            assertFalse(download.isCancelled());
            download.complete(clip);
            assertSame(clip, second.join());
            assertSame(clip, cache.load(AUDIO).join());
            assertEquals(1, calls.get());
        }
    }

    @Test
    void failuresCanBeRetried() throws Exception {
        var calls = new AtomicInteger();
        var clip = DecodedPronunciation.decode(DecodedPronunciationTest.silentMp3());
        try (var cache = new PronunciationAudioCache(uri -> calls.incrementAndGet() == 1
            ? CompletableFuture.failedFuture(new IOException("Unavailable"))
            : CompletableFuture.completedFuture(clip))) {
            assertThrows(CompletionException.class, () -> cache.load(AUDIO).join());
            assertSame(clip, cache.load(AUDIO).join());
            assertEquals(2, calls.get());
        }
    }

    @Test
    void evictsLeastRecentlyUsedClips() throws Exception {
        var calls = new AtomicInteger();
        var clip = DecodedPronunciation.decode(DecodedPronunciationTest.silentMp3());
        try (var cache = new PronunciationAudioCache(uri -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(clip);
        })) {
            for (int index = 0; index < 32; index++) cache.load(AUDIO.resolve(index + ".mp3")).join();
            cache.load(AUDIO.resolve("0.mp3")).join();
            cache.load(AUDIO.resolve("32.mp3")).join();
            cache.load(AUDIO.resolve("0.mp3")).join();
            assertEquals(33, calls.get());
            cache.load(AUDIO.resolve("1.mp3")).join();
            assertEquals(34, calls.get());
        }
    }

    @Test
    void rejectsNonHttpUrlsBeforeLoading() {
        try (var cache = new PronunciationAudioCache(uri -> {
            fail("Invalid URLs must not reach the loader");
            return null;
        })) {
            assertThrows(CompletionException.class, () -> cache.load(URI.create("file:///audio.mp3")).join());
            assertThrows(CompletionException.class, () -> cache.load(URI.create("word.mp3")).join());
        }
    }

    @Test
    void closingCancelsPendingLoadsAndDisallowsNewWork() {
        var download = new CompletableFuture<DecodedPronunciation>();
        var cache = new PronunciationAudioCache(uri -> download);
        var load = cache.load(AUDIO);
        cache.close();
        assertTrue(download.isCancelled());
        assertTrue(load.isCompletedExceptionally());
        assertThrows(CompletionException.class, () -> cache.load(AUDIO).join());
        cache.close();
    }
}
