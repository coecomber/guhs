package nl.juiced.guhs.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.world.FlatJigsawStructure;

/** Our own structure types (used by the JSON in data/guhs/worldgen/structure), and the placement filter that goes with them. */
public final class ModStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<FlatJigsawStructure>> FLAT_JIGSAW =
            STRUCTURE_TYPES.register("flat_jigsaw", () -> () -> FlatJigsawStructure.CODEC);

    /** 1.1.2: placement guhs:gegarandeerd (one guaranteed copy in a ring around spawn, see GegarandeerdPlacement). */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Guhs.MODID);

    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>,
            net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<nl.juiced.guhs.world.GegarandeerdPlacement>> GEGARANDEERD =
            PLACEMENT_TYPES.register("gegarandeerd", () -> () -> nl.juiced.guhs.world.GegarandeerdPlacement.CODEC);

    /** Placement filter guhs:buiten_gebouwen (no plants on roofs). */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.placement.PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Guhs.MODID);

    public static final DeferredHolder<net.minecraft.world.level.levelgen.placement.PlacementModifierType<?>,
            net.minecraft.world.level.levelgen.placement.PlacementModifierType<nl.juiced.guhs.world.BuitenGebouwenFilter>> BUITEN_GEBOUWEN =
            PLACEMENT_MODIFIERS.register("buiten_gebouwen", () -> () -> nl.juiced.guhs.world.BuitenGebouwenFilter.CODEC);

    /** 2.10: guhs:grond_single_pool_element, a single pool element with its own ground level (no moat around sunk buildings). */
    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType<?>> POOL_ELEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_POOL_ELEMENT, Guhs.MODID);

    public static final DeferredHolder<net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType<?>,
            net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType<nl.juiced.guhs.world.grond.GrondPoolElement>> GROND_SINGLE_POOL_ELEMENT =
            POOL_ELEMENT_TYPES.register("grond_single_pool_element", () -> () -> nl.juiced.guhs.world.grond.GrondPoolElement.CODEC);

    private ModStructureTypes() {
    }
}
