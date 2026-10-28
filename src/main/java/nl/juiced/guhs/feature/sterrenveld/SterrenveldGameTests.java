package nl.juiced.guhs.feature.sterrenveld;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the Sterrenkussenveld (1.8.0, batch "sterrenveld"): a falling star lands and glows, catching one gives
 * droomzand, and a star that nobody catches fades away.
 */
public class SterrenveldGameTests {
    private static final String BATCH = "sterrenveld", KAMER = "vadskracht_test_kamer";

    /** A star drifts down and, on the floor, starts to glow. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 120)
    public static void sterValtEnGloeit(GameTestHelper helper) {
        VallendeSter s = VallendeSter.nieuw(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 5, 2))));
        helper.succeedWhen(() -> {
            helper.assertTrue(s.fase() == VallendeSter.GLOEIT, "the star landed and glows");
            helper.assertTrue(s.getY() < helper.absolutePos(new BlockPos(2, 4, 2)).getY(), "it came down");
            s.discard();
        });
    }

    /** Catching a star: droomzand in the pocket, and the star is gone. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void vangenGeeftDroomzand(GameTestHelper helper) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sterrenvanger"));
        VallendeSter s = VallendeSter.nieuw(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 3, 2))));
        helper.assertTrue(s.vang(p), "caught");
        helper.assertTrue(s.isRemoved(), "the star is gone");
        helper.assertTrue(p.getInventory().countItem(DroomblokkenFeature.DROOMZAND.get()) >= 1, "droomzand in the pocket");
        helper.assertFalse(s.vang(p), "a caught star can't be caught twice");
        helper.succeed();
    }

    /** Nobody catches it: it fades and disappears. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 120)
    public static void sterVervaagt(GameTestHelper helper) {
        VallendeSter s = VallendeSter.nieuw(helper.getLevel(), Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
        s.zetFase(VallendeSter.VERVAAGT);
        helper.succeedWhen(() -> helper.assertTrue(s.isRemoved(), "the fading star disappeared"));
    }
}
