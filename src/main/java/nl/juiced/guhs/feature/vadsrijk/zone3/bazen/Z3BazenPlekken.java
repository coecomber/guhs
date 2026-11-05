package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Every coordinate of the zone-3 bazen slice (Z3_BRIEF: one class). Zone 3 lies in the EAST (REVAMP, ronde 20: box x 1900..2700,
 * z -400..400, trunk axis T = (2300, 0); POI_Z3.md v2: the werf on its own island in the north-east, the Pelszaal under the
 * Schorsstad floor, the Hart a ring of rooms round the roots hall, the nest on top of the trunk). Each spot is looked up first in z3-wereld's generated {@code Z3WereldPlekken.plek(id)} (by
 * reflection, so this branch compiles without z3-wereld); the numbers are only the fallback, taken from ZONE3_PLAN (mirrored to
 * the east). The POI ids we need are requested in POI_VERZOEKEN_Z3.md (section z3-bazen). Layouts inside an arena (cages, lantern
 * posts, rings...) are offsets from the arena centre, so they follow the arena when wereld moves it.
 * <p>Y values: "stand" = the first air block above the floor.
 */
public final class Z3BazenPlekken {
    private Z3BazenPlekken() {
    }

    /** (K-INT) z3-wereld's spot (direct reference); the numbers are only used for an id it lacks. */
    static BlockPos w(String id, int x, int y, int z) {
        BlockPos b = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.plek(id);
        return b != null ? b : new BlockPos(x - 2300 + nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_X, y, z + nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_Z);   // (v2-frame fallback, shifted)
    }

    /** The trunk axis (from the map: zone 3 can move). */
    public static final BlockPos STAM = new BlockPos(nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_X, 64, nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_Z);

    /** The offset of POI {@code id} from {@code c} (layouts inside an arena follow the arena). */
    static int[] rel(BlockPos c, String id, int x, int y, int z) {
        BlockPos b = w(id, x, y, z);
        return new int[] {b.getX() - c.getX(), b.getY() - c.getY(), b.getZ() - c.getZ()};
    }

    // ---- De Kruinroofvogel (world boss, lv 38): the Kale Kruin, ~200 blocks from the Kruintop -----------------------------
    public static final BlockPos ROOF_CENTRUM = w("arena_kruinroofvogel", 2509, 268, -80);
    public static final int ROOF_STRAAL = 22;
    public static final BlockPos ROOF_BORD = w("arena_kruinroofvogel_bord", 2489, 268, -60);
    /** (request: arena_kruinroofvogel_deur) where fainted fighters wake: by the board, outside the bowl. */
    public static final BlockPos ROOF_DEUR = w("arena_kruinroofvogel_deur", 2489, 268, -60);
    public static final int ROOF_RESPAWN = 25 * 60;
    /** His two other perches on the crown (the chase phase), each a small flat spot. */
    public static final BlockPos ROOF_ZIT_1 = w("kruinroofvogel_zit_1", 2359, 262, -250);
    public static final BlockPos ROOF_ZIT_2 = w("kruinroofvogel_zit_2", 2549, 247, 110);
    public static final int ZIT_STRAAL = 20;

    // ---- Kerker 1: De Kaaszeppelinwerf (lv 38-40): its own island in the north-east, three docks far apart -------------------
    /** The dungeon box (the z3_zeppelinwerf island, centre ~(2551, -251) r 95): the yard is at ~160, the docks 168 / 192 / 224;
     *  WERF_Y0 = the island top (the safety net catches falls below it), WERF_Y1 = above the highest deck. */
    public static final int WERF_X0 = werf(0), WERF_X1 = werf(1), WERF_Z0 = werf(2), WERF_Z1 = werf(3), WERF_Y0 = 156, WERF_Y1 = 242;

