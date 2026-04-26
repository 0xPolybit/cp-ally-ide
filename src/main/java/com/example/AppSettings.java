package com.example;

record AppSettings(int x, int y, int width, int height, int dividerLocation, int testCasesDividerLocation, boolean maximized, String lastLanguage) {
    static AppSettings defaults(String defaultLanguage) {
        return new AppSettings(-1, -1, 1200, 760, 420, 420, false, defaultLanguage);
    }
}
