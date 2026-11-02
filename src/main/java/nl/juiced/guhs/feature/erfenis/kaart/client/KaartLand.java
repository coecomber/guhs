package nl.juiced.guhs.feature.erfenis.kaart.client;

import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.erfenis.kaart.KaartDorp;

/**
 * The pastel world of the map, drawn once into a dynamic texture: soft mint/pink land islets on light blue/lilac water.
 * The land is a "metaball" field around every village (so every village stands on land and clusters become islands) plus
 * a few empty islets, roughed up with value noise from the world's map seed. Remade only when villages appear or move.
 */
final class KaartLand {
    static final Identifier ID = Guhs.id("dynamic/erfenis_kaart_land");
    /** The water outside the texture (the deepest water colour). */
    static final int DIEP = 0xFF9DB3E8;
    private static final float DREMPEL = 0.45f;

    final int minCx, minCz, breedte, hoogte, px;
    final long sleutel;
    private DynamicTexture tex;

    private KaartLand(int minCx, int minCz, int breedte, int hoogte, int px, long sleutel) {
        this.minCx = minCx;
        this.minCz = minCz;
        this.breedte = breedte;
        this.hoogte = hoogte;
        this.px = px;
        this.sleutel = sleutel;
    }

    int texW() {
        return breedte * px;
    }

    int texH() {
        return hoogte * px;
    }

    /** A key for "the same land": the seed and every village's cell. */
    static long sleutel(long seed, List<KaartDorp> dorpen) {
        long h = seed;
        for (KaartDorp d : dorpen) {
            h = h * 31 + (d.cx() * 73856093L ^ d.cz() * 19349663L);
        }
        return h;
    }

    static KaartLand maak(long seed, List<KaartDorp> dorpen) {
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        for (KaartDorp d : dorpen) {
            minX = Math.min(minX, d.cx());
            maxX = Math.max(maxX, d.cx());
            minZ = Math.min(minZ, d.cz());
            maxZ = Math.max(maxZ, d.cz());
        }
        int marge = 5;
        minX -= marge;
        minZ -= marge;
        maxX += marge;
        maxZ += marge;
        int w = maxX - minX + 1, h = maxZ - minZ + 1;
        if (w < 28) {   // never a tiny map: a bit of sea around even one village
            minX -= (28 - w) / 2;
            w = 28;
        }
        if (h < 28) {
            minZ -= (28 - h) / 2;
            h = 28;
        }
        int px = Math.max(6, Math.min(16, 1400 / Math.max(w, h)));
        KaartLand land = new KaartLand(minX, minZ, w, h, px, sleutel(seed, dorpen));
        land.teken(seed, dorpen);
        return land;
    }

    private void teken(long seed, List<KaartDorp> dorpen) {
        int tw = texW(), th = texH();
        float[] veld = new float[tw * th];
        float sigma = 0.95f * px;
        for (KaartDorp d : dorpen) {
            blob(veld, tw, th, (d.cx() - minCx + 0.5f) * px, (d.cz() - minCz + 0.5f) * px, sigma, 1f);
        }
        Random r = new Random(seed ^ 0x6C616E64L);
        int leeg = 8 + breedte * hoogte / 90;
        for (int i = 0; i < leeg; i++) {   // empty islets and sandbanks, for the looks
            blob(veld, tw, th, r.nextFloat() * tw, r.nextFloat() * th, (0.35f + r.nextFloat() * 0.8f) * px, 0.55f + r.nextFloat() * 0.35f);
        }
        Ruis ruis = new Ruis(seed);
        NativeImage img = new NativeImage(tw, th, false);
        for (int y = 0; y < th; y++) {
            for (int x = 0; x < tw; x++) {
                float fx = x / (float) px, fy = y / (float) px;
                float v = veld[y * tw + x] + (ruis.fbm(fx / 2.3f, fy / 2.3f) - 0.5f) * 0.75f;
                img.setPixel(x, y, kleur(v, fx, fy, ruis, x, y));
            }
        }
        tex = new DynamicTexture(() -> "guhs erfenis kaart", img);
        Minecraft.getInstance().getTextureManager().register(ID, tex);
    }

