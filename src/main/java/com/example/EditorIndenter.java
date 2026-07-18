package com.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless policy for Tab / Shift+Tab indentation in the editor.
 *
 * <p>{@link #decideTab} and {@link #decideShiftTab} return an
 * {@link Action} describing what to do with the current selection:</p>
 * <ul>
 *   <li>{@link Action.Kind#INSERT_TAB} — no selection, insert a single
 *       tab character (or the configured number of spaces).</li>
 *   <li>{@link Action.Kind#INDENT_LINES} — selection spans one or more
 *       lines, indent each line by one tab/indent unit.</li>
 *   <li>{@link Action.Kind#OUTDENT_LINES} — selection spans one or more
 *       lines, remove one indent unit from each line (Shift+Tab).</li>
 *   <li>{@link Action.Kind#PASS_THROUGH} — let the default editor
 *       behavior run.</li>
 * </ul>
 *
 * <p>The policy is pure: it inspects the current text and selection and
 * returns the action; the caller is responsible for actually
 * inserting/removing the characters in the document.</p>
 */
final class EditorIndenter {

    static final class Action {
        public enum Kind { INSERT_TAB, INDENT_LINES, OUTDENT_LINES, PASS_THROUGH }

        public static Action insertTab() { return new Action(Kind.INSERT_TAB, "", 0, 0, 0, ""); }
        public static Action passThrough() { return new Action(Kind.PASS_THROUGH, "", 0, 0, 0, ""); }
        /** Indent or outdent every line from {@code startLine} to {@code endLine} inclusive. */
        public static Action indentLines(int startLine, int endLine, int delta, String indent) {
            return new Action(Kind.INDENT_LINES, "", startLine, endLine, delta, indent);
        }
        public static Action outdentLines(int startLine, int endLine, int delta, String indent) {
            return new Action(Kind.OUTDENT_LINES, "", startLine, endLine, delta, indent);
        }

        public final Kind kind;
        public final int startLine;
        public final int endLine;
        public final int delta;
        public final String indent;

        Action(Kind kind, String unused, int startLine, int endLine, int delta, String indent) {
            this.kind = kind;
            this.startLine = startLine;
            this.endLine = endLine;
            this.delta = delta;
            this.indent = indent;
        }
    }

    private final int tabSpacing;
    private final boolean useTabsAsSpaces;

    EditorIndenter(int tabSpacing, boolean useTabsAsSpaces) {
        this.tabSpacing = Math.max(1, tabSpacing);
        this.useTabsAsSpaces = useTabsAsSpaces;
    }

    String singleIndent() {
        if (useTabsAsSpaces) {
            StringBuilder sb = new StringBuilder(tabSpacing);
            for (int i = 0; i < tabSpacing; i++) sb.append(' ');
            return sb.toString();
        }
        return "\t";
    }

    Action decideTab(Context ctx) {
        if (ctx.hasSelection && !ctx.selectionOnSingleLine) {
            return Action.indentLines(ctx.firstLine, ctx.lastLine, 1, singleIndent());
        }
        if (ctx.hasSelection && ctx.selectionOnSingleLine) {
            // Selecting a partial line: insert a single tab unit at the
            // start of the line.
            return Action.indentLines(ctx.firstLine, ctx.firstLine, 1, singleIndent());
        }
        return Action.insertTab();
    }

    Action decideShiftTab(Context ctx) {
        if (ctx.firstLine < 0) {
            return Action.passThrough();
        }
        return Action.outdentLines(ctx.firstLine, ctx.lastLine, 1, singleIndent());
    }

    /**
     * Compute the actual edits required to perform a line-level
     * indent/outdent. Returns one edit per affected line: the
     * original line content and the new content. Caller is responsible
     * for applying edits in reverse offset order so that earlier
     * edits do not invalidate later offsets.
     */
    List<LineEdit> computeLineEdits(List<String> lines, Action action) {
        List<LineEdit> edits = new ArrayList<>();
        if (action.kind == Action.Kind.PASS_THROUGH) {
            return edits;
        }
        if (action.startLine < 0 || action.startLine >= lines.size()) {
            return edits;
        }
        int end = Math.min(action.endLine, lines.size() - 1);
        for (int i = action.startLine; i <= end; i++) {
            String original = lines.get(i);
            String updated;
            if (action.kind == Action.Kind.INDENT_LINES) {
                updated = action.indent + original;
            } else if (action.kind == Action.Kind.OUTDENT_LINES) {
                updated = removeIndent(original, action.indent);
            } else {
                updated = original;
            }
            edits.add(new LineEdit(i, original, updated));
        }
        return edits;
    }

    /**
     * Removes up to one indent unit from the start of {@code line}.
     * If the line is empty or whitespace-only, outdent is a no-op
     * (avoids deleting tabs the user wanted to keep).
     */
    static String removeIndent(String line, String indent) {
        if (line == null || line.isEmpty()) return line;
        if (indent == null || indent.isEmpty()) return line;
        if (line.startsWith(indent)) {
            return line.substring(indent.length());
        }
        if (indent.length() == 1) {
            // Outdent unit is one tab and the line starts with a tab.
            char c = line.charAt(0);
            if (c == '\t') {
                return line.substring(1);
            }
        }
        // Outdent unit is N spaces; remove up to N leading spaces.
        int removable = 0;
        while (removable < indent.length()
                && removable < line.length()
                && line.charAt(removable) == ' ') {
            removable++;
        }
        if (removable == 0) {
            return line;
        }
        return line.substring(removable);
    }

    /** A line-level edit produced by {@link #computeLineEdits}. */
    static final class LineEdit {
        final int lineIndex;
        final String original;
        final String updated;

        LineEdit(int lineIndex, String original, String updated) {
            this.lineIndex = lineIndex;
            this.original = original;
            this.updated = updated;
        }
    }

    /** Snapshot of the editor's text + selection. */
    static final class Context {
        final boolean hasSelection;
        final int firstLine;
        final int lastLine;
        final boolean selectionOnSingleLine;

        Context(boolean hasSelection, int firstLine, int lastLine,
                boolean selectionOnSingleLine) {
            this.hasSelection = hasSelection;
            this.firstLine = firstLine;
            this.lastLine = lastLine;
            this.selectionOnSingleLine = selectionOnSingleLine;
        }
    }
}
