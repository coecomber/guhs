package nl.juiced.guhs.feature.erfenis;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.erfenis.vorm.DecoWezens;
import nl.juiced.guhs.feature.slaapzorg.SlaapBlokken;
import nl.juiced.guhs.feature.slaapzorg.SlaapzorgFeature;
import nl.juiced.guhs.feature.slaapzorg.Slapen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the Erfenis Muziekdoosje (1.10.10, batch "erfenis_muziek"): a box with the old stamped state (speelt=1, no
 * tick) is not stuck; the dance tune starts, a second start is refused while it plays, a wild guh and a show guh without AI
 * dance (and keep dancing past the first step), nobody is sung asleep, and after the 30 seconds the box is quiet and
 * everyone stops dancing.
 */
public class ErfenisMuziekGameTests {
    private static final String EMPTY = "empty", BATCH = "erfenis_muziek";

    private static GuhEntity guh(GameTestHelper helper, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.setPersistenceRequired();
        return guh;
    }

    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = ErfenisMuziek.DUUR + 200)
    public static void muziekdoosjeLaatGuhsDansen(GameTestHelper helper) {
        BlockPos doos = new BlockPos(2, 1, 2);
        SlaapBlokken.Speeldoos blok = SlaapzorgFeature.MUZIEKDOOSJE.get();
        helper.setBlock(doos, blok.defaultBlockState().setValue(SlaapBlokken.Speeldoos.SPEELT, 1));
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(doos);
        helper.assertFalse(blok.speelt(level, abs, helper.getBlockState(doos)), "speelt=1 without a waiting tick (old stamp) is not stuck");
        GuhEntity wild = guh(helper, new BlockPos(1, 1, 3));
        GuhEntity show = guh(helper, new BlockPos(3, 1, 1));
        show.setNoAi(true);
        show.addTag(DecoWezens.TAG);
        helper.runAfterDelay(5, () -> {   // (standing on the ground first)
            ErfenisMuziek.forceer(level, abs);
            helper.assertTrue(ErfenisMuziek.dansHier(level, abs), "a dance box here");
            BlockState s = helper.getBlockState(doos);
            int dansers = blok.speel(level, abs, s);
            helper.assertTrue(dansers == 2, "two guhs dance, got " + dansers);
            helper.assertTrue(helper.getBlockState(doos).getValue(SlaapBlokken.Speeldoos.SPEELT) == ErfenisMuziek.STAPPEN, "it plays");
            helper.assertTrue(blok.speelt(level, abs, helper.getBlockState(doos)), "playing: a second click does nothing");
            helper.assertTrue(wild.emotes.dansOpMuziek() && show.emotes.dansOpMuziek(), "the wild guh and the show guh dance");
            helper.assertFalse(Slapen.isIngezongen(wild), "nobody is sung asleep");
        });
        helper.runAfterDelay(5 + ErfenisMuziek.STAP * 3 + 10, () ->
                helper.assertTrue(wild.emotes.dansOpMuziek() && show.emotes.dansOpMuziek(), "still dancing after three steps"));
        helper.runAfterDelay(5 + ErfenisMuziek.DUUR + 5, () -> {
            BlockState s = helper.getBlockState(doos);
            helper.assertTrue(s.getValue(SlaapBlokken.Speeldoos.SPEELT) == 0 && !blok.speelt(level, abs, s), "the song is over");
            helper.assertFalse(wild.emotes.dansOpMuziek() || show.emotes.dansOpMuziek(), "everyone stopped dancing");
            helper.assertFalse(ErfenisMuziek.dansHier(level, abs), "back to a lullaby box outside De Erfenis");
            helper.succeed();
        });
    }
}
