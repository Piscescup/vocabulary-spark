package io.github.piscescup.vocabularyspark.client.audio;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DecodedPronunciationTest {
    /** Three MPEG-1 Layer III stereo frames containing silence, generated without external audio. */
    static byte[] silentMp3() {
        byte[] bytes = new byte[417 * 3];
        for (int offset = 0; offset < bytes.length; offset += 417) {
            bytes[offset] = (byte) 0xFF;
            bytes[offset + 1] = (byte) 0xFB;
            bytes[offset + 2] = (byte) 0x90;
        }
        return bytes;
    }

    @Test
    void decodesMp3IntoCompletePcmFrames() throws Exception {
        var audio = DecodedPronunciation.decode(silentMp3());
        try (var stream = audio.openStream()) {
            assertEquals(44100, stream.getFormat().getSampleRate());
            assertEquals(2, stream.getFormat().getChannels());
            assertEquals(16, stream.getFormat().getSampleSizeInBits());
            assertEquals(3 * 1152 * 4, audio.byteSize());
            int bytes = 0;
            for (var block = stream.read(127); block != null; block = stream.read(127)) {
                assertTrue(block.isDirect());
                assertEquals(0, block.remaining() % stream.getFormat().getFrameSize());
                bytes += block.remaining();
            }
            assertEquals(audio.byteSize(), bytes);
            assertNull(stream.read(127));
        }
    }

    @Test
    void repeatedPlaybackHasIndependentCursorsAndBuffers() throws Exception {
        var audio = DecodedPronunciation.decode(silentMp3());
        try (var first = audio.openStream(); var second = audio.openStream()) {
            var firstBytes = first.read(16);
            byte original = firstBytes.get(0);
            firstBytes.put(0, (byte) (original ^ 0xFF));
            assertEquals(original, second.read(16).get(0));
            first.close();
            assertThrows(IOException.class, () -> first.read(16));
            assertNotNull(second.read(16));
        }
    }

    @Test
    void rejectsMissingOrNonAudioResponses() {
        assertThrows(IOException.class, () -> DecodedPronunciation.decode(new byte[0]));
        assertThrows(IOException.class, () -> DecodedPronunciation.decode("<html>Access denied</html>".getBytes()));
    }
}
