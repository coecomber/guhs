package nl.juiced.guhs.feature.vadsrijk.huizen;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * U-HUIZEN: "rust-xp thuis". Log out INSIDE your own house and you come back rested: per hour away the character you log in with
 * gets {@value #PER_UUR_PROCENT} % of a level's xp as a rested pool (max {@value #MAX_LEVELS} levels' worth). While the pool lasts,
 * every xp gain counts double (the bonus comes out of the pool), WoW-style. The pool lives with the character
 * ({@code SpelerData.extra("huizen").rust}), so each character rests on its own.
 */
public final class RustXp {
    public static final int PER_UUR_PROCENT = 5;
    public static final double MAX_LEVELS = 1.5;

    private RustXp() {
    }

    private static CompoundTag tag(ServerPlayer p) {
        return Spelers.data(p).extra("huizen");
    }

    public static long pot(ServerPlayer p) {
        return tag(p).getLongOr("rust", 0);
    }

    static long max(ServerPlayer p) {
        int lv = Math.min(Levels.MAX_GENERIEK, Math.max(1, Spelers.level(p)));
        return (long) (Levels.xpVoorGeneriek(lv) * MAX_LEVELS);
    }

    /** Adds the rest for {@code uren} hours away; returns what was added. */
    public static long geef(ServerPlayer p, double uren) {
        if (uren <= 0) {
            return 0;
        }
        int lv = Math.min(Levels.MAX_GENERIEK, Math.max(1, Spelers.level(p)));
        long erbij = (long) (Levels.xpVoorGeneriek(lv) * PER_UUR_PROCENT / 100.0 * uren);
        long oud = pot(p), nieuw = Math.min(max(p), oud + erbij);
        tag(p).putLong("rust", nieuw);
        Spelers.opgeslagen(p);
        return nieuw - oud;
    }

    /** kern hook ({@code Spelers.XP_EXTRA}): the rested bonus for a gain of {@code n} (taken from the pool). */
    public static long bonus(ServerPlayer p, long n) {
        long pot = pot(p);
        if (pot <= 0 || n <= 0) {
            return 0;
        }
        long b = Math.min(pot, n);
        tag(p).putLong("rust", pot - b);
        if (pot - b <= 0) {
            p.sendSystemMessage(Component.translatable("vadsrijk.huizen.rust.op").withStyle(ChatFormatting.AQUA));
        }
        return b;
    }
}
