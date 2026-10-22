package nl.juiced.guhs.feature.bouwhulp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Blauwdrukken;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Catalogus;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.kern.Spoken;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Test round 5 (FX-C): a keet builds a whole catalogue design (the user saw only the half nearest the keet of "rijtje
 * pastel guh-huisjes"), and its range grows with its level.
 */
public class BouwkeetBereikGameTests {
    private static final String BATCH = "bouwkeet_bereik";
    private static final String VLOER = "px_test_16";

    /** "Rijtje pastel guh-huisjes" from the keet's Blauwdrukken tab: every block of the ghost is there in the end. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 4000)
    public static void bouwkeetBouwtHeelRijtje(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.CREATIVE);   // (creative builds for free)
        ServerLevel level = helper.getLevel();
        BlockPos kern = new BlockPos(7, 2, 15);
        helper.setBlock(kern, BouwhulpFeature.BOUWKEET.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        BouwkeetBlockEntity keet = (BouwkeetBlockEntity) level.getBlockEntity(helper.absolutePos(kern));
        Blauwdrukken.alsProject(p, level, keet, Catalogus.get("guh_rijtje").ontwerp, "rijtje");
        Spook s = keet.project();
        helper.assertTrue(s != null, "the rijtje is the keet's project");
        List<Map.Entry<BlockPos, BlockState>> doel = new ArrayList<>(s.volgorde());
        helper.assertTrue(Spookbouw.bouw(p, s, false).begonnen(), "it builds");
        helper.succeedWhen(() -> {
            helper.assertTrue(keet.project() == null, "done: " + (keet.project() == null ? "" : keet.project().gedaan() + " placed, " + keet.project().nogTeGaan() + " to go"));
            List<String> mis = new ArrayList<>();
            for (Map.Entry<BlockPos, BlockState> e : doel) {
                BlockState nu = level.getBlockState(e.getKey());
                if (!nu.is(e.getValue().getBlock())) {
                    mis.add(helper.relativePos(e.getKey()).toShortString() + " " + e.getValue().getBlock() + " is " + nu.getBlock());
                }
            }
            helper.assertTrue(mis.isEmpty(), mis.size() + " of " + doel.size() + " spots differ: " + mis.subList(0, Math.min(8, mis.size())));
        });
    }

    /** The keet's reach grows with its level (64 .. 192); a block just out of reach stays a ghost until the keet goes up a level. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 400)
    public static void bouwkeetBereikGroeitPerNiveau(GameTestHelper helper) {
        helper.assertTrue(BouwNiveau.HANDWERK.bereik == 64 && BouwNiveau.GUHRAD.bereik == 96 && BouwNiveau.ZOUT.bereik == 128
                && BouwNiveau.SAUS.bereik == 160 && BouwNiveau.GLOEI.bereik == 192, "64 / 96 / 128 / 160 / 192");
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.CREATIVE);
        ServerLevel level = helper.getLevel();
        BlockPos kern = new BlockPos(2, 2, 8);
        helper.setBlock(kern, BouwhulpFeature.BOUWKEET.get().defaultBlockState());
        BouwkeetBlockEntity keet = (BouwkeetBlockEntity) level.getBlockEntity(helper.absolutePos(kern));
        BlockPos k = keet.getBlockPos();
        BlockPos start = new BlockPos(k.getX() + 63, k.getY(), k.getZ());   // three planks: +63, +64 (in reach), +65 (out)
        for (int cx = (start.getX() >> 4) - 1; cx <= (start.getX() + 3 >> 4) + 1; cx++) {
            level.setChunkForced(cx, start.getZ() >> 4, true);
        }
        Spook s = Spookbouw.start(p, Ontwerp.doos(3, 1, 1, Blocks.OAK_PLANKS.defaultBlockState()), start, Rotation.NONE, Mirror.NONE);
        helper.assertTrue(s != null && keet.zetProject(level, s), "the keet's project");
        helper.assertTrue(Spoken.buiten(level, s, keet) == 1, "one block is out of reach: " + Spoken.buiten(level, s, keet));
        helper.assertTrue(Spookbouw.bouw(p, s, false).begonnen(), "it builds");
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(level.getBlockState(start).is(Blocks.OAK_PLANKS) && level.getBlockState(start.east()).is(Blocks.OAK_PLANKS),
                    "+63 and +64 are built");
            helper.assertTrue(!level.getBlockState(start.east(2)).is(Blocks.OAK_PLANKS), "+65 is not (out of reach)");
            helper.assertTrue(s.status() == Spook.Status.WACHT, "the rest stays a ghost: " + s.status());
            keet.zetNiveau(1);
            helper.assertTrue(Spoken.buiten(level, s, keet) == 1, "level 1 without vadskracht works as handwerk: still 64");
            keet.vadsStroom(true);                          // (as if its net runs)
            helper.assertTrue(keet.bereik(level) == 96 && Spoken.buiten(level, s, keet) == 0, "level 1 with vadskracht reaches 96");
            for (int cx = (start.getX() >> 4) - 1; cx <= (start.getX() + 3 >> 4) + 1; cx++) {
                level.setChunkForced(cx, start.getZ() >> 4, false);
            }
            Spookbouw.annuleer(level, s);
            helper.succeed();
        });
    }
}
