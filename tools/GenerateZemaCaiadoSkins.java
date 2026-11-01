import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;

/**
 * 0.022: deterministic 64x64 player-format skins drawn pixel by pixel (classic layout with overlays), with no outside
 * source:
 * <ul>
 *     <li>Romeu Zema: short white hair, light blue dress shirt with rolled sleeves and the orange pin of the Novo,
 *     dark trousers and belt.</li>
 *     <li>Ronaldo Caiado: white hair under a brown cowboy hat (overlay), navy suit, white shirt, blue tie, big belt
 *     buckle and cowboy boots.</li>
 *     <li>Five Direita villagers: terno azul, agroboy (flannel, jeans, hat), camisa amarela, empresário (grey suit, watch)
 *     and mauricinho (polo, chinos, sunglasses).</li>
 * </ul>
 * Run from the project root: java tools/GenerateZemaCaiadoSkins.java
 */
public class GenerateZemaCaiadoSkins {
    static final Path TEXTURES = Path.of("src/main/resources/assets/guerra_ezuberante/textures/entity");
    static BufferedImage img;
    static Random noise;

    /** A box of the skin: base UV origin and size (width, height, depth). */
    record Part(int u, int v, int w, int h, int d) {
        int[] top() { return new int[]{u + d, v, w, d}; }
        int[] bottom() { return new int[]{u + d + w, v, w, d}; }
        int[] right() { return new int[]{u, v + d, d, h}; }
        int[] front() { return new int[]{u + d, v + d, w, h}; }
        int[] left() { return new int[]{u + d + w, v + d, d, h}; }
        int[] back() { return new int[]{u + 2 * d + w, v + d, w, h}; }
        List<int[]> sides() { return List.of(right(), front(), left(), back()); }
        List<int[]> all() { return List.of(top(), bottom(), right(), front(), left(), back()); }
    }

    static final Part HEAD = new Part(0, 0, 8, 8, 8), HAT = new Part(32, 0, 8, 8, 8);
    static final Part BODY = new Part(16, 16, 8, 12, 4), JACKET = new Part(16, 32, 8, 12, 4);
    static final Part RARM = new Part(40, 16, 4, 12, 4), RSLEEVE = new Part(40, 32, 4, 12, 4);
    static final Part LARM = new Part(32, 48, 4, 12, 4), LSLEEVE = new Part(48, 48, 4, 12, 4);
    static final Part RLEG = new Part(0, 16, 4, 12, 4), RPANTS = new Part(0, 32, 4, 12, 4);
    static final Part LLEG = new Part(16, 48, 4, 12, 4), LPANTS = new Part(0, 48, 4, 12, 4);
    static final List<Part> ARMS = List.of(RARM, LARM), LEGS = List.of(RLEG, LLEG);

    // ------------------------------------------------------------------ painting

    /** Slight per-pixel variation, so flat colours look like cloth and skin. */
    static int vary(int color, double amount) {
        if ((color >>> 24) == 0) { return color; }
        double f = 1 + (noise.nextDouble() * 2 - 1) * amount;
        int r = clamp(((color >> 16) & 255) * f), g = clamp(((color >> 8) & 255) * f), b = clamp((color & 255) * f);
        return (color & 0xFF000000) | r << 16 | g << 8 | b;
    }

    static int clamp(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }

    static int shade(int color, double f) {
        return (color & 0xFF000000) | clamp(((color >> 16) & 255) * f) << 16 | clamp(((color >> 8) & 255) * f) << 8 | clamp((color & 255) * f);
    }

    static void px(int x, int y, int color) { img.setRGB(x, y, vary(color, .035)); }

    static void fill(int[] face, int color) {
        for (int y = 0; y < face[3]; y++) for (int x = 0; x < face[2]; x++) { px(face[0] + x, face[1] + y, color); }
    }

    /** Rows y0..y1 of a face. */
    static void rows(int[] face, int y0, int y1, int color) {
        for (int y = y0; y <= y1; y++) for (int x = 0; x < face[2]; x++) { px(face[0] + x, face[1] + y, color); }
    }

    static void at(int[] face, int x, int y, int color) { px(face[0] + x, face[1] + y, color); }

    static void part(Part part, int color) { for (int[] face : part.all()) { fill(face, color); } }

    /** A horizontal band all around a box, rows y0..y1. */
    static void band(Part part, int y0, int y1, int color) { for (int[] face : part.sides()) { rows(face, y0, y1, color); } }

    // ------------------------------------------------------------------ people

    /** Everything a person needs; null colours are not drawn. */
    static final class Look {
        int skin = 0xFFE8B896, hair = 0xFF3B2A1E, eyes = 0xFF3B5B8C, shirt = 0xFFF3F4F6, jacket, tie, pants = 0xFF1F2937,
                shoes = 0xFF2B1B12, belt = 0xFF3F2A1A, buckle, pin, hat, hatBand, glasses, sunglasses, watch, plaid, collar;
        boolean bald, oldAge, rolledSleeves, suit, longHair, cap;
    }

