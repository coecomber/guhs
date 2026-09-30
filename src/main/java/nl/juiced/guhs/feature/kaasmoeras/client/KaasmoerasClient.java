package nl.juiced.guhs.feature.kaasmoeras.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.IEventBus;
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
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import javax.annotation.Nullable;

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
            event.registerEntityRenderer(KaasmoerasFeature.KAASMOT.get(), context -> new GeoEntityRenderer<KaasmotEntity, LivingEntityRenderState>(context,
                    new DefaultedEntityGeoModel<KaasmotEntity>(Guhs.id("kaasmot"))));
            event.registerEntityRenderer(KaasmoerasFeature.MOERASHEKS_MIKA.get(), context -> {
                var renderer = new GeoEntityRenderer<MoerasheksMikaEntity, LivingEntityRenderState>(context,
                        new DefaultedEntityGeoModel<MoerasheksMikaEntity>(Guhs.id("moerasheks_mika"))) {
                    /** The "head" bone follows where she looks (GeckoLib 4: DefaultedEntityGeoModel(id, true)). */
                    @Override
                    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
                    }
                };
                renderer.withScale(1.1f);
                return renderer;
            });
            event.registerEntityRenderer(KaasmoerasFeature.DRANKJE.get(), ThrownItemRenderer::new);
        });
        // (1.1.0: the motknabbel item's colour is the client item definition's minecraft:block_state select on "kleur")
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tickMist());
        NeoForge.EVENT_BUS.addListener(KaasmoerasClient::onFog);
    }

    private static void tickMist() {
        Minecraft mc = Minecraft.getInstance();
        lastMist = mist;
        boolean in = mc.level != null && mc.player != null && KaasmoerasEvents.inKaasmoeras(mc.level, mc.player.blockPosition());
        mist = Mth.clamp(mist + (in ? MIST_SPEED : -MIST_SPEED), 0f, 1f);
    }

    /** (1.1.0: one fog event now; the environmental fog is the old terrain fog. The old cylinder shape is gone.) */
    private static void onFog(ViewportEvent.RenderFog event) {
        if (event.getType() != FogType.ATMOSPHERIC) { // 1.1.0: air is ATMOSPHERIC in 26.1 (NONE never comes)
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
    }

    /** The kikkerguh: one model, a texture per colour, babies at half size. */
    public static class KikkerguhRenderer extends GeoEntityRenderer<KikkerguhEntity, LivingEntityRenderState> {
        private static final DataTicket<String> KLEUR = DataTicket.create("guhs_kikkerguh_kleur", String.class);

        public KikkerguhRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<KikkerguhEntity>(Guhs.id("kikkerguh")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Guhs.id("textures/entity/kikkerguh_" + state.getOrDefaultGeckolibData(KLEUR, "roze") + ".png");
                }
            });
            this.shadowRadius = 0.35f;
        }

        @Override
        public void addRenderData(KikkerguhEntity kikker, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(KLEUR, kikker.getKleur().getSerializedName());
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float scale = info.renderState().isBaby ? 0.55f : 1f;
            super.scaleModelForRender(info, widthScale * scale, heightScale * scale);
        }
    }

    private KaasmoerasClient() {
    }
}
