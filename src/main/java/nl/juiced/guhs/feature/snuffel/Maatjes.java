package nl.juiced.guhs.feature.snuffel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * The companions of the players. Which of the three a player gets is part of their {@link Keuze choice}; WHEN it appears
 * is the story's ({@link #geef}: the island slice's scene with the bucket). From then on, whenever the player is a dog
 * on the island, their {@link MaatjeEntity} floats next to them, seen by nobody else.
 */
public final class Maatjes {
    private static final Map<UUID, MaatjeEntity> ACTIEF = new ConcurrentHashMap<>();

    private Maatjes() {
    }

    /** Did the companion appear for this player? */
    public static boolean heeft(ServerPlayer p) {
        return SnuffelData.heeft(p) && SnuffelData.van(p).getBooleanOr("Maatje", false);
    }

    /** The companion appears for this player (and stays). True the first time. */
    public static boolean geef(ServerPlayer p) {
        if (heeft(p)) {
            return false;
        }
        SnuffelData.van(p).putBoolean("Maatje", true);
        Stand.stuur(p);
        Eiland.Plaats plaats = Eiland.van(p);
        if (plaats != null && Hondvorm.actief(p)) {
            tick(p, plaats);
        }
        return true;
    }

    /** (Dev, tests) the companion is gone again. */
    public static void neemAf(ServerPlayer p) {
        if (SnuffelData.heeft(p)) {
            SnuffelData.van(p).remove("Maatje");
        }
        weg(p);
        Stand.stuur(p);
    }

    /** This player's companion when it floats in the world now. */
    @Nullable
    public static MaatjeEntity van(ServerPlayer p) {
        MaatjeEntity m = ACTIEF.get(p.getUUID());
        return m == null || m.isRemoved() || m.level() != p.level() ? null : m;
    }

    /** The naughty face for a while (it is up to something), then happy again. */
    public static void ondeugend(ServerPlayer p, int ticks) {
        MaatjeEntity m = van(p);
        if (m != null) {
            m.zetOndeugend(ticks);
        }
    }

    /** A happy spin. */
    public static void blij(ServerPlayer p) {
        MaatjeEntity m = van(p);
        if (m != null) {
            m.doe(MaatjeEntity.BLIJ, 24);
        }
    }

    /** Keeps the companion of a dog on an island alive (every tick; cheap when it is there). */
    static void tick(ServerPlayer p, Eiland.Plaats plaats) {
        if (!heeft(p) || p.isSpectator()) {
            return;
        }
        MaatjeEntity m = ACTIEF.get(p.getUUID());
        if (m != null && !m.isRemoved() && m.level() == p.level()) {
            return;
        }
        MaatjeEntity nieuw = SnuffelFeature.SNUFFEL_MAATJE.get().create(p.level(), EntitySpawnReason.TRIGGERED);
        if (nieuw == null) {
            return;
        }
        nieuw.zetEigenaar(p.getUUID());
        nieuw.zetSoort(Keuze.vanOfStandaard(p).maatje());
        nieuw.snapTo(p.getX(), p.getY() + 0.9, p.getZ(), p.getYRot(), 0f);
        p.level().addFreshEntity(nieuw);
        ACTIEF.put(p.getUUID(), nieuw);
    }

    /** The companion leaves the world (its player left, logged out, died); it comes back with them. */
    static void weg(ServerPlayer p) {
        MaatjeEntity m = ACTIEF.remove(p.getUUID());
        if (m != null && !m.isRemoved()) {
            m.discard();
        }
    }

    /** The player chose another companion: the next tick makes the new one. */
    static void opnieuw(ServerPlayer p) {
        weg(p);
    }

    static void opStop() {
        ACTIEF.clear();
    }
}
