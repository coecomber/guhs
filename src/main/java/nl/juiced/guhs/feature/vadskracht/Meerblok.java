package nl.juiced.guhs.feature.vadskracht;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Machines bigger than one block (the pattern of the Guhrad, generalised): one <i>kern</i> block (the controller, with the
 * block entity) and invisible part blocks ({@link MachineDeelBlock}) that fill the rest of the box. The kern is the bottom
 * block at the front, in the middle of the width: the box is {@code breed} wide (sideways, along
 * {@code facing.getClockWise()}; with an even width the extra column is on the clockwise side), {@code hoog} high (up from
 * the kern) and {@code diep} deep (backwards from the kern, away from {@code facing}). At most 5 x 5 x 5, and the kern at
 * most 2 blocks from any part sideways and backwards (a part remembers its offset in its block state).
 * <p>
 * A kern block that implements {@link Vorm} ({@link MachineBlock} does) gives its own size, so {@link #plaats} and
 * {@link #ruimOp} only need the kern.
 */
public final class Meerblok {
    /** The size of a kern block's machine. */
    public interface Vorm {
        int breed();

        int hoog();

        int diep();
    }

    /** The positions of the parts (everything of the box except the kern). */
    public static List<BlockPos> delen(BlockPos kern, Direction facing, int breed, int hoog, int diep) {
        if (breed < 1 || hoog < 1 || diep < 1 || breed > 5 || hoog > 5 || diep > 3) {
            throw new IllegalArgumentException("Meerblok: " + breed + "x" + hoog + "x" + diep + " past niet (hooguit 5 breed, 5 hoog, 3 diep)");
        }
        List<BlockPos> out = new ArrayList<>(breed * hoog * diep - 1);
        Direction opzij = facing.getClockWise(), naarAchter = facing.getOpposite();
        int links = (breed - 1) / 2;
        for (int d = 0; d < diep; d++) {
            for (int h = 0; h < hoog; h++) {
                for (int b = -links; b < breed - links; b++) {
                    if (b != 0 || h != 0 || d != 0) {
                        out.add(kern.relative(opzij, b).above(h).relative(naarAchter, d));
                    }
                }
            }
        }
        return out;
    }

    /** {@link #delen} for a kern block that knows its own size ({@link Vorm}); empty for any other block. */
    public static List<BlockPos> delen(BlockPos kern, BlockState kernState) {
        if (!(kernState.getBlock() instanceof Vorm vorm) || !kernState.hasProperty(MachineBlock.FACING)) {
            return List.of();
        }
        return delen(kern, kernState.getValue(MachineBlock.FACING), vorm.breed(), vorm.hoog(), vorm.diep());
    }

    /** Is there room for the parts (every spot replaceable)? */
    public static boolean past(Level level, BlockPos kern, BlockState kernState) {
        for (BlockPos deel : delen(kern, kernState)) {
            if (level.isOutsideBuildHeight(deel) || !level.getBlockState(deel).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    /** Places the part blocks around a kern that stands already. False = no room (nothing placed). */
    public static boolean plaats(Level level, BlockPos kern, BlockState kernState, Block deel) {
        if (!past(level, kern, kernState)) {
            return false;
        }
        for (BlockPos plek : delen(kern, kernState)) {
            level.setBlock(plek, MachineDeelBlock.staat(deel, plek.subtract(kern)), Block.UPDATE_ALL);
        }
        return true;
    }

    /** Removes the part blocks of a kern (that is being removed). */
    public static void ruimOp(Level level, BlockPos kern, BlockState kernState, Block deel) {
        for (BlockPos plek : delen(kern, kernState)) {
            BlockState state = level.getBlockState(plek);
            if (state.is(deel) && kern(state, plek).equals(kern)) {
                level.setBlock(plek, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** The kern a part block belongs to. */
    public static BlockPos kern(BlockState deelState, BlockPos deelPos) {
        return MachineDeelBlock.kern(deelState, deelPos);
    }

    private Meerblok() {
    }
}
