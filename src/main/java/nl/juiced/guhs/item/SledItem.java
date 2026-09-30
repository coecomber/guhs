package nl.juiced.guhs.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhSleeEntity;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.slee.SleePath;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** The guh sled as an item: put it on a sled rail. */
public class SledItem extends Item {
    public SledItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        SleePath.Piece piece = SleePath.Piece.of(level, context.getClickedPos());
        if (piece == null) {
            if (level.isClientSide() && context.getPlayer() != null) {
                context.getPlayer().sendOverlayMessage(Component.translatable("item.guhs.guh_slee.needs_rail").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            GuhSleeEntity sled = ModEntities.GUH_SLEE.get().create(level, EntitySpawnReason.TRIGGERED);
            if (sled == null) {
                return InteractionResult.FAIL;
            }
            Player player = context.getPlayer();
            Vec3 look = player != null ? player.getLookAngle() : new Vec3(0, 0, 1);
            sled.snapTo(context.getClickLocation());
            sled.putOn(piece, context.getClickLocation(), look);
            level.addFreshEntity(sled);
            level.playSound(null, sled.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1f, 1.5f);
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.guh_slee.lore").withStyle(ChatFormatting.GRAY));
    }
}
