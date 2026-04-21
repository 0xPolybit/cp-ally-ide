package com.example;

import com.formdev.flatlaf.FlatDarkLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

public class MainWindow {

    private static final String APP_NAME = "Competitive Programming Ally";
    private static final String PROBLEM_PLACEHOLDER = "Enter problem code (eg: 2208A)";
    private static final int LEFT_FIELD_WIDTH = 280;
    private static final int LEFT_FIELD_HEIGHT = 32;
    private static final Color ACCENT = new Color(55, 247, 19);
    private static final int MIN_WINDOW_WIDTH = 1000;
    private static final int MIN_WINDOW_HEIGHT = 680;
    private static final int MIN_LEFT_PANEL_WIDTH = 280;
    private static final int MIN_RIGHT_PANEL_WIDTH = 420;

    public void showWindow() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        FlatDarkLaf.setup();
        applyGlobalDarkPalette();

        JFrame frame = new JFrame(APP_NAME);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT));
        frame.getRootPane().putClientProperty("JRootPane.menuBarEmbedded", true);
        frame.getRootPane().putClientProperty("JRootPane.titleBarBackground", new Color(43, 45, 48));
        frame.getRootPane().putClientProperty("JRootPane.titleBarForeground", new Color(230, 233, 238));
        frame.setJMenuBar(createEmbeddedTitleBar());

        frame.add(createContentSplit(), BorderLayout.CENTER);

        frame.setSize(1200, 760);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void applyGlobalDarkPalette() {
        Color surface0 = new Color(30, 31, 34);
        Color surface1 = new Color(43, 45, 48);
        Color surface2 = new Color(50, 53, 58);
        Color surface3 = new Color(60, 63, 68);
        Color border = new Color(67, 71, 76);
        Color text = new Color(223, 225, 229);

        UIManager.put("Component.accentColor", ACCENT);
        UIManager.put("Component.focusColor", ACCENT);

        UIManager.put("Panel.background", surface0);
        UIManager.put("RootPane.background", surface0);
        UIManager.put("Label.foreground", text);

        UIManager.put("ToolBar.background", surface1);
        UIManager.put("ToolBar.borderColor", border);
        UIManager.put("ToolBar.dockingBackground", surface1);
        UIManager.put("ToolBar.overflowBackground", surface1);

        UIManager.put("Button.background", surface2);
        UIManager.put("Button.foreground", text);
        UIManager.put("Button.hoverBackground", surface3);
        UIManager.put("Button.default.background", ACCENT);
        UIManager.put("Button.default.foreground", new Color(10, 11, 14));

        UIManager.put("TextField.background", surface3);
        UIManager.put("TextField.foreground", text);
        UIManager.put("TextField.caretForeground", ACCENT);
        UIManager.put("TextField.selectionBackground", new Color(55, 247, 19, 70));
        UIManager.put("TextField.selectionForeground", text);

        UIManager.put("SplitPane.background", surface0);
        UIManager.put("SplitPaneDivider.background", surface1);
        UIManager.put("SplitPaneDivider.style", "grip");
        UIManager.put("SplitPaneDivider.gripColor", new Color(122, 128, 137));
        UIManager.put("SplitPaneDivider.draggingColor", ACCENT);

        UIManager.put("ScrollBar.background", surface0);
        UIManager.put("ScrollBar.track", new Color(36, 38, 41));
        UIManager.put("ScrollBar.thumb", new Color(80, 84, 90));
        UIManager.put("ScrollBar.thumbHover", new Color(96, 101, 108));
        UIManager.put("ScrollBar.thumbPressed", new Color(110, 115, 122));
    }

    private JMenuBar createEmbeddedTitleBar() {
        JMenuBar titleBar = new JMenuBar();
        titleBar.setLayout(new BorderLayout());
        titleBar.setOpaque(true);
        titleBar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        titleBar.setBackground(new Color(43, 45, 48));

        JToolBar leftToolbar = new JToolBar();
        leftToolbar.setFloatable(false);
        leftToolbar.setOpaque(false);
        leftToolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        disableFocus(leftToolbar);
        leftToolbar.add(createToolbarButton("File"));
        leftToolbar.add(Box.createHorizontalStrut(6));
        leftToolbar.add(createToolbarButton("Edit"));
        leftToolbar.add(Box.createHorizontalStrut(6));
        leftToolbar.add(createToolbarButton("View"));

        JToolBar rightToolbar = new JToolBar();
        rightToolbar.setFloatable(false);
        rightToolbar.setOpaque(false);
        rightToolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        disableFocus(rightToolbar);
        rightToolbar.add(createToolbarButton("Settings"));
        rightToolbar.add(Box.createHorizontalStrut(6));
        rightToolbar.add(createToolbarButton("Help"));

        titleBar.add(leftToolbar, BorderLayout.WEST);
        titleBar.add(rightToolbar, BorderLayout.EAST);
        return titleBar;
    }

    private JSplitPane createContentSplit() {
        JPanel leftPanel = createProblemStatementPanel();
        JPanel rightPanel = createEditorPanel();

        leftPanel.setMinimumSize(new Dimension(MIN_LEFT_PANEL_WIDTH, 0));
        rightPanel.setMinimumSize(new Dimension(MIN_RIGHT_PANEL_WIDTH, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.35);
        splitPane.setDividerLocation(420);
        splitPane.setDividerSize(14);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        disableFocus(splitPane);

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
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 10));
        panel.setBackground(new Color(30, 31, 34));

        JPanel centerBox = new JPanel(new GridBagLayout());
        centerBox.setOpaque(false);

        JPanel form = new JPanel();
        form.setOpaque(true);
        form.setBackground(new Color(43, 45, 48));
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(67, 71, 76)),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JTextField input = createPlaceholderField(PROBLEM_PLACEHOLDER);
        input.setMaximumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        input.setMinimumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        input.setPreferredSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        input.setHorizontalAlignment(JTextField.CENTER);
        input.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton fetchButton = new JButton("Fetch from CodeForces");
        fetchButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        fetchButton.setMaximumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        fetchButton.setMinimumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        fetchButton.setPreferredSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));

        JLabel connectivityLabel = new JLabel("Checking CodeForces...");
        connectivityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectivityLabel.setForeground(new Color(160, 167, 177));
        connectivityLabel.setFont(connectivityLabel.getFont().deriveFont(Font.PLAIN, 12f));

        form.add(input);
        form.add(Box.createRigidArea(new Dimension(0, 12)));
        form.add(fetchButton);
        form.add(Box.createRigidArea(new Dimension(0, 14)));
        form.add(connectivityLabel);

        checkCodeforcesStatusAsync(connectivityLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        centerBox.add(form, gbc);

        panel.add(centerBox, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 16));
        panel.setBackground(new Color(30, 31, 34));

        JToolBar editorToolbar = new JToolBar();
        editorToolbar.setFloatable(false);
        editorToolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        editorToolbar.setOpaque(false);
        disableFocus(editorToolbar);
        JButton runButton = createToolbarButton("Run");
        runButton.setEnabled(false);
        editorToolbar.add(runButton);
        editorToolbar.add(Box.createHorizontalGlue());
        JButton languageButton = createToolbarButton("Language: Java");
        languageButton.setEnabled(false);
        editorToolbar.add(languageButton);

        RSyntaxTextArea codeEditor = new RSyntaxTextArea(24, 80);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        codeEditor.setCodeFoldingEnabled(false);
        codeEditor.setFocusable(true);
        codeEditor.setRequestFocusEnabled(true);
        applyEclipseEditorTheme(codeEditor);
        codeEditor.setText("Select a problem to get started...");
        codeEditor.setCaretPosition(0);

        RTextScrollPane scrollPane = new RTextScrollPane(codeEditor);
        scrollPane.setFoldIndicatorEnabled(true);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(67, 71, 76)));
        scrollPane.getGutter().setBackground(new Color(36, 38, 41));
        scrollPane.getGutter().setLineNumberColor(new Color(169, 176, 188));
        scrollPane.getGutter().setBorderColor(new Color(67, 71, 76));
        scrollPane.getVerticalScrollBar().setBackground(new Color(30, 31, 34));
        scrollPane.getVerticalScrollBar().setForeground(new Color(84, 89, 96));
        scrollPane.getHorizontalScrollBar().setBackground(new Color(30, 31, 34));
        scrollPane.getHorizontalScrollBar().setForeground(new Color(84, 89, 96));
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        scrollPane.setBackground(new Color(43, 45, 48));

        panel.add(editorToolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void applyEclipseEditorTheme(RSyntaxTextArea editor) {
        Color background = new Color(30, 31, 34);
        Color foreground = new Color(187, 187, 187);
        Color keyword = new Color(204, 120, 50);
        Color typeColor = new Color(152, 118, 170);
        Color comment = new Color(128, 128, 128);
        Color stringColor = new Color(106, 135, 89);
        Color numberColor = new Color(104, 151, 187);
        Color functionColor = new Color(255, 198, 109);
        Color operatorColor = new Color(169, 183, 198);

        editor.setBackground(background);
        editor.setForeground(foreground);
        editor.setCaretColor(new Color(240, 240, 240));
        editor.setCurrentLineHighlightColor(new Color(50, 54, 60));
        editor.setSelectionColor(new Color(55, 247, 19, 58));
        editor.setMatchedBracketBGColor(new Color(58, 63, 70));
        editor.setMatchedBracketBorderColor(new Color(55, 247, 19, 140));
        editor.setAnimateBracketMatching(false);
        editor.setPaintMatchedBracketPair(true);

        SyntaxScheme scheme = editor.getSyntaxScheme();
        setTokenStyle(scheme, Token.RESERVED_WORD, keyword, true, false);
        setTokenStyle(scheme, Token.RESERVED_WORD_2, typeColor, true, false);
        setTokenStyle(scheme, Token.DATA_TYPE, typeColor, false, false);
        setTokenStyle(scheme, Token.FUNCTION, functionColor, false, false);
        setTokenStyle(scheme, Token.LITERAL_STRING_DOUBLE_QUOTE, stringColor, false, false);
        setTokenStyle(scheme, Token.LITERAL_CHAR, stringColor, false, false);
        setTokenStyle(scheme, Token.LITERAL_NUMBER_DECIMAL_INT, numberColor, false, false);
        setTokenStyle(scheme, Token.LITERAL_NUMBER_FLOAT, numberColor, false, false);
        setTokenStyle(scheme, Token.LITERAL_NUMBER_HEXADECIMAL, numberColor, false, false);
        setTokenStyle(scheme, Token.COMMENT_EOL, comment, false, true);
        setTokenStyle(scheme, Token.COMMENT_MULTILINE, comment, false, true);
        setTokenStyle(scheme, Token.COMMENT_DOCUMENTATION, comment, false, true);
        setTokenStyle(scheme, Token.OPERATOR, operatorColor, false, false);
        setTokenStyle(scheme, Token.SEPARATOR, operatorColor, false, false);
        setTokenStyle(scheme, Token.IDENTIFIER, foreground, false, false);

        editor.revalidate();
        editor.repaint();
    }

    private void setTokenStyle(SyntaxScheme scheme, int token, Color color, boolean bold, boolean italic) {
        Style current = scheme.getStyle(token);
        Font baseFont = current != null && current.font != null ? current.font : new Font(Font.MONOSPACED, Font.PLAIN, 14);

        int fontStyle = Font.PLAIN;
        if (bold) {
            fontStyle |= Font.BOLD;
        }
        if (italic) {
            fontStyle |= Font.ITALIC;
        }

        Style style = new Style(color, null, baseFont.deriveFont(fontStyle));
        scheme.setStyle(token, style);
    }

    private JTextField createPlaceholderField(String placeholder) {
        JTextField field = new JTextField(placeholder);
        field.setForeground(new Color(145, 150, 159));
        field.setFocusable(true);
        field.setRequestFocusEnabled(true);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (placeholder.equals(field.getText())) {
                    field.setText("");
                    field.setForeground(new Color(223, 225, 229));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().trim().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(145, 150, 159));
                }
            }
        });
        return field;
    }

    private JButton createToolbarButton(String text) {
        JButton button = new JButton(text);
        disableFocus(button);
        return button;
    }

    private void checkCodeforcesStatusAsync(JLabel statusLabel) {
        statusLabel.setText("Checking CodeForces...");
        statusLabel.setForeground(new Color(160, 167, 177));

        SwingWorker<ConnectivityResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ConnectivityResult doInBackground() {
                return evaluateCodeforcesConnectivity();
            }

            @Override
            protected void done() {
                try {
                    ConnectivityResult result = get();
                    statusLabel.setText(result.message());
                    statusLabel.setForeground(result.color());
                } catch (Exception ignored) {
                    statusLabel.setText("CodeForces unresponsive");
                    statusLabel.setForeground(new Color(246, 86, 86));
                }
            }
        };
        worker.execute();
    }

    private ConnectivityResult evaluateCodeforcesConnectivity() {
        try {
            InetAddress address = InetAddress.getByName("codeforces.com");
            boolean pingReachable = address.isReachable(2500);
            boolean httpReachable = isCodeforcesHttpResponsive();

            if (pingReachable || httpReachable) {
                return new ConnectivityResult("CodeForces online and responsive", new Color(97, 214, 110));
            }
            return new ConnectivityResult("CodeForces unresponsive", new Color(246, 86, 86));
        } catch (UnknownHostException e) {
            return new ConnectivityResult("CodeForces offline", new Color(246, 86, 86));
        } catch (IOException e) {
            return new ConnectivityResult("CodeForces unresponsive", new Color(246, 86, 86));
        }
    }

    private boolean isCodeforcesHttpResponsive() {
        try {
            URL url = new URL("https://codeforces.com/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            int code = connection.getResponseCode();
            return code >= 200 && code < 500;
        } catch (IOException e) {
            return false;
        }
    }

    private record ConnectivityResult(String message, Color color) {
    }

    private void disableFocus(Component component) {
        component.setFocusable(false);
        if (component instanceof JComponent jComponent) {
            jComponent.setRequestFocusEnabled(false);
        }
    }
}
