package com.example;

import java.util.Map;

record RenderedProblemView(String html, Map<String, String> copyPayloads) {
}