    static void head(Look look) {
        part(HEAD, look.skin);
        int[] front = HEAD.front();
        // Hair: the top, the back down to the neck, the sides above the ears and a fringe line on the front.
        fill(HEAD.top(), look.hair);
        rows(HEAD.back(), 0, look.longHair ? 7 : 5, look.hair);
        for (int[] side : List.of(HEAD.left(), HEAD.right())) {
            rows(side, 0, 2, look.hair);
            for (int y = 3; y <= (look.longHair ? 6 : 4); y++) { at(side, side == HEAD.left() ? 7 : 0, y, look.hair); }
            // Sideburn and ear.
            at(side, side == HEAD.left() ? 1 : 6, 3, look.hair);
            at(side, 3, 4, shade(look.skin, .85));
            at(side, 4, 4, shade(look.skin, .85));
            at(side, 3, 5, shade(look.skin, .8));
        }
        if (look.bald) {
            rows(front, 0, 0, look.skin);
            at(front, 0, 1, look.hair); at(front, 7, 1, look.hair);
        } else {
            rows(front, 0, 0, look.hair);
            at(front, 0, 1, look.hair); at(front, 1, 1, look.hair); at(front, 6, 1, look.hair); at(front, 7, 1, look.hair);
            if (look.longHair) { at(front, 0, 2, look.hair); at(front, 7, 2, look.hair); }
        }
        int skinDark = shade(look.skin, .82), brow = shade(look.hair, look.hair == 0xFFF1F1EE ? .78 : .9);
        // Eyebrows, eyes looking forward, nose, mouth.
        at(front, 1, 3, brow); at(front, 2, 3, brow); at(front, 5, 3, brow); at(front, 6, 3, brow);
        at(front, 1, 4, 0xFFF8F8F8); at(front, 2, 4, look.eyes); at(front, 5, 4, look.eyes); at(front, 6, 4, 0xFFF8F8F8);
        at(front, 3, 5, skinDark); at(front, 4, 5, skinDark);
        int lips = (look.skin & 0xFF000000) | clamp(((look.skin >> 16) & 255) * .82) << 16 | clamp(((look.skin >> 8) & 255) * .58) << 8
                | clamp((look.skin & 255) * .55);
        at(front, 2, 6, skinDark); at(front, 3, 6, lips); at(front, 4, 6, lips); at(front, 5, 6, skinDark);
        if (look.oldAge) {
            // Forehead lines and the lines beside the nose.
            at(front, 2, 2, skinDark); at(front, 3, 2, skinDark); at(front, 5, 2, skinDark);
            at(front, 2, 5, shade(look.skin, .9)); at(front, 5, 5, shade(look.skin, .9));
            at(front, 1, 7, shade(look.skin, .92)); at(front, 6, 7, shade(look.skin, .92));
        }
        rows(HEAD.bottom(), 0, 7, shade(look.skin, .9));
        int[] visor = HAT.front();
        if (look.glasses != 0) {
            for (int x : new int[]{1, 2, 5, 6}) { img.setRGB(visor[0] + x, visor[1] + 3, look.glasses); }
            img.setRGB(visor[0] + 3, visor[1] + 4, look.glasses); img.setRGB(visor[0] + 4, visor[1] + 4, look.glasses);
            img.setRGB(visor[0] + 0, visor[1] + 4, look.glasses); img.setRGB(visor[0] + 7, visor[1] + 4, look.glasses);
        }
        if (look.sunglasses != 0) {
            for (int x : new int[]{1, 2, 3, 4, 5, 6}) { img.setRGB(visor[0] + x, visor[1] + 4, x == 3 || x == 4 ? shade(look.sunglasses, 1.3) : look.sunglasses); }
            img.setRGB(visor[0] + 1, visor[1] + 3, look.sunglasses); img.setRGB(visor[0] + 6, visor[1] + 3, look.sunglasses);
        }
        if (look.hat != 0) { hat(look); }
    }

    /** A hat on the overlay: the crown over the top rows and a brim band a little lower. */
    static void hat(Look look) {
        fill(HAT.top(), look.hat);
        int brim = look.cap ? 2 : 3;
        for (int[] face : HAT.sides()) {
            rows(face, 0, brim - 1, look.hat);
            rows(face, brim, brim, shade(look.hat, .8));
            if (look.hatBand != 0) { rows(face, brim - 1, brim - 1, look.hatBand); }
        }
        // A dent in the crown, and a peak in front for a cap.
        at(HAT.top(), 3, 3, shade(look.hat, .85)); at(HAT.top(), 4, 3, shade(look.hat, .85)); at(HAT.top(), 3, 4, shade(look.hat, .85));
        if (look.cap) { rows(HAT.front(), 2, 2, shade(look.hat, .7)); }
    }

