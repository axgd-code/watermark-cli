package fr.dossierfacile.watermarkcli;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PdfWatermarkEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void watermarkPdf_shouldGenerateOutputPdfWithSamePageCount() throws IOException {
        Path input = tempDir.resolve("input.pdf");
        Path output = tempDir.resolve("out.pdf");
        TestPdfFactory.createSimplePdf(input, "doc-test", 2);

        PdfWatermarkEngine engine = new PdfWatermarkEngine(false, false);
        engine.watermarkPdf(input, output, "WATERMARK-TEST");

        assertThat(Files.exists(output)).isTrue();
        assertThat(Files.size(output)).isGreaterThan(0L);

        try (PDDocument inputDoc = Loader.loadPDF(Files.readAllBytes(input));
             PDDocument outputDoc = Loader.loadPDF(Files.readAllBytes(output))) {
            assertThat(outputDoc.getNumberOfPages()).isEqualTo(inputDoc.getNumberOfPages());
            assertThat(outputDoc.getDocumentInformation().getCreator()).contains("DossierFacile watermark-cli");
        }
    }
}
