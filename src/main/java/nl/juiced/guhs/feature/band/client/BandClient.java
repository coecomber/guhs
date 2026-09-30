package nl.juiced.guhs.feature.band.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.BandPayloads;

/**
 * Client side of the band (2.10): the heart particles, the floating hearts over your guh when it gets hearts (a big
 * heart burst at a level-up) and the cache of the Guhdex tab "Mijn guhs" ({@link MijnGuhsCache}).
 */
public final class BandClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(BandFeature.HARTJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Hartje(level, x, y, z, dx, dy, dz, sprites, false));
            event.registerSpriteSet(BandFeature.GROOT_HARTJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Hartje(level, x, y, z, dx, dy, dz, sprites, true));
        });
        // (2.10.1: right away when we're already on the game thread, so the data with the "Dagboekje" focus is there before
        // the Guhdex screen opens, which the next payload does right away; Minecraft.execute would queue it after that)
        BandPayloads.mijnGuhsOntvanger = p -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.isSameThread()) {
                MijnGuhsCache.zet(p.data());
            } else {
                mc.execute(() -> MijnGuhsCache.zet(p.data()));
            }
        };
        BandPayloads.hartjesOntvanger = p -> Minecraft.getInstance().execute(() -> hartjes(p.guh(), p.n()));
    }

    /** Floating hearts over a guh (n hearts added), or a big burst for a level-up (n &lt; 0). */
    static void hartjes(int guhId, int n) {
        ClientLevel level = Minecraft.getInstance().level;
        Entity guh = level == null ? null : level.getEntity(guhId);
        if (guh == null) {
            return;
        }
        double top = guh.getY() + guh.getBbHeight() + 0.25;
        if (n < 0) {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI * 2 / 24;
                level.addParticle(BandFeature.HARTJE.get(), guh.getX() + Math.cos(a) * 0.3, top, guh.getZ() + Math.sin(a) * 0.3,
                        Math.cos(a) * 0.12, 0.08 + level.random.nextDouble() * 0.05, Math.sin(a) * 0.12);
            }
            level.addParticle(BandFeature.GROOT_HARTJE.get(), guh.getX(), top + 0.4, guh.getZ(), 0, 0.02, 0);
            return;
        }
        int count = Math.min(8, 1 + n / 2);
        for (int i = 0; i < count; i++) {
            level.addParticle(BandFeature.HARTJE.get(), guh.getX() + (level.random.nextDouble() - 0.5) * guh.getBbWidth(),
                    top + level.random.nextDouble() * 0.2, guh.getZ() + (level.random.nextDouble() - 0.5) * guh.getBbWidth(),
                    (level.random.nextDouble() - 0.5) * 0.02, 0.04 + level.random.nextDouble() * 0.03, (level.random.nextDouble() - 0.5) * 0.02);
        }
    }

    /** A pink heart that floats up, wobbles a little and fades; the big one grows and pulses. */
    static class Hartje extends TextureSheetParticle {
        private final boolean groot;

        Hartje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, boolean groot) {
            super(level, x, y, z);
            this.groot = groot;
            pickSprite(sprites);
            lifetime = groot ? 40 : 22 + random.nextInt(14);
            quadSize = groot ? 0.6f : 0.08f + random.nextFloat() * 0.06f;
            gravity = groot ? 0f : -0.02f;
            xd = dx;
            yd = dy == 0 ? 0.03 : dy;
            zd = dz;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            xd *= 0.92;
            zd *= 0.92;
            if (groot) {
                quadSize = 0.6f + 0.08f * (float) Math.sin(age * 0.5);
                yd = 0.01;
            } else {
                xd += Math.sin((age + x * 10) * 0.4) * 0.002;
            }
            alpha = Math.min(1f, (lifetime - age) / 10f);
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

    private BandClient() {
    }
}
