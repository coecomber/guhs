package nl.juiced.guhs.feature.timmerguh;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.beroepen.BeroepenFeature;

/**
 * A dakpluisje, loaned by the Timmerguh: right-click a see-through ghost tile of his roof ({@link DakplekBlock}) and that bit
 * of the oortjesdak is on (tik tik, pluf!). It is no block item, so the Knuffeldal town protection lets it through; anywhere
 * else it says "die past alleen op het dak van de Timmerguh". Leftovers go back to him when the roof is done.
 */
public class DakpluisjeItem extends Item {
    public DakpluisjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(TimmerguhFeature.DAKPLEK.get())) {
            if (!level.isClientSide && context.getPlayer() instanceof ServerPlayer p) {
                p.displayClientMessage(Component.translatable("gui.guhs.timmerguh.past_niet").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide && context.getPlayer() instanceof ServerPlayer p) {
            leg((ServerLevel) level, pos, p, context.getItemInHand());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Lays the bit of roof on the ghost tile at pos (one dakpluisje from the stack): true when it worked. */
    public static boolean leg(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack stack) {
        BlockState plek = level.getBlockState(pos);
        if (!plek.is(TimmerguhFeature.DAKPLEK.get()) || stack.isEmpty() || !stack.is(TimmerguhFeature.DAKPLUISJE.get())) {
            return false;
        }
        BlockState gelegd = plek.getValue(DakplekBlock.DEEL).gelegd();
        level.setBlock(pos, gelegd, 3);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, BeroepenFeature.HAMER.get(), SoundSource.BLOCKS, 0.9f, 1.05f + level.random.nextFloat() * 0.2f);
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, gelegd), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                8, 0.3, 0.1, 0.3, 0.05);
        Timmerguh.gelegd(level, pos, player);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.timmerguh_dakpluisje.lore").withStyle(ChatFormatting.GRAY));
    }
}
