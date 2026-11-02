package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.gametest.GuhTest;

/** De Erfenis, wereldkaart: NPC village plans keep the base rules, the same seed gives the same map, the data survives saving. */
public class KaartGameTests {
    private static final String BATCH = "erfenis_kaart";

    private static KaartData data(long seed) {
        KaartData d = new KaartData();
        d.seed = seed;
        return d;
    }

    /** Every generated plan (all levels, many seeds) and every plan after 60 growth steps: valid, levels within the limits. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void npcPlannenBinnenDeRegels(GameTestHelper helper) {
        for (int seed = 0; seed < 120; seed++) {
            for (int level = 1; level <= KaartGebouwType.MAX_STADHUIS; level++) {
                Random r = new Random(seed * 31L + level);
                List<KaartGebouw> plan = new java.util.ArrayList<>(NpcBouwer.maak(level, r));
                List<String> f = NpcBouwer.fouten(plan);
                helper.assertTrue(f.isEmpty(), "plan seed " + seed + " level " + level + ": " + f);
                helper.assertTrue(NpcBouwer.stadhuisLevel(plan) == level, "the stadhuis has the asked level");
                for (int i = 0; i < 60; i++) {
                    NpcBouwer.groei(plan, r);
                    f = NpcBouwer.fouten(plan);
                    helper.assertTrue(f.isEmpty(), "after growth " + i + " (seed " + seed + ", start " + level + "): " + f);
                }
                helper.assertTrue(NpcBouwer.stadhuisLevel(plan) <= KaartGebouwType.MAX_STADHUIS, "stadhuis at most 5");
                helper.assertTrue(NpcBouwer.knusheid(plan) > 0, "it has Knusheid");
            }
        }
        helper.succeed();
    }

    /** The same seed seeds the same villages (names, cells, plans); a different seed a different map; cells are unique. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zelfdeZaadZelfdeKaart(GameTestHelper helper) {
        Set<Long> spelers = Set.of(KaartData.sleutel(0, 0), KaartData.sleutel(1, 0));
        KaartData a = data(12345L), b = data(12345L), c = data(999L);
        KaartLeven.zaai(a, 45, spelers, 0L);
        KaartLeven.zaai(b, 45, spelers, 0L);
        KaartLeven.zaai(c, 45, spelers, 0L);
        helper.assertTrue(a.npcs().size() >= 40, "about 45 villages were made (" + a.npcs().size() + ")");
        helper.assertTrue(a.save().equals(b.save()), "the same seed gives the same villages");
        helper.assertFalse(a.save().equals(c.save()), "another seed gives another map");
        Set<Long> cellen = new HashSet<>();
        for (NpcDorp d : a.npcs()) {
            helper.assertTrue(cellen.add(KaartData.sleutel(d.cx, d.cz)), "one village per cell (" + d.cx + "," + d.cz + ")");
            helper.assertFalse(Math.abs(d.cx) <= KaartLeven.KERN && Math.abs(d.cz) <= KaartLeven.KERN, "the middle stays free for players");
            helper.assertTrue(NpcBouwer.fouten(d.plan).isEmpty(), d.naam + " is a valid base");
            helper.assertFalse(d.naam.isBlank() || d.eigenaar.isBlank(), "a name and an owner");
        }
        // growing is just as stable: the same ticks on the same data give the same result
        for (int i = 0; i < 25; i++) {
            KaartLeven.groei(a, spelers, 1000L * i);
            KaartLeven.groei(b, spelers, 1000L * i);
        }
        helper.assertTrue(a.save().equals(b.save()), "the same seed grows the same way");
        helper.assertFalse(a.nieuws().isEmpty(), "growing makes news");
        helper.assertTrue(a.nieuws().size() <= KaartData.MAX_NIEUWS, "the news feed stays short");
        helper.succeed();
    }

    /** Saving and loading keeps everything (villages, plans, news, counters, the fallback player cells). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void opslaanEnLaden(GameTestHelper helper) {
        KaartData a = data(42L);
        KaartLeven.zaai(a, 30, Set.of(), 0L);
        for (int i = 0; i < 10; i++) {
            KaartLeven.groei(a, Set.of(), 5000L * i);
        }
        KaartLeven.nieuweDorpen(a, 3, Set.of(), 99_000L);
        a.snel = true;
        KaartData b = KaartData.load(a.save());
        helper.assertTrue(a.save().equals(b.save()), "load(save) gives the same data");
        helper.assertTrue(b.npcs().size() == a.npcs().size() && b.groeiTeller == a.groeiTeller && b.snel, "counts and switches kept");
        helper.succeed();
    }

    /** The map's copy of the building numbers matches the core, and every seeded plan becomes the same real base. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void zelfdeRegelsAlsDeKern(GameTestHelper helper) {
        for (KaartGebouwType k : KaartGebouwType.values()) {
            nl.juiced.guhs.feature.erfenis.GebouwType t = nl.juiced.guhs.feature.erfenis.GebouwType.van(k.id);
            helper.assertTrue(t != null, k.id + " exists in the core");
            helper.assertTrue(t.maat == k.maat && t.topLevel() == k.topLevel(), k.id + ": same size and top level");
            for (int th = 1; th <= KaartGebouwType.MAX_STADHUIS; th++) {
                // (the stadhuis's own max level means nothing in either: it may always go up to its top level)
                helper.assertTrue(t.maxAantal(th) == k.maxAantal(th) && (k == KaartGebouwType.STADHUIS || t.maxLevel(th) == k.maxLevel(th)),
                        k.id + ": same limits at stadhuis " + th);
            }
            for (int l = 0; l <= k.topLevel(); l++) {
                helper.assertTrue(t.knus(l) == k.knus(l), k.id + ": same Knusheid at level " + l);
            }
        }
        KaartData a = data(777L);
        KaartLeven.zaai(a, 40, Set.of(), 0L);
        for (NpcDorp d : a.npcs()) {
            nl.juiced.guhs.feature.erfenis.Base b = nl.juiced.guhs.feature.erfenis.Base.npc(d.id, d.naam, d.cx, d.cz, KaartBronErfenis.plan(d), 0, 0L);
            helper.assertTrue(b.gebouwen.size() == d.plan.size(), d.naam + ": every building of the plan made it into the base");
            helper.assertTrue(b.stadhuisLevel() == d.stadhuisLevel() && b.knusheid() == d.punten(), d.naam + ": same level and points");
        }
        helper.succeed();
    }
}
