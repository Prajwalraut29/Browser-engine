package org.example.browser.report;

import java.util.*;

import org.example.browser.css.*;
import org.example.browser.dom.*;
import org.example.browser.tree.*;

/**
 * Builds out/report.html: a self-contained page that shows how the HTML was
 * parsed into a DOM tree (SVG diagram annotated with DFS pre/post-order and
 * BFS level indices) and how the CSS was parsed into rules with specificity.
 */
public class ReportGenerator {

    private static final double GAP = 28;       // min horizontal gap between sibling subtrees
    private static final double V_SPACING = 74; // vertical distance between levels
    private static final double MARGIN = 30;

    private static final Set<String> STRUCTURAL = Set.of("html", "body", "head", "root");
    private static final Set<String> HEADING = Set.of("h1", "h2", "h3", "h4", "h5", "h6");
    private static final Set<String> INLINE = Set.of(
            "span", "b", "a", "em", "strong", "i", "small", "code", "label", "u", "sub", "sup");

    private static final String PAGE_CSS =
            "body{font-family:'Segoe UI',Arial,sans-serif;margin:24px;color:#212121}" +
            "h1{font-size:20px;margin-bottom:4px}" +
            ".sub{color:#666;font-size:13px;margin-top:0}" +
            "h2{font-size:16px;border-bottom:2px solid #1565c0;padding-bottom:4px;margin-top:30px}" +
            ".stats{display:flex;gap:12px;flex-wrap:wrap;margin:14px 0}" +
            ".card{background:#e3f2fd;border:1px solid #90caf9;border-radius:8px;padding:8px 16px;font-size:13px;min-width:110px}" +
            ".card b{font-size:20px;color:#0d47a1;display:block}" +
            "pre.code{background:#263238;color:#e6edf3;padding:14px;border-radius:8px;overflow:auto;font:13px/1.5 Consolas,monospace;white-space:pre-wrap}" +
            "pre.code .c{color:#7fb069}pre.code .dt{color:#ffd54f}pre.code .t{color:#82aaff}pre.code .e{color:#ffcb6b}" +
            "svg{display:block;margin:10px 0;max-width:100%;height:auto}" +
            ".hint{color:#777;font-size:12px}" +
            "code{background:#eceff1;padding:1px 5px;border-radius:4px;font:12px Consolas,monospace}" +
            ".legend{display:flex;flex-wrap:wrap;gap:12px;font-size:12px;color:#555;margin:10px 0;align-items:center}" +
            ".legend .sw{display:inline-block;width:13px;height:13px;border-radius:3px;vertical-align:middle;margin-right:5px}" +
            ".toggle{display:inline-block;font-size:12px;color:#555;margin:6px 0 10px}" +
            ".toggle input{vertical-align:middle;margin-right:5px}" +
            ".rule{background:#fafafa;border:1px solid #e0e0e0;border-radius:8px;padding:10px 14px;margin:10px 0}" +
            ".rule-head{font-size:13px;margin-bottom:6px}" +
            ".spec{color:#777;font-size:11px}" +
            "pre.decl{margin:0;font:12px Consolas,monospace;background:#fff;border:1px dashed #e0e0e0;padding:8px;border-radius:6px}" +
            "details.sty{background:#fafafa;border:1px solid #e0e0e0;border-radius:6px;padding:6px 10px;margin:6px 0}" +
            "details.sty summary{cursor:pointer;font:13px Consolas,monospace}" +
            "table.st{border-collapse:collapse;margin:8px 0;font:12px Consolas,monospace}" +
            "table.st td{border:1px solid #e0e0e0;padding:2px 10px}" +
            "table.st td:first-child{color:#1565c0;font-weight:bold}" +
            "p.lvl{margin:4px 0;font:12px Consolas,monospace}" +
            "button{margin:4px 6px 0 0;padding:5px 12px;border:1px solid #90caf9;background:#e3f2fd;border-radius:6px;cursor:pointer;font-size:12px}" +
            ".traversal{font:13px/1.7 Consolas,monospace;background:#fafafa;border:1px solid #e0e0e0;border-radius:8px;padding:10px 14px}";

