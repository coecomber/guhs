package nl.juiced.guhs.feature.baltoslee;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.balto.Nomguh;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * 3.0: the medicine ride through the storm on the Nomguh route (balto-slee), started by balto's questline (CONTRACT_30 §4.10).
 * You steer the sled yourself ({@link SleeEntity}, {@link SleeRit}): Baltoguh and the guh-sledehondjes pull it through a heavy
 * storm with gusts, over an ice bridge and past avalanche slopes, with vuurkorf rest points on the way. The moments:
 * <ul>
 *   <li>{@link Moment#START}: the countdown begins;</li>
 *   <li>{@link Moment#BERGHUT}: at the berghut (the medicine chest goes on the sled). The ride waits until {@link #verder}
 *   (or goes on by itself after {@value SleeRit#PAUZE_MAX} ticks); from then on the clock runs for the way back;</li>
 *   <li>{@link Moment#DIEPTEPUNT}: on the way back, where the storm is worst. It waits for {@link #verder} (after the wolf
 *   moment; {@link #stormKlaartOp} clears the storm; by itself after {@value SleeRit#PAUZE_MAX} ticks, with the storm
 *   clearing too); the clock stops while it waits;</li>
 *   <li>{@link Moment#AANKOMST}: on time at the hospital (the rider stands at the hospital's spot of the route);</li>
 *   <li>{@link Moment#TE_LAAT}: the time for the way back ran out ("Njeg, nog een keer!": the rider is back at the stable);</li>
 *   <li>{@link Moment#GESTOPT}: the rider stopped (held sneak), left, logged out.</li>
 * </ul>
 * Listeners are told on the server thread, after the ride's state changed (so {@link #bezig} is already false at the end).
 */
public final class SleeTocht {
    /** What happens on the ride (balto listens: BERGHUT and DIEPTEPUNT pause the ride until {@link #verder}). */
    public enum Moment {
        START, BERGHUT, DIEPTEPUNT, AANKOMST, TE_LAAT, GESTOPT
    }

    @FunctionalInterface
    public interface Luisteraar {
        void op(ServerPlayer p, Moment m);
    }

    private static final List<Luisteraar> LUISTERAARS = new CopyOnWriteArrayList<>();

    /** Starts the medicine ride for p (Baltoguh's story copy pulls along, if given); false = busy or no Nomguh near. */
    public static boolean startMedicijn(ServerPlayer p, @Nullable GuhEntity baltoKopie) {
        if (bezig(p)) {
            return false;
        }
        ServerLevel level = p.serverLevel();
        BlockPos anker = Nomguh.anker(level, p.blockPosition());
        if (anker == null && baltoKopie != null) {
            anker = Nomguh.anker(level, baltoKopie.blockPosition());
        }
        if (anker == null) {
            return false;
        }
        return startMet(p, NomguhRoute.laad().in(anker), baltoKopie);
    }

    /** Starts the medicine ride over this route (world coordinates): for tests and the op command. */
    public static boolean startMet(ServerPlayer p, NomguhRoute wereld, @Nullable GuhEntity baltoKopie) {
        return SleeRit.start(p, wereld, SleeRit.Modus.TOCHT, Niveau.MAKKELIJK, baltoKopie, null) != null;
    }

    public static void luister(Luisteraar l) {
        LUISTERAARS.add(l);
    }

    /** After BERGHUT / DIEPTEPUNT the ride pauses until this (or it goes on by itself after 600 ticks). */
    public static void verder(ServerPlayer p) {
        SleeRit rit = SleeRit.van(p);
        if (rit != null && rit.modus == SleeRit.Modus.TOCHT) {
            rit.verder(p);
        }
    }

    /** After the wolf howl: the storm fades for the rest of the ride. */
    public static void stormKlaartOp(ServerPlayer p) {
        SleeRit rit = SleeRit.van(p);
        if (rit != null && rit.modus == SleeRit.Modus.TOCHT) {
            rit.stormKlaartOp(p);
        }
    }

    public static boolean bezig(ServerPlayer p) {
        return SleeRit.rijdt(p, SleeRit.Modus.TOCHT);
    }

    /** (for the implementation) tells every listener. */
    static void meld(ServerPlayer p, Moment m) {
        for (Luisteraar l : LUISTERAARS) {
            try {
                l.op(p, m);
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("SleeTocht listener failed at " + m, e);
            }
        }
    }

    /** (tests) forget a listener. */
    static void vergeet(Luisteraar l) {
        LUISTERAARS.remove(l);
    }

    private SleeTocht() {
    }
}
