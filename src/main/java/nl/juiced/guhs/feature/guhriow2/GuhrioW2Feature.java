package nl.juiced.guhs.feature.guhriow2;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * bbq2 (guhrio-w2): world 2 of Super Guhrio, "de kelders": the levels 2-1 (De buizenkelder) and 2-2 (Het nest van Guhshi)
 * of the Kasteel van de Grote Nether-Mika. The levels themselves are lanes built with the engine's lane builder
 * (tools/features/guhrio_w2.py, plugged into the castle by guhrio_kasteel.py); this package holds what world 2 adds to the
 * engine ({@code feature/guhrio}):
 * <ul>
 *     <li>the cellar's own masonry: {@link #KELDERGROND} and {@link #KELDERSTEEN} (plain blocks, the teal of an old
 *     underground level);</li>
 *     <li>Guhshi's egg, found and hatched: the egg itself is the engine's piece; {@link #EISLOT} is the lock that only
 *     opens for who carries the egg, {@link #BROEDPLEK} the spot where the egg is laid in the warm {@link #NEST} and
 *     Guhshi crawls out of it (a cutscene, once per player; {@link GuhrioW2#broed});</li>
 *     <li>the warp room: {@link #WARPPIJP} (on its {@link #WARPBUIS}) opens a later level for the player and puts them
 *     right in it ({@link GuhrioW2#warp}).</li>
 * </ul>
 * Everything is per player; nothing in the world ever changes. Resources: tools/features/guhrio_w2.py.
 */
public final class GuhrioW2Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The ground and the bricks of the cellars (plain blocks to build with). */
    public static final DeferredBlock<Block> KELDERGROND = BLOCKS.registerSimpleBlock("guhriow2_keldergrond",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5f, 6f).sound(SoundType.DEEPSLATE_BRICKS));
    public static final DeferredBlock<Block> KELDERSTEEN = BLOCKS.registerSimpleBlock("guhriow2_keldersteen",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5f, 6f).sound(SoundType.DEEPSLATE_BRICKS));
    /** The body of a warp pipe (a pillar, like the green pipe's body). */
    public static final DeferredBlock<RotatedPillarBlock> WARPBUIS = BLOCKS.registerBlock("guhriow2_warpbuis", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5f, 6f).sound(SoundType.COPPER));
    public static final DeferredBlock<GuhrioW2Blocks.WarpPijp> WARPPIJP = BLOCKS.registerBlock("guhriow2_warppijp", GuhrioW2Blocks.WarpPijp::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.5f, 6f).sound(SoundType.COPPER).lightLevel(s -> 9)
                    .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).dynamicShape());
    public static final DeferredBlock<GuhrioW2Blocks.EiSlot> EISLOT = BLOCKS.registerBlock("guhriow2_eislot", GuhrioW2Blocks.EiSlot::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 6f).sound(SoundType.METAL).lightLevel(s -> 5));
    public static final DeferredBlock<GuhrioW2Blocks.Nest> NEST = BLOCKS.registerBlock("guhriow2_nest", GuhrioW2Blocks.Nest::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5f, 6f).sound(SoundType.GRASS).lightLevel(s -> 10));
    public static final DeferredBlock<GuhrioW2Blocks.Broedplek> BROEDPLEK = BLOCKS.registerBlock("guhriow2_broedplek", GuhrioW2Blocks.Broedplek::new,
            () -> BlockBehaviour.Properties.of().noCollision().noLootTable().strength(-1f, 3600000f).noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK));

    public static final List<DeferredItem<BlockItem>> BLOK_ITEMS = List.of(
            ITEMS.registerSimpleBlockItem(KELDERGROND), ITEMS.registerSimpleBlockItem(KELDERSTEEN), ITEMS.registerSimpleBlockItem(WARPBUIS),
            ITEMS.registerSimpleBlockItem(WARPPIJP), ITEMS.registerSimpleBlockItem(EISLOT), ITEMS.registerSimpleBlockItem(NEST),
            ITEMS.registerSimpleBlockItem(BROEDPLEK));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.register(GuhrioW2Events.class);
        GuhrioW2.registreer();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : BLOK_ITEMS) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private GuhrioW2Feature() {
    }
}
