package com.example;

import com.formdev.flatlaf.FlatLightLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class App {
    public static void main(String[] args) {
        FlatLightLaf.setup();

        RSyntaxTextArea editor = new RSyntaxTextArea();
        editor.setSyntaxEditingStyle("text/java");

        System.out.println("Hello, world from Maven + FlatLaf + RSyntaxTextArea!");
    }
}
