package com.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemStatementMetadataTest {

    @Test
    void extractsTitleTimeLimitMemoryLimitAndIoMode() {
        String html = "<div class='problem-statement'>"
                + "<div class='header'><div class='title'>A. Binary Cut</div>"
                + "<div class='time-limit'>2 seconds</div>"
                + "<div class='memory-limit'>256 megabytes</div>"
                + "<div class='input-file'>standard input</div>"
                + "<div class='output-file'>standard output</div>"
                + "</div></div>";
        RenderedProblemView view = new RenderedProblemView(html, java.util.Map.of());
        ProblemStatementMetadata metadata = ProblemStatementMetadata.from(
                new ProblemDetails("2208A", "A. Binary Cut", html),
                view, List.of(), "2208A");

        assertEquals("2208A", metadata.problemCode);
        assertEquals("A. Binary Cut", metadata.title);
        assertTrue(metadata.timeLimit.startsWith("2"));
        assertTrue(metadata.memoryLimit.startsWith("256"));
        assertTrue(metadata.ioMode.toLowerCase().contains("standard input"));
    }

    @Test
    void includesSheetInformationAndDefaults() {
        String html = "<div class='problem-statement'>"
                + "<div class='header'><div class='title'>Demo</div></div></div>";
        RenderedProblemView view = new RenderedProblemView(html, java.util.Map.of());
        ProblemStatementMetadata metadata = ProblemStatementMetadata.from(
                new ProblemDetails("1A", "Demo", html),
                view,
                List.of(new SheetInfo("Sheet A", "https://example.com/sheet-a")),
                "1A");

        assertEquals(List.of("Sheet A"), metadata.sheetNames);
        assertEquals(List.of("https://example.com/sheet-a"), metadata.sheetUrls);
        assertEquals("Demo", metadata.title);
    }
}
