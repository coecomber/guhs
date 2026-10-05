package nl.juiced.guhs.feature.techmachine;

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
 * bbq2 (tech-machines): Oogster, Knabbelaar, Neerzetter, Knutselmachine + Tekentafel + Bouwtekening, Plantagebak, the powered molen.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/tech_machines.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class TechmachineFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<Block> OOGSTER = BLOCKS.registerBlock("oogster", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> OOGSTER_ITEM = ITEMS.registerSimpleBlockItem(OOGSTER);
    public static final DeferredBlock<Block> KNABBELAAR = BLOCKS.registerBlock("knabbelaar", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> KNABBELAAR_ITEM = ITEMS.registerSimpleBlockItem(KNABBELAAR);
    public static final DeferredBlock<Block> NEERZETTER = BLOCKS.registerBlock("neerzetter", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> NEERZETTER_ITEM = ITEMS.registerSimpleBlockItem(NEERZETTER);
    public static final DeferredBlock<Block> KNUTSELMACHINE = BLOCKS.registerBlock("knutselmachine", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> KNUTSELMACHINE_ITEM = ITEMS.registerSimpleBlockItem(KNUTSELMACHINE);
    public static final DeferredBlock<Block> TEKENTAFEL = BLOCKS.registerBlock("tekentafel", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> TEKENTAFEL_ITEM = ITEMS.registerSimpleBlockItem(TEKENTAFEL);
    public static final DeferredBlock<Block> PLANTAGEBAK = BLOCKS.registerBlock("plantagebak", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> PLANTAGEBAK_ITEM = ITEMS.registerSimpleBlockItem(PLANTAGEBAK);
    public static final DeferredBlock<Block> VADSMOLEN = BLOCKS.registerBlock("vadsmolen", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> VADSMOLEN_ITEM = ITEMS.registerSimpleBlockItem(VADSMOLEN);
    public static final DeferredItem<Item> BOUWTEKENING = ITEMS.registerItem("bouwtekening", Item::new, () -> new Item.Properties());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(OOGSTER_ITEM.get()));
        output.accept(new ItemStack(KNABBELAAR_ITEM.get()));
        output.accept(new ItemStack(NEERZETTER_ITEM.get()));
        output.accept(new ItemStack(KNUTSELMACHINE_ITEM.get()));
        output.accept(new ItemStack(TEKENTAFEL_ITEM.get()));
        output.accept(new ItemStack(PLANTAGEBAK_ITEM.get()));
        output.accept(new ItemStack(VADSMOLEN_ITEM.get()));
        output.accept(new ItemStack(BOUWTEKENING.get()));
    }

    private TechmachineFeature() {
    }
}
