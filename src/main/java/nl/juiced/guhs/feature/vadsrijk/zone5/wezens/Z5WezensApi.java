package nl.juiced.guhs.feature.vadsrijk.zone5.wezens;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;

/**
 * Hooks of the zone-5 combat slice for other slices (quests, achievements):
 * <ul>
 *   <li>{@link #GESPARD} (player, wins so far): a sparring bout won in the Proefarena (also {@code NpcApi.meld(p, "z5_sparren",
 *       "gewonnen")});</li>
 *   <li>{@link #BAAS} (player, boss id "zilverstaart" | "gorgonzola" | "donderbuik"): a zone-5 boss beaten by this player (also
 *       {@code NpcApi.meld(p, "z5_baas", <id>)}); kern's own Haken.VERSLAGEN fires as for every creature.</li>
 * </ul>
 */
public final class Z5WezensApi {
    public static final List<BiConsumer<ServerPlayer, Integer>> GESPARD = new CopyOnWriteArrayList<>();
    public static final List<BiConsumer<ServerPlayer, String>> BAAS = new CopyOnWriteArrayList<>();

    private Z5WezensApi() {
    }

    public static void gespard(ServerPlayer p, int n) {
        for (BiConsumer<ServerPlayer, Integer> h : GESPARD) {
            h.accept(p, n);
        }
        veilig(() -> NpcApi.meld(p, "z5_sparren", "gewonnen"));
    }

    public static void baas(ServerPlayer p, String id) {
        for (BiConsumer<ServerPlayer, String> h : BAAS) {
            h.accept(p, id);
        }
        veilig(() -> NpcApi.meld(p, "z5_baas", id));
    }

    private static void veilig(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("guhs/z5wezens").warn("z5wezens hook failed", e);
        }
    }
}
