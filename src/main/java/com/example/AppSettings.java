package com.example;

record AppSettings(
        int x,
        int y,
        int width,
        int height,
        int dividerLocation,
        int testCasesDividerLocation,
        boolean maximized,
        String lastLanguage,
        int editorFontSize,
        String editorColorScheme,
        String appTheme,
        boolean useTabsAsSpaces,
        int tabSpacing,
        boolean autosaveEnabled,
        int autosaveIntervalSeconds,
        String codeforcesUsername,
        boolean bottomToolOpen,
        int bottomToolHeight,
        String bottomToolTab) {
    static AppSettings defaults(String defaultLanguage) {
        return new AppSettings(
                -1, -1, 1200, 760, 420, 420, false,
                defaultLanguage, 14, "Eclipse Dark", "Dark",
                false, 4, true, 10, "",
                true, 280, "tests");
    }
}
