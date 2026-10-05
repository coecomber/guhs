package nl.juiced.guhs.feature.techbezorg;

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
 * bbq2 (tech-bezorg): the Bezorgguhtje, its Stepstation, the Haltepaaltjes and the whistle.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/tech_bezorg.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class TechbezorgFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredBlock<Block> STEPSTATION = BLOCKS.registerBlock("stepstation", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> STEPSTATION_ITEM = ITEMS.registerSimpleBlockItem(STEPSTATION);
    public static final DeferredBlock<Block> HALTEPAALTJE = BLOCKS.registerBlock("haltepaaltje", Block::new,
            () -> BlockBehaviour.Properties.of().strength(1.5f));
    public static final DeferredItem<BlockItem> HALTEPAALTJE_ITEM = ITEMS.registerSimpleBlockItem(HALTEPAALTJE);
    public static final DeferredItem<Item> BEZORGGUHTJE_FLUITJE = ITEMS.registerItem("bezorgguhtje_fluitje", Item::new, () -> new Item.Properties());
    public static final DeferredHolder<EntityType<?>, EntityType<Plaatshouder>> BEZORGGUHTJE = Plaatshouder.type(ENTITY_TYPES, "bezorgguhtje");

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(STEPSTATION_ITEM.get()));
        output.accept(new ItemStack(HALTEPAALTJE_ITEM.get()));
        output.accept(new ItemStack(BEZORGGUHTJE_FLUITJE.get()));
    }

    private TechbezorgFeature() {
    }
}
