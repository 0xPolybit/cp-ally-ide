package com.example;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tiny helper for bounded stream reads. Extracted so we can unit-test the
 * "stop after N bytes" behavior without spinning up a real network process.
 */
final class BoundedStreams {

    private static final int CHUNK = 8192;

    private BoundedStreams() {
    }

    /**
     * Reads at most {@code maxBytes} from {@code in} and returns them. Throws
     * {@link IOException} if the stream produces more than {@code maxBytes}
     * before EOF, so the caller cannot accidentally read an unbounded amount.
     */
    static byte[] read(InputStream in, int maxBytes, String source) throws IOException {
        if (maxBytes < 0) {
            throw new IllegalArgumentException("maxBytes must be >= 0");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxBytes, CHUNK));
        byte[] buffer = new byte[CHUNK];
        int total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("Stream from " + source + " exceeded " + maxBytes + " bytes");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
