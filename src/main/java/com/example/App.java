package com.example;

import javax.swing.SwingUtilities;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SplashScreenWindow splashScreenWindow = new SplashScreenWindow();
            splashScreenWindow.showSplash();

            Thread startupThread = new Thread(() -> {
                MainWindow mainWindow = new MainWindow();
                splashScreenWindow.sleepUntilMinimumDuration(3000L);
                splashScreenWindow.closeSplash();
                splashScreenWindow.sleepSilently(500L);
                SwingUtilities.invokeLater(mainWindow::showWindow);
            }, "main-window-startup");
            startupThread.start();
        });
    }
}
