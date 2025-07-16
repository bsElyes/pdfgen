package tn.eternity.model;

import java.util.Collections;
import java.util.List;

/** Represents a sidebar item (either a category or document link). */
public class SidebarItem {
  public final List<SidebarItem> children;
  public String label;

  public SidebarItem(String label, List<SidebarItem> children) {
    this.label = label;
    this.children = children != null ? children : Collections.emptyList();
  }

  public boolean isCategory() {
    return children != null && !children.isEmpty();
  }

  @Override
  public String toString() {
    return "SidebarItem{" + "label='" + label + '\'' + ", children=" + children.size() + '}';
  }
}
