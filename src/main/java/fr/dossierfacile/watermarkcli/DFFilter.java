package fr.dossierfacile.watermarkcli;

import com.jhlabs.image.TransformFilter;

import java.awt.image.BufferedImage;

// Copie du filtre de distorsion utilise dans dossierfacile-pdf-generator.
public class DFFilter extends TransformFilter {
    private float width;
    private float height;
    private int xFrequency = 12;
    private int yFrequency = 8;
    private int maxDistorsion = 28;

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        this.width = src.getWidth();
        this.height = src.getHeight();
        return super.filter(src, dst);
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        out[0] = x;
        float r = (float) Math.sin(x * xFrequency / width);
        out[1] = y + maxDistorsion * (float) Math.sin(y * yFrequency / height) * r * r;
    }
}
