package nl.juiced.guhs.feature.vadsrijk.wezens;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * 1.10.2 (LUC_DOOD_ANALYSE): the Kristalklodder's mirror used to roll on EVERY target of every area spell (also the automatic
 * Liedpuls) and sent back 50 % of the caster's full hit as true damage with the [mobs] multiplier on top: a reflected Meteoorguh
 * one-shot a level-20 mage. Now:
 * <ul>
 *   <li>at most one roll per cast: the first Kristalklodder a cast (player + skill + game tick) hits rolls, the others don't;</li>
 *   <li>never for automatic/passive procs ({@link #NOOIT}: Liedpuls, puddles, burning ground, chain bolts, statuses);</li>
 *   <li>the reflect is normal magic damage (Verdediging counts), capped at {@link #MAX_DEEL} of the caster's max Vads, and gets no
 *       mob/boss multiplier ({@link Schade#WEERKAATST}).</li>
 * </ul>
 */
public final class Weerkaatsen {
    public static final String SKILL = Schade.WEERKAATST;
    /** One reflect is at most this share of the caster's max Vads. */
    public static final double MAX_DEEL = 0.15;
    /** Automatic/passive procs: never reflected. */
    static final Set<String> NOOIT = Set.of("liedpuls", "sterrenwever", "meteoorvuur", "kettingbolt", "status", SKILL);
    /** Tests: every allowed roll reflects. */
    public static volatile boolean TEST_ALTIJD;
    /** Per player: the last cast that already rolled ("skill@tick"). */
    private static final Map<UUID, String> GEROLD = new ConcurrentHashMap<>();

    private Weerkaatsen() {
    }

    /** May this hit roll the mirror? (a player's active cast, once per cast) */
    public static boolean magRollen(LivingEntity bron, String skill, long tik) {
        if (!(bron instanceof ServerPlayer p) || skill == null || NOOIT.contains(skill) || skill.startsWith("vfx_")) {
            return false;
        }
        String sleutel = skill + "@" + tik;
        return !sleutel.equals(GEROLD.put(p.getUUID(), sleutel));
    }

    /** The reflected amount: half the hit, at most {@link #MAX_DEEL} of the caster's max Vads. */
    public static double terug(LivingEntity bron, double n) {
        double t = n * 0.5;
        if (bron instanceof ServerPlayer p) {
            t = Math.min(t, Spelers.maxVads(p) * MAX_DEEL);
        }
        return Math.max(0, t);
    }

    /**
     * 1.10.18 (Luc_Ermeling fainted 9x in 3 minutes): the zone-2 Spiegelwacht mirrors {@link #SPIEGEL_DEEL} of every ranged/magic
     * hit from the front back as true damage. It used to be uncapped AND got the [mobs] schade multiplier (x2.028): ~81 % of a
     * lv-48 mage's own hit came back (up to 2705 of 4455 Vads). Now: no multiplier (skill {@link #SKILL}), at most {@link #MAX_DEEL}
     * of the caster's max Vads, and never for automatic/passive procs ({@link #automatisch}).
     */
    public static final double SPIEGEL_DEEL = 0.4;

    /** An automatic/passive proc (Liedpuls, puddles, chain bolts, statuses, vfx): never reflected or mirrored. */
    public static boolean automatisch(String skill) {
        return skill == null || NOOIT.contains(skill) || skill.startsWith("vfx_");
    }

    /** The Spiegelwacht's mirrored amount: {@link #SPIEGEL_DEEL} of the hit, at most {@link #MAX_DEEL} of the caster's max Vads. */
    public static double gespiegeld(LivingEntity bron, double n) {
        double t = n * SPIEGEL_DEEL;
        if (bron instanceof ServerPlayer p) {
            t = Math.min(t, Spelers.maxVads(p) * MAX_DEEL);
        }
        return Math.max(0, t);
    }

    /** (tests) forget the rolled casts. */
    public static void vergeet(UUID id) {
        GEROLD.remove(id);
    }
}
