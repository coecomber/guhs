package nl.juiced.guhs.feature.ring;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;

/**
 * bbq2 (ring-kern): which players get to know about an entity (asked by the entity tracker through mixin RingZichtMixin).
 * A player whose game was never told about an entity sees no model, no name, hears none of its sounds and can't click it.
 * <ul>
 *   <li>a creature behind a Guhdalfs sluier that is closed for the player: never ({@link Sluiers#magZien});</li>
 *   <li>{@link #alleenVoor}: an entity of ONE player (their own Sam-guh, their own Smikagol as a guide, the Nine that hunt
 *       them): two players on the same spot each have their own;</li>
 *   <li>{@link #alleenBij}: a cast member that only exists for players at certain steps of a questline (the fellowship, ONLY
 *       in their own scenes): {@code Zicht.alleenBij(npc, "ring_h2", 1, 4)}; in a template the entity NBT carries the same
 *       (python {@code ring.cast(...)}).</li>
 * </ul>
 * Spectators and creative operators see everything ({@link Sluiers#passeert}). The marks are saved with the entity (a tag in
 * its tag set for the cheap first question, the details in its persistent data).
 */
public final class Zicht {
    /** The entity tag that says "ask {@link Zicht}" (in the entity's tag set: saved, and one hash lookup for all others). */
    public static final String MERK = "guhs_ring_zicht";
    /** Persistent data: the UUID (string) of the only player who sees it. */
    public static final String ALLEEN = "guhs_ring_alleen";
    /** Persistent data: "lijn:van-tot": only players whose step of that questline is van..tot (inclusive). */
    public static final String BIJ = "guhs_ring_bij";

    private record Bereik(String lijn, int van, int tot) {
    }

    private static final Map<String, Bereik> BEREIKEN = new ConcurrentHashMap<>();

    /** Only this player's game knows about the entity from now on. */
    public static void alleenVoor(Entity e, UUID speler) {
        e.addTag(MERK);
        e.getPersistentData().putString(ALLEEN, speler.toString());
    }

    /** Only players whose step of this questline is van..tot (inclusive; tot = 99: from van on, done included) see the entity. */
    public static void alleenBij(Entity e, String lijn, int van, int tot) {
        e.addTag(MERK);
        e.getPersistentData().putString(BIJ, lijn + ":" + van + "-" + tot);
    }

    /** Everybody sees the entity again. */
    public static void voorIedereen(Entity e) {
        e.removeTag(MERK);
        e.getPersistentData().remove(ALLEEN);
        e.getPersistentData().remove(BIJ);
    }

    /** The one player this entity is for (null: not a personal entity). */
    @Nullable
    public static UUID eigenaar(Entity e) {
        if (!e.entityTags().contains(MERK)) {
            return null;
        }
        String s = e.getPersistentData().getStringOr(ALLEEN, "");
        try {
            return s.isEmpty() ? null : UUID.fromString(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** May this player's game know about this entity? */
    public static boolean magZien(ServerPlayer p, Entity e) {
        if (e instanceof Player) {
            return true;
        }
        if (!Sluiers.magZien(p, e)) {
            return false;
        }
        if (!e.entityTags().contains(MERK) || Sluiers.passeert(p)) {
            return true;
        }
        var data = e.getPersistentData();
        String alleen = data.getStringOr(ALLEEN, "");
        if (!alleen.isEmpty() && !alleen.equals(p.getStringUUID())) {
            return false;
        }
        String bij = data.getStringOr(BIJ, "");
        if (!bij.isEmpty()) {
            Bereik b = BEREIKEN.computeIfAbsent(bij, Zicht::lees);
            Verhaallijn lijn = Verhaallijnen.van(b.lijn());
            if (lijn != null) {
                int stap = lijn.stap(p);
                return stap >= b.van() && stap <= b.tot();
            }
        }
        return true;
    }

    private static Bereik lees(String s) {
        try {
            int dubbel = s.lastIndexOf(':'), streep = s.lastIndexOf('-');
            return new Bereik(s.substring(0, dubbel), Integer.parseInt(s.substring(dubbel + 1, streep)), Integer.parseInt(s.substring(streep + 1)));
        } catch (RuntimeException ex) {
            return new Bereik("", 0, 99);
        }
    }

    /** The tracker looks again for this player right now (their story moved on: cast appears or goes). */
    public static void kijkOpnieuw(ServerPlayer p) {
        if (p.connection != null) {
            p.level().getChunkSource().move(p);
        }
    }

    private Zicht() {
    }
}
