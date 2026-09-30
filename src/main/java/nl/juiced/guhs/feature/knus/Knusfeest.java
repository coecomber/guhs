package nl.juiced.guhs.feature.knus;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The state of the Grote Knusfeest (2.8) per player, and the hooks the features use.
 * <p>
 * <b>Flow</b>: Burgemeester Vadsema (feature/knuffeldal) asks the six {@link Feesttaak}s ({@link Stap#GEVRAAGD}) →
 * while {@link #open} a feature offers its special feest-activity; on success it gives its feest-item (tag
 * {@code guhs:knus/<id>}) <b>and calls {@link #gemaakt}</b> → for the cake, the tea set and the garlands a Kruimel-Mika
 * steals it on the way ({@link Stap#GESTOLEN}; the crumb trail, lure it with a treat: {@link Stap#TERUGGEVONDEN}) →
 * the player brings it to the Burgemeester ({@link Stap#GEBRACHT}) → after all six: the feestbuffet finale.
 * <p>
 * After the finale the Knusfeest comes back once per season (EvenementType KNUSFEEST): the Burgemeester asks 2-3
 * random tasks again, through the same {@link #open} / {@link #gemaakt} hooks, so features need nothing extra.
 * <p>
 * Saved in {@link GuhQuests#saved} ({@value #KEY}): per task its step for the current round, the round, and whether
 * the Grote Knusfeest is done.
 */
public final class Knusfeest {
    public static final String KEY = "guhs_knusfeest";

    /** The steps of one task (in this order). */
    public enum Stap { GEVRAAGD, GEMAAKT, GESTOLEN, TERUGGEVONDEN, GEBRACHT }

    @FunctionalInterface
    public interface Listener {
        void stap(ServerPlayer player, Feesttaak taak, Stap stap);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private Knusfeest() {
    }

    /** Hear about every step of every task (register in your Feature.register). */
    public static void luister(Listener listener) {
        LISTENERS.add(listener);
    }

    // --- the hooks for the features ------------------------------------------------------------------------------------

    /** Is this task asked by the Burgemeester and not delivered yet (in the current round)? Then offer the feest-activity. */
    public static boolean open(ServerPlayer player, Feesttaak taak) {
        Stap stap = stap(player, taak);
        return stap != null && stap != Stap.GEBRACHT;
    }

    /**
     * Call this right after you gave the feest-item for an {@link #open} task (idempotent: only the first call of a
     * round counts). Grants {@code guhs:quest/knusfeest_<id>_gemaakt}.
     */
    public static void gemaakt(ServerPlayer player, Feesttaak taak) {
        Stap stap = stap(player, taak);
        if (stap == Stap.GEVRAAGD) {
            zet(player, taak, Stap.GEMAAKT);
        }
        if (stap != null) {
            GuhAdvancements.grant(player, "knusfeest_" + taak.id() + "_gemaakt");
        }
    }

    /** Has the player delivered this task (in the current round)? */
    public static boolean gebracht(ServerPlayer player, Feesttaak taak) {
        return stap(player, taak) == Stap.GEBRACHT;
    }

    // --- state (used by the Burgemeester, the Kruimel-Mika's and the tests) ----------------------------------------------

    static CompoundTag data(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (!saved.contains(KEY)) {
            saved.put(KEY, new CompoundTag());
        }
        return saved.getCompoundOrEmpty(KEY);
    }

    /** The step of this task in the current round (null: not asked in this round). */
    @Nullable
    public static Stap stap(ServerPlayer player, Feesttaak taak) {
        int i = data(player).getCompoundOrEmpty("Stappen").getIntOr(taak.id(), 0);
        return i >= 1 && i <= Stap.values().length ? Stap.values()[i - 1] : null;
    }

    /** Sets a task's step (and tells the listeners when it changed). */
    public static void zet(ServerPlayer player, Feesttaak taak, @Nullable Stap stap) {
        CompoundTag d = data(player);
        CompoundTag stappen = d.getCompoundOrEmpty("Stappen");
        int was = stappen.getIntOr(taak.id(), 0);
        int now = stap == null ? 0 : stap.ordinal() + 1;
        if (was == now) {
            return;
        }
        stappen.putInt(taak.id(), now);
        d.put("Stappen", stappen);
        if (stap == Stap.GESTOLEN) {
            CompoundTag when = d.getCompoundOrEmpty("GestolenOp");
            when.putLong(taak.id(), player.level().getGameTime());
            d.put("GestolenOp", when);
        }
        if (stap != null) {
            for (Listener l : LISTENERS) {
                l.stap(player, taak, stap);
            }
        }
    }

    /** Game time when this task's item was stolen (0: never). */
    public static long gestolenOp(ServerPlayer player, Feesttaak taak) {
        return data(player).getCompoundOrEmpty("GestolenOp").getLongOr(taak.id(), 0L);
    }

    /** The tasks of the current round (asked, in any step). */
    public static Set<Feesttaak> taken(ServerPlayer player) {
        Set<Feesttaak> out = EnumSet.noneOf(Feesttaak.class);
        for (Feesttaak t : Feesttaak.values()) {
            if (stap(player, t) != null) {
                out.add(t);
            }
        }
        return out;
    }

    /** Is every task of the current round delivered (and is there a round)? */
    public static boolean alleGebracht(ServerPlayer player) {
        Set<Feesttaak> taken = taken(player);
        return !taken.isEmpty() && taken.stream().allMatch(t -> gebracht(player, t));
    }

    /** Starts a round: these tasks are asked (all others forgotten). ronde 0 = the Grote Knusfeest, n > 0 = the seasonal feest of season n - 1. */
    public static void nieuweRonde(ServerPlayer player, long ronde, Set<Feesttaak> gevraagd) {
        CompoundTag d = data(player);
        d.put("Stappen", new CompoundTag());
        d.put("GestolenOp", new CompoundTag());
        d.putLong("Ronde", ronde);
        d.putBoolean("RondeKlaar", false);
        d.putBoolean("RondeBezig", true);
        for (Feesttaak t : Feesttaak.values()) {
            if (gevraagd.contains(t)) {
                zet(player, t, Stap.GEVRAAGD);
            }
        }
    }

    /** The current round: 0 = the Grote Knusfeest, n > 0 = the seasonal feest of season n - 1; -1 = none yet. */
    public static long ronde(ServerPlayer player) {
        CompoundTag d = data(player);
        return d.getBooleanOr("RondeBezig", false) || d.contains("Ronde") ? d.getLongOr("Ronde", 0L) : -1;
    }

    /** Is a round going on (asked and not celebrated yet)? */
    public static boolean rondeBezig(ServerPlayer player) {
        return data(player).getBooleanOr("RondeBezig", false);
    }

    /** The round was celebrated (finale or seasonal feest): nothing is open any more. */
    public static void rondeGevierd(ServerPlayer player) {
        CompoundTag d = data(player);
        d.putBoolean("RondeBezig", false);
        d.putBoolean("RondeKlaar", true);
        if (d.getLongOr("Ronde", 0L) == 0) {
            d.putBoolean("Klaar", true);
        } else {
            d.putLong("LaatsteSeizoensfeest", d.getLongOr("Ronde", 0L));
        }
        d.put("Stappen", new CompoundTag());
    }

    /** Has the player celebrated the Grote Knusfeest (the finale)? */
    public static boolean isKlaar(ServerPlayer player) {
        return data(player).getBooleanOr("Klaar", false);
    }

    /** Counts this season's feast as done (the Grote Knusfeest's finale: the seasonal feasts start next season). */
    public static void markeerSeizoensfeest(ServerPlayer player, long ronde) {
        CompoundTag d = data(player);
        d.putLong("LaatsteSeizoensfeest", Math.max(d.getLongOr("LaatsteSeizoensfeest", 0L), ronde));
    }

    /** The last seasonal round that was celebrated (0: none). */
    public static long laatsteSeizoensfeest(ServerPlayer player) {
        return data(player).getLongOr("LaatsteSeizoensfeest", 0L);
    }

    /** (Tests / new start) forgets everything. */
    public static void vergeet(ServerPlayer player) {
        GuhQuests.saved(player).remove(KEY);
    }

    /** The tasks in a fixed order, as a list (for screens). */
    public static List<Feesttaak> lijst(Set<Feesttaak> set) {
        List<Feesttaak> out = new ArrayList<>();
        for (Feesttaak t : Feesttaak.values()) {
            if (set.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }
}
