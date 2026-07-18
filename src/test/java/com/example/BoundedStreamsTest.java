package com.example;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedStreamsTest {

    @Test
    void readsBytesUpToCap() throws IOException {
        byte[] data = "hello world".getBytes();
        BoundedStreams.Result out = BoundedStreams.read(new ByteArrayInputStream(data), 1024, "test");
        assertArrayEquals(data, out.bytes());
        assertFalse(out.truncated());
    }

    @Test
    void emptyStreamReturnsEmpty() throws IOException {
        BoundedStreams.Result out = BoundedStreams.read(new ByteArrayInputStream(new byte[0]), 1024, "test");
        assertEquals(0, out.bytes().length);
        assertFalse(out.truncated());
    }

    @Test
    void streamLargerThanCapTruncates() throws IOException {
        byte[] data = new byte[2048];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xFF);
        }
        BoundedStreams.Result out = BoundedStreams.read(new ByteArrayInputStream(data), 1024, "oversize");
        assertTrue(out.truncated());
        // Only the first cap bytes should be in the result.
        assertEquals(1024, out.bytes().length);
    }

    @Test
    void exactlyAtCapSucceeds() throws IOException {
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xFF);
        }
        BoundedStreams.Result out = BoundedStreams.read(new ByteArrayInputStream(data), 1024, "exact");
        assertEquals(1024, out.bytes().length);
        assertFalse(out.truncated());
    }

    @Test
    void negativeCapRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BoundedStreams.read(new ByteArrayInputStream(new byte[0]), -1, "x"));
    }

    @Test
    void canReadFromInfiniteStreamUntilCap() throws IOException {
        // A stream that would otherwise never return -1. read() must give up
        // after maxBytes and signal truncation.
        InputStream infinite = new InputStream() {
            @Override
            public int read() {
                return 0;
            }
        };
        BoundedStreams.Result out = BoundedStreams.read(infinite, 16, "infinite");
        assertTrue(out.truncated());
    }

    @Test
    void readProgressesAcrossChunks() throws IOException {
        // Build a stream that returns one byte at a time to exercise the
        // chunked read path with reads smaller than the buffer.
        AtomicInteger i = new AtomicInteger(0);
        InputStream oneByteAtATime = new InputStream() {
            @Override
            public int read() {
                int idx = i.getAndIncrement();
                return idx < 100 ? (idx & 0xFF) : -1;
            }
        };
        BoundedStreams.Result out = BoundedStreams.read(oneByteAtATime, 1024, "slow");
        assertEquals(100, out.bytes().length);
        for (int k = 0; k < 100; k++) {
            assertEquals(k & 0xFF, out.bytes()[k] & 0xFF);
        }
    }
}
