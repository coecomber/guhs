package nl.juiced.guhs.feature.kermisuitbouw;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.Kermis;

/**
 * The stamp of the kermis extension: far from the other tests, on a flat world with the kermis floor 21 blocks above the ground
 * (a valley as deep as the one at kermis C), a hill above the floor, and turned like kermis C. The rides and stalls may not
 * exist in this tree: their anchors are then simply air.
 */
public class KermisUitbouwGameTests {
    /** The stalls in the kermis frame: x, z of the block, and the step to its back (away from FACING). */
    private static final int[][] KRAMEN = {{14, 39, -1, 0}, {14, 48, -1, 0}, {1, 54, 0, -1}, {-8, 54, 0, -1}, {22, 39, 1, 0}, {22, 48, 1, 0}, {44, 46, -1, 0}};

    @GuhTest(template = "empty", batch = "kermis_uitbouw", timeoutTicks = 100)
    public static void kermisUitbouwRecht(GameTestHelper helper) {
        test(helper, new BlockPos(24_000, 0, 24_000), Rotation.NONE);
    }

    @GuhTest(template = "empty", batch = "kermis_uitbouw", timeoutTicks = 100)
    public static void kermisUitbouwGedraaid(GameTestHelper helper) {
        test(helper, new BlockPos(28_000, 0, 28_000), Rotation.COUNTERCLOCKWISE_90);
    }

    @GuhTest(template = "empty", batch = "kermis_uitbouw", timeoutTicks = 100)
    public static void kermisGrondBlijftUitDeKaasmakerij(GameTestHelper helper) {
        helper.assertTrue(!KermisGrond.magVullen(53, true), "no ground in a structure below y 54");
        helper.assertTrue(KermisGrond.magVullen(54, true) && KermisGrond.magVullen(53, false), "elsewhere it fills");
        helper.succeed();
    }

