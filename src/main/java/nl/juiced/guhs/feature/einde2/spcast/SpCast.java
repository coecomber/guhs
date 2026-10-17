package nl.juiced.guhs.feature.einde2.spcast;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 3, 7.2), slice sp_cast: the story LOOKS of the Sponsguh cast, the API every story slice
 * calls.
 * <p>
 * A look is a synced, saved on/off state of one cast entity ({@link SponsguhWezen}) that its renderer draws (a bone
 * {@code look_<look>} of its model, tools/features/einde2_sp_cast_looks.py). The looks of {@link #LOOKS} and who has them:
 * <ul>
 *   <li>{@value #SNOR}: Mindy-guh's moustache of knabbelkoordraad (Sponsguh, Patguh);</li>
 *   <li>{@value #EMMER}: one of Plankmika's dented bucket helmets with two eye holes (every {@code bikinibodem_guh}, Octo and
 *       Octoliam, Parel, Meneer Krabguh, Sandy-guh, Patguh, Sponsguh);</li>
 *   <li>{@value #DROOG}: dried out under the drying lamp: grey-brown and a bit smaller (Sponsguh, Patguh);</li>
 *   <li>{@value #IJSJE}: a towering sundae in the paw (Sponsguh, Patguh);</li>
 *   <li>{@value #NETJE}: a jelly net held up (Sponsguh, Patguh);</li>
 *   <li>{@value #PETJE} / {@value #MANAGER}: the Krokante Knabbel cap / the manager's cap with a gold band (Sponsguh);</li>
 *   <li>{@value #KROON_VAST}: Neptuguh's crown held up high in both hands (Sponsguh).</li>
 * </ul>
 * <b>On a cutscene actor</b> (the actors live on the client, made from the scene's NBT): give the actor its looks in its NBT,
 * {@code b.acteur("sponsguh", E2SponsguhFeature.SPONSGUH::get, plek, yaw, SpCast.nbt(SpCast.SNOR))}, or with
 * {@link #nbt(CompoundTag, String...)} inside an NBT consumer of your own. (The skelet's stand-in form, the persistent data
 * key {@code guhs_einde2_sp_cast_look_<look>} = true, is read too.)
 */
public final class SpCast {
    public static final String SNOR = "snor", EMMER = "emmer", DROOG = "droog", IJSJE = "ijsje", NETJE = "netje", PETJE = "petje",
            MANAGER = "manager", KROON_VAST = "kroon_vast";
    public static final List<String> LOOKS = List.of(SNOR, EMMER, DROOG, IJSJE, NETJE, PETJE, MANAGER, KROON_VAST);

    private SpCast() {
    }

    /** Switches a look of a cast entity on or off (an unknown look or an entity that cannot show it: nothing happens). */
    public static void zetLook(Entity e, String look, boolean aan) {
        if (e instanceof SponsguhWezen w && LOOKS.contains(look)) {
            w.zetLook(look, aan);
        }
    }

    /** Is this look on for this entity? */
    public static boolean heeftLook(Entity e, String look) {
        return e instanceof SponsguhWezen w && w.heeftLook(look);
    }

    /** Can this entity show this look (its model has it)? */
    public static boolean kan(Entity e, String look) {
        return e instanceof SponsguhWezen w && w.cast().kanLook(look);
    }

    /** (extra) Can this character of the cast show this look? */
    public static boolean kan(Cast cast, String look) {
        return cast.kanLook(look);
    }

    /** (extra) Writes these looks into an actor's (or any saved) NBT: the entity reads them when it is made. */
    public static CompoundTag nbt(CompoundTag tag, String... looks) {
        StringBuilder s = new StringBuilder(tag.getStringOr("Looks", ""));
        for (String look : looks) {
            if (LOOKS.contains(look)) {
                s.append(s.isEmpty() ? "" : ",").append(look);
            }
        }
        tag.putString("Looks", s.toString());
        return tag;
    }

    /** (extra) An NBT consumer for {@code Cutscene.Builder.acteur(..., nbt)} with these looks on. */
    public static Consumer<CompoundTag> nbt(String... looks) {
        return tag -> nbt(tag, looks);
    }
}
