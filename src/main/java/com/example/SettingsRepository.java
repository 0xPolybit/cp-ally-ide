package com.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

class SettingsRepository {

    private final String settingsDirName;
    private final String settingsFileName;
    private final String defaultLanguage;

    SettingsRepository(String settingsDirName, String settingsFileName, String defaultLanguage) {
        this.settingsDirName = settingsDirName;
        this.settingsFileName = settingsFileName;
        this.defaultLanguage = defaultLanguage;
    }

    AppSettings load() {
        Path settingsFile = getSettingsFilePath();
        try {
            Files.createDirectories(settingsFile.getParent());
            if (!Files.exists(settingsFile)) {
                AppSettings defaults = AppSettings.defaults(defaultLanguage);
                save(defaults);
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
            String language = properties.getProperty("language.last", defaultLanguage);

            return new AppSettings(x, y, width, height, divider, maximized, language);
        } catch (IOException e) {
            return AppSettings.defaults(defaultLanguage);
        }
    }

    void save(AppSettings settings) {
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

    Path getAppDataDirectory() {
        return getSettingsFilePath().getParent();
    }

    private Path getSettingsFilePath() {
        String appData = System.getenv("APPDATA");
        Path basePath;
        if (appData != null && !appData.isBlank()) {
            basePath = Path.of(appData);
        } else {
            basePath = Path.of(System.getProperty("user.home"), "AppData", "Roaming");
        }
        return basePath.resolve(settingsDirName).resolve(settingsFileName);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
