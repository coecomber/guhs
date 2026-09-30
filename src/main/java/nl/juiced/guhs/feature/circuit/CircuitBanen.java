package nl.juiced.guhs.feature.circuit;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.race.RaceBaan;
import nl.juiced.guhs.feature.race.RaceBlocks;
import nl.juiced.guhs.feature.race.RaceFeature;
import nl.juiced.guhs.feature.race.RaceTrack;

/**
 * The three tracks of the Guh-Circuit as {@link RaceBaan}s (Regenboogbaan 1, Vadsbaan 2, Kaasbergbaan 3), and how the
 * circuit's template is found in the world from Coach Vahoegvroem: the Regenboogbaan's start marker (it faces east in the
 * template) near her tells where the template lies and how it was turned (its "frame"); from that frame every spot of the
 * template (the tracks' boxes, the floating scoreboards) is known without scanning the whole circuit.
 * Keep the numbers in sync with tools/features/circuit.py and circuit_banen.py.
 */
public final class CircuitBanen {
    public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guh_circuit"));
    /** The template: 192 x 60 x 192, the Regenboogbaan's start marker (facing east) at TEMPLATE_START. */
    public static final int W = 192, H = 60, D = 192;
    public static final BlockPos TEMPLATE_START = new BlockPos(59, 4, 62);
    /** How far from Coach Vahoegvroem the Regenboogbaan's start marker may be (it is ~48 blocks away in the template). */
    public static final int NEAR = 56, NEAR_Y = 16;
    /** The boxes the tracks' markers are in (template, inclusive): regenboog, vads, kaasberg. */
    static final int[][] BOXES = {{0, 0, 0, 191, 59, 79}, {0, 0, 108, 95, 59, 191}, {96, 0, 108, 191, 59, 191}};
    /** The floating scoreboards (template): per track the whole races and the fastest laps. */
    static final BlockPos[][] BOARDS = {
            {new BlockPos(84, 15, 69), new BlockPos(108, 15, 69)},
            {new BlockPos(70, 14, 118), new BlockPos(80, 14, 118)},
            {new BlockPos(112, 14, 118), new BlockPos(122, 14, 118)}};

    /** The Vadslooping (the same numbers as circuit_banen.py LOOP_*): radius, length, to the right, ticks. */
    public static final double LOOP_R = 7.0, LOOP_L = 12.0, LOOP_W = 8.0;
    public static final int LOOP_TICKS = 56;

    public static final RaceBaan REGENBOOG = RaceBaan.register(new RaceBaan("regenboog", 1, 3, new int[]{20 * 96, 20 * 113, 20 * 140},
            Minigames.CIRCUIT, () -> CircuitFeature.CIRCUITBEKER.get(), (level, npc) -> box(level, npc, 0)));
    public static final RaceBaan VADS = RaceBaan.register(new RaceBaan("vads", 2, 3, new int[]{20 * 84, 20 * 99, 20 * 123},
            Minigames.CIRCUIT, () -> CircuitFeature.CIRCUITBEKER.get(), (level, npc) -> box(level, npc, 1)));
    public static final RaceBaan KAASBERG = RaceBaan.register(new RaceBaan("kaasberg", 3, 3, new int[]{20 * 103, 20 * 121, 20 * 150},
            Minigames.CIRCUIT, () -> CircuitFeature.CIRCUITBEKER.get(), (level, npc) -> box(level, npc, 2)));
    public static final List<RaceBaan> BANEN = List.of(REGENBOOG, VADS, KAASBERG);

    /** The circuit's track with this id (regenboog, vads, kaasberg), or null. */
    @Nullable
    public static RaceBaan baan(String id) {
        for (RaceBaan b : BANEN) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        return null;
    }

    /** Where the template lies: the Regenboogbaan's start marker in the world and the way it faces. */
    public record Frame(BlockPos marker, Direction facing) {
        public BlockPos toWorld(BlockPos template) {
            return RaceTrack.templateToWorld(marker, facing, TEMPLATE_START, template);
        }

        public Vec3 toWorld(Vec3 template) {
            BlockPos b = toWorld(BlockPos.containing(template));
            return Vec3.atBottomCenterOf(b).add(0, template.y - Math.floor(template.y), 0);
        }
    }

    /** The frame of Coach Vahoegvroem's circuit (kept in her roleData once found), or null. */
    @Nullable
    public static Frame frame(ServerLevel level, GuhNpcEntity npc) {
        if (npc.roleData.contains("CircuitFrame")) {
            CompoundTag tag = npc.roleData.getCompoundOrEmpty("CircuitFrame");
            return new Frame(BlockPos.of(tag.getLongOr("Marker", 0L)), Direction.from2DDataValue(tag.getIntOr("Facing", 0)));
        }
        BlockPos at = npc.blockPosition();
        if (!level.hasChunksAt(at.getX() - NEAR, at.getZ() - NEAR, at.getX() + NEAR, at.getZ() + NEAR)) {
            return null;
        }
        BlockPos found = null;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-NEAR, -NEAR_Y, -NEAR), at.offset(NEAR, NEAR_Y, NEAR))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(RaceFeature.RACE_START.get()) && state.getValue(RaceBlocks.BAAN) == REGENBOOG.index
                    && (found == null || pos.distSqr(at) < found.distSqr(at))) {
                found = pos.immutable();
            }
        }
        if (found == null) {
            return null;
        }
        Frame frame = new Frame(found, level.getBlockState(found).getValue(RaceBlocks.Start.FACING));
        CompoundTag tag = new CompoundTag();
        tag.putLong("Marker", found.asLong());
        tag.putInt("Facing", frame.facing().get2DDataValue());
        npc.roleData.put("CircuitFrame", tag);
        return frame;
    }

    /** The box (world) of track {@code i} (0 regenboog, 1 vads, 2 kaasberg), or null when the circuit isn't found. */
    @Nullable
    static BoundingBox box(ServerLevel level, GuhNpcEntity npc, int i) {
        Frame frame = frame(level, npc);
        if (frame == null) {
            return null;
        }
        int[] b = BOXES[i];
        return BoundingBox.fromCorners(frame.toWorld(new BlockPos(b[0], b[1], b[2])), frame.toWorld(new BlockPos(b[3], b[4], b[5])));
    }

    /** Where a track's scoreboard floats (0: whole races, 1: fastest laps). */
    @Nullable
    static Vec3 board(ServerLevel level, GuhNpcEntity npc, int baan, int which) {
        Frame frame = frame(level, npc);
        return frame == null ? null : Vec3.atBottomCenterOf(frame.toWorld(BOARDS[baan][which]));
    }

    /** Makes sure the tracks are registered (class loading). */
    static void init() {
    }

    private CircuitBanen() {
    }
}
