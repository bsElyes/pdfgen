package tn.eternity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import picocli.CommandLine;
import tn.eternity.model.DocumentationPage;
import tn.eternity.model.SidebarItem;
import tn.eternity.util.SidebarParser;

@CommandLine.Command(
    name = "pdf-generator",
    description = "PDFGen2: Generate, enhance, validate, and parse Docusaurus documentation PDFs",
    subcommands = {
      PdfGenerator.MetadataCmd.class,
      PdfGenerator.ValidateCmd.class,
      PdfGenerator.SitemapCmd.class
    })
public class PdfGenerator implements Runnable {

  private final Map<String, PDPage> pageMap = new HashMap<>();

  @CommandLine.Option(
      names = {"--no-toc"},
      defaultValue = "false",
      description = "Skip PDF outline/table of contents")
  private boolean noToc;

  @CommandLine.Option(
      names = {"-i", "--input"},
      required = true,
      description = "Docs directory")
  private String inputDir;

  @CommandLine.Option(
      names = {"-o", "--output"},
      required = true,
      description = "Output PDF file")
  private String outputFile;

  @CommandLine.Option(
      names = {"-c", "--css"},
      description = "Print CSS file")
  private String cssFile;

  @CommandLine.Option(
      names = {"-s", "--sidebar"},
      description = "Sidebar config JSON file",
      required = false)
  private String sidebarConfig;

  @CommandLine.Option(
      names = {"--sitemap"},
      description = "Sitemap-based config JSON")
  private String sitemapJson;

  private PDDocument document;
  private PDDocumentOutline outline;
  private List<SidebarItem> sidebarStructure;

  public static void main(String[] args) {
    new CommandLine(new PdfGenerator()).execute(args);
  }

  // -------------------- UTILITY METHODS --------------------
  private static void setMetadata(PDDocument document, String title, String description) {
    PDDocumentInformation info = document.getDocumentInformation();
    info.setTitle(title);
    info.setSubject("Docusaurus Documentation");
    info.setKeywords("documentation,internal,docusaurus");
    info.setCreator("Docusaurus PDF Generator");
    info.setCustomMetadataValue("description", description);
  }

  private static void enhanceOutline(PDDocumentOutline outline, int maxLevel) {
    if (outline == null) return;
    enhanceOutlineItems(outline, 0, maxLevel);
  }

  private static void enhanceOutlineItems(PDOutlineNode node, int currentLevel, int maxLevel) {
    if (node == null || currentLevel > maxLevel) return;
    PDOutlineItem current = node.getFirstChild();
    while (current != null) {
      if (currentLevel <= 1) {
        current.setBold(true);
        current.setItalic(false);
      } else {
        current.setBold(false);
        current.setItalic(true);
      }
      enhanceOutlineItems(current, currentLevel + 1, maxLevel);
      current = current.getNextSibling();
    }
  }

  private static void validatePageCount(PDDocument document, int minPages) {
    int pageCount = document.getNumberOfPages();
    if (pageCount < minPages) {
      throw new RuntimeException("Insufficient pages: " + pageCount + " < " + minPages);
    }
  }

  private static void validateTocItems(PDDocument document, int minTocItems) {
    int tocItemCount = countTocItems(document.getDocumentCatalog().getDocumentOutline());
    if (tocItemCount < minTocItems) {
      throw new RuntimeException("Insufficient TOC items: " + tocItemCount + " < " + minTocItems);
    }
  }

  private static int countTocItems(PDOutlineNode node) {
    if (node == null) return 0;
    int count = 0;
    PDOutlineItem current = node.getFirstChild();
    while (current != null) {
      count++;
      count += countTocItems(current);
      current = current.getNextSibling();
    }
    return count;
  }

  private static void logInfo(String msg) {
    System.out.println("[INFO] " + msg);
  }

  private static void logError(String msg) {
    System.err.println("[ERROR] " + msg);
  }

