package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * W-LEVEN (round W playtest fixes) on top of the U-LEVEN-STAD test town ({@link StadslevenGameTests#bouwStad}):
 * <ul>
 *   <li>{@code muizenFactor}: the pieppiepmuisje multiplier per town (0.3 everywhere, 1.0 in the Kandijkom): the caps, and 40
 *       planning passes stay under them (mice and blobs each);</li>
 *   <li>{@code huisdierAfstand}: a walker's pet trails about 2.2 blocks behind its guh (was 1.2) and never runs ahead of it;</li>
 *   <li>{@code smalleSteiger}: a 3-wide pier through the pond counts as narrow, the lawn does not, and with the player on the
 *       pier only a few town-life entities stand on it.</li>
 * </ul>
 * Run: {@code runGameTestServer -Pgt=WLeven}.
 */
public class WLevenGameTests {
    private static final Steden.Stad KANDIJ = new Steden.Stad("test_kandij", 2, Set.of(), true, 1.0);

    private static void draai(ServerLevel sl, BlockPos o, Steden.Stad stad, Vec3 waar, int passes) {
        for (int i = 0; i < passes; i++) {
            StadsLeven.plan(sl, List.of(new StadsLeven.Kijker(waar, stad, StadsLeven.Niveau.VOL)), List.of(waar));
            for (int t = 0; t < 10; t++) {
                StadsLeven.tickGroepen(sl);
            }
        }
    }

    @GuhTest(template = "empty", batch = "vadsrijk_wleven_muizen", timeoutTicks = 600)
    public static void muizenFactor(GameTestHelper helper) {
        // the multiplier per town
        helper.assertTrue(Steden.van("kandijkom").muizen() == 1.0, "Kandijkom keeps its mice");
        for (String id : new String[] {"knuffelkamp", "zwamzoet", "haven", "honingtak", "lampionhaven", "hoogvads", "saffraanpoort"}) {
            helper.assertTrue(Steden.van(id).muizen() == Steden.MUIZEN_STANDAARD, id + ": mice at 30%");
        }
        Steden.Stad kamp = Steden.van("knuffelkamp"), kandij = Steden.van("kandijkom");
        StadsLeven.Niveau vol = StadsLeven.Niveau.VOL;
        helper.assertTrue(Beestjes.blobMax(kamp, vol) == 3 && Beestjes.blobMax(kandij, vol) == 3, "the blobs stay at 3");
        helper.assertTrue(Beestjes.muisMax(kamp, vol) == 2, "Knuffelkamp: 2 mice (was ~6): " + Beestjes.muisMax(kamp, vol));
        helper.assertTrue(Beestjes.muisMax(kandij, vol) == 6, "Kandijkom: 6 mice as before: " + Beestjes.muisMax(kandij, vol));
        helper.assertTrue(Beestjes.muisMax(kamp, StadsLeven.Niveau.UIT) == 0 && Beestjes.blobMax(kamp, StadsLeven.Niveau.UIT) == 0, "UIT: none");

        ServerLevel sl = helper.getLevel();
        BlockPos o = StadslevenGameTests.bouwStad(helper);
        StadslevenGameTests.forceer(helper, o, true);
        StadslevenGameTests.wacht(helper, o, 0, () -> {
            StadslevenGameTests.begin(helper, o);
            Vec3 midden = Vec3.atBottomCenterOf(o.offset(14, 1, 14));
            try {
                for (Steden.Stad stad : new Steden.Stad[] {StadslevenGameTests.testStad(), KANDIJ}) {
                    StadsLeven.allesWeg();
                    Steden.testStad = stad;
                    draai(sl, o, stad, midden, 40);
                    int[] mb = StadsLeven.diertjes(stad);
                    helper.assertTrue(mb[0] <= Beestjes.muisMax(stad, vol), stad.id() + ": mice over the cap: " + mb[0]);
                    helper.assertTrue(mb[1] <= Beestjes.blobMax(stad, vol), stad.id() + ": blobs over the cap: " + mb[1]);
                    helper.assertTrue(mb[0] >= 1, stad.id() + ": still some mice: " + mb[0]);
                    if (stad == KANDIJ) {
                        helper.assertTrue(mb[0] >= 3, "Kandijkom-like town: lots of mice: " + mb[0]);
                    }
                }
            } finally {
                StadslevenGameTests.einde(helper, o);
            }
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = "vadsrijk_wleven_huisdier", timeoutTicks = 600)
    public static void huisdierAfstand(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = StadslevenGameTests.bouwStad(helper);
        StadslevenGameTests.forceer(helper, o, true);
        StadslevenGameTests.wacht(helper, o, 0, () -> {
            StadslevenGameTests.begin(helper, o);
            try {
                Straat.Wandelaar w = null;
                for (int i = 0; i < 20 && w == null; i++) {
                    w = Straat.Wandelaar.maak(sl, StadslevenGameTests.testStad(), Vec3.atBottomCenterOf(o.offset(14, 1, 14)), sl.getRandom());
                }
                helper.assertTrue(w != null, "a walker with a pet");
                double start = w.guh.position().distanceTo(w.dier.position());
                helper.assertTrue(start > 1.9, "the pet starts about 2 blocks from its guh: " + start);
                double max = 0, min = 99;
                int lopend = 0;
                for (int t = 0; t < 400; t++) {
                    w.tick(sl);
                    double d = Math.sqrt(Math.pow(w.guh.getX() - w.dier.getX(), 2) + Math.pow(w.guh.getZ() - w.dier.getZ(), 2));
                    max = Math.max(max, d);
                    if (w.loper.beweegt()) {
                        lopend++;
                        min = Math.min(min, d);
                    }
                }
                helper.assertTrue(lopend > 20, "the guh walked: " + lopend + " ticks");
                helper.assertTrue(max <= Straat.Wandelaar.AFSTAND + 0.3, "never further than the leash: " + max);
                helper.assertTrue(max >= 1.9, "about 2 blocks behind while walking: " + max);
                helper.assertTrue(min >= 0.8, "the pet never bumps into its guh while it walks: " + min);
                w.weg();
            } finally {
                StadslevenGameTests.einde(helper, o);
            }
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = "vadsrijk_wleven_steiger", timeoutTicks = 600)
    public static void smalleSteiger(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = StadslevenGameTests.bouwStad(helper);
        // a pond on the east half of the lawn (x 15..27), with a 3-wide plank pier through it (z 19..21)
        for (int x = 15; x < 28; x++) {
            for (int z = 10; z < 28; z++) {
                boolean pier = z >= 19 && z <= 21;
                sl.setBlock(o.offset(x, 0, z), pier ? Blocks.OAK_PLANKS.defaultBlockState() : Blocks.WATER.defaultBlockState(), 2);
                sl.setBlock(o.offset(x, -1, z), Blocks.WATER.defaultBlockState(), 2);
                for (int y = 1; y < 8; y++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        StadslevenGameTests.forceer(helper, o, true);
        StadslevenGameTests.wacht(helper, o, 0, () -> {
            StadslevenGameTests.begin(helper, o);
            try {
                helper.assertTrue(Plekken.smal(sl, o.offset(22, 1, 20)), "the middle of the pier is narrow");
                helper.assertTrue(!Plekken.smal(sl, o.offset(7, 1, 14)), "the lawn is not narrow");
                Vec3 eind = Vec3.atBottomCenterOf(o.offset(26, 1, 20));
                draai(sl, o, StadslevenGameTests.testStad(), eind, 40);
                AABB pier = new AABB(o.getX() + 15, o.getY(), o.getZ() + 19, o.getX() + 28, o.getY() + 4, o.getZ() + 22);
                int op = 0, totaal = 0;
                for (Groep g : StadsLeven.GROEPEN.values()) {
                    for (Entity e : g.leden) {
                        totaal++;
                        if (pier.contains(e.position())) {
                            op++;
                        }
                    }
                }
                helper.assertTrue(totaal > 4, "the town is still lively: " + totaal);
                helper.assertTrue(op <= 5, "only a few on the pier: " + op + " of " + totaal);
            } finally {
                StadslevenGameTests.einde(helper, o);
            }
            helper.succeed();
        });
    }
}
