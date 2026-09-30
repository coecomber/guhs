package nl.juiced.guhs.feature.kamperen;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.sterrenwacht.Buiten;
import nl.juiced.guhs.quest.GuhAdvancements;

/** The guh_slaapzak: no spawn point, and uitgerust after a whole night in it. */
public final class KamperenEvents {
    /** How long you're uitgerust (5 minutes). */
    public static final int UITGERUST_TICKS = 20 * 60 * 5;

    /** Sleeping in a slaapzak doesn't make it your home (your spawn point stays where it was). */
    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getNewSpawn() != null && !event.isForced() && event.getEntity().level().getBlockState(event.getNewSpawn()).getBlock() instanceof SlaapzakBlock) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(Component.translatable("gui.guhs.kamperen.geen_thuis").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.wakeImmediately()) {
            return;
        }
        player.getSleepingPos().ifPresent(pos -> {
            if (player.level().getBlockState(pos).getBlock() instanceof SlaapzakBlock) {
                uitgeslapen(player);
            }
        });
    }

    /** A whole night in a slaapzak: uitgerust, the counter, the advancement. */
    public static void uitgeslapen(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(KamperenFeature.UITGERUST, UITGERUST_TICKS, 0));
        player.displayClientMessage(Component.translatable("gui.guhs.kamperen.uitgerust").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        KnusVoortgang.tel(player, Verhalen.UITGESLAPEN, 1);
        GuhAdvancements.grant(player, "kamperen_slaapzak");
        Buiten.toon(player, "kamperen_slaapzak");
    }

    private KamperenEvents() {
    }
}
