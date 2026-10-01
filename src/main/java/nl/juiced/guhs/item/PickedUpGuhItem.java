package nl.juiced.guhs.item;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.registry.ModDataComponents;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * A tamed guh you picked up (sneak + right-click it). Right-click a block to put it down again, or right-click a
 * Guh Wheel with it to let it run. Keeps everything: name, size, health, saddle, owner...
 */
public class PickedUpGuhItem extends Item {
    public PickedUpGuhItem(Properties properties) {
        super(properties);
    }

    /** Takes the guh out of the world and returns it as an item. */
    public static ItemStack pickUp(GuhEntity guh) {
        CompoundTag tag = nl.juiced.guhs.storage.Nbt.saveWithoutId(guh);
        tag.putString("id", EntityType.getKey(guh.getType()).toString());
        tag.putBoolean("Sitting", false);
        if (guh.hasCustomName()) {
            nl.juiced.guhs.taal.Tekst.put(tag, "GuhDisplayName", guh.getCustomName());   // (1.2.0: the name itself, any language)
        }
        guh.discard();
        ItemStack stack = of(tag);
        if (!guh.level().isClientSide()) {   // 2.10: "waar is mijn guh": in its owner's pockets now
            nl.juiced.guhs.feature.band.GuhVolger.item(stack, nl.juiced.guhs.feature.band.PlekSoort.ITEM_SPELER, guh.level().dimension(), guh.blockPosition(),
                    guh.getOwner() instanceof net.minecraft.world.entity.player.Player owner ? owner.getGameProfile().name() : "", guh.level().getGameTime());
        }
        return stack;
    }

    /** 2.10: "waar is mijn guh": in someone's pockets (checked every 5 seconds). */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, Entity entity, @javax.annotation.Nullable net.minecraft.world.entity.EquipmentSlot slot) {
        // 26.1: server only, no slot index any more (1.21.1 spread the checks over the slots with gameTime + slot)
        if (entity instanceof net.minecraft.world.entity.player.Player holder && level.getGameTime() % 100 == 0) {
            nl.juiced.guhs.feature.band.GuhVolger.inZakken(stack, holder);
        }
    }

    /** 2.10: "waar is mijn guh": dropped on the ground. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, net.minecraft.world.entity.item.ItemEntity entity) {
        if (!entity.level().isClientSide() && entity.tickCount % 100 == 1) {
            nl.juiced.guhs.feature.band.GuhVolger.item(stack, nl.juiced.guhs.feature.band.PlekSoort.ITEM_GROND, entity.level().dimension(), entity.blockPosition(), "", entity.level().getGameTime());
        }
        return false;
    }

    public static ItemStack of(CompoundTag guhData) {
        ItemStack stack = new ItemStack(ModItems.PICKED_UP_GUH.get());
        stack.set(ModDataComponents.GUH_DATA.get(), CustomData.of(guhData));
        return stack;
    }

    public static CompoundTag guhData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.GUH_DATA.get(), CustomData.EMPTY).copyTag();
    }

    /** Puts the guh back into the world at the given spot. Returns the entity, or null if the item was empty. */
    public static Entity release(Level level, CompoundTag data, double x, double y, double z, float yaw) {
        if (data.isEmpty()) {
            return null;
        }
        Entity entity = EntityType.loadEntityRecursive(data, level, net.minecraft.world.entity.EntitySpawnReason.LOAD, e -> {
            e.snapTo(x, y, z, yaw, 0);
            return e;
        });
        if (entity != null) {
            level.addFreshEntity(entity);
        }
        return entity;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Entity guh = release(level, guhData(context.getItemInHand()), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                context.getRotation() + 180f);
        if (guh != null) {
            level.playSound(null, pos, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1f);
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag data = guhData(stack);
        if (data.contains("GuhDisplayName")) {
            return Component.translatable("item.guhs.picked_up_guh.named", nl.juiced.guhs.taal.Tekst.get(data, "GuhDisplayName"));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundTag data = guhData(stack);
        double scale = 1.0;
        double maxHealth = 25;
        ListTag attributes = data.getListOrEmpty("attributes");
        for (int i = 0; i < attributes.size(); i++) {
            CompoundTag attribute = attributes.getCompoundOrEmpty(i);
            if (attribute.getStringOr("id", "").endsWith("scale")) {
                scale = attribute.getDoubleOr("base", 0.0);
            } else if (attribute.getStringOr("id", "").endsWith("max_health")) {
                maxHealth = attribute.getDoubleOr("base", 0.0);
            }
        }
        tooltip.accept(Component.translatable("gui.guhs.menu.size", String.format(Locale.ROOT, "%.1f", scale * 1.45)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("gui.guhs.menu.hp", (int) Math.ceil(data.getFloatOr("Health", 0.0F)), (int) maxHealth).withStyle(ChatFormatting.GRAY));
        if (data.getBooleanOr("Saddle", false)) {
            tooltip.accept(Component.translatable("item.minecraft.saddle").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("item.guhs.picked_up_guh.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
