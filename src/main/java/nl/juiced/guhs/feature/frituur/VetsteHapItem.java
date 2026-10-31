package nl.juiced.guhs.feature.frituur;

import java.util.List;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.knus.KnusVoortgang;

/** De Vetste Hap: fills your whole bar, and the first time you finish one there is a little party. */
public class VetsteHapItem extends Item {
    public VetsteHapItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (entity instanceof ServerPlayer player && level instanceof ServerLevel server
                && KnusVoortgang.tel(player, SnackRecepten.TELLER_HAP, 1) == 1) {
            feestje(server, player);
        }
        return rest;
    }

    private static void feestje(ServerLevel level, ServerPlayer player) {
        for (int i = 0; i < 3; i++) {
            ItemStack vuurpijl = new ItemStack(Items.FIREWORK_ROCKET);
            vuurpijl.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL,
                    IntList.of(0xFFD27A, 0xFF7FB6, 0xFFFFFF), IntList.of(0xE8506E), true, true))));
            level.addFreshEntity(new FireworkRocketEntity(level, player.getX() + (i - 1) * 2.5, player.getY() + 1, player.getZ() + (i - 1), vuurpijl));
        }
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 0.8, 0.6, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        player.sendSystemMessage(Component.translatable("item.guhs.vetste_hap.feestje").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.guhs.vetste_hap.lore").withStyle(ChatFormatting.GRAY));
    }
}
