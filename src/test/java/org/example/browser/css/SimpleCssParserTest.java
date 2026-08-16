package org.example.browser.css;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SimpleCssParserTest {

    private final SimpleCssParser parser = new SimpleCssParser();

    @Test
    public void parsesRuleCount() {
        List<CssRule> rules = parser.parse("h1 { color: red; } p { font-size: 12px; }");
        assertEquals(2, rules.size());
    }

    @Test
    public void parsesTagSelector() {
        CssSelector sel = parser.parse("div { color: blue; }").get(0).selectors.get(0);
        assertEquals(1, sel.parts.size());
        assertEquals("div", sel.parts.get(0).compound.tag);
        assertNull(sel.parts.get(0).compound.id);
    }

    @Test
    public void parsesIdAndClassSelectors() {
        CssSelector sel = parser.parse("#main { color: blue; }").get(0).selectors.get(0);
        assertEquals("main", sel.parts.get(0).compound.id);

        CssSelector cls = parser.parse(".box { color: blue; }").get(0).selectors.get(0);
        assertTrue(cls.parts.get(0).compound.classes.contains("box"));
    }

    @Test
    public void parsesCompoundSelector() {
        CssSelector sel = parser.parse("div.box#main { color: blue; }").get(0).selectors.get(0);
        SimpleSelector s = sel.parts.get(0).compound;
        assertEquals("div", s.tag);
        assertTrue(s.classes.contains("box"));
        assertEquals("main", s.id);
    }

    @Test
    public void parsesUniversalSelector() {
        CssSelector sel = parser.parse("* { color: red; }").get(0).selectors.get(0);
        assertNull(sel.parts.get(0).compound.tag);
    }

    @Test
    public void parsesMultipleSelectorsPerRule() {
        CssRule rule = parser.parse("h1, h2, .title { color: red; }").get(0);
        assertEquals(3, rule.selectors.size());
    }

    @Test
    public void parsesDescendantCombinator() {
        CssSelector sel = parser.parse("div p { color: red; }").get(0).selectors.get(0);
        assertEquals(2, sel.parts.size());
        assertEquals("div", sel.parts.get(0).compound.tag);
        assertEquals(" ", sel.parts.get(1).combinator);
        assertEquals("p", sel.parts.get(1).compound.tag);
    }

    @Test
    public void parsesChildCombinator() {
        CssSelector sel = parser.parse("div > p { color: red; }").get(0).selectors.get(0);
        assertEquals(">", sel.parts.get(1).combinator);
    }

    @Test
    public void parsesSiblingCombinators() {
        CssSelector plus = parser.parse("h2 + p { color: red; }").get(0).selectors.get(0);
        assertEquals("+", plus.parts.get(1).combinator);

        CssSelector tilde = parser.parse("h2 ~ p { color: red; }").get(0).selectors.get(0);
        assertEquals("~", tilde.parts.get(1).combinator);
    }

    @Test
    public void parsesPseudoClasses() {
        CssSelector first = parser.parse("p:first-child { color: red; }").get(0).selectors.get(0);
        assertTrue(first.parts.get(0).compound.pseudoClasses.contains("first-child"));

        CssSelector nth = parser.parse("li:nth-child(2n+1) { color: red; }").get(0).selectors.get(0);
        assertTrue(nth.parts.get(0).compound.pseudoClasses.contains("nth-child(2n+1)"));
    }

    @Test
    public void parsesNotPseudoClass() {
        CssSelector sel = parser.parse("input:not(.disabled) { color: red; }").get(0).selectors.get(0);
        assertTrue(sel.parts.get(0).compound.pseudoClasses.contains("not(.disabled)"));
    }

    @Test
    public void parsesDeclarations() {
        Map<String, CssDeclaration> decls = parser.parse("p { color: red; font-size: 12px; }").get(0).declarations;
        assertEquals("red", decls.get("color").value);
        assertEquals("12px", decls.get("font-size").value);
    }

    @Test
    public void detectsImportantFlag() {
        Map<String, CssDeclaration> decls = parser.parse("p { color: red !important; }").get(0).declarations;
        CssDeclaration d = decls.get("color");
        assertTrue(d.important);
        assertEquals("red", d.value);
    }

    @Test
    public void normalDeclarationsAreNotImportant() {
        CssDeclaration d = parser.parse("p { color: red; }").get(0).declarations.get("color");
        assertFalse(d.important);
    }

    @Test
    public void skipsCommentsAndWhitespace() {
        List<CssRule> rules = parser.parse("/* header */\n  h1 { color: red; } /* trailing */");
        assertEquals(1, rules.size());
        assertEquals("h1", rules.get(0).selectors.get(0).parts.get(0).compound.tag);
    }

    @Test
    public void ignoresMalformedDeclarations() {
        Map<String, CssDeclaration> decls = parser.parse("p { color: red; broken }").get(0).declarations;
        assertEquals(1, decls.size());
    }

    @Test
    public void expandsMarginShorthand() {
        Map<String, CssDeclaration> d = parser.parse("p { margin: 10px 20px; }").get(0).declarations;
        assertEquals("10px", d.get("margin-top").value);
        assertEquals("20px", d.get("margin-right").value);
        assertEquals("10px", d.get("margin-bottom").value);
        assertEquals("20px", d.get("margin-left").value);
        assertNull(d.get("margin"));
    }

    @Test
    public void expandsPaddingShorthandFourValues() {
        Map<String, CssDeclaration> d = parser.parse("p { padding: 1px 2px 3px 4px; }").get(0).declarations;
        assertEquals("1px", d.get("padding-top").value);
        assertEquals("2px", d.get("padding-right").value);
        assertEquals("3px", d.get("padding-bottom").value);
        assertEquals("4px", d.get("padding-left").value);
    }

    @Test
    public void expandsBorderWidthShorthand() {
        Map<String, CssDeclaration> d = parser.parse("p { border-width: 5px; }").get(0).declarations;
        assertEquals("5px", d.get("border-top-width").value);
        assertEquals("5px", d.get("border-bottom-width").value);
    }

    @Test
    public void expandsBorderShorthand() {
        Map<String, CssDeclaration> d = parser.parse("p { border: 2px solid red; }").get(0).declarations;
        assertEquals("2px", d.get("border-top-width").value);
        assertNull(d.get("border"));
    }

    @Test
    public void expandsBackgroundShorthand() {
        Map<String, CssDeclaration> d = parser.parse("p { background: #fff; }").get(0).declarations;
        assertEquals("#fff", d.get("background-color").value);
        assertNull(d.get("background"));
    }

    @Test
    public void specificityOfId() {
        assertArrayEquals(new int[]{1, 0, 0}, parser.parse("#main { color: red; }")
                .get(0).selectors.get(0).specificity());
    }

    @Test
    public void specificityOfClasses() {
        assertArrayEquals(new int[]{0, 2, 0}, parser.parse(".a.b { color: red; }")
                .get(0).selectors.get(0).specificity());
    }

    @Test
    public void specificityOfTag() {
        assertArrayEquals(new int[]{0, 0, 1}, parser.parse("p { color: red; }")
                .get(0).selectors.get(0).specificity());
    }

    @Test
    public void specificitySumsAcrossParts() {
        assertArrayEquals(new int[]{1, 1, 2}, parser.parse("div.box #x p { color: red; }")
                .get(0).selectors.get(0).specificity());
    }

    @Test
    public void specificityOfPseudoClassCountsAsClass() {
        assertArrayEquals(new int[]{0, 1, 1}, parser.parse("li:nth-child(2n) { color: red; }")
                .get(0).selectors.get(0).specificity());
    }

    @Test
    public void specificityOfNotUsesItsArgument() {
        assertArrayEquals(new int[]{0, 1, 1}, parser.parse("input:not(.disabled) { color: red; }")
                .get(0).selectors.get(0).specificity());
    }

    @Test
    public void parseCompoundParsesTagIdClass() {
        SimpleSelector s = SimpleCssParser.parseCompound("input#user.active");
        assertEquals("input", s.tag);
        assertEquals("user", s.id);
        assertTrue(s.classes.contains("active"));
    }
}
