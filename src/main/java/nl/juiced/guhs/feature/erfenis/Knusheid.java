package nl.juiced.guhs.feature.erfenis;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.feature.titels.Titels;

/**
 * De Erfenis: Knusheid (cosiness). It counts towards Hoeve goals (Knusheid 250 / 1.000 / 2.000) and titles; no ranking.
 * Total = the buildings (2 per level, the Hoeve 10 per level; deco.md) + the paths (1 per 10 tiles) + the deco value, which
 * the deco code keeps up to date with {@link #zet}.
 * <p>
 * Deco slice: the milestones ({@link #MIJLPALEN}: a cheerful message the first time the village gets there, see
 * {@link Erfenis#meldKnus}) and three titles on the highest Knusheid ever reached ({@link Base#knusRecord}), so putting deco
 * away never takes a title back. The {@code knusheid:<totaal>} moment is fired by the kern on every change.
 */
public final class Knusheid {
    /** The milestones (FTB uses 250 / 1.000 / 2.500, the Hoeve goals 250 / 1.000 / 2.000). */
    public static final int[] MIJLPALEN = {50, 100, 250, 500, 1000, 1500, 2000, 2500, 5000};
    /** The titles: {Knusheid, id}. Lang {@code gui.guhs.titels.naam.<id>} / {@code gui.guhs.titels.hint.<id>} (erfenis_deco.py). */
    public static final String TITEL_250 = "erfenis_knus_250", TITEL_1000 = "erfenis_knus_1000", TITEL_2500 = "erfenis_knus_2500";

    private Knusheid() {
    }

    /** Sets the deco part of a base's Knusheid (the deco system calls this after every change). */
    public static void zet(Base base, int decoWaarde) {
        base.knusDeco = Math.max(0, decoWaarde);
    }

    /** The whole Knusheid of a base. */
    public static int totaal(Base base) {
        return base.knusheid();
    }

    /** The buildings' part (2 per level, the Hoeve 10). */
    public static int gebouwen(Base b) {
        int k = 0;
        for (Gebouw g : b.gebouwen) {
            k += g.type.knus(g.level);
        }
        return k;
    }

    /** The paths' part (1 per 10 tiles). */
    public static int paden(Base b) {
        return Paden.tegels(b) / 10;
    }

    /** The next milestone above {@code k}, or -1. */
    public static int volgende(int k) {
        for (int m : MIJLPALEN) {
            if (m > k) {
                return m;
            }
        }
        return -1;
    }

    /** The highest Knusheid this player's village ever reached (0: no village). */
    public static int record(MinecraftServer server, java.util.UUID speler) {
        Base b = ErfenisData.get(server).van(speler);
        return b == null ? 0 : Math.max(b.knusRecord, b.knusheid());
    }

    /** The three Knusheid titles (from ErfenisFeature.register; common code). */
    static void titels() {
        Object[][] t = {{TITEL_250, 250, ChatFormatting.LIGHT_PURPLE, "minecraft:pink_tulip"},
                {TITEL_1000, 1000, ChatFormatting.GOLD, "guhs:guh_lantaarnpaal"}, {TITEL_2500, 2500, ChatFormatting.AQUA, "guhs:guh_kristal_lamp"}};
        for (Object[] r : t) {
            String id = (String) r[0];
            int n = (Integer) r[1];
            Titels.registreer(new Titels.Titel(id, "gui.guhs.titels.naam." + id, (ChatFormatting) r[2], (String) r[3],
                    p -> p.level().getServer() != null && record(p.level().getServer(), p.getUUID()) >= n));
        }
    }
}
