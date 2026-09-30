package nl.juiced.guhs.feature.vissen;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
/**
 * The Guhvis-hengel: the Visguh lends it to you for a contest (you never need your own rod). It bites fast (like Lure
 * III), never breaks, and it only exists during your contest: it can't be dropped, and outside a contest (after the
 * contest, in a chest, after dying...) it swims straight back to the Visguh.
 */
public class GuhvisHengel extends FishingRodItem {
    /** How much faster than a normal rod something bites (in ticks; Lure III is 15 seconds). */
    public static final int LURE_TICKS = 20 * 15;

    public GuhvisHengel(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && !VisWedstrijd.isFishing(serverPlayer)) {
            stack.setCount(0);
            serverPlayer.sendOverlayMessage(Component.translatable("gui.guhs.vissen.rod_gone").withStyle(ChatFormatting.AQUA));
            return InteractionResult.CONSUME.heldItemTransformedTo(stack);
        }
        if (player.fishing != null) {
            if (!level.isClientSide()) {
                player.fishing.retrieve(stack);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL,
                    1.0f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL,
                    0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
            if (!level.isClientSide()) {
                level.addFreshEntity(player instanceof ServerPlayer sp
                        ? new GuhvisDobber(player, level, 0, LURE_TICKS, VisWedstrijd.niveauVan(sp))   // (the bite window of your level)
                        : new FishingHook(player, level, 0, LURE_TICKS));
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            player.gameEvent(GameEvent.ITEM_INTERACT_START);
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    /** Outside a contest the rod goes back (so it's never kept: not after the game, not after taking it out of a chest). */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot equipSlot) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player && !VisWedstrijd.isFishing(player)) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        if (player instanceof ServerPlayer serverPlayer && VisWedstrijd.isFishing(serverPlayer)) {
            serverPlayer.sendOverlayMessage(Component.translatable("gui.guhs.vissen.no_drop").withStyle(ChatFormatting.AQUA));
        }
        return false;
    }

    /** A rod on the ground (somehow) is gone at once. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.discard();
        }
        return true;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.guhvis_hengel.lore").withStyle(ChatFormatting.GRAY));
    }
}
