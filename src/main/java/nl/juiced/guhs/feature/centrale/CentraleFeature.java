package nl.juiced.guhs.feature.centrale;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.techbron.LaagDeelBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * 1.8.0 (vadskracht theme, ontwerp-guhs180-vadskracht.md V3/V4):
 * <ul>
 *   <li><b>De Mega-knuffelcentrale</b> ({@link MegaKnuffelcentraleBlock}): the end source of the progression, a cushion of
 *       5 x 3 where sixteen guhs cuddle; up to 352 VK with food and fresh blankets; one per net.</li>
 *   <li><b>Export</b> ({@link Export}): the guh villagers buy the factories' bulk for rare things, new trades every day.</li>
 * </ul>
 * Resources and texts: tools/features/centrale.py.
 */
public final class CentraleFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final DeferredBlock<MegaKnuffelcentraleBlock> CENTRALE = BLOCKS.registerBlock("mega_knuffelcentrale", MegaKnuffelcentraleBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> CENTRALE_ITEM = ITEMS.registerSimpleBlockItem(CENTRALE);
    /** The rest of the cushion (no item): a visible low quilted slab. */
    public static final DeferredBlock<LaagDeelBlock> DEEL = BLOCKS.registerBlock("mega_knuffelcentrale_deel",
            p -> new LaagDeelBlock(p, MegaKnuffelcentraleBlock.HOOGTE, true),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).sound(SoundType.WOOL).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MegaKnuffelcentraleBlockEntity>> CENTRALE_BE = BLOCK_ENTITIES.register(
            "mega_knuffelcentrale", () -> new BlockEntityType<>(MegaKnuffelcentraleBlockEntity::new, CENTRALE.get()));

    private CentraleFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(CentraleFeature::capabilities);
        Export.register();
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(CENTRALE_ITEM.get()));
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        VadskrachtFeature.knoopCapability(event, CENTRALE_BE.get());
        VadskrachtFeature.deelCapabilities(event, DEEL.get());   // (also routes the parts' item capability to the kern)
        event.registerBlockEntity(Capabilities.Item.BLOCK, CENTRALE_BE.get(), MegaKnuffelcentraleBlockEntity::handler);
    }
}
