package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice sponsguh_wezens: the ride on David Hasselguh's back through the void.
 * Skeleton stub: nobody is carried until the sponsguh_wezens slice fills it in.
 * <p>
 * hook fase B: the way home at the end of the Sponsguh story is {@code draag(p, <the landing spot at Bikinibodem>)}, and the
 * story hears about the landing through {@link #AANGEKOMEN}.
 */
public final class Hasselguh {
    /** Called when a ride ({@link #draag}) sets its player down, with that player. */
    public static final List<Consumer<ServerPlayer>> AANGEKOMEN = new CopyOnWriteArrayList<>();

    /**
     * Seats the player on a David Hasselguh at the player's spot (or on the one within 16 blocks), swims through the void to
     * {@code doel} and sets them down there; the player can't fall off or get off on the way.
     *
     * @return false when the ride can't start
     */
    public static boolean draag(ServerPlayer p, Vec3 doel) {
        return false;
    }

    public static boolean draagt(ServerPlayer p) {
        return false;
    }

    public static void stop(ServerPlayer p) {
    }

    private Hasselguh() {
    }
}
