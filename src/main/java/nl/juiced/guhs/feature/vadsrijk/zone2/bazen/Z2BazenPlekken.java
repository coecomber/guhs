package nl.juiced.guhs.feature.vadsrijk.zone2.bazen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * Every coordinate of the zone-2 bazen slice (Z2_BRIEF: one class), aligned with z2-wereld's POI_Z2.md v1 (generated from the
 * real map; the POI ids are in the comments). Literals, so this branch compiles without z2-wereld: integration can switch them to
 * {@code Z2WereldPlekken.plek("id")}. Everything else (puzzle layouts, hall eyes) is relative to these anchors.
 * <p>Y values: "stand" = the first air block above the floor.
 */
public final class Z2BazenPlekken {
    private Z2BazenPlekken() {
    }

    /** (integration) The spot from z2-wereld's generated Z2WereldPlekken; the numbers are only the fallback. */
    private static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.plek(id);
        return b != null ? b : nl.juiced.guhs.feature.vadsrijk.indeling.VadsrijkZones.z2(x, y, z);   // T-WERELD: local zone-2 fallback
    }

    // ---- De Galmwolf (world boss): the flat snow bowl in the Galmhellingen ------------------------------------------------
    /** POI arena_galmwolf (bowl r 32, floor top 120). */
    public static final BlockPos GALM_CENTRUM = w("arena_galmwolf", 430, 121, -2280);
    public static final int GALM_STRAAL = 32;
    /** POI arena_galmwolf_deur / _bord (west entrance, outside the bowl). */
    public static final BlockPos GALM_DEUR = w("arena_galmwolf_deur", 392, 121, -2280);
    public static final BlockPos GALM_BORD = w("arena_galmwolf_bord", 393, 121, -2276);
    public static final int GALM_RESPAWN = 20 * 60;
    /** POI galmwolf_pilaar_1..8: the centres of wereld's 8 frozen pillars (3x3, 9 high), relative to the arena centre. */
    public static final int[][] GALM_PILAREN = {{19, 4}, {10, 16}, {-4, 19}, {-16, 10}, {-19, -4}, {-10, -16}, {4, -19}, {16, -10}};
    public static final int GALM_PILAAR_HOOG = 9;

    // ---- De Spiegelburcht (the dungeon on the NE peak) ----------------------------------------------------------------------
    /** (fix round 2) The castle was moved as one piece: its v1 layout (x centred on 0, door plane z -2856) + (BURCHT_X, 0, DZ).
     *  Every z below is a world z; every x in the puzzle/room tables is relative to BURCHT_X (use {@link #k}). */
    public static final int BURCHT_X = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.BURCHT_DX;
    public static final int DZ = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.BURCHT_DZ;
    /** The stand level of the whole burcht (floor top 175). */
    public static final int BURCHT_Y = 176;
    /** The front door (WereldDeuren "spiegelburcht", plane z -2856) and where you stand in front of it (deur_spiegelburcht_voor). */
    public static final int BURCHT_DEUR_Z = -2856 + DZ;
    public static final BlockPos BURCHT_VOOR = w("deur_spiegelburcht_voor", BURCHT_X, BURCHT_Y, -2851 + DZ);
    /** Rooms (inner boxes, wereld plan.BURCHT_KAMERS): voorhal x ±16 z -2879..-2857, lichtkamer x ±14 z -2899..-2881, spiegelgang x ±9
     *  z -2917..-2901, prismazaal x ±14 z -2937..-2919. The gates poort_1..4 (open AIR, x -2..2, y +0..+4) are at z -2880, -2900, -2918,
     *  -2938; bazen fills poort_2..4. */
    public static final int VOORHAL_X = 16, VOORHAL_Z0 = -2879 + DZ, VOORHAL_Z1 = -2857 + DZ;
    public static final int POORT_BREED = 2, POORT_HOOG = 4;
    /** De Spiegelgang = the gauntlet (crystal-shard blades + 3 waves); its exit gate is poort_3. */
    public static final int GANG_X = 9, GANG_Z0 = -2917 + DZ, GANG_Z1 = -2901 + DZ, GANG_POORT = -2918 + DZ;
    /** Vrouwe Spiegelzucht's hall (arena_spiegelzucht, r 22, a glass dome from y +14), door arena_spiegelzucht_deur (inside the
     *  prismazaal, before poort_4). */
    public static final BlockPos SPIEGEL_CENTRUM = w("arena_spiegelzucht", BURCHT_X, BURCHT_Y, -2963 + DZ);
    public static final int SPIEGEL_STRAAL = 22;
    public static final BlockPos SPIEGEL_DEUR = w("arena_spiegelzucht_deur", BURCHT_X, BURCHT_Y, -2937 + DZ);
    public static final BlockPos SPIEGEL_BORD = w("arena_spiegelzucht_bord", BURCHT_X + 4, BURCHT_Y, -2936 + DZ);
    public static final int SPIEGEL_RESPAWN = 5 * 60;
    /** The dome (breaks open ring by ring in fase 3): from y +14 to +38 inside radius 24. */
    public static final int KOEPEL_ONDER = 14, KOEPEL_BOVEN = 38, KOEPEL_STRAAL = 24;
    /** POI spiegelzucht_spiegel_1..6: the hall mirrors (radius 17), relative to the centre. */
    public static final int[][] HAL_SPIEGELS = {{15, 9}, {0, 17}, {-15, 9}, {-15, -9}, {0, -17}, {15, -9}};

    /** One crystal eye of the hall: where (relative), the direction it shines (0 N, 1 E, 2 S, 3 W), the mirror (index into
     *  {@link #HAL_SPIEGELS}) on its line, and the spot (relative) where she channels so the beam can reach her. */
    public record Oog(int dx, int dz, int richting, int spiegel, int sdx, int sdz) {
    }

    /** Spot A (0,+9) is reached from the two eyes on the z +9 row (via mirrors 1 and 3 = index 0, 2), spot B (0,-9) from the two
     *  on the z -9 row (mirrors 6 and 4 = index 5, 3). (No eyes on the x 0 line: the entrance and the shard pedestal are there.) */
    public static final List<Oog> HAL_OGEN = List.of(
            new Oog(19, 9, 3, 0, 0, 9),
            new Oog(-19, 9, 1, 2, 0, 9),
            new Oog(19, -9, 3, 5, 0, -9),
            new Oog(-19, -9, 1, 3, 0, -9));

    /** x relative to the castle's centre line, z = world z. */
    public static BlockPos k(int x, int z) {
        return new BlockPos(BURCHT_X + x, BURCHT_Y, z);
    }

    /** x relative, z in the v1 layout (the puzzle tables below). */
    private static BlockPos kv(int x, int zv1) {
        return k(x, zv1 + DZ);
    }

    /** One beam puzzle: room (half width x, z0 north end, z1 south end), the gate it opens (z), the emitters (pos + direction
     *  0 N, 1 E, 2 S, 3 W), the mirrors (pos, start stand, solution stand or -1 = doesn't matter), the locks. */
    public record Puzzel(String id, int x, int z0, int z1, int poortZ, List<BlockPos> bronnen, List<Integer> richtingen, List<BlockPos> spiegels,
            List<Integer> startStanden, List<Integer> oplossing, List<BlockPos> sloten) {
    }

    /** Mirror stands: 0 = "/", 1 = "\", 2 = folded flat (the beam passes). Mirrors stand on wereld's puzzle spots (puzzel_*). */
    public static final List<Puzzel> PUZZELS = List.of(
            // De Lichtkamer: one beam, three turns (puzzel_lichtkamer_3, _1, _2; _4 and _5 are decoys), lock in the NE corner
            new Puzzel("lichtkamer", 14, -2899 + DZ, -2881 + DZ, -2900 + DZ, List.of(kv(-14, -2885)), List.of(1),
                    List.of(kv(-7, -2885), kv(-7, -2894), kv(7, -2894), kv(7, -2885), kv(0, -2890)), List.of(1, 1, 2, 0, 0),
                    List.of(0, 0, 0, -1, -1), List.of(kv(7, -2899))),
            // De Prismazaal: two beams that must light both locks at once (puzzel_prismazaal_2..5; _1 in the middle is a decoy)
            new Puzzel("prismazaal", 14, -2937 + DZ, -2919 + DZ, -2938 + DZ, List.of(kv(-14, -2923), kv(14, -2923)), List.of(1, 3),
                    List.of(kv(-8, -2923), kv(-8, -2933), kv(8, -2923), kv(8, -2933), kv(0, -2928)), List.of(1, 2, 0, 1, 0),
                    List.of(0, 1, 1, 0, -1), List.of(kv(-14, -2933), kv(14, -2933))));

    /** The burcht's whole box (for "is anyone inside", resets, fainting). */
    public static boolean inBurcht(double x, double y, double z) {
        return Math.abs(x - BURCHT_X) <= 26 && z <= BURCHT_DEUR_Z + 1 && z >= SPIEGEL_CENTRUM.getZ() - 26 && y >= BURCHT_Y - 4 && y <= BURCHT_Y + 40;
    }

    // ---- De Mika-boormeester (secret): the cave in the north wall of the Splinterkloof ------------------------------------------
    /** POI arena_boormeester (cave r 20, floor top 65), entrance arena_boormeester_deur. */
    public static final BlockPos BOOR_CENTRUM = w("arena_boormeester", -500, 66, -2836);
    public static final int BOOR_STRAAL = 20;
    public static final BlockPos BOOR_DEUR = w("arena_boormeester_deur", -500, 66, -2779);
    public static final int BOOR_TERUG_SECONDEN = 10 * 60;
    /** POI mika_fort: where the defeated Boormeester's cart rolls off to (the smoke trail points there). */
    public static final BlockPos MIKA_FORT = w("mika_fort", 300, 140, -2885);

    /** (dev / AutoCheck) */
    public static List<BlockPos> galmPilaarPlekken() {
        List<BlockPos> l = new ArrayList<>();
        for (int[] o : GALM_PILAREN) {
            l.add(GALM_CENTRUM.offset(o[0], 0, o[1]));
        }
        return l;
    }
}
