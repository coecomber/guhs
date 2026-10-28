package nl.juiced.guhs.feature.droomblokken;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.world.GuhTime;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Game tests of the dream blocks (1.8.0, batch "droomblokken"): the maanbloemetje opens at night and closes by day, the
 * slaapliedjesgras counts as dirt (flowers grow on it), and falling into the droomzee calls the dream's handler (no collision).
 */
public class DroomblokkenGameTests {
    private static final String BATCH = "droomblokken", KAMER = "vadskracht_test_kamer";

    /** Night: the moon flower opens (and glows); day: a closed bud again. It grows on slaapliedjesgras, which is dirt. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void maanbloemetjeOpentAlleenSNachts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long tijd = GuhTime.dayTime(level);
        BlockPos gras = new BlockPos(2, 1, 2), bloem = gras.above();
        helper.setBlock(gras, DroomblokkenFeature.SLAAPLIEDJESGRAS.get());
        helper.assertTrue(helper.getBlockState(gras).is(BlockTags.DIRT), "slaapliedjesgras is dirt (flowers and saplings grow on it)");
        helper.setBlock(bloem, DroomblokkenFeature.MAANBLOEMETJE.get());
        try {
            GuhTime.setDayTime(level, 18000);
            BlockState s = helper.getBlockState(bloem);
            s.randomTick(level, helper.absolutePos(bloem), level.getRandom());
            helper.assertTrue(helper.getBlockState(bloem).getValue(DroomBlokken.Maanbloemetje.OPEN), "open at midnight");
            helper.assertTrue(helper.getBlockState(bloem).getLightEmission() > 0, "and it glows");
            GuhTime.setDayTime(level, 6000);
            helper.getBlockState(bloem).randomTick(level, helper.absolutePos(bloem), level.getRandom());
            helper.assertFalse(helper.getBlockState(bloem).getValue(DroomBlokken.Maanbloemetje.OPEN), "a closed bud at noon");
            helper.assertTrue(helper.getBlockState(bloem).canSurvive(level, helper.absolutePos(bloem)), "it may grow on slaapliedjesgras");
        } finally {
            GuhTime.setDayTime(level, tijd);
        }
        helper.succeed();
    }

    /** Something falling into the droomzee sinks into it (no collision) and the dream's handler hears of it. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void droomzeeRoeptDeDroomAan(GameTestHelper helper) {
        AtomicInteger gehoord = new AtomicInteger();
        DroomBlokken.Droomzee.bijVallen((entity, pos) -> gehoord.incrementAndGet());
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                for (int y = 1; y <= 2; y++) {
                    helper.setBlock(new BlockPos(x, y, z), DroomblokkenFeature.DROOMZEE.get());
                }
            }
        }
        helper.spawn(EntityType.PIG, new BlockPos(2, 4, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(gehoord.get() > 0, "the handler was called");
            DroomBlokken.Droomzee.bijVallen(null);
        });
    }
}
