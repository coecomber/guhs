package nl.juiced.guhs.feature.guhpolder.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.guhpolder.GuhpolderFeature;

/**
 * Client side of the Guhpolder: the guh-molentje's turning sails ({@link MolentjeRenderer}), the frost glitter particle,
 * and the Pinguh's looks, waddle and belly-slide ({@link PinguhRender}, called from client.GuhRenderer).
 */
public final class GuhpolderClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(GuhpolderClient::renderers);
        modBus.addListener(GuhpolderClient::models);
        modBus.addListener(GuhpolderClient::particles);
        GuhSneeuw.init(modBus);                         // (2.10.1: the polder's own sparse guh-sneeuw)
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(GuhpolderFeature.GUH_MOLENTJE_BE.get(), MolentjeRenderer::new);
    }

    private static void models(ModelEvent.RegisterAdditional event) {
        event.register(MolentjeRenderer.WIEKEN);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GuhpolderFeature.GLINSTER.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Glinster(level, x, y, z, dx, dy, dz, sprites));
    }

    /** A tiny frost glitter: twinkles (grows and shrinks) while it drifts, then fades. */
    static class Glinster extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final float groot;

        Glinster(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            this.sprites = sprites;
            setSpriteFromAge(sprites);
            lifetime = 14 + random.nextInt(16);
            groot = 0.05f + random.nextFloat() * 0.05f;
            quadSize = groot;
            gravity = 0f;
            xd = dx + (random.nextDouble() - 0.5) * 0.01;
            yd = dy;
            zd = dz + (random.nextDouble() - 0.5) * 0.01;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            setSpriteFromAge(sprites);
            float t = (float) age / lifetime;
            quadSize = groot * (0.6f + 0.4f * (float) Math.abs(Math.sin(age * 0.7)));
            alpha = 1f - t * t;
        }

        @Override
        protected int getLightColor(float partialTick) {
            return 0xF000F0;   // (it glitters, even in the dark)
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private GuhpolderClient() {
    }
}
