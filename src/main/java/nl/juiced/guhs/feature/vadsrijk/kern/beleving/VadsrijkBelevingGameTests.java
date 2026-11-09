package nl.juiced.guhs.feature.vadsrijk.kern.beleving;

import java.util.List;
import java.util.UUID;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.ProefKlodder;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Vadsrijk beleving: only the logic. The Vrolijke reeks (count, break, xp bonus, milestones, the rising note) and the damage
 * number classification (size, crit, element colour, Zwak!/Taai, DoT merge). Run: {@code runGameTestServer -Pgt=VadsrijkBeleving}.
 */
public class VadsrijkBelevingGameTests {
    private static final String BATCH = "vadsrijk_beleving";

    @GuhTest(template = "empty", batch = BATCH)
    public static void vrolijkeReeks(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        long t = 1000;
        helper.assertTrue(Reeks.tel(id, t) == 1 && Reeks.tel(id, t + 40) == 2 && Reeks.tel(id, t + 40 + Reeks.VENSTER) == 3,
                "cheer-ups within 5 s count up (also exactly at 5 s)");
        helper.assertTrue(Reeks.stand(id, t + 40 + Reeks.VENSTER) == 3 && Reeks.stand(id, t + 41 + 2 * Reeks.VENSTER) == 0, "the count, then broken");
        helper.assertTrue(Reeks.tel(id, t + 41 + 2 * Reeks.VENSTER) == 1, "after a break it starts at 1");
        for (int i = 0; i < 10; i++) {   // (the first one starts a new streak: more than 5 s after the last)
            Reeks.tel(id, t + 500);
        }
        helper.assertTrue(Reeks.stand(id, t + 500) == 10 && Reeks.xpFactor(10) == 1.10 && Reeks.xpFactor(9) == 1.0, "x10 gives +10 % xp, x9 nothing");
        helper.assertTrue(Reeks.xpFactor(25) == 1.15 && Reeks.xpFactor(50) == 1.20, "the higher milestones");
        helper.assertTrue(Reeks.mijlpaal(10) && Reeks.mijlpaal(25) && Reeks.mijlpaal(50) && Reeks.mijlpaal(75) && !Reeks.mijlpaal(11) && !Reeks.mijlpaal(60),
                "milestones 10/25/50/75");
        helper.assertTrue(!Reeks.verlopen(t + 500 + Reeks.VENSTER).containsKey(id), "not broken at exactly 5 s");   // (other tests run in parallel: only our id)
        helper.assertTrue(Reeks.verlopen(t + 501 + Reeks.VENSTER).get(id) == 10 && Reeks.stand(id, t + 501 + Reeks.VENSTER) == 0,
                "the break reports the count it reached");
        float vorige = 0;
        for (int n = 1; n <= 30; n++) {
            float toon = Reeks.toon(n);
            helper.assertTrue(toon >= vorige && toon <= 2.0f && toon >= 0.5f, "the plof note climbs (n=" + n + ": " + toon + ")");
            vorige = toon;
        }
        helper.assertTrue(Reeks.toon(2) > Reeks.toon(1) && Reeks.toon(8) > Reeks.toon(7), "every step is a higher note (scale)");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void schadegetallen(GameTestHelper helper) {
        Getalstijl.Stijl klein = Getalstijl.van(3, false, Element.GEEN, 1, false, false);
        Getalstijl.Stijl groot = Getalstijl.van(300, false, Element.GEEN, 1, false, false);
        helper.assertTrue(groot.schaal() > klein.schaal() && klein.schaal() >= 0.75f && groot.schaal() <= 2.4f, "the size grows with the damage");
        Getalstijl.Stijl krit = Getalstijl.van(50, true, Element.VUUR, 1, false, false);
        helper.assertTrue(krit.kleur() == Getalstijl.GOUD && krit.getal().startsWith("★") && krit.getal().endsWith("50") && krit.vet()
                && krit.schaal() > Getalstijl.van(50, false, Element.VUUR, 1, false, false).schaal(), "crit = gold, a star, bigger");
        helper.assertTrue(Getalstijl.van(10, false, Element.WATER, 1, false, false).kleur() == Element.WATER.rgb
                && Getalstijl.van(10, false, Element.GEEN, 1, false, false).kleur() == Getalstijl.WIT, "the element colour (white without one)");
        helper.assertTrue("zwak".equals(Getalstijl.van(10, false, Element.VUUR, 1.5, false, false).label())
                && "taai".equals(Getalstijl.van(10, false, Element.VUUR, 0.7, false, false).label())
                && "".equals(Getalstijl.van(10, false, Element.VUUR, 1.0, false, false).label()), "Zwak! at x1.5, Taai at x0.7");
        String tekst = Getalstijl.tekst(Getalstijl.van(10, false, Element.VUUR, 1.5, false, false)).getString();   // W-INT: nl or en
        helper.assertTrue(tekst.equals("10 " + net.minecraft.locale.Language.getInstance().getOrDefault("vadsrijk.kern.beleving.zwak", "Zwak!")),
                "the text: " + tekst);
        Getalstijl.Stijl dot = Getalstijl.van(400, false, Element.VUUR, 1.5, true, false);
        helper.assertTrue("".equals(dot.label()) && dot.schaal() <= 1.0f, "DoT ticks stay small and never say Zwak!");
        helper.assertTrue(Getalstijl.van(10, false, Element.VUUR, 1, false, true).kleur() == Getalstijl.SPELER_ROOD, "damage on a player is red");
        helper.assertTrue(Beleving.hitStop(true, true) == 4 && Beleving.hitStop(true, false) == 3 && Beleving.hitStop(false, true) == 2
                && Beleving.hitStop(false, false) == 0, "hit-stop 2-4 ticks on heavy hits and crits only");
        helper.assertTrue(Kistceremonie.noten(Zeldzaamheid.GEWOON) < Kistceremonie.noten(Zeldzaamheid.EPISCH)
                && Kistceremonie.noten(Zeldzaamheid.EPISCH) < Kistceremonie.noten(Zeldzaamheid.GUHDDELIJK), "the chest tone climbs further per rarity");
        helper.assertTrue(Kistceremonie.beste(List.of()) == Zeldzaamheid.GEWOON, "an empty chest counts as Gewoon");

        // DoT ticks on one target merge into one number (through the real pipeline)
        ServerPlayer p = GuhMockPlayer.of(helper);
        ProefKlodder k = KernRegistry.PROEFKLODDER.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(k != null, "a test klodder");
        net.minecraft.core.BlockPos bp = helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        k.snapTo(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5, 0, 0);
        helper.getLevel().addFreshEntity(k);
        for (int i = 0; i < 4; i++) {
            Schade.doe(new Schade.Klap(p, k, 0.2, Element.VUUR, Schade.Soort.WAAR, false, "status"));
        }
        Schade.doe(new Schade.Klap(p, k, 0.2, Element.VUUR, Schade.Soort.WAAR, false, "test"));
        List<Display.TextDisplay> getallen = helper.getLevel().getEntitiesOfClass(Display.TextDisplay.class, new AABB(k.blockPosition()).inflate(4),
                d -> d.entityTags().contains(Getallen.TAG));
        helper.assertTrue(getallen.size() == 2, "4 DoT ticks = 1 number, plus 1 normal hit (got " + getallen.size() + ")");
        getallen.forEach(net.minecraft.world.entity.Entity::discard);
        k.discard();
        helper.succeed();
    }
}
