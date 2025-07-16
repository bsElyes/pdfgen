// Refactored SitemapParser to support Docusaurus v3.8+ and clean model separation
package tn.eternity;

import java.io.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import picocli.CommandLine;

/**
 * CLI tool to validate PDF structure and outline.
 * Checks for minimum page count and minimum TOC items.
 */
@CommandLine.Command(name = "pdf-validator", description = "Validates PDF structure")
public class PdfValidator implements Runnable {

  /** Minimum number of pages required in the PDF. */
  @CommandLine.Option(
      names = {"-p", "--min-pages"},
      description = "Minimum number of pages")
  private int minPages = 5;

  /** Minimum number of TOC items required in the PDF outline. */
  @CommandLine.Option(
      names = {"-t", "--min-toc-items"},
      description = "Minimum TOC items")
  private int minTocItems = 0;

  /** Path to the input PDF file to validate. */
  @CommandLine.Option(
      names = {"-i", "--input"},
      required = true,
      description = "Input PDF file")
  private String inputFile;

  public static void main(String[] args) {
    new CommandLine(new PdfValidator()).execute(args);
  }

  @Override
  public void run() {
    try (PDDocument document = PDDocument.load(new File(inputFile))) {
      logInfo("Validating PDF structure...");
      validatePageCount(document);
      validateTocItems(document);
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

  private void validatePageCount(PDDocument document) {
    int pageCount = document.getNumberOfPages();
    if (pageCount < minPages) {
      throw new RuntimeException("Insufficient pages: " + pageCount + " < " + minPages);
    }
  }

  private void validateTocItems(PDDocument document) {
    int tocItemCount = countTocItems(document.getDocumentCatalog().getDocumentOutline());
    if (tocItemCount < minTocItems) {
      throw new RuntimeException("Insufficient TOC items: " + tocItemCount + " < " + minTocItems);
    }
  }

  private int countTocItems(PDOutlineNode node) {
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

  private void logInfo(String msg) {
    System.out.println("[INFO] " + msg);
  }

  private void logError(String msg) {
    System.err.println("[ERROR] " + msg);
  }
}
