package org.example.browser;

import org.example.browser.html.SimpleHtmlParser;
import org.example.browser.css.*;
import org.example.browser.style.StyleResolver;
import org.example.browser.dom.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {

        // 1. Load the HTML file (from a filesystem path or the bundled default)
        String html = loadText(args.length > 0 ? args[0] : "index.html");

        SimpleHtmlParser htmlParser = new SimpleHtmlParser();
        DomNode document = htmlParser.parse(html);

        // 2. Extract CSS from <style> elements
        String css = extractCss(document);

        // 3. Merge external CSS file after the embedded <style> CSS
        String externalCss = loadText(args.length > 1 ? args[1] : "style.css");
        css = css + "\n" + externalCss;
        System.out.println("=== Extracted CSS ===");
        System.out.println(css);

        // 4. Parse CSS into rules
        SimpleCssParser cssParser = new SimpleCssParser();
        List<CssRule> rules = cssParser.parse(css);
        System.out.println("\n=== Parsed " + rules.size() + " CSS rule(s) ===");

        // 5. Resolve styles for all elements
        StyleResolver resolver = new StyleResolver(rules);
        resolver.resolveTree(document, null);

        // 6. Print the DOM tree with computed styles
        System.out.println("\n=== DOM tree with computed styles ===");
        printTree(document, 0);

    }

    // Simple recursive pretty‑printer
    private static void printTree(DomNode node, int indent) {
        String prefix = "  ".repeat(indent);
        if (node.isElement()) {
            ElementNode el = (ElementNode) node;
            Map<String, String> style = el.getComputedStyle();
            System.out.println(prefix + "<" + el.getNodeName() + ">" +
                    "  /* computed style: " + style + " */");
            for (DomNode child : el.getChildren()) {
                printTree(child, indent + 1);
            }
            System.out.println(prefix + "</" + el.getNodeName() + ">");
        } else {
            System.out.println(prefix + "\"" + node.getTextContent() + "\"");
        }
    }

    // Reads a file from the filesystem if it exists, otherwise from the
    // classpath resources (bundled defaults).
    private static String loadText(String path) throws IOException {
        if (Files.exists(Path.of(path))) {
            return Files.readString(Path.of(path));
        }
        try (InputStream in = Main.class.getResourceAsStream("/" + path)) {
            if (in == null) {
                throw new IOException("File not found on disk or in resources: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extractCss(DomNode node) {
        StringBuilder sb = new StringBuilder();
        extractCssRecursive(node, sb);
        return sb.toString();
    }

    private static void extractCssRecursive(DomNode node, StringBuilder sb) {
        if (node.isElement()) {
            ElementNode el = (ElementNode) node;
            if ("style".equals(el.getNodeName())) {
                for (DomNode child : el.getChildren()) {
                    if (!child.isElement())
                        sb.append(child.getTextContent());
                }
            }
        }
        for (DomNode child : node.getChildren()) {
            extractCssRecursive(child, sb);
        }
    }

}
