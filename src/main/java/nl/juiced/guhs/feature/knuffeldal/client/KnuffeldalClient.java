package nl.juiced.guhs.feature.knuffeldal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CherryParticle;
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
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalPayloads;
import nl.juiced.guhs.feature.knuffeldal.KruimelMikaEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Client side of the Knuffeldal: the Kruimel-Mika (the Mika's model with its own texture), the particles (pluisje,
 * kruimel, bloesemblaadje, sneeuwvlokje), the own models of Burgemeester Vadsema (hat and chain of office) and
 * Cocotje (her long hanging ears), and the talking screen.
 */
public final class KnuffeldalClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(KnuffeldalClient::renderers);
        modBus.addListener(KnuffeldalClient::particles);
        // the characters' own models (tools/features/knuffeldal_npcs.py); animations: the sitting guh's
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BURGEMEESTERGUH, Guhs.id("geo/entity/guh_npc_burgemeesterguh.geo.json"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.COCOTJE, Guhs.id("geo/entity/guh_npc_cocotje.geo.json"));
        // Cocotje's ears hang and sway a little; the Burgemeester's chain glints (it swings with his breathing)
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.COCOTJE, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.06f;
            bot.apply("ear_left").ifPresent(b -> b.setRotZ(b.getRotZ() + (float) Math.sin(t) * 0.08f));
            bot.apply("ear_right").ifPresent(b -> b.setRotZ(b.getRotZ() - (float) Math.sin(t + 1.3f) * 0.08f));
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.BURGEMEESTERGUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.05f;
            bot.apply("burgemeester_ketting").ifPresent(b -> b.setRotX((float) Math.sin(t) * 0.05f));   // (not animated: absolute)
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KnuffeldalFeature.KRUIMEL_MIKA.get(), context -> new GeoEntityRenderer<KruimelMikaEntity>(context,
                new DefaultedEntityGeoModel<KruimelMikaEntity>(Guhs.id("kruimel_mika"), true) {
                    @Override
                    public ResourceLocation getAnimationResource(KruimelMikaEntity mika) {
                        return Guhs.id("animations/entity/guh.animation.json");
                    }
                }) {
            {
                this.shadowRadius = 0.3f;
            }
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(KnuffeldalFeature.PLUISJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Pluisje(level, x, y, z, sprites));
        event.registerSpriteSet(KnuffeldalFeature.KRUIMEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Kruimel(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(KnuffeldalFeature.BLOESEMBLAADJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Blaadje(level, x, y, z, sprites));
        event.registerSpriteSet(KnuffeldalFeature.SNEEUWVLOKJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Vlokje(level, x, y, z, sprites));
    }

    /** guhs:knuffeldal_open: the talking screen (opened, updated or closed). */
    public static void open(KnuffeldalPayloads.Open payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.data().getBoolean("Sluit")) {
            if (mc.screen instanceof PraatScherm) {
                mc.setScreen(null);
            }
            return;
        }
        if (mc.screen instanceof PraatScherm scherm && (scherm.npcId() == payload.npcId() || payload.data().contains("Sleutel"))) {
            scherm.update(payload.npcId(), payload.data());
        } else {
            mc.setScreen(new PraatScherm(payload.npcId(), payload.data()));
        }
    }

    /** A soft pink fluff: drifts slowly, fades in and out. */
    static class Pluisje extends TextureSheetParticle {
        Pluisje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 80 + random.nextInt(80);
            quadSize = 0.05f + random.nextFloat() * 0.05f;
            gravity = -0.001f;
            xd = (random.nextDouble() - 0.5) * 0.02;
            yd = random.nextDouble() * 0.008;
            zd = (random.nextDouble() - 0.5) * 0.02;
            hasPhysics = false;
            alpha = 0f;
        }

        @Override
        public void tick() {
            super.tick();
            float t = age / (float) lifetime;
            alpha = Math.min(1f, Math.min(t * 5f, (1f - t) * 4f)) * 0.95f;
            xd += Math.sin(age * 0.06) * 0.0005;
            zd += Math.cos(age * 0.05) * 0.0005;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A cookie crumb: tiny, a little hop, then it lies still a while (the crumb trail). */
    static class Kruimel extends TextureSheetParticle {
        Kruimel(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
            pickSprite(sprites);
            lifetime = 30 + random.nextInt(20);
            quadSize = 0.045f + random.nextFloat() * 0.03f;
            gravity = 0.6f;
            xd = dx + (random.nextDouble() - 0.5) * 0.02;
            yd = dy + 0.04;
            zd = dz + (random.nextDouble() - 0.5) * 0.02;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    /** A blossom petal, falling and spinning like a cherry petal. */
    static class Blaadje extends CherryParticle {
        Blaadje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites);
        }
    }

    /** A snowflake: drifts down, wobbling. */
    static class Vlokje extends TextureSheetParticle {
        Vlokje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 60 + random.nextInt(60);
            quadSize = 0.06f + random.nextFloat() * 0.04f;
            gravity = 0.02f;
            xd = (random.nextDouble() - 0.5) * 0.01;
            yd = -0.02;
            zd = (random.nextDouble() - 0.5) * 0.01;
        }

        @Override
        public void tick() {
            super.tick();
            xd += Math.sin(age * 0.1) * 0.001;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private KnuffeldalClient() {
    }
}
