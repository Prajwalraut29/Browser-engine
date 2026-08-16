package org.example.browser.style;

import org.example.browser.css.SimpleCssParser;
import org.example.browser.dom.DomNode;
import org.example.browser.dom.ElementNode;
import org.example.browser.html.SimpleHtmlParser;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class StyleResolverTest {

    private final SimpleHtmlParser htmlParser = new SimpleHtmlParser();
    private final SimpleCssParser cssParser = new SimpleCssParser();

    @Test
    public void appliesBrowserDefaults() {
        assertEquals("block", styleOf("<div></div>", "div").get("display"));
        assertEquals("inline", styleOf("<span></span>", "span").get("display"));
        assertEquals("black", styleOf("<p></p>", "p").get("color"));
        assertEquals("16px", styleOf("<p></p>", "p").get("font-size"));
    }

    @Test
    public void appliesTagRule() {
        assertEquals("red", styleOf("<p></p>", "p", "p { color: red; }").get("color"));
    }

    @Test
    public void appliesClassRule() {
        assertEquals("green", styleOf("<div class=\"box\"></div>", "div", ".box { color: green; }").get("color"));
    }

    @Test
    public void idBeatsClass() {
        Map<String, String> style = styleOf("<div id=\"main\" class=\"box\"></div>", "div",
                "#main { color: red; } .box { color: blue; }");
        assertEquals("red", style.get("color"));
    }

    @Test
    public void classBeatsTag() {
        assertEquals("red", styleOf("<div class=\"box\"></div>", "div",
                "div { color: blue; } div.box { color: red; }").get("color"));
    }

    @Test
    public void laterSourceOrderWinsOnTie() {
        assertEquals("blue", styleOf("<p></p>", "p",
                "p { color: red; } p { color: blue; }").get("color"));
    }

    @Test
    public void inlineStyleBeatsRules() {
        assertEquals("green", styleOf("<p style=\"color: green\"></p>", "p",
                "p { color: red; }").get("color"));
    }

    @Test
    public void importantRuleBeatsNormalInline() {
        assertEquals("green", styleOf("<p style=\"color: red\"></p>", "p",
                "p { color: green !important; }").get("color"));
    }

    @Test
    public void importantInlineBeatsEverything() {
        assertEquals("green", styleOf("<p style=\"color: green !important\"></p>", "p",
                "p { color: red !important; }").get("color"));
    }

    @Test
    public void inheritsFromParent() {
        Map<String, String> span = styleOf("<div><span></span></div>", "span",
                "div { color: blue; font-size: 20px; }");
        assertEquals("blue", span.get("color"));
        assertEquals("20px", span.get("font-size"));
    }

    @Test
    public void descendantCombinatorMatchesAnyAncestor() {
        assertEquals("red", styleOf("<div><section><p></p></section></div>", "p",
                "div p { color: red; }").get("color"));
    }

    @Test
    public void childCombinatorRequiresDirectParent() {
        assertEquals("red", styleOf("<div><p></p></div>", "p",
                "div > p { color: red; }").get("color"));
        assertEquals("black", styleOf("<div><section><p></p></section></div>", "p",
                "div > p { color: red; }").get("color"));
    }

    @Test
    public void adjacentSiblingCombinator() {
        DomNode root = resolve("<h2></h2><p></p><p></p>", "h2 + p { color: red; }");
        List<ElementNode> ps = findAll(root, "p");
        assertEquals("red", ps.get(0).getComputedStyle().get("color"));
        assertEquals("black", ps.get(1).getComputedStyle().get("color"));
    }

    @Test
    public void nthChildMatchesEvenItems() {
        DomNode root = resolve("<ul><li>1</li><li>2</li><li>3</li><li>4</li></ul>",
                "li:nth-child(2n) { color: red; }");
        List<ElementNode> lis = findAll(root, "li");
        assertEquals("black", lis.get(0).getComputedStyle().get("color"));
        assertEquals("red", lis.get(1).getComputedStyle().get("color"));
        assertEquals("black", lis.get(2).getComputedStyle().get("color"));
        assertEquals("red", lis.get(3).getComputedStyle().get("color"));
    }

    @Test
    public void firstChildPseudoClass() {
        assertEquals("red", styleOf("<div><p></p><span></span></div>", "p",
                "p:first-child { color: red; }").get("color"));
    }

    @Test
    public void notPseudoClassNegates() {
        DomNode root = resolve("<p class=\"x\"></p><p></p>", "p:not(.x) { color: red; }");
        List<ElementNode> ps = findAll(root, "p");
        assertEquals("black", ps.get(0).getComputedStyle().get("color"));
        assertEquals("red", ps.get(1).getComputedStyle().get("color"));
    }

    @Test
    public void universalSelectorMatchesAll() {
        assertEquals("red", styleOf("<div><p></p></div>", "p", "* { color: red; }").get("color"));
    }

    @Test
    public void nonMatchingRuleLeavesDefaultUntouched() {
        assertEquals("16px", styleOf("<p></p>", "p", "p { color: red; }").get("font-size"));
    }

    // -------- helpers ----------------------------------------------------

    private DomNode resolve(String html, String css) {
        DomNode root = htmlParser.parse(html);
        new StyleResolver(cssParser.parse(css)).resolveTree(root, null);
        return root;
    }

    private Map<String, String> styleOf(String html, String tag) {
        return styleOf(html, tag, "");
    }

    private Map<String, String> styleOf(String html, String tag, String css) {
        return findFirst(resolve(html, css), tag).getComputedStyle();
    }

    private ElementNode findFirst(DomNode node, String tag) {
        List<ElementNode> all = findAll(node, tag);
        return all.isEmpty() ? null : all.get(0);
    }

    private List<ElementNode> findAll(DomNode node, String tag) {
        List<ElementNode> out = new ArrayList<>();
        if (node.isElement()) {
            ElementNode el = (ElementNode) node;
            if (tag.equals(el.getNodeName())) out.add(el);
            for (DomNode child : el.getChildren()) out.addAll(findAll(child, tag));
        }
        return out;
    }
}
