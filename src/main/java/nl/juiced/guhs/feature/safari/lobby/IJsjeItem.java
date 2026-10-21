package nl.juiced.guhs.feature.safari.lobby;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The animal ice creams of the ijskraam and the roasted marshmallow of the kampvuur (test round 3, design ronde 45):
 * always edible (in the safari park nobody gets hungry, so a normal food could not be eaten there) and each with a small
 * effect for the walk ({@code effect} null: only a happy little cloud).
 */
public class IJsjeItem extends Item {
    private final Supplier<Holder<MobEffect>> effect;
    private final int ticks;

    public IJsjeItem(Properties properties, Supplier<Holder<MobEffect>> effect, int ticks) {
        super(properties);
        this.effect = effect;
        this.ticks = ticks;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer p) {
            Holder<MobEffect> e = effect == null ? null : effect.get();
            if (e != null) {
                p.addEffect(new MobEffectInstance(e, ticks, 0, false, true, true));
            }
            server.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.9, p.getZ(), 2, 0.3, 0.2, 0.3, 0);
            p.sendOverlayMessage(Component.translatable(getDescriptionId() + ".op").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return rest;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }
}
