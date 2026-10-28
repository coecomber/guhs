package nl.juiced.guhs.feature.hamsterbuis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.gametest.GuhTest;

/** 1.8.0 (vadskracht): the hamsterbuis network (a trip in the same dimension; a network without vadskracht stands still). */
public class HamsterbuisGameTests {
    private static final String BATCH = "hamsterbuis", KAMER = "vadskracht_test_kamer";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    static Station.BlockEntityImpl station(GameTestHelper helper, ServerPlayer p, BlockPos rel) {
        helper.setBlock(rel, HamsterbuisFeature.STATION.get().defaultBlockState());
        Station.BlockEntityImpl s = (Station.BlockEntityImpl) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        s.inschrijven(p);
        return s;
    }

    /** Two stations, a knooppunt with VK: the trip takes you to the other station and costs REIS of the buffer. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void reisBinnenDezelfdeDimensie(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        HamsterNet net = HamsterNet.van(helper.getLevel().getServer());
        Station.BlockEntityImpl a = station(helper, p, new BlockPos(1, 1, 1));
        Station.BlockEntityImpl b = station(helper, p, new BlockPos(5, 1, 5));
        net.zetKnooppunt(p.getUUID(), GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(3, 1, 3))));
        net.zetLading(p.getUUID(), HamsterbuisFeature.BUFFER);
        BlockPos doel = helper.absolutePos(new BlockPos(5, 1, 5));
        helper.assertTrue(a.reis(p, b.id()), "the trip goes");
        helper.assertTrue(p.blockPosition().equals(doel), "the player stands in the other tube (" + p.blockPosition() + " vs " + doel + ")");
        helper.assertTrue(net.lading(p.getUUID()) == HamsterbuisFeature.BUFFER - HamsterbuisFeature.REIS, "one trip's VK is paid");
        helper.assertTrue(net.bereikbaar(helper.getLevel().getServer(), p.getUUID()).size() >= 2, "both stations are in the network");
        net.wis(a.id());
        net.wis(b.id());
        net.zetKnooppunt(p.getUUID(), null);
        helper.succeed();
    }

    /** No knooppunt, or an empty buffer: the network stands still, nobody moves. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void zonderVadskrachtStaatHetNetStil(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        HamsterNet net = HamsterNet.van(helper.getLevel().getServer());
        Station.BlockEntityImpl a = station(helper, p, new BlockPos(1, 1, 1));
        Station.BlockEntityImpl b = station(helper, p, new BlockPos(5, 1, 5));
        net.zetKnooppunt(p.getUUID(), null);
        net.zetLading(p.getUUID(), HamsterbuisFeature.BUFFER);
        BlockPos voor = p.blockPosition();
        helper.assertFalse(a.reis(p, b.id()), "no knooppunt: no trip");
        net.zetKnooppunt(p.getUUID(), GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(3, 1, 3))));
        net.zetLading(p.getUUID(), HamsterbuisFeature.REIS - 1);
        helper.assertFalse(net.draait(p.getUUID()), "too little VK: the network stands still");
        helper.assertFalse(a.reis(p, b.id()), "too little VK: no trip");
        helper.assertTrue(p.blockPosition().equals(voor), "the player did not move");
        helper.assertTrue(net.lading(p.getUUID()) == HamsterbuisFeature.REIS - 1, "nothing was paid");
        net.wis(a.id());
        net.wis(b.id());
        net.zetKnooppunt(p.getUUID(), null);
        helper.succeed();
    }
}
