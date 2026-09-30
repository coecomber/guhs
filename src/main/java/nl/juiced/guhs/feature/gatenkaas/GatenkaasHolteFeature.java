package nl.juiced.guhs.feature.gatenkaas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModFluids;

/**
 * One "hole in the cheese" of the Gatenkaasgrotten (a carver-like feature, placed in raw generation, before anything
 * else): a big round hole with smaller round holes bubbling out of it (and sometimes a string of tiny holes), like the
 * inside of a real Swiss cheese. Holes of neighbouring chunks overlap into a cheesy maze. Its walls become gatenkaas
 * (with a bit of kaaskorrel ore), the floor gets glowing kaasmos, the ceiling kaas stalactites (stalagmites below), and
 * some holes have a little kaassaus pool at the bottom or a kaassaus drip from the ceiling.
 * <p>
 * It stays well under the surface ({@link #SURFACE_MARGIN}) and between {@link #MIN_Y} and {@link #MAX_Y}; it only
 * eats natural rock, never a building (its hole runs after the buildings of a neighbouring chunk are placed, and those
 * are made of the same rock: it leaves the pieces of every structure around alone), and stays within {@link #REACH}
 * blocks of its chunk's middle (so it only writes in the neighbouring chunks, as a feature may).
 */
public class GatenkaasHolteFeature extends Feature<NoneFeatureConfiguration> {
    public static final int MIN_Y = 4, MAX_Y = 46, SURFACE_MARGIN = 10, REACH = 22;

    public GatenkaasHolteFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** A round hole: centre and radius. */
    private record Hole(double x, double y, double z, double r) {
        boolean contains(BlockPos p, double extra) {
            double dx = p.getX() + 0.5 - x, dy = p.getY() + 0.5 - y, dz = p.getZ() + 0.5 - z;
            return dx * dx + dy * dy + dz * dz <= (r + extra) * (r + extra);
        }
    }

