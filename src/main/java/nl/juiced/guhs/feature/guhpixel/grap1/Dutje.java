package nl.juiced.guhs.feature.guhpixel.grap1;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import nl.juiced.guhs.feature.guhpixel.Sessies;

/**
 * Lying in a bed inside Skyblok, Bedwars and Vadsnite: the three games end by lying down, so a player in one of them
 * may always lie down in a bed that is close enough (whatever the hour or the dimension's bed rule; the game tests run
 * in the overworld by day) and stays lying.
 * <p>
 * Vanilla wakes everybody once every player of a dimension has slept for 100 ticks. A lone player in guhpixel would be
 * thrown out of bed after five seconds, in the middle of a punchline, so {@link #blijfLiggen} keeps the server-side
 * sleep counter of a player in a game low (the counter is private and has no setter: reflection, and when that ever
 * fails the games still work, the player just has to lie down again).
 */
final class Dutje {
    @Nullable
    private static Field teller;
    private static boolean gezocht;

    static void onSlapen(CanPlayerSleepEvent event) {
        Player.BedSleepingProblem probleem = event.getProblem();
        if (probleem == null || probleem == Player.BedSleepingProblem.TOO_FAR_AWAY || probleem == Player.BedSleepingProblem.OBSTRUCTED) {
            return;
        }
        if (Sessies.van(event.getEntity()) instanceof GrapSessie s && !s.afgelopen && event.getState().is(BlockTags.BEDS)
                && !event.getEntity().isSleeping() && event.getEntity().isAlive()) {
            event.setProblem(null);
        }
    }

    static void onDoorslapen(CanContinueSleepingEvent event) {
        if (!event.mayContinueSleeping() && event.getEntity() instanceof ServerPlayer p && Sessies.van(p) instanceof GrapSessie
                && p.getSleepingPos().filter(pos -> p.level().getBlockState(pos).is(BlockTags.BEDS)).isPresent()) {
            event.setContinueSleeping(true);
        }
    }

    private static void zoek() {
        if (gezocht) {
            return;
        }
        gezocht = true;
        try {
            Field f = Player.class.getDeclaredField("sleepCounter");
            f.setAccessible(true);
            teller = f;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LogUtils.getLogger().warn("Guhpixel: cannot keep a sleeping player asleep (Player.sleepCounter): {}", e.toString());
        }
    }

    /** Keeps the server from counting this player as "slept long enough" (see the class comment). */
    static void blijfLiggen(ServerPlayer p) {
        if (p.getSleepTimer() >= 90) {
            zet(p, 60);
        }
    }

    /** Sets the sleep counter (the trick itself, and the tests); false when this runtime does not allow it. */
    static boolean zet(ServerPlayer p, int ticks) {
        zoek();
        if (teller == null) {
            return false;
        }
        try {
            teller.setInt(p, ticks);
            return true;
        } catch (IllegalAccessException | RuntimeException e) {
            teller = null;
            return false;
        }
    }

    private Dutje() {
    }
}
