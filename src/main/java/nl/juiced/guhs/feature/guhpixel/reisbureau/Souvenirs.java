package nl.juiced.guhs.feature.guhpixel.reisbureau;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The table of the 32 souvenirs, the Gouden koffertje and the Koffertje: wall or floor, the block's shape (facing north),
 * its light and its little effect. The same numbers as tools/features/guhpixel_reisbureau_modellen.py, which writes them to
 * data/guhs/reisbureau/vormen.json; the game test PxReisbureauGameTests#souvenirsKloppenMetDeModellen compares the two.
 */
public final class Souvenirs {
    /** What a souvenir does besides standing there (client side only, SouvenirBlock#animateTick). */
    public enum Effect { GEEN, GLINSTER, STOOM, VUUR, GLIM, SNEEUW, MUZIEK }

    public record Soort(String id, boolean muur, double x0, double y0, double z0, double x1, double y1, double z1, int licht, Effect effect) {
        public VoxelShape vorm() {
            return Block.box(x0, y0, z0, x1, y1, z1);
        }

        public boolean zeldzaam() {
            return id.startsWith("zeldzaam_");
        }

        /** One of the 32 album souvenirs (not the koffertjes). */
        public boolean souvenir() {
            return id.startsWith("souvenir_") || zeldzaam();
        }
    }

    public static final List<Soort> ALLE;

    static {
        List<Soort> a = new ArrayList<>();
        s(a, "souvenir_lingsesdijk", true, 0.5, 0.5, 14, 15.5, 15.5, 16, 0, Effect.GEEN);
        s(a, "zeldzaam_lingsesdijk", true, 0.5, 0, 14, 15.5, 16, 16, 0, Effect.GEEN);
        s(a, "souvenir_kaasmarkt", false, 3, 0, 3, 13, 13, 13.5, 0, Effect.GEEN);
        s(a, "zeldzaam_kaasmarkt", false, 3, 0, 3, 13, 8, 13, 0, Effect.GLINSTER);
        s(a, "souvenir_vadswoud", false, 1.5, 0, 2, 14.5, 5.5, 14, 0, Effect.GEEN);
        s(a, "zeldzaam_vadswoud", false, 5, 0, 5, 11, 12.5, 11, 10, Effect.GLIM);
        s(a, "souvenir_knuffeldal", false, 4, 0, 1.5, 12, 11, 14.5, 0, Effect.GEEN);
        s(a, "zeldzaam_knuffeldal", false, 4, 0, 4, 12, 14.5, 12, 0, Effect.GLINSTER);
        s(a, "souvenir_guhwaii", false, 2, 0, 2, 14, 13, 14, 0, Effect.GEEN);
        s(a, "zeldzaam_guhwaii", false, 3, 0, 5, 13, 16, 11, 0, Effect.GLINSTER);
        s(a, "souvenir_barbecuether", false, 1.5, 0, 2.5, 14.5, 9.5, 13.5, 0, Effect.GEEN);
        s(a, "zeldzaam_barbecuether", false, 1.5, 0, 3.5, 14.5, 10, 12.5, 0, Effect.STOOM);
        s(a, "souvenir_efteguh", false, 2.5, 0, 1.5, 13.5, 16, 13, 0, Effect.GEEN);
        s(a, "zeldzaam_efteguh", false, 2, 0, 2, 14, 12.5, 14, 4, Effect.MUZIEK);
        s(a, "souvenir_guhkenhof", false, 5, 0, 5.5, 11, 14.5, 10.5, 0, Effect.GEEN);
        s(a, "zeldzaam_guhkenhof", false, 5, 0, 5.5, 11, 14.5, 10.5, 0, Effect.GLINSTER);
        s(a, "souvenir_nomguh", false, 3, 0, 3, 13, 12, 13, 0, Effect.GEEN);
        s(a, "zeldzaam_nomguh", false, 3, 0, 3, 13, 12, 13, 0, Effect.SNEEUW);
        s(a, "souvenir_guhrijs", false, 3, 0, 3, 13, 16, 13, 0, Effect.GEEN);
        s(a, "zeldzaam_guhrijs", false, 3, 0, 3, 13, 16, 13, 12, Effect.GLINSTER);
        s(a, "souvenir_camping", false, 1.5, 0, 2, 14.5, 9, 14, 0, Effect.GEEN);
        s(a, "zeldzaam_camping", false, 2.5, 0, 4, 15.5, 8, 12, 13, Effect.VUUR);
        s(a, "souvenir_guhnetie", false, 1, 0, 4, 15, 11.5, 12, 0, Effect.GEEN);
        s(a, "zeldzaam_guhnetie", true, 3, 3.5, 13.5, 13, 16, 16, 0, Effect.GEEN);
        s(a, "souvenir_kaasmaan", false, 3, 0, 3, 13, 10.5, 13, 0, Effect.GEEN);
        s(a, "zeldzaam_kaasmaan", false, 4, 0, 4, 12, 16, 12, 0, Effect.GLINSTER);
        s(a, "souvenir_wereldreis", false, 3, 0, 4, 12, 13, 12, 0, Effect.GEEN);
        s(a, "zeldzaam_wereldreis", false, 3, 0, 4, 12, 13, 12, 0, Effect.GLINSTER);
        s(a, "souvenir_cruise", false, 2, 0, 5, 16, 8, 11, 0, Effect.GEEN);
        s(a, "zeldzaam_cruise", true, 1.5, 1.5, 14, 14.5, 15, 16, 0, Effect.GEEN);
        s(a, "souvenir_balkonie", true, 0.5, 0.5, 14, 15.5, 15.5, 16, 0, Effect.GEEN);
        s(a, "zeldzaam_balkonie", true, 1.5, 4.5, 15, 14.5, 14, 16, 0, Effect.GEEN);
        s(a, "gouden_koffertje", false, 3, 0, 4.5, 13, 9, 11, 0, Effect.GLINSTER);
        s(a, "koffertje", false, 3, 0, 4.5, 13, 9, 11, 0, Effect.GEEN);
        ALLE = List.copyOf(a);
    }

    private static void s(List<Soort> a, String id, boolean muur, double x0, double y0, double z0, double x1, double y1, double z1, int licht, Effect effect) {
        a.add(new Soort(id, muur, x0, y0, z0, x1, y1, z1, licht, effect));
    }

    private Souvenirs() {
    }
}
