package tn.eternity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine;
import tn.eternity.model.DocumentationPage;

@CommandLine.Command(
    name = "sitemap-parser",
    description = "Parses Docusaurus sitemap.xml to JSON structure")
public class SitemapParser implements Callable<Integer> {

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

  public static void main(String[] args) {
    int exitCode = new CommandLine(new SitemapParser()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
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
                Comparator.comparingInt((DocumentationPage a) -> a.depth).thenComparing(a -> a.url))
            .collect(Collectors.toList());

    // Serialize the documentation structure to a pretty-printed JSON file
    new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .writeValue(new File(outputPath), docs);

    System.out.println("Generated structured JSON: " + outputPath);
    return 0;
  }

  /**
   * Represents the root element of the sitemap XML (urlset).
   * Contains a list of Url objects.
   */
  @JacksonXmlRootElement(localName = "urlset")
  public static class UrlSet {
    @JacksonXmlProperty(localName = "url")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Url> urls;
  }

  /**
   * Represents a single <url> entry in the sitemap XML.
   * Includes location, last modification date, change frequency, and priority.
   */
  public static class Url {
    public String loc;         // The URL location
    public String lastmod;     // Last modification date
    public String changefreq;  // Change frequency
    public Float priority;     // Priority value
  }
}
