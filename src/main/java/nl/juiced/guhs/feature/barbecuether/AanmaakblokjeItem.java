package nl.juiced.guhs.feature.barbecuether;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * The Aanmaakblokje: a barbecue firelighter. Click it on (the inside of) a grillkool frame in the Guhmensie or the
 * Barbecuether and the barbecue portal flares up. Anywhere else it's just a lighter, like flint and steel.
 */
public class AanmaakblokjeItem extends FlintAndSteelItem {
    public AanmaakblokjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos inside = context.getClickedPos().relative(context.getClickedFace());
        Optional<GrillPortalShape> shape = GrillPortalShape.findEmptyShape(level, inside);
        if (shape.isPresent()) {
            if (GrillPortalForcer.targetDimension(level.dimension()) == null) {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    player.sendOverlayMessage(Component.translatable("quest.guhs.barbecuether.wrong_dimension").withStyle(ChatFormatting.GOLD));
                }
                return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
                light(level, shape.get(), inside, context.getPlayer() instanceof ServerPlayer p ? p : null);
                ItemStack stack = context.getItemInHand();
                if (context.getPlayer() != null) {
                    stack.hurtAndBreak(1, context.getPlayer(), context.getHand().asEquipmentSlot());
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    /** Lights the portal (and tells a Grillguh nearby: his barbecue burns again). */
    public static void light(Level level, GrillPortalShape shape, BlockPos inside, @javax.annotation.Nullable ServerPlayer player) {
        shape.createPortalBlocks();
        level.playSound(null, inside, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0f, 0.8f + level.getRandom().nextFloat() * 0.3f);
        level.playSound(null, inside, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.6f, 1.4f);
        if (player != null) {
            Grillguh.portalLit(player, inside);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.aanmaakblokje.lore").withStyle(ChatFormatting.GRAY));
    }
}
