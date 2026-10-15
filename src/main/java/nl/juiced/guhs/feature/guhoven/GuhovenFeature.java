package nl.juiced.guhs.feature.guhoven;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * De Guhoven (1.2.5): a furnace that needs no fuel, it bakes on vadskracht. It smelts exactly like a vanilla furnace (the same
 * smelting recipes, 200 ticks, XP, hoppers, comparator, lit state, flames and crackle), but only while its vadskracht net
 * runs (bbq2, feature/vadskracht): the oven is a consumer of {@link VadsGetallen#GUH_OVEN} VK, so it needs a real source (a
 * Guhrad with a guh in it...) next to it or joined to it with Guhdraad, and it stands still with everything else when the net
 * asks more than its sources give ({@link #guhKracht}). Ordinary redstone (levers, torches, dust, a redstone block) does
 * nothing. Like every guh machine it has a face ({@code MachineBlock.SNOET}): asleep without vadskracht, awake with it,
 * surprised when the result slot is full. There is no fuel slot: the screen ({@code client.GuhOvenScreen}) shows a little guh
 * wheel there, and hoppers on the sides fill the input slot.
 * tools/features/guhoven.py makes the resources (textures, model, recipe, loot, texts, FTB quest).
 */
public final class GuhovenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Guhs.MODID);

    public static final DeferredBlock<GuhOvenBlock> GUH_OVEN = BLOCKS.registerBlock("guh_oven", GuhOvenBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE).mapColor(MapColor.COLOR_PINK));
    public static final DeferredItem<BlockItem> GUH_OVEN_ITEM = ITEMS.registerItem("guh_oven",
            p -> new BlockItem(GUH_OVEN.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("block.guhs.guh_oven.lore").withStyle(ChatFormatting.GRAY));
                    tooltip.accept(Component.translatable("block.guhs.guh_oven.lore.lief").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }, () -> new Item.Properties());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuhOvenBlockEntity>> GUH_OVEN_BE = BLOCK_ENTITIES.register("guh_oven",
            () -> new BlockEntityType<>(GuhOvenBlockEntity::new, GUH_OVEN.get()));

    public static final DeferredHolder<MenuType<?>, MenuType<GuhOvenMenu>> GUH_OVEN_MENU = MENUS.register("guh_oven",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new GuhOvenMenu(id, inventory)));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(GuhovenFeature::capabilities);
    }

    /** Pipes and hoppers (like a vanilla furnace): in from the top and the sides, out at the bottom. And the oven is a vadskracht knoop. */
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, GUH_OVEN_BE.get(), WorldlyContainerWrapper::new);
        VadskrachtFeature.knoopCapability(event, GUH_OVEN_BE.get());
    }

    /**
     * Does the oven at this spot get vadskracht: did its net run at the last evaluation? (bbq2: this used to look for a running
     * Guhrad or powered Guhdraad next to the oven; now the net decides, {@link VadsKracht#heeftKracht}.)
     */
    public static boolean guhKracht(ServerLevel level, BlockPos pos) {
        return VadsKracht.heeftKracht(level, pos);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GUH_OVEN_ITEM.get()));
    }

    private GuhovenFeature() {
    }
}
