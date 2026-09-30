package nl.juiced.guhs.feature.bakkerij.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.bakkerij.BakkerijPayloads;

/**
 * Client side of the Knabbelbakkerij: the customer guhs with their order bubbles ({@link KlantRenderer}), the particles
 * (knabbelwolkje, meelstofje), Bakker Korstje's own model (a tall baker's toque with a kaasknabbel and an apron; his
 * toque wobbles when he talks) and the two screens ({@link KorstjeScherm}, {@link BakScherm}).
 */
public final class BakkerijClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(BakkerijFeature.KLANT.get(), KlantRenderer::new));
        modBus.addListener(BakkerijClient::particles);
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BAKKERGUH, Guhs.id("geo/entity/guh_npc_bakkerguh.geo.json"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.BAKKERGUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.07f;
            bot.apply("korstje_muts").ifPresent(b -> b.setRotZ((float) Math.sin(t) * 0.06f));
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BakkerijFeature.KNABBELWOLKJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) -> new Wolkje(level, x, y, z, dx, dy, dz, sprites));
        event.registerSpriteSet(BakkerijFeature.MEELSTOFJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) -> new Meel(level, x, y, z, dx, dy, dz, sprites));
    }

    /** guhs:bakkerij_open: Korstje's screen or the baking screen. */
    public static void open(BakkerijPayloads.Open payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.soort() == BakkerijPayloads.KORSTJE) {
            mc.setScreen(new KorstjeScherm((int) payload.ref(), payload.data()));
        } else {
            mc.setScreen(new BakScherm(payload.ref(), payload.data()));
        }
    }

    /** guhs:bakkerij_status: the baking screen's update. */
    public static void status(BakkerijPayloads.Status payload) {
        if (Minecraft.getInstance().screen instanceof BakScherm scherm) {
            scherm.update(payload.data());
        }
    }

    /** A little knabbel-shaped cloud: rises, grows a bit, drifts and fades. */
    static class Wolkje extends TextureSheetParticle {
        private final float start;

        Wolkje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 50 + random.nextInt(40);
            start = quadSize = 0.18f + random.nextFloat() * 0.12f;
            xd = dx + (random.nextDouble() - 0.5) * 0.01;
            yd = Math.max(0.02, dy);
            zd = dz + (random.nextDouble() - 0.5) * 0.01;
            gravity = -0.002f;
            friction = 0.97f;
            hasPhysics = false;
            alpha = 0f;
        }

        @Override
        public void tick() {
            super.tick();
            float t = age / (float) lifetime;
            quadSize = start * (1f + t * 1.2f);
            alpha = Math.min(1f, Math.min(t * 6f, (1f - t) * 2.5f)) * 0.9f;
            xd += Math.sin(age * 0.08 + x) * 0.0006;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A pinch of flour: tiny, drifts down slowly. */
    static class Meel extends TextureSheetParticle {
        Meel(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 30 + random.nextInt(30);
            quadSize = 0.04f + random.nextFloat() * 0.04f;
            xd = dx + (random.nextDouble() - 0.5) * 0.02;
            yd = dy + random.nextDouble() * 0.01;
            zd = dz + (random.nextDouble() - 0.5) * 0.02;
            gravity = 0.03f;
            friction = 0.9f;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private BakkerijClient() {
    }
}
