package nl.juiced.guhs.feature.guhvillagers.client;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.guhvillagers.GuhVillagersFeature;

/** 1.7.0: the new guh villager look (the villager renderer, see GuhsVillagerRenderer) and the IJzerguhlem. */
public final class GuhVillagersClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions e) -> {
            e.registerLayerDefinition(GuhVillagerModellen.VOLWASSEN, GuhVillagerModellen::volwassen);
            e.registerLayerDefinition(GuhVillagerModellen.KIND, GuhVillagerModellen::kind);
            e.registerLayerDefinition(GuhVillagerModellen.HOEDJES, GuhVillagerModellen::hoedjes);
            e.registerLayerDefinition(GuhVillagerModellen.IJZERGUHLEM, GuhVillagerModellen::ijzerguhlem);
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(EntityType.VILLAGER, GuhsVillagerRenderer::new);
            e.registerEntityRenderer(GuhVillagersFeature.IJZERGUHLEM.get(), IJzerguhlemRenderer::new);
        });
    }

    private GuhVillagersClient() {
    }
}
