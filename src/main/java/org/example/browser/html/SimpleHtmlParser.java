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
                // Comment / DOCTYPE / CDATA / other <!...> declarations
                if (pos + 1 < input.length() && input.charAt(pos + 1) == '!') {
                    if (input.startsWith("<!--", pos)) {
                        pos = skipComment(pos);
                        continue;
                    }
                    if (input.regionMatches(true, pos, "<![CDATA[", 0, 9)) {
                        int end = input.indexOf("]]>", pos + 9);
                        if (end < 0) end = input.length();
                        addTextNode(parent, input.substring(pos + 9, end), false);
                        pos = (end >= input.length()) ? end : end + 3;
                        continue;
                    }
                    // <!DOCTYPE html>, <!ENTITY ...>, etc. – skip the declaration
                    skipToGt();
                    continue;
                }
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
                addTextNode(parent, input.substring(start, pos), true);
            }
        }
    }

    /**
     * Skips past a <!-- comment --> starting at the given position.
     */
    private int skipComment(int commentStart) {
        int end = input.indexOf("-->", commentStart);
        return (end < 0) ? input.length() : end + 3;
    }

    /**
     * Skips the current <!...> declaration up to and including its closing '>'.
     */
    private void skipToGt() {
        while (pos < input.length() && input.charAt(pos) != '>') pos++;
        if (pos < input.length()) pos++;
    }

    /**
     * Normalizes whitespace and (optionally) decodes entities, then appends a text node.
     */
    private void addTextNode(DomNode parent, String raw, boolean decodeEntities) {
        String text = raw.replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) return;
        parent.getChildren().add(new TextNode(decodeEntities ? decodeEntities(text) : text));
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

    /**
     * Decodes character references (named entities and numeric &#NN; / &#xNN; forms)
     * in a single pass. Unknown or malformed references are left as-is.
     */
    private String decodeEntities(String s) {
        if (s.indexOf('&') < 0) return s;
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != '&') {
                out.append(c);
                i++;
                continue;
            }
            int semi = s.indexOf(';', i);
            int end = (semi < 0) ? s.length() : semi;
            if (end - i <= 32) {
                String decoded = resolveEntity(s.substring(i + 1, end));
                if (decoded != null) {
                    out.append(decoded);
                    i = end + 1;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** Resolves a reference body (without the '&' and ';') to its decoded value, or null. */
    private String resolveEntity(String name) {
        if (name.startsWith("#x") || name.startsWith("#X")) {
            try {
                int code = Integer.parseInt(name.substring(2), 16);
                return String.valueOf(Character.toChars(code));
            } catch (RuntimeException e) {
                return null;
            }
        }
        if (name.startsWith("#")) {
            try {
                int code = Integer.parseInt(name.substring(1));
                return String.valueOf(Character.toChars(code));
            } catch (RuntimeException e) {
                return null;
            }
        }
        return NAMED_ENTITIES.get(name);
    }

    /**
     * HTML elements that never have closing tags.
     */
    private boolean isVoidElement(String tag) {
        return Set.of("br", "hr", "img", "input", "meta", "link").contains(tag);
    }

    /** Common named character references (name without the '&' and ';'). */
    private static final Map<String, String> NAMED_ENTITIES = Map.ofEntries(
            Map.entry("amp", "&"),
            Map.entry("lt", "<"),
            Map.entry("gt", ">"),
            Map.entry("quot", "\""),
            Map.entry("apos", "'"),
            Map.entry("nbsp", "\u00A0"),
            Map.entry("iexcl", "\u00A1"),
            Map.entry("cent", "\u00A2"),
            Map.entry("pound", "\u00A3"),
            Map.entry("curren", "\u00A4"),
            Map.entry("yen", "\u00A5"),
            Map.entry("brvbar", "\u00A6"),
            Map.entry("sect", "\u00A7"),
            Map.entry("uml", "\u00A8"),
            Map.entry("copy", "\u00A9"),
            Map.entry("ordf", "\u00AA"),
            Map.entry("laquo", "\u00AB"),
            Map.entry("not", "\u00AC"),
            Map.entry("shy", "\u00AD"),
            Map.entry("reg", "\u00AE"),
            Map.entry("macr", "\u00AF"),
            Map.entry("deg", "\u00B0"),
            Map.entry("plusmn", "\u00B1"),
            Map.entry("sup2", "\u00B2"),
            Map.entry("sup3", "\u00B3"),
            Map.entry("acute", "\u00B4"),
            Map.entry("micro", "\u00B5"),
            Map.entry("para", "\u00B6"),
            Map.entry("middot", "\u00B7"),
            Map.entry("cedil", "\u00B8"),
            Map.entry("sup1", "\u00B9"),
            Map.entry("ordm", "\u00BA"),
            Map.entry("raquo", "\u00BB"),
            Map.entry("frac14", "\u00BC"),
            Map.entry("frac12", "\u00BD"),
            Map.entry("frac34", "\u00BE"),
            Map.entry("iquest", "\u00BF"),
            Map.entry("Agrave", "\u00C0"),
            Map.entry("Aacute", "\u00C1"),
            Map.entry("Acirc", "\u00C2"),
            Map.entry("Atilde", "\u00C3"),
            Map.entry("Auml", "\u00C4"),
            Map.entry("Aring", "\u00C5"),
            Map.entry("AElig", "\u00C6"),
            Map.entry("Ccedil", "\u00C7"),
            Map.entry("Egrave", "\u00C8"),
            Map.entry("Eacute", "\u00C9"),
            Map.entry("Ecirc", "\u00CA"),
            Map.entry("Euml", "\u00CB"),
            Map.entry("Igrave", "\u00CC"),
            Map.entry("Iacute", "\u00CD"),
            Map.entry("Icirc", "\u00CE"),
            Map.entry("Iuml", "\u00CF"),
            Map.entry("ETH", "\u00D0"),
            Map.entry("Ntilde", "\u00D1"),
            Map.entry("Ograve", "\u00D2"),
            Map.entry("Oacute", "\u00D3"),
            Map.entry("Ocirc", "\u00D4"),
            Map.entry("Otilde", "\u00D5"),
            Map.entry("Ouml", "\u00D6"),
            Map.entry("times", "\u00D7"),
            Map.entry("Oslash", "\u00D8"),
            Map.entry("Ugrave", "\u00D9"),
            Map.entry("Uacute", "\u00DA"),
            Map.entry("Ucirc", "\u00DB"),
            Map.entry("Uuml", "\u00DC"),
            Map.entry("Yacute", "\u00DD"),
            Map.entry("THORN", "\u00DE"),
            Map.entry("szlig", "\u00DF"),
            Map.entry("agrave", "\u00E0"),
            Map.entry("aacute", "\u00E1"),
            Map.entry("acirc", "\u00E2"),
            Map.entry("atilde", "\u00E3"),
            Map.entry("auml", "\u00E4"),
            Map.entry("aring", "\u00E5"),
            Map.entry("aelig", "\u00E6"),
            Map.entry("ccedil", "\u00E7"),
            Map.entry("egrave", "\u00E8"),
            Map.entry("eacute", "\u00E9"),
            Map.entry("ecirc", "\u00EA"),
            Map.entry("euml", "\u00EB"),
            Map.entry("igrave", "\u00EC"),
            Map.entry("iacute", "\u00ED"),
            Map.entry("icirc", "\u00EE"),
            Map.entry("iuml", "\u00EF"),
            Map.entry("eth", "\u00F0"),
            Map.entry("ntilde", "\u00F1"),
            Map.entry("ograve", "\u00F2"),
            Map.entry("oacute", "\u00F3"),
            Map.entry("ocirc", "\u00F4"),
            Map.entry("otilde", "\u00F5"),
            Map.entry("ouml", "\u00F6"),
            Map.entry("divide", "\u00F7"),
            Map.entry("oslash", "\u00F8"),
            Map.entry("ugrave", "\u00F9"),
            Map.entry("uacute", "\u00FA"),
            Map.entry("ucirc", "\u00FB"),
            Map.entry("uuml", "\u00FC"),
            Map.entry("yacute", "\u00FD"),
            Map.entry("thorn", "\u00FE"),
            Map.entry("yuml", "\u00FF"),
            Map.entry("OElig", "\u0152"),
            Map.entry("oelig", "\u0153"),
            Map.entry("Scaron", "\u0160"),
            Map.entry("scaron", "\u0161"),
            Map.entry("Yuml", "\u0178"),
            Map.entry("fnof", "\u0192"),
            Map.entry("circ", "\u02C6"),
            Map.entry("tilde", "\u02DC"),
            Map.entry("ensp", "\u2002"),
            Map.entry("emsp", "\u2003"),
            Map.entry("thinsp", "\u2009"),
            Map.entry("zwnj", "\u200C"),
            Map.entry("zwj", "\u200D"),
            Map.entry("lrm", "\u200E"),
            Map.entry("rlm", "\u200F"),
            Map.entry("ndash", "\u2013"),
            Map.entry("mdash", "\u2014"),
            Map.entry("lsquo", "\u2018"),
            Map.entry("rsquo", "\u2019"),
            Map.entry("sbquo", "\u201A"),
            Map.entry("ldquo", "\u201C"),
            Map.entry("rdquo", "\u201D"),
            Map.entry("bdquo", "\u201E"),
            Map.entry("dagger", "\u2020"),
            Map.entry("Dagger", "\u2021"),
            Map.entry("bull", "\u2022"),
            Map.entry("hellip", "\u2026"),
            Map.entry("permil", "\u2030"),
            Map.entry("prime", "\u2032"),
            Map.entry("Prime", "\u2033"),
            Map.entry("lsaquo", "\u2039"),
            Map.entry("rsaquo", "\u203A"),
            Map.entry("oline", "\u203E"),
            Map.entry("frasl", "\u2044"),
            Map.entry("euro", "\u20AC"),
            Map.entry("larr", "\u2190"),
            Map.entry("uarr", "\u2191"),
            Map.entry("rarr", "\u2192"),
            Map.entry("darr", "\u2193"),
            Map.entry("harr", "\u2194"),
            Map.entry("minus", "\u2212"),
            Map.entry("infin", "\u221E"),
            Map.entry("radic", "\u221A"),
            Map.entry("int", "\u222B"),
            Map.entry("and", "\u2227"),
            Map.entry("or", "\u2228"),
            Map.entry("le", "\u2264"),
            Map.entry("ge", "\u2265"),
            Map.entry("ne", "\u2260"),
            Map.entry("sum", "\u2211"),
            Map.entry("prod", "\u220F"),
            Map.entry("pi", "\u03C0"),
            Map.entry("alpha", "\u03B1"),
            Map.entry("beta", "\u03B2"),
            Map.entry("gamma", "\u03B3"),
            Map.entry("Delta", "\u0394"),
            Map.entry("Sigma", "\u03A3"),
            Map.entry("tau", "\u03C4"),
            Map.entry("omega", "\u03C9"),
            Map.entry("trade", "\u2122"),
            Map.entry("loz", "\u25CA"),
            Map.entry("spades", "\u2660"),
            Map.entry("clubs", "\u2663"),
            Map.entry("hearts", "\u2665"),
            Map.entry("diams", "\u2666")
    );
}
