package tn.eternity.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

/** Model representing the <urlset> root of a sitemap.xml. */
@JacksonXmlRootElement(localName = "urlset")
public class UrlSet {

  @JacksonXmlProperty(localName = "url")
  @JacksonXmlElementWrapper(useWrapping = false)
  public List<Url> urls;

  public static class Url {
    public String loc;
    public String lastmod;
    public String changefreq;
    public Float priority;
  }
}
