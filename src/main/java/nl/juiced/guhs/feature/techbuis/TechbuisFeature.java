package nl.juiced.guhs.feature.techbuis;

import java.util.function.Consumer;

import net.minecraft.world.item.BlockItem;
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
 * bbq2 (tech-buizen): Knabbelbuizen with filter and direction piece, the Opzuiger and the sensors.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/tech_buizen.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class TechbuisFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<Block> KNABBELBUIS = BLOCKS.registerBlock("knabbelbuis", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> KNABBELBUIS_ITEM = ITEMS.registerSimpleBlockItem(KNABBELBUIS);
    public static final DeferredBlock<Block> KNABBELBUIS_FILTER = BLOCKS.registerBlock("knabbelbuis_filter", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> KNABBELBUIS_FILTER_ITEM = ITEMS.registerSimpleBlockItem(KNABBELBUIS_FILTER);
    public static final DeferredBlock<Block> KNABBELBUIS_RICHTING = BLOCKS.registerBlock("knabbelbuis_richting", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> KNABBELBUIS_RICHTING_ITEM = ITEMS.registerSimpleBlockItem(KNABBELBUIS_RICHTING);
    public static final DeferredBlock<Block> OPZUIGER = BLOCKS.registerBlock("opzuiger", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> OPZUIGER_ITEM = ITEMS.registerSimpleBlockItem(OPZUIGER);
    public static final DeferredBlock<Block> VOORRAADMETER = BLOCKS.registerBlock("voorraadmeter", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> VOORRAADMETER_ITEM = ITEMS.registerSimpleBlockItem(VOORRAADMETER);
    public static final DeferredBlock<Block> SNUFFELSENSOR = BLOCKS.registerBlock("snuffelsensor", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> SNUFFELSENSOR_ITEM = ITEMS.registerSimpleBlockItem(SNUFFELSENSOR);
    public static final DeferredBlock<Block> GUHKLOK = BLOCKS.registerBlock("guhklok", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> GUHKLOK_ITEM = ITEMS.registerSimpleBlockItem(GUHKLOK);
    public static final DeferredBlock<Block> GUHTELLER = BLOCKS.registerBlock("guhteller", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> GUHTELLER_ITEM = ITEMS.registerSimpleBlockItem(GUHTELLER);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNABBELBUIS_ITEM.get()));
        output.accept(new ItemStack(KNABBELBUIS_FILTER_ITEM.get()));
        output.accept(new ItemStack(KNABBELBUIS_RICHTING_ITEM.get()));
        output.accept(new ItemStack(OPZUIGER_ITEM.get()));
        output.accept(new ItemStack(VOORRAADMETER_ITEM.get()));
        output.accept(new ItemStack(SNUFFELSENSOR_ITEM.get()));
        output.accept(new ItemStack(GUHKLOK_ITEM.get()));
        output.accept(new ItemStack(GUHTELLER_ITEM.get()));
    }

    private TechbuisFeature() {
    }
}
