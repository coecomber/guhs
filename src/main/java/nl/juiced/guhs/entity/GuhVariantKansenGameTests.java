package nl.juiced.guhs.entity;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * The odds of the wild guh variants (1.3.1: every variant that is not the normal guh a little rarer): the table of
 * {@link GuhVariant#roll}, the note guh (Brococolief, a flat 1 in N before the table) and the three biome guhs.
 */
public class GuhVariantKansenGameTests {
    private static final String EMPTY = "empty";

    @GuhTest(template = EMPTY)
    public static void variantenZijnIetsZeldzamer(GameTestHelper helper) {
        // the table: out of 1000 wild Guhmension guhs
        Map<GuhVariant, Integer> gewicht = new EnumMap<>(GuhVariant.class);
        gewicht.put(GuhVariant.MINT, 17);          // was 25
        gewicht.put(GuhVariant.CHOCO, 17);         // was 25
        gewicht.put(GuhVariant.SNOW, 13);          // was 20
        gewicht.put(GuhVariant.BRONTOSAURUS, 6);   // under 1%: unchanged
        gewicht.put(GuhVariant.GOLDEN, 2);
        gewicht.put(GuhVariant.RAINBOW, 3);
        gewicht.put(GuhVariant.GHOST, 4);
        gewicht.put(GuhVariant.TECKEL, 5);
        int som = 0;
        for (GuhVariant v : GuhVariant.values()) {
            helper.assertTrue(v.weight == gewicht.getOrDefault(v, 0), "the weight of " + v + ": " + v.weight);
            som += v.weight;
        }
        helper.assertTrue(GuhVariant.ROLL_OUT_OF == 1000 && som == 72, "72 of 1000 wild guhs are a variant (was 90): " + som);
        // the roll follows the table, and a ghost is a normal guh by day
        RandomSource random = RandomSource.create(131);
        Map<GuhVariant, Integer> gezien = new EnumMap<>(GuhVariant.class);
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            gezien.merge(GuhVariant.roll(random), 1, Integer::sum);
        }
        helper.assertTrue(Math.abs(gezien.get(GuhVariant.NORMAL) - n * 0.928) < n * 0.005, "about 92.8% normal: " + gezien.get(GuhVariant.NORMAL));
        helper.assertTrue(Math.abs(gezien.get(GuhVariant.MINT) - n * 0.017) < n * 0.003 && Math.abs(gezien.get(GuhVariant.SNOW) - n * 0.013) < n * 0.003,
                "mint about 1.7%, snow about 1.3%: " + gezien);
        for (int i = 0; i < 2000; i++) {
            helper.assertTrue(GuhEntity.wildeVariant(random, true) != GuhVariant.GHOST, "no ghosts by day");
        }
        // the note guh: a flat 1 in 500 (was 200), rolled before the table
        helper.assertTrue(GuhEntity.SECRET_NOTE_CHANCE == 500 && GuhVariant.BROCOCOLIEF.weight == 0, "Brococolief: 1 in 500, never from the table");
        // the biome guhs: a third less often
        helper.assertTrue(nl.juiced.guhs.feature.guhpolder.Pinguh.KANS == 0.33f, "Pinguh: a third of the Guhpolder guhs (was half)");
        helper.assertTrue(nl.juiced.guhs.feature.kaasmoeras.KaasmoerasEvents.MOERASGUH_CHANCE == 0.27f, "Kaasmoerasguh: 27% (was 40%)");
        helper.assertTrue(nl.juiced.guhs.feature.knuffeldal.KnuffeldalEvents.PLUISGUH_CHANCE == 0.23f, "Pluisguh: 23% (was 35%)");
        helper.succeed();
    }
}
