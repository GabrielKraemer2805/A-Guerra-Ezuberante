import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Original pixel art for 0.013 (no vanilla textures copied):
 * red protester zombie skin, "Faz o L" and yellow book sprites, "Zzz" particle and the Bored effect icon.
 * Run from the project root: java tools/GenerateProgressionAssets.java
 */
public final class GenerateProgressionAssets {
    private static final String ROOT = "src/main/resources/assets/guerra_ezuberante/textures/";

    public static void main(String[] args) throws Exception {
        manifestante();
        fazOL();
        yellowBook();
        zzz();
        boredIcon();
    }

    private static int argb(int a, int r, int g, int b) { return a << 24 | r << 16 | g << 8 | b; }

    private static void save(BufferedImage image, String path) throws Exception {
        File file = new File(ROOT + path);
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
        System.out.println("wrote " + file);
    }

    private static void fill(BufferedImage image, int x0, int y0, int x1, int y1, int color) {
        for (int x = x0; x < x1; x++) for (int y = y0; y < y1; y++) image.setRGB(x, y, color);
    }

    /** 64x64 humanoid layout, every part in shades of red. The hat overlay (32..64, 0..16) stays transparent. */
    private static void manifestante() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(13);
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                if (y < 16 && x >= 32) { continue; }
                int shade = random.nextInt(36);
                image.setRGB(x, y, argb(255, 196 - shade, 18 + shade / 4, 22 + shade / 4));
            }
        }
        // Darker shirt band on the body and arms, darker trousers on the legs: same red, different value.
        for (int x = 16; x < 40; x++) for (int y = 20; y < 32; y++) image.setRGB(x, y, darker(image.getRGB(x, y), .78));
        for (int x = 40; x < 56; x++) for (int y = 20; y < 26; y++) image.setRGB(x, y, darker(image.getRGB(x, y), .78));
        for (int x = 0; x < 16; x++) for (int y = 20; y < 32; y++) image.setRGB(x, y, darker(image.getRGB(x, y), .6));
        // Face (front of the head: 8..16, 8..16): hollow eyes and an open mouth.
        int eye = argb(255, 30, 0, 0), mouth = argb(255, 90, 0, 6);
        fill(image, 9, 11, 11, 13, eye);
        fill(image, 13, 11, 15, 13, eye);
        fill(image, 10, 14, 14, 15, mouth);
        save(image, "entity/manifestante.png");
    }

    private static int darker(int color, double factor) {
        int r = (int) (((color >> 16) & 255) * factor), g = (int) (((color >> 8) & 255) * factor), b = (int) ((color & 255) * factor);
        return argb(255, r, g, b);
    }

    private static void outlined(BufferedImage image, boolean[][] shape, int fill, int edge) {
        int size = shape.length;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (shape[x][y]) { image.setRGB(x, y, fill); continue; }
                boolean near = false;
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx >= 0 && ny >= 0 && nx < size && ny < size && shape[nx][ny]) { near = true; }
                }
                if (near) { image.setRGB(x, y, edge); }
            }
        }
    }

    /** A thick red L, drawn big by the renderer (scale 3). */
    private static void fazOL() throws Exception {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        boolean[][] shape = new boolean[16][16];
        for (int x = 3; x < 7; x++) for (int y = 1; y < 15; y++) shape[x][y] = true;
        for (int x = 3; x < 14; x++) for (int y = 11; y < 15; y++) shape[x][y] = true;
        outlined(image, shape, argb(255, 220, 20, 28), argb(255, 90, 0, 6));
        // Light edge on the left side for some volume.
        for (int y = 1; y < 15; y++) image.setRGB(3, y, argb(255, 255, 92, 92));
        for (int x = 3; x < 14; x++) image.setRGB(x, 11, argb(255, 255, 92, 92));
        save(image, "item/faz_o_l.png");
    }

    /** A yellow rectangular book: cover, spine and the white edge of the pages. */
    private static void yellowBook() throws Exception {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int edge = argb(255, 120, 86, 0), cover = argb(255, 250, 204, 21), spine = argb(255, 202, 150, 8), pages = argb(255, 245, 240, 225);
        fill(image, 3, 1, 13, 15, edge);
        fill(image, 4, 2, 12, 14, cover);
        fill(image, 4, 2, 6, 14, spine);
        fill(image, 12, 3, 13, 14, pages);
        fill(image, 7, 5, 11, 6, edge);
        fill(image, 7, 7, 10, 8, edge);
        save(image, "item/livro_amarelo.png");
    }

    private static void drawZ(BufferedImage image, int x0, int y0, int size, int color) {
        for (int i = 0; i < size; i++) {
            image.setRGB(x0 + i, y0, color);
            image.setRGB(x0 + i, y0 + size - 1, color);
            image.setRGB(x0 + size - 1 - i, y0 + i, color);
        }
    }

    private static void zzz() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        drawZ(image, 1, 1, 6, argb(255, 235, 240, 255));
        drawZ(image, 2, 1, 5, argb(255, 235, 240, 255));
        save(image, "particle/zzz.png");
    }

    private static void boredIcon() throws Exception {
        BufferedImage image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        int yellow = argb(255, 250, 204, 21), dark = argb(255, 120, 86, 0);
        fill(image, 2, 6, 11, 17, dark);
        fill(image, 3, 7, 10, 16, yellow);
        drawZ(image, 10, 1, 6, argb(255, 235, 240, 255));
        drawZ(image, 11, 1, 5, argb(255, 235, 240, 255));
        save(image, "mob_effect/entediado.png");
    }
}
