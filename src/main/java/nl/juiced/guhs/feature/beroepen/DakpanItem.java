package nl.juiced.guhs.feature.beroepen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * A dakpan (loaned by Bob de Guhbouwer): it only fits on a ghost tile of Bob's roof ({@link DakplekBlock}); right-click
 * one and the tile is laid (tik tik with the hammer). Anywhere else: "die past alleen op Bob's dak".
 */
public class DakpanItem extends BlockItem {
    public DakpanItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(BeroepenFeature.DAKPLEK.get())) {
            if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer p) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.bouw.past_niet").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer p) {
            leg((ServerLevel) level, pos, p, context.getItemInHand());
        }
        return InteractionResult.SUCCESS;
    }

    /** Lays a dakpan on the ghost tile at pos (one from the stack): true when it worked. */
    public static boolean leg(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack stack) {
        if (!level.getBlockState(pos).is(BeroepenFeature.DAKPLEK.get()) || stack.isEmpty()) {
            return false;
        }
        level.setBlock(pos, BeroepenFeature.DAKPAN.get().defaultBlockState(), 3);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, BeroepenFeature.HAMER.get(), SoundSource.BLOCKS, 1.0f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, BeroepenFeature.DAKPAN.get().defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.05);
        Bouw.gelegd(level, pos, player);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.beroepen_dakpan.lore").withStyle(ChatFormatting.GRAY));
    }
}
