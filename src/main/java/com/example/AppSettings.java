package com.example;

record AppSettings(int x, int y, int width, int height, int dividerLocation, int testCasesDividerLocation, boolean maximized, String lastLanguage, int editorFontSize, String editorColorScheme, String appTheme, boolean useTabsAsSpaces, int tabSpacing, boolean autosaveEnabled, int autosaveIntervalSeconds, String codeforcesUsername, double editorZoom, double problemZoom, boolean codeFolding, int runTimeoutSeconds, int maxOutputBytes) {
    AppSettings(int x, int y, int width, int height, int dividerLocation, int testCasesDividerLocation, boolean maximized, String lastLanguage, int editorFontSize, String editorColorScheme, String appTheme, boolean useTabsAsSpaces, int tabSpacing, boolean autosaveEnabled, int autosaveIntervalSeconds, String codeforcesUsername, double editorZoom, double problemZoom, boolean codeFolding) {
        this(x, y, width, height, dividerLocation, testCasesDividerLocation, maximized, lastLanguage, editorFontSize, editorColorScheme, appTheme, useTabsAsSpaces, tabSpacing, autosaveEnabled, autosaveIntervalSeconds, codeforcesUsername, editorZoom, problemZoom, codeFolding, 2, 1024 * 1024);
    }
    static AppSettings defaults(String defaultLanguage) {
        return new AppSettings(-1, -1, 1200, 760, 420, 420, false, defaultLanguage, 14, "Eclipse Dark", "Dark", false, 4, true, 10, "", 1.0, 1.0, true, 2, 1024 * 1024);
    }
}
