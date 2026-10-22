package nl.juiced.guhs.feature.safari.camera;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.SafariFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhNummers;

/**
 * Op Safari! S1: the stars of a photo, what the camera sees (walls block it, glass doesn't), the day's missions, and the
 * Guhdex numbers of tamed guhs (F3b).
 */
public class CameraGameTests {
    private static final String BATCH = "safari";
    private static final String VLOER = "px_test_16";

    /** Points to stars: 0-1 = 1, 2-3 = 2, 4-5 = 3; the photo tag keeps what the caption needs. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void sterren(GameTestHelper helper) {
        Foto niks = new Foto("leeuw", "man", "savanne", "idle", false, false, false, 1, 1, 0, 0, 0, 0);
        Foto ver = new Foto("leeuw", "man", "savanne", "idle", false, false, false, 1, 1, 1, 0, 0, 0);
        Foto twee = new Foto("leeuw", "man", "savanne", "idle", false, false, false, 1, 1, 2, 1, 0, 0);
        Foto mooi = new Foto("rode_panda", "vrouw", "bamboe", "slaap", true, true, false, 2, 1, 2, 1, 1, 1);
        helper.assertTrue(niks.sterren() == 1 && ver.sterren() == 1, "0 and 1 point: 1 star");
        helper.assertTrue(twee.sterren() == 2, "3 points: 2 stars");
        helper.assertTrue(mooi.punten() == 5 && mooi.sterren() == 3, "5 points: 3 stars");
        var tag = mooi.tag(7, "Dev");
        helper.assertTrue("slaap".equals(tag.getStringOr("Actie", "")) && tag.getBooleanOr("Baby", false) && tag.getBooleanOr("Dichtbij", false)
                && tag.getBooleanOr("Familie", false) && tag.getIntOr("Sterren", 0) == 3, "the photo tag keeps action, baby, close, family, stars");
        helper.assertTrue("".equals(new Foto("zebra", "vrouw", "savanne", "walk", false, false, false, 1, 1, 0, 0, 0, 0).actieWoord()),
                "walking is no special action");
        // the missions: always three different kinds, the same for the same day
        var a = Missies.van(123);
        var b = Missies.van(123);
        helper.assertTrue(a.size() == 3 && a.equals(b) && a.stream().map(Missies.Missie::soort).distinct().count() == 3, "three different missions per day");
        helper.assertTrue(new Missies.Missie(Missies.Soort.SLAPEND_3, "", "").klopt(mooi), "a sleeping 3-star photo");
        helper.assertTrue(!new Missies.Missie(Missies.Soort.SLAPEND_3, "", "").klopt(twee), "not an awake 2-star one");
        helper.assertTrue(new Missies.Missie(Missies.Soort.ZONE, "", "bamboe").klopt(mooi), "an animal from the bamboo mountains");
        helper.succeed();
    }

    /** The camera sees a lion 6 blocks away; a stone wall hides it, a glass wall (also terrarium glass, kasglas, water) does not. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 40)
    public static void zicht(GameTestHelper helper) {
        BlockPos plek = new BlockPos(10, 1, 8);
        SafariDier d = helper.spawn(SafariFeature.DIER.get(), plek);
        d.zet("leeuw", "man", helper.absolutePos(plek), 3, -1, -1, 0);
        d.setNoAi(true);
        BlockPos camera = helper.absolutePos(new BlockPos(3, 1, 8));
        Vec3 oog = new Vec3(camera.getX() + 0.5, camera.getY() + 1.0, camera.getZ() + 0.5);
        helper.runAfterDelay(2, () -> {
            Vec3 naar = d.getBoundingBox().getCenter().subtract(oog);
            Foto vrij = Fotograaf.kijk(helper.getLevel(), oog, naar, false);
            helper.assertTrue(vrij != null && "leeuw".equals(vrij.soort()), "a lion in plain sight is in the picture");
            for (int y = 1; y <= 4; y++) {
                for (int z = 5; z <= 11; z++) {
                    helper.setBlock(new BlockPos(6, y, z), Blocks.STONE.defaultBlockState());
                }
            }
            helper.assertTrue(Fotograaf.kijk(helper.getLevel(), oog, naar, false) == null, "a stone wall hides it");
            for (int y = 1; y <= 4; y++) {
                for (int z = 5; z <= 11; z++) {
                    helper.setBlock(new BlockPos(6, y, z), Blocks.GLASS.defaultBlockState());
                }
            }
            helper.assertTrue(Fotograaf.kijk(helper.getLevel(), oog, naar, false) != null, "through glass it can be seen");
            // test round 5: the Kriebelhuis tanks (terrariumglas, kasglas) and a water tank behind glass don't block a photo
            for (var glas : java.util.List.of(nl.juiced.guhs.feature.safari.kriebel.KriebelBlokken.TERRARIUMGLAS.get(),
                    nl.juiced.guhs.feature.safari.kriebel.KriebelBlokken.KASGLAS.get(), Blocks.PINK_STAINED_GLASS)) {
                for (int y = 1; y <= 4; y++) {
                    for (int z = 5; z <= 11; z++) {
                        helper.setBlock(new BlockPos(6, y, z), glas.defaultBlockState());
                        helper.setBlock(new BlockPos(7, y, z), Blocks.WATER.defaultBlockState());
                    }
                }
                helper.assertTrue(Fotograaf.kijk(helper.getLevel(), oog, naar, false) != null, "through " + glas + " and water it can be seen");
            }
            helper.assertTrue(Fotograaf.kijk(helper.getLevel(), oog, naar.yRot(1.2f), false) == null, "looking far away: not in the picture");
            helper.succeed();
        });
    }

    /** Numbers are handed out in taming order and never change (F3b); an unknown guh has none. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void guhNummers(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID eigenaar = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID();
        int na = GuhNummers.getemd(server, eigenaar, a);
        int nb = GuhNummers.getemd(server, eigenaar, b);
        helper.assertTrue(na == 1 && nb == 2, "the first tamed guh is #1, the next #2 (was " + na + ", " + nb + ")");
        helper.assertTrue(GuhNummers.getemd(server, eigenaar, a) == 1 && GuhNummers.nummer(server, eigenaar, b) == 2, "numbers stay");
        helper.assertTrue(GuhNummers.nummer(server, eigenaar, UUID.randomUUID()) == 0, "a guh that isn't theirs has no number");
        helper.assertTrue(GuhNummers.getemd(server, UUID.randomUUID(), a) == 1, "every owner counts from 1");
        helper.succeed();
    }
}
