package nl.juiced.guhs.feature.bio.blokkenwolk;

import java.util.function.IntPredicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Which water is a BIG waterfall: the rule behind the foam, the mist and the rushing sound (drawn and played by the client
 * only, {@code client/WatervalEffecten}). Plain functions, so the rule can be tested without a client.
 * <p>
 * A fall is a column of FALLING water (vanilla's own flag: water that came down from the block above). Its foot is its
 * lowest block. Its height is how far the water drops: the falling blocks, plus one when it lands in water (the pool's own
 * top block is not "falling", but the water did drop past it). A fall of {@link #GROOT} or more is big; a river that steps
 * down one or two blocks never is. Any water counts, in any biome and any dimension; kaassaus and other fluids do not.
 */
public final class Waterval {
    /** From this height on a fall gets foam, mist and sound. */
    public static final int GROOT = 4;
    /** Falls are not measured further than this (taller ones count as this tall). */
    public static final int MAX_HOOGTE = 32;

    /** The foot of a fall: the y of its lowest falling block, how far the water drops, and whether it lands in water. */
    public record Voet(int y, int hoogte, boolean inWater) {
        public boolean groot() {
            return Waterval.groot(hoogte);
        }
    }

    public static boolean groot(int hoogte) {
        return hoogte >= GROOT;
    }

    /**
     * The fall this block is part of, or null when there is no falling water at y.
     *
     * @param valt  is there falling water at this y
     * @param water is there any water at this y (asked only for the block under the foot)
     */
    @Nullable
    public static Voet meet(IntPredicate valt, IntPredicate water, int y) {
        if (!valt.test(y)) {
            return null;
        }
        int voet = y;
        for (int i = 0; i < MAX_HOOGTE && valt.test(voet - 1); i++) {
            voet--;
        }
        int top = y;
        while (top - voet + 1 < MAX_HOOGTE && valt.test(top + 1)) {
            top++;
        }
        boolean inWater = water.test(voet - 1);
        return new Voet(voet, Math.min(MAX_HOOGTE, top - voet + 1 + (inWater ? 1 : 0)), inWater);
    }

    /** Falling water (not a source, not water flowing sideways). */
    public static boolean valt(BlockGetter level, BlockPos pos) {
        FluidState f = level.getFluidState(pos);
        return f.is(FluidTags.WATER) && f.hasProperty(FlowingFluid.FALLING) && f.getValue(FlowingFluid.FALLING);
    }

    /**
     * The highest fall in a column between two heights (looking down from yBoven), or null. One column costs at most
     * yBoven - yOnder fluid lookups; the client spreads a few columns over every tick.
     */
    @Nullable
    public static Voet zoek(BlockGetter level, int x, int z, int yBoven, int yOnder) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, yBoven, z);
        for (int y = yBoven; y >= yOnder; y--) {
            if (valt(level, p.setY(y))) {
                return meet(yy -> valt(level, p.setY(yy)), yy -> level.getFluidState(p.setY(yy)).is(FluidTags.WATER), y);
            }
        }
        return null;
    }

    private Waterval() {
    }
}
