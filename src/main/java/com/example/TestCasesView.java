package com.example;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;

/** Master/detail test case view backed by {@link TestCaseModel}. */
final class TestCasesView {

    private final Frame owner;
    private final AppThemePalette palette;
    private final TestCaseModel model = new TestCaseModel();
    private final JPanel rootPanel = new JPanel(new BorderLayout());
    private final JList<TestCaseModel.Entry> caseList = new JList<>();
    private final JTextArea detailInput;
    private final JTextArea detailExpected;
    private Consumer<List<CodeExecutionService.TestCaseSpec>> executionSpecListener;

    TestCasesView(Frame owner, AppThemePalette palette) {
        this.owner = owner;
        this.palette = palette != null ? palette : AppThemePalette.dark();
        caseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        caseList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> renderEntry(value, isSelected));
        caseList.setBackground(palette.surfaceBackground());
        caseList.setForeground(palette.textColor());

        detailInput = createCodeArea();
        detailExpected = createCodeArea();

        rootPanel.setOpaque(false);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(
                UiTokens.SPACE_3, UiTokens.SPACE_3, UiTokens.SPACE_3, UiTokens.SPACE_3));

        JLabel sectionLabel = new JLabel("Test Cases");
        sectionLabel.setForeground(palette.textColor());
        sectionLabel.setFont(sectionLabel.getFont().deriveFont(Font.BOLD, UiTokens.SECTION_FONT_SIZE));

        JButton addButton = new JButton();
        javax.swing.ImageIcon addIcon = UiIconLoader.loadThemedClasspathIcon(
                "add.png", palette, UiTokens.ICON_SMALL, UiTokens.ICON_SMALL);
        if (addIcon != null) {
            addButton.setIcon(addIcon);
        } else {
            addButton.setText("+");
        }
        addButton.setToolTipText("Add Test Case (Ctrl+Shift+T)");
        addButton.getAccessibleContext().setAccessibleName("Add test case");
        addButton.setFocusable(false);
        addButton.setRequestFocusEnabled(false);
        addButton.setPreferredSize(new Dimension(UiTokens.CONTROL_HEIGHT, UiTokens.COMPACT_CONTROL_HEIGHT));
        addButton.addActionListener(event -> showAddCustomTestCaseDialog());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, UiTokens.SPACE_3, 0));
        topBar.add(sectionLabel, BorderLayout.WEST);
        topBar.add(addButton, BorderLayout.EAST);
        rootPanel.add(topBar, BorderLayout.NORTH);

        caseList.addListSelectionListener(event -> renderDetail());
        JScrollPane listScroll = new JScrollPane(caseList);
        listScroll.setBorder(BorderFactory.createLineBorder(palette.subtleBorderColor()));
        listScroll.setPreferredSize(new Dimension(220, 0));

        JPanel detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setOpaque(false);
        detailPanel.setBorder(BorderFactory.createEmptyBorder(0, UiTokens.SPACE_3, 0, 0));
        detailPanel.add(buildDetailSection("Input", detailInput));
        detailPanel.add(Box.createVerticalStrut(UiTokens.SPACE_2));
        detailPanel.add(buildDetailSection("Expected Output", detailExpected));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, detailPanel);
        splitPane.setResizeWeight(0.4);
        splitPane.setDividerSize(UiTokens.DIVIDER_SIZE);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        rootPanel.add(splitPane, BorderLayout.CENTER);
        rootPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
                "addTestCase");
        rootPanel.getActionMap().put("addTestCase", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showAddCustomTestCaseDialog();
            }
        });
        rootPanel.getAccessibleContext().setAccessibleName("Test cases");
    }

    JPanel createPanel() {
        return rootPanel;
    }

    void setSamplePayloads(Map<String, String> payloads) {
        model.setSamples(SampleTestCaseCollector.collect(payloads));
        refresh();
    }

    List<CodeExecutionService.TestCaseSpec> getExecutionTestCases() {
        return model.toExecutionSpecs();
    }

    void addCustomTestCase() {
        showAddCustomTestCaseDialog();
    }

    void onExecutionSpecChanged(Consumer<List<CodeExecutionService.TestCaseSpec>> listener) {
        this.executionSpecListener = listener;
    }

    private void showAddCustomTestCaseDialog() {
        AddCustomTestCaseDialog dialog = new AddCustomTestCaseDialog(owner, palette, model);
        dialog.setVisible(true);
        refresh();
        if (executionSpecListener != null) {
            executionSpecListener.accept(model.toExecutionSpecs());
        }
    }

    private void refresh() {
        List<TestCaseModel.Entry> entries = model.entries();
        caseList.setListData(entries.toArray(new TestCaseModel.Entry[0]));
        if (!entries.isEmpty()) {
            caseList.setSelectedIndex(0);
        } else {
            detailInput.setText("");
            detailExpected.setText("");
        }
    }

    private void renderDetail() {
        TestCaseModel.Entry entry = caseList.getSelectedValue();
        if (entry == null) {
            detailInput.setText("");
            detailExpected.setText("");
            return;
        }
        detailInput.setText(entry.input());
        detailExpected.setText(entry.expectedOutputProvided() ? entry.expectedOutput() : "Not provided");
        detailInput.setCaretPosition(0);
        detailExpected.setCaretPosition(0);
    }

    private JLabel renderEntry(TestCaseModel.Entry entry, boolean isSelected) {
        JLabel label = new JLabel(entry == null ? "" : entry.displayName());
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        if (isSelected) {
            label.setBackground(palette.surfaceRaised());
            label.setForeground(palette.textColor());
        } else {
            label.setBackground(palette.surfaceBackground());
            label.setForeground(palette.mutedTextColor());
        }
        return label;
    }

    private JPanel buildDetailSection(String title, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout(0, UiTokens.SPACE_1));
        panel.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(palette.textColor());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, UiTokens.CAPTION_FONT_SIZE));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(palette.subtleBorderColor()));
        scroll.getVerticalScrollBar().setUnitIncrement(10);
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JTextArea createCodeArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFocusable(false);
        area.setBackground(palette.surfaceBackground());
        area.setForeground(palette.textColor());
        area.setCaretColor(palette.textColor());
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, (int) UiTokens.CAPTION_FONT_SIZE));
        area.setLineWrap(true);
        area.setWrapStyleWord(false);
        area.setMargin(new java.awt.Insets(6, 6, 6, 6));
        return area;
    }
}