    /** The werf island box from the map's z3_zeppelinwerf cylinder (minX, maxX, minZ, maxZ); v2-frame fallback. */
    private static int werf(int i) {
        for (var g : nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.GEBIEDEN) {
            if (g.id().equals("z3_zeppelinwerf")) {
                int[] b = g.cilinder() ? new int[] {g.a() - g.d(), g.a() + g.d(), g.b() - g.d(), g.b() + g.d()} : new int[] {g.a(), g.c(), g.b(), g.d()};
                return b[i];
            }
        }
        int sx = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_X - 2300, sz = nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_Z;
        return new int[] {2456 + sx, 2646 + sx, -346 + sz, -156 + sz}[i];
    }
    public static final BlockPos WERF_INGANG = w("werf_ingang", 2861, 169, -537);
    public static final BlockPos WERF_ALTAAR = w("werf_vahoeg_altaar", 2864, 169, -540);
    /** De Gieterij (fondue gutters): a box around the room centre; the gutters run along x every 4 blocks. */
    public static final BlockPos GIETERIJ = w("werf_kamer_gieterij", 2873, 126, -586);
    public static final BlockPos GIETERIJ_A = GIETERIJ.offset(-8, 0, -8), GIETERIJ_B = GIETERIJ.offset(8, 0, 8);
    /** De Bewapeningsloods: 3 levers, 3 cannons, the gate they must aim at (request werf_loods_poort; fallback: 9 south). */
    public static final BlockPos LOODS = w("werf_kamer_bewapening", 2927, 126, -554);
    public static final BlockPos LOODS_POORT = w("werf_loods_poort", LOODS.getX(), LOODS.getY(), LOODS.getZ() + 9);
    public static final BlockPos TALEGGIO_CENTRUM = w("arena_taleggio", 2921, 141, -584);
    public static final BlockPos BROKKEL_CENTRUM = w("arena_brokkel", 2875, 126, -554);
    public static final int[][] BROKKEL_KANONNEN = {rel(BROKKEL_CENTRUM, "arena_brokkel_kanon_1", 2883, 126, -546),
            rel(BROKKEL_CENTRUM, "arena_brokkel_kanon_2", 2867, 126, -546)};
    public static final BlockPos ROQUEFORT_CENTRUM = w("arena_roquefort", 2899, 111, -570);
    /** The wreck next to the flagship's deck (fase 3: jump over). Request roquefort_wrak; fallback: the cabin end of the deck. */
    public static final BlockPos ROQUEFORT_WRAK = w("roquefort_wrak", 2887, 111, -570);
    /** Dungeon fights: wake at the dungeon entrance; boards on the deck edges. */
    public static final BlockPos TALEGGIO_DEUR = WERF_INGANG, BROKKEL_DEUR = WERF_INGANG, ROQUEFORT_DEUR = WERF_INGANG;
    public static final BlockPos TALEGGIO_BORD = TALEGGIO_CENTRUM.offset(0, 0, 13), BROKKEL_BORD = BROKKEL_CENTRUM.offset(0, 0, -13),
            ROQUEFORT_BORD = ROQUEFORT_CENTRUM.offset(0, 0, -17);
    public static final int D1_STRAAL = 15, ROQUEFORT_STRAAL = 19, D1_RESPAWN = 5 * 60;

    // ---- Graaf Piepvel: De Pelszaal, UNDER the Schorsstad floor (lv 42; revamp: hall air 61..74, the plein stands at 81) -------
    public static final BlockPos PIEP_CENTRUM = w("arena_piepvel", 2300, 61, 0);
    public static final int PIEP_STRAAL = 16;
    /** (P-VERHUIS) the Pelszaal's height window: 6 below, 14 above its floor (players on the Schorsstad plein are NOT in it). */
    public static final int PIEP_ONDER = 6, PIEP_BOVEN = 14;
    public static final BlockPos PIEP_DEUR = w("arena_piepvel_deur", 2301, 61, -13);
    public static final BlockPos PIEP_BORD = PIEP_DEUR.offset(3, 0, 0);
    public static final int PIEP_RESPAWN = 10 * 60;
    /** The 4 hanging cages (offsets from the centre: different heights) + the big cage by the throne. */
    public static final int[][] PIEP_KOOIEN = {rel(PIEP_CENTRUM, "piepvel_kooi_1", 2292, 186, 7), rel(PIEP_CENTRUM, "piepvel_kooi_2", 2307, 189, 7),
            rel(PIEP_CENTRUM, "piepvel_kooi_3", 2307, 192, -8), rel(PIEP_CENTRUM, "piepvel_kooi_4", 2292, 195, -8)};
    public static final int[] PIEP_GROTE_KOOI = rel(PIEP_CENTRUM, "piepvel_grote_kooi", 2294, 184, -12);
    public static final int[] PIEP_TROON = rel(PIEP_CENTRUM, "piepvel_troon", 2299, 184, -12);
    /** The 6 blown-out lantern posts on a ring. */
    public static final int[][] PIEP_PALEN = {rel(PIEP_CENTRUM, "piepvel_lantaarnpaal_1", 2285, 184, 0), rel(PIEP_CENTRUM, "piepvel_lantaarnpaal_2", 2292, 184, 12),
            rel(PIEP_CENTRUM, "piepvel_lantaarnpaal_3", 2306, 184, 12), rel(PIEP_CENTRUM, "piepvel_lantaarnpaal_4", 2313, 184, 0),
            rel(PIEP_CENTRUM, "piepvel_lantaarnpaal_5", 2307, 184, -13), rel(PIEP_CENTRUM, "piepvel_lantaarnpaal_6", 2292, 184, -13)};
    /**
     * Where the Keizer's arm comes in (flash F1): a knot hole in the Pelszaal wall, in the direction of the map's
     * {@code flash_f1_kwastgat} (that one is in the trunk wall higher up, where the grey fingerprint stays), at the hall's height.
     */
    public static final BlockPos PIEP_KWASTGAT = kwastgat();

