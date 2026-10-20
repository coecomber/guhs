package nl.juiced.guhs.feature.safari.kern;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.safari.ParkData;

/**
 * Op Safari! step 2 (S0): what one player has done in the safari, server-authoritative (kept in {@link SafariData}).
 * Every method exists for (server, uuid), so it works for offline players and in gametests, and most also for a
 * {@link ServerPlayer}. A change marks the data dirty and (for an online player) sends the client its {@link SafariSync}.
 * <ul>
 *   <li><b>dex</b> per species: spotted, best photo stars 0..3, and that photo's data (a free tag for the camera slice:
 *       {@link #registreerFoto}); a better photo fires {@link SafariEvents#dexNieuw}, a full zone {@link SafariEvents#zoneCompleet};</li>
 *   <li><b>geheimpjes</b> found ({@link #vindGeheimpje}), <b>badges</b> claimed ({@link #claimBadge});</li>
 *   <li><b>vlaggen</b> (welkom gezien, lodge, ...), <b>quest</b> states (a free string per key);</li>
 *   <li><b>dagtellers</b> per real day (missions, aaihoek: {@link #telDag});</li>
 *   <li><b>bord-cooldown</b> per species: one verblijfsbord per 7 real days ({@link #kanBordKopen}, {@link #bordGekocht}).</li>
 * </ul>
 * Real time is guhpixel's {@link Klok} (wall clock plus the dev offset): store absolute {@code Klok.nu()} stamps.
 */
public final class SafariSpeler {
    public static final long BORD_COOLDOWN = 7 * Klok.DAG;
    private static final String DEX = "Dex", GEZIEN = "Gezien", STERREN = "Sterren", FOTO = "Foto", GEHEIM = "Geheimpjes",
            BADGES = "Badges", VLAG = "Vlaggen", QUEST = "Quests", DAG = "Dag", DAGNR = "DagNr", BORD = "Bord";

    /** One species in this player's dex. */
    public record DexRegel(boolean gezien, int sterren, CompoundTag foto) {
    }

    static CompoundTag tag(MinecraftServer server, UUID id) {
        return SafariData.speler(server, id);
    }

