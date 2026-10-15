package nl.juiced.guhs.feature.snuffel;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The dog a player chose for Het Snuffeleiland: breed, coat colour, the dog's name, and which of the three companions
 * comes along. Kept per player ({@link SnuffelData}, key {@code Keuze}). The choice screen only opens where the story
 * allows it: the server opens it ({@link #open}) and only then accepts an answer ({@link #ontvang}); a client that sends
 * a choice by itself is ignored.
 * <p>
 * A player who lands on the island without ever choosing (a command, an op) is the {@link #standaard} dog.
 */
public record Keuze(String ras, String kleur, String naam, String maatje) {
    public static final int NAAM_MAX = 16;
    private static final Set<UUID> OPEN = ConcurrentHashMap.newKeySet();
    private static final List<BiConsumer<ServerPlayer, Keuze>> LUISTERAARS = new CopyOnWriteArrayList<>();

    /** Did this player choose a dog? */
    public static boolean heeft(Player p) {
        return SnuffelData.heeft(p) && SnuffelData.van(p).getCompound("Keuze").isPresent();
    }

    /** The chosen dog (null: never chose). */
    @Nullable
    public static Keuze van(Player p) {
        if (!heeft(p)) {
            return null;
        }
        CompoundTag t = SnuffelData.van(p).getCompoundOrEmpty("Keuze");
        Keuze k = new Keuze(t.getStringOr("Ras", ""), t.getStringOr("Kleur", ""), t.getStringOr("Naam", ""), t.getStringOr("Maatje", ""));
        return k.geldig() ? k : null;
    }

    /** The chosen dog, or the dog of somebody who never chose: a red shiba with the player's own name and companion B. */
    public static Keuze vanOfStandaard(Player p) {
        Keuze k = van(p);
        return k != null ? k : standaard(p);
    }

    public static Keuze standaard(Player p) {
        return new Keuze("shiba", "rood", netjes(p.getGameProfile().name(), "Snuffel"), "b");
    }

    /** A playable breed with one of its coats, a name, one of the three companions. */
    public boolean geldig() {
        Honden.Ras r = Honden.ras(ras);
        return r != null && r.speelbaar() && r.kleuren().contains(kleur) && !naam.isBlank() && naam.length() <= NAAM_MAX && Honden.MAATJES.contains(maatje);
    }

    /** A name as it may be kept: no control characters or formatting codes, trimmed, at most {@link #NAAM_MAX} long. */
    public static String netjes(@Nullable String naam, String anders) {
        StringBuilder b = new StringBuilder();
        if (naam != null) {
            naam.codePoints().filter(c -> c >= 32 && c != 127 && c != 0xA7 && Character.isDefined(c)).forEach(b::appendCodePoint);
        }
        String s = b.toString().strip().replaceAll("\\s+", " ");
        if (s.length() > NAAM_MAX) {
            s = s.substring(0, NAAM_MAX).strip();
        }
        return s.isEmpty() ? anders : s;
    }

    /**
     * Stores the choice (false: not a valid one). Everybody who sees the player learns the new look at once, and the
     * listeners ({@link #opKeuze}) hear it.
     */
    public static boolean zet(ServerPlayer p, Keuze k) {
        Keuze schoon = new Keuze(k.ras(), k.kleur(), netjes(k.naam(), p.getGameProfile().name()), k.maatje());
        if (!schoon.geldig()) {
            return false;
        }
        CompoundTag t = new CompoundTag();
        t.putString("Ras", schoon.ras());
        t.putString("Kleur", schoon.kleur());
        t.putString("Naam", schoon.naam());
        t.putString("Maatje", schoon.maatje());
        SnuffelData.van(p).put("Keuze", t);
        Hondvorm.sync(p);
        Hondvorm.ververs(p);
        Maatjes.opnieuw(p);
        Stand.stuur(p);
        for (BiConsumer<ServerPlayer, Keuze> l : LUISTERAARS) {
            l.accept(p, schoon);
        }
        return true;
    }

    /**
     * The story lets this player choose (the first time at the dock, before sailing) or choose again: the choice screen
     * opens with the dog and the companion on two pages. Until an answer comes (or the player logs out) one answer is
     * accepted.
     */
    public static void open(ServerPlayer p) {
        OPEN.add(p.getUUID());
        Keuze nu = van(p);
        CompoundTag data = new CompoundTag();
        if (nu != null) {
            data.putString("Ras", nu.ras());
            data.putString("Kleur", nu.kleur());
            data.putString("Naam", nu.naam());
            data.putString("Maatje", nu.maatje());
        }
        ModNetworking.sendTo(p, new SnuffelPayloads.Open(SnuffelPayloads.Open.KEUZE, data));
    }

    /** May this player answer the choice screen now? */
    public static boolean magKiezen(ServerPlayer p) {
        return OPEN.contains(p.getUUID());
    }

    /** Hears every accepted choice (the dock goes on with its story from here). */
    public static void opKeuze(BiConsumer<ServerPlayer, Keuze> l) {
        LUISTERAARS.add(l);
    }

    /** The answer of the choice screen. Ignored unless the server opened that screen for this player. */
    static boolean ontvang(ServerPlayer p, Keuze k) {
        if (!OPEN.contains(p.getUUID())) {
            return false;
        }
        if (!zet(p, k)) {
            return false;
        }
        OPEN.remove(p.getUUID());
        return true;
    }

    static void vergeetOpen(UUID id) {
        OPEN.remove(id);
    }
}
