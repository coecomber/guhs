package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * De Erfenis, bestellingen: one order cart (Hay Day style). A cart goes round: {@link #HEEN} (driving to your village with an
 * order) -&gt; {@link #OPEN} (parked at your Marktplein: the order can be delivered or refused; it never expires) -&gt;
 * {@link #TERUG} (driving home, with your goods or empty after a refusal) -&gt; {@link #WACHT} (resting for the Marktplein's
 * wait) -&gt; a new order, {@link #HEEN} again. The gold star cart ({@link #ster}) is the daily extra one.
 * <p>
 * {@link #maak} makes an order from balans.md sections 4 and 5: the order's tier is the player's Hoeve tier; the reward is
 * the goods' value x 2.5 (a lower village), 3 (equal), 3.5 (higher) or 4 (the star order).
 */
public final class Bestelling {
    public static final int HEEN = 0, OPEN = 1, TERUG = 2, WACHT = 3;
    /** Reward per good value, by relation (-1 lower, 0 equal, +1 higher) and for the star order. */
    public static final double LAGER = 2.5, GELIJK = 3, HOGER = 3.5, STER = 4;
    /** Reward range (Ƒ) of an equal village's order per tier (balans 4), index = tier. */
    static final int[] MIN_F = {0, 40, 120, 300, 600, 1000}, MAX_F = {0, 80, 250, 500, 1000, 1800};
    /** Goods per order per tier (balans 4). */
    static final int[] MIN_N = {0, 5, 10, 15, 20, 25}, MAX_N = {0, 15, 30, 40, 50, 60};
    /** Chance (%) of a special deco piece on delivery, by relation -1/0/+1 (balans 4). */
    static final int[] DECO_KANS = {3, 8, 15};
    public static final int REDENEN = 20, STER_REDENEN = 8;

    public record Regel(String goed, int n) {
    }

    public int fase = WACHT;
    public long start, eind;
    public boolean ster;
    /** Empty while the cart is waiting at home. */
    public UUID dorp = new UUID(0, 0);
    public int relatie;
    public final List<Regel> regels = new ArrayList<>();
    public int beloning;
    /** The star order's guaranteed deco piece ("" for other orders: they roll on delivery). */
    public String deco = "";
    public int reden;
    /** Who gave this order +20% with "Handje helpen" ("" = nobody). */
    public String hulp = "";
    /** The cart drives home empty (refused): the next order comes after the normal wait. */
    public boolean geweigerd;

    public double factor() {
        return ster ? STER : relatie < 0 ? LAGER : relatie > 0 ? HOGER : GELIJK;
    }

    /** What delivering pays: the reward, +20% with a helping hand. */
    public int uitbetaling() {
        return hulp.isEmpty() ? beloning : (int) Math.round(beloning * 1.2);
    }

    public int aantalGoederen() {
        int n = 0;
        for (Regel r : regels) {
            n += r.n();
        }
        return n;
    }

    /** The deco chance (%) of this order on delivery (100 for the star order). */
    public int decoKans() {
        return ster ? 100 : DECO_KANS[Math.max(-1, Math.min(1, relatie)) + 1];
    }

    // =====================================================================================================================
    // a new order
    // =====================================================================================================================

    /**
     * Fills this cart with a new order from {@code dorp}: tier = the player's Hoeve tier (1..5), relation -1/0/+1 of that
     * village to the player. {@code kanMaken}: a good of the NEXT tier the player can already make (an order from a higher
     * village may hold one such kind). Deterministic for the {@link Random}.
     */
    public void maak(Random rng, UUID dorp, int tier, int relatie, boolean ster, Predicate<BestelGoed> kanMaken) {
        tier = Math.max(1, Math.min(5, tier));
        this.dorp = dorp;
        this.relatie = Math.max(-1, Math.min(1, relatie));
        this.ster = ster;
        regels.clear();
        hulp = "";
        geweigerd = false;
        deco = "";
        // how much value the village wants (an equal village pays MIN_F..MAX_F at x3)
        double waarde = (MIN_F[tier] + rng.nextDouble() * (MAX_F[tier] - MIN_F[tier])) / GELIJK;
        int soorten = tier == 1 ? 1 + rng.nextInt(2) : tier <= 3 ? 2 + rng.nextInt(2) : 3 + rng.nextInt(2);
        int maxN = MAX_N[tier];
        if (ster) {
            waarde *= 1.6;
            soorten = tier == 1 ? 2 : tier <= 3 ? 3 : 4;
            maxN = maxN * 3 / 2;
        } else if (this.relatie < 0) {   // easy and small
            waarde *= 0.7;
            soorten = Math.max(1, soorten - 1);
            maxN = maxN * 3 / 4;
        }
        // the kinds: one of this tier's own goods first (from T2 on), the rest from everything up to this tier
        List<BestelGoed> pot = BestelGoed.totTier(tier), gekozen = new ArrayList<>();
        if (tier > 1) {
            List<BestelGoed> eigen = BestelGoed.vanTier(tier);
            gekozen.add(eigen.get(rng.nextInt(eigen.size())));
        }
        while (gekozen.size() < soorten && gekozen.size() < pot.size()) {
            BestelGoed g = pot.get(rng.nextInt(pot.size()));
            if (!gekozen.contains(g)) {
                gekozen.add(g);
            }
        }
        if (this.relatie > 0 && !ster && tier < 5 && rng.nextBoolean()) {   // a higher village may ask one kind of your next tier
            List<BestelGoed> volgende = new ArrayList<>();
            for (BestelGoed g : BestelGoed.vanTier(tier + 1)) {
                if (kanMaken.test(g) && !gekozen.contains(g)) {
                    volgende.add(g);
                }
            }
            if (!volgende.isEmpty()) {
                gekozen.set(gekozen.size() - 1, volgende.get(rng.nextInt(volgende.size())));
            }
        }
        // split the value over the kinds, as whole and friendly numbers
        double[] w = new double[gekozen.size()];
        double som = 0;
        for (int i = 0; i < w.length; i++) {
            w[i] = 1 + rng.nextDouble();
            som += w[i];
        }
        int[] n = new int[w.length];
        int totaal = 0;
        for (int i = 0; i < w.length; i++) {
            n[i] = Math.max(1, (int) Math.round(waarde * w[i] / som / gekozen.get(i).waarde));
            totaal += n[i];
        }
        if (totaal > maxN) {   // too many goods (cheap planks and stones): fewer of each
            double s = maxN / (double) totaal;
            for (int i = 0; i < n.length; i++) {
                n[i] = Math.max(1, (int) Math.floor(n[i] * s));
            }
        }
        double echteWaarde = 0;
        for (int i = 0; i < n.length; i++) {
            n[i] = mooi(n[i]);
            regels.add(new Regel(gekozen.get(i).id, n[i]));
            echteWaarde += n[i] * gekozen.get(i).waarde;
        }
        beloning = Math.max(5, (int) Math.round(echteWaarde * factor() / 5.0) * 5);
        reden = rng.nextInt(ster ? STER_REDENEN : REDENEN);
    }

    /** A friendly number: above 20 a multiple of 5. */
    static int mooi(int n) {
        return n > 20 ? n / 5 * 5 : n;
    }

    // =====================================================================================================================

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Fase", fase);
        t.putLong("Start", start);
        t.putLong("Eind", eind);
        t.putBoolean("Ster", ster);
        t.store("Dorp", UUIDUtil.CODEC, dorp);
        t.putInt("Relatie", relatie);
        ListTag l = new ListTag();
        for (Regel r : regels) {
            CompoundTag c = new CompoundTag();
            c.putString("G", r.goed());
            c.putInt("N", r.n());
            l.add(c);
        }
        t.put("Regels", l);
        t.putInt("Beloning", beloning);
        t.putString("Deco", deco);
        t.putInt("Reden", reden);
        t.putString("Hulp", hulp);
        t.putBoolean("Geweigerd", geweigerd);
        return t;
    }

    public static Bestelling load(CompoundTag t) {
        Bestelling b = new Bestelling();
        b.fase = t.getIntOr("Fase", WACHT);
        b.start = t.getLongOr("Start", 0L);
        b.eind = t.getLongOr("Eind", 0L);
        b.ster = t.getBooleanOr("Ster", false);
        b.dorp = t.read("Dorp", UUIDUtil.CODEC).orElse(new UUID(0, 0));
        b.relatie = t.getIntOr("Relatie", 0);
        for (Tag e : t.getListOrEmpty("Regels")) {
            if (e instanceof CompoundTag c) {
                b.regels.add(new Regel(c.getStringOr("G", "ei"), c.getIntOr("N", 1)));
            }
        }
        b.beloning = t.getIntOr("Beloning", 0);
        b.deco = t.getStringOr("Deco", "");
        b.reden = t.getIntOr("Reden", 0);
        b.hulp = t.getStringOr("Hulp", "");
        b.geweigerd = t.getBooleanOr("Geweigerd", false);
        return b;
    }
}
