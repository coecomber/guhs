package nl.juiced.guhs.feature.bouwhulp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity;
import nl.juiced.guhs.feature.bouwhulp.kern.Materiaal;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;
import nl.juiced.guhs.feature.bouwhulp.kern.Spoken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Deel 2 F8: the spookbouw builds what the ghost shows, from real items (inventory first), stops at a shortage unless
 * "alsnog", keeps what was in the way for undo (no doubling), turns around the middle of its footprint; the Bouwkeet asks
 * no vadskracht at level 0 and goes up one level at a time.
 */
public class BouwhulpGameTests {
    private static final String BATCH = "bouwhulp";
    private static final String VLOER = "px_test_16";

    private static ServerPlayer speler(GameTestHelper helper, int planken) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        if (planken > 0) {
            p.getInventory().add(new ItemStack(Items.OAK_PLANKS, planken));
        }
        return p;
    }

    private static int planken(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(Items.OAK_PLANKS)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** What a block costs: a double slab two, the top half of a door nothing, fire cannot be built. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 20)
    public static void bouwhulpKosten(GameTestHelper helper) {
        Materiaal.Kost slab = Materiaal.kost(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
        helper.assertTrue(slab != null && slab.item() == Items.OAK_SLAB && slab.aantal() == 2, "a double slab is two slabs");
        Materiaal.Kost deur = Materiaal.kost(Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        helper.assertTrue(deur != null && deur.aantal() == 0, "the top half of a door is free");
        helper.assertTrue(Materiaal.kost(Blocks.FIRE.defaultBlockState()) == null, "fire cannot be built");
        helper.assertTrue(Materiaal.kost(Blocks.AIR.defaultBlockState()).aantal() == 0, "digging out is free");
        helper.succeed();
    }

    /** Turning: a line along x turns to a line along z around the middle; a stair turns with it. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 20)
    public static void bouwhulpDraaien(GameTestHelper helper) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        for (int x = 0; x < 3; x++) {
            m.put(new BlockPos(x, 0, 0), Blocks.STONE.defaultBlockState());
        }
        m.put(new BlockPos(1, 1, 0), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        Ontwerp o = Ontwerp.van(m);
        helper.assertTrue(o.grootte().getX() == 3 && o.grootte().getY() == 2 && o.grootte().getZ() == 1, "the box is 3x2x1");
        BlockPos oorsprong = new BlockPos(100, 64, 100);
        List<Map.Entry<BlockPos, BlockState>> gedraaid = o.geplaatst(oorsprong, Rotation.CLOCKWISE_90, Mirror.NONE);
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (Map.Entry<BlockPos, BlockState> e : gedraaid) {
            helper.assertTrue(e.getKey().getX() == oorsprong.getX() + 1, "turned, every block lies on one x line: " + e.getKey());
            minZ = Math.min(minZ, e.getKey().getZ());
            maxZ = Math.max(maxZ, e.getKey().getZ());
            if (e.getValue().is(Blocks.OAK_STAIRS)) {
                helper.assertTrue(e.getValue().getValue(StairBlock.FACING) == Direction.EAST, "the stair turned along (north -> east)");
            }
        }
        helper.assertTrue(maxZ - minZ == 2, "three long along z");
        helper.assertTrue(gedraaid.get(0).getKey().getY() == 64, "built from the bottom up");
        Ontwerp terug = Ontwerp.laad(o.opslaan());
        helper.assertTrue(terug.blokken().equals(o.blokken()), "saved and loaded: the same ontwerp");
        helper.succeed();
    }

    /** Enough planks: a 2x1x2 floor appears, and exactly four planks left the inventory. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void bouwhulpBouwtUitInventaris(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 10);
        BlockPos plek = helper.absolutePos(new BlockPos(3, 2, 3));
        Spook s = Spookbouw.start(p, Ontwerp.doos(2, 1, 2, Blocks.OAK_PLANKS.defaultBlockState()), plek, Rotation.NONE, Mirror.NONE);
        helper.assertTrue(s != null, "a ghost");
        Spoken.Bouwen b = Spookbouw.bouw(p, s, false);
        helper.assertTrue(b.begonnen() && b.tekort().isEmpty(), "it starts, nothing missing");
        helper.succeedWhen(() -> {
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(3 + x, 2, 3 + z));
                }
            }
            helper.assertTrue(planken(p) == 6, "four planks used (" + planken(p) + " left)");
            helper.assertTrue(Spookbouw.get(helper.getLevel(), s.id()) == null, "the ghost is gone when it is done");
        });
    }

    /** Too few planks: "Bouw!" says what is missing; "alsnog" builds what it can and the rest waits as a ghost. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void bouwhulpTekortEnAlsnog(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 2);
        BlockPos plek = helper.absolutePos(new BlockPos(3, 2, 3));
        Spook s = Spookbouw.start(p, Ontwerp.doos(2, 1, 2, Blocks.OAK_PLANKS.defaultBlockState()), plek, Rotation.NONE, Mirror.NONE);
        helper.assertTrue(s != null, "a ghost");
        Spoken.Bouwen b = Spookbouw.bouw(p, s, false);
        helper.assertTrue(!b.begonnen() && b.tekort().getOrDefault(Items.OAK_PLANKS, 0) == 2, "two planks short, nothing starts");
        helper.assertTrue(Spookbouw.bouw(p, s, true).begonnen(), "alsnog: it starts");
        helper.succeedWhen(() -> {
            helper.assertTrue(s.status() == Spook.Status.WACHT, "it waits for the rest");
            helper.assertTrue(planken(p) == 0, "both planks used");
            int staan = 0;
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    if (helper.getBlockState(new BlockPos(3 + x, 2, 3 + z)).is(Blocks.OAK_PLANKS)) {
                        staan++;
                    }
                }
            }
            helper.assertTrue(staan == 2, "two built (" + staan + ")");
            helper.assertTrue(Spookbouw.get(helper.getLevel(), s.id()) != null, "the ghost stays");
            Spookbouw.annuleer(helper.getLevel(), s);
        });
    }

    /**
     * Undo: the stone that was in the way comes back (from the action, not doubled), the planks come back to the
     * inventory, nothing else appears.
     */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 120)
    public static void bouwhulpTerugDraaien(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 4);
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.STONE);
        BlockPos plek = helper.absolutePos(new BlockPos(3, 2, 3));
        Spook s = Spookbouw.start(p, Ontwerp.doos(2, 1, 2, Blocks.OAK_PLANKS.defaultBlockState()), plek, Rotation.NONE, Mirror.NONE);
        helper.assertTrue(s != null && Spookbouw.bouw(p, s, false).begonnen(), "it builds");
        helper.runAfterDelay(30, () -> {
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 2, 4));
            helper.assertTrue(planken(p) == 0, "four planks used");
            helper.assertTrue(Spookbouw.ongedaan(p), "there is something to undo");
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(4, 2, 4));
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 2, 3));
            helper.assertTrue(planken(p) == 4, "the four planks are back (" + planken(p) + ")");
            int steen = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(Items.STONE) || p.getInventory().getItem(i).is(Items.COBBLESTONE)) {
                    steen++;
                }
            }
            helper.assertTrue(steen == 0, "the stone stands again, so no stone item as well");
            helper.assertTrue(!Spookbouw.ongedaan(p), "nothing more to undo");
            helper.succeed();
        });
    }

    /** The keet: level 0 asks no vadskracht, an upgrade can only be the next level, then it asks its VK. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 40)
    public static void bouwhulpKeetNiveaus(GameTestHelper helper) {
        BlockPos kern = new BlockPos(7, 2, 3);
        helper.setBlock(kern, BouwhulpFeature.BOUWKEET.get().defaultBlockState());
        BouwkeetBlockEntity keet = (BouwkeetBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(kern));
        helper.assertTrue(keet != null, "the keet has its block entity");
        helper.assertBlockPresent(nl.juiced.guhs.feature.vadskracht.VadskrachtFeature.MACHINE_DEEL.get(), kern.above(2));
        helper.assertTrue(keet.vadsVraag() == 0 && keet.werkNiveau() == BouwNiveau.HANDWERK, "level 0: no VK");
        helper.assertTrue(!keet.upgrade(BouwNiveau.ZOUT), "level 2 before level 1: no");
        helper.assertTrue(keet.upgrade(BouwNiveau.GUHRAD), "level 1: yes");
        helper.assertTrue(keet.vadsVraag() == BouwNiveau.GUHRAD.vk, "level 1 asks " + BouwNiveau.GUHRAD.vk + " VK");
        helper.assertTrue(keet.werkNiveau() == BouwNiveau.HANDWERK, "without vadskracht it works as handwerk");
        keet.zetAan(false);
        helper.assertTrue(keet.vadsVraag() == 0, "switched off: no VK");
        helper.succeed();
    }

    /** A keet's project builds at the keet's tempo and the keet lets go of it when it is done. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void bouwhulpKeetBouwtProject(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 0);
        p.setGameMode(GameType.CREATIVE);   // (creative builds for free)
        BlockPos kern = new BlockPos(12, 2, 3);
        helper.setBlock(kern, BouwhulpFeature.BOUWKEET.get().defaultBlockState());
        BouwkeetBlockEntity keet = (BouwkeetBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(kern));
        Spook s = Spookbouw.start(p, Ontwerp.doos(3, 2, 2, Blocks.PINK_WOOL.defaultBlockState()), helper.absolutePos(new BlockPos(2, 2, 9)),
                Rotation.NONE, Mirror.NONE);
        helper.assertTrue(s != null && keet.zetProject(helper.getLevel(), s), "the keet takes the project");
        helper.assertTrue(keet.project() == s, "it is the keet's project");
        helper.assertTrue(keet.tempo(helper.getLevel(), s) >= BouwkeetBlockEntity.BASIS, "at least the base tempo");
        helper.assertTrue(Spookbouw.bouw(p, s, false).begonnen(), "it builds");
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.PINK_WOOL, new BlockPos(4, 3, 10));
            helper.assertTrue(keet.project() == null, "done: the keet is free again");
            helper.assertTrue(!keet.bouwt(), "and not building any more");
        });
    }
}
