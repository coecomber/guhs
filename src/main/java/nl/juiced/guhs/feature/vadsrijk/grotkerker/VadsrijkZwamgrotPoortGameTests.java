package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData.Deur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepConfig;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpeler;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpelers;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepVulling;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoortConfig;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * X-ZONE1: the Zwamgrot (zone 1's dungeon) with ONLY zone 1 open (gate 1, strict: no creative bypass): the queue is filled by
 * nep-spelers and the group goes in; after the clear the cooldown refuses a new start; solo works. Run:
 * {@code runGameTestServer -Pgt=VadsrijkZwamgrotPoort}.
 */
public class VadsrijkZwamgrotPoortGameTests {
    @GuhTest(template = "empty", batch = "vadsrijk_zwamgrot_poort", timeoutTicks = 200)
    public static void zwamgrotMetAlleenZone1(GameTestHelper helper) {
        ZonePoortConfig.TEST = 1;
        ZonePoort.TEST_STRENG = true;
        Kerker k = VadsrijkDungeonGameTests.opzet(helper);
        Deur d = DungeonDeur.hoofd(k.id());
        ServerPlayer p = VadsrijkDungeonGameTests.speler(helper, d);
        NepConfig.TEST = true;
        NepSpelers.wisAlles();
        NepVulling.wisAlles();
        NepSpelers.zetStand(p.getUUID(), true);
        DungeonRij.zetGeenNpcVulling(p, false);
        helper.runAfterDelay(3, () -> {
            try {
                MinecraftServer s = helper.getLevel().getServer();
                helper.assertTrue(k.zone() == 1 && ZonePoort.dungeonDicht(p, k.id()) == null, "the Zwamgrot (zone 1) is open with gate 1");
                helper.assertTrue(DungeonRij.weiger(p, k.id(), true) == null, "no refusal for the Zwamgrot");
                // queue + nep-spelers fill + ready check + in
                helper.assertTrue(DungeonRij.inRij(p, k.id()), "into the queue");
                long t0 = 2_000_000L;
                NepVulling.tick(s, t0, false);
                int erbij = NepVulling.tick(s, t0 + NepVulling.VUL_TICKS, false);
                helper.assertTrue(erbij == 3, "3 nep-spelers fill the Zwamgrot queue: " + erbij);
                DungeonRij.Check c = DungeonRij.CHECKS.get(k.id());
                helper.assertTrue(c != null && c.leden.size() == 4, "ready check for 4");
                DungeonRij.antwoord(p, true);
                DungeonRij.tickRij(s, c.start + 20);
                DungeonRij.Instantie i = DungeonRij.instantie(k.id());
                helper.assertTrue(i != null && i.leden.contains(p.getUUID()) && k.bevat(p.getX(), p.getY(), p.getZ()), "the group went in");
                List<NepSpeler> binnen = NepSpelers.alle().stream().filter(n -> k.id().equals(n.dungeon)).toList();
                helper.assertTrue(binnen.size() == 3, "3 nep-spelers came along: " + binnen.size());
                // out; the cooldown after a clear refuses a new start
                DungeonRij.uitzetten(helper.getLevel(), Dungeons.van(k.id()));
                DungeonRij.wisAlles();
                DungeonRij.zetWachttijd(p, k.id(), 60_000);
                helper.assertTrue(DungeonRij.weiger(p, k.id(), true) != null, "cooldown: no new start");
                DungeonRij.zetWachttijd(p, k.id(), 0);
                helper.assertTrue(DungeonRij.solo(p, k.id(), false), "after the cooldown: solo start works with gate 1");
                helper.succeed();
            } finally {
                NepSpelers.wisAlles();
                NepVulling.wisAlles();
                NepConfig.TEST = null;
                ZonePoortConfig.TEST = null;
                ZonePoort.TEST_STRENG = false;
                VadsrijkDungeonGameTests.opruimen(helper, k, p);
            }
        });
    }
}
