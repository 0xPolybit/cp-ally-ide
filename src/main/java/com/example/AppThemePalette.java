package com.example;

import java.awt.Color;

record AppThemePalette(
        String name,
        boolean lightTheme,
        Color frameBackground,
        Color panelBackground,
        Color surfaceBackground,
        Color surfaceRaised,
        Color borderColor,
        Color textColor,
        Color mutedTextColor,
        Color accentColor,
        Color accentForeground,
        Color successColor,
        Color warningColor,
        Color errorColor,
        Color inputBackground,
        Color inputForeground,
        Color titleBarBackground,
        Color titleBarForeground,
        Color gutterBackground,
        Color scrollbarTrack,
        Color scrollbarThumb,
        Color scrollbarThumbHover,
        Color scrollbarThumbPressed,
        Color selectionBackground) {

    /** Accent used for links, focus, selection, and primary actions. */
    Color linkColor() {
        return accentColor;
    }

    /** Informational notices should not look like success or error states. */
    Color infoColor() {
        return lightTheme ? new Color(37, 99, 235) : new Color(96, 165, 250);
    }

    Color disabledTextColor() {
        return lightTheme ? new Color(145, 154, 166) : new Color(112, 119, 131);
    }

    Color focusColor() {
        return accentColor;
    }

    Color subtleBorderColor() {
        return lightTheme ? new Color(226, 231, 238) : new Color(55, 59, 66);
    }

    static AppThemePalette fromName(String themeName) {
        String normalized = themeName == null ? "dark" : themeName.trim().toLowerCase();
        return switch (normalized) {
            case "light" -> light();
            case "ultra dark" -> ultraDark();
            default -> dark();
        };
    }

    static AppThemePalette light() {
        return new AppThemePalette(
                "Light",
                true,
                new Color(245, 247, 250),
                new Color(255, 255, 255),
                new Color(248, 250, 252),
                new Color(239, 243, 248),
                new Color(210, 218, 227),
                new Color(35, 40, 48),
                new Color(93, 103, 117),
                new Color(59, 130, 246),
                new Color(255, 255, 255),
                new Color(34, 139, 85),
                new Color(168, 108, 0),
                new Color(208, 66, 66),
                new Color(255, 255, 255),
                new Color(35, 40, 48),
                new Color(236, 240, 245),
                new Color(35, 40, 48),
                new Color(242, 245, 249),
                new Color(233, 238, 244),
                new Color(170, 179, 190),
                new Color(130, 140, 152),
                new Color(115, 126, 140),
                new Color(32, 142, 98, 60));
    }

    static AppThemePalette ultraLight() {
        return new AppThemePalette(
                "Ultra Light",
                true,
                new Color(252, 252, 253),
                new Color(255, 255, 255),
                new Color(250, 250, 252),
                new Color(244, 246, 250),
                new Color(225, 230, 236),
                new Color(28, 32, 38),
                new Color(96, 106, 120),
                new Color(37, 99, 235),
                new Color(255, 255, 255),
                new Color(31, 133, 78),
                new Color(176, 117, 0),
                new Color(201, 63, 63),
                new Color(255, 255, 255),
                new Color(28, 32, 38),
                new Color(242, 244, 247),
                new Color(28, 32, 38),
                new Color(247, 248, 251),
                new Color(236, 240, 245),
                new Color(174, 183, 194),
                new Color(138, 149, 162),
                new Color(120, 132, 145),
                new Color(18, 154, 112, 58));
    }

    static AppThemePalette lightContrast() {
        return new AppThemePalette(
                "Light Contrast",
                true,
                new Color(235, 239, 244),
                new Color(248, 250, 252),
                new Color(241, 245, 249),
                new Color(230, 236, 243),
                new Color(184, 195, 208),
                new Color(26, 31, 38),
                new Color(74, 83, 95),
                new Color(37, 99, 235),
                new Color(255, 255, 255),
                new Color(24, 124, 74),
                new Color(154, 101, 0),
                new Color(185, 53, 53),
                new Color(255, 255, 255),
                new Color(26, 31, 38),
                new Color(232, 237, 243),
                new Color(26, 31, 38),
                new Color(236, 241, 247),
                new Color(226, 232, 239),
                new Color(148, 160, 173),
                new Color(112, 124, 137),
                new Color(96, 109, 122),
                new Color(8, 135, 96, 60));
    }

    static AppThemePalette dark() {
        return new AppThemePalette(
                "Dark",
                false,
                new Color(30, 31, 34),
                new Color(43, 45, 48),
                new Color(50, 53, 58),
                new Color(60, 63, 68),
                new Color(67, 71, 76),
                new Color(223, 225, 229),
                new Color(169, 176, 188),
                new Color(86, 156, 214),
                new Color(10, 11, 14),
                new Color(97, 214, 110),
                new Color(246, 198, 67),
                new Color(246, 86, 86),
                new Color(50, 53, 58),
                new Color(223, 225, 229),
                new Color(43, 45, 48),
                new Color(230, 233, 238),
                new Color(36, 38, 41),
                new Color(36, 38, 41),
                new Color(80, 84, 90),
                new Color(96, 101, 108),
                new Color(110, 115, 122),
                new Color(55, 247, 19, 70));
    }

    static AppThemePalette ultraDark() {
        return new AppThemePalette(
                "Ultra Dark",
                false,
                new Color(18, 19, 22),
                new Color(26, 28, 31),
                new Color(31, 34, 38),
                new Color(38, 41, 46),
                new Color(50, 54, 60),
                new Color(235, 237, 240),
                new Color(148, 156, 167),
                new Color(96, 165, 250),
                new Color(10, 11, 14),
                new Color(97, 214, 110),
                new Color(246, 198, 67),
                new Color(240, 89, 89),
                new Color(31, 34, 38),
                new Color(235, 237, 240),
                new Color(26, 28, 31),
                new Color(242, 244, 246),
                new Color(27, 29, 33),
                new Color(27, 29, 33),
                new Color(74, 79, 85),
                new Color(94, 99, 106),
                new Color(107, 113, 120),
                new Color(69, 219, 78, 66));
    }

    static AppThemePalette darkContrast() {
        return new AppThemePalette(
                "Dark Contrast",
                false,
                new Color(22, 24, 28),
                new Color(34, 37, 42),
                new Color(40, 44, 50),
                new Color(52, 56, 62),
                new Color(77, 82, 89),
                new Color(240, 242, 245),
                new Color(176, 184, 194),
                new Color(96, 165, 250),
                new Color(9, 10, 12),
                new Color(108, 232, 121),
                new Color(248, 206, 82),
                new Color(255, 100, 100),
                new Color(40, 44, 50),
                new Color(240, 242, 245),
                new Color(34, 37, 42),
                new Color(245, 246, 248),
                new Color(30, 32, 36),
                new Color(30, 32, 36),
                new Color(90, 96, 103),
                new Color(112, 118, 126),
                new Color(127, 134, 142),
                new Color(79, 255, 32, 72));
    }
}