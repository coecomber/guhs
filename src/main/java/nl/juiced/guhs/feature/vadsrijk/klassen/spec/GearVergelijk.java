package nl.juiced.guhs.feature.vadsrijk.klassen.spec;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * U-SPECS (ronde 4, gear decision: no item level, only level + rarity, with a clear better/worse arrow): compares a piece with
 * what you wear in that slot. Pure maths (the tooltip is {@code client.SpecClient}, the gametest checks the verdict):
 * <ul>
 *   <li>per stat the difference (▲ green / ▼ red), weapons also the damage per hit;</li>
 *   <li>an overall verdict: every difference relative to the bigger of the two values (at least a small reference so a tiny
 *   extra stat doesn't count as "100 % better"), weighted: weapon damage x3, the class's main stat x2, the rest x1. Above +3 %
 *   = beter, below -3 % = slechter, else "ongeveer gelijk".</li>
 * </ul>
 */
public final class GearVergelijk {
    public static final double DREMPEL = 0.03;

    /** One line: a stat (null = weapon damage per hit) and the difference new - worn. */
    public record Verschil(@Nullable Stat stat, double verschil) {
    }

    /** The outcome: the lines (biggest first) and the verdict +1 beter, 0 gelijk, -1 slechter (score for tests). */
    public record Uitslag(List<Verschil> regels, double score, int oordeel) {
    }

    private GearVergelijk() {
    }

    /**
     * 1.10.1 (opruimen): what the player wears in the slot of {@code u}, the same on both sides (the tooltip on the client, the
     * "alle slechtere" selection on the server). Weapons: the main-hand weapon of the same weapon class (another class: null, no
     * fair comparison); armour: the worn piece; jewellery: the worn one (rings: the weaker of the two, when both are worn).
     * Returns null when nothing comparable is worn; {@code leeg[0]} = the slot is empty.
     *
     * @param hand    the main-hand stack
     * @param harnas  the worn stack per vanilla armour slot
     * @param sieraad the worn piece per virtual slot (kern.Sieraden), or null
     * @param klasse  the class for the main-stat weight (rings: which one is weaker)
     */
    @Nullable
    public static Uitrusting gedragen(Uitrusting u, net.minecraft.world.item.ItemStack hand,
            java.util.function.Function<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack> harnas,
            java.util.function.Function<Slot, Uitrusting> sieraad, @Nullable String klasse, boolean[] leeg) {
        nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType bt = u.basisType();
        if (bt == null) {
            return null;
        }
        Slot slot = bt.slot();
        Uitrusting oud;
        if (slot == Slot.WAPEN) {
            oud = Rollen.van(hand);
            if (oud == null || oud.basisType() == null || !java.util.Objects.equals(nl.juiced.guhs.feature.vadsrijk.kern.Spelers.wapenKlasse(oud.basisType()),
                    nl.juiced.guhs.feature.vadsrijk.kern.Spelers.wapenKlasse(bt))) {
                return null;   // (another class's weapon: no fair comparison)
            }
        } else if (slot.vanilla != null) {
            oud = Rollen.van(harnas.apply(slot.vanilla));
        } else if (slot == Slot.RING || slot == Slot.RING2) {
            Uitrusting a = sieraad.apply(Slot.RING), b = sieraad.apply(Slot.RING2);
            if (u.equals(a) || u.equals(b)) {
                return null;
            }
            oud = a == null || b == null ? null : vergelijk(a, b, klasse).score() < 0 ? a : b;   // the weaker ring is the one you'd swap
        } else {
            oud = sieraad.apply(slot);
        }
        if (oud == null) {
            leeg[0] = true;
        }
        return oud;
    }

    /** The main stat of a class (Kracht / Behendigheid / Wijsheid). */
    public static Stat hoofdstat(@Nullable String klasse) {
        return Klassen.JAGER.equals(klasse) ? Stat.BEHENDIGHEID : Klassen.MAGIER.equals(klasse) ? Stat.WIJSHEID : Stat.KRACHT;
    }

    private static double ref(@Nullable Stat s) {
        return s == null ? 1 : s == Stat.VADS ? 60 : 8;
    }

    /**
     * Compares {@code nieuw} with {@code oud} (null = the slot is empty: everything counts as better) for a player of class
     * {@code klasse} (the main-stat weight).
     */
    public static Uitslag vergelijk(Uitrusting nieuw, @Nullable Uitrusting oud, @Nullable String klasse) {
        Stats a = Rollen.stats(nieuw), b = oud == null ? new Stats() : Rollen.stats(oud);
        Set<Stat> alle = EnumSet.noneOf(Stat.class);
        alle.addAll(a.alles().keySet());
        alle.addAll(b.alles().keySet());
        List<Verschil> l = new ArrayList<>();
        double som = 0, gewicht = 0;
        boolean wapen = nieuw.basisType() != null && nieuw.basisType().slot() == Slot.WAPEN;
        if (wapen) {
            double na = Rollen.klapSchade(nieuw), oa = oud == null ? 0 : Rollen.klapSchade(oud);
            if (Math.abs(na - oa) >= 0.5) {
                l.add(new Verschil(null, na - oa));
            }
            som += 3 * (na - oa) / Math.max(Math.max(Math.abs(na), Math.abs(oa)), ref(null));
            gewicht += 3;
        }
        Stat hoofd = hoofdstat(klasse);
        for (Stat s : alle) {
            double n = a.get(s), o = b.get(s), d = n - o;
            if (Math.abs(d) < 0.5) {
                continue;
            }
            l.add(new Verschil(s, d));
            double w = s == hoofd ? 2 : 1;
            som += w * d / Math.max(Math.max(Math.abs(n), Math.abs(o)), ref(s));
            gewicht += w;
        }
        l.sort((x, y) -> Double.compare(Math.abs(y.verschil() / ref(y.stat())), Math.abs(x.verschil() / ref(x.stat()))));
        double score = gewicht == 0 ? 0 : som / gewicht;
        int oordeel = oud == null ? 1 : score > DREMPEL ? 1 : score < -DREMPEL ? -1 : 0;
        return new Uitslag(l, score, oordeel);
    }
}
