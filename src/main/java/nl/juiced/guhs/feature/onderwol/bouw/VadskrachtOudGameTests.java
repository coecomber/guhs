package nl.juiced.guhs.feature.onderwol.bouw;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.boerderij.BoerderijProtection;
import nl.juiced.guhs.feature.kaasmijn.KaasmijnFeature;
import nl.juiced.guhs.feature.kaasmijn.KaasmijnProtection;
import nl.juiced.guhs.feature.techmachine.VadsmolenBlockEntity;
import nl.juiced.guhs.feature.techsaus.SausvatBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0, ronde 45-47 "Vadskracht in de structuren" (tools/features/vadskracht_oud.py): the working vadskracht of the NEW
 * copies of four old structures, each template placed by itself (also turned) and checked with {@link KrachtProef}:
 * <ul>
 *   <li>the guh villages (layouts a, b, c): the Guhbakkerij, a Guhrad with a Guhoven and a Vadsmolen (early tier);</li>
 *   <li>the kaasmijn (protected): the Sauspompen fill the Sausvaten, the Knabbelaar gnaws a kaasader (it is left mined
 *       out, it grows back) and the kaasbrokken roll through the Knabbelbuis into the chest, also while the mine is
 *       protected (a building's own machine works in its building);</li>
 *   <li>the Barbecuether: the Spieskeuken of the Spiesburcht and the grill kitchen of the Mika-grillpaleis (early tier),
 *       the Grillkoolfabriek of the Mika-woonblokken (protected: pumps for saus and water, the Grillkoolpers, the
 *       Frituurautomaat and tubes into a chest);</li>
 *   <li>the Guhboerderij (protected): the Oogster harvests the wheat into the chest through the tube, the Plantagebak grows a
 *       tree and a klusjes-guh of the farm chops it.</li>
 * </ul>
 * Every setup has a lever next to its wheel: on, its guh takes a break and the net stands still; off again, it runs on.
 */
public class VadskrachtOudGameTests {
    private static final String BATCH = "vadskracht_oud";

    // =====================================================================================================================
    // the guh village
    // =====================================================================================================================
    @GuhTest(template = "guh_village/layout_a", batch = BATCH, timeoutTicks = 300)
    public static void dorpBakkerijA(GameTestHelper helper) {
        bakkerij(helper);
    }

    @GuhTest(template = "guh_village/layout_a", batch = BATCH, timeoutTicks = 300, rotationSteps = 1)
    public static void dorpBakkerijAGedraaid(GameTestHelper helper) {
        bakkerij(helper);
    }

    @GuhTest(template = "guh_village/layout_b", batch = BATCH, timeoutTicks = 300)
    public static void dorpBakkerijB(GameTestHelper helper) {
        bakkerij(helper);
    }

    @GuhTest(template = "guh_village/layout_c", batch = BATCH, timeoutTicks = 300, rotationSteps = 2)
    public static void dorpBakkerijC(GameTestHelper helper) {
        bakkerij(helper);
    }

    @GuhTest(template = "guh_village/layout_a", batch = BATCH, timeoutTicks = 400)
    public static void dorpHendel(GameTestHelper helper) {
        hendels(helper, 1);
    }

    private static void bakkerij(GameTestHelper helper) {
        KrachtProef.draait(helper, 1, 2, 3, g -> {
            maalt(helper, g, 1);
            grilt(helper, g);
        });
    }

    // =====================================================================================================================
    // the kaasmijn
    // =====================================================================================================================
    @GuhTest(template = "kaasmijn", batch = BATCH, timeoutTicks = 600)
    public static void kaasmijnPomphuisEnKaasbrokken(GameTestHelper helper) {
        kaasmijn(helper);
    }

    @GuhTest(template = "kaasmijn", batch = BATCH, timeoutTicks = 600, rotationSteps = 3)
    public static void kaasmijnGedraaid(GameTestHelper helper) {
        kaasmijn(helper);
    }

    @GuhTest(template = "kaasmijn", batch = BATCH, timeoutTicks = 400)
    public static void kaasmijnHendels(GameTestHelper helper) {
        hendels(helper, 2);
    }

    private static void kaasmijn(GameTestHelper helper) {
        // the mine is protected (as in the world): the Knabbelaar, a machine of the building, still gnaws in it
        AABB mijn = helper.getBounds();
        KaasmijnProtection.TEST_AREAS.add(mijn);
        try {
            KrachtProef.draait(helper, 2, 3, 6, g -> {
                vatenVol(helper, g, 5);
                helper.assertTrue(inKist(helper, KaasmijnFeature.KAASBROK.get()) > 0, "kaasbrokken in the chest (Knabbelaar -> Richtingstuk -> Knabbelbuis)");
                KaasmijnProtection.TEST_AREAS.remove(mijn);
                // the gnawed vein is left mined out (it grows back), not a hole
                boolean ader = false;
                for (BlockPos p : blokken(helper)) {
                    var s = helper.getLevel().getBlockState(p);
                    ader |= s.is(KaasmijnFeature.UITGEMIJNDE_KAASADER.get());
                }
                helper.assertTrue(ader, "a mined-out kaasader where the Knabbelaar gnawed");
            });
        } catch (RuntimeException e) {
            KaasmijnProtection.TEST_AREAS.remove(mijn);
            throw e;
        }
    }

    // =====================================================================================================================
    // the Barbecuether
    // =====================================================================================================================
    @GuhTest(template = "spiesburcht/stuk_1_1", batch = BATCH, timeoutTicks = 300)
    public static void spiesburchtSpieskeuken(GameTestHelper helper) {
        spieskeuken(helper);
    }

    @GuhTest(template = "spiesburcht/stuk_1_1", batch = BATCH, timeoutTicks = 300, rotationSteps = 1)
    public static void spiesburchtSpieskeukenGedraaid(GameTestHelper helper) {
        spieskeuken(helper);
    }

    @GuhTest(template = "spiesburcht/stuk_1_1", batch = BATCH, timeoutTicks = 400)
    public static void spiesburchtHendel(GameTestHelper helper) {
        hendels(helper, 1);
    }

    private static void spieskeuken(GameTestHelper helper) {
        KrachtProef.draait(helper, 1, 3, 6, g -> {
            maalt(helper, g, 1);
            grilt(helper, g);
        });
    }

    @GuhTest(template = "mika_grillpaleis/stuk_0_0", batch = BATCH, timeoutTicks = 300)
    public static void grillpaleisKeuken(GameTestHelper helper) {
        grillkeuken(helper);
    }

    @GuhTest(template = "mika_grillpaleis/stuk_0_0", batch = BATCH, timeoutTicks = 300, rotationSteps = 2)
    public static void grillpaleisKeukenGedraaid(GameTestHelper helper) {
        grillkeuken(helper);
    }

    @GuhTest(template = "mika_grillpaleis/stuk_0_0", batch = BATCH, timeoutTicks = 400)
    public static void grillpaleisHendel(GameTestHelper helper) {
        hendels(helper, 1);
    }

    private static void grillkeuken(GameTestHelper helper) {
        KrachtProef.draait(helper, 1, 2, 4, g -> {
            vatenVol(helper, g, 5);
            grilt(helper, g);
        });
    }

    @GuhTest(template = "mika_woonblokken/stuk_0_1", batch = BATCH, timeoutTicks = 900)
    public static void woonblokkenGrillkoolfabriek(GameTestHelper helper) {
        grillkoolfabriek(helper);
    }

    @GuhTest(template = "mika_woonblokken/stuk_0_1", batch = BATCH, timeoutTicks = 900, rotationSteps = 3)
    public static void woonblokkenGrillkoolfabriekGedraaid(GameTestHelper helper) {
        grillkoolfabriek(helper);
    }

    @GuhTest(template = "mika_woonblokken/stuk_0_1", batch = BATCH, timeoutTicks = 400)
    public static void woonblokkenHendel(GameTestHelper helper) {
        hendels(helper, 1);   // (one lever between the two wheels)
    }

    private static void grillkoolfabriek(GameTestHelper helper) {
        KrachtProef.draait(helper, 2, 4, 7, g -> {
            helper.assertTrue(inKist(helper, BarbecuetherFeature.GRILLKOOL.get().asItem()) > 0, "grillkool from the pers in the chest");
            helper.assertTrue(inKist(helper, ModItems.GEFRITUURDE_KAASKNABBELS.get()) > 0, "fried kaasknabbels from the frituur in the chest");
        });
    }

    // =====================================================================================================================
    // the Guhboerderij
    // =====================================================================================================================
    @GuhTest(template = "guhboerderij", batch = BATCH, timeoutTicks = 2400)
    public static void boerderijOogstEnPlantage(GameTestHelper helper) {
        boerderij(helper);
    }

    @GuhTest(template = "guhboerderij", batch = BATCH, timeoutTicks = 2400, rotationSteps = 1)
    public static void boerderijOogstEnPlantageGedraaid(GameTestHelper helper) {
        boerderij(helper);
    }

    @GuhTest(template = "guhboerderij", batch = BATCH, timeoutTicks = 400)
    public static void boerderijHendels(GameTestHelper helper) {
        hendels(helper, 2);
    }

    private static void boerderij(GameTestHelper helper) {
        // the farm is protected (as in the world): its own Oogster and Plantagebak still work in it
        BoundingBox boerderij = BoundingBox.fromCorners(BlockPos.containing(helper.getBounds().minX, helper.getBounds().minY, helper.getBounds().minZ),
                BlockPos.containing(helper.getBounds().maxX - 1, helper.getBounds().maxY - 1, helper.getBounds().maxZ - 1));
        BoerderijProtection.TEST_AREAS.add(boerderij);
        try {
            KrachtProef.draait(helper, 2, 2, 5, g -> {
                helper.assertTrue(inKist(helper, Items.WHEAT) > 0, "wheat in the chest (Oogster -> Richtingstuk -> Knabbelbuis)");
                helper.assertTrue(inKist(helper, Items.OAK_LOG) > 0, "oak logs in the chest by the Plantagebak (the klusjes-guh chopped the tree)");
                BoerderijProtection.TEST_AREAS.remove(boerderij);
            });
        } catch (RuntimeException e) {
            BoerderijProtection.TEST_AREAS.remove(boerderij);
            throw e;
        }
    }

    // =====================================================================================================================
    // the levers: on = the building's guh takes a break (the whole setup stops), off = it runs on
    // =====================================================================================================================
    private static void hendels(GameTestHelper helper, int verwacht) {
        ServerLevel level = helper.getLevel();
        List<BlockPos> wielen = KrachtProef.zoek(helper).wielen();
        List<BlockPos> hendels = new ArrayList<>();
        for (BlockPos w : wielen) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(w.relative(d)).is(Blocks.LEVER) && !hendels.contains(w.relative(d))) {
                    hendels.add(w.relative(d).immutable());
                }
            }
        }
        helper.assertTrue(hendels.size() == verwacht, verwacht + " lever(s) next to the wheels, found " + hendels.size());
        int[] fase = {0};
        long[] sinds = {0};
        helper.succeedWhen(() -> {
            long nu = helper.getTick();
            switch (fase[0]) {
                case 0 -> {   // everything runs: pull every lever
                    for (BlockPos w : wielen) {
                        helper.assertTrue(VadsKracht.net(level, w).draait(), "the net of the Guhrad at " + w + " runs first");
                    }
                    for (BlockPos h : hendels) {
                        trek(level, h);
                    }
                    fase[0] = 1;
                    sinds[0] = nu;
                    helper.fail("levers pulled");
                }
                case 1 -> {   // every guh takes a break, every net stands still
                    for (BlockPos h : hendels) {
                        helper.assertTrue(level.getBlockState(h).getValue(LeverBlock.POWERED), "the lever at " + h + " is on");
                    }
                    for (BlockPos w : wielen) {
                        GuhWheelBlockEntity be = (GuhWheelBlockEntity) level.getBlockEntity(w);
                        helper.assertTrue(be != null && be.rust() && be.vadsAanbod() == 0, "the guh in the wheel at " + w + " takes a break");
                        helper.assertFalse(level.getBlockState(w).getValue(GuhWheelBlock.RUNNING), "the wheel at " + w + " stands still");
                        helper.assertFalse(VadsKracht.net(level, w).draait(), "the net of the Guhrad at " + w + " stands still");
                    }
                    for (BlockPos h : hendels) {
                        trek(level, h);
                    }
                    fase[0] = 2;
                    helper.fail("levers pulled back");
                }
                default -> {   // and it all runs again
                    for (BlockPos w : wielen) {
                        GuhWheelBlockEntity be = (GuhWheelBlockEntity) level.getBlockEntity(w);
                        helper.assertTrue(be != null && !be.rust() && be.vadsAanbod() > 0, "the guh in the wheel at " + w + " runs again");
                        helper.assertTrue(VadsKracht.net(level, w).draait(), "the net of the Guhrad at " + w + " runs again");
                    }
                }
            }
        });
    }

    /** Flips the lever at this (absolute) spot, as a player's click does. */
    private static void trek(ServerLevel level, BlockPos h) {
        var state = level.getBlockState(h);
        ((LeverBlock) state.getBlock()).pull(state, level, h, null);
    }

    // =====================================================================================================================
    // small checks
    // =====================================================================================================================
    private static void maalt(GameTestHelper helper, KrachtProef.Gevonden g, int molens) {
        ServerLevel level = helper.getLevel();
        long n = g.machines().stream().filter(m -> level.getBlockEntity(m) instanceof VadsmolenBlockEntity).count();
        helper.assertTrue(n == molens, molens + " Vadsmolen(s), found " + n);
        helper.assertTrue(g.machines().stream().allMatch(m -> !(level.getBlockEntity(m) instanceof VadsmolenBlockEntity molen)
                || molen.vakken().getAmountAsInt(VadsmolenBlockEntity.UIT) > 0), "every Vadsmolen grinds");
    }

    private static void grilt(GameTestHelper helper, KrachtProef.Gevonden g) {
        helper.assertTrue(!g.ovens().isEmpty() && g.ovens().stream().allMatch(o -> helper.getLevel().getBlockState(o).getValue(AbstractFurnaceBlock.LIT)),
                "every Guhoven bakes");
    }

    private static void vatenVol(GameTestHelper helper, KrachtProef.Gevonden g, int vaten) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(g.vaten().size() == vaten, vaten + " Sausvaten, found " + g.vaten().size());
        helper.assertTrue(g.vaten().stream().anyMatch(v -> level.getBlockEntity(v) instanceof SausvatBlockEntity vat && vat.tank().inhoud() > 0),
                "the pumps fill the Sausvaten");
    }

    /** How many of this item lie in the chests of the test. */
    private static int inKist(GameTestHelper helper, Item item) {
        int n = 0;
        for (BlockPos p : blokken(helper)) {
            BlockEntity be = helper.getLevel().getBlockEntity(p);
            if (be instanceof ChestBlockEntity kist) {
                Container c = kist;
                for (int i = 0; i < c.getContainerSize(); i++) {
                    if (c.getItem(i).is(item)) {
                        n += c.getItem(i).getCount();
                    }
                }
            }
        }
        return n;
    }

    private static Iterable<BlockPos> blokken(GameTestHelper helper) {
        AABB box = helper.getBounds();
        return BlockPos.betweenClosed((int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ),
                (int) Math.ceil(box.maxX) - 1, (int) Math.ceil(box.maxY) - 1, (int) Math.ceil(box.maxZ) - 1);
    }
}
