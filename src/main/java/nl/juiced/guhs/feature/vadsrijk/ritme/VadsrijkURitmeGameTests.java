package nl.juiced.guhs.feature.vadsrijk.ritme;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.vadsrijk.hud.Emote;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-LEVEN-RITME: the night schedule never blocks a quest (the quest robot's assumption: quest NPCs reachable 24/7), the night
 * bell / sleepy shopkeeper always opens the service, the knuffelmeter (once per guh per day, rewards at the thresholds, guhs react
 * to emotes) and the Pluizenblaadje (chores the same for everybody, reset per day, every text exists). Run:
 * {@code runGameTestServer -Pgt=VadsrijkURitme}.
 */
public class VadsrijkURitmeGameTests {
    private static final String B = "vadsrijk_u_ritme";

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void roosterBlokkeertNooitQuests(GameTestHelper helper) {
        Set<String> quest = Rooster.questNpcs();
        helper.assertTrue(!Quests.alle().isEmpty() && quest.size() > 20, "the quests are loaded: " + quest.size() + " NPC ids");
        int dicht = 0, winkels = 0;
        List<String> fout = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            for (String id : List.of(q.gever(), q.inleveren())) {
                NpcDef d = Npcs.van(id);
                if (d != null && Rooster.magSluiten(d)) {
                    fout.add(q.id() + ":" + id);
                }
            }
            for (Quest.Doel doel : q.doelen()) {
                NpcDef d = Npcs.van(doel.doel());
                if (d != null && Rooster.magSluiten(d)) {
                    fout.add(q.id() + ":" + doel.doel());
                }
            }
        }
        helper.assertTrue(fout.isEmpty(), "no quest NPC ever goes home at night: " + fout);
        for (NpcDef d : Npcs.alle()) {
            if (d.rol() != null && Rooster.WINKELROLLEN.contains(d.rol())) {
                winkels++;
            }
            if (!Rooster.magSluiten(d)) {
                continue;
            }
            dicht++;
            helper.assertTrue(NpcRollen.gekoppeld(d.rol()) || NpcRollen.bestaat(d.rol()), d.id() + ": the night bell can open " + d.rol());
            helper.assertTrue(!QuestMotor.reisNpc(d.id()) && !QuestMotor.EXTRA.containsKey(d.id()), d.id() + ": no travel/extra choices behind a closed door");
            helper.assertTrue(Steden.op(d.plek()) != null, d.id() + ": only town shops close");
            helper.assertTrue(!quest.contains(d.id()), d.id() + ": not in any quest");
        }
        helper.assertTrue(winkels > 0, "there are shopkeepers: " + winkels + " (closing at night: " + dicht + ")");
        // the sleepy shopkeeper: a click opens the shop (never blocks); an awake guh is left to the quest engine
        ServerPlayer p = GuhMockPlayer.of(helper);
        NpcDef winkel = null;
        for (NpcDef d : Npcs.alle()) {
            if (Rooster.magSluiten(d)) {
                winkel = d;
                break;
            }
        }
        if (winkel != null) {
            RijkGuh g = NpcRegistry.GUH.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
            BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
            g.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            g.stelIn(winkel);
            helper.getLevel().addFreshEntity(g);
            try {
                helper.assertTrue(!Rooster.klikVoor(p, g), "awake: the normal click (quests first)");
                Rooster.testSlaap(winkel.id(), g, true);
                helper.assertTrue(Rooster.slaapt(g) && RijkGuh.STIL.test(g), "asleep: STIL (no waving/strolling)");
                helper.assertTrue(Rooster.klikVoor(p, g), "asleep: the click still opens " + winkel.rol());
            } finally {
                Rooster.testSlaap(winkel.id(), g, false);
                g.discard();
            }
        }
        // the clock
        helper.assertTrue(Rooster.nachtOp(18000) && Rooster.nachtOp(Rooster.DICHT) && !Rooster.nachtOp(6000) && !Rooster.nachtOp(Rooster.OPEN),
                "night = 13000..23000");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void knuffelmeter(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Knuffelmeter.reset(p);
        long munten0 = Spelers.data(p).munten;
        long dag = 1000;
        helper.assertTrue(Knuffelmeter.tel(p, "zwamzoet", "npc:oma_truus", dag), "the first wave at Oma Truus counts");
        helper.assertTrue(!Knuffelmeter.tel(p, "zwamzoet", "npc:oma_truus", dag), "the same guh the same day: no second point");
        helper.assertTrue(Knuffelmeter.stand(p, "zwamzoet") == 1, "1 in Zwamzoet");
        for (int i = 0; i < 4; i++) {
            Knuffelmeter.tel(p, "zwamzoet", "guh" + i, dag);
        }
        helper.assertTrue(Knuffelmeter.stand(p, "zwamzoet") == Knuffelmeter.DREMPELS[0], "5 in Zwamzoet");
        helper.assertTrue(Spelers.data(p).munten == munten0 + Knuffelmeter.DREMPEL_MUNTEN[0], "the first threshold pays " + Knuffelmeter.DREMPEL_MUNTEN[0]);
        helper.assertTrue(Knuffelmeter.tel(p, "zwamzoet", "npc:oma_truus", dag + 1), "the next day Oma Truus counts again");
        helper.assertTrue(Knuffelmeter.tel(p, "kandijkom", "npc:oma_truus", dag + 1), "towns count separately");
        helper.assertTrue(Knuffelmeter.totaal(p) == 7 && Knuffelmeter.standen(p).size() == 2, "total 7 over 2 towns: " + Knuffelmeter.totaal(p));
        Knuffelmeter.tel(p, "zwamzoet", "x", dag + 1);
        helper.assertTrue(Spelers.data(p).munten == munten0 + Knuffelmeter.DREMPEL_MUNTEN[0], "a threshold pays only once");
        // the Karakter line
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        Knuffelmeter.karakter(p, t);
        helper.assertTrue(t.getListOrEmpty("extra").size() == 2, "two lines in the Karakter tab");
        // reactions: a guh next to you hugs back; a wave queues a wave back
        RijkGuh g = NpcRegistry.GUH.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        g.snapTo(p.getX() + 1.2, p.getY(), p.getZ());
        helper.getLevel().addFreshEntity(g);
        try {
            helper.assertTrue(Knuffelmeter.opEmote(p, Emote.KNUFFEL), "the guh hugs back");
            helper.assertTrue(!Knuffelmeter.opEmote(p, Emote.ZIT), "sitting down: no hug");
        } finally {
            g.discard();
            Knuffelmeter.reset(p);
            Knuffelmeter.wis();
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void krantReset(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        JsonObject nl = taal("nl_nl"), en = taal("en_us");
        helper.assertTrue(nl != null && en != null, "lang files readable");
        int metKlusjes = 0;
        for (Steden.Stad s : Steden.ALLE) {
            helper.assertTrue(nl.has(s.naamKey()) && en.has(s.naamKey()), "town name " + s.id());
            if (s.bord() == null) {
                continue;
            }
            helper.assertTrue(nl.has(s.krantKey()) && en.has(s.krantKey()), "newspaper title " + s.id());
            for (long dag = 0; dag < 6; dag++) {
                for (Praat.Regel r : Pluizenblaadje.nieuws(sl, s, dag)) {
                    helper.assertTrue(nl.has(r.tekstKey()) && en.has(r.tekstKey()), s.id() + " day " + dag + ": text " + r.tekstKey());
                    for (Object a : r.args()) {
                        if (a instanceof Component c && c.getContents() instanceof TranslatableContents tc && tc.getKey().startsWith(Ritme.P)) {
                            helper.assertTrue(nl.has(tc.getKey()), "arg text " + tc.getKey());
                        }
                    }
                }
            }
            List<Pluizenblaadje.Klus> a = Pluizenblaadje.klusjes(s, 500), b = Pluizenblaadje.klusjes(s, 500);
            helper.assertTrue(a.equals(b), s.id() + ": the same chores for everybody on a day");
            if (!a.isEmpty()) {
                metKlusjes++;
                helper.assertTrue(a.size() == Pluizenblaadje.KLUSJES && a.get(0).soort() != a.get(1).soort(), s.id() + ": two different chores");
                boolean anders = false;
                for (long d = 501; d < 510 && !anders; d++) {
                    anders = !Pluizenblaadje.klusjes(s, d).equals(a);
                }
                helper.assertTrue(anders, s.id() + ": the chores change from day to day");
                for (Pluizenblaadje.Klus k : a) {
                    helper.assertTrue(!Rooster.questNpcs().contains(k.vrager()) && !Rooster.questNpcs().contains(k.ander()), "chore citizens have no quest role");
                    helper.assertTrue(nl.has(Ritme.P + "krant.klus." + k.soort().name().toLowerCase(java.util.Locale.ROOT)), "chore text");
                }
            }
        }
        helper.assertTrue(metKlusjes >= 1, "at least one town has chores: " + metKlusjes);
        // progress per player resets with the day (a non-find chore: no world spot needed)
        Steden.Stad zz = null;
        Pluizenblaadje.Klus k = null;
        long vandaag = Pluizenblaadje.dag(sl);
        for (Steden.Stad s : Steden.ALLE) {
            for (Pluizenblaadje.Klus x : s.bord() == null ? List.<Pluizenblaadje.Klus>of() : Pluizenblaadje.klusjes(s, vandaag)) {
                if (x.soort() != Pluizenblaadje.Soort.ZOEK && k == null) {
                    zz = s;
                    k = x;
                }
            }
        }
        if (k != null) {
            try {
                helper.assertTrue(Pluizenblaadje.staat(p, k, vandaag) == Pluizenblaadje.OPEN, "open at first");
                helper.assertTrue(Pluizenblaadje.neem(p, sl, k) && Pluizenblaadje.staat(p, k, vandaag) == Pluizenblaadje.BEZIG, "taken");
                helper.assertTrue(!Pluizenblaadje.lever(p, sl, k), "not ready yet: no reward");
                Pluizenblaadje.dagVerschuiving = 1;
                helper.assertTrue(Pluizenblaadje.staat(p, k, vandaag + 1) == Pluizenblaadje.OPEN, "a new day: progress gone (" + zz.id() + ")");
            } finally {
                Pluizenblaadje.dagVerschuiving = 0;
                Knuffelmeter.reset(p);
                Knuffelmeter.data(p).remove("klus");
            }
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void stedenEnWeer(GameTestHelper helper) {
        // my town boxes agree with kern's areas where those exist in this tree
        for (Steden.Stad s : Steden.ALLE) {
            for (String id : s.gebieden()) {
                Gebieden.Gebied g = Gebieden.van(id);
                if (g == null || g.laag() != null) {
                    continue;
                }
                double cx = (g.minX() + g.maxX()) / 2.0, cz = (g.minZ() + g.maxZ()) / 2.0;
                helper.assertTrue(s.bevatXZ(cx, cz), s.id() + " contains the middle of kern area " + id);
            }
        }
        helper.assertTrue(Steden.zone(0, 64, 0) == 1 && Steden.zone(3392, 69, -714) == 6 && Steden.zone(2748, 219, -529) == 5 && Steden.zone(1848, 67, -467) == 4,
                "zones by position");
        helper.assertTrue(Steden.op(2748, 219, -529) == Steden.van("hoogvads") && Steden.op(1848, 67, -467) == Steden.van("lampionhaven"), "towns by position");
        // the weather timetable: deterministic, sometimes a shower, never in zone 1 (fix-sfeer has its own), mist every other day
        int buien = 0, mist = 0;
        for (long d = 0; d < 30; d++) {
            helper.assertTrue(WeerKlok.buiStart(d, 4) == WeerKlok.buiStart(d, 4), "deterministic");
            helper.assertTrue(WeerKlok.buiStart(d, 1) < 0, "zone 1 keeps fix-sfeer's pink shower");
            if (WeerKlok.buiStart(d, 4) >= 0) {
                buien++;
                long s = WeerKlok.buiStart(d, 4);
                helper.assertTrue(WeerKlok.bui(d, s + WeerKlok.BUI_DUUR / 2, 4) > 0.9f && WeerKlok.regenboog(d, s + WeerKlok.BUI_DUUR + 300, 4) > 0.5f,
                        "a shower, then a rainbow");
            }
            if (WeerKlok.mistDag(d)) {
                mist++;
            }
        }
        helper.assertTrue(buien > 10 && buien < 30 && mist > 5 && mist < 25, "showers " + buien + "/30, mist " + mist + "/30");
        helper.assertTrue(WeerKlok.gloed(12500, 4) > 0.5f && WeerKlok.gloed(12500, 3) == 0f, "the lampion glow is zone 4's evening");
        helper.succeed();
    }

    private static JsonObject taal(String t) {
        try (InputStream in = VadsrijkURitmeGameTests.class.getResourceAsStream("/assets/guhs/lang/" + t + ".json")) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}
