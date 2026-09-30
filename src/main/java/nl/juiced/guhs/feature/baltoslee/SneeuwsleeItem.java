package nl.juiced.guhs.feature.baltoslee;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The sneeuwslee in your pocket (balto gives it at the end of the Nomguh story): right-click the ground to put it down,
 * with its four guh-sledehondjes. Best on snow: that's the only place they run.
 */
public class SneeuwsleeItem extends Item {
    public SneeuwsleeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        float yaw = player == null ? 0 : player.getYRot();
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        SneeuwsleeEntity slee = BaltoSleeFeature.SNEEUWSLEE_ENTITY.get().create(sl);
        if (slee == null) {
            return InteractionResult.FAIL;
        }
        double y = pos.getY() + (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() ? 0 : level.getBlockState(pos).getCollisionShape(level, pos).max(net.minecraft.core.Direction.Axis.Y));
        slee.moveTo(pos.getX() + 0.5, y, pos.getZ() + 0.5, yaw, 0);
        if (!level.noCollision(slee, slee.getBoundingBox().deflate(0.05)) || !level.getEntities(slee, slee.getBoundingBox()).isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("gui.guhs.baltoslee.eigen.geen_plek").withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResult.FAIL;
        }
        slee.zetEigenaar(player == null ? null : player.getUUID());
        sl.addFreshEntity(slee);
        sl.playSound(null, pos, BaltoSleeFeature.WOEF.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        sl.playSound(null, pos, BaltoSleeFeature.BELLEN.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.sneeuwslee.lore").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.guhs.sneeuwslee.lore2").withStyle(ChatFormatting.GRAY));
    }
}
