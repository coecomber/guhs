package nl.juiced.guhs.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.block.entity.FryingPanBlockEntity;
import nl.juiced.guhs.block.entity.GuhSpawnerBlockEntity;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FryingPanBlockEntity>> FRYING_PAN = BLOCK_ENTITIES.register("frying_pan",
            () -> BlockEntityType.Builder.of(FryingPanBlockEntity::new, ModBlocks.FRYING_PAN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhSpawnerBlockEntity>> GUH_SPAWNER = BLOCK_ENTITIES.register("guh_spawner",
            () -> BlockEntityType.Builder.of(GuhSpawnerBlockEntity::new, ModBlocks.GUH_SPAWNER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhWheelBlockEntity>> GUH_WHEEL = BLOCK_ENTITIES.register("guh_wheel",
            () -> BlockEntityType.Builder.of(GuhWheelBlockEntity::new, ModBlocks.GUH_WHEEL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BankGuhBlockEntity>> BANK_GUH = BLOCK_ENTITIES.register("bank_guh",
            () -> BlockEntityType.Builder.of(BankGuhBlockEntity::new, ModBlocks.BANK_GUH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<nl.juiced.guhs.block.entity.SleeRailBlockEntity>> SLEE_RAIL = BLOCK_ENTITIES.register("slee_rail",
            () -> BlockEntityType.Builder.of(nl.juiced.guhs.block.entity.SleeRailBlockEntity::new, ModBlocks.SLEE_RAIL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<nl.juiced.guhs.block.KoningsTroonBlock.Entity>> KONINGSTROON =
            BLOCK_ENTITIES.register("koningstroon", () -> BlockEntityType.Builder.of(nl.juiced.guhs.block.KoningsTroonBlock.Entity::new,
                    ModBlocks.KONINGSTROON.get()).build(null));

    private ModBlockEntities() {
    }
}
