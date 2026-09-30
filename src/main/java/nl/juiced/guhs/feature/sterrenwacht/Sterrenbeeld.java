package nl.juiced.guhs.feature.sterrenwacht;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;

/**
 * The guh constellations you connect in the telescope of the Guh-Sterrenwacht (the same on both sides): twelve that
 * are always in the night sky and three rare ones that only show during a sterrenregen. Each is a few stars (x, y in
 * 0..1, y down) and the lines between them. The sterrenatlas (Knus collection {@code sterrenatlas}) uses {@link #id()}
 * as its entries; names and texts: {@code gui.guhs.knus.sterrenatlas.<id>} (+ {@code .info}).
 */
public enum Sterrenbeeld {
    /** A big kaasknabbel (a cheese wedge with a hole in it). */
    GROTE_KNABBEL(false, new float[][]{{0.10f, 0.80f}, {0.90f, 0.80f}, {0.74f, 0.22f}, {0.40f, 0.64f}, {0.56f, 0.52f}, {0.64f, 0.70f}},
            new int[][]{{0, 1}, {1, 2}, {2, 0}, {3, 4}, {4, 5}, {5, 3}}),
    /** A little guh head with two ears. */
    KLEINE_VADS(false, new float[][]{{0.22f, 0.12f}, {0.38f, 0.36f}, {0.62f, 0.36f}, {0.78f, 0.12f}, {0.80f, 0.62f}, {0.50f, 0.84f}, {0.20f, 0.62f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {2, 4}, {4, 5}, {5, 6}, {6, 1}}),
    /** One big round guh ear. */
    GUHOOR(false, new float[][]{{0.50f, 0.10f}, {0.85f, 0.36f}, {0.74f, 0.82f}, {0.26f, 0.82f}, {0.15f, 0.36f}, {0.50f, 0.52f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 0}, {0, 5}}),
    /** A Mika running off with a sack of stolen kaasknabbels. */
    VLUCHTENDE_MIKA(false, new float[][]{{0.34f, 0.16f}, {0.40f, 0.38f}, {0.46f, 0.62f}, {0.22f, 0.86f}, {0.72f, 0.84f}, {0.70f, 0.34f}, {0.88f, 0.18f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {2, 4}, {1, 5}, {5, 6}}),
    /** A cheese slicer: a handle and a blade. */
    KAASSCHAAF(false, new float[][]{{0.10f, 0.86f}, {0.34f, 0.62f}, {0.54f, 0.42f}, {0.80f, 0.16f}, {0.92f, 0.34f}, {0.66f, 0.56f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 2}}),
    /** A guh lying asleep, with a Z above it. */
    SLAPENDE_GUH(false, new float[][]{{0.10f, 0.72f}, {0.34f, 0.56f}, {0.64f, 0.58f}, {0.86f, 0.76f}, {0.50f, 0.88f},
            {0.58f, 0.12f}, {0.82f, 0.12f}, {0.58f, 0.34f}, {0.82f, 0.34f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 0}, {5, 6}, {6, 7}, {7, 8}}),
    /** The guh frying pan: a round pan with a long handle. */
    FRITUURPANNETJE(false, new float[][]{{0.14f, 0.50f}, {0.30f, 0.28f}, {0.54f, 0.28f}, {0.66f, 0.50f}, {0.54f, 0.72f}, {0.30f, 0.72f}, {0.94f, 0.46f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 0}, {3, 6}}),
    /** A round, vahoege belly. */
    VAHOEGE_BUIK(false, new float[][]{{0.50f, 0.08f}, {0.80f, 0.20f}, {0.92f, 0.50f}, {0.80f, 0.80f}, {0.50f, 0.92f}, {0.20f, 0.80f}, {0.08f, 0.50f}, {0.20f, 0.20f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 0}}),
    /** A curly guh tail. */
    GUHSTAART(false, new float[][]{{0.08f, 0.82f}, {0.34f, 0.76f}, {0.56f, 0.62f}, {0.62f, 0.36f}, {0.46f, 0.18f}, {0.28f, 0.28f}, {0.34f, 0.46f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}}),
    /** A heart: for everyone you cuddle. */
    KNUFFELHART(false, new float[][]{{0.50f, 0.30f}, {0.30f, 0.12f}, {0.10f, 0.30f}, {0.18f, 0.56f}, {0.50f, 0.88f}, {0.82f, 0.56f}, {0.90f, 0.30f}, {0.70f, 0.12f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 0}}),
    /** A hot-air balloon with its basket (Kapitein Wolkje's favourite). */
    LUCHTBALLON(false, new float[][]{{0.50f, 0.08f}, {0.78f, 0.26f}, {0.70f, 0.56f}, {0.30f, 0.56f}, {0.22f, 0.26f}, {0.42f, 0.84f}, {0.58f, 0.84f}},
            new int[][]{{0, 1}, {1, 2}, {3, 4}, {4, 0}, {2, 6}, {3, 5}, {5, 6}}),
    /** A campfire: a flame on two crossed logs (Opa Guh's favourite). */
    KAMPVUUR(false, new float[][]{{0.50f, 0.08f}, {0.70f, 0.50f}, {0.30f, 0.50f}, {0.14f, 0.88f}, {0.86f, 0.88f}, {0.14f, 0.66f}, {0.86f, 0.66f}},
            new int[][]{{0, 1}, {1, 2}, {2, 0}, {3, 6}, {4, 5}}),
    // --- the rare ones: only during a sterrenregen ------------------------------------------------------------------------
    /** A whole guh face in gold: ears, head and eyes. */
    GOUDEN_GUH(true, new float[][]{{0.18f, 0.08f}, {0.82f, 0.08f}, {0.30f, 0.30f}, {0.70f, 0.30f}, {0.86f, 0.60f}, {0.50f, 0.90f}, {0.14f, 0.60f},
            {0.38f, 0.54f}, {0.62f, 0.54f}},
            new int[][]{{0, 2}, {1, 3}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 2}, {7, 8}}),
    /** A falling kaasknabbel with a long tail. */
    VALLENDE_KNABBEL(true, new float[][]{{0.84f, 0.16f}, {0.68f, 0.32f}, {0.52f, 0.46f}, {0.34f, 0.62f}, {0.10f, 0.88f}, {0.96f, 0.32f}, {0.68f, 0.04f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {0, 5}, {0, 6}}),
    /** The crown of the Koningguh. */
    GUHKROON(true, new float[][]{{0.10f, 0.82f}, {0.90f, 0.82f}, {0.90f, 0.28f}, {0.70f, 0.56f}, {0.50f, 0.14f}, {0.30f, 0.56f}, {0.10f, 0.28f}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 0}});

    public final boolean zeldzaam;
    private final float[][] sterren;
    private final int[][] lijnen;

    Sterrenbeeld(boolean zeldzaam, float[][] sterren, int[][] lijnen) {
        this.zeldzaam = zeldzaam;
        this.sterren = sterren;
        this.lijnen = lijnen;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int sterren() {
        return sterren.length;
    }

    /** Star i: {x, y} in 0..1 (y down). */
    public float[] ster(int i) {
        return sterren[i].clone();
    }

    /** The lines, as pairs of star indices (the lower one first). */
    public List<int[]> lijnen() {
        List<int[]> out = new ArrayList<>();
        for (int[] l : lijnen) {
            out.add(new int[]{Math.min(l[0], l[1]), Math.max(l[0], l[1])});
        }
        return out;
    }

    /** A line as one number (a * 32 + b, a &lt; b), to compare sets. */
    public static int sleutel(int a, int b) {
        return Math.min(a, b) * 32 + Math.max(a, b);
    }

    public Set<Integer> sleutels() {
        Set<Integer> out = new HashSet<>();
        for (int[] l : lijnen) {
            out.add(sleutel(l[0], l[1]));
        }
        return out;
    }

    /** Is a-b one of its lines? */
    public boolean isLijn(int a, int b) {
        return a != b && sleutels().contains(sleutel(a, b));
    }

    public Component naam() {
        return Component.translatable("gui.guhs.knus.sterrenatlas." + id());
    }

    @Nullable
    public static Sterrenbeeld byId(String id) {
        for (Sterrenbeeld b : values()) {
            if (b.id().equals(id)) {
                return b;
            }
        }
        return null;
    }

    @Nullable
    public static Sterrenbeeld byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : null;
    }

    /** The twelve that are always there. */
    public static List<Sterrenbeeld> gewoon() {
        return java.util.Arrays.stream(values()).filter(b -> !b.zeldzaam).toList();
    }

    /** The three that only show during a sterrenregen. */
    public static List<Sterrenbeeld> zeldzame() {
        return java.util.Arrays.stream(values()).filter(b -> b.zeldzaam).toList();
    }

    /** All ids, in atlas order. */
    public static List<String> ids() {
        return java.util.Arrays.stream(values()).map(Sterrenbeeld::id).toList();
    }
}