    private static void blob(float[] veld, int tw, int th, float cx, float cy, float sigma, float sterkte) {
        int r = (int) Math.ceil(sigma * 3);
        float tweeS2 = 2 * sigma * sigma;
        for (int y = Math.max(0, (int) cy - r); y < Math.min(th, (int) cy + r + 1); y++) {
            for (int x = Math.max(0, (int) cx - r); x < Math.min(tw, (int) cx + r + 1); x++) {
                float dx = x + 0.5f - cx, dy = y + 0.5f - cy;
                veld[y * tw + x] += sterkte * (float) Math.exp(-(dx * dx + dy * dy) / tweeS2);
            }
        }
    }

    private int kleur(float v, float fx, float fy, Ruis ruis, int x, int y) {
        int hash = Ruis.hash(x, y, 77);
        if (v < DREMPEL - 0.06f) {   // water: deeper = more lilac-blue, near the shore lighter
            float t = Math.max(0, Math.min(1, v / (DREMPEL - 0.06f)));
            int c = mix(DIEP, 0xFFBEDCF6, t * t);
            c = mix(c, 0xFFB6A8EC, Math.max(0, ruis.fbm(fx / 5f + 40, fy / 5f) - 0.45f) * 0.9f);   // lilac drifts
            // towards the edge of the texture the water fades into the plain sea around it (no seam when zoomed out)
            int rand = Math.min(Math.min(x, texW() - 1 - x), Math.min(y, texH() - 1 - y));
            c = mix(DIEP, c, Math.min(1f, rand / (3f * px)));
            if (rand < 3 * px) {
                return c;
            }
            if (t < 0.7f && (hash & 1023) < 6) {
                c = mix(c, 0xFFFFFFFF, 0.55f);   // a glint on the water
            } else if (((int) (fy * 3 + Math.sin(fx * 1.7) * 1.2) & 7) == 0 && (hash & 3) == 0 && t > 0.25f) {
                c = mix(c, 0xFFFFFFFF, 0.18f);   // soft ripples near the islands
            }
            return c;
        }
        if (v < DREMPEL) {
            return 0xFFEAF7FC;   // foam
        }
        if (v < DREMPEL + 0.08f) {
            return (hash & 15) == 0 ? 0xFFF6E2BE : 0xFFFFEFD2;   // sand
        }
        float n = ruis.fbm(fx / 1.6f + 13, fy / 1.6f - 7);
        int c = mix(0xFFBEE8C6, 0xFFA5DAB3, Math.max(0, Math.min(1, (n - 0.35f) * 2.5f)));
        float bloesem = ruis.fbm(fx / 2.8f - 31, fy / 2.8f + 5);
        if (bloesem > 0.62f) {
            c = mix(c, 0xFFF7CFE0, Math.min(1, (bloesem - 0.62f) * 9));   // blossom meadows
        }
        if (v > 1.25f) {
            c = mix(c, 0xFFE2F5DF, 0.45f);   // little hills
        }
        if ((hash & 255) < 5 && v > DREMPEL + 0.2f) {
            c = 0xFF7FC495;   // a tree
        } else if ((hash & 511) < 3 && v > DREMPEL + 0.2f) {
            c = 0xFFFFF59A;   // a flower
        }
        return c;
    }

    static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }

    void sluit() {
        if (tex != null) {
            Minecraft.getInstance().getTextureManager().release(ID);
            tex = null;
        }
    }

    /** Small seeded value noise (smooth, 0..1) with a few octaves. */
    static final class Ruis {
        private final int zaad;

        Ruis(long seed) {
            this.zaad = (int) (seed ^ seed >>> 32);
        }

        static int hash(int x, int y, int z) {
            int h = x * 374761393 + y * 668265263 + z * 1442695041;
            h = (h ^ h >>> 13) * 1274126177;
            return h ^ h >>> 16;
        }

        private float rooster(int x, int y) {
            return (hash(x, y, zaad) & 0xFFFF) / 65535f;
        }

        float waarde(float x, float y) {
            int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
            float tx = x - x0, ty = y - y0;
            tx = tx * tx * (3 - 2 * tx);
            ty = ty * ty * (3 - 2 * ty);
            float a = rooster(x0, y0), b = rooster(x0 + 1, y0), c = rooster(x0, y0 + 1), d = rooster(x0 + 1, y0 + 1);
            return a + (b - a) * tx + (c - a) * ty + (a - b - c + d) * tx * ty;
        }

        float fbm(float x, float y) {
            return (waarde(x, y) * 0.55f + waarde(x * 2.1f + 17, y * 2.1f) * 0.3f + waarde(x * 4.3f, y * 4.3f + 9) * 0.15f);
        }
    }
}
