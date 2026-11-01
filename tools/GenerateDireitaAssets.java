import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;

/**
 * 0.020: deterministic assets of the Direita characters and the castle guards.
 * <ul>
 *     <li>Augusto Cury's drone: GeckoLib model (body, camera, four arms, four rotors), animations ("fly" with spinning
 *     rotors and a gentle bob, "dive" for the bomb drone, "shoot" with a small recoil) and one 32x32 texture per drone type.</li>
 *     <li>16x16 item sprites of Enéias's bombinha and diploma.</li>
 *     <li>A 64x64 player-format skin for the tower guards of the factions without villagers of their own: an ordinary
 *     villager's brown robe, green eyes and a long nose.</li>
 * </ul>
 * Run from the project root: java tools/GenerateDireitaAssets.java
 */
public class GenerateDireitaAssets {
    static final Path ASSETS = Path.of("src/main/resources/assets/guerra_ezuberante");

    public static void main(String[] args) throws Exception {
        droneModel();
        droneAnimations();
        for (String[] type : new String[][]{{"atirador", "6B7280"}, {"bomba", "B91C1C"}, {"regeneracao", "16A34A"}}) {
            droneTexture(type[0], Integer.parseInt(type[1], 16));
        }
        bombinha();
        diploma();
        guardSkin();
        System.out.println("Generated drone model, 3 animations, 3 drone textures, bombinha, diploma and the villager guard skin.");
    }

    // ------------------------------------------------------------------ JSON

