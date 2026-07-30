package org.example.browser;

import org.example.browser.html.SimpleHtmlParser;
import org.example.browser.css.*;
import org.example.browser.style.StyleResolver;
import org.example.browser.dom.*;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        String html = "<html>\n" +
                "            <head><style>\n" +
                "                div { display: block; background: white; margin: 10px; }\n" +
                "                .box { width: 200px; background: lightblue; }\n" +
                "                #main { margin: 20px; background: #f9f9f9; }\n" +
                "            </style></head>\n" +
                "            <body>\n" +
                "                <div id=\"main\">\n" +
                "                    <p>Hello, <span style=\"color: red;\">world</span>!</p>\n" +
                "                    <div class=\"box\">A box</div>\n" +
                "                </div>\n" +
                "            </body>\n" +
                "            </html>";

        SimpleHtmlParser htmlParser = new SimpleHtmlParser();
        DomNode document = htmlParser.parse(html);

        // 2. Extract CSS from <style> elements
        String css = extractCss(document);
        System.out.println("=== Extracted CSS ===");
        System.out.println(css);

        // 3. Parse CSS into rules
        SimpleCssParser cssParser = new SimpleCssParser();
        List<CssRule> rules = cssParser.parse(css);
        System.out.println("\n=== Parsed " + rules.size() + " CSS rule(s) ===");

        // 4. Resolve styles for all elements
        StyleResolver resolver = new StyleResolver(rules);
        resolver.resolveTree(document, null);

        // 5. Print the DOM tree with computed styles
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
