package nl.juiced.guhs.feature.vadsrijk.voltooiing;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPlekken;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Ding;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Staat;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.ZoneBoek;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * I-VOLTOOIING: the counting of the Verkenningsboek. Zone 1 starts at 0 for a fresh player; every source moves exactly its own
 * category by one (a main quest done, a side quest busy = not counted, a lookout, a campfire through the kern hook snapped to the
 * known spot, a lost knuffel, a clue, a boss first-kill key, an area walked into, an area derived from a secret found there);
 * zone membership by id; the milestones (25/50/75/100 %, only the highest new one, never twice) and the 100 % reward (once).
 * Run: {@code runGameTestServer -Pgt=VadsrijkVoltooiing}.
 */
public class VadsrijkVoltooiingGameTests {
    private static final String BATCH = "vadsrijk_voltooiing";

    private static int cat(ServerPlayer p, ZoneBoek z, String id) {
        for (Categorie c : Voltooiing.categorieen(z)) {
            if (c.id().equals(id)) {
                return Voltooiing.tel(p, c).klaar();
            }
        }
        throw new IllegalStateException("no category " + id);
    }

    private static int totaal(ZoneBoek z, String id) {
        for (Categorie c : Voltooiing.categorieen(z)) {
            if (c.id().equals(id)) {
                return c.dingen().size();
            }
        }
        return -1;
    }