    private static void veranderd(MinecraftServer server, UUID id) {
        SafariData.vuil(server);
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) {
            SafariSync.stuur(p);
        }
    }

    // --- dex ------------------------------------------------------------------------------------------------------------

    public static DexRegel dex(MinecraftServer server, UUID id, String soort) {
        CompoundTag d = tag(server, id).getCompoundOrEmpty(DEX).getCompoundOrEmpty(soort);
        return new DexRegel(d.getBooleanOr(GEZIEN, false), d.getIntOr(STERREN, 0), d.getCompoundOrEmpty(FOTO).copy());
    }

    /** The species this player has photographed (stars >= 1), in dex order. */
    public static Set<String> gefotografeerd(MinecraftServer server, UUID id) {
        Set<String> uit = new LinkedHashSet<>();
        CompoundTag dex = tag(server, id).getCompoundOrEmpty(DEX);
        for (String s : dex.keySet()) {
            if (dex.getCompoundOrEmpty(s).getIntOr(STERREN, 0) > 0) {
                uit.add(s);
            }
        }
        return uit;
    }

    /** Marks a species as spotted (seen close by); true when that is new. */
    public static boolean spot(MinecraftServer server, UUID id, String soort) {
        CompoundTag d = SafariData.sub(SafariData.sub(tag(server, id), DEX), soort);
        if (d.getBooleanOr(GEZIEN, false)) {
            return false;
        }
        d.putBoolean(GEZIEN, true);
        veranderd(server, id);
        return true;
    }

    /**
     * A photo of a species with 1..3 stars. Kept only when it beats the best one (more stars); then {@code foto} (any tag
     * the camera slice wants for the album) replaces the old one, {@link SafariEvents#dexNieuw} fires with the previous
     * stars, and when this completed the species' zone, {@link SafariEvents#zoneCompleet}. Returns the previous best stars
     * (so {@code result < sterren} means "improved").
     */
    public static int registreerFoto(MinecraftServer server, UUID id, String soort, int sterren, CompoundTag foto) {
        sterren = Math.max(1, Math.min(3, sterren));
        CompoundTag d = SafariData.sub(SafariData.sub(tag(server, id), DEX), soort);
        int was = d.getIntOr(STERREN, 0);
        d.putBoolean(GEZIEN, true);
        if (sterren <= was) {
            return was;
        }
        boolean zoneWasCompleet = zoneCompleet(server, id, zoneVan(soort));
        d.putInt(STERREN, sterren);
        d.put(FOTO, foto == null ? new CompoundTag() : foto.copy());
        veranderd(server, id);
        SafariEvents.dexNieuw(server, id, soort, sterren, was);
        String zone = zoneVan(soort);
        if (!zoneWasCompleet && zone != null && zoneCompleet(server, id, zone)) {
            SafariEvents.zoneCompleet(server, id, zone);
        }
        return was;
    }

    private static String zoneVan(String soort) {
        ParkData.Soort s = ParkData.get().soort(soort);
        return s == null ? null : s.zone();
    }

    /** True when every dex species of this zone (aaihoek and the gouden dikdik do not count) has a photo. */
    public static boolean zoneCompleet(MinecraftServer server, UUID id, String zone) {
        if (zone == null || zone.equals("entree")) {
            return false;
        }
        Set<String> foto = gefotografeerd(server, id);
        boolean iets = false;
        for (ParkData.Soort s : ParkData.get().soorten.values()) {
            if (zone.equals(s.zone()) && telt(s.naam())) {
                iets = true;
                if (!foto.contains(s.naam())) {
                    return false;
                }
            }
        }
        return iets;
    }

    /** Species that count for the dex (the park's safari animals: not the aaihoek, not the secret gouden dikdik). */
    public static boolean telt(String soort) {
        ParkData.Soort s = ParkData.get().soort(soort);
        return s != null && s.zone() != null && !s.zone().isEmpty() && !s.zone().equals("entree") && !soort.equals("gouden_dikdik");
    }

    // --- geheimpjes, badges, vlaggen, quests ----------------------------------------------------------------------------

    /** True when this geheimpje is new for this player (then {@link SafariEvents#geheimpje} fires). */
    public static boolean vindGeheimpje(MinecraftServer server, UUID id, String geheimpje) {
        CompoundTag g = SafariData.sub(tag(server, id), GEHEIM);
        if (g.getBooleanOr(geheimpje, false)) {
            return false;
        }
        g.putBoolean(geheimpje, true);
        veranderd(server, id);
        SafariEvents.geheimpje(server, id, geheimpje);
        return true;
    }

    public static Set<String> geheimpjes(MinecraftServer server, UUID id) {
        return new LinkedHashSet<>(tag(server, id).getCompoundOrEmpty(GEHEIM).keySet());
    }

    /** A zone badge claimed at its expert-guh; true when that is new. */
    public static boolean claimBadge(MinecraftServer server, UUID id, String zone) {
        CompoundTag b = SafariData.sub(tag(server, id), BADGES);
        if (b.getBooleanOr(zone, false)) {
            return false;
        }
        b.putBoolean(zone, true);
        veranderd(server, id);
        return true;
    }

    public static boolean heeftBadge(MinecraftServer server, UUID id, String zone) {
        return tag(server, id).getCompoundOrEmpty(BADGES).getBooleanOr(zone, false);
    }

    public static boolean vlag(MinecraftServer server, UUID id, String vlag) {
        return tag(server, id).getCompoundOrEmpty(VLAG).getBooleanOr(vlag, false);
    }

    public static void zetVlag(MinecraftServer server, UUID id, String vlag, boolean aan) {
        CompoundTag v = SafariData.sub(tag(server, id), VLAG);
        if (v.getBooleanOr(vlag, false) == aan) {
            return;
        }
        if (aan) {
            v.putBoolean(vlag, true);
        } else {
            v.remove(vlag);
        }
        veranderd(server, id);
    }

    /** A quest state ("" when never set). Keys are owned by the quest slice, e.g. {@code vermomde_guh}. */
    public static String quest(MinecraftServer server, UUID id, String quest) {
        return tag(server, id).getCompoundOrEmpty(QUEST).getStringOr(quest, "");
    }

    public static void zetQuest(MinecraftServer server, UUID id, String quest, String stand) {
        SafariData.sub(tag(server, id), QUEST).putString(quest, stand);
        veranderd(server, id);
    }

    // --- dagtellers -----------------------------------------------------------------------------------------------------

    /** What this counter reached today (a new real day starts every counter at 0). */
    public static int vandaag(MinecraftServer server, UUID id, String teller) {
        return dag(server, id).getIntOr(teller, 0);
    }

    /** Adds up to {@code n} to today's counter, never past {@code max}; returns what was really added (0..n). */
    public static int telDag(MinecraftServer server, UUID id, String teller, int n, int max) {
        CompoundTag d = dag(server, id);
        int al = d.getIntOr(teller, 0);
        int erbij = Math.max(0, Math.min(n, max - al));
        if (erbij > 0) {
            d.putInt(teller, al + erbij);
            veranderd(server, id);
        }
        return erbij;
    }

    private static CompoundTag dag(MinecraftServer server, UUID id) {
        CompoundTag t = tag(server, id);
        long nu = Klok.dag();
        if (t.getLongOr(DAGNR, Long.MIN_VALUE) != nu) {
            t.putLong(DAGNR, nu);
            t.put(DAG, new CompoundTag());
            SafariData.vuil(server);
        }
        return SafariData.sub(t, DAG);
    }

    // --- verblijfsbord-cooldown -----------------------------------------------------------------------------------------

    /** The Klok time from when this player may buy a board of this species again (0: now). */
    public static long bordVrijOp(MinecraftServer server, UUID id, String soort) {
        return tag(server, id).getCompoundOrEmpty(BORD).getLongOr(soort, 0L);
    }

    public static boolean kanBordKopen(MinecraftServer server, UUID id, String soort) {
        return Klok.nu() >= bordVrijOp(server, id, soort);
    }

    /** Starts the 7-day cooldown for this species. */
    public static void bordGekocht(MinecraftServer server, UUID id, String soort) {
        SafariData.sub(tag(server, id), BORD).putLong(soort, Klok.nu() + BORD_COOLDOWN);
        veranderd(server, id);
    }

    // --- ServerPlayer shortcuts -----------------------------------------------------------------------------------------

    public static DexRegel dex(ServerPlayer p, String soort) {
        return dex(p.level().getServer(), p.getUUID(), soort);
    }

    public static int registreerFoto(ServerPlayer p, String soort, int sterren, CompoundTag foto) {
        return registreerFoto(p.level().getServer(), p.getUUID(), soort, sterren, foto);
    }

    public static boolean vlag(ServerPlayer p, String vlag) {
        return vlag(p.level().getServer(), p.getUUID(), vlag);
    }

    public static void zetVlag(ServerPlayer p, String vlag, boolean aan) {
        zetVlag(p.level().getServer(), p.getUUID(), vlag, aan);
    }

    /** (Dev, tests) forgets everything this player did in the safari (the safarimunten too). */
    public static void wis(MinecraftServer server, UUID id) {
        SafariData.wis(server, id);
        veranderd(server, id);
    }

    private SafariSpeler() {
    }
}
