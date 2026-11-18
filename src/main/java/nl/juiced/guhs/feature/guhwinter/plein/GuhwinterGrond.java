package nl.juiced.guhs.feature.guhwinter.plein;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The ground under a square (or the arrival), one column at a time. Inside the rectangle everything above the floor goes and
 * the ground below is filled up to it; in a band around it the natural ground slopes back (smoothstep), cut where it is higher
 * and filled where it is lower. The top block of a cut column stays what it was (pink wool for dirt), the fill is pink wool,
 * the Guhmension's own ground. Trees whose trunk is cut go all the way ({@link #bomen}).
 */
final class GuhwinterGrond {
    static final int VLAG = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final String[] BOOM = {"leaves", "bladeren", "_blad", "naalden", "_log", "_stam", "paddenstoel", "_gezicht"};
    /** Never fill deeper than this below the floor. */
    private static final int DIEPTE = 24;

    /** A rectangle with its floor and band. */
    record Vlak(int x1, int z1, int x2, int z2, int vloer, int band) {
        double afstand(int x, int z) {
            return Math.hypot(Math.max(0, Math.max(x1 - x, x - x2)), Math.max(0, Math.max(z1 - z, z - z2)));
        }

        boolean binnen(int x, int z) {
            return x >= x1 && x <= x2 && z >= z1 && z <= z2;
        }
    }

    private GuhwinterGrond() {
    }

    static boolean boom(BlockState s) {
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) {
            return true;
        }
        if (s.isAir() || !s.getFluidState().isEmpty()) {
            return false;
        }
        String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        for (String deel : BOOM) {
            if (id.contains(deel)) {
                return true;
            }
        }
        return false;
    }

    /** Not ground: air, water, plants, trees, thin blocks. */
    private static boolean zacht(ServerLevel level, BlockPos p, BlockState s) {
        return s.isAir() || !s.getFluidState().isEmpty() || boom(s) || !s.isCollisionShapeFullBlock(level, p);
    }

    private static double smooth(double u) {
        u = Math.max(0, Math.min(1, u));
        return u * u * (3 - 2 * u);
    }

    /**
     * One column. Returns the number of blocks changed. {@code alleenBinnen}: only the rectangle itself (a later square on
     * ground that was already shaped). Tree blocks that were cut are handed to {@code bomen}.
     */
    static int kolom(ServerLevel level, Vlak v, int x, int z, boolean alleenBinnen, Set<Long> bomen) {
        double t = v.afstand(x, z);
        if (t >= v.band() || (alleenBinnen && t > 0)) {
            return 0;
        }
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, top, z);
        int h = top;
        for (; h > level.getMinY(); h--) {
            m.setY(h);
            if (!zacht(level, m, level.getBlockState(m))) {
                break;
            }
        }
        int doel = t == 0 ? v.vloer() : (int) Math.round(v.vloer() + (h - v.vloer()) * smooth(t / v.band()));
        if (t > 0 && doel == h) {
            return 0;
        }
        m.setY(h);
        BlockState blad = level.getBlockState(m);
        if (blad.is(Blocks.DIRT) || blad.is(Blocks.GRASS_BLOCK) || zacht(level, m, blad) || level.getBlockEntity(m) != null) {
            blad = Blocks.PINK_WOOL.defaultBlockState();
        }
        int n = 0;
        for (int y = top; y > doel; y--) {
            m.setY(y);
            BlockState s = level.getBlockState(m);
            if (!s.isAir()) {
                if (boom(s)) {
                    bomen.add(m.asLong());
                }
                level.setBlock(m, Blocks.AIR.defaultBlockState(), VLAG);
                n++;
            }
        }
        if (doel != h) {
            m.setY(doel);
            level.setBlock(m, blad, VLAG);
            n++;
        }
        BlockState wol = Blocks.PINK_WOOL.defaultBlockState();
        for (int y = doel - 1; y > h && y >= v.vloer() - DIEPTE; y--) {
            m.setY(y);
            level.setBlock(m, wol, VLAG);
            n++;
        }
        return n;
    }

    /** The rest of the trees whose trunks were cut (connected logs and leaves, at most {@code max} blocks). */
    static int bomen(ServerLevel level, Set<Long> start, int max, java.util.function.Predicate<BlockPos> vrij) {
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        Set<Long> gezien = new HashSet<>(start);
        for (long l : start) {
            rij.add(BlockPos.of(l));
        }
        int n = 0;
        while (!rij.isEmpty() && n < max) {
            BlockPos p = rij.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos q = p.offset(dx, dy, dz);
                        if (gezien.add(q.asLong()) && level.isLoaded(q) && boom(level.getBlockState(q)) && !vrij.test(q)) {
                            level.setBlock(q, Blocks.AIR.defaultBlockState(), VLAG);
                            n++;
                            rij.add(q);
                        }
                    }
                }
            }
        }
        return n;
    }
}
