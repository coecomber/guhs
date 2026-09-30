package nl.juiced.guhs.feature.barbecuether.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import org.joml.Vector3f;

/**
 * The Barbecuether on the client: the look of the frying sauce, the sky (none: a smoky ceiling, like the Nether, with
 * the biome's orange / grey fog) and extra thick smoke in the Rookdelta and the Asdal.
 */
public final class BarbecuetherClient {
    public static final ResourceKey<Biome> ROOKDELTA = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, Guhs.id("rookdelta"));
    public static final ResourceKey<Biome> ASDAL = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, Guhs.id("asdal"));

    public static void init(IEventBus modBus) {
        modBus.addListener(BarbecuetherClient::fluidLooks);
        modBus.addListener(BarbecuetherClient::effects);
        NeoForge.EVENT_BUS.addListener(BarbecuetherClient::fog);
    }

    private static void fluidLooks(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return Guhs.id("block/kaasfrituursaus_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return Guhs.id("block/kaasfrituursaus_flow");
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                                           Vector3f fluidFogColor) {
                return new Vector3f(0.85f, 0.42f, 0.05f);
            }

            /** In the sauce you see almost nothing (like in lava). */
            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance,
                                        float farDistance, FogShape shape) {
                boolean fireproof = camera.getEntity() instanceof net.minecraft.world.entity.LivingEntity living
                        && living.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
                RenderSystem.setShaderFogStart(fireproof ? 0.0f : 0.25f);
                RenderSystem.setShaderFogEnd(fireproof ? 5.0f : 1.0f);
            }
        }, BarbecuetherFeature.KAASFRITUURSAUS_TYPE.get());
    }

    /** No sky: a ceiling, and fog everywhere (tinted per biome: orange, red, mustard, grey, smoky). */
    public static class BarbecuetherSky extends DimensionSpecialEffects {
        public BarbecuetherSky() {
            super(Float.NaN, true, SkyType.NONE, false, true);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
            return color;
        }

        @Override
        public boolean isFoggyAt(int x, int z) {
            return true;
        }
    }

    private static void effects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Guhs.id("barbecuether"), new BarbecuetherSky());
    }

    /** The Rookdelta is full of barbecue smoke, the Asdal of drifting ash: shorter view there. */
    private static void fog(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        if (!(camera.getEntity().level() instanceof ClientLevel level) || level.dimension() != BarbecuetherFeature.BARBECUETHER
                || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN || !camera.getFluidInCamera().equals(net.minecraft.world.level.material.FogType.NONE)) {
            return;
        }
        var biome = level.getBiome(camera.getBlockPosition());
        float far;
        if (biome.is(ROOKDELTA)) {
            far = 40f;
        } else if (biome.is(ASDAL)) {
            far = 64f;
        } else {
            return;
        }
        if (event.getFarPlaneDistance() > far) {
            event.setNearPlaneDistance(0f);
            event.setFarPlaneDistance(far);
            event.setCanceled(true);
        }
    }

    private BarbecuetherClient() {
    }
}
