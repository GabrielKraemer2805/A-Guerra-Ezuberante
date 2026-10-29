import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Original banner and shield pattern masks for the 0.014 faction flags (no vanilla textures copied):
 * star, small star, dollar sign and the map of Brazil. White opaque pixels are dyed in game; the rest is transparent.
 * Run from the project root: java tools/GenerateVillageAssets.java
 */
public final class GenerateVillageAssets {
    private static final String ROOT = "src/main/resources/assets/guerra_ezuberante/textures/entity/";
    /** Banner front face: x 1..20, y 1..40; the back face (x 22..41) mirrors it. Shield front: x 1..12, y 1..22. */
    private static final int BANNER_W = 20, BANNER_H = 40, SHIELD_W = 12, SHIELD_H = 22;

    /** Cifrão, 10 x 16: an S with a bar through it. */
    private static final String[] DOLLAR = {
            "....##....",
            "..######..",
            ".#######..",
            "##..##..##",
            "##..##....",
            "##..##....",
            ".#######..",
            "..#######.",
            "....##..##",
            "....##..##",
            "....##..##",
            "##..##..##",
            ".#######..",
            "..######..",
            "....##....",
            "....##....",
    };

    /** Simplified outline of Brazil, longitude/latitude, clockwise from Roraima. */
    private static final double[][] BRAZIL = {
            {-60.0, 5.2}, {-56.5, 1.9}, {-54.0, 2.3}, {-51.6, 4.2}, {-50.0, 1.8}, {-48.5, -1.0}, {-44.0, -2.4},
            {-40.0, -2.9}, {-35.2, -5.4}, {-34.8, -7.5}, {-35.3, -9.5}, {-37.0, -11.0}, {-38.9, -13.0},
            {-39.2, -17.7}, {-40.9, -21.9}, {-43.1, -23.0}, {-46.4, -24.0}, {-48.5, -25.9}, {-48.6, -28.5},
            {-50.2, -30.9}, {-52.1, -32.3}, {-53.4, -33.7}, {-53.8, -32.0}, {-55.6, -30.8}, {-57.6, -30.2},
            {-55.7, -28.1}, {-54.6, -25.6}, {-54.3, -24.0}, {-55.4, -23.9}, {-57.8, -22.1}, {-58.2, -20.1},
            {-57.5, -18.2}, {-58.3, -16.3}, {-60.2, -15.9}, {-60.4, -13.5}, {-62.0, -13.0}, {-65.3, -10.9},
            {-66.6, -9.9}, {-69.0, -11.0}, {-70.5, -11.0}, {-72.6, -10.0}, {-73.9, -7.3}, {-73.0, -5.0},
            {-70.0, -4.2}, {-69.4, -1.1}, {-69.8, 1.7}, {-67.3, 2.2}, {-66.9, 1.2}, {-64.0, 2.0}, {-63.4, 3.9},
            {-61.0, 4.5},
    };

    interface Mask { boolean at(double x, double y, int width, int height); }

    public static void main(String[] args) throws Exception {
        write("estrela", (x, y, w, h) -> star(x, y, w / 2.0, w == BANNER_W ? 14 : 9, w == BANNER_W ? 8.6 : 5.2));
        write("estrela_pequena", (x, y, w, h) -> star(x, y, w / 2.0, w == BANNER_W ? 9 : 6, w == BANNER_W ? 4.2 : 2.7));
        write("cifrao", GenerateVillageAssets::dollar);
        write("brasil", GenerateVillageAssets::brazil);
    }

    private static void write(String id, Mask mask) throws Exception {
        BufferedImage banner = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        paint(banner, mask, BANNER_W, BANNER_H, true);
        save(banner, "banner/" + id + ".png");
        BufferedImage shield = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        paint(shield, mask, SHIELD_W, SHIELD_H, false);
        save(shield, "shield/" + id + ".png");
    }

    private static void paint(BufferedImage image, Mask mask, int width, int height, boolean back) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!mask.at(x + .5, y + .5, width, height)) { continue; }
                image.setRGB(1 + x, 1 + y, 0xFFFFFFFF);
                if (back) { image.setRGB(1 + width + 1 + (width - 1 - x), 1 + y, 0xFFFFFFFF); }
            }
        }
    }

    private static void save(BufferedImage image, String path) throws Exception {
        File file = new File(ROOT + path);
        file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
        System.out.println("wrote " + file);
    }

    /** Five-pointed star, one point up. */
    private static boolean star(double x, double y, double cx, double cy, double outer) {
        double[][] points = new double[10][];
        for (int i = 0; i < 10; i++) {
            double radius = i % 2 == 0 ? outer : outer * .4;
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            points[i] = new double[]{cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius};
        }
        return inside(points, x, y);
    }

    private static boolean dollar(double x, double y, int width, int height) {
        int rows = DOLLAR.length, cols = DOLLAR[0].length();
        boolean banner = width == BANNER_W;
        int left = (width - cols) / 2, top = banner ? 6 : 3;
        int col = (int) Math.floor(x) - left, row = (int) Math.floor(y) - top;
        return col >= 0 && col < cols && row >= 0 && row < rows && DOLLAR[row].charAt(col) == '#';
    }

    private static boolean brazil(double x, double y, int width, int height) {
        boolean banner = width == BANNER_W;
        double size = banner ? 18 : 11, left = (width - size) / 2, top = banner ? 4 : 3;
        double minLon = -74.0, maxLat = 5.3, span = 39.5;
        double[][] points = new double[BRAZIL.length][];
        for (int i = 0; i < BRAZIL.length; i++) {
            points[i] = new double[]{left + (BRAZIL[i][0] - minLon) / span * size, top + (maxLat - BRAZIL[i][1]) / span * size};
        }
        return inside(points, x, y);
    }

    /** Even-odd point in polygon. */
    private static boolean inside(double[][] polygon, double x, double y) {
        boolean in = false;
        for (int i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            double xi = polygon[i][0], yi = polygon[i][1], xj = polygon[j][0], yj = polygon[j][1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) { in = !in; }
        }
        return in;
    }
}
