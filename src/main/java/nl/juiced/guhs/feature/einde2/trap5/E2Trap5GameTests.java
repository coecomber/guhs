package nl.juiced.guhs.feature.einde2.trap5;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.TestMachineBlock;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * The few game tests of tier 5 (batch {@code einde2_trap5_machines}, CONTRACT_E2_B 8): the core of the slice.
 * <ol>
 *   <li>{@link #einde2Trap5ZenderOntvanger}: a Vadszender feeds a machine through a Vadsontvanger across the room, and the
 *       two nets keep ONE rule: too heavy on the far side = everything still, also the zender's own net.</li>
 *   <li>{@link #einde2Trap5Lijn}: the Werktuigguh's line for two players: the door opens only for the one who finished the
 *       Guhvatar story, projects done ahead count, the line steps forward by itself, the other player's state is untouched.</li>
 *   <li>{@link #einde2Trap5Poortbuis}: an item goes into one Knabbelpoort-buis and comes out of its partner into a chest.</li>
 * </ol>
 * Template: main's {@code vadskracht_test_kamer} (9 x 6 x 11, stone floor; things stand at helper y 2).
 */
public class E2Trap5GameTests {
    private static final String BATCH = "einde2_trap5_machines", KAMER = "vadskracht_test_kamer";

    static BlockPos p(int x, int z) {
        return new BlockPos(x, 2, z);
    }

    static void bron(GameTestHelper helper, BlockPos pos, int kracht) {
        helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, kracht));
    }

    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    /** Zender on a 10 VK source, an ontvanger 9 blocks away with a test machine (5 VK): it runs. A second machine: 4 + 10 > 10, all still. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void einde2Trap5ZenderOntvanger(GameTestHelper helper) {
        bron(helper, p(1, 1), 1);
        helper.setBlock(p(1, 2), E2Trap5Feature.ZENDER.get());
        helper.setBlock(p(7, 9), E2Trap5Feature.ONTVANGER.get());
        helper.setBlock(p(7, 8), VadskrachtFeature.TESTMACHINE.get());
        TestMachineBlock.Kern een = (TestMachineBlock.Kern) helper.getLevel().getBlockEntity(helper.absolutePos(p(7, 8)));
        Zender.BlockEntityImpl zender = (Zender.BlockEntityImpl) helper.getLevel().getBlockEntity(helper.absolutePos(p(1, 2)));
        Ontvanger.BlockEntityImpl ontvanger = (Ontvanger.BlockEntityImpl) helper.getLevel().getBlockEntity(helper.absolutePos(p(7, 9)));
        int[] fase = {0};
        TestMachineBlock.Kern[] twee = {null};
        helper.succeedWhen(() -> {
            if (fase[0] == 0) {
                helper.assertTrue(helper.absolutePos(p(1, 2)).equals(ontvanger.zender()), "the ontvanger tuned to the zender");
                helper.assertTrue(een.heeftKracht(), "the machine behind the ontvanger runs on the zender's source");
                helper.assertTrue(zender.doorgeven() == TestMachineBlock.VRAAG && zender.vadsVraag() == T5Getallen.ZENDER + TestMachineBlock.VRAAG,
                        "the zender asks its own 4 plus the 5 of the far side: " + zender.vadsVraag());
                helper.assertTrue(zender.zendt(), "the zender's net runs (9 of 10 VK)");
                // too heavy: a second machine on the far side
                helper.setBlock(p(6, 8), VadskrachtFeature.TESTMACHINE.get());
                twee[0] = (TestMachineBlock.Kern) helper.getLevel().getBlockEntity(helper.absolutePos(p(6, 8)));
                fase[0] = 1;
                helper.assertTrue(false, "(on to the heavy part)");
            }
            helper.assertTrue(zender.doorgeven() == 2 * TestMachineBlock.VRAAG, "the zender now forwards both machines: " + zender.doorgeven());
            helper.assertFalse(zender.heeftKracht(), "4 + 10 > 10: the zender's own net stands still");
            helper.assertFalse(een.heeftKracht() || twee[0].heeftKracht(), "and so does everything behind the ontvanger");
        });
    }

    /** The line: door, projects ahead, stepping forward by itself, two players apart. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void einde2Trap5Lijn(GameTestHelper helper) {
        ServerPlayer a = speler(helper), b = speler(helper);
        try {
            E2Trap5Commando.standIns();
            Verhaallijn lijn = E2Trap5.lijn();
            lijn.wis(a);
            lijn.wis(b);
            E2Trap5Commando.finaleKlaar(a);
            helper.assertTrue(E2Trap5.open(a) && !E2Trap5.open(b), "tier 5 is open only for who finished the story");
            helper.assertTrue(E2Trap5.deurOpen(a) && !E2Trap5.deurOpen(b), "the door: walk-through for a, solid for b");

            // a builds a zender ahead (before the first talk): remembered, the step stays 0
            E2Trap5.meld(a, E2Trap5.P_ZENDER);
            helper.assertTrue(lijn.vlag(a, E2Trap5.P_ZENDER) && lijn.stap(a) == E2Trap5.S_PRAAT, "a project ahead is remembered, nothing skipped");
            // the first talk: step 1, and the zender project counts at once
            lijn.begin(a);
            lijn.verder(a, E2Trap5.S_PRAAT);
            E2Trap5.vooruit(a);
            helper.assertTrue(lijn.stap(a) == E2Trap5.S_TERMINAL, "the done project moved the line on: " + lijn.stap(a));
            // the elements ahead, then the projects in between: it runs on to the sources
            for (String f : E2Trap5.ELEMENTEN) {
                E2Trap5.meld(a, f);
            }
            helper.assertTrue(lijn.stap(a) == E2Trap5.S_TERMINAL, "elements ahead do not skip the terminal");
            E2Trap5.meld(a, E2Trap5.P_TERMINAL);
            E2Trap5.meld(a, E2Trap5.P_BESTEL);
            E2Trap5.meld(a, E2Trap5.P_POORTBUIS);
            E2Trap5.meld(a, E2Trap5.P_THUISPOORT);
            helper.assertTrue(lijn.stap(a) == E2Trap5.S_BRONNEN, "past the elements, waiting for the sources: " + lijn.stap(a));
            E2Trap5.meld(a, E2Trap5.P_ENDERGUHRAD);
            helper.assertTrue(lijn.stap(a) == E2Trap5.S_BRONNEN, "one source is not both");
            E2Trap5.meld(a, E2Trap5.P_KOMEET);
            helper.assertTrue(lijn.stap(a) == E2Trap5.S_TERUG && !lijn.klaar(a), "both sources: back to the Werktuigguh (a talk finishes it)");

            // b: a project counts as a flag, the line does not move, nothing of a's leaks
            E2Trap5.meld(b, E2Trap5.P_ZENDER);
            helper.assertTrue(lijn.vlag(b, E2Trap5.P_ZENDER) && lijn.stap(b) == 0 && !lijn.begonnen(b), "b keeps step 0");
            helper.assertFalse(lijn.vlag(b, E2Trap5.P_TERMINAL), "b has none of a's projects");
            helper.assertFalse(Komeetkern.wacht(b), "the comet shard is not shown to b");
            helper.assertTrue(Komeetkern.wacht(a), "a may pick up the shard");
            Komeetkern.pak(a);
            helper.assertFalse(Komeetkern.wacht(a), "once");
            helper.succeed();
        } finally {
            E2Trap5.lijn().wis(a);
            E2Trap5.lijn().wis(b);
            helper.getLevel().removePlayerImmediately(a, Entity.RemovalReason.DISCARDED);
            helper.getLevel().removePlayerImmediately(b, Entity.RemovalReason.DISCARDED);
        }
    }

    /** Five cobblestone into one piece of Knabbelpoort-buis come out of its partner into the chest in front of it. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2Trap5Poortbuis(GameTestHelper helper) {
        bron(helper, p(0, 1), 1);
        helper.setBlock(p(1, 1), E2Trap5Feature.POORTBUIS.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        bron(helper, p(7, 5), 1);
        helper.setBlock(p(6, 5), E2Trap5Feature.POORTBUIS.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        helper.setBlock(p(6, 6), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        Poortbuis.BlockEntityImpl in = (Poortbuis.BlockEntityImpl) helper.getLevel().getBlockEntity(helper.absolutePos(p(1, 1)));
        Poortbuis.BlockEntityImpl uit = (Poortbuis.BlockEntityImpl) helper.getLevel().getBlockEntity(helper.absolutePos(p(6, 5)));
        in.koppel(GlobalPos.of(helper.getLevel().dimension(), uit.getBlockPos()));
        uit.koppel(GlobalPos.of(helper.getLevel().dimension(), in.getBlockPos()));
        ItemStack rest = Kisten.stop(in.handler(Direction.UP), new ItemStack(Items.COBBLESTONE, 5));
        helper.assertTrue(rest.isEmpty(), "the tube takes the five");
        helper.assertTrue(Kisten.stop(in.handler(Direction.UP), new ItemStack(Items.DIRT), true).getCount() == 1, "one kind at a time in the in slot");
        helper.succeedWhen(() -> {
            var kist = Kisten.van(helper.getLevel(), helper.absolutePos(p(6, 6)), null);
            helper.assertTrue(kist != null && Kisten.tel(kist, s -> s.is(Items.COBBLESTONE)) == 5, "all five came out at the other side");
            helper.assertTrue(in.vakken().getAmountAsInt(Poortbuis.BlockEntityImpl.IN) == 0, "nothing stays behind");
        });
    }
}
