package nl.juiced.guhs.feature.bio.bouwwolk2;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.GuhTime;

/**
 * A treat a player may take once per game day (the pot at the end of the rainbow, the hoard of the giant): the day it was
 * last taken is kept with the player ({@link GuhQuests#saved}), so every player has a turn of their own at every pot and
 * hoard, and it does not matter which bridge or castle they visit.
 */
public final class Dagtraktatie {
    public static final String POT = "guhs_bio_bouw_wolk2_pot_dag", SCHAT = "guhs_bio_bouw_wolk2_schat_dag";

    /** The rule: not on the day it was last taken (laatst -1: never). */
    public static boolean mag(long laatst, long vandaag) {
        return laatst != vandaag;
    }

    public static boolean mag(ServerPlayer speler, String sleutel) {
        return mag(GuhQuests.saved(speler).getLongOr(sleutel, -1L), GuhTime.day(speler.level()));
    }

    /** Takes the treat of today: true when the player may (and it is marked taken), false when they already had it today. */
    public static boolean neem(ServerPlayer speler, String sleutel) {
        if (!mag(speler, sleutel)) {
            return false;
        }
        GuhQuests.saved(speler).putLong(sleutel, GuhTime.day(speler.level()));
        return true;
    }

    /** (Tests, dev) as if it was last taken on this day (-1: never). */
    public static void zet(ServerPlayer speler, String sleutel, long dag) {
        CompoundTag saved = GuhQuests.saved(speler);
        if (dag < 0) {
            saved.remove(sleutel);
        } else {
            saved.putLong(sleutel, dag);
        }
    }

    private Dagtraktatie() {
    }
}
