package com.example;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import java.awt.Image;
import java.net.URL;

final class UiIconLoader {

    private UiIconLoader() {
    }

    static ImageIcon loadClasspathIcon(String resourcePath) {
        try {
            URL iconUrl = UiIconLoader.class.getResource(resourcePath);
            if (iconUrl == null) {
                return null;
            }

            ImageIcon icon = new ImageIcon(iconUrl);
            if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                return null;
            }
            return icon;
        } catch (Exception ignored) {
            return null;
        }
    }

    static ImageIcon loadScaledClasspathIcon(String resourcePath, int width, int height) {
        ImageIcon icon = loadClasspathIcon(resourcePath);
        if (icon == null) {
            return null;
        }

        if (icon.getIconWidth() == width && icon.getIconHeight() == height) {
            return icon;
        }

        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    static void applyWindowIcon(JFrame frame, String resourcePath) {
        if (frame == null) {
            return;
        }

        ImageIcon icon = loadClasspathIcon(resourcePath);
        if (icon != null) {
            frame.setIconImage(icon.getImage());
        }
    }
}