package nl.juiced.guhs.feature.tuintjes.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.tuintjes.TuintjesFeature;

/** Client side of the tuintjes: the particles (gieterdruppel, groeisprankel). The blocks draw with their models. */
public final class TuintjesClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(TuintjesFeature.GIETERDRUPPEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Druppel(level, x, y, z, sprites));
            event.registerSpriteSet(TuintjesFeature.GROEISPRANKEL.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Sprankel(level, x, y, z, sprites));
        });
    }

    /** A water drop: a little hop, then down it falls. */
    static class Druppel extends TextureSheetParticle {
        Druppel(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 14 + random.nextInt(8);
            quadSize = 0.05f + random.nextFloat() * 0.03f;
            gravity = 0.9f;
            xd = (random.nextDouble() - 0.5) * 0.04;
            yd = 0.05;
            zd = (random.nextDouble() - 0.5) * 0.04;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A green-gold sparkle rising from a plant that grows: twinkles and fades. */
    static class Sprankel extends TextureSheetParticle {
        private final SpriteSet sprites;

        Sprankel(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
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
        public int getLightColor(float partialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private TuintjesClient() {
    }
}
