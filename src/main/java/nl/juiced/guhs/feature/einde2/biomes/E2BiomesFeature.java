package nl.juiced.guhs.feature.einde2.biomes;

import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice biomes: the six biomes of the outer islands: their blocks, plants, worldgen features, the kristalkaas and the knabbelkoor.
 * Skeleton stub: the slice fills this class in place (it is already wired in feature/einde2/Einde2).
 * Resources: tools/features/einde2_biomes.py.
 */
public final class E2BiomesFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    // --- the ids the other slices build with (build contract 3.4): plain stand-in blocks and items until this slice makes the real ones ---
    public static final DeferredBlock<Block> KRUIMELROTS = plaatshouder("kruimelrots");
    public static final DeferredBlock<Block> KRUIMELZAND = plaatshouder("kruimelzand");
    public static final DeferredBlock<Block> GEBARSTEN_KORST = plaatshouder("gebarsten_korst");
    public static final DeferredBlock<Block> KRUIMELWOLK = plaatshouder("kruimelwolk");
    public static final DeferredBlock<Block> DICHTE_KRUIMELWOLK = plaatshouder("dichte_kruimelwolk");
    public static final DeferredBlock<Block> SCHIMMELKAAS = plaatshouder("schimmelkaas");
    public static final DeferredBlock<Block> KLIPKAAS = plaatshouder("klipkaas");
    public static final DeferredBlock<Block> KRISTALKAAS_KRISTAL = plaatshouder("kristalkaas_kristal");
    public static final DeferredBlock<Block> KOORKORST = plaatshouder("koorkorst");
    public static final DeferredBlock<Block> KNABBELKOORPLANT = plaatshouder("knabbelkoorplant");
    public static final DeferredBlock<Block> KNABBELKOORBLOEM = plaatshouder("knabbelkoorbloem");
    public static final DeferredBlock<Block> KNABBELKOORBLOK = plaatshouder("knabbelkoorblok");
    public static final DeferredBlock<Block> LICHTSTAAFJE = plaatshouder("lichtstaafje");
    public static final DeferredItem<Item> KRISTALKAAS = ITEMS.registerItem("kristalkaas", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> KRISTALKAAS_BROKJE = ITEMS.registerItem("kristalkaas_brokje", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> KNABBELKOORVRUCHT = ITEMS.registerItem("knabbelkoorvrucht", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> GEPOFTE_KNABBELKOORVRUCHT = ITEMS.registerItem("gepofte_knabbelkoorvrucht", Item::new, () -> new Item.Properties());

    /** A plain stand-in block with its item. */
    private static DeferredBlock<Block> plaatshouder(String id) {
        DeferredBlock<Block> block = BLOCKS.registerBlock(id, Block::new, () -> BlockBehaviour.Properties.of().strength(1.5f));
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2BiomesFeature() {
    }
}
