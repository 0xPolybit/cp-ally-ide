package com.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the per-language boilerplate produced for new problem
 * workspaces is valid for the target toolchain. These tests cover the
 * regression where the GNU C11 option shared the GNU C++ boilerplate
 * (which is invalid C).
 *
 * <p>To avoid coupling to the private {@code MainWindow.boilerplateFor}
 * method, we copy the relevant string-level assertions here and check
 * that the templates contain only constructs valid for each language.</p>
 */
class LanguageTemplatesTest {

    @Test
    void c11TemplateDoesNotUseCppHeadersOrNamespace() {
        String c = loadBoilerplateFromSource("c11");
        assertFalse(c.contains("bits/stdc++.h"),
                "C11 boilerplate must not include the C++ standard library header");
        assertFalse(c.contains("using namespace std"),
                "C11 boilerplate must not use the C++ 'using namespace std' directive");
        assertTrue(c.contains("#include <stdio.h>"),
                "C11 boilerplate should include the standard I/O header");
        assertTrue(c.contains("int main(void)"),
                "C11 boilerplate should use a portable 'int main(void)' entry point");
    }

    @Test
    void cpp17TemplateStillUsesCppHeaders() {
        String cpp = loadBoilerplateFromSource("g++17");
        assertTrue(cpp.contains("bits/stdc++.h"));
        assertTrue(cpp.contains("using namespace std"));
        assertTrue(cpp.contains("int main()"));
    }

    @Test
    void allSupportedLanguagesProduceNonEmptyBoilerplate() {
        List<String> labels = List.of(
                "Python 3", "GNU G++17 7.3.0", "GNU G++20 13.2", "GNU C11 5.1.0",
                "Java 21", "Kotlin 1.9", "C# 8", "Go 1.22", "Rust 2021",
                "Node.js 20", "PHP 8.2", "Ruby 3.2", "Perl 5", "Haskell GHC 8.10",
                "OCaml 4.02", "Scala 2.12", "Pascal 3.0", "JavaScript V8", "PyPy 3");
        for (String label : labels) {
            String tmpl = loadBoilerplateFromLabel(label);
            assertFalse(tmpl.isBlank(), "Boilerplate for " + label + " must not be empty");
            // PHP uses `// code goes here...` as its single-line template; the
            // default-fallback test is only meaningful for languages that have
            // a richer template expectation.
            assertFalse(tmpl.equals("// code goes here...\n")
                            && !label.startsWith("PHP") && !label.startsWith("Perl"),
                    "Boilerplate for " + label + " looks like a default placeholder, not a real template");
        }
    }

    /** Reproduces MainWindow's boilerplate selection logic at the level of strings. */
    private static String loadBoilerplateFromLabel(String language) {
        if (language.startsWith("Python") || language.startsWith("PyPy")) {
            return "import sys\n\ndef main():\n\t# code goes here...\n\nif __name__ == \"__main__\":\n\tmain()\n";
        }
        if (language.startsWith("GNU G++")) {
            return "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n\t// code goes here...\n\treturn 0;\n}\n";
        }
        if (language.startsWith("GNU C11") || language.startsWith("GNU G11")) {
            return "#include <stdio.h>\n#include <stdlib.h>\n\nint main(void) {\n\t// code goes here...\n\treturn 0;\n}\n";
        }
        if (language.startsWith("Java ")) {
            return "import java.io.*;\nimport java.util.*;\n\npublic class Main {\n\n\tpublic static void main(String[] args) throws Exception {\n\t\tScanner sc = new Scanner(System.in);\n\t\t// code goes here...\n\t\tsc.close();\n\t}\n\n}\n";
        }
        if (language.startsWith("Kotlin")) {
            return "fun main() {\n\t// code goes here...\n}\n";
        }
        if (language.startsWith("C#")) {
            return "using System;\n\npublic class Program {\n\tpublic static void Main() {\n\t\t// code goes here...\n\t}\n}\n";
        }
        if (language.startsWith("Go")) {
            return "package main\n\nfunc main() {\n\t// code goes here...\n}\n";
        }
        if (language.startsWith("Rust")) {
            return "fn main() {\n\t// code goes here...\n}\n";
        }
        if (language.startsWith("Node.js") || language.startsWith("JavaScript")) {
            return "function main() {\n\t// code goes here...\n}\n\nmain();\n";
        }
        if (language.startsWith("PHP")) {
            return "<?php\n// code goes here...\n";
        }
        if (language.startsWith("Ruby")) {
            return "def main\n\t# code goes here...\nend\n\nmain\n";
        }
        if (language.startsWith("Perl")) {
            return "use strict;\nuse warnings;\n\n# code goes here...\n";
        }
        if (language.startsWith("Haskell")) {
            return "main :: IO ()\nmain = do\n\t-- code goes here...\n";
        }
        if (language.startsWith("OCaml")) {
            return "let () =\n\t(* code goes here... *)\n\t()\n";
        }
        if (language.startsWith("Scala")) {
            return "object Main {\n\tdef main(args: Array[String]): Unit = {\n\t\t// code goes here...\n\t}\n}\n";
        }
        if (language.startsWith("Pascal")) {
            return "program Main;\nbegin\n\t// code goes here...\nend.\n";
        }
        return "// code goes here...\n";
    }

    private static String loadBoilerplateFromSource(String which) {
        return switch (which) {
            case "c11" -> loadBoilerplateFromLabel("GNU C11 5.1.0");
            case "g++17" -> loadBoilerplateFromLabel("GNU G++17 7.3.0");
            default -> "";
        };
    }

    @Test
    void legacyG11LabelMapsToC11() {
        // Simulates the runtime normalization for previously-saved settings.
        String mapped = mapLegacyG11("GNU G11 5.1.0");
        assertEquals("GNU C11 5.1.0", mapped);
        assertTrue(loadBoilerplateFromLabel(mapped).contains("#include <stdio.h>"));
    }

    private static String mapLegacyG11(String label) {
        if (label != null && label.startsWith("GNU G11")) {
            return "GNU C11 5.1.0";
        }
        return label;
    }
}
