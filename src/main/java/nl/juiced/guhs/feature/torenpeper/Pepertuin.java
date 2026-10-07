package nl.juiced.guhs.feature.torenpeper;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.spiesburcht.GuhbrouwketelBlockEntity;
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

    /**
     * Step 2 is done as soon as the player has a pepper drink (checked now and then by {@link TorenpeperEvents}), drank
     * one, or stirred a pepper of their own into a ketel ({@link #eigenPeper}).
     */
    public static void gebrouwen(ServerPlayer p) {
        if (LIJN.stap(p) == 2 && (heeftDrankje(p) || LIJN.vlag(p, "geproefd") || LIJN.vlag(p, "eigen_peper")) && LIJN.verder(p, 2)) {
            GuhQuests.hint(p, "quest.guhs.torenpeper.hint.gebrouwen");
        }
    }

    /** Is this a pepper that brews a pepper drink (the red and the pink one)? */
    public static boolean isBrouwpeper(ItemStack stack) {
        Brouwsel b = Brouwsel.forIngredient(stack);
        return b == Brouwsel.PEPERVUUR || b == Brouwsel.PEPERZOET;
    }

    /** Does the player carry a pepper that brews? */
    public static boolean heeftBrouwpeper(ServerPlayer p) {
        return GuhQuests.count(p, PeperSoort.ROOD.peper()) > 0 || GuhQuests.count(p, PeperSoort.ROZE.peper()) > 0;
    }

    /**
     * The player right-clicks this block with this stack (called before the block itself looks at the click): when that
     * stirs a brewing pepper into a Guhbrouwketel at step 2, the first pepper drink is THEIRS and the step is done, whoever
     * fills the bottles afterwards. The ketel of the kas is one pan for everybody: without this, somebody else who taps a
     * player's brew would leave that player with nothing to show. True when the step was counted.
     */
    public static boolean eigenPeper(ServerPlayer p, BlockPos pos, ItemStack stack) {
        if (LIJN.stap(p) != 2 || !isBrouwpeper(stack) || !(p.level().getBlockEntity(pos) instanceof GuhbrouwketelBlockEntity ketel)) {
            return false;
        }
        // (exactly when GuhbrouwketelBlockEntity#use takes an ingredient: bouillon in the pan, nothing bubbling, a fire under it)
        if (ketel.portions() == 0 || ketel.contents() != Brouwsel.BOUILLON || ketel.isBrewing() || ketel.fuel() <= 0) {
            return false;
        }
        LIJN.vlag(p, "eigen_peper", true);
        if (LIJN.verder(p, 2)) {
            GuhQuests.hint(p, "quest.guhs.torenpeper.hint.borrelt");
        }
        return true;
    }
}
