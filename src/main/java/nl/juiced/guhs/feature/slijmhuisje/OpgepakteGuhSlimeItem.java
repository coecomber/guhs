package nl.juiced.guhs.feature.slijmhuisje;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.GuhSlimeEntity;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.storage.Nbt;

/**
 * "Opgepakte guh-slime": a guh slime in your pockets (sneak + right-click one with an empty hand, any size). The whole slime
 * (name, size, health...) rides along in the item's custom data, like the vanilla bucket of axolotl, plus its size under
 * {@link #GROOTTE} for the tooltip and the Slijmhuisje. Right-click a block: it bounces down there, exactly as it was.
 * Right-click a {@link SlijmhuisjeBlock}: it moves in.
 * <p>
 * The pick-up is a NeoForge event ({@link #oppakken}), so {@link GuhSlimeEntity} itself needs no change. A plain right-click
 * (not sneaking) is left alone: that belongs to the slime itself.
 */
public class OpgepakteGuhSlimeItem extends Item {
    /** The slime's size (1, 2, 4) in the item's custom data. */
    public static final String GROOTTE = "guhs_slijm_grootte";

    public OpgepakteGuhSlimeItem(Properties properties) {
        super(properties);
    }

    // --- picking up --------------------------------------------------------------------------------------------------------------

    /** The item holding this slime (the slime stays in the world: the caller removes it). */
    public static ItemStack van(GuhSlimeEntity slijm) {
        CompoundTag tag = new CompoundTag();
        Nbt.saveWithoutId(slijm, tag);
        tag.remove("UUID");
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("leash");
        tag.remove("Leash");
        tag.putInt(GROOTTE, slijm.getSize());
        ItemStack stack = new ItemStack(SlijmhuisjeFeature.OPGEPAKTE_GUH_SLIME.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (slijm.hasCustomName()) {
            stack.set(DataComponents.CUSTOM_NAME, slijm.getCustomName());
        }
        return stack;
    }

    /** The slime's size in this item (1 when it says nothing). */
    public static int grootte(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        int g = data == null ? 1 : data.copyTag().getIntOr(GROOTTE, 1);
        return g <= 1 ? 1 : g <= 2 ? 2 : 4;
    }

    /** (game bus) Sneak + right-click a guh slime with an empty hand: into your hand it goes. */
    public static void oppakken(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!(event.getTarget() instanceof GuhSlimeEntity slijm) || !player.isShiftKeyDown() || event.getHand() != InteractionHand.MAIN_HAND
                || !player.getMainHandItem().isEmpty() || !slijm.isAlive()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(player instanceof ServerPlayer speler)) {
            return;
        }
        ItemStack stack = van(slijm);
        slijm.level().playSound(null, slijm.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.8f, 1.4f);
        slijm.discard();
        speler.setItemInHand(InteractionHand.MAIN_HAND, stack);
        speler.sendOverlayMessage(Component.translatable("gui.guhs.slijmhuisje.opgepakt", stack.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // --- putting down ------------------------------------------------------------------------------------------------------------

    /** A fresh slime from an item (not yet in the world). */
    @Nullable
    public static GuhSlimeEntity naar(ItemStack stack, Level level) {
        GuhSlimeEntity slijm = ModEntities.GUH_SLIME.get().create(level, EntitySpawnReason.TRIGGERED);
        if (slijm == null) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            Nbt.load(slijm, data.copyTag());
        }
        if (slijm.getSize() != grootte(stack)) {
            slijm.setSize(grootte(stack), false);
        }
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            slijm.setCustomName(stack.getHoverName());
        }
        slijm.setPersistenceRequired();
        return slijm;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(context.getClickedFace());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        GuhSlimeEntity slijm = naar(stack, level);
        if (slijm == null) {
            return InteractionResult.FAIL;
        }
        float yaw = player == null ? 0 : player.getYRot() + 180;
        slijm.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        slijm.setDeltaMovement(Vec3.ZERO);
        slijm.fallDistance = 0;
        level.addFreshEntity(slijm);
        level.playSound(null, pos, SoundEvents.SLIME_JUMP_SMALL, SoundSource.NEUTRAL, 0.8f, 1.2f);
        stack.shrink(1);
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.slijmhuisje.neergezet", slijm.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            tooltip.accept(Component.translatable(getDescriptionId()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.accept(Component.translatable("item.guhs.opgepakte_guh_slime.grootte." + grootte(stack)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.guhs.opgepakte_guh_slime.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
