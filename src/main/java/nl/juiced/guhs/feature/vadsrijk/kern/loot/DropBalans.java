package nl.juiced.guhs.feature.vadsrijk.kern.loot;

/**
 * 1101-balans: the gear-drop knobs of the creature tables of every zone ({@code wezens:*}, {@code z2wezens:*} .. {@code z6wezens:*}).
 * <ul>
 *   <li>{@link #GEWOON}: a normal creature's gear chance x0.2 (zone 1 averaged 0.224 per kill, now ~0.045);</li>
 *   <li>{@link #ELITE_KANS}: an elite's 2 rolls at min(1, chance x3.5) x0.45 (zone 1 ~0.78 per roll before, now ~0.35);</li>
 *   <li>{@link #ELITE_GELUK}: an elite's luck x0.64 (zone 1 2.5 -> 1.6: Guhddelijk 6.8 % -> 1.4 % of its drops; zone 2-6 likewise).</li>
 * </ul>
 * At 4 kills/min in zone 1: ~73 -> ~20 items/h, Legendarisch+ ~4.5 -> ~0.6/h (BALANS_1101_ANALYSE.md 3.2). Bosses, the dungeon
 * chest, quest rewards and coins are not touched by these.
 */
public final class DropBalans {
    public static final double GEWOON = 0.2;
    public static final double ELITE_KANS = 0.45;
    public static final double ELITE_GELUK = 0.64;
    /**
     * 1.10.4 (Smeden = sieraden): random jewellery (ring, ketting, oorbel, armband) drops 5x less often than before, so mostly the gear
     * Smeden can NOT make drops (weapons, armour, gloves, capes). Applied to every random pick: loot-table rolls ({@link LootTabel#rol},
     * a jewellery rule weighs 1/{@code SIERAAD_DEEL} of its weight) and the zone pools ({@link ZonePools#stuk}). Named boss items
     * ({@link ZonePools#benoemd}: first-kill jewellery like the boss rings and amulets) are not touched.
     */
    public static final int SIERAAD_DEEL = 5;

    private DropBalans() {
    }

    /** 1.10.4: a random jewellery base type (not a named boss item)? */
    public static boolean sieraadDrop(String basisType) {
        BasisType t = BasisTypes.van(basisType);
        return t != null && !ZonePools.benoemd(basisType)
                && (t.slot() == Slot.RING || t.slot() == Slot.KETTING || t.slot() == Slot.OORBEL || t.slot() == Slot.ARMBAND);
    }

    /** 1.10.4: the weight a loot rule really rolls with, in units of 1/{@link #SIERAAD_DEEL} (jewellery 1x, everything else 5x). */
    public static int gewicht(LootTabel.Regel r) {
        return sieraadDrop(r.basisType()) ? r.gewicht() : r.gewicht() * SIERAAD_DEEL;
    }

    /** A normal creature table's gear chance. */
    public static double gewoon(double kans) {
        return kans * GEWOON;
    }

    /** An elite table's gear chance per roll (from the normal creature's original chance). */
    public static double elite(double kans) {
        return Math.min(1, kans * 3.5) * ELITE_KANS;
    }

    /** An elite table's luck. */
    public static double eliteGeluk(double geluk) {
        return Math.round(geluk * ELITE_GELUK * 100) / 100.0;
    }
}
