package nl.juiced.guhs.feature.bio.bouwmeer.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bio.bouwmeer.BouwMeerSlice;

/**
 * Client side of the biomes3 slice "bouw-meer": the roeibootje is drawn by vanilla's boat renderer with its own picture
 * (the layer {@code guhs:boat/roeibootje} makes the renderer read {@code guhs:textures/entity/boat/roeibootje.png}), and the
 * visser-guh has his own model (the sitting guh with a straw hat and a rod: tools/features/bio_bouw_meer.py). The hanami
 * guhs are the plain sitting guh in their own furs.
 */
public final class BouwMeerClient {
    public static final ModelLayerLocation ROEIBOOTJE_LAAG = new ModelLayerLocation(Guhs.id("boat/roeibootje"), "main");

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> event.registerLayerDefinition(ROEIBOOTJE_LAAG, BoatModel::createBoatModel));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(BouwMeerSlice.ROEIBOOTJE.get(), context -> new BoatRenderer(context, ROEIBOOTJE_LAAG)));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH, Guhs.id("entity/guh_npc_botenhuisje_visserguh"));
    }

    private BouwMeerClient() {
    }
}
