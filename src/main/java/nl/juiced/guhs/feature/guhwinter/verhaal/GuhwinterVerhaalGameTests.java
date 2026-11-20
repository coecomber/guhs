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
     * Playtest round 1: the tree cards hang in a grid on the open sides of the tree (1.10.11: west first, then north and
     * south; never east, against the house wall; never two in one spot), the walk-to radius
     * of Ozovads' hiding spot reaches from the end of the carrot trail, there is a hiding-spot Ozovads, and the plein guhs
     * never plan a walk through water.
     */
    @GuhTest(template = "empty", batch = B)
    public static void playtestRonde1(GameTestHelper helper) {
        // --- the tree cards
        double kantAf = nl.juiced.guhs.feature.guhwinter.tekenen.KerstboomTekeningen.KANT;
        int max = nl.juiced.guhs.feature.guhwinter.tekenen.KerstboomTekeningen.KAARTJES_MAX;
        helper.assertTrue(max >= 20, "room for 20 cards");
        java.util.Set<String> plekken = new java.util.HashSet<>();
        for (int i = 0; i < 30; i++) {
            double[] k = nl.juiced.guhs.feature.guhwinter.tekenen.KerstboomTekeningen.plek(i);
            double midden = k[1] + 0.32;
            boolean west = Math.abs(k[0] + kantAf) < 1e-9 && Math.abs(k[2]) <= 1.6 + 1e-9 && k[3] == -90.0;
            boolean noord = Math.abs(k[2] + kantAf) < 1e-9 && Math.abs(k[0]) <= 1.6 + 1e-9 && k[3] == 0.0;
            boolean zuid = Math.abs(k[2] - kantAf) < 1e-9 && Math.abs(k[0]) <= 1.6 + 1e-9 && k[3] == 180.0;
            helper.assertTrue(west || noord || zuid, "card " + i + " hangs on the west, north or south side, facing outwards: "
                    + java.util.Arrays.toString(k));
            helper.assertTrue(k[0] < kantAf - 0.5, "card " + i + " is not on the east side (the house wall)");
            helper.assertTrue(i >= 10 || west, "cards 1-10 hang on the west side (the room side)");
            helper.assertTrue(midden > 2.0 && midden < 3.3, "card " + i + " hangs 2-3 blocks above the floor");
            helper.assertTrue(i >= max || i < 10 || midden < 2.5, "of the first 20, north and south have one row");
            helper.assertTrue(plekken.add(Math.round(k[0] * 10) + ":" + Math.round(k[1] * 10) + ":" + Math.round(k[2] * 10)),
                    "card " + i + " has a spot of its own");
        }
        double[] eerste = nl.juiced.guhs.feature.guhwinter.tekenen.KerstboomTekeningen.plek(0);
        helper.assertTrue(eerste[0] < 0 && eerste[2] == 0 && eerste[3] == -90.0 && Math.abs(eerste[1] + 0.32 - 2.4) < 0.05,
                "card 1 hangs in the middle of the west side, about 2.4 above the floor (in front of kerstboom_voor)");
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
