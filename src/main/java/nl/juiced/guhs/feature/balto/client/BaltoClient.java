package nl.juiced.guhs.feature.balto.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.balto.BaltoFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.feature.verhaal.client.VariantUiterlijk;
import org.joml.Quaternionf;

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
            SittingGuhRenderers.NPC_MODELEN.put(kind, Guhs.id("geo/entity/guh_npc_" + kind.id() + ".geo.json"));
        }
        SittingGuhRenderers.NPC_ANIMATIES.put(GuhNpcEntity.Kind.BORIS, Guhs.id("animations/entity/guh_npc_boris.animation.json"));
        SittingGuhRenderers.NPC_ANIMATIES.put(GuhNpcEntity.Kind.STEELE_MIKA, Guhs.id("animations/entity/guh_npc_steele_mika.animation.json"));
        // the white wolf-guh floats a little and her tail sways; Rosy snuggles in her bed; Muk and Luk sway like big bears
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.WITTE_WOLFGUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.05f;
            bot.apply("root").ifPresent(b -> b.setPosY(2.5f + Mth.sin(t) * 1.2f));
            bot.apply("wolf_staart").ifPresent(b -> b.setRotY(Mth.sin(t * 1.7f) * 0.35f));
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.ROSY, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.04f;
            bot.apply("head").ifPresent(b -> b.setRotZ(b.getRotZ() + Mth.sin(t) * 0.06f));
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.MUK, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.035f;
            bot.apply("body").ifPresent(b -> b.setRotZ(Mth.sin(t) * 0.05f));   // (absolute: the idle only scales the body, so a += drifted until they lay flat)
        });
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.LUK, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.035f + 1.6f;
            bot.apply("body").ifPresent(b -> b.setRotZ(Mth.sin(t) * 0.05f));   // (absolute: the idle only scales the body, so a += drifted until they lay flat)
        });
        // the Baltoguh: nose down and a little sniff-sniff while he smells the way home
        VariantUiterlijk.zet(GuhVariant.BALTOGUH, new VariantUiterlijk.Uiterlijk() {
            @Override
            public void botten(GuhEntity guh, java.util.function.Function<String, java.util.Optional<com.geckolib.cache.model.GeoBone>> bot,
                               float partialTick) {
                if (GuhHooks.heeft(guh, VerhaalVlaggen.SNUFFELT)) {
                    float t = (guh.tickCount + partialTick) * 0.9f;
                    bot.apply("head").ifPresent(b -> {
                        b.setRotX(b.getRotX() - 0.45f + Mth.sin(t) * 0.05f);
                        b.setRotY(b.getRotY() + Mth.sin(t * 0.23f) * 0.25f);
                    });
                    bot.apply("tail").ifPresent(b -> b.setRotY(b.getRotY() + Mth.sin(t * 0.8f) * 0.4f));
                }
            }
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BaltoFeature.SNUFFEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Pootje(level, x, y, z, sprites));
        event.registerSpriteSet(BaltoFeature.WOLFGLANS.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Glans(level, x, y, z, dx, dy, dz, sprites));
    }

    /** A glowing paw print lying flat on the snow: fades in, glows a moment, fades out. */
    static class Pootje extends TextureSheetParticle {
        private final float draai;

        Pootje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
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
        public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
            // flat on the ground (both sides), turned a little
            Quaternionf q = new Quaternionf().rotationY(draai).rotateX((float) Math.PI / 2f);
            renderRotatedQuad(buffer, camera, q, partialTicks);
            renderRotatedQuad(buffer, camera, new Quaternionf().rotationY(draai).rotateX(-(float) Math.PI / 2f), partialTicks);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A soft white-blue sparkle that drifts up and twinkles (the white wolf-guh, the route lamps). */
    static class Glans extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final float groot;

        Glans(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
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
        protected int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private BaltoClient() {
    }
}
