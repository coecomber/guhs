package nl.juiced.guhs.feature.rozegraan;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HayBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0: the roze graan set of the Zingende Weide. The pink grasses drop roze zaadjes instead of wheat seeds (with shears you
 * take the grass itself along; loot tables: tools/features/rozegraan.py), the seeds grow into {@link RozeGraanBlock}, and the
 * grain becomes a roze broodje (guhs and Blubs adore it: it tames and makes them fall in love, like kaasknabbels), a roze
 * knabbelkoekje (a snack) or a roze hooibaal.
 */
public final class RozegraanFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<RozeGraanBlock> GRAAN_PLANT = BLOCKS.registerBlock("roze_graan_plant", RozeGraanBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<HayBlock> HOOIBAAL = BLOCKS.registerBlock("roze_hooibaal", HayBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).mapColor(MapColor.COLOR_PINK).sound(SoundType.GRASS)
                    .pushReaction(PushReaction.NORMAL));

    public static final DeferredItem<BlockItem> ZAADJES = ITEMS.registerItem("roze_zaadjes",
            p -> new BlockItem(GRAAN_PLANT.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                            TooltipFlag flag) {
                    tooltip.accept(Component.translatable("item.guhs.roze_zaadjes.lore").withStyle(ChatFormatting.GRAY));
                }
            }, () -> new Item.Properties().useItemDescriptionPrefix());
    public static final DeferredItem<Item> GRAAN = lore("roze_graan", new Item.Properties());
    public static final DeferredItem<Item> BROODJE = lore("roze_broodje",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build()));
    public static final DeferredItem<Item> KOEKJE = lore("roze_knabbelkoekje",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).alwaysEdible().build()));
    public static final DeferredItem<BlockItem> HOOIBAAL_ITEM = ITEMS.registerSimpleBlockItem(HOOIBAAL);

    /** A plain item with its lore line (item.guhs.&lt;naam&gt;.lore). */
    private static DeferredItem<Item> lore(String naam, Item.Properties props) {
        return ITEMS.registerItem(naam, p -> new Item(p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                        TooltipFlag flag) {
                tooltip.accept(Component.translatable("item.guhs." + naam + ".lore").withStyle(ChatFormatting.GRAY));
            }
        }, () -> props);
    }

    /** Is this the roze broodje? Guhs and Blubs take it like kaasknabbels (taming, falling in love, a treat). */
    public static boolean isBroodje(ItemStack stack) {
        return !stack.isEmpty() && stack.is(BROODJE.get());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(ZAADJES.get()));
        output.accept(new ItemStack(GRAAN.get()));
        output.accept(new ItemStack(BROODJE.get()));
        output.accept(new ItemStack(KOEKJE.get()));
        output.accept(new ItemStack(HOOIBAAL_ITEM.get()));
    }

    private RozegraanFeature() {
    }
}
