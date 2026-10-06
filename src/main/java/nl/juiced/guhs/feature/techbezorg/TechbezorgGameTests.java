package nl.juiced.guhs.feature.techbezorg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.techbezorg.StepstationBlockEntity.Fase;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.NlTekst;

/**
 * Game tests of the Bezorgguhtje (bbq2, batch "techbezorg"): a round from chest to chest with a filter (also with the
 * "afleveren" stop first in the round), sleeping without
 * vadskracht, taking only what can be delivered, a furnace (what is done comes out, the fuel stays), nothing is ever lost
 * (the guhtje killed on the road, a stranger that claims the station, the station broken with a full backpack, save and
 * load), linking poles to stations, hopping over a wall, the whistle, the two menus, the hover readout and the texts.
 * Template techbezorg_test_kamer: 15 x 6 x 15 with a stone floor (things stand at helper y 2).
 * <p>
 * The tests of one batch stand next to each other in the same world and a pole looks for a station within 96 blocks, so
 * every test links its poles to its own station by hand ({@code koppel}); where the looking itself is tested the test's
 * own station is the nearest one by far.
 */
public class TechbezorgGameTests {
    private static final String BATCH = "techbezorg", KAMER = "techbezorg_test_kamer";

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** A spot on the floor of the test room. */
    static BlockPos p(int x, int z) {
        return new BlockPos(x, 2, z);
    }

    static Item knabbel() {
        return ModItems.KAAS_KNABBELS.get();
    }

    /** A Stepstation that faces this way, with a test source (10 VK) behind it when it should have vadskracht. */
    static StepstationBlockEntity station(GameTestHelper helper, BlockPos pos, Direction voor, boolean kracht) {
        if (kracht) {
            bron(helper, pos.relative(voor.getOpposite()));
        }
        helper.setBlock(pos, TechbezorgFeature.STEPSTATION.get().defaultBlockState().setValue(MachineBlock.FACING, voor));
        return (StepstationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    static void bron(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 1));
    }

