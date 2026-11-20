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

    /** 1.10.10: a golden star gives 10 droomzand; ten in a row give 5 extra; a star fading near you breaks the row. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void goudenSterEnSterrenreeks(GameTestHelper helper) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        Vec3 plek = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 3, 2)));
        p.snapTo(plek.x, plek.y - 1, plek.z);
        p.getInventory().clearContent();
        Sterrenreeks.vergeet(p);
        var zand = DroomblokkenFeature.DROOMZAND.get();
        VallendeSter goud = VallendeSter.nieuw(helper.getLevel(), plek);
        goud.zetGoud(true);
        helper.assertTrue(goud.getItem().is(SterrenveldFeature.LOOK_GOUD.get()), "a golden star looks golden");
        helper.assertTrue(goud.vang(p), "caught the golden one");
        helper.assertTrue(p.getInventory().countItem(zand) == VallendeSter.GOUD_ZAND, "a golden star: 10 droomzand, got " + p.getInventory().countItem(zand));
        for (int i = 2; i <= 9; i++) {                                   // catches 2..9 of the row
            VallendeSter s = VallendeSter.nieuw(helper.getLevel(), plek);
            s.zetGoud(false);
            s.vang(p);
        }
        helper.assertTrue(Sterrenreeks.stand(p) == 9, "nine in a row: " + Sterrenreeks.stand(p));
        int voor = p.getInventory().countItem(zand);
        VallendeSter tiende = VallendeSter.nieuw(helper.getLevel(), plek);
        tiende.zetGoud(false);
        tiende.vang(p);
        int erbij = p.getInventory().countItem(zand) - voor;
        helper.assertTrue(erbij >= 1 + Sterrenreeks.BONUS && erbij <= 2 + Sterrenreeks.BONUS, "the tenth: its own droomzand + 5, got " + erbij);
        helper.assertTrue(Sterrenreeks.stand(p) == 0, "the row starts over");
        for (int i = 0; i < 3; i++) {
            VallendeSter s = VallendeSter.nieuw(helper.getLevel(), plek);
            s.zetGoud(false);
            s.vang(p);
        }
        helper.assertTrue(Sterrenreeks.stand(p) == 3, "three in a new row");
        VallendeSter weg = VallendeSter.nieuw(helper.getLevel(), plek);
        weg.vervaagd(helper.getLevel());
        helper.assertTrue(weg.isRemoved() && Sterrenreeks.stand(p) == 0, "a star fading near you breaks the row");
        Sterrenreeks.vergeet(p);
        helper.succeed();
    }
}
