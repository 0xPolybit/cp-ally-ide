package com.example;

import java.nio.file.Path;

/**
 * Centralized cache directory layout. Splits what used to be a single
 * {@code <appData>/cache/} directory into three purpose-specific
 * subdirectories so that "clear source" never deletes rendered LaTeX
 * images, and vice versa.
 *
 * <pre>
 *   &lt;appData&gt;/
 *     problems/         (problem HTML cache, see ProblemCacheRepository)
 *     tests/            (custom test cases, see CustomTestRepository)
 *     cache/
 *       source/         (program source code, see ProgramCacheRepository)
 *       latex/          (rendered LaTeX images, see LatexImageRenderer)
 *       icons/          (scaled UI icons, see ProblemHtmlRenderer)
 * </pre>
 */
final class CacheNames {

    private static final String LEGACY_CACHE_DIR = "cache";
    private static final String SOURCE_DIR = "source";
    private static final String LATEX_DIR = "latex";
    private static final String ICONS_DIR = "icons";

    private CacheNames() {
    }

    static Path sourceDir(Path appDataDirectory) {
        return appDataDirectory.resolve(LEGACY_CACHE_DIR).resolve(SOURCE_DIR);
    }

    static Path latexDir(Path appDataDirectory) {
        return appDataDirectory.resolve(LEGACY_CACHE_DIR).resolve(LATEX_DIR);
    }

    static Path iconsDir(Path appDataDirectory) {
        return appDataDirectory.resolve(LEGACY_CACHE_DIR).resolve(ICONS_DIR);
    }

    static Path legacyCacheDir(Path appDataDirectory) {
        return appDataDirectory.resolve(LEGACY_CACHE_DIR);
    }
}
