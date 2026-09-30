package nl.juiced.guhs.feature.vadswoud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The giant guh tree of the Vadswoud: a thick round trunk (2-3.5 blocks radius) with a root flare and roots over the
 * ground, little guh faces in its bark (vadshout_gezicht, looking out on every side), 4-6 branches with leaf clouds and a
 * huge crown, 25-40 blocks high. Everything stays within {@link #REACH} blocks of the origin (a feature may only write
 * in the chunks around its own). Worldgen trees keep away from structures and from each other; four saplings in a
 * square grow one too (smaller), and the players nearby get an advancement.
 */
public class ReuzenguhboomFeature extends Feature<NoneFeatureConfiguration> {
    public static final int REACH = 15;
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public ReuzenguhboomFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        boolean fromSaplings = level instanceof ServerLevel;
        Tree tree = grow(level, context.random(), context.origin(), fromSaplings);
        if (tree != null && fromSaplings && level instanceof ServerLevel server) {
            for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(context.origin()).inflate(32))) {
                VadsAdvancements.grant(player, "vadswoud_reuzenboom");
                VadsAdvancements.award(player, "guhmension/vadswoud_reuzenboom");
                player.sendOverlayMessage(Component.translatable("gui.guhs.vadswoud.reuzenboom").withStyle(ChatFormatting.GREEN));
            }
        }
        return tree != null;
    }

    /** What was placed (for the game tests). */
    public record Tree(int logs, int leaves, int faces, int height) {
    }

    /**
     * Grows a giant guh tree with the middle of its trunk at the corner of origin (x+1, z+1: the middle of a 2x2 of saplings).
     * Returns null when there's no room (or, in worldgen, a structure or another giant tree is too close).
     */
    @Nullable
    public static Tree grow(WorldGenLevel level, RandomSource rnd, BlockPos origin, boolean fromSaplings) {
        int base = origin.getY();
        double cx = origin.getX() + 1.0, cz = origin.getZ() + 1.0;
        BlockPos below = origin.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP) || ground.is(BlockTags.LEAVES) || ground.is(BlockTags.LOGS)) {
            return null;
        }
        if (!fromSaplings && (nearStructure(level, origin) || nearOtherGiant(level, origin))) {
            return null;
        }
        double r = fromSaplings ? 2.0 + rnd.nextDouble() * 0.8 : 2.2 + rnd.nextDouble() * 1.2;
        int height = fromSaplings ? 22 + rnd.nextInt(8) : 26 + rnd.nextInt(13);
        if (base + height + 12 >= level.getMaxY() + 1) {
            return null;
        }
        int top = base + height;
        Map<BlockPos, BlockState> logs = new HashMap<>();
        BlockState log = VadswoudFeature.VADSHOUT_STAM.get().defaultBlockState();
        int taperFrom = base + (int) (height * 0.6);
        // --- the trunk, its flare and roots ---
        Map<Integer, Set<Long>> cells = new HashMap<>();
        for (int y = base - 3; y <= top; y++) {
            double rr = y < base ? r + 2.0 : y <= base + 6 ? r + 2.2 * Math.exp(-(y - base) / 1.8) : r;
            if (y > taperFrom) {
                rr = r * (1 - 0.42 * (y - taperFrom) / (double) (top - taperFrom));
            }
            for (int x = (int) Math.floor(cx - rr - 1); x <= cx + rr + 1; x++) {
                for (int z = (int) Math.floor(cz - rr - 1); z <= cz + rr + 1; z++) {
                    if (dist(x + 0.5, z + 0.5, cx, cz) <= rr + 0.3) {
                        logs.put(new BlockPos(x, y, z), log);
                        cells.computeIfAbsent(y, k -> new HashSet<>()).add(BlockPos.asLong(x, 0, z));
                    }
                }
            }
        }
        int roots = 5 + rnd.nextInt(3);
        for (int i = 0; i < roots; i++) {
            double a = (i + rnd.nextDouble() * 0.6) / roots * Math.PI * 2;
            double dx = Math.cos(a), dz = Math.sin(a), len = 3.5 + rnd.nextDouble() * 3;
            Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
            for (double s = r; s <= r + len; s += 0.5) {
                int y = s < r + len * 0.6 ? base : base - 1;
                logs.putIfAbsent(BlockPos.containing(cx + dx * s, y, cz + dz * s), log.setValue(RotatedPillarBlock.AXIS, axis));
            }
        }
        // --- little guh faces in the bark, looking out ---
        int faces = 0;
        List<BlockPos> faceSpots = new ArrayList<>();
        for (int y = base + 2; y <= taperFrom; y += 2) {
            Set<Long> ring = cells.get(y);
            for (long c : ring) {
                int x = BlockPos.getX(c), z = BlockPos.getZ(c);
                for (Direction d : SIDES) {
                    Direction side = d.getClockWise();
                    if (!ring.contains(BlockPos.asLong(x + d.getStepX(), 0, z + d.getStepZ()))
                            && ring.contains(BlockPos.asLong(x + side.getStepX(), 0, z + side.getStepZ()))
                            && ring.contains(BlockPos.asLong(x - side.getStepX(), 0, z - side.getStepZ()))) {
                        faceSpots.add(new BlockPos(x, y, z));
                        if (rnd.nextFloat() < 0.07f) {
                            logs.put(new BlockPos(x, y, z), face(d, rnd));
                            faces++;
                        }
                        break;
                    }
                }
            }
        }
        if (faces < 2 && !faceSpots.isEmpty()) {   // at least two faces, at the front and the back
            for (BlockPos p : List.of(faceSpots.get(0), faceSpots.get(faceSpots.size() / 2))) {
                Direction d = outward(p, cx, cz);
                logs.put(p, face(d, rnd));
                faces++;
            }
        }
        // --- branches with leaf clouds, and the crown ---
        List<double[]> blobs = new ArrayList<>();   // x, y, z, horizontal radius, vertical radius
        double crown = fromSaplings ? 6.5 + rnd.nextDouble() * 1.5 : 7.5 + rnd.nextDouble() * 2.0;
        blobs.add(new double[]{cx, top + 1.5, cz, crown, 4.5 + rnd.nextDouble()});
        int branches = 4 + rnd.nextInt(3);
        for (int i = 0; i < branches; i++) {
            double a = (i + rnd.nextDouble() * 0.5) / branches * Math.PI * 2;
            double y0 = base + height * (0.55 + rnd.nextDouble() * 0.3), len = 5 + rnd.nextDouble() * 3;
            double[] end = branch(logs, log, cx, y0, cz, Math.cos(a), Math.sin(a), r * 0.5, len, 0.35 + rnd.nextDouble() * 0.25);
            blobs.add(new double[]{end[0], end[1] + 0.5, end[2], 3.8 + rnd.nextDouble() * 0.8, 2.5 + rnd.nextDouble() * 0.7});
        }
        for (int i = 0; i < 5; i++) {
            double a = (i + 0.3) / 5 * Math.PI * 2;
            double[] end = branch(logs, log, cx, top - 2, cz, Math.cos(a), Math.sin(a), 0, crown * 0.5, 0.25);
            blobs.add(new double[]{end[0], end[1], end[2], crown * 0.55, 3.2});
        }
        Set<BlockPos> leafSpots = new HashSet<>();
        for (double[] b : blobs) {
            for (int x = (int) Math.floor(b[0] - b[3] - 1); x <= b[0] + b[3] + 1; x++) {
                for (int z = (int) Math.floor(b[2] - b[3] - 1); z <= b[2] + b[3] + 1; z++) {
                    for (int y = (int) Math.floor(b[1] - b[4] - 1); y <= b[1] + b[4] + 1; y++) {
                        double q = sq((x + 0.5 - b[0]) / b[3]) + sq((z + 0.5 - b[2]) / b[3]) + sq((y + 0.5 - b[1]) / b[4]);
                        double wobble = 0.8 + 0.28 * Math.sin(x * 1.7 + z * 2.3 + y * 0.9 + origin.getX());
                        if (q <= wobble) {
                            leafSpots.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
        }
        // strands of leaves hanging from the underside of the clouds
        for (BlockPos p : List.copyOf(leafSpots)) {
            if (!leafSpots.contains(p.below()) && !logs.containsKey(p.below()) && rnd.nextFloat() < 0.08f) {
                int n = 1 + rnd.nextInt(3);
                for (int k = 1; k <= n; k++) {
                    leafSpots.add(p.below(k));
                }
            }
        }
        leafSpots.removeAll(logs.keySet());
        // --- leaves only as far as a log can feed them (distance 1-6), so nothing decays ---
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        for (BlockPos p : logs.keySet()) {
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (leafSpots.contains(n) && !distance.containsKey(n)) {
                    distance.put(n, 1);
                    todo.add(n);
                }
            }
        }
        while (!todo.isEmpty()) {
            BlockPos p = todo.poll();
            int dd = distance.get(p);
            if (dd >= 6) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (leafSpots.contains(n) && !distance.containsKey(n)) {
                    distance.put(n, dd + 1);
                    todo.add(n);
                }
            }
        }
        // --- place it all ---
        int placedLogs = 0, placedLeaves = 0, placedFaces = 0;
        for (var e : logs.entrySet()) {
            BlockPos p = e.getKey();
            if (inReach(origin, p) && !level.isOutsideBuildHeight(p) && canReplace(level.getBlockState(p), p.getY() < base)) {
                level.setBlock(p, e.getValue(), 19);
                placedLogs++;
                if (e.getValue().is(VadswoudFeature.VADSHOUT_GEZICHT.get())) {
                    placedFaces++;
                }
            }
        }
        BlockState leaves = VadswoudFeature.VADSHOUT_BLADEREN.get().defaultBlockState();
        for (var e : distance.entrySet()) {
            BlockPos p = e.getKey();
            BlockState here = level.getBlockState(p);
            if (inReach(origin, p) && !level.isOutsideBuildHeight(p) && (here.isAir() || here.canBeReplaced())) {
                level.setBlock(p, leaves.setValue(LeavesBlock.DISTANCE, e.getValue()), 19);
                placedLeaves++;
            }
        }
        return new Tree(placedLogs, placedLeaves, placedFaces, height);
    }

    private static BlockState face(Direction d, RandomSource rnd) {
        return VadswoudFeature.VADSHOUT_GEZICHT.get().defaultBlockState().setValue(VadshoutBlocks.Gezicht.FACING, d)
                .setValue(VadshoutBlocks.Gezicht.STEMMING, rnd.nextInt(4));
    }

    private static Direction outward(BlockPos p, double cx, double cz) {
        double dx = p.getX() + 0.5 - cx, dz = p.getZ() + 0.5 - cz;
        return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    /** A branch from the trunk: logs along a 6-connected line; returns its end (x, y, z). */
    private static double[] branch(Map<BlockPos, BlockState> logs, BlockState log, double cx, double y0, double cz, double dx, double dz,
                                   double from, double len, double rise) {
        Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
        BlockState state = log.setValue(RotatedPillarBlock.AXIS, axis);
        BlockPos last = null;
        for (double s = from; s <= from + len; s += 0.25) {
            BlockPos p = BlockPos.containing(cx + dx * s, y0 + (s - from) * rise, cz + dz * s);
            if (last != null) {
                // (in between: first x, then z, then y, so the branch is one piece)
                BlockPos.MutableBlockPos m = last.mutable();
                while (!m.equals(p)) {
                    if (m.getX() != p.getX()) {
                        m.move(p.getX() > m.getX() ? 1 : -1, 0, 0);
                    } else if (m.getZ() != p.getZ()) {
                        m.move(0, 0, p.getZ() > m.getZ() ? 1 : -1);
                    } else {
                        m.move(0, p.getY() > m.getY() ? 1 : -1, 0);
                    }
                    logs.putIfAbsent(m.immutable(), state);
                }
            }
            logs.putIfAbsent(p, state);
            last = p;
        }
        return new double[]{last.getX() + 0.5, last.getY(), last.getZ() + 0.5};
    }

    private static boolean canReplace(BlockState state, boolean underground) {
        if (state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS)) {
            return true;
        }
        return underground && (state.is(BlockTags.DIRT) || state.is(BlockTags.WOOL) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.GRAVEL) || state.is(VadswoudFeature.VADSMOS.get()));
    }

    private static boolean inReach(BlockPos origin, BlockPos p) {
        return Math.abs(p.getX() - origin.getX()) <= REACH && Math.abs(p.getZ() - origin.getZ()) <= REACH;
    }

    /** Is a structure (a village, a camp...) in or next to this chunk? Then no giant tree here. */
    private static boolean nearStructure(WorldGenLevel level, BlockPos origin) {
        int ox = origin.getX() >> 4, oz = origin.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.hasChunk(ox + dx, oz + dz) && !level.getChunk(ox + dx, oz + dz).getAllReferences().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Is there already a giant trunk close by (its trunk logs about 10 blocks up)? */
    private static boolean nearOtherGiant(WorldGenLevel level, BlockPos origin) {
        for (int i = 0; i < 16; i++) {
            double a = i / 16.0 * Math.PI * 2;
            for (int rr : new int[]{5, 9, 12}) {
                BlockPos p = origin.offset((int) Math.round(Math.cos(a) * rr), 10, (int) Math.round(Math.sin(a) * rr));
                if (inReach(origin, p) && !level.isOutsideBuildHeight(p) && level.getBlockState(p).is(VadswoudFeature.VADSHOUT_STAM.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double dist(double x, double z, double cx, double cz) {
        return Math.sqrt(sq(x - cx) + sq(z - cz));
    }

    private static double sq(double v) {
        return v * v;
    }
}
