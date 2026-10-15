package nl.juiced.guhs.feature.techsaus;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * A sauce block in your pockets: a grey line that says what it is for ({@code block.guhs.<id>.lore}), and for a Sausvat
 * that was broken with sauce in it, what it still holds ({@link TechsausFeature#INHOUD}).
 */
public class SausBlokItem extends BlockItem {
    public SausBlokItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        SimpleFluidContent inhoud = stack.getOrDefault(TechsausFeature.INHOUD.get(), SimpleFluidContent.EMPTY);
        if (!inhoud.isEmpty()) {
            tooltip.accept(Component.translatable(SausTekst.K + "vat.item", SausTekst.emmers(inhoud.getAmount()),
                    SausTekst.naam(FluidResource.of(inhoud.copy()))).withStyle(ChatFormatting.YELLOW));
        }
    }
}
