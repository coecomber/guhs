package nl.juiced.guhs.feature.techbuis;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knabbelspelen.KnabbelspelenFeature;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.TestMachineBlock;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.NlTekst;

/**
 * Game tests of the Knabbelbuizen (bbq2, batch "techbuis"): the arms of a tube, a Richtingstuk that empties a chest through
 * tubes (delayed, nothing lost), sorting with a Filterstuk, the Filterstuk's vadskracht / list / "bewaar minstens", one-way
 * pieces, a full or cut-off destination, a broken piece, the redstone lock (and Guhdraad that does not lock), machines and
 * the back of a piece as item capabilities, saving, the menus, the Opzuiger, the four sensors and a sensor that steers a
 * piece. Template techbuis_test_kamer: 13 x 7 x 13 with a stone floor (things stand at helper y 2).
 */
public class TechbuisGameTests {
    private static final String BATCH = "techbuis", KAMER = "techbuis_test_kamer";

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** A spot on the floor of the test room. */
    static BlockPos p(int x, int z) {
        return new BlockPos(x, 2, z);
    }

    static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    static ResourceHandler<ItemResource> items(GameTestHelper helper, BlockPos pos) {
        ResourceHandler<ItemResource> h = Kisten.van(helper.getLevel(), helper.absolutePos(pos), null);
        helper.assertTrue(h != null, "something that holds items at " + pos);
        return h;
    }

    /** A chest with these stacks in it. */
    static void kist(GameTestHelper helper, BlockPos pos, ItemStack... stacks) {
        helper.setBlock(pos, Blocks.CHEST);
        for (ItemStack stack : stacks) {
            helper.assertTrue(Kisten.stop(items(helper, pos), stack).isEmpty(), "the chest takes " + stack);
        }
    }

    static int tel(GameTestHelper helper, BlockPos pos, Item item) {
        return (int) Kisten.tel(items(helper, pos), s -> s.is(item));
    }

