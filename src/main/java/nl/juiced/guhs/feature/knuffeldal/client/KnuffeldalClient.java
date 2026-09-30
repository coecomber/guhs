package nl.juiced.guhs.feature.knuffeldal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalPayloads;
import nl.juiced.guhs.feature.knuffeldal.KruimelMikaEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

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
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BURGEMEESTERGUH, Guhs.id("entity/guh_npc_burgemeesterguh"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.COCOTJE, Guhs.id("entity/guh_npc_cocotje"));
        // Cocotje's ears hang and sway a little; the Burgemeester's chain glints (it swings with his breathing)
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.COCOTJE, (npc, tick) -> {
            float t = (float) tick * 0.06f;
            return bot -> {
                bot.ifPresent("ear_left", b -> b.setRotZ(b.getRotZ() + (float) Math.sin(t) * 0.08f));
                bot.ifPresent("ear_right", b -> b.setRotZ(b.getRotZ() - (float) Math.sin(t + 1.3f) * 0.08f));
            };
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.BURGEMEESTERGUH, (npc, tick) -> {
            float t = (float) tick * 0.05f;
            return bot -> bot.ifPresent("burgemeester_ketting", b -> b.setRotX((float) Math.sin(t) * 0.05f));   // (not animated: absolute)
        });
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KnuffeldalFeature.KRUIMEL_MIKA.get(), context -> new GeoEntityRenderer<KruimelMikaEntity, LivingEntityRenderState>(context,
                new DefaultedEntityGeoModel<KruimelMikaEntity>(Guhs.id("kruimel_mika")).withAltAnimations(Guhs.id("guh"))) {
            {
                this.shadowRadius = 0.3f;
            }

            /** Turns its head to where it looks (was DefaultedEntityGeoModel(id, true)). */
            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            }
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(KnuffeldalFeature.PLUISJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Pluisje(level, x, y, z, sprites));
        event.registerSpriteSet(KnuffeldalFeature.KRUIMEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Kruimel(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(KnuffeldalFeature.BLOESEMBLAADJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Blaadje(level, x, y, z, sprites));
        event.registerSpriteSet(KnuffeldalFeature.SNEEUWVLOKJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Vlokje(level, x, y, z, sprites));
    }

    /** guhs:knuffeldal_open: the talking screen (opened, updated or closed). */
    public static void open(KnuffeldalPayloads.Open payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.data().getBooleanOr("Sluit", false)) {
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
    static class Pluisje extends SingleQuadParticle {
        Pluisje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()));
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
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A cookie crumb: tiny, a little hop, then it lies still a while (the crumb trail). */
    static class Kruimel extends SingleQuadParticle {
        Kruimel(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites.get(level.getRandom()));
            lifetime = 30 + random.nextInt(20);
            quadSize = 0.045f + random.nextFloat() * 0.03f;
            gravity = 0.6f;
            xd = dx + (random.nextDouble() - 0.5) * 0.02;
            yd = dy + 0.04;
            zd = dz + (random.nextDouble() - 0.5) * 0.02;
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.OPAQUE;
        }
    }

    /** A blossom petal, falling and spinning like a cherry petal (1.1.0: vanilla's cherry settings of FallingLeavesParticle). */
    static class Blaadje extends FallingLeavesParticle {
        Blaadje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()), 0.25F, 2.0F, false, true, 1.0F, 0.0F);
        }
    }

    /** A snowflake: drifts down, wobbling. */
    static class Vlokje extends SingleQuadParticle {
        Vlokje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()));
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
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private KnuffeldalClient() {
    }
}
