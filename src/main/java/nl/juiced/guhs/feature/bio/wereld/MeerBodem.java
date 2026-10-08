package nl.juiced.guhs.feature.bio.wereld;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * biomes3 fix-klein, the Bloesemmeertje: what the lake's bed is made of (resources: tools/features/bio_wereld_meer.py).
 * <p>
 * Soft sediment of our own, so a player who digs the lake floor gets lake floor and not wool: under the vanilla sand of
 * the shallows first {@code bloesemmeertje_meerzand} (pale), then three tones of silt that get bluer and darker with the
 * depth: {@code bloesemmeertje_meerslib_licht}, {@code bloesemmeertje_meerslib}, {@code bloesemmeertje_meerslib_diep}.
 * All four are plain blocks (they do not fall), dug with a shovel, and drop themselves. {@link #trap} orders them the
 * way {@link MeerVulling#bodem} lays them.
 * <p>
 * Their use: any meerslib bakes in a furnace into {@code bloesemmeertje_meertegels}, a pale turquoise tile with slab and
 * stairs; meerzand melts into glass like sand.
 */
public final class MeerBodem {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static BlockBehaviour.Properties zand() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.SNARE).strength(0.5f).sound(SoundType.SAND);
    }

    private static BlockBehaviour.Properties slib(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(0.5f).sound(SoundType.MUD);
    }

    private static BlockBehaviour.Properties tegel() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(1.5f, 6f).sound(SoundType.CALCITE);
    }

    public static final DeferredBlock<Block> MEERZAND = BLOCKS.registerSimpleBlock("bloesemmeertje_meerzand", MeerBodem::zand);
    public static final DeferredBlock<Block> SLIB_LICHT = BLOCKS.registerSimpleBlock("bloesemmeertje_meerslib_licht", () -> slib(MapColor.COLOR_LIGHT_BLUE));
    public static final DeferredBlock<Block> SLIB = BLOCKS.registerSimpleBlock("bloesemmeertje_meerslib", () -> slib(MapColor.WARPED_STEM));
    public static final DeferredBlock<Block> SLIB_DIEP = BLOCKS.registerSimpleBlock("bloesemmeertje_meerslib_diep", () -> slib(MapColor.COLOR_CYAN));
    public static final DeferredBlock<Block> TEGELS = BLOCKS.registerSimpleBlock("bloesemmeertje_meertegels", MeerBodem::tegel);
    public static final DeferredBlock<SlabBlock> TEGELS_PLAAT = BLOCKS.registerBlock("bloesemmeertje_meertegels_plaat", SlabBlock::new, MeerBodem::tegel);
    public static final DeferredBlock<StairBlock> TEGELS_TRAP = BLOCKS.registerBlock("bloesemmeertje_meertegels_trap",
            p -> new StairBlock(TEGELS.get().defaultBlockState(), p), MeerBodem::tegel);

    /** Every block here, in the order of the creative tab. */
    public static List<DeferredBlock<? extends Block>> blokken() {
        return List.of(MEERZAND, SLIB_LICHT, SLIB, SLIB_DIEP, TEGELS, TEGELS_PLAAT, TEGELS_TRAP);
    }

    private static final List<DeferredItem<BlockItem>> ITEMS_LIJST = blokken().stream().map(b -> ITEMS.registerSimpleBlockItem(b)).toList();

    /** The four sediments from the shallows to the deep (after the sand of the very edge). */
    public static List<BlockState> trap() {
        return List.of(MEERZAND.get().defaultBlockState(), SLIB_LICHT.get().defaultBlockState(), SLIB.get().defaultBlockState(),
                SLIB_DIEP.get().defaultBlockState());
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : ITEMS_LIJST) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private MeerBodem() {
    }
}
