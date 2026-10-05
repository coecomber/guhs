package nl.juiced.guhs.feature.techquest;

import java.util.function.Consumer;

import net.minecraft.world.item.BlockItem;
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
 * bbq2 (tech-quests): the Oude Guhrad-centrale, the uitvinder-guh's questline and practice hall, the FTB chapter Guh-technologie, De Grote Knabbelmachine.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/tech_quests.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class TechquestFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<Block> GROTE_KNABBELMACHINE = BLOCKS.registerBlock("grote_knabbelmachine", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> GROTE_KNABBELMACHINE_ITEM = ITEMS.registerSimpleBlockItem(GROTE_KNABBELMACHINE);
    public static final DeferredItem<Item> PERFECTE_KNABBEL = ITEMS.registerItem("perfecte_knabbel", Item::new, () -> new Item.Properties());
    public static final DeferredBlock<Block> KNABBELMACHINE_BEELDJE = BLOCKS.registerBlock("knabbelmachine_beeldje", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> KNABBELMACHINE_BEELDJE_ITEM = ITEMS.registerSimpleBlockItem(KNABBELMACHINE_BEELDJE);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GROTE_KNABBELMACHINE_ITEM.get()));
        output.accept(new ItemStack(PERFECTE_KNABBEL.get()));
        output.accept(new ItemStack(KNABBELMACHINE_BEELDJE_ITEM.get()));
    }

    private TechquestFeature() {
    }
}
