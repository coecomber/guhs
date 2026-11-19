package nl.juiced.guhs.feature.guhwinter.verhaal;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.guhwinter.GuhwinterFase;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/** Guhwinter (verhaal slice): the chapters open by phase, steps only go forward, a finished chapter gives its deco block once. */
public class GuhwinterVerhaalGameTests {
    private static final String B = "guhwinter_verhaal";

    @GuhTest(template = "empty", batch = B)
    public static void stappenEnFasen(GameTestHelper helper) {
        // --- phase gating: Sint all event long, Kerst from 6 Dec, Oud & Nieuw from 26 Dec, nothing after 3 Jan
        WinterVerhaal.Hoofdstuk sint = WinterVerhaal.Hoofdstuk.SINT, kerst = WinterVerhaal.Hoofdstuk.KERST, oud = WinterVerhaal.Hoofdstuk.OUDJAAR;
        helper.assertTrue(sint.open(GuhwinterFase.VOOR) && sint.open(GuhwinterFase.SINT) && sint.open(GuhwinterFase.KERST) && sint.open(GuhwinterFase.OUDJAAR),
                "the Sint chapter is open all event long (late joiners too)");
        helper.assertTrue(!kerst.open(GuhwinterFase.SINT) && !kerst.open(GuhwinterFase.PAKJESAVOND) && kerst.open(GuhwinterFase.KERST)
                && kerst.open(GuhwinterFase.OUDJAAR), "Kerst from 6 December");
        helper.assertTrue(!oud.open(GuhwinterFase.KERST) && oud.open(GuhwinterFase.OUDJAAR), "Oud & Nieuw from 26 December");
        for (WinterVerhaal.Hoofdstuk h : WinterVerhaal.Hoofdstuk.values()) {
            helper.assertFalse(h.open(GuhwinterFase.AFGELOPEN), h + " is closed from 3 January");
        }
        // --- the prikbord cards
        helper.assertTrue(Voornemens.schoon("  §cMeer   OV\n ").equals("Meer OV"), "colour codes, runs of spaces and newlines go");
        helper.assertTrue(Voornemens.schoon("x".repeat(200)).length() == Voornemens.MAX, "a card is at most " + Voornemens.MAX + " characters");
        helper.assertTrue(Voornemens.schoon(" \n\t ").isEmpty(), "an empty card stays empty");
        // --- steps go forward one at a time; done = the reward, once
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        WinterVerhaal.SINT.wis(p);
        helper.assertTrue(WinterVerhaal.SINT.stap(p) == WinterVerhaal.S_INTOCHT, "a new player starts at the intocht");
        helper.assertTrue(WinterVerhaal.SINT.verder(p, WinterVerhaal.S_INTOCHT) && WinterVerhaal.SINT.stap(p) == WinterVerhaal.S_OZOVADS, "intocht -> Ozovads");
        helper.assertFalse(WinterVerhaal.SINT.verder(p, WinterVerhaal.S_INTOCHT), "a step is never done twice");
        helper.assertFalse(WinterVerhaal.SINT.verder(p, WinterVerhaal.S_PAKJES), "no skipping ahead");
        WinterVerhaal.SINT.zet(p, WinterVerhaal.Hoofdstuk.SINT.stappen);
        helper.assertTrue(WinterVerhaal.SINT.klaar(p), "the chapter is done");
        helper.assertTrue(GuhQuests.count(p, VerhaalBlokken.PAKJESSTAPEL_ITEM.get()) == 1, "done: one Pakjesstapel");
        WinterVerhaal.beloon(p, WinterVerhaal.Hoofdstuk.SINT);
        helper.assertTrue(GuhQuests.count(p, VerhaalBlokken.PAKJESSTAPEL_ITEM.get()) == 1, "the reward comes only once");
        WinterVerhaal.SINT.wis(p);
        helper.succeed();
    }

    /**
     * Playtest round 1: the tree cards hang in a grid on the east and west side (never two in one spot), the walk-to radius
     * of Ozovads' hiding spot reaches from the end of the carrot trail, there is a hiding-spot Ozovads, and the plein guhs
     * never plan a walk through water.
     */
    @GuhTest(template = "empty", batch = B)
    public static void playtestRonde1(GameTestHelper helper) {
        // --- the tree cards
        java.util.Set<String> plekken = new java.util.HashSet<>();
        for (int i = 0; i < 24; i++) {
            double[] k = nl.juiced.guhs.feature.guhwinter.tekenen.KerstboomTekeningen.plek(i);
            boolean oost = k[0] > 0;
            helper.assertTrue(Math.abs(Math.abs(k[0]) - nl.juiced.guhs.feature.guhwinter.tekenen.KerstboomTekeningen.KANT) < 1e-9,
                    "card " + i + " hangs on the east or west side");
            helper.assertTrue(oost == (i % 2 == 0) && k[3] == (oost ? 90.0 : -90.0), "card " + i + " alternates sides and faces outwards");
            helper.assertTrue(plekken.add(oost + ":" + Math.round(k[1] * 10) + ":" + Math.round(k[2] * 10)), "card " + i + " has a spot of its own");
        }
        double[] eerste = nl.juiced.guhs.feature.guhwinter.tekenen.KerstboomTekeningen.plek(0);
        helper.assertTrue(Math.abs(eerste[1] + 0.32 - 2.4) < 0.05, "the first row is centred about 2 blocks above the floor");
        // --- Ozovads: found from the end of the trail, and there is one in the hiding spot
        java.util.List<nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers.Anker> spoor = nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers.reeks("paard_spoor_");
        nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers.Anker verstop = nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers.zoek("paard_verstop");
        if (verstop != null && spoor.size() >= 2) {
            net.minecraft.world.phys.Vec3 eind = spoor.get(spoor.size() - 2).vec(), v = verstop.vec();
            helper.assertTrue(Math.hypot(eind.x - v.x, eind.z - v.z) < VerhaalStappen.VERSTOP_STRAAL, "the end of the trail is close enough to find him");
        }
        nl.juiced.guhs.feature.guhwinter.npc.NpcDefs.Def d = nl.juiced.guhs.feature.guhwinter.npc.NpcDefs.van("ozovads_verstop");
        helper.assertTrue(d != null && d.soort() == nl.juiced.guhs.feature.guhwinter.npc.NpcDefs.Soort.PAARD
                && d.in(nl.juiced.guhs.feature.guhwinter.npc.NpcFase.SINT) && !d.in(nl.juiced.guhs.feature.guhwinter.npc.NpcFase.KERST)
                && d.ankers().get(0).anker().equals("npc_ozovads_verstop"), "a hiding-spot Ozovads, Sint time only");
        // --- the plein guhs never walk into water
        net.minecraft.world.phys.Vec3 bij = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)));
        net.minecraft.world.entity.Entity e = nl.juiced.guhs.feature.guhwinter.npc.GuhwinterNpcs.zet(helper.getLevel(), "anthony", bij, 0);
        helper.assertTrue(e instanceof net.minecraft.world.entity.Mob m
                && m.getPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER) < 0, "Anthony never plans a walk through water");
        e.discard();
        helper.succeed();
    }
}
