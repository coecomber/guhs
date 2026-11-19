package nl.juiced.guhs.feature.guhwinter.kern;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/** 1.10.10 Guhwinter kern: the pepernoot and the tags (resources: tools/features/guhwinter_kern.py). */
public final class KernRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** Items with this tag melt away (with a message) from 3 Jan: inventories, containers players open, items on the ground. */
    public static final TagKey<Item> SMELT = TagKey.create(Registries.ITEM, Guhs.id("guhwinter_smelt"));
    /** Placed blocks with this tag melt away from 3 Jan when they are near a player or in a chunk that loads. */
    public static final TagKey<Block> SMELT_BLOK = TagKey.create(Registries.BLOCK, Guhs.id("guhwinter_smelt_blok"));
    /** Items that may be used on blocks inside the Winterplein (fireworks; slices add theirs). */
    public static final TagKey<Item> PLEIN_MAG = TagKey.create(Registries.ITEM, Guhs.id("guhwinter_plein_mag"));

    /** The pepernoot: a little kruidig koekje, the currency of the Winterplein. Quick to eat, always edible. */
    public static final DeferredItem<Item> PEPERNOOT = ITEMS.registerItem("guhwinter_pepernoot", Pepernoot::new, () -> new Item.Properties()
            .food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2f).alwaysEdible().build(),
                    Consumables.defaultFood().consumeSeconds(0.6f).build()));

    private KernRegistry() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(PEPERNOOT.get()));
    }

    /** The pepernoot item (its tooltip). */
    public static final class Pepernoot extends Item {
        public Pepernoot(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.guhwinter_pepernoot.lore").withStyle(ChatFormatting.GOLD));
        }
    }
}
