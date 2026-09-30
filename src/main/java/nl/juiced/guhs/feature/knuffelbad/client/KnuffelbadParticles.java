package nl.juiced.guhs.feature.knuffelbad.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;

/** The Knuffelbad's particles. */
final class KnuffelbadParticles {
    /** A soap bubble: floats up slowly, wobbling, shimmers, then pops. */
    static class Zeepbelletje extends TextureSheetParticle {
        private final float wob;

        Zeepbelletje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 30 + random.nextInt(50);
            quadSize = 0.06f + random.nextFloat() * 0.09f;
            gravity = -0.006f;
            xd = dx + (random.nextDouble() - 0.5) * 0.015;
            yd = dy + random.nextDouble() * 0.01;
            zd = dz + (random.nextDouble() - 0.5) * 0.015;
            hasPhysics = false;
            wob = random.nextFloat() * 6f;
            alpha = 0.85f;
        }

        @Override
        public void tick() {
            super.tick();
            xd += Math.sin((age + wob) * 0.3) * 0.002;
            zd += Math.cos((age + wob) * 0.27) * 0.002;
            xd *= 0.96;
            zd *= 0.96;
            if (age > lifetime - 4) {
                quadSize *= 1.15f;                     // (pop!)
                alpha *= 0.6f;
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A flake of pink foam: drifts about and slowly sinks. */
    static class Schuimvlokje extends TextureSheetParticle {
        Schuimvlokje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
            pickSprite(sprites);
            lifetime = 40 + random.nextInt(40);
            quadSize = 0.08f + random.nextFloat() * 0.1f;
            gravity = 0.02f;
            xd = dx + (random.nextDouble() - 0.5) * 0.03;
            yd = dy + random.nextDouble() * 0.03;
            zd = dz + (random.nextDouble() - 0.5) * 0.03;
            friction = 0.92f;
        }

        @Override
        public void tick() {
            super.tick();
            alpha = Math.min(1f, (lifetime - age) / 10f);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A sparkle: a little star that twinkles (grows and shrinks) and glows in the dark. */
    static class Glinstering extends TextureSheetParticle {
        private final float basis;

        Glinstering(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
            pickSprite(sprites);
            lifetime = 18 + random.nextInt(22);
            basis = 0.05f + random.nextFloat() * 0.07f;
            quadSize = basis;
            gravity = 0f;
            xd = dx;
            yd = dy;
            zd = dz;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            float t = (float) age / lifetime;
            quadSize = basis * (0.4f + Mth.sin(t * Mth.PI) * (0.8f + 0.2f * Mth.sin(age * 1.3f)));
        }

        @Override
        protected int getLightColor(float partialTick) {
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A splash drop: flies off and falls back (a PLONS is a fountain of them). */
    static class Plons extends TextureSheetParticle {
        Plons(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz);
            pickSprite(sprites);
            lifetime = 16 + random.nextInt(16);
            quadSize = 0.06f + random.nextFloat() * 0.08f;
            gravity = 0.7f;
            xd = dx;
            yd = dy;
            zd = dz;
            friction = 0.98f;
        }

        @Override
        public void tick() {
            super.tick();
            if (onGround) {
                remove();
            }
            alpha = Math.min(1f, (lifetime - age) / 6f);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private KnuffelbadParticles() {
    }
}
