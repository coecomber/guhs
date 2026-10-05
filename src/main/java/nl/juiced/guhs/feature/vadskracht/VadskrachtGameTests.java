package nl.juiced.guhs.feature.vadskracht;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.block.GuhWireBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.guhoven.GuhOvenBlockEntity;
import nl.juiced.guhs.feature.guhoven.GuhovenFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.taal.NlTekst;

/**
 * Game tests of the vadskracht (bbq2, batch "vadskracht"): a net forms over Guhdraad (and falls apart, and climbs a step),
 * the cap per kind of source, the whole net stands still when it is too heavy, the battery, the hover readout (the Dutch
 * lines a player reads), the machine base classes with their face, machines bigger than one block, and the item and fluid
 * helpers. Template vadskracht_test_kamer: 9 x 6 x 11 with a stone floor (things stand at helper y 2).
 */
public class VadskrachtGameTests {
    private static final String BATCH = "vadskracht", KAMER = "vadskracht_test_kamer";

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** A spot on the floor of the test room. */
    static BlockPos p(int x, int z) {
        return new BlockPos(x, 2, z);
    }

    static void bron(GameTestHelper helper, BlockPos pos, int kracht) {
        helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, kracht));
    }

    /** Guhdraad from x0 to x1 on row z. */
    static void draad(GameTestHelper helper, int x0, int x1, int z) {
        for (int x = x0; x <= x1; x++) {
            helper.setBlock(p(x, z), ModBlocks.GUH_WIRE.get());
        }
    }

    static TestMachineBlock.Kern machine(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, VadskrachtFeature.TESTMACHINE.get());
        return (TestMachineBlock.Kern) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    static VadsNet net(GameTestHelper helper, BlockPos pos) {
        return VadsKracht.net(helper.getLevel(), helper.absolutePos(pos));
    }

    /** The hover readout of this block as a Dutch player reads it. */
    static List<String> lees(GameTestHelper helper, BlockPos pos) {
        return VadsKracht.regels(helper.getLevel(), helper.absolutePos(pos)).stream().map(NlTekst::tekst).toList();
    }

    static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    static void draadAan(GameTestHelper helper, BlockPos pos, boolean aan) {
        helper.assertBlockProperty(pos, GuhWireBlock.POWERED, aan);
    }

    // =====================================================================================================================
    // the net
    // =====================================================================================================================

    /** Source - Guhdraad - Guhoven is one net; cut the wire and it is two; the wire climbs a step; redstone in does nothing. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void vadskrachtNetOverGuhdraad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        bron(helper, p(1, 1), 1);
        draad(helper, 2, 4, 1);
        helper.setBlock(p(5, 1), GuhovenFeature.GUH_OVEN.get());
        GuhOvenBlockEntity oven = (GuhOvenBlockEntity) level.getBlockEntity(helper.absolutePos(p(5, 1)));

        VadsNet net = net(helper, p(3, 1));
        gelijk(helper, VadsNet.Status.DRAAIT, net.status(), "status");
        gelijk(helper, VadsGetallen.GUHRAD, net.aanbod(), "aanbod");
        gelijk(helper, VadsGetallen.GUH_OVEN, net.vraag(), "vraag");
        gelijk(helper, 2, net.knopen().size(), "knopen");
        gelijk(helper, 3, net.draden(), "draden");
        gelijk(helper, 5, net.grootte(), "blocks");
        helper.assertTrue(net.knopen().contains(helper.absolutePos(p(1, 1))) && net.knopen().contains(helper.absolutePos(p(5, 1))), "source and oven are the knopen");
        helper.assertTrue(net(helper, p(1, 1)) == net && net(helper, p(5, 1)) == net, "every block of the net gives the same (cached) net");
        draadAan(helper, p(3, 1), true);
        helper.assertTrue(VadsKracht.heeftKracht(level, helper.absolutePos(p(5, 1))) && oven.heeftKracht(), "the oven has vadskracht");
        helper.assertTrue(GuhovenFeature.guhKracht(level, helper.absolutePos(p(5, 1))), "GuhovenFeature.guhKracht follows the net");
        helper.assertTrue(net(helper, new BlockPos(1, 1, 1)) == VadsNet.EMPTY, "the floor is no net");

        // cut the wire: two nets, the oven's has no source
        helper.setBlock(p(3, 1), Blocks.AIR);
        VadsNet links = net(helper, p(2, 1)), rechts = net(helper, p(4, 1));
        helper.assertTrue(links != rechts && links != net, "two new nets");
        gelijk(helper, VadsNet.Status.DRAAIT, links.status(), "the source's half");
        gelijk(helper, VadsNet.Status.GEEN_BRON, rechts.status(), "the oven's half");
        draadAan(helper, p(2, 1), true);
        draadAan(helper, p(4, 1), false);
        helper.assertFalse(oven.heeftKracht(), "the oven lost its vadskracht");

        // a step up: stone in the gap with Guhdraad on top joins the two halves diagonally
        helper.setBlock(p(3, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 3, 1), ModBlocks.GUH_WIRE.get());
        VadsNet heel = net(helper, p(5, 1));
        gelijk(helper, VadsNet.Status.DRAAIT, heel.status(), "joined over the step");
        gelijk(helper, 3, heel.draden(), "draden over the step");
        helper.assertTrue(oven.heeftKracht(), "the oven has vadskracht again");
        draadAan(helper, p(4, 1), true);

        // redstone INTO the wire does nothing (a redstone block where the source was)
        helper.setBlock(p(1, 1), Blocks.REDSTONE_BLOCK);
        gelijk(helper, VadsNet.Status.GEEN_BRON, net(helper, p(2, 1)).status(), "a redstone block is no source");
        draadAan(helper, p(2, 1), false);
        helper.assertFalse(oven.heeftKracht(), "no vadskracht from redstone");
        helper.succeed();
    }

    /** A net is only rebuilt when a block of it changes: asking, waiting and a changed number keep the same net. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void vadskrachtNetIsGecachet(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        bron(helper, p(1, 1), 1);
        draad(helper, 2, 4, 1);
        TestMachineBlock.Kern machine = machine(helper, p(5, 1));
        VadsNet net = net(helper, p(3, 1));
        helper.runAfterDelay(45, () -> {
            VadsNetten beheer = VadsNetten.van(level);
            helper.assertTrue(beheer.bekend(helper.absolutePos(p(3, 1))) == net, "two seconds later: still the same net object");
            helper.assertTrue(machine.heeftKracht(), "the machine was told it has vadskracht");
            // what a knoop asks or gives changes: evaluated again, not rebuilt
            machine.uit = true;
            VadsKracht.veranderd(level, helper.absolutePos(p(5, 1)));
            helper.assertTrue(net(helper, p(3, 1)) == net, "a changed number keeps the net");
            gelijk(helper, 0, net.vraag(), "the switched-off machine asks nothing");
            bron(helper, p(1, 1), 3);   // (the same block, another state)
            helper.assertTrue(net(helper, p(3, 1)) == net, "a source that gives more keeps the net");
            gelijk(helper, 30, net.aanbod(), "aanbod of the stronger source");
            // a block more: now it is rebuilt
            helper.setBlock(p(4, 2), ModBlocks.GUH_WIRE.get());
            VadsNet nieuw = net(helper, p(3, 1));
            helper.assertTrue(nieuw != net && nieuw.draden() == 4, "a new piece of Guhdraad gives a new net");
            helper.succeed();
        });
    }

    /**
     * Too big: more blocks than fit in one net. Every piece of the setup stands still (also what did not fit in the first
     * net), and it runs again as soon as it is small enough. (Alone in its batch: it makes the limit small for a moment.)
     */
    @GuhTest(template = KAMER, batch = BATCH + "_groot", timeoutTicks = 100)
    public static void vadskrachtTeGroot(GameTestHelper helper) {
        int echt = VadsNetten.maxNet;
        VadsNetten.maxNet = 6;
        try {
            bron(helper, p(0, 1), 1);
            draad(helper, 1, 7, 1);
            TestMachineBlock.Kern machine = machine(helper, p(8, 1));   // 9 blocks in a row
            VadsNet bijBron = net(helper, p(0, 1)), bijMachine = net(helper, p(8, 1));
            gelijk(helper, VadsNet.Status.TE_GROOT, bijBron.status(), "the source's piece");
            gelijk(helper, VadsNet.Status.TE_GROOT, bijMachine.status(), "the machine's piece");
            helper.assertTrue(bijBron.grootte() <= 6 && bijMachine.grootte() <= 6, "no net is bigger than the limit");
            helper.assertFalse(machine.heeftKracht(), "a setup that is too big stands still");
            draadAan(helper, p(4, 1), false);
            // (the text names the real limit, VadsGetallen.MAX_NET)
            helper.assertTrue(lees(helper, p(8, 1)).contains("Te groot: meer dan " + VadsGetallen.MAX_NET
                    + " blokken aan elkaar, daar wordt een guh duizelig van. Alles staat stil."), "readout: " + lees(helper, p(8, 1)));
            // shorter: source, three wires, machine fit in one net
            for (int x = 4; x <= 8; x++) {
                helper.setBlock(p(x, 1), Blocks.AIR);
            }
            TestMachineBlock.Kern dichtbij = machine(helper, p(4, 1));
            VadsNet klein = net(helper, p(0, 1));
            gelijk(helper, VadsNet.Status.DRAAIT, klein.status(), "small enough again");
            gelijk(helper, 5, klein.grootte(), "one net of five blocks");
            helper.assertTrue(dichtbij.heeftKracht(), "and it runs");
        } finally {
            VadsNetten.maxNet = echt;
        }
        helper.succeed();
    }

    /** Chunks come and go: the nets in them are rebuilt (here the chunk is still there, so the net comes back as it was). */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void vadskrachtChunksKomenEnGaan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        bron(helper, p(1, 1), 1);
        draad(helper, 2, 4, 1);
        TestMachineBlock.Kern machine = machine(helper, p(5, 1));
        VadsNet net = net(helper, p(3, 1));
        helper.assertTrue(net.draait() && machine.heeftKracht(), "the net runs");
        VadsNetten beheer = VadsNetten.van(level);
        net.knopen().forEach(plek -> beheer.chunkWeg(ChunkPos.containing(plek)));
        helper.runAfterDelay(3, () -> {
            VadsNet nieuw = beheer.bekend(helper.absolutePos(p(3, 1)));
            helper.assertTrue(nieuw != null && nieuw != net, "after an unload event the net was built again");
            helper.assertTrue(nieuw.draait() && nieuw.grootte() == 5 && machine.heeftKracht(), "and it is the same net as before");
            beheer.chunkErbij(ChunkPos.containing(helper.absolutePos(p(3, 1))));
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(beheer.bekend(helper.absolutePos(p(3, 1))) == nieuw, "a chunk that loads with knopen that have a net already changes nothing");
                helper.assertTrue(machine.heeftKracht(), "still running");
                helper.succeed();
            });
        });
    }

    // =====================================================================================================================
    // the cap per kind of source, the Guhrad
    // =====================================================================================================================

    static GuhWheelBlockEntity rad(GameTestHelper helper, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        BlockState state = ModBlocks.GUH_WHEEL.get().defaultBlockState();   // facing north: 3 wide along x, 3 high
        helper.setBlock(pos, state);
        BlockPos abs = helper.absolutePos(pos);
        state.getBlock().setPlacedBy(level, abs, level.getBlockState(abs), null, ItemStack.EMPTY);   // its 8 part blocks
        return (GuhWheelBlockEntity) level.getBlockEntity(abs);
    }

    static CompoundTag guh(GuhVariant variant) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Variant", variant.id());
        return tag;
    }

    /** Five Guhraden in one net: only four count; the strongest count first; an empty wheel gives nothing. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void vadskrachtHooguitVierGuhraden(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhWheelBlockEntity[] raden = new GuhWheelBlockEntity[5];
        for (int i = 0; i < 5; i++) {
            raden[i] = rad(helper, p(1, 1 + 2 * i));
            raden[i].insert(guh(GuhVariant.NORMAL));
            helper.setBlock(p(3, 1 + 2 * i), ModBlocks.GUH_WIRE.get());     // next to the wheel's right part block
            if (i > 0) {
                helper.setBlock(p(3, 2 * i), ModBlocks.GUH_WIRE.get());
            }
        }
        VadsNet net = net(helper, p(3, 1));
        gelijk(helper, 5, net.knopen().size(), "five wheels in one net");
        gelijk(helper, 5, net.aantal(BronSoort.GUHRAD), "aantal");
        gelijk(helper, BronSoort.GUHRAD.max, net.telt(BronSoort.GUHRAD), "telt");
        gelijk(helper, 4 * VadsGetallen.GUHRAD, net.aanbod(), "aanbod of five ordinary guhs");
        gelijk(helper, 5 * 9 + 9, net.grootte(), "every block of every wheel is in the net");
        // equal output: the lowest positions count, so the last wheel is the one too many
        helper.assertTrue(raden[0].teltMee() && raden[3].teltMee() && !raden[4].teltMee(), "the fifth wheel does not count");
        helper.assertFalse(net.teltMee(raden[4]), "VadsNet.teltMee");
        List<String> regels = lees(helper, p(2, 9).above(2));   // a part block of the fifth wheel
        helper.assertTrue(regels.contains("Deze opstelling gebruikt 0/40 vadskracht"), "readout of the net: " + regels);
        helper.assertTrue(regels.contains("Dit geeft 10 vadskracht"), "readout of the wheel: " + regels);
        helper.assertTrue(regels.contains("Telt niet mee: hooguit 4 Guhraden per opstelling"), "readout says it does not count: " + regels);
        helper.assertFalse(lees(helper, p(1, 1)).stream().anyMatch(r -> r.startsWith("Telt niet mee")), "a wheel that counts does not say so");

        // a story guh runs harder: now the fifth wheel is the strongest, and another one does not count
        raden[4].takeOut();
        raden[4].insert(guh(GuhVariant.BALTOGUH));
        int balto = GuhradKracht.van(GuhVariant.BALTOGUH).kracht();
        helper.assertTrue(balto > VadsGetallen.GUHRAD, "the Baltoguh gives more than an ordinary guh: " + balto);
        net = net(helper, p(3, 1));
        gelijk(helper, balto + 3 * VadsGetallen.GUHRAD, net.aanbod(), "aanbod with a Baltoguh");
        helper.assertTrue(raden[4].teltMee() && !raden[3].teltMee(), "the strongest count first");

        // a happy guh (blij when it goes in) gives more for as long as it runs
        raden[0].takeOut();
        CompoundTag blij = guh(GuhVariant.NORMAL);
        CompoundTag data = new CompoundTag();
        data.putLong(Band.BLIJ_TOT, level.getGameTime() + 6000);
        blij.put("NeoForgeData", data);
        raden[0].insert(blij);
        helper.assertTrue(raden[0].isBlij() && raden[0].vadsAanbod() == VadsGetallen.GUHRAD_BLIJ, "a happy guh gives " + VadsGetallen.GUHRAD_BLIJ);
        gelijk(helper, balto + VadsGetallen.GUHRAD_BLIJ + 2 * VadsGetallen.GUHRAD, net(helper, p(3, 1)).aanbod(), "aanbod with a happy guh");
        helper.assertTrue(lees(helper, p(1, 1)).contains("Een blij guhtje: het rent extra hard, njeg!"), "the wheel's own line");

        // an empty wheel gives nothing and says so
        raden[1].takeOut();
        gelijk(helper, 0, raden[1].vadsAanbod(), "an empty wheel");
        gelijk(helper, balto + VadsGetallen.GUHRAD_BLIJ + 2 * VadsGetallen.GUHRAD, net(helper, p(3, 1)).aanbod(), "the empty wheel is the one that does not count");
        helper.assertTrue(lees(helper, p(1, 3)).contains("Er rent geen guh in dit rad"), "readout of an empty wheel: " + lees(helper, p(1, 3)));
        helper.succeed();
    }

    /** The Guhrad table is data (data/guhs/vadskracht/guhrad.json). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void vadskrachtGuhradTabel(GameTestHelper helper) {
        gelijk(helper, new GuhradKracht.Kracht(VadsGetallen.GUHRAD, VadsGetallen.GUHRAD_BLIJ), GuhradKracht.van(GuhVariant.NORMAL), "an ordinary guh");
        gelijk(helper, GuhradKracht.van(GuhVariant.NORMAL), GuhradKracht.van("een_variant_die_niet_bestaat"), "unknown variants give the standard");
        for (GuhVariant verhaal : new GuhVariant[] {GuhVariant.BALTOGUH, GuhVariant.MEWTWO, GuhVariant.STITCH626}) {
            GuhradKracht.Kracht k = GuhradKracht.van(verhaal);
            helper.assertTrue(k.kracht() >= 20 && k.blij() <= 25 && k.blij() >= k.kracht(), verhaal + " gives 20-25, got " + k);
        }
        CompoundTag guh = guh(GuhVariant.MEWTWO);
        helper.assertFalse(GuhradKracht.isBlij(guh, 100), "a guh without the happy buff");
        gelijk(helper, GuhradKracht.van(GuhVariant.MEWTWO).kracht(), GuhradKracht.van(guh, false), "from saved guh data");
        helper.succeed();
    }

    // =====================================================================================================================
    // too heavy: everything stands still
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void vadskrachtTeZwaarStaatAllesStil(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        bron(helper, p(1, 1), 1);
        draad(helper, 2, 6, 1);
        TestMachineBlock.Kern a = machine(helper, p(2, 2)), b = machine(helper, p(4, 2)), c = machine(helper, p(6, 2));
        a.vakken().set(0, ItemResource.of(Items.COBBLESTONE), 4);
        VadsNet net = net(helper, p(3, 1));
        gelijk(helper, VadsNet.Status.TE_ZWAAR, net.status(), "three machines of 5 on a source of 10");
        gelijk(helper, 15, net.vraag(), "vraag");
        gelijk(helper, 10, net.aanbod(), "aanbod");
        gelijk(helper, 5, net.tekort(), "tekort");
        helper.assertFalse(a.heeftKracht() || b.heeftKracht() || c.heeftKracht(), "not one machine runs");
        draadAan(helper, p(4, 1), false);
        List<String> regels = lees(helper, p(4, 2));
        gelijk(helper, List.of("Deze opstelling gebruikt 15/10 vadskracht", "Te zwaar: er is 5 vadskracht te weinig, dus alles staat stil. Njeg!",
                "Dit gebruikt 5 vadskracht"), regels, "readout of a machine in a net that is too heavy");
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(a.vakken().getAmountAsInt(0) == 4 && a.vakken().getAmountAsInt(1) == 0, "the machine did no work at all");
            helper.assertBlockProperty(p(2, 2), MachineBlock.SNOET, Snoet.SLAAPT);
            // switch one off: it fits again, and everything else runs
            c.uit = true;
            helper.runAfterDelay(6, () -> {
                VadsNet nu = net(helper, p(3, 1));
                gelijk(helper, VadsNet.Status.DRAAIT, nu.status(), "two machines of 5 on a source of 10");
                helper.assertTrue(a.heeftKracht() && b.heeftKracht(), "the other machines run");
                draadAan(helper, p(4, 1), true);
                gelijk(helper, List.of("Deze opstelling gebruikt 10/10 vadskracht", "Dit staat uit"), lees(helper, p(6, 2)), "readout of the machine that is off");
                helper.succeedWhen(() -> {
                    helper.assertBlockProperty(p(2, 2), MachineBlock.SNOET, Snoet.WERKT);
                    helper.assertBlockProperty(p(6, 2), MachineBlock.SNOET, Snoet.SLAAPT);
                    helper.assertTrue(a.vakken().getAmountAsInt(1) >= 1, "the machine works now");
                });
            });
        });
    }

    // =====================================================================================================================
    // the battery
    // =====================================================================================================================

    /** A battery covers the difference for as long as it lasts (then everything stops), and what is left over charges it. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void vadskrachtBatterij(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        bron(helper, p(1, 1), 1);
        draad(helper, 2, 7, 1);
        TestMachineBlock.Kern a = machine(helper, p(2, 2)), b = machine(helper, p(4, 2)), c = machine(helper, p(6, 2));
        helper.setBlock(p(7, 2), VadskrachtFeature.TESTBATTERIJ.get());
        BatterijBlockEntity batterij = (BatterijBlockEntity) level.getBlockEntity(helper.absolutePos(p(7, 2)));
        gelijk(helper, VadsGetallen.BATTERIJ, batterij.vadsMax(), "what a battery holds");
        batterij.zetInhoud(12);
        VadsNet net = net(helper, p(3, 1));
        gelijk(helper, VadsNet.Status.DRAAIT, net.status(), "15 asked, 10 given, 12 in the battery");
        gelijk(helper, 12L, net.buffer(), "buffer");
        gelijk(helper, VadsGetallen.BATTERIJ, net.bufferMax(), "bufferMax");
        helper.assertTrue(a.heeftKracht() && b.heeftKracht() && c.heeftKracht(), "everything runs on the battery");
        helper.assertTrue(lees(helper, p(4, 1)).contains("De batterij past 5 vadskracht bij: nog 2 tellen"), "readout on the battery: " + lees(helper, p(4, 1)));
        helper.assertTrue(lees(helper, p(7, 2)).contains("Opgeslagen: 12/18000 vadskracht"), "readout of the battery: " + lees(helper, p(7, 2)));
        helper.assertBlockProperty(p(7, 2), BatterijBlock.LADING, 1);
        int[] fase = {0};
        helper.onEachTick(() -> {
            VadsNet nu = VadsNetten.van(level).bekend(helper.absolutePos(p(3, 1)));
            if (fase[0] == 0 && nu != null && nu.status() == VadsNet.Status.TE_ZWAAR) {
                // two seconds of 5 VK taken out, then 2 VK cannot cover a second any more: everything stops and the 2 stay
                gelijk(helper, 2L, batterij.vadsInhoud(), "what is left when the battery cannot cover a second");
                helper.assertFalse(a.heeftKracht() || b.heeftKracht() || c.heeftKracht(), "everything stopped");
                b.uit = true;
                c.uit = true;   // 5 asked, 10 given: 5 per second go into the battery
                fase[0] = 1;
            } else if (fase[0] == 1 && batterij.vadsInhoud() >= 12) {
                helper.assertTrue(a.heeftKracht(), "the machine that is still on runs");
                gelijk(helper, 0L, (batterij.vadsInhoud() - 2) % 5, "charged in steps of 5 VK per second");
                helper.assertTrue(level.getBlockState(helper.absolutePos(p(7, 2))).getAnalogOutputSignal(level, helper.absolutePos(p(7, 2)), Direction.NORTH) >= 1,
                        "a comparator sees the charge");
                // full is full
                batterij.zetInhoud(VadsGetallen.BATTERIJ - 3);
                gelijk(helper, 3L, batterij.vadsLaad(100), "only what fits goes in");
                helper.assertBlockProperty(p(7, 2), BatterijBlock.LADING, 4);
                gelijk(helper, 7L, batterij.vadsOntlaad(7), "taking out");
                helper.assertBlockProperty(p(7, 2), BatterijBlock.LADING, 3);
                fase[0] = 2;
                helper.succeed();
            }
        });
    }

    // =====================================================================================================================
    // the hover readout
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void vadskrachtUitlezing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        bron(helper, p(1, 1), 2);
        draad(helper, 2, 4, 1);
        helper.setBlock(p(5, 1), GuhovenFeature.GUH_OVEN.get());
        for (BlockPos pos : List.of(p(1, 1), p(3, 1), p(5, 1))) {
            helper.assertTrue(helper.getBlockState(pos).is(VadsKracht.TOON), "in the tag guhs:vadskracht: " + helper.getBlockState(pos));
        }
        for (var blok : List.of(ModBlocks.GUH_WHEEL, ModBlocks.GUH_WHEEL_PART, VadskrachtFeature.MACHINE_DEEL, VadskrachtFeature.TESTBATTERIJ,
                VadskrachtFeature.TESTMACHINE, VadskrachtFeature.TESTMACHINE_GROOT)) {
            helper.assertTrue(blok.get().defaultBlockState().is(VadsKracht.TOON), "in the tag guhs:vadskracht: " + blok.getId());
        }
        gelijk(helper, List.of("Deze opstelling gebruikt 5/20 vadskracht"), lees(helper, p(3, 1)), "Guhdraad");
        gelijk(helper, List.of("Deze opstelling gebruikt 5/20 vadskracht", "Dit geeft 20 vadskracht"), lees(helper, p(1, 1)), "the source");
        gelijk(helper, List.of("Deze opstelling gebruikt 5/20 vadskracht", "Dit gebruikt 5 vadskracht"), lees(helper, p(5, 1)), "the oven");
        gelijk(helper, List.of(), lees(helper, new BlockPos(3, 1, 1)), "the floor");

        // what goes over the network to a player who looks at it
        ServerPlayer speler = GuhMockPlayer.of(helper);
        VadsPayloads.Stand stand = VadsPayloads.stand(speler, helper.absolutePos(p(3, 1)));
        helper.assertTrue(stand != null && stand.pos().equals(helper.absolutePos(p(3, 1))), "an answer for the Guhdraad");
        gelijk(helper, List.of("Deze opstelling gebruikt 5/20 vadskracht"), stand.regels().stream().map(NlTekst::tekst).toList(), "the lines sent");
        helper.assertTrue(VadsPayloads.stand(speler, helper.absolutePos(p(3, 1))) == null, "asked again in the same tick: no second answer");
        helper.assertTrue(VadsPayloads.stand(speler, helper.absolutePos(p(5, 1))) != null, "another block: answered at once");
        helper.assertTrue(VadsPayloads.stand(speler, helper.absolutePos(new BlockPos(3, 1, 1))) == null, "stone is no vadskracht block");
        helper.assertTrue(VadsPayloads.stand(speler, helper.absolutePos(p(1, 1)).offset(500, 0, 0)) == null, "too far away");
        level.removePlayerImmediately(speler, Entity.RemovalReason.DISCARDED);

        // without a source
        helper.setBlock(p(1, 1), Blocks.AIR);
        gelijk(helper, List.of("Deze opstelling gebruikt 5/0 vadskracht", "Geen vadskracht: er rent nergens een guh. Zet een tamme guh in een Guhrad!",
                "Dit gebruikt 5 vadskracht"), lees(helper, p(5, 1)), "an oven without a source");
        helper.succeed();
    }

    // =====================================================================================================================
    // the machine base classes
    // =====================================================================================================================

    /** A machine right next to a source (no Guhdraad): it works, shows its face, and pipes only reach the right slots. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void vadskrachtMachineWerktMetEenSnoet(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TestMachineBlock.Kern machine = machine(helper, p(2, 1));
        helper.assertBlockProperty(p(2, 1), MachineBlock.SNOET, Snoet.SLAAPT);
        BlockPos abs = helper.absolutePos(p(2, 1));
        ResourceHandler<ItemResource> handler = Kisten.van(level, abs, Direction.UP);
        helper.assertTrue(handler != null, "the machine has the item capability");
        // in only into the in slot, out only of the out slot
        gelijk(helper, 0, Kisten.stop(handler, new ItemStack(Items.COBBLESTONE, 3)).getCount(), "three cobblestone go in");
        helper.assertTrue(machine.vakken().getAmountAsInt(0) == 3 && machine.vakken().getAmountAsInt(1) == 0, "into the in slot");
        helper.assertTrue(Kisten.neem(handler, s -> true, 64).isEmpty(), "nothing comes out of the in slot");
        gelijk(helper, 7, Kisten.stop(handler, new ItemStack(Items.DIRT, 7)).getCount(), "nothing goes into the out slot: all the dirt comes back");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(machine.vakken().getAmountAsInt(0) == 3 && !machine.bezig(), "without vadskracht it does nothing");
            bron(helper, p(1, 1), 1);
            helper.succeedWhen(() -> {
                helper.assertTrue(machine.heeftKracht(), "vadskracht from the source next to it");
                helper.assertBlockProperty(p(2, 1), MachineBlock.SNOET, Snoet.WERKT);
                gelijk(helper, 3, machine.vakken().getAmountAsInt(1), "three items worked through");
                gelijk(helper, 3, Kisten.neem(handler, s -> s.is(Items.COBBLESTONE), 64, true).getCount(), "(simulated) the results can be taken");
                gelijk(helper, 3L, Kisten.tel(handler, s -> s.is(Items.COBBLESTONE)), "tel");
                helper.assertTrue(level.getBlockState(abs).getAnalogOutputSignal(level, abs, Direction.NORTH) > 0, "a comparator sees the content");
            });
        });
    }

    /** Full: the out slot holds something else, so the face is surprised; and an owner is remembered and saved. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void vadskrachtMachineVolEnEigenaar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        bron(helper, p(1, 1), 1);
        TestMachineBlock.Kern machine = machine(helper, p(2, 1));
        machine.vakken().set(0, ItemResource.of(Items.COBBLESTONE), 2);
        machine.vakken().set(1, ItemResource.of(Items.DIRT), 1);
        java.util.UUID wie = java.util.UUID.randomUUID();
        machine.zetEigenaar(wie);
        helper.runAfterDelay(40, () -> {
            helper.assertBlockProperty(p(2, 1), MachineBlock.SNOET, Snoet.VOL);
            helper.assertTrue(machine.heeftKracht() && !machine.bezig(), "it has vadskracht but cannot work");
            gelijk(helper, 2, machine.vakken().getAmountAsInt(0), "nothing was worked");
            // saved and loaded: the items, the owner
            CompoundTag tag = machine.saveWithoutMetadata(level.registryAccess());
            TestMachineBlock.Kern kopie = new TestMachineBlock.Kern(machine.getBlockPos(), machine.getBlockState());
            kopie.loadWithComponents(nl.juiced.guhs.storage.Nbt.input(level.registryAccess(), tag));
            gelijk(helper, wie, kopie.eigenaar(), "the owner survives saving");
            helper.assertTrue(kopie.vakken().getResource(0).is(Items.COBBLESTONE) && kopie.vakken().getAmountAsInt(0) == 2
                    && kopie.vakken().getResource(1).is(Items.DIRT), "the items survive saving");
            // take the dirt out: it goes on
            machine.vakken().set(1, ItemResource.EMPTY, 0);
            helper.succeedWhen(() -> {
                helper.assertBlockProperty(p(2, 1), MachineBlock.SNOET, Snoet.WERKT);
                gelijk(helper, 2, machine.vakken().getAmountAsInt(1), "worked through after the out slot was emptied");
            });
        });
    }

    /** A machine of 2 x 2 blocks: its parts belong to it (vadskracht, items), and breaking a part breaks all of it. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void vadskrachtMeerblok(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos kern = p(4, 3), abs = helper.absolutePos(kern);
        BlockState state = VadskrachtFeature.TESTMACHINE_GROOT.get().defaultBlockState();   // facing north: the second column is east
        gelijk(helper, List.of(abs.east(), abs.above(), abs.east().above()), Meerblok.delen(abs, Direction.NORTH, 2, 2, 1), "the parts of a 2 x 2 x 1 machine");
        gelijk(helper, 3 * 3 * 2 - 1, Meerblok.delen(abs, Direction.EAST, 3, 3, 2).size(), "the parts of a 3 x 3 x 2 machine");
        helper.setBlock(kern.east(), Blocks.STONE);
        helper.setBlock(kern, state);
        helper.assertFalse(Meerblok.plaats(level, abs, state, VadskrachtFeature.MACHINE_DEEL.get()), "no room: nothing is placed");
        helper.assertBlockNotPresent(VadskrachtFeature.MACHINE_DEEL.get(), kern.above());
        helper.setBlock(kern.east(), Blocks.AIR);
        helper.assertTrue(Meerblok.plaats(level, abs, state, VadskrachtFeature.MACHINE_DEEL.get()), "room: the parts are placed");
        for (BlockPos deel : List.of(kern.east(), kern.above(), kern.east().above())) {
            helper.assertBlockPresent(VadskrachtFeature.MACHINE_DEEL.get(), deel);
            BlockPos deelAbs = helper.absolutePos(deel);
            gelijk(helper, abs, Meerblok.kern(level.getBlockState(deelAbs), deelAbs), "a part knows its kern");
            helper.assertTrue(VadsKracht.knoop(level, deelAbs) == level.getBlockEntity(abs), "a part is a block of the kern's knoop");
            helper.assertTrue(Kisten.van(level, deelAbs, null) != null, "a part gives the kern's items");
        }
        // Guhdraad next to a PART (not the kern) powers the machine
        bron(helper, p(7, 3), 1);
        helper.setBlock(p(6, 3), ModBlocks.GUH_WIRE.get());
        VadsNet net = net(helper, p(6, 3));
        gelijk(helper, VadsNet.Status.DRAAIT, net.status(), "source - wire - part");
        gelijk(helper, 2, net.knopen().size(), "the machine is ONE knoop, however many blocks");
        gelijk(helper, 1 + 1 + 4, net.grootte(), "source, wire, four machine blocks");
        TestMachineBlock.Kern machine = (TestMachineBlock.Kern) level.getBlockEntity(abs);
        helper.assertTrue(machine.heeftKracht(), "the big machine has vadskracht");
        gelijk(helper, List.of("Deze opstelling gebruikt 5/10 vadskracht", "Dit gebruikt 5 vadskracht"), lees(helper, kern.east().above()), "readout on a part");
        // items in the machine drop when a part is broken, and the whole machine goes
        machine.vakken().set(0, ItemResource.of(Items.COBBLESTONE), 5);
        helper.destroyBlock(kern.east().above());
        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(VadskrachtFeature.TESTMACHINE_GROOT.get(), kern);
            helper.assertBlockNotPresent(VadskrachtFeature.MACHINE_DEEL.get(), kern.east());
            helper.assertBlockNotPresent(VadskrachtFeature.MACHINE_DEEL.get(), kern.above());
            helper.assertItemEntityCountIs(Items.COBBLESTONE, kern, 3.0, 5);
            gelijk(helper, 1, net(helper, p(6, 3)).knopen().size(), "only the source is left in the net");
        });
    }

    // =====================================================================================================================
    // items and fluids
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void vadskrachtKisten(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(p(1, 1), Blocks.CHEST);
        helper.setBlock(p(4, 1), Blocks.BARREL);
        ResourceHandler<ItemResource> kist = Kisten.van(level, helper.absolutePos(p(1, 1)), null);
        ResourceHandler<ItemResource> ton = Kisten.van(level, helper.absolutePos(p(4, 1)), Direction.UP);
        helper.assertTrue(kist != null && ton != null, "vanilla containers have the item capability");
        helper.assertTrue(Kisten.van(level, helper.absolutePos(new BlockPos(1, 1, 1)), null) == null, "stone holds no items");
        // nothing is ever lost: what does not fit comes back
        ItemStack veel = new ItemStack(Items.COBBLESTONE, 64);
        for (int i = 0; i < 26; i++) {
            helper.assertTrue(Kisten.stop(kist, veel).isEmpty(), "a stack fits");
        }
        gelijk(helper, 64, veel.getCount(), "the given stack is not changed");
        helper.assertTrue(Kisten.past(kist, new ItemStack(Items.DIRT, 64)) && !Kisten.past(kist, new ItemStack(Items.DIRT, 65)), "past");
        gelijk(helper, 0, Kisten.stop(kist, new ItemStack(Items.DIRT, 40), true).getCount(), "simulated: fits");
        gelijk(helper, 0L, Kisten.tel(kist, s -> s.is(Items.DIRT)), "simulated: nothing went in");
        ItemStack rest = Kisten.stop(kist, new ItemStack(Items.DIRT, 64));
        helper.assertTrue(rest.isEmpty(), "64 dirt in the last slot");
        rest = Kisten.stop(kist, new ItemStack(Items.DIRT, 10));
        helper.assertTrue(rest.is(Items.DIRT) && rest.getCount() == 10, "the chest is full: all 10 come back, got " + rest);
        gelijk(helper, 26L * 64, Kisten.tel(kist, s -> s.is(Items.COBBLESTONE)), "tel");
        // taking
        ItemStack eruit = Kisten.neem(kist, s -> s.is(Items.DIRT), 5);
        helper.assertTrue(eruit.is(Items.DIRT) && eruit.getCount() == 5, "five dirt taken, got " + eruit);
        gelijk(helper, 59L, Kisten.tel(kist, s -> s.is(Items.DIRT)), "59 left");
        helper.assertTrue(Kisten.neem(kist, s -> s.is(Items.DIAMOND), 5).isEmpty(), "nothing that passes: nothing taken");
        gelijk(helper, 3, Kisten.neem(kist, s -> s.is(Items.DIRT), 3, true).getCount(), "simulated taking");
        gelijk(helper, 59L, Kisten.tel(kist, s -> s.is(Items.DIRT)), "simulated: still 59");
        // moving
        gelijk(helper, 100, Kisten.verplaats(kist, ton, s -> s.is(Items.COBBLESTONE), 100), "100 cobblestone moved to the barrel");
        gelijk(helper, 100L, Kisten.tel(ton, s -> true), "in the barrel");
        gelijk(helper, 26L * 64 - 100, Kisten.tel(kist, s -> s.is(Items.COBBLESTONE)), "out of the chest");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void vadskrachtSauzen(GameTestHelper helper) {
        // the four fluids of the Guh-technologie (milk is a real fluid: NeoForge's, switched on by VadskrachtFeature)
        for (FluidResource saus : List.of(Sauzen.kaassaus(), Sauzen.frituursaus(), Sauzen.water(), Sauzen.melk())) {
            helper.assertTrue(!saus.isEmpty() && Sauzen.isTechniek(saus), "in the tag guhs:techniek_sauzen: " + saus);
        }
        helper.assertFalse(Sauzen.isTechniek(FluidResource.of(Fluids.LAVA)), "lava is not one of them");
        int[] veranderd = {0};
        SausTank tank = new SausTank(4 * Sauzen.EMMER, Sauzen::isTechniek, () -> veranderd[0]++);
        SausTank klein = new SausTank(Sauzen.EMMER, Sauzen::isTechniek, () -> { });
        gelijk(helper, 0, tank.vul(FluidResource.of(Fluids.LAVA), 1000, false), "the tank refuses what it may not hold");
        gelijk(helper, 1500, tank.vul(Sauzen.kaassaus(), 1500, true), "simulated filling");
        helper.assertTrue(tank.isLeeg() && veranderd[0] == 0, "simulated: nothing changed");
        gelijk(helper, 1500, tank.vul(Sauzen.kaassaus(), 1500, false), "1500 mB of kaassaus in");
        gelijk(helper, 1, veranderd[0], "the change is told once");
        gelijk(helper, 0, tank.vul(Sauzen.water(), 500, false), "one fluid at a time");
        gelijk(helper, 2500, tank.vul(Sauzen.kaassaus(), 9999, false), "only what fits");
        helper.assertTrue(tank.inhoud() == 4000 && tank.ruimte() == 0 && tank.vulling() == 1f && tank.saus().equals(Sauzen.kaassaus()), "full of kaassaus");
        // moving: whatever comes first, at most what fits
        gelijk(helper, 1000, Sauzen.verplaats(tank, klein, null, 2500), "the small tank takes one bucket");
        gelijk(helper, 3000, tank.inhoud(), "the rest stays");
        gelijk(helper, 0, Sauzen.verplaats(tank, klein, Sauzen.kaassaus(), 500), "full is full");
        gelijk(helper, 400, klein.tap(400, false), "tapping");
        gelijk(helper, 400, Sauzen.verplaats(tank, klein, Sauzen.kaassaus(), 5000), "and topping up again");
        gelijk(helper, 2600L, Sauzen.tel(tank, Sauzen.kaassaus()), "tel");
        gelijk(helper, Sauzen.kaassaus(), Sauzen.eerste(tank), "eerste");
        gelijk(helper, 250, Sauzen.neem(tank, Sauzen.kaassaus(), 250, false), "neem");
        gelijk(helper, 100, Sauzen.stop(tank, Sauzen.kaassaus(), 100, false), "stop");
        tank.zet(Sauzen.melk(), 750);
        helper.assertTrue(tank.saus().equals(Sauzen.melk()) && tank.inhoud() == 750, "zet replaces the content");
        // saved and loaded
        ServerLevel level = helper.getLevel();
        CompoundTag tag = nl.juiced.guhs.storage.Nbt.write(level.registryAccess(), uit -> tank.opslaan(uit, "Tank"));
        SausTank kopie = new SausTank(4 * Sauzen.EMMER, Sauzen::isTechniek, () -> { });
        kopie.laden(nl.juiced.guhs.storage.Nbt.input(level.registryAccess(), tag), "Tank");
        helper.assertTrue(kopie.saus().equals(Sauzen.melk()) && kopie.inhoud() == 750, "the tank survives saving: " + kopie.saus() + " " + kopie.inhoud());
        helper.succeed();
    }
}
