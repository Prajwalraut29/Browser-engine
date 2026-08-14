package org.example.browser.html;

import org.example.browser.dom.*;

import java.util.*;

public class SimpleHtmlParser {
    private String input;
    private int pos = 0;

    /**
     * Parses an HTML string and returns the root of the DOM tree.
     * The root is an artificial <root> element that wraps the whole document.
     */
    public DomNode parse(String html) {
        this.input = html;
        this.pos = 0;
        DomNode root = new ElementNode("root", Map.of());
        parseNodes(root);
        return root;
    }

    /**
     * Recursively fills parent with child nodes until a closing tag is reached.
     */
    private void parseNodes(DomNode parent) {
        while (pos < input.length()) {
            if (input.charAt(pos) == '<') {
                // Check for a closing tag – stop parsing children
                if (pos + 1 < input.length() && input.charAt(pos + 1) == '/') {
                    break;
                }
                // Opening tag
                pos++; // skip '<'
                StringBuilder tagName = new StringBuilder();
                while (pos < input.length() && !Character.isWhitespace(input.charAt(pos)) && input.charAt(pos) != '>') {
                    tagName.append(input.charAt(pos));
                    pos++;
                }
                String tag = tagName.toString().toLowerCase();
                Map<String, String> attrs = parseAttributes();
                boolean selfClosing = false;
                if (pos < input.length() && input.charAt(pos) == '/') {
                    selfClosing = true;
                    pos++;
                }
                if (pos < input.length() && input.charAt(pos) == '>') pos++;

                ElementNode element = new ElementNode(tag, attrs);
                element.setParent(parent);
                parent.getChildren().add(element);

                // If not self-closing and not a void element, parse its children
                if (!selfClosing && !isVoidElement(tag)) {
                    parseNodes(element);
                    // Expect a matching closing tag
                    if (pos < input.length() && input.startsWith("</" + tag, pos)) {
                        pos += 2 + tag.length();
                        while (pos < input.length() && input.charAt(pos) != '>') pos++;
                        if (pos < input.length()) pos++; // skip '>'
                    }
                }
            } else {
                // Text content
                int start = pos;
                while (pos < input.length() && input.charAt(pos) != '<') pos++;
                String text = input.substring(start, pos).replaceAll("\\s+", " ").trim();
                if (!text.isEmpty()) {
                    parent.getChildren().add(new TextNode(decodeEntities(text)));
                }
            }
        }
    }

    /**
     * Parses attributes inside a tag (e.g., id="main" class="foo").
     */
    private Map<String, String> parseAttributes() {
        Map<String, String> attrs = new HashMap<>();
        skipWhitespace();
        while (pos < input.length() && input.charAt(pos) != '>' && input.charAt(pos) != '/') {
            skipWhitespace();
            StringBuilder attrName = new StringBuilder();
            while (pos < input.length() && !Character.isWhitespace(input.charAt(pos)) && input.charAt(pos) != '=' && input.charAt(pos) != '>' && input.charAt(pos) != '/') {
                attrName.append(input.charAt(pos));
                pos++;
            }
            skipWhitespace();
            String value = "";
            if (pos < input.length() && input.charAt(pos) == '=') {
                pos++;
                skipWhitespace();
                char quote = 0;
                if (pos < input.length() && (input.charAt(pos) == '"' || input.charAt(pos) == '\'')) {
                    quote = input.charAt(pos);
                    pos++;
                }
                StringBuilder val = new StringBuilder();
                while (pos < input.length() && (quote != 0 ? input.charAt(pos) != quote : !Character.isWhitespace(input.charAt(pos)) && input.charAt(pos) != '>' && input.charAt(pos) != '/')) {
                    val.append(input.charAt(pos));
                    pos++;
                }
                if (quote != 0 && pos < input.length()) pos++; // skip closing quote
                value = decodeEntities(val.toString());
            }
            if (!attrName.isEmpty()) attrs.put(attrName.toString().toLowerCase(), value);
        }
        return attrs;
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
    }

    /** Decodes common HTML character entities in text (and attribute values). */
    private String decodeEntities(String s) {
        return s.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&middot;", "·")
                .replace("&mdash;", "—")
                .replace("&ndash;", "–")
                .replace("&hellip;", "…")
                .replace("&lsquo;", "‘")
                .replace("&rsquo;", "’")
                .replace("&ldquo;", "“")
                .replace("&rdquo;", "”")
                .replace("&amp;", "&"); // last: so "&amp;lt;" becomes "&lt;" not "<"
    }

    /**
     * HTML elements that never have closing tags.
     */
    private boolean isVoidElement(String tag) {
        return Set.of("br", "hr", "img", "input", "meta", "link").contains(tag);
    }
}