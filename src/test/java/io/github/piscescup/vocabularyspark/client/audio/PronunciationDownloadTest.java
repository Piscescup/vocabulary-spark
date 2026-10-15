package io.github.piscescup.vocabularyspark.client.audio;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PronunciationDownloadTest {
    @Test
    void downloadsCachesAndRetriesHttpErrors() throws Exception {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException exception) {
            // Some Windows sandboxes cannot create the selector's loopback pipe.
            // Only server setup is skipped; failures in the production downloader remain failures.
            if ("Unable to establish loopback connection".equals(exception.getMessage())) {
                Assumptions.abort("The test environment cannot create a local HTTP server: " + exception);
            }
            throw exception;
        }
        var calls = new AtomicInteger();
        byte[] mp3 = DecodedPronunciationTest.silentMp3();
        server.createContext("/word.mp3", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(200, mp3.length);
            try (var body = exchange.getResponseBody()) {
                body.write(mp3);
            }
        });
        var retry = new AtomicInteger();
        server.createContext("/retry.mp3", exchange -> {
            boolean fail = retry.incrementAndGet() == 1;
            exchange.sendResponseHeaders(fail ? 503 : 200, fail ? -1 : mp3.length);
            try (var body = exchange.getResponseBody()) {
                if (!fail) body.write(mp3);
            }
        });
        server.createContext("/large.mp3", exchange -> {
            // The advertised response already exceeds the download limit.
            exchange.sendResponseHeaders(200, 2 * 1024 * 1024 + 1);
            exchange.close();
        });
        server.start();
        try (var cache = new PronunciationAudioCache()) {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            var first = cache.load(base.resolve("/word.mp3")).get(10, TimeUnit.SECONDS);
            assertSame(first, cache.load(base.resolve("/word.mp3")).get(10, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
            assertThrows(ExecutionException.class,
                () -> cache.load(base.resolve("/retry.mp3")).get(10, TimeUnit.SECONDS));
            assertNotNull(cache.load(base.resolve("/retry.mp3")).get(10, TimeUnit.SECONDS));
            assertEquals(2, retry.get());
            assertThrows(ExecutionException.class,
                () -> cache.load(base.resolve("/large.mp3")).get(10, TimeUnit.SECONDS));
        } finally {
            server.stop(0);
        }
    }
}