    static Map<String, Object> map(Object... pairs) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) { map.put((String) pairs[i], pairs[i + 1]); }
        return map;
    }

    static List<Double> vec(double x, double y, double z) { return List.of(round(x), round(y), round(z)); }
    static double round(double v) { return Math.round(v * 1000) / 1000.0; }

    static String json(Object value, int indent) {
        if (value instanceof String s) { return "\"" + s + "\""; }
        if (value instanceof Map<?, ?> m) {
            var entries = new ArrayList<String>();
            for (var e : m.entrySet()) { entries.add(" ".repeat(indent + 2) + json(e.getKey(), 0) + ": " + json(e.getValue(), indent + 2)); }
            return "{\n" + String.join(",\n", entries) + "\n" + " ".repeat(indent) + "}";
        }
        if (value instanceof List<?> l) { return "[" + String.join(", ", l.stream().map(v -> json(v, indent)).toList()) + "]"; }
        return value.toString();
    }

    static void write(String relative, Object value) throws Exception {
        Path file = ASSETS.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, json(value, 0) + "\n");
    }

    // ------------------------------------------------------------------ drone model

    static Map<String, Object> cube(double x, double y, double z, double w, double h, double d, int u, int v) {
        return map("origin", vec(x, y, z), "size", vec(w, h, d), "uv", List.of(u, v));
    }

    static Map<String, Object> bone(String name, String parent, double[] pivot, List<Object> cubes) {
        var bone = map("name", name);
        if (parent != null) { bone.put("parent", parent); }
        bone.put("pivot", vec(pivot[0], pivot[1], pivot[2]));
        if (!cubes.isEmpty()) { bone.put("cubes", cubes); }
        return bone;
    }

    /** A quadcopter about 12 pixels wide floating at the entity's centre; 32x32 box UV. */
    static void droneModel() throws Exception {
        var bones = new ArrayList<Object>();
        bones.add(bone("root", null, new double[]{0, 0, 0}, List.of()));
        bones.add(bone("body", "root", new double[]{0, 4, 0}, List.of(
                cube(-2.5, 3, -2.5, 5, 2, 5, 0, 0),
                cube(-1, 2, -3.5, 2, 2, 1, 0, 7))));
        // Each arm points along +x and is turned 45, 135, 225 and 315 degrees: the motor and the rotor ride at its tip.
        for (int index = 0; index < 4; index++) {
            String arm = "arm_" + index, rotor = "rotor_" + index;
            var armBone = bone(arm, "body", new double[]{0, 4, 0}, List.of(
                    cube(2, 3.5, -.5, 4.4, 1, 1, 20, 0),
                    cube(5.4, 3.25, -1, 2, 1.5, 2, 20, 4)));
            armBone.put("rotation", vec(0, 45 + 90 * index, 0));
            bones.add(armBone);
            bones.add(bone(rotor, arm, new double[]{6.4, 5, 0}, List.of(
                    cube(3.4, 5, -.5, 6, .25, 1, 0, 11),
                    cube(5.9, 5.05, -3, 1, .25, 6, 14, 11))));
        }        var geometry = map("description", map("identifier", "geometry.guerra.drone", "texture_width", 32, "texture_height", 32,
                "visible_bounds_width", 2, "visible_bounds_height", 2, "visible_bounds_offset", vec(0, .5, 0)), "bones", bones);
        write("geo/drone.geo.json", map("format_version", "1.12.0", "minecraft:geometry", List.of(geometry)));
    }

    static Map<String, Object> frames(double length, int steps, java.util.function.DoubleFunction<List<Double>> value) {
        var frames = new LinkedHashMap<String, Object>();
        for (int i = 0; i <= steps; i++) {
            double t = length * i / steps;
            frames.put(String.format(Locale.ROOT, "%.3f", t), value.apply(t));
        }
        return frames;
    }

    static void droneAnimations() throws Exception {
        var animations = new LinkedHashMap<String, Object>();
        // Rotors: a full turn every .2 s (four turns per loop), opposite pairs spinning the other way.
        java.util.function.Function<java.util.function.DoubleFunction<List<Double>>, Map<String, Object>> withRotors = body -> {
            var bones = new LinkedHashMap<String, Object>();
            bones.put("body", map("rotation", frames(.8, 16, body)));
            for (int i = 0; i < 4; i++) {
                int sign = i == 0 || i == 3 ? 1 : -1;
                bones.put("rotor_" + i, map("rotation", frames(.8, 16, t -> vec(0, sign * 360 * t / .2, 0))));
            }
            return bones;
        };
        var fly = withRotors.apply(t -> vec(3 * Math.sin(t / .8 * 2 * Math.PI), 0, 2 * Math.cos(t / .8 * 2 * Math.PI)));
        fly.put("root", map("position", frames(.8, 16, t -> vec(0, .6 * Math.sin(t / .8 * 2 * Math.PI), 0))));
        animations.put("animation.guerra.drone_fly", map("loop", true, "animation_length", .8, "bones", fly));
        // Bomb drone diving at its target: nose down, rotors at full speed.
        var dive = withRotors.apply(t -> vec(-35, 0, 4 * Math.sin(t * 20)));
        animations.put("animation.guerra.drone_dive", map("loop", true, "animation_length", .8, "bones", dive));
        // Shot: the body kicks back and settles in .25 s.
        var shoot = withRotors.apply(t -> vec(t < .25 ? 14 * (1 - t / .25) : 0, 0, 0));
        animations.put("animation.guerra.drone_shoot", map("loop", false, "animation_length", .8, "bones", shoot));
        write("animations/drone.animation.json", map("format_version", "1.8.0", "animations", animations));
    }

    // ------------------------------------------------------------------ pixel art

    static void fill(BufferedImage image, int x, int y, int w, int h, int rgb) {
        for (int i = x; i < x + w; i++) { for (int j = y; j < y + h; j++) { image.setRGB(i, j, 0xFF000000 | rgb); } }
    }

    static int shade(int rgb, double factor) {
        int r = (int) Math.min(255, ((rgb >> 16) & 255) * factor), g = (int) Math.min(255, ((rgb >> 8) & 255) * factor);
        int b = (int) Math.min(255, (rgb & 255) * factor);
        return r << 16 | g << 8 | b;
    }

    static void save(BufferedImage image, String relative) throws Exception {
        Path file = ASSETS.resolve(relative);
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
    }

    /** Body in the type colour with a darker frame, black camera, grey arms and motors, light rotor blades. */
    static void droneTexture(String name, int color) throws Exception {
        var image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        fill(image, 0, 0, 20, 7, shade(color, .7));
        fill(image, 5, 0, 5, 5, color);
        fill(image, 6, 1, 3, 3, shade(color, 1.25));
        fill(image, 5, 5, 5, 2, color);
        fill(image, 0, 7, 6, 3, 0x111111);
        fill(image, 2, 8, 1, 1, 0x60A5FA);
        fill(image, 20, 0, 8, 3, 0x4B5563);
        fill(image, 20, 4, 8, 4, 0x1F2937);
        fill(image, 0, 11, 14, 2, 0xD1D5DB);
        fill(image, 14, 11, 14, 8, 0xD1D5DB);
        fill(image, 0, 12, 1, 1, color);
        save(image, "textures/entity/drone_" + name + ".png");
    }

    static void sprite(String[] rows, Map<Character, Integer> palette, String relative) throws Exception {
        var image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                Integer rgb = palette.get(rows[y].charAt(x));
                if (rgb != null) { image.setRGB(x, y, 0xFF000000 | rgb); }
            }
        }
        save(image, relative);
    }

    /** A round black bomb with a highlight and a lit fuse. */
    static void bombinha() throws Exception {
        sprite(new String[]{
                "..........YO....",
                ".........Y.O....",
                "........FF......",
                ".......FF.......",
                ".....GGGGG......",
                "...BBBBBBBBB....",
                "..BBWWBBBBBBB...",
                ".BBWWBBBBBBBBB..",
                ".BBWBBBBBBBBBB..",
                ".BBBBBBBBBBBBB..",
                ".BBBBBBBBBBBBB..",
                ".BBBBBBBBBBBDB..",
                "..BBBBBBBBBDB...",
                "...BBBBBBBDB....",
                ".....BBBBB......",
                "................"},
                Map.of('B', 0x1F1F24, 'W', 0x9CA3AF, 'D', 0x0B0B0E, 'G', 0x4B5563, 'F', 0x8B6B3A, 'Y', 0xFDE047, 'O', 0xF97316),
                "textures/item/bombinha.png");
    }

    /** A rolled diploma tied with a red ribbon and a gold seal. */
    static void diploma() throws Exception {
        sprite(new String[]{
                "................",
                "............EE..",
                "...........EPPE.",
                "..........EPPPE.",
                ".........EPPPE..",
                "........EPPPE...",
                ".......EPRRE....",
                "......EPRRRE....",
                ".....EPPRRE.....",
                "....EPPPYYE.....",
                "...EPPPEYY......",
                "..EPPPE.........",
                ".EPPPE..........",
                ".EPPE...........",
                "..EE............",
                "................"},
                Map.of('P', 0xF5ECD7, 'E', 0xA8977A, 'R', 0xC1121F, 'Y', 0xEAB308),
                "textures/item/diploma.png");
    }

    /**
     * Player-format (classic 64x64) skin of an ordinary villager for the tower guards of the factions without villagers
     * of their own: bald head, unibrow, green eyes, the long nose drawn on the face, brown robe with a darker belt.
     */
    static void guardSkin() throws Exception {
        var image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int skin = 0xB98766, skinDark = 0x9C6F51, robe = 0x6B4A2E, robeDark = 0x4E3520, belt = 0x2E1F12, shoe = 0x3B2A1B;
        // Head: every face skin, then the face details on the front (8,8)-(15,15).
        fill(image, 0, 0, 32, 16, 0);
        fill(image, 8, 0, 16, 8, skinDark);
        fill(image, 0, 8, 32, 8, skin);
        fill(image, 8, 9, 8, 1, 0x4A3222);
        fill(image, 9, 11, 2, 1, 0xFFFFFF);
        fill(image, 13, 11, 2, 1, 0xFFFFFF);
        fill(image, 10, 11, 1, 1, 0x1C7C2E);
        fill(image, 13, 11, 1, 1, 0x1C7C2E);
        fill(image, 11, 11, 2, 4, 0xA4714F);
        fill(image, 11, 14, 2, 1, 0x8E5F41);
        fill(image, 9, 14, 2, 1, skinDark);
        fill(image, 13, 14, 2, 1, skinDark);
        // Body and arms: robe, belt across the middle; legs: robe down to dark shoes.
        fill(image, 16, 16, 24, 16, robe);
        fill(image, 20, 16, 8, 4, robeDark);
        fill(image, 16, 25, 24, 1, belt);
        fill(image, 40, 16, 16, 16, robe);
        fill(image, 44, 16, 8, 4, robeDark);
        fill(image, 40, 28, 16, 4, skin);
        fill(image, 48, 16, 4, 4, skin);
        fill(image, 32, 48, 16, 16, robe);
        fill(image, 36, 48, 8, 4, robeDark);
        fill(image, 32, 60, 16, 4, skin);
        fill(image, 40, 48, 4, 4, skin);
        fill(image, 0, 16, 16, 16, robeDark);
        fill(image, 0, 29, 16, 3, shoe);
        fill(image, 4, 16, 8, 4, robeDark);
        fill(image, 16, 48, 16, 16, robeDark);
        fill(image, 16, 61, 16, 3, shoe);
        fill(image, 20, 48, 8, 4, robeDark);
        // Clear the overlay areas: transparent, like a skin without a second layer.
        for (int[] area : new int[][]{{32, 0, 32, 16}, {16, 32, 24, 16}, {40, 32, 16, 16}, {0, 32, 16, 16}, {0, 48, 16, 16}, {48, 48, 16, 16}}) {
            for (int x = area[0]; x < area[0] + area[2]; x++) { for (int y = area[1]; y < area[1] + area[3]; y++) { image.setRGB(x, y, 0); } }
        }
        // Unused corners of the base layer stay transparent too.
        for (int[] area : new int[][]{{0, 0, 8, 8}, {24, 0, 8, 8}, {0, 16, 4, 4}, {12, 16, 8, 4}, {36, 16, 8, 4}, {52, 16, 4, 4},
                {56, 16, 8, 16}, {16, 48, 4, 4}, {28, 48, 8, 4}, {44, 48, 4, 4}}) {
            for (int x = area[0]; x < area[0] + area[2]; x++) { for (int y = area[1]; y < area[1] + area[3]; y++) { image.setRGB(x, y, 0); } }
        }
        fill(image, 4, 16, 4, 4, robeDark);
        fill(image, 8, 16, 4, 4, shoe);
        fill(image, 20, 48, 4, 4, robeDark);
        fill(image, 24, 48, 4, 4, shoe);
        fill(image, 36, 48, 4, 4, robeDark);
        fill(image, 40, 48, 4, 4, skin);
        fill(image, 44, 16, 4, 4, robeDark);
        fill(image, 48, 16, 4, 4, skin);
        fill(image, 20, 16, 8, 4, robeDark);
        fill(image, 28, 16, 8, 4, robeDark);
        fill(image, 8, 0, 8, 8, skinDark);
        fill(image, 16, 0, 8, 8, skinDark);
        save(image, "textures/entity/guarda_aldeao.png");
    }
}
