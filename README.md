# PDFGen2

PDFGen2 is a Java CLI tool for generating structured PDF documentation from Docusaurus HTML files, enhancing metadata,
validating PDF structure, and parsing sitemaps—all from a single entrypoint.

## Features

- Generate a PDF from Docusaurus HTML docs using either a sidebar config or a sitemap
- Apply custom CSS for print styling
- Enhance PDF metadata (title, description, TOC levels)
- Validate PDF structure (page count, TOC items)
- Parse Docusaurus sitemap.xml to structured JSON

## Prerequisites

- Java 21+
- Maven
- Node.js (required for parsing Docusaurus sidebars.ts)

## Setup

1. **Clone the repository**
   ```sh
   git clone <your-repo-url>
   cd pdfgen2
   ```
2. **Install dependencies**
   ```sh
   mvn clean install
   ```

## Usage

All CLI operations are now handled by the single entrypoint:

```sh
java -jar target/pdfgen-0.2.0.jar
```

### Subcommands

#### 1. Generate PDF (Recommended: Sitemap-based)

```sh
java -jar target/pdfgen-0.2.0.jar [OPTIONS]
```

- Use `--sitemap` for sitemap-based generation
- Use `--sidebar` for sidebar config (sidebars.ts or sidebars.json)
- Other options: `--input`, `--output`, `--css`, `--no-toc`

#### 2. Enhance PDF Metadata

```sh
java -jar target/pdfgen-0.2.0.jar metadata --input output.pdf --title "My Documentation" --toc-levels 3 --description "Generated from Docusaurus"
```

#### 3. Validate PDF Structure

```sh
java -jar target/pdfgen-0.2.0.jar validate --input output.pdf --min-pages 5 --min-toc-items 10
```

#### 4. Parse Sitemap

```sh
java -jar target/pdfgen-0.2.0.jar sitemap --input build/sitemap.xml --output sitemap-structure.json
```

## Example Workflow

1. **Parse sitemap to JSON**
   ```sh
   java -jar target/pdfgen-0.2.0.jar sitemap --input build/sitemap.xml --output sitemap-structure.json
   ```
2. **Generate PDF**
   ```sh
   java -jar target/pdfgen-0.2.0.jar --input build/docs --sitemap sitemap-structure.json --output output.pdf --css src/css/custom.css
   ```
3. **Enhance Metadata**
   ```sh
   java -jar target/pdfgen-0.2.0.jar metadata --input output.pdf --title "My Documentation" --toc-levels 3 --description "Generated from Docusaurus"
   ```
4. **Validate PDF**
   ```sh
   java -jar target/pdfgen-0.2.0.jar validate --input output.pdf --min-pages 5 --min-toc-items 10
   ```

## Automation

You can use the provided shell scripts to automate the workflow:

- `generate-docs.sh`: Run all steps with custom arguments.
- `generate-docs-default.sh`: Run all steps with default values and folder structure.

## Project Structure

```
pdfgen2/
├── pom.xml
├── README.md
├── generate-docs.sh
├── generate-docs-default.sh
├── src/
│   └── main/
│       └── java/
│           └── tn/
│               └── eternity/
│                   ├── PdfGenerator.java
│                   └── model/
│                       ├── DocumentationPage.java
│                       ├── SidebarItem.java
│                       └── UrlSet.java
├── build/
│   └── sitemap.xml
│   └── docs/
│       └── ...
├── docs/
│   └── sidebars.ts
│   └── ...
```

## Troubleshooting

- Ensure all input paths are correct and files exist
- Use Java 21 or newer
- Node.js is required for parsing sidebars.ts
- If you encounter dependency issues, run `mvn clean install` again

## License

MIT

## Author

Elyes Bensalah
