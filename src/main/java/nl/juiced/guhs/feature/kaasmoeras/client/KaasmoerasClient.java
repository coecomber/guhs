package nl.juiced.guhs.feature.kaasmoeras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasEvents;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature;
import nl.juiced.guhs.feature.kaasmoeras.KaasmotEntity;
import nl.juiced.guhs.feature.kaasmoeras.KikkerguhEntity;
import nl.juiced.guhs.feature.kaasmoeras.MoerasheksMikaEntity;
import nl.juiced.guhs.feature.kaasmoeras.MotknabbelBlock;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * Client side of the kaasmoeras: the GeckoLib renderers of the kikkerguh (a texture per colour, babies half size), the
 * kaasmot and the Moerasheks-Mika, the thrown drankje, the motknabbel item colours, and the swamp mist: in the
 * kaasmoeras the fog slowly closes in (and lifts again when you leave).
 */
public final class KaasmoerasClient {
    /**
     * Fog distance in the thick of the kaasmoeras (blocks), and how fast the mist comes and goes (per tick). Clear up to
     * MIST_NEAR, a cheesy haze after that: you still see ~60-80 blocks, also from high up.
     */
    public static final float MIST_FAR = 104f, MIST_NEAR = 30f, MIST_SPEED = 0.02f;
    private static float mist, lastMist;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(KaasmoerasFeature.KIKKERGUH.get(), KikkerguhRenderer::new);
            event.registerEntityRenderer(KaasmoerasFeature.KAASMOT.get(), context -> new GeoEntityRenderer<>(context,
                    new DefaultedEntityGeoModel<KaasmotEntity>(Guhs.id("kaasmot"))));
            event.registerEntityRenderer(KaasmoerasFeature.MOERASHEKS_MIKA.get(), context -> {
                var renderer = new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<MoerasheksMikaEntity>(Guhs.id("moerasheks_mika"), true));
                renderer.withScale(1.1f);
                return renderer;
            });
            event.registerEntityRenderer(KaasmoerasFeature.DRANKJE.get(), ThrownItemRenderer::new);
        });
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> ItemProperties.register(KaasmoerasFeature.MOTKNABBEL_ITEM.get(),
                Guhs.id("kleur"), (stack, level, entity, seed) -> MotknabbelBlock.kleur(stack).ordinal() / 2f)));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tickMist());
        NeoForge.EVENT_BUS.addListener(KaasmoerasClient::onFog);
    }

    private static void tickMist() {
        Minecraft mc = Minecraft.getInstance();
        lastMist = mist;
        boolean in = mc.level != null && mc.player != null && KaasmoerasEvents.inKaasmoeras(mc.level, mc.player.blockPosition());
        mist = Mth.clamp(mist + (in ? MIST_SPEED : -MIST_SPEED), 0f, 1f);
    }

    private static void onFog(ViewportEvent.RenderFog event) {
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || event.getType() != FogType.NONE) {
            return;
        }
        float m = Mth.lerp((float) event.getPartialTick(), lastMist, mist);
        if (m <= 0.001f) {
            return;
        }
        float far = event.getFarPlaneDistance();
        if (far <= MIST_FAR) {
            return;
        }
        m = m * m * (3 - 2 * m);   // smoothstep
        event.setFarPlaneDistance(Mth.lerp(m, far, MIST_FAR));
        event.setNearPlaneDistance(Mth.lerp(m, event.getNearPlaneDistance(), MIST_NEAR));
        event.setFogShape(com.mojang.blaze3d.shaders.FogShape.CYLINDER);   // only the horizontal distance counts
        event.setCanceled(true);
    }

    /** The kikkerguh: one model, a texture per colour, babies at half size. */
    public static class KikkerguhRenderer extends GeoEntityRenderer<KikkerguhEntity> {
        public KikkerguhRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<KikkerguhEntity>(Guhs.id("kikkerguh"), true) {
                @Override
                public Identifier getTextureResource(KikkerguhEntity kikker) {
                    return Guhs.id("textures/entity/kikkerguh_" + kikker.getKleur().getSerializedName() + ".png");
                }
            });
            this.shadowRadius = 0.35f;
        }

        @Override
        public void render(KikkerguhEntity kikker, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            float scale = kikker.isBaby() ? 0.55f : 1f;
            this.scaleWidth = scale;
            this.scaleHeight = scale;
            super.render(kikker, yaw, partialTick, pose, buffers, light);
        }
    }

    private KaasmoerasClient() {
    }
}
