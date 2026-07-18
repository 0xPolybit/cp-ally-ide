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
     * Reads at most {@code maxBytes} from {@code in} and returns them in a
     * {@link Result} along with a {@code truncated} flag. If the stream is
     * still open at the cap, the cap is hit and the flag is set; the caller
     * decides what to do (kill the process, surface a verdict, etc.). This
     * is a defense-in-depth measure in case the upstream tool (curl, child
     * process) does not enforce the cap itself.
     */
    static Result read(InputStream in, int maxBytes, String source) throws IOException {
        if (maxBytes < 0) {
            throw new IllegalArgumentException("maxBytes must be >= 0");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(maxBytes, CHUNK));
        // Use a chunk size no larger than the cap so a single read() cannot
        // overshoot the limit. A read() that returns more bytes than fit in
        // the cap is consumed in two pieces: the first piece fills the cap,
        // the second piece is what triggers truncation.
        int chunk = Math.min(CHUNK, Math.max(1, maxBytes));
        byte[] buffer = new byte[chunk];
        int total = 0;
        boolean truncated = false;
        while (true) {
            int remaining = maxBytes - total;
            int wanted = Math.min(chunk, Math.max(0, remaining));
            int read;
            if (wanted == 0) {
                // We have already read the cap; one more read tells us
                // whether there was more (truncated=true) or the stream
                // ended (truncated=false).
                read = in.read();
                if (read == -1) {
                    break;
                }
                truncated = true;
                break;
            }
            read = in.read(buffer, 0, wanted);
            if (read == -1) {
                break;
            }
            out.write(buffer, 0, read);
            total += read;
            if (total == maxBytes) {
                // One more read to determine if we hit EOF or truncation.
                int extra = in.read();
                if (extra != -1) {
                    truncated = true;
                }
                break;
            }
        }
        return new Result(out.toByteArray(), truncated);
    }

    /** Read result: bytes + whether the stream was truncated at the cap. */
    record Result(byte[] bytes, boolean truncated) { }
}
