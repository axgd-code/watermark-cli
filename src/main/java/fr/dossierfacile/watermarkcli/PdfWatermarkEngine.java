package fr.dossierfacile.watermarkcli;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PdfWatermarkEngine {
    private static final PageDimension MAX_PAGE = PageDimension.A4_150;

    private static final Color[] COLORS = {
            new Color(64, 64, 64, 255),
            new Color(32, 32, 32, 220),
            new Color(0, 0, 0, 110),
            new Color(0, 0, 91, 170),
            new Color(255, 0, 0, 170)
    };

    private final boolean useColors;
    private final boolean useDistortion;

    public PdfWatermarkEngine(boolean useColors, boolean useDistortion) {
        this.useColors = useColors;
        this.useDistortion = useDistortion;
    }

    /**
     * Callback de progression : (pageActuelle, totalPages) -&gt; void.
     * Appele apres le rendu de chaque page.
     */
    @FunctionalInterface
    public interface PageProgressListener {
        void onPage(int current, int total);
    }

    public void watermarkPdf(Path inputPdf, Path outputPdf, String watermarkText) throws IOException {
        watermarkPdf(inputPdf, outputPdf, watermarkText, (current, total) -> {});
    }

    public void watermarkPdf(Path inputPdf, Path outputPdf, String watermarkText,
                              PageProgressListener onPage) throws IOException {
        String watermarkToApply = watermarkText + "   ";

        try (PDDocument input = Loader.loadPDF(Files.readAllBytes(inputPdf));
             PDDocument output = new PDDocument()) {

            PDFRenderer renderer = new PDFRenderer(input);
            PDPageTree pagesTree = input.getPages();
            int totalPages = pagesTree.getCount();

            for (int i = 0; i < totalPages; i++) {
                PDRectangle pageMediaBox = pagesTree.get(i).getMediaBox();
                float scale = getScale(pageMediaBox);
                BufferedImage page = renderer.renderImage(i, scale * 2, ImageType.RGB);
                BufferedImage fitted = fitImageToPage(page);
                BufferedImage watermarked = applyWatermark(fitted, watermarkToApply);
                addImageAsPageToDocument(output, watermarked);
                onPage.onPage(i + 1, totalPages);
            }

            PDDocumentInformation information = new PDDocumentInformation();
            information.setCreator("DossierFacile watermark-cli");
            information.setCreationDate(Calendar.getInstance());
            information.setModificationDate(Calendar.getInstance());
            information.setCustomMetadataValue("generated-at", Instant.now().toString());
            output.setDocumentInformation(information);

            Files.createDirectories(outputPdf.getParent());
            output.save(outputPdf.toFile());
        }
    }

    private static ConvolveOp getGaussianBlurFilter(int radius, boolean horizontal) {
        int size = radius * 2 + 1;
        float[] data = new float[size];

        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;

        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += data[index];
        }

        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }

        Kernel kernel = horizontal ? new Kernel(size, 1, data) : new Kernel(1, size, data);
        return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
    }

    private float getScale(PDRectangle pageMediaBox) {
        float ratioImage = pageMediaBox.getHeight() / pageMediaBox.getWidth();
        float ratioPdf = PDRectangle.A4.getHeight() / PDRectangle.A4.getWidth();

        PageDimension dimension = ratioImage < ratioPdf
                ? new PageDimension((int) pageMediaBox.getWidth(), (int) (pageMediaBox.getWidth() * ratioPdf), 0)
                : new PageDimension((int) (pageMediaBox.getHeight() / ratioPdf), (int) pageMediaBox.getHeight(), 0);

        return (dimension.width < MAX_PAGE.width) ? 1f : MAX_PAGE.width / pageMediaBox.getWidth();
    }

    // Reprise de l'algorithme de BOPdfDocumentTemplate.applyWatermark().
    private BufferedImage applyWatermark(BufferedImage bim, String watermarkText) {
        int diagonal = (int) Math.sqrt(bim.getWidth() * bim.getWidth() + bim.getHeight() * bim.getHeight());
        BufferedImage watermarkLayer = new BufferedImage(diagonal, diagonal, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = watermarkLayer.createGraphics();
        String watermark = watermarkText.repeat(1 + (128 / watermarkText.length()));

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ThreadLocalRandom.current().nextFloat(0.52f, 0.6f)));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float spaceBetweenText = diagonal / ThreadLocalRandom.current().nextFloat(8f, 10f);
        for (int i = 1; i < 11; i++) {
            Font font = new Font("Arial", Font.PLAIN, 28 * bim.getWidth() / MAX_PAGE.width);
            if (useColors) {
                g.setColor(COLORS[ThreadLocalRandom.current().nextInt(0, COLORS.length)]);
            } else {
                g.setColor(Color.DARK_GRAY);
            }
            g.setFont(font);
            g.drawString(watermark, 0, i * spaceBetweenText);
        }

        int radius = ThreadLocalRandom.current().nextInt(45, 65);
        BufferedImage blurredTextLayer = new BufferedImage(diagonal, diagonal, BufferedImage.TYPE_INT_ARGB);
        Graphics2D blurredTextLayerGraphics = blurredTextLayer.createGraphics();
        blurredTextLayerGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ThreadLocalRandom.current().nextFloat(0.75f, 0.95f)));
        blurredTextLayerGraphics.drawImage(watermarkLayer, 0, 0, null);
        blurredTextLayer = getGaussianBlurFilter(radius, true).filter(blurredTextLayer, null);
        blurredTextLayer = getGaussianBlurFilter(radius, false).filter(blurredTextLayer, null);
        blurredTextLayerGraphics.dispose();

        Graphics2D gf = bim.createGraphics();
        gf.drawImage(bim, 0, 0, null);

        BufferedImage buffer;
        if (useDistortion) {
            DFFilter filter = new DFFilter();
            buffer = filter.filter(watermarkLayer, null);
        } else {
            buffer = watermarkLayer;
        }

        BufferedImage rotated = new BufferedImage(diagonal, diagonal, buffer.getType());
        Graphics2D graphic = rotated.createGraphics();
        graphic.rotate(Math.toRadians(-25), diagonal / 2f, diagonal / 2f);
        graphic.drawImage(buffer, null, 0, 0);
        graphic.drawImage(blurredTextLayer, 0, 0, null);
        graphic.dispose();

        BufferedImage cropedRotated = rotated.getSubimage(
                diagonal / 2 - bim.getWidth() / 2,
                diagonal / 2 - bim.getHeight() / 2,
                diagonal / 2 + bim.getWidth() / 2,
                diagonal / 2 + bim.getHeight() / 2
        );

        List<Rectangle> qrCodes = detectQRCodes(bim);
        Graphics2D graphics = cropedRotated.createGraphics();
        graphics.setComposite(AlphaComposite.Clear);
        int magicBorderSize = 20;
        for (Rectangle qrCode : qrCodes) {
            int x = Math.max(qrCode.x - magicBorderSize, 0);
            int y = Math.max(qrCode.y - magicBorderSize, 0);
            int w = Math.min(qrCode.width + magicBorderSize * 2, cropedRotated.getWidth() - x);
            int h = Math.min(qrCode.height + magicBorderSize * 2, cropedRotated.getHeight() - y);
            graphics.fillRect(x, y, w, h);
        }
        graphics.dispose();

        gf.drawImage(cropedRotated, 0, 0, null);
        gf.dispose();

        return bim;
    }

    private List<Rectangle> detectQRCodes(BufferedImage image) {
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            MultipleBarcodeReader reader = new GenericMultipleBarcodeReader(new MultiFormatReader());
            Result[] results = reader.decodeMultiple(bitmap);
            List<Rectangle> rectangles = new ArrayList<>();
            for (Result result : results) {
                Rectangle rect = toRectangle(result.getResultPoints());
                if (rect != null) {
                    rectangles.add(rect);
                }
            }
            return rectangles;
        } catch (NotFoundException e) {
            return List.of();
        }
    }

    private Rectangle toRectangle(ResultPoint[] points) {
        if (points == null || points.length == 0) {
            return null;
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (ResultPoint point : points) {
            minX = Math.min(minX, point.getX());
            minY = Math.min(minY, point.getY());
            maxX = Math.max(maxX, point.getX());
            maxY = Math.max(maxY, point.getY());
        }

        int width = Math.max(1, Math.round(maxX - minX));
        int height = Math.max(1, Math.round(maxY - minY));
        return new Rectangle(Math.round(minX), Math.round(minY), width, height);
    }

    private BufferedImage fitImageToPage(BufferedImage bim) {
        float ratioImage = bim.getHeight() / (float) bim.getWidth();
        float ratioPdf = PDRectangle.A4.getHeight() / PDRectangle.A4.getWidth();

        PageDimension dimension = (ratioImage < ratioPdf)
                ? new PageDimension(bim.getWidth(), (int) (bim.getWidth() * ratioPdf), 0)
                : new PageDimension((int) (bim.getHeight() / ratioPdf), bim.getHeight(), 0);

        float scale = (dimension.width < MAX_PAGE.width) ? 1f : MAX_PAGE.width / (float) bim.getWidth();

        AffineTransform affineTransform = new AffineTransform();
        affineTransform.translate(scale * (dimension.width - bim.getWidth()) / 2, scale * (dimension.height - bim.getHeight()) / 2);
        affineTransform.scale(scale, scale);

        BufferedImage resultImage = new BufferedImage(
                (int) (scale * dimension.width),
                (int) (scale * dimension.height),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = resultImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, (int) (scale * dimension.width), (int) (scale * dimension.height));
        g.drawImage(bim, affineTransform, null);
        g.dispose();

        return resultImage;
    }

    private void addImageAsPageToDocument(PDDocument document, BufferedImage bim) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIOUtil.writeImage(bim, "jpg", out, MAX_PAGE.dpi, 0.9f);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, out.toByteArray(), "");
            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.OVERWRITE,
                    true,
                    true
            )) {
                contentStream.drawImage(
                        pdImage,
                        0,
                        0,
                        PDRectangle.A4.getWidth(),
                        bim.getHeight() * PDRectangle.A4.getWidth() / bim.getWidth()
                );
            }
        }
    }
}
