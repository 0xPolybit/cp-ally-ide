package com.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorIndenterTest {

    @Test
    void spacesIndentWhenUseTabsAsSpaces() {
        EditorIndenter indenter = new EditorIndenter(4, true);
        assertEquals("    ", indenter.singleIndent());
    }

    @Test
    void tabIndentWhenNotUseTabsAsSpaces() {
        EditorIndenter indenter = new EditorIndenter(4, false);
        assertEquals("\t", indenter.singleIndent());
    }

    @Test
    void tabWithSelectionAcrossMultipleLinesIndents() {
        EditorIndenter indenter = new EditorIndenter(4, true);
        EditorIndenter.Action action = indenter.decideTab(
                new EditorIndenter.Context(true, 2, 5, false));
        assertEquals(EditorIndenter.Action.Kind.INDENT_LINES, action.kind);
        assertEquals(2, action.startLine);
        assertEquals(5, action.endLine);
        assertEquals("    ", action.indent);
    }

    @Test
    void tabWithoutSelectionInsertsTab() {
        EditorIndenter indenter = new EditorIndenter(4, true);
        EditorIndenter.Action action = indenter.decideTab(
                new EditorIndenter.Context(false, 0, 0, false));
        assertEquals(EditorIndenter.Action.Kind.INSERT_TAB, action.kind);
    }

    @Test
    void shiftTabOutdents() {
        EditorIndenter indenter = new EditorIndenter(4, true);
        EditorIndenter.Action action = indenter.decideShiftTab(
                new EditorIndenter.Context(false, 3, 3, false));
        assertEquals(EditorIndenter.Action.Kind.OUTDENT_LINES, action.kind);
        assertEquals(3, action.startLine);
    }

    @Test
    void computeLineEdentsAppliesIndent() {
        EditorIndenter indenter = new EditorIndenter(2, true);
        List<String> lines = List.of("a", "b", "c");
        EditorIndenter.Action action = EditorIndenter.Action.indentLines(0, 1, 1, indenter.singleIndent());
        List<EditorIndenter.LineEdit> edits = indenter.computeLineEdits(lines, action);
        assertEquals(2, edits.size());
        assertEquals("  a", edits.get(0).updated);
        assertEquals("  b", edits.get(1).updated);
    }

    @Test
    void computeLineEditsAppliesOutdent() {
        EditorIndenter indenter = new EditorIndenter(2, true);
        List<String> lines = List.of("  a", "  b", "c");
        EditorIndenter.Action action = EditorIndenter.Action.outdentLines(0, 1, 1, indenter.singleIndent());
        List<EditorIndenter.LineEdit> edits = indenter.computeLineEdits(lines, action);
        assertEquals(2, edits.size());
        assertEquals("a", edits.get(0).updated);
        assertEquals("b", edits.get(1).updated);
    }

    @Test
    void outdentIsNoOpOnEmptyOrUnindentedLine() {
        // A line with no leading whitespace should be left alone on
        // outdent, so the user never loses characters they intended.
        String result = EditorIndenter.removeIndent("hello", "    ");
        assertEquals("hello", result);
    }

    @Test
    void outdentRemovesOnlyMatchingIndent() {
        // Outdent must remove up to one full indent unit, not more.
        String result = EditorIndenter.removeIndent("        a", "    ");
        assertEquals("    a", result);
    }

    @Test
    void outdentHandlesTabIndent() {
        EditorIndenter indenter = new EditorIndenter(4, false);
        assertEquals("\t", indenter.singleIndent());
        String result = EditorIndenter.removeIndent("\thello", "\t");
        assertEquals("hello", result);
    }

    @Test
    void outdentIsSafeOnEmptyString() {
        assertEquals("", EditorIndenter.removeIndent("", "    "));
    }

    @Test
    void outdentClampedAtStart() {
        // Leading spaces fewer than the indent unit should be removed
        // entirely (we never delete past the start of the line).
        String result = EditorIndenter.removeIndent("  a", "    ");
        assertEquals("a", result);
    }

    @Test
    void passThroughActionYieldsNoEdits() {
        EditorIndenter indenter = new EditorIndenter(4, true);
        List<EditorIndenter.LineEdit> edits = indenter.computeLineEdits(
                List.of("a", "b"),
                EditorIndenter.Action.passThrough());
        assertTrue(edits.isEmpty());
    }

    @Test
    void indentRangeClippedToLineCount() {
        EditorIndenter indenter = new EditorIndenter(2, true);
        List<String> lines = List.of("a");
        // Action asks for lines 0..5 but only 1 line exists; the impl
        // must not throw.
        EditorIndenter.Action action = EditorIndenter.Action.indentLines(0, 5, 1, indenter.singleIndent());
        List<EditorIndenter.LineEdit> edits = indenter.computeLineEdits(lines, action);
        assertEquals(1, edits.size());
        assertEquals("  a", edits.get(0).updated);
    }

    @Test
    void indentOutOfRangeYieldsNoEdits() {
        EditorIndenter indenter = new EditorIndenter(2, true);
        List<String> lines = List.of("a", "b");
        // startLine is past the end; impl must not throw.
        EditorIndenter.Action action = EditorIndenter.Action.indentLines(5, 7, 1, indenter.singleIndent());
        List<EditorIndenter.LineEdit> edits = indenter.computeLineEdits(lines, action);
        assertFalse(edits.isEmpty() == false);
    }
}
