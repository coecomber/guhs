package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
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
import nl.juiced.guhs.feature.einde2.Einde2;
import nl.juiced.guhs.feature.einde2.Einde2Plaatshouder;

/**
 * einde2, slice sponsguh_wezens: the cast of the Sponsguh story as creatures, the Knabbelburgermobiel and the story's pure-content rewards (food, blocks, a toy, outfits).
 * Skeleton stub: the slice fills this class in place (it is already wired in feature/einde2/Einde2).
 * Resources: tools/features/einde2_sponsguh_wezens.py.
 */
public final class E2SponsguhFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    // --- the ids the two builds of the story place (build contract 3.4): plain stand-in blocks and items until this slice makes the real ones ---
    public static final DeferredBlock<Block> KAREN = plaatshouder("karen");
    public static final DeferredBlock<Block> KROKANTE_GRILL = plaatshouder("krokante_grill");
    public static final DeferredBlock<Block> BESTELTAFEL = plaatshouder("besteltafel");
    public static final DeferredBlock<Block> VRUCHTHUIS_WAND = plaatshouder("vruchthuis_wand");
    public static final DeferredBlock<Block> VRUCHTHUIS_DAK = plaatshouder("vruchthuis_dak");
    public static final DeferredBlock<Block> VRUCHTHUIS_RAAM = plaatshouder("vruchthuis_raam");
    public static final DeferredBlock<Block> VRUCHTHUIS_DEUR = plaatshouder("vruchthuis_deur");
    public static final DeferredBlock<Block> BELLENBLAAS = plaatshouder("bellenblaas");
    public static final DeferredBlock<Block> SPOOKLANTAARN = plaatshouder("spooklantaarn");
    public static final DeferredItem<Item> KNABBELBURGER = ITEMS.registerItem("knabbelburger", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> KWALLENJAM = ITEMS.registerItem("kwallenjam", Item::new, () -> new Item.Properties());
    // --- the buddy and the pet (their Guhdex pages): stand-ins until this slice makes the real creatures; the rest of the cast is the slice's own ---
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> SPONSGUH = Einde2.plaatshouder(ENTITY_TYPES, "sponsguh");
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> GERRIT = Einde2.plaatshouder(ENTITY_TYPES, "gerrit");

    /** A plain stand-in block with its item. */
    private static DeferredBlock<Block> plaatshouder(String id) {
        DeferredBlock<Block> block = BLOCKS.registerBlock(id, Block::new, () -> BlockBehaviour.Properties.of().strength(1.5f));
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2SponsguhFeature() {
    }
}
