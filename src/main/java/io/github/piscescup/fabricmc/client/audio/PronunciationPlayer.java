package io.github.piscescup.fabricmc.client.audio;

import io.github.piscescup.fabricmc.References;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.NonNull;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Plays dictionary pronunciations through Minecraft's UI sound category.
 *
 * <p>Downloads and MP3 decoding run in the background. A new request stops
 * the previous clip; outdated completions cannot begin playback. All public
 * methods must be called on the Minecraft client thread.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class PronunciationPlayer implements AutoCloseable {
    /** The current state of a pronunciation request. */
    public enum State {
        /** No request is active for this pronunciation. */
        IDLE,
        /** The clip is being downloaded or decoded. */
        LOADING,
        /** The sound engine has started the clip. */
        PLAYING,
        /** Loading or starting playback failed; another click may retry. */
        FAILED,
        /** The sound engine accepted the clip silently because of sound settings. */
        MUTED
    }

    private final Minecraft client;
    private final PronunciationAudioCache audio;
    private URI requested;
    private SoundInstance current;
    private State state = State.IDLE;
    private long sequence;
    private boolean closed;

    /**
     * Creates a client-owned player and its session audio cache.
     *
     * @param client the client whose sound manager and executor will be used
     * @throws NullPointerException if {@code client} is null
     */
    public PronunciationPlayer(Minecraft client) {
        this.client = Objects.requireNonNull(client, "client");
        audio = new PronunciationAudioCache();
    }

    /**
     * Starts loading and playing the requested clip, stopping any previous clip.
     * Completed downloads are reused for subsequent requests in this session.
     *
     * @param uri the absolute HTTP(S) pronunciation URL
     * @throws NullPointerException if {@code uri} is null
     */
    public void play(URI uri) {
        Objects.requireNonNull(uri, "uri");
        if (closed) return;
        stop();
        requested = uri;
        state = State.LOADING;
        long request = sequence;
        audio.load(uri).whenCompleteAsync((clip, failure) -> {
            if (closed || sequence != request) return;
            if (failure != null) {
                state = State.FAILED;
                References.MOD_LOGGER.warn("Unable to load pronunciation {}", uri, failure);
                return;
            }
            try {
                current = new PronunciationSound(clip);
                var result = client.getSoundManager().play(current);
                state = switch (result) {
                    case STARTED -> State.PLAYING;
                    case STARTED_SILENTLY -> State.MUTED;
                    case NOT_STARTED -> State.FAILED;
                };
                if (result == SoundEngine.PlayResult.NOT_STARTED) {
                    References.MOD_LOGGER.warn("The sound engine could not start pronunciation {}", uri);
                }
            } catch (RuntimeException exception) {
                if (current != null) client.getSoundManager().stop(current);
                current = null;
                state = State.FAILED;
                References.MOD_LOGGER.warn("Unable to play pronunciation {}", uri, exception);
            }
        }, client::schedule);
    }

    /**
     * Returns the playback state for one audio URL.
     *
     * @param uri the pronunciation URL to inspect
     * @return the active request state, or {@link State#IDLE} for another URL
     */
    public State state(URI uri) {
        return Objects.equals(requested, uri) ? state : State.IDLE;
    }

    /** Updates the playing state when the sound engine finishes a clip. */
    public void tick() {
        if (state == State.PLAYING && current != null && !client.getSoundManager().isActive(current)) {
            current = null;
            state = State.IDLE;
        }
    }

    /** Stops the current clip and prevents any older request from starting it again. */
    public void stop() {
        sequence++;
        if (current != null) client.getSoundManager().stop(current);
        current = null;
        requested = null;
        state = State.IDLE;
    }

    /** Stops playback and releases downloads, decoder threads and cached clips. */
    @Override
    public void close() {
        if (closed) return;
        stop();
        closed = true;
        audio.close();
    }

    /** Supplies a fresh PCM stream through Fabric's existing sound extension. */
    private static final class PronunciationSound extends AbstractSoundInstance implements FabricSoundInstance {
        private final DecodedPronunciation audio;

        private PronunciationSound(DecodedPronunciation audio) {
            super(References.fromPath("pronunciation"), SoundSource.UI, SoundInstance.createUnseededRandom());
            this.audio = audio;
            volume = 1.0f;
            pitch = 1.0f;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
        }

        @NonNull
        @Override
        public CompletableFuture<AudioStream> getAudioStream(
            @NonNull SoundBufferLibrary library, @NonNull Identifier id, boolean repeatInstantly
        ) {
            return CompletableFuture.completedFuture(audio.openStream());
        }
    }
}
