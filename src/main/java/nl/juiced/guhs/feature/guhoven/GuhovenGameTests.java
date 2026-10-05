package nl.juiced.guhs.feature.guhoven;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.GuhWheelPartBlock;
import nl.juiced.guhs.block.GuhWireBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsNet;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * Game tests of the Guhoven (1.2.5, batch "guhoven"): it smelts on vadskracht (bbq2: a Guhrad with a guh in it next to the
 * oven, one of the wheel's part blocks next to it, or Guhdraad from a wheel), it does nothing without a source, with an empty
 * wheel or with ordinary redstone (a lever, a redstone block), it stands still with everything else when the net is too
 * heavy, its face shows how it is doing, and the fuel slot never takes or burns anything.
 */
public class GuhovenGameTests {
    private static final String BATCH = "guhoven";
    /** Raw iron takes the standard 200 ticks; a little margin for the first tick and the lit flip. */
    private static final int BAKTIJD = 215;

    static GuhOvenBlockEntity oven(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, GuhovenFeature.GUH_OVEN.get().defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH));
        GuhOvenBlockEntity be = (GuhOvenBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(be != null, "the oven has a block entity");
        be.setItem(0, new ItemStack(Items.RAW_IRON, 1));
        return be;
    }

    /** A Guhrad (only its wheel block); running: with a guh in it (an empty wheel gives no vadskracht). */
    static void rad(GameTestHelper helper, BlockPos pos, boolean running) {
        helper.setBlock(pos, ModBlocks.GUH_WHEEL.get().defaultBlockState().setValue(GuhWheelBlock.RUNNING, running));
        if (running) {
            guhErin(helper, pos);
        }
    }

    static void guhErin(GameTestHelper helper, BlockPos pos) {
        CompoundTag guh = new CompoundTag();
        guh.putString("Variant", "normal");
        ((GuhWheelBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos))).insert(guh);
    }

    static void gebakken(GameTestHelper helper, GuhOvenBlockEntity be) {
        helper.assertTrue(be.getItem(2).is(Items.IRON_INGOT) && be.getItem(2).getCount() == 1, "baked an iron ingot, got " + be.getItem(2));
        helper.assertTrue(be.getItem(0).isEmpty(), "the raw iron is used up");
        helper.assertTrue(be.getItem(1).isEmpty(), "the fuel slot stays empty");
    }

    static void nietGebakken(GameTestHelper helper, GuhOvenBlockEntity be, BlockPos pos, String waarom) {
        helper.assertTrue(be.getItem(0).is(Items.RAW_IRON), waarom + ": the raw iron is still there");
        helper.assertTrue(be.getItem(2).isEmpty(), waarom + ": nothing baked");
        helper.assertFalse(be.bakt(), waarom + ": not baking");
        helper.assertBlockProperty(pos, AbstractFurnaceBlock.LIT, false);
    }

    // =====================================================================================================================

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void baktNaastEenDraaiendGuhrad(GameTestHelper helper) {
        BlockPos ovenPos = new BlockPos(2, 1, 2);
        rad(helper, new BlockPos(1, 1, 2), true);
        GuhOvenBlockEntity be = oven(helper, ovenPos);
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(be.bakt(), "baking on guh power");
            helper.assertBlockProperty(ovenPos, AbstractFurnaceBlock.LIT, true);
        });
        helper.runAtTickTime(BAKTIJD, () -> {
            gebakken(helper, be);
            helper.succeedWhen(() -> helper.assertBlockProperty(ovenPos, AbstractFurnaceBlock.LIT, false));   // nothing left to bake
        });
    }

    /** The big wheel is 3x3: an oven next to one of its (invisible) part blocks bakes too. And a stopped wheel gives nothing. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void baktNaastEenDeelVanHetRad(GameTestHelper helper) {
        BlockPos wiel = new BlockPos(1, 1, 2);
        rad(helper, wiel, false);
        // facing north: "along" is east, so side 2 (right) sits one block east of the wheel
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.GUH_WHEEL_PART.get().defaultBlockState()
                .setValue(GuhWheelPartBlock.FACING, Direction.NORTH).setValue(GuhWheelPartBlock.SIDE, 2).setValue(GuhWheelPartBlock.HEIGHT, 0));
        BlockPos ovenPos = new BlockPos(3, 1, 2);
        GuhOvenBlockEntity be = oven(helper, ovenPos);
        helper.runAtTickTime(40, () -> {
            nietGebakken(helper, be, ovenPos, "a wheel without a guh");
            helper.assertTrue(be.getItem(0).getCount() == 1, "still one raw iron");
            helper.setBlock(wiel, helper.getBlockState(wiel).setValue(GuhWheelBlock.RUNNING, true));
            helper.runAfterDelay(2, () -> nietGebakken(helper, be, ovenPos, "a wheel that only looks like it runs (no guh in it)"));
        });
        helper.runAtTickTime(44, () -> guhErin(helper, wiel));
        helper.runAtTickTime(50, () -> helper.assertTrue(be.bakt(), "the part block passes on the running wheel's vadskracht"));
        helper.runAtTickTime(50 + BAKTIJD, () -> {
            gebakken(helper, be);
            helper.succeed();
        });
    }

    /** Guhrad -> 3 blocks of Guhdraad -> oven (the oven is not next to the wheel). */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void baktViaGuhdraad(GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE);
        }
        rad(helper, new BlockPos(0, 1, 1), true);
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), ModBlocks.GUH_WIRE.get());
        }
        BlockPos ovenPos = new BlockPos(4, 1, 1);
        GuhOvenBlockEntity be = oven(helper, ovenPos);
        VadsNet net = VadsKracht.net(helper.getLevel(), helper.absolutePos(ovenPos));
        helper.assertTrue(net.draait() && net.aanbod() == VadsGetallen.GUHRAD && net.vraag() == VadsGetallen.GUH_OVEN,
                "one net: the wheel gives " + net.aanbod() + ", the oven asks " + net.vraag());
        helper.assertBlockProperty(new BlockPos(3, 1, 1), GuhWireBlock.POWERED, true);
        helper.runAtTickTime(5, () -> helper.assertTrue(be.bakt(), "baking on guh power through the wire"));
        helper.runAtTickTime(BAKTIJD, () -> {
            gebakken(helper, be);
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void zonderKrachtNiks(GameTestHelper helper) {
        BlockPos ovenPos = new BlockPos(2, 1, 2);
        GuhOvenBlockEntity be = oven(helper, ovenPos);
        helper.runAtTickTime(BAKTIJD, () -> {
            nietGebakken(helper, be, ovenPos, "no power");
            helper.succeed();
        });
    }

    /** A redstone block and a lever give the oven a strong redstone signal, but that isn't guh power. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void gewoneRedstoneNiks(GameTestHelper helper) {
        BlockPos ovenPos = new BlockPos(2, 1, 2);
        helper.setBlock(new BlockPos(3, 0, 2), Blocks.STONE);
        GuhOvenBlockEntity be = oven(helper, ovenPos);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.REDSTONE_BLOCK);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.POWERED, true));
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.REDSTONE_TORCH);
        helper.assertTrue(helper.getLevel().hasNeighborSignal(helper.absolutePos(ovenPos)), "the oven does get a redstone signal");
        helper.assertFalse(GuhovenFeature.guhKracht(helper.getLevel(), helper.absolutePos(ovenPos)), "but no vadskracht");
        helper.assertTrue(VadsKracht.net(helper.getLevel(), helper.absolutePos(ovenPos)).status() == VadsNet.Status.GEEN_BRON, "its net has no source");
        helper.runAtTickTime(BAKTIJD, () -> {
            nietGebakken(helper, be, ovenPos, "lever, torch and redstone block");
            helper.succeed();
        });
    }

    /**
     * bbq2: three ovens (3 x 5 VK) on one Guhrad (10 VK) is too heavy: not one of them bakes. Take one away and the other two
     * bake. And the face: asleep without vadskracht, awake with it, surprised when the result slot is full.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void teZwaarBaktNietEenOven(GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, 0, 1), Blocks.STONE);
        }
        rad(helper, new BlockPos(0, 1, 1), true);
        for (int x = 1; x <= 4; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), ModBlocks.GUH_WIRE.get());
        }
        BlockPos[] ovens = {new BlockPos(1, 1, 2), new BlockPos(3, 1, 2), new BlockPos(4, 1, 0)};
        GuhOvenBlockEntity[] be = new GuhOvenBlockEntity[3];
        for (int i = 0; i < 3; i++) {
            be[i] = oven(helper, ovens[i]);
        }
        VadsNet net = VadsKracht.net(helper.getLevel(), helper.absolutePos(ovens[0]));
        helper.assertTrue(net.status() == VadsNet.Status.TE_ZWAAR && net.vraag() == 3 * VadsGetallen.GUH_OVEN && net.tekort() == 5,
                "too heavy: " + net.status() + " " + net.vraag() + "/" + net.aanbod());
        helper.assertBlockProperty(new BlockPos(2, 1, 1), GuhWireBlock.POWERED, false);
        helper.runAtTickTime(40, () -> {
            for (int i = 0; i < 3; i++) {
                nietGebakken(helper, be[i], ovens[i], "a net that is too heavy");
                helper.assertBlockProperty(ovens[i], GuhOvenBlock.SNOET, Snoet.SLAAPT);
            }
            helper.setBlock(ovens[2], Blocks.AIR);
            // the second oven's result slot is full: it has vadskracht but cannot bake, and looks surprised
            be[1].setItem(2, new ItemStack(Items.IRON_INGOT, 64));
        });
        helper.runAtTickTime(46, () -> {
            helper.assertTrue(be[0].bakt() && be[0].heeftKracht() && be[1].heeftKracht(), "two ovens on one wheel fit");
            helper.assertFalse(be[1].bakt(), "the full oven does not bake");
            helper.assertBlockProperty(new BlockPos(2, 1, 1), GuhWireBlock.POWERED, true);
            helper.assertBlockProperty(ovens[0], GuhOvenBlock.SNOET, Snoet.WERKT);
            helper.assertBlockProperty(ovens[1], GuhOvenBlock.SNOET, Snoet.VOL);
        });
        helper.runAtTickTime(46 + BAKTIJD, () -> {
            gebakken(helper, be[0]);
            helper.assertTrue(be[1].getItem(0).is(Items.RAW_IRON), "the full oven kept its raw iron");
            helper.succeedWhen(() -> helper.assertBlockProperty(ovens[0], GuhOvenBlock.SNOET, Snoet.WERKT));   // done, awake, not lit
        });
    }

    /** The fuel slot takes nothing (screen, hoppers, pipes), and coal that still lands in it is never burned: it pops out. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void brandstofvakjeBlijftLeeg(GameTestHelper helper) {
        BlockPos ovenPos = new BlockPos(2, 1, 2);
        GuhOvenBlockEntity be = oven(helper, ovenPos);
        ItemStack kolen = new ItemStack(Items.COAL, 8);
        helper.assertFalse(be.canPlaceItem(1, kolen), "the container refuses fuel");
        for (Direction d : Direction.values()) {
            helper.assertFalse(be.canPlaceItemThroughFace(1, kolen, d), "no fuel through face " + d);
            for (int slot : be.getSlotsForFace(d)) {
                helper.assertTrue(slot != 1, "face " + d + " doesn't offer the fuel slot");
            }
        }
        helper.assertTrue(be.getSlotsForFace(Direction.EAST)[0] == 0, "side hoppers fill the input slot");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GuhOvenMenu menu = new GuhOvenMenu(0, player.getInventory(), be, new SimpleContainerData(4));
        helper.assertFalse(menu.getSlot(1).mayPlace(kolen), "the screen's (hidden) fuel slot takes nothing");
        helper.assertFalse(menu.getSlot(1).isActive(), "the fuel slot is hidden");

        // forced in anyway (like a command would): it is not burned, it pops out, and the oven still bakes on guh power only
        be.setItem(1, kolen.copy());
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(be.getItem(1).isEmpty(), "the coal left the fuel slot");
            helper.assertItemEntityCountIs(Items.COAL, ovenPos, 2.0, 8);
            nietGebakken(helper, be, ovenPos, "coal but no guh power");
            rad(helper, new BlockPos(1, 1, 2), true);
        });
        helper.runAtTickTime(3 + BAKTIJD, () -> {
            gebakken(helper, be);
            helper.assertItemEntityCountIs(Items.COAL, ovenPos, 3.0, 8);   // all 8 still there
            helper.succeed();
        });
    }
}
