package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import nl.juiced.guhs.feature.erfenis.Prijzen.Doel;
import nl.juiced.guhs.feature.erfenis.Prijzen.Prijs;
import nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen;

/**
 * De Erfenis ("Een opknapper!"): one village. The same class holds the rules on the server (the authority) and the copy
 * the client draws (sent whole by {@link ErfenisPayloads.Sync}). Nothing here touches the world: stamping is {@link Stempel},
 * the plot's geometry {@link Plot}, the list of all bases {@link ErfenisData}, the server side {@link Erfenis}.
 * <ul>
 *   <li><b>Money and goods:</b> Guhlden in the Hoeve (capped per Hoeve level), goods in the storage (Hoeve + Opslagschuren,
 *   one shared room), main-world items handed in at the Loket ({@link #loket}).</li>
 *   <li><b>Time</b> is real-world millis; {@link #bijwerken} catches up on everything since the last time (production, builds,
 *   residents moving in), also for the hours nobody was there. The client runs the same to show live numbers.</li>
 *   <li><b>Builders:</b> 1, 2 or 3 jobs at a time ({@link #bouwen}): new buildings, upgrades and clearing plukjes.</li>
 *   <li><b>Land:</b> the open middle, the 72 plukjes ({@link #opgeruimd}), the 16 kavels ({@link #kavels}).</li>
 *   <li><b>Rules:</b> every {@code kan*} returns null when allowed, else a {@link Fout}; the actions check again.</li>
 * </ul>
 * After {@link #bijwerken} the server drains {@link #momenten} (story moments), {@link #klaar} (finished jobs to stamp) and
 * {@link #meldingen} (toasts for the owner).
 */
public final class Base {
    /** A builder's job: {@code soort} GEBOUW (building {@code ref} becomes level {@code naar}) or PLUKJE (plukje {@code ref} is cleared). */
    public record Bouw(int soort, int ref, int naar, long start, long eind) {
        public static final int GEBOUW = 0, PLUKJE = 1;

        public float voortgang(long nu) {
            return eind <= start ? 1f : Math.max(0f, Math.min(1f, (nu - start) / (float) (eind - start)));
        }
    }

    /** Why something may not (or a message): a lang key and its arguments (numbers, or other lang keys). */
    public record Fout(String key, String... args) {
    }

    /** A placed decoration piece (phase 2: the deco catalogue; the data lives here already). */
    public record Deco(String id, int x, int z, int rot) {
    }

    /** One of your own tamed guhs staying in the Guhpension: its stored data (server only), and the building it helps (-1: none). */
    public static final class Loge {
        public final UUID id;
        public final CompoundTag opslag;
        public int gebouw;

        public Loge(UUID id, CompoundTag opslag, int gebouw) {
            this.id = id;
            this.opslag = opslag;
            this.gebouw = gebouw;
        }
    }

    /** One building of a hand-made or generated layout (the world map's NPC villages). */
    public record Plan(GebouwType type, int level, int x, int z, int rot) {
    }

    /** What the makelaar leaves you: 300 Guhlden and opa's last planks and eggs (so the very first builds can start). */
    public static final int START_GUHLDEN = 300;
    public static final Map<Goed, Integer> START_GOED = Map.of(Goed.PLANK, 50, Goed.EI, 10);

    public final UUID id;
    @Nullable
    public UUID eigenaar;
    public String naam;
    public boolean npc;
    public int cx, cz;
    public final List<Gebouw> gebouwen = new ArrayList<>();
    public int volgendeId = 1;
    public final List<Bouw> bouwen = new ArrayList<>();
    public int guhlden;
    public final Map<Goed, Double> goederen = new EnumMap<>(Goed.class);
    /** Which Bouwkeet blocks the "bouwblok" goods in the storage are (block id -> count). */
    public final Map<String, Integer> bouwblokSoorten = new LinkedHashMap<>();
    public int bewoners;
    /** When the next resident moves in (0: no room now). */
    public long volgendeIntrek;
    public int bestellingen;
    public final boolean[] opgeruimd = new boolean[Plot.PLUKJES];
    /** Bought kavels (bit k = kavel k, see {@link Plot#kavelOp}). */
    public int kavels;
    public final List<String> vondsten = new ArrayList<>();
    public final Map<String, Integer> decoOpslag = new LinkedHashMap<>();
    public final List<Deco> deco = new ArrayList<>();
    /** The path layer: per tile 0 (none) or 1 + an index into {@link #padSoorten}. */
    public byte[] paden = new byte[Plot.MAAT * Plot.MAAT];
    public final List<String> padSoorten = new ArrayList<>();
    public int knusDeco;
    /** Main-world items handed in at the Loket (item id -> count). */
    public final Map<String, Integer> loket = new LinkedHashMap<>();
    /** The Leverkist allowance per good (balans 9.4: cap per hour, saves up to 8 hours) and when it was last counted. */
    public final Map<Goed, Double> leverTegoed = new EnumMap<>(Goed.class);
    public long leverBijgewerkt;
    public final List<Loge> loges = new ArrayList<>();
    /** The VK arriving at the Vadsmast (before its cap), as last known (the server sets it before every catch-up). */
    public int vk;
    /** The tutorial's first Kippenhok is free. */
    public boolean kippenhokGratis = true;
    /** The owner arrived here at least once ("aangekomen" moment); the Knusheid last announced ("knusheid:<n>" moment). */
    public boolean aangekomen;
    /** The makelaar's tutorial (Makelaarstour): 0 not started, 1..5 the steps, 6 done (he went on holiday). */
    public int tutorial;
    public int knusGemeld;
    /** (1.9 afwerking) Sherlock's reward flags, mirrored here so the village itself can use them (stamp, recipes, client):
     *  {@code recept_theetaartje}, {@code oma_bestelling}, {@code opas_kelder}, {@code bertha_optreden}, {@code bureau_eindlook} ... */
    public final List<String> beloningen = new ArrayList<>();
    public long bijgewerkt;
    /** The blocks in the world are behind: stamp on the next visit. */
    public boolean moetStempelen = true;
    /** (client only) Sherlock has a case waiting: the magnifier bubble above the Detectivebureau. */
    public transient boolean zaakKlaar;

    // transient: what happened during the last catch-up, for the server to act on (never saved or synced)
    public final transient List<String> momenten = new ArrayList<>();
    public final transient List<Bouw> klaar = new ArrayList<>();
    public final transient List<Fout> meldingen = new ArrayList<>();

    public Base(UUID id, @Nullable UUID eigenaar, String naam, boolean npc, int cx, int cz) {
        this.id = id;
        this.eigenaar = eigenaar;
        this.naam = naam;
        this.npc = npc;
        this.cx = cx;
        this.cz = cz;
    }

    // =====================================================================================================================
    // a new base
    // =====================================================================================================================

    /** The Hoeve's spot: the north-west of the open middle (the rest of the middle stays free for the first buildings). */
    public static final int HOEVE_X = Plot.MIDDEN + 1, HOEVE_Z = Plot.MIDDEN + 1;

    public static Base nieuw(UUID eigenaar, String naam, int cx, int cz, long nu) {
        Base b = new Base(eigenaar, eigenaar, naam, false, cx, cz);
        b.vulStart(nu);
        return b;
    }

