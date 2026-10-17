package nl.juiced.guhs.feature.einde2.spcast;

import java.util.List;

import net.minecraft.world.entity.Entity;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 3, 7.2), slice sp_cast: the story LOOKS of the Sponsguh cast, the API every story slice
 * calls (the skelet wrote this file with stand-in bodies; sp_cast fills in the bodies and keeps names and signatures).
 * <p>
 * A look is a synced, saved on/off state of one cast entity ({@code SponsguhWezen} or a {@code GuhNpcEntity} of sp_cast's
 * Kinds) that its renderer shows. The looks of {@link #LOOKS}:
 * <ul>
 *   <li>{@value #SNOR}: a moustache of knabbelkoordraad (Sponsguh, Patguh; part 2 from the loopgraaf on);</li>
 *   <li>{@value #EMMER}: one of Plankmika's bucket helmets (every {@code bikinibodem_guh}, Krabguh's staff, Octo: part 2's end);</li>
 *   <li>{@value #DROOG}: dried out under the drying lamp (Sponsguh, Patguh: grey, crinkled, a bit smaller);</li>
 *   <li>{@value #IJSJE}: holding a towering sundae (Sponsguh, Patguh in the Guhfy Guhber);</li>
 *   <li>{@value #NETJE}: a jellyfish net in the hand (Sponsguh, Patguh: jellyfishing);</li>
 *   <li>{@value #PETJE}: the Krokante Knabbel cap (Sponsguh at work; the manager's cap at the end: {@value #MANAGER});</li>
 *   <li>{@value #KROON_VAST}: holding Neptuguh's crown (Sponsguh on the way home).</li>
 * </ul>
 * Stand-in: the state is only kept in the entity's persistent data (server side, nothing drawn): story code can already set
 * and ask it, and works unchanged once sp_cast draws it.
 */
public final class SpCast {
    public static final String SNOR = "snor", EMMER = "emmer", DROOG = "droog", IJSJE = "ijsje", NETJE = "netje", PETJE = "petje",
            MANAGER = "manager", KROON_VAST = "kroon_vast";
    public static final List<String> LOOKS = List.of(SNOR, EMMER, DROOG, IJSJE, NETJE, PETJE, MANAGER, KROON_VAST);
    /** (stand-in) the persistent data key of a look. */
    static final String KEY = "guhs_einde2_sp_cast_look_";

    private SpCast() {
    }

    /** Switches a look of a cast entity on or off (an unknown look or an entity that cannot show it: nothing happens). */
    public static void zetLook(Entity e, String look, boolean aan) {
        if (!LOOKS.contains(look)) {
            return;
        }
        if (aan) {
            e.getPersistentData().putBoolean(KEY + look, true);
        } else {
            e.getPersistentData().remove(KEY + look);
        }
    }

    /** Is this look on for this entity? */
    public static boolean heeftLook(Entity e, String look) {
        return e.getPersistentData().getBooleanOr(KEY + look, false);
    }

    /** Can this entity show this look (its model has it)? Stand-in: every look on every entity. */
    public static boolean kan(Entity e, String look) {
        return LOOKS.contains(look);
    }
}
