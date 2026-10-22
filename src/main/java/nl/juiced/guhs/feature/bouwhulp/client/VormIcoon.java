package nl.juiced.guhs.feature.bouwhulp.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Test round 5: a little side-view silhouette per shape, roof and pattern for the {@link KeuzeRaster} (14 x 14 cells, drawn
 * as squares): a ball is a disc, a dome a half disc, an arch a bow, a heart a heart... Unknown ids get a plain block.
 */
final class VormIcoon {
    private static final int N = 14;

    static void teken(GuiGraphicsExtractor g, String id, int x, int y, int maat, int kleur) {
        int cel = Math.max(1, maat / N);
        int licht = (kleur & 0xFF000000) | mix(kleur, 0xFFFFFF, 0.45f);
        for (int cy = 0; cy < N; cy++) {
            for (int cx = 0; cx < N; cx++) {
                double u = (cx + 0.5) / N * 2 - 1, v = 1 - (cy + 0.5) / N * 2;   // -1..1, v up
                int vul = vorm(id, u, v, cx, cy);
                if (vul > 0) {
                    g.fill(x + cx * cel, y + cy * cel, x + (cx + 1) * cel, y + (cy + 1) * cel, vul == 2 ? licht : kleur);
                }
            }
        }
    }

    private static int mix(int a, int b, float t) {
        int r = (int) ((a >> 16 & 0xFF) * (1 - t) + (b >> 16 & 0xFF) * t), gr = (int) ((a >> 8 & 0xFF) * (1 - t) + (b >> 8 & 0xFF) * t),
                bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | gr << 8 | bl;
    }

    /** 0: empty, 1: the shape, 2: its light side. */
    private static int vorm(String id, double u, double v, int cx, int cy) {
        double r = Math.sqrt(u * u + v * v);
        boolean in = switch (id) {
            case "bol", "rond" -> r < 0.9;
            case "ei" -> u * u / 0.5 + (v - 0.05) * (v - 0.05) / 0.85 < 1;
            case "koepel", "gewelfd" -> v > -0.6 && u * u + (v + 0.6) * (v + 0.6) < 1.2;
            case "cilinder", "vierkant" -> Math.abs(u) < 0.6 && Math.abs(v) < 0.9;
            case "halve_cilinder" -> Math.abs(u) < 0.8 && v < 0.2 && v > -0.8 || u * u + (v - 0.2) * (v - 0.2) < 0.64 && v >= 0.2 && v < 1.0;
            case "boog" -> v > -0.8 && u * u + (v + 0.8) * (v + 0.8) < 1.3 && u * u + (v + 0.8) * (v + 0.8) > 0.55;
            case "tunnel" -> v > -0.8 && u * u + (v + 0.8) * (v + 0.8) < 1.4 && !(Math.abs(u) < 0.5 && v < 0.0);
            case "brug" -> Math.abs(u) < 0.95 && v > 0.1 && v < 0.35 || Math.abs(u) > 0.7 && v <= 0.1 && v > -0.9
                    || v <= 0.1 && v > -0.4 && u * u + (v + 0.9) * (v + 0.9) > 0.6 && Math.abs(u) < 0.95;
            case "piramide", "zadel" -> v > -0.8 && Math.abs(u) < (0.9 - (v + 0.8) * 0.5);
            case "kegel", "tent" -> v > -0.8 && Math.abs(u) < (0.6 - (v + 0.8) * 0.33);
            case "schild" -> v > -0.8 && v < 0.5 && Math.abs(u) < (0.95 - (v + 0.8) * 0.45);
            case "oortjes" -> v > -0.8 && Math.abs(u) < (0.9 - (v + 0.8) * 0.5) || (Math.abs(Math.abs(u) - 0.45) < 0.14 && v > 0.15 && v < 0.65);
            case "ring" -> r < 0.9 && r > 0.55;
            case "rots" -> r < 0.75 + 0.15 * Math.sin(u * 7 + v * 3) && v > -0.8;
            case "heuvel" -> v > -0.8 && u * u / 0.9 + (v + 0.8) * (v + 0.8) / 0.7 < 1;
            case "boomkruin" -> u * u + (v - 0.25) * (v - 0.25) < 0.45 || Math.abs(u) < 0.12 && v < 0 && v > -0.9;
            case "eiland" -> v < 0.3 && v > -0.9 && Math.abs(u) < 0.9 * (v + 0.9) / 1.2 || v >= 0.3 && v < 0.5 && Math.abs(u) < 0.85;
            case "wenteltrap" -> cy >= cx && cy - cx < 4 || cx > 11;
            case "helix" -> Math.abs(u - Math.sin(v * 4) * 0.6) < 0.18;
            case "hart" -> {
                double a = Math.abs(u) - 0.42, b = v - 0.3;
                yield a * a + b * b < 0.2 || v <= 0.3 && Math.abs(u) < (v + 0.9) * 0.75;
            }
            case "guh" -> u * u + (v + 0.1) * (v + 0.1) < 0.55 || (Math.abs(Math.abs(u) - 0.45) < 0.15 && v > 0.45 && v < 0.85);
            case "tekst" -> (cx == 2 || cx == 6) && cy > 3 && cy < 11 || cy == 4 && cx > 2 && cx < 6 || cy == 7 && cx > 2 && cx < 6
                    || (cx == 9 || cx == 12) && cy > 6 && cy < 11 || cy == 7 && cx > 9 && cx < 12 || cy == 10 && cx > 9 && cx < 12;
            case "kasteelmuur" -> Math.abs(u) < 0.95 && v < 0.3 && v > -0.9 || v >= 0.3 && v < 0.65 && Math.abs(u) < 0.95 && (cx / 3) % 2 == 0;
            case "verloop" -> true;
            case "streep", "zebra", "sabeltand" -> (cx + cy / 3) % 4 < 2;
            case "dambord", "pinguin", "panda" -> (cx / 3 + cy / 3) % 2 == 0;
            case "guhgezicht" -> r < 0.85 && !(Math.abs(u) - 0.35 < 0.1 && Math.abs(u) > 0.25 && v > 0.1 && v < 0.35) && !(v < -0.25 && v > -0.4 && Math.abs(u) < 0.35);
            case "giraffe", "luipaard", "jachtluipaard", "rode_panda" -> (cx * 7 + cy * 11) % 9 < 4;
            default -> Math.abs(u) < 0.7 && Math.abs(v) < 0.7;
        };
        if (!in) {
            return 0;
        }
        if (id.equals("verloop")) {
            return cx > 7 ? 2 : 1;
        }
        return u + v > 0.5 ? 2 : 1;   // (the light from the top right)
    }

    private VormIcoon() {
    }
}
