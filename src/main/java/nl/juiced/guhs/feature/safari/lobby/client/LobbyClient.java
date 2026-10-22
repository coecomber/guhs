package nl.juiced.guhs.feature.safari.lobby.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.safari.lobby.LobbyPayloads;

/** The client side of the safari lobby: opens (or refreshes) the giftshop screen; the Kriebelhuis expert's and the Aussie guhs' own models. */
public final class LobbyClient {
    public static void init(IEventBus modBus) {
        // round 4 (L4): the ninth expert wears a lab coat and holds a big magnifying glass (tools/features/safari_lobby.py)
        nl.juiced.guhs.client.SittingGuhRenderers.NPC_MODELEN.put(nl.juiced.guhs.entity.GuhNpcEntity.Kind.SAFARI_EXPERT_KRIEBELHUIS,
                nl.juiced.guhs.Guhs.id("entity/guh_npc_safari_expert_kriebelhuis"));
        // test round 5: the three Aussie guhs of Australia wear a cork hat (tools/features/safari_g1.py aussies)
        for (var kind : java.util.List.of(nl.juiced.guhs.entity.GuhNpcEntity.Kind.SAFARI_AUSSIE_HANGMAT, nl.juiced.guhs.entity.GuhNpcEntity.Kind.SAFARI_AUSSIE_POST,
                nl.juiced.guhs.entity.GuhNpcEntity.Kind.SAFARI_AUSSIE_KAMP)) {
            nl.juiced.guhs.client.SittingGuhRenderers.NPC_MODELEN.put(kind, nl.juiced.guhs.Guhs.id("entity/guh_npc_" + kind.id()));
        }
        LobbyPayloads.winkelOpener = p -> {
            Minecraft mc = Minecraft.getInstance();
            Runnable r = () -> {
                if (mc.screen instanceof SafariWinkelScherm s) {
                    s.update(p.data());
                } else {
                    mc.setScreen(new SafariWinkelScherm(p.data()));
                }
            };
            if (mc.isSameThread()) {
                r.run();
            } else {
                mc.execute(r);
            }
        };
    }

    private LobbyClient() {
    }
}
