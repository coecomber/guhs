package nl.juiced.guhs.feature.katapult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;

/**
 * The 12 hand-made Mika forts of the Knabbelkatapult (structure templates {@code katapult/fort_01} .. {@code fort_12},
 * made by tools/features/katapult_forten.py) and the little physics of their blocks: how strong a block is, and which
 * blocks don't hold any more after others were knocked away.
 * <p>
 * A fort stands on the fort plot: template x across (0..{@value #W}-1, the plot's middle at x = 6), y up (0 = on the plot),
 * z backwards (0 = the front, facing the catapult). The fortplek block (FACING = towards the catapult) lies in the plot's
 * floor under the front middle (x 6, z 0).
 * <p>
 * Support: a block on the plot holds; a block holds when it rests on a block that holds, or hangs on to one sideways (or
 * below one) at most {@value #OVERHANG} blocks away from something standing: anything further out falls.
 */
public final class KatapultFort {
    public static final int W = 13, H = 14, D = 9, MIDDEN = 6, FORTS = 12, OVERHANG = 3;

    /** One block of a fort: where in the fort (template coordinates) and what. */
    public record Stuk(BlockPos local, BlockState state) {
    }

    private static final Map<Integer, List<Stuk>> CACHE = new ConcurrentHashMap<>();

    /** The blocks of fort number index (0..11), as in its template (not turned). Empty when the template is missing. */
    public static List<Stuk> load(ServerLevel level, int index) {
        return CACHE.computeIfAbsent(index, i -> read(level, i));
    }

    private static List<Stuk> read(ServerLevel level, int index) {
        Optional<StructureTemplate> template = level.getStructureManager().get(Guhs.id(String.format("katapult/fort_%02d", index + 1)));
        if (template.isEmpty()) {
            return List.of();
        }
        CompoundTag tag = template.get().save(new CompoundTag());
        HolderGetter<Block> blocks = level.holderLookup(Registries.BLOCK);
        ListTag palette = tag.getListOrEmpty("palette");
        List<BlockState> states = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            states.add(NbtUtils.readBlockState(blocks, palette.getCompoundOrEmpty(i)));
        }
        List<Stuk> list = new ArrayList<>();
        ListTag entries = tag.getListOrEmpty("blocks");
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag e = entries.getCompoundOrEmpty(i);
            ListTag pos = e.getListOrEmpty("pos");
            int s = e.getIntOr("state", 0);
            if (s < 0 || s >= states.size() || states.get(s).isAir()) {
                continue;
            }
            list.add(new Stuk(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), states.get(s)));
        }
        return List.copyOf(list);
    }

    /** (Tests / a reload) forget the loaded forts. */
    public static void forget() {
        CACHE.clear();
    }

    // --- the frame ---------------------------------------------------------------------------------------------------

    /** The world position of a fort block (template coordinates) on the plot of this fortplek. */
    public static BlockPos world(BlockPos plek, Direction facing, int tx, int ty, int tz) {
        Direction right = facing.getClockWise(), back = facing.getOpposite();
        return plek.offset(right.getStepX() * (tx - MIDDEN) + back.getStepX() * tz, ty + 1, right.getStepZ() * (tx - MIDDEN) + back.getStepZ() * tz);
    }

    /** Template coordinates of a world position, or null when it's outside the fort's box. */
    public static BlockPos local(BlockPos plek, Direction facing, BlockPos pos) {
        Direction right = facing.getClockWise(), back = facing.getOpposite();
        int dx = pos.getX() - plek.getX(), dz = pos.getZ() - plek.getZ();
        int tx = dx * right.getStepX() + dz * right.getStepZ() + MIDDEN;
        int tz = dx * back.getStepX() + dz * back.getStepZ();
        int ty = pos.getY() - plek.getY() - 1;
        if (tx < 0 || tx >= W || ty < 0 || ty >= H || tz < 0 || tz >= D) {
            return null;
        }
        return new BlockPos(tx, ty, tz);
    }

    /** How a template block turns on a plot facing this way (templates face north). */
    public static Rotation rotation(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    // --- the blocks ----------------------------------------------------------------------------------------------------

    public static boolean isMika(BlockState state) {
        return state.is(KatapultFeature.MIKA.get());
    }

    public static boolean isKist(BlockState state) {
        return state.is(KatapultFeature.KNABBELKIST.get());
    }

    /**
     * How much a knock costs (the energy of a pluisbal is its speed squared times {@link PluisbalEntity#ENERGY}): Mikas,
     * crates, glass, ice and wool 1, wood 2, stone 4.
     */
    public static int strength(BlockState state) {
        if (isMika(state) || isKist(state)) {
            return 1;
        }
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.ICE) || state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL_CARPETS)) {
            return 1;
        }
        SoundType sound = state.getSoundType();
        if (sound == SoundType.GLASS || sound == SoundType.WOOL || sound == SoundType.GRASS || sound == SoundType.CHAIN || sound == SoundType.LANTERN) {
            return 1;
        }
        if (sound == SoundType.WOOD || sound == SoundType.BAMBOO_WOOD || sound == SoundType.CHERRY_WOOD || sound == SoundType.NETHER_WOOD
                || sound == SoundType.SCAFFOLDING || state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS)) {
            return 2;
        }
        return 4;
    }

    /** Points for a block knocked out of a fort. */
    public static int points(BlockState state) {
        return isMika(state) ? 500 : isKist(state) ? 300 : 10;
    }

    /**
     * The blocks (template coordinates) that don't hold: not resting on the plot through a column of blocks, nor within
     * {@value #OVERHANG} blocks (sideways or hanging below) of one that does.
     */
    public static Set<BlockPos> unstable(Set<BlockPos> blocks) {
        Map<BlockPos, Integer> dist = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos p : blocks) {
            if (p.getY() == 0) {
                dist.put(p, 0);
                queue.add(p);
            }
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.pollFirst();
            int d = dist.get(p);
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                if (!blocks.contains(n)) {
                    continue;
                }
                int nd = d + (dir == Direction.UP ? 0 : 1);
                if (nd > OVERHANG) {
                    continue;
                }
                Integer old = dist.get(n);
                if (old == null || nd < old) {
                    dist.put(n, nd);
                    if (dir == Direction.UP) {
                        queue.addFirst(n);
                    } else {
                        queue.addLast(n);
                    }
                }
            }
        }
        Set<BlockPos> out = new HashSet<>();
        for (BlockPos p : blocks) {
            if (!dist.containsKey(p)) {
                out.add(p);
            }
        }
        return out;
    }

    private KatapultFort() {
    }
}
