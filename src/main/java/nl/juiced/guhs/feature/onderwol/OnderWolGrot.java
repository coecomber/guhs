package nl.juiced.guhs.feature.onderwol;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DripstoneClusterConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.PointedDripstoneConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.7.0 "Onder de Wol": three new kinds of cave and a little pink crystal for all the Guhmension's caves (resources:
 * tools/features/onderwol_grot.py; the biomes that get them: onderwol.py):
 * <ul>
 *   <li>kristalpegel caves: kristalpegelblok with pink kristalpegels hanging and standing ({@link Kristalpegel}, like
 *       dripstone; the tips drip a little kaas saus);</li>
 *   <li>knuffelwortel caves: knuffelwortelaarde in the ceiling with knuffelwortels hanging down (like rooted dirt and
 *       hanging roots), a soft floor of knuffelgras;</li>
 *   <li>belletjes caves: belletjeskristal on the walls with belletjestrosjes, which ring a note that depends on where they
 *       are ({@link OnderWolGrotBlokken});</li>
 *   <li>everywhere in the knuffelsteen: small rozenkwarts druppels on the cave ceilings and walls.</li>
 * </ul>
 * The features themselves: {@link OnderWolGrotWorldgen}.
 */
public final class OnderWolGrot {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);

    // --- the kristalpegels -----------------------------------------------------------------------------------------------
    /** The rock kristalpegels grow from (like a dripstone block). */
    public static final DeferredBlock<Block> KRISTALPEGELBLOK = BLOCKS.registerSimpleBlock("kristalpegelblok",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DRIPSTONE_BLOCK).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<Kristalpegel> KRISTALPEGEL = BLOCKS.registerBlock("kristalpegel", Kristalpegel::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POINTED_DRIPSTONE).mapColor(MapColor.COLOR_PINK).lightLevel(Kristalpegel::licht));

    // --- the knuffelwortels ----------------------------------------------------------------------------------------------
    public static final DeferredBlock<OnderWolGrotBlokken.Knuffelwortelaarde> KNUFFELWORTELAARDE = BLOCKS.registerBlock("knuffelwortelaarde",
            OnderWolGrotBlokken.Knuffelwortelaarde::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.ROOTED_DIRT).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<HangingRootsBlock> KNUFFELWORTELS = BLOCKS.registerBlock("knuffelwortels", HangingRootsBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.HANGING_ROOTS).mapColor(MapColor.COLOR_PINK));

    // --- the belletjes -----------------------------------------------------------------------------------------------------
    public static final DeferredBlock<OnderWolGrotBlokken.Belletjeskristal> BELLETJESKRISTAL = BLOCKS.registerBlock("belletjeskristal",
            OnderWolGrotBlokken.Belletjeskristal::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<OnderWolGrotBlokken.Belletjestrosje> BELLETJESTROSJE = BLOCKS.registerBlock("belletjestrosje",
            p -> new OnderWolGrotBlokken.Belletjestrosje(7.0f, 10.0f, p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).mapColor(MapColor.COLOR_PINK).lightLevel(s -> 4));

    // --- the rozenkwarts druppel -----------------------------------------------------------------------------------------
    /** A small pink crystal bud on any face (like a small amethyst bud). */
    public static final DeferredBlock<AmethystClusterBlock> ROZENKWARTS_DRUPPEL = BLOCKS.registerBlock("rozenkwarts_druppel",
            p -> new AmethystClusterBlock(3.0f, 8.0f, p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SMALL_AMETHYST_BUD).mapColor(MapColor.COLOR_PINK).sound(SoundType.SMALL_AMETHYST_BUD)
                    .lightLevel(s -> 2));

    /** Every block of this class, in creative-tab order (all have an item). */
    public static final List<DeferredBlock<? extends Block>> BLOCK_ITEMS = List.of(KRISTALPEGELBLOK, KRISTALPEGEL, KNUFFELWORTELAARDE,
            KNUFFELWORTELS, BELLETJESKRISTAL, BELLETJESTROSJE, ROZENKWARTS_DRUPPEL);
    static {
        for (DeferredBlock<? extends Block> block : BLOCK_ITEMS) {
            ITEMS.registerSimpleBlockItem(block);
        }
    }

    // --- worldgen ------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, Feature<DripstoneClusterConfiguration>> KRISTALPEGEL_CLUSTER =
            FEATURES.register("onderwol_kristalpegel_cluster", OnderWolGrotWorldgen.KristalpegelCluster::new);
    public static final DeferredHolder<Feature<?>, Feature<PointedDripstoneConfiguration>> KRISTALPEGEL_PUNT =
            FEATURES.register("onderwol_kristalpegel", OnderWolGrotWorldgen.KristalpegelPunt::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> BELLETJES =
            FEATURES.register("onderwol_belletjes", OnderWolGrotWorldgen.Belletjes::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> DRUPPELS =
            FEATURES.register("onderwol_druppels", OnderWolGrotWorldgen.Druppels::new);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
    }

    private OnderWolGrot() {
    }
}