    private static BlockPos kwastgat() {
        BlockPos k = w("flash_f1_kwastgat", 2319, 128, 8);
        double dx = k.getX() - PIEP_CENTRUM.getX(), dz = k.getZ() - PIEP_CENTRUM.getZ(), l = Math.max(1, Math.hypot(dx, dz));
        return PIEP_CENTRUM.offset((int) Math.round(dx / l * (PIEP_STRAAL - 1)), 4, (int) Math.round(dz / l * (PIEP_STRAAL - 1)));
    }
    public static final Vec3 PIEP_KWASTGAT_RICHTING = Vec3.atCenterOf(PIEP_KWASTGAT).subtract(Vec3.atCenterOf(PIEP_CENTRUM)).normalize();

    // ---- Kerker 2: Het Hart van de Boom (lv 49-50): rooms in a ring at r 112 round the roots hall, floor stand 29 -------------
    /** The Hart ring (z3_hart: r 76..150, y 14..52); the hall inside it (r < 76) and the Wortelgang (east) are not the dungeon. */
    public static final int HART_Y0 = 14, HART_Y1 = 52, HART_STRAAL_BOX = 150, HART_STRAAL_BINNEN = 76;
    public static final BlockPos HART_INGANG = w("hart_poort", 2299, 13, -50);
    public static final BlockPos HART_ALTAAR = w("hart_vahoeg_altaar", 2305, 13, -46);
    /** De Aderloop (r 12), De Schimmelkwekerij (r 16, 3 kettles), De Ringenzaal (r 14, 3 rings). */
    public static final BlockPos ADERLOOP = w("hart_kamer_aderloop", 2367, 51, -68);
    public static final BlockPos ADERLOOP_A = ADERLOOP.offset(-11, 0, -11), ADERLOOP_B = ADERLOOP.offset(11, 0, 11);
    public static final BlockPos KWEKERIJ = w("hart_kamer_schimmelkwekerij", 2394, 51, 0);
    public static final int[][] KWEKERIJ_KETELS = {{-7, 0, -6}, {7, 0, -6}, {0, 0, 8}};
    /** The gates (requests hart_kwekerij_poort / hart_ringenzaal_poort; fallback: the room edge toward the trunk). */
    public static final BlockPos KWEKERIJ_POORT = w("hart_kwekerij_poort", 2379, 51, 0);
    public static final BlockPos RINGEN = w("hart_kamer_ringenzaal", 2367, 51, 67);
    public static final int[] RING_STRALEN = {3, 6, 9};
    public static final BlockPos RINGEN_POORT = w("hart_ringenzaal_poort", 2356, 51, 57);
    public static final BlockPos WURGER_CENTRUM = w("arena_wortelwurger", 2299, 51, 95);
    public static final int[][] WURGER_WORTELS = {{10, 0, 0}, {0, 0, 10}, {-10, 0, 0}, {0, 0, -10}};
    public static final BlockPos WACHTER_CENTRUM = w("arena_nestwachter", 2232, 51, 67);
    public static final BlockPos BLEU_CENTRUM = w("arena_baron_bleu", 2204, 51, 0);
    public static final BlockPos WURGER_DEUR = HART_INGANG, WACHTER_DEUR = HART_INGANG, BLEU_DEUR = HART_INGANG;
    public static final BlockPos WURGER_BORD = WURGER_CENTRUM.offset(0, 0, -18), WACHTER_BORD = WACHTER_CENTRUM.offset(0, 0, -18),
            BLEU_BORD = BLEU_CENTRUM.offset(18, 0, 0);
    /** The egg the Kolos reaches for (stack circle in fase 3) and the 4 lantern posts that burn the mould away. */
    public static final int[] BLEU_EI = {-12, 0, 0};
    public static final int[][] BLEU_PALEN = {{8, 0, 10}, {8, 0, -10}, {-6, 0, 12}, {-6, 0, -12}};
    public static final int D2_STRAAL = 18, BLEU_STRAAL = 20, D2_RESPAWN = 5 * 60;

