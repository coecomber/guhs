package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * "Surprise van &lt;maker&gt;": the 7x7 build someone made for you (design R5: "als plaatsbaar object voor de survivalwereld").
 * Hold it: a see-through preview shows where it goes (turn it with R or shift + scroll); right-click puts it down (the
 * client sends the spot, {@link SurprisePlaatsen#plaats} checks it). Shift + right-click on the placed gift makes it this
 * item again. Permanent: it never melts.
 */
public class SurpriseItem extends Item {
    public SurpriseItem(Properties properties) {
        super(properties);
    }

    public static ItemStack maak(SurpriseData d) {
        ItemStack s = new ItemStack(LootjesFeature.SURPRISE_ITEM.get());
        s.set(LootjesFeature.SURPRISE_DATA.get(), d);
        return s;
    }

    @Override
    public Component getName(ItemStack stack) {
        SurpriseData d = stack.get(LootjesFeature.SURPRISE_DATA.get());
        return d == null ? super.getName(stack) : Component.translatable("item.guhs.guhwinter_surprise.van", d.maker());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        SurpriseData d = stack.get(LootjesFeature.SURPRISE_DATA.get());
        if (d == null) {
            builder.accept(Component.translatable("item.guhs.guhwinter_surprise.niks").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!d.voor().isEmpty()) {
            builder.accept(Component.translatable("item.guhs.guhwinter_surprise.voor", d.voor()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (d.leeg()) {
            builder.accept(Component.translatable("item.guhs.guhwinter_surprise.leeg").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        builder.accept(Component.translatable("item.guhs.guhwinter_surprise.blokken", d.aantal()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.guhs.guhwinter_surprise.lore").withStyle(ChatFormatting.GRAY));
    }
}
