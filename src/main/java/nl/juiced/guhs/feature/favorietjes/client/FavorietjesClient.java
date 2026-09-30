package nl.juiced.guhs.feature.favorietjes.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.favorietjes.FavorietjesFeature;

/**
 * Client side of the favorietjes (2.10): the particles. The heart explosion of a discovered favourite is an emitter that
 * throws rings of pink hearts and golden sparkles in every direction for a few ticks, with one big pulsing heart in the
 * middle; a curious guh gets a bobbing pink question mark, a sniffing one little puffs at its snoet.
 */
public final class FavorietjesClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            event.registerSpecial(FavorietjesFeature.EXPLOSIE.get(), (type, level, x, y, z, dx, dy, dz) -> new Explosie(level, x, y, z));
            event.registerSpriteSet(FavorietjesFeature.GLINSTER.get(),
                    sprites -> (type, level, x, y, z, dx, dy, dz) -> new Glinster(level, x, y, z, dx, dy, dz, sprites));
            event.registerSpriteSet(FavorietjesFeature.VRAAGJE.get(),
                    sprites -> (type, level, x, y, z, dx, dy, dz) -> new Vraagje(level, x, y, z, sprites));
            event.registerSpriteSet(FavorietjesFeature.SNUFFEL.get(),
                    sprites -> (type, level, x, y, z, dx, dy, dz) -> new Snuffel(level, x, y, z, dx, dy, dz, sprites));
        });
    }

    /** The big heart explosion: 6 ticks of heart rings and sparkles, one big heart. */
    static class Explosie extends NoRenderParticle {
        Explosie(ClientLevel level, double x, double y, double z) {
            super(level, x, y, z, 0, 0, 0);
            lifetime = 6;
        }

        @Override
        public void tick() {
            if (age == 0) {
                level.addParticle(BandFeature.GROOT_HARTJE.get(), x, y + 0.9, z, 0, 0.02, 0);
            }
            int n = age == 0 ? 18 : 10;
            for (int i = 0; i < n; i++) {
                // evenly around a sphere (golden angle), a little random, faster in the first tick
                double t = (i + 0.5) / n;
                double inclinatie = Math.acos(1 - 2 * t);
                double azimut = Math.PI * (1 + Math.sqrt(5)) * i + age * 0.7;
                double v = (age == 0 ? 0.42 : 0.3) * (0.8 + random.nextDouble() * 0.4);
                double vx = Math.sin(inclinatie) * Math.cos(azimut) * v;
                double vy = Math.cos(inclinatie) * v * 0.7 + 0.08;
                double vz = Math.sin(inclinatie) * Math.sin(azimut) * v;
                level.addParticle(BandFeature.HARTJE.get(), x, y, z, vx, vy, vz);
                if (i % 3 == 0) {
                    level.addParticle(FavorietjesFeature.GLINSTER.get(), x, y, z, vx * 1.2, vy * 1.2, vz * 1.2);
                }
            }
            if (++age >= lifetime) {
                remove();
            }
        }
    }

    /** A twinkling little star: grows and shrinks, fades out. */
    static class Glinster extends TextureSheetParticle {
        private final float basis;

        Glinster(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 16 + random.nextInt(12);
            basis = 0.07f + random.nextFloat() * 0.06f;
            quadSize = basis;
            xd = dx;
            yd = dy;
            zd = dz;
            gravity = 0.01f;
            friction = 0.88f;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            quadSize = basis * (0.6f + 0.4f * Mth.sin(age * 0.9f));
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

    /** A pink question mark that pops up above a curious guh and bobs a little. */
    static class Vraagje extends TextureSheetParticle {
        Vraagje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 34;
            quadSize = 0.02f;
            gravity = 0f;
            yd = 0.012;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            quadSize = age < 5 ? 0.04f * age + 0.02f : 0.22f + 0.015f * Mth.sin(age * 0.5f);
            xd = Mth.sin(age * 0.4f) * 0.006;
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

    /** A little sniff puff: out of the snoet, drifts and fades. */
    static class Snuffel extends TextureSheetParticle {
        Snuffel(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 12 + random.nextInt(8);
            quadSize = 0.05f + random.nextFloat() * 0.04f;
            xd = dx + (random.nextDouble() - 0.5) * 0.03;
            yd = 0.01 + random.nextDouble() * 0.01;
            zd = dz + (random.nextDouble() - 0.5) * 0.03;
            friction = 0.9f;
            gravity = -0.005f;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            quadSize *= 1.03f;
            alpha = Math.min(0.9f, (lifetime - age) / 6f);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private FavorietjesClient() {
    }
}
