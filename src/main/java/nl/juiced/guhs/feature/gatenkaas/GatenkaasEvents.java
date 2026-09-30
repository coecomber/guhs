package nl.juiced.guhs.feature.gatenkaas;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.GuhAdvancements;

/** Small things of the gatenkaas caves: the Guhdex page of the Vadswaker, and the advancements the mod grants itself. */
public final class GatenkaasEvents {
    /** How close you must have been to a Vadswaker for its Guhdex page (from a safe-ish distance: he's blind). */
    public static final double SEEN_RANGE = 12;

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 7
                && !player.level().getEntitiesOfClass(VadswakerEntity.class, player.getBoundingBox().inflate(SEEN_RANGE)).isEmpty()) {
            grant(player, "found_vadswaker");
        }
    }

    static void onVadswakerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof VadswakerEntity && event.getSource().getEntity() instanceof ServerPlayer player) {
            grant(player, "gatenkaas_vadswaker_verslagen");
        }
    }

    /** A hidden quest advancement (guhs:quest/...). */
    public static void grant(ServerPlayer player, String name) {
        GuhAdvancements.grant(player, name);
    }

    /** One of our visible advancements whose criterion is "done" (granted by the mod). */
    public static void award(ServerPlayer player, String path) {
        AdvancementHolder holder = player.server.getAdvancements().get(Guhs.id(path));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    private GatenkaasEvents() {
    }
}
