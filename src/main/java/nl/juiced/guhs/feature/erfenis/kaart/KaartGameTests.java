package nl.juiced.guhs.feature.erfenis.kaart;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * De Erfenis, wereldkaart + bestellingen: NPC village plans keep the base rules; the 1.9 village rule (3 around the first
 * player, 1 per next player, following their level, minimum distance, POC migration); order generation and rewards
 * (balans 4 + 5); the cart cycle (never expires, star once a day); Handje helpen limits and stamps; saving.
 */
public class KaartGameTests {
    private static final String BATCH = "erfenis_kaart";
    private static final UUID P1 = new UUID(1, 1), P2 = new UUID(2, 2), P3 = new UUID(3, 3);
    /** Noon of a fixed day in the server's zone (so "+1 day" is safely another day). */
    private static final long MIDDAG = LocalDate.of(2026, 11, 3).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    private static final long DAG = 24 * 3_600_000L;

    private static KaartData data(long seed) {
        KaartData d = new KaartData();
        d.seed = seed;
        return d;
    }

    private static Set<Long> cellen(KaartData d, List<KaartLeven.Speler> spelers) {
        Set<Long> s = new HashSet<>(d.npcCellen());
        for (KaartLeven.Speler p : spelers) {
            s.add(KaartData.sleutel(p.cx(), p.cz()));
        }
        return s;
    }

    /** Every NPC village keeps MIN_AFSTAND to every other base. */
    private static void ruimte(GameTestHelper helper, KaartData d, List<KaartLeven.Speler> spelers) {
        List<int[]> alle = new ArrayList<>();
        for (KaartLeven.Speler p : spelers) {
            alle.add(new int[] {p.cx(), p.cz(), 0});
        }
        for (NpcDorp n : d.npcs()) {
            alle.add(new int[] {n.cx, n.cz, 1});
        }
        for (int i = 0; i < alle.size(); i++) {
            for (int j = 0; j < i; j++) {
                int[] a = alle.get(i), b = alle.get(j);
                if (a[2] == 1 || b[2] == 1) {
                    helper.assertTrue(KaartLeven.afstand(a[0], a[1], b[0], b[1]) >= KaartLeven.MIN_AFSTAND,
                            "NPC village too close: " + a[0] + "," + a[1] + " / " + b[0] + "," + b[1]);
                }
            }
        }
    }

