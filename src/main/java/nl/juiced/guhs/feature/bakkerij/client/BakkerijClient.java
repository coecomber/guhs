package nl.juiced.guhs.feature.bakkerij.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BAKKERGUH, Guhs.id("entity/guh_npc_bakkerguh"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.BAKKERGUH, (npc, tick) -> {
            float t = (float) tick * 0.07f;
            return bones -> bones.ifPresent("korstje_muts", b -> b.setRotZ((float) Math.sin(t) * 0.06f));
        });
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BakkerijFeature.KNABBELWOLKJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Wolkje(level, x, y, z, dx, dy, dz, sprites.get(random)));
        event.registerSpriteSet(BakkerijFeature.MEELSTOFJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Meel(level, x, y, z, dx, dy, dz, sprites.get(random)));
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
    static class Wolkje extends SingleQuadParticle {
        private final float start;

        Wolkje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
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
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A pinch of flour: tiny, drifts down slowly. */
    static class Meel extends SingleQuadParticle {
        Meel(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
            lifetime = 30 + random.nextInt(30);
            quadSize = 0.04f + random.nextFloat() * 0.04f;
            xd = dx + (random.nextDouble() - 0.5) * 0.02;
            yd = dy + random.nextDouble() * 0.01;
            zd = dz + (random.nextDouble() - 0.5) * 0.02;
            gravity = 0.03f;
            friction = 0.9f;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    private BakkerijClient() {
    }
}
