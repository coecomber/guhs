package nl.juiced.guhs.feature.vadsrijk.zone6.wezens;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Z6Wezen;

/**
 * Hooks of the zone-6 combat slice for other slices (u-z6verhaal's quests, achievements, ambacht):
 * <ul>
 *   <li>{@link #WEZEN} (player, creature): a zone-6 creature cheered up / beaten by this player (also
 *       {@code NpcApi.meld(p, "z6_wezen", <naam>)}: quests like "Zout op de staart" (zoutkreeft) or "De Dadelapenbende" count it);</li>
 *   <li>{@link #BAAS} (player, boss id "wormelien" | "zoutkolos" | "astrolabiumwachter" | "glimmertje" | "sterrenguh" |
 *       "zoutslang"): a zone-6 boss / the cave elite beaten (also {@code NpcApi.meld(p, "z6_baas", <id>)});</li>
 *   <li>{@link #KOMPAS} (player): the Verzonken Sterrenguh is cheered up and hands over the <b>Sterrenkompas</b> (hq08: "haal het
 *       Sterrenkompas terug"); also {@code NpcApi.meld(p, "z6_sterrenwacht", "kompas")}.</li>
 * </ul>
 * Story starts the other way round: {@code zone6.wezens.baas.Z6Bazen.start(ServerPlayer, String fase)} (static; "wormelien",
 * "zoutkolos", "sterrenwacht").
 */
public final class Z6WezensApi {
    public static final List<BiConsumer<ServerPlayer, Z6Wezen>> WEZEN = new CopyOnWriteArrayList<>();
    public static final List<BiConsumer<ServerPlayer, String>> BAAS = new CopyOnWriteArrayList<>();
    public static final List<Consumer<ServerPlayer>> KOMPAS = new CopyOnWriteArrayList<>();

    private Z6WezensApi() {
    }

    public static void verslagen(ServerPlayer p, Z6Wezen w) {
        for (BiConsumer<ServerPlayer, Z6Wezen> h : WEZEN) {
            veilig(() -> h.accept(p, w));
        }
        veilig(() -> NpcApi.meld(p, "z6_wezen", w.naam()));
    }

    public static void baas(ServerPlayer p, String id) {
        for (BiConsumer<ServerPlayer, String> h : BAAS) {
            veilig(() -> h.accept(p, id));
        }
        veilig(() -> NpcApi.meld(p, "z6_baas", id));
    }

    public static void kompas(ServerPlayer p) {
        for (Consumer<ServerPlayer> h : KOMPAS) {
            veilig(() -> h.accept(p));
        }
        veilig(() -> NpcApi.meld(p, "z6_sterrenwacht", "kompas"));
    }

    private static void veilig(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("guhs/z6wezens").warn("z6wezens hook failed", e);
        }
    }
}
