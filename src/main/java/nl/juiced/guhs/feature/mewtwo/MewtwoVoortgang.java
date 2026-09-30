package nl.juiced.guhs.feature.mewtwo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Per player: how far the questline of the kloon-eiland is (in {@code GuhQuests.saved(player)}, survives dying):
 * <ul>
 *   <li>{@code guhs_mewtwo_stap}: {@link #NIEUW} (never talked to the professor), {@link #NOTITIES} (looking for the 6 notes),
 *       {@link #ONDERDELEN} (repairing the tank), {@link #MAALTIJD} (Mieuwguh is here: the big meal), {@link #KLAAR} (done:
 *       the Guhtwo is released, VerhaalGuhs);</li>
 *   <li>{@code guhs_mewtwo_notities}: the notes found (bits 1..6), {@code guhs_mewtwo_onderdelen}: the parts found (bits 1..4),
 *       {@code guhs_mewtwo_ingebouwd}: the parts built in (bits 1..4), {@code guhs_mewtwo_knabbels} / {@code _snacks}: what's in
 *       the knabbelschaal.</li>
 * </ul>
 * The client gets its own copy ({@link MewtwoPayloads.Stand}): the kloontank's renderer shows YOUR tank (cracked or whole,
 * the lamps of the parts you built in), the note spots and crates sparkle while you still need them.
 */
public final class MewtwoVoortgang {
    public static final String STAP = "guhs_mewtwo_stap", NOTITIES_KEY = "guhs_mewtwo_notities", ONDERDELEN_KEY = "guhs_mewtwo_onderdelen",
            INGEBOUWD = "guhs_mewtwo_ingebouwd", KNABBELS = "guhs_mewtwo_knabbels", SNACKS = "guhs_mewtwo_snacks";
    public static final int NIEUW = 0, NOTITIES = 1, ONDERDELEN = 2, MAALTIJD = 3, KLAAR = 4;

    private static CompoundTag d(Player p) {
        return GuhQuests.saved(p);
    }

    public static int stap(Player p) {
        return d(p).getIntOr(STAP, 0);
    }

    public static void zetStap(ServerPlayer p, int stap) {
        d(p).putInt(STAP, stap);
        sync(p);
    }

    public static int notities(Player p) {
        return d(p).getIntOr(NOTITIES_KEY, 0);
    }

    public static boolean heeftNotitie(Player p, int n) {
        return (notities(p) & (1 << n)) != 0;
    }

    public static int aantalNotities(Player p) {
        return Integer.bitCount(notities(p) & 0b1111110);
    }

    /** Marks note n found; true when it was new. */
    public static boolean vondNotitie(ServerPlayer p, int n) {
        if (heeftNotitie(p, n)) {
            return false;
        }
        d(p).putInt(NOTITIES_KEY, notities(p) | (1 << n));
        sync(p);
        return true;
    }

    public static int onderdelen(Player p) {
        return d(p).getIntOr(ONDERDELEN_KEY, 0);
    }

    public static boolean heeftOnderdeel(Player p, int n) {
        return (onderdelen(p) & (1 << n)) != 0;
    }

    public static boolean vondOnderdeel(ServerPlayer p, int n) {
        if (heeftOnderdeel(p, n)) {
            return false;
        }
        d(p).putInt(ONDERDELEN_KEY, onderdelen(p) | (1 << n));
        sync(p);
        return true;
    }

    public static int ingebouwd(Player p) {
        return d(p).getIntOr(INGEBOUWD, 0);
    }

    public static boolean isIngebouwd(Player p, int n) {
        return (ingebouwd(p) & (1 << n)) != 0;
    }

    public static int aantalIngebouwd(Player p) {
        return Integer.bitCount(ingebouwd(p) & 0b11110);
    }

    public static void bouwIn(ServerPlayer p, int n) {
        d(p).putInt(INGEBOUWD, ingebouwd(p) | (1 << n));
        sync(p);
    }

    /** Is the tank repaired, for this player? */
    public static boolean tankHeel(Player p) {
        return stap(p) >= MAALTIJD;
    }

    public static int knabbels(Player p) {
        return d(p).getIntOr(KNABBELS, 0);
    }

    public static int snacks(Player p) {
        return d(p).getIntOr(SNACKS, 0);
    }

    public static void zetSchaal(ServerPlayer p, int knabbels, int snacks) {
        d(p).putInt(KNABBELS, knabbels);
        d(p).putInt(SNACKS, snacks);
    }

    /** Sends this player's state to its client (the tank, the spots). */
    public static void sync(ServerPlayer p) {
        ModNetworking.sendTo(p, new MewtwoPayloads.Stand(stap(p), notities(p), onderdelen(p), ingebouwd(p)));
    }

    /** (tests / ops) forget the whole questline of this player. */
    public static void wis(ServerPlayer p) {
        for (String k : new String[]{STAP, NOTITIES_KEY, ONDERDELEN_KEY, INGEBOUWD, KNABBELS, SNACKS}) {
            d(p).remove(k);
        }
        sync(p);
    }

    private MewtwoVoortgang() {
    }
}
