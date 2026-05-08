package fr.dossierfacile.watermarkcli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "watermark-cli",
        mixinStandardHelpOptions = true,
        version = "watermark-cli 1.0.0",
        description = "Applique un watermark texte sur un PDF ou un dossier de PDF (recursif)."
)
public final class WatermarkCommand implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, required = true, description = "PDF source ou dossier source")
    private Path inputPath;

    @Option(names = {"-o", "--output"}, required = true, description = "PDF de sortie ou dossier de sortie")
    private Path outputPath;

    @Option(names = {"-w", "--watermark"}, required = true, description = "Texte du watermark")
    private String watermark;

    @Option(names = "--overwrite", defaultValue = "false", description = "Autorise l'ecrasement des fichiers de sortie")
    private boolean overwrite;

    @Option(names = "--use-colors", defaultValue = "false", description = "Active la variante couleur du watermark")
    private boolean useColors;

    @Option(names = "--use-distortion", defaultValue = "false", description = "Active la distorsion du watermark")
    private boolean useDistortion;

    @Override
    public Integer call() {
        try {
            validateInput();
            PdfWatermarkEngine engine = new PdfWatermarkEngine(useColors, useDistortion);

            if (Files.isRegularFile(inputPath)) {
                processSingleFile(engine);
            } else {
                processDirectory(engine);
            }

            return 0;
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            return 1;
        }
    }

    private void validateInput() {
        if (!Files.exists(inputPath)) {
            throw new IllegalArgumentException("Le chemin d'entree n'existe pas: " + inputPath);
        }
        if (watermark == null || watermark.isBlank()) {
            throw new IllegalArgumentException("Le texte de watermark ne peut pas etre vide.");
        }
    }

    private void processSingleFile(PdfWatermarkEngine engine) throws IOException {
        if (!isPdf(inputPath)) {
            throw new IllegalArgumentException("Le fichier d'entree doit etre un PDF: " + inputPath);
        }

        Path resolvedOutput;
        if (Files.exists(outputPath) && Files.isDirectory(outputPath)) {
            resolvedOutput = outputPath.resolve(inputPath.getFileName());
        } else if (outputPath.toString().toLowerCase().endsWith(".pdf")) {
            resolvedOutput = outputPath;
        } else {
            throw new IllegalArgumentException("Pour un fichier source, --output doit etre un fichier .pdf ou un dossier existant.");
        }

        writeOne(engine, inputPath, resolvedOutput, 1, 1);
        clearProgress();
        System.out.println("OK: " + inputPath + " -> " + resolvedOutput);
    }

    private void processDirectory(PdfWatermarkEngine engine) throws IOException {
        Files.createDirectories(outputPath);
        if (!Files.isDirectory(outputPath)) {
            throw new IllegalArgumentException("Le chemin de sortie doit etre un dossier pour un input dossier.");
        }

        final Path sourceRoot = inputPath;

        // Passe 1 : compter les PDF pour afficher un progress total.
        List<Path> pdfs = new ArrayList<>();
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isPdf(file)) pdfs.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        int total = pdfs.size();

        // Passe 2 : traitement avec progression.
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            private int processed = 0;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceRoot.relativize(dir);
                Files.createDirectories(outputPath.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isPdf(file)) {
                    processed++;
                    Path relative = sourceRoot.relativize(file);
                    Path target = outputPath.resolve(relative);
                    writeOne(engine, file, target, processed, total);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        clearProgress();
        System.out.println("OK: " + total + " PDF traite(s).");
    }

    private void writeOne(PdfWatermarkEngine engine, Path source, Path target,
                          int fileIndex, int fileTotal) throws IOException {
        if (Files.exists(target) && !overwrite) {
            throw new IllegalArgumentException("Le fichier existe deja (utilisez --overwrite): " + target);
        }

        Files.createDirectories(target.getParent());

        String fileName = source.getFileName().toString();
        engine.watermarkPdf(source, target, watermark, (page, totalPages) ->
                printProgress(fileIndex, fileTotal, fileName, page, totalPages));
    }

    /** Affiche une ligne de progression ecrasable (\r). */
    private static void printProgress(int fileIndex, int fileTotal,
                                      String fileName, int page, int totalPages) {
        String fileLabel = fileTotal > 1
                ? String.format("[%d/%d] ", fileIndex, fileTotal)
                : "";
        String pageLabel = totalPages > 1
                ? String.format(" [page %d/%d]", page, totalPages)
                : "";
        // Tronque le nom de fichier si necessaire pour eviter les lignes trop longues.
        String name = fileName.length() > 40 ? "..." + fileName.substring(fileName.length() - 37) : fileName;
        String line = fileLabel + name + pageLabel;
        System.out.print("\r" + line);
        System.out.flush();
    }

    /** Efface la ligne de progression avant d'afficher un message final. */
    private static void clearProgress() {
        System.out.print("\r" + " ".repeat(80) + "\r");
        System.out.flush();
    }

    private boolean isPdf(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".pdf");
    }
}
