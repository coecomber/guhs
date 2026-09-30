package nl.juiced.guhs.feature.knuffelbad;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A rubber duck: put it on the water (or anywhere) and it floats there, bobbing and squeaking when you poke it. */
public class BadeendjeItem extends Item {
    public BadeendjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Vec3 at = context.getClickLocation();
        if (!level.isClientSide) {
            zet((ServerLevel) level, new Vec3(at.x, context.getClickedPos().getY() + 1.0, at.z), context.getPlayer(), context.getItemInHand());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).isSource()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            BlockPos p = hit.getBlockPos();
            zet((ServerLevel) level, new Vec3(hit.getLocation().x, p.getY() + 0.9, hit.getLocation().z), player, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void zet(ServerLevel level, Vec3 at, Player player, ItemStack stack) {
        BadeendjeEntity duck = KnuffelbadFeature.BADEENDJE.get().create(level);
        if (duck == null) {
            return;
        }
        duck.moveTo(at.x, at.y, at.z, player == null ? 0 : player.getYRot() + 180f, 0);
        level.addFreshEntity(duck);
        level.playSound(null, duck.blockPosition(), KnuffelbadFeature.EENDJE_PIEP.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.knuffelbad_badeendje.lore").withStyle(ChatFormatting.GRAY));
    }
}
