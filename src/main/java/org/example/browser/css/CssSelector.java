package org.example.browser.css;
import java.util.*;

public class CssSelector {
 public String tag;   // "div"
 public String id;   // "main"
 public Set<String> classes; // "container"   

 // specficity calculation 

  public int specificity() {
        int s = 0;
        if (id != null) s += 100;
        if (classes != null) s += classes.size() * 10;
        if (tag != null) s += 1;
        return s;
    }
}
