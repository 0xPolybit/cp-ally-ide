package com.example;

import com.formdev.flatlaf.FlatDarkLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.event.ItemEvent;

public class MainWindow {

    private static final String APP_NAME = "Competitive Programming Ally";
    private static final String PROBLEM_PLACEHOLDER = "Enter problem code (eg: 2208A)";
    private static final Pattern PROBLEM_CODE_PATTERN = Pattern.compile("^(\\d{1,6})([A-Za-z][A-Za-z0-9]{0,2})$");
    private static final String DEFAULT_LANGUAGE = "Python 3";
    private static final String SETTINGS_DIR_NAME = "CompetitiveProgrammingAlly";
    private static final String SETTINGS_FILE_NAME = "settings.properties";
    private static final String CURRENT_APP_VERSION = "0.1.2";
    private static final String VERSION_SOURCE_URL = "https://pastebin.com/raw/uzU8MUWs";
    private static final String RELEASES_URL = "https://github.com/0xPolybit/cp-ally-ide/releases";
    private static final Pattern SEMVER_PATTERN = Pattern.compile("\\b(\\d+\\.\\d+\\.\\d+)\\b");
    private static final int LEFT_FIELD_WIDTH = 280;
    private static final int LEFT_FIELD_HEIGHT = 32;
    private static final Color ACCENT = new Color(55, 247, 19);
    private static final int MIN_WINDOW_WIDTH = 1000;
    private static final int MIN_WINDOW_HEIGHT = 680;
    private static final int MIN_LEFT_PANEL_WIDTH = 280;
    private static final int MIN_RIGHT_PANEL_WIDTH = 420;
    private static final int RUN_ICON_SIZE = 24;

    private final SettingsRepository settingsRepository = new SettingsRepository(
            SETTINGS_DIR_NAME,
            SETTINGS_FILE_NAME,
            DEFAULT_LANGUAGE);
        private final ProgramCacheRepository programCacheRepository = new ProgramCacheRepository(settingsRepository.getAppDataDirectory());
    private CodeforcesService codeforcesService;
    private final CodeExecutionService codeExecutionService = new CodeExecutionService();

    private ProblemHtmlRenderer problemHtmlRenderer;
    private JButton initialFocusButton;
    private JComboBox<String> languageDropdown;
    private JLabel runtimeSupportLabel;
    private JLabel executionStateLabel;
    private AppSettings appSettings;
    private JFrame mainFrame;
    private JTextField problemCodeInput;
    private JButton fetchProblemButton;
    private JLabel fetchStatusLabel;
    private JButton runButton;
    private RSyntaxTextArea codeEditor;
    private RTextScrollPane codeScrollPane;
    private JPanel leftPanelContainer;
    private JPanel problemEntryPanel;
    private final Map<String, String> copyPayloads = new HashMap<>();
    private JSplitPane contentSplitPane;
    private JSplitPane statementTestCasesSplitPane;
    private TestCasesPanel testCasesPanel;
    private boolean problemStatementLoaded;
    private String currentProblemCode;

    public void showWindow() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        FlatDarkLaf.setup();
        applyGlobalDarkPalette();

        appSettings = settingsRepository.load();
        Path appDataDir = settingsRepository.getAppDataDirectory();
        problemHtmlRenderer = new ProblemHtmlRenderer(appDataDir);
        codeforcesService = new CodeforcesService(appDataDir);

