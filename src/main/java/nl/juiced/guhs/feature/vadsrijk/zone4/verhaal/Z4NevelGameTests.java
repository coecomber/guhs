package nl.juiced.guhs.feature.vadsrijk.zone4.verhaal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcMarkers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcVloer;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.vadsrijk.npc.TegelVloer;
import nl.juiced.guhs.feature.vadsrijk.zone4.wereld.Z4WereldPlekken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.21, Luc (live): "De Grauwe Mistbank", step "Praat met Verdwaalde Nevel bij de grauwe poort" never counted; she saw the
 * white "?" (NpcMarkers.PRATEN: a talk step of an active quest waits here) but could not talk to him. Nevel did not stand at the
 * gate at all: the T-INT alias put him on the boat landing (mysterie_strand), one block of the 45-degree stone staircase above the
 * water, 26 blocks from and 14 below the gate, in the thickest fog. Now he stands on the flat moss right in front of the gate.
 * Run: {@code runGameTestServer -Pgt=Z4NevelGameTests}.
 */
public class Z4NevelGameTests {
    private static final String B = "vadsrijk_z4_nevel";

    /** Standable cells beside this one at the same height (a landing to stand next to him, not one step of a staircase). */
    private static int naast(NpcVloer.Wereld w, BlockPos s) {
        int n = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (NpcVloer.staan(w, s.relative(d))) {
                n++;
            }
        }
        return n;
    }

    /** On the map tiles: Nevel stands by the grey gate, on a free floor with room around him (the old landing step had none). */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 600)
    public static void nevelStaatBijDePoort(GameTestHelper helper) {
        BlockPos plek = Npcs.van("z4_nevel").plek();
        BlockPos poort = Z4VerhaalPlekken.poi("z4_mysterie_poort");
        double dx = plek.getX() - poort.getX(), dz = plek.getZ() - poort.getZ();
        helper.assertTrue(Math.sqrt(dx * dx + dz * dz) <= Z4VerhaalPlekken.POI_STRAAL && Math.abs(plek.getY() - poort.getY()) <= 3,
                "Nevel stands by the grey gate (" + plek.toShortString() + ", gate " + poort.toShortString() + ")");
        TegelVloer w = new TegelVloer(helper.getLevel());
        helper.assertTrue(w.heeftTegel(plek), "the map tile under Nevel's spot is there");
        BlockPos s = NpcVloer.staanPlek(w, plek);
        helper.assertTrue(NpcVloer.staan(w, s) && !w.afgesloten(s, 20000), "Nevel's spot is a free floor under the open sky: " + s.toShortString()
                + " (feet " + w.naam(s) + ", below " + w.naam(s.below()) + ")");
        helper.assertTrue(naast(w, s) == 4, "a flat landing: you can stand right beside him on all four sides (" + naast(w, s) + "/4)");
        // the old spot (the boat landing): a single step of the staircase above the water
        BlockPos strand = Z4WereldPlekken.plek("mysterie_strand");
        if (strand != null && w.heeftTegel(strand)) {
            BlockPos o = NpcVloer.staanPlek(w, strand);
            helper.assertTrue(naast(w, o) < 4 && Math.abs(o.getY() - poort.getY()) > 10, "self-check: the old landing step " + o.toShortString()
                    + " is a staircase step far below the gate (" + naast(w, o) + "/4 beside it)");
        }
        helper.succeed();
    }

    /** Luc's exact state [1,1,0,0]: the white "?" on Nevel, one click on him counts the step, then only Zachtje waits. */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void praatMetNevelTelt(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        Z4Haken.vergeet(p);
        Quest q = Quests.van(Z4Haken.HQ04);
        Questlog.neemAan(p, q);
        for (int i = 0; i < 2; i++) {
            Quest.Doel d = q.doelen().get(i);
            Questlog.tel(p, d.type(), x -> x == d, d.n());
        }
        helper.assertTrue(Z4Haken.doelKlaar(p, Z4Haken.HQ04, 1) && !Z4Haken.doelKlaar(p, Z4Haken.HQ04, 2), "Luc's state: gate + mist flash done");
        helper.assertTrue(NpcMarkers.marker(p, "z4_nevel") == NpcMarkers.PRATEN, "the white ? on Nevel = talk to me for this step");
        RijkGuh nevel = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(1, 1, 1));
        nevel.stelIn(Npcs.van("z4_nevel"));
        QuestMotor.klik(p, nevel);
        helper.assertTrue(Z4Haken.doelKlaar(p, Z4Haken.HQ04, 2), "one click on Nevel counts the step");
        helper.assertTrue(NpcMarkers.marker(p, "z4_nevel") == NpcMarkers.GEEN, "Nevel's ? is gone");
        helper.assertTrue(NpcMarkers.marker(p, "z4_opvang") == NpcMarkers.PRATEN, "next: Opvangmoeder Zachtje");
        QuestMotor.vergeet(p);
        nevel.discard();
        Z4Haken.vergeet(p);
        Questlog.wisAlles(p);
        helper.succeed();
    }

    /**
     * 1.10.23 (Luc, live): the real right-click on Nevel, through the interaction event. GuheindeEvents answered every MAGER-variant
     * guh with "heb je een kaasknabbel?" and cancelled the click, so the quest motor never heard it (the test above calls the motor
     * directly and missed that). Nevel and the other grey refugees of zones 4-6 are MAGER guhs.
     */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 200)
    public static void echteKlikOpMagereNevel(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        Z4Haken.vergeet(p);
        Quest q = Quests.van(Z4Haken.HQ04);
        Questlog.neemAan(p, q);
        for (int i = 0; i < 2; i++) {
            Quest.Doel d = q.doelen().get(i);
            Questlog.tel(p, d.type(), x -> x == d, d.n());
        }
        RijkGuh nevel = helper.spawn(NpcRegistry.GUH.get(), new BlockPos(1, 1, 1));
        nevel.stelIn(Npcs.van("z4_nevel"));
        helper.assertTrue(nevel.getVariant() == nl.juiced.guhs.entity.GuhVariant.MAGER, "self-check: Nevel is a MAGER guh");
        p.interactOn(nevel, InteractionHand.MAIN_HAND, nevel.getBoundingBox().getCenter().subtract(nevel.position()));
        helper.assertTrue(Z4Haken.doelKlaar(p, Z4Haken.HQ04, 2), "a real right-click on Nevel counts the step (not the Knabbelkelder's hunger line)");
        QuestMotor.vergeet(p);
        nevel.discard();
        Z4Haken.vergeet(p);
        Questlog.wisAlles(p);
        helper.succeed();
    }

    /** 1.10.23 (Luc): every flessenpost lies in the open with a free floor within find range (fles_5 lay inside the hollow Vuurtoreneiland). */
    @GuhTest(template = "empty", batch = B, timeoutTicks = 600)
    public static void flessenpostIsTeVinden(GameTestHelper helper) {
        TegelVloer w = new TegelVloer(helper.getLevel());
        for (var e : Z4VerhaalPlekken.FLESSENPOST.entrySet()) {
            BlockPos b = e.getValue();
            if (!w.heeftTegel(b)) {
                continue;
            }
            // (the old fles_5 had 2 such cells: two steps of the cliff face beside the hollow island; a beach has dozens)
            int n = 0;
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = -Z4VerhaalPlekken.VIND_HOOGTE; dy <= Z4VerhaalPlekken.VIND_HOOGTE; dy++) {
                        BlockPos s = b.offset(dx, dy, dz);
                        if (NpcVloer.staan(w, s) && !w.afgesloten(s, 20000)) {
                            n++;
                        }
                    }
                }
            }
            helper.assertTrue(n >= 6, e.getKey() + " " + b.toShortString() + ": free floor within find range (" + n + " cells, want 6+)");
        }
        helper.succeed();
    }
}
