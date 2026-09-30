package nl.juiced.guhs.feature.balto.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.balto.BaltoFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import org.joml.Quaternionf;
import nl.juiced.guhs.client.GuhRenderer;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.particle.SingleQuadParticle;

/**
 * Client side of the balto slice: the characters' own models (tools/features/balto_modellen.py): Boris the goose (a whole
 * goose model with its own animations), Steele-Mika (the Mika with a sled-leader cap, goggles and a medal), Muk and Luk (two
 * big white polar-bear guhs with scarves), Rosy (a baby guh in her little bed with a scarf and a hot-water bottle) and the
 * white wolf-guh (glowing white, pointy ears, a bushy tail; she floats a little); the Baltoguh's sniffing pose; the particles
 * (the scent trail's paw prints lie flat on the snow and glow; the white sparkle) and the snow storm ({@link Sneeuwstorm}).
 */
public final class BaltoClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(BaltoClient::particles);
        Sneeuwstorm.init(modBus);
        for (GuhNpcEntity.Kind kind : new GuhNpcEntity.Kind[]{GuhNpcEntity.Kind.BORIS, GuhNpcEntity.Kind.STEELE_MIKA, GuhNpcEntity.Kind.MUK,
                GuhNpcEntity.Kind.LUK, GuhNpcEntity.Kind.ROSY, GuhNpcEntity.Kind.WITTE_WOLFGUH}) {
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("entity/guh_npc_" + kind.id()));
        }
        SittingGuhRenderers.NPC_ANIMATIES.put(GuhNpcEntity.Kind.BORIS, Guhs.id("entity/guh_npc_boris"));
        SittingGuhRenderers.NPC_ANIMATIES.put(GuhNpcEntity.Kind.STEELE_MIKA, Guhs.id("entity/guh_npc_steele_mika"));
        // the white wolf-guh floats a little and her tail sways; Rosy snuggles in her bed; Muk and Luk sway like big bears
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.WITTE_WOLFGUH, (npc, tick) -> {
            float t = (float) tick * 0.05f;
            return bones -> {
                bones.ifPresent("root", b -> b.setTranslateY(2.5f + Mth.sin(t) * 1.2f));
                bones.ifPresent("wolf_staart", b -> b.setRotY(Mth.sin(t * 1.7f) * 0.35f));
            };
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.ROSY, (npc, tick) -> {
            float t = (float) tick * 0.04f;
            return bones -> bones.ifPresent("head", b -> b.setRotZ(b.getRotZ() + Mth.sin(t) * 0.06f));
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.MUK, (npc, tick) -> {
            float t = (float) tick * 0.035f;
            return bones -> bones.ifPresent("body", b -> b.setRotZ(Mth.sin(t) * 0.05f));   // (absolute: the idle only scales the body, so a += drifted until they lay flat)
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.LUK, (npc, tick) -> {
            float t = (float) tick * 0.035f + 1.6f;
            return bones -> bones.ifPresent("body", b -> b.setRotZ(Mth.sin(t) * 0.05f));   // (absolute: the idle only scales the body, so a += drifted until they lay flat)
        });
        // the Baltoguh: nose down and a little sniff-sniff while he smells the way home
        // (1.1.0: its own GuhRenderer hook; the values are computed at extract time, the bones move at render time)
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (frame.variant != GuhVariant.BALTOGUH || !GuhHooks.heeft(guh, VerhaalVlaggen.SNUFFELT)) {
                return;
            }
            float t = (guh.tickCount + partialTick) * 0.9f;
            frame.bones(bones -> {
                bones.ifPresent("head", b -> {
                    b.setRotX(b.getRotX() - 0.45f + Mth.sin(t) * 0.05f);
                    b.setRotY(b.getRotY() + Mth.sin(t * 0.23f) * 0.25f);
                });
                bones.ifPresent("tail", b -> b.setRotY(b.getRotY() + Mth.sin(t * 0.8f) * 0.4f));
            });
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BaltoFeature.SNUFFEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Pootje(level, x, y, z, sprites.get(random)));
        event.registerSpriteSet(BaltoFeature.WOLFGLANS.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Glans(level, x, y, z, dx, dy, dz, sprites));
    }

    /** A glowing paw print lying flat on the snow: fades in, glows a moment, fades out. */
    static class Pootje extends SingleQuadParticle {
        private final float draai;

        Pootje(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
            lifetime = 26;
            quadSize = 0.16f;
            gravity = 0f;
            hasPhysics = false;
            alpha = 0f;
            draai = random.nextFloat() * 0.5f - 0.25f;
        }

        @Override
        public void tick() {
            super.tick();
            float t = (float) age / lifetime;
            alpha = t < 0.2f ? t / 0.2f : 1f - (t - 0.2f) / 0.8f;
        }

        @Override
        public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
            // flat on the ground (both sides), turned a little
            Quaternionf q = new Quaternionf().rotationY(draai).rotateX((float) Math.PI / 2f);
            extractRotatedQuad(state, camera, q, partialTicks);
            extractRotatedQuad(state, camera, new Quaternionf().rotationY(draai).rotateX(-(float) Math.PI / 2f), partialTicks);
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A soft white-blue sparkle that drifts up and twinkles (the white wolf-guh, the route lamps). */
    static class Glans extends SingleQuadParticle {
        private final SpriteSet sprites;
        private final float groot;

        Glans(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, sprites.first());
            this.sprites = sprites;
            setSpriteFromAge(sprites);
            lifetime = 18 + random.nextInt(18);
            groot = 0.06f + random.nextFloat() * 0.07f;
            quadSize = groot;
            gravity = -0.01f;
            xd = dx + (random.nextDouble() - 0.5) * 0.02;
            yd = dy + 0.01;
            zd = dz + (random.nextDouble() - 0.5) * 0.02;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            float t = (float) age / lifetime;
            quadSize = groot * (0.6f + 0.4f * Mth.abs(Mth.sin(age * 0.6f)));
            alpha = 1f - t * t;
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private BaltoClient() {
    }
}
