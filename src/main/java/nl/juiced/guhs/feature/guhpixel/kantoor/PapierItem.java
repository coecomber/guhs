package nl.juiced.guhs.feature.guhpixel.kantoor;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import nl.juiced.guhs.taal.Tekst;

/**
 * A paper of the Guhkantoor as an item: right-click in the air reads it (the paper screen), right-click on a wall hangs
 * it up (the block keeps the paper's data). The tooltip says whose it is.
 */
public class PapierItem extends BlockItem {
    public PapierItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        if (level.isClientSide()) {
            KantoorSlice.papierLezer.accept(Papier.tag(s));
        } else {
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.7f, 1.2f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundTag t = Papier.tag(stack);
        if (t.isEmpty()) {
            tooltip.accept(Component.translatable("gui.guhs.guhkantoor.papier.leeg").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            Component naam = Tekst.get(t, "Naam");
            if (!Tekst.empty(naam)) {
                tooltip.accept(Component.translatable("gui.guhs.guhkantoor.papier.van", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            Component baas = Tekst.get(t, "Baas");
            if (!Tekst.empty(baas)) {
                tooltip.accept(Component.translatable("gui.guhs.guhkantoor.papier.kantoor", baas).withStyle(ChatFormatting.DARK_GRAY));
            }
            if (Papier.soort(t) != Papier.Soort.OORKONDE) {
                tooltip.accept(Component.translatable("gui.guhs.guhkantoor.papier.nr", t.getIntOr("Nr", 1)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }
}
