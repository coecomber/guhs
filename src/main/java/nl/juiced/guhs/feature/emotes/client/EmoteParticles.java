package nl.juiced.guhs.feature.emotes.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.emotes.EmotesFeature;

/** The emote particles: a "z" drifting up from a sleeping guh, and a wide "VAHOEG!" popping up above a jumping one. */
public final class EmoteParticles {
    /** A z that drifts up and sideways, grows (small, medium, big z) and fades. */
    static class Zzz extends TextureSheetParticle {
        private final SpriteSet sprites;

        Zzz(ClientLevel level, double x, double y, double z, double dx, double dy, SpriteSet sprites) {
            super(level, x, y, z);
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
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** "VAHOEG!": pops up (a bit of a bounce in its size), floats up and fades; four times as wide as it is high. */
    static class Vahoeg extends TextureSheetParticle {
        Vahoeg(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            this.xd = 0;
            this.yd = 0.035;
            this.zd = 0;
            this.gravity = 0;
            this.hasPhysics = false;
            this.lifetime = 34;
            this.quadSize = 0.3f;
            pickSprite(sprites);
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

        @Override
        protected void renderRotatedQuad(VertexConsumer buffer, Quaternionf quaternion, float x, float y, float z, float partialTicks) {
            float size = getQuadSize(partialTicks);
            int light = getLightColor(partialTicks);
            vertex(buffer, quaternion, x, y, z, 4f, -1f, size, getU1(), getV1(), light);
            vertex(buffer, quaternion, x, y, z, 4f, 1f, size, getU1(), getV0(), light);
            vertex(buffer, quaternion, x, y, z, -4f, 1f, size, getU0(), getV0(), light);
            vertex(buffer, quaternion, x, y, z, -4f, -1f, size, getU0(), getV1(), light);
        }

        private void vertex(VertexConsumer buffer, Quaternionf q, float x, float y, float z, float dx, float dy, float size,
                            float u, float v, int light) {
            Vector3f p = new Vector3f(dx, dy, 0f).rotate(q).mul(size).add(x, y, z);
            buffer.addVertex(p.x(), p.y(), p.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
        }

        @Override
        public net.minecraft.world.phys.AABB getRenderBoundingBox(float partialTicks) {
            return super.getRenderBoundingBox(partialTicks).inflate(getQuadSize(partialTicks) * 4);
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0; // always bright
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(EmotesFeature.GUH_ZZZ.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) -> new Zzz(level, x, y, z, dx, dy, sprites));
        event.registerSpriteSet(EmotesFeature.GUH_VAHOEG.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) -> new Vahoeg(level, x, y, z, sprites));
    }

    private EmoteParticles() {
    }
}
