package nl.juiced.guhs.feature.wereldleven.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.feature.knus.client.GuhRenderHooks;
import nl.juiced.guhs.feature.wereldleven.GrijpmachineBlockEntity;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;
import nl.juiced.guhs.feature.wereldleven.WereldlevenPayloads;

/**
 * Client side of the wereldleven feature: IJscoguh Tingeling's renderer, the grijpmachine (its plushies and claw in the
 * glass case, the claw screen), the xylofoon screen / liedjesboekje, the guh layers (ice-cream hat, blushing cheeks, the
 * nap nestje) and the particles (zangnootje, ijsjeshartje, fluitstoom).
 */
public final class WereldlevenClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(WereldlevenClient::renderers);
        modBus.addListener(WereldlevenClient::particles);
        modBus.addListener(WereldlevenClient::models);
        GuhRenderHooks.laag(WereldlevenLagen::render);
        WereldlevenFeature.openXylofoon = pos -> Minecraft.getInstance().setScreen(new XylofoonScherm(pos));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WereldlevenFeature.IJSCOGUH.get(), IJscoguhRenderer::new);
        event.registerBlockEntityRenderer(WereldlevenFeature.GRIJPMACHINE_BE.get(), GrijpmachineRenderer::new);
    }

    private static void models(ModelEvent.RegisterAdditional event) {
        event.register(GrijpmachineRenderer.KLAUW);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(WereldlevenFeature.ZANGNOOTJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Nootje(level, x, y, z, sprites));
        event.registerSpriteSet(WereldlevenFeature.IJSJESHARTJE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Hartje(level, x, y, z, sprites));
        event.registerSpriteSet(WereldlevenFeature.FLUITSTOOM.get(), sprites -> (type, level, x, y, z, dx, dy, dz) -> new Stoom(level, x, y, z, dx, dy, dz, sprites));
    }

    /** guhs:wereldleven_grijp_open */
    public static void grijpOpen(WereldlevenPayloads.GrijpOpen p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof GrijpmachineScherm scherm && scherm.pos().equals(p.pos())) {
            scherm.prijzen(GrijpmachineBlockEntity.leesPrijzen(p.prijzen()));
            return;
        }
        mc.setScreen(new GrijpmachineScherm(p.pos(), GrijpmachineBlockEntity.leesPrijzen(p.prijzen())));
    }

    /** guhs:wereldleven_grijp_uitslag */
    public static void grijpUitslag(WereldlevenPayloads.GrijpUitslag p) {
        if (Minecraft.getInstance().screen instanceof GrijpmachineScherm scherm && scherm.pos().equals(p.pos())) {
            scherm.uitslag(p.index(), p.gepakt(), p.knuffel(), GrijpmachineBlockEntity.leesPrijzen(p.prijzen()));
        }
    }

    /** A music note in a soft colour: floats up, wobbling, and fades. */
    static class Nootje extends TextureSheetParticle {
        private final float fase;

        Nootje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 30 + random.nextInt(20);
            quadSize = 0.12f + random.nextFloat() * 0.05f;
            gravity = -0.02f;
            yd = 0.02 + random.nextDouble() * 0.01;
            xd = zd = 0;
            hasPhysics = false;
            fase = random.nextFloat() * 6.28f;
            float[][] kleuren = {{1f, 0.62f, 0.8f}, {0.6f, 0.9f, 0.8f}, {1f, 0.86f, 0.4f}, {0.7f, 0.75f, 1f}, {0.9f, 0.7f, 1f}};
            float[] k = kleuren[random.nextInt(kleuren.length)];
            setColor(k[0], k[1], k[2]);
        }

        @Override
        public void tick() {
            super.tick();
            xd = Math.sin(age * 0.3 + fase) * 0.02;
            zd = Math.cos(age * 0.3 + fase) * 0.01;
            alpha = Math.min(1f, (lifetime - age) / 10f);
            oRoll = roll;
            roll = (float) Math.sin(age * 0.25 + fase) * 0.3f;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A little pink heart: floats up and fades. */
    static class Hartje extends TextureSheetParticle {
        Hartje(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 24 + random.nextInt(16);
            quadSize = 0.08f + random.nextFloat() * 0.05f;
            gravity = -0.015f;
            xd = (random.nextDouble() - 0.5) * 0.02;
            yd = 0.03;
            zd = (random.nextDouble() - 0.5) * 0.02;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            alpha = Math.min(1f, (lifetime - age) / 8f);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** A puff of steam from the fluitje: grows and fades. */
    static class Stoom extends TextureSheetParticle {
        Stoom(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z);
            pickSprite(sprites);
            lifetime = 16 + random.nextInt(8);
            quadSize = 0.06f;
            gravity = -0.01f;
            xd = dx + (random.nextDouble() - 0.5) * 0.01;
            yd = 0.015;
            zd = dz + (random.nextDouble() - 0.5) * 0.01;
            hasPhysics = false;
            alpha = 0.8f;
        }

        @Override
        public void tick() {
            super.tick();
            quadSize += 0.006f;
            alpha = 0.8f * (1f - age / (float) lifetime);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private WereldlevenClient() {
    }
}
