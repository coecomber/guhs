package nl.juiced.guhs.feature.einde2.appa;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice appa: a player's own Appa-guh (exactly one per player, never lost).
 * Skeleton stub: nobody has an Appa until the appa slice fills it in (then {@link #geef} never returns null).
 * <p>
 * hook fase B: nothing in phase A gives an Appa in survival; the story calls {@link #geef}, flies the player to the next
 * structure with {@link #vlieg} and hears about the landing through {@link #AANGEKOMEN}.
 */
public final class Appa {
    /** Called when an auto flight ({@link #vlieg}) lands, with the rider. */
    public static final List<Consumer<ServerPlayer>> AANGEKOMEN = new CopyOnWriteArrayList<>();

    public static boolean heeft(ServerPlayer p) {
        return false;
    }

    /** Gives this player their Appa-guh, once: when they have one already, that one comes. */
    @Nullable
    public static AppaGuhEntity geef(ServerPlayer p) {
        return null;
    }

    /** Brings the player's Appa-guh to them (null: they have none, or it is being kept away). */
    @Nullable
    public static AppaGuhEntity roep(ServerPlayer p) {
        return null;
    }

    /** While true the fluitje only says a line ("Appa wordt ontvoerd"). */
    public static void houdWeg(ServerPlayer p, boolean weg) {
    }

    /**
     * Seats the player if needed and flies to {@code doel}, landing there. With {@code spelerStuurt} Appa only points the
     * way until the player lets go for 3 seconds, then he flies himself.
     *
     * @return false when the flight can't start (no Appa)
     */
    public static boolean vlieg(ServerPlayer p, Vec3 doel, boolean spelerStuurt) {
        return false;
    }

    public static boolean vliegt(ServerPlayer p) {
        return false;
    }

    public static void stop(ServerPlayer p) {
    }

    private Appa() {
    }
}
