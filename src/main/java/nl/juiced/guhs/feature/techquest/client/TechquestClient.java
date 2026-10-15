package nl.juiced.guhs.feature.techquest.client;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.techquest.TechquestFeature;

/**
 * Client side of bbq2 (tech-quests): how far the local player's own Grote Knabbelmachine is (sent by the server:
 * TechquestPayloads.Stand), the renderer that draws the machine accordingly on its kern block, and the model of the
 * Uitvinder-guh (welding goggles on his forehead, a wild white tuft, a pencil behind his ear, a leather apron).
 */
public final class TechquestClient {
    /** The stage of the local player's machine: 0 (nothing) .. 6 (it stands). */
    private static volatile int fase;
    /** Does today's perfect knabbel lie in the bowl? */
    private static volatile boolean knabbel;

    private TechquestClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(TechquestFeature.KNABBELMACHINE_BE.get(), KnabbelmachineRenderer::new));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.UITVINDERGUH, Guhs.id("entity/guh_npc_uitvinderguh"));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            fase = 0;
            knabbel = false;
        });
    }

    /** New state from the server. */
    public static void zet(CompoundTag data) {
        fase = data.getIntOr("Fase", 0);
        knabbel = data.getBooleanOr("Knabbel", false);
    }

    public static int fase() {
        return fase;
    }

    public static boolean knabbel() {
        return knabbel;
    }
}
