package com.example;

import com.formdev.flatlaf.FlatDarkLaf;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public class MainWindow {

    private static final String APP_NAME = "Competitive Programming Ally";
    private static final String PROBLEM_PLACEHOLDER = "Enter problem code (eg: 2208A)";
    private static final Pattern PROBLEM_CODE_PATTERN = Pattern.compile("^(\\d{1,6})([A-Za-z][A-Za-z0-9]{0,2})$");
    private static final String DEFAULT_LANGUAGE = "Python 3";
    private static final String SETTINGS_DIR_NAME = "CompetitiveProgrammingAlly";
    private static final String SETTINGS_FILE_NAME = "settings.properties";
    private static final int LEFT_FIELD_WIDTH = 280;
    private static final int LEFT_FIELD_HEIGHT = 32;
    private static final Color ACCENT = new Color(55, 247, 19);
    private static final int MIN_WINDOW_WIDTH = 1000;
    private static final int MIN_WINDOW_HEIGHT = 680;
    private static final int MIN_LEFT_PANEL_WIDTH = 280;
    private static final int MIN_RIGHT_PANEL_WIDTH = 420;
    private JButton initialFocusButton;
    private JComboBox<String> languageDropdown;
    private AppSettings appSettings;
    private JTextField problemCodeInput;
    private JButton fetchProblemButton;
    private JLabel fetchStatusLabel;
    private JPanel leftPanelContainer;
    private JPanel problemEntryPanel;
    private final Map<String, String> copyPayloads = new HashMap<>();
    private final Map<String, String> iconSourceCache = new HashMap<>();
    private JSplitPane contentSplitPane;

    public void showWindow() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        FlatDarkLaf.setup();
        applyGlobalDarkPalette();
        appSettings = loadSettings();

        JFrame frame = new JFrame(APP_NAME);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT));
        frame.getRootPane().putClientProperty("JRootPane.menuBarEmbedded", true);
        frame.getRootPane().putClientProperty("JRootPane.titleBarBackground", new Color(43, 45, 48));
        frame.getRootPane().putClientProperty("JRootPane.titleBarForeground", new Color(230, 233, 238));
        frame.setJMenuBar(createEmbeddedTitleBar());

        frame.add(createContentSplit(), BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                persistSettings(frame);
            }
        });

        applyWindowSettings(frame, appSettings);
        frame.setVisible(true);

        if (contentSplitPane != null && appSettings.dividerLocation() > 0) {
            SwingUtilities.invokeLater(() -> contentSplitPane.setDividerLocation(appSettings.dividerLocation()));
        }

        if (appSettings.maximized()) {
            SwingUtilities.invokeLater(() -> frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH));
        }

        if (initialFocusButton != null) {
            SwingUtilities.invokeLater(() -> initialFocusButton.requestFocusInWindow());
        }
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
        splitPane.setDividerLocation(appSettings != null ? appSettings.dividerLocation() : 420);
        splitPane.setDividerSize(14);
        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        disableFocus(splitPane);
        contentSplitPane = splitPane;

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
        leftPanelContainer = new JPanel(new BorderLayout());
        leftPanelContainer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 10));
        leftPanelContainer.setBackground(new Color(30, 31, 34));

        problemEntryPanel = createProblemEntryPanel();
        leftPanelContainer.add(problemEntryPanel, BorderLayout.CENTER);
        return leftPanelContainer;
    }

    private JPanel createProblemEntryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel form = new JPanel();
        form.setOpaque(true);
        form.setBackground(new Color(43, 45, 48));
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(67, 71, 76)),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        problemCodeInput = createPlaceholderField(PROBLEM_PLACEHOLDER);
        problemCodeInput.setMaximumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        problemCodeInput.setMinimumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        problemCodeInput.setPreferredSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        problemCodeInput.setHorizontalAlignment(JTextField.CENTER);
        problemCodeInput.setAlignmentX(Component.CENTER_ALIGNMENT);

        fetchProblemButton = new JButton("Fetch from CodeForces");
        fetchProblemButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        fetchProblemButton.setMaximumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        fetchProblemButton.setMinimumSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        fetchProblemButton.setPreferredSize(new Dimension(LEFT_FIELD_WIDTH, LEFT_FIELD_HEIGHT));
        fetchProblemButton.addActionListener(e -> onFetchProblemClicked());
        initialFocusButton = fetchProblemButton;

        JLabel connectivityLabel = new JLabel("Checking CodeForces...");
        connectivityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectivityLabel.setForeground(new Color(160, 167, 177));
        connectivityLabel.setFont(connectivityLabel.getFont().deriveFont(Font.PLAIN, 12f));

        fetchStatusLabel = new JLabel(" ");
        fetchStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        fetchStatusLabel.setForeground(new Color(246, 86, 86));
        fetchStatusLabel.setFont(fetchStatusLabel.getFont().deriveFont(Font.PLAIN, 12f));

        form.add(problemCodeInput);
        form.add(Box.createRigidArea(new Dimension(0, 12)));
        form.add(fetchProblemButton);
        form.add(Box.createRigidArea(new Dimension(0, 14)));
        form.add(connectivityLabel);
        form.add(Box.createRigidArea(new Dimension(0, 8)));
        form.add(fetchStatusLabel);

        checkCodeforcesStatusAsync(connectivityLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(form, gbc);
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
        languageDropdown = new JComboBox<>(new String[]{
            "Python 3",
            "GNU G++17 7.3.0",
            "GNU G++20 13.2",
            "GNU C11 5.1.0",
            "GNU G11 5.1.0",
            "Java 21",
            "Kotlin 1.9",
            "C# 8",
            "Go 1.22",
            "Rust 2021",
            "Node.js 20",
            "PHP 8.2",
            "Ruby 3.2",
            "Perl 5",
            "Haskell GHC 8.10",
            "OCaml 4.02",
            "Scala 2.12",
            "Pascal 3.0",
            "JavaScript V8",
            "PyPy 3"
        });
        String preferredLanguage = appSettings != null ? appSettings.lastLanguage() : DEFAULT_LANGUAGE;
        languageDropdown.setSelectedItem(preferredLanguage);
        if (languageDropdown.getSelectedItem() == null) {
            languageDropdown.setSelectedItem(DEFAULT_LANGUAGE);
        }
        languageDropdown.setPreferredSize(new Dimension(190, LEFT_FIELD_HEIGHT));
        languageDropdown.setMaximumSize(new Dimension(220, LEFT_FIELD_HEIGHT));
        languageDropdown.setFocusable(false);
        languageDropdown.setRequestFocusEnabled(false);
        editorToolbar.add(languageDropdown);

        RSyntaxTextArea codeEditor = new RSyntaxTextArea(24, 80);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        codeEditor.setCodeFoldingEnabled(false);
        codeEditor.setEditable(false);
        codeEditor.setFocusable(false);
        codeEditor.setRequestFocusEnabled(false);
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

    private void onFetchProblemClicked() {
        String rawCode = problemCodeInput.getText() != null ? problemCodeInput.getText().trim() : "";
        if (rawCode.isEmpty() || PROBLEM_PLACEHOLDER.equals(rawCode)) {
            showFetchWarning("Enter a problem code, for example 2208A.");
            return;
        }

        Matcher matcher = PROBLEM_CODE_PATTERN.matcher(rawCode);
        if (!matcher.matches()) {
            showFetchWarning("Invalid problem code. Use format like 2208A.");
            return;
        }

        String contestId = matcher.group(1);
        String index = matcher.group(2).toUpperCase();

        showLeftPanelLoading(contestId + index);

        SwingWorker<ProblemDetails, Void> worker = new SwingWorker<>() {
            @Override
            protected ProblemDetails doInBackground() throws Exception {
                return fetchProblemDetails(contestId, index);
            }

            @Override
            protected void done() {
                try {
                    ProblemDetails details = get();
                    showCodeforcesProblemView(details);
                } catch (Exception ex) {
                    restoreProblemEntryPanelWithError("Could not fetch that problem.");
                    JOptionPane.showMessageDialog(null,
                            "Could not fetch the specified CodeForces problem. Please verify the code and try again.",
                            APP_NAME,
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showFetchWarning(String message) {
        fetchStatusLabel.setForeground(new Color(246, 86, 86));
        fetchStatusLabel.setText(message);
        JOptionPane.showMessageDialog(null, message, APP_NAME, JOptionPane.WARNING_MESSAGE);
    }

    private JLabel createLoadingLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(160, 167, 177));
        label.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 6));
        return label;
    }

    private ProblemDetails fetchProblemDetails(String contestId, String index) throws IOException {
        String url = "https://codeforces.com/problemset/problem/" + contestId + "/" + index;
        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get();

        Element statementRoot = document.selectFirst("div.problem-statement");
        if (statementRoot == null) {
            throw new IOException("Missing problem statement");
        }

        Element titleElement = statementRoot.selectFirst("div.header div.title");
        String title = titleElement != null ? titleElement.text() : (contestId + index);

        // Remove scripts/styles and keep statement markup so rendering matches Codeforces structure.
        Element statementClone = statementRoot.clone();
        statementClone.select("script, style").remove();

        return new ProblemDetails(contestId + index, title, statementClone.outerHtml());
    }

    private void showLeftPanelLoading(String problemCode) {
        fetchProblemButton.setEnabled(false);
        fetchStatusLabel.setForeground(new Color(160, 167, 177));
        fetchStatusLabel.setText("Fetching problem " + problemCode + "...");

        JPanel loadingPanel = new JPanel(new GridBagLayout());
        loadingPanel.setOpaque(false);
        loadingPanel.add(createLoadingLabel("Loading statement and sample tests..."));

        leftPanelContainer.removeAll();
        leftPanelContainer.add(loadingPanel, BorderLayout.CENTER);
        leftPanelContainer.revalidate();
        leftPanelContainer.repaint();
    }

    private void restoreProblemEntryPanelWithError(String message) {
        fetchProblemButton.setEnabled(true);
        fetchStatusLabel.setForeground(new Color(246, 86, 86));
        fetchStatusLabel.setText(message);

        leftPanelContainer.removeAll();
        leftPanelContainer.add(problemEntryPanel, BorderLayout.CENTER);
        leftPanelContainer.revalidate();
        leftPanelContainer.repaint();
    }

    private void showCodeforcesProblemView(ProblemDetails details) {
        String html = buildCodeforcesLikeHtml(details);

        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setText(html);
        pane.setCaretPosition(0);
        pane.setBackground(new Color(30, 31, 34));
        pane.addHyperlinkListener(event -> {
            if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED || event.getDescription() == null) {
                return;
            }
            if (event.getDescription().startsWith("copy:")) {
                String key = event.getDescription().substring("copy:".length());
                String payload = copyPayloads.get(key);
                if (payload != null) {
                    copyToClipboard(payload);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(pane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(30, 31, 34));
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);

        leftPanelContainer.removeAll();
        leftPanelContainer.add(scrollPane, BorderLayout.CENTER);
        leftPanelContainer.revalidate();
        leftPanelContainer.repaint();
    }

    private String buildCodeforcesLikeHtml(ProblemDetails details) {
        String problemHtml = prepareProblemHtml(details.problemHtml());
        String css = """
                <style>
                body { background:#1e1f22; color:#dfe1e5; font-family:Segoe UI, Arial, sans-serif; margin:12px; }
                .problem-statement { background:#2b2d30; border-radius:8px; padding:14px; }
                .header .title { font-size:18px; font-weight:700; color:#eceff4; margin-bottom:10px; }
                .metrics-row { margin-top:4px; margin-bottom:12px; display:flex; flex-wrap:wrap; gap:20px; }
                .metric-item { display:inline-flex; align-items:center; color:#b8bec8; white-space:nowrap; }
                .metric-icon { width:14px; height:14px; vertical-align:middle; margin-right:6px; }
                .section-title { font-weight:700; margin-top:12px; margin-bottom:6px; color:#e8ebf0; }
                .sample-tests .input, .sample-tests .output { margin-top:8px; }
                .io-header { display:flex; align-items:center; justify-content:space-between; min-height:18px; margin-bottom:4px; }
                .io-label { display:inline-flex; align-items:center; line-height:18px; }
                .copy-btn { display:inline-flex; align-items:center; justify-content:center; height:18px; width:18px; text-decoration:none; border:none; outline:none; background:transparent; }
                .copy-btn img { width:14px; height:14px; vertical-align:middle; opacity:0.92; border:none; }
                pre { background:#24262a; color:#d9dde4; border:1px solid #43474c; border-radius:6px; padding:10px; white-space:pre-wrap; }
                p { color:#d3d7de; line-height:1.45; }
                .tex-font-style-bf { font-weight:bold; }
                </style>
                """;

        String body = "<div class='problem-statement'>" + problemHtml + "</div>";
        return "<html><head>" + css + "</head><body>" + body + "</body></html>";
    }

    private String prepareProblemHtml(String rawProblemHtml) {
        copyPayloads.clear();

        Document doc = Jsoup.parseBodyFragment(rawProblemHtml);
        Element root = doc.body().children().isEmpty() ? doc.body() : doc.body().child(0);

        enhanceHeaderMetrics(root);
        enhanceSampleTestsWithCopy(root);
        return root.outerHtml();
    }

    private void enhanceHeaderMetrics(Element root) {
        Element header = root.selectFirst("div.header");
        if (header == null) {
            return;
        }

        Element timeLimit = header.selectFirst("div.time-limit");
        Element memoryLimit = header.selectFirst("div.memory-limit");
        Element inputFile = header.selectFirst("div.input-file");
        Element outputFile = header.selectFirst("div.output-file");

        if (timeLimit == null && memoryLimit == null && inputFile == null && outputFile == null) {
            return;
        }

        String timeText = metricValueOnly(timeLimit != null ? timeLimit.text() : "", "time limit per test");
        String memoryText = metricValueOnly(memoryLimit != null ? memoryLimit.text() : "", "memory limit per test");
        String inputText = metricValueOnly(inputFile != null ? inputFile.text() : "", "input", "input file");
        String outputText = metricValueOnly(outputFile != null ? outputFile.text() : "", "output", "output file");

        if (timeLimit != null) {
            timeLimit.remove();
        }
        if (memoryLimit != null) {
            memoryLimit.remove();
        }
        if (inputFile != null) {
            inputFile.remove();
        }
        if (outputFile != null) {
            outputFile.remove();
        }

        Element row = new Element(Tag.valueOf("div"), "");
        row.addClass("metrics-row");

        addMetricItem(row, "time.png", timeText);
        addMetricItem(row, "memory.png", memoryText);
        String ioMetric = combineInputOutputMetric(inputText, outputText);
        addMetricItem(row, "input.png", ioMetric);

        header.appendChild(row);
    }

    private void addMetricItem(Element row, String iconFile, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String iconSrc = loadIconSource(iconFile, 14);
        Element item = new Element(Tag.valueOf("span"), "").addClass("metric-item");
        if (!iconSrc.isBlank()) {
            Element icon = new Element(Tag.valueOf("img"), "");
            icon.addClass("metric-icon");
            icon.attr("src", iconSrc);
            icon.attr("alt", "");
            item.appendChild(icon);
        }
        item.appendText(text);
        row.appendChild(item);
    }

    private void enhanceSampleTestsWithCopy(Element root) {
        String copyIcon = loadIconSource("copy.png", 14);
        if (copyIcon.isBlank()) {
            return;
        }

        int inputCounter = 1;
        for (Element input : root.select("div.sample-tests div.input")) {
            Element pre = input.selectFirst("pre");
            if (pre == null) {
                continue;
            }
            String key = "sample-input-" + inputCounter++;
            copyPayloads.put(key, pre.text());
            decorateIoBlockHeader(input, "Input", key, copyIcon);
        }

        int outputCounter = 1;
        for (Element output : root.select("div.sample-tests div.output")) {
            Element pre = output.selectFirst("pre");
            if (pre == null) {
                continue;
            }
            String key = "sample-output-" + outputCounter++;
            copyPayloads.put(key, pre.text());
            decorateIoBlockHeader(output, "Output", key, copyIcon);
        }
    }

    private void decorateIoBlockHeader(Element ioBlock, String fallbackLabel, String key, String copyIcon) {
        Element existingTitle = ioBlock.selectFirst("div.title");
        String label = existingTitle != null ? existingTitle.text() : fallbackLabel;
        if (existingTitle != null) {
            existingTitle.remove();
        }
        ioBlock.prepend(createCopyHeaderHtml(label, key, copyIcon));
    }

    private String createCopyHeaderHtml(String label, String key, String copyIconDataUri) {
        return "<div class='io-header'><span class='io-label'>" + label + "</span><a class='copy-btn' href='copy:" + key + "'><img src='" + copyIconDataUri + "' alt=''/></a></div>";
    }

    private String combineInputOutputMetric(String inputText, String outputText) {
        if ((inputText == null || inputText.isBlank()) && (outputText == null || outputText.isBlank())) {
            return "";
        }
        if (inputText == null || inputText.isBlank()) {
            return outputText;
        }
        if (outputText == null || outputText.isBlank()) {
            return inputText;
        }
        return inputText + " / " + outputText;
    }

    private String loadIconSource(String iconFile, int size) {
        String cacheKey = iconFile + "@" + size;
        if (iconSourceCache.containsKey(cacheKey)) {
            return iconSourceCache.get(cacheKey);
        }

        try {
            Path iconPath = Path.of("assets", iconFile);
            if (!Files.exists(iconPath)) {
                return "";
            }

            BufferedImage source = ImageIO.read(iconPath.toFile());
            if (source == null) {
                return "";
            }

            BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = target.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(source, 0, 0, size, size, null);
            g2.dispose();

            Path iconCacheDir = getSettingsFilePath().getParent().resolve("cache").resolve("icons");
            Files.createDirectories(iconCacheDir);
            String sanitized = iconFile.replace('.', '_');
            Path scaledFile = iconCacheDir.resolve(sanitized + "_" + size + "px.png");
            ImageIO.write(target, "png", scaledFile.toFile());

            String src = scaledFile.toUri().toString();
            iconSourceCache.put(cacheKey, src);
            return src;
        } catch (IOException e) {
            return "";
        }
    }

    private String metricValueOnly(String text, String... prefixes) {
        if (text == null) {
            return "";
        }

        String cleaned = text.trim();
        String lowered = cleaned.toLowerCase();

        for (String prefix : prefixes) {
            String lowerPrefix = prefix.toLowerCase();
            if (lowered.startsWith(lowerPrefix)) {
                cleaned = cleaned.substring(prefix.length()).trim();
                if (cleaned.startsWith(":")) {
                    cleaned = cleaned.substring(1).trim();
                }
                return cleaned;
            }
        }

        int colon = cleaned.indexOf(':');
        if (colon >= 0 && colon + 1 < cleaned.length()) {
            return cleaned.substring(colon + 1).trim();
        }
        return cleaned;
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
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

    private AppSettings loadSettings() {
        Path settingsFile = getSettingsFilePath();
        try {
            Files.createDirectories(settingsFile.getParent());
            if (!Files.exists(settingsFile)) {
                AppSettings defaults = AppSettings.defaults();
                saveSettings(defaults);
                return defaults;
            }

            Properties properties = new Properties();
            try (InputStream input = new BufferedInputStream(Files.newInputStream(settingsFile))) {
                properties.load(input);
            }

            int x = parseInt(properties.getProperty("window.x"), -1);
            int y = parseInt(properties.getProperty("window.y"), -1);
            int width = parseInt(properties.getProperty("window.width"), 1200);
            int height = parseInt(properties.getProperty("window.height"), 760);
            int divider = parseInt(properties.getProperty("window.dividerLocation"), 420);
            boolean maximized = Boolean.parseBoolean(properties.getProperty("window.maximized", "false"));
            String language = properties.getProperty("language.last", DEFAULT_LANGUAGE);

            return new AppSettings(x, y, width, height, divider, maximized, language);
        } catch (IOException e) {
            return AppSettings.defaults();
        }
    }

    private void persistSettings(JFrame frame) {
        int state = frame.getExtendedState();
        boolean maximized = (state & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
        String language = DEFAULT_LANGUAGE;
        if (languageDropdown != null && languageDropdown.getSelectedItem() != null) {
            language = languageDropdown.getSelectedItem().toString();
        }
        int dividerLocation = contentSplitPane != null ? contentSplitPane.getDividerLocation() : 420;

        AppSettings settings = new AppSettings(
                frame.getX(),
                frame.getY(),
                frame.getWidth(),
                frame.getHeight(),
            dividerLocation,
                maximized,
                language);
        saveSettings(settings);
    }

    private void saveSettings(AppSettings settings) {
        Path settingsFile = getSettingsFilePath();
        try {
            Files.createDirectories(settingsFile.getParent());
            Properties properties = new Properties();
            properties.setProperty("window.x", Integer.toString(settings.x()));
            properties.setProperty("window.y", Integer.toString(settings.y()));
            properties.setProperty("window.width", Integer.toString(settings.width()));
            properties.setProperty("window.height", Integer.toString(settings.height()));
            properties.setProperty("window.dividerLocation", Integer.toString(settings.dividerLocation()));
            properties.setProperty("window.maximized", Boolean.toString(settings.maximized()));
            properties.setProperty("language.last", settings.lastLanguage());

            try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(settingsFile))) {
                properties.store(output, "Competitive Programming Ally settings");
            }
        } catch (IOException ignored) {
            // Ignore persistence failures silently to avoid disrupting app startup/shutdown.
        }
    }

    private void applyWindowSettings(JFrame frame, AppSettings settings) {
        int width = Math.max(MIN_WINDOW_WIDTH, settings.width());
        int height = Math.max(MIN_WINDOW_HEIGHT, settings.height());
        frame.setSize(width, height);

        if (settings.x() >= 0 && settings.y() >= 0) {
            frame.setLocation(settings.x(), settings.y());
        } else {
            frame.setLocationRelativeTo(null);
        }
    }

    private Path getSettingsFilePath() {
        String appData = System.getenv("APPDATA");
        Path basePath;
        if (appData != null && !appData.isBlank()) {
            basePath = Path.of(appData);
        } else {
            basePath = Path.of(System.getProperty("user.home"), "AppData", "Roaming");
        }
        return basePath.resolve(SETTINGS_DIR_NAME).resolve(SETTINGS_FILE_NAME);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private record AppSettings(int x, int y, int width, int height, int dividerLocation, boolean maximized, String lastLanguage) {
        private static AppSettings defaults() {
            return new AppSettings(-1, -1, 1200, 760, 420, false, DEFAULT_LANGUAGE);
        }
    }

    private record ProblemDetails(
            String code,
            String title,
            String problemHtml) {
    }

    private void disableFocus(Component component) {
        component.setFocusable(false);
        if (component instanceof JComponent jComponent) {
            jComponent.setRequestFocusEnabled(false);
        }
    }
}