    static ChestBlockEntity kist(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    /** A pole here that serves the block on this side; linked to the station when one is given. */
    static HaltepaaltjeBlockEntity paal(GameTestHelper helper, BlockPos pos, Direction kant, boolean ophalen, @Nullable StepstationBlockEntity station) {
        helper.setBlock(pos, TechbezorgFeature.HALTEPAALTJE.get().defaultBlockState().setValue(HaltepaaltjeBlock.FACING, kant)
                .setValue(HaltepaaltjeBlock.OPHALEN, ophalen));
        HaltepaaltjeBlockEntity halte = (HaltepaaltjeBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (station != null) {
            helper.assertTrue(station.koppel(halte), "the pole at " + pos + " joins the station");
        }
        return halte;
    }

    static int tel(Container kist, Item item) {
        return kist.countItem(item);
    }

    static void gelijk(GameTestHelper helper, Object verwacht, Object echt, String wat) {
        helper.assertTrue(verwacht.equals(echt), wat + ": expected " + verwacht + ", got " + echt);
    }

    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            p.closeContainer();
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static List<String> lees(GameTestHelper helper, BlockPos pos) {
        return VadsKracht.regels(helper.getLevel(), helper.absolutePos(pos)).stream().map(NlTekst::tekst).toList();
    }

    /** The usual round: a station in the middle, chest A with an "ophalen" pole on the left, chest B with an "afleveren" pole on the right. */
    private record Ronde(StepstationBlockEntity station, ChestBlockEntity a, ChestBlockEntity b, HaltepaaltjeBlockEntity paalA, HaltepaaltjeBlockEntity paalB) {
    }

    private static Ronde ronde(GameTestHelper helper, boolean kracht) {
        StepstationBlockEntity station = station(helper, p(7, 7), Direction.NORTH, kracht);
        ChestBlockEntity a = kist(helper, p(2, 3)), b = kist(helper, p(12, 3));
        HaltepaaltjeBlockEntity paalA = paal(helper, p(2, 4), Direction.NORTH, true, station);
        HaltepaaltjeBlockEntity paalB = paal(helper, p(12, 4), Direction.NORTH, false, station);
        return new Ronde(station, a, b, paalA, paalB);
    }

    // =====================================================================================================================
    // the round
    // =====================================================================================================================

    /** Chest A -> the backpack -> chest B: only what B's filter asks for; the rest stays; the backpack ends empty. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 800)
    public static void techbezorgBrengtVanKistNaarKist(GameTestHelper helper) {
        Ronde r = ronde(helper, true);
        r.a.setItem(0, new ItemStack(knabbel(), 40));
        r.a.setItem(1, new ItemStack(Items.COBBLESTONE, 20));
        r.paalB.zetFilter(0, new ItemStack(knabbel()));
        gelijk(helper, VadsGetallen.STEPSTATION, VadsKracht.net(helper.getLevel(), helper.absolutePos(p(7, 7))).vraag(), "the station asks its vadskracht");
        gelijk(helper, List.of(r.paalA.getBlockPos(), r.paalB.getBlockPos()), r.station.haltes(), "the round");
        helper.succeedWhen(() -> {
            gelijk(helper, 40, tel(r.b, knabbel()), "kaasknabbels in chest B");
            gelijk(helper, 0, tel(r.a, knabbel()), "kaasknabbels left in chest A");
            gelijk(helper, 20, tel(r.a, Items.COBBLESTONE), "cobblestone stays in chest A (nobody asks for it)");
            gelijk(helper, 0, tel(r.b, Items.COBBLESTONE), "cobblestone in chest B");
            helper.assertTrue(r.station.rugzakLeeg(), "the backpack is empty again");
            gelijk(helper, 40L, r.station.gebracht(), "items delivered");
            BezorgguhtjeEntity k = r.station.koerier();
            helper.assertTrue(k != null && !k.slaapt(), "the guhtje is there and awake");
            helper.assertBlockProperty(p(7, 7), MachineBlock.SNOET, Snoet.WERKT);
        });
    }

    /**
     * The "afleveren" stop comes BEFORE the "ophalen" stop in the round: everything still arrives in ONE round (it rides
     * past the "afleveren" stops once more before it goes home), and the face never says "full".
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 800)
    public static void techbezorgAfleverHalteEerst(GameTestHelper helper) {
        StepstationBlockEntity station = station(helper, p(7, 7), Direction.NORTH, true);
        ChestBlockEntity a = kist(helper, p(2, 3)), b = kist(helper, p(12, 3));
        HaltepaaltjeBlockEntity paalB = paal(helper, p(12, 4), Direction.NORTH, false, station);
        HaltepaaltjeBlockEntity paalA = paal(helper, p(2, 4), Direction.NORTH, true, station);
        gelijk(helper, List.of(paalB.getBlockPos(), paalA.getBlockPos()), station.haltes(), "the round: afleveren first");
        a.setItem(0, new ItemStack(knabbel(), 40));
        int[] thuisMetSpullen = {0};
        helper.onEachTick(() -> {
            if (station.fase() == Fase.RUST && !station.rugzakLeeg()) {
                thuisMetSpullen[0]++;
            }
            helper.assertTrue(station.snoet() != Snoet.VOL, "the face never says the backpack does not get empty");
        });
        helper.succeedWhen(() -> {
            gelijk(helper, 40, tel(b, knabbel()), "kaasknabbels in chest B");
            gelijk(helper, Fase.RUST, station.fase(), "home again");
            gelijk(helper, 0, thuisMetSpullen[0], "ticks it rested at home with things still in the backpack");
            helper.assertTrue(station.rugzakLeeg(), "the backpack is empty");
        });
    }

    /** Without vadskracht the guhtje exists but sleeps at home and brings nothing; with vadskracht it starts. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 900)
    public static void techbezorgZonderVadskrachtSlaapt(GameTestHelper helper) {
        Ronde r = ronde(helper, false);
        r.a.setItem(0, new ItemStack(knabbel(), 16));
        helper.runAfterDelay(100, () -> {
            BezorgguhtjeEntity k = r.station.koerier();
            helper.assertTrue(k != null, "the station made its guhtje");
            helper.assertTrue(k.slaapt(), "it sleeps without vadskracht");
            helper.assertTrue(k.isBij(r.station.dok(helper.getLevel())), "it sleeps at its dock");
            gelijk(helper, helper.absolutePos(p(7, 6)), r.station.dok(helper.getLevel()), "the dock is in front of the station");
            gelijk(helper, Fase.SLAAPT, r.station.fase(), "fase");
            helper.assertTrue(r.station.rugzakOpen(), "the backpack can be reached while it is home");
            helper.assertBlockProperty(p(7, 7), MachineBlock.SNOET, Snoet.SLAAPT);
            gelijk(helper, 16, tel(r.a, knabbel()), "nothing was taken");
            List<String> regels = lees(helper, p(7, 7));
            helper.assertTrue(regels.contains("Het Bezorgguhtje slaapt: het Stepstation heeft geen vadskracht. Zzz, njeg."), "readout: " + regels);
            helper.assertTrue(regels.contains("Rugzak: 0/9 stapels"), "readout: " + regels);
            bron(helper, p(7, 8));
            helper.succeedWhen(() -> {
                gelijk(helper, 16, tel(r.b, knabbel()), "kaasknabbels in chest B once there is vadskracht");
                helper.assertTrue(!k.slaapt(), "awake");
            });
        });
    }

    /** B has room for 10: exactly 10 are taken, the backpack does not fill up with what cannot be delivered. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 900)
    public static void techbezorgNeemtAlleenWatPast(GameTestHelper helper) {
        Ronde r = ronde(helper, true);
        r.a.setItem(0, new ItemStack(knabbel(), 64));
        r.a.setItem(1, new ItemStack(Items.DIRT, 64));
        for (int i = 0; i < r.b.getContainerSize(); i++) {
            r.b.setItem(i, i == 0 ? new ItemStack(knabbel(), 54) : new ItemStack(Items.STONE, 64));
        }
        boolean[] klaar = {false};
        helper.onEachTick(() -> helper.assertTrue(r.station.gevuld() <= 1 && tel(r.a, knabbel()) >= 54,
                "it never takes more than chest B can hold (backpack " + r.station.gevuld() + ", chest A " + tel(r.a, knabbel()) + ")"));
        helper.succeedWhen(() -> {
            gelijk(helper, 64, tel(r.b, knabbel()), "kaasknabbels in chest B");
            if (!klaar[0]) {
                klaar[0] = true;
                // ...and it stays that way: nothing more is taken, the face is not "full"
                helper.runAfterDelay(160, () -> {
                    gelijk(helper, 54, tel(r.a, knabbel()), "kaasknabbels left in chest A");
                    gelijk(helper, 64, tel(r.a, Items.DIRT), "the dirt stays (chest B has no room for it)");
                    helper.assertTrue(r.station.rugzakLeeg(), "the backpack is empty");
                    helper.assertBlockProperty(p(7, 7), MachineBlock.SNOET, Snoet.WERKT);
                    gelijk(helper, Fase.RUST, r.station.fase(), "no work: it rests at home");
                    helper.succeed();
                });
            }
            helper.assertTrue(false, "waiting to see that it stays that way");
        });
    }

    /**
     * A furnace: the pole ON it fills what is baked (raw iron), the pole BESIDE it takes what is done (ingots) and leaves the
     * fuel and what still has to bake, also when an "afleveren" stop would like to have those.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 1200)
    public static void techbezorgOven(GameTestHelper helper) {
        StepstationBlockEntity station = station(helper, p(7, 9), Direction.NORTH, true);
        ChestBlockEntity erts = kist(helper, p(2, 3)), klaar = kist(helper, p(12, 3));
        erts.setItem(0, new ItemStack(Items.RAW_IRON, 20));
        helper.setBlock(p(7, 3), Blocks.FURNACE);
        AbstractFurnaceBlockEntity oven = (AbstractFurnaceBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(p(7, 3)));
        oven.setItem(2, new ItemStack(Items.IRON_INGOT, 12));
        // a second furnace with only fuel in it
        helper.setBlock(p(4, 12), Blocks.FURNACE);
        AbstractFurnaceBlockEntity kolen = (AbstractFurnaceBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(p(4, 12)));
        kolen.setItem(1, new ItemStack(Items.COAL, 8));
        paal(helper, p(2, 4), Direction.NORTH, true, station);                                       // takes the raw iron
        HaltepaaltjeBlockEntity opOven = paal(helper, p(7, 3).above(), Direction.DOWN, false, station);   // brings it into the furnace
        opOven.zetFilter(0, new ItemStack(Items.RAW_IRON));
        paal(helper, p(7, 4), Direction.NORTH, true, station);                                       // takes what the furnace made
        paal(helper, p(4, 11), Direction.SOUTH, true, station);                                      // "takes" from the fuel furnace
        HaltepaaltjeBlockEntity bijKlaar = paal(helper, p(12, 4), Direction.NORTH, false, station);
        bijKlaar.zetFilter(0, new ItemStack(Items.IRON_INGOT));
        bijKlaar.zetFilter(1, new ItemStack(Items.COAL));
        boolean[] wacht = {false};
        helper.succeedWhen(() -> {
            gelijk(helper, 12, tel(klaar, Items.IRON_INGOT), "ingots in the chest");
            gelijk(helper, 20, oven.getItem(0).getCount(), "raw iron in the furnace");
            if (!wacht[0]) {
                wacht[0] = true;
                helper.runAfterDelay(200, () -> {
                    gelijk(helper, 20, oven.getItem(0).getCount(), "the raw iron stays in the furnace");
                    gelijk(helper, 8, kolen.getItem(1).getCount(), "the fuel stays in the furnace");
                    gelijk(helper, 0, tel(klaar, Items.COAL), "coal in the chest");
                    gelijk(helper, 0, tel(klaar, Items.RAW_IRON), "raw iron in the chest");
                    helper.assertTrue(station.rugzakLeeg(), "the backpack is empty");
                    helper.succeed();
                });
            }
            helper.assertTrue(false, "waiting to see that the furnaces keep what is theirs");
        });
    }

    /** A stop behind a wall: the guhtje cannot ride there, so it hops in (and out again). */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 1200)
    public static void techbezorgHoptOverDeMuur(GameTestHelper helper) {
        StepstationBlockEntity station = station(helper, p(3, 9), Direction.NORTH, true);
        ChestBlockEntity a = kist(helper, p(2, 3));
        a.setItem(0, new ItemStack(knabbel(), 8));
        paal(helper, p(2, 4), Direction.NORTH, true, station);
        // a walled-in pole at 11,11 with its chest in the wall (walls two high: no way in)
        for (int x = 10; x <= 12; x++) {
            for (int z = 10; z <= 12; z++) {
                if (x != 11 || z != 11) {
                    helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                    helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
                }
            }
        }
        ChestBlockEntity b = kist(helper, p(11, 10));
        paal(helper, p(11, 11), Direction.NORTH, false, station);
        boolean[] binnen = {false};
        helper.onEachTick(() -> {
            BezorgguhtjeEntity k = station.koerier();
            if (k != null && k.blockPosition().equals(helper.absolutePos(p(11, 11)))) {
                binnen[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            gelijk(helper, 8, tel(b, knabbel()), "kaasknabbels behind the wall");
            helper.assertTrue(binnen[0], "the guhtje was inside the walls");
            BezorgguhtjeEntity k = station.koerier();
            helper.assertTrue(k != null && station.fase() == Fase.RUST && k.isBij(station.dok(helper.getLevel())), "and it came home again");
        });
    }

    // =====================================================================================================================
    // nothing is ever lost
    // =====================================================================================================================

    /**
     * The guhtje vanishes on the road with a full backpack: the station makes a new one and everything still arrives. A
     * guhtje that says it belongs to this station but is not the station's own poofs away.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 1200)
    public static void techbezorgNooitKwijt(GameTestHelper helper) {
        Ronde r = ronde(helper, true);
        r.a.setItem(0, new ItemStack(knabbel(), 64));
        r.a.setItem(1, new ItemStack(knabbel(), 64));
        BezorgguhtjeEntity vreemd = helper.spawn(TechbezorgFeature.BEZORGGUHTJE.get(), p(5, 11));
        vreemd.zetStation(helper.absolutePos(p(7, 7)));
        UUID[] eerste = {null};
        helper.onEachTick(() -> {
            BezorgguhtjeEntity k = r.station.koerier();
            if (eerste[0] == null && k != null && r.station.gevuld() > 0 && r.station.fase() == Fase.RIJDT) {
                eerste[0] = k.getUUID();
                gelijk(helper, 2, k.lading(), "the guhtje shows what is in the backpack");
                k.discard();   // gone, however that happened
            }
            helper.assertTrue(tel(r.a, knabbel()) + tel(r.b, knabbel()) + (int) nl.juiced.guhs.feature.vadskracht.Kisten.tel(r.station.vakken(), s -> true) == 128,
                    "not one kaasknabbel is lost or doubled");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(eerste[0] != null, "the first guhtje was removed on the road");
            gelijk(helper, 128, tel(r.b, knabbel()), "kaasknabbels in chest B");
            BezorgguhtjeEntity nieuw = r.station.koerier();
            helper.assertTrue(nieuw != null && !nieuw.getUUID().equals(eerste[0]), "the station made a new guhtje");
            helper.assertTrue(vreemd.isRemoved(), "the stranger poofed away");
            int guhtjes = helper.getLevel().getEntitiesOfClass(BezorgguhtjeEntity.class, helper.getBounds().inflate(2), Entity::isAlive).size();
            gelijk(helper, 1, guhtjes, "guhtjes around");
        });
    }

    /**
     * The station is broken with things in the backpack: they fall out, the guhtje goes, the poles are loose. A new station
     * placed by a player takes the loose poles in.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void techbezorgStationWegSpullenTerug(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Ronde r = ronde(helper, false);
        r.station.vakken().set(0, ItemResource.of(new ItemStack(knabbel())), 64);
        r.station.vakken().set(4, ItemResource.of(new ItemStack(Items.DIAMOND)), 3);
        helper.runAfterDelay(20, () -> {
            BezorgguhtjeEntity k = r.station.koerier();
            helper.assertTrue(k != null, "the guhtje is there");
            gelijk(helper, 2, k.lading(), "stacks on its back");
            level.destroyBlock(helper.absolutePos(p(7, 7)), true);
            helper.assertTrue(k.isRemoved(), "the guhtje went with its station");
            int knabbels = 0, diamanten = 0, stations = 0;
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2))) {
                ItemStack stack = item.getItem();
                knabbels += stack.is(knabbel()) ? stack.getCount() : 0;
                diamanten += stack.is(Items.DIAMOND) ? stack.getCount() : 0;
                stations += stack.is(TechbezorgFeature.STEPSTATION_ITEM.get()) ? stack.getCount() : 0;
                item.discard();
            }
            gelijk(helper, 64, knabbels, "kaasknabbels dropped");
            gelijk(helper, 3, diamanten, "diamonds dropped");
            gelijk(helper, 1, stations, "the station itself dropped");
            helper.assertTrue(r.paalA.station() == null && r.paalA.isLos() && r.paalB.isLos(), "the poles are loose");
            // a player places a new station a little further: the loose poles join it, nearest first
            ServerPlayer speler = speler(helper, p(7, 12));
            StepstationBlockEntity nieuw = station(helper, p(5, 8), Direction.NORTH, false);
            nieuw.zetEigenaar(speler.getUUID());
            nieuw.geplaatst(speler);
            helper.assertTrue(nieuw.haltes().contains(r.paalA.getBlockPos()) && nieuw.haltes().contains(r.paalB.getBlockPos()),
                    "the new station took the loose poles in: " + nieuw.haltes());
            gelijk(helper, nieuw.getBlockPos(), r.paalA.station(), "pole A's station");
            gelijk(helper, 0, nieuw.nummer(r.paalA.getBlockPos()), "the nearest pole is the first stop");
            weg(helper, speler);
            helper.succeed();
        });
    }

    /** What the station, a pole and the guhtje save comes back when they are loaded. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void techbezorgOpslaanEnLaden(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Ronde r = ronde(helper, false);
        r.station.vakken().set(2, ItemResource.of(new ItemStack(knabbel())), 33);
        r.paalB.zetFilter(3, new ItemStack(Items.CARROT, 7));
        helper.runAfterDelay(10, () -> {
            BezorgguhtjeEntity k = r.station.koerier();
            helper.assertTrue(k != null, "the guhtje is there");
            // the station
            CompoundTag tag = r.station.saveWithoutMetadata(level.registryAccess());
            StepstationBlockEntity kopie = new StepstationBlockEntity(r.station.getBlockPos(), r.station.getBlockState());
            kopie.loadWithComponents(Nbt.input(level.registryAccess(), tag));
            gelijk(helper, r.station.haltes(), kopie.haltes(), "the round");
            gelijk(helper, 33, kopie.vakken().getAmountAsInt(2), "the backpack");
            helper.assertTrue(kopie.vakken().getResource(2).is(knabbel()), "the backpack's item");
            helper.assertTrue(kopie.isKoerier(k), "the station knows its guhtje again");
            gelijk(helper, r.station.fase(), kopie.fase(), "fase");
            // a pole
            CompoundTag paalTag = r.paalB.saveWithoutMetadata(level.registryAccess());
            HaltepaaltjeBlockEntity paalKopie = new HaltepaaltjeBlockEntity(r.paalB.getBlockPos(), r.paalB.getBlockState());
            paalKopie.loadWithComponents(Nbt.input(level.registryAccess(), paalTag));
            gelijk(helper, r.station.getBlockPos(), paalKopie.station(), "the pole's station");
            helper.assertTrue(paalKopie.filter(3).is(Items.CARROT) && paalKopie.filter(3).getCount() == 1, "the filter keeps ONE carrot");
            helper.assertTrue(paalKopie.past(new ItemStack(Items.CARROT)) && !paalKopie.past(new ItemStack(Items.APPLE)), "the filter works after loading");
            // the guhtje
            CompoundTag guhTag = Nbt.saveWithoutId(k);
            BezorgguhtjeEntity guhKopie = TechbezorgFeature.BEZORGGUHTJE.get().create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
            Nbt.load(guhKopie, guhTag);
            gelijk(helper, r.station.getBlockPos(), guhKopie.station(), "the guhtje's station");
            guhKopie.discard();
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // poles and stations
    // =====================================================================================================================

    /**
     * A pole placed by a player joins the nearest station; the first takes, the second brings; the order can be changed; a
     * station has room for eight; a broken pole leaves the round; "Ander station" moves a pole to the next station.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void techbezorgHaltesKoppelen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer speler = speler(helper, p(7, 12));
        StepstationBlockEntity station = station(helper, p(7, 7), Direction.NORTH, false);
        kist(helper, p(7, 4));
        HaltepaaltjeBlockEntity een = paal(helper, p(7, 5), Direction.NORTH, true, null);
        helper.assertTrue(een.isLos() && een.station() == null, "a pole is loose until it finds a station");
        een.geplaatst(speler);
        gelijk(helper, station.getBlockPos(), een.station(), "the first pole found the station");
        gelijk(helper, 0, station.nummer(een.getBlockPos()), "its stop");
        helper.assertTrue(een.ophalen() && een.heeftKist(), "the first pole takes things, and it stands at a chest");
        gelijk(helper, speler.getUUID(), een.eigenaar(), "who placed it");
        HaltepaaltjeBlockEntity twee = paal(helper, p(6, 5), Direction.EAST, true, null);
        twee.geplaatst(speler);
        gelijk(helper, 1, station.nummer(twee.getBlockPos()), "the second pole's stop");
        helper.assertTrue(!twee.ophalen(), "the second pole brings things (nothing did yet)");
        helper.assertBlockProperty(p(6, 5), HaltepaaltjeBlock.OPHALEN, false);
        helper.assertTrue(!twee.heeftKist(), "another pole is no chest");
        HaltepaaltjeBlockEntity drie = paal(helper, p(8, 5), Direction.DOWN, true, null);
        drie.geplaatst(speler);
        helper.assertTrue(drie.ophalen(), "the third pole takes things again");
        // the order of the round
        helper.assertTrue(station.schuif(2, -1), "stop 3 moves one earlier");
        gelijk(helper, List.of(een.getBlockPos(), drie.getBlockPos(), twee.getBlockPos()), station.haltes(), "the round after moving");
        helper.assertTrue(!station.schuif(0, -1) && !station.schuif(2, 1), "the first cannot go earlier, the last not later");
        // room for eight
        List<HaltepaaltjeBlockEntity> meer = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            meer.add(paal(helper, p(2 + i, 2), Direction.DOWN, true, null));
        }
        for (int i = 0; i < 5; i++) {
            helper.assertTrue(station.koppel(meer.get(i)), "pole " + (4 + i) + " joins");
        }
        gelijk(helper, Bezorgnet.MAX_HALTES, station.haltes().size(), "stops");
        helper.assertTrue(!station.koppel(meer.get(5)), "the ninth pole does not fit");
        helper.assertTrue(station.koppel(een), "a pole that is a stop already just stays one");
        helper.assertTrue(Bezorgnet.binnenBereik(BlockPos.ZERO, new BlockPos(Bezorgnet.BEREIK, 0, 0))
                && !Bezorgnet.binnenBereik(BlockPos.ZERO, new BlockPos(Bezorgnet.BEREIK + 1, 0, 0)), "reach is " + Bezorgnet.BEREIK + " blocks");
        // a broken pole leaves the round
        level.destroyBlock(helper.absolutePos(p(8, 5)), false);
        gelijk(helper, Bezorgnet.MAX_HALTES - 1, station.haltes().size(), "stops after breaking one");
        helper.assertTrue(station.nummer(drie.getBlockPos()) < 0, "the broken pole is no stop any more");
        // "Ander station": a second station a little further away than the first
        StepstationBlockEntity ander = station(helper, p(7, 1), Direction.SOUTH, false);
        een.volgendStation(level);
        gelijk(helper, ander.getBlockPos(), een.station(), "the pole moved to the other station");
        helper.assertTrue(station.nummer(een.getBlockPos()) < 0 && ander.nummer(een.getBlockPos()) == 0, "and left the first one");
        // (tidy up: no loose or foreign poles stay behind for the tests next door)
        for (int i = 0; i < meer.size(); i++) {
            helper.setBlock(p(2 + i, 2), Blocks.AIR);
        }
        weg(helper, speler);
        helper.succeed();
    }

    // =====================================================================================================================
    // the whistle
    // =====================================================================================================================

    /** The whistle: your own guhtje comes to you and its backpack opens; sneaking sends it home. Somebody else's does not listen. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 900)
    public static void techbezorgFluitje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer baas = speler(helper, p(2, 12)), ander = speler(helper, p(12, 12));
        StepstationBlockEntity station = station(helper, p(7, 4), Direction.SOUTH, true);
        station.zetEigenaar(baas.getUUID());
        station.vakken().set(0, ItemResource.of(new ItemStack(knabbel())), 5);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(station.koerier() != null && station.fase() == Fase.RUST, "the guhtje is home and awake"))
                .thenExecute(() -> {
                    gelijk(helper, 0, FluitjeItem.fluit(ander, false), "somebody else's whistle");
                    gelijk(helper, Fase.RUST, station.fase(), "it stays home");
                    gelijk(helper, 1, FluitjeItem.fluit(baas, false), "the owner's whistle");
                    gelijk(helper, Fase.NAAR_SPELER, station.fase(), "it comes");
                    helper.assertTrue(!station.rugzakOpen(), "on the road the backpack is shut");
                })
                .thenWaitUntil(() -> {
                    gelijk(helper, Fase.BIJ_SPELER, station.fase(), "fase");
                    helper.assertTrue(station.koerier().distanceToSqr(baas) <= 3 * 3, "it stands next to the owner");
                    helper.assertTrue(baas.containerMenu instanceof StepstationMenu, "its backpack opened for the owner");
                    helper.assertTrue(station.rugzakOpen(), "and can be reached");
                })
                .thenExecute(() -> {
                    StepstationMenu menu = (StepstationMenu) baas.containerMenu;
                    helper.assertTrue(menu.stillValid(baas), "the menu stays open far from the station, next to the guhtje");
                    helper.assertTrue(menu.getSlot(0).getItem().is(knabbel()) && menu.getSlot(0).mayPickup(baas), "the owner can take from the backpack");
                    gelijk(helper, 1, FluitjeItem.fluit(baas, true), "guhtjes sent home");
                    gelijk(helper, 0, FluitjeItem.fluit(ander, true), "somebody else sends nobody home");
                })
                .thenWaitUntil(() -> {
                    gelijk(helper, Fase.RUST, station.fase(), "fase");
                    helper.assertTrue(station.koerier().isBij(station.dok(level)), "it is home again");
                })
                .thenExecute(() -> weg(helper, baas, ander))
                .thenSucceed();
    }

    // =====================================================================================================================
    // the menus
    // =====================================================================================================================

    /** The pole's menu: ghost slots take nothing, the buttons work. The station's menu: the backpack shuts on the road. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 600)
    public static void techbezorgMenus(GameTestHelper helper) {
        ServerPlayer speler = speler(helper, p(7, 9));
        speler.setGameMode(GameType.SURVIVAL);
        Ronde r = ronde(helper, true);
        // --- the pole ---
        HalteMenu halte = (HalteMenu) r.paalB.createMenu(1, speler.getInventory(), speler);
        halte.setCarried(new ItemStack(knabbel(), 5));
        halte.clicked(0, 0, ContainerInput.PICKUP, speler);
        helper.assertTrue(r.paalB.filter(0).is(knabbel()) && r.paalB.filter(0).getCount() == 1, "a click puts one kaasknabbel in the filter");
        gelijk(helper, 5, halte.getCarried().getCount(), "and takes nothing from the hand");
        helper.assertTrue(r.paalB.past(new ItemStack(knabbel())) && !r.paalB.past(new ItemStack(Items.STONE)), "the filter");
        speler.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 10));
        halte.quickMoveStack(speler, 9 + 27);
        helper.assertTrue(r.paalB.filter(1).is(Items.COBBLESTONE), "a shift-click in the inventory adds the item to the filter");
        gelijk(helper, 10, speler.getInventory().getItem(0).getCount(), "and the stack stays where it is");
        halte.quickMoveStack(speler, 9 + 27);
        helper.assertTrue(r.paalB.filter(2).isEmpty(), "the same item is not added twice");
        halte.clicked(0, 0, ContainerInput.QUICK_MOVE, speler);
        helper.assertTrue(r.paalB.filter(0).isEmpty() && r.paalB.filter(1).is(Items.COBBLESTONE), "a shift-click on a filter slot clears it");
        halte.setCarried(ItemStack.EMPTY);
        helper.assertTrue(halte.clickMenuButton(speler, HalteMenu.KNOP_WIS) && r.paalB.past(new ItemStack(Items.STONE)), "the button empties the filter");
        helper.assertTrue(halte.clickMenuButton(speler, HalteMenu.KNOP_SOORT) && r.paalB.ophalen(), "the button turns afleveren into ophalen");
        helper.assertBlockProperty(p(12, 4), HaltepaaltjeBlock.OPHALEN, true);
        helper.assertTrue(halte.clickMenuButton(speler, HalteMenu.KNOP_SOORT) && !r.paalB.ophalen(), "and back");
        helper.assertTrue(halte.stillValid(speler), "the pole's menu is valid near the pole");
        r.paalB.zetFilter(0, new ItemStack(knabbel()));
        // --- the station ---
        StepstationMenu menu = (StepstationMenu) r.station.createMenu(2, speler.getInventory(), speler);
        helper.assertTrue(menu.clickMenuButton(speler, 1 * 4 + StepstationMenu.EERDER), "the button moves stop 2 one earlier");
        gelijk(helper, List.of(r.paalB.getBlockPos(), r.paalA.getBlockPos()), r.station.haltes(), "the round after the button");
        helper.assertTrue(menu.clickMenuButton(speler, 0 * 4 + StepstationMenu.LATER), "and later again");
        gelijk(helper, 2, menu.aantal(), "the menu knows how many stops there are");
        gelijk(helper, r.paalA.getBlockPos(), menu.halte(0), "and where the first one is");
        helper.assertTrue(menu.ophalen(0) && !menu.ophalen(1) && menu.heeftKist(0) && menu.bereikbaar(1), "and what happens there");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(r.station.koerier() != null && r.station.fase() == Fase.RUST && menu.open(), "home: the backpack is open"))
                .thenExecute(() -> {
                    speler.getInventory().setItem(0, new ItemStack(Items.APPLE, 6));
                    menu.quickMoveStack(speler, 9 + 27);
                    gelijk(helper, 6, r.station.vakken().getAmountAsInt(0), "a shift-click puts the apples in the backpack");
                    helper.assertTrue(speler.getInventory().getItem(0).isEmpty(), "and out of the inventory");
                    menu.quickMoveStack(speler, 0);
                    gelijk(helper, 0, r.station.gevuld(), "and a shift-click takes them out again");
                    gelijk(helper, 6, speler.getInventory().countItem(Items.APPLE), "apples back in the inventory");
                    speler.getInventory().clearContent();
                    // now there is work: it leaves, and the backpack shuts
                    r.a.setItem(0, new ItemStack(knabbel(), 4));
                })
                .thenWaitUntil(() -> helper.assertTrue(r.station.fase() == Fase.RIJDT && r.station.gevuld() > 0, "on the road with the kaasknabbels"))
                .thenExecute(() -> {
                    helper.assertTrue(!menu.open(), "on the road the backpack is shut");
                    helper.assertTrue(!menu.getSlot(0).mayPickup(speler) && !menu.getSlot(1).mayPlace(new ItemStack(Items.APPLE)), "its slots take and give nothing");
                    helper.assertTrue(menu.quickMoveStack(speler, 0).isEmpty() && r.station.gevuld() > 0, "a shift-click takes nothing out");
                    speler.getInventory().setItem(0, new ItemStack(knabbel(), 3));
                    menu.quickMoveStack(speler, 9 + 27);
                    gelijk(helper, 3, speler.getInventory().getItem(0).getCount(), "and puts nothing in (not even onto the same item)");
                    gelijk(helper, 1, menu.doel(), "the menu says which stop it rides to");
                })
                .thenWaitUntil(() -> gelijk(helper, 4, tel(r.b, knabbel()), "kaasknabbels in chest B"))
                .thenExecute(() -> weg(helper, speler))
                .thenSucceed();
    }

    // =====================================================================================================================
    // the readout, the guhtje itself, the texts
    // =====================================================================================================================

    /** The hover readout of the station says what is wrong with the round. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void techbezorgUitlezing(GameTestHelper helper) {
        StepstationBlockEntity station = station(helper, p(7, 7), Direction.NORTH, true);
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(p(7, 7))).is(VadsKracht.TOON), "the station is in the tag guhs:vadskracht");
        helper.runAfterDelay(30, () -> {
            List<String> regels = lees(helper, p(7, 7));
            helper.assertTrue(regels.contains("Deze opstelling gebruikt " + VadsGetallen.STEPSTATION + "/10 vadskracht"), "readout: " + regels);
            helper.assertTrue(regels.contains("Het Bezorgguhtje rust uit bij het station."), "readout: " + regels);
            helper.assertTrue(regels.contains("Geen haltes: zet een Haltepaaltje tegen een kist (tot " + Bezorgnet.BEREIK + " blokken ver)"), "readout: " + regels);
            kist(helper, p(2, 3));
            paal(helper, p(2, 4), Direction.NORTH, true, station);
            regels = lees(helper, p(7, 7));
            helper.assertTrue(regels.contains("Geen aflever-halte: niemand wil de spullen hebben"), "readout: " + regels);
            paal(helper, p(12, 4), Direction.NORTH, false, station);
            regels = lees(helper, p(7, 7));
            helper.assertTrue(regels.stream().noneMatch(s -> s.startsWith("Geen ")), "readout with a full round: " + regels);
            helper.succeed();
        });
    }

    /** The guhtje cannot be hurt, hurts nobody, likes a kaasknabbel, has its Guhdex page; all texts and advancements exist. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 100)
    public static void techbezorgGuhtjeIsLief(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer speler = speler(helper, p(7, 9));
        speler.setGameMode(GameType.SURVIVAL);
        BezorgguhtjeEntity k = helper.spawn(TechbezorgFeature.BEZORGGUHTJE.get(), p(7, 7));
        float gezond = k.getHealth();
        helper.assertTrue(!k.hurtServer(level, level.damageSources().generic(), 5f), "damage does nothing");
        helper.assertTrue(!k.hurtServer(level, level.damageSources().playerAttack(speler), 5f), "a player's hit does nothing");
        helper.assertTrue(!k.hurtServer(level, level.damageSources().lava(), 5f), "the frituursaus does nothing");
        gelijk(helper, gezond, k.getHealth(), "health");
        helper.assertTrue(!k.isPushable() && !k.canBeLeashed() && !k.removeWhenFarAway(10_000), "not pushed, not leashed, never despawns");
        helper.assertTrue(k.station() == null, "a spawn egg guhtje has no station");
        float spelerGezond = speler.getHealth();
        speler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(knabbel(), 2));
        k.mobInteract(speler, InteractionHand.MAIN_HAND);
        gelijk(helper, 1, speler.getMainHandItem().getCount(), "it ate one kaasknabbel");
        speler.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        k.mobInteract(speler, InteractionHand.MAIN_HAND);
        gelijk(helper, spelerGezond, speler.getHealth(), "the player's health");
        helper.assertTrue(GuhDex.isCreaturePage(GuhVariant.BEZORGGUHTJE), "the Bezorgguhtje has its Guhdex page");
        for (String adv : List.of("quest/seen_bezorgguhtje", "quest/tech_bezorg_station", "quest/tech_bezorg_halte", "quest/tech_bezorg_bezorgd",
                "quest/tech_bezorg_fluitje", "techniek/tech_bezorg_station", "techniek/tech_bezorg_bezorgd", "techniek/tech_bezorg_fluitje")) {
            helper.assertTrue(level.getServer().getAdvancements().get(Guhs.id(adv)) != null, "advancement guhs:" + adv);
        }
        for (Fase fase : Fase.values()) {
            String sleutel = "gui.guhs.techbezorg.stand." + fase.id();
            helper.assertTrue(NlTekst.has(sleutel), "text " + sleutel);
        }
        for (String sleutel : List.of("block.guhs.stepstation", "block.guhs.stepstation.lore", "block.guhs.haltepaaltje", "block.guhs.haltepaaltje.lore",
                "item.guhs.bezorgguhtje_fluitje", "item.guhs.bezorgguhtje_fluitje.lore", "item.guhs.bezorgguhtje_fluitje.lore.sluip",
                "entity.guhs.bezorgguhtje", "gui.guhs.guhdex.info.bezorgguhtje", "gui.guhs.techbezorg.stand.zoek", "gui.guhs.techbezorg.rugzak.dicht",
                "gui.guhs.techbezorg.halte.gekoppeld", "gui.guhs.techbezorg.halte.geen_station", "gui.guhs.techbezorg.halte.vol",
                "gui.guhs.techbezorg.halte.geen_kist", "gui.guhs.techbezorg.station.gekoppeld", "gui.guhs.techbezorg.station.nieuw",
                "gui.guhs.techbezorg.fluit.komt", "gui.guhs.techbezorg.fluit.niemand", "gui.guhs.techbezorg.fluit.slaapt",
                "gui.guhs.techbezorg.fluit.naar_huis", "gui.guhs.techbezorg.guhtje.dakloos", "gui.guhs.techbezorg.guhtje.lekker")) {
            helper.assertTrue(NlTekst.has(sleutel), "text " + sleutel);
        }
        helper.assertTrue(NlTekst.get("block.guhs.stepstation.lore").contains(VadsGetallen.STEPSTATION + " vadskracht")
                && NlTekst.get("block.guhs.stepstation.lore").contains(Bezorgnet.BEREIK + " blokken"), "the lore names the real numbers");
        k.discard();
        weg(helper, speler);
        helper.succeed();
    }

    private TechbezorgGameTests() {
    }
}
