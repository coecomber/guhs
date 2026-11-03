package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * Every coordinate the bazen slice uses (SLICE_BRIEF: one class). POI_VADSRIJK.md did not exist yet when these were chosen:
 * they come from the zone map (renders-vadsrijk/_bron/stad/zonekaart.py, map x/z minus 600) and the Zwamzoet generator
 * (zwamzoet.py ZWAMMEN, scene x/z minus 128, cap top = ground 64 + height). Integration aligns them with the wereld POI file;
 * the requests are in POI_VERZOEKEN.md (section "bazen").
 */
public final class BazenPlekken {
    private BazenPlekken() {
    }

    // ---- Koning Klodder: the arena on the Klodderweide (zonekaart ARENA = (1010, 690)) ----------------------------------
    public static final BlockPos KK_CENTRUM = new BlockPos(410, 65, 90);
    public static final int KK_STRAAL = 28;
    public static final BlockPos KK_DEUR = new BlockPos(410, 65, 122);
    public static final BlockPos KK_BORD = new BlockPos(414, 65, 121);
    public static final int KK_RESPAWN = 20 * 60;

    // ---- De Zwamtempel (berg = map (930,190) -> (330,-410)); entrance south, the rooms go north (-z) ----------------
    public static final int TEMPEL_VLOER = 96;   // the first air block above the temple floor
    /** The big temple door (a plane at z = -360): x 324..336, y 96..115. */
    public static final BlockPos TEMPELDEUR_MIN = new BlockPos(324, TEMPEL_VLOER, -360), TEMPELDEUR_MAX = new BlockPos(336, TEMPEL_VLOER + 19, -360);
    public static final BlockPos TEMPELDEUR_VOOR = new BlockPos(330, TEMPEL_VLOER, -354);
    public static final int TEMPEL_MIN_LEVEL = 16;

    /** Room 1, the Lichtzwamkamer (light the mushrooms in the shown order). */
    public static final BlockPos K1_MIN = new BlockPos(318, TEMPEL_VLOER - 1, -393), K1_MAX = new BlockPos(342, TEMPEL_VLOER + 10, -373);
    public static final List<BlockPos> K1_ZWAMMEN = List.of(new BlockPos(322, TEMPEL_VLOER, -388), new BlockPos(326, TEMPEL_VLOER, -391),
            new BlockPos(330, TEMPEL_VLOER, -392), new BlockPos(334, TEMPEL_VLOER, -391), new BlockPos(338, TEMPEL_VLOER, -388));
    public static final BlockPos K1_POORT_MIN = new BlockPos(328, TEMPEL_VLOER, -394), K1_POORT_MAX = new BlockPos(332, TEMPEL_VLOER + 3, -394);

    /** Room 2, the Hendelkamer (levers, mirrored hint mushrooms, a pressure plate to check). */
    public static final BlockPos K2_MIN = new BlockPos(318, TEMPEL_VLOER - 1, -415), K2_MAX = new BlockPos(342, TEMPEL_VLOER + 10, -395);
    public static final List<BlockPos> K2_HENDELS = List.of(new BlockPos(322, TEMPEL_VLOER, -405), new BlockPos(326, TEMPEL_VLOER, -405),
            new BlockPos(334, TEMPEL_VLOER, -405), new BlockPos(338, TEMPEL_VLOER, -405));
    /** The hint mushrooms on the north wall, seen from the levers they are mirrored (index i hints lever i). */
    public static final List<BlockPos> K2_HINTS = List.of(new BlockPos(338, TEMPEL_VLOER + 5, -414), new BlockPos(334, TEMPEL_VLOER + 5, -414),
            new BlockPos(326, TEMPEL_VLOER + 5, -414), new BlockPos(322, TEMPEL_VLOER + 5, -414));
    public static final BlockPos K2_PLAAT = new BlockPos(330, TEMPEL_VLOER, -412);
    public static final BlockPos K2_POORT_MIN = new BlockPos(328, TEMPEL_VLOER, -416), K2_POORT_MAX = new BlockPos(332, TEMPEL_VLOER + 3, -416);

    /** Room 3, the Ridderzaal (gauntlet). Its entrance is K2's gate; its exit gate leads to the boss hall. */
    public static final BlockPos K3_MIN = new BlockPos(314, TEMPEL_VLOER - 1, -441), K3_MAX = new BlockPos(346, TEMPEL_VLOER + 12, -417);
    public static final List<BlockPos> K3_SPAWNS = List.of(new BlockPos(320, TEMPEL_VLOER, -436), new BlockPos(330, TEMPEL_VLOER, -438),
            new BlockPos(340, TEMPEL_VLOER, -436), new BlockPos(318, TEMPEL_VLOER, -426), new BlockPos(342, TEMPEL_VLOER, -426));
    public static final BlockPos K3_POORT_MIN = new BlockPos(327, TEMPEL_VLOER, -442), K3_POORT_MAX = new BlockPos(333, TEMPEL_VLOER + 4, -442);

    /** The boss hall of the Opperzwam. Its 4 Lichtzwammen are centre + {@link #OZ_LICHT_OFFSETS} (the boss uses offsets). */
    public static final BlockPos OZ_CENTRUM = new BlockPos(330, TEMPEL_VLOER, -462);
    public static final int OZ_STRAAL = 18;
    public static final BlockPos OZ_DEUR = new BlockPos(330, TEMPEL_VLOER, -446);
    public static final BlockPos OZ_BORD = new BlockPos(334, TEMPEL_VLOER, -445);
    public static final int OZ_RESPAWN = 5 * 60;
    public static final int[][] OZ_LICHT_OFFSETS = {{-13, 0, -13}, {13, 0, -13}, {-13, 0, 13}, {13, 0, 13}};
    /** The ceiling that breaks open in phase 3: radius around the centre, y from centre + PLAFOND_ONDER .. + PLAFOND_BOVEN. */
    public static final int OZ_PLAFOND_STRAAL = 11, OZ_PLAFOND_ONDER = 12, OZ_PLAFOND_BOVEN = 20;

    // ---- De Mika-spion: the market stall in Zwamzoet and the caps he flees over -----------------------------------------
    public static final BlockPos MIKA_KRAAM = new BlockPos(-12, 65, 38);
    /** Caps (x, z, fallback top y): zuidw, west, oost (zwamzoet.py ZWAMMEN). The real top is read from the heightmap. */
    public static final int[][] MIKA_HOEDEN = {{-78, 32, 131}, {-66, -30, 153}, {66, -36, 163}};
    public static final int MIKA_HOED_STRAAL = 9;
    public static final int MIKA_TERUG_SECONDEN = 10 * 60;
}
