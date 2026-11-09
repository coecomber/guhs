package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;

/**
 * U-LEVEN-STAD: the guh towns of zones 1-6 that get street life, keyed by their kern area ids (so zone 6, built in another
 * branch, works as soon as its areas exist). The Lampionkust islands are little towns of their own. Every town names its zone (the gossip pool) and
 * whether its streets are snowy (soup instead of ice cream).
 */
public final class Steden {
    /**
     * W-LEVEN: {@code muizen} = the pieppiepmuisje multiplier of this town (regular and pink alike): the share of the critter cap
     * that may be mice, times this. {@link #MUIZEN_STANDAARD} everywhere (the user: "ongeveer 30% van wat ik nu zie"), 1.0 in the
     * Kandijkom ("daar vind ik het een goed aantal"). It also thins the mice among the walkers' pets.
     */
    public record Stad(String id, int zone, Set<String> gebieden, boolean koud, double muizen) {
        public Stad(String id, int zone, Set<String> gebieden, boolean koud) {
            this(id, zone, gebieden, koud, MUIZEN_STANDAARD);
        }

        public String naamKey() {
            return "vadsrijk.stadsleven.stad." + id;
        }
    }

    /** W-LEVEN: the pieppiepmuisje multiplier of every town without its own (see {@link Stad#muizen}). */
    public static final double MUIZEN_STANDAARD = 0.3;
    /** W-LEVEN: towns with their own pieppiepmuisje multiplier (id -> multiplier). */
    static final Map<String, Double> MUIZEN = Map.of("kandijkom", 1.0);

    public static final Map<String, Stad> ALLE = new LinkedHashMap<>();

    static {
        voeg("knuffelkamp", 1, false, "knuffelkamp");
        voeg("zwamzoet", 1, false, "zwamzoet");
        voeg("haven", 1, false, "haven");
        voeg("kandijkom", 2, true, "z2_kandijkom", "z2_kandijkom_dok");
        voeg("honingtak", 3, false, "z3_honingtak");
        voeg("lampionhaven", 4, false, "z4_lampionhaven", "z4_havenplein");
        voeg("knuffeltij", 4, false, "z4_knuffeltij");   // the islands of the Lampionkust: each its own little town (own caps)
        voeg("strandfeest", 4, false, "z4_strandfeest");
        voeg("bloemeneiland", 4, false, "z4_bloemeneiland");
        voeg("vliegereiland", 4, false, "z4_vliegereiland");
        voeg("vuurtoreneiland", 4, false, "z4_vuurtoreneiland");
        voeg("schildpadeiland", 4, false, "z4_schildpad");
        voeg("hoogvads", 5, false, "z5_hoogvads", "z5_marktlaan", "z5_universiteit", "z5_academie", "z5_kathedraal", "z5_staartpark",
                "z5_ooggaarde", "z5_paleisheuvel", "z5_luchthaven");
        voeg("rozenkloof", 6, false, "z6_rozenkloof");   // zone 6 (u-z6wereld): works once its areas are merged
        voeg("saffraanpoort", 6, false, "z6_saffraanpoort");
    }

    private static void voeg(String id, int zone, boolean koud, String... gebieden) {
        ALLE.put(id, new Stad(id, zone, Set.of(gebieden), koud, MUIZEN.getOrDefault(id, MUIZEN_STANDAARD)));
    }

    private Steden() {
    }

    /** The town at this spot (kern areas, zone-3 layers included), or null. */
    /** Gametests: a test town in a box of the test level (null = none). */
    @Nullable
    static Stad testStad;
    @Nullable
    static net.minecraft.world.phys.AABB testDoos;

    @Nullable
    public static Stad op(Vec3 p) {
        if (testStad != null && testDoos != null && testDoos.contains(p)) {
            return testStad;
        }
        Gebieden.Gebied g = Gebieden.op(p.x, p.y, p.z);
        return g == null ? null : vanGebied(g.id());
    }

    @Nullable
    public static Stad vanGebied(String gebied) {
        for (Stad s : ALLE.values()) {
            if (s.gebieden().contains(gebied)) {
                return s;
            }
        }
        return null;
    }

    @Nullable
    public static Stad van(String id) {
        return ALLE.get(id);
    }

    public static List<String> ids() {
        return List.copyOf(ALLE.keySet());
    }
}
