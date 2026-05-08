package fr.dossierfacile.watermarkcli;

public final class PageDimension {
    public static final PageDimension A4_150 = new PageDimension(1240, 1754, 150);

    public final int width;
    public final int height;
    public final int dpi;

    public PageDimension(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }
}
