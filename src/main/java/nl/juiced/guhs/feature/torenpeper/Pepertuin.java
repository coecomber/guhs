package nl.juiced.guhs.feature.torenpeper;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * De Pepertuin met kas (structure guhs:pepertuin, one template of the same name): where its things are, in template
 * coordinates, and the Peperteler-guh's questline outside his own talk ({@link PepertelerRol}). The numbers come from
 * tools/features/toren_peper_bouw.py (PLEKKEN_TUIN); the generator's self-check compares them with this file, so keep the
 * {@code new BlockPos(x, y, z)} lines in this shape.
 */
public final class Pepertuin {
    public static final String STRUCTUUR = "pepertuin";
    /** The Bezetting id (and the tag of the template's own NPC). */
    public static final String PEPERTELER = "torenpeper_peperteler";
    /** The template's ground layer. */
    public static final int G = 3;

    public static final BlockPos NPC = new BlockPos(11, 4, 23);
    public static final float NPC_YAW = 0.0f;
    /** The three kweekbakken: green, red, pink. */
    public static final List<BlockPos> BAKKEN = List.of(new BlockPos(7, 4, 17), new BlockPos(7, 4, 13), new BlockPos(7, 4, 9));
    /** The Guhbrouwketel of the brewing corner. */
    public static final BlockPos KETEL = new BlockPos(9, 4, 6);

    private static final Verhaallijn LIJN = TorenpeperFeature.PEPERTUIN;

    private Pepertuin() {
    }

    /** Did this player pick this pepper from a kweekbak (step 1 of the questline)? */
    public static boolean geplukt(ServerPlayer p, PeperSoort soort) {
        return LIJN.vlag(p, "geplukt_" + soort.id());
    }

    public static int aantalGeplukt(ServerPlayer p) {
        int n = 0;
        for (PeperSoort s : PeperSoort.values()) {
            n += geplukt(p, s) ? 1 : 0;
        }
        return n;
    }

    /** The player picked a ripe plant of their own from a kweekbak: at step 1 it counts, the third kind finishes the step. */
    public static void opGeplukt(ServerPlayer p, PeperSoort soort) {
        if (LIJN.stap(p) != 1) {
            return;
        }
        LIJN.vlag(p, "geplukt_" + soort.id(), true);
        if (aantalGeplukt(p) >= PeperSoort.values().length) {
            if (LIJN.verder(p, 1)) {
                GuhQuests.hint(p, "quest.guhs.torenpeper.hint.geplukt");
            }
        } else {
            GuhQuests.hint(p, "quest.guhs.torenpeper.hint.nog_plukken");
        }
    }

    /** Does the player carry one of the two pepper drinks? */
    public static boolean heeftDrankje(ServerPlayer p) {
        return GuhQuests.count(p, TorenpeperFeature.PEPERVUURDRANKJE.get()) > 0 || GuhQuests.count(p, TorenpeperFeature.PEPERZOETDRANKJE.get()) > 0;
    }

    /** The player drank a pepper drink (so "your first pepper drink" was brewed, even when the bottle is empty by now). */
    public static void geproefd(ServerPlayer p) {
        if (LIJN.stap(p) == 2) {
            LIJN.vlag(p, "geproefd", true);
            gebrouwen(p);
        }
    }

    /** Step 2 is done as soon as the player has a pepper drink (checked now and then by {@link TorenpeperEvents}). */
    public static void gebrouwen(ServerPlayer p) {
        if (LIJN.stap(p) == 2 && (heeftDrankje(p) || LIJN.vlag(p, "geproefd")) && LIJN.verder(p, 2)) {
            GuhQuests.hint(p, "quest.guhs.torenpeper.hint.gebrouwen");
        }
    }
}
