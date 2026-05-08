# watermark-cli

Language: [Francais](README.md) | English

Java CLI to apply a text watermark to PDF files, either as a single file operation or as a bulk recursive directory process.

## Why this project?

- Process one PDF or a full directory tree (recursive)
- Preserve output folder structure
- Protect QR code areas
- Display CLI progress for long-running tasks (files/pages)
- Reuse a battle-tested open source algorithm

## Inspiration

This project is inspired by the DossierFacile repository:
https://github.com/MTES-MCT/dossierfacile-backend

More specifically, the implementation follows the logic from the `dossierfacile-pdf-generator` module, especially the `BOPdfDocumentTemplate` class (including its distortion filter), to reuse a robust existing approach instead of reinventing the algorithm.

What this CLI reuses:

- rendering PDF pages as images
- applying repeated diagonal watermark text with gaussian blur
- protecting QR code zones (local watermark removal)
- rebuilding a final PDF page by page

## Requirements

- Java 21+
- Maven 3.9+

## Build

```bash
mvn clean package
```

Generated artifact:

```bash
target/watermark-cli.jar
```

## Quick Start

```bash
java -jar target/watermark-cli.jar \
  --input /path/to/source/document.pdf \
  --output /path/to/output/document-watermarked.pdf \
  --watermark "RENTAL FILE - FIRSTNAME LASTNAME"
```

## Detailed Usage

### 1. Process a single PDF

```bash
java -jar target/watermark-cli.jar \
  --input /path/to/source/document.pdf \
  --output /path/to/output/document-watermarked.pdf \
  --watermark "RENTAL FILE - FIRSTNAME LASTNAME"
```

You can also provide an existing output directory when input is a single file:

```bash
java -jar target/watermark-cli.jar \
  -i /path/to/source/document.pdf \
  -o /path/to/output/ \
  -w "RENTAL FILE - FIRSTNAME LASTNAME"
```

### 2. Process a directory (recursive)

```bash
java -jar target/watermark-cli.jar \
  --input /path/to/source/directories \
  --output /path/to/output/watermarked-directories \
  --watermark "RENTAL FILE - FIRSTNAME LASTNAME"
```

Behavior:

- recursive traversal of all nested directories
- processing of all `*.pdf` files
- recreation of the same directory structure in output
- non-PDF files are ignored

### 3. Overwrite existing output files

By default, the command fails if the output PDF already exists.

To allow overwrite:

```bash
java -jar target/watermark-cli.jar \
  -i /path/to/source \
  -o /path/to/output \
  -w "MY WATERMARK" \
  --overwrite
```

## Options

- `-i, --input` (required): path to a PDF file or directory
- `-o, --output` (required): output PDF file path or output directory path
- `-w, --watermark` (required): watermark text
- `--overwrite` (optional): overwrite existing files
- `--use-colors` (optional): enable color watermark variant
- `--use-distortion` (optional): enable watermark distortion

## Tests

Run tests:

```bash
mvn test
```

Current test coverage:

- engine test: generate a watermarked PDF and verify page count
- CLI test for single-file processing
- CLI test for recursive directory processing with structure preservation
- CLI error test when output exists without `--overwrite`

## Technical Notes

- The rendering/watermark pipeline intentionally follows the DossierFacile implementation.
- The project scope is intentionally limited to PDF input files (no direct image input).
- Output is a newly rasterized and watermarked PDF.

## License

See the `LICENSE` file.
