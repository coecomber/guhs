package nl.juiced.guhs.registry;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/** Lets the game index guh portal blocks, so the return trip finds an existing portal quickly. */
public final class ModPoiTypes {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Guhs.MODID);

    public static final DeferredHolder<PoiType, PoiType> GUH_PORTAL = POI_TYPES.register("guh_portal",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.GUH_PORTAL.get().getStateDefinition().getPossibleStates()), 0, 1));

    // guh village job sites
    public static final DeferredHolder<PoiType, PoiType> KNABBELBAK = POI_TYPES.register("knabbelbak",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.KNABBELBAK.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<PoiType, PoiType> NAAITAFEL = POI_TYPES.register("naaitafel",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.NAAITAFEL.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<PoiType, PoiType> VADSAAMBEELD = POI_TYPES.register("vadsaambeeld",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.VADSAAMBEELD.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<PoiType, PoiType> BUIZENBANK = POI_TYPES.register("buizenbank",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.BUIZENBANK.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<PoiType, PoiType> MIKATROFEE = POI_TYPES.register("mikatrofee",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.MIKATROFEE.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final DeferredHolder<PoiType, PoiType> ZAADBAK = POI_TYPES.register("zaadbak",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.ZAADBAK.get().getStateDefinition().getPossibleStates()), 1, 1));

    /** The knabbelkorf (a beehive for guh bees): in the minecraft:bee_home tag, so bees find it. */
    public static final DeferredHolder<PoiType, PoiType> KNABBELKORF = POI_TYPES.register("knabbelkorf",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.KNABBELKORF.get().getStateDefinition().getPossibleStates()), 0, 1));

    /** The knabbelkorf uses the vanilla beehive block entity. */
    public static void addHiveBlocks(net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent event) {
        event.modify(net.minecraft.world.level.block.entity.BlockEntityType.BEEHIVE, ModBlocks.KNABBELKORF.get());
        event.modify(net.minecraft.world.level.block.entity.BlockEntityType.BARREL, ModBlocks.GUH_KAST.get()); // the cupboard
    }

    private ModPoiTypes() {
    }
}