    /** One drawn node: size, final position, visual info and layout temporaries. */
    private static class NodeBox {
        double x, y, w, h;                 // x = center, y = center (px)
        int depth;
        boolean element;
        String label;
        String chipStr = "";
        String fill, stroke, textFill;
        double offset, leftExtent, rightExtent; // tidy-layout temporaries
    }

    public static String generate(String htmlSource, String cssSource,
                                  DomNode domRoot, List<CssRule> rules) {
        Tree<DomNode> tree = DomTreeBuilder.build(domRoot);

        // ---- 1) run the tree algorithms ----------------------------------
        List<TreeNode<DomNode>> pre = tree.preOrder();
        List<TreeNode<DomNode>> post = tree.postOrder();
        List<List<TreeNode<DomNode>>> levels = tree.levels();

        Map<TreeNode<DomNode>, Integer> preIdx = new HashMap<>();
        Map<TreeNode<DomNode>, Integer> postIdx = new HashMap<>();
        Map<TreeNode<DomNode>, Integer> levelIdx = new HashMap<>();
        Map<TreeNode<DomNode>, Integer> bfsIdx = new HashMap<>();
        for (int i = 0; i < pre.size(); i++) preIdx.put(pre.get(i), i);
        for (int i = 0; i < post.size(); i++) postIdx.put(post.get(i), i);
        int b = 0;
        for (int l = 0; l < levels.size(); l++) {
            for (TreeNode<DomNode> n : levels.get(l)) {
                levelIdx.put(n, l);
                bfsIdx.put(n, b++);
            }
        }

        // ---- 2) build sized boxes, then a tidy no-overlap layout ---------
        Map<TreeNode<DomNode>, NodeBox> boxes = new HashMap<>();
        for (TreeNode<DomNode> n : pre) boxes.put(n, makeBox(n.getValue()));

        assignOffsets(tree.getRoot(), boxes);
        assignAbsolute(tree.getRoot(), 0, 0, boxes);

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (NodeBox box : boxes.values()) {
            minX = Math.min(minX, box.x - box.w / 2);
            maxX = Math.max(maxX, box.x + box.w / 2);
            minY = Math.min(minY, box.y - box.h / 2);
            maxY = Math.max(maxY, box.y + box.h / 2);
        }
        double sx = MARGIN - minX, sy = MARGIN - minY;
        for (NodeBox box : boxes.values()) { box.x += sx; box.y += sy; }
        int svgW = (int) Math.ceil(maxX - minX + 2 * MARGIN);
        int svgH = (int) Math.ceil(maxY - minY + 2 * MARGIN);

        Map<Integer, Double> depthY = new TreeMap<>();
        for (NodeBox box : boxes.values()) depthY.putIfAbsent(box.depth, box.y);

        // ---- 3) draw the SVG ---------------------------------------------
        StringBuilder svg = new StringBuilder();
        svg.append("<svg width=\"").append(svgW).append("\" height=\"").append(svgH)
           .append("\" viewBox=\"0 0 ").append(svgW).append(' ').append(svgH)
           .append("\" style=\"background:#fdfdfd;border:1px solid #e0e0e0\">\n");
        svg.append("<defs>")
           .append("<filter id=\"sh\" x=\"-30%\" y=\"-30%\" width=\"160%\" height=\"160%\">")
           .append("<feDropShadow dx=\"0\" dy=\"1.5\" stdDeviation=\"1.8\" flood-color=\"#000\" flood-opacity=\"0.22\"/></filter>")
           .append("<marker id=\"arr\" viewBox=\"0 0 10 10\" refX=\"9\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto-start-reverse\">")
           .append("<path d=\"M0,0 L10,5 L0,10 z\" fill=\"#90a4ae\"/></marker>")
           .append("<style>.nl{font:bold 14px Consolas,monospace}.nc{font:10px Consolas,monospace}.nb{font:10px Consolas,monospace;fill:#666}.nt{font:italic 13px Consolas,monospace}</style>")
           .append("</defs>\n");

        // depth guide lines
        for (Map.Entry<Integer, Double> e : depthY.entrySet()) {
            double gy = e.getValue();
            svg.append("<line x1=\"0\" y1=\"").append(fmt(gy)).append("\" x2=\"").append(svgW)
               .append("\" y2=\"").append(fmt(gy)).append("\" stroke=\"#eceff1\" stroke-width=\"1\" stroke-dasharray=\"4 4\"/>\n");
        }

        // edges: curved bezier from parent bottom to child top
        for (TreeNode<DomNode> node : pre) {
            if (node.getParent() == null) continue;
            NodeBox p = boxes.get(node.getParent());
            NodeBox c = boxes.get(node);
            double x1 = p.x, y1 = p.y + p.h / 2.0;
            double x2 = c.x, y2 = c.y - c.h / 2.0;
            double dy = Math.max(24, y2 - y1);
            double cy1 = y1 + dy * 0.5;
            svg.append("<path d=\"M").append(fmt(x1)).append(' ').append(fmt(y1))
               .append(" C ").append(fmt(x1)).append(' ').append(fmt(cy1))
               .append(", ").append(fmt(x2)).append(' ').append(fmt(cy1))
               .append(", ").append(fmt(x2)).append(' ').append(fmt(y2))
               .append("\" fill=\"none\" stroke=\"").append(p.fill)
               .append("\" stroke-opacity=\"0.55\" stroke-width=\"1.8\" stroke-linecap=\"round\" marker-end=\"url(#arr)\"/>\n");
        }

        // node boxes + badges (badges in a toggleable group)
        StringBuilder badges = new StringBuilder();
        for (TreeNode<DomNode> node : pre) {
            NodeBox box = boxes.get(node);
            DomNode v = node.getValue();
            double left = box.x - box.w / 2.0, top = box.y - box.h / 2.0;
            double rx = box.element ? 9 : 13;

            svg.append("<g>");
            svg.append("<rect x=\"").append(fmt(left)).append("\" y=\"").append(fmt(top))
               .append("\" width=\"").append(fmt(box.w)).append("\" height=\"").append(fmt(box.h))
               .append("\" rx=\"").append(rx).append("\" fill=\"").append(box.fill)
               .append("\" stroke=\"").append(box.stroke).append("\" stroke-width=\"1.5\" filter=\"url(#sh)\"/>");
            svg.append("<text x=\"").append(fmt(box.x)).append("\" y=\"").append(fmt(box.element ? box.y - 4 : box.y + 5))
               .append("\" text-anchor=\"middle\" fill=\"").append(box.textFill)
               .append("\" class=\"").append(box.element ? "nl" : "nt").append("\">")
               .append(HtmlUtil.escape(box.label)).append("</text>");
            if (box.element && !box.chipStr.isEmpty()) {
                svg.append("<text x=\"").append(fmt(box.x)).append("\" y=\"").append(fmt(box.y + 14))
                   .append("\" text-anchor=\"middle\" fill=\"#e3f2fd\" class=\"nc\">")
                   .append(HtmlUtil.escape(box.chipStr)).append("</text>");
            }
            svg.append("<title>").append(HtmlUtil.escape(infoFor(v))).append("</title>");
            svg.append("</g>\n");

            badges.append("<text x=\"").append(fmt(box.x)).append("\" y=\"").append(fmt(box.y + box.h / 2.0 + 12))
                   .append("\" text-anchor=\"middle\" class=\"nb\">pre=").append(preIdx.get(node))
                   .append(" post=").append(postIdx.get(node)).append(" L=").append(levelIdx.get(node))
                   .append(" b=").append(bfsIdx.get(node)).append("</text>\n");
        }
        svg.append("<g id=\"badges\" style=\"display:none\">\n").append(badges).append("</g>\n");
        svg.append("</svg>\n");

        // ---- 4) assemble the page ----------------------------------------
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n");
        sb.append("<title>Browser Engine - Parse Tree Report</title>\n<style>\n").append(PAGE_CSS).append("\n</style>\n</head>\n<body>\n");
        sb.append("<h1>Browser Engine - Parse Tree Report</h1>\n");
        sb.append("<p class=\"sub\">How the HTML was parsed into a DOM tree and how the CSS was parsed into rules.</p>\n");

        sb.append("<div class=\"stats\">");
        card(sb, "DOM nodes", tree.nodeCount());
        card(sb, "Tree height", tree.height());
        card(sb, "Leaf nodes", tree.leafCount());
        card(sb, "CSS rules", rules.size());
        sb.append("</div>\n");

        sb.append("<h2>1 - Input HTML</h2>\n<pre class=\"code\">").append(highlightHtml(htmlSource)).append("</pre>\n");
        sb.append("<details><summary style=\"cursor:pointer\">Input CSS</summary><pre class=\"code\">")
          .append(HtmlUtil.escape(cssSource)).append("</pre></details>\n");

        sb.append("<h2>2 - DOM parse tree</h2>\n");
        sb.append("<p class=\"hint\">Hover a node to see its attributes and computed style.</p>\n");
        sb.append(legend());
        sb.append("<label class=\"toggle\"><input type=\"checkbox\" onchange=\"document.getElementById('badges').style.display=this.checked?'':'none'\"> Show traversal indices (pre / post / L / b)</label>\n");
        sb.append(svg);

        sb.append("<h2>3 - Tree algorithms</h2>\n<div class=\"traversal\">");
        sb.append("<p><b>Pre-order</b> (DFS, discovery):<br>").append(HtmlUtil.escape(orderList(pre))).append("</p>\n");
        sb.append("<p><b>Post-order</b> (DFS, finish):<br>").append(HtmlUtil.escape(orderList(post))).append("</p>\n");
        sb.append("<p><b>BFS level-order</b> (queue):</p>\n");
        for (int l = 0; l < levels.size(); l++) {
            sb.append("<p class=\"lvl\"><b>Level ").append(l).append(":</b> ")
              .append(HtmlUtil.escape(orderList(levels.get(l)))).append("</p>\n");
        }
        sb.append("</div>\n");

        sb.append("<h2>4 - Parsed CSS rules</h2>\n").append(rulesSection(rules));

        sb.append("<h2>5 - Computed styles (cascade output)</h2>\n");
        sb.append("<button onclick=\"toggleAll(true)\">Expand all</button> <button onclick=\"toggleAll(false)\">Collapse all</button>\n");
        sb.append(styleSection(domRoot));

        sb.append("<script>function toggleAll(open){document.querySelectorAll('details.sty').forEach(function(d){d.open=open;});}</script>\n");
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    // ---- tidy layout -----------------------------------------------------

    /**
     * Bottom-up: lays children in a row spaced so that the bounding envelopes
     * of sibling subtrees never overlap (gap = GAP at every depth), then
     * centers the parent over its children. Each child stores its offset from
     * the parent's center; each node stores the left/right extent of its whole
     * subtree (relative to its own center).
     */
    private static void assignOffsets(TreeNode<DomNode> node, Map<TreeNode<DomNode>, NodeBox> boxes) {
        NodeBox box = boxes.get(node);
        List<TreeNode<DomNode>> kids = node.getChildren();
        if (kids.isEmpty()) {
            box.offset = 0;
            box.leftExtent = box.w / 2.0;
            box.rightExtent = box.w / 2.0;
            return;
        }
        for (TreeNode<DomNode> k : kids) assignOffsets(k, boxes);

        int n = kids.size();
        double[] pos = new double[n];
        pos[0] = 0;
        for (int i = 1; i < n; i++) {
            NodeBox prev = boxes.get(kids.get(i - 1));
            NodeBox cur = boxes.get(kids.get(i));
            pos[i] = pos[i - 1] + prev.rightExtent + GAP + cur.leftExtent;
        }
        double spanL = pos[0] - boxes.get(kids.get(0)).leftExtent;
        double spanR = pos[n - 1] + boxes.get(kids.get(n - 1)).rightExtent;
        double shift = -(spanL + spanR) / 2.0;

        box.offset = 0;
        box.leftExtent = box.w / 2.0;
        box.rightExtent = box.w / 2.0;
        for (int i = 0; i < n; i++) {
            NodeBox cb = boxes.get(kids.get(i));
            cb.offset = pos[i] + shift;
            box.leftExtent = Math.max(box.leftExtent, cb.leftExtent - cb.offset);
            box.rightExtent = Math.max(box.rightExtent, cb.offset + cb.rightExtent);
        }
    }

    /** Top-down: resolve offsets into absolute center positions. */
    private static void assignAbsolute(TreeNode<DomNode> node, double x, int depth,
                                       Map<TreeNode<DomNode>, NodeBox> boxes) {
        NodeBox box = boxes.get(node);
        box.x = x;
        box.y = depth * V_SPACING;
        box.depth = depth;
        for (TreeNode<DomNode> k : node.getChildren()) {
            assignAbsolute(k, x + boxes.get(k).offset, depth + 1, boxes);
        }
    }

    // ---- node boxes ------------------------------------------------------

    private static NodeBox makeBox(DomNode v) {
        NodeBox b = new NodeBox();
        b.element = v.isElement();
        if (b.element) {
            ElementNode el = (ElementNode) v;
            String tag = el.getNodeName();
            b.label = "<" + tag + ">";
            List<String> chips = new ArrayList<>();
            for (Map.Entry<String, String> a : el.getAttributes().entrySet()) {
                chips.add(a.getKey() + "=\"" + a.getValue() + "\"");
            }
            double lw = 9.0 * b.label.length();
            double cw = 0;
            for (String c : chips) cw = Math.max(cw, 7.5 * c.length());
            b.w = clamp(Math.max(lw, cw) + 28, 76, 230);
            b.h = 46;
            String[] pal = palette(tag);
            b.fill = pal[0];
            b.stroke = pal[1];
            b.textFill = "#ffffff";
            if (!chips.isEmpty()) {
                String joined = chips.size() > 2
                        ? chips.get(0) + " " + chips.get(1) + " +" + (chips.size() - 2)
                        : String.join(" ", chips);
                int maxChars = Math.max(4, (int) ((b.w - 20) / 7.5));
                b.chipStr = truncate(joined, maxChars);
            }
        } else {
            String t = v.getTextContent().replace('\n', ' ').trim();
            b.label = "\"" + truncate(t, 22) + "\"";
            b.w = clamp(7.5 * b.label.length() + 22, 40, 200);
            b.h = 26;
            b.fill = "#f5f5f5";
            b.stroke = "#bdbdbd";
            b.textFill = "#37474f";
        }
        return b;
    }

    private static String[] palette(String tag) {
        if (HEADING.contains(tag)) return new String[]{"#5e35b1", "#4527a0"};
        if (STRUCTURAL.contains(tag)) return new String[]{"#263238", "#141a1e"};
        if (INLINE.contains(tag)) return new String[]{"#00897b", "#00695c"};
        return new String[]{"#1565c0", "#0d47a1"};
    }

    private static String legend() {
        return "<div class=\"legend\">" +
                "<span><span class=\"sw\" style=\"background:#263238\"></span>structural</span>" +
                "<span><span class=\"sw\" style=\"background:#1565c0\"></span>block</span>" +
                "<span><span class=\"sw\" style=\"background:#00897b\"></span>inline</span>" +
                "<span><span class=\"sw\" style=\"background:#5e35b1\"></span>heading</span>" +
                "<span><span class=\"sw\" style=\"background:#f5f5f5;border:1px solid #bdbdbd\"></span>text node</span>" +
                "</div>\n";
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ---- labels / info ---------------------------------------------------

    private static String labelFor(DomNode v) {
        if (v.isElement()) {
            ElementNode el = (ElementNode) v;
            StringBuilder s = new StringBuilder("<").append(el.getNodeName());
            String id = el.getAttributes().get("id");
            if (id != null) s.append(" #").append(id);
            s.append(">");
            return s.toString();
        }
        return "\"" + truncate(v.getTextContent().replace('\n', ' ').trim(), 16) + "\"";
    }

    private static String infoFor(DomNode v) {
        StringBuilder sb = new StringBuilder();
        if (v.isElement()) {
            ElementNode el = (ElementNode) v;
            sb.append('<').append(el.getNodeName());
            for (Map.Entry<String, String> a : el.getAttributes().entrySet()) {
                sb.append(' ').append(a.getKey()).append("=\"").append(a.getValue()).append('"');
            }
            sb.append('>');
            Map<String, String> style = el.getComputedStyle();
            if (style != null) sb.append("\ncomputed style: ").append(style);
        } else {
            sb.append(v.getTextContent());
        }
        return sb.toString();
    }

    private static String truncate(String s, int n) {
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    // ---- sections --------------------------------------------------------

    private static void card(StringBuilder sb, String label, int value) {
        sb.append("<div class=\"card\"><b>").append(value).append("</b>").append(label).append("</div>\n");
    }

    private static String orderList(List<TreeNode<DomNode>> nodes) {
        StringBuilder sb = new StringBuilder();
        for (TreeNode<DomNode> n : nodes) {
            sb.append(labelFor(n.getValue())).append(", ");
        }
        if (sb.length() > 2) sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private static String rulesSection(List<CssRule> rules) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            CssRule r = rules.get(i);
            sb.append("<div class=\"rule\"><div class=\"rule-head\">Rule #").append(i + 1);
            for (CssSelector sel : r.selectors) {
                int[] spec = sel.specificity();
                sb.append(" &nbsp; <code>").append(HtmlUtil.escape(selectorString(sel)))
                  .append("</code> <span class=\"spec\">(ids=").append(spec[0])
                  .append(", classes=").append(spec[1]).append(", types=").append(spec[2]).append(")</span>");
            }
            sb.append("</div><pre class=\"decl\">");
            for (Map.Entry<String, CssDeclaration> e : r.declarations.entrySet()) {
                sb.append(HtmlUtil.escape(e.getKey())).append(": ")
                  .append(HtmlUtil.escape(e.getValue().value))
                  .append(e.getValue().important ? "  <b>!important</b>" : "").append('\n');
            }
            sb.append("</pre></div>\n");
        }
        return sb.toString();
    }

    private static String styleSection(DomNode root) {
        StringBuilder sb = new StringBuilder();
        styleSectionRec(root, sb);
        return sb.toString();
    }

    private static void styleSectionRec(DomNode node, StringBuilder sb) {
        if (node.isElement()) {
            ElementNode el = (ElementNode) node;
            Map<String, String> style = el.getComputedStyle();
            sb.append("<details class=\"sty\"><summary>&lt;").append(el.getNodeName()).append("&gt;");
            for (Map.Entry<String, String> a : el.getAttributes().entrySet()) {
                sb.append(' ').append(HtmlUtil.escape(a.getKey())).append("=\"")
                  .append(HtmlUtil.escape(a.getValue())).append('"');
            }
            if (style != null) sb.append("  (").append(style.size()).append(" props)");
            sb.append("</summary>");
            if (style != null) sb.append(styleTable(style));
            else sb.append("<p class=\"hint\">no computed style</p>");
            sb.append("</details>\n");
        }
        for (DomNode child : node.getChildren()) styleSectionRec(child, sb);
    }

    private static String styleTable(Map<String, String> style) {
        StringBuilder tb = new StringBuilder("<table class=\"st\">");
        for (Map.Entry<String, String> e : style.entrySet()) {
            tb.append("<tr><td>").append(HtmlUtil.escape(e.getKey()))
              .append("</td><td>").append(HtmlUtil.escape(e.getValue())).append("</td></tr>");
        }
        return tb.append("</table>").toString();
    }

    // ---- CSS selectors ---------------------------------------------------

    private static String selectorString(CssSelector sel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sel.parts.size(); i++) {
            CssSelector.Part p = sel.parts.get(i);
            if (i > 0) {
                String comb = p.combinator;
                sb.append(comb.isEmpty() ? " " : " " + comb + " ");
            }
            sb.append(compoundString(p.compound));
        }
        return sb.toString();
    }

    private static String compoundString(SimpleSelector s) {
        StringBuilder sb = new StringBuilder();
        if (s.tag != null) sb.append(s.tag);
        if (s.id != null) sb.append('#').append(s.id);
        for (String c : s.classes) sb.append('.').append(c);
        for (String p : s.pseudoClasses) sb.append(':').append(p);
        return sb.length() == 0 ? "*" : sb.toString();
    }

    // ---- source highlighting ---------------------------------------------

    private static String highlightHtml(String src) {
        String s = HtmlUtil.escape(src);
        s = s.replaceAll("(&lt;!--[\\s\\S]*?--&gt;)", "<span class=\"c\">$1</span>");
        s = s.replaceAll("(&lt;!DOCTYPE[\\s\\S]*?&gt;)", "<span class=\"dt\">$1</span>");
        s = s.replaceAll("(&lt;/?[A-Za-z][^&gt;]*&gt;)", "<span class=\"t\">$1</span>");
        s = s.replaceAll("(&amp;\\w+;|&amp;#[xX]?[0-9a-fA-F]+;)", "<span class=\"e\">$1</span>");
        return s;
    }

    // ---- helpers ---------------------------------------------------------

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }
}
