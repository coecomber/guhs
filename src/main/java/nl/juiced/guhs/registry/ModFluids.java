package nl.juiced.guhs.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.juiced.guhs.Guhs;

/**
 * Kaas saus: warm cheese sauce. Spreads as far as water but 3x slower (lava is 6x slower), and is thicker to
 * move through than water. Right-click it with an empty hand to fill your hunger bar (see KaasSausDrinking).
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Guhs.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Guhs.MODID);

    public static final DeferredHolder<FluidType, FluidType> KAAS_SAUS_TYPE = FLUID_TYPES.register("kaas_saus",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.guhs.kaas_saus")
                    .density(1500)          // water 1000, lava 3000
                    .viscosity(3000)        // water 1000, lava 6000
                    .temperature(330)
                    .motionScale(0.009)     // water 0.014, lava 0.0023 (0.007 in the nether)
                    .fallDistanceModifier(0.3f)
                    .canSwim(true)
                    .canDrown(true)
                    .canExtinguish(true)
                    .supportsBoating(true)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> KAAS_SAUS = FLUIDS.register("kaas_saus",
            () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_KAAS_SAUS = FLUIDS.register("flowing_kaas_saus",
            () -> new BaseFlowingFluid.Flowing(properties()));

    /** Guh stomach acid: bubbling yellow-green, only there for the atmosphere. */
    public static final DeferredHolder<FluidType, FluidType> MAAGZUUR_TYPE = FLUID_TYPES.register("maagzuur",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("block.guhs.maagzuur")
                    .density(1100).viscosity(1400).temperature(310).motionScale(0.012)
                    .canSwim(true).canDrown(true).canExtinguish(true).supportsBoating(true).lightLevel(4)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> MAAGZUUR = FLUIDS.register("maagzuur",
            () -> new BaseFlowingFluid.Source(maagzuurProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_MAAGZUUR = FLUIDS.register("flowing_maagzuur",
            () -> new BaseFlowingFluid.Flowing(maagzuurProperties()));

    private static BaseFlowingFluid.Properties maagzuurProperties() {
        return new BaseFlowingFluid.Properties(MAAGZUUR_TYPE, MAAGZUUR, FLOWING_MAAGZUUR)
                .bucket(ModItems.MAAGZUUR_BUCKET).block(ModBlocks.MAAGZUUR)
                .tickRate(8).slopeFindDistance(3).levelDecreasePerBlock(2).explosionResistance(100f);
    }

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(KAAS_SAUS_TYPE, KAAS_SAUS, FLOWING_KAAS_SAUS)
                .bucket(ModItems.KAAS_SAUS_BUCKET)
                .block(ModBlocks.KAAS_SAUS)
                .tickRate(15)               // water 5, lava 30
                .slopeFindDistance(3)       // water 4, lava 2
                .levelDecreasePerBlock(1)   // like water: flows 7 blocks
                .explosionResistance(100f);
    }

    private ModFluids() {
    }
}