    static void torso(Look look) {
        part(BODY, look.shirt);
        int[] front = BODY.front(), back = BODY.back();
        if (look.plaid != 0) {
            for (int[] face : BODY.sides()) for (int y = 0; y < face[3]; y++) for (int x = 0; x < face[2]; x++) {
                if ((x / 2 + y / 2) % 2 == 0) { at(face, x, y, look.plaid); }
                else if (x % 4 == 1 || y % 4 == 1) { at(face, x, y, shade(look.plaid, .6)); }
            }
        }
        if (look.collar != 0) { rows(front, 0, 0, look.collar); at(front, 3, 1, look.collar); at(front, 4, 1, look.collar); rows(back, 0, 0, look.collar); }
        if (look.suit) {
            // Jacket with a V opening over the shirt, the tie in the middle, two buttons.
            for (int[] face : BODY.sides()) { if (face != front) { fill(face, look.jacket); } }
            for (int y = 0; y < 12; y++) {
                int open = y < 6 ? 2 - y / 3 : 0;
                for (int x = 0; x < 8; x++) {
                    boolean inside = x >= 4 - open - 1 && x <= 3 + open + 1 && y < 6;
                    if (!inside) { at(front, x, y, look.jacket); }
                }
            }
            at(front, 2, 0, shade(look.jacket, .8)); at(front, 5, 0, shade(look.jacket, .8));
            at(front, 2, 1, shade(look.jacket, .8)); at(front, 5, 1, shade(look.jacket, .8));
            if (look.tie != 0) { for (int y = 1; y <= 6; y++) { at(front, 3, y, look.tie); at(front, 4, y, y == 1 ? shade(look.tie, .8) : look.tie); } }
            at(front, 3, 8, shade(look.jacket, .6)); at(front, 3, 10, shade(look.jacket, .6));
            fill(BODY.top(), look.jacket);
        } else {
            // Buttons down the placket.
            for (int y = 2; y <= 9; y += 3) { at(front, 3, y, shade(look.shirt, .82)); }
            if (look.tie != 0) { for (int y = 1; y <= 8; y++) { at(front, 3, y, look.tie); at(front, 4, y, look.tie); } }
        }
        if (look.belt != 0) {
            band(BODY, 11, 11, look.belt);
            if (look.buckle != 0) { at(front, 3, 11, look.buckle); at(front, 4, 11, look.buckle); at(front, 3, 10, look.suit ? look.jacket : look.buckle); }
        }
        if (look.pin != 0) { at(front, 1, 3, look.pin); }
    }

    static void arms(Look look) {
        for (Part arm : ARMS) {
            int sleeve = look.suit ? look.jacket : look.shirt;
            part(arm, sleeve);
            if (look.plaid != 0 && !look.suit) {
                for (int[] face : arm.sides()) for (int y = 0; y < 12; y++) for (int x = 0; x < 4; x++) {
                    if ((x / 2 + y / 2) % 2 == 0) { at(face, x, y, look.plaid); }
                }
            }
            if (look.rolledSleeves) {
                band(arm, 5, 5, shade(look.shirt, .85));
                band(arm, 6, 11, look.skin);
                fill(arm.bottom(), look.skin);
            } else {
                if (look.suit) { band(arm, 10, 10, look.shirt); }
                band(arm, 11, 11, look.skin);
                fill(arm.bottom(), look.skin);
            }
            if (look.watch != 0 && arm == LARM) { band(arm, look.rolledSleeves ? 9 : 10, look.rolledSleeves ? 9 : 10, look.watch); }
        }
        // Short sleeves of a polo or a football shirt: the arms are bare below the sleeve.
    }

    static void legs(Look look) {
        for (Part leg : LEGS) {
            part(leg, look.pants);
            band(leg, 10, 11, look.shoes);
            fill(leg.bottom(), shade(look.shoes, .7));
            // A crease down the front of the trousers.
            for (int y = 0; y < 10; y++) { at(leg.front(), leg == RLEG ? 2 : 1, y, shade(look.pants, .88)); }
        }
    }

    static void shortSleeves(Look look) {
        for (Part arm : ARMS) {
            band(arm, 4, 11, look.skin);
            band(arm, 3, 3, look.collar != 0 ? look.collar : shade(look.shirt, .85));
            fill(arm.bottom(), look.skin);
            if (look.watch != 0 && arm == LARM) { band(arm, 10, 10, look.watch); }
        }
    }

