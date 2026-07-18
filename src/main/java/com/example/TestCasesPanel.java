package com.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Font;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class TestCasesPanel {

    /**
     * Listener notified whenever the test-case list changes (samples or custom).
     * The parent UI uses this to update Run button state, dirty indicators, etc.
     */
    interface Listener {
        void onTestCasesChanged(TestCasesPanel source);
    }

    /** Callback that runs a single test case by its 1-based index. */
    interface RunTestHandler {
        void runSingleTest(int testIndex);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<CodeExecutionService.TestCaseSpec>> LIST_TYPE =
            new TypeReference<>() { };

    private final Frame owner;
    private final AppThemePalette theme;
    private final Map<String, String> copyPayloads = new HashMap<>();
    private final List<CodeExecutionService.TestCaseSpec> customTestCases = new ArrayList<>();
    private final JTabbedPane testCasesTabs = new JTabbedPane();
    private final JPanel rootPanel = new JPanel(new BorderLayout());
    private final List<Listener> listeners = new ArrayList<>();
    private final CustomTestRepository customTestRepository;
    private RunTestHandler runTestHandler;
    private String currentProblemCode = "";
    private List<CodeExecutionService.TestCaseSpec> sampleTestCases = List.of();

    TestCasesPanel(Frame owner, AppThemePalette theme) {
        this(owner, theme, null);
    }

    TestCasesPanel(Frame owner, AppThemePalette theme, CustomTestRepository customTestRepository) {
        this.owner = owner;
        this.theme = theme != null ? theme : AppThemePalette.dark();
        this.customTestRepository = customTestRepository;
        testCasesTabs.setBackground(this.theme.panelBackground());
        testCasesTabs.setForeground(this.theme.textColor());

        JButton addTestCaseButton = new JButton();
        javax.swing.ImageIcon addIcon = UiIconLoader.loadThemedClasspathIcon("add.png", this.theme, 16, 16);
        if (addIcon != null) {
            addTestCaseButton.setIcon(addIcon);
        } else {
            addTestCaseButton.setText("+");
        }
        addTestCaseButton.setToolTipText("Add Test Case (Ctrl+Shift+T)");
        addTestCaseButton.setFocusable(false);
        addTestCaseButton.setRequestFocusEnabled(false);
        addTestCaseButton.setPreferredSize(new Dimension(32, 28));

        JPopupMenu addMenu = new JPopupMenu();
        JMenuItem addBlank = new JMenuItem("Add blank test…");
        addBlank.addActionListener(e -> showAddCustomTestCaseDialog());
        JMenuItem importClipboard = new JMenuItem("Import from clipboard…");
        importClipboard.addActionListener(e -> importFromClipboard());
        JMenuItem importFile = new JMenuItem("Import from file…");
        importFile.addActionListener(e -> importFromFile());
        addMenu.add(addBlank);
        addMenu.addSeparator();
        addMenu.add(importClipboard);
        addMenu.add(importFile);
        addTestCaseButton.addActionListener(e -> addMenu.show(addTestCaseButton, 0, addTestCaseButton.getHeight()));

        JLabel sectionLabel = new JLabel("Test Cases");
        sectionLabel.setForeground(this.theme.textColor());
        sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD, 15f));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        topBar.add(sectionLabel, BorderLayout.WEST);
        topBar.add(addTestCaseButton, BorderLayout.EAST);

        rootPanel.setOpaque(false);
        rootPanel.add(topBar, BorderLayout.NORTH);
        rootPanel.add(testCasesTabs, BorderLayout.CENTER);

        refreshTabs(-1);
    }

    JPanel createPanel() {
        return rootPanel;
    }

    /**
     * Replaces both the sample-derived payloads AND clears custom test cases.
     * Use this only when the problem actually changes (e.g. a new fetch), not
     * for cosmetic re-renders like zoom or theme changes — those should call
     * {@link #updateSamplePayloads(Map)} to preserve user-added tests.
     */
    void setSamplePayloads(Map<String, String> payloads) {
        setSamplePayloadsForProblem(null, payloads);
    }

    /**
     * Replaces the sample payloads and loads the persisted custom tests for
     * {@code problemCode}, if a {@link CustomTestRepository} is configured.
     * This is the entry point for problem changes; cosmetic re-renders
     * should use {@link #updateSamplePayloads(Map)} instead.
     */
    void setSamplePayloadsForProblem(String problemCode, Map<String, String> payloads) {
        copyPayloads.clear();
        if (payloads != null) {
            copyPayloads.putAll(payloads);
        }
        customTestCases.clear();
        currentProblemCode = problemCode == null ? "" : problemCode;
        if (customTestRepository != null && !currentProblemCode.isBlank()) {
            customTestCases.addAll(customTestRepository.load(currentProblemCode));
        }
        refreshTabs(-1);
        fireChanged();
    }

    /**
     * Refreshes the sample-derived copy payloads (e.g. after a problem
     * statement re-render for zoom or theme changes) without touching the
     * custom test cases. This is the safe entry point for any code path that
     * only needs to rebuild the sample test set.
     */
    void updateSamplePayloads(Map<String, String> payloads) {
        copyPayloads.clear();
        if (payloads != null) {
            copyPayloads.putAll(payloads);
        }
        // Refresh the tabs only if the sample-derived set actually changed,
        // so the user's custom test selection is preserved when possible.
        List<CodeExecutionService.TestCaseSpec> newSamples = SampleTestCaseCollector.collect(copyPayloads);
        if (!newSamples.equals(sampleTestCases)) {
            refreshTabs(-1);
            fireChanged();
        }
    }

    /**
     * Replaces the custom test-case list. Used by persistence / restore.
     * Sample tests are not affected. Triggers a change notification and
     * schedules a debounced save when a {@link CustomTestRepository} is
     * configured.
     */
    void setCustomTestCases(List<CodeExecutionService.TestCaseSpec> tests) {
        customTestCases.clear();
        if (tests != null) {
            for (CodeExecutionService.TestCaseSpec spec : tests) {
                if (spec != null && spec.custom()) {
                    customTestCases.add(spec);
                }
            }
        }
        refreshTabs(-1);
        persistCustomTests();
        fireChanged();
    }

    /** Returns an immutable snapshot of the current custom tests. */
    List<CodeExecutionService.TestCaseSpec> getCustomTestCases() {
        return List.copyOf(customTestCases);
    }

    /**
     * Returns the unified list of test cases the executor should run.
     * Custom tests follow samples; the indices match the tab indices
     * (1-based, sample index for the first n tabs, then custom indices).
     */
    List<CodeExecutionService.TestCaseSpec> getAllTestCases() {
        List<CodeExecutionService.TestCaseSpec> all = new ArrayList<>(sampleTestCases);
        all.addAll(customTestCases);
        return all;
    }

    /**
     * Returns the spec for a single test by its 1-based index in the
     * combined list returned by {@link #getAllTestCases()}. Returns null
     * if the index is out of range.
     */
    CodeExecutionService.TestCaseSpec getTestCase(int oneBasedIndex) {
        List<CodeExecutionService.TestCaseSpec> all = getAllTestCases();
        int idx = oneBasedIndex - 1;
        if (idx < 0 || idx >= all.size()) {
            return null;
        }
        return all.get(idx);
    }

    void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    void setRunTestHandler(RunTestHandler handler) {
        this.runTestHandler = handler;
    }

    private void fireChanged() {
        for (Listener listener : new ArrayList<>(listeners)) {
            try {
                listener.onTestCasesChanged(this);
            } catch (Exception ex) {
                DiagnosticLogger.error("[TestCasesPanel] Listener threw: " + ex.getMessage(), ex);
            }
        }
    }

    List<CodeExecutionService.TestCaseSpec> getExecutionTestCases() {
        List<CodeExecutionService.TestCaseSpec> testCases = new ArrayList<>(sampleTestCases);
        testCases.addAll(customTestCases);
        return testCases;
    }

    private void persistCustomTests() {
        if (customTestRepository == null || currentProblemCode.isBlank()) {
            return;
        }
        customTestRepository.save(currentProblemCode, customTestCases);
    }

    private void refreshTabs(int selectedIndex) {
        sampleTestCases = SampleTestCaseCollector.collect(copyPayloads);
        testCasesTabs.removeAll();

        if (sampleTestCases.isEmpty() && customTestCases.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBackground(theme.frameBackground());
            testCasesTabs.addTab("No Test Cases", emptyPanel);
            testCasesTabs.setSelectedIndex(0);
            return;
        }

        for (int i = 0; i < sampleTestCases.size(); i++) {
            int testIndex = i + 1;
            testCasesTabs.addTab("Test " + testIndex,
                    createTestCasePanel(sampleTestCases.get(i), testIndex, false));
        }

        for (int i = 0; i < customTestCases.size(); i++) {
            int customIndex = i;
            int testIndex = sampleTestCases.size() + i + 1;
            String title = "Custom Test Case " + testIndex;
            // Renumber the spec so results dialogs match the tab title after deletions.
            CodeExecutionService.TestCaseSpec spec = customTestCases.get(i);
            if (!title.equals(spec.displayName())) {
                spec = new CodeExecutionService.TestCaseSpec(
                        spec.input(), spec.expectedOutput(), spec.custom(), spec.expectedOutputProvided(), title);
                customTestCases.set(i, spec);
            }
            testCasesTabs.addTab(title, createTestCasePanel(spec, testIndex, true));
            testCasesTabs.setTabComponentAt(sampleTestCases.size() + i,
                    createClosableTabHeader(title, () -> {
                        if (customIndex >= 0 && customIndex < customTestCases.size()) {
                            customTestCases.remove(customIndex);
                            refreshTabs(Math.max(0, sampleTestCases.size() + customIndex - 1));
                            persistCustomTests();
                            fireChanged();
                        }
                    }));
        }

        if (selectedIndex >= 0 && selectedIndex < testCasesTabs.getTabCount()) {
            testCasesTabs.setSelectedIndex(selectedIndex);
        } else if (testCasesTabs.getTabCount() > 0) {
            testCasesTabs.setSelectedIndex(0);
        }
    }

    private JPanel createClosableTabHeader(String title, Runnable onClose) {
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setForeground(theme.textColor());

        JButton closeButton = new JButton("x");
        closeButton.setFocusable(false);
        closeButton.setRequestFocusEnabled(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        closeButton.setMargin(new java.awt.Insets(0, 4, 0, 4));
        closeButton.addActionListener(e -> onClose.run());

        header.add(label, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createTestCasePanel(CodeExecutionService.TestCaseSpec testCase,
                                       int testIndex,
                                       boolean isCustom) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(theme.frameBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(theme.frameBackground());
        splitPane.setResizeWeight(0.5);

        splitPane.setLeftComponent(createTestDataPanel("Input", testCase.input()));
        String expectedOutput = testCase.expectedOutputProvided() ? testCase.expectedOutput() : "Not provided";
        splitPane.setRightComponent(createTestDataPanel("Expected Output", expectedOutput));

        // Action row sits below the input/expected output split so the
        // user can re-run just this test, edit/duplicate it, or copy
        // its content. The row is inside the tab content; the frozen
        // outer layout is unchanged.
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        actions.setLayout(new javax.swing.BoxLayout(actions, javax.swing.BoxLayout.X_AXIS));

        JButton runButton = new JButton("Run this test");
        runButton.setFocusable(false);
        runButton.setToolTipText("Run only this test case (Ctrl+Shift+Enter on the editor)");
        runButton.addActionListener(e -> runSingleTest(testIndex));
        actions.add(runButton);

        actions.add(Box.createHorizontalGlue());

        if (isCustom) {
            JButton editButton = new JButton("Edit");
            editButton.setFocusable(false);
            editButton.addActionListener(e -> editCustomTest(testCase));
            actions.add(Box.createHorizontalStrut(6));
            actions.add(editButton);

            JButton duplicateButton = new JButton("Duplicate");
            duplicateButton.setFocusable(false);
            duplicateButton.addActionListener(e -> duplicateCustomTest(testCase));
            actions.add(Box.createHorizontalStrut(6));
            actions.add(duplicateButton);

            JButton exportButton = new JButton("Export…");
            exportButton.setFocusable(false);
            exportButton.addActionListener(e -> exportCustomTest(testCase));
            actions.add(Box.createHorizontalStrut(6));
            actions.add(exportButton);
        }

        JButton copyButton = new JButton("Copy");
        copyButton.setFocusable(false);
        copyButton.addActionListener(e -> copyToClipboard(testCase));
        actions.add(Box.createHorizontalStrut(6));
        actions.add(copyButton);

        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createTestDataPanel(String title, String data) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(theme.frameBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(theme.textColor());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        JTextArea textArea = new JTextArea(data);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setBackground(theme.surfaceBackground());
        textArea.setForeground(theme.textColor());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(false);
        textArea.setMargin(new java.awt.Insets(6, 6, 6, 6));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(theme.borderColor()));
        scrollPane.setBackground(theme.frameBackground());
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /** Triggers the per-test run handler, if one is registered. */
    private void runSingleTest(int testIndex) {
        if (runTestHandler != null) {
            try {
                runTestHandler.runSingleTest(testIndex);
            } catch (Exception ex) {
                DiagnosticLogger.error("[TestCasesPanel] Run handler threw: " + ex.getMessage(), ex);
            }
        } else {
            JOptionPane.showMessageDialog(owner,
                    "No run handler is registered.",
                    "Cannot run this test",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Opens the editor on the selected custom test and replaces it in place. */
    private void editCustomTest(CodeExecutionService.TestCaseSpec original) {
        int index = findCustomIndex(original);
        if (index < 0) {
            return;
        }
        CodeExecutionService.TestCaseSpec edited = showEditCustomTestDialog(original);
        if (edited == null) {
            return;
        }
        customTestCases.set(index, edited);
        refreshTabs(sampleTestCases.size() + index);
        persistCustomTests();
        fireChanged();
    }

    /** Duplicates the selected custom test (with a new display name). */
    private void duplicateCustomTest(CodeExecutionService.TestCaseSpec original) {
        String newName = "Custom Test Case " + (customTestCases.size() + sampleTestCases.size() + 1);
        CodeExecutionService.TestCaseSpec dup = new CodeExecutionService.TestCaseSpec(
                original.input(), original.expectedOutput(),
                true, original.expectedOutputProvided(), newName);
        customTestCases.add(dup);
        refreshTabs(sampleTestCases.size() + customTestCases.size() - 1);
        persistCustomTests();
        fireChanged();
    }

    /** Writes a single custom test to a JSON or plain-text file. */
    private void exportCustomTest(CodeExecutionService.TestCaseSpec testCase) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export test case");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON / text", "json", "txt"));
        chooser.setSelectedFile(new java.io.File("custom-test.json"));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        try {
            if (target.getFileName().toString().toLowerCase().endsWith(".json")) {
                byte[] bytes = MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValueAsBytes(List.of(testCase));
                Files.write(target, bytes);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Input:\n").append(testCase.input()).append("\n");
                if (testCase.expectedOutputProvided()) {
                    sb.append("Expected:\n").append(testCase.expectedOutput()).append("\n");
                } else {
                    sb.append("Expected: (not provided)\n");
                }
                Files.writeString(target, sb.toString(), StandardCharsets.UTF_8);
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(owner,
                    "Failed to export test:\n" + ioe.getMessage(),
                    "Export error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copyToClipboard(CodeExecutionService.TestCaseSpec testCase) {
        StringBuilder sb = new StringBuilder();
        sb.append("Input:\n").append(testCase.input()).append("\n");
        if (testCase.expectedOutputProvided()) {
            sb.append("Expected:\n").append(testCase.expectedOutput()).append("\n");
        } else {
            sb.append("Expected: (not provided)\n");
        }
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(sb.toString()), null);
    }

    /** Reads test cases from the system clipboard and appends them. */
    private void importFromClipboard() {
        try {
            java.awt.datatransfer.Transferable t = java.awt.Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getContents(null);
            if (t == null || !t.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                JOptionPane.showMessageDialog(owner,
                        "Clipboard does not contain text.",
                        "Import failed",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String text = (String) t.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
            int added = importTestsFromText(text, "clipboard");
            if (added > 0) {
                JOptionPane.showMessageDialog(owner,
                        "Imported " + added + " test case(s) from clipboard.",
                        "Import complete",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner,
                    "Failed to read clipboard:\n" + ex.getMessage(),
                    "Import failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Reads test cases from a JSON or text file and appends them. */
    private void importFromFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import test cases");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON / text", "json", "txt"));
        if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path file = chooser.getSelectedFile().toPath();
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            int added = importTestsFromText(text, file.getFileName().toString());
            if (added > 0) {
                JOptionPane.showMessageDialog(owner,
                        "Imported " + added + " test case(s) from " + file.getFileName(),
                        "Import complete",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(owner,
                    "Failed to import test cases:\n" + ioe.getMessage(),
                    "Import failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Imports test cases from arbitrary text. Two formats are supported:
     * <ol>
     *   <li>JSON array of {@link CodeExecutionService.TestCaseSpec}.</li>
     *   <li>Plain text with {@code Input:} / {@code Expected:} headers per
     *       test case, separated by blank lines.</li>
     * </ol>
     */
    int importTestsFromText(String text, String sourceLabel) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int added = 0;
        try {
            // Try JSON first; fall through to plain text on parse failure.
            List<CodeExecutionService.TestCaseSpec> parsed = MAPPER.readValue(text, LIST_TYPE);
            for (CodeExecutionService.TestCaseSpec spec : parsed) {
                if (spec == null) continue;
                added++;
                customTestCases.add(rebrand(spec));
            }
        } catch (Exception jsonEx) {
            added += importTestsFromPlainText(text);
        }
        if (added > 0) {
            refreshTabs(sampleTestCases.size() + customTestCases.size() - 1);
            persistCustomTests();
            fireChanged();
        }
        return added;
    }

    private int importTestsFromPlainText(String text) {
        // Split on blank lines; each block is "Input: ... \n\nExpected: ..."
        String[] blocks = text.split("\\R\\R+");
        int added = 0;
        for (String block : blocks) {
            if (block == null || block.isBlank()) continue;
            String[] lines = block.split("\\R", -1);
            StringBuilder input = new StringBuilder();
            StringBuilder expected = new StringBuilder();
            int section = 0; // 0 = input, 1 = expected
            for (String line : lines) {
                String lower = line.toLowerCase().trim();
                if (lower.startsWith("input")) {
                    int colon = line.indexOf(':');
                    if (colon >= 0 && colon < line.length() - 1) {
                        input.append(line.substring(colon + 1).stripLeading()).append('\n');
                    }
                    section = 0;
                    continue;
                }
                if (lower.startsWith("expected") || lower.startsWith("output")) {
                    int colon = line.indexOf(':');
                    if (colon >= 0 && colon < line.length() - 1) {
                        expected.append(line.substring(colon + 1).stripLeading()).append('\n');
                    }
                    section = 1;
                    continue;
                }
                if (section == 0) {
                    input.append(line).append('\n');
                } else {
                    expected.append(line).append('\n');
                }
            }
            if (input.length() == 0) continue;
            String inputText = stripTrailingBlankLine(input.toString());
            String expectedText = stripTrailingBlankLine(expected.toString());
            customTestCases.add(new CodeExecutionService.TestCaseSpec(
                    ensureTrailingNewline(inputText),
                    expectedText.isEmpty() ? "" : ensureTrailingNewline(expectedText),
                    true,
                    !expectedText.isEmpty(),
                    nextCustomName()));
            added++;
        }
        return added;
    }

    private String nextCustomName() {
        return "Custom Test Case " + (customTestCases.size() + sampleTestCases.size() + 1);
    }

    private CodeExecutionService.TestCaseSpec rebrand(CodeExecutionService.TestCaseSpec spec) {
        return new CodeExecutionService.TestCaseSpec(
                spec.input(), spec.expectedOutput(),
                spec.custom(), spec.expectedOutputProvided(),
                nextCustomName());
    }

    private int findCustomIndex(CodeExecutionService.TestCaseSpec spec) {
        for (int i = 0; i < customTestCases.size(); i++) {
            if (customTestCases.get(i) == spec) {
                return i;
            }
        }
        return -1;
    }

    private static String stripTrailingBlankLine(String s) {
        if (s == null) return "";
        if (s.endsWith("\n\n")) return s.substring(0, s.length() - 1);
        return s;
    }

    private void showAddCustomTestCaseDialog() {
        CodeExecutionService.TestCaseSpec draft = showCustomTestDialog("Add Test Case", null);
        if (draft == null) {
            return;
        }
        customTestCases.add(draft);
        refreshTabs(sampleTestCases.size() + customTestCases.size() - 1);
        persistCustomTests();
        fireChanged();
    }

    private CodeExecutionService.TestCaseSpec showEditCustomTestDialog(CodeExecutionService.TestCaseSpec existing) {
        return showCustomTestDialog("Edit Test Case", existing);
    }

    /**
     * Shared Add/Edit dialog. Returns the constructed spec on accept,
     * or null on cancel.
     */
    private CodeExecutionService.TestCaseSpec showCustomTestDialog(String title,
                                                                  CodeExecutionService.TestCaseSpec existing) {
        JDialog dialog = new JDialog(owner, title, true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(theme.frameBackground());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(theme.frameBackground());
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel inputLabel = new JLabel("Input (required)");
        inputLabel.setForeground(theme.textColor());
        content.add(inputLabel);
        content.add(Box.createVerticalStrut(6));

        JTextArea inputArea = new JTextArea(8, 40);
        inputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        inputArea.setBackground(theme.surfaceBackground());
        inputArea.setForeground(theme.textColor());
        inputArea.setCaretColor(theme.textColor());
        if (existing != null) {
            inputArea.setText(existing.input());
        }
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createLineBorder(theme.borderColor()));
        inputScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(inputScroll);
        content.add(Box.createVerticalStrut(12));

        JLabel outputLabel = new JLabel("Expected Output (optional)");
        outputLabel.setForeground(theme.textColor());
        content.add(outputLabel);
        content.add(Box.createVerticalStrut(6));

        JTextArea outputArea = new JTextArea(8, 40);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setBackground(theme.surfaceBackground());
        outputArea.setForeground(theme.textColor());
        outputArea.setCaretColor(theme.textColor());
        if (existing != null && existing.expectedOutputProvided()) {
            outputArea.setText(existing.expectedOutput());
        }
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createLineBorder(theme.borderColor()));
        outputScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(outputScroll);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFocusable(false);
        cancelButton.addActionListener(e -> {
            // The dialog must return null; we use a 1-element holder to
            // communicate that back through the lambda.
            dialogResult[0] = null;
            dialog.dispose();
        });
        JButton saveButton = new JButton(existing == null ? "Add" : "Save");
        saveButton.setFocusable(false);
        saveButton.addActionListener(e -> {
            String input = inputArea.getText();
            if (input == null || input.isBlank()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Input is required.",
                        "Missing Input",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String expectedOutput = outputArea.getText();
            boolean hasExpectedOutput = expectedOutput != null && !expectedOutput.isBlank();
            String normalizedInput = ensureTrailingNewline(input);
            String normalizedExpectedOutput = hasExpectedOutput ? ensureTrailingNewline(expectedOutput) : "";
            String name = (existing != null) ? existing.displayName() : null;
            if (name == null || name.isBlank()) {
                name = "Custom Test Case " + (customTestCases.size() + sampleTestCases.size() + 1);
            }
            // Add a tiny UUID-based discriminator to the display name so
            // the existing-tab title update in refreshTabs() doesn't collapse
            // multiple identical names into a single tab.
            dialogResult[0] = new CodeExecutionService.TestCaseSpec(
                    normalizedInput,
                    normalizedExpectedOutput,
                    true,
                    hasExpectedOutput,
                    existing == null ? (name + " " + UUID.randomUUID().toString().substring(0, 4)) : name);
            dialog.dispose();
        });

        JPanel buttonRow = new JPanel();
        buttonRow.setOpaque(false);
        buttonRow.add(cancelButton);
        buttonRow.add(saveButton);
        actions.add(buttonRow, BorderLayout.EAST);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(640, 560);
        dialog.setLocationRelativeTo(owner);

        // 1-element array is used to communicate the result out of the
        // modal dialog's lambdas; the dialog itself just blocks on
        // setVisible(true).
        final CodeExecutionService.TestCaseSpec[] dialogResult = new CodeExecutionService.TestCaseSpec[1];
        this.dialogResult = dialogResult;
        dialog.setVisible(true);
        return dialogResult[0];
    }

    private CodeExecutionService.TestCaseSpec[] dialogResult;

    void addCustomTestCase() {
        showAddCustomTestCaseDialog();
    }

    private String ensureTrailingNewline(String value) {
        if (value == null || value.isEmpty()) {
            return "\n";
        }
        return value.endsWith("\n") ? value : value + "\n";
    }
}
