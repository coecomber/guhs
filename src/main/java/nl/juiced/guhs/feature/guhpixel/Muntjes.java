package nl.juiced.guhs.feature.guhpixel;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Guhpixel-muntjes: one number per player (NOT an item, not tradeable), kept in {@link PxData} ("kern").
 * <ul>
 *   <li>{@link #verdienEens}: a key pays once per player forever (the whole one-time budget is 750: six joke games of 100
 *   ({@code grap:<id>}, paid by {@link Grappen#voltooi}), {@code lobby:parkour} 50, {@code lobby:knabbel_0..9} 10 each);</li>
 *   <li>{@link #verdienDagelijks}: a pot with a cap per REAL day ({@link Klok#dag}); the only pot is {@code among};</li>
 *   <li>{@link #betaal}: the shop ({@link Winkel}).</li>
 * </ul>
 * {@link #totaal} (ever earned) never goes down and decides the {@link Rang}. Every payment shows "+n muntjes" (overlay and
 * a sound) and refreshes the HUD.
 */
public final class Muntjes {
    public static final String KERN = "kern";
    private static final String SALDO = "Saldo", TOTAAL = "Totaal", EENS = "Eens", DAG = "PotDag", POTTEN = "Potten";

    static CompoundTag data(ServerPlayer p) {
        return PxData.deel(p, KERN);
    }

    public static int saldo(ServerPlayer p) {
        return data(p).getIntOr(SALDO, 0);
    }

    /** Ever earned; never goes down. */
    public static int totaal(ServerPlayer p) {
        return data(p).getIntOr(TOTAAL, 0);
    }

    public static Rang rang(ServerPlayer p) {
        return Rang.bij(totaal(p));
    }

    /** True when paid now: a sleutel pays once per player forever. */
    public static boolean verdienEens(ServerPlayer p, String sleutel, int n) {
        CompoundTag eens = PxData.sub(data(p), EENS);
        if (n <= 0 || eens.getBooleanOr(sleutel, false)) {
            return false;
        }
        eens.putBoolean(sleutel, true);
        geef(p, n);
        return true;
    }

    public static boolean isVerdiend(ServerPlayer p, String sleutel) {
        return data(p).getCompoundOrEmpty(EENS).getBooleanOr(sleutel, false);
    }

    /** Pays from a pot with a cap per real day; returns what was really paid (0..n). */
    public static int verdienDagelijks(ServerPlayer p, String pot, int n, int dagMax) {
        CompoundTag potten = potten(p);
        int al = potten.getIntOr(pot, 0);
        int nu = Math.max(0, Math.min(n, dagMax - al));
        if (nu > 0) {
            potten.putInt(pot, al + nu);
            geef(p, nu);
        }
        return nu;
    }

    /** What this pot already paid today. */
    public static int vandaag(ServerPlayer p, String pot) {
        return potten(p).getIntOr(pot, 0);
    }

    /** The pots of today (a new real day empties them). */
    private static CompoundTag potten(ServerPlayer p) {
        CompoundTag d = data(p);
        long dag = Klok.dag();
        if (d.getLongOr(DAG, Long.MIN_VALUE) != dag) {
            d.putLong(DAG, dag);
            d.put(POTTEN, new CompoundTag());
            PxData.vuil(p.level().getServer());
        }
        return PxData.sub(d, POTTEN);
    }

    /** (Dev) empties today's pots. */
    public static void dagpotReset(ServerPlayer p) {
        data(p).put(POTTEN, new CompoundTag());
        PxData.vuil(p.level().getServer());
    }

    /** False and nothing taken when the saldo is too low. */
    public static boolean betaal(ServerPlayer p, int n) {
        CompoundTag d = data(p);
        int saldo = d.getIntOr(SALDO, 0);
        if (n < 0 || saldo < n) {
            return false;
        }
        d.putInt(SALDO, saldo - n);
        PxData.vuil(p.level().getServer());
        GuhpixelPayloads.hud(p);
        return true;
    }

    /** (Dev) sets the saldo; the total only grows with it. */
    public static void zet(ServerPlayer p, int saldo) {
        CompoundTag d = data(p);
        d.putInt(SALDO, Math.max(0, saldo));
        d.putInt(TOTAAL, Math.max(d.getIntOr(TOTAAL, 0), saldo));
        PxData.vuil(p.level().getServer());
        Rangen.ververs(p);
        GuhpixelPayloads.hud(p);
    }

    /** (Dev) forgets everything this player earned (the once-keys too). */
    public static void wis(ServerPlayer p) {
        CompoundTag d = data(p);
        for (String k : new String[] {SALDO, TOTAAL, EENS, DAG, POTTEN}) {
            d.remove(k);
        }
        PxData.vuil(p.level().getServer());
        Rangen.ververs(p);
        GuhpixelPayloads.hud(p);
    }

    /** Adds muntjes (saldo and total), with the overlay, the sound and the HUD. Only the two verdien methods and the dev command use it. */
    static void geef(ServerPlayer p, int n) {
        CompoundTag d = data(p);
        Rang voor = Rang.bij(d.getIntOr(TOTAAL, 0));
        d.putInt(SALDO, d.getIntOr(SALDO, 0) + n);
        d.putInt(TOTAAL, d.getIntOr(TOTAAL, 0) + n);
        PxData.vuil(p.level().getServer());
        p.sendOverlayMessage(Component.translatable("gui.guhs.guhpixel.muntjes.erbij", n).withStyle(ChatFormatting.GOLD));
        PxGeluid.speel(p, GuhpixelFeature.MUNTJE.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
        Rang na = Rang.bij(d.getIntOr(TOTAAL, 0));
        if (na != voor) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhpixel.rang.nieuw", na.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
            Rangen.ververs(p);
        }
        GuhpixelPayloads.hud(p);
    }

    private Muntjes() {
    }
}
