package io.github.piscescup.fabricmc.vocabulary.pronunciation;

import com.google.gson.annotations.SerializedName;
import io.github.piscescup.fabricmc.utils.ComponentFormatUtils;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.NonNull;

import java.net.URL;
import java.util.Locale;
import java.util.Objects;

/**
 * A pronunciation associated with a non-blank region code.
 *
 * <p>The region and phonetic transcription are stripped before storage. The
 * region retains its source case; {@link #area()} uppercases it using
 * {@link Locale#ROOT} and appends {@code "_En"} for display. For example,
 * {@code "uk"} is displayed as {@code "UK_En"} without changing the stored code.
 *
 * <p>Display methods create new components on each call. Construction requires
 * a non-null audio URL but does not check its protocol or fetch its contents.
 * Use {@link PronunciationData#pronunciation()} to validate source HTTP(S) URLs.
 *
 * @param rawArea the non-blank source region code
 * @param rawPhoneticSymbol the source phonetic transcription; may be blank
 * @param pronunciationUrl the non-null audio resource URL
 * @see PronunciationData
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public
record AreaPronunciation(
    @SerializedName("lang")
    String rawArea,

    @SerializedName("pron")
    String rawPhoneticSymbol,

    @SerializedName("url")
    URL pronunciationUrl
)
    implements Pronunciation
{

    /**
     * Creates a regional pronunciation with stripped source text.
     *
     * @param rawArea the region code to strip and store
     * @param rawPhoneticSymbol the phonetic transcription to strip and store
     * @param pronunciationUrl the audio resource URL
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code rawArea} is blank
     */
    public AreaPronunciation {
        rawPhoneticSymbol = Objects.requireNonNull(rawPhoneticSymbol, "rawPhoneticSymbol")
            .strip();
        rawArea = Objects.requireNonNull(rawArea, "rawArea")
            .strip();
        if (rawArea.isEmpty()) {
            throw new IllegalArgumentException("rawArea must not be blank");
        }
        Objects.requireNonNull(pronunciationUrl, "pronunciationUrl");
    }

    /**
     * Formats the stored phonetic transcription as a new component.
     *
     * @return a newly created phonetic-symbol component
     */
    @NonNull
    @Override
    public MutableComponent phoneticSymbol() {
        return ComponentFormatUtils.pronunciationSymbolFormat(rawPhoneticSymbol);
    }

    /**
     * Formats the uppercase region code followed by {@code "_En"}.
     *
     * @return a newly created region component, such as {@code "US_En"}
     */
    @NonNull
    @Override
    public MutableComponent area() {
        return ComponentFormatUtils.pronunciationAreaFormat(rawArea.toUpperCase(Locale.ROOT) + "_En");
    }
}
