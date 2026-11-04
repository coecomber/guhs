package nl.juiced.guhs.feature.vadsrijk.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.wereld.PortaalBlok;
import nl.juiced.guhs.feature.vadsrijk.wereld.Portalen;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldRegistry;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Fix round 2, g-werver: the temple portal waits for the talk with Werver Wimpel, and the two mount return quests only appear
 * once you own a mount.
 */
public class VadsrijkWerverGameTests {
    private static final String B = "vadsrijk_werver";

    @GuhTest(template = "empty", batch = B)
    public static void portaalWachtOpWerver(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        var d = Spelers.data(p);
        d.gezien.remove(Werver.GESPROKEN);
        d.gezien.remove(Portalen.ONTWAAKT);
        d.gezien.remove(Portalen.BINNEN_GEWEEST);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockState slaapt = WereldRegistry.PORTAAL.get().defaultBlockState().setValue(PortaalBlok.AXIS, Direction.Axis.X).setValue(PortaalBlok.WAKKER, false);
        helper.getLevel().setBlock(pos, slaapt, 2);
        // before the talk: the sleeping portal does not wake
        helper.assertFalse(Werver.magDoor(p, helper.getLevel(), pos, false), "sleeping portal, no talk yet: closed");
        Portalen.aanraking(p, pos, slaapt);
        helper.assertFalse(helper.getLevel().getBlockState(pos).getValue(PortaalBlok.WAKKER), "the portal still sleeps");
        helper.assertFalse(d.gezien.contains(Portalen.ONTWAAKT), "not woken by this player");
        // a self-built (awake, outside a temple) portal never needs him
        helper.assertTrue(Werver.magDoor(p, helper.getLevel(), pos, true), "a self-built portal is always open");
        // the talk: Wimpel's dialogue, then the after-reading action marks it
        RijkGuh w = Werver.zet(helper.getLevel(), new Werver.Plek(helper.absolutePos(new BlockPos(3, 2, 3)), 0, pos));
        helper.assertTrue(w != null && Werver.ID.equals(w.npcId()), "Wimpel spawns outside the Vadsrijk");
        QuestMotor.klik(p, w);
        helper.assertFalse(Werver.gesproken(p), "only after reading");
        QuestMotor.afronden(p);
        helper.assertTrue(Werver.gesproken(p), "talked to Wimpel");
        helper.assertTrue(Werver.magDoor(p, helper.getLevel(), pos, false), "now the sleeping portal may wake");
        // Wimpel's spot: 4 in front of the portal on the temple's side, 3 to the side, looking away from the portal
        Werver.Plek pl = Werver.plek(new BlockPos(0, 64, 0), Direction.Axis.X, new BlockPos(0, 64, 20));
        helper.assertTrue(pl.stand().equals(new BlockPos(3, 64, 4)) && Math.abs(pl.yaw()) < 0.01, "spot in front (south): " + pl);
        w.discard();
        d.gezien.remove(Werver.GESPROKEN);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void rijdierQuestsNaEenRijdier(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        Rijdieren.reset(p);
        Spelers.zetLevel(p, 25);
        Quest hert = Quests.van(RijdierQuests.HERT), wolf = Quests.van(RijdierQuests.WOLF), schaap = Quests.van(RijdierQuests.SCHAAP);
        helper.assertTrue(hert != null && wolf != null && schaap != null, "the three return quests exist");
        for (Quest q : new Quest[] {hert, wolf, schaap}) {
            for (String na : q.na()) {
                if (!na.equals(RijdierQuests.POORT)) {
                    Questlog.neemAan(p, Quests.van(na));
                    Questlog.voltooi(p, Quests.van(na));
                }
            }
        }
        Rijdieren.reset(p);   // (a finished story quest may have handed out a mount)
        helper.assertFalse(Questlog.beschikbaar(p, hert) || Questlog.beschikbaar(p, wolf) || Questlog.beschikbaar(p, schaap),
                "no mount: no return quest is there");
        helper.assertTrue(RijdierQuests.teaser(p, schaap.gever()) == schaap && RijdierQuests.teaser(p, hert.gever()) == hert
                && RijdierQuests.teaser(p, wolf.gever()) == wolf, "but their givers tease them");
        helper.assertTrue(RijdierQuests.trackerQuest(p) == null, "and nothing in the tracker");
        Rijdieren.geef(p, "wolkenschaapje");
        helper.assertTrue(Questlog.beschikbaar(p, hert) && Questlog.beschikbaar(p, wolf) && Questlog.beschikbaar(p, schaap),
                "owning a mount: all three appear");
        helper.assertTrue(RijdierQuests.teaser(p, schaap.gever()) == null, "no teaser any more");
        Quest t = RijdierQuests.trackerQuest(p);
        helper.assertTrue(t != null, "one is in the tracker");
        helper.assertTrue(NpcVolg.bouw(p).regels().stream().anyMatch(r -> r.quest().equals(t.id()) && r.doel().isPresent()),
                "tracked with a beam target (the giver)");
        // the reward action gives the mount
        QuestMotor.doeActie(p, hert.actieKlaar(), null);
        QuestMotor.doeActie(p, wolf.actieKlaar(), null);
        helper.assertTrue(Rijdieren.heeft(p, RijdierSoort.KNUFFELHERT) && Rijdieren.heeft(p, RijdierSoort.GUHWOLF), "Knuffelhert + Guhwolf given");
        Rijdieren.reset(p);
        Questlog.wisAlles(p);
        helper.succeed();
    }
}
