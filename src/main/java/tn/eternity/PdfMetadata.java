package tn.eternity;

import java.io.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import picocli.CommandLine;

@CommandLine.Command(name = "pdf-metadata", description = "Enhances PDF metadata")
public class PdfMetadata implements Runnable {

  @CommandLine.Option(
      names = {"-t", "--title"},
      description = "PDF title")
  private String title = "Internal Documentation";

  @CommandLine.Option(
      names = {"-l", "--toc-levels"},
      description = "Max TOC levels to style")
  private int tocLevels = 3;

  @CommandLine.Option(
      names = {"-d", "--description"},
      description = "PDF description")
  private String description = "Generated from Docusaurus";

  @CommandLine.Option(
      names = {"-i", "--input"},
      required = true,
      description = "Input PDF file")
  private String inputFile;

  /**
   * Entry point for the PDF metadata enhancer CLI.
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    new CommandLine(new PdfMetadata()).execute(args);
  }

  /**
   * Main execution logic for enhancing PDF metadata and styling outline.
   * Loads the PDF, sets metadata, styles the outline, and saves the result.
   */
  @Override
  public void run() {
    try (PDDocument document = PDDocument.load(new File(inputFile))) {
      logInfo("Enhancing PDF metadata...");
      setMetadata(document);
      logInfo("Styling outline TOC levels up to " + tocLevels);
      enhanceOutline(document.getDocumentCatalog().getDocumentOutline(), tocLevels);
      document.save(inputFile);
      logInfo("Metadata enhanced: " + inputFile);
    } catch (Exception e) {
      logError("Metadata update failed: " + e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Sets metadata fields (title, subject, keywords, creator, description) on the PDF document.
   * @param document The PDF document to update
   */
  private void setMetadata(PDDocument document) {
    PDDocumentInformation info = document.getDocumentInformation();
    info.setTitle(title);
    info.setSubject("Docusaurus Documentation");
    info.setKeywords("documentation,internal,docusaurus");
    info.setCreator("Docusaurus PDF Generator");
    info.setCustomMetadataValue("description", description);
  }

  private void enhanceOutline(PDDocumentOutline outline, int maxLevel) {
    if (outline == null) return;
    enhanceOutlineItems(outline, 0, maxLevel);
  }

  private void enhanceOutlineItems(PDOutlineNode node, int currentLevel, int maxLevel) {
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

  private void logInfo(String msg) {
    System.out.println("[INFO] " + msg);
  }

  private void logError(String msg) {
    System.err.println("[ERROR] " + msg);
  }
}
