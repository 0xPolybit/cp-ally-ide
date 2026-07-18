package com.example;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedStreamsTest {

    @Test
    void readsBytesUpToCap() throws IOException {
        byte[] data = "hello world".getBytes();
        byte[] out = BoundedStreams.read(new ByteArrayInputStream(data), 1024, "test");
        assertArrayEquals(data, out);
    }

    @Test
    void emptyStreamReturnsEmpty() throws IOException {
        byte[] out = BoundedStreams.read(new ByteArrayInputStream(new byte[0]), 1024, "test");
        assertEquals(0, out.length);
    }

    @Test
    void streamLargerThanCapThrows() {
        byte[] data = new byte[2048];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xFF);
        }
        IOException ex = assertThrows(IOException.class,
                () -> BoundedStreams.read(new ByteArrayInputStream(data), 1024, "oversize"));
        assertTrue(ex.getMessage().contains("1024"),
                "Exception should mention the cap that was exceeded");
    }

    @Test
    void exactlyAtCapSucceeds() throws IOException {
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xFF);
        }
        byte[] out = BoundedStreams.read(new ByteArrayInputStream(data), 1024, "exact");
        assertEquals(1024, out.length);
    }

    @Test
    void negativeCapRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BoundedStreams.read(new ByteArrayInputStream(new byte[0]), -1, "x"));
    }

    @Test
    void canReadFromInfiniteStreamUntilCap() {
        // A stream that would otherwise never return -1. read() must give up
        // after maxBytes and throw.
        InputStream infinite = new InputStream() {
            @Override
            public int read() {
                return 0;
            }
        };
        assertThrows(IOException.class,
                () -> BoundedStreams.read(infinite, 16, "infinite"));
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
        byte[] out = BoundedStreams.read(oneByteAtATime, 1024, "slow");
        assertEquals(100, out.length);
        for (int k = 0; k < 100; k++) {
            assertEquals(k & 0xFF, out[k] & 0xFF);
        }
    }
}
