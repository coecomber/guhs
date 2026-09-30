package nl.juiced.guhs.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.DrinkKaasSausPayload;
import nl.juiced.guhs.registry.ModFluids;
import org.joml.Vector4f;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/** Kaas saus on the client: how it looks (thick like lava, but cheese: guhs:block/kaas_saus_still/flow) and "drinking" it. */
@EventBusSubscriber(modid = Guhs.MODID, value = Dist.CLIENT)
public final class KaasSausClient {
    /**
     * 1.1.0 (MC 26.1): fluid textures and tint are a {@link FluidModel} now (RegisterFluidModelsEvent); the client fluid
     * type extension only keeps the fog colour.
     */
    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        // (the textures are cheese-coloured already: no tint)
        event.register(new FluidModel.Unbaked(new Material(Guhs.id("block/kaas_saus_still")), new Material(Guhs.id("block/kaas_saus_flow")),
                null, null), ModFluids.KAAS_SAUS, ModFluids.FLOWING_KAAS_SAUS);
        event.register(new FluidModel.Unbaked(new Material(Identifier.withDefaultNamespace("block/water_still")),
                new Material(Identifier.withDefaultNamespace("block/water_flow")), new Material(Identifier.withDefaultNamespace("block/water_overlay")),
                FluidTintSources.constant(0xFFB8E03A)), ModFluids.MAAGZUUR, ModFluids.FLOWING_MAAGZUUR);
    }

    public static void registerFluidLooks(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                       float darkenWorldAmount, Vector4f fluidFogColor) {
                fluidFogColor.set(0.95f, 0.65f, 0.1f, fluidFogColor.w);
            }
        }, ModFluids.KAAS_SAUS_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                       float darkenWorldAmount, Vector4f fluidFogColor) {
                fluidFogColor.set(0.6f, 0.8f, 0.15f, fluidFogColor.w);
            }
        }, ModFluids.MAAGZUUR_TYPE.get());
    }

    /** Empty hand + right-click on kaas saus (you normally can't click fluids) -> ask the server for a sip. */
    @SubscribeEvent
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isUseItem() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || mc.player == null
                || !mc.player.getMainHandItem().isEmpty() || mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult) {
            return;
        }
        if (DrinkKaasSausPayload.lookedAtSaus(mc.player) != null) {
            ClientPacketDistributor.sendToServer(DrinkKaasSausPayload.INSTANCE);
            event.setSwingHand(true);
            event.setCanceled(true);
        }
    }

    private KaasSausClient() {
    }
}
