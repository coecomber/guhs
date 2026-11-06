package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;

/** Rolling gear, and what a rolled piece gives. */
public final class Rollen {
    /** Economie hook: the extra stats of the stars on a piece (default: none). */
    public static Function<Uitrusting, Stats> STER_BONUS = u -> new Stats();
    /** Economie hook: weapon damage factor of the stars (default +5 % per star). */
    public static Function<Uitrusting, Double> STER_SCHADE = u -> 1 + 0.05 * u.sterren();
    /** z2-systemen hook: how many gem sockets a fresh roll gets (default none; zone2.systemen sets it). */
    public static SokjesRol SOKJES = (t, level, z, r) -> 0;
    /** z2-systemen hook: the extra stats of the gems in a piece's sockets (default none). */
    public static Function<Uitrusting, Stats> SOKJE_BONUS = u -> new Stats();

    /**
     * T-ITEMS (itemisatie 2.0): the stat budget of a slot: everything a piece in this slot gives (base, bonuses, stars, gems) x
     * this. Default 1; feature/vadsrijk/items sets it from the balans file ([items] budget_*: the virtual slots give less, so 12
     * slots don't blow up the totals). The tooltip shows the values after this factor.
     */
    public static java.util.function.ToDoubleFunction<Slot> SLOT_BUDGET = s -> 1.0;
    /** T-ITEMS: the "Vertrouwd" growth of a piece: base stats and weapon damage x this (default 1). */
    public static java.util.function.ToDoubleFunction<Uitrusting> ERF_FACTOR = u -> 1.0;

    /** T-ITEMS: the budget factor of a piece (its base type's slot). */
    public static double budget(Uitrusting u) {
        BasisType t = u.basisType();
        return t == null ? 1.0 : SLOT_BUDGET.applyAsDouble(t.slot());
    }

    @FunctionalInterface
    public interface SokjesRol {
        int aantal(BasisType t, int level, Zeldzaamheid z, RandomSource r);
    }

    private Rollen() {
    }

    public static Uitrusting rol(BasisType t, int level, Zeldzaamheid z, RandomSource r) {
        int n = z.minBonus + (z.maxBonus > z.minBonus ? r.nextInt(z.maxBonus - z.minBonus + 1) : 0);
        Uitrusting u = new Uitrusting(t.id(), z, level, Bonussen.rol(n, level, z, r, t.slot()), t.element(), t.flavours() <= 0 ? 0 : r.nextInt(t.flavours()), 0,
                false, false, r.nextLong());
        int sokjes = Math.max(0, Math.min(3, SOKJES.aantal(t, level, z, r)));   // z2-systemen (z3-systemen: up to 3 from lv 45)
        return sokjes == 0 ? u : u.metSokjes(java.util.Collections.nCopies(sokjes, ""));
    }

    /** The stats a piece gives (nothing when verpluisd). */
    public static Stats stats(Uitrusting u) {
        Stats s = new Stats();
        BasisType t = u.basisType();
        if (t == null || u.verpluisd()) {
            return s;
        }
        s.plus(t.statsOp(u.level()).maal(u.zeldzaamheid().factor * ERF_FACTOR.applyAsDouble(u)));   // T-ITEMS: x Vertrouwd
        for (Bonus b : u.bonussen()) {
            s.plus(b.stat(), b.waarde());
        }
        s.plus(STER_BONUS.apply(u));
        s.plus(SOKJE_BONUS.apply(u));   // z2-systemen
        double budget = budget(u);   // T-ITEMS: the slot budget
        return budget == 1.0 ? s : s.maal(budget);
    }

    /** A weapon's base damage per hit (0 for armour; verpluisd: 1). */
    public static double wapenSchade(Uitrusting u) {
        BasisType t = u.basisType();
        if (t == null || t.slot() != Slot.WAPEN) {
            return 0;
        }
        if (u.verpluisd()) {
            return 1;
        }
        return t.schadeOp(u.level()) * u.zeldzaamheid().factor * STER_SCHADE.apply(u) * (isStartzwaard(u) ? STARTZWAARD_FACTOR : 1)
                * ERF_FACTOR.applyAsDouble(u);   // T-ITEMS: Vertrouwd
    }

    /**
     * h-melee (playtest round 3: "melee is much harder than ranged/magic"): the Knuffelridder's starter sword from
     * Knuffelkamp (klassen KlasseBalie.startwapen: a BOUND, Gewoon, level-1 kussenzwaard) does twice its damage, so the
     * tutorial Klodders go down in ~2-3 hits instead of ~5. Loot/shop swords (not bound) are unchanged.
     */
    public static final double STARTZWAARD_FACTOR = 2.0;

    public static boolean isStartzwaard(Uitrusting u) {
        return u.gebonden() && u.level() == 1 && u.zeldzaamheid() == Zeldzaamheid.GEWOON && "kussenzwaard".equals(u.type());
    }

    /** One basic hit of this weapon (before the player's stats): rating x class factor x base interval (kern.Aanvalstempo). */
    public static double klapSchade(Uitrusting u) {
        BasisType t = u.basisType();
        return wapenSchade(u) * nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo.klasseFactor(t) * nl.juiced.guhs.feature.vadsrijk.kern.Aanvalstempo.seconden(t);
    }

    /** The item stack for this roll (empty when the base type's item is unknown). */
    public static ItemStack stack(Uitrusting u) {
        BasisType t = u.basisType();
        if (t == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(t.itemId()));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack s = new ItemStack(item);
        s.set(KernRegistry.UITRUSTING.get(), u);
        return s;
    }

    @Nullable
    public static Uitrusting van(ItemStack s) {
        return s.isEmpty() ? null : s.get(KernRegistry.UITRUSTING.get());
    }
}
