package com.example;

import java.util.HashMap;
import java.util.Map;

/**
 * Stateless policy for the editor's auto-pair / skip-over behavior.
 *
 * <p>Given a typed character, a small context (caret position, current
 * selection, the character immediately to the right of the caret),
 * {@link #decideAutoPair(char, Context)} returns either:</p>
 * <ul>
 *   <li>{@link Decision#INSERT(String, int)} — insert a literal
 *       string at the given offset; the cursor lands at the returned
 *       position.</li>
 *   <li>{@link Decision#WRAP(int, int, int)} — wrap the current
 *       selection between an opener and a closer, leaving the cursor
 *       between them.</li>
 *   <li>{@link Decision#SKIP(int)} — the user typed the closer of a
 *       pair that is already at the caret; just move the caret over
 *       it without inserting.</li>
 *   <li>{@link Decision#DELETE_PAIR(int)} — backspace should remove
 *       a just-typed pair (opener + closer) atomically.</li>
 *   <li>{@link Decision#PASS_THROUGH} — nothing special; let the
 *       default key handling do its thing.</li>
 * </ul>
 *
 * <p>The class is intentionally small and pure so it can be unit-tested
 * without an {@code RSyntaxTextArea}. The Swing glue that translates
 * the Decision into real document operations lives in
 * {@code MainWindow.installEditorAutoPairs}.</p>
 */
final class EditorKeyBindings {

    static final class Context {
        final boolean hasSelection;
        final int caretPosition;
        final char charBeforeCaret;
        final char charAfterCaret;

        Context(boolean hasSelection, int caretPosition,
                char charBeforeCaret, char charAfterCaret) {
            this.hasSelection = hasSelection;
            this.caretPosition = caretPosition;
            this.charBeforeCaret = charBeforeCaret;
            this.charAfterCaret = charAfterCaret;
        }
    }

    static final class Decision {
        public enum Kind { INSERT, WRAP, SKIP, DELETE_PAIR, PASS_THROUGH }

        /** Insert a literal at offset and place caret at the returned index. */
        static Decision insert(String literal, int caretAfter) {
            return new Decision(Kind.INSERT, literal, caretAfter, -1, -1, -1);
        }
        /** Wrap selection from selStart to selEnd; caret at wrapCaret. */
        static Decision wrap(int selStart, int selEnd, int wrapCaret) {
            return new Decision(Kind.WRAP, null, -1, selStart, selEnd, wrapCaret);
        }
        /** Move caret forward by one (skip a just-typed closer). */
        static Decision skip(int newCaret) {
            return new Decision(Kind.SKIP, null, newCaret, -1, -1, -1);
        }
        /** Backspace should delete the pair (opener, closer) at caret-1. */
        static Decision deletePair(int newCaret) {
            return new Decision(Kind.DELETE_PAIR, null, newCaret, -1, -1, -1);
        }
        /** Let the default key handling do its thing. */
        static final Decision PASS_THROUGH = new Decision(Kind.PASS_THROUGH, null, -1, -1, -1, -1);

        private final Kind kind;
        private final String literal;
        private final int caret;
        private final int selStart;
        private final int selEnd;
        private final int wrapCaret;

        private Decision(Kind kind, String literal, int caret, int selStart,
                         int selEnd, int wrapCaret) {
            this.kind = kind;
            this.literal = literal;
            this.caret = caret;
            this.selStart = selStart;
            this.selEnd = selEnd;
            this.wrapCaret = wrapCaret;
        }

        public Kind kind() { return kind; }
        public String literal() { return literal; }
        public int caret() { return caret; }
        public int selStart() { return selStart; }
        public int selEnd() { return selEnd; }
        public int wrapCaret() { return wrapCaret; }
    }

    /** Returns the matching closer for an opener char, or the char itself. */
    static char matchingCloser(char opener) {
        Character c = OPEN_TO_CLOSE.get(opener);
        return c == null ? opener : c;
    }

    private static final Map<Character, Character> OPEN_TO_CLOSE = new HashMap<>();
    private static final Map<Character, Character> CLOSE_TO_OPEN = new HashMap<>();
    static {
        OPEN_TO_CLOSE.put('(', ')');
        OPEN_TO_CLOSE.put('[', ']');
        OPEN_TO_CLOSE.put('{', '}');
        OPEN_TO_CLOSE.put('"', '"');
        OPEN_TO_CLOSE.put('\'', '\'');
        for (Map.Entry<Character, Character> e : OPEN_TO_CLOSE.entrySet()) {
            CLOSE_TO_OPEN.put(e.getValue(), e.getKey());
        }
    }

    /**
     * Decision for a typed character. Returns the {@link Decision}
     * that {@code MainWindow.installEditorAutoPairs} should execute.
     */
    Decision decideAutoPair(char typed, Context ctx) {
        if (ctx.hasSelection) {
            Character closer = OPEN_TO_CLOSE.get(typed);
            if (closer != null) {
                // Caret is placed just after the opener so the user can
                // keep typing inside the wrapped selection.
                int wrapCaret = ctx.caretPosition + 1;
                return Decision.wrap(ctx.caretPosition, ctx.caretPosition, wrapCaret);
            }
            return Decision.PASS_THROUGH;
        }

        // No selection: detect "skip the just-typed closer".
        Character openerForTyped = CLOSE_TO_OPEN.get(typed);
        if (openerForTyped != null && ctx.charAfterCaret == typed) {
            return Decision.skip(ctx.caretPosition + 1);
        }

        // No selection: open a new pair.
        Character closer = OPEN_TO_CLOSE.get(typed);
        if (closer != null) {
            // Two-character pairs for most brackets; identical pair for
            // both quote kinds. The cursor sits between opener and
            // closer so the next keystroke lands inside the pair.
            String literal = String.valueOf(typed) + closer;
            return Decision.insert(literal, ctx.caretPosition + 1);
        }

        return Decision.PASS_THROUGH;
    }

    /**
     * Decision for a backspace. If the caret sits between a freshly
     * typed pair (opener on the left, matching closer on the right),
     * the pair should be deleted atomically so the user is not left
     * with a stray closer.
     */
    Decision decideBackspace(Context ctx) {
        if (ctx.hasSelection) {
            return Decision.PASS_THROUGH;
        }
        Character expected = OPEN_TO_CLOSE.get(ctx.charBeforeCaret);
        if (expected != null && ctx.charAfterCaret == expected) {
            return Decision.deletePair(ctx.caretPosition - 1);
        }
        return Decision.PASS_THROUGH;
    }
}
