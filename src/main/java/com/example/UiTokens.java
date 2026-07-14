package com.example;

import java.awt.Dimension;

/**
 * Shared geometry and typography values for the Swing UI.
 *
 * Keep visual constants here so a future density/accessibility setting can
 * change the interface consistently instead of requiring scattered edits.
 */
final class UiTokens {

    private UiTokens() {
    }

    // Spacing scale
    static final int SPACE_1 = 4;
    static final int SPACE_2 = 8;
    static final int SPACE_3 = 12;
    static final int SPACE_4 = 16;
    static final int SPACE_5 = 24;
    static final int SPACE_6 = 32;

    // Common controls and icons
    static final int CONTROL_HEIGHT = 32;
    static final int CONTROL_HEIGHT_LARGE = 38;
    static final int COMPACT_CONTROL_HEIGHT = 28;
    static final int SPINNER_WIDTH = 90;
    static final int ICON_SMALL = 16;
    static final int ICON_NORMAL = 20;
    static final int ICON_LARGE = 24;
    static final int DIVIDER_SIZE = 8;

    // Primary window constraints
    static final int MIN_WINDOW_WIDTH = 1000;
    static final int MIN_WINDOW_HEIGHT = 680;
    static final int MIN_LEFT_PANEL_WIDTH = 280;
    static final int MIN_RIGHT_PANEL_WIDTH = 420;
    static final int PROBLEM_FIELD_WIDTH = 280;

    // Typography roles in points. Code areas use their own configured size.
    static final float TITLE_FONT_SIZE = 17f;
    static final float SECTION_FONT_SIZE = 15f;
    static final float BODY_FONT_SIZE = 13f;
    static final float CAPTION_FONT_SIZE = 12f;
    static final float SMALL_CAPTION_FONT_SIZE = 11f;

    static Dimension controlSize() {
        return new Dimension(PROBLEM_FIELD_WIDTH, CONTROL_HEIGHT);
    }

    static Dimension compactIconButtonSize() {
        return new Dimension(ICON_NORMAL + SPACE_3, ICON_NORMAL + SPACE_2);
    }
}
