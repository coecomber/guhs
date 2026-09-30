package nl.juiced.guhs.feature.vissen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.juiced.guhs.feature.vissen.VissenFeature;
import nl.juiced.guhs.feature.vissen.VissenPayloads;

/** Client side of the vissen feature: the Visguh's screen and the "cast" look of the Guhvis-hengel. */
public final class VissenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(VissenClient::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        // like the vanilla rod: the line is out -> the "cast" model
        event.enqueueWork(() -> ItemProperties.register(VissenFeature.GUHVIS_HENGEL.get(), ResourceLocation.withDefaultNamespace("cast"),
                (stack, level, entity, seed) -> {
                    if (!(entity instanceof Player player) || player.fishing == null) {
                        return 0f;
                    }
                    boolean main = player.getMainHandItem() == stack;
                    boolean off = player.getOffhandItem() == stack && !(player.getMainHandItem().getItem() instanceof FishingRodItem);
                    return main || off ? 1f : 0f;
                }));
    }

    public static void open(VissenPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new VissenScreen(payload));
    }

    private VissenClient() {
    }
}
