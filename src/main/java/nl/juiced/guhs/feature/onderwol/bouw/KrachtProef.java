package nl.juiced.guhs.feature.onderwol.bouw;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.GuhWheelPartBlock;
import nl.juiced.guhs.block.GuhWireBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.feature.guhoven.GuhOvenBlockEntity;
import nl.juiced.guhs.feature.techsaus.SausvatBlockEntity;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsNet;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * 1.7.0 playtest: game-test checks for the working vadskracht setups of the underground buildings (the Kaasmakerij's
 * pomphuis, the Wolclub's krachtcentrale, the Vadsmijnstadje's Guhrad-centrale). A test places the building's template
 * itself (also turned: every jigsaw building is placed turned) and waits until everything of vadskracht in it runs:
 * <ul>
 *   <li>every Guhrad stands whole (its 8 parts point back at it), has a guh, and its net runs and gets what it gives;</li>
 *   <li>every machine has vadskracht and is awake, every Guhoven's net runs;</li>
 *   <li>every Guhdraad glows (its net runs) and every redstone lamp under a Guhdraad burns;</li>
 *   <li>and what the building asks on top ({@code extra}: a vat that fills, a mill that grinds).</li>
 * </ul>
 */
public final class KrachtProef {
    /** What of vadskracht stands in the test's bounds (absolute positions). */
    public record Gevonden(List<BlockPos> wielen, List<BlockPos> machines, List<BlockPos> ovens, List<BlockPos> draden,
                           List<BlockPos> lampen, List<BlockPos> vaten) {
    }

    public static Gevonden zoek(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AABB box = helper.getBounds();
        List<BlockPos> wielen = new ArrayList<>(), machines = new ArrayList<>(), ovens = new ArrayList<>(), draden = new ArrayList<>(),
                lampen = new ArrayList<>(), vaten = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed((int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ),
                (int) Math.ceil(box.maxX) - 1, (int) Math.ceil(box.maxY) - 1, (int) Math.ceil(box.maxZ) - 1)) {
            BlockState state = level.getBlockState(p);
            if (state.is(ModBlocks.GUH_WHEEL.get())) {
                wielen.add(p.immutable());
            } else if (state.is(ModBlocks.GUH_WIRE.get())) {
                draden.add(p.immutable());
                if (level.getBlockState(p.below()).is(Blocks.REDSTONE_LAMP)) {
                    lampen.add(p.below().immutable());
                }
            }
            if (state.hasBlockEntity()) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof MachineBlockEntity) {
                    machines.add(p.immutable());
                } else if (be instanceof GuhOvenBlockEntity) {
                    ovens.add(p.immutable());
                } else if (be instanceof SausvatBlockEntity) {
                    vaten.add(p.immutable());
                }
            }
        }
        return new Gevonden(wielen, machines, ovens, draden, lampen, vaten);
    }

    /**
     * Waits until all vadskracht of the building runs (see the class text), with at least this many wheels, machines (Guhovens
     * included) and lamps, and {@code extra} passes too (it may throw an assertion until then).
     */
    public static void draait(GameTestHelper helper, int wielen, int machines, int lampen, java.util.function.Consumer<Gevonden> extra) {
        Gevonden g = zoek(helper);
        helper.assertTrue(g.wielen().size() >= wielen, "at least " + wielen + " Guhrad(en), found " + g.wielen().size());
        helper.assertTrue(g.machines().size() + g.ovens().size() >= machines, "at least " + machines + " machines, found "
                + (g.machines().size() + g.ovens().size()));
        helper.assertTrue(g.lampen().size() >= lampen, "at least " + lampen + " lamps under Guhdraad, found " + g.lampen().size());
        ServerLevel level = helper.getLevel();
        helper.succeedWhen(() -> {
            for (BlockPos wiel : g.wielen()) {
                heel(helper, wiel);
                GuhWheelBlockEntity be = (GuhWheelBlockEntity) level.getBlockEntity(wiel);
                helper.assertTrue(be != null && be.hasGuh() && be.vadsAanbod() > 0, "the Guhrad at " + wiel + " has a running guh");
                helper.assertTrue(level.getBlockState(wiel).getValue(GuhWheelBlock.RUNNING), "the Guhrad at " + wiel + " turns");
                VadsNet net = VadsKracht.net(level, wiel);
                helper.assertTrue(net.draait() && net.aanbod() > 0 && net.teltMee(be),
                        "the net of the Guhrad at " + wiel + " runs (" + net.status() + ", " + net.vraag() + "/" + net.aanbod() + " VK)");
            }
            for (BlockPos m : g.machines()) {
                MachineBlockEntity be = (MachineBlockEntity) level.getBlockEntity(m);
                helper.assertTrue(be != null && be.heeftKracht() && be.snoet() != Snoet.SLAAPT,
                        "the machine " + level.getBlockState(m).getBlock() + " at " + m + " has vadskracht and is awake");
            }
            for (BlockPos o : g.ovens()) {
                helper.assertTrue(VadsKracht.heeftKracht(level, o), "the Guhoven at " + o + " has vadskracht");
            }
            for (BlockPos d : g.draden()) {
                helper.assertTrue(level.getBlockState(d).getValue(GuhWireBlock.POWERED), "the Guhdraad at " + d + " glows");
            }
            for (BlockPos l : g.lampen()) {
                helper.assertTrue(level.getBlockState(l).getValue(RedstoneLampBlock.LIT), "the lamp under the Guhdraad at " + l + " burns");
            }
            extra.accept(g);
        });
    }

    /** The wheel and its 8 parts: each part says this wheel is its wheel (also after the building was turned). */
    public static void heel(GameTestHelper helper, BlockPos wiel) {
        ServerLevel level = helper.getLevel();
        BlockState state = level.getBlockState(wiel);
        Direction facing = state.getValue(GuhWheelBlock.FACING);
        for (BlockPos deel : GuhWheelBlock.partPositions(wiel, facing)) {
            BlockState d = level.getBlockState(deel);
            helper.assertTrue(d.is(ModBlocks.GUH_WHEEL_PART.get()) && GuhWheelPartBlock.wheelPos(d, deel).equals(wiel),
                    "the Guhrad at " + wiel + " (facing " + facing + ") has its part at " + deel + " (" + d + ")");
        }
    }

    private KrachtProef() {
    }
}
