package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorKeyBindingsTest {

    private static EditorKeyBindings.Context ctx(int caret, char before, char after) {
        return new EditorKeyBindings.Context(false, caret, before, after);
    }

    private static EditorKeyBindings.Context sel(int start, int end, char before, char after) {
        return new EditorKeyBindings.Context(true, start, before, after);
    }

    @Test
    void typingParenInsertsPairAndKeepsCaretBetween() {
        EditorKeyBindings bindings = new EditorKeyBindings();
        EditorKeyBindings.Decision d = bindings.decideAutoPair('(',
                ctx(5, 'a', 'b'));
        assertEquals(EditorKeyBindings.Decision.Kind.INSERT, d.kind());
        assertEquals("()", d.literal());
        assertEquals(6, d.caret());
    }

    @Test
    void typingCurlyBracketInsertsPair() {
        EditorKeyBindings.Decision d = new EditorKeyBindings().decideAutoPair('{',
                ctx(0, '\0', '\0'));
        assertEquals("{}", d.literal());
    }

    @Test
    void typingQuoteInsertsIdenticalPair() {
        EditorKeyBindings.Decision d = new EditorKeyBindings().decideAutoPair('"',
                ctx(0, '\0', '\0'));
        assertEquals("\"\"", d.literal());
    }

    @Test
    void typingCloserWhenAlreadyPresentSkips() {
        EditorKeyBindings.Decision d = new EditorKeyBindings().decideAutoPair(')',
                ctx(7, 'a', ')'));
        assertEquals(EditorKeyBindings.Decision.Kind.SKIP, d.kind());
        assertEquals(8, d.caret());
    }

    @Test
    void typingCloserWithoutMatchPassesThrough() {
        EditorKeyBindings.Decision d = new EditorKeyBindings().decideAutoPair(')',
                ctx(7, 'a', 'b'));
        assertEquals(EditorKeyBindings.Decision.Kind.PASS_THROUGH, d.kind());
    }

    @Test
    void typingInsideSelectionWrapsSelection() {
        EditorKeyBindings.Decision d = new EditorKeyBindings().decideAutoPair('(',
                sel(3, 7, 'a', 'b'));
        assertEquals(EditorKeyBindings.Decision.Kind.WRAP, d.kind());
        // The policy records the caret position as both selStart and
        // selEnd; the editor reads the actual selection range from the
        // Swing component at apply time. The wrapCaret places the caret
        // just after the inserted opener.
        assertEquals(3, d.selStart());
        assertEquals(3, d.selEnd());
        assertEquals(4, d.wrapCaret());
    }

    @Test
    void backspaceWithPairDeletesBothChars() {
        EditorKeyBindings.Decision d = new EditorKeyBindings().decideBackspace(
                ctx(5, '(', ')'));
        assertEquals(EditorKeyBindings.Decision.Kind.DELETE_PAIR, d.kind());
        assertEquals(4, d.caret());
    }

    @Test
    void backspaceWithoutPairPassesThrough() {
        EditorKeyBindings.Decision d = new EditorKeyBindings().decideBackspace(
                ctx(5, 'a', 'b'));
        assertEquals(EditorKeyBindings.Decision.Kind.PASS_THROUGH, d.kind());
    }

    @Test
    void backspaceWithSelectionPassesThrough() {
        EditorKeyBindings.Decision d = new EditorKeyBindings().decideBackspace(
                sel(3, 7, '(', ')'));
        assertEquals(EditorKeyBindings.Decision.Kind.PASS_THROUGH, d.kind());
    }

    @Test
    void matchingCloserReturnsCorrectChar() {
        assertEquals(')', EditorKeyBindings.matchingCloser('('));
        assertEquals(']', EditorKeyBindings.matchingCloser('['));
        assertEquals('}', EditorKeyBindings.matchingCloser('{'));
        assertEquals('"', EditorKeyBindings.matchingCloser('"'));
        assertEquals('\'', EditorKeyBindings.matchingCloser('\''));
    }

    @Test
    void matchingCloserFallsBackForUnmapped() {
        // 'x' has no mapping; matchingCloser returns the char itself so
        // wrap-selection can use it as the closing half without crashing.
        assertEquals('x', EditorKeyBindings.matchingCloser('x'));
    }

    @Test
    void typingQuoteWhileClosingPresentSkips() {
        // Cursor is just before a '"' character; the user types '"'.
        // Auto-pair should advance the cursor over the existing close,
        // not insert a second one.
        EditorKeyBindings.Decision d = new EditorKeyBindings().decideAutoPair('"',
                ctx(3, 'a', '"'));
        assertEquals(EditorKeyBindings.Decision.Kind.SKIP, d.kind());
        assertEquals(4, d.caret());
    }

    @Test
    void typingSingleQuoteOpensStringWhenNoClosePresent() {
        // Today's policy: typing a quote opens a pair unless the
        // closing quote is already next to the caret. The deeper
        // "do not break an identifier" rule is a follow-up that lives
        // in a syntax-aware layer; here we just lock in the current
        // contract so future changes are intentional.
        EditorKeyBindings.Decision d = new EditorKeyBindings().decideAutoPair('\'',
                ctx(2, 'n', 'a'));
        assertEquals(EditorKeyBindings.Decision.Kind.INSERT, d.kind());
        assertEquals("''", d.literal());
    }
}
