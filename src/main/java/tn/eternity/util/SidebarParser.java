package tn.eternity.util;

import java.util.*;
import tn.eternity.model.SidebarItem;

/**
 * Utility for parsing Docusaurus sidebar configuration.
 * Supports recursive parsing of sidebar items from JS/TS/JSON structures.
 */
public class SidebarParser {

  /**
   * Recursively parses sidebar items from a Docusaurus sidebar structure.
   * @param items The sidebar items (List or Map structure from JS/TS/JSON)
   * @return List of SidebarItem objects representing the sidebar hierarchy
   */
  public static List<SidebarItem> parseSidebarItems(Object items) {
    List<SidebarItem> result = new ArrayList<>();
    // If items is a list, iterate and parse each item
    if (items instanceof List<?> list) {
      for (Object item : list) {
        // If item is a string, treat as a leaf doc
        if (item instanceof String str) {
          result.add(new SidebarItem(str, Collections.emptyList()));
        // If item is a map, check its type and parse accordingly
        } else if (item instanceof Map<?, ?> map) {
          String type = (String) map.get("type");
          if ("category".equals(type)) {
            String label = (String) map.get("label");
            // Extract link if present (for category navigation)
            String link = Optional.ofNullable(map.get("link"))
                .map(l -> ((Map<?, ?>) l).get("id"))
                .map(Object::toString)
                .orElse(null);
            // Recursively parse children
            List<SidebarItem> children = parseSidebarItems(map.get("items"));
            SidebarItem category = new SidebarItem(label, children);
            if (link != null) {
              category.label = link;
            }
            result.add(category);
          } else if ("doc".equals(type) && map.containsKey("id")) {
            // Doc type: add as leaf
            result.add(new SidebarItem((String) map.get("id"), Collections.emptyList()));
          }
        }
      }
    }
    // Return the parsed sidebar hierarchy
    return result;
  }
}
