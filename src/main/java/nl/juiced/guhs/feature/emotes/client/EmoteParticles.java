package nl.juiced.guhs.feature.emotes.client;

import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.ARGB;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.emotes.EmotesFeature;

/** The emote particles: a "z" drifting up from a sleeping guh, and a wide "VAHOEG!" popping up above a jumping one. */
public final class EmoteParticles {
    /** A z that drifts up and sideways, grows (small, medium, big z) and fades. */
    static class Zzz extends SingleQuadParticle {
        private final SpriteSet sprites;

        Zzz(ClientLevel level, double x, double y, double z, double dx, double dy, SpriteSet sprites) {
            super(level, x, y, z, sprites.first());
            this.sprites = sprites;
            this.xd = dx;
            this.yd = dy;
            this.zd = 0;
            this.gravity = 0;
            this.hasPhysics = false;
            this.lifetime = 50;
            this.quadSize = 0.18f;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!this.removed) {
                setSpriteFromAge(sprites);
                this.xd = Math.sin(this.age * 0.2) * 0.012;
                this.alpha = 1f - Mth.clamp((this.age - 30) / 20f, 0f, 1f);
                this.quadSize = 0.18f + this.age * 0.003f;
            }
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** "VAHOEG!": pops up (a bit of a bounce in its size), floats up and fades; four times as wide as it is high. */
    static class Vahoeg extends SingleQuadParticle {
        Vahoeg(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()));
            this.xd = 0;
            this.yd = 0.035;
            this.zd = 0;
            this.gravity = 0;
            this.hasPhysics = false;
            this.lifetime = 34;
            this.quadSize = 0.3f;
        }

        @Override
        public float getQuadSize(float partialTick) {
            float t = (this.age + partialTick) / 6f;
            float pop = t < 1 ? t * 1.2f : 1f + 0.2f * Math.max(0f, 1f - (t - 1f) * 2f);
            return this.quadSize * Mth.clamp(pop, 0f, 1.2f);
        }

        @Override
        public void tick() {
            super.tick();
            this.alpha = 1f - Mth.clamp((this.age - 22) / 12f, 0f, 1f);
        }

        /**
         * 1.1.0 (MC 26.1): particle quads are square in the new particle render state, so the 4:1 "VAHOEG!" is extracted as four
         * squares side by side, each with its quarter of the texture (same corners and uv as the old single wide quad).
         */
        @Override
        protected void extractRotatedQuad(QuadParticleRenderState state, Quaternionf quaternion, float x, float y, float z, float partialTicks) {
            float size = getQuadSize(partialTicks);
            int light = getLightCoords(partialTicks);
            int colour = ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol);
            float u0 = getU0();
            float du = (getU1() - u0) / 4f;
            for (int i = 0; i < 4; i++) {
                float c = -3f + 2f * i; // centre of this square in the old quad (-4..4)
                Vector3f p = new Vector3f(c, 0f, 0f).rotate(quaternion).mul(size).add(x, y, z);
                state.add(getLayer(), p.x(), p.y(), p.z(), quaternion.x, quaternion.y, quaternion.z, quaternion.w, size,
                        u0 + du * i, u0 + du * (i + 1), getV0(), getV1(), colour, light);
            }
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0; // always bright
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(EmotesFeature.GUH_ZZZ.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Zzz(level, x, y, z, dx, dy, sprites));
        event.registerSpriteSet(EmotesFeature.GUH_VAHOEG.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Vahoeg(level, x, y, z, sprites));
    }

    private EmoteParticles() {
    }
}