    static void person(String file, Look look, Runnable extra) throws Exception {
        img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        noise = new Random(file.hashCode());
        head(look);
        torso(look);
        arms(look);
        legs(look);
        if (extra != null) { extra.run(); }
        Path out = TEXTURES.resolve(file);
        Files.createDirectories(out.getParent());
        ImageIO.write(img, "png", out.toFile());
    }

    public static void main(String[] args) throws Exception {
        int white = 0xFFF1F1EE, navy = 0xFF1E2A4A;

        var zema = new Look();
        zema.skin = 0xFFEFC4A4; zema.hair = white; zema.eyes = 0xFF4B6B8F; zema.shirt = 0xFFA8C8EC; zema.rolledSleeves = true;
        zema.pants = 0xFF283044; zema.belt = 0xFF4A3220; zema.buckle = 0xFFB8B8B8; zema.pin = 0xFFF97316; zema.oldAge = true;
        zema.shoes = 0xFF3A2416;
        person("romeu_zema.png", zema, null);

        var caiado = new Look();
        caiado.skin = 0xFFE9B994; caiado.hair = white; caiado.eyes = 0xFF4A3B2C; caiado.suit = true; caiado.jacket = navy;
        caiado.shirt = 0xFFF5F5F5; caiado.tie = 0xFF2563EB; caiado.pants = navy; caiado.shoes = 0xFF6B3E1F; caiado.belt = 0xFF4A2A14;
        caiado.buckle = 0xFFD4A62A; caiado.hat = 0xFF8B5A2B; caiado.hatBand = 0xFF3B2412; caiado.oldAge = true; caiado.bald = true;
        person("ronaldo_caiado.png", caiado, () -> {
            // Cowboy boots: taller, with a lighter stitched shaft.
            for (Part leg : LEGS) { band(leg, 8, 9, shade(0xFF6B3E1F, 1.15)); at(leg.front(), 1, 8, 0xFFC08A4A); at(leg.front(), 2, 8, 0xFFC08A4A); }
        });

        var terno = new Look();
        terno.hair = 0xFF2A1D14; terno.suit = true; terno.jacket = 0xFF1D3A8A; terno.tie = 0xFFB91C1C; terno.pants = 0xFF1D3A8A;
        terno.shoes = 0xFF141414; terno.glasses = 0xFF1A1A1A; terno.belt = 0xFF141414;
        person("direitista/direitista_0.png", terno, null);

        var agro = new Look();
        agro.skin = 0xFFD9A47A; agro.hair = 0xFF5A3A22; agro.shirt = 0xFFB91C1C; agro.plaid = 0xFF1F1F1F; agro.pants = 0xFF2F4F7F;
        agro.shoes = 0xFF7A4A24; agro.belt = 0xFF5A3418; agro.buckle = 0xFFD4A62A; agro.hat = 0xFFD8C08A; agro.hatBand = 0xFF5A3418;
        agro.rolledSleeves = true;
        person("direitista/direitista_1.png", agro, null);

        var amarela = new Look();
        amarela.skin = 0xFFC98F66; amarela.hair = 0xFF1A1410; amarela.shirt = 0xFFFACC15; amarela.collar = 0xFF15803D;
        amarela.pants = 0xFF2B4C7E; amarela.shoes = 0xFFF5F5F5; amarela.belt = 0;
        person("direitista/direitista_2.png", amarela, () -> {
            shortSleeves(amarela);
            // A small badge on the chest and a number on the back.
            at(BODY.front(), 5, 2, 0xFF1D4ED8); at(BODY.front(), 6, 2, 0xFF15803D);
            int[] back = BODY.back();
            for (int y = 3; y <= 7; y++) { at(back, 3, y, 0xFF15803D); at(back, 4, y, 0xFF15803D); }
        });

        var empresario = new Look();
        empresario.hair = 0xFF9CA3AF; empresario.oldAge = true; empresario.suit = true; empresario.jacket = 0xFF4B5563;
        empresario.tie = 0; empresario.pants = 0xFF4B5563; empresario.shoes = 0xFF3A2416; empresario.watch = 0xFFD4A62A;
        empresario.belt = 0xFF141414;
        person("direitista/direitista_3.png", empresario, null);

        var mauricinho = new Look();
        mauricinho.skin = 0xFFF0C8A8; mauricinho.hair = 0xFFB8862E; mauricinho.shirt = 0xFFF4A3B4; mauricinho.collar = 0xFFFFFFFF;
        mauricinho.pants = 0xFFD9C7A0; mauricinho.shoes = 0xFF7A4A24; mauricinho.sunglasses = 0xFF111827; mauricinho.belt = 0xFF7A4A24;
        mauricinho.watch = 0xFFE5E7EB;
        person("direitista/direitista_4.png", mauricinho, () -> shortSleeves(mauricinho));

        System.out.println("Generated Romeu Zema, Ronaldo Caiado and 5 direitista skins.");
    }
}
