package nl.juiced.guhs.feature.einde2.bouwbikinibodem;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice bouw_bikinibodem: the build of Bikinibodem onder de leegte (the town under its soap-bubble dome, deep between the islands).
 * Skeleton stub: the slice fills this class in place (it is already wired in feature/einde2/Einde2).
 * Resources: tools/features/einde2_bouw_bikinibodem.py.
 */
public final class E2BouwBikinibodemFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    // --- the ids the build of Schelpenstad may place too (build contract 3.4): plain stand-in blocks until this slice makes the real ones ---
    public static final DeferredBlock<Block> ZEEPBEL = plaatshouder("zeepbel");
    public static final DeferredBlock<Block> BUBBELSTROOM = plaatshouder("bubbelstroom");
    public static final DeferredBlock<Block> BLOEMWOLK = plaatshouder("bloemwolk");

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

    private E2BouwBikinibodemFeature() {
    }
}
