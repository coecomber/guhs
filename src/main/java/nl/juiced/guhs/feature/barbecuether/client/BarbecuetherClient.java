package nl.juiced.guhs.feature.barbecuether.client;

import javax.annotation.Nullable;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import org.joml.Vector4f;

/**
 * The Barbecuether on the client: the look of the frying sauce, the sky (none: a smoky ceiling, like the Nether, with
 * the biome's orange / grey fog) and extra thick smoke in the Rookdelta and the Asdal.
 * <p>
 * 1.1.0 (MC 26.1): the sky ({@code DimensionSpecialEffects}, SkyType.NONE, foggy everywhere) is data now: the dimension
 * type's {@code "skybox": "none"} and its fog attributes (like the Nether); the fluid textures are a {@link FluidModel}.
 */
public final class BarbecuetherClient {
    public static final ResourceKey<Biome> ROOKDELTA = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, Guhs.id("rookdelta"));
    public static final ResourceKey<Biome> ASDAL = ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, Guhs.id("asdal"));

    public static void init(IEventBus modBus) {
        modBus.addListener(BarbecuetherClient::fluidLooks);
        modBus.addListener(BarbecuetherClient::fluidModels);
        NeoForge.EVENT_BUS.addListener(BarbecuetherClient::fog);
    }

    private static void fluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(new Material(Guhs.id("block/kaasfrituursaus_still")), new Material(Guhs.id("block/kaasfrituursaus_flow")),
                null, null), BarbecuetherFeature.KAASFRITUURSAUS, BarbecuetherFeature.FLOWING_KAASFRITUURSAUS);
    }

    private static void fluidLooks(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                                       Vector4f fluidFogColor) {
                fluidFogColor.set(0.85f, 0.42f, 0.05f, fluidFogColor.w);
            }

            /** In the sauce you see almost nothing (like in lava). */
            @Override
            public void modifyFogRender(Camera camera, @Nullable FogEnvironment environment, float renderDistance, float partialTick, FogData fog) {
                boolean fireproof = camera.entity() instanceof net.minecraft.world.entity.LivingEntity living
                        && living.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE);
                fog.environmentalStart = fireproof ? 0.0f : 0.25f;
                fog.environmentalEnd = fireproof ? 5.0f : 1.0f;
                fog.skyEnd = fog.environmentalEnd;
                fog.cloudEnd = fog.environmentalEnd;
            }
        }, BarbecuetherFeature.KAASFRITUURSAUS_TYPE.get());
    }

    /** The Rookdelta is full of barbecue smoke, the Asdal of drifting ash: shorter view there. */
    private static void fog(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        if (camera.entity() == null || !(camera.entity().level() instanceof ClientLevel level) || level.dimension() != BarbecuetherFeature.BARBECUETHER
                || !event.getType().equals(net.minecraft.world.level.material.FogType.ATMOSPHERIC)) { // 1.1.0: air = ATMOSPHERIC
            return;
        }
        var biome = level.getBiome(camera.blockPosition());
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
        }
    }

    private BarbecuetherClient() {
    }
}
