package nl.juiced.guhs.feature.ringh5;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.wereld.Plaatshouder;

/**
 * bbq2 (ring-h5): chapter 5 of the Knabbelring: De Zwarte Roosterpoort.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/ring_h5.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class RingH5Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Plaatshouder>> OOG_VAN_SAUSRON = Plaatshouder.type(ENTITY_TYPES, "oog_van_sausron");
    public static final DeferredBlock<Block> OOG_VAN_SAUSRON_BEELDJE = BLOCKS.registerBlock("oog_van_sausron_beeldje", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> OOG_VAN_SAUSRON_BEELDJE_ITEM = ITEMS.registerSimpleBlockItem(OOG_VAN_SAUSRON_BEELDJE);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(OOG_VAN_SAUSRON_BEELDJE_ITEM.get()));
    }

    private RingH5Feature() {
    }
}
