package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Beloningen;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.Reiskaart;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoortConfig;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.4 "Ambachten in het verhaal": Meester Knutsel's islet lesson, the craft steps of every zone, the zone gate that asks for
 * them, and the migration of older players. Run: {@code runGameTestServer -Pgt=VadsrijkAmbachtVerhaal}.
 */
public class VadsrijkAmbachtVerhaalGameTests {
    private static final String BATCH = "vadsrijk_ambacht_verhaal";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Questlog.wisAlles(p);
        AmbachtData.reset(p);
        p.getInventory().clearContent();
        return p;
    }

    private static void opruimen(ServerPlayer p) {
        Questlog.wisAlles(p);
        AmbachtData.reset(p);
        p.getInventory().clearContent();
        Spelers.data(p).extra.remove("zonepoort");
        ZonePoort.vergeetCache(p.getUUID());
    }

    /** Every main quest of zones 1..n done, except the craft steps (an older player); the zone flags set. */
    private static void verhaalZonder(ServerPlayer p, int n) {
        for (Quest q : Quests.alle().values()) {
            if (q.hoofd() && ZoneBoeken.zoneVan(q.id()) <= n && !AmbachtVerhaal.isStap(q.id())) {
                zetKlaar(p, q);
            }
        }
        Questlog.zetVlag(p, NpcApi.ZONE1_VLAG, true);
        if (n >= 2) {
            Questlog.zetVlag(p, "zone2_klaar", true);
        }
    }

    /** Marks a quest done without its reward (no loot bundles in the test world). */
    private static void zetKlaar(ServerPlayer p, Quest q) {
        Questlog.neemAan(p, q);
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putInt("s", Questlog.KLAAR);
        ((net.minecraft.nbt.CompoundTag) Questlog.data(p).get("q")).put(q.id(), t);
    }

    static boolean heeft(Component c, String key) {
        if (c.getContents() instanceof TranslatableContents t) {
            if (t.getKey().equals(key)) {
                return true;
            }
            for (Object a : t.getArgs()) {
                if (a instanceof Component ac && heeft(ac, key)) {
                    return true;
                }
            }
        }
        for (Component s : c.getSiblings()) {
            if (heeft(s, key)) {
                return true;
            }
        }
        return false;
    }

    /** Hands a quest in the way QuestMotor does (rewards, then the after-action, e.g. "start:<next>"). */
    private static void inleveren(ServerPlayer p, Quest q) {
        Questlog.voltooi(p, q);
        QuestMotor.doeActie(p, q.actieKlaar(), null);
    }

    // ---- the data ------------------------------------------------------------------------------------------------------------------

    /** The chains are in the questlog at their place, the quest after each waits for it, recipes have the zone's tier level. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void ketensEnRecepten(GameTestHelper helper) {
        List<String> fout = AmbachtVerhaal.fouten();
        helper.assertTrue(fout.isEmpty(), "chains: " + fout);
        List<String> volgorde = new ArrayList<>(Quests.alle().keySet());
        for (AmbachtVerhaal.Keten k : AmbachtVerhaal.KETENS) {
            Quest eerste = Quests.van(k.quests().get(0));
            int i = volgorde.indexOf(k.quests().get(0));
            helper.assertTrue(i > 0 && volgorde.get(i - 1).equals(eerste.na().get(0)), k.id() + ": right after " + eerste.na());
            for (int j = 1; j < k.quests().size(); j++) {
                helper.assertTrue(volgorde.indexOf(k.quests().get(j)) == i + j, k.id() + ": steps in order");
            }
            helper.assertTrue(volgorde.indexOf(k.volgende()) > i + k.quests().size() - 1, k.id() + ": the next main quest comes after");
            for (String id : k.quests()) {
                helper.assertTrue(!ZoneBoeken.quests(k.zone(), Quest.Soort.HOOFD).stream().anyMatch(d -> d.id().equals(id)), id + " is no book row");
            }
        }
        helper.assertTrue(volgorde.indexOf("hq03c_opruimen") < volgorde.indexOf("hq03d_knutsel")
                && volgorde.indexOf("hq03f_maken") < volgorde.indexOf("hq04_kampvuur"), "the lesson sits between Opruimen and the campfire");
        // story recipes: the zone's tier level (z1 5, z2 12, z3 22, z4 32, z5 40, z6 48)
        Object[][] eis = {{"tempelsleutel", Ambacht.SMEDEN, 5}, {"warme_kandijsoep", Ambacht.KOKEN, 12}, {"lijmdrankje", Ambacht.KOKEN, 22},
                {"koraallampion", Ambacht.SMEDEN, 32}, {"wolkendekentje", Ambacht.SMEDEN, 40}, {"kompaskoord", Ambacht.SMEDEN, 48}};
        for (Object[] e : eis) {
            Recepten.Recept r = Recepten.van((String) e[0]);
            helper.assertTrue(r != null && r.soort().equals("verhaal") && r.ambacht() == e[1] && r.level() == (int) e[2],
                    e[0] + ": " + (r == null ? "missing" : r.ambacht() + " " + r.level()));
            helper.assertTrue(!Recepten.toon(r).isEmpty(), e[0] + ": the story item exists");
            // the step asks exactly for this item
            Quest q = Quests.van(AmbachtVerhaal.RECEPT_STAP.get(r.id()));
            helper.assertTrue(q != null && q.doelen().stream().anyMatch(d -> d.type() == Quest.Doel.Type.VERZAMEL
                    && Questlog.item(d.doel()) == Recepten.toon(r).getItem()), e[0] + ": its step asks for it");
        }
        Recepten.Recept klein = Recepten.van("klein_vadsdrankje");
        helper.assertTrue(klein != null && klein.ambacht() == Ambacht.KOKEN && klein.level() == 1 && Recepten.toon(klein).is(EcoSpul.KLEIN_VADSDRANKJE.get()),
                "Klein Vadsdrankje: a Koken 1 recipe");
        // main-story xp factors of 1.10.3 apply to the craft steps too
        helper.assertTrue(Beloningen.xpFactor(Quests.van("z2_hq06a_ijsforellen")) == Beloningen.HOOFD_XP_ZONE2
                && Beloningen.xpFactor(Quests.van("z3_hq07b_lijmdrankje")) == Beloningen.HOOFD_XP_LAAT
                && Beloningen.xpFactor(Quests.van("hq10a_zwamhout")) == 1.0, "xp factors per zone");
        helper.succeed();
    }

    /** Every gathering step's materials come from a gathering group of that zone at that tier (zone 6: Hoogvads, no T6 yet). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void materialenTeHalen(GameTestHelper helper) {
        for (AmbachtVerhaal.Keten k : AmbachtVerhaal.KETENS) {
            if (k.id().equals("les")) {
                continue;
            }
            int zone = Math.min(5, k.zone());   // (zone 6 has no gathering spots yet: Zone6Haken)
            Quest q = Quests.van(k.quests().get(0));
            for (Quest.Doel d : q.doelen()) {
                helper.assertTrue(d.type() == Quest.Doel.Type.VERZAMEL && Questlog.HOUD.equals(d.bron()), q.id() + ": materials kept for the next step");
                Materialen.Materiaal m = Materialen.van(new ItemStack(Questlog.item(d.doel())));
                helper.assertTrue(m != null, q.id() + ": " + d.doel() + " is a material");
                boolean ergens = false;
                for (AmbachtPlekken.Groep g : AmbachtPlekken.GROEPEN) {
                    if (g.zone() != zone || g.anker() == null) {
                        continue;
                    }
                    ergens |= g.soort() == NodeSoort.KORF ? m.id().equals(g.honing())
                            : g.soort() == m.soort() && g.tier() == m.tier() || m.id().equals("roze_hars") && g.soort() == NodeSoort.HOUT && g.tier() == 3;
                }
                helper.assertTrue(ergens, q.id() + ": " + m.id() + " is gathered somewhere in zone " + zone);
                int nodig = m.soort() == NodeSoort.KORF ? Imkeren.korfLevel(zone) : AmbachtXp.tierLevel(m.tier());
                helper.assertTrue(nodig <= Math.max(1, AmbachtXp.TIER_MIN[zone]), q.id() + ": " + m.id() + " needs level " + nodig);
            }
        }
        helper.succeed();
    }

    /** The islet: Meester Knutsel, the four hut masters on their POIs, six T1 groups with a fast respawn, the boards, z2/z3 masters. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void eilandGeregistreerd(GameTestHelper helper) {
        NpcDef k = Npcs.van(EilandRegistratie.KNUTSEL);
        helper.assertTrue(k != null && k.plek().equals(WereldPlekken.plek("ambacht_knutsel")), "Meester Knutsel on POI ambacht_knutsel");
        for (Map.Entry<String, String> e : EilandRegistratie.MEESTERS.entrySet()) {
            AmbachtPlekken.Werkplaats w = AmbachtPlekken.werkplaats(e.getKey());
            helper.assertTrue(w != null && w.zone() == 1 && w.poi().equals(e.getValue()) && w.anker() != null && AmbachtPlekken.VAST.contains(e.getKey()),
                    e.getKey() + " on " + e.getValue());
            helper.assertTrue(AmbachtNpcs.rol(w.ambacht()).equals(e.getKey().split("_")[1]), e.getKey() + ": its role");
        }
        for (Map.Entry<String, String> e : EilandRegistratie.GROEPEN.entrySet()) {
            AmbachtPlekken.Groep g = AmbachtPlekken.groep(e.getKey());
            helper.assertTrue(g != null && g.zone() == 1 && g.tier() == 1 && g.poi().equals(e.getValue()) && g.anker() != null, e.getKey() + " at " + e.getValue());
            if (g.soort() != NodeSoort.KORF) {
                helper.assertTrue(g.leegMin() == 20 && g.leegMax() == 30 && g.aantal() >= 5, e.getKey() + ": 20-30 s, " + g.aantal() + " spots");
            }
        }
        for (String[] m : new String[][] {{"amb_verzamel_z2", "2"}, {"amb_vis_z2", "2"}, {"amb_verzamel_z3", "3"}, {"amb_vis_z3", "3"}}) {
            AmbachtPlekken.Werkplaats w = AmbachtPlekken.werkplaats(m[0]);
            helper.assertTrue(w != null && w.zone() == Integer.parseInt(m[1]) && w.ambacht() != null && w.ambacht().verzamel && w.anker() != null, m[0] + ": hub master");
        }
        helper.assertTrue(EilandRegistratie.bordId(WereldPlekken.plek("ambacht_brug_eiland")).equals("welkom"), "welcome board at the bridge");
        helper.assertTrue(EilandRegistratie.bordId(WereldPlekken.plek("ambacht_smederij.deur").offset(-2, 0, 1)).equals("smederij"), "smithy board");
        helper.assertTrue(EilandRegistratie.bordId(WereldPlekken.plek("ambacht_groeve.midden")).equals("wegwijzer"), "a signpost elsewhere");
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(EilandRegistratie.bordTekst(p, WereldPlekken.plek("ambacht_plein")) != null, "boards on the islet get a text");
        helper.assertTrue(EilandRegistratie.bordTekst(p, new BlockPos(0, 64, 0)) == null, "elsewhere the safari text stays");
        helper.succeed();
    }

    // ---- the tutorial --------------------------------------------------------------------------------------------------------------

    /** Opruimen -> Meester Knutsel -> gather/fish on the islet -> ring + Klein Vadsdrankje -> back to the campfire. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void tutorialLes(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            for (String id : List.of("hq01_welkom", "hq02_pad", "hq03_poppen", "hq03b_elementen")) {
                zetKlaar(p, Quests.van(id));
            }
            Quest opruimen = Quests.van("hq03c_opruimen");
            Questlog.neemAan(p, opruimen);
            NpcApi.meld(p, "opruimen", "verkocht");
            NpcApi.meld(p, "opruimen", "ontmanteld");
            inleveren(p, opruimen);
            Quest d = Quests.van("hq03d_knutsel"), e = Quests.van("hq03e_verzamelen"), f = Quests.van("hq03f_maken");
            helper.assertTrue(Questlog.beschikbaar(p, d) && d.gever().equals("kok"), "after Opruimen the cook offers 'Meester Knutsel wacht'");
            helper.assertTrue(!Questlog.beschikbaar(p, Quests.van("hq04_kampvuur")), "the campfire waits for the lesson");
            QuestStap s = QuestStap.hoofdlijn(p);
            helper.assertTrue(s != null && s.quest().equals(d.id()) && s.doel() != null, "the tracker: the cook first: " + (s == null ? null : s.quest()));
            Questlog.neemAan(p, d);
            s = QuestStap.hoofdlijn(p);
            helper.assertTrue(s != null && s.quest().equals(d.id()) && s.doel() != null && s.doel().equals(WereldPlekken.plek("ambacht_brug_kamp")),
                    "then the tracker points at the bridge");
            Questlog.tel(p, Quest.Doel.Type.GEBIED, x -> x.doel().equals("ambachtseiland"), 1);
            Questlog.tel(p, Quest.Doel.Type.PRAAT, x -> x.doel().equals(EilandRegistratie.KNUTSEL), 1);
            helper.assertTrue(Questlog.allesKlaar(p, d) && d.inleveren().equals(EilandRegistratie.KNUTSEL), "over the bridge, Knutsel greeted");
            inleveren(p, d);
            helper.assertTrue(Questlog.actief(p, e.id()), "lesson one starts by itself");
            // gather on the islet (the materials come in through the MateriaalTas, as the gathering does)
            MateriaalTas.geef(p, Materialen.stack("pluisijzer", 0, 1));
            helper.assertTrue(!Questlog.doelKlaar(p, e, 0), "one ore of two");
            MateriaalTas.geef(p, Materialen.stack("pluisijzer", 1, 1));
            MateriaalTas.geef(p, Materialen.stack("klodderklaver", 0, 1));
            MateriaalTas.geef(p, Materialen.stack("kloddervisje", 0, 1));
            helper.assertTrue(Questlog.allesKlaar(p, e), "ore x2, herbs, a fish");
            inleveren(p, e);
            helper.assertTrue(Questlog.actief(p, f.id()), "lesson two starts by itself");
            helper.assertTrue(MateriaalTas.tel(p, x -> Materialen.van(x) != null && Materialen.van(x).id().equals("pluisijzer")) >= 5,
                    "Knutsel gave the ring materials");
            List<nl.juiced.guhs.feature.vadsrijk.kern.Stat> twee = Maatwerk.keuzes().subList(0, 2);
            String ring = Werkplaats.maak(p, "ring_t1", twee, null, null, 1, 1);
            helper.assertTrue(ring.equals("vadsrijk.ambacht.werk.klaar") || ring.equals("vadsrijk.ambacht.werk.vadsig"), "ring forged: " + ring);
            helper.assertTrue(Questlog.doelKlaar(p, f, 0), "the ring counts");
            String drank = Werkplaats.maak(p, "klein_vadsdrankje", List.of(), null, null, 1, 1);
            helper.assertTrue(drank.startsWith("vadsrijk.ambacht.werk.") && !drank.endsWith("te_weinig") && !drank.endsWith("te_laag"), "potion: " + drank);
            helper.assertTrue(Questlog.allesKlaar(p, f), "ring + Klein Vadsdrankje");
            inleveren(p, f);
            int kleine = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(EcoSpul.KLEIN_VADSDRANKJE.get())) {
                    kleine += p.getInventory().getItem(i).getCount();
                }
            }
            helper.assertTrue(kleine >= 6, "3 made + 3 from Knutsel: " + kleine);
            helper.assertTrue(Questlog.beschikbaar(p, Quests.van("hq04_kampvuur")), "back to the campfire");
            helper.assertTrue(AmbachtVerhaal.telling(p, 1)[0] == 3, "the lesson counts for the zone gate");
        } finally {
            opruimen(p);
        }
        helper.succeed();
    }

    // ---- the zone steps ------------------------------------------------------------------------------------------------------------

    /** Every zone's two steps can be played: gather (kept), make the story item at the tier level (only then), hand it in. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void zoneStappen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ZonePoortConfig.TEST = 6;
        try {
            for (AmbachtVerhaal.Keten k : AmbachtVerhaal.KETENS) {
                if (k.id().equals("les")) {
                    continue;
                }
                Quest verzamel = Quests.van(k.quests().get(0)), maak = Quests.van(k.quests().get(1));
                for (String na : verzamel.na()) {
                    zetKlaar(p, Quests.van(na));
                }
                ZonePoort.zetHoogste(p, k.zone());
                Spelers.zetLevel(p, Math.max(Quests.van(k.volgende()).lv(), Math.max(verzamel.lv(), maak.lv())));
                helper.assertTrue(Questlog.beschikbaar(p, verzamel), k.id() + ": the gathering step is offered");
                helper.assertTrue(!Questlog.beschikbaar(p, Quests.van(k.volgende())), k.id() + ": " + k.volgende() + " waits");
                Questlog.neemAan(p, verzamel);
                for (Quest.Doel d : verzamel.doelen()) {
                    MateriaalTas.geef(p, new ItemStack(Questlog.item(d.doel()), d.n()));
                }
                helper.assertTrue(Questlog.allesKlaar(p, verzamel), k.id() + ": gathered");
                String recept = null;
                for (Map.Entry<String, String> e : AmbachtVerhaal.RECEPT_STAP.entrySet()) {
                    if (e.getValue().equals(maak.id())) {
                        recept = e.getKey();
                    }
                }
                Recepten.Recept r = Recepten.van(recept);
                helper.assertTrue(!AmbachtVerhaal.magMaken(p, recept) && Werkplaats.maak(p, recept, List.of(), null, null, 0, 1).equals("vadsrijk.ambacht.werk.verhaal_niet"),
                        k.id() + ": not makeable before its step");
                inleveren(p, verzamel);
                helper.assertTrue(Questlog.actief(p, maak.id()), k.id() + ": the making step starts by itself");
                AmbachtData.zetLevel(p, r.ambacht(), r.level() - 1);
                helper.assertTrue(Werkplaats.maak(p, recept, List.of(), null, null, 0, 1).equals("vadsrijk.ambacht.werk.te_laag"), k.id() + ": level " + r.level() + " needed");
                AmbachtData.zetLevel(p, r.ambacht(), r.level());
                String m = Werkplaats.maak(p, recept, List.of(), null, null, 0, 1);
                helper.assertTrue(m.equals("vadsrijk.ambacht.werk.klaar") || m.equals("vadsrijk.ambacht.werk.vadsig"), k.id() + ": made " + m);
                helper.assertTrue(Questlog.allesKlaar(p, maak) && !AmbachtVerhaal.magMaken(p, recept), k.id() + ": the story item done, no second one");
                inleveren(p, maak);
                helper.assertTrue(Questlog.klaar(p, maak.id()) && Questlog.beschikbaar(p, Quests.van(k.volgende())) || Quests.van(k.volgende()).auto(),
                        k.id() + ": the story goes on with " + k.volgende());
                helper.assertTrue(MateriaalTas.tel(p, x -> Materialen.van(x) != null) == 0, k.id() + ": the materials went into the item");
                AmbachtData.reset(p);
            }
        } finally {
            ZonePoortConfig.TEST = null;
            opruimen(p);
        }
        helper.succeed();
    }

    // ---- the gate ------------------------------------------------------------------------------------------------------------------

    /** An older player: story + book done, craft steps missing: the next zone stays closed, everywhere says why; done = open. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void poortVraagtAmbachten(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            ZonePoortConfig.TEST = 6;
            ZonePoortConfig.TEST_BOEK = 0;
            ZonePoort.TEST_STRENG = true;
            verhaalZonder(p, 1);
            Spelers.zetLevel(p, 20);
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(ZonePoort.verhaalKlaar(p, 1) && ZonePoort.boekOk(p, 1) && !ZonePoort.ambachtOk(p, 1), "story + book, no craft steps");
            helper.assertTrue(ZonePoort.dicht(p, 2), "zone 2 closed without the craft steps");
            String slot = QuestMotor.reisSlot(p, "kandijkom");
            helper.assertTrue((ZonePoort.K + "eis.2").equals(slot), "the captain's per-player line: " + slot);
            Object[] a = ZonePoort.eisArgs(p, 2);
            helper.assertTrue(a.length == 9 && a[6].toString().contains("✘") && (int) a[7] == 0 && (int) a[8] == 5, "✘ ambachtstappen (0/5)");
            QuestStap s = QuestStap.hoofdlijn(p);
            helper.assertTrue(s != null && heeft(s.titel(), "vadsrijk.zonepoort.ambacht.titel") && s.quest().equals("hq03d_knutsel"),
                    "tracker: 'Volgende: ambachtstap ...' " + (s == null ? null : s.quest()));
            var rp = Reiskaart.plek(p, "kandijkom");
            helper.assertTrue("vadsrijk.zonepoort.eis.kaart_ambacht".equals(rp.reden()), "travel map: the craft steps line: " + rp.reden());
            helper.assertTrue(!Questlog.beschikbaar(p, Quests.van("z2_hq01_splinterpieken")), "zone 2's first quest not offered");
            // the craft steps done: open
            for (AmbachtVerhaal.Keten k : AmbachtVerhaal.KETENS) {
                if (k.zone() == 1) {
                    for (String id : k.quests()) {
                        zetKlaar(p, Quests.van(id));
                    }
                }
            }
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(!ZonePoort.dicht(p, 2) && QuestMotor.reisSlot(p, "kandijkom") == null, "all craft steps: zone 2 open");
            helper.assertTrue(ZonePoort.eisArgs(p, 2)[6].toString().contains("✔"), "✔ ambachtstappen");
            // a player already in zone 3 is never sent back; going further asks for zones 1-3's steps
            Questlog.wisAlles(p);
            Spelers.data(p).extra.remove("zonepoort");
            verhaalZonder(p, 2);
            ZonePoort.zetHoogste(p, 2);
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(!ZonePoort.dicht(p, 2), "in zone 2 already: stays open");
            helper.assertTrue(ZonePoort.dicht(p, 3), "zone 3: the steps of zones 1 and 2 first");
            helper.assertTrue((int) ZonePoort.eisArgs(p, 3)[8] == 7, "zones 1+2: 7 craft steps");
            ZonePoort.zetHoogste(p, 3);
            ZonePoort.vergeetCache(p.getUUID());
            helper.assertTrue(!ZonePoort.dicht(p, 3), "already been in zone 3: never thrown back");
        } finally {
            ZonePoortConfig.TEST = null;
            ZonePoortConfig.TEST_BOEK = null;
            ZonePoort.TEST_STRENG = false;
            opruimen(p);
        }
        helper.succeed();
    }

    // ---- older players -------------------------------------------------------------------------------------------------------------

    /** Past hq06: the lesson arrives as an active quest; past zone 2: the zone 1 and 2 steps too; nothing twice; new players nothing. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bestaandeSpelers(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            // a new player who just did Opruimen: nothing to migrate
            for (String id : List.of("hq01_welkom", "hq02_pad", "hq03_poppen", "hq03b_elementen", "hq03c_opruimen")) {
                zetKlaar(p, Quests.van(id));
            }
            helper.assertTrue(AmbachtVerhaal.bijwerken(p) == 0 && !AmbachtVerhaal.gemist(p, "hq03d_knutsel"), "a new player: no letter");
            // past hq06 (before 1.10.4)
            for (String id : List.of("hq04_kampvuur", "hq05_klodderreus", "hq06_zwamzoet")) {
                zetKlaar(p, Quests.van(id));
            }
            helper.assertTrue(AmbachtVerhaal.gemist(p, "hq03d_knutsel"), "the lesson is behind this player");
            helper.assertTrue(AmbachtVerhaal.bijwerken(p) == 1 && Questlog.actief(p, "hq03d_knutsel"), "Meester Knutsel's letter: the lesson is active");
            helper.assertTrue(AmbachtVerhaal.bijwerken(p) == 0, "only once");
            QuestStap s = QuestStap.hoofdlijn(p);
            helper.assertTrue(s != null && s.quest().equals("hq07_littekens"), "the tracker keeps the real story: " + (s == null ? null : s.quest()));
            // past zone 2's story: the zone-1 and zone-2 steps too
            Questlog.wisAlles(p);
            verhaalZonder(p, 2);
            ZonePoort.zetHoogste(p, 2);
            int n = AmbachtVerhaal.bijwerken(p);
            helper.assertTrue(n == 3 && Questlog.actief(p, "hq03d_knutsel") && Questlog.actief(p, "hq10a_zwamhout") && Questlog.actief(p, "z2_hq06a_ijsforellen"),
                    "lesson + zone 1 + zone 2 started: " + n);
            helper.assertTrue(Questlog.status(p, "z3_hq07a_hars") == 0, "zone 3's step not (not past it)");
            helper.assertTrue(AmbachtVerhaal.telling(p, 2)[1] == 7 && AmbachtVerhaal.telling(p, 2)[0] == 0, "7 steps to do for zone 3");
            // a missed zone step can be finished where its giver is
            Quest z1 = Quests.van("hq10a_zwamhout");
            for (Quest.Doel d : z1.doelen()) {
                MateriaalTas.geef(p, new ItemStack(Questlog.item(d.doel()), d.n()));
            }
            inleveren(p, z1);
            helper.assertTrue(Questlog.actief(p, "hq10b_tempelsleutel"), "the next missed step follows by itself");
        } finally {
            opruimen(p);
        }
        helper.succeed();
    }
}
