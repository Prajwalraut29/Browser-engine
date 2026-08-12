# Browser Engine

A simple Java-based browser engine that parses HTML, builds a DOM tree, applies CSS, and computes styles for every element — a miniature version of the browser rendering pipeline.

## Overview

The engine implements a subset of a real browser pipeline:

```
HTML file
   │  SimpleHtmlParser.parse()      → build DOM tree
   ▼
DOM tree (DomNode)
   │  extractCss()                  → pull CSS out of <style> elements
   ▼
CSS string
   │  SimpleCssParser.parse()       → turn into a list of CssRule
   ▼
List<CssRule>
   │  StyleResolver.resolveTree()   → walk the DOM, compute final styles
   ▼
DOM tree with computedStyle → printed to the console
```

## Project Structure

```
src/main/java/org/example/browser/
├── Main.java                 – entry point: loads files, runs the whole pipeline, prints the result
├── html/
│   └── SimpleHtmlParser.java – hand-written HTML parser → DOM tree
├── dom/
│   ├── DomNode.java          – interface: common node API (name, attributes, children, text)
│   ├── ElementNode.java      – a tag node (div, span, ...) + its computed style
│   └── TextNode.java         – a text leaf node
├── css/
│   ├── CssRule.java          – a rule: selectors + declarations (each with an !important flag)
│   ├── CssSelector.java      – complex selector: chain of compounds joined by combinators
│   ├── SimpleSelector.java   – one compound selector: tag / id / class / pseudo-classes
│   ├── CssDeclaration.java   – a property declaration (value + !important flag)
│   └── SimpleCssParser.java  – CSS string → List<CssRule>
└── style/
    └── StyleResolver.java    – computes final styles (defaults + inheritance + rules + inline)
```

Bundled sample files (used as defaults when no arguments are given):

```
src/main/resources/
├── index.html  – demo markup with an embedded <style> block
└── style.css   – demo external stylesheet
```

## Requirements

- Java 17 or newer
- Maven

## Run the Project

```bash
mvn compile
mvn exec:java
```

This uses the bundled `index.html` and `style.css` from `src/main/resources` and prints the extracted CSS, the number of parsed rules, and the DOM tree with computed styles.

### Using your own files

You can load your own HTML and CSS files instead of the hardcoded defaults by passing paths as arguments.

**HTML file only** (CSS comes from the `<style>` block inside the HTML):

```bash
mvn exec:java "-Dexec.args=C:\path\to\page.html"
```

**HTML and CSS files** (external CSS is merged after the embedded `<style>` CSS):

```bash
mvn exec:java "-Dexec.args=C:\path\to\page.html C:\path\to\style.css"
```

**Files inside `src/main/resources`** (pass the filename only — the loader falls back to the classpath):

```bash
mvn exec:java "-Dexec.args=index.html style.css"
```

### Alternative: run with `java` directly

```bash
mvn compile
java -cp target/classes org.example.browser.Main C:\path\to\page.html C:\path\to\style.css
```

### How file loading works

`Main.loadText()` checks the filesystem first; if the path does not exist, it falls back to a bundled resource with the same name. So:

| Command | HTML source | CSS source |
|---|---|---|
| `mvn exec:java` | bundled `index.html` | `<style>` + bundled `style.css` |
| `mvn exec:java "-Dexec.args=page.html"` | your `page.html` | `<style>` + bundled `style.css` |
| `mvn exec:java "-Dexec.args=page.html style.css"` | your file | `<style>` + your `style.css` |

> Note (PowerShell): wrap the whole `-Dexec.args=...` argument in quotes. On CMD you can write `-Dexec.args="C:\path\page.html"` instead.

## How Styles Are Computed

`StyleResolver` builds each element's final style by layering sources in order of increasing priority:

1. Browser defaults (a tiny user-agent stylesheet: display, margins, padding, color, font-size, ...)
2. Inherited properties from the parent (`color`, `font-size`, `font-family`)
3. All matching CSS rules, sorted by specificity (`#id` = 100, `.class` = 10, `tag` = 1)
4. Inline `style="..."` attributes (highest priority)

External CSS is appended after embedded `<style>` CSS, so on ties the external rules win (source order).

## CSS Selector Support

Selectors are parsed into a chain of compound parts joined by combinators and matched right-to-left against the DOM tree (nodes carry a parent pointer for ancestor/sibling traversal).

**Combinators:**
| Selector | Meaning |
|---|---|
| `div p` | descendant — any `p` inside a `div` |
| `div > p` | child — direct child only |
| `h2 + p` | adjacent sibling — `p` immediately after `h2` |
| `h2 ~ p` | general sibling — any `p` after `h2` |
| `*` | universal — matches any element |

**Pseudo-classes:**
- `:first-child`, `:last-child`, `:only-child`
- `:nth-child(An+B)` — also `odd`, `even`, and plain numbers (e.g. `2n+1`, `-n+3`, `3`)
- `:first-of-type`, `:last-of-type`, `:nth-of-type(An+B)`
- `:not(selector)` — accepts one compound selector as its argument
- Interaction pseudo-classes (`:hover`, `:active`, `:focus`, `:visited`, `:link`) parse but never match, since the engine has no interaction state

**Cascade & specificity:**
- Specificity is a true `(ids, classes, types)` triple summed over every compound part (including `:not()` arguments), compared lexicographically — not just a flat number.
- A rule's cascade weight comes from the **highest-specificity selector that actually matched**.
- Ties are broken by source order (the stable sort keeps the later rule winning).
- `!important` declarations win over all normal ones; among important declarations, specificity and source order still apply.
- Priority: `normal rules → normal inline → !important rules → !important inline`.

**Example:**
```css
ul li { color: green; }                 /* descendant: all <li> inside <ul>      */
div > p { color: purple; }              /* child only                             */
li:nth-child(2n+1) { background: yellow; } /* odd rows                            */
p.note { color: red !important; }       /* beats any normal rule, even #wrap p    */
```

## Known Limitations

- The HTML parser assumes well-formed input; it does not validate that closing tags match.
- No attribute selectors (`[type="text"]`) or pseudo-elements (`::before`) yet.
- `:not()` accepts only a single compound selector (no comma lists or combinators).
- Structural pseudo-classes count only element siblings (whitespace-only text nodes are already stripped by the HTML parser).
- No cascade tie-breaking beyond source order, no `!important` for inline styles beyond what is listed above.

## Next Steps

You can extend this project by adding:

- a layout / rendering stage (the natural next pipeline step)
- attribute selectors (`[type="text"]`), pseudo-elements (`::before`), richer `:not()` lists
- multi-cascade layering (user agent < user < author < !important)
- navigation and page loading over HTTP
