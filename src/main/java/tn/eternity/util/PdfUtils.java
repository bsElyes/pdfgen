package tn.eternity.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;

/**
 * Utility class for PDF-related helpers, such as JSON serialization.
 * This class is stateless and provides static helper methods.
 */
public class PdfUtils {

  /**
   * Serializes the given object as pretty-printed JSON to the specified file.
   * Uses Jackson ObjectMapper with INDENT_OUTPUT enabled for readability.
   *
   * @param data   The object to serialize as JSON
   * @param output The file to write the JSON output to
   * @throws IOException if writing to the file fails
   */
  public static void writeJson(Object data, File output) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(output, data);
  }
}