    /** Natural rock of the Guhmension underground (and its ores): the only things a hole may eat. */
    public static boolean carvable(BlockState state) {
        return state.is(Blocks.PINK_WOOL) || state.is(Blocks.MAGENTA_WOOL) || state.is(Blocks.PINK_TERRACOTTA)
                || state.is(GatenkaasFeature.GATENKAAS.get()) || state.is(GatenkaasFeature.KAASKORRELERTS.get())
                || state.is(ModBlocks.BLOCK_OF_KAASKNABBELS.get()) || state.is(ModBlocks.COMPRESSED_SUPER_VAHOEGE_VADS.get());
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int mx = (origin.getX() & ~15) + 8, mz = (origin.getZ() & ~15) + 8;
        List<Hole> holes = holes(random, mx + random.nextInt(5) - 2, origin.getY(), mz + random.nextInt(5) - 2, mx, mz);
        Hole main = holes.get(0);

        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        for (Hole h : holes) {
            x0 = Math.min(x0, (int) Math.floor(h.x - h.r) - 2);
            x1 = Math.max(x1, (int) Math.ceil(h.x + h.r) + 2);
            y0 = Math.min(y0, (int) Math.floor(h.y - h.r) - 2);
            y1 = Math.max(y1, (int) Math.ceil(h.y + h.r) + 2);
            z0 = Math.min(z0, (int) Math.floor(h.z - h.r) - 2);
            z1 = Math.max(z1, (int) Math.ceil(h.z + h.r) + 2);
        }
        x0 = Math.max(x0, mx - REACH - 2);
        x1 = Math.min(x1, mx + REACH + 1);
        z0 = Math.max(z0, mz - REACH - 2);
        z1 = Math.min(z1, mz + REACH + 1);
        y0 = Math.max(y0, MIN_Y - 1);
        y1 = Math.min(y1, MAX_Y + 1);

        List<net.minecraft.world.level.levelgen.structure.BoundingBox> buildings = nl.juiced.guhs.world.BouwRuimte.piecesNear(level, x0, z0, x1, z1);

        // 1. carve
        Set<BlockPos> carved = new HashSet<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int top = Math.min(MAX_Y, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - SURFACE_MARGIN);
                for (int y = Math.max(y0, MIN_Y); y <= Math.min(y1, top); y++) {
                    p.set(x, y, z);
                    if (inAny(holes, p, 0) && carvable(level.getBlockState(p)) && !nl.juiced.guhs.world.BouwRuimte.inAny(buildings, p)) {
                        level.setBlock(p, Blocks.CAVE_AIR.defaultBlockState(), 2);
                        carved.add(p.immutable());
                    }
                }
            }
        }
        if (carved.isEmpty()) {
            return false;
        }
        // 2. the walls turn to cheese (also where the hole pokes out of the gatenkaas biome), with a bit of kaaskorrel
        BlockState gatenkaas = GatenkaasFeature.GATENKAAS.get().defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    p.set(x, y, z);
                    if (!carved.contains(p) && inAny(holes, p, 2.2) && carvable(level.getBlockState(p)) && !nl.juiced.guhs.world.BouwRuimte.inAny(buildings, p)) {
                        boolean exposed = touches(carved, p);
                        level.setBlock(p, exposed && random.nextInt(70) == 0 ? GatenkaasFeature.KAASKORRELERTS.get().defaultBlockState() : gatenkaas, 2);
                    }
                }
            }
        }
        // 3. a kaassaus pool in the bottom of the big hole (only cells that can't leak)
        Set<BlockPos> pool = new HashSet<>();
        if (random.nextInt(3) == 0) {
            int bottom = (int) Math.floor(main.y - main.r);
            for (BlockPos c : carved) {
                if (c.getY() <= bottom + 2 && main.contains(c, 0)) {
                    pool.add(c);
                }
            }
            boolean changed = true;
            while (changed) {
                changed = false;
                for (BlockPos c : List.copyOf(pool)) {
                    boolean leaks = carved.contains(c.below()) && !pool.contains(c.below());
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        BlockPos n = c.relative(d);
                        leaks |= carved.contains(n) && !pool.contains(n) || !carved.contains(n) && !level.getBlockState(n).isSolid();
                    }
                    if (leaks) {
                        pool.remove(c);
                        changed = true;
                    }
                }
            }
            BlockState saus = ModBlocks.KAAS_SAUS.get().defaultBlockState();
            for (BlockPos c : pool) {
                level.setBlock(c, saus, 2);
            }
        }
        // 4. moss on the floors, stalactites and stalagmites, a drip of kaassaus here and there
        boolean drips = random.nextInt(3) == 0;
        for (BlockPos c : carved) {
            if (pool.contains(c) || !level.getBlockState(c).isAir()) {
                continue;
            }
            BlockPos below = c.below(), above = c.above();
            boolean floor = !carved.contains(below) && level.getBlockState(below).is(GatenkaasFeature.GATENKAAS.get());
            boolean ceiling = !carved.contains(above) && level.getBlockState(above).is(GatenkaasFeature.GATENKAAS.get());
            if (floor) {
                if (mossy(c)) {
                    level.setBlock(below, GatenkaasFeature.KAASMOS.get().defaultBlockState(), 2);
                    if (random.nextInt(4) == 0) {
                        level.setBlock(c, GatenkaasFeature.KAASMOS_TAPIJT.get().defaultBlockState(), 2);
                        continue;
                    }
                } else if (random.nextInt(22) == 0) {
                    grow(level, carved, c, Direction.UP, 1 + random.nextInt(3));
                    continue;
                }
            }
            if (ceiling) {
                if (drips && random.nextInt(260) == 0) {
                    level.setBlock(above, ModBlocks.KAAS_SAUS.get().defaultBlockState(), 2);
                    level.scheduleTick(above, ModFluids.KAAS_SAUS.get(), 5);
                } else if (random.nextInt(14) == 0) {
                    grow(level, carved, c, Direction.DOWN, 1 + random.nextInt(random.nextInt(8) == 0 ? 6 : 3));
                }
            }
        }
        return true;
    }

    /** The big hole and the smaller ones bubbling out of it. */
    private static List<Hole> holes(RandomSource random, int cx, int cy, int cz, int mx, int mz) {
        List<Hole> out = new ArrayList<>();
        double r = 5 + random.nextInt(8) + random.nextDouble();
        out.add(clamp(cx, cy, cz, r, mx, mz));
        int small = 2 + random.nextInt(4);
        for (int i = 0; i < small; i++) {
            double r2 = 2.5 + random.nextDouble() * 4.5;
            double yaw = random.nextDouble() * Math.PI * 2, pitch = (random.nextDouble() - 0.4) * 1.4;
            double d = (r + r2) * (0.5 + random.nextDouble() * 0.35);
            out.add(clamp(cx + Math.cos(yaw) * Math.cos(pitch) * d, cy + Math.sin(pitch) * d, cz + Math.sin(yaw) * Math.cos(pitch) * d, r2, mx, mz));
        }
        if (random.nextInt(3) == 0) {                   // a string of tiny holes, the way cheese holes join up
            double yaw = random.nextDouble() * Math.PI * 2, x = cx, y = cy - r * 0.3, z = cz;
            for (int i = 0, n = 4 + random.nextInt(4); i < n; i++) {
                x += Math.cos(yaw) * 2.6;
                z += Math.sin(yaw) * 2.6;
                y += random.nextDouble() * 2 - 1;
                yaw += random.nextDouble() * 0.8 - 0.4;
                out.add(clamp(x + Math.cos(yaw) * r, y, z + Math.sin(yaw) * r, 1.4 + random.nextDouble() * 1.1, mx, mz));
            }
        }
        return out;
    }

    /** Keeps a hole within reach of the chunk's middle (shrinking it if needed). */
    private static Hole clamp(double x, double y, double z, double r, int mx, int mz) {
        r = Math.max(1.3, Math.min(r, REACH - 1));
        x = Math.max(mx - REACH + r, Math.min(mx + REACH - r, x));
        z = Math.max(mz - REACH + r, Math.min(mz + REACH - r, z));
        y = Math.max(MIN_Y + 1 + r * 0.5, Math.min(MAX_Y - 2 - r * 0.5, y));
        return new Hole(x, y, z, r);
    }

    private static boolean inAny(List<Hole> holes, BlockPos p, double extra) {
        for (Hole h : holes) {
            if (h.contains(p, extra)) {
                return true;
            }
        }
        return false;
    }

    private static boolean touches(Set<BlockPos> carved, BlockPos p) {
        for (Direction d : Direction.values()) {
            if (carved.contains(p.relative(d))) {
                return true;
            }
        }
        return false;
    }

    /** Patches of kaasmos: a cheap wobbly pattern, the same for every hole. */
    private static boolean mossy(BlockPos c) {
        return Math.sin(c.getX() * 0.35 + c.getY() * 0.2) + Math.cos(c.getZ() * 0.31 - c.getX() * 0.12) > 0.25;
    }

    /** A kaas stalactite (down) or stalagmite (up) of this length, into the hole (never through anything). */
    private static void grow(WorldGenLevel level, Set<BlockPos> carved, BlockPos start, Direction dir, int length) {
        int n = 0;
        while (n < length && carved.contains(start.relative(dir, n)) && level.getBlockState(start.relative(dir, n)).isAir()
                && level.getBlockState(start.relative(dir, n + 1)).isAir()) {
            n++;
        }
        if (n == 0) {
            return;
        }
        for (int i = 0; i < n; i++) {
            DripstoneThickness t = i == n - 1 ? DripstoneThickness.TIP : i == n - 2 ? DripstoneThickness.FRUSTUM
                    : i == 0 ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
            level.setBlock(start.relative(dir, i), GatenkaasFeature.KAAS_STALACTIET.get().defaultBlockState()
                    .setValue(KaasStalactietBlock.TIP_DIRECTION, dir).setValue(KaasStalactietBlock.THICKNESS, t), 2);
        }
    }
}
