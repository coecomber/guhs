package nl.juiced.guhs.feature.guhpad;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.GrootVerhaal;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Het Guhpad: the regular look at where a player is on the path ({@link #kijk}: every {@link #CHECK_TICKS} ticks and at
 * login). It only reads the stories' own progress and never moves or changes a player:
 * <ul>
 *   <li>a finished big story gives the hidden advancement {@code guhs:quest/guhpad_klaar_<id>}, a world that is open
 *       {@code guhs:quest/guhpad_open_<wereld>}, the beaten Aangebrande Mika {@code guhs:quest/guhpad_mika}: the lock
 *       quests of the FTB chapter group "Het Guhpad" tick themselves off with them;</li>
 *   <li>the statistic {@code guhs:verhalen_gevolgd} holds the number of finished big stories (the counter "Verhalen
 *       gevolgd" of the FTB chapter "Het echte Guheinde" is a stat task on it);</li>
 *   <li>the client is told when something changed ({@link GuhpadPayloads.Stand}).</li>
 * </ul>
 */
public final class GuhpadEvents {
    public static final int CHECK_TICKS = 40;
    /** What each online player was told last. */
    private static final Map<UUID, GuhpadPayloads.Stand> VERTELD = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % CHECK_TICKS == 21) {
            kijk(p);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            VERTELD.remove(p.getUUID());
            kijk(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        VERTELD.remove(id);
        Guhpad.vergeet(id);
        GuhpadKompas.vergeet(id);
    }

    /** The statistic "Verhalen gevolgd" (a custom stat; its value is set, never counted up). */
    public static Stat<?> statistiek() {
        // (the registered instance: the registry of custom stats knows its values by identity, not by equals)
        return Stats.CUSTOM.get(GuhpadFeature.VERHALEN_GEVOLGD.get());
    }

    /** Looks where this player is on the path (see the class text). Safe at any time, as often as you like. */
    public static void kijk(ServerPlayer p) {
        GuhpadPayloads.Stand stand = GuhpadPayloads.stand(p);
        for (GrootVerhaal v : GroteVerhalen.alle()) {
            if (v.klaar(p)) {
                GuhAdvancements.grant(p, "guhpad_klaar_" + v.id());
            }
        }
        for (Wereld w : Wereld.values()) {
            if (stand.open(w)) {
                GuhAdvancements.grant(p, "guhpad_open_" + w.id());
            }
        }
        if (stand.eisen().stream().anyMatch(e -> e.sleutel().equals(Guhpad.EIS_MIKA) && e.voldaan())) {
            GuhAdvancements.grant(p, "guhpad_mika");
        }
        Stat<?> stat = statistiek();
        if (p.getStats().getValue(stat) != stand.gevolgd()) {
            p.getStats().setValue(p, stat, stand.gevolgd());
        }
        if (!stand.equals(VERTELD.put(p.getUUID(), stand))) {
            ModNetworking.sendTo(p, stand);
        }
    }

    private GuhpadEvents() {
    }
}
