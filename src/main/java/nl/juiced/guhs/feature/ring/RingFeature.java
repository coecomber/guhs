package nl.juiced.guhs.feature.ring;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
 * bbq2 (ring-kern): the Knabbelring: the ring and its effects, Sam-guh, Guhdalf, Smikagol, the cast, the Nine, the gifts, rest points, the portal lock, the travel map.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/ring.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class RingFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredItem<Item> KNABBELRING = ITEMS.registerItem("knabbelring", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> LICHTFLESJE = ITEMS.registerItem("lichtflesje", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> ELFENMANTELTJE = ITEMS.registerItem("elfenmanteltje", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> ELFENTOUW = ITEMS.registerItem("elfentouw", Item::new, () -> new Item.Properties());
    public static final DeferredBlock<Block> ELFENTOUW_HAAK = BLOCKS.registerBlock("elfentouw_haak", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> ELFENTOUW_HAAK_ITEM = ITEMS.registerSimpleBlockItem(ELFENTOUW_HAAK);
    public static final DeferredHolder<EntityType<?>, EntityType<Plaatshouder>> SMIKAGOL = Plaatshouder.type(ENTITY_TYPES, "smikagol");
    public static final DeferredHolder<EntityType<?>, EntityType<Plaatshouder>> KNEKEL_RUITER = Plaatshouder.type(ENTITY_TYPES, "knekel_ruiter");

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNABBELRING.get()));
        output.accept(new ItemStack(LICHTFLESJE.get()));
        output.accept(new ItemStack(ELFENMANTELTJE.get()));
        output.accept(new ItemStack(ELFENTOUW.get()));
        output.accept(new ItemStack(ELFENTOUW_HAAK_ITEM.get()));
    }

    private RingFeature() {
    }
}