    /** Every generated plan (all Hoeve levels, many seeds), after growth and level changes: valid, within the limits. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void npcPlannenBinnenDeRegels(GameTestHelper helper) {
        for (int seed = 0; seed < 40; seed++) {
            for (int h = 1; h <= KaartGebouwType.MAX_HOEVE; h++) {
                Random r = new Random(seed * 31L + h);
                List<KaartGebouw> plan = new ArrayList<>(NpcBouwer.maak(h, r));
                helper.assertTrue(NpcBouwer.fouten(plan).isEmpty(), "plan seed " + seed + " H" + h + ": " + NpcBouwer.fouten(plan));
                helper.assertTrue(NpcBouwer.hoeveLevel(plan) == h, "the Hoeve has the asked level");
                helper.assertTrue(NpcBouwer.aantal(plan, KaartGebouwType.MARKTPLEIN) == 1, "every village has its Marktplein");
                for (int i = 0; i < 30; i++) {
                    NpcBouwer.groei(plan, r);
                }
                helper.assertTrue(NpcBouwer.fouten(plan).isEmpty(), "after growth (seed " + seed + ", H" + h + "): " + NpcBouwer.fouten(plan));
                NpcBouwer.zetHoeve(plan, h + 1, r);
                helper.assertTrue(NpcBouwer.fouten(plan).isEmpty() && NpcBouwer.hoeveLevel(plan) == Math.min(10, h + 1), "level up keeps the rules");
                NpcBouwer.zetHoeve(plan, 1, r);
                helper.assertTrue(NpcBouwer.fouten(plan).isEmpty() && NpcBouwer.hoeveLevel(plan) == 1, "back to H1 (reset) keeps the rules");
            }
        }
        helper.succeed();
    }

    /** 3 villages around the first player (-1, 0, +1), 1 per next player, minimum distance, stable, following the level. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dorpjesVolgensDeRegel(GameTestHelper helper) {
        for (long seed = 1; seed <= 25; seed++) {
            KaartData d = data(seed * 7919L);
            List<KaartLeven.Speler> spelers = new ArrayList<>(List.of(new KaartLeven.Speler(P1, "Joost", 0, 0, 4)));
            KaartLeven.zorgBuren(d, spelers, cellen(d, spelers), 0L);
            helper.assertTrue(P1.equals(d.eerste), "the first player is remembered");
            helper.assertTrue(d.npcs().size() == 3 && d.volgers(P1).size() == 3, "three villages for the first player");
            Set<Integer> afw = new HashSet<>();
            for (NpcDorp n : d.volgers(P1)) {
                afw.add(n.afwijking);
                helper.assertTrue(n.hoeveLevel() == 4 + n.afwijking, "level = player's level + offset (" + n.hoeveLevel() + ")");
                int a = KaartLeven.afstand(n.cx, n.cz, 0, 0);
                helper.assertTrue(a >= KaartLeven.MIN_AFSTAND && a <= KaartLeven.MIN_AFSTAND + 2, "fairly close to the player (" + a + ")");
                helper.assertTrue(NpcBouwer.fouten(n.plan).isEmpty(), "a valid plan");
            }
            helper.assertTrue(afw.equals(Set.of(-1, 0, 1)), "one lower, one equal, one higher");
            ruimte(helper, d, spelers);
            // nothing changes when asked again
            helper.assertTrue(KaartLeven.zorgBuren(d, spelers, cellen(d, spelers), 1L).veranderd().isEmpty(), "stable");
            // two more players: one village each
            int[] c2 = KaartLeven.spelerPlek(cellen(d, spelers));   // (where kern should put the next players)
            spelers.add(new KaartLeven.Speler(P2, "Mika", c2[0], c2[1], 1));
            KaartLeven.zorgBuren(d, spelers, cellen(d, spelers), 2L);
            int[] c3 = KaartLeven.spelerPlek(cellen(d, spelers));
            spelers.add(new KaartLeven.Speler(P3, "Pim", c3[0], c3[1], 9));
            KaartLeven.zorgBuren(d, spelers, cellen(d, spelers), 2L);
            helper.assertTrue(d.npcs().size() == 5 && d.volgers(P2).size() == 1 && d.volgers(P3).size() == 1, "1 per next player");
            NpcDorp p3 = d.volgers(P3).get(0);
            helper.assertTrue(p3.hoeveLevel() == KaartLeven.doel(9, p3.afwijking) && Math.abs(p3.afwijking) <= 1, "~1 lower, equal or ~1 higher");
            ruimte(helper, d, spelers);
            // following: the first player goes to H10; one level per tick, clamped at 10
            spelers.set(0, new KaartLeven.Speler(P1, "Joost", 0, 0, 10));
            KaartLeven.volg(d, spelers, 3L);
            for (NpcDorp n : d.volgers(P1)) {
                helper.assertTrue(n.hoeveLevel() == 5 + n.afwijking, "one step per tick");
            }
            for (int i = 0; i < 10; i++) {
                KaartLeven.volg(d, spelers, 4L + i);
            }
            for (NpcDorp n : d.volgers(P1)) {
                helper.assertTrue(n.hoeveLevel() == Math.min(10, 10 + n.afwijking), "caught up (" + n.hoeveLevel() + ")");
                helper.assertTrue(NpcBouwer.fouten(n.plan).isEmpty(), "still a valid plan");
            }
        }
        helper.succeed();
    }

    /** A POC map (40 random villages, version 0): good spots are adopted, the rest dropped, players untouched. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void pocKaartMigreert(GameTestHelper helper) {
        KaartData d = data(4242L);
        d.versie = 0;
        Random r = new Random(5);
        Set<Long> gebruikt = new HashSet<>();
        gebruikt.add(KaartData.sleutel(0, 0));
        while (d.npcs().size() < 40) {
            int cx = r.nextInt(17) - 8, cz = r.nextInt(17) - 8;
            if (gebruikt.add(KaartData.sleutel(cx, cz))) {
                NpcDorp n = new NpcDorp(new UUID(r.nextLong(), r.nextLong()), "Oud" + d.npcs().size(), "Opa", 0xFFF29BB8, cx, cz);
                d.voegToe(n);
            }
        }
        List<KaartLeven.Speler> spelers = List.of(new KaartLeven.Speler(P1, "Joost", 0, 0, 2));
        Set<Long> bezet = new HashSet<>(gebruikt);   // (the POC villages have core bases in their cells)
        KaartLeven.Uitkomst u = KaartLeven.zorgBuren(d, spelers, bezet, 0L);
        helper.assertTrue(d.versie == KaartData.VERSIE, "migrated");
        helper.assertTrue(d.npcs().size() == 3, "only the three villages of the first player stay (" + d.npcs().size() + ")");
        helper.assertTrue(u.weg().size() + u.veranderd().size() >= 40 && u.weg().size() >= 37, "the rest is dropped");
        int geadopteerd = 0;
        for (NpcDorp n : d.npcs()) {
            helper.assertTrue(P1.equals(n.volgt), "they follow the player");
            geadopteerd += n.naam.startsWith("Oud") ? 1 : 0;
        }
        helper.assertTrue(geadopteerd >= 1, "a POC village at a good spot was kept");
        for (NpcDorp w : u.weg()) {
            helper.assertFalse(w.id.equals(P1), "a player's base is never dropped");
        }
        ruimte(helper, d, spelers);
        helper.succeed();
    }

    /** Orders: goods per tier, never main-world or kaasknabbels, the reward = value x factor (balans 4 + 5), friendly numbers. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bestellingenEnBeloningen(GameTestHelper helper) {
        Map<Integer, double[]> gemiddeld = new HashMap<>();
        for (int tier = 1; tier <= 5; tier++) {
            for (int rel = -1; rel <= 2; rel++) {   // (2 = the star order)
                boolean ster = rel == 2;
                double som = 0;
                for (int seed = 0; seed < 200; seed++) {
                    Bestelling b = new Bestelling();
                    b.maak(new Random(seed * 131L + tier * 7 + rel), new UUID(9, 9), tier, ster ? 0 : rel, ster, g -> true);
                    helper.assertFalse(b.regels.isEmpty(), "an order asks something");
                    Set<String> soorten = new HashSet<>();
                    double waarde = 0;
                    for (Bestelling.Regel r : b.regels) {
                        BestelGoed g = BestelGoed.van(r.goed());
                        helper.assertTrue(g != null, "a known village good: " + r.goed());
                        helper.assertTrue(soorten.add(r.goed()), "no kind twice");
                        helper.assertTrue(g.tier <= tier || (rel == 1 && g.tier == tier + 1), "goods of the player's tier (" + r.goed() + " T" + tier + ")");
                        helper.assertTrue(r.n() >= 1 && (r.n() <= 20 || r.n() % 5 == 0), "friendly numbers: " + r.n());
                        waarde += g.waarde * r.n();
                    }
                    helper.assertTrue(b.beloning == Math.max(5, (int) Math.round(waarde * b.factor() / 5.0) * 5), "reward = value x factor");
                    helper.assertTrue(b.beloning % 5 == 0, "round rewards");
                    helper.assertTrue(b.aantalGoederen() <= Bestelling.MAX_N[tier] * 3 / 2 + 4 * b.regels.size(), "not too many goods");
                    helper.assertTrue(b.decoKans() == (ster ? 100 : rel < 0 ? 3 : rel == 0 ? 8 : 15), "deco chance per relation");
                    som += b.beloning;
                }
                gemiddeld.put(tier * 10 + rel, new double[] {som / 200});
            }
            double gelijk = gemiddeld.get(tier * 10)[0];
            helper.assertTrue(gelijk >= Bestelling.MIN_F[tier] * 0.6 && gelijk <= Bestelling.MAX_F[tier] * 1.2,
                    "T" + tier + " equal orders pay about balans 4 (" + gelijk + ")");
            helper.assertTrue(gemiddeld.get(tier * 10 - 1)[0] < gelijk && gelijk < gemiddeld.get(tier * 10 + 1)[0]
                    && gemiddeld.get(tier * 10 + 1)[0] < gemiddeld.get(tier * 10 + 2)[0], "lower < equal < higher < star (T" + tier + ")");
        }
        helper.assertTrue(BestelGoed.van("kaasknabbel") == null && BestelGoed.van("glasscherf") == null, "never kaasknabbels or glasscherven");
        helper.assertTrue(Bestellingen.karren(0) == 2 && Bestellingen.karren(2) == 3 && Bestellingen.karren(10) == 7, "carts by Marktplein level");
        // star gift and deco rolls
        helper.assertTrue(Bestellingen.sterDeco(1, new Random(1)).equals("mika_schrik"), "a guaranteed piece even at H1");
        Bestelling laag = new Bestelling();
        laag.relatie = 1;
        int gevallen = 0;
        for (int i = 0; i < 2000; i++) {
            gevallen += Bestellingen.rolDeco(laag, 2, new Random(i)) != null ? 1 : 0;
        }
        helper.assertTrue(gevallen == 0, "no order deco below H4");
        for (int i = 0; i < 2000; i++) {
            gevallen += Bestellingen.rolDeco(laag, 7, new Random(i)) != null ? 1 : 0;
        }
        helper.assertTrue(gevallen > 200 && gevallen < 420, "about 15% from a higher village (" + gevallen + ")");
        helper.succeed();
    }

    private static Bestellingen.Omgeving omgeving(int hoeve, int markt) {
        List<Bestellingen.Buur> buren = List.of(new Bestellingen.Buur(new UUID(7, 1), "Knabbeldorp", 0xFFF29BB8, hoeve - 1),
                new Bestellingen.Buur(new UUID(7, 2), "Kaasdijk", 0xFF9BD8F2, hoeve), new Bestellingen.Buur(new UUID(7, 3), "Vadsveen", 0xFFB8F29B, hoeve + 1));
        return new Bestellingen.Omgeving() {
            @Override
            public List<Bestellingen.Buur> buren() {
                return buren;
            }

            @Override
            public int hoeve() {
                return hoeve;
            }

            @Override
            public int marktplein() {
                return markt;
            }

            @Override
            public boolean kanMaken(BestelGoed g) {
                return false;
            }
        };
    }

    /** The cart cycle: first orders at once, then drive/wait/drive; an order never expires; one star a day; 1-3 per neighbour. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void karretjesRijden(GameTestHelper helper) {
        BestelData.Speler s = new BestelData.Speler();
        Bestellingen.Omgeving o = omgeving(3, 0);
        long nu = MIDDAG;
        Bestellingen.bijwerken(s, o, 1L, P1, nu, false);
        helper.assertTrue(s.karren.size() == 2, "no Marktplein yet: 2 carts");
        for (Bestelling b : s.karren) {
            helper.assertTrue(b.fase == Bestelling.OPEN && !b.regels.isEmpty(), "the very first orders are there at once");
        }
        helper.assertTrue(s.ster != null && s.ster.fase == Bestelling.OPEN && !s.ster.deco.isEmpty(), "today's star order, with its gift");
        // never expires
        Bestelling eerste = s.karren.get(0);
        String voor = eerste.save().toString();
        Bestellingen.bijwerken(s, o, 1L, P1, nu + 30 * DAG, false);
        helper.assertTrue(eerste.fase == Bestelling.OPEN && eerste.save().toString().equals(voor), "an open order never expires");
        // refuse: home empty, rest, come back with something else
        nu += 30 * DAG;
        helper.assertTrue(Bestellingen.weiger(s, 0, o, nu, false) && eerste.fase == Bestelling.TERUG && eerste.geweigerd, "refused: drives home");
        Bestellingen.bijwerken(s, o, 1L, P1, nu + 5 * 60_000L, false);
        helper.assertTrue(eerste.fase == Bestelling.WACHT, "home again, resting (Marktplein wait)");
        Bestellingen.bijwerken(s, o, 1L, P1, nu + 15 * 60_000L, false);
        helper.assertTrue(eerste.fase == Bestelling.HEEN && !eerste.geweigerd, "a new order on its way");
        Bestellingen.bijwerken(s, o, 1L, P1, nu + 20 * 60_000L, false);
        helper.assertTrue(eerste.fase == Bestelling.OPEN, "arrived");
        // the star: delivered -> home -> none until tomorrow
        long dag2 = nu;
        Bestellingen.bijwerken(s, o, 1L, P1, dag2, false);
        s.ster.fase = Bestelling.TERUG;   // (what Bestellingen.lever does)
        s.ster.start = dag2;
        s.ster.eind = dag2 + 60_000L;
        Bestellingen.bijwerken(s, o, 1L, P1, dag2 + 2 * 60_000L, false);
        helper.assertTrue(s.ster == null, "delivered star: none for the rest of today");
        Bestellingen.bijwerken(s, o, 1L, P1, dag2 + 3 * 3_600_000L, false);
        helper.assertTrue(s.ster == null, "still none today");
        Bestellingen.bijwerken(s, o, 1L, P1, dag2 + DAG, false);
        helper.assertTrue(s.ster != null && s.ster.ster, "a new star order the next day");
        // a big Marktplein: 7 carts, at most 3 per neighbour, the relation follows the village's level
        Bestellingen.Omgeving groot = omgeving(3, 10);
        long t = dag2 + DAG;
        for (int i = 0; i < 40; i++) {
            Bestellingen.bijwerken(s, groot, 1L, P1, t + i * 60 * 60_000L, false);
        }
        helper.assertTrue(s.karren.size() == 7, "Marktplein L10: 7 carts");
        Map<UUID, Integer> per = new HashMap<>();
        for (Bestelling b : s.karren) {
            if (b.fase != Bestelling.WACHT) {
                per.merge(b.dorp, 1, Integer::sum);
                int verwacht = b.dorp.equals(new UUID(7, 1)) ? -1 : b.dorp.equals(new UUID(7, 2)) ? 0 : 1;
                helper.assertTrue(b.relatie == verwacht, "relation from the village's level");
            }
        }
        for (int n : per.values()) {
            helper.assertTrue(n >= 1 && n <= Bestellingen.MAX_PER_BUUR, "1-3 orders per neighbour (" + per + ")");
        }
        helper.succeed();
    }

    /** Handje helpen: once per village per day, rewards for the first 5 villages a day, stamps and the burenstukken, +20%. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void handjeHelpen(GameTestHelper helper) {
        BestelData.Speler h = new BestelData.Speler();
        long nu = MIDDAG;
        for (int i = 0; i < 7; i++) {
            UUID dorp = new UUID(50, i);
            helper.assertTrue(Handje.mag(h, dorp, nu), "may help a new village");
            Handje.Beloning b = Handje.boek(h, dorp, 5, nu);
            helper.assertTrue(i < 5 ? b.guhlden() == 60 && b.stempel() : b.guhlden() == 0 && !b.stempel(),
                    "20 x own tier and a stamp for the first 5 villages (" + i + ")");
            helper.assertFalse(Handje.mag(h, dorp, nu + 3_600_000L), "once per day per village");
        }
        helper.assertTrue(h.stempels == 5, "5 stamps a day");
        helper.assertTrue(Handje.mag(h, new UUID(50, 0), nu + DAG), "again the next day");
        helper.assertTrue(Handje.boek(h, new UUID(50, 0), 1, nu + DAG).guhlden() == 20, "rewards again the next day (T1: 20)");
        helper.assertTrue("burenhulp_bloempot".equals(Handje.burenstuk(10)) && Handje.burenstuk(9) == null && Handje.burenstuk(11) == null
                && "burenhulp_bankje".equals(Handje.burenstuk(35)) && "burenhulp_vlaggetjes".equals(Handje.burenstuk(60))
                && "burenhulp_vlaggetjes".equals(Handje.burenstuk(85)) && Handje.burenstuk(36) == null, "10 stamps, then every 25");
        // +20% on an order
        BestelData.Speler s = new BestelData.Speler();
        Bestellingen.bijwerken(s, omgeving(4, 2), 3L, P2, nu, false);
        Bestelling b = s.karren.get(0);
        int voor = b.uitbetaling();
        helper.assertTrue(Bestellingen.geefHulp(s, "Joost"), "an open order gets the bonus");
        Bestelling geholpen = null;
        for (Bestelling k : s.karren) {
            geholpen = k.hulp.isEmpty() ? geholpen : k;
        }
        if (s.ster != null && !s.ster.hulp.isEmpty()) {
            geholpen = s.ster;
        }
        helper.assertTrue(geholpen != null && geholpen.uitbetaling() == Math.round(geholpen.beloning * 1.2), "+20% Guhlden");
        helper.assertTrue(voor == b.beloning || b == geholpen, "only one order got it");
        helper.succeed();
    }

    /** Saving and loading keeps the map (villages, followers, version) and the orders (carts, star, stamps). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opslaanEnLaden(GameTestHelper helper) {
        KaartData a = data(42L);
        List<KaartLeven.Speler> spelers = List.of(new KaartLeven.Speler(P1, "Joost", 0, 0, 5), new KaartLeven.Speler(P2, "Mika", 1, 0, 2));
        KaartLeven.zorgBuren(a, spelers, cellen(a, spelers), 0L);
        for (int i = 0; i < 10; i++) {
            KaartLeven.groei(a, 5000L * i);
        }
        a.snel = true;
        KaartData b = KaartData.load(a.save());
        helper.assertTrue(a.save().equals(b.save()), "map: load(save) gives the same data");
        helper.assertTrue(b.eerste.equals(P1) && b.versie == KaartData.VERSIE && b.volgers(P2).size() == 1, "followers kept");
        BestelData d = new BestelData();
        BestelData.Speler s = d.van(P1);
        Bestellingen.bijwerken(s, omgeving(5, 4), 9L, P1, MIDDAG, false);
        s.stempels = 12;
        s.geholpen.add(P2);
        s.berichten.add("Mika|bouw:15");
        BestelData e = BestelData.load(d.save());
        helper.assertTrue(d.save().equals(e.save()), "orders: load(save) gives the same data");
        helper.assertTrue(e.van(P1).karren.size() == 4 && e.van(P1).ster != null, "carts and the star kept");
        helper.succeed();
    }
}
