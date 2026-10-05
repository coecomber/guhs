package nl.juiced.guhs.feature.bank;

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
 * bbq2 (bank): the Bank Guh cap and upgrade, the Hapluikje and its link key.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/bank.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class BankFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<Block> HAPLUIKJE = BLOCKS.registerBlock("hapluikje", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> HAPLUIKJE_ITEM = ITEMS.registerSimpleBlockItem(HAPLUIKJE);
    public static final DeferredItem<Item> BANK_SLEUTEL = ITEMS.registerItem("bank_sleutel", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> BANK_UPGRADE = ITEMS.registerItem("bank_upgrade", Item::new, () -> new Item.Properties());

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(HAPLUIKJE_ITEM.get()));
        output.accept(new ItemStack(BANK_SLEUTEL.get()));
        output.accept(new ItemStack(BANK_UPGRADE.get()));
    }

    private BankFeature() {
    }
}
