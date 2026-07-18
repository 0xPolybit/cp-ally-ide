package com.example;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.BorderFactory;
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * In-app Find/Replace bar for the RSyntaxTextArea editor.
 *
 * <p>This is a self-contained Swing panel built on top of the
 * {@code SearchContext} / {@code SearchEngine} classes that ship with
 * RSyntaxTextArea. The bar is hidden by default and shown when the
 * user triggers Find (Ctrl+F) or Replace (Ctrl+H) from the Edit
 * menu.</p>
 *
 * <p>The bar is placed below the editor toolbar without changing the
 * frozen main layout: when hidden, the container reserves no vertical
 * space.</p>
 */
final class EditorFindBar {

    private final RSyntaxTextArea editor;
    private final SearchContext searchContext;
    private final JPanel container;
    private final JTextField searchField;
    private final JTextField replaceField;
    private final JCheckBox matchCaseCheck;
    private final JCheckBox regexCheck;
    private final JCheckBox wrapCheck;
    private final JLabel statusLabel;
    private Mode currentMode = Mode.FIND;

    EditorFindBar(RSyntaxTextArea editor) {
        this.editor = editor;
        this.searchContext = new SearchContext();
        this.searchContext.setSearchForward(true);
        this.searchContext.setMatchCase(false);
        this.searchContext.setRegularExpression(false);
        this.searchContext.setSearchWrap(true);

        this.searchField = new JTextField(20);
        this.replaceField = new JTextField(20);
        this.matchCaseCheck = new JCheckBox("Match case");
        this.regexCheck = new JCheckBox("Regex");
        this.wrapCheck = new JCheckBox("Wrap", true);
        this.statusLabel = new JLabel(" ");

        container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        container.setVisible(false);
        container.setOpaque(true);
    }

    JPanel container() {
        return container;
    }

    boolean isVisible() {
        return container.isVisible();
    }

    void hide() {
        container.setVisible(false);
        container.removeAll();
        if (editor != null) {
            editor.requestFocusInWindow();
        }
    }

    void showFind() {
        show(Mode.FIND);
    }

    void showReplace() {
        show(Mode.REPLACE);
    }

    private void show(Mode mode) {
        this.currentMode = mode;
        container.removeAll();
        container.add(buildToolbar(mode), BorderLayout.CENTER);
        container.setVisible(true);
        if (editor != null && editor.getSelectedText() != null) {
            searchField.setText(editor.getSelectedText());
        }
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    private JPanel buildToolbar(Mode mode) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0;

        JLabel findLabel = new JLabel("Find:");
        gbc.gridx = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(findLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(searchField, gbc);

        JButton findNext = new JButton("Next");
        findNext.setMnemonic(KeyEvent.VK_N);
        findNext.addActionListener(this::onFindNext);
        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(findNext, gbc);
        JButton findPrev = new JButton("Prev");
        findPrev.setMnemonic(KeyEvent.VK_P);
        findPrev.addActionListener(this::onFindPrev);
        gbc.gridx = 3; panel.add(findPrev, gbc);

        JButton closeBtn = new JButton("Close");
        closeBtn.setMnemonic(KeyEvent.VK_C);
        closeBtn.addActionListener(e -> hide());
        gbc.gridx = 4; panel.add(closeBtn, gbc);

        gbc.gridx = 5; panel.add(matchCaseCheck);
        gbc.gridx = 6; panel.add(regexCheck);
        gbc.gridx = 7; panel.add(wrapCheck);

        if (mode == Mode.REPLACE) {
            gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
            panel.add(new JLabel("Replace:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(replaceField, gbc);

            JButton replaceBtn = new JButton("Replace");
            replaceBtn.addActionListener(this::onReplace);
            gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
            panel.add(replaceBtn, gbc);

            JButton replaceAllBtn = new JButton("Replace All");
            replaceAllBtn.addActionListener(this::onReplaceAll);
            gbc.gridx = 3; panel.add(replaceAllBtn, gbc);
        }

        gbc.gridy = mode == Mode.REPLACE ? 2 : 1;
        gbc.gridx = 0; gbc.gridwidth = 8; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        statusLabel.setForeground(new Color(120, 120, 120));
        panel.add(statusLabel, gbc);

        // Enter triggers Find Next; Esc closes the bar.
        searchField.addActionListener(this::onFindNext);
        searchField.getInputMap().put(
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cpa.findEscape");
        searchField.getActionMap().put("cpa.findEscape", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { hide(); }
        });

        return panel;
    }

    private void onFindNext(ActionEvent e) {
        if (editor == null) return;
        syncContextToField();
        searchContext.setSearchForward(true);
        syncContext(SearchEngine.find(editor, searchContext));
    }

    private void onFindPrev(ActionEvent e) {
        if (editor == null) return;
        syncContextToField();
        searchContext.setSearchForward(false);
        syncContext(SearchEngine.find(editor, searchContext));
    }

    private void onReplace(ActionEvent e) {
        if (editor == null) return;
        String find = searchField.getText();
        if (find == null || find.isEmpty()) {
            return;
        }
        syncContextToField();
        searchContext.setReplaceWith(replaceField.getText() == null ? "" : replaceField.getText());
        // If the current selection matches the find text, replace it and
        // advance; otherwise just find the next occurrence.
        String selected = editor.getSelectedText();
        if (selected != null && matchFind(selected, find)) {
            editor.replaceSelection(replaceField.getText() == null ? "" : replaceField.getText());
        }
        SearchResult r = SearchEngine.replace(editor, searchContext);
        syncContext(r);
    }

    private void onReplaceAll(ActionEvent e) {
        if (editor == null) return;
        syncContextToField();
        searchContext.setReplaceWith(replaceField.getText() == null ? "" : replaceField.getText());
        SearchResult r = SearchEngine.replaceAll(editor, searchContext);
        syncContext(r);
        if (r != null && r.getCount() > 0) {
            statusLabel.setText("Replaced " + r.getCount() + " occurrence(s).");
        } else {
            statusLabel.setText("No matches.");
        }
    }

    /**
     * Pushes the current field state into the {@link SearchContext} that
     * {@link SearchEngine} reads from. Called before every search/replace
     * so the user's edits in the find/replace fields are reflected in
     * the next operation.
     */
    private void syncContextToField() {
        searchContext.setSearchFor(searchField.getText() == null ? "" : searchField.getText());
        searchContext.setMatchCase(matchCaseCheck.isSelected());
        searchContext.setRegularExpression(regexCheck.isSelected());
        searchContext.setSearchWrap(wrapCheck.isSelected());
    }

    private boolean matchFind(String selected, String find) {
        if (searchContext.getMatchCase()) {
            return selected.equals(find);
        }
        return selected.equalsIgnoreCase(find);
    }

    private void syncContext(SearchResult r) {
        if (r == null) {
            statusLabel.setText("Not found.");
            return;
        }
        if (r.wasFound()) {
            statusLabel.setText(" ");
            return;
        }
        statusLabel.setText("Not found.");
        if (searchContext.getSearchWrap()) {
            // Re-anchor and try once more from the top.
            int oldCaret = editor.getCaretPosition();
            if (searchContext.getSearchForward()) {
                editor.setCaretPosition(0);
            } else {
                editor.setCaretPosition(editor.getDocument().getLength());
            }
            SearchResult r2 = SearchEngine.find(editor, searchContext);
            if (r2 != null && r2.wasFound()) {
                statusLabel.setText("Wrapped.");
            } else {
                editor.setCaretPosition(oldCaret);
            }
        }
    }

    private enum Mode { FIND, REPLACE }
}