    /** Back to the start (keeps id, owner and cell): the makelaar's krakkemikkig schuurtje on an overgrown plot. */
    public void vulStart(long nu) {
        gebouwen.clear();
        volgendeId = 1;
        bouwen.clear();
        guhlden = START_GUHLDEN;
        goederen.clear();
        START_GOED.forEach((g, n) -> goederen.put(g, n.doubleValue()));
        bouwblokSoorten.clear();
        bewoners = 0;
        volgendeIntrek = 0;
        bestellingen = 0;
        java.util.Arrays.fill(opgeruimd, false);
        kavels = 0;
        vondsten.clear();
        decoOpslag.clear();
        deco.clear();
        paden = new byte[Plot.MAAT * Plot.MAAT];
        padSoorten.clear();
        knusDeco = 0;
        loket.clear();
        leverTegoed.clear();
        for (Goed g : Goed.values()) {
            if (g.capPerUur > 0) {
                leverTegoed.put(g, g.capPerUur * 8.0);
            }
        }
        leverBijgewerkt = nu;
        kippenhokGratis = true;
        bijgewerkt = nu;
        nieuwGebouw(GebouwType.HOEVE, 1, HOEVE_X, HOEVE_Z, 0, nu);
        moetStempelen = true;
    }

    /** An NPC village (dev /guhs erfenis npc): a Hoeve of this level and as many of everything as that level allows. */
    public static Base npc(UUID id, String naam, int cx, int cz, int hoeveLevel, long seed, long nu) {
        Base b = new Base(id, null, naam, true, cx, cz);
        b.allesOpen();
        Random rng = new Random(seed);
        int h = Math.max(1, Math.min(GebouwType.MAX_HOEVE, hoeveLevel));
        b.nieuwGebouw(GebouwType.HOEVE, h, HOEVE_X, HOEVE_Z, 0, nu);
        for (GebouwType t : GebouwType.values()) {
            if (t == GebouwType.HOEVE) {
                continue;
            }
            int n = t.maxAantal(h);
            for (int i = 0, poging = 0; i < n && poging < 300; poging++) {
                int rot = rng.nextInt(4);
                int x = 2 + rng.nextInt(Plot.MAAT - 4 - t.b(rot)), z = 2 + rng.nextInt(Plot.MAAT - 4 - t.d(rot));
                if (b.vrij(x, z, t.b(rot), t.d(rot), -1)) {
                    b.nieuwGebouw(t, 1 + rng.nextInt(Math.max(1, t.maxLevel(h))), x, z, rot, nu);
                    i++;
                }
            }
        }
        b.bewoners = b.maxBewoners();
        b.guhlden = rng.nextInt(Productie.guhldenCap(h) + 1);
        return b;
    }

    public static Base npc(UUID id, String naam, int cx, int cz, List<Plan> plan, int guhlden, long nu) {
        Base b = new Base(id, null, naam, true, cx, cz);
        b.allesOpen();
        b.vervangPlan(plan, nu);
        b.guhlden = Math.max(0, Math.min(guhlden, Productie.guhldenCap(b.hoeveLevel())));
        return b;
    }

    /** Replaces every building by this list IN PLACE (an NPC village that grew). Overlapping or outside entries are skipped. */
    public void vervangPlan(List<Plan> plan, long nu) {
        gebouwen.clear();
        volgendeId = 1;
        bouwen.clear();
        for (Plan p : plan) {
            if (p.type() == GebouwType.HOEVE && hoeve() != null) {
                continue;
            }
            int rot = p.rot() & 3;
            if (vrij(p.x(), p.z(), p.type().b(rot), p.type().d(rot), -1)) {
                nieuwGebouw(p.type(), Math.max(1, Math.min(p.type().topLevel(), p.level())), p.x(), p.z(), rot, nu);
            }
        }
        if (hoeve() == null && vrij(HOEVE_X, HOEVE_Z, 11, 11, -1)) {
            nieuwGebouw(GebouwType.HOEVE, 1, HOEVE_X, HOEVE_Z, 0, nu);
        }
        bewoners = maxBewoners();
        moetStempelen = true;
    }

    /** (NPC villages) every plukje cleared and every kavel bought: the whole plot is open land. */
    void allesOpen() {
        java.util.Arrays.fill(opgeruimd, true);
        kavels = (1 << Plot.KAVELS) - 1;
    }

    Gebouw nieuwGebouw(GebouwType type, int level, int x, int z, int rot, long nu) {
        Gebouw g = new Gebouw(volgendeId++, type, level, x, z, rot);
        g.palet = GebouwVormen.vormTier(type.id, Math.max(1, level));
        gebouwen.add(g);
        return g;
    }

    // =====================================================================================================================
    // what follows from the buildings
    // =====================================================================================================================

