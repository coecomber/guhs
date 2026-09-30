package nl.juiced.guhs.feature.guheinde;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/**
 * De Knabbelkroon: Opper-Mika's crown, yours after your first win. A helmet as strong as vahoege vads, with the
 * vahoeg-aura: you never get really hungry, guhs around you now and then go VAHOEG! (hearts), and Mika's can't stand it:
 * they come for you first, and you hit them harder ({@link #MIKA_DAMAGE}, see GuheindeEvents).
 * (1.1.0: a plain item; the helmet part comes from {@code Item.Properties#humanoidArmor}, see GuheindeFeature.)
 */
public class KnabbelkroonItem extends Item {
    public static final float MIKA_DAMAGE = 1.5f;
    public static final int AURA_RANGE = 8;

    public KnabbelkroonItem(Properties properties) {
        super(properties);
    }

    public static boolean wears(net.minecraft.world.entity.LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof KnabbelkroonItem;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot equipSlot) {
        if (!(entity instanceof Player player) || player.getItemBySlot(EquipmentSlot.HEAD) != stack) {
            return;
        }
        if (player.tickCount % 100 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0, true, false, true));
        }
        if (player.tickCount % 60 == 0 && level instanceof ServerLevel server) {
            for (GuhEntity guh : server.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(AURA_RANGE))) {
                if (server.getRandom().nextInt(3) == 0) {
                    server.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 1, 0.2, 0.1, 0.2, 0);
                    if (server.getRandom().nextInt(4) == 0) {
                        guh.playSound(ModSounds.GUH_HAPPY.get(), 0.8f, guh.getVoicePitch());
                    }
                }
            }
            for (MikaEntity mika : server.getEntitiesOfClass(MikaEntity.class, player.getBoundingBox().inflate(16),
                    m -> m.getTarget() == null && !player.isCreative() && !player.isSpectator())) {
                mika.setTarget(player);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.knabbelkroon.lore").withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("item.guhs.knabbelkroon.lore2").withStyle(ChatFormatting.GRAY));
    }
}