        JFrame frame = new JFrame(APP_NAME);
        applyWindowIcon(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT));
        frame.getRootPane().putClientProperty("JRootPane.menuBarEmbedded", true);
        frame.getRootPane().putClientProperty("JRootPane.titleBarBackground", new Color(43, 45, 48));
        frame.getRootPane().putClientProperty("JRootPane.titleBarForeground", new Color(230, 233, 238));
        frame.setJMenuBar(createEmbeddedTitleBar());
        mainFrame = frame;
        testCasesPanel = new TestCasesPanel(mainFrame);

        frame.add(createContentSplit(), BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveCurrentProgramToCache();
                persistSettings(frame);
            }
        });

        applyWindowSettings(frame, appSettings);
        frame.setVisible(true);

        if (contentSplitPane != null && appSettings.dividerLocation() > 0) {
            SwingUtilities.invokeLater(() -> contentSplitPane.setDividerLocation(appSettings.dividerLocation()));
        }

        if (statementTestCasesSplitPane != null && appSettings.testCasesDividerLocation() > 0) {
            SwingUtilities.invokeLater(() -> statementTestCasesSplitPane.setDividerLocation(appSettings.testCasesDividerLocation()));
        }

        if (appSettings.maximized()) {
            SwingUtilities.invokeLater(() -> frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH));
        }

        if (initialFocusButton != null) {
            SwingUtilities.invokeLater(() -> initialFocusButton.requestFocusInWindow());
        }

        checkForAppUpdatesAsync();
    }

    private void applyWindowIcon(JFrame frame) {
        if (frame == null) {
            return;
        }

        try {
            Path logoPath = Path.of("assets", "logo.png");
            if (!Files.exists(logoPath)) {
                return;
            }

            ImageIcon logo = new ImageIcon(logoPath.toAbsolutePath().toString());
            if (logo.getIconWidth() <= 0 || logo.getIconHeight() <= 0) {
                return;
            }
            frame.setIconImage(logo.getImage());
        } catch (Exception ignored) {
            // Ignore icon-loading failures to keep startup robust.
        }
    }

    private void checkForAppUpdatesAsync() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return fetchLatestVersion();
            }

            @Override
            protected void done() {
                try {
                    String latestVersion = get();
                    if (latestVersion == null || latestVersion.isBlank()) {
                        return;
                    }
                    if (!CURRENT_APP_VERSION.equals(latestVersion)) {
                        showUpdateAvailableDialog(latestVersion);
                    }
                } catch (Exception ignored) {
                    // Ignore update-check failures to avoid interrupting normal app usage.
                }
            }
        };
        worker.execute();
    }

    private String fetchLatestVersion() {
        HttpURLConnection connection = null;
        try {
            URL url = URI.create(VERSION_SOURCE_URL).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "cp-ally-ide");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
                return extractVersion(content.toString());
            }
        } catch (IOException ignored) {
            return "";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String extractVersion(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        Matcher matcher = SEMVER_PATTERN.matcher(rawText);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private void showUpdateAvailableDialog(String latestVersion) {
        Object[] options = {"Open Releases", "Later"};
        int selection = JOptionPane.showOptionDialog(
                mainFrame,
                "A new version of Competitive Programming Ally is available.\n\n"
                        + "Current version: " + CURRENT_APP_VERSION + "\n"
                        + "Latest version: " + latestVersion,
                "Update Available",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        if (selection == JOptionPane.YES_OPTION) {
            openExternalUrl(RELEASES_URL);
        }
    }

    private void openExternalUrl(String url) {
        try {
            if (!Desktop.isDesktopSupported() || url == null || url.isBlank()) {
                return;
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ignored) {
            // Silently ignore browser launch issues.
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
        titleBar.setOpaque(true);
        titleBar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        titleBar.setBackground(new Color(43, 45, 48));

        JMenu fileMenu = new JMenu("File");
        JMenuItem clearProgramCacheItem = new JMenuItem("Clear Programming Cache");
        clearProgramCacheItem.addActionListener(e -> onClearProgrammingCacheClicked());
        JMenuItem clearProblemCacheItem = new JMenuItem("Clear Problem Cache");
        clearProblemCacheItem.addActionListener(e -> onClearProblemCacheClicked());
        fileMenu.add(clearProgramCacheItem);
        fileMenu.add(clearProblemCacheItem);
        titleBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");

        JMenuItem fetchLatestVersionItem = new JMenuItem("Fetch Latest Version");
        fetchLatestVersionItem.addActionListener(e -> onFetchLatestVersionClicked());

        JMenuItem githubReleasesItem = new JMenuItem("GitHub Releases");
        githubReleasesItem.addActionListener(e -> openExternalUrl(RELEASES_URL));

        JMenuItem creditsItem = new JMenuItem("Credits");
        creditsItem.addActionListener(e -> showCreditsDialog());

        helpMenu.add(fetchLatestVersionItem);
        helpMenu.add(githubReleasesItem);
        helpMenu.add(creditsItem);
        titleBar.add(helpMenu);
        return titleBar;
    }

    private void onClearProgrammingCacheClicked() {
        int response = JOptionPane.showConfirmDialog(
                mainFrame,
                "Are you sure you want to delete all programming cache? This action cannot be undone.",
                "Confirm Clear Programming Cache",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (response == JOptionPane.YES_OPTION) {
            programCacheRepository.clearAll();
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Programming cache cleared successfully.",
                    "Cache Cleared",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onClearProblemCacheClicked() {
        int response = JOptionPane.showConfirmDialog(
                mainFrame,
                "Are you sure you want to delete all problem cache? This action cannot be undone.",
                "Confirm Clear Problem Cache",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (response == JOptionPane.YES_OPTION) {
            codeforcesService.clearProblemCache();
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Problem cache cleared successfully.",
                    "Cache Cleared",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onFetchLatestVersionClicked() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return fetchLatestVersion();
            }

            @Override
            protected void done() {
                try {
                    String latestVersion = get();
                    String message = (latestVersion == null || latestVersion.isBlank())
                            ? "Unable to fetch latest version right now."
                            : "Latest version: " + latestVersion;
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            message,
                            "Latest Version",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Unable to fetch latest version right now.",
                            "Latest Version",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showCreditsDialog() {
        JDialog dialog = new JDialog(mainFrame, "Credits", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(30, 31, 34));

        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        pane.setText("""
                <html><body style='background:#1e1f22;color:#dfe1e5;font-family:Segoe UI, Arial, sans-serif;'>
                <h2 style='margin-top:0;'>Credits</h2>
                <p><b>Name:</b> Swastik Biswas</p>
                <p><b>College:</b> Kalinga Institute for Industrial Technology</p>
                <p><b>Nationality:</b> United States of America</p>
                <p><b>GitHub:</b> <a href='https://github.com/0xPolybit'>https://github.com/0xPolybit</a></p>
                <p><b>Instagram:</b> <a href='https://www.instagram.com/swastikbiswas1776/'>https://www.instagram.com/swastikbiswas1776/</a></p>
                <p><b>X:</b> <a href='https://x.com/0xSwastikBiswas'>https://x.com/0xSwastikBiswas</a></p>
                <p><b>LinkedIn:</b> <a href='https://www.linkedin.com/in/polybit/'>https://www.linkedin.com/in/polybit/</a></p>
                <p><b>CodeForces:</b> <a href='https://codeforces.com/profile/swastikpolybitbiswas'>https://codeforces.com/profile/swastikpolybitbiswas</a></p>
                <p><b>LeetCode:</b> <a href='https://leetcode.com/u/swastikbiswas/'>https://leetcode.com/u/swastikbiswas/</a></p>
                </body></html>
                """);
        pane.setCaretPosition(0);
        pane.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED && event.getDescription() != null) {
                openExternalUrl(event.getDescription());
            }
        });

        JScrollPane scrollPane = new JScrollPane(pane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(30, 31, 34));

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.setSize(700, 520);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
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
        problemCodeInput.addActionListener(e -> fetchProblemButton.doClick());

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

        runButton = createToolbarButton("Run");
        runButton.setEnabled(false);
        applyRunButtonIcons();
        runButton.addActionListener(e -> onRunButtonClicked());
        editorToolbar.add(runButton);
        editorToolbar.add(Box.createHorizontalGlue());

        runtimeSupportLabel = new JLabel("Executable: checking...");
        runtimeSupportLabel.setForeground(new Color(169, 176, 188));
        runtimeSupportLabel.setFont(runtimeSupportLabel.getFont().deriveFont(Font.PLAIN, 12f));
        editorToolbar.add(runtimeSupportLabel);
        editorToolbar.add(Box.createHorizontalStrut(10));

        executionStateLabel = new JLabel("Status: Idle");
        executionStateLabel.setForeground(new Color(169, 176, 188));
        executionStateLabel.setFont(executionStateLabel.getFont().deriveFont(Font.PLAIN, 12f));
        editorToolbar.add(executionStateLabel);
        editorToolbar.add(Box.createHorizontalStrut(10));

        languageDropdown = new JComboBox<>(new String[] {
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
        languageDropdown.setBackground(new Color(50, 53, 58));
        languageDropdown.setForeground(new Color(223, 225, 229));
        languageDropdown.setFocusable(false);
        languageDropdown.setRequestFocusEnabled(false);
        languageDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                saveCurrentProgramToCache(e.getItem() != null ? e.getItem().toString() : null);
                return;
            }

            updateExecutionAvailability();
            if (problemStatementLoaded) {
                applyLanguageTemplateOrCachedProgram();
            }
        });
        editorToolbar.add(languageDropdown);
        updateExecutionAvailability();

        codeEditor = new RSyntaxTextArea(24, 80);
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        codeEditor.setTabSize(4);
        codeEditor.setCodeFoldingEnabled(false);
        codeEditor.setEditable(false);
        codeEditor.setFocusable(false);
        codeEditor.setRequestFocusEnabled(false);
        applyEclipseEditorTheme(codeEditor);
        installEditorAutoPairs(codeEditor);
        codeEditor.setText("Select a problem to get started...");
        codeEditor.setCaretPosition(0);
     
        // Editor zoom (font size) keybindings: Ctrl + Plus / Ctrl + Equals / NumpadAdd to increase,
        // Ctrl + Minus / NumpadSubtract to decrease by 2pt.
        javax.swing.Action zoomInAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                java.awt.Font f = codeEditor.getFont();
                int newSize = Math.min(40, Math.max(8, f.getSize() + 2));
                codeEditor.setFont(f.deriveFont((float) newSize));

                // Update syntax scheme fonts so token styles scale as well
                SyntaxScheme scheme = codeEditor.getSyntaxScheme();
                if (scheme != null) {
                    for (int i = 0; i < scheme.getStyleCount(); i++) {
                        org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(i);
                        if (s != null && s.font != null) {
                            s.font = s.font.deriveFont((float) newSize);
                        }
                    }
                    codeEditor.setSyntaxScheme(scheme);
                }

                // Update gutter line number font if available
                if (codeScrollPane != null) {
                    try {
                        codeScrollPane.getGutter().setLineNumberFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, newSize));
                    } catch (Exception ignored) {
                    }
                }

                codeEditor.revalidate();
                codeEditor.repaint();
            }
         };

        javax.swing.Action zoomOutAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                java.awt.Font f = codeEditor.getFont();
                int newSize = Math.min(40, Math.max(8, f.getSize() - 2));
                codeEditor.setFont(f.deriveFont((float) newSize));

                SyntaxScheme scheme = codeEditor.getSyntaxScheme();
                if (scheme != null) {
                    for (int i = 0; i < scheme.getStyleCount(); i++) {
                        org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(i);
                        if (s != null && s.font != null) {
                            s.font = s.font.deriveFont((float) newSize);
                        }
                    }
                    codeEditor.setSyntaxScheme(scheme);
                }

                if (codeScrollPane != null) {
                    try {
                        codeScrollPane.getGutter().setLineNumberFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, newSize));
                    } catch (Exception ignored) {
                    }
                }

                codeEditor.revalidate();
                codeEditor.repaint();
            }
        };

        // Bind multiple keystrokes for plus (since '+' often requires Shift)
        javax.swing.InputMap im = codeEditor.getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.ActionMap am = codeEditor.getActionMap();
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ADD, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomIn");
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomOut");
        im.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, java.awt.event.InputEvent.CTRL_DOWN_MASK), "zoomOut");
        am.put("zoomIn", zoomInAction);
        am.put("zoomOut", zoomOutAction);
        RTextScrollPane scrollPane = new RTextScrollPane(codeEditor);
        codeScrollPane = scrollPane;

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
                return codeforcesService.evaluateConnectivity();
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
        fetchProblemByCode(rawCode, true);
    }

    private void fetchProblemByCode(String rawCode, boolean showWarnings) {
        if (rawCode.isEmpty() || PROBLEM_PLACEHOLDER.equals(rawCode)) {
            if (showWarnings) {
                showFetchWarning("Enter a problem code, for example 2208A.");
            }
            return;
        }

        saveCurrentProgramToCache();

        Matcher matcher = PROBLEM_CODE_PATTERN.matcher(rawCode);
        if (!matcher.matches()) {
            if (showWarnings) {
                showFetchWarning("Invalid problem code. Use format like 2208A.");
            }
            return;
        }

        String contestId = matcher.group(1);
        String index = matcher.group(2).toUpperCase();

        showLeftPanelLoading(contestId + index);

        SwingWorker<RenderedProblemView[], Void> worker = new SwingWorker<>() {
            @Override
            protected RenderedProblemView[] doInBackground() throws Exception {
                ProblemDetails details = codeforcesService.fetchProblemDetails(contestId, index);
                RenderedProblemView full = problemHtmlRenderer.render(details);
                RenderedProblemView statementOnly = problemHtmlRenderer.renderStatementOnly(details);
                return new RenderedProblemView[]{statementOnly, full};
            }

            @Override
            protected void done() {
                try {
                    RenderedProblemView[] renders = get();
                    showCodeforcesProblemView(contestId + index, renders[0], renders[1]);
                } catch (Exception ex) {
                    restoreProblemEntryPanelWithError("Could not fetch that problem.");
                    JOptionPane.showMessageDialog(
                            null,
                            "Could not fetch the specified CodeForces problem. Please verify the code and try again.",
                            APP_NAME,
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void promptForDifferentProblem() {
        String initialValue = "";
        if (problemCodeInput != null && problemCodeInput.getText() != null) {
            String current = problemCodeInput.getText().trim();
            if (!current.isEmpty() && !PROBLEM_PLACEHOLDER.equals(current)) {
                initialValue = current;
            }
        }

        String entered = (String) JOptionPane.showInputDialog(
                mainFrame,
                "Enter problem code (e.g. 2208A):",
                "Choose Different Problem",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                initialValue);

        if (entered == null) {
            return;
        }

        String rawCode = entered.trim();
        if (problemCodeInput != null) {
            problemCodeInput.setText(rawCode.isEmpty() ? PROBLEM_PLACEHOLDER : rawCode);
        }
        fetchProblemByCode(rawCode, true);
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

    private void showCodeforcesProblemView(String problemCode, RenderedProblemView statementOnly, RenderedProblemView full) {
        copyPayloads.clear();
        copyPayloads.putAll(full.copyPayloads());
        if (testCasesPanel != null) {
            testCasesPanel.setSamplePayloads(full.copyPayloads());
        }

        // Top panel: Statement without test cases
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setText(statementOnly.html());
        pane.setCaretPosition(0);
        pane.setBackground(new Color(30, 31, 34));
        pane.addHyperlinkListener(event -> {
            if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED || event.getDescription() == null) {
                return;
            }
            String description = event.getDescription();
            if (description.startsWith("copy:")) {
                String key = description.substring("copy:".length());
                String payload = copyPayloads.get(key);
                if (payload != null) {
                    copyToClipboard(payload);
                }
                return;
            }

            if (description.startsWith("http://") || description.startsWith("https://")) {
                openExternalUrl(description);
            }
        });

        JScrollPane scrollPane = new JScrollPane(pane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(30, 31, 34));
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);

        JButton chooseDifferentProblemButton = new JButton("Choose Different Problem");
        chooseDifferentProblemButton.setFocusable(false);
        chooseDifferentProblemButton.setRequestFocusEnabled(false);
        chooseDifferentProblemButton.setPreferredSize(new Dimension(280, LEFT_FIELD_HEIGHT));
        chooseDifferentProblemButton.addActionListener(e -> promptForDifferentProblem());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        topBar.add(chooseDifferentProblemButton, BorderLayout.WEST);

        JPanel statementPanel = new JPanel(new BorderLayout());
        statementPanel.setOpaque(false);
        statementPanel.add(topBar, BorderLayout.NORTH);
        statementPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel: Test cases in tabs
        JPanel testCasesSection = testCasesPanel != null ? testCasesPanel.createPanel() : new JPanel();

        // Split pane: statement on top, test cases on bottom
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statementPanel, testCasesSection);
        splitPane.setResizeWeight(0.5);

        int minTestCaseHeight = MIN_WINDOW_HEIGHT / 3;
        int preferredDivider = appSettings != null && appSettings.testCasesDividerLocation() > 0
            ? appSettings.testCasesDividerLocation()
            : mainFrame.getHeight() - minTestCaseHeight - 10;
        splitPane.setDividerLocation(preferredDivider);

        statementTestCasesSplitPane = splitPane;

        leftPanelContainer.removeAll();
        leftPanelContainer.add(splitPane, BorderLayout.CENTER);
        leftPanelContainer.revalidate();
        leftPanelContainer.repaint();

        currentProblemCode = problemCode;
        problemStatementLoaded = true;
        enableEditorForProblem();
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    private void enableEditorForProblem() {
        if (codeEditor != null) {
            codeEditor.setEditable(true);
            codeEditor.setFocusable(true);
            codeEditor.setRequestFocusEnabled(true);
        }

        applyLanguageTemplateOrCachedProgram();
        updateExecutionAvailability();
    }

    private void applyLanguageTemplateOrCachedProgram() {
        if (codeEditor == null || languageDropdown == null || languageDropdown.getSelectedItem() == null) {
            return;
        }

        String language = languageDropdown.getSelectedItem().toString();
        String cachedProgram = currentProblemCode != null
                ? programCacheRepository.loadLatestSource(currentProblemCode, language)
                : null;

        if (cachedProgram != null) {
            codeEditor.setSyntaxEditingStyle(resolveSyntaxStyle(language));
            codeEditor.setText(cachedProgram);
            codeEditor.setCaretPosition(Math.min(codeEditor.getText().length(), cachedProgram.length()));
            return;
        }

        applyLanguageTemplate();
    }

    private void applyLanguageTemplate() {
        if (codeEditor == null || languageDropdown == null || languageDropdown.getSelectedItem() == null) {
            return;
        }

        String language = languageDropdown.getSelectedItem().toString();
        codeEditor.setSyntaxEditingStyle(resolveSyntaxStyle(language));
        String boilerplate = boilerplateFor(language);
        codeEditor.setText(boilerplate);

        int cursor = boilerplate.indexOf("// code goes here...");
        if (cursor < 0) {
            cursor = boilerplate.indexOf("# code goes here...");
        }
        codeEditor.setCaretPosition(Math.max(0, cursor));
    }

    private void saveCurrentProgramToCache() {
        if (languageDropdown == null || languageDropdown.getSelectedItem() == null) {
            return;
        }

        saveCurrentProgramToCache(languageDropdown.getSelectedItem().toString());
    }

    private void saveCurrentProgramToCache(String language) {
        if (!problemStatementLoaded || currentProblemCode == null || codeEditor == null || language == null || language.isBlank()) {
            return;
        }

        String sourceCode = codeEditor.getText();
        if (sourceCode == null) {
            return;
        }

        programCacheRepository.save(currentProblemCode, language, sourceCode);
    }

    private void updateExecutionAvailability() {
        if (languageDropdown == null || runtimeSupportLabel == null || runButton == null) {
            return;
        }

        String language = selectedLanguage();
        CodeExecutionService.LanguageSupport support = codeExecutionService.detectSupport(language);
        boolean ready = problemStatementLoaded && support.supported();

        runtimeSupportLabel.setText("<html>Executable: <span style='color:"
                + (support.supported() ? "#61d66e" : "#f65656")
                + ";'>"
                + (support.supported() ? "Yes" : "No")
                + "</span></html>");
        runtimeSupportLabel.setToolTipText(support.message());
        runButton.setEnabled(ready);
        runButton.setToolTipText(ready ? "Run the sample test cases locally" : support.message());
    }

    private String selectedLanguage() {
        if (languageDropdown == null || languageDropdown.getSelectedItem() == null) {
            return DEFAULT_LANGUAGE;
        }
        return languageDropdown.getSelectedItem().toString();
    }

    private void onRunButtonClicked() {
        if (!problemStatementLoaded || codeEditor == null) {
            return;
        }

        String language = selectedLanguage();
        CodeExecutionService.LanguageSupport support = codeExecutionService.detectSupport(language);
        if (!support.supported()) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    support.message(),
                    "Execution unavailable",
                    JOptionPane.WARNING_MESSAGE);
            updateExecutionAvailability();
            return;
        }

        List<CodeExecutionService.TestCaseSpec> testCases = testCasesPanel != null
            ? testCasesPanel.getExecutionTestCases()
            : SampleTestCaseCollector.collect(copyPayloads);
        if (testCases.isEmpty()) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "No test cases were found for this problem.",
                    "Nothing to run",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String sourceCode = codeEditor.getText();
        runButton.setEnabled(false);
        runButton.setToolTipText("Running sample test cases...");
        setExecutionRunningState(true);

        SwingWorker<CodeExecutionService.ExecutionReport, Void> worker = new SwingWorker<>() {
            @Override
            protected CodeExecutionService.ExecutionReport doInBackground() throws Exception {
                return codeExecutionService.runSampleTests(language, sourceCode, testCases);
            }

            @Override
            protected void done() {
                setExecutionRunningState(false);
                updateExecutionAvailability();
                try {
                    showExecutionResultsDialog(language, get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Failed to run the selected language locally.\n\n" + ex.getMessage(),
                            "Execution error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void setExecutionRunningState(boolean running) {
        if (executionStateLabel == null) {
            return;
        }

        if (running) {
            executionStateLabel.setText("Status: Running");
            executionStateLabel.setForeground(new Color(247, 215, 26));
        } else {
            executionStateLabel.setText("Status: Idle");
            executionStateLabel.setForeground(new Color(169, 176, 188));
        }
    }

    private void installEditorAutoPairs(RSyntaxTextArea editor) {
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!editor.isEditable() || e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
                    return;
                }

                if (editor.getSelectionStart() != editor.getSelectionEnd()) {
                    return;
                }

                int caret = editor.getCaretPosition();
                int length = editor.getDocument().getLength();
                if (caret <= 0 || caret >= length) {
                    return;
                }

                try {
                    String left = editor.getDocument().getText(caret - 1, 1);
                    String right = editor.getDocument().getText(caret, 1);
                    if (left.length() == 1 && right.length() == 1) {
                        char opening = left.charAt(0);
                        char closing = right.charAt(0);
                        char expected = switch (opening) {
                            case '(' -> ')';
                            case '[' -> ']';
                            case '{' -> '}';
                            default -> '\0';
                        };

                        if (expected != '\0' && closing == expected) {
                            editor.getDocument().remove(caret - 1, 2);
                            editor.setCaretPosition(caret - 1);
                            e.consume();
                        }
                    }
                } catch (Exception ignored) {
                    // Keep default backspace behavior if paired deletion fails.
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (!editor.isEditable()) {
                    return;
                }

                Character closing = switch (e.getKeyChar()) {
                    case '(' -> ')';
                    case '[' -> ']';
                    case '{' -> '}';
                    default -> null;
                };

                if (closing == null) {
                    return;
                }

                int caret = editor.getCaretPosition();
                if (caret < 0 || caret > editor.getDocument().getLength()) {
                    return;
                }

                try {
                    editor.getDocument().insertString(caret, String.valueOf(closing), null);
                    editor.setCaretPosition(caret);
                } catch (Exception ignored) {
                    // Keep default typing behavior if insertion fails.
                }
            }
        });
    }

    private void showExecutionResultsDialog(String language, CodeExecutionService.ExecutionReport report) {
        if (!report.success()) {
            showCompilationErrorDialog(language, report.failureMessage());
            return;
        }

        JDialog dialog = new JDialog(mainFrame, "Execution Results", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(30, 31, 34));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(43, 45, 48));
        header.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Local execution for " + language);
        title.setForeground(new Color(236, 239, 244));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        header.add(title, BorderLayout.NORTH);

        JLabel summary = new JLabel(ExecutionResultFormatter.summary(report));
        summary.setForeground(new Color(169, 176, 188));
        header.add(summary, BorderLayout.SOUTH);
        dialog.add(header, BorderLayout.NORTH);

        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setText(ExecutionResultFormatter.buildResultsHtml(language, report));
        pane.setCaretPosition(0);
        pane.setBorder(BorderFactory.createEmptyBorder());

        JScrollPane scrollPane = new JScrollPane(pane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(30, 31, 34));
        dialog.add(scrollPane, BorderLayout.CENTER);

        dialog.setSize(860, 620);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    private void showCompilationErrorDialog(String language, String failureMessage) {
        JDialog dialog = new JDialog(mainFrame, "Execution Results", true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(30, 31, 34));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(43, 45, 48));
        header.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Local execution for " + language);
        title.setForeground(new Color(236, 239, 244));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        header.add(title, BorderLayout.NORTH);

        JLabel summary = new JLabel("Compilation failed.");
        summary.setForeground(new Color(246, 86, 86));
        header.add(summary, BorderLayout.SOUTH);
        dialog.add(header, BorderLayout.NORTH);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(new Color(30, 31, 34));
        area.setForeground(new Color(223, 225, 229));
        area.setCaretColor(new Color(223, 225, 229));
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        area.setText(failureMessage == null ? "Compilation failed." : failureMessage);
        area.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(30, 31, 34));
        dialog.add(scrollPane, BorderLayout.CENTER);

        dialog.setSize(860, 620);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    private void applyRunButtonIcons() {
        if (runButton == null) {
            return;
        }

        ImageIcon normal = loadToolbarIcon("run.png");
        ImageIcon hover = loadToolbarIcon("run-hover.png");
        if (normal == null) {
            return;
        }

        runButton.setText("");
        runButton.setIcon(normal);
        runButton.setDisabledIcon(normal);
        runButton.setRolloverEnabled(hover != null);
        if (hover != null) {
            runButton.setRolloverIcon(hover);
        }
        runButton.setPreferredSize(new Dimension(RUN_ICON_SIZE + 14, RUN_ICON_SIZE + 10));
    }

    private ImageIcon loadToolbarIcon(String fileName) {
        try {
            Path iconPath = resolveToolbarIconPath(fileName);
            if (!Files.exists(iconPath)) {
                return null;
            }

            ImageIcon raw = new ImageIcon(iconPath.toAbsolutePath().toString());
            if (raw.getIconWidth() <= 0 || raw.getIconHeight() <= 0) {
                return null;
            }

            if (raw.getIconWidth() == RUN_ICON_SIZE && raw.getIconHeight() == RUN_ICON_SIZE) {
                return raw;
            }

            BufferedImage scaled = new BufferedImage(RUN_ICON_SIZE, RUN_ICON_SIZE, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.drawImage(raw.getImage(), 0, 0, RUN_ICON_SIZE, RUN_ICON_SIZE, null);
            g2.dispose();
            return new ImageIcon(scaled);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Path resolveToolbarIconPath(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot >= 0 ? fileName.substring(dot) : "";

        Path hiDpiPath = Path.of("assets", base + "@2x" + ext);
        if (Files.exists(hiDpiPath)) {
            return hiDpiPath;
        }
        return Path.of("assets", fileName);
    }

    private String resolveSyntaxStyle(String language) {
        if (language.startsWith("Python") || language.startsWith("PyPy")) {
            return SyntaxConstants.SYNTAX_STYLE_PYTHON;
        }
        if (language.startsWith("GNU G++") || language.startsWith("GNU C11") || language.startsWith("GNU G11")) {
            return SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
        }
        if (language.startsWith("Java ")) {
            return SyntaxConstants.SYNTAX_STYLE_JAVA;
        }
        if (language.startsWith("Kotlin")) {
            return SyntaxConstants.SYNTAX_STYLE_KOTLIN;
        }
        if (language.startsWith("C#")) {
            return SyntaxConstants.SYNTAX_STYLE_CSHARP;
        }
        if (language.startsWith("Go")) {
            return SyntaxConstants.SYNTAX_STYLE_GO;
        }
        if (language.startsWith("Rust")) {
            return SyntaxConstants.SYNTAX_STYLE_RUST;
        }
        if (language.startsWith("Node.js") || language.startsWith("JavaScript")) {
            return SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
        }
        if (language.startsWith("PHP")) {
            return SyntaxConstants.SYNTAX_STYLE_PHP;
        }
        if (language.startsWith("Ruby")) {
            return SyntaxConstants.SYNTAX_STYLE_RUBY;
        }
        if (language.startsWith("Perl")) {
            return SyntaxConstants.SYNTAX_STYLE_PERL;
        }
        if (language.startsWith("Scala")) {
            return SyntaxConstants.SYNTAX_STYLE_SCALA;
        }
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }

    private String boilerplateFor(String language) {
        if (language.startsWith("Python") || language.startsWith("PyPy")) {
            return "import sys\n"
                + "\n"
                + "def main():\n"
                + "\t# code goes here...\n"
                + "\n"
                + "if __name__ == \"__main__\":\n"
                + "\tmain()\n";
        }
        if (language.startsWith("GNU G++") || language.startsWith("GNU C11") || language.startsWith("GNU G11")) {
            return "#include <bits/stdc++.h>\n"
                    + "using namespace std;\n"
                    + "\n"
                    + "int main() {\n"
                    + "\t// code goes here...\n"
                    + "\treturn 0;\n"
                    + "}\n";
        }
        if (language.startsWith("Java ")) {
            return "import java.io.*;\n"
                    + "import java.util.*;\n"
                    + "\n"
                    + "public class Main {\n"
                    + "\n"
                    + "\tpublic static void main(String[] args) throws Exception {\n"
                    + "\t\tScanner sc = new Scanner(System.in);\n"
                    + "\t\t// code goes here...\n"
                    + "\t\tsc.close();\n"
                    + "\t}\n"
                    + "\n"
                    + "}\n";
        }
        if (language.startsWith("Kotlin")) {
            return "fun main() {\n"
                    + "\t// code goes here...\n"
                    + "}\n";
        }
        if (language.startsWith("C#")) {
            return "using System;\n"
                    + "\n"
                    + "public class Program {\n"
                    + "\tpublic static void Main() {\n"
                    + "\t\t// code goes here...\n"
                    + "\t}\n"
                    + "}\n";
        }
        if (language.startsWith("Go")) {
            return "package main\n"
                    + "\n"
                    + "func main() {\n"
                    + "\t// code goes here...\n"
                    + "}\n";
        }
        if (language.startsWith("Rust")) {
            return "fn main() {\n"
                    + "\t// code goes here...\n"
                    + "}\n";
        }
        if (language.startsWith("Node.js") || language.startsWith("JavaScript")) {
            return "function main() {\n"
                    + "\t// code goes here...\n"
                    + "}\n"
                    + "\n"
                    + "main();\n";
        }
        if (language.startsWith("PHP")) {
            return "<?php\n"
                    + "// code goes here...\n";
        }
        if (language.startsWith("Ruby")) {
            return "def main\n"
                    + "\t# code goes here...\n"
                    + "end\n"
                    + "\n"
                    + "main\n";
        }
        if (language.startsWith("Perl")) {
            return "use strict;\n"
                    + "use warnings;\n"
                    + "\n"
                    + "# code goes here...\n";
        }
        if (language.startsWith("Haskell")) {
            return "main :: IO ()\n"
                    + "main = do\n"
                    + "\t-- code goes here...\n";
        }
        if (language.startsWith("OCaml")) {
            return "let () =\n"
                    + "\t(* code goes here... *)\n"
                    + "\t()\n";
        }
        if (language.startsWith("Scala")) {
            return "object Main {\n"
                    + "\tdef main(args: Array[String]): Unit = {\n"
                    + "\t\t// code goes here...\n"
                    + "\t}\n"
                    + "}\n";
        }
        if (language.startsWith("Pascal")) {
            return "program Main;\n"
                    + "begin\n"
                    + "\t// code goes here...\n"
                    + "end.\n";
        }
        return "// code goes here...\n";
    }

    private void persistSettings(JFrame frame) {
        int state = frame.getExtendedState();
        boolean maximized = (state & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;

        String language = DEFAULT_LANGUAGE;
        if (languageDropdown != null && languageDropdown.getSelectedItem() != null) {
            language = languageDropdown.getSelectedItem().toString();
        }

        int dividerLocation = contentSplitPane != null ? contentSplitPane.getDividerLocation() : 420;
        int testCasesDividerLocation = statementTestCasesSplitPane != null ? statementTestCasesSplitPane.getDividerLocation() : 420;

        AppSettings settings = new AppSettings(
                frame.getX(),
                frame.getY(),
                frame.getWidth(),
                frame.getHeight(),
                dividerLocation,
                testCasesDividerLocation,
                maximized,
                language);

        settingsRepository.save(settings);
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

    private void disableFocus(Component component) {
        component.setFocusable(false);
        if (component instanceof JComponent jComponent) {
            jComponent.setRequestFocusEnabled(false);
        }
    }
}
