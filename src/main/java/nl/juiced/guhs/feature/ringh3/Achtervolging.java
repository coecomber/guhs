package nl.juiced.guhs.feature.ringh3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Zicht;

/**
 * bbq2 (ring-h3): the chase through the great hall. A player whose own story is at the step of the hall (5) walks in: drums,
 * and that player's OWN Barbecuerog ({@link BarbecuerogEntity}, only their game is told about him: {@link Zicht#alleenVoor})
 * wakes in the Diepe Poort, rises and comes down the nave after them. He stomps (a shove) and, when he gets within reach of
 * his whip, puts them back at their rest fire: then he is gone and the chase starts over when they walk in again. He stops at
 * the edge of the chasm; over the Brokkelpad and the bridge the player is on their own. Reaching the east bank starts the
 * bridge scene, and that is the end of him.
 * <p>
 * Nothing here is saved (the entity type is noSave): after a restart a player simply wakes him again.
 */
public final class Achtervolging {
    /** (not saved) player -> their Barbecuerog. */
    private static final Map<UUID, BarbecuerogEntity> JACHT = new ConcurrentHashMap<>();
    /** Further than this from his prey (blocks) he gives up. */
    public static final double KWIJT = 90;

    private Achtervolging() {
    }

    @Nullable
    public static BarbecuerogEntity van(ServerPlayer p) {
        BarbecuerogEntity rog = JACHT.get(p.getUUID());
        if (rog != null && (rog.isRemoved() || rog.level() != p.level())) {
            JACHT.remove(p.getUUID());
            return null;
        }
        return rog;
    }

    /** Wakes this player's Barbecuerog in this copy of the mine (nothing happens when he is already after them). */
    @Nullable
    public static BarbecuerogEntity start(ServerPlayer p, Mijn m) {
        BarbecuerogEntity rog = van(p);
        if (rog != null) {
            return rog;
        }
        ServerLevel level = m.level();
        rog = RingH3Feature.BARBECUEROG.get().create(level, EntitySpawnReason.TRIGGERED);
        if (rog == null) {
            return null;
        }
        Vec3 start = m.midden(Plekken.ROG_START), einde = m.midden(Plekken.ROG_EINDE);
        rog.snapTo(start.x, start.y, start.z, m.yaw(270f), 0f);
        rog.setInvulnerable(true);
        Zicht.alleenVoor(rog, p.getUUID());
        rog.jaag(p, einde);
        level.addFreshEntity(rog);
        JACHT.put(p.getUUID(), rog);
        level.playSound(null, p.blockPosition(), RingH3Feature.TROMMEL.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
        p.sendOverlayMessage(Component.translatable("quest.guhs.ringh3.rog.trommels").withStyle(ChatFormatting.GOLD));
        return rog;
    }

    /** Is this chase still on? (asked by the Barbecuerog every tick: not when his prey is done with the hall, gone or far away) */
    static boolean geldt(ServerPlayer p, BarbecuerogEntity rog) {
        return JACHT.get(p.getUUID()) == rog && RingH3Feature.LIJN.stap(p) == 5 && p.distanceToSqr(rog) <= KWIJT * KWIJT;
    }

    /** The whip has this player: back to the rest fire (nothing lost, nobody hurt), and he is gone until they come again. */
    static void gepakt(ServerPlayer p, BarbecuerogEntity rog) {
        Vec3 hier = rog.position();
        Mijn m = Mijn.van(p);
        stop(p);
        Ring.behaald(p, "ring_h3_gepakt");
        MijnEvents.terug(p, m, BarbecuerogEntity.REDEN, hier);
    }

    /** The chase of this player is over: their Barbecuerog goes up in smoke. */
    public static void stop(ServerPlayer p) {
        stop(p.getUUID());
    }

    static void stop(UUID speler) {
        BarbecuerogEntity rog = JACHT.remove(speler);
        if (rog != null && !rog.isRemoved()) {
            rog.verdwijn();
        }
    }

    static void wisAlles() {
        for (Entity e : JACHT.values()) {
            if (!e.isRemoved()) {
                e.discard();
            }
        }
        JACHT.clear();
    }
}
