package com.example;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.util.List;
import java.util.Map;

/** Stable metadata for one supported editor/execution language. */
record LanguageDefinition(String id, String displayName, String sourceFileName,
                          String syntaxStyle, boolean interpreted,
                          List<String> requiredCommands, String boilerplate) {

    private static final List<LanguageDefinition> DEFINITIONS = List.of(
            def("python3", "Python 3", "Main.py", SyntaxConstants.SYNTAX_STYLE_PYTHON, true, List.of("python"),
                    "import sys\n\ndef main():\n\t# code goes here...\n\nif __name__ == \"__main__\":\n\tmain()\n"),
            def("cpp17", "GNU G++17 7.3.0", "Main.cpp", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, false, List.of("g++"),
                    "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n\t// code goes here...\n\treturn 0;\n}\n"),
            def("cpp20", "GNU G++20 13.2", "Main.cpp", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, false, List.of("g++"),
                    "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n\t// code goes here...\n\treturn 0;\n}\n"),
            def("c11", "GNU C11 5.1.0", "Main.c", SyntaxConstants.SYNTAX_STYLE_C, false, List.of("gcc"),
                    "#include <stdio.h>\n#include <stdlib.h>\n\nint main(void) {\n\t// code goes here...\n\treturn 0;\n}\n"),
            def("java21", "Java 21", "Main.java", SyntaxConstants.SYNTAX_STYLE_JAVA, false, List.of("javac", "java"),
                    "import java.io.*;\nimport java.util.*;\n\npublic class Main {\n\tpublic static void main(String[] args) throws Exception {\n\t\tScanner sc = new Scanner(System.in);\n\t\t// code goes here...\n\t\tsc.close();\n\t}\n}\n"),
            def("kotlin", "Kotlin 1.9", "Main.kt", SyntaxConstants.SYNTAX_STYLE_KOTLIN, false, List.of("kotlinc", "java"), "fun main() {\n\t// code goes here...\n}\n"),
            def("csharp", "C# 8", "Program.cs", SyntaxConstants.SYNTAX_STYLE_CSHARP, false, List.of("csc"), "using System;\n\npublic class Program {\n\tpublic static void Main() {\n\t\t// code goes here...\n\t}\n}\n"),
            def("go", "Go 1.22", "Main.go", SyntaxConstants.SYNTAX_STYLE_GO, true, List.of("go"), "package main\n\nfunc main() {\n\t// code goes here...\n}\n"),
            def("rust", "Rust 2021", "Main.rs", SyntaxConstants.SYNTAX_STYLE_RUST, false, List.of("rustc"), "fn main() {\n\t// code goes here...\n}\n"),
            def("node", "Node.js 20", "Main.js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, true, List.of("node"), "function main() {\n\t// code goes here...\n}\n\nmain();\n"),
            def("javascript", "JavaScript V8", "Main.js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, true, List.of("node"), "function main() {\n\t// code goes here...\n}\n\nmain();\n"),
            def("php", "PHP 8.2", "Main.php", SyntaxConstants.SYNTAX_STYLE_PHP, true, List.of("php"), "<?php\n// code goes here...\n"),
            def("ruby", "Ruby 3.2", "Main.rb", SyntaxConstants.SYNTAX_STYLE_RUBY, true, List.of("ruby"), "def main\n\t# code goes here...\nend\n\nmain\n"),
            def("perl", "Perl 5", "Main.pl", SyntaxConstants.SYNTAX_STYLE_PERL, true, List.of("perl"), "use strict;\nuse warnings;\n\n# code goes here...\n"),
            def("scala", "Scala 2.12", "Main.scala", SyntaxConstants.SYNTAX_STYLE_SCALA, true, List.of("scala"), "object Main {\n\tdef main(args: Array[String]): Unit = {\n\t\t// code goes here...\n\t}\n}\n"),
            def("pypy3", "PyPy 3", "Main.py", SyntaxConstants.SYNTAX_STYLE_PYTHON, true, List.of("python3"), "import sys\n\ndef main():\n\t# code goes here...\n\nif __name__ == \"__main__\":\n\tmain()\n")
    );

    private static LanguageDefinition def(String id, String name, String file, String syntax,
                                          boolean interpreted, List<String> commands, String template) {
        return new LanguageDefinition(id, name, file, syntax, interpreted, commands, template);
    }

    static List<LanguageDefinition> all() { return DEFINITIONS; }

    static LanguageDefinition forDisplayName(String name) {
        if (name == null) return null;
        for (LanguageDefinition definition : DEFINITIONS) {
            if (definition.displayName().equalsIgnoreCase(name.trim())) return definition;
        }
        // Legacy label migration.
        if (name.startsWith("GNU G11")) return forDisplayName("GNU C11 5.1.0");
        return null;
    }

    static LanguageDefinition forId(String id) {
        if (id == null) return null;
        return DEFINITIONS.stream().filter(d -> d.id().equals(id)).findFirst().orElse(null);
    }
}
