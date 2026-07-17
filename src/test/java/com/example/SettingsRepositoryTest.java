package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void missingFileLoadsDefaultsAndCreatesTheFile() {
        Path settingsFile = temporaryDirectory.resolve("settings.properties");
        SettingsRepository repository = new SettingsRepository(settingsFile, "Python 3");

        AppSettings settings = repository.load();

        assertEquals(AppSettings.defaults("Python 3"), settings);
        assertTrue(Files.exists(settingsFile));
    }

    @Test
    void savesAndLoadsAllSettingsFields() {
        Path settingsFile = temporaryDirectory.resolve("settings.properties");
        SettingsRepository repository = new SettingsRepository(settingsFile, "Python 3");
        AppSettings expected = new AppSettings(
                11, 22, 1400, 900, 500, 600, true,
                "Java 21", 18, "Dracula Dark", "Ultra Dark",
                true, 2, false, 45, "tourist", false, 320, "results");

        repository.save(expected);

        assertEquals(expected, repository.load());
    }

    @Test
    void malformedNumericValuesUseSafeFallbacks() throws Exception {
        Path settingsFile = temporaryDirectory.resolve("settings.properties");
        Properties properties = new Properties();
        properties.setProperty("window.width", "not-a-number");
        properties.setProperty("editor.fontSize", "also-invalid");
        properties.setProperty("language.last", "Java 21");
        try (var output = Files.newOutputStream(settingsFile)) {
            properties.store(output, "test");
        }

        AppSettings settings = new SettingsRepository(settingsFile, "Python 3").load();

        assertEquals(1200, settings.width());
        assertEquals(14, settings.editorFontSize());
        assertEquals("Java 21", settings.lastLanguage());
        assertFalse(settings.maximized());
    }
}
