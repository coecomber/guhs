package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/**
 * One job of a helper family (design 2.8): where its board stands, which animal, which task and variant, how big (the
 * happier the family, the bigger and prettier: {@link #grootte} 0..2), how far it may look ({@link #zoekStraal}, 48 from
 * the board), plus what the templates ask of the terrain around (the ground, the water, trees).
 */
public record Klus(ServerLevel level, BlockPos bord, String soort, String taak, int variant, int grootte, int zoekStraal, RandomSource r) {
    /** How far from the board a helper project may lie (design: "tot 48 blokken van het bord"). */
    public static final int MAX_AFSTAND = 48;

    /** A template that cannot (no water, no tree...): the message key under gui.guhs.bouwopdracht.nee.* */
    public static final class Nee extends RuntimeException {
        public final String sleutel;

        public Nee(String sleutel) {
            super(sleutel, null, false, false);
            this.sleutel = sleutel;
        }
    }

    static Nee nee(String sleutel) {
        return new Nee(sleutel);
    }

    // --- the ground ----------------------------------------------------------------------------------------------------------

    /** Loose stuff that is not ground: plants, snow layers, leaves, logs, fluids. */
    boolean losOfLeeg(BlockState s) {
        return s.isAir() || s.is(Blocks.BARRIER) || !s.getFluidState().isEmpty() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)
                || s.is(Blocks.SNOW) || !s.blocksMotion();
    }

    /** The y of the top ground block (solid, not a tree, not under water's surface: the bed under water). */
    int grond(int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        int min = level.getMinY();
        while (y > min && losOfLeeg(level.getBlockState(new BlockPos(x, y, z)))) {
            y--;
        }
        return y;
    }

    BlockPos grondPos(int x, int z) {
        return new BlockPos(x, grond(x, z), z);
    }

    /** The y of the topmost block of the column that is not air (nor a barrier: a test's roof, the world's edge). */
    int top(int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        int min = level.getMinY();
        while (y > min) {
            BlockState s = level.getBlockState(new BlockPos(x, y, z));
            if (!s.isAir() && !s.is(Blocks.BARRIER)) {
                break;
            }
            y--;
        }
        return y;
    }

    /** The y of the water's surface in this column, or Integer.MIN_VALUE when there is no water on top. */
    int water(int x, int z) {
        BlockPos p = new BlockPos(x, top(x, z), z);
        BlockState s = level.getBlockState(p);
        if (s.is(Blocks.LILY_PAD)) {
            p = p.below();
            s = level.getBlockState(p);
        }
        return s.getFluidState().is(Fluids.WATER) || s.getFluidState().is(Fluids.FLOWING_WATER) ? p.getY() : Integer.MIN_VALUE;
    }

    boolean isWater(int x, int z) {
        return water(x, z) != Integer.MIN_VALUE;
    }

    boolean isWater(BlockPos p) {
        return !level.getBlockState(p).getFluidState().isEmpty() && level.getBlockState(p).getFluidState().is(net.minecraft.tags.FluidTags.WATER);
    }

    /** Is this a natural block (terrain the helpers may dig or flatten, never a player's build)? */
    static boolean natuur(BlockState s) {
        return aarde(s) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY)
                || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.SNOW) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.ICE)
                || s.is(Blocks.PACKED_ICE) || s.is(Blocks.MUD) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE) || s.is(Blocks.TERRACOTTA)
                || s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.REPLACEABLE)
                || s.is(BlockTags.LEAVES) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.FARMLAND)
                || s.is(BlockTags.SAPLINGS) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.CALCITE)
                || s.is(Blocks.TUFF) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL);
    }

    /** Earth plants grow in (the dirt tag, and the usual ones by name in case a version moves them). */
    static boolean aarde(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL)
                || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MUD);
    }

    BlockState staat(BlockPos p) {
        return level.getBlockState(p);
    }

    /** A rotation-free direction toward higher ground from p (the hillside a tunnel goes into), or null. */
    @Nullable
    Direction helling(BlockPos p, int ver, int min) {
        Direction beste = null;
        int top = min - 1;
        int g = grond(p.getX(), p.getZ());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int h = grond(p.getX() + d.getStepX() * ver, p.getZ() + d.getStepZ() * ver) - g;
            if (h > top) {
                top = h;
                beste = d;
            }
        }
        return beste;
    }

    /** Every column within r of the board (x, z), nearest first, in a stable order. */
    List<BlockPos> rondBord(int r) {
        List<BlockPos> uit = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r) {
                    uit.add(new BlockPos(bord.getX() + dx, 0, bord.getZ() + dz));
                }
            }
        }
        uit.sort(java.util.Comparator.comparingInt(p -> (p.getX() - bord.getX()) * (p.getX() - bord.getX()) + (p.getZ() - bord.getZ()) * (p.getZ() - bord.getZ())));
        return uit;
    }

    /** Is this column within the job's reach of the board? */
    boolean binnenBereik(BlockPos p) {
        int dx = p.getX() - bord.getX(), dz = p.getZ() - bord.getZ();
        return dx * dx + dz * dz <= zoekStraal * zoekStraal;
    }

    /** A spot of dry, roughly flat ground near the board (not the board's own column): the default "Zoek plek". */
    BlockPos vlakkePlek(int half) {
        for (BlockPos k : rondBord(zoekStraal)) {
            int d2 = (k.getX() - bord.getX()) * (k.getX() - bord.getX()) + (k.getZ() - bord.getZ()) * (k.getZ() - bord.getZ());
            if (d2 < (half + 3) * (half + 3)) {
                continue;
            }
            if (!level.isLoaded(k)) {
                continue;
            }
            int g = grond(k.getX(), k.getZ());
            boolean ok = true;
            for (int dx = -half; dx <= half && ok; dx += Math.max(1, half)) {
                for (int dz = -half; dz <= half && ok; dz += Math.max(1, half)) {
                    int x = k.getX() + dx, z = k.getZ() + dz;
                    ok = !isWater(x, z) && Math.abs(grond(x, z) - g) <= 1 && natuur(level.getBlockState(new BlockPos(x, grond(x, z), z)));
                }
            }
            if (ok) {
                return new BlockPos(k.getX(), g, k.getZ());
            }
        }
        throw nee("geen_plek");
    }
}
