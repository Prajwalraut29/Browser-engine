# Browser Engine

A simple Java-based browser engine that parses HTML, builds a DOM tree, applies CSS, computes styles, lays out the page into boxes, and renders it to a PNG — a miniature version of the browser rendering pipeline.

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
DOM tree with computedStyle
   │  LayoutTreeBuilder.build()     → styled DOM → layout box tree
   ▼
Layout tree (LayoutBox)
   │  LayoutEngine.layout()         → block + inline layout (box model, margins)
   ▼
Positioned boxes + text lines
   │  Renderer.render()             → paint to a PNG (optional box-model overlay)
   ▼
out/render.png
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
    └── StyleResolver.java  – computes final styles (defaults + inheritance + rules + inline)
```

And the layout & rendering stage (added after the style stage):

```
src/main/java/org/example/browser/layout/
├── LayoutTreeBuilder.java – styled DOM → layout box tree (block/inline boxes + anonymous blocks)
├── LayoutEngine.java      – entry point: lays out a tree in a viewport; prints the box tree
├── LayoutBox.java         – one box: type, element, style spec, dimensions, children, text lines
├── BoxType.java           – BlockNode / InlineNode / AnonymousBlock
├── BoxSpec.java           – parsed style: lengths, colors, font info
├── Length.java            – a length: px / percent / auto
├── BoxDimensions.java     – content / padding / border / margin rects
├── Rect.java, EdgesSizes.java – rectangle and per-edge sizes
├── BlockLayout.java       – block layout: widths, auto margins, child stacking, heights
├── InlineLayout.java      – wraps inline text into lines (measure + word wrap)
├── LineBox.java, TextFragment.java – a line and one word in it
├── TextMetrics.java       – real font metrics via java.awt.Font
├── MarginCollapser.java   – vertical margin collapsing
└── Renderer.java          – paints the page to a PNG (with optional box-model debug overlay)
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

This uses the bundled `index.html` and `style.css` from `src/main/resources` and prints the extracted CSS, the number of parsed rules, the DOM tree with computed styles, the layout tree, and writes `out/render.png`.

### CLI arguments

| Position | Meaning | Default |
|---|---|---|
| `args[0]` | HTML file (filesystem path or bundled resource name) | `index.html` |
| `args[1]` | External CSS file (merged after the embedded `<style>` CSS) | `style.css` |
| `args[2]` | Viewport width in px | `800` |
| `args[3]` | Viewport height in px | `600` |
| `args[4]` | `--boxmodel` → also write `out/render-boxmodel.png` with the margin/border/padding/content debug overlay | — |
| `args[5]` | Custom output path for the content image (box-model image gets a `-boxmodel` suffix) | `out/render.png` |

### Output folder

Rendered images go into the **`out/`** folder. It is cleared on every run, so it always holds only the **latest** pair:

- `out/render.png` — the plain page (content) image
- `out/render-boxmodel.png` — with the box-model debug overlay (only if `--boxmodel` is passed)

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

**The box-model demo, with the debug overlay:**

```bash
mvn exec:java "-Dexec.args=boxmodel.html boxmodel.css 900 500 --boxmodel"
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
- The layout engine handles block and inline boxes only (no floats, positioning, flex/grid); only `background-color` (plus the `background` shorthand) and `border-*-width` are painted; inline elements get no box (their width is measured but they don't produce boxes or wrap on their own line).
- `TextMetrics` uses Java AWT font metrics, so results vary with the platform's fonts.

## Testing

The engine ships with unit tests for the core pipeline, written in **JUnit 4** under `src/test/java/org/example/browser/`:

- `html/SimpleHtmlParserTest` — DOM parsing, attributes, comments, entities, whitespace
- `css/SimpleCssParserTest` — selectors, combinators, pseudo-classes, `!important`, specificity, shorthands
- `style/StyleResolverTest` — cascade priority, inheritance, inline styles
- `layout/LengthTest` — length parsing and resolution

Run them with:

```bash
mvn test
```

## Next Steps

You can extend this project by adding:

- inline element boxes (width/height on `span`/`a` etc.), line-height, and `vertical-align`
- floats, `position`, flexbox / grid layout
- attribute selectors (`[type="text"]`), pseudo-elements (`::before`), richer `:not()` lists
- multi-cascade layering (user agent < user < author < !important)
- navigation and page loading over HTTP