    /** The way it really goes: chunks that do not exist yet are generated through tickets, the work takes its time over ticks. */
    @GuhTest(template = "empty", batch = "kermis_uitbouw_later", timeoutTicks = 100000)
    public static void kermisUitbouwWachtOpChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KermisUitbouw.Plek plek = new KermisUitbouw.Plek(new BlockPos(-26_000, -40, 31_000), Rotation.CLOCKWISE_90);
        helper.assertTrue(level.getChunkSource().getChunkNow(-26_000 >> 4, 31_000 >> 4) == null, "the chunks are not there yet");
        KermisUitbouw.startNormaal(level, plek);
        helper.assertTrue(Kermis.protectedAt(level, l(plek, 30, 3, 60)), "protected from the start");
        helper.succeedWhen(() -> {
            helper.assertTrue(KermisUitbouwData.van(level).versie(plek) == KermisUitbouw.VERSIE, "still at work: " + KermisUitbouw.stand(level, plek));
            BlockState s = level.getBlockState(l(plek, 18, 1, 40));
            helper.assertTrue(s.is(Blocks.WHITE_CONCRETE) || s.is(Blocks.PINK_CONCRETE), "the street");
        });
    }

    private static void test(GameTestHelper helper, BlockPos o, Rotation draai) {
        ServerLevel level = helper.getLevel();
        level.getChunk(o.getX() >> 4, o.getZ() >> 4);
        int grond = level.getHeight(Heightmap.Types.WORLD_SURFACE, o.getX(), o.getZ()) - 1;
        int vloer = grond + 21;
        BlockPos oorsprong = new BlockPos(o.getX(), vloer - KermisUitbouw.VLOER, o.getZ());
        KermisUitbouw.Plek plek = new KermisUitbouw.Plek(oorsprong, draai);
        for (var c : plek.chunks()) {
            level.getChunk(c.x(), c.z());
        }
        // a hill on the field up to 9 above the floor, with a chest on top, and a tree in the band
        for (int x = 28; x <= 32; x++) {
            for (int z = 58; z <= 62; z++) {
                for (int y = grond + 1; y <= vloer + 9; y++) {
                    level.setBlock(l(plek, x, y - oorsprong.getY(), z), Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        BlockPos kist = l(plek, 30, vloer + 10 - oorsprong.getY(), 60);
        level.setBlock(kist, Blocks.CHEST.defaultBlockState(), 2);
        ((Container) level.getBlockEntity(kist)).setItem(3, new ItemStack(Items.DIAMOND, 5));
        BlockPos boom = l(plek, 86, grond + 1 - oorsprong.getY(), 60);
        for (int i = 0; i < 6; i++) {
            level.setBlock(boom.above(i), Blocks.OAK_LOG.defaultBlockState(), 2);
        }
        BlockPos kermis = l(plek, 10, 1, 10);                       // the kermis itself stays
        level.setBlock(kermis, Blocks.GOLD_BLOCK.defaultBlockState(), 2);

        helper.assertTrue(KermisUitbouw.bouwNu(level, plek), "it stamps");
        helper.assertTrue(KermisUitbouwData.van(level).versie(plek) == KermisUitbouw.VERSIE, "the version is kept");
        helper.assertTrue(!KermisUitbouw.bouwNu(level, plek), "once");

        // explicit coordinates: the frame turns like the kermis (kermis C: x' = x + z, z' = z - x)
        BlockPos mast = draai == Rotation.NONE ? oorsprong.offset(-21, 6, 21) : oorsprong.offset(21, 6, 21);
        helper.assertTrue(!level.getBlockState(mast).isAir(), "the Booster's mast stands at " + mast.toShortString());
        BlockPos weg = l(plek, 18, 1, 40);
        BlockState straat = level.getBlockState(weg);
        helper.assertTrue(straat.is(Blocks.WHITE_CONCRETE) || straat.is(Blocks.PINK_CONCRETE), "the main street at the floor, not " + straat);
        helper.assertTrue(level.getBlockState(l(plek, 30, 11, 60)).isAir() && !level.getBlockState(l(plek, 30, 1, 60)).isAir(), "the hill is cut to the floor");
        helper.assertTrue(level.getBlockState(l(plek, 70, 0, 60)).is(Blocks.PINK_WOOL), "the valley is filled");
        helper.assertTrue(level.getBlockState(l(plek, -41, 0, 90 + 12)).isAir(), "outside the band nothing happened");
        for (int i = 0; i < 6; i++) {
            helper.assertTrue(!level.getBlockState(boom.above(i)).is(Blocks.OAK_LOG), "the tree in the band is gone");
        }
        helper.assertTrue(level.getBlockState(kermis).is(Blocks.GOLD_BLOCK), "the kermis itself is left alone");

        // the slope: halfway through the band between floor and ground
        int y = -1;
        BlockPos rand = l(plek, 85, 0, 60);
        for (int yy = vloer + 1; yy >= grond; yy--) {
            if (!level.getBlockState(new BlockPos(rand.getX(), yy, rand.getZ())).isAir()) {
                y = yy;
                break;
            }
        }
        helper.assertTrue(y > grond && y < vloer, "the band slopes, it is at y " + y);

        // behind every stall's counter two blocks of air for its Kermis-guh, also turned
        for (int[] k : KRAMEN) {
            for (int y2 = 2; y2 <= 3; y2++) {
                BlockPos plekje = l(plek, k[0] + k[2], y2, k[1] + k[3]);
                helper.assertTrue(level.getBlockState(plekje).isAir(), "room for the stall guh at " + plekje.toShortString());
            }
        }

        BlockPos bewaar = l(plek, 15, 2, 87);
        helper.assertTrue(level.getBlockEntity(bewaar) instanceof ChestBlockEntity c && c.countItem(Items.DIAMOND) == 5, "the player's things are saved");

        helper.assertTrue(Kermis.protectedAt(level, l(plek, 30, 3, 60)), "the field is protected");
        helper.assertTrue(Kermis.protectedAt(level, l(plek, -20, 50, 20)), "the Booster's field too");
        helper.assertTrue(Kermis.protectedAt(level, l(plek, -44, 41, 25)), "and where the Booster's arm turns");
        helper.assertTrue(!Kermis.protectedAt(level, l(plek, 200, 3, 60)), "not further");
        helper.succeed();
    }

    /** A spot of the kermis frame in the world. */
    private static BlockPos l(KermisUitbouw.Plek plek, int x, int y, int z) {
        return plek.wereld(new BlockPos(x, y, z));
    }
}
