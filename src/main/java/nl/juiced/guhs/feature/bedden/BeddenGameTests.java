package nl.juiced.guhs.feature.bedden;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.slaapzorg.SlaapplekGoal;
import nl.juiced.guhs.feature.wereldleven.Dagritme;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the 1.8.0 beds (batch "bedden"): a player sleeps in a Droombed and its spawn point is set there; a tame guh
 * and a tame Blub each find their own vakje in a family bed (the Guh-hoofd-bank) and sleep; a guh and a Blub snuggle up
 * together in the Guh-en-Blub-mandje.
 */
public class BeddenGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "bedden";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static GuhEntity guh(GameTestHelper helper, BlockPos at, ServerPlayer baas) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.setPersistenceRequired();
        guh.tame(baas);
        BedSlaapGoal.TEST_NACHT.add(guh.getUUID());
        return guh;
    }

    private static BlubGuhEntity blub(GameTestHelper helper, BlockPos at, ServerPlayer baas) {
        BlubGuhEntity b = helper.spawn(BlubFeature.BLUBGUH.get(), at);
        b.setPersistenceRequired();
        b.tame(baas);
        BedSlaapGoal.TEST_NACHT.add(b.getUUID());
        return b;
    }

    /** A Droombed is a real bed: the player lies down in it at night and the spawn point is set. */
    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 100)
    public static void spelerSlaaptInDroombed(GameTestHelper helper) {
        nl.juiced.guhs.world.GuhTime.setDayTime(helper.getLevel(), 18000);
        BlockPos voet = new BlockPos(3, 2, 4), hoofd = voet.north();
        BlockState s = BeddenFeature.DROOMBED.get().defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        helper.setBlock(voet, s.setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(hoofd, s.setValue(BedBlock.PART, BedPart.HEAD));
        helper.assertTrue(helper.getBlockState(hoofd).is(net.minecraft.tags.BlockTags.BEDS), "the Droombed is a #minecraft:beds bed");
        ServerPlayer p = speler(helper, new BlockPos(4, 2, 4));
        helper.runAfterDelay(5, () -> {   // (the night reaches the bed rule a moment after the clock is set)
            var r = p.startSleepInBed(helper.absolutePos(hoofd));
            helper.assertTrue(r.right().isPresent(), "the player can sleep in it: " + r.left().map(Object::toString).orElse(""));
            helper.assertTrue(p.isSleeping(), "asleep");
            helper.assertTrue(p.getRespawnConfig() != null, "the spawn point is set by the Droombed");
            p.stopSleeping();
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
            helper.succeed();
        });
    }

    /** A family bed: a tame guh and a tame Blub each find their own vakje in the Guh-hoofd-bank and fall asleep there. */
    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 600)
    public static void guhEnBlubVindenHunVakjeInFamiliebed(GameTestHelper helper) {
        FamilieBedBlock bank = BeddenFeature.FAMILIE.get("guh_hoofdbank").get();
        BlockPos anker = new BlockPos(2, 2, 6);
        bank.plaatsRest(helper.getLevel(), helper.absolutePos(anker), Direction.NORTH);
        helper.assertTrue(helper.getBlockState(anker).is(bank), "the bench stands (anchor)");
        helper.assertTrue(helper.getBlockState(anker.east(4).north()).is(bank), "the whole bench stands (5 by 2)");
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 8));
        GuhEntity g = guh(helper, new BlockPos(3, 2, 8), p);
        BlubGuhEntity b = blub(helper, new BlockPos(5, 2, 8), p);
        helper.succeedWhen(() -> {
            helper.assertTrue(BedSlaapGoal.slaaptInVakje(g), "the guh sleeps in its vakje");
            helper.assertTrue(BedSlaapGoal.slaaptInVakje(b) && b.slaapt(), "the Blub sleeps in its vakje");
            helper.assertFalse(g.getPersistentData().getStringOr(Vakjes.VAKJE, "").equals(b.getPersistentData().getStringOr(Vakjes.VAKJE, "x")),
                    "each its own vakje");
            BedSlaapGoal.TEST_NACHT.remove(g.getUUID());
            BedSlaapGoal.TEST_NACHT.remove(b.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        });
    }

    /** The Guh-en-Blub-mandje: the guh curls up in it (a sleeping spot of the tag) and the Blub cuddles up in its vakje. */
    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 600)
    public static void guhEnBlubInHetMandje(GameTestHelper helper) {
        BlockPos mandje = new BlockPos(4, 2, 4);
        helper.setBlock(mandje, BeddenFeature.MANDJE.get().defaultBlockState());
        ServerPlayer p = speler(helper, new BlockPos(8, 2, 8));
        GuhEntity g = guh(helper, new BlockPos(2, 2, 2), p);
        SlaapplekGoal.TEST_NACHT.add(g.getUUID());
        Dagritme.TEST_DAGDEEL.put(g.getUUID(), Dagdeel.NACHT);
        BedSlaapGoal.TEST_NACHT.remove(g.getUUID());   // (the guh takes the basket by the sleeping-spot tag)
        BlubGuhEntity b = blub(helper, new BlockPos(6, 2, 2), p);
        helper.succeedWhen(() -> {
            helper.assertTrue(SlaapplekGoal.slaaptInPlek(g) && g.blockPosition().equals(helper.absolutePos(mandje)), "the guh sleeps in the basket");
            helper.assertTrue(BedSlaapGoal.slaaptInVakje(b) && b.blockPosition().equals(helper.absolutePos(mandje)), "the Blub cuddles up in it");
            SlaapplekGoal.TEST_NACHT.remove(g.getUUID());
            Dagritme.TEST_DAGDEEL.remove(g.getUUID());
            BedSlaapGoal.TEST_NACHT.remove(b.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        });
    }
}
