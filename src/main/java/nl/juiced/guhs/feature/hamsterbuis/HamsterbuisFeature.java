package nl.juiced.guhs.feature.hamsterbuis;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0 (Dromen en vadskracht): the hamsterbuis network, the one teleport that works across every dimension.
 * <ul>
 *   <li>{@link Knooppunt} ({@code guhs:hamsterknooppunt}): the owner's main station, on vadskracht; it fills the owner's
 *       buffer ({@link HamsterNet}). No knooppunt, or an empty buffer, and the owner's network stands still.</li>
 *   <li>{@link Station} ({@code guhs:hamsterbuis_station}): put it anywhere (needs no vadskracht), step into the tube and
 *       pick where to go.</li>
 * </ul>
 * Balance (design choice): a trip costs {@link #REIS} VK from the DESTINATION owner's buffer (their network carries you),
 * cheaper than a Thuispoort jump (200+) because the stations are many and simple; the buffer holds {@link #BUFFER} VK (40
 * trips). The Reisguhs stay the free way to travel inside the Guhmension.
 */
public final class HamsterbuisFeature {
    /** What one trip costs (VK) and what the owner's buffer holds (VK). */
    public static final int REIS = 100;
    public static final long BUFFER = 4000;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final DeferredBlock<Knooppunt.Block> KNOOPPUNT = BLOCKS.registerBlock("hamsterknooppunt", Knooppunt.Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.5f, 6f).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK).noOcclusion().lightLevel(s -> 6));
    public static final DeferredItem<Item> KNOOPPUNT_ITEM = ITEMS.registerItem("hamsterknooppunt",
            p -> lore(KNOOPPUNT, "block.guhs.hamsterknooppunt.lore", p), () -> new Item.Properties().rarity(Rarity.RARE).useBlockDescriptionPrefix());
    public static final DeferredBlock<Station.BlockImpl> STATION = BLOCKS.registerBlock("hamsterbuis_station", Station.BlockImpl::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).sound(SoundType.GLASS)
                    .pushReaction(PushReaction.BLOCK).noOcclusion().lightLevel(s -> 4));
    public static final DeferredItem<Item> STATION_ITEM = ITEMS.registerItem("hamsterbuis_station",
            p -> lore(STATION, "block.guhs.hamsterbuis_station.lore", p), () -> new Item.Properties().useBlockDescriptionPrefix());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Knooppunt.BlockEntityImpl>> KNOOPPUNT_BE = BLOCK_ENTITIES.register(
            "hamsterknooppunt", () -> new BlockEntityType<>(Knooppunt.BlockEntityImpl::new, KNOOPPUNT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Station.BlockEntityImpl>> STATION_BE = BLOCK_ENTITIES.register(
            "hamsterbuis_station", () -> new BlockEntityType<>(Station.BlockEntityImpl::new, STATION.get()));

    private HamsterbuisFeature() {
    }

    private static Item lore(DeferredBlock<?> blok, String sleutel, Item.Properties p) {
        return new BlockItem(blok.get(), p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                tooltip.accept(Component.translatable(sleutel, REIS, BUFFER).withStyle(ChatFormatting.GRAY));
            }
        };
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
        HamsterPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNOOPPUNT_ITEM.get()));
        output.accept(new ItemStack(STATION_ITEM.get()));
    }
}
