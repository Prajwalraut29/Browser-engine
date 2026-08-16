package org.example.browser.html;

import org.example.browser.dom.DomNode;
import org.example.browser.dom.ElementNode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SimpleHtmlParserTest {

    private final SimpleHtmlParser parser = new SimpleHtmlParser();

    @Test
    public void parsesBasicNesting() {
        DomNode root = parser.parse("<div><p>hello</p></div>");
        ElementNode div = findFirst(root, "div");
        assertEquals(1, div.getChildren().size());
        ElementNode p = findFirst(div, "p");
        assertEquals("hello", p.getChildren().get(0).getTextContent());
    }

    @Test
    public void parsesAttributes() {
        DomNode root = parser.parse("<a href=\"https://x.com\" id='link' data-x=1></a>");
        ElementNode a = findFirst(root, "a");
        assertEquals("https://x.com", a.getAttributes().get("href"));
        assertEquals("link", a.getAttributes().get("id"));
        assertEquals("1", a.getAttributes().get("data-x"));
    }

    @Test
    public void lowercasesTagNamesAndAttributeNames() {
        DomNode root = parser.parse("<DIV ID=\"Main\"></DIV>");
        ElementNode div = findFirst(root, "div");
        assertEquals("Main", div.getAttributes().get("id"));
    }

    @Test
    public void voidElementsHaveNoChildrenAndDontSwallowSiblings() {
        DomNode root = parser.parse("<div>a<br>b<hr><img src=\"x.png\"></div>");
        ElementNode div = findFirst(root, "div");
        assertTrue(findFirst(root, "br").getChildren().isEmpty());
        assertTrue(findFirst(root, "img").getChildren().isEmpty());
        assertTrue(findFirst(root, "hr").getChildren().isEmpty());
        assertTrue(div.getChildren().stream().anyMatch(n -> "b".equals(n.getTextContent())));
    }

    @Test
    public void stripsWhitespaceOnlyText() {
        DomNode root = parser.parse("<div>\n  <p>hi</p>\n</div>");
        ElementNode div = findFirst(root, "div");
        assertTrue(div.getChildren().stream().noneMatch(n -> !n.isElement()));
        assertEquals(1, div.getChildren().size());
    }

    @Test
    public void collapsesInnerWhitespace() {
        DomNode root = parser.parse("<p>a  \n   b</p>");
        ElementNode p = findFirst(root, "p");
        assertEquals("a b", p.getChildren().get(0).getTextContent());
    }

    @Test
    public void skipsComments() {
        DomNode root = parser.parse("<div><!-- hi there --><p>x</p></div>");
        ElementNode div = findFirst(root, "div");
        assertEquals(1, div.getChildren().size());
        assertEquals("p", div.getChildren().get(0).getNodeName());
    }

    @Test
    public void skipsDoctypeAndDeclarations() {
        DomNode root = parser.parse("<!DOCTYPE html><div>ok</div>");
        ElementNode div = findFirst(root, "div");
        assertEquals("ok", div.getChildren().get(0).getTextContent());
    }

    @Test
    public void keepsCdataAsRawText() {
        DomNode root = parser.parse("<div><![CDATA[a & b]]></div>");
        ElementNode div = findFirst(root, "div");
        assertEquals("a & b", div.getChildren().get(0).getTextContent());
    }

    @Test
    public void decodesNamedEntities() {
        DomNode root = parser.parse("<p>Tom &amp; Jerry &lt;3</p>");
        ElementNode p = findFirst(root, "p");
        assertEquals("Tom & Jerry <3", p.getChildren().get(0).getTextContent());
    }

    @Test
    public void decodesDecimalEntities() {
        DomNode root = parser.parse("<p>&#38;&#65;</p>");
        ElementNode p = findFirst(root, "p");
        assertEquals("&A", p.getChildren().get(0).getTextContent());
    }

    @Test
    public void decodesHexEntities() {
        DomNode root = parser.parse("<p>&#x26;&#x41;</p>");
        ElementNode p = findFirst(root, "p");
        assertEquals("&A", p.getChildren().get(0).getTextContent());
    }

    @Test
    public void decodesSinglePassOnly() {
        DomNode root = parser.parse("<p>&amp;lt;</p>");
        ElementNode p = findFirst(root, "p");
        assertEquals("&lt;", p.getChildren().get(0).getTextContent());
    }

    @Test
    public void leavesUnknownEntitiesAlone() {
        DomNode root = parser.parse("<p>&nope;</p>");
        ElementNode p = findFirst(root, "p");
        assertEquals("&nope;", p.getChildren().get(0).getTextContent());
    }

    @Test
    public void decodesEntitiesInAttributes() {
        DomNode root = parser.parse("<p title=\"a &amp; b\"></p>");
        ElementNode p = findFirst(root, "p");
        assertEquals("a & b", p.getAttributes().get("title"));
    }

    @Test
    public void rootNodeIsArtificialAndWrapsEverything() {
        DomNode root = parser.parse("<p>a</p><p>b</p>");
        assertEquals("root", root.getNodeName());
        assertTrue(root.isElement());
        assertEquals(2, root.getChildren().size());
    }

    @Test
    public void textNodesAreNotElements() {
        DomNode root = parser.parse("<p>x</p>");
        ElementNode p = findFirst(root, "p");
        DomNode text = p.getChildren().get(0);
        assertFalse(text.isElement());
        assertEquals("#text", text.getNodeName());
        assertTrue(text.getChildren().isEmpty());
    }

    private ElementNode findFirst(DomNode node, String tag) {
        return findAll(node, tag).get(0);
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
