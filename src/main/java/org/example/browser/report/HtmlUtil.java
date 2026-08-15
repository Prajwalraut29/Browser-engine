package org.example.browser.report;
/** HTML escaping helpers – required because the report is generated HTML. */

public class HtmlUtil {
     private HtmlUtil() {}

    public static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
