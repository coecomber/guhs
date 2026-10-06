package nl.juiced.guhs.feature.fossielmijn.client;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.fossielmijn.FossielmijnFeature;

/**
 * Client side of bbq2 (fossiel-mijn): what the local player did at the Fossiel-opgraving (sent by the server:
 * FossielmijnPayloads.Stand), the renderer that draws the stand and the bottenzand accordingly, and the models of the
 * Archeoloog-guh (pith helmet, neckerchief, moustache) and the Mijnwerker-guh (helmet with a lamp, belt).
 */
public final class FossielmijnClient {
    /** The mask of the bones the local player put on the stand. */
    private static volatile int rek;
    /** The spots of bottenzand the local player brushed empty. */
    private static volatile LongSet gekwast = new LongOpenHashSet();

    private FossielmijnClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(FossielmijnFeature.TEKENING.get(), TekenRenderer::new));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.ARCHEOLOOGGUH, Guhs.id("entity/guh_npc_archeoloogguh"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.MIJNWERKERGUH, Guhs.id("entity/guh_npc_mijnwerkerguh"));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> wis());
    }

    /** New state from the server. */
    public static void zet(CompoundTag data) {
        rek = data.getIntOr("Rek", 0);
        gekwast = new LongOpenHashSet(data.getLongArray("Gekwast").orElse(new long[0]));
    }

    private static void wis() {
        rek = 0;
        gekwast = new LongOpenHashSet();
    }

    public static int rek() {
        return rek;
    }

    public static boolean isGekwast(BlockPos pos) {
        return gekwast.contains(pos.asLong());
    }
}
