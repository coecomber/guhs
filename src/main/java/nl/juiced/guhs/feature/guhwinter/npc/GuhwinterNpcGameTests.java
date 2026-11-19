package nl.juiced.guhs.feature.guhwinter.npc;

import java.time.LocalDateTime;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.gametest.GuhTest;

/** Guhwinter (npc slice): every guh of the Winterplein can be made, looks right, belongs to the right phases and has lines. */
public class GuhwinterNpcGameTests {
    private static final String B = "guhwinter_npc";

    @GuhTest(template = "empty", batch = B)
    public static void allemaalTeMaken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 bij = Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1)));
        // the phases by date (Europe/Amsterdam)
        helper.assertTrue(NpcFaseBron.opDatum(LocalDateTime.of(2026, 11, 24, 12, 0)) == NpcFase.SINT, "before the Sint leaves: SINT");
        helper.assertTrue(NpcFaseBron.opDatum(LocalDateTime.of(2026, 12, 5, 23, 59)) == NpcFase.SINT, "pakjesavond is still Sint");
        helper.assertTrue(NpcFaseBron.opDatum(LocalDateTime.of(2026, 12, 6, 0, 0)) == NpcFase.KERST, "6 Dec: KERST");
        helper.assertTrue(NpcFaseBron.opDatum(LocalDateTime.of(2026, 12, 26, 0, 0)) == NpcFase.OUDJAAR, "26 Dec: OUDJAAR");
        helper.assertTrue(NpcFaseBron.opDatum(LocalDateTime.of(2027, 1, 3, 0, 0)) == NpcFase.WEG, "3 Jan: gone");
        helper.assertTrue(!NpcDefs.van("wijnand").in(NpcFase.SINT) && NpcDefs.van("wijnand").in(NpcFase.KERST), "opa from KERST on");
        helper.assertTrue(NpcDefs.van("sint").in(NpcFase.SINT) && !NpcDefs.van("sint").in(NpcFase.KERST)
                && !NpcDefs.van("ozovads").in(NpcFase.KERST) && !NpcDefs.van("pakjesguh_0").in(NpcFase.KERST), "the Sint's company leaves on 6 Dec");
        for (String naam : GuhwinterNpcs.namen()) {
            helper.assertFalse(NpcDefs.van(naam).in(NpcFase.WEG), naam + " is gone at the end");
            Entity e = GuhwinterNpcs.zet(level, naam, bij, 0);
            helper.assertTrue(e != null && GuhwinterNpcs.get(level, naam) == e, naam + " spawns and is found by name");
            helper.assertTrue(naam.equals(GuhwinterNpcs.naamVan(e)) && GuhwinterNpcs.isNpc(e), naam + " knows its name");
            if (naam.startsWith("ozovads")) {
                helper.assertTrue(e instanceof Horse h && h.isInvulnerable() && h.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "Ozovads: a still horse");
            } else {
                helper.assertTrue(e instanceof WinterGuh, naam + " is a WinterGuh");
            }
            for (NpcFase f : new NpcFase[] {NpcFase.SINT, NpcFase.KERST, NpcFase.OUDJAAR}) {
                if (NpcDefs.van(naam).in(f)) {
                    int n = NpcPraat.aantal(naam, f);
                    helper.assertTrue(n >= 3 && n <= 6, naam + " has 3-6 lines in " + f + ", found " + n);
                }
            }
        }
        WinterGuh evi = (WinterGuh) GuhwinterNpcs.get(level, "evi"), liv = (WinterGuh) GuhwinterNpcs.get(level, "liv");
        WinterGuh niels = (WinterGuh) GuhwinterNpcs.get(level, "niels");
        helper.assertTrue(evi.getScale() > 1.05f && liv.getScale() < 0.7f, "Evi is tall, Liv is small");
        helper.assertTrue(niels.getClothes(GuhClothes.Slot.EYES) == GuhClothes.BIEB_LEESBRIL && niels.getHaarkleur() >= 0, "Niels: glasses and dyed hair");
        // dressed for the phase
        evi.kleed(NpcDefs.van("evi"), NpcFase.KERST);
        helper.assertTrue(evi.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.SANTA_HAT && evi.getClothes(GuhClothes.Slot.NECK) == GuhClothes.JAPAN_STRIKJE,
                "Evi at Christmas: santa hat, still her bow");
        // a scene walk
        Vec3 doel = bij.add(3, 0, 0);
        helper.assertTrue(GuhwinterNpcs.walkTo(level, "joost", doel, 1.0) && GuhwinterNpcs.isWalking(level, "joost"), "Joost walks");
        helper.succeedWhen(() -> {
            helper.assertFalse(GuhwinterNpcs.isWalking(level, "joost"), "Joost arrived");
            GuhwinterNpcs.wisAlles(level);
        });
    }
}