    @Nullable
    public Gebouw gebouw(int id) {
        for (Gebouw g : gebouwen) {
            if (g.id == id) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    public Gebouw op(int tx, int tz) {
        for (Gebouw g : gebouwen) {
            if (g.bevat(tx, tz)) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    public Gebouw hoeve() {
        for (Gebouw g : gebouwen) {
            if (g.type == GebouwType.HOEVE) {
                return g;
            }
        }
        return null;
    }

    public int hoeveLevel() {
        Gebouw s = hoeve();
        return s == null ? 1 : Math.max(1, s.level);
    }


    /** The palettes unlocked: 1 .. the tier the Hoeve reached (T1 = H1-2 ... T5 = H9-10). */
    public int maxPalet() {
        return (hoeveLevel() + 1) / 2;
    }

    public int aantal(GebouwType type) {
        int n = 0;
        for (Gebouw g : gebouwen) {
            if (g.type == type) {
                n++;
            }
        }
        return n;
    }

    /** The highest built level of this type (0: none built). */
    public int besteLevel(GebouwType type) {
        int l = 0;
        for (Gebouw g : gebouwen) {
            if (g.type == type) {
                l = Math.max(l, g.level);
            }
        }
        return l;
    }

    /** Goods room: the Hoeve plus every Opslagschuur. */
    public int opslag() {
        int cap = Productie.HOEVE_OPSLAG;
        for (Gebouw g : gebouwen) {
            if (g.type == GebouwType.OPSLAGSCHUUR) {
                cap += Productie.opslagschuur(g.level);
            }
        }
        return cap;
    }

    /** Goods in the storage now (rounded up: a half egg takes room too). */
    public int opslagGebruikt() {
        double s = 0;
        for (double v : goederen.values()) {
            s += v;
        }
        return (int) Math.ceil(s - 1e-6);
    }

    public int opslagVrij() {
        return Math.max(0, opslag() - opslagGebruikt());
    }

    public int guhldenCap() {
        return Productie.guhldenCap(hoeveLevel());
    }

    public double goed(Goed g) {
        return goederen.getOrDefault(g, 0.0);
    }

    /** Whole units of this good in the storage. */
    public int heeft(Goed g) {
        return (int) Math.floor(goed(g) + 1e-6);
    }

    void verander(Goed g, double delta) {
        double n = Math.max(0, goed(g) + delta);
        if (n < 1e-9) {
            goederen.remove(g);
        } else {
            goederen.put(g, n);
        }
    }

    /** Adds Guhlden up to the Hoeve's cap; returns what fitted. */
    public int geefGuhlden(int n) {
        int erin = Math.max(0, Math.min(n, guhldenCap() - guhlden));
        guhlden += erin;
        return erin;
    }

    /** Adds goods up to the free room; returns what fitted (Bouwkeet blocks get the default kind). */
    public int geefGoed(Goed g, int n) {
        int erin = Math.max(0, Math.min(n, opslagVrij()));
        if (erin > 0) {
            verander(g, erin);
            if (g == Goed.BOUWBLOK) {
                bouwblokSoorten.merge(Bouwkeet.STANDAARD, erin, Integer::sum);
            }
        }
        return erin;
    }

    /** Takes whole goods (false: not enough, nothing taken). */
    public boolean neemGoed(Goed g, int n) {
        if (heeft(g) < n) {
            return false;
        }
        verander(g, -n);
        if (g == Goed.BOUWBLOK) {
            neemSoorten(n);
        }
        return true;
    }

    /** Takes n Bouwkeet blocks of whatever kinds there are (the biggest piles first). */
    private Map<String, Integer> neemSoorten(int n) {
        Map<String, Integer> uit = new LinkedHashMap<>();
        while (n > 0 && !bouwblokSoorten.isEmpty()) {
            String grootste = Collections.max(bouwblokSoorten.entrySet(), Map.Entry.comparingByValue()).getKey();
            int heeft = bouwblokSoorten.get(grootste);
            int neem = Math.min(n, Math.max(1, Math.min(heeft, (n + 1) / 2)));
            uit.merge(grootste, neem, Integer::sum);
            n -= neem;
            if (heeft - neem <= 0) {
                bouwblokSoorten.remove(grootste);
            } else {
                bouwblokSoorten.put(grootste, heeft - neem);
            }
        }
        if (n > 0) {
            uit.merge(Bouwkeet.STANDAARD, n, Integer::sum);
        }
        return uit;
    }

    /** Residents room: every Guhhutje plus the Speeltuin's extra. */
    public int maxBewoners() {
        int n = 0;
        for (Gebouw g : gebouwen) {
            if (g.type == GebouwType.GUHHUTJE) {
                n += Productie.hutjeBewoners(g.level);
            } else if (g.type == GebouwType.SPEELTUIN) {
                n += Productie.speeltuinBewoners(g.level);
            }
        }
        return n;
    }

    /** All workplaces of the village (for the residents panel). */
    public int werkplekken() {
        int n = 0;
        for (Gebouw g : gebouwen) {
            n += Productie.plekken(g.type, g.level);
        }
        return n;
    }

    /** The VK the village really has: what arrives, capped by the Vadsmast (none without a Vadsmast). */
    public int vkInDorp() {
        int cap = Productie.vadsmastCap(besteLevel(GebouwType.VADSMAST));
        return Math.max(0, Math.min(vk, cap));
    }

    /** Café + Muziektent bonus in percent. */
    public int dorpsBonus() {
        return Productie.bonusProcent(besteLevel(GebouwType.DORPSCAFE)) + Productie.bonusProcent(besteLevel(GebouwType.MUZIEKTENT));
    }

    public int logesBij(int gebouwId) {
        int n = 0;
        for (Loge l : loges) {
            if (l.gebouw == gebouwId) {
                n++;
            }
        }
        return n;
    }

    public int pensionPlekken() {
        return Productie.pensionPlekken(besteLevel(GebouwType.GUHPENSION));
    }

    public int bouwers() {
        return Productie.bouwers(besteLevel(GebouwType.TIMMERSCHUUR));
    }

    public int vrijeBouwers() {
        return Math.max(0, bouwers() - bouwen.size());
    }

    public boolean inAanbouw(Gebouw g) {
        return bouwVan(Bouw.GEBOUW, g.id) != null;
    }

    @Nullable
    public Bouw bouwVan(int soort, int ref) {
        for (Bouw b : bouwen) {
            if (b.soort() == soort && b.ref() == ref) {
                return b;
            }
        }
        return null;
    }

    public int dierenverblijven() {
        int n = 0;
        for (Gebouw g : gebouwen) {
            if (g.type.familie == GebouwType.Familie.DIEREN && g.level > 0) {
                n++;
            }
        }
        return n;
    }

    public int opgeruimdTotaal() {
        int n = 0;
        for (boolean b : opgeruimd) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    public int opgeruimdRing(int ring) {
        int n = 0;
        for (int i = 0; i < Plot.PLUKJES; i++) {
            if (opgeruimd[i] && Plot.ring(i) == ring) {
                n++;
            }
        }
        return n;
    }

    public int kavelsGekocht() {
        return Integer.bitCount(kavels);
    }

    public boolean kavelGekocht(int k) {
        return (kavels >> k & 1) != 0;
    }

    /** May something stand on this tile (the middle, a cleared plukje, a bought kavel)? */
    public boolean open(int tx, int tz) {
        if (!Plot.opPlot(tx, tz)) {
            return false;
        }
        if (Plot.inMidden(tx, tz)) {
            return true;
        }
        int p = Plot.plukjeOp(tx, tz);
        if (p >= 0) {
            return opgeruimd[p];
        }
        int k = Plot.kavelOp(tx, tz);
        return k >= 0 && kavelGekocht(k);
    }

    /** Knusheid: the buildings (2 per level, the Hoeve 10), the paths (1 per 10 tiles) and the deco ({@link Knusheid#zet}). */
    public int knusheid() {
        int k = 0;
        for (Gebouw g : gebouwen) {
            k += g.type.knus(g.level);
        }
        int paadjes = 0;
        for (byte p : paden) {
            if (p != 0) {
                paadjes++;
            }
        }
        return k + paadjes / 10 + knusDeco + kaartjesbordExtra();
    }

    /** (1.9 afwerking) How many postcards of the makelaar this village has (found while clearing + sent from his holiday). */
    public int ansichtkaarten() {
        int n = 0;
        for (String v : vondsten) {
            if (v.startsWith("ansichtkaart")) {
                n++;
            }
        }
        return Math.min(8, n);
    }

    /** The Kaartjesbord (deco.md #64) into the decoschuurtje, unless the village already has one (stored or placed). */
    public void kaartjesbordErbij() {
        if (!decoOpslag.containsKey("kaartjesbord") && deco.stream().noneMatch(d -> d.id().equals("kaartjesbord"))) {
            decoOpslag.put("kaartjesbord", 1);
        }
    }

    /** deco.md #64: the board grows with every card, 2 Knusheid per card (the piece itself already counts 2). */
    private int kaartjesbordExtra() {
        boolean staat = false;
        for (Deco d : deco) {
            staat |= d.id().equals("kaartjesbord");
        }
        return staat ? 2 * Math.max(0, ansichtkaarten() - 1) : 0;
    }

    // =====================================================================================================================
    // deco from the decoschuurtje (finds, orders, Burenhulp, Sherlock): placed straight away, free to put away again
    // =====================================================================================================================

    /** {breedte, diepte} of a placed or new deco piece after turning. */
    public static int[] decoMaat(String id, int rot) {
        int[] m = GebouwVormen.decoMaat(id);
        return (rot & 1) == 0 ? new int[] {m[0], m[1]} : new int[] {m[1], m[0]};
    }

    /** The placed deco piece on this tile, or null. */
    @Nullable
    public Deco decoOp(int tx, int tz) {
        for (Deco d : deco) {
            int[] m = decoMaat(d.id(), d.rot());
            if (tx >= d.x() && tx < d.x() + m[0] && tz >= d.z() && tz < d.z() + m[1]) {
                return d;
            }
        }
        return null;
    }

    @Nullable
    public Fout kanDeco(String id, int x, int z, int rot) {
        if (decoOpslag.getOrDefault(id, 0) <= 0) {
            return new Fout("erfenis.fout.deco_op");
        }
        int[] m = decoMaat(id, rot);
        if (m[0] <= 0) {
            return new Fout("erfenis.fout.weg");
        }
        Fout f = plekFout(x, z, m[0], m[1], -1);
        if (f != null) {
            return f;
        }
        for (int dx = 0; dx < m[0]; dx++) {
            for (int dz = 0; dz < m[1]; dz++) {
                if (decoOp(x + dx, z + dz) != null) {
                    return new Fout("erfenis.fout.overlap");
                }
            }
        }
        return null;
    }

    /** Places a piece from the decoschuurtje. */
    @Nullable
    public Fout plaatsDeco(String id, int x, int z, int rot) {
        Fout f = kanDeco(id, x, z, rot);
        if (f != null) {
            return f;
        }
        decoOpslag.merge(id, -1, Integer::sum);
        decoOpslag.remove(id, 0);
        deco.add(new Deco(id, x, z, rot & 3));
        Knusheid.zet(this, ErfenisDeco.knusheid(deco));
        return null;
    }

    /** Puts a placed piece back into the decoschuurtje. */
    public void bergDecoOp(Deco d) {
        if (deco.remove(d)) {
            decoOpslag.merge(d.id(), 1, Integer::sum);
            Knusheid.zet(this, ErfenisDeco.knusheid(deco));
        }
    }

    /** A building comes to stand here: the deco under it goes back into the decoschuurtje (never lost). */
    void decoWijkt(int x, int z, int w, int h) {
        for (Deco d : List.copyOf(deco)) {
            int[] m = decoMaat(d.id(), d.rot());
            if (d.x() < x + w && d.x() + m[0] > x && d.z() < z + h && d.z() + m[1] > z) {
                bergDecoOp(d);
            }
        }
    }

    /** Is a footprint of w x h at (x, z) on open land and free (ignoring building {@code negeer})? */
    public boolean vrij(int x, int z, int w, int h, int negeer) {
        if (x < 0 || z < 0 || x + w > Plot.MAAT || z + h > Plot.MAAT) {
            return false;
        }
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < h; dz++) {
                if (!open(x + dx, z + dz)) {
                    return false;
                }
            }
        }
        for (Gebouw g : gebouwen) {
            if (g.id != negeer && g.overlapt(x, z, w, h)) {
                return false;
            }
        }
        return true;
    }

    public boolean binnen(int x, int z, int w, int h) {
        return x >= 0 && z >= 0 && x + w <= Plot.MAAT && z + h <= Plot.MAAT;
    }

    // =====================================================================================================================
    // prices and goals
    // =====================================================================================================================

    /** What a new building of this type costs now (the tutorial's first Kippenhok is free). */
    public Prijs prijsNieuw(GebouwType t) {
        if (t == GebouwType.KIPPENHOK && kippenhokGratis && aantal(t) == 0) {
            return new Prijs(0, Map.of(), Map.of(), Prijzen.gebouw(t, 1).tijdMs());
        }
        return Prijzen.gebouw(t, 1);
    }

    /** Can this price be paid now? Null = yes. Main-world items that don't exist (a slice not merged) are skipped. */
    @Nullable
    public Fout kanBetalen(Prijs p) {
        if (guhlden < p.guhlden()) {
            return new Fout("erfenis.fout.guhlden", Integer.toString(p.guhlden()));
        }
        for (Map.Entry<Goed, Integer> e : p.goederen().entrySet()) {
            if (heeft(e.getKey()) < e.getValue()) {
                return new Fout("erfenis.fout.goed", Integer.toString(e.getValue() - heeft(e.getKey())), e.getKey().naamKey());
            }
        }
        for (Map.Entry<String, Integer> e : p.hoofdwereld().entrySet()) {
            if (Goed.item(e.getKey()) == null) {
                continue;
            }
            if (aantal(GebouwType.LOKET) == 0) {
                return new Fout("erfenis.fout.loket_nodig");
            }
            if (loket.getOrDefault(e.getKey(), 0) < e.getValue()) {
                return new Fout("erfenis.fout.loket");
            }
        }
        return null;
    }

    void betaal(Prijs p) {
        guhlden -= p.guhlden();
        p.goederen().forEach((g, n) -> neemGoed(g, n));
        p.hoofdwereld().forEach((id, n) -> {
            if (Goed.item(id) != null) {
                int over = loket.getOrDefault(id, 0) - n;
                if (over > 0) {
                    loket.put(id, over);
                } else {
                    loket.remove(id);
                }
            }
        });
    }

    /** The current value of a Hoeve goal. */
    public int doelStand(String soort) {
        return switch (soort) {
            case "bewoners" -> bewoners;
            case "bestellingen" -> bestellingen;
            case "plukjes" -> opgeruimdTotaal();
            case "ring1" -> opgeruimdRing(1);
            case "ring12" -> opgeruimdRing(1) + opgeruimdRing(2);
            case "dierenverblijven" -> dierenverblijven();
            case "knusheid" -> knusheid();
            case "vadskracht" -> vkInDorp();
            case "kavels" -> kavelsGekocht();
            case "loket" -> aantal(GebouwType.LOKET) > 0 && besteLevel(GebouwType.LOKET) > 0 ? 1 : 0;
            default -> 0;
        };
    }

    /** The goals for the Hoeve's next level that are not reached yet. */
    public List<Doel> openDoelen(int naar) {
        List<Doel> l = new ArrayList<>();
        for (Doel d : Prijzen.doelen(naar)) {
            if (doelStand(d.soort()) < d.n()) {
                l.add(d);
            }
        }
        return l;
    }

    // =====================================================================================================================
    // the rules
    // =====================================================================================================================

    @Nullable
    public Fout kanKopen(GebouwType type) {
        int h = hoeveLevel();
        if (type == GebouwType.HOEVE) {
            return new Fout("erfenis.fout.limiet", type.naamKey());
        }
        int heeft = aantal(type);
        if (heeft >= type.maxAantal(h)) {
            int s = type.ontgrendeltAantal(heeft);
            return s > 0 ? new Fout("erfenis.fout.slot", Integer.toString(s)) : new Fout("erfenis.fout.limiet", type.naamKey());
        }
        if (vrijeBouwers() <= 0) {
            return new Fout("erfenis.fout.bouwer");
        }
        return kanBetalen(prijsNieuw(type));
    }

    @Nullable
    public Fout kanPlaatsen(GebouwType type, int x, int z, int rot) {
        Fout f = kanKopen(type);
        return f != null ? f : plekFout(x, z, type.b(rot), type.d(rot), -1);
    }

    @Nullable
    private Fout plekFout(int x, int z, int w, int h, int negeer) {
        if (!binnen(x, z, w, h)) {
            return new Fout("erfenis.fout.buiten");
        }
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < h; dz++) {
                if (!open(x + dx, z + dz)) {
                    return new Fout("erfenis.fout.overwoekerd");
                }
            }
        }
        return vrij(x, z, w, h, negeer) ? null : new Fout("erfenis.fout.overlap");
    }

    @Nullable
    public Fout kanVerplaatsen(int id, int x, int z) {
        Gebouw g = gebouw(id);
        return g == null ? new Fout("erfenis.fout.weg") : plekFout(x, z, g.b(), g.d(), id);
    }

    @Nullable
    public Fout kanDraaien(int id) {
        Gebouw g = gebouw(id);
        if (g == null) {
            return new Fout("erfenis.fout.weg");
        }
        int r = (g.rot + 1) & 3;
        return plekFout(g.x, g.z, g.type.b(r), g.type.d(r), id);
    }

    /** The price of this building's next level. */
    public Prijs prijsVolgende(Gebouw g) {
        return Prijzen.gebouw(g.type, g.level + 1);
    }

    @Nullable
    public Fout kanVerbeteren(int id) {
        Gebouw g = gebouw(id);
        if (g == null) {
            return new Fout("erfenis.fout.weg");
        }
        if (g.level >= g.type.topLevel()) {
            return new Fout("erfenis.fout.max");
        }
        if (inAanbouw(g)) {
            return new Fout("erfenis.fout.bezig");
        }
        int naar = g.level + 1;
        if (g.type == GebouwType.HOEVE) {
            List<Doel> open = openDoelen(naar);
            if (!open.isEmpty()) {
                Doel d = open.get(0);
                return new Fout("erfenis.fout.doel", "erfenis.doel." + d.soort(), Integer.toString(doelStand(d.soort())), Integer.toString(d.n()));
            }
        } else if (naar > g.type.maxLevel(hoeveLevel())) {
            return new Fout("erfenis.fout.hoeve_level", Integer.toString(g.type.ontgrendeltLevel(naar)));
        }
        if (vrijeBouwers() <= 0) {
            return new Fout("erfenis.fout.bouwer");
        }
        return kanBetalen(prijsVolgende(g));
    }

    @Nullable
    public Fout kanOpruimen(int n) {
        if (n < 0 || n >= Plot.PLUKJES) {
            return new Fout("erfenis.fout.weg");
        }
        if (opgeruimd[n]) {
            return new Fout("erfenis.fout.al_opgeruimd");
        }
        if (bouwVan(Bouw.PLUKJE, n) != null) {
            return new Fout("erfenis.fout.bezig");
        }
        int ring = Plot.ring(n);
        if (hoeveLevel() < Prijzen.ringOpent(ring)) {
            return new Fout("erfenis.fout.slot", Integer.toString(Prijzen.ringOpent(ring)));
        }
        if (vrijeBouwers() <= 0) {
            return new Fout("erfenis.fout.bouwer");
        }
        return kanBetalen(Prijzen.plukje(ring));
    }

    @Nullable
    public Fout kanKavel(int k) {
        if (k < 0 || k >= Plot.KAVELS) {
            return new Fout("erfenis.fout.weg");
        }
        if (kavelGekocht(k)) {
            return new Fout("erfenis.fout.al_gekocht");
        }
        if (opgeruimdTotaal() < Plot.PLUKJES) {
            return new Fout("erfenis.fout.eerst_opruimen");
        }
        int max = Prijzen.maxKavels(hoeveLevel());
        if (kavelsGekocht() >= max) {
            int h = hoeveLevel();
            while (h < GebouwType.MAX_HOEVE && Prijzen.maxKavels(h) <= kavelsGekocht()) {
                h++;
            }
            return Prijzen.maxKavels(h) > kavelsGekocht() ? new Fout("erfenis.fout.slot", Integer.toString(h)) : new Fout("erfenis.fout.max");
        }
        return kanBetalen(Prijzen.kavel(kavelsGekocht() + 1));
    }

    @Nullable
    public Fout kanOphalen(int id) {
        Gebouw g = gebouw(id);
        if (g == null || !g.type.maakt()) {
            return new Fout("erfenis.fout.weg");
        }
        if (g.klaar() < 1) {
            return new Fout("erfenis.fout.niks");
        }
        return opslagVrij() <= 0 ? new Fout("erfenis.fout.vol") : null;
    }

    @Nullable
    public Fout kanVerven(int id, int palet) {
        if (palet < 1 || palet > maxPalet()) {
            return new Fout("erfenis.fout.palet", Integer.toString(palet * 2 - 1));
        }
        return id < 0 || gebouw(id) != null ? null : new Fout("erfenis.fout.weg");
    }

    // =====================================================================================================================
    // the actions (each checks again and returns what went wrong, or null)
    // =====================================================================================================================

    /** Builds a new one: the builder's job. {@code duurMs} &lt; 0: the type's own time. */
    @Nullable
    public Fout plaats(GebouwType type, int x, int z, int rot, long nu, long duurMs) {
        Fout f = kanPlaatsen(type, x, z, rot);
        if (f != null) {
            return f;
        }
        Prijs p = prijsNieuw(type);
        betaal(p);
        if (type == GebouwType.KIPPENHOK) {
            kippenhokGratis = false;
        }
        Gebouw g = nieuwGebouw(type, 0, x, z, rot & 3, nu);
        decoWijkt(x, z, g.b(), g.d());
        if (type == GebouwType.BOUWBLOKKENWERKPLAATS) {
            g.selectie.add(Bouwkeet.STANDAARD);
        }
        bouwen.add(new Bouw(Bouw.GEBOUW, g.id, 1, nu, nu + (duurMs < 0 ? p.tijdMs() : duurMs)));
        return null;
    }

    @Nullable
    public Fout verplaats(int id, int x, int z) {
        Fout f = kanVerplaatsen(id, x, z);
        if (f != null) {
            return f;
        }
        Gebouw g = gebouw(id);
        g.x = x;
        g.z = z;
        decoWijkt(x, z, g.b(), g.d());
        return null;
    }

    @Nullable
    public Fout draai(int id) {
        Fout f = kanDraaien(id);
        if (f != null) {
            return f;
        }
        Gebouw g = gebouw(id);
        g.rot = (g.rot + 1) & 3;
        decoWijkt(g.x, g.z, g.b(), g.d());
        return null;
    }

    @Nullable
    public Fout verbeter(int id, long nu, long duurMs) {
        Fout f = kanVerbeteren(id);
        if (f != null) {
            return f;
        }
        Gebouw g = gebouw(id);
        Prijs p = prijsVolgende(g);
        betaal(p);
        bouwen.add(new Bouw(Bouw.GEBOUW, id, g.level + 1, nu, nu + (duurMs < 0 ? p.tijdMs() : duurMs)));
        return null;
    }

    @Nullable
    public Fout opruimen(int n, long nu, long duurMs) {
        Fout f = kanOpruimen(n);
        if (f != null) {
            return f;
        }
        Prijs p = Prijzen.plukje(Plot.ring(n));
        betaal(p);
        bouwen.add(new Bouw(Bouw.PLUKJE, n, 1, nu, nu + (duurMs < 0 ? p.tijdMs() : duurMs)));
        return null;
    }

    @Nullable
    public Fout kavelKopen(int k) {
        Fout f = kanKavel(k);
        if (f != null) {
            return f;
        }
        betaal(Prijzen.kavel(kavelsGekocht() + 1));
        kavels |= 1 << k;
        return null;
    }

    /** Collects a building's basket: as much as fits, its own product first. Returns what came in (good -> n), or a Fout. */
    public Object ophalen(int id, Random rng) {
        Fout f = kanOphalen(id);
        if (f != null) {
            return f;
        }
        Gebouw g = gebouw(id);
        Map<Goed, Integer> uit = new LinkedHashMap<>();
        List<Goed> volgorde = new ArrayList<>(g.buffer.keySet());
        Recept r = Productie.recept(g);
        if (r != null && volgorde.remove(r.uit())) {
            volgorde.add(0, r.uit());
        }
        for (Goed goed : volgorde) {
            int heel = (int) Math.floor(g.inBuffer(goed) + 1e-9);
            int neem = Math.min(heel, opslagVrij());
            if (neem <= 0) {
                continue;
            }
            g.buffer.put(goed, g.inBuffer(goed) - neem);
            verander(goed, neem);
            uit.merge(goed, neem, Integer::sum);
            if (goed == Goed.BOUWBLOK) {
                List<String> keus = Bouwkeet.geldig(g, this);
                for (int i = 0; i < neem; i++) {
                    bouwblokSoorten.merge(keus.get(rng.nextInt(keus.size())), 1, Integer::sum);
                }
            }
        }
        g.buffer.values().removeIf(v -> v < 1e-9);
        return uit.isEmpty() ? new Fout("erfenis.fout.vol") : uit;
    }

    @Nullable
    public Fout verf(int id, int palet) {
        Fout f = kanVerven(id, palet);
        if (f != null) {
            return f;
        }
        for (Gebouw g : gebouwen) {
            if (id < 0 || g.id == id) {
                g.palet = palet;
                g.paletGekozen = true;
            }
        }
        return null;
    }

    /** Chooses a recipe (a crop for the Akker). */
    @Nullable
    public Fout kies(int id, int keuze) {
        Gebouw g = gebouw(id);
        if (g == null || keuze < 0 || keuze >= Productie.recepten(g.type).size()) {
            return new Fout("erfenis.fout.weg");
        }
        Recept r = Productie.recepten(g.type).get(keuze);
        if (!Productie.magRecept(this, r)) {
            return new Fout("erfenis.fout.recept_geheim");
        }
        if (r.minLevel() > Math.max(1, g.level)) {
            return new Fout("erfenis.fout.recept_level", Integer.toString(r.minLevel()));
        }
        g.keuze = keuze;
        return null;
    }

    @Nullable
    public Fout kleur(int id, int kleur) {
        Gebouw g = gebouw(id);
        if (g == null || g.type != GebouwType.KAARSENMAKERIJ) {
            return new Fout("erfenis.fout.weg");
        }
        g.kleur = Math.max(0, Math.min(16, kleur));
        return null;
    }

    /** The Bouwkeet blocks a Bouwblokkenwerkplaats makes (only blocks of its level's sets; at least one). */
    @Nullable
    public Fout selectie(int id, List<String> blokken) {
        Gebouw g = gebouw(id);
        if (g == null || g.type != GebouwType.BOUWBLOKKENWERKPLAATS) {
            return new Fout("erfenis.fout.weg");
        }
        List<String> mag = Bouwkeet.beschikbaar(g, this);
        List<String> nieuw = new ArrayList<>();
        for (String s : blokken) {
            if (mag.contains(s) && !nieuw.contains(s)) {
                nieuw.add(s);
            }
        }
        if (nieuw.isEmpty()) {
            return new Fout("erfenis.fout.selectie_leeg");
        }
        g.selectie.clear();
        g.selectie.addAll(nieuw);
        return null;
    }

    // =====================================================================================================================
    // the Leverkist ("Naar huis sturen")
    // =====================================================================================================================

    /** Brings the Leverkist allowance up to now (cap per hour, saves up to 8 hours). */
    public void werkTegoedBij(long nu) {
        double uren = Math.max(0, nu - leverBijgewerkt) / 3_600_000.0;
        for (Goed g : Goed.values()) {
            if (g.capPerUur > 0) {
                leverTegoed.put(g, Math.min(g.capPerUur * 8.0, leverTegoed.getOrDefault(g, 0.0) + g.capPerUur * uren));
            }
        }
        leverBijgewerkt = nu;
    }

    /** How many of this good may go home right now (storage and the cap). */
    public int magNaarHuis(Goed g, long nu) {
        if (!g.naarHuis()) {
            return 0;
        }
        int n = heeft(g);
        if (g.capPerUur > 0) {
            double uren = Math.max(0, nu - leverBijgewerkt) / 3_600_000.0;
            double tegoed = Math.min(g.capPerUur * 8.0, leverTegoed.getOrDefault(g, 0.0) + g.capPerUur * uren);
            n = Math.min(n, (int) Math.floor(tegoed + 1e-6));
        }
        return n;
    }

    @Nullable
    public Fout kanNaarHuis(Goed g, int n, long nu) {
        if (!g.naarHuis()) {
            return new Fout("erfenis.fout.niet_naar_huis", g.naamKey());
        }
        if (n <= 0) {
            return new Fout("erfenis.fout.niks");
        }
        if (heeft(g) < n) {
            return new Fout("erfenis.fout.goed", Integer.toString(n - heeft(g)), g.naamKey());
        }
        if (magNaarHuis(g, nu) < n) {
            return new Fout("erfenis.fout.leverkist_cap", Integer.toString(magNaarHuis(g, nu)), Integer.toString(g.capPerUur));
        }
        return null;
    }

    /** Takes the goods out of the storage for the Leverkist; returns item id -> count (or a Fout). */
    public Object naarHuis(Goed g, int n, long nu) {
        Fout f = kanNaarHuis(g, n, nu);
        if (f != null) {
            return f;
        }
        werkTegoedBij(nu);
        if (g.capPerUur > 0) {
            leverTegoed.put(g, leverTegoed.getOrDefault(g, 0.0) - n);
        }
        verander(g, -n);
        Map<String, Integer> uit = new LinkedHashMap<>();
        if (g == Goed.BOUWBLOK) {
            uit.putAll(neemSoorten(n));
        } else if (g == Goed.KAARS) {
            int kleur = 0;
            for (Gebouw k : gebouwen) {
                if (k.type == GebouwType.KAARSENMAKERIJ) {
                    kleur = k.kleur;
                    break;
                }
            }
            uit.put(Bouwkeet.kaars(kleur), n);
        } else {
            uit.put(g.thuisItem, n);
        }
        return uit;
    }

    // =====================================================================================================================
    // the clock: catching up (production, builds, residents)
    // =====================================================================================================================

    /** Everything that happened between the last catch-up and {@code nu}, in order of time. */
    public void bijwerken(long nu) {
        if (npc) {
            bijgewerkt = nu;
            return;
        }
        long t = bijgewerkt <= 0 || bijgewerkt > nu ? nu : bijgewerkt;
        rondAf(t);
        intrek(t);
        int veilig = 0;
        while (t < nu && veilig++ < 10_000) {
            long volgende = nu;
            for (Bouw b : bouwen) {
                if (b.eind() > t && b.eind() < volgende) {
                    volgende = b.eind();
                }
            }
            if (volgendeIntrek > t && volgendeIntrek < volgende) {
                volgende = volgendeIntrek;
            }
            Productie.produceer(this, t, volgende);
            t = volgende;
            rondAf(t);
            intrek(t);
        }
        rondAf(nu);
        intrek(nu);
        bijgewerkt = nu;
    }

    /** Finishes every job whose time has come (earliest first). */
    private void rondAf(long t) {
        List<Bouw> af = new ArrayList<>();
        for (Bouw b : bouwen) {
            if (b.eind() <= t) {
                af.add(b);
            }
        }
        if (af.isEmpty()) {
            return;
        }
        af.sort(Comparator.comparingLong(Bouw::eind));
        int oudeMax = maxBewoners();
        for (Bouw b : af) {
            bouwen.remove(b);
            klaar.add(b);
            if (b.soort() == Bouw.PLUKJE) {
                opgeruimd(b.ref());
                continue;
            }
            Gebouw g = gebouw(b.ref());
            if (g == null) {
                continue;
            }
            g.level = Math.max(g.level, b.naar());
            if (!g.paletGekozen) {
                g.palet = GebouwVormen.vormTier(g.type.id, g.level);
            }
            if (g.level == 1) {
                momenten.add("gebouw_gebouwd:" + g.type.id);
            }
            momenten.add("gebouw_level:" + g.type.id + ":" + g.level);
            if (g.type == GebouwType.HOEVE) {
                momenten.add("hoeve_level:" + g.level);
            }
            meldingen.add(new Fout("erfenis.ok.klaar", g.type.naamKey(), Integer.toString(g.level)));
        }
        if (maxBewoners() > oudeMax) {
            regelIntrek(t);
        }
    }

    /** Makes sure a move-in is planned when there is room (and none when there isn't). */
    private void regelIntrek(long t) {
        if (bewoners >= maxBewoners()) {
            volgendeIntrek = 0;
        } else if (volgendeIntrek <= 0) {
            volgendeIntrek = t + Productie.intrekMs(besteLevel(GebouwType.SPEELTUIN) > 0);
        }
    }

    private void intrek(long t) {
        while (volgendeIntrek > 0 && volgendeIntrek <= t && bewoners < maxBewoners()) {
            bewoners++;
            momenten.add("bewoners:" + bewoners);
            meldingen.add(new Fout("erfenis.ok.bewoner", Integer.toString(bewoners)));
            long volgende = volgendeIntrek + Productie.intrekMs(besteLevel(GebouwType.SPEELTUIN) > 0);
            volgendeIntrek = bewoners < maxBewoners() ? volgende : 0;
        }
        regelIntrek(t);
    }

    // =====================================================================================================================
    // clearing: what you find (balans 6, deco.md "Vondst", sherlock.md proloog)
    // =====================================================================================================================

    /** The kind of overgrowth on plukje n (0 struik, 1 omgevallen boom, 2 rotsblok, 3 brandnetels, 4 oude kar, 5 molshoopjes). */
    public int plukjeSoort(int n) {
        long h = id.getLeastSignificantBits() * 6364136223846793005L + n * 1442695040888963407L;
        return (int) Math.floorMod(h >>> 33, 6);
    }

    /** The fixed story finds on fixed plukjes. */
    public static String vasteVondst(int n) {
        if (n == Plot.plukjeIndex(4, 2)) {
            return "ansichtkaart_1";
        }
        if (n == Plot.plukjeIndex(1, 4)) {
            return "oud_kistje";
        }
        if (n == Plot.plukjeIndex(7, 4)) {
            return "ansichtkaart_2";
        }
        if (n == Plot.plukjeIndex(4, 8)) {
            return "trekguh";
        }
        if (n == Plot.plukjeIndex(0, 0)) {
            return "ansichtkaart_3";
        }
        return null;
    }

    private static final String[][] DECO_VONDST = {{"weerhaan"}, {"strooguh", "melkkar"}, {"knabbeltrommel"}};
    private static final String[] ZELDZAAM = {"zaadje_gouden_graan", "zaadje_regenboogwortel", "dier_gouden_kippetje", "dier_regenboogschaapje",
            "dier_vlekjeskoe"};

    /** Plukje n is cleared: the land is free, and you find things (always some wood or stone, sometimes more). */
    private void opgeruimd(int n) {
        if (opgeruimd[n]) {
            return;
        }
        opgeruimd[n] = true;
        int ring = Plot.ring(n);
        Random r = new Random(id.getMostSignificantBits() ^ (n + 1) * 0x9E3779B97F4A7C15L);
        Goed[] soort = ring == 1 ? new Goed[] {Goed.STAM, Goed.STEEN} : ring == 2 ? new Goed[] {Goed.STAM, Goed.STEEN, Goed.KLEI}
                : new Goed[] {Goed.STAM, Goed.STEEN, Goed.ZAND};
        int basis = ring == 1 ? 10 : ring == 2 ? 20 : 40;
        Goed g = soort[r.nextInt(soort.length)];
        int erin = geefGoed(g, basis + r.nextInt(basis + 1));
        meldingen.add(new Fout("erfenis.ok.opgeruimd", Integer.toString(erin), g.naamKey()));
        if (r.nextInt(100) < 30) {
            int lo = ring == 1 ? 50 : ring == 2 ? 200 : 600, hi = ring == 1 ? 150 : ring == 2 ? 500 : 1500;
            int zak = lo + r.nextInt(hi - lo + 1);
            int echt = geefGuhlden(zak);
            meldingen.add(new Fout("erfenis.ok.zakje", Integer.toString(echt)));
            vondst("guhldenzakje");
        }
        boolean bijzonder = false;
        String vast = vasteVondst(n);
        if (vast != null) {
            vondst(vast);
            if (vast.equals("trekguh")) {
                decoOpslag.merge("trekguh", 1, Integer::sum);
            } else if (vast.startsWith("ansichtkaart")) {
                kaartjesbordErbij();
            }
            bijzonder = true;
        }
        if (r.nextInt(100) < (ring == 1 ? 10 : ring == 2 ? 15 : 20)) {
            String[] keus = DECO_VONDST[ring - 1];
            String d = keus[r.nextInt(keus.length)];
            decoOpslag.merge(ErfenisDeco.vorm(d), 1, Integer::sum);
            vondst("deco_" + d);
            bijzonder = true;
        }
        if (ring >= 2 && r.nextInt(100) < 5) {
            vondst(ZELDZAAM[r.nextInt(ZELDZAAM.length)]);
            bijzonder = true;
        }
        // the prologue (sherlock.md): the first special find also brings the rusty little key; by the 3rd plukje for sure
        if (!vondsten.contains("roestig_sleuteltje") && (bijzonder || opgeruimdTotaal() >= 3)) {
            vondst("roestig_sleuteltje");
        }
        momenten.add("plukje_opgeruimd:" + opgeruimdTotaal());
    }

    private void vondst(String id) {
        if (!id.equals("guhldenzakje")) {
            vondsten.add(id);
            meldingen.add(new Fout("erfenis.ok.vondst", "erfenis.vondst." + id));
        }
        momenten.add("vondst:" + id);
    }

    /** Dev: every timestamp {@code ms} earlier, so the next catch-up lets that much time pass. */
    public void verschuif(long ms) {
        bijgewerkt -= ms;
        leverBijgewerkt -= ms;
        if (volgendeIntrek > 0) {
            volgendeIntrek -= ms;
        }
        bouwen.replaceAll(b -> new Bouw(b.soort(), b.ref(), b.naar(), b.start() - ms, b.eind() - ms));
    }

    // =====================================================================================================================
    // saving (also the network form)
    // =====================================================================================================================

    public CompoundTag save() {
        return save(false);
    }

    /** {@code voorClient}: without the stored guh data of the logé guhs (nobody needs it on the client). */
    public CompoundTag save(boolean voorClient) {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, id);
        if (eigenaar != null) {
            t.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }
        t.putString("Naam", naam);
        t.putBoolean("Npc", npc);
        t.putInt("Cx", cx);
        t.putInt("Cz", cz);
        t.putInt("VolgendeId", volgendeId);
        t.putInt("Guhlden", guhlden);
        t.putBoolean("MoetStempelen", moetStempelen);
        t.putInt("Bewoners", bewoners);
        t.putLong("VolgendeIntrek", volgendeIntrek);
        t.putInt("Bestellingen", bestellingen);
        t.putInt("Kavels", kavels);
        t.putInt("KnusDeco", knusDeco);
        t.putInt("Vk", vk);
        t.putBoolean("KippenhokGratis", kippenhokGratis);
        t.putBoolean("Aangekomen", aangekomen);
        t.putInt("Tutorial", tutorial);
        t.putInt("KnusGemeld", knusGemeld);
        t.putLong("Bijgewerkt", bijgewerkt);
        t.putLong("LeverBijgewerkt", leverBijgewerkt);
        byte[] op = new byte[Plot.PLUKJES];
        for (int i = 0; i < op.length; i++) {
            op[i] = (byte) (opgeruimd[i] ? 1 : 0);
        }
        t.putByteArray("Opgeruimd", op);
        ListTag lijst = new ListTag();
        for (Gebouw g : gebouwen) {
            lijst.add(g.save());
        }
        t.put("Gebouwen", lijst);
        ListTag bl = new ListTag();
        for (Bouw b : bouwen) {
            CompoundTag c = new CompoundTag();
            c.putInt("Soort", b.soort());
            c.putInt("Ref", b.ref());
            c.putInt("Naar", b.naar());
            c.putLong("Start", b.start());
            c.putLong("Eind", b.eind());
            bl.add(c);
        }
        t.put("Bouwen", bl);
        CompoundTag gt = new CompoundTag();
        goederen.forEach((g, v) -> gt.putDouble(g.id, v));
        t.put("Goederen", gt);
        t.put("BouwblokSoorten", telling(bouwblokSoorten));
        t.put("DecoOpslag", telling(decoOpslag));
        t.put("Loket", telling(loket));
        CompoundTag lt = new CompoundTag();
        leverTegoed.forEach((g, v) -> lt.putDouble(g.id, v));
        t.put("LeverTegoed", lt);
        t.put("Vondsten", tekstLijst(vondsten));
        t.put("Beloningen", tekstLijst(beloningen));
        t.put("PadSoorten", tekstLijst(padSoorten));
        boolean paadjes = false;
        for (byte p : paden) {
            paadjes |= p != 0;
        }
        if (paadjes) {
            t.putByteArray("Paden", paden);
        }
        ListTag dl = new ListTag();
        for (Deco d : deco) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", d.id());
            c.putInt("X", d.x());
            c.putInt("Z", d.z());
            c.putInt("Rot", d.rot());
            dl.add(c);
        }
        t.put("Deco", dl);
        ListTag ll = new ListTag();
        for (Loge l : loges) {
            CompoundTag c = new CompoundTag();
            c.store("Id", UUIDUtil.CODEC, l.id);
            c.putInt("Gebouw", l.gebouw);
            CompoundTag o = l.opslag.copy();
            if (voorClient) {
                o.remove("Data");
            }
            c.put("Opslag", o);
            ll.add(c);
        }
        t.put("Loges", ll);
        return t;
    }

    private static CompoundTag telling(Map<String, Integer> m) {
        CompoundTag c = new CompoundTag();
        m.forEach(c::putInt);
        return c;
    }

    private static ListTag tekstLijst(List<String> l) {
        ListTag t = new ListTag();
        for (String s : l) {
            t.add(StringTag.valueOf(s));
        }
        return t;
    }

    private static void leesTelling(CompoundTag c, Map<String, Integer> m) {
        for (String k : c.keySet()) {
            int n = c.getIntOr(k, 0);
            if (n > 0) {
                m.put(k, n);
            }
        }
    }

    @Nullable
    public static Base load(CompoundTag t) {
        UUID id = t.read("Id", UUIDUtil.CODEC).orElse(null);
        if (id == null) {
            return null;
        }
        Base b = new Base(id, t.read("Eigenaar", UUIDUtil.CODEC).orElse(null), t.getStringOr("Naam", "?"), t.getBooleanOr("Npc", false),
                t.getIntOr("Cx", 0), t.getIntOr("Cz", 0));
        b.zaakKlaar = t.getBooleanOr("ZaakKlaar", false);
        b.volgendeId = t.getIntOr("VolgendeId", 1);
        b.guhlden = t.getIntOr("Guhlden", 0);
        b.moetStempelen = t.getBooleanOr("MoetStempelen", true);
        b.bewoners = t.getIntOr("Bewoners", 0);
        b.volgendeIntrek = t.getLongOr("VolgendeIntrek", 0L);
        b.bestellingen = t.getIntOr("Bestellingen", 0);
        b.kavels = t.getIntOr("Kavels", 0);
        b.knusDeco = t.getIntOr("KnusDeco", 0);
        b.vk = t.getIntOr("Vk", 0);
        b.kippenhokGratis = t.getBooleanOr("KippenhokGratis", true);
        b.aangekomen = t.getBooleanOr("Aangekomen", false);
        b.tutorial = t.getIntOr("Tutorial", 0);
        b.knusGemeld = t.getIntOr("KnusGemeld", 0);
        b.bijgewerkt = t.getLongOr("Bijgewerkt", 0L);
        b.leverBijgewerkt = t.getLongOr("LeverBijgewerkt", 0L);
        byte[] op = t.getByteArray("Opgeruimd").orElse(new byte[0]);
        for (int i = 0; i < Math.min(op.length, Plot.PLUKJES); i++) {
            b.opgeruimd[i] = op[i] != 0;
        }
        for (Tag e : t.getListOrEmpty("Gebouwen")) {
            if (e instanceof CompoundTag c) {
                Gebouw g = Gebouw.load(c);
                if (g != null) {
                    b.gebouwen.add(g);
                    b.volgendeId = Math.max(b.volgendeId, g.id + 1);
                }
            }
        }
        for (Tag e : t.getListOrEmpty("Bouwen")) {
            if (e instanceof CompoundTag c) {
                b.bouwen.add(new Bouw(c.getIntOr("Soort", 0), c.getIntOr("Ref", -1), c.getIntOr("Naar", 1), c.getLongOr("Start", 0L),
                        c.getLongOr("Eind", 0L)));
            }
        }
        CompoundTag gt = t.getCompoundOrEmpty("Goederen");
        for (String k : gt.keySet()) {
            Goed g = Goed.van(k);
            if (g != null) {
                b.goederen.put(g, gt.getDoubleOr(k, 0));
            }
        }
        leesTelling(t.getCompoundOrEmpty("BouwblokSoorten"), b.bouwblokSoorten);
        leesTelling(t.getCompoundOrEmpty("DecoOpslag"), b.decoOpslag);
        leesTelling(t.getCompoundOrEmpty("Loket"), b.loket);
        CompoundTag lt = t.getCompoundOrEmpty("LeverTegoed");
        for (String k : lt.keySet()) {
            Goed g = Goed.van(k);
            if (g != null) {
                b.leverTegoed.put(g, lt.getDoubleOr(k, 0));
            }
        }
        for (Tag e : t.getListOrEmpty("Vondsten")) {
            e.asString().ifPresent(b.vondsten::add);
        }
        for (Tag e : t.getListOrEmpty("Beloningen")) {
            e.asString().ifPresent(b.beloningen::add);
        }
        for (Tag e : t.getListOrEmpty("PadSoorten")) {
            e.asString().ifPresent(b.padSoorten::add);
        }
        byte[] p = t.getByteArray("Paden").orElse(null);
        if (p != null && p.length == Plot.MAAT * Plot.MAAT) {
            b.paden = p;
        }
        for (Tag e : t.getListOrEmpty("Deco")) {
            if (e instanceof CompoundTag c) {
                b.deco.add(new Deco(c.getStringOr("Id", ""), c.getIntOr("X", 0), c.getIntOr("Z", 0), c.getIntOr("Rot", 0)));
            }
        }
        for (Tag e : t.getListOrEmpty("Loges")) {
            if (e instanceof CompoundTag c) {
                UUID lid = c.read("Id", UUIDUtil.CODEC).orElse(null);
                if (lid != null) {
                    b.loges.add(new Loge(lid, c.getCompoundOrEmpty("Opslag"), c.getIntOr("Gebouw", -1)));
                }
            }
        }
        return b;
    }

    /** The buildings, read only (the world map). */
    public List<Gebouw> gebouwenLijst() {
        return Collections.unmodifiableList(gebouwen);
    }
}
