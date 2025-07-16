package tn.eternity.model;

/** Represents a documentation page extracted from the sitemap. */
public class DocumentationPage {
  public String url;
  public String title;
  public int depth;
  public String lastModified;

  public DocumentationPage() {}

  public DocumentationPage(String url, String title, int depth, String lastModified) {
    this.url = url;
    this.title = title;
    this.depth = depth;
    this.lastModified = lastModified;
  }
}