    private static void schoon(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        d.extra.remove("npc");
        d.extra.remove("huisdieren");
        d.extra.remove(Voltooiing.SLICE);
        d.gezien.removeIf(s -> s.startsWith("vr_w_uitkijk_") || s.startsWith("baaskill:"));
        d.checkpoint = null;
        d.gebied = null;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void telling(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        try {
            ZoneBoek z1 = ZoneBoeken.ZONE1, z2 = ZoneBoeken.ZONE2;
            helper.assertTrue(Voltooiing.zone("zone1") == z1 && Voltooiing.zone("zone2") == z2, "both zones registered");
            helper.assertTrue(Voltooiing.tel(p, z1).klaar() == 0, "a fresh player has done nothing in zone 1, got " + Voltooiing.tel(p, z1).klaar());
            helper.assertTrue(totaal(z1, "hoofd") == 13 && totaal(z2, "hoofd") == 12, "13 + 12 main quests (zone by id prefix)");
            helper.assertTrue(totaal(z1, "uitkijk") == WereldPlekken.UITKIJKEN.size() && totaal(z1, "kampvuur") == WereldPlekken.KAMPVUREN.size(),
                    "every lookout and campfire of zone 1");
            helper.assertTrue(totaal(z1, "knuffel") == NpcPlekken.KNUFFEL.size() && totaal(z1, "geheim") == NpcPlekken.AANWIJZING.size() + NpcPlekken.SCHAT.size(),
                    "knuffels; clues + treasures");
            // J-INT: zone 1 = Wolkenschaapje + Knuffelhert (Hoedhupper/Klodderros are gone), zone 2 = Guhwolf; a "later" category
            // (mounts start in zone 3): shown, but not in the zone total
            helper.assertTrue(totaal(z1, "rijdier") == 2 && totaal(z2, "rijdier") == 1, "mounts per zone");
            int zonderLater = 0;
            for (Categorie c : Voltooiing.categorieen(z1)) {
                if (c.id().equals("rijdier")) {
                    helper.assertTrue(c.later(), "the mounts are a later category");
                } else {
                    zonderLater += c.dingen().size();
                }
            }
            helper.assertTrue(Voltooiing.tel(p, z1).totaal() == zonderLater, "later categories are not counted in the zone total");
            helper.assertTrue(totaal(z1, "huisdier") == 2 && totaal(z2, "huisdier") == 1, "pets by their boss's zone");
            helper.assertTrue(ZoneBoeken.zoneVan("z2_hq01_x") == 2 && ZoneBoeken.zoneVan("z3_a") == 3 && ZoneBoeken.zoneVan("zij_x") == 1
                    && ZoneBoeken.zoneVan("z17_koning") == 1 && ZoneBoeken.zoneVan("z01_wolletjes") == 1, "zone ids (zone 1's z01..z20 stay zone 1)");

            // quests: done counts, busy doesn't
            CompoundTag q = new CompoundTag();
            CompoundTag k = new CompoundTag();
            k.putInt("s", Questlog.KLAAR);
            q.put("hq01_welkom", k);
            CompoundTag b = new CompoundTag();
            b.putInt("s", Questlog.ACTIEF);
            q.put("z01_wolletjes", b);
            Questlog.data(p).put("q", q);
            helper.assertTrue(cat(p, z1, "hoofd") == 1 && cat(p, z1, "zij") == 0, "one main quest done, a busy side quest not counted");
            helper.assertTrue(cat(p, z2, "hoofd") == 0, "zone 2 untouched");

            // a lookout
            Spelers.data(p).gezien.add("vr_w_uitkijk_" + WereldPlekken.UITKIJKEN.get(0).id());
            helper.assertTrue(cat(p, z1, "uitkijk") == 1, "lookout");

            // a campfire through the kern hook, clicked one block off the known spot
            BlockPos vuur = WereldPlekken.KAMPVUREN.get(0);
            Haken.KAMPVUUR.forEach(h -> h.accept(p, vuur.east()));
            helper.assertTrue(cat(p, z1, "kampvuur") == 1 && Voltooiing.vuurAan(p, vuur), "campfire snapped to the known one");

            // secrets: a knuffel and a clue
            Questlog.map(p, "knuffels").putBoolean("holle_boom", true);
            Questlog.map(p, "aanwijzingen").putBoolean("pluisplukje", true);
            helper.assertTrue(cat(p, z1, "knuffel") == 1 && cat(p, z1, "geheim") == 1, "knuffel + clue");

            // a boss by the kern first-kill key
            Spelers.data(p).gezien.add("baaskill:guhs:vr_baas_koning_klodder");
            helper.assertTrue(cat(p, z1, "baas") == 1, "Koning Klodder by kill key");

            // areas: walked into (hook) + derived (the knuffel in the Sporenwoud's hollow tree) + the campfire's area
            int voor = cat(p, z1, "gebieden");
            Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, "knuffelkamp"));
            helper.assertTrue(Voltooiing.bezocht(p, "knuffelkamp"), "the hook remembers the area");
            int na = cat(p, z1, "gebieden");
            helper.assertTrue(na == voor + 1 || voor > 0 && na >= voor, "area walked into counts (" + voor + " -> " + na + ")");
            String knuffelGebied = ZoneBoeken.gebiedId(NpcPlekken.KNUFFEL.get("holle_boom"), ZoneBoeken.GEBIEDEN_Z1);
            if (knuffelGebied != null) {
                helper.assertTrue(ZoneBoeken.bezochtAfgeleid(p, knuffelGebied, ZoneBoeken.GEBIEDEN_Z1),
                        "a secret found in " + knuffelGebied + " means you were there");
            }
            helper.assertTrue(Voltooiing.tel(p, z2).klaar() == 0, "nothing of this leaks into zone 2");

            // the screen data agrees with the counting
            CompoundTag scherm = Voltooiing.schermData(p);
            CompoundTag zt = scherm.getListOrEmpty("zones").getCompoundOrEmpty(0);
            helper.assertTrue(zt.getIntOr("k", -1) == Voltooiing.tel(p, z1).klaar() && zt.getIntOr("t", -1) == Voltooiing.tel(p, z1).totaal(),
                    "screen data = counting");
        } finally {
            schoon(p);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void mijlpalenEnBeloning(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        try {
            helper.assertTrue(Voltooiing.mijlpaalVoor(0) == 0 && Voltooiing.mijlpaalVoor(24) == 0 && Voltooiing.mijlpaalVoor(25) == 1
                    && Voltooiing.mijlpaalVoor(74) == 2 && Voltooiing.mijlpaalVoor(99) == 3 && Voltooiing.mijlpaalVoor(100) == 4, "steps");
            // a little test zone of 8 switches (not registered: no title, not in the real book)
            boolean[] aan = new boolean[8];
            List<Ding> dingen = new java.util.ArrayList<>();
            for (int i = 0; i < aan.length; i++) {
                int n = i;
                dingen.add(new Ding("t" + i, "x", "", "", "", false, sp -> aan[n] ? Staat.KLAAR : Staat.NIETS, null));
            }
            AtomicBoolean kapot = new AtomicBoolean();
            dingen.add(new Ding("kapot", "x", "", "", "", false, sp -> {
                kapot.set(true);
                throw new IllegalStateException("a broken slice");
            }, null));
            ZoneBoek test = new ZoneBoek("vr_test_zone", "x", "vr_test_titel", 1234, () -> List.of(new Categorie("c", "x", "minecraft:stone", dingen)));
            List<ZoneBoek> lijst = List.of(test);
            helper.assertTrue(Voltooiing.tel(p, test).totaal() == 9 && Voltooiing.tel(p, test).klaar() == 0 && kapot.get(),
                    "a broken entry counts as not done, never crashes");
            helper.assertTrue(Voltooiing.controleer(p, lijst).isEmpty(), "0 %: nothing");
            aan[0] = aan[1] = aan[2] = true;   // 3/9 = 33 %
            helper.assertTrue(Voltooiing.controleer(p, lijst).equals(List.of("vr_test_zone")), "25 % step");
            helper.assertTrue(Voltooiing.controleer(p, lijst).isEmpty(), "never twice");
            aan[3] = aan[4] = aan[5] = aan[6] = true;   // 7/9 = 77 %: jumps 50 and 75 -> one toast
            helper.assertTrue(Voltooiing.controleer(p, lijst).size() == 1 && Voltooiing.map(p, "mijlpaal").getIntOr("vr_test_zone", 0) == 3,
                    "only the highest new step");
            helper.assertTrue(!Voltooiing.beloond(p, "vr_test_zone"), "no reward before 100 %");
            long munten = Spelers.data(p).munten;
            // make the broken one "done" too: replace the zone with a full one
            aan[7] = true;
            ZoneBoek vol = new ZoneBoek("vr_test_zone", "x", "vr_test_titel", 1234, () -> List.of(new Categorie("c", "x", "minecraft:stone", dingen.subList(0, 8))));
            helper.assertTrue(Voltooiing.tel(p, vol).vol() && Voltooiing.tel(p, vol).procent() == 100, "100 %");
            Voltooiing.controleer(p, List.of(vol));
            helper.assertTrue(Voltooiing.beloond(p, "vr_test_zone") && Spelers.data(p).munten == munten + 1234, "100 %: the reward");
            Voltooiing.controleer(p, List.of(vol));
            helper.assertTrue(Spelers.data(p).munten == munten + 1234, "the reward only once");
            helper.assertTrue(Voltooiing.muntTekst(25000).equals("2 goud 50 zilver") && Voltooiing.muntTekst(0).equals("0 brons"), "coin text");
        } finally {
            schoon(p);
        }
        helper.succeed();
    }
}
