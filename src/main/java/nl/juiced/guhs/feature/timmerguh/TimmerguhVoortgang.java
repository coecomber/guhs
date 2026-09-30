package nl.juiced.guhs.feature.timmerguh;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Where a player is in "Samen een huisje bouwen": an int step in {@code GuhQuests.saved(p)["guhs_timmerguh_stap"]}
 * (survives death).
 * <ol start="0">
 *   <li>{@link #NIEUW}: not started;</li>
 *   <li>{@link #MATERIAAL}: the Timmerguh asked for planks and pink wool;</li>
 *   <li>{@link #DAK}: delivered, laying the oortjesdak with the dakpluisjes;</li>
 *   <li>{@link #BEWONER}: the roof is on; you got a small huisje: let one of your guhs live in a huisje;</li>
 *   <li>{@link #KLAAR}: done: the bouwboekje (the recipes), a small huisje and the timmermanshelmpje;</li>
 *   <li>{@link #KNUS}: the optional step too (a toy and a guhlampje in the home area): the gereedschapsriem.</li>
 * </ol>
 */
public final class TimmerguhVoortgang {
    public static final String STAP = "guhs_timmerguh_stap";
    public static final int NIEUW = 0, MATERIAAL = 1, DAK = 2, BEWONER = 3, KLAAR = 4, KNUS = 5;

    public static int stap(Player p) {
        return GuhQuests.saved(p).getInt(STAP);
    }

    public static void zet(Player p, int stap) {
        CompoundTag t = GuhQuests.saved(p);
        t.putInt(STAP, stap);
    }

    /** Has this player finished the questline (the huisje recipes are theirs)? */
    public static boolean klaar(Player p) {
        return stap(p) >= KLAAR;
    }

    private TimmerguhVoortgang() {
    }
}
