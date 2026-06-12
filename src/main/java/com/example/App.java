package com.example;

import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SplashScreenWindow splashScreenWindow = new SplashScreenWindow();
            splashScreenWindow.showSplash();

            Thread startupThread = new Thread(() -> {
                try {
                    SettingsRepository tempSettings = new SettingsRepository("CompetitiveProgrammingAlly", "settings.properties", "Python 3");
                    DiagnosticLogger.initialize(tempSettings.getAppDataDirectory());
                    DiagnosticLogger.info("App starting up...");

                    MainWindow mainWindow = new MainWindow();
                    splashScreenWindow.sleepUntilMinimumDuration(3000L);
                    splashScreenWindow.closeSplash();
                    splashScreenWindow.sleepSilently(500L);
                    SwingUtilities.invokeLater(mainWindow::showWindow);
                } catch (Throwable t) {
                    DiagnosticLogger.error("Fatal exception during startup", t);
                }
            }, "main-window-startup");
            startupThread.start();
        });
    }
}
