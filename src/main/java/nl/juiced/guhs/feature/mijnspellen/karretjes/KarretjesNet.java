package nl.juiced.guhs.feature.mijnspellen.karretjes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * The three rail networks of the Wisselgrot (Karretjes-wissels), as pure data: no world, so the GameTests can route carts
 * without a level. A network is a little binary tree on a grid in the baan's own frame: {@code u} forward (the way the
 * carts roll, out of the tunnel), {@code v} to the right (seen from the balcony, looking along u). The tunnel mouth (the
 * marker block {@code karretjes_spoorbegin}, in the floor) is (0, 0); the carts come out of the tunnel at {@link #START}.
 * <p>
 * Every switch ({@link Wissel}) sends a cart left or right: first sideways along its own row, then forward to the next
 * switch or to a bin ({@link Bak}). The game ({@link KarretjesGame}) lays the rails, bins and levers from this
 * (idempotently) and moves the carts along it ({@link Rit}): scripted, so a cart never derails, never gets stuck on a
 * corner and always reacts to the switch the moment it reaches it (vanilla minecart physics on a busy network is not that
 * reliable). Resources: tools/features/mijnspellen.py builds the halls around these networks (same numbers).
 */
public final class KarretjesNet {
    /** The ores in the carts (the colour of the cart's load = the bin it must go to). */
    public enum Erts {
        KOLEN("guhs:kolenerts_vilt", "minecraft:coal_ore"),
        IJZER("guhs:ijzererts_vilt", "minecraft:iron_ore"),
        GOUD("guhs:gouderts_vilt", "minecraft:gold_ore"),
        VADS("guhs:vadserts_vilt", "minecraft:pink_wool"),
        KAASKNABBEL("guhs:kaasknabbelerts_vilt", "minecraft:yellow_wool"),
        DIAMANT("guhs:diamanterts_vilt", "minecraft:diamond_ore");

        /** The block shown in the cart, and the one used when that block doesn't exist. */
        public final String blok, reserve;

        Erts(String blok, String reserve) {
            this.blok = blok;
            this.reserve = reserve;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A cell of the baan's frame. */
    public record Punt(int u, int v) {
        public Punt plus(int du, int dv) {
            return new Punt(u + du, v + dv);
        }
    }

    /** The four sides of a cell in the baan's frame. */
    public enum Kant {
        VOOR(1, 0), ACHTER(-1, 0), LINKS(0, -1), RECHTS(0, 1);

        public final int du, dv;

        Kant(int du, int dv) {
            this.du = du;
            this.dv = dv;
        }

        @Nullable
        public static Kant van(Punt a, Punt b) {
            for (Kant k : values()) {
                if (b.u() - a.u() == k.du && b.v() - a.v() == k.dv) {
                    return k;
                }
            }
            return null;
        }
    }

    public sealed interface Knoop permits Wissel, Bak {
        Punt p();
    }

    /** A switch: lever {@code nr} (true = right). */
    public record Wissel(int nr, Punt p, Knoop links, Knoop rechts) implements Knoop {
        public Knoop kies(boolean rechtsaf) {
            return rechtsaf ? rechts : links;
        }
    }

    /** A bin at the end of a branch, for one ore. */
    public record Bak(int nr, Punt p, Erts erts) implements Knoop {
    }

    /** A whole network: its level, the first switch, the switches by lever number, the bins left to right, its extent. */
    public record Baan(Niveau niveau, Wissel wortel, List<Wissel> wissels, List<Bak> bakken, int vMin, int vMax, int uMax) {
        public List<Erts> ertsen() {
            return bakken.stream().map(Bak::erts).distinct().toList();
        }
    }

    /** Where a cart appears: inside the tunnel, three cells behind the mouth. */
    public static final Punt START = new Punt(-3, 0);
    /** The levers stand on the balcony railing: this row, {@link #HENDEL_Y} above the floor, above their switch's v. */
    public static final int HENDEL_U = -1, HENDEL_Y = 6;
    /** Where the player stands on the balcony (row u, height above the floor). */
    public static final int BALKON_U = -3, BALKON_Y = 5;

    /** The three networks: makkelijk (2 switches, 3 bins), medium (3, 4), lastig (5, 6). */
    public static final List<Baan> BANEN;

    // (python: tools/features/mijnspellen.py KAR_BANEN has the same vMin / vMax / uMax per baan)
    static {
        List<Baan> banen = new ArrayList<>();
        banen.add(baan(Niveau.MAKKELIJK, new Bouwer().wissel(2, 0,
                b -> b.wissel(5, -3, l -> l.bak(8, -5, Erts.KOLEN), r -> r.bak(8, -1, Erts.IJZER)),
                b -> b.bak(8, 3, Erts.VADS))));
        banen.add(baan(Niveau.MEDIUM, new Bouwer().wissel(2, 0,
                b -> b.wissel(5, -4, l -> l.bak(9, -6, Erts.KOLEN), r -> r.bak(9, -2, Erts.GOUD)),
                b -> b.wissel(5, 4, l -> l.bak(9, 2, Erts.VADS), r -> r.bak(9, 6, Erts.IJZER)))));
        banen.add(baan(Niveau.LASTIG, new Bouwer().wissel(2, 0,
                b -> b.wissel(5, -5,
                        l -> l.wissel(8, -7, ll -> ll.bak(11, -9, Erts.KOLEN), lr -> lr.bak(11, -5, Erts.DIAMANT)),
                        r -> r.bak(11, -2, Erts.GOUD)),
                b -> b.wissel(5, 5,
                        l -> l.bak(11, 2, Erts.KAASKNABBEL),
                        r -> r.wissel(8, 7, rl -> rl.bak(11, 5, Erts.IJZER), rr -> rr.bak(11, 9, Erts.VADS))))));
        BANEN = List.copyOf(banen);
    }

    public static Baan baan(Niveau niveau) {
        return BANEN.get(niveau.ordinal());
    }

    /** Builds a tree; switches are numbered left to right (by v), bins too. */
    private static final class Bouwer {
        Knoop wissel(int u, int v, java.util.function.Function<Bouwer, Knoop> links, java.util.function.Function<Bouwer, Knoop> rechts) {
            return new Wissel(-1, new Punt(u, v), links.apply(this), rechts.apply(this));
        }

        Knoop bak(int u, int v, Erts erts) {
            return new Bak(-1, new Punt(u, v), erts);
        }
    }

    private static Baan baan(Niveau niveau, Knoop wortel) {
        List<Punt> wisselPunten = new ArrayList<>(), bakPunten = new ArrayList<>();
        verzamel(wortel, wisselPunten, bakPunten);
        wisselPunten.sort(java.util.Comparator.comparingInt(Punt::v));
        bakPunten.sort(java.util.Comparator.comparingInt(Punt::v));
        Knoop genummerd = nummer(wortel, wisselPunten, bakPunten);
        List<Wissel> wissels = new ArrayList<>();
        List<Bak> bakken = new ArrayList<>();
        verzamelGenummerd(genummerd, wissels, bakken);
        wissels.sort(java.util.Comparator.comparingInt(Wissel::nr));
        bakken.sort(java.util.Comparator.comparingInt(Bak::nr));
        int vMin = 0, vMax = 0, uMax = 0;
        for (Bak b : bakken) {
            vMin = Math.min(vMin, b.p().v());
            vMax = Math.max(vMax, b.p().v());
            uMax = Math.max(uMax, b.p().u());
        }
        return new Baan(niveau, (Wissel) genummerd, List.copyOf(wissels), List.copyOf(bakken), vMin, vMax, uMax);
    }

    private static void verzamel(Knoop k, List<Punt> wissels, List<Punt> bakken) {
        if (k instanceof Wissel w) {
            wissels.add(w.p());
            verzamel(w.links(), wissels, bakken);
            verzamel(w.rechts(), wissels, bakken);
        } else {
            bakken.add(k.p());
        }
    }

    private static Knoop nummer(Knoop k, List<Punt> wissels, List<Punt> bakken) {
        if (k instanceof Wissel w) {
            return new Wissel(wissels.indexOf(w.p()), w.p(), nummer(w.links(), wissels, bakken), nummer(w.rechts(), wissels, bakken));
        }
        Bak b = (Bak) k;
        return new Bak(bakken.indexOf(b.p()), b.p(), b.erts());
    }

    private static void verzamelGenummerd(Knoop k, List<Wissel> wissels, List<Bak> bakken) {
        if (k instanceof Wissel w) {
            wissels.add(w);
            verzamelGenummerd(w.links(), wissels, bakken);
            verzamelGenummerd(w.rechts(), wissels, bakken);
        } else {
            bakken.add((Bak) k);
        }
    }

    // --- the tracks --------------------------------------------------------------------------------------------------------

    /** The cells from the tunnel to the first switch (both included). */
    public static List<Punt> stam(Baan baan) {
        List<Punt> out = new ArrayList<>();
        for (int u = START.u(); u <= baan.wortel().p().u(); u++) {
            out.add(new Punt(u, 0));
        }
        return out;
    }

    /** The cells from a switch (included) to the next knoop (included): sideways along the switch's row, then forward. */
    public static List<Punt> tak(Punt van, Knoop naar) {
        List<Punt> out = new ArrayList<>();
        out.add(van);
        int stap = Integer.signum(naar.p().v() - van.v());
        for (int v = van.v() + stap; stap != 0 && v != naar.p().v() + stap; v += stap) {
            out.add(new Punt(van.u(), v));
        }
        for (int u = van.u() + 1; u <= naar.p().u(); u++) {
            out.add(new Punt(u, naar.p().v()));
        }
        return out;
    }

    /** Where a cart with these switch settings ends (pure: for the tests and the hints). */
    public static Bak route(Baan baan, boolean[] standen) {
        Knoop k = baan.wortel();
        while (k instanceof Wissel w) {
            k = w.kies(stand(standen, w.nr()));
        }
        return (Bak) k;
    }

    /** The settings that send a cart to this bin (true = right), only the switches on its way set. */
    public static boolean[] standenNaar(Baan baan, Bak doel) {
        boolean[] out = new boolean[baan.wissels().size()];
        zoek(baan.wortel(), doel, out);
        return out;
    }

    private static boolean zoek(Knoop k, Bak doel, boolean[] out) {
        if (k instanceof Bak b) {
            return b.nr() == doel.nr();
        }
        Wissel w = (Wissel) k;
        if (zoek(w.links(), doel, out)) {
            out[w.nr()] = false;
            return true;
        }
        if (zoek(w.rechts(), doel, out)) {
            out[w.nr()] = true;
            return true;
        }
        return false;
    }

    private static boolean stand(boolean[] standen, int nr) {
        return nr >= 0 && nr < standen.length && standen[nr];
    }

    /**
     * Every rail cell of the network with the sides it connects (a switch: back, left and right; which side is open
     * depends on its lever). The bins are not in it (they get a bin block).
     */
    public static Map<Punt, Set<Kant>> rails(Baan baan) {
        Map<Punt, Set<Kant>> out = new LinkedHashMap<>();
        verbind(out, stam(baan));
        out.computeIfAbsent(START, k -> EnumSet.noneOf(Kant.class)).add(Kant.ACHTER);   // (straight into the tunnel)
        takken(baan.wortel(), out);
        return out;
    }

    private static void takken(Knoop k, Map<Punt, Set<Kant>> out) {
        if (k instanceof Wissel w) {
            for (Knoop naar : List.of(w.links(), w.rechts())) {
                List<Punt> cellen = tak(w.p(), naar);
                if (naar instanceof Bak) {
                    cellen = cellen.subList(0, cellen.size() - 1);
                    Punt laatste = cellen.get(cellen.size() - 1);
                    verbind(out, cellen);
                    out.computeIfAbsent(laatste, x -> EnumSet.noneOf(Kant.class)).add(Kant.VOOR);   // (into the bin)
                } else {
                    verbind(out, cellen);
                }
                takken(naar, out);
            }
        }
    }

    private static void verbind(Map<Punt, Set<Kant>> out, List<Punt> cellen) {
        for (int i = 0; i < cellen.size(); i++) {
            Set<Kant> kanten = out.computeIfAbsent(cellen.get(i), x -> EnumSet.noneOf(Kant.class));
            if (i > 0) {
                kanten.add(Kant.van(cellen.get(i), cellen.get(i - 1)));
            }
            if (i + 1 < cellen.size()) {
                kanten.add(Kant.van(cellen.get(i), cellen.get(i + 1)));
            }
        }
    }

    /** The two sides a rail cell shows: a switch shows back + the side its lever opens, every other cell its own two. */
    public static Kant[] getoond(Baan baan, Punt cel, Set<Kant> kanten, boolean[] standen) {
        for (Wissel w : baan.wissels()) {
            if (w.p().equals(cel)) {
                Kant open = Kant.van(cel, tak(cel, w.kies(stand(standen, w.nr()))).get(1));
                return new Kant[] {Kant.ACHTER, open};
            }
        }
        Kant[] k = kanten.toArray(new Kant[0]);
        return k.length >= 2 ? new Kant[] {k[0], k[1]} : new Kant[] {Kant.ACHTER, Kant.VOOR};
    }

    // --- a cart on its way -------------------------------------------------------------------------------------------------

    /**
     * One cart rolling through a network: the cells of the stretch it is on (from the last knoop to the next), how far
     * along it is (blocks), and the knoop at the end of it. At a switch it takes the branch the lever says at that very
     * moment; at a bin it is done.
     */
    public static final class Rit {
        public final Baan baan;
        public final Erts erts;
        private List<Punt> cellen;
        private Knoop doel;
        private double d;
        @Nullable
        private Bak aangekomen;
        /** How far it rolled in all (blocks). */
        private double totaal;

        public Rit(Baan baan, Erts erts) {
            this.baan = baan;
            this.erts = erts;
            this.cellen = stam(baan);
            this.doel = baan.wortel();
        }

        /** Rolls on; returns the bin it arrived in on this step (null: still rolling, or arrived before). */
        @Nullable
        public Bak stap(double snelheid, boolean[] standen) {
            if (aangekomen != null) {
                return null;
            }
            d += snelheid;
            totaal += snelheid;
            while (d >= cellen.size() - 1) {
                d -= cellen.size() - 1;
                if (doel instanceof Bak b) {
                    aangekomen = b;
                    d = 0;
                    cellen = List.of(b.p(), b.p());
                    return b;
                }
                Wissel w = (Wissel) doel;
                Knoop naar = w.kies(stand(standen, w.nr()));
                cellen = tak(w.p(), naar);
                doel = naar;
            }
            return null;
        }

        @Nullable
        public Bak aangekomen() {
            return aangekomen;
        }

        public double totaal() {
            return totaal;
        }

        /** Where it is now: {u, v} (between cell centres). */
        public double[] positie() {
            int i = Math.min((int) Math.floor(d), cellen.size() - 2);
            double f = d - i;
            Punt a = cellen.get(i), b = cellen.get(i + 1);
            return new double[] {a.u() + (b.u() - a.u()) * f, a.v() + (b.v() - a.v()) * f};
        }

        /** The way it rolls now (a unit step in u or v). */
        public Kant richting() {
            int i = Math.min((int) Math.floor(d), cellen.size() - 2);
            Kant k = Kant.van(cellen.get(i), cellen.get(i + 1));
            return k == null ? Kant.VOOR : k;
        }

        /** The knoop it rolls to now. */
        public Knoop doel() {
            return doel;
        }
    }

    private KarretjesNet() {
    }
}
