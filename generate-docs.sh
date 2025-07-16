#!/bin/bash
set -euo pipefail

# Paths and defaults
SRC_DIR="docs/src"
BUILD_DIR="docs/build"
PRINT_CSS="$SRC_DIR/css/custom.css"
OUTPUT_PDF="output.pdf"
SITEMAP_JSON="sitemap-structure.json"
SITEMAP_XML="$BUILD_DIR/sitemap.xml"

TITLE="Internal Documentation"
TOC_LEVELS="3"
DESCRIPTION="Generated from Docusaurus"
MIN_PAGES="5"
MIN_TOC_ITEMS="0"

# CLI JAR
JAR="target/pdfgen-0.2.0-jar-with-dependencies.jar"

# Logging
log()   { echo -e "\033[1;34m[INFO]\033[0m $1"; }
error() { echo -e "\033[1;31m[ERROR]\033[0m $1" >&2; }

check_file_exists() {
  [[ -f "$1" ]] || { error "File not found: $1"; exit 1; }
}
check_dir_exists() {
  [[ -d "$1" ]] || { error "Directory not found: $1"; exit 1; }
}

log "Verifying required files and directories..."
check_dir_exists "$SRC_DIR"
check_file_exists "$PRINT_CSS"
check_file_exists "$SITEMAP_XML"
check_file_exists "$JAR"

# Step 1: Generate sitemap JSON
log "Generating sitemap structure from $SITEMAP_XML..."
java -jar "$JAR" sitemap \
  --input "$SITEMAP_XML" \
  --output "$SITEMAP_JSON"
if ! [ -f "$SITEMAP_JSON" ]; then
  error "Sitemap JSON was not created: $SITEMAP_JSON"
  exit 1
fi

# Step 2: Generate PDF
log "Generating PDF..."
java -jar "$JAR" --input "$BUILD_DIR/docs" --output "$OUTPUT_PDF" --sitemap "$SITEMAP_JSON" --css "$PRINT_CSS"
check_file_exists "$OUTPUT_PDF"

# Step 3: Enhance Metadata
log "Enhancing PDF metadata..."
java -jar "$JAR" metadata --input "$OUTPUT_PDF" --title "$TITLE" --toc-levels "$TOC_LEVELS" --description "$DESCRIPTION"
check_file_exists "$OUTPUT_PDF"

# Step 4: Validate PDF
log "Validating PDF..."
java -jar "$JAR" validate --input "$OUTPUT_PDF" --min-pages "$MIN_PAGES" --min-toc-items "$MIN_TOC_ITEMS"

log "✅ Documentation PDF created successfully: $OUTPUT_PDF"
