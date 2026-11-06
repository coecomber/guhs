package nl.juiced.guhs.feature.vadsrijk.zone4.bazen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Hooks for the other zone-4 slices (t-z4verhaal: the Knuffeltij story, quests, the NPCs). Every moment also goes to the npc
 * {@code NpcApi.meld} channel t-z4verhaal listens on: {@code z4_zeeslurf/opgevrolijkt} and {@code z4_vliegervanger/verslagen}.
 * {@link #MOMENT} fires per player who helped:
 * <ul>
 *   <li>{@code "baas:zeeslurf"} / {@code "baas:vliegervanger"}: the boss was beaten (cheered up / fled);</li>
 *   <li>{@code "zeeslurf_opgevrolijkt"}: the sea cow turned pink and sang (the lagoon is happy again);</li>
 *   <li>{@code "wrak_knuffels:<n>"}: how many of the stolen plushies the group caught in the Vliegervanger's last phase (0-3);
 *       the story can hand out that many Knuffeltij plushies.</li>
 * </ul>
 */
public final class Z4BazenApi {
    public static final List<BiConsumer<ServerPlayer, String>> MOMENT = new CopyOnWriteArrayList<>();

    private Z4BazenApi() {
    }

    public static void moment(ServerPlayer p, String id) {
        for (BiConsumer<ServerPlayer, String> m : MOMENT) {
            m.accept(p, id);
        }
        // t-z4verhaal listens on the npc "meld" channel (NOTITIES_T_Z4VERHAAL: z4_zeeslurf/opgevrolijkt, z4_vliegervanger/verslagen)
        if (id.equals("zeeslurf_opgevrolijkt")) {
            nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.meld(p, "z4_zeeslurf", "opgevrolijkt");
        } else if (id.equals("baas:vliegervanger")) {
            nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.meld(p, "z4_vliegervanger", "verslagen");
        }
    }

    static void verslagen(Z4Baas b) {
        if (!(b.level() instanceof ServerLevel sl)) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (b.bijdragen().spelers().contains(p.getUUID())) {
                moment(p, "baas:" + b.baasId());
                if (b instanceof Zeeslurf) {
                    moment(p, "zeeslurf_opgevrolijkt");
                }
                if (b instanceof Vliegervanger v) {
                    moment(p, "wrak_knuffels:" + v.knuffelsGered());
                }
            }
        }
    }
}
