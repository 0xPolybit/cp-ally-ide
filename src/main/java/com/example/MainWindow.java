package com.example;

import com.formdev.flatlaf.FlatDarkLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;

public class MainWindow {

    private static final Color ACCENT = new Color(55, 247, 19);
    private static final int MIN_WINDOW_WIDTH = 1000;
    private static final int MIN_WINDOW_HEIGHT = 680;
    private static final int MIN_LEFT_PANEL_WIDTH = 280;
    private static final int MIN_RIGHT_PANEL_WIDTH = 420;

    public void showWindow() {
        FlatDarkLaf.setup();
        UIManager.put("Component.accentColor", ACCENT);
        UIManager.put("Button.default.background", ACCENT);
        UIManager.put("Button.default.foreground", new Color(15, 15, 15));

        JFrame frame = new JFrame("CodeForces Helper IDE");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT));

        frame.add(createMainToolbar(), BorderLayout.NORTH);
        frame.add(createContentSplit(), BorderLayout.CENTER);

        frame.setSize(1200, 760);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JToolBar createMainToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        toolbar.add(createToolbarButton("File"));
        toolbar.add(createToolbarButton("Edit"));
        toolbar.add(createToolbarButton("View"));
        toolbar.add(createToolbarButton("Help"));

        return toolbar;
    }

    private JSplitPane createContentSplit() {
        JPanel leftPanel = createProblemStatementPanel();
        JPanel rightPanel = createEditorPanel();

        leftPanel.setMinimumSize(new Dimension(MIN_LEFT_PANEL_WIDTH, 0));
        rightPanel.setMinimumSize(new Dimension(MIN_RIGHT_PANEL_WIDTH, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.35);
        splitPane.setDividerLocation(420);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Keep left panel within [MIN_LEFT_PANEL_WIDTH, half of available width].
        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> clampDivider(splitPane));
        splitPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                clampDivider(splitPane);
            }
        });

        return splitPane;
    }

    private void clampDivider(JSplitPane splitPane) {
        int width = splitPane.getWidth();
        if (width <= 0) {
            return;
        }

        int maxLeft = Math.max(MIN_LEFT_PANEL_WIDTH, width / 2);
        int minLeft = MIN_LEFT_PANEL_WIDTH;
        int current = splitPane.getDividerLocation();
        int clamped = Math.max(minLeft, Math.min(current, maxLeft));

        // Also respect right panel minimum width where possible.
        int rightLimited = width - MIN_RIGHT_PANEL_WIDTH;
        if (rightLimited > minLeft) {
            clamped = Math.min(clamped, rightLimited);
        }

        if (clamped != current) {
            splitPane.setDividerLocation(clamped);
        }
    }

    private JPanel createProblemStatementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 8));

        JLabel title = new JLabel("Problem Statement", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(title, BorderLayout.NORTH);

        JPanel centerBox = new JPanel();
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));

        JTextField input = new JTextField();
        input.setMaximumSize(new Dimension(320, 36));
        input.setPreferredSize(new Dimension(320, 36));
        input.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton fetchButton = new JButton("Fetch from CodeForces");
        fetchButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerBox.add(Box.createVerticalGlue());
        centerBox.add(input);
        centerBox.add(Box.createRigidArea(new Dimension(0, 12)));
        centerBox.add(fetchButton);
        centerBox.add(Box.createVerticalGlue());

        panel.add(centerBox, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 16));

        JToolBar editorToolbar = new JToolBar();
        editorToolbar.setFloatable(false);
        editorToolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        editorToolbar.add(createToolbarButton("Run"));

        RSyntaxTextArea codeEditor = new RSyntaxTextArea(24, 80);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeEditor.setCodeFoldingEnabled(true);
        applyEditorDarkTheme(codeEditor);
        codeEditor.setText("public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, CodeForces!\");\n" +
                "    }\n" +
                "}\n");

        RTextScrollPane scrollPane = new RTextScrollPane(codeEditor);
        scrollPane.setFoldIndicatorEnabled(true);

        panel.add(editorToolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void applyEditorDarkTheme(RSyntaxTextArea editor) {
        try (InputStream stream = getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml")) {
            if (stream != null) {
                Theme.load(stream).apply(editor);
                return;
            }
        } catch (IOException ignored) {
            // Fallback colors are applied below if theme load fails.
        }

        editor.setBackground(new Color(24, 24, 24));
        editor.setCurrentLineHighlightColor(new Color(45, 45, 45));
        editor.setCaretColor(new Color(230, 230, 230));
        editor.setForeground(new Color(230, 230, 230));
        editor.setSelectionColor(new Color(55, 247, 19, 90));
    }

    private JButton createToolbarButton(String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        return button;
    }
}
