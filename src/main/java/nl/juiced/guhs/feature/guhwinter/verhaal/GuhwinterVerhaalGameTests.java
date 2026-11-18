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
}
