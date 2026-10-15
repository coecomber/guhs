package nl.juiced.guhs.feature.bio.blokkenwolk.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.bio.blokkenwolk.BlokkenWolkSlice;

/** The particles of this slice: the wolkenstroom's puffs and arrows, the foam and the mist of a big waterfall. */
final class Deeltjes {
    /** The pictures of wolkenstroom_pluis, in the order of its particle file: puffs 0..2, then the two arrows. */
    private static final int PIJL_OP = 3, PIJL_NEER = 4, BEELDEN = 5;

    static void registreer(RegisterParticleProvidersEvent event) {
        // five pictures (particles/wolkenstroom_pluis.json): three puffs, the arrow up, the arrow down. One in three is an arrow.
        event.registerSpriteSet(BlokkenWolkSlice.WOLKENSTROOM_PLUIS.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
                random.nextInt(3) == 0 ? new Pijl(level, x, y, z, dy, sprites.get(dy >= 0 ? PIJL_OP : PIJL_NEER, BEELDEN - 1))
                        : new Pluis(level, x, y, z, dy, sprites.get(random.nextInt(PIJL_OP), BEELDEN - 1)));
        event.registerSpriteSet(BlokkenWolkSlice.WATERVAL_SCHUIM.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Schuim(level, x, y, z, dx, dy, dz, sprites.get(random)));
        event.registerSpriteSet(BlokkenWolkSlice.WATERVAL_NEVEL.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Nevel(level, x, y, z, dx, dy, dz, sprites.get(random)));
    }

    /** A puff in the stream: straight up or down at the speed it was given, white going up, pink going down. */
    static final class Pluis extends SingleQuadParticle {
        private final float basis;

        Pluis(ClientLevel level, double x, double y, double z, double dy, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0, 0, 0, sprite);
            boolean omlaag = dy < 0;
            lifetime = omlaag ? 30 + random.nextInt(14) : 16 + random.nextInt(10);
            basis = 0.14f + random.nextFloat() * 0.12f;
            quadSize = basis;
            gravity = 0;
            friction = 1.0f;
            hasPhysics = false;
            xd = (random.nextDouble() - 0.5) * 0.01;
            yd = dy;
            zd = (random.nextDouble() - 0.5) * 0.01;
            if (omlaag) {
                setColor(1.0f, 0.80f + random.nextFloat() * 0.08f, 0.92f);
            } else {
                float w = 0.92f + random.nextFloat() * 0.08f;
                setColor(w, w, 1.0f);
            }
            alpha = 0;
        }

        @Override
        public void tick() {
            super.tick();
            float f = age / (float) lifetime;
            quadSize = basis * (0.9f + 0.3f * f);
            alpha = Math.min(1f, Math.min(f * 5f, (1f - f) * 2.5f)) * 0.9f;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A little arrow in the stream: upright whatever way you look, moving the way it points. */
    static final class Pijl extends SingleQuadParticle {
        Pijl(ClientLevel level, double x, double y, double z, double dy, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0, 0, 0, sprite);
            lifetime = dy < 0 ? 34 + random.nextInt(10) : 18 + random.nextInt(8);
            quadSize = 0.2f;
            gravity = 0;
            friction = 1.0f;
            hasPhysics = false;
            xd = 0;
            yd = dy;
            zd = 0;
            alpha = 0;
        }

        @Override
        public void tick() {
            super.tick();
            float f = age / (float) lifetime;
            alpha = Math.min(1f, Math.min(f * 6f, (1f - f) * 3f)) * 0.95f;
        }

        @Override
        public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
            return SingleQuadParticle.FacingCameraMode.LOOKAT_Y;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }

        /** (always bright: the arrows are how you find a lift at night) */
        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }
    }

    /** A fleck of foam: hops away from the foot of the fall, falls back, fades. */
    static final class Schuim extends SingleQuadParticle {
        Schuim(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0, 0, 0, sprite);
            lifetime = 12 + random.nextInt(12);
            quadSize = 0.07f + random.nextFloat() * 0.08f;
            gravity = 0.5f;
            friction = 0.94f;
            hasPhysics = false;
            xd = dx;
            yd = dy;
            zd = dz;
            float w = 0.93f + random.nextFloat() * 0.07f;
            setColor(w, w, 1.0f);
            alpha = 0.9f;
        }

        @Override
        public void tick() {
            super.tick();
            float f = age / (float) lifetime;
            alpha = Math.min(0.9f, (1f - f) * 2.2f);
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A puff of mist: big, faint, drifting up and thinning out. */
    static final class Nevel extends SingleQuadParticle {
        private final float basis;

        Nevel(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0, 0, 0, sprite);
            lifetime = 50 + random.nextInt(40);
            basis = 0.5f + random.nextFloat() * 0.5f;
            quadSize = basis;
            gravity = 0;
            friction = 0.97f;
            hasPhysics = false;
            xd = dx;
            yd = dy;
            zd = dz;
            roll = oRoll = random.nextFloat() * 6.28f;
            alpha = 0;
        }

        @Override
        public void tick() {
            super.tick();
            float f = age / (float) lifetime;
            quadSize = basis * (0.8f + 0.9f * f);
            alpha = Math.min(1f, Math.min(f * 5f, (1f - f) * 1.6f)) * 0.22f;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private Deeltjes() {
    }
}
