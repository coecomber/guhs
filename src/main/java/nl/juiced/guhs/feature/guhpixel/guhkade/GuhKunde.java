package nl.juiced.guhs.feature.guhpixel.guhkade;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.util.RandomSource;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Spel;

/**
 * How good a guh is at the Guhkade. Every guh starts clumsy and gets better the more it plays, creeping up to a ceiling
 * that depends on its kind (a Wolkguh can fly, so Flappy Guh suits it; a Teckelguh is one long paddle) and a little on
 * its character. The ceiling is tough but a player can beat it; a guh never gets above it, however much it practises.
 * <p>
 * Persistent data of the guh (travels with it): {@link #KEREN}{@code <spel>} = games played (practice games after it was
 * beaten count double), {@link #OEFEN} = practice games it still wants to play, {@link #RUST} = no game before this time.
 */
public final class GuhKunde {
    public static final String KEREN = "guhs_px_guhkade_keren_", OEFEN = "guhs_px_guhkade_oefen", RUST = "guhs_px_guhkade_rust";
    /** Practice games a guh wants after a player beat it. */
    public static final int OEFEN_NA_VERLIES = 3, OEFEN_MAX = 6;
    /** After about this many games a guh is two thirds of the way to its ceiling. */
    private static final double LEERTEMPO = 18.0;

    private static final Map<GuhVariant, int[]> PLAFOND = new EnumMap<>(GuhVariant.class);
    /** Everybody else: {Flappy Guh, Mika-Pong}. */
    private static final int[] GEWOON = {16, 40};

    static {
        zet(GuhVariant.NORMAL, 16, 40);
        zet(GuhVariant.MINT, 18, 44);
        zet(GuhVariant.CHOCO, 18, 42);
        zet(GuhVariant.SNOW, 20, 48);
        zet(GuhVariant.BRONTOSAURUS, 12, 62);     // (that long neck keeps bumping the pillars, but what a reach)
        zet(GuhVariant.GOLDEN, 28, 60);
        zet(GuhVariant.RAINBOW, 26, 56);
        zet(GuhVariant.STARRY, 30, 58);
        zet(GuhVariant.GHOST, 32, 34);            // (floats nicely; the rolling guh rolls right through it)
        zet(GuhVariant.TECKEL, 12, 70);           // (one long paddle on short legs)
        zet(GuhVariant.BROCOCOLIEF, 22, 50);
        zet(GuhVariant.ENDER, 34, 52);
        zet(GuhVariant.KONING, 24, 54);
        zet(GuhVariant.WOLK, 38, 36);             // (it can really fly)
        zet(GuhVariant.ZEEMEERGUH, 20, 46);
        zet(GuhVariant.MAGER, 30, 32);            // (fits through every gap; not much of a paddle)
        zet(GuhVariant.VAHOEGE_ENDER, 40, 64);
        zet(GuhVariant.KAASMOERASGUH, 18, 44);
        zet(GuhVariant.ASGUH, 22, 50);
        zet(GuhVariant.PLUISGUH, 20, 48);
        zet(GuhVariant.PINGUH, 10, 66);           // (pinguhs cannot fly, but they slide like the best)
    }

    private static void zet(GuhVariant v, int flappy, int pong) {
        PLAFOND.put(v, new int[] {flappy, pong});
    }

    /** The best score a guh of this kind and character can ever get. */
    public static int plafond(Spel spel, GuhVariant variant, GuhPersonality aard) {
        int basis = PLAFOND.getOrDefault(variant, GEWOON)[spel.ordinal()];
        double factor = switch (aard) {
            case PLAYFUL -> 1.15;
            case CURIOUS -> 1.08;
            case BRAVE -> 1.05;
            case LAZY -> 0.85;
            case VADSIG -> 0.9;
            default -> 1.0;
        };
        return Math.max(3, (int) Math.round(basis * factor));
    }

    /** How far along it is after this many games: 0.2 (the first game) creeping up to 1. */
    public static double kunde(int keren) {
        return 0.2 + 0.8 * (1.0 - Math.exp(-Math.max(0, keren) / LEERTEMPO));
    }

    /** The score a guh aims for in its next game: its skill now, and how well it is doing today (70..100 %). */
    public static int doel(Spel spel, GuhVariant variant, GuhPersonality aard, int keren, RandomSource random) {
        double vorm = 0.7 + 0.3 * random.nextDouble();
        return Math.max(1, (int) Math.round(plafond(spel, variant, aard) * kunde(keren) * vorm));
    }

    public static int keren(GuhEntity guh, Spel spel) {
        return guh.getPersistentData().getIntOr(KEREN + spel.id, 0);
    }

    public static int oefen(GuhEntity guh) {
        return guh.getPersistentData().getIntOr(OEFEN, 0);
    }

    /** One more game played (a practice game counts double and uses up one of the practice games it wanted). */
    public static void gespeeld(GuhEntity guh, Spel spel) {
        var data = guh.getPersistentData();
        int oefen = data.getIntOr(OEFEN, 0);
        data.putInt(KEREN + spel.id, Math.min(10_000, data.getIntOr(KEREN + spel.id, 0) + (oefen > 0 ? 2 : 1)));
        if (oefen > 1) {
            data.putInt(OEFEN, oefen - 1);
        } else {
            data.remove(OEFEN);
        }
    }

    private GuhKunde() {
    }
}
