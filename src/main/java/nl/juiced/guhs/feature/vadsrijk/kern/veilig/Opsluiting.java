package nl.juiced.guhs.feature.vadsrijk.kern.veilig;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Is this player shut in?" A small flood fill over the spots a survival player can walk to from {@code start} (step up 1,
 * fall down, swim/climb up, doors count as open). Shut in = it never gets {@code straal} blocks (2D) away. Big spaces (more
 * than {@code maxKnopen} spots) count as open. Same rules as the map audit (tools/vadsrijk/audit), only local.
 */
public final class Opsluiting {
    private static final int MAX_VAL = 48;

    private Opsluiting() {
    }

    public static boolean ingesloten(BlockGetter w, BlockPos start, int straal, int maxKnopen) {
        BlockPos s = start;
        // the start: drop to the floor (the player may be mid-jump or swimming)
        for (int i = 0; i < 4 && staan(w, s) && !gedragen(w, s); i++) {
            s = s.below();
        }
        if (!staan(w, s)) {
            s = start;
        }
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        Set<BlockPos> gezien = new HashSet<>();
        rij.add(s);
        gezien.add(s);
        long r2 = (long) straal * straal;
        while (!rij.isEmpty()) {
            BlockPos n = rij.poll();
            long dx = n.getX() - start.getX(), dz = n.getZ() - start.getZ();
            if (dx * dx + dz * dz >= r2) {
                return false;
            }
            if (gezien.size() > maxKnopen) {
                return false;
            }
            // up: swimming or climbing
            if (klim(w, n) && staan(w, n.above())) {
                voeg(rij, gezien, n.above());
            }
            boolean kanSpringen = gedragen(w, n) || klim(w, n);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos m = n.relative(d);
                if (staan(w, m)) {
                    BlockPos v = m;
                    int val = 0;
                    while (!gedragen(w, v) && staan(w, v.below()) && val < MAX_VAL) {
                        v = v.below();
                        val++;
                    }
                    if (val >= MAX_VAL) {
                        return false;   // a long drop: out of here (or into the void: the void rescue takes over)
                    }
                    voeg(rij, gezien, v);
                } else if (kanSpringen && staan(w, m.above()) && vrij(w, n.above(2)) && laag(w, m)) {
                    voeg(rij, gezien, m.above());
                }
            }
        }
        return true;
    }

    private static void voeg(ArrayDeque<BlockPos> rij, Set<BlockPos> gezien, BlockPos p) {
        if (gezien.add(p)) {
            rij.add(p);
        }
    }

    /** Room for a player: feet and head free. */
    static boolean staan(BlockGetter w, BlockPos p) {
        return vrij(w, p) && vrij(w, p.above());
    }

    /** Standing on something (or held by water / a ladder). */
    static boolean gedragen(BlockGetter w, BlockPos p) {
        return !vrij(w, p.below()) || klim(w, p);
    }

    static boolean klim(BlockGetter w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        return s.is(BlockTags.CLIMBABLE) || s.getFluidState().is(FluidTags.WATER);
    }

    static boolean vrij(BlockGetter w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        if (s.getFluidState().is(FluidTags.LAVA)) {
            return false;
        }
        if (s.is(BlockTags.CLIMBABLE)) {
            return true;   // (ladders/vines have a thin shape, but you are inside them)
        }
        if ((s.getBlock() instanceof DoorBlock || s.getBlock() instanceof FenceGateBlock || s.getBlock() instanceof TrapDoorBlock)
                && !s.is(Blocks.IRON_DOOR) && !s.is(Blocks.IRON_TRAPDOOR)) {
            return true;
        }
        return s.getCollisionShape(w, p).isEmpty();
    }

    /** At most one block high (fences and walls are 1.5: you can't jump onto them). */
    static boolean laag(BlockGetter w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        var vorm = s.getCollisionShape(w, p);
        return vorm.isEmpty() || vorm.max(net.minecraft.core.Direction.Axis.Y) <= 1.0;
    }
}
