package nl.juiced.guhs.feature.tuintjes.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.tuintjes.TuintjesFeature;

/** Client side of the tuintjes: the particles (gieterdruppel, groeisprankel). The blocks draw with their models. */
public final class TuintjesClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(TuintjesFeature.GIETERDRUPPEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Druppel(level, x, y, z, sprites));
            event.registerSpriteSet(TuintjesFeature.GROEISPRANKEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Sprankel(level, x, y, z, sprites));
        });
    }

    /** A water drop: a little hop, then down it falls. */
    static class Druppel extends SingleQuadParticle {
        Druppel(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.get(level.getRandom()));
            lifetime = 14 + random.nextInt(8);
            quadSize = 0.05f + random.nextFloat() * 0.03f;
            gravity = 0.9f;
            xd = (random.nextDouble() - 0.5) * 0.04;
            yd = 0.05;
            zd = (random.nextDouble() - 0.5) * 0.04;
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A green-gold sparkle rising from a plant that grows: twinkles and fades. */
    static class Sprankel extends SingleQuadParticle {
        private final SpriteSet sprites;

        Sprankel(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.first());
            this.sprites = sprites;
            setSpriteFromAge(sprites);
            lifetime = 20 + random.nextInt(14);
            quadSize = 0.07f + random.nextFloat() * 0.05f;
            gravity = -0.03f;
            xd = (random.nextDouble() - 0.5) * 0.02;
            yd = 0.02;
            zd = (random.nextDouble() - 0.5) * 0.02;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            alpha = Math.min(1f, (lifetime - age) / 8f);
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private TuintjesClient() {
    }
}
