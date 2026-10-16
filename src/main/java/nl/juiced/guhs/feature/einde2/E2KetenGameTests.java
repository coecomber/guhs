package nl.juiced.guhs.feature.einde2;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.world.GegarandeerdPlacement;

/**
 * einde2 fix round 1 (playtest 1, item 8): the four stops of the Guhvatar story are a chain (Luchttempel around 0,0, every
 * next one in a ring around the one before), and two consecutive stops are ALWAYS 1500-5000 blocks apart, landing spot to
 * landing spot: (1) for any seed and any world, every ring the search can ever walk through ({@link GegarandeerdPlacement#ringenOoit},
 * the wider rings of alleen_nieuw cut off at max_ver), widened by both landing spots, stays inside the bounds; (2) for three
 * seeds of the real Guheinde generator the chain really lands, and its landing spots lie inside the bounds.
 */
public class E2KetenGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "einde2_keten";
    private static final List<String> KETEN = Einde2Ids.VERHAAL_STRUCTUREN;
    private static final int MIN = 1500, MAX = 5000;
    /** How far a landing spot can lie from its build's anchor: every tile lies within 136 blocks of it in x and in z. */
    private static final double SPEL = Math.hypot(136, 136);

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2KetenAfstanden(GameTestHelper helper) {
        var sets = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (int i = 1; i < KETEN.size(); i++) {
            String vorige = KETEN.get(i - 1), naam = KETEN.get(i);
            StructureSet set = sets.getValue(Guhs.id(naam + "_gegarandeerd"));
            helper.assertTrue(set != null && set.placement() instanceof GegarandeerdPlacement, naam + ": a guaranteed set");
            GegarandeerdPlacement g = (GegarandeerdPlacement) set.placement();
            helper.assertTrue(g.rond().isPresent() && g.rond().get().equals(Guhs.id(vorige + "_gegarandeerd")), naam + ": its ring lies around " + vorige);
            helper.assertTrue(!g.alleenNieuw() || g.maxVer() > 0, naam + ": alleen_nieuw with a ceiling");
            for (int[] ring : g.ringenOoit()) {
                helper.assertTrue(ring[0] - 2 * SPEL >= MIN && ring[1] + 2 * SPEL <= MAX, vorige + " -> " + naam + ": a ring of " + ring[0] + ".." + ring[1]);
            }
        }
        for (long seed : Einde2Test.SEEDS) {
            Einde2Test.Wereld w = Einde2Test.wereld(helper, seed);
            Vec3 vorig = null;
            for (String naam : KETEN) {
                ChunkPos chunk = Einde2Test.gegarandeerd(w, naam);
                helper.assertTrue(chunk != null, naam + " (seed " + seed + "): a spot");
                LeegteBouwStructure bouw = Plekken.leegteBouw(helper.getLevel().registryAccess(), naam);
                Plekken.Plek a = Plekken.plek(naam, "aankomst");
                helper.assertTrue(bouw != null && a != null, naam + ": a build with an aankomst");
                var o = bouw.oorsprong(chunk);
                var tot = a.tot() == null ? a.pos() : a.tot();
                Vec3 hier = new Vec3(o.getX() + (a.pos().getX() + tot.getX()) / 2.0, 0, o.getZ() + (a.pos().getZ() + tot.getZ()) / 2.0);
                if (vorig != null) {
                    double d = hier.distanceTo(vorig);
                    helper.assertTrue(d >= MIN && d <= MAX, naam + " (seed " + seed + "): " + (int) d + " blocks from the stop before");
                }
                vorig = hier;
            }
        }
        helper.succeed();
    }
}
