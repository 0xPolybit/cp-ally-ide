package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemHtmlRendererTest {
    @Test
    void renderBothProducesFullAndStatementViewsFromOneRequest(@TempDir Path temp) {
        ProblemHtmlRenderer renderer = new ProblemHtmlRenderer(temp);
        ProblemDetails details = new ProblemDetails("1A", "Test",
                "<div><div class='header'><div class='title'>Test</div></div>"
                        + "<div class='statement'><p>Statement</p></div>"
                        + "<div class='sample-tests'><div class='input'><pre>1</pre></div></div></div>");
        RenderedProblemView[] views = renderer.renderBoth(details);
        assertEquals(2, views.length);
        assertTrue(views[1].html().contains("sample-tests"));
        assertTrue(!views[0].html().contains("<div class=\"sample-tests\""));
        assertTrue(views[0].html().contains("Statement"));
    }
}
