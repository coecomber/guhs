package nl.juiced.guhs.feature.techbron;

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
 * bbq2 (tech-bronnen): the vadskracht sources: Guhrad per variant, Knuffelgenerator, Disco-dynamo, Blubkacheltje, Gloeisterkern, Knabbelbatterij.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/tech_bronnen.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class TechbronFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<Block> KNUFFELGENERATOR = BLOCKS.registerBlock("knuffelgenerator", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> KNUFFELGENERATOR_ITEM = ITEMS.registerSimpleBlockItem(KNUFFELGENERATOR);
    public static final DeferredBlock<Block> DISCO_DYNAMO = BLOCKS.registerBlock("disco_dynamo", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> DISCO_DYNAMO_ITEM = ITEMS.registerSimpleBlockItem(DISCO_DYNAMO);
    public static final DeferredBlock<Block> BLUBKACHELTJE = BLOCKS.registerBlock("blubkacheltje", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> BLUBKACHELTJE_ITEM = ITEMS.registerSimpleBlockItem(BLUBKACHELTJE);
    public static final DeferredBlock<Block> GLOEISTERKERN = BLOCKS.registerBlock("gloeisterkern", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> GLOEISTERKERN_ITEM = ITEMS.registerSimpleBlockItem(GLOEISTERKERN);
    public static final DeferredBlock<Block> KNABBELBATTERIJ = BLOCKS.registerBlock("knabbelbatterij", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> KNABBELBATTERIJ_ITEM = ITEMS.registerSimpleBlockItem(KNABBELBATTERIJ);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNUFFELGENERATOR_ITEM.get()));
        output.accept(new ItemStack(DISCO_DYNAMO_ITEM.get()));
        output.accept(new ItemStack(BLUBKACHELTJE_ITEM.get()));
        output.accept(new ItemStack(GLOEISTERKERN_ITEM.get()));
        output.accept(new ItemStack(KNABBELBATTERIJ_ITEM.get()));
    }

    private TechbronFeature() {
    }
}
