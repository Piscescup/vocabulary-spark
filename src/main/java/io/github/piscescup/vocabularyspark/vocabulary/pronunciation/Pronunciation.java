package io.github.piscescup.vocabularyspark.vocabulary.pronunciation;

import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.NonNull;

import java.net.URL;

/**
 * A regional pronunciation with a display label, phonetic transcription and
 * an audio resource URL.
 *
 * <p>The URL identifies the audio resource; obtaining it does not imply that
 * the resource has been downloaded or played. Display formatting and the
 * mutability of returned components are defined by each implementation.
 *
 * @see AreaPronunciation
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface Pronunciation {
    /**
     * Returns the regional variety label formatted for display.
     *
     * @return the region display component
     */
    @NonNull
    MutableComponent area();

    /**
     * Returns the phonetic transcription formatted for display.
     *
     * @return the phonetic-symbol component
     */
    @NonNull
    MutableComponent phoneticSymbol();

    /**
     * Returns the URL of the pronunciation audio without fetching the resource.
     *
     * @return the audio resource URL
     */
    @NonNull
    URL pronunciationUrl();


}
