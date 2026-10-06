package nl.juiced.guhs.feature.paleizen;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A Worstzwijntje in a basket (bbq2): the reward of the Stalknecht-guh, two per player. Right-click the ground and a young
 * Worstzwijntje hops out that is yours to keep: a farm animal ({@link WorstzwijntjeEntity}) that never despawns.
 */
public class MandjeItem extends Item {
    public MandjeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        WorstzwijntjeEntity z = laatLos(server, pos, context.getRotation());
        if (z == null) {
            if (context.getPlayer() instanceof ServerPlayer p) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.paleizen.mandje.geen_plek").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.FAIL;
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        if (context.getPlayer() instanceof ServerPlayer p) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.paleizen.mandje.los").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    /** A young Worstzwijntje of your own at this spot (null: no room there). */
    public static WorstzwijntjeEntity laatLos(ServerLevel level, BlockPos pos, float yaw) {
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return null;
        }
        WorstzwijntjeEntity z = PaleizenFeature.WORSTZWIJNTJE.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (z == null) {
            return null;
        }
        z.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw + 180f, 0f);
        z.setBaby(true);
        z.setPersistenceRequired();
        level.addFreshEntity(z);
        level.sendParticles(ParticleTypes.HEART, z.getX(), z.getY() + 0.8, z.getZ(), 5, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, z, PaleizenFeature.KNOR.get(), SoundSource.NEUTRAL, 1f, 1.35f);
        return z;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.paleizen_worstzwijntje_mandje.lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.paleizen_worstzwijntje_mandje.lore2").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
