package nl.juiced.guhs.feature.blub.knuffel;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import nl.juiced.guhs.feature.blub.BlubTrekking;

/** 1.8.0: the Blub-knuffel item: its tooltip tells whose plush it is (name, soort, tint, meter, size). */
public class BlubKnuffelItem extends BlockItem {
    public BlubKnuffelItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundTag blub = BlubKnuffelBlockEntity.lees(stack);
        if (blub.isEmpty()) {
            tooltip.accept(Component.translatable("item.guhs.blub_knuffel.leeg").withStyle(ChatFormatting.GRAY));
            return;
        }
        Component naam = nl.juiced.guhs.taal.Tekst.get(blub, "Naam");
        if (!naam.getString().isEmpty()) {
            tooltip.accept(Component.translatable("item.guhs.blub_knuffel.van", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        BlubTrekking.Soort soort = BlubTrekking.Soort.van(blub.getStringOr("BlubSoort", ""));
        int tint = blub.getIntOr("Tint", 0);
        tooltip.accept(Component.translatable("gui.guhs.blubguh.menu.soort_tint", Component.translatable("gui.guhs.blubguh.soort." + soort.id()),
                Component.translatable("gui.guhs.blubguh.tint." + soort.id() + "." + tint)).withStyle(ChatFormatting.GRAY));
        int meter = blub.getIntOr("Meter", BlubTrekking.METER_MIDDEN);
        tooltip.accept(Component.translatable("gui.guhs.blubguh.menu.meter", BlubTrekking.metertje(meter), meter).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.blub_knuffel.grootte", blub.getIntOr("Grootte", 100)).withStyle(ChatFormatting.GRAY));
    }
}
