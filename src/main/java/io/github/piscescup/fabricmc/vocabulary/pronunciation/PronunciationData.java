package io.github.piscescup.fabricmc.vocabulary.pronunciation;

import com.google.gson.annotations.SerializedName;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

/**
 * Source pronunciation data retaining its region, transcription, audio URL
 * text and part-of-speech label.
 *
 * <p>Null strings become empty strings, and supplied strings are stripped of
 * surrounding whitespace. Construction does not parse the URL. Conversion and
 * URL validation occur only when {@link #pronunciation()} is called, allowing
 * incomplete source entries to remain available in this record.
 *
 * @param region the source region code, or {@code null}
 * @param phoneticSymbol the phonetic transcription, or {@code null}
 * @param audioUrl the source audio URL text, or {@code null}
 * @param rawPartOfSpeech the source part-of-speech label, or {@code null}
 * @see AreaPronunciation
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public record PronunciationData(
    @SerializedName("lang")
    String region,

    @SerializedName("pron")
    String phoneticSymbol,

    @SerializedName("url")
    String audioUrl,

    @SerializedName("pos")
    String rawPartOfSpeech
) {
    /**
     * Creates source data with stripped, non-null strings.
     *
     * @param region the source region code, or {@code null}
     * @param phoneticSymbol the phonetic transcription, or {@code null}
     * @param audioUrl the source audio URL text, or {@code null}
     * @param rawPartOfSpeech the source part-of-speech label, or {@code null}
     */
    public PronunciationData {
        region = region == null ? "" : region.strip();
        phoneticSymbol = phoneticSymbol == null ? "" : phoneticSymbol.strip();
        audioUrl = audioUrl == null ? "" : audioUrl.strip();
        rawPartOfSpeech = rawPartOfSpeech == null ? "" : rawPartOfSpeech.strip();
    }

    /**
     * Converts a complete pronunciation to its display model. Incomplete data
     * stays available in the record but has no display pronunciation.
     * Any non-blank region code uses the same area-based display model.
     *
     * <p>For complete entries, the audio URL must use HTTP or HTTPS and contain
     * a host. A URL beginning with {@code "//"} is interpreted using HTTPS.
     * Conversion does not fetch the resource or alter the stored URL text.
     *
     * @return a new pronunciation, or {@link Optional#empty()} if the region,
     *         phonetic transcription or audio URL text is empty
     * @throws IllegalArgumentException if a complete entry has a malformed URL,
     *         an unsupported scheme or no host
     */
    public Optional<Pronunciation> pronunciation() {
        if (region.isEmpty() || phoneticSymbol.isEmpty() || audioUrl.isEmpty()) return Optional.empty();
        URL url = parseAudioUrl();
        return Optional.of(new AreaPronunciation(region, phoneticSymbol, url));
    }

    /**
     * Parses the source URL, supplying HTTPS for a scheme-relative URL.
     *
     * @return an absolute HTTP(S) URL with a host
     * @throws IllegalArgumentException if the source text is not an accepted URL
     */
    private URL parseAudioUrl() {
        try {
            URI uri = URI.create(audioUrl.startsWith("//") ? "https:" + audioUrl : audioUrl);
            if (uri.getHost() == null || !("https".equalsIgnoreCase(uri.getScheme())
                || "http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException("Expected an absolute HTTP(S) URL");
            }
            return uri.toURL();
        } catch (IllegalArgumentException | MalformedURLException exception) {
            throw new IllegalArgumentException("Invalid pronunciation URL: " + audioUrl, exception);
        }
    }
}
