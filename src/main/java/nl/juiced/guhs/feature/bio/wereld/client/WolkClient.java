package nl.juiced.guhs.feature.bio.wereld.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.feature.bio.wereld.WolkBlokken;

/**
 * Client side of the Wolkenweide (biomes3 wereld-wolk): its two ambient particles (floating fluff and glints; the biome
 * file asks for them, the game spawns them around the camera) and its sky ({@link WolkLucht}: a warmer sunset and more
 * stars). WereldClient calls {@link #init}.
 * <p>
 * {@link #diepte} says how deep the camera is in the Wolkenweide (0 at its edge and outside, 1 well inside): a few biome
 * lookups around the camera twice a second, eased per tick, so the sky never jumps at the edge of the biome.
 */
public final class WolkClient {
    private static float diepte, doel;
    private static int teller;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(WolkBlokken.PLUISJE.get(),
                    sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Pluisje(level, x, y, z, sprites.get(random)));
            event.registerSpriteSet(WolkBlokken.GLINSTER.get(),
                    sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Glinster(level, x, y, z, sprites.get(random)));
        });
        modBus.addListener((RegisterCustomEnvironmentEffectRendererEvent event) -> event.registerSkyboxRenderer(Guhs.id("wolkenweide"), new WolkLucht()));
        NeoForge.EVENT_BUS.addListener(WolkClient::onTick);
    }

    /** How deep the camera is in the Wolkenweide: 0 outside and at its edge, 1 well inside. */
    public static float diepte() {
        return diepte;
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            diepte = doel = 0;
            return;
        }
        if (teller++ % 10 == 0) {
            BlockPos p = mc.player.blockPosition();
            int binnen = 0;
            for (int i = 0; i < 9; i++) {
                int dx = (i % 3 - 1) * 40, dz = (i / 3 - 1) * 40;
                binnen += level.getBiome(p.offset(dx, 0, dz)).is(Bio.WOLKENWEIDE) ? 1 : 0;
            }
            doel = level.getBiome(p).is(Bio.WOLKENWEIDE) ? Mth.clamp((binnen - 4) / 5f, 0f, 1f) : 0f;
        }
        diepte += (doel - diepte) * 0.03f;
        if (Math.abs(doel - diepte) < 0.002f) {
            diepte = doel;
        }
    }

    /** A bit of fluff: drifts slowly sideways on the wind, bobbing, and fades in and out. */
    static final class Pluisje extends SingleQuadParticle {
        private final float fase;

        Pluisje(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0, 0, 0, sprite);
            lifetime = 120 + random.nextInt(100);
            quadSize = 0.07f + random.nextFloat() * 0.07f;
            fase = random.nextFloat() * 6.283f;
            gravity = 0;
            friction = 1.0f;
            hasPhysics = false;
            xd = 0.012 + random.nextDouble() * 0.012;
            yd = (random.nextDouble() - 0.4) * 0.006;
            zd = (random.nextDouble() - 0.5) * 0.012;
            float w = 0.94f + random.nextFloat() * 0.06f;
            setColor(1.0f, w, 0.97f + random.nextFloat() * 0.03f);
            alpha = 0;
        }

        @Override
        public void tick() {
            super.tick();
            float f = age / (float) lifetime;
            yd += Mth.sin(age * 0.07f + fase) * 0.0003;
            alpha = Math.min(1f, Math.min(f * 6f, (1f - f) * 4f)) * 0.8f;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** A glint: hangs still, swells and fades once; always bright. */
    static final class Glinster extends SingleQuadParticle {
        private final float basis;

        Glinster(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0, 0, 0, sprite);
            lifetime = 18 + random.nextInt(16);
            basis = 0.05f + random.nextFloat() * 0.05f;
            quadSize = 0;
            gravity = 0;
            friction = 1.0f;
            hasPhysics = false;
            xd = 0;
            yd = 0.002;
            zd = 0;
            int soort = random.nextInt(3);
            setColor(1.0f, soort == 0 ? 0.95f : soort == 1 ? 0.86f : 0.97f, soort == 0 ? 0.80f : soort == 1 ? 0.96f : 1.0f);
            alpha = 0;
        }

        @Override
        public void tick() {
            super.tick();
            float f = age / (float) lifetime, puls = Mth.sin(f * 3.1416f);
            quadSize = basis * (0.3f + puls);
            alpha = puls * 0.9f;
        }

        @Override
        public SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;
        }
    }

    private WolkClient() {
    }
}