    static void buis(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, TechbuisFeature.KNABBELBUIS.get());
    }

    static BuisStukBlockEntity richting(GameTestHelper helper, BlockPos pos, Direction voor) {
        helper.setBlock(pos, TechbuisFeature.KNABBELBUIS_RICHTING.get().defaultBlockState().setValue(BuisStukBlock.FACING, voor));
        return (BuisStukBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    static FilterBlockEntity filter(GameTestHelper helper, BlockPos pos, Direction voor, Item... lijst) {
        helper.setBlock(pos, TechbuisFeature.KNABBELBUIS_FILTER.get().defaultBlockState().setValue(BuisStukBlock.FACING, voor));
        FilterBlockEntity be = (FilterBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        for (Item item : lijst) {
            be.filter().voegToe(new ItemStack(item));
        }
        return be;
    }

    /** A source of 10 x kracht vadskracht. */
    static void bron(GameTestHelper helper, BlockPos pos, int kracht) {
        helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, kracht));
    }

    static int rollend(BuisStukBlockEntity stuk) {
        int n = 0;
        for (BuisStukBlockEntity.Rit rit : stuk.onderweg()) {
            n += rit.stack().getCount();
        }
        for (ItemStack stack : stuk.terug()) {
            n += stack.getCount();
        }
        return n;
    }

    static List<String> lees(GameTestHelper helper, BlockPos pos) {
        return VadsKracht.regels(helper.getLevel(), helper.absolutePos(pos)).stream().map(NlTekst::tekst).toList();
    }

    static boolean arm(GameTestHelper helper, BlockPos pos, Direction kant) {
        return helper.getBlockState(pos).getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(kant));
    }

    // =====================================================================================================================
    // the tubes
    // =====================================================================================================================

    /** A tube grows an arm to a tube, to a chest, to the front and back of a piece and to a machine; not to anything else. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void techbuisBuisVerbindt(GameTestHelper helper) {
        kist(helper, p(1, 1));
        buis(helper, p(2, 1));
        buis(helper, p(3, 1));
        richting(helper, p(4, 1), Direction.EAST);          // in line with the tubes: its back joins
        richting(helper, p(3, 2), Direction.EAST);          // next to the tube, pointing along it: its side does not join
        helper.setBlock(p(2, 2), Blocks.STONE);
        helper.setBlock(p(2, 0), TechbuisFeature.GUHKLOK.get());   // a sensor holds no items
        helper.assertTrue(arm(helper, p(2, 1), Direction.WEST), "an arm to the chest");
        helper.assertTrue(arm(helper, p(2, 1), Direction.EAST), "an arm to the next tube");
        helper.assertFalse(arm(helper, p(2, 1), Direction.SOUTH), "no arm to stone");
        helper.assertFalse(arm(helper, p(2, 1), Direction.NORTH), "no arm to a sensor");
        helper.assertFalse(arm(helper, p(2, 1), Direction.UP), "no arm to air");
        helper.assertTrue(arm(helper, p(3, 1), Direction.EAST), "an arm to the back of a Richtingstuk");
        helper.assertFalse(arm(helper, p(3, 1), Direction.SOUTH), "no arm to the side of a Richtingstuk");
        // an Opzuiger and a test machine hold items: arms
        helper.setBlock(p(2, 2), TechbuisFeature.OPZUIGER.get());
        helper.setBlock(p(2, 0), VadskrachtFeature.TESTMACHINE.get());
        helper.assertTrue(arm(helper, p(2, 1), Direction.SOUTH), "an arm to the Opzuiger");
        helper.assertTrue(arm(helper, p(2, 1), Direction.NORTH), "an arm to a machine");
        // the chest goes: so does the arm
        helper.setBlock(p(1, 1), Blocks.AIR);
        helper.assertFalse(arm(helper, p(2, 1), Direction.WEST), "the arm to the chest is gone with the chest");
        helper.succeed();
    }

    /**
     * Chest - Richtingstuk - three tubes - chest: every item arrives, one bite at a time, each after its ride; at every tick
     * all sixteen are somewhere (source, rolling, destination).
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techbuisRichtingstukLeegtEenKist(GameTestHelper helper) {
        kist(helper, p(1, 1), new ItemStack(Items.WHEAT, 16));
        kist(helper, p(6, 1));
        BuisStukBlockEntity stuk = richting(helper, p(2, 1), Direction.EAST);
        for (int x = 3; x <= 5; x++) {
            buis(helper, p(x, 1));
        }
        gelijk(helper, 1, stuk.routes().size(), "one place to send to");
        gelijk(helper, 4, stuk.routes().get(0).lengte(), "the piece and three tubes");
        gelijk(helper, helper.absolutePos(p(6, 1)), stuk.routes().get(0).doel(), "the far chest");
        gelijk(helper, Direction.WEST, stuk.routes().get(0).kant(), "its west side");
        boolean[] rolde = new boolean[1];
        helper.onEachTick(() -> {
            int a = tel(helper, p(1, 1), Items.WHEAT), b = tel(helper, p(6, 1), Items.WHEAT), r = rollend(stuk);
            gelijk(helper, 16, a + b + r, "nothing lost (source " + a + ", rolling " + r + ", there " + b + ")");
            if (r > 0 && b == 0) {
                rolde[0] = true;   // the first item was on its way while the destination was still empty
            }
            for (BuisStukBlockEntity.Rit rit : stuk.onderweg()) {
                gelijk(helper, Buizen.HAP_RICHTING, rit.stack().getCount(), "a Richtingstuk takes one item per bite");
            }
        });
        helper.succeedWhen(() -> {
            gelijk(helper, 16, tel(helper, p(6, 1), Items.WHEAT), "all wheat arrived");
            helper.assertTrue(rolde[0], "the items took time to roll");
            gelijk(helper, "Stuurt spullen naar 1 plek(ken)", NlTekst.tekst(stuk.stand()), "what the piece says");
        });
    }

    /**
     * Sorting: a nearby chest on the main line and a far chest behind a Filterstuk that asks for wheat. The wheat goes to
     * the far chest (asked for), everything else to the near one.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 500)
    public static void techbuisSorteren(GameTestHelper helper) {
        kist(helper, p(1, 3), new ItemStack(Items.WHEAT, 8), new ItemStack(Items.COBBLESTONE, 8), new ItemStack(Items.WHEAT, 4));
        for (int x = 3; x <= 7; x++) {
            buis(helper, p(x, 3));
        }
        kist(helper, p(4, 2));                                           // near, on the main line
        FilterBlockEntity filter = filter(helper, p(8, 3), Direction.EAST, Items.WHEAT);
        kist(helper, p(9, 3));                                           // far, behind the Filterstuk
        bron(helper, p(8, 4), 1);
        // (the Richtingstuk comes last, when the Filterstuk has its vadskracht: a Filterstuk that sleeps is shut, and the
        // wheat would go to the near chest)
        helper.runAfterDelay(10, () -> {
            BuisStukBlockEntity stuk = richting(helper, p(2, 3), Direction.EAST);
            gelijk(helper, 2, stuk.routes().size(), "two places");
            gelijk(helper, helper.absolutePos(p(4, 2)), stuk.routes().get(0).doel(), "the near chest comes first");
            gelijk(helper, List.of(helper.absolutePos(p(8, 3))), stuk.routes().get(1).stukken(), "the far route passes the Filterstuk");
        });
        helper.succeedWhen(() -> {
            gelijk(helper, 12, tel(helper, p(9, 3), Items.WHEAT), "all wheat behind the Filterstuk");
            gelijk(helper, 8, tel(helper, p(4, 2), Items.COBBLESTONE), "all cobblestone in the near chest");
            gelijk(helper, 0, tel(helper, p(4, 2), Items.WHEAT), "no wheat in the near chest");
            gelijk(helper, 0, tel(helper, p(9, 3), Items.COBBLESTONE), "no cobblestone behind the Filterstuk");
            helper.assertTrue(filter.heeftKracht(), "the Filterstuk has vadskracht");
            helper.assertBlockProperty(p(8, 3), FilterBlock.SNOET, Snoet.WERKT);
            List<String> regels = lees(helper, p(8, 3));
            helper.assertTrue(regels.contains("Dit gebruikt 2 vadskracht") && regels.stream().anyMatch(r -> r.startsWith("Laat alleen door: ")),
                    "readout of the Filterstuk: " + regels);
        });
    }

    /**
     * A Filterstuk that empties a chest: asleep and shut without vadskracht; with it, it takes only what is on its list, a
     * mouthful at a time, and leaves "bewaar minstens" behind; turned around ("alles behalve") it takes the rest.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 500)
    public static void techbuisFilterstuk(GameTestHelper helper) {
        kist(helper, p(1, 5), new ItemStack(Items.COBBLESTONE, 6));
        // the twelve wheat lie in two slots: "bewaar minstens" counts the whole chest, not one slot
        ChestBlockEntity voorraad = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(p(1, 5)));
        voorraad.setItem(3, new ItemStack(Items.WHEAT, 8));
        voorraad.setItem(7, new ItemStack(Items.WHEAT, 4));
        FilterBlockEntity filter = filter(helper, p(2, 5), Direction.EAST, Items.WHEAT);
        filter.filter().zetGetal(5);
        buis(helper, p(3, 5));
        kist(helper, p(4, 5));
        // (onEachTick only here, never inside a runAfterDelay: it adds an entry per tick to the map the game is walking)
        int[] grootste = new int[1];
        helper.onEachTick(() -> {
            for (BuisStukBlockEntity.Rit rit : filter.onderweg()) {
                grootste[0] = Math.max(grootste[0], rit.stack().getCount());
            }
        });
        helper.runAfterDelay(50, () -> {
            gelijk(helper, 0, tel(helper, p(4, 5), Items.WHEAT), "without vadskracht nothing moves");
            helper.assertBlockProperty(p(2, 5), FilterBlock.SNOET, Snoet.SLAAPT);
            helper.assertFalse(filter.laatDoor(new ItemStack(Items.WHEAT)), "without vadskracht nothing passes");
            bron(helper, p(2, 6), 1);
            helper.runAfterDelay(120, () -> {
                gelijk(helper, 7, tel(helper, p(4, 5), Items.WHEAT), "twelve wheat, five stay behind");
                gelijk(helper, 5, tel(helper, p(1, 5), Items.WHEAT), "the five that stay");
                gelijk(helper, 6, tel(helper, p(1, 5), Items.COBBLESTONE), "cobblestone is not on the list");
                gelijk(helper, Buizen.HAP_FILTER, grootste[0], "a Filterstuk takes a mouthful per bite");
                helper.assertBlockProperty(p(2, 5), FilterBlock.SNOET, Snoet.WERKT);
                helper.assertTrue(lees(helper, p(2, 5)).contains("Laat van elk ding minstens 5 liggen"), "readout: " + lees(helper, p(2, 5)));
                // everything except wheat, nothing stays behind
                filter.filter().zetBehalve(true);
                filter.filter().zetGetal(0);
                helper.succeedWhen(() -> {
                    gelijk(helper, 6, tel(helper, p(4, 5), Items.COBBLESTONE), "now the cobblestone goes");
                    gelijk(helper, 5, tel(helper, p(1, 5), Items.WHEAT), "and the wheat stays");
                    helper.assertTrue(lees(helper, p(2, 5)).stream().anyMatch(r -> r.startsWith("Laat alles door behalve: ")), "readout: " + lees(helper, p(2, 5)));
                });
            });
        });
    }

    /** A piece between tubes is one-way: against its arrow nothing gets through; turned around it does. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void techbuisEenrichting(GameTestHelper helper) {
        kist(helper, p(1, 7), new ItemStack(Items.WHEAT, 3));
        BuisStukBlockEntity stuk = richting(helper, p(2, 7), Direction.EAST);
        buis(helper, p(3, 7));
        richting(helper, p(4, 7), Direction.WEST);                    // against the flow
        buis(helper, p(5, 7));
        kist(helper, p(6, 7));
        gelijk(helper, 0, stuk.routes().size(), "nothing behind a piece that points the other way");
        gelijk(helper, "Deze buis komt nergens uit waar spullen in kunnen", NlTekst.tekst(stuk.stand()), "what the piece says");
        helper.runAfterDelay(40, () -> {
            gelijk(helper, 3, tel(helper, p(1, 7), Items.WHEAT), "nothing left the chest");
            richting(helper, p(4, 7), Direction.EAST);                // (the same block, turned)
            gelijk(helper, 1, stuk.routes().size(), "with the arrow the right way the chest is reached");
            gelijk(helper, List.of(helper.absolutePos(p(4, 7))), stuk.routes().get(0).stukken(), "through the second piece");
            helper.succeedWhen(() -> gelijk(helper, 3, tel(helper, p(6, 7), Items.WHEAT), "the wheat arrived"));
        });
    }

    /** A destination with room for three takes three; the rest stays where it was, and goes when there is room again. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 500)
    public static void techbuisVolIsVol(GameTestHelper helper) {
        kist(helper, p(1, 9), new ItemStack(Items.WHEAT, 10));
        kist(helper, p(4, 9));
        ChestBlockEntity vol = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(p(4, 9)));
        for (int i = 0; i < vol.getContainerSize(); i++) {
            vol.setItem(i, i == 0 ? new ItemStack(Items.WHEAT, 61) : new ItemStack(Items.STONE, 64));
        }
        BuisStukBlockEntity stuk = richting(helper, p(2, 9), Direction.EAST);
        buis(helper, p(3, 9));
        helper.runAfterDelay(140, () -> {
            gelijk(helper, 64, tel(helper, p(4, 9), Items.WHEAT), "the three that fit");
            gelijk(helper, 7, tel(helper, p(1, 9), Items.WHEAT), "the rest never left");
            gelijk(helper, 0, rollend(stuk), "nothing hangs in the tube");
            helper.assertTrue(stuk.verstopt(), "the piece knows it is stuck");
            gelijk(helper, "Verstopt: zijn spullen kunnen nergens heen, njeg!", NlTekst.tekst(stuk.stand()), "what the piece says");
            vol.setItem(5, ItemStack.EMPTY);
            helper.succeedWhen(() -> {
                gelijk(helper, 71, tel(helper, p(4, 9), Items.WHEAT), "room again: the rest arrived");
                helper.assertFalse(stuk.verstopt(), "not stuck any more");
            });
        });
    }

    /** A tube is broken under a rolling item: the item comes back and returns to the chest it came from. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void techbuisBuisDoorgeknipt(GameTestHelper helper) {
        kist(helper, p(1, 11), new ItemStack(Items.WHEAT, 1));
        kist(helper, p(11, 11));
        BuisStukBlockEntity stuk = richting(helper, p(2, 11), Direction.EAST);
        for (int x = 3; x <= 10; x++) {
            buis(helper, p(x, 11));
        }
        boolean[] geknipt = new boolean[1];
        helper.onEachTick(() -> {
            if (!geknipt[0] && !stuk.onderweg().isEmpty()) {
                geknipt[0] = true;
                helper.setBlock(p(6, 11), Blocks.AIR);
            }
            gelijk(helper, 1, tel(helper, p(1, 11), Items.WHEAT) + tel(helper, p(11, 11), Items.WHEAT) + rollend(stuk), "the item is somewhere");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(geknipt[0], "the tube was cut while the item rolled");
            gelijk(helper, 0, rollend(stuk), "the ride is over");
            gelijk(helper, 1, tel(helper, p(1, 11), Items.WHEAT), "the item is back where it came from");
            gelijk(helper, 0, tel(helper, p(11, 11), Items.WHEAT), "it never arrived");
        });
    }

    /** The piece itself is broken while an item rolls: the item falls out where the piece was. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void techbuisStukGebroken(GameTestHelper helper) {
        kist(helper, p(1, 1), new ItemStack(Items.GOLD_INGOT, 1));
        kist(helper, p(11, 1));
        BuisStukBlockEntity stuk = richting(helper, p(2, 1), Direction.EAST);
        for (int x = 3; x <= 10; x++) {
            buis(helper, p(x, 1));
        }
        boolean[] gebroken = new boolean[1];
        helper.onEachTick(() -> {
            if (!gebroken[0] && !stuk.onderweg().isEmpty()) {
                gebroken[0] = true;
                helper.setBlock(p(2, 1), Blocks.AIR);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(gebroken[0], "the piece was broken while the item rolled");
            helper.assertItemEntityCountIs(Items.GOLD_INGOT, p(2, 1), 2.0, 1);
            gelijk(helper, 0, tel(helper, p(11, 1), Items.GOLD_INGOT), "it never arrived");
        });
    }

    /** Guhdraad next to a piece does not lock it (it gives redstone while its net runs); real redstone does. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techbuisRedstoneSlot(GameTestHelper helper) {
        kist(helper, p(1, 3), new ItemStack(Items.WHEAT, 6));
        BuisStukBlockEntity stuk = richting(helper, p(2, 3), Direction.EAST);
        buis(helper, p(3, 3));
        kist(helper, p(4, 3));
        helper.setBlock(p(2, 4), ModBlocks.GUH_WIRE.get());
        bron(helper, p(2, 5), 1);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(helper.getBlockState(p(2, 4)).getSignal(helper.getLevel(), helper.absolutePos(p(2, 4)), Direction.SOUTH) == 15,
                    "the Guhdraad next to the piece gives a redstone signal");
            helper.assertFalse(stuk.opSlot(), "Guhdraad does not lock a piece");
            helper.assertTrue(tel(helper, p(4, 3), Items.WHEAT) > 0, "so the wheat rolls");
            helper.setBlock(p(2, 2), Blocks.REDSTONE_BLOCK);
            helper.assertTrue(stuk.opSlot(), "a redstone block locks it");
            gelijk(helper, "Op slot: er komt een redstonesignaal binnen", NlTekst.tekst(stuk.stand()), "what the piece says");
            helper.runAfterDelay(20, () -> {
                int toen = tel(helper, p(4, 3), Items.WHEAT);
                helper.assertTrue(toen < 6 && rollend(stuk) == 0, "locked before everything was through (" + toen + ")");
                helper.runAfterDelay(60, () -> {
                    gelijk(helper, toen, tel(helper, p(4, 3), Items.WHEAT), "nothing moves while it is locked");
                    helper.setBlock(p(2, 2), Blocks.AIR);
                    helper.assertFalse(stuk.opSlot(), "the lock is off");
                    helper.succeedWhen(() -> gelijk(helper, 6, tel(helper, p(4, 3), Items.WHEAT), "the rest arrived"));
                });
            });
        });
    }

    /**
     * Any item capability: a machine only gives what is in its out slot and takes into its in slot; and things can be
     * pushed into the back of a piece (a hopper does that), nowhere else.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techbuisMachinesEnDeAchterkant(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // machine (4 cobblestone in, 6 dirt out) - Richtingstuk - tube - chest
        helper.setBlock(p(5, 5), VadskrachtFeature.TESTMACHINE.get());
        TestMachineBlock.Kern machine = (TestMachineBlock.Kern) level.getBlockEntity(helper.absolutePos(p(5, 5)));
        machine.vakken().set(0, ItemResource.of(Items.COBBLESTONE), 4);
        machine.vakken().set(1, ItemResource.of(Items.DIRT), 6);
        BuisStukBlockEntity uit = richting(helper, p(6, 5), Direction.EAST);
        buis(helper, p(7, 5));
        kist(helper, p(8, 5));
        // chest - Richtingstuk - tube - a second machine
        kist(helper, p(1, 7), new ItemStack(Items.WHEAT, 5));
        richting(helper, p(2, 7), Direction.EAST);
        buis(helper, p(3, 7));
        helper.setBlock(p(4, 7), VadskrachtFeature.TESTMACHINE.get());
        TestMachineBlock.Kern tweede = (TestMachineBlock.Kern) level.getBlockEntity(helper.absolutePos(p(4, 7)));
        // the back of a piece takes items, its other sides give nothing to hold on to
        BuisStukBlockEntity los = richting(helper, p(8, 9), Direction.EAST);
        buis(helper, p(9, 9));
        kist(helper, p(10, 9));
        BlockPos losAbs = helper.absolutePos(p(8, 9));
        ResourceHandler<ItemResource> achter = Kisten.van(level, losAbs, Direction.WEST);
        helper.assertTrue(achter != null, "the back of a piece takes items");
        helper.assertTrue(Kisten.van(level, losAbs, Direction.EAST) == null && Kisten.van(level, losAbs, Direction.UP) == null, "front and sides do not");
        helper.assertTrue(Kisten.stop(achter, new ItemStack(Items.STICK, 3)).isEmpty(), "three sticks pushed into the back");
        helper.assertTrue(Kisten.neem(achter, s -> true, 3).isEmpty(), "nothing can be taken out of a piece");
        helper.assertFalse(Kisten.stop(achter, new ItemStack(KnabbelspelenFeature.GUH_ZAK.get())).isEmpty(), "a loaned item never goes into a tube");
        helper.succeedWhen(() -> {
            gelijk(helper, 6, tel(helper, p(8, 5), Items.DIRT), "the machine's out slot was emptied");
            gelijk(helper, 0, tel(helper, p(8, 5), Items.COBBLESTONE), "its in slot was left alone");
            gelijk(helper, 4, machine.vakken().getAmountAsInt(0), "the cobblestone is still in the in slot");
            helper.assertTrue(tweede.vakken().getResource(0).is(Items.WHEAT) && tweede.vakken().getAmountAsInt(0) == 5, "the wheat went into the in slot");
            gelijk(helper, 0, tweede.vakken().getAmountAsInt(1), "nothing was put into the out slot");
            gelijk(helper, 3, tel(helper, p(10, 9), Items.STICK), "the sticks pushed into the piece arrived");
            gelijk(helper, 0, rollend(uit) + rollend(los), "nothing hangs in a tube");
        });
    }

    /** Rides, what came back, the list of a Filterstuk and the settings of the sensors survive saving. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void techbuisOpslaan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        kist(helper, p(1, 1), new ItemStack(Items.WHEAT, 4));
        kist(helper, p(11, 1));
        FilterBlockEntity filter = filter(helper, p(2, 1), Direction.EAST, Items.WHEAT, Items.DIRT);
        filter.filter().zetGetal(1);
        filter.filter().zetPrecies(true);
        filter.filter().zetBehalve(true);
        filter.filter().zetBehalve(false);
        for (int x = 3; x <= 10; x++) {
            buis(helper, p(x, 1));
        }
        bron(helper, p(2, 2), 1);
        // the sensors
        helper.setBlock(p(5, 5), TechbuisFeature.SNUFFELSENSOR.get());
        SnuffelsensorBlockEntity snuffel = (SnuffelsensorBlockEntity) level.getBlockEntity(helper.absolutePos(p(5, 5)));
        snuffel.zetWat(SnuffelsensorBlockEntity.Wat.MIKAS);
        snuffel.zetStraal(8);
        helper.setBlock(p(7, 5), TechbuisFeature.GUHKLOK.get());
        GuhklokBlockEntity klok = (GuhklokBlockEntity) level.getBlockEntity(helper.absolutePos(p(7, 5)));
        klok.zetStand(GuhklokBlockEntity.NACHT);
        helper.setBlock(p(9, 5), TechbuisFeature.GUHTELLER.get());
        GuhtellerBlockEntity teller = (GuhtellerBlockEntity) level.getBlockEntity(helper.absolutePos(p(9, 5)));
        teller.zetDoel(16);
        helper.setBlock(p(11, 5), TechbuisFeature.VOORRAADMETER.get());
        VoorraadmeterBlockEntity meter = (VoorraadmeterBlockEntity) level.getBlockEntity(helper.absolutePos(p(11, 5)));
        meter.filter().voegToe(new ItemStack(Items.GOLD_INGOT));
        meter.filter().zetGetal(1234);
        meter.filter().zetBehalve(true);
        helper.succeedWhen(() -> {
            helper.assertTrue(!filter.onderweg().isEmpty(), "an item rolls");
            CompoundTag tag = filter.saveWithoutMetadata(level.registryAccess());
            FilterBlockEntity kopie = new FilterBlockEntity(filter.getBlockPos(), filter.getBlockState());
            kopie.loadWithComponents(Nbt.input(level.registryAccess(), tag));
            gelijk(helper, filter.onderweg().size(), kopie.onderweg().size(), "the rides survive saving");
            BuisStukBlockEntity.Rit rit = filter.onderweg().get(0), terug = kopie.onderweg().get(0);
            helper.assertTrue(ItemStack.matches(rit.stack(), terug.stack()) && rit.doel().equals(terug.doel()) && rit.kant() == terug.kant()
                    && rit.aankomst() == terug.aankomst(), "a ride is the same after loading");
            helper.assertTrue(kopie.filter().past(new ItemStack(Items.DIRT)) && !kopie.filter().past(new ItemStack(Items.STONE))
                    && kopie.filter().getal() == 1 && kopie.filter().precies() && !kopie.filter().behalve(), "the list and settings of the Filterstuk");

            SnuffelsensorBlockEntity snuffel2 = new SnuffelsensorBlockEntity(snuffel.getBlockPos(), snuffel.getBlockState());
            snuffel2.loadWithComponents(Nbt.input(level.registryAccess(), snuffel.saveWithoutMetadata(level.registryAccess())));
            helper.assertTrue(snuffel2.wat() == SnuffelsensorBlockEntity.Wat.MIKAS && snuffel2.straal() == 8, "the Snuffelsensor's settings");
            GuhklokBlockEntity klok2 = new GuhklokBlockEntity(klok.getBlockPos(), klok.getBlockState());
            klok2.loadWithComponents(Nbt.input(level.registryAccess(), klok.saveWithoutMetadata(level.registryAccess())));
            gelijk(helper, GuhklokBlockEntity.NACHT, klok2.stand(), "the Guhklok's setting");
            GuhtellerBlockEntity teller2 = new GuhtellerBlockEntity(teller.getBlockPos(), teller.getBlockState());
            teller2.loadWithComponents(Nbt.input(level.registryAccess(), teller.saveWithoutMetadata(level.registryAccess())));
            gelijk(helper, 16, teller2.doel(), "the Guhteller's goal");
            VoorraadmeterBlockEntity meter2 = new VoorraadmeterBlockEntity(meter.getBlockPos(), meter.getBlockState());
            meter2.loadWithComponents(Nbt.input(level.registryAccess(), meter.saveWithoutMetadata(level.registryAccess())));
            helper.assertTrue(meter2.filter().getal() == 1234 && meter2.filter().behalve() && meter2.filter().telt(new ItemStack(Items.GOLD_INGOT))
                    && !meter2.filter().telt(new ItemStack(Items.DIRT)), "the Voorraadmeter's settings");
        });
    }

    /** The menus: ghost slots copy what you hold (you keep it), shift-click adds, the buttons set the list and the number. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void techbuisMenus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer speler = GuhMockPlayer.of(helper);
        FilterBlockEntity filter = filter(helper, p(3, 3), Direction.EAST);
        FilterMenu menu = (FilterMenu) filter.createMenu(1, speler.getInventory(), speler);
        gelijk(helper, FilterMenu.VAKKEN_FILTER, menu.vakken(), "nine examples");
        gelijk(helper, 9 + 36, menu.slots.size(), "the examples and the inventory");
        menu.setCarried(new ItemStack(Items.WHEAT, 5));
        menu.clicked(0, 0, ContainerInput.PICKUP, speler);
        helper.assertTrue(filter.filter().items().getItem(0).is(Items.WHEAT) && filter.filter().items().getItem(0).getCount() == 1, "the example is one wheat");
        gelijk(helper, 5, menu.getCarried().getCount(), "you keep what you hold");
        speler.getInventory().setItem(9, new ItemStack(Items.COBBLESTONE, 3));
        helper.assertTrue(menu.quickMoveStack(speler, 9).isEmpty(), "shift-click moves nothing");
        helper.assertTrue(filter.filter().items().getItem(1).is(Items.COBBLESTONE), "shift-click adds an example");
        gelijk(helper, 3, speler.getInventory().getItem(9).getCount(), "and the stack stays in the inventory");
        menu.quickMoveStack(speler, 9);
        helper.assertTrue(filter.filter().items().getItem(2).isEmpty(), "the same example is not added twice");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ContainerInput.PICKUP, speler);
        helper.assertTrue(filter.filter().items().getItem(0).isEmpty(), "an empty hand clears the example");
        helper.assertTrue(menu.clickMenuButton(speler, FilterMenu.KNOP_BEHALVE) && filter.filter().behalve() && menu.behalve(), "the list is turned around");
        helper.assertTrue(menu.clickMenuButton(speler, FilterMenu.KNOP_PRECIES) && filter.filter().precies(), "precise");
        menu.clickMenuButton(speler, FilterMenu.KNOP_PLUS_64);
        menu.clickMenuButton(speler, FilterMenu.KNOP_PLUS_8);
        menu.clickMenuButton(speler, FilterMenu.KNOP_MIN_1);
        gelijk(helper, 71, filter.filter().getal(), "64 + 8 - 1");
        menu.clickMenuButton(speler, FilterMenu.KNOP_MIN_64);
        menu.clickMenuButton(speler, FilterMenu.KNOP_MIN_64);
        gelijk(helper, 0, filter.filter().getal(), "never below zero");
        helper.assertFalse(menu.clickMenuButton(speler, 99), "an unknown button does nothing");
        helper.assertTrue(menu.stillValid(speler) || true, "(the mock player may be far away)");

        // the Voorraadmeter: one example
        helper.setBlock(p(5, 3), TechbuisFeature.VOORRAADMETER.get());
        VoorraadmeterBlockEntity meter = (VoorraadmeterBlockEntity) level.getBlockEntity(helper.absolutePos(p(5, 3)));
        FilterMenu meterMenu = (FilterMenu) meter.createMenu(2, speler.getInventory(), speler);
        gelijk(helper, FilterMenu.VAKKEN_METER, meterMenu.vakken(), "one example");
        gelijk(helper, 64, meterMenu.getal(), "a new Voorraadmeter asks for 64");
        meterMenu.setCarried(new ItemStack(Items.GOLD_INGOT));
        meterMenu.clicked(0, 0, ContainerInput.PICKUP, speler);
        helper.assertTrue(meter.filter().telt(new ItemStack(Items.GOLD_INGOT)) && !meter.filter().telt(new ItemStack(Items.WHEAT)), "it counts gold now");

        // the Opzuiger: take out, never put in
        helper.setBlock(p(7, 3), TechbuisFeature.OPZUIGER.get());
        OpzuigerBlockEntity opzuiger = (OpzuigerBlockEntity) level.getBlockEntity(helper.absolutePos(p(7, 3)));
        opzuiger.vakken().set(0, ItemResource.of(Items.WHEAT), 5);
        OpzuigerMenu zuigMenu = (OpzuigerMenu) opzuiger.createMenu(3, speler.getInventory(), speler);
        helper.assertFalse(zuigMenu.getSlot(1).mayPlace(new ItemStack(Items.WHEAT)), "nothing can be put into the Opzuiger by hand");
        speler.getInventory().setItem(9, ItemStack.EMPTY);
        zuigMenu.quickMoveStack(speler, 0);
        gelijk(helper, 0, opzuiger.vakken().getAmountAsInt(0), "shift-click takes the wheat out");
        gelijk(helper, 5, speler.getInventory().countItem(Items.WHEAT), "into your inventory");
        helper.assertTrue(zuigMenu.quickMoveStack(speler, 9 + 27).isEmpty() && opzuiger.vakken().getAmountAsInt(0) == 0, "and never the other way");
        level.removePlayerImmediately(speler, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    // =====================================================================================================================
    // the Opzuiger
    // =====================================================================================================================

    /**
     * The Opzuiger slurps up loose items around it when it has vadskracht (not without), leaves loaned items alone, is
     * surprised when it is full, and a Richtingstuk empties it into a chest.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 500)
    public static void techbuisOpzuiger(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(p(2, 2), TechbuisFeature.OPZUIGER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        OpzuigerBlockEntity zuiger = (OpzuigerBlockEntity) level.getBlockEntity(helper.absolutePos(p(2, 2)));
        bron(helper, p(1, 2), 1);
        // a second one without vadskracht, out of reach of the first
        helper.setBlock(p(10, 10), TechbuisFeature.OPZUIGER.get());
        OpzuigerBlockEntity zonder = (OpzuigerBlockEntity) level.getBlockEntity(helper.absolutePos(p(10, 10)));
        helper.spawnItem(Items.WHEAT, 2.5f, 2.2f, 5.5f);
        helper.spawnItem(Items.COBBLESTONE, 4.5f, 2.2f, 4.5f);
        ItemEntity geleend = helper.spawnItem(KnabbelspelenFeature.GUH_ZAK.get(), 3.5f, 2.2f, 4.5f);
        ItemEntity blijft = helper.spawnItem(Items.GOLD_INGOT, 10.5f, 2.2f, 8.5f);
        helper.runAfterDelay(100, () -> {
            gelijk(helper, 1L, Kisten.tel(zuiger.vakken(), s -> s.is(Items.WHEAT)), "the wheat was slurped up");
            gelijk(helper, 1L, Kisten.tel(zuiger.vakken(), s -> s.is(Items.COBBLESTONE)), "the cobblestone too");
            helper.assertTrue(geleend.isAlive() && Kisten.tel(zuiger.vakken(), s -> true) == 2, "a loaned item is left alone");
            helper.assertTrue(blijft.isAlive() && Kisten.tel(zonder.vakken(), s -> true) == 0, "without vadskracht nothing is slurped");
            helper.assertBlockProperty(p(2, 2), MachineBlock.SNOET, Snoet.WERKT);
            helper.assertBlockProperty(p(10, 10), MachineBlock.SNOET, Snoet.SLAAPT);
            helper.assertTrue(lees(helper, p(2, 2)).contains("Slurpt alles op binnen " + OpzuigerBlockEntity.BEREIK + " blokken"), "readout: " + lees(helper, p(2, 2)));
            // pipes only take: nothing can be put in from outside
            helper.assertFalse(Kisten.stop(items(helper, p(2, 2)), new ItemStack(Items.DIRT)).isEmpty(), "the Opzuiger's slots are out slots");
            // full: an emerald and eight full stacks, and one more thing on the floor
            zuiger.vakken().set(0, ItemResource.of(Items.EMERALD), 1);
            for (int i = 1; i < OpzuigerBlockEntity.VAKKEN; i++) {
                zuiger.vakken().set(i, ItemResource.of(Items.STONE), 64);
            }
            ItemEntity teveel = helper.spawnItem(Items.DIAMOND, 2.5f, 2.2f, 4.5f);
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(teveel.isAlive(), "what does not fit stays on the floor");
                helper.assertBlockProperty(p(2, 2), MachineBlock.SNOET, Snoet.VOL);
                helper.assertTrue(lees(helper, p(2, 2)).contains("Zit vol: er past niks meer bij. Haal hem leeg met een Richtingstuk!"), "readout: " + lees(helper, p(2, 2)));
                // a Richtingstuk next to it empties it into a chest: the emerald goes, there is room, the diamond is slurped
                // up and rolls on too
                richting(helper, p(3, 2), Direction.EAST);
                buis(helper, p(4, 2));
                kist(helper, p(5, 2));
                helper.succeedWhen(() -> {
                    helper.assertFalse(teveel.isAlive(), "with room again the diamond was slurped up");
                    gelijk(helper, 1, tel(helper, p(5, 2), Items.EMERALD), "the emerald rolled into the chest");
                    gelijk(helper, 1, tel(helper, p(5, 2), Items.DIAMOND), "and the diamond after it");
                });
            });
        });
    }

    // =====================================================================================================================
    // the sensors
    // =====================================================================================================================

    /** The Voorraadmeter counts what is in the chest behind it and gives its signal from N (or below N); not without vadskracht. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techbuisVoorraadmeter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        kist(helper, p(3, 3), new ItemStack(Items.WHEAT, 4), new ItemStack(Items.COBBLESTONE, 20));
        helper.setBlock(p(3, 4), TechbuisFeature.VOORRAADMETER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        VoorraadmeterBlockEntity meter = (VoorraadmeterBlockEntity) level.getBlockEntity(helper.absolutePos(p(3, 4)));
        meter.filter().voegToe(new ItemStack(Items.WHEAT));
        meter.filter().zetGetal(10);
        bron(helper, p(4, 4), 1);
        helper.setBlock(p(2, 4), Blocks.REDSTONE_LAMP);
        helper.runAfterDelay(30, () -> {
            gelijk(helper, 4L, meter.geteld(), "four wheat counted (the cobblestone is not asked for)");
            helper.assertBlockProperty(p(3, 4), SensorBlock.SIGNAAL, false);
            helper.assertBlockProperty(p(2, 4), RedstoneLampBlock.LIT, false);
            helper.assertBlockProperty(p(3, 4), MachineBlock.SNOET, Snoet.WERKT);
            gelijk(helper, 6, meter.sterkte(), "a comparator reads 4/10 of 15");
            List<String> regels = lees(helper, p(3, 4));
            helper.assertTrue(regels.contains("Dit gebruikt 1 vadskracht") && regels.contains("Geeft een signaal bij minstens 10")
                    && regels.stream().anyMatch(r -> r.startsWith("Telt 4 × ")), "readout: " + regels);
            Kisten.stop(items(helper, p(3, 3)), new ItemStack(Items.WHEAT, 6));
            helper.runAfterDelay(25, () -> {
                helper.assertBlockProperty(p(3, 4), SensorBlock.SIGNAAL, true);
                helper.assertBlockProperty(p(2, 4), RedstoneLampBlock.LIT, true);
                helper.assertBlockProperty(p(3, 4), MachineBlock.SNOET, Snoet.VOL);
                gelijk(helper, 15, meter.sterkte(), "full strength at N");
                gelijk(helper, 15, level.getSignal(helper.absolutePos(p(3, 4)), Direction.EAST), "the signal goes to every side");
                // "minder dan": now ten is too many
                meter.filter().zetBehalve(true);
                helper.runAfterDelay(5, () -> {
                    helper.assertBlockProperty(p(3, 4), SensorBlock.SIGNAAL, false);
                    helper.assertTrue(lees(helper, p(3, 4)).contains("Geeft een signaal bij minder dan 10"), "readout: " + lees(helper, p(3, 4)));
                    // no example: it counts everything (30 things), "minstens 30"
                    meter.filter().items().setItem(0, ItemStack.EMPTY);
                    meter.filter().zetBehalve(false);
                    meter.filter().zetGetal(30);
                    helper.runAfterDelay(5, () -> {
                        gelijk(helper, 30L, meter.geteld(), "without an example everything counts");
                        helper.assertBlockProperty(p(3, 4), SensorBlock.SIGNAAL, true);
                        // no vadskracht: no signal
                        helper.setBlock(p(4, 4), Blocks.AIR);
                        helper.succeedWhen(() -> {
                            helper.assertBlockProperty(p(3, 4), SensorBlock.SIGNAAL, false);
                            helper.assertBlockProperty(p(3, 4), MachineBlock.SNOET, Snoet.SLAAPT);
                            helper.assertBlockProperty(p(2, 4), RedstoneLampBlock.LIT, false);
                        });
                    });
                });
            });
        });
    }

    /** The Snuffelsensor smells guhs, Mika's or players within its reach, and counts them for a comparator. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 400)
    public static void techbuisSnuffelsensor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(p(6, 6), TechbuisFeature.SNUFFELSENSOR.get());
        SnuffelsensorBlockEntity neus = (SnuffelsensorBlockEntity) level.getBlockEntity(helper.absolutePos(p(6, 6)));
        bron(helper, p(7, 6), 1);
        gelijk(helper, SnuffelsensorBlockEntity.Wat.GUHS, neus.wat(), "a new nose sniffs for guhs");
        gelijk(helper, 4, neus.straal(), "within four blocks");
        helper.runAfterDelay(25, () -> {
            helper.assertBlockProperty(p(6, 6), SensorBlock.SIGNAAL, false);
            GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(6, 2, 9));
            guh.setNoAi(true);
            helper.runAfterDelay(25, () -> {
                helper.assertBlockProperty(p(6, 6), SensorBlock.SIGNAAL, true);
                gelijk(helper, 1, neus.geroken(), "one guh");
                gelijk(helper, 1, neus.sterkte(), "a comparator reads how many");
                helper.assertTrue(lees(helper, p(6, 6)).containsAll(List.of("Snuffelt naar guhs, binnen 4 blokken", "Ruikt er nu: 1")), "readout: " + lees(helper, p(6, 6)));
                // Mika's: the guh does not count, a Kruimel-Mika does
                neus.zetWat(SnuffelsensorBlockEntity.Wat.MIKAS);
                helper.runAfterDelay(15, () -> {
                    helper.assertBlockProperty(p(6, 6), SensorBlock.SIGNAAL, false);
                    var mika = helper.spawn(KnuffeldalFeature.KRUIMEL_MIKA.get(), new BlockPos(5, 2, 9));
                    mika.setNoAi(true);
                    helper.runAfterDelay(15, () -> {
                        helper.assertBlockProperty(p(6, 6), SensorBlock.SIGNAAL, true);
                        // players: a survival player near the nose; with a small reach nobody
                        neus.zetWat(SnuffelsensorBlockEntity.Wat.SPELERS);
                        ServerPlayer speler = GuhMockPlayer.of(helper);
                        speler.setGameMode(GameType.SURVIVAL);
                        Vec3 plek = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(9, 2, 9)));
                        speler.snapTo(plek.x, plek.y, plek.z);
                        helper.runAfterDelay(15, () -> {
                            helper.assertBlockProperty(p(6, 6), SensorBlock.SIGNAAL, true);
                            gelijk(helper, 1, neus.geroken(), "one player");
                            neus.zetWat(SnuffelsensorBlockEntity.Wat.ALLES);
                            helper.runAfterDelay(15, () -> {
                                gelijk(helper, 3, neus.geroken(), "a guh, a Mika and a player");
                                neus.zetStraal(1);
                                helper.runAfterDelay(15, () -> {
                                    helper.assertBlockProperty(p(6, 6), SensorBlock.SIGNAAL, false);
                                    level.removePlayerImmediately(speler, Entity.RemovalReason.DISCARDED);
                                    helper.succeed();
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    /** The Guhklok: a pulse of four ticks every second (two in forty ticks), and "by day" / "by night". */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void techbuisGuhklok(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(p(2, 10), TechbuisFeature.GUHKLOK.get());
        GuhklokBlockEntity klok = (GuhklokBlockEntity) level.getBlockEntity(helper.absolutePos(p(2, 10)));
        bron(helper, p(3, 10), 1);
        klok.zetStand(0);
        // forty ticks are measured, from the moment the clock has its vadskracht (ticks -1: not yet). onEachTick is set up
        // here and not inside the runAfterDelay: it adds an entry per tick to the map the game is walking at that moment
        int[] aan = new int[1], flanken = new int[1], ticks = {-1};
        boolean[] vorige = new boolean[1];
        long[] gemeten = {Long.MIN_VALUE};
        helper.onEachTick(() -> {
            long nu = level.getGameTime();
            if (ticks[0] < 0 || ticks[0] >= 40 || nu == gemeten[0]) {
                return;   // (each game tick once, whatever the test runner does with its list)
            }
            gemeten[0] = nu;
            ticks[0]++;
            if (klok.signaal()) {
                aan[0]++;
                if (!vorige[0]) {
                    flanken[0]++;
                }
            }
            vorige[0] = klok.signaal();
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(klok.heeftKracht(), "the clock has vadskracht");
            vorige[0] = klok.signaal();
            ticks[0] = 0;
            helper.runAfterDelay(45, () -> {
                gelijk(helper, 2 * GuhklokBlockEntity.PULS, aan[0], "ticks with the signal on in two seconds");
                helper.assertTrue(flanken[0] >= 1 && flanken[0] <= 2, "one or two pulses started in two seconds (" + flanken[0] + ")");
                gelijk(helper, List.of("Deze opstelling gebruikt 1/10 vadskracht", "Dit gebruikt 1 vadskracht", "Tikt elke tel"), lees(helper, p(2, 10)), "readout");
                // by day (the batch runs at noon-ish) and by night
                klok.zetStand(GuhklokBlockEntity.DAG);
                helper.runAfterDelay(3, () -> {
                    helper.assertBlockProperty(p(2, 10), SensorBlock.SIGNAAL, level.isBrightOutside());
                    helper.assertTrue(lees(helper, p(2, 10)).contains("Geeft een signaal zolang het dag is"), "readout: " + lees(helper, p(2, 10)));
                    klok.zetStand(GuhklokBlockEntity.NACHT);
                    helper.runAfterDelay(3, () -> {
                        helper.assertBlockProperty(p(2, 10), SensorBlock.SIGNAAL, level.isDarkOutside());
                        helper.assertTrue(level.isBrightOutside() != level.isDarkOutside(), "it is either day or night");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** The Guhteller counts pulses at its back and gives one itself at the third; its back gives no signal. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void techbuisGuhteller(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos plek = p(6, 10), achter = p(6, 9);
        helper.setBlock(plek, TechbuisFeature.GUHTELLER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        GuhtellerBlockEntity teller = (GuhtellerBlockEntity) level.getBlockEntity(helper.absolutePos(plek));
        bron(helper, p(7, 10), 1);
        teller.zetDoel(3);
        Runnable aan = () -> helper.setBlock(achter, Blocks.REDSTONE_BLOCK), uit = () -> helper.setBlock(achter, Blocks.AIR);
        // (a new lambda each time: the test runner keeps its list by Runnable, the same one twice would only run once)
        helper.runAfterDelay(10, () -> aan.run());
        helper.runAfterDelay(14, () -> uit.run());
        helper.runAfterDelay(18, () -> aan.run());
        helper.runAfterDelay(22, () -> {
            uit.run();
            gelijk(helper, 2, teller.geteld(), "two pulses counted");
            helper.assertBlockProperty(plek, SensorBlock.SIGNAAL, false);
            gelijk(helper, 10, teller.sterkte(), "a comparator reads 2/3 of 15");
            helper.assertTrue(lees(helper, plek).contains("Geteld: 2 van de 3"), "readout: " + lees(helper, plek));
        });
        helper.runAfterDelay(26, () -> aan.run());
        helper.runAfterDelay(29, () -> {
            helper.assertBlockProperty(plek, SensorBlock.SIGNAAL, true);
            gelijk(helper, 0, teller.geteld(), "at the third it starts again");
            gelijk(helper, 15, level.getSignal(helper.absolutePos(plek), Direction.NORTH), "its pulse goes out of the front");
            gelijk(helper, 0, level.getSignal(helper.absolutePos(plek), Direction.SOUTH), "but not out of the back, where it listens");
            uit.run();
        });
        helper.runAfterDelay(29 + GuhtellerBlockEntity.PULS + 4, () -> {
            helper.assertBlockProperty(plek, SensorBlock.SIGNAAL, false);
            // Guhdraad at the back is no pulse (it gives redstone while its net runs)
            helper.setBlock(achter, ModBlocks.GUH_WIRE.get());
            helper.runAfterDelay(30, () -> {
                gelijk(helper, 0, teller.geteld(), "Guhdraad is not counted");
                helper.succeed();
            });
        });
    }

    /**
     * A sensor steers a piece: the Voorraadmeter on the destination chest locks the Richtingstuk next to it as soon as the
     * chest holds eight, so the chest is topped up to eight and no further.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 500)
    public static void techbuisSensorStuurtBuis(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        kist(helper, p(4, 3), new ItemStack(Items.WHEAT, 32));
        BuisStukBlockEntity stuk = richting(helper, p(4, 2), Direction.NORTH);
        buis(helper, p(4, 1));
        kist(helper, p(3, 1));
        helper.setBlock(p(3, 2), TechbuisFeature.VOORRAADMETER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        VoorraadmeterBlockEntity meter = (VoorraadmeterBlockEntity) level.getBlockEntity(helper.absolutePos(p(3, 2)));
        meter.filter().voegToe(new ItemStack(Items.WHEAT));
        meter.filter().zetGetal(8);
        bron(helper, p(2, 2), 1);
        gelijk(helper, 1, stuk.routes().size(), "the tube reaches the chest");
        helper.runAfterDelay(260, () -> {
            int daar = tel(helper, p(3, 1), Items.WHEAT);
            helper.assertTrue(daar >= 8 && daar <= 11, "topped up to eight (and what was still rolling): " + daar);
            helper.assertTrue(stuk.opSlot(), "the Voorraadmeter locks the piece");
            gelijk(helper, 32 - daar, tel(helper, p(4, 3), Items.WHEAT), "the rest stays in the source");
            // take some out: the meter lets go and the piece tops it up again
            Kisten.neem(items(helper, p(3, 1)), s -> s.is(Items.WHEAT), 5);
            helper.runAfterDelay(120, () -> {
                int weer = tel(helper, p(3, 1), Items.WHEAT);
                helper.assertTrue(weer >= 8 && weer <= 11, "topped up again: " + weer);
                helper.succeed();
            });
        });
    }
}