    // ---- Vahoegnir: het oude nest op de Kruintop (lv 50, story + Droomsteen replays) -------------------------------------
    public static final BlockPos NEST_CENTRUM = w("arena_vahoegnir", 2299, 280, -40);
    public static final int NEST_STRAAL = 22;
    public static final BlockPos NEST_DEUR = w("nest_rand", 2299, 283, -18);
    /** The egg hollow (offset), and the 6 big nest branches to hide behind (crystal howl). */
    public static final int[] NEST_EI = {0, 0, 0};
    public static final int[][] NEST_TAKKEN = {{11, 0, 5}, {-11, 0, 5}, {0, 0, 13}, {11, 0, -7}, {-11, 0, -7}, {0, 0, -14}};
    public static final int NEST_TAK_HOOG = 5;
    /** Where the pink beam points: Hoogvads, far east above the clouds. */
    public static final Vec3 HOOGVADS = new Vec3(nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_X + 2300, 330, nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken.STAM_Z - 120);   // far east of the trunk

    // ---- (M-BAZEN) arena layout: no two arenas share space -------------------------------------------------------------------
    /** Height window of a dungeon arena (werf decks, Hart rooms): 6 below the floor (a wreck to jump to), 14 above. */
    public static final int KERKER_ONDER = 6, KERKER_BOVEN = 14;
    /** Below this height difference two overlapping arenas are not split by height but shrunk (never below MIN_STRAAL). */
    public static final int MIN_DY = 6, MIN_STRAAL = 8, KIER = 2;

    /**
     * Makes the arenas disjoint, whatever the map does (POIs move with the zone-3 revamp): two arenas whose discs overlap are split
     * at the height halfway between their floors (stacked decks), or, when their floors are less than {@link #MIN_DY} apart, both
     * radii shrink until there is a {@link #KIER}-block gap (logged: ask wereld for more room, POI_VERZOEKEN_Z3.md).
     */
    public static List<nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena> indeel(List<nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena> in) {
        List<nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena> l = new java.util.ArrayList<>(in);
        for (int ronde = 0; ronde < 6; ronde++) {
            boolean anders = false;
            for (int i = 0; i < l.size(); i++) {
                for (int j = i + 1; j < l.size(); j++) {
                    var a = l.get(i);
                    var b = l.get(j);
                    if (!a.overlapt(b)) {
                        continue;
                    }
                    boolean aLaag = a.centrum().getY() <= b.centrum().getY();
                    var lo = aLaag ? a : b;
                    var hi = aLaag ? b : a;
                    int dy = hi.centrum().getY() - lo.centrum().getY();
                    if (dy >= MIN_DY) {
                        int mid = lo.centrum().getY() + dy / 2;
                        lo = lo.met(lo.straal(), lo.onder(), Math.min(lo.boven(), mid - lo.centrum().getY()));
                        hi = hi.met(hi.straal(), Math.min(hi.onder(), hi.centrum().getY() - mid), hi.boven());
                    } else {
                        double d = Math.sqrt(Math.pow(a.centrum().getX() - b.centrum().getX(), 2) + Math.pow(a.centrum().getZ() - b.centrum().getZ(), 2));
                        double f = Math.max(0, d - KIER) / (lo.straal() + hi.straal());
                        int rl = Math.max(MIN_STRAAL, (int) Math.floor(lo.straal() * f)), rh = Math.max(MIN_STRAAL, (int) Math.floor(hi.straal() * f));
                        if (rl == lo.straal() && rh == hi.straal()) {
                            org.slf4j.LoggerFactory.getLogger("guhs/z3bazen").warn("arenas {} and {} overlap and cannot shrink further: move them apart (POI_VERZOEKEN_Z3)",
                                    lo.id(), hi.id());
                            continue;
                        }
                        org.slf4j.LoggerFactory.getLogger("guhs/z3bazen").warn("arenas {} and {} overlap on one floor: radii {} -> {} and {} -> {}", lo.id(),
                                hi.id(), lo.straal(), rl, hi.straal(), rh);
                        lo = lo.met(rl, lo.onder(), lo.boven());
                        hi = hi.met(rh, hi.onder(), hi.boven());
                    }
                    anders = true;
                    l.set(i, aLaag ? lo : hi);
                    l.set(j, aLaag ? hi : lo);
                }
            }
            if (!anders) {
                break;
            }
        }
        return l;
    }

    public static BlockPos op(BlockPos c, int[] o) {
        return c.offset(o[0], o[1], o[2]);
    }

    public static List<BlockPos> alle(BlockPos c, int[][] o) {
        return java.util.Arrays.stream(o).map(x -> op(c, x)).toList();
    }
}
