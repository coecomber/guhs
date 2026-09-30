package nl.juiced.guhs.feature.tuintjes;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * De guh_gieter: a pink watering can with a guh face. Right-click a tuintje: it waters the thirsty plants in a 3 x 3 around
 * it (one sip each); right-click water to fill it up again. The durability bar is the water in it ({@link #VOL} sips).
 */
public class GuhGieterItem extends Item {
    public static final int VOL = 24;

    public GuhGieterItem(Properties properties) {
        super(properties.durability(VOL).setNoRepair());
    }

    public static int water(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!TuinBlock.isTuin(level.getBlockState(pos))) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (water(stack) <= 0) {
            if (player != null && !level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.tuintjes.gieter_leeg").withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int gegoten = giet(level, pos, stack);
        level.playSound(null, pos, TuintjesFeature.GIETER_GELUID.get(), SoundSource.PLAYERS, 1f, 1f);
        if (player instanceof ServerPlayer sp && gegoten > 0) {
            TuintjesVoortgang.gegoten(sp, gegoten);
        }
        return InteractionResult.SUCCESS;
    }

    /** Waters the thirsty plants in the 3 x 3 (and a step up and down) around pos, as long as there is water: returns how many. */
    public static int giet(Level level, BlockPos pos, ItemStack stack) {
        int gegoten = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (water(stack) <= 0) {
                break;
            }
            if (TuinBlock.water(level, p.immutable())) {
                stack.setDamageValue(stack.getDamageValue() + 1);
                gegoten++;
            }
        }
        return gegoten;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            return InteractionResult.PASS;
        }
        if (stack.getDamageValue() == 0) {
            return InteractionResult.PASS;
        }
        stack.setDamageValue(0);
        level.playSound(player, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1f, 1.3f);
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.tuintjes.gieter_vol").withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.guhs.guh_gieter.water", water(stack), VOL).withStyle(ChatFormatting.AQUA));
    }
}
