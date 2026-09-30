package nl.juiced.guhs.feature.creche.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.creche.CrecheBabyguh;
import nl.juiced.guhs.feature.creche.CrecheFeature;
import nl.juiced.guhs.feature.creche.CrechePayloads;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.base.BoneSnapshots;

/**
 * Client side of the Knuffelcreche: the babyguhtjes (a tiny guh with a pacifier, the guh's own animations), the sleepy
 * stars, Juf Knuffel's own model (nurse cap and apron) and her two screens: {@link CrecheScreen} and the lullaby
 * rhythm game {@link SlaapliedjeScreen}.
 */
public final class CrecheClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(CrecheClient::renderers);
        modBus.addListener(CrecheClient::particles);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.JUF_KNUFFEL, Guhs.id("entity/guh_npc_juf_knuffel"));
        // her cap's little heart bobs along with her breathing
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.JUF_KNUFFEL, (npc, tick) -> {
            float t = (float) tick * 0.07f;
            return bones -> bones.ifPresent("juf_hartje", b -> b.setTranslateY((float) Math.sin(t) * 0.25f));   // (not animated: absolute)
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CrecheFeature.BABYGUH.get(), context -> {
            // (1.1.0: the guh's own animations via withAltAnimations; the head follows the look like DefaultedEntityGeoModel(id, true) did)
            GeoEntityRenderer<CrecheBabyguh, LivingEntityRenderState> renderer = new GeoEntityRenderer<CrecheBabyguh, LivingEntityRenderState>(context,
                    new DefaultedEntityGeoModel<CrecheBabyguh>(Guhs.id("creche_babyguh")).withAltAnimations(Guhs.id("guh"))) {
                @Override
                public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                    DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
                }
            };
            renderer.withScale(0.42f);
            return renderer;
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CrecheFeature.SLAAPSTERRETJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Sterretje(level, x, y, z, sprites));
    }

    /** guhs:creche_open: Juf Knuffel's screen. */
    public static void open(CrechePayloads.Open payload) {
        Minecraft.getInstance().setScreen(new CrecheScreen(payload));
    }

    /** guhs:creche_liedje: sing a lullaby for a baby. */
    public static void liedje(CrechePayloads.Liedje payload) {
        Minecraft.getInstance().setScreen(new SlaapliedjeScreen(payload));
    }

    /** A little sleepy star: rises slowly, twinkles and fades. */
    static class Sterretje extends SingleQuadParticle {
        Sterretje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()));
            lifetime = 40 + random.nextInt(30);
            quadSize = 0.06f + random.nextFloat() * 0.05f;
            gravity = -0.004f;
            xd = (random.nextDouble() - 0.5) * 0.01;
            yd = 0.01 + random.nextDouble() * 0.01;
            zd = (random.nextDouble() - 0.5) * 0.01;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            float t = age / (float) lifetime;
            alpha = Math.min(1f, (1f - t) * 3f) * (0.75f + 0.25f * (float) Math.sin(age * 0.5));
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }
    }

    private CrecheClient() {
    }
}
