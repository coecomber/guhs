package nl.juiced.guhs.feature.safari.kern;

import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Op Safari! step 2 (S0): Safarimunten, the park's own currency. Same pattern as guhpixel's Muntjes: one number per player
 * (NOT an item, not tradeable), kept in {@link SafariData}.
 * <ul>
 *   <li>{@link #geef}: pays n (balance and ever-earned total), with the overlay "+n Safarimunten" and a coin sound;</li>
 *   <li>{@link #verdienEens}: a key pays once per player forever (first photos, geheimpjes, zone badges, quests);</li>
 *   <li>{@link #verdienDagelijks}: a pot with a cap per real day (daily missions, aaihoek);</li>
 *   <li>{@link #kan} / {@link #neem}: the shop (false and nothing taken when the balance is too low).</li>
 * </ul>
 * {@code reden} is only for the log/overlay flavour, never a key. Amounts are the design's v1 numbers (design 1.8); the
 * slices pass them in, nothing here hard-codes a price.
 */
public final class SafariMunten {
    private static final String SALDO = "Munten", TOTAAL = "MuntenTotaal", EENS = "MuntenEens";

    public static int saldo(MinecraftServer server, UUID id) {
        return SafariSpeler.tag(server, id).getIntOr(SALDO, 0);
    }

    /** Ever earned; never goes down. */
    public static int totaal(MinecraftServer server, UUID id) {
        return SafariSpeler.tag(server, id).getIntOr(TOTAAL, 0);
    }

    /** Pays n (no overlay when the player is offline). */
    public static void geef(MinecraftServer server, UUID id, int n, String reden) {
        if (n <= 0) {
            return;
        }
        CompoundTag t = SafariSpeler.tag(server, id);
        t.putInt(SALDO, t.getIntOr(SALDO, 0) + n);
        t.putInt(TOTAAL, t.getIntOr(TOTAAL, 0) + n);
        SafariData.vuil(server);
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.munten.erbij", n).withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), nl.juiced.guhs.feature.safari.spullen.SafariGeluid.MUNT.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
            SafariSync.stuur(p);
        }
        SafariEvents.munten(server, id, n, reden);
    }

    public static boolean kan(MinecraftServer server, UUID id, int n) {
        return n >= 0 && saldo(server, id) >= n;
    }

    /** False and nothing taken when the balance is too low. */
    public static boolean neem(MinecraftServer server, UUID id, int n) {
        if (!kan(server, id, n)) {
            return false;
        }
        CompoundTag t = SafariSpeler.tag(server, id);
        t.putInt(SALDO, t.getIntOr(SALDO, 0) - n);
        SafariData.vuil(server);
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) {
            SafariSync.stuur(p);
        }
        SafariEvents.munten(server, id, -n, "betaald");
        return true;
    }

    /** True when paid now: a key pays once per player forever. */
    public static boolean verdienEens(MinecraftServer server, UUID id, String sleutel, int n, String reden) {
        CompoundTag eens = SafariData.sub(SafariSpeler.tag(server, id), EENS);
        if (n <= 0 || eens.getBooleanOr(sleutel, false)) {
            return false;
        }
        eens.putBoolean(sleutel, true);
        geef(server, id, n, reden);
        return true;
    }

    public static boolean isVerdiend(MinecraftServer server, UUID id, String sleutel) {
        return SafariSpeler.tag(server, id).getCompoundOrEmpty(EENS).getBooleanOr(sleutel, false);
    }

    /** Pays from a pot with a cap per real day; returns what was really paid (0..n). */
    public static int verdienDagelijks(MinecraftServer server, UUID id, String pot, int n, int dagMax, String reden) {
        int nu = SafariSpeler.telDag(server, id, "munten:" + pot, n, dagMax);
        geef(server, id, nu, reden);
        return nu;
    }

    /** (Dev) sets the balance; the total only grows with it. */
    public static void zet(MinecraftServer server, UUID id, int saldo) {
        CompoundTag t = SafariSpeler.tag(server, id);
        t.putInt(SALDO, Math.max(0, saldo));
        t.putInt(TOTAAL, Math.max(t.getIntOr(TOTAAL, 0), saldo));
        SafariData.vuil(server);
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) {
            SafariSync.stuur(p);
        }
    }

    // --- ServerPlayer shortcuts -----------------------------------------------------------------------------------------

    public static int saldo(ServerPlayer p) {
        return saldo(p.level().getServer(), p.getUUID());
    }

    public static void geef(ServerPlayer p, int n, String reden) {
        geef(p.level().getServer(), p.getUUID(), n, reden);
    }

    public static boolean kan(ServerPlayer p, int n) {
        return kan(p.level().getServer(), p.getUUID(), n);
    }

    public static boolean neem(ServerPlayer p, int n) {
        return neem(p.level().getServer(), p.getUUID(), n);
    }

    public static boolean verdienEens(ServerPlayer p, String sleutel, int n, String reden) {
        return verdienEens(p.level().getServer(), p.getUUID(), sleutel, n, reden);
    }

    public static int verdienDagelijks(ServerPlayer p, String pot, int n, int dagMax, String reden) {
        return verdienDagelijks(p.level().getServer(), p.getUUID(), pot, n, dagMax, reden);
    }

    private SafariMunten() {
    }
}
