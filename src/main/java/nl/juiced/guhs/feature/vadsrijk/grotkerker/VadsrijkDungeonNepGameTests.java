package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonDeurData.Deur;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.party.DungeonRijBrug;
import nl.juiced.guhs.feature.vadsrijk.party.Wachtrijen;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepConfig;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepRol;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpeler;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpelers;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepVulling;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * W-INT (W-PARTY x W-GROT): a dungeon queue with one real player is filled by nep-spelers after {@link NepVulling#VUL_TICKS}
 * (not before), the ready check runs for the 4, and after the player's "yes" the group goes in: the player is the dungeon's group
 * and the three fillers (tank, heler, schade) stand inside as his companions; the queue is empty.
 * Run: {@code runGameTestServer -Pgt=VadsrijkDungeon}.
 */
public class VadsrijkDungeonNepGameTests {
    @GuhTest(template = "empty", batch = "vadsrijk_dungeon_nep", timeoutTicks = 200)
    public static void nepVultRijEnStart(GameTestHelper helper) {
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
                helper.assertTrue(Wachtrijen.DUNGEON instanceof DungeonRijBrug, "the party side is coupled to w-grot's queue");
                MinecraftServer s = helper.getLevel().getServer();
                helper.assertTrue(DungeonRij.inRij(p, k.id()), "into the queue");
                long t0 = 1_000_000L;
                helper.assertTrue(NepVulling.tick(s, t0, false) == 0, "no fill right away");
                helper.assertTrue(NepVulling.tick(s, t0 + NepVulling.VUL_TICKS - 20, false) == 0, "no fill before the timeout");
                helper.assertTrue(DungeonRij.rijGrootte(k.id()) == 1, "still 1 in the queue");
                int erbij = NepVulling.tick(s, t0 + NepVulling.VUL_TICKS, false);
                helper.assertTrue(erbij == 3, "3 nep-spelers fill the queue after the timeout: " + erbij);
                DungeonRij.Check c = DungeonRij.CHECKS.get(k.id());
                helper.assertTrue(c != null && c.leden.size() == 4, "a ready check for 1 player + 3 nep-spelers");
                DungeonRij.antwoord(p, true);
                DungeonRij.tickRij(s, c.start + 20);   // the companions answer "yes"
                DungeonRij.Instantie i = DungeonRij.instantie(k.id());
                helper.assertTrue(i != null && i.leden.contains(p.getUUID()), "the group went in; the dungeon is the player's group");
                helper.assertTrue(DungeonRij.CHECKS.isEmpty() && DungeonRij.rijGrootte(k.id()) == 0, "check done, queue empty");
                helper.assertTrue(k.bevat(p.getX(), p.getY(), p.getZ()), "the player is in the dungeon");
                List<NepSpeler> binnen = NepSpelers.alle().stream().filter(n -> k.id().equals(n.dungeon)).toList();
                helper.assertTrue(binnen.size() == 3, "3 nep-spelers came along: " + binnen.size());
                Set<NepRol> rollen = EnumSet.noneOf(NepRol.class);
                for (NepSpeler n : binnen) {
                    rollen.add(n.rol());
                    helper.assertTrue(p.getUUID().equals(n.eigenaar), "companion of the player");
                    helper.assertTrue(n.distanceTo(p) < 4, "next to the player");
                }
                for (NepRol r : NepRol.values()) {   // the player's own class counts as a filled role
                    if (r.klasse.equals(nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).klasse)) {
                        rollen.add(r);
                    }
                }
                helper.assertTrue(rollen.contains(NepRol.TANK) && rollen.contains(NepRol.HELER), "the missing roles first: " + rollen);
                DungeonRij.uitzetten(helper.getLevel(), Dungeons.van(k.id()));
                helper.assertTrue(NepSpelers.alle().stream().filter(n -> k.id().equals(n.dungeon) && n.isAlive()).count() == 3,
                        "w-grot's clean-up leaves the nep-spelers alone");
                helper.succeed();
            } finally {
                NepSpelers.wisAlles();
                NepVulling.wisAlles();
                NepConfig.TEST = null;
                VadsrijkDungeonGameTests.opruimen(helper, k, p);
            }
        });
    }
}
