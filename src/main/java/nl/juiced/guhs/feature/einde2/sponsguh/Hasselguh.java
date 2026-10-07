package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * einde2, slice sponsguh_wezens: the ride on David Hasselguh's back through the void ({@link DavidHasselguhEntity}).
 * <p>
 * hook fase B: the way home at the end of the Sponsguh story is {@code draag(p, <the landing spot at Bikinibodem>)}, and the
 * story hears about the landing through {@link #AANGEKOMEN}.
 */
public final class Hasselguh {
    /** Called when a ride ({@link #draag}) sets its player down at the goal, with that player. */
    public static final List<Consumer<ServerPlayer>> AANGEKOMEN = new CopyOnWriteArrayList<>();
    /** How near a waiting David Hasselguh must be to be the one that carries. */
    public static final double BEREIK = 16.0;

    /**
     * Seats the player on a David Hasselguh at the player's spot (or on the one within 16 blocks), swims through the void to
     * {@code doel} and sets them down there; the player can't fall off or get off on the way.
     *
     * @return false when the ride can't start (the player is on one already, or no Hasselguh could come)
     */
    public static boolean draag(ServerPlayer p, Vec3 doel) {
        if (draagt(p) || !p.isAlive() || p.isSpectator()) {
            return false;
        }
        ServerLevel level = p.level();
        DavidHasselguhEntity david = level.getEntitiesOfClass(DavidHasselguhEntity.class, p.getBoundingBox().inflate(BEREIK),
                d -> d.isAlive() && !d.onderweg()).stream().min(Comparator.comparingDouble(d -> d.distanceToSqr(p))).orElse(null);
        if (david == null) {
            david = E2SponsguhFeature.DAVID_HASSELGUH.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (david == null) {
                return false;
            }
            // he surfaces under the player: his back at their feet
            david.snapTo(p.getX(), p.getY() - david.rugHoogte(), p.getZ(), p.getYRot(), 0);
            david.zetTijdelijk();
            level.addFreshEntity(david);
        }
        return david.vertrek(p, doel);
    }

    public static boolean draagt(ServerPlayer p) {
        return p.getVehicle() instanceof DavidHasselguhEntity david && david.onderweg();
    }

    /** Breaks the ride off: the player stands where the ride began (never in the void). */
    public static void stop(ServerPlayer p) {
        if (p.getVehicle() instanceof DavidHasselguhEntity david && david.onderweg()) {
            david.zetAf(false);
        }
    }

    /**
     * No getting off halfway (sneaking does nothing: "Blijf zitten, njeg"). Only the sneak key is refused: a teleport (a
     * command, a story, a change of dimension) takes its player off first, and refusing that would leave them a passenger of
     * an entity they are no longer near. David notices the empty back in his next tick and ends the ride.
     */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof DavidHasselguhEntity david
                && david.onderweg() && !david.uitstappen && event.getEntityMounting() instanceof ServerPlayer player && player.isAlive()
                && !player.isRemoved() && player.isShiftKeyDown()) {
            event.setCanceled(true);
            if (player.tickCount % 20 == 0) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_sponsguh_wezens.hasselguh.blijf_zitten").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** Whoever logs out during a ride stands at its start again. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            stop(p);
        }
    }

    private Hasselguh() {
    }
}
