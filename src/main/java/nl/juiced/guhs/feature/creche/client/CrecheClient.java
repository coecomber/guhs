package nl.juiced.guhs.feature.creche.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.creche.CrecheBabyguh;
import nl.juiced.guhs.feature.creche.CrecheFeature;
import nl.juiced.guhs.feature.creche.CrechePayloads;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Client side of the Knuffelcreche: the babyguhtjes (a tiny guh with a pacifier, the guh's own animations), the sleepy
 * stars, Juf Knuffel's own model (nurse cap and apron) and her two screens: {@link CrecheScreen} and the lullaby
 * rhythm game {@link SlaapliedjeScreen}.
 */
public final class CrecheClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(CrecheClient::renderers);
        modBus.addListener(CrecheClient::particles);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.JUF_KNUFFEL, Guhs.id("geo/entity/guh_npc_juf_knuffel.geo.json"));
        // her cap's little heart bobs along with her breathing
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.JUF_KNUFFEL, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.07f;
            bot.apply("juf_hartje").ifPresent(b -> b.setPosY((float) Math.sin(t) * 0.25f));   // (not animated: absolute)
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CrecheFeature.BABYGUH.get(), context -> {
            GeoEntityRenderer<CrecheBabyguh> renderer = new GeoEntityRenderer<>(context, new DefaultedEntityGeoModel<CrecheBabyguh>(Guhs.id("creche_babyguh"), true) {
                @Override
                public ResourceLocation getAnimationResource(CrecheBabyguh baby) {
                    return Guhs.id("animations/entity/guh.animation.json");
                }
            });
            renderer.withScale(0.42f);
            return renderer;
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(CrecheFeature.SLAAPSTERRETJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Sterretje(level, x, y, z, sprites));
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
    static class Sterretje extends TextureSheetParticle {
        Sterretje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
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
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }
    }

    private CrecheClient() {
    }
}
