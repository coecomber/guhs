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
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.DrinkKaasSausPayload;
import nl.juiced.guhs.registry.ModFluids;
import org.joml.Vector3f;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/** Kaas saus on the client: how it looks (thick like lava, but cheese: guhs:block/kaas_saus_still/flow) and "drinking" it. */
@EventBusSubscriber(modid = Guhs.MODID, value = Dist.CLIENT)
public final class KaasSausClient {
    public static void registerFluidLooks(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public Identifier getStillTexture() {
                return Guhs.id("block/kaas_saus_still");
            }

            @Override
            public Identifier getFlowingTexture() {
                return Guhs.id("block/kaas_saus_flow");
            }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF; // (the textures are cheese-coloured already)
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                           float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(0.95f, 0.65f, 0.1f);
            }
        }, ModFluids.KAAS_SAUS_TYPE.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public Identifier getStillTexture() {
                return Identifier.withDefaultNamespace("block/water_still");
            }

            @Override
            public Identifier getFlowingTexture() {
                return Identifier.withDefaultNamespace("block/water_flow");
            }

            @Override
            public Identifier getOverlayTexture() {
                return Identifier.withDefaultNamespace("block/water_overlay");
            }

            @Override
            public int getTintColor() {
                return 0xFFB8E03A;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                           float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(0.6f, 0.8f, 0.15f);
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
