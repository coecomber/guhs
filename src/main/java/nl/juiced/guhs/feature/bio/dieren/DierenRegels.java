package nl.juiced.guhs.feature.bio.dieren;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import nl.juiced.guhs.world.WildeDieren;

/**
 * The numbers and rules that keep the animals of this slice from piling up (the live server froze on that once: see
 * {@link WildeDieren}). Everything here is a plain function of its arguments, so the game tests can ask it without a
 * Guhmensie.
 * <ul>
 *   <li>A natural spawn only happens in the animal's own biome and only while there are fewer than the cap of wild ones
 *       around the spot ({@link #koiMag}, {@link #schaapjeMag}, {@link #kikkerMag}).</li>
 *   <li>Every wild one a spawner brings is a come-and-go animal ({@link WildeDieren#markeer}): never saved, gone when
 *       every player is far ({@link #komtEnGaat}).</li>
 *   <li>Feeding koi gives a kleintje only while the pond holds fewer than {@link #KOI_MAX_VIJVER} koi, and a koi has to
 *       wait {@link #KLEINTJE_WACHT} between two ({@link #kleintjeMag}); wolkenschaapjes fall in love only in a herd of
 *       fewer than {@link #SCHAAPJE_MAX_KUDDE}.</li>
 *   <li>A tidy-up every 30 s ({@link #tidyUp}): more wild ones of a kind in a level than {@link #max} and the farthest go,
 *       exactly like {@link WildeDieren#tidyUp} does for the older animals (this slice may not edit its list of kinds).</li>
 * </ul>
 */
public final class DierenRegels {
    /** Natural spawns: at most this many WILD koi within {@link #KOI_TEL_STRAAL} blocks of the spot. */
    public static final int KOI_MAX_DICHTBIJ = 8;
    public static final int KOI_TEL_STRAAL = 24;
    /** Natural spawns: at most this many WILD wolkenschaapjes within {@link #SCHAAPJE_TEL_STRAAL} blocks of the spot. */
    public static final int SCHAAPJE_MAX_DICHTBIJ = 5;
    public static final int SCHAAPJE_TEL_STRAAL = 40;
    /** Natural spawns in the new biomes: at most this many kikkerguhs within {@link #KIKKER_TEL_STRAAL} blocks of the spot. */
    public static final int KIKKER_MAX_DICHTBIJ = 4;
    public static final int KIKKER_TEL_STRAAL = 32;
    /** Feeding: no kleintje while this many koi (wild and kept alike) swim within {@link #KOI_VIJVER_STRAAL} blocks. */
    public static final int KOI_MAX_VIJVER = 12;
    public static final int KOI_VIJVER_STRAAL = 12;
    /** A koi that got a kleintje waits this long (ticks) before the next one. */
    public static final int KLEINTJE_WACHT = 20 * 60 * 10;
    /** Two fed koi count as a pair when both were fed within this many ticks. */
    public static final int GEVOERD_GELDIG = 20 * 30;
    /** Wolkenschaapjes: no falling in love in a herd of this many within {@link #SCHAAPJE_KUDDE_STRAAL} blocks. */
    public static final int SCHAAPJE_MAX_KUDDE = 12;
    public static final int SCHAAPJE_KUDDE_STRAAL = 16;
    /** The tidy-up: at most this many wild ones of a kind per level, plus this many per player there. */
    public static final int MAX_BASIS = 120, MAX_PER_SPELER = 40;

    /** A spawn the world does by itself (the natural spawner, chunk generation): the biome and cap rules apply. */
    public static boolean natuurlijk(EntitySpawnReason reden) {
        return reden == EntitySpawnReason.NATURAL || reden == EntitySpawnReason.CHUNK_GENERATION;
    }

    /** The same question as {@link WildeDieren#komtEnGaat}: does a spawn of this kind come and go (never saved)? */
    public static boolean komtEnGaat(EntitySpawnReason reden) {
        return WildeDieren.komtEnGaat(reden);
    }

    /** May a koi spawn here? Always in water; by itself only in its two biomes and under the cap. */
    public static boolean koiMag(EntitySpawnReason reden, boolean inWater, boolean inBiome, int wildDichtbij) {
        if (!inWater) {
            return false;
        }
        return !natuurlijk(reden) || inBiome && wildDichtbij < KOI_MAX_DICHTBIJ;
    }

    /** May a wolkenschaapje spawn here? By itself only on ground in the Wolkenweide, in the light, under the cap. */
    public static boolean schaapjeMag(EntitySpawnReason reden, boolean opGrond, boolean licht, boolean inBiome, int wildDichtbij) {
        if (!natuurlijk(reden)) {
            return true;
        }
        return opGrond && licht && inBiome && wildDichtbij < SCHAAPJE_MAX_DICHTBIJ;
    }

    /**
     * The extra rule for a kikkerguh that spawns by itself in one of the three new biomes (elsewhere nothing changes):
     * water close by and under the cap.
     */
    public static boolean kikkerMag(EntitySpawnReason reden, boolean inNieuwBiome, boolean waterDichtbij, int dichtbij) {
        if (!natuurlijk(reden) || !inNieuwBiome) {
            return true;
        }
        return waterDichtbij && dichtbij < KIKKER_MAX_DICHTBIJ;
    }

    /** Do two fed koi get a kleintje? Both grown, both fed just now, both rested, and room in the pond. */
    public static boolean kleintjeMag(boolean beideGroot, boolean beideNetGevoerd, boolean beideUitgerust, int koiInVijver) {
        return beideGroot && beideNetGevoerd && beideUitgerust && koiInVijver < KOI_MAX_VIJVER;
    }

    public static int max(int spelers) {
        return MAX_BASIS + MAX_PER_SPELER * spelers;
    }

    /** The kinds of this slice that come and go (for the tidy-up; the kikkerguh is in WildeDieren's own list). */
    public static List<EntityType<?>> soorten() {
        return List.of(DierenSlice.KOI.get(), DierenSlice.WOLKENSCHAAPJE.get());
    }

    /** The safety net (see the class comment). Returns how many went. */
    public static int tidyUp(ServerLevel level) {
        int spelers = (int) level.players().stream().filter(p -> !p.isSpectator()).count();
        int weg = 0;
        for (EntityType<?> soort : soorten()) {
            weg += WildeDieren.opruimen(wilde(level, soort), level.players(), max(spelers));
        }
        return weg;
    }

    /** The wild come-and-go ones of this kind in the level. */
    public static <T extends Entity> List<Mob> wilde(ServerLevel level, EntityType<T> soort) {
        List<Mob> wild = new ArrayList<>();
        for (T e : level.getEntities(soort, e -> e instanceof Mob m && !m.isRemoved() && WildeDieren.isKomEnGa(m))) {
            wild.add((Mob) e);
        }
        return wild;
    }

    private DierenRegels() {
    }
}
