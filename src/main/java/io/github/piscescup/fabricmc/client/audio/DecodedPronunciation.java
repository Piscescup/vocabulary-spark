package io.github.piscescup.fabricmc.client.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.sounds.AudioStream;
import org.jspecify.annotations.NonNull;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A decoded MP3 clip stored as signed 16-bit PCM samples.
 * Each playback opens an independent stream over the same immutable sample data.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
final class DecodedPronunciation {
    private static final int MAX_PCM_BYTES = 8 * 1024 * 1024;
    private static final boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
    private final byte[] samples;
    private final AudioFormat format;

    private DecodedPronunciation(byte[] samples, AudioFormat format) {
        this.samples = samples;
        this.format = format;
    }

    /**
     * Decodes an MP3 clip, rejecting empty audio and excessive decoded data.
     *
     * @param mp3 the downloaded MP3 bytes
     * @return the decoded samples and their format
     * @throws IOException if the clip cannot be decoded, changes format or
     *         exceeds the decoded size limit
     */
    static DecodedPronunciation decode(byte[] mp3) throws IOException {
        var bitstream = new Bitstream(new ByteArrayInputStream(mp3));
        try {
            var decoder = new Decoder();
            var pcm = new ByteArrayOutputStream();
            AudioFormat format = null;
            for (var header = bitstream.readFrame(); header != null; header = bitstream.readFrame()) {
                try {
                    if (Thread.currentThread().isInterrupted()) throw new IOException("Audio decoding interrupted");
                    var buffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    if (format == null) {
                        format = new AudioFormat(buffer.getSampleFrequency(), 16,
                            buffer.getChannelCount(), true, BIG_ENDIAN);
                    } else if (format.getSampleRate() != buffer.getSampleFrequency()
                        || format.getChannels() != buffer.getChannelCount()) {
                        throw new IOException("Pronunciation changes audio format between frames");
                    }
                    int length = buffer.getBufferLength();
                    if (pcm.size() + length * Short.BYTES > MAX_PCM_BYTES) {
                        throw new IOException("Decoded pronunciation exceeds 8 MiB");
                    }
                    short[] values = buffer.getBuffer();
                    for (int index = 0; index < length; index++) {
                        int sample = values[index];
                        pcm.write(BIG_ENDIAN ? sample >>> 8 : sample);
                        pcm.write(BIG_ENDIAN ? sample : sample >>> 8);
                    }
                } finally {
                    bitstream.closeFrame();
                }
            }
            if (format == null || pcm.size() == 0) throw new IOException("No MP3 audio frames were found");
            return new DecodedPronunciation(pcm.toByteArray(), format);
        } catch (JavaLayerException | RuntimeException exception) {
            throw new IOException("Invalid MP3 pronunciation", exception);
        } finally {
            try {
                bitstream.close();
            } catch (JavaLayerException exception) {
                // The underlying source is an in-memory byte array.
            }
        }
    }

    /**
     * Returns the memory weight used by the pronunciation cache.
     *
     * @return the number of decoded bytes retained by the clip
     */
    int byteSize() {
        return samples.length;
    }

    /**
     * Opens an independent playback stream over the decoded samples.
     *
     * @return a new stream starting at the beginning of this clip
     */
    AudioStream openStream() {
        return new AudioStream() {
            private int offset;
            private boolean closed;

            @NonNull
            @Override
            public AudioFormat getFormat() {
                return format;
            }

            @NonNull
            @Override
            public ByteBuffer read(int size) throws IOException {
                if (closed) throw new IOException("Pronunciation stream is closed");
                if (size <= 0) throw new IllegalArgumentException("size must be positive");
                if (offset == samples.length) return null;
                int frameSize = format.getFrameSize();
                int length = Math.min(samples.length - offset, Math.max(frameSize, size - size % frameSize));
                var result = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
                result.put(samples, offset, length).flip();
                offset += length;
                return result;
            }

            @Override
            public void close() {
                closed = true;
            }
        };
    }
}
