package nl.juiced.guhs.feature.knuffelbad.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/** The Knuffelbad's particles. */
final class KnuffelbadParticles {
    /** A soap bubble: floats up slowly, wobbling, shimmers, then pops. */
    static class Zeepbelletje extends SingleQuadParticle {
        private final float wob;

        Zeepbelletje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()));
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
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A flake of pink foam: drifts about and slowly sinks. */
    static class Schuimvlokje extends SingleQuadParticle {
        Schuimvlokje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites.get(level.getRandom()));
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
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A sparkle: a little star that twinkles (grows and shrinks) and glows in the dark. */
    static class Glinstering extends SingleQuadParticle {
        private final float basis;

        Glinstering(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites.get(level.getRandom()));
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
        protected int getLightCoords(float partialTick) {
            return LightCoordsUtil.FULL_BRIGHT;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A splash drop: flies off and falls back (a PLONS is a fountain of them). */
    static class Plons extends SingleQuadParticle {
        Plons(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites.get(level.getRandom()));
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
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private KnuffelbadParticles() {
    }
}