  @Override
  public void run() {
    try {
      document = new PDDocument();
      if (!noToc) {
        outline = new PDDocumentOutline();
        document.getDocumentCatalog().setDocumentOutline(outline);
      }

      if (sitemapJson != null) {
        logInfo("Using sitemap-based document structure...");
        generatePdfFromSitemap(sitemapJson);
      } else if (sidebarConfig != null) {
        logInfo("Parsing sidebar config...");
        parseSidebarConfig();
        logInfo("Processing structured pages...");
        processStructuredPages();
      } else {
        throw new IllegalArgumentException("Either --sidebar or --sitemap must be provided.");
      }

      logInfo("Saving PDF to " + outputFile);
      document.save(outputFile);
      document.close();
      logInfo("Structured PDF generated: " + outputFile);
    } catch (Exception e) {
      logError("PDF generation failed: " + e.getMessage());
      System.exit(1);
    }
  }

  private void parseSidebarConfig() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> sidebarData =
        mapper.readValue(new File(sidebarConfig), new TypeReference<Map<String, Object>>() {});
    sidebarStructure = SidebarParser.parseSidebarItems(sidebarData.get("docsSidebar"));
  }

  private void processStructuredPages() throws IOException {
    for (SidebarItem item : sidebarStructure) {
      processSidebarItem(item, outline);
    }
  }

  private void processSidebarItem(SidebarItem item, PDOutlineNode parent) throws IOException {
    if (item.isCategory()) {
      PDOutlineItem categoryItem = createOutlineItem(item.label, null, parent);
      for (SidebarItem child : item.children) {
        processSidebarItem(child, categoryItem);
      }
    } else {
      processPage(item.label, parent);
    }
  }

  private void processPage(String label, PDOutlineNode parent) throws IOException {
    Path htmlPath = resolveDocsDir().resolve(label + ".html");
    if (!Files.exists(htmlPath)) {
      logError("HTML file not found: " + htmlPath);
      return;
    }
    PDPage page = createPageFromHtml(htmlPath);
    document.addPage(page);
    pageMap.put(label, page);
    String title = extractPageTitle(htmlPath);
    createOutlineItem(title, page, parent);
  }

  private void generatePdfFromSitemap(String sitemapJson) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    List<DocumentationPage> pages =
        mapper.readValue(new File(sitemapJson), new TypeReference<>() {});

    PDOutlineItem rootToc = new PDOutlineItem();
    rootToc.setTitle("Documentation Structure");
    Map<Integer, PDOutlineItem> lastAtLevel = new HashMap<>();
    lastAtLevel.put(0, rootToc);

    for (DocumentationPage page : pages) {
      // Extract path from full URL (e.g., /docs/tutorial-basics/create-a-page)
      URI uri = URI.create(page.url);
      String relativePath = uri.getPath().replaceFirst("^/", "") + ".html";
      Path htmlPath = Paths.get(inputDir, relativePath);

      // try alternative path if .html doesn't exist
      if (!Files.exists(htmlPath)) {
        Path indexPath = Paths.get(inputDir, relativePath.replace(".html", ""), "index.html");
        if (Files.exists(indexPath)) {
          htmlPath = indexPath;
        } else {
          System.out.println("Skipping missing file: " + htmlPath);
          continue;
        }
      }

      PDPage pdfPage = createPageFromHtml(htmlPath);
      document.addPage(pdfPage);

      if (!noToc) {
        PDOutlineItem tocItem = new PDOutlineItem();
        tocItem.setTitle(page.title);
        PDPageDestination dest = new PDPageFitDestination();
        dest.setPage(pdfPage);
        tocItem.setDestination(dest);

        int parentLevel = Math.max(0, page.depth - 1);
        PDOutlineItem parent = lastAtLevel.get(parentLevel);
        if (parent != null) {
          parent.addLast(tocItem);
        } else {
          rootToc.addLast(tocItem);
        }
        lastAtLevel.put(page.depth, tocItem);
      }
    }

    if (!noToc) {
      outline.addLast(rootToc);
    }
  }

  private Path resolveDocsDir() throws IOException {
    try (Stream<Path> stream = Files.walk(Paths.get(inputDir), 2)) {
      return stream
          .filter(
              p -> p.toString().matches(".*/version-\\d+(\\.\\d+)*?/docs$") || p.endsWith("docs"))
          .findFirst()
          .orElseThrow(() -> new IOException("Docs directory not found"));
    }
  }

  private PDPage createPageFromHtml(Path htmlPath) throws IOException {
    String htmlContent = Files.readString(htmlPath);
    htmlContent =
        htmlContent
            .replace("src=\"/assets/", "src=\"assets/")
            .replace("href=\"/assets/", "href=\"assets/");
    // Parse and clean the HTML
    Document doc = Jsoup.parse(htmlContent);
    doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

    // Remove common Docusaurus layout elements
    doc.select("header").remove();
    doc.select("nav").remove();
    doc.select("aside").remove(); // sidebar
    doc.select("footer").remove();
    doc.select(".theme-doc-toc-desktop").remove(); // right-side TOC
    doc.select(".theme-doc-footer").remove(); // bottom nav
    doc.select(".theme-doc-markdown.markdown").tagName("article"); // optional: re-tag main content

    // Remove unwanted Docusaurus elements from the HTML before rendering
    doc.select("a[href='#__docusaurus_skipToContent_fallback']").remove(); // Skip to content link
    doc.select(".theme-edit-this-page").remove(); // "Edit this page" button
    doc.select(".pagination-nav").remove(); // Bottom prev/next navigation

    // Ensure output is well-formed XHTML for PDF rendering
    doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

    // Inject custom print CSS if provided
    if (cssFile != null) {
      String css = Files.readString(Paths.get(cssFile));
      doc.head().append("<style>" + css + "</style>");
    }

    // Prepare to render the sanitized HTML to PDF
    ByteArrayOutputStream pdfBytes = new ByteArrayOutputStream();
    PdfRendererBuilder builder = new PdfRendererBuilder();
    // Set base URI for relative links and resources
    String baseUri = htmlPath.getParent().getParent().toUri().toString();
    System.out.println("PDF base URI: " + baseUri);
    builder.withHtmlContent(doc.html(), baseUri); // Pass sanitized XHTML
    builder.toStream(pdfBytes); // Output stream for PDF bytes
    builder.run(); // Render the PDF

    PDDocument tempDoc = PDDocument.load(pdfBytes.toByteArray());
    return tempDoc.getPage(0);
  }

  private PDOutlineItem createOutlineItem(String title, PDPage page, PDOutlineNode parent) {
    PDOutlineItem item = new PDOutlineItem();
    item.setTitle(title);
    if (page != null) {
      PDPageDestination dest = new PDPageFitDestination();
      dest.setPage(page);
      item.setDestination(dest);
    }
    parent.addLast(item);
    return item;
  }

  private String extractPageTitle(Path htmlPath) throws IOException {
    Document doc = Jsoup.parse(htmlPath.toFile(), "UTF-8");
    String frontmatterTitle = doc.select("meta[name=title]").attr("content");
    return !frontmatterTitle.isEmpty() ? frontmatterTitle : doc.title().split("\\|")[0].trim();
  }

  // -------------------- METADATA SUBCOMMAND --------------------
  @CommandLine.Command(name = "metadata", description = "Enhance PDF metadata and outline")
  public static class MetadataCmd implements Runnable {
    @CommandLine.Option(
        names = {"-t", "--title"},
        description = "PDF title")
    private  String title = "Internal Documentation";

    @CommandLine.Option(
        names = {"-l", "--toc-levels"},
        description = "Max TOC levels to style")
    private  int tocLevels = 3;

    @CommandLine.Option(
        names = {"-d", "--description"},
        description = "PDF description")
    private  String description = "Generated from Docusaurus";

    @CommandLine.Option(
        names = {"-i", "--input"},
        required = true,
        description = "Input PDF file")
    private String inputFile;

    @Override
    public void run() {
      try (PDDocument document = PDDocument.load(new File(inputFile))) {
        logInfo("Enhancing PDF metadata...");
        setMetadata(document, title, description);
        logInfo("Styling outline TOC levels up to " + tocLevels);
        enhanceOutline(document.getDocumentCatalog().getDocumentOutline(), tocLevels);
        document.save(inputFile);
        logInfo("Metadata enhanced: " + inputFile);
      } catch (Exception e) {
        logError("Metadata update failed: " + e.getMessage());
        System.exit(1);
      }
    }
  }

  // -------------------- VALIDATE SUBCOMMAND --------------------
  @CommandLine.Command(name = "validate", description = "Validate PDF structure and outline")
  public static class ValidateCmd implements Runnable {
    @CommandLine.Option(
        names = {"-p", "--min-pages"},
        description = "Minimum number of pages")
    private  int minPages = 5;

    @CommandLine.Option(
        names = {"-t", "--min-toc-items"},
        description = "Minimum TOC items")
    private  int minTocItems = 0;

    @CommandLine.Option(
        names = {"-i", "--input"},
        required = true,
        description = "Input PDF file")
    private String inputFile;

    @Override
    public void run() {
      try (PDDocument document = PDDocument.load(new File(inputFile))) {
        logInfo("Validating PDF structure...");
        validatePageCount(document, minPages);
        validateTocItems(document, minTocItems);
        logInfo(
            "PDF validation passed: "
                + document.getNumberOfPages()
                + " pages, "
                + countTocItems(document.getDocumentCatalog().getDocumentOutline())
                + " TOC items");
      } catch (Exception e) {
        logError("Validation failed: " + e.getMessage());
        System.exit(1);
      }
    }
  }

  // -------------------- SITEMAP SUBCOMMAND --------------------
  @CommandLine.Command(
      name = "sitemap",
      description = "Parse Docusaurus sitemap.xml to JSON structure")
  public static class SitemapCmd implements Runnable {
    @CommandLine.Option(
        names = {"-i", "--input"},
        required = true,
        description = "Sitemap.xml path")
    private String sitemapPath;

    @CommandLine.Option(
        names = {"-o", "--output"},
        description = "Output JSON path",
        defaultValue = "sitemap-structure.json")
    private String outputPath;

    @Override
    public void run() {
      try {
        XmlMapper xmlMapper = new XmlMapper();
        UrlSet urlSet = xmlMapper.readValue(new File(sitemapPath), UrlSet.class);
        List<DocumentationPage> docs =
            urlSet.urls.stream()
                .filter(url -> url.loc.contains("/docs/"))
                .map(
                    url -> {
                      Path path = Path.of(url.loc);
                      String[] parts = url.loc.split("/docs/");
                      String relativePath = parts.length > 1 ? parts[1] : "";
                      int depth = relativePath.split("/").length;
                      return new DocumentationPage(
                          url.loc, relativePath.replace(".html", ""), depth, url.lastmod);
                    })
                .sorted(
                    Comparator.comparingInt((DocumentationPage a) -> a.depth)
                        .thenComparing(a -> a.url))
                .collect(Collectors.toList());
        new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .writeValue(new File(outputPath), docs);
        System.out.println("Generated structured JSON: " + outputPath);
      } catch (Exception e) {
        logError("Sitemap parsing failed: " + e.getMessage());
        System.exit(1);
      }
    }

    // Sitemap XML model classes
    @JacksonXmlRootElement(localName = "urlset")
    public static class UrlSet {
      @JacksonXmlProperty(localName = "url")
      @JacksonXmlElementWrapper(useWrapping = false)
      public List<Url> urls;
    }

    public static class Url {
      public String loc;
      public String lastmod;
      public String changefreq;
      public Float priority;
    }
  }
}
