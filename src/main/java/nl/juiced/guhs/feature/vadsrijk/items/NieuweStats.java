package nl.juiced.guhs.feature.vadsrijk.items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * T-ITEMS: the 4 new stats in the damage pipeline (kern.Schade calls {@link #negeren} and {@link #naAanval}; the rest are NA_KLAP
 * hooks from {@link #installeer}). Only player hits on Vadsrijk creatures; never player vs player, never guhs.
 * <ul>
 *   <li><b>Verdediging negeren %</b>: Vadsrijk creatures have no armour number, so it takes back that share of everything that
 *   makes your hit smaller on them: a bad element match (x0.7 -> closer to x1), blocks/shields of creatures and bosses (the
 *   VOOR_KLAP part, but never a full dodge/reflect) and a creature's Knuffelschild (that share goes through). Max
 *   {@code negeren_max} (50).</li>
 *   <li><b>Schade tegen bazen %</b>: x(1 + %/100) on every boss ({@link VadsrijkBaas}).</li>
 *   <li><b>Leven per treffer</b>: that many Vads back for every hit that does damage (not DoT ticks; at most {@code leven_per_tik}
 *   heals per tick, so an area skill on 10 creatures doesn't heal 10x).</li>
 *   <li><b>Kans op effect %</b>: that chance per hit that the element of the hit sets its status: Vuur = Brandend (3 s, 10 % of
 *   the hit per second), Natuur = Gesporen (3 s, 8 %), Water = Plakkerig (2 s), Roze = Verdoofd (0,5 s, not on bosses).
 *   No element = nothing. Max {@code effect_max} (40).</li>
 * </ul>
 */
public final class NieuweStats {
    private static final Map<UUID, long[]> HEALS = new ConcurrentHashMap<>();

    private NieuweStats() {
    }

    /** The attacker's "Verdediging negeren" as a share 0..max/100. */
    public static double negeren(Stats s) {
        return Math.max(0, Math.min(ItemsBalans.get("negeren_max"), s.get(Stat.VERDEDIGING_NEGEREN))) / 100.0;
    }

    /**
     * After the attacker part (kern.Schade step 2): Schade tegen bazen, and negeren on a weak element match ({@code ef} &lt; 1).
     */
    public static double naAanval(double n, Stats s, LivingEntity doel, double ef, double negeren) {
        if (doel instanceof VadsrijkBaas) {
            n *= 1 + Math.max(0, s.get(Stat.BAZENSCHADE)) / 100.0;
        }
        if (ef > 0 && ef < 1 && negeren > 0) {
            n *= (ef + (1 - ef) * negeren) / ef;
        }
        return n;
    }

    /** Pure maths for the tests: an element factor after negeren. */
    public static double elementNa(double ef, double negeren) {
        return ef < 1 ? ef + (1 - ef) * negeren : ef;
    }

    static void installeer() {
        Schade.NA_KLAP.add(NieuweStats::naKlap);
    }

    private static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        if (!(k.bron() instanceof ServerPlayer p) || !(k.doel() instanceof VadsrijkMob m) || u.schade() <= 0 || k.soort() == Schade.Soort.WAAR
                || "status".equals(k.skillId())) {
            return;
        }
        Stats s = Spelers.stats(p);
        double leven = s.get(Stat.LEVEN_PER_TREFFER);
        if (leven > 0 && magHealen(p)) {
            Schade.heal(p, p, leven);
        }
        double kans = Math.min(ItemsBalans.get("effect_max"), s.get(Stat.KANS_OP_EFFECT));
        if (kans > 0 && !m.geplofd() && p.getRandom().nextDouble() * 100 < kans) {
            zetEffect(k.element(), m, u.schade(), p);
        }
    }

    private static boolean magHealen(ServerPlayer p) {
        long nu = p.level().getGameTime();
        long[] t = HEALS.computeIfAbsent(p.getUUID(), x -> new long[2]);
        if (t[0] != nu) {
            t[0] = nu;
            t[1] = 0;
        }
        return ++t[1] <= ItemsBalans.get("leven_per_tik");
    }

    /** The status an element sets (null: none). Public for the tests. */
    @Nullable
    public static Status effectVan(Element e, boolean baas) {
        return switch (e) {
            case VUUR -> Status.BRANDEND;
            case NATUUR -> Status.GESPOREN;
            case WATER -> Status.PLAKKERIG;
            case ROZE -> baas ? null : Status.VERDOOFD;
            default -> null;
        };
    }

    static void zetEffect(Element e, VadsrijkMob m, double schade, ServerPlayer p) {
        Status st = effectVan(e, m instanceof VadsrijkBaas);
        if (st == null) {
            return;
        }
        switch (st) {
            case BRANDEND -> Statussen.geef(m, st, 60, Math.max(1, schade * 0.10), p);
            case GESPOREN -> Statussen.geef(m, st, 60, Math.max(1, schade * 0.08), p);
            case PLAKKERIG -> Statussen.geef(m, st, 40, 0.35, p);
            case VERDOOFD -> Statussen.geef(m, st, 10, 1, p);
            default -> {
            }
        }
    }

    static void vergeet(UUID id) {
        HEALS.remove(id);
    }
}
