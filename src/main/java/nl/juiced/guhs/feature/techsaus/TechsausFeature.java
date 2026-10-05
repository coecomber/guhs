package nl.juiced.guhs.feature.techsaus;

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
 * bbq2 (tech-vloeistof): the pump, Sausslang and Sausvat, the automatic brouwketel and frituur, the Grillkoolpers.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/tech_vloeistof.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class TechsausFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<Block> SAUSPOMP = BLOCKS.registerBlock("sauspomp", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> SAUSPOMP_ITEM = ITEMS.registerSimpleBlockItem(SAUSPOMP);
    public static final DeferredBlock<Block> SAUSSLANG = BLOCKS.registerBlock("sausslang", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> SAUSSLANG_ITEM = ITEMS.registerSimpleBlockItem(SAUSSLANG);
    public static final DeferredBlock<Block> SAUSVAT = BLOCKS.registerBlock("sausvat", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> SAUSVAT_ITEM = ITEMS.registerSimpleBlockItem(SAUSVAT);
    public static final DeferredBlock<Block> BROUWAUTOMAAT = BLOCKS.registerBlock("brouwautomaat", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> BROUWAUTOMAAT_ITEM = ITEMS.registerSimpleBlockItem(BROUWAUTOMAAT);
    public static final DeferredBlock<Block> FRITUURAUTOMAAT = BLOCKS.registerBlock("frituurautomaat", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> FRITUURAUTOMAAT_ITEM = ITEMS.registerSimpleBlockItem(FRITUURAUTOMAAT);
    public static final DeferredBlock<Block> GRILLKOOLPERS = BLOCKS.registerBlock("grillkoolpers", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> GRILLKOOLPERS_ITEM = ITEMS.registerSimpleBlockItem(GRILLKOOLPERS);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(SAUSPOMP_ITEM.get()));
        output.accept(new ItemStack(SAUSSLANG_ITEM.get()));
        output.accept(new ItemStack(SAUSVAT_ITEM.get()));
        output.accept(new ItemStack(BROUWAUTOMAAT_ITEM.get()));
        output.accept(new ItemStack(FRITUURAUTOMAAT_ITEM.get()));
        output.accept(new ItemStack(GRILLKOOLPERS_ITEM.get()));
    }

    private TechsausFeature() {
    }
}
