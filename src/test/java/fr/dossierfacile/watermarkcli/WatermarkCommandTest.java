package fr.dossierfacile.watermarkcli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WatermarkCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void command_shouldProcessSinglePdfInput() throws IOException {
        Path input = tempDir.resolve("single.pdf");
        Path output = tempDir.resolve("single-out.pdf");
        TestPdfFactory.createSimplePdf(input, "single", 1);

        int exitCode = new CommandLine(new WatermarkCommand()).execute(
                "--input", input.toString(),
                "--output", output.toString(),
                "--watermark", "CLI-SINGLE"
        );

        assertThat(exitCode).isEqualTo(0);
        assertThat(Files.exists(output)).isTrue();
        assertThat(Files.size(output)).isGreaterThan(0L);
    }

    @Test
    void command_shouldProcessNestedDirectoryAndKeepStructure() throws IOException {
        Path sourceRoot = tempDir.resolve("in");
        Path nestedA = sourceRoot.resolve("a");
        Path nestedB = sourceRoot.resolve("a/b");
        Path outputRoot = tempDir.resolve("out");

        Files.createDirectories(nestedA);
        Files.createDirectories(nestedB);

        Path pdf1 = nestedA.resolve("doc1.pdf");
        Path pdf2 = nestedB.resolve("doc2.pdf");
        Path txt = nestedB.resolve("ignored.txt");

        TestPdfFactory.createSimplePdf(pdf1, "doc1", 1);
        TestPdfFactory.createSimplePdf(pdf2, "doc2", 1);
        Files.writeString(txt, "must stay ignored");

        int exitCode = new CommandLine(new WatermarkCommand()).execute(
                "--input", sourceRoot.toString(),
                "--output", outputRoot.toString(),
                "--watermark", "CLI-DIR"
        );

        assertThat(exitCode).isEqualTo(0);
        assertThat(Files.exists(outputRoot.resolve("a/doc1.pdf"))).isTrue();
        assertThat(Files.exists(outputRoot.resolve("a/b/doc2.pdf"))).isTrue();
        assertThat(Files.exists(outputRoot.resolve("a/b/ignored.txt"))).isFalse();
    }

    @Test
    void command_shouldFailWhenOutputExistsWithoutOverwrite() throws IOException {
        Path input = tempDir.resolve("single.pdf");
        Path output = tempDir.resolve("single-out.pdf");
        TestPdfFactory.createSimplePdf(input, "single", 1);
        Files.writeString(output, "existing");

        int exitCode = new CommandLine(new WatermarkCommand()).execute(
                "--input", input.toString(),
                "--output", output.toString(),
                "--watermark", "CLI-SINGLE"
        );

        assertThat(exitCode).isEqualTo(1);
    }
}
