package nl.juiced.guhs.feature.zingendeweide.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.feature.zingendeweide.ZingendeWeideFeature;
import org.joml.Vector4f;

/**
 * De Zingende Weide on the client: the trillertjes (swarms of little coloured lights, the biome's ambient particle), the
 * bubbelsap's look (an animated rainbow sheen; a soft rainbow fog under it), the sky with the night's light pillars
 * ({@link WeideLucht}) and Badmeester Bubbels' look (the Knuffelbad's lifeguard).
 */
public final class WeideClient {
    private WeideClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterParticleProvidersEvent event) -> event.registerSpriteSet(ZingendeWeideFeature.TRILLERTJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Trillertje(level, x, y, z, sprites.get(random))));
        modBus.addListener((RegisterCustomEnvironmentEffectRendererEvent event) -> event.registerSkyboxRenderer(Guhs.id("zingende_weide"), new WeideLucht()));
        modBus.addListener((RegisterFluidModelsEvent event) -> event.register(new FluidModel.Unbaked(new Material(Guhs.id("block/bubbelsap_still")),
                new Material(Guhs.id("block/bubbelsap_flow")), null, null), ZingendeWeideFeature.BUBBELSAP, ZingendeWeideFeature.BUBBELSAP_STROOM));
        modBus.addListener((RegisterClientExtensionsEvent event) -> event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f fog) {
                float t = (level.getGameTime() + partialTick) * 0.01f;
                fog.set(0.75f + 0.2f * Mth.sin(t), 0.8f + 0.15f * Mth.sin(t + 2.1f), 0.95f, fog.w);
            }
        }, ZingendeWeideFeature.BUBBELSAP_TYPE.get()));
        SittingGuhRenderers.NPC_MODELEN.put(ZingendeWeideFeature.BADGUH, Guhs.id("entity/guh_npc_badmeesterguh"));
    }

    /** A trillertje: a little light in one of the Sift's colours, drifting in a slow swarm-like wobble, glowing. */
    static final class Trillertje extends SingleQuadParticle {
        private static final float[][] KLEUREN = {{0.55f, 0.75f, 1f}, {1f, 0.92f, 0.45f}, {0.78f, 0.6f, 1f}, {1f, 0.6f, 0.82f}};
        private final float fase;

        Trillertje(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0, 0, 0, sprite);
            lifetime = 100 + random.nextInt(120);
            quadSize = 0.035f + random.nextFloat() * 0.035f;
            fase = random.nextFloat() * 6.283f;
            gravity = 0;
            friction = 1.0f;
            hasPhysics = false;
            xd = (random.nextDouble() - 0.5) * 0.02;
            yd = (random.nextDouble() - 0.3) * 0.008;
            zd = (random.nextDouble() - 0.5) * 0.02;
            float[] k = KLEUREN[random.nextInt(KLEUREN.length)];
            setColor(k[0], k[1], k[2]);
            alpha = 0;
        }

        @Override
        public void tick() {
            super.tick();
            float f = age / (float) lifetime;
            xd += Mth.cos(age * 0.11f + fase) * 0.0012;
            zd += Mth.sin(age * 0.09f + fase) * 0.0012;
            yd += Mth.sin(age * 0.05f + fase) * 0.0004;
            alpha = Math.min(1f, Math.min(f * 5f, (1f - f) * 4f)) * (0.75f + 0.25f * Mth.sin(age * 0.4f + fase));
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
}
