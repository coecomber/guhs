package nl.juiced.guhs.feature.race;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * A guh race track as found in the world: the start marker (and the way it faces), the checkpoint rings in order (ring 0
 * is the start/finish) and the area it covers. Found once by scanning the racebaan around the Raceguh and kept in her
 * roleData. Positions of a race (the ghost) are kept in the track's own frame (right / up / forward from the start),
 * so a ghost recorded on one racebaan drives the same on every other one, however it was rotated.
 */
public final class RaceTrack {
    public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("guh_racebaan"));
    /**
     * Where the start marker sits in the racebaan template (tools/features/race.py), facing east, and the template's size:
     * without the structure (tests, old worlds) the track is looked for in the template's box around the start marker
     * nearest to the Raceguh (rotated the way that marker faces). Keep in sync with race.py.
     */
    public static final BlockPos TEMPLATE_START = new BlockPos(43, 4, 80);
    public static final int TEMPLATE_W = 100, TEMPLATE_H = 34, TEMPLATE_D = 100, NEAR = 24;

    /** Blocks of this tag are a track's special spots (2.9: Mika-pikkers, rolling knabbels, loopings...): kept in {@link #markers}. */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> MARKER_BLOCKS =
            net.minecraft.tags.TagKey.create(Registries.BLOCK, Guhs.id("race_marker"));

    /** A special spot of a track: the block's id (path), where, which way it faces (-1: none) and from which level on it counts. */
    public record Marker(String kind, BlockPos pos, int facing, int vanaf) {
        public Direction direction() {
            return facing < 0 ? Direction.NORTH : Direction.from2DDataValue(facing);
        }

        public Vec3 centre() {
            return Vec3.atBottomCenterOf(pos);
        }
    }

    public final BlockPos start;
    public final Direction facing;
    /** The checkpoint rings, by number (0 = start/finish). */
    public final List<AABB> gates;
    /** The track's special spots (marker blocks of {@link #MARKER_BLOCKS}). */
    public final List<Marker> markers;
    /** Everything of the track, with room around it (for "you drove off the track"; the protection is {@link #protectedArea}). */
    public final AABB area;
    /** How much room {@link #area} has around the pads, rings and start. */
    public static final int OFF_TRACK_MARGIN_XZ = 12, OFF_TRACK_MARGIN_Y = 8;
    /** How far around the track itself stays protected. */
    public static final int PROTECT_MARGIN = 1;

    /** The track itself plus a block (protected: the wider {@link #area} would reach over neighbouring builds). */
    public AABB protectedArea() {
        return area.deflate(OFF_TRACK_MARGIN_XZ - PROTECT_MARGIN, OFF_TRACK_MARGIN_Y - PROTECT_MARGIN, OFF_TRACK_MARGIN_XZ - PROTECT_MARGIN);
    }

    public RaceTrack(BlockPos start, Direction facing, List<AABB> gates, AABB area) {
        this(start, facing, gates, area, List.of());
    }

    public RaceTrack(BlockPos start, Direction facing, List<AABB> gates, AABB area, List<Marker> markers) {
        this.start = start;
        this.facing = facing;
        this.gates = gates;
        this.area = area;
        this.markers = markers;
    }

    /** The markers of one kind (e.g. "circuit_mikaplek"). */
    public List<Marker> markers(String kind) {
        return markers.stream().filter(m -> m.kind().equals(kind)).toList();
    }

    // --- finding it ------------------------------------------------------------------------------------------------

    /** The track of this Raceguh (the old racebaan): from her saved data, or found by scanning (null if the racebaan is broken). */
    @Nullable
    public static RaceTrack of(GuhNpcEntity npc) {
        return of(npc, RaceBaan.RACEBAAN);
    }

    /**
     * A track of a race guh NPC: from her saved data, or found by scanning its box (the racebaan: {@link #searchBox}; the
     * circuit's tracks: their {@link RaceBaan.Zoeker}, whose box is also the track's area). Null if it's broken.
     */
    @Nullable
    public static RaceTrack of(GuhNpcEntity npc, RaceBaan baan) {
        return of(npc, baan, false);
    }

    /** As {@link #of(GuhNpcEntity, RaceBaan)}, but when it isn't known yet only scans if every chunk of the track is loaded. */
    @Nullable
    public static RaceTrack ofLoaded(GuhNpcEntity npc, RaceBaan baan) {
        return of(npc, baan, true);
    }

    @Nullable
    private static RaceTrack of(GuhNpcEntity npc, RaceBaan baan, boolean onlyLoaded) {
        String key = baan.trackKey();
        if (npc.roleData.contains(key)) {
            RaceTrack track = load(npc.roleData.getCompoundOrEmpty(key));
            if (track != null) {
                RaceProtection.remember(npc.level(), track.protectedArea());
                return track;
            }
        }
        ServerLevel level = (ServerLevel) npc.level();
        RaceTrack track;
        BoundingBox box = baan.zoeker() == null ? searchBox(level, npc.blockPosition()) : baan.zoeker().box(level, npc);
        if (box == null || onlyLoaded && !level.hasChunksAt(box.minX(), box.minZ(), box.maxX(), box.maxZ())) {
            return null;
        }
        track = scan(level, box, baan.index, baan.zoeker() == null ? null : AABB.of(box));
        if (track != null) {
            npc.roleData.put(key, track.save());
            RaceProtection.remember(npc.level(), track.protectedArea());
        }
        return track;
    }

    /** The racebaan's own box if the Raceguh stands in one, otherwise the template's box around her start marker. */
    @Nullable
    static BoundingBox searchBox(ServerLevel level, BlockPos npc) {
        Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(STRUCTURE);
        if (structure != null) {
            var start = level.structureManager().getStructureAt(npc, structure);
            if (start.isValid()) {
                return start.getBoundingBox();
            }
        }
        BlockPos marker = null;
        for (BlockPos pos : BlockPos.betweenClosed(npc.offset(-NEAR, -8, -NEAR), npc.offset(NEAR, 8, NEAR))) {
            BlockState here = level.getBlockState(pos);
            if (here.is(RaceFeature.RACE_START.get()) && here.getValue(RaceBlocks.BAAN) == 0 && (marker == null || pos.distSqr(npc) < marker.distSqr(npc))) {
                marker = pos.immutable();
            }
        }
        if (marker == null) {
            return null;
        }
        Direction facing = level.getBlockState(marker).getValue(RaceBlocks.Start.FACING);
        BlockPos a = templateToWorld(marker, facing, BlockPos.ZERO), b = templateToWorld(marker, facing, new BlockPos(TEMPLATE_W - 1, TEMPLATE_H - 1, TEMPLATE_D - 1));
        return BoundingBox.fromCorners(a, b);
    }

    /** A spot of the template in the world, for a template placed with its start marker at {@code marker} facing {@code facing}. */
    static BlockPos templateToWorld(BlockPos marker, Direction facing, BlockPos template) {
        return templateToWorld(marker, facing, TEMPLATE_START, template);
    }

    /**
     * A spot of a template in the world: the template has a marker at {@code templateMarker} that faces EAST in the template;
     * in the world that marker is at {@code marker} and faces {@code facing} (the template was turned that way).
     */
    public static BlockPos templateToWorld(BlockPos marker, Direction facing, BlockPos templateMarker, BlockPos template) {
        int dx = template.getX() - templateMarker.getX(), dy = template.getY() - templateMarker.getY(), dz = template.getZ() - templateMarker.getZ();
        Direction side = facing.getClockWise();       // the template's east is the marker's facing, its south is clockwise of that
        return marker.offset(facing.getStepX() * dx + side.getStepX() * dz, dy, facing.getStepZ() * dx + side.getStepZ() * dz);
    }

    @Nullable
    static RaceTrack scan(ServerLevel level, BoundingBox box) {
        return scan(level, box, 0, null);
    }

    /** Scans a box for the markers of track {@code baan} (areaOverride: the area to use instead of the rings' + pads' box). */
    @Nullable
    public static RaceTrack scan(ServerLevel level, BoundingBox box, int baan, @Nullable AABB areaOverride) {
        BlockPos start = null;
        Direction facing = Direction.NORTH;
        List<Marker> markers = new ArrayList<>();
        List<int[]> rings = new ArrayList<>();          // per number: min x/y/z, max x/y/z
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        // (every chunk of the racebaan is read, loading it if needed: a half-loaded scan would save rings that are too small)
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            if (state.is(RaceFeature.RACE_START.get())) {
                if (state.getValue(RaceBlocks.BAAN) == baan) {
                    start = pos.immutable();
                    facing = state.getValue(RaceBlocks.Start.FACING);
                }
            } else if (state.is(MARKER_BLOCKS)) {
                markers.add(marker(state, pos.immutable()));
            } else if (state.is(RaceFeature.RACE_CHECKPOINT.get())) {
                if (state.getValue(RaceBlocks.BAAN) != baan) {
                    continue;
                }
                int n = state.getValue(RaceBlocks.Checkpoint.NUMMER);
                while (rings.size() <= n) {
                    rings.add(null);
                }
                int[] r = rings.get(n);
                if (r == null) {
                    rings.set(n, new int[]{pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()});
                } else {
                    r[0] = Math.min(r[0], pos.getX());
                    r[1] = Math.min(r[1], pos.getY());
                    r[2] = Math.min(r[2], pos.getZ());
                    r[3] = Math.max(r[3], pos.getX());
                    r[4] = Math.max(r[4], pos.getY());
                    r[5] = Math.max(r[5], pos.getZ());
                }
            } else if (state.is(RaceFeature.RACE_PAD.get())) {
                x0 = Math.min(x0, pos.getX());
                x1 = Math.max(x1, pos.getX());
                z0 = Math.min(z0, pos.getZ());
                z1 = Math.max(z1, pos.getZ());
                y0 = Math.min(y0, pos.getY());
                y1 = Math.max(y1, pos.getY());
            }
        }
        if (start == null || rings.size() < 2 || rings.contains(null)) {
            return null;
        }
        List<AABB> gates = new ArrayList<>();
        for (int[] r : rings) {
            gates.add(new AABB(r[0], r[1], r[2], r[3] + 1, r[4] + 1, r[5] + 1).inflate(0.5, 1.0, 0.5));
            x0 = Math.min(x0, r[0]);
            y0 = Math.min(y0, r[1]);
            z0 = Math.min(z0, r[2]);
            x1 = Math.max(x1, r[3]);
            y1 = Math.max(y1, r[4]);
            z1 = Math.max(z1, r[5]);
        }
        AABB area = areaOverride != null ? areaOverride.inflate(OFF_TRACK_MARGIN_XZ, OFF_TRACK_MARGIN_Y, OFF_TRACK_MARGIN_XZ)
                : new AABB(Math.min(x0, start.getX()), Math.min(y0, start.getY()), Math.min(z0, start.getZ()),
                Math.max(x1, start.getX()) + 1, Math.max(y1, start.getY()) + 1, Math.max(z1, start.getZ()) + 1).inflate(OFF_TRACK_MARGIN_XZ, OFF_TRACK_MARGIN_Y, OFF_TRACK_MARGIN_XZ);
        return new RaceTrack(start, facing, gates, area, markers);
    }

    /** A marker block as a {@link Marker}: its facing (if it has one) and its "vanaf" level (if it has one). */
    static Marker marker(BlockState state, BlockPos pos) {
        var facingProperty = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
        int facing = state.hasProperty(facingProperty) ? state.getValue(facingProperty).get2DDataValue() : -1;
        int vanaf = 0;
        for (var property : state.getProperties()) {
            if (property.getName().equals("vanaf") && state.getValue(property) instanceof Integer i) {
                vanaf = i;
            }
        }
        return new Marker(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath(), pos, facing, vanaf);
    }

    // --- the race ---------------------------------------------------------------------------------------------------

    /**
     * The ring a guh went through going from a to b (or -1): the first one it enters (or is still in) along the way.
     * A ring it is only leaving doesn't count.
     */
    public int gateBetween(Vec3 a, Vec3 b) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < gates.size(); i++) {
            AABB gate = gates.get(i);
            boolean inA = gate.contains(a), inB = gate.contains(b);
            double dist;
            if (inA && inB) {
                dist = 0;
            } else if (inA) {
                continue;
            } else if (inB) {
                dist = gate.clip(a, b).map(a::distanceToSqr).orElse(a.distanceToSqr(b));
            } else {
                var hit = gate.clip(a, b);
                if (hit.isEmpty()) {
                    continue;
                }
                dist = a.distanceToSqr(hit.get());
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    public Vec3 centre(int gate) {
        return gates.get(gate).getCenter();
    }

    /** Where the race guh stands at the start (on the marker, looking forward). */
    public Vec3 startPos() {
        return new Vec3(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
    }

    public float startYaw() {
        return facing.toYRot();
    }

    // --- the track's own frame (x = right, y = up, z = forward from the start marker) -----------------------------------

    public Vec3 toLocal(Vec3 world) {
        Vec3 d = world.subtract(startPos());
        Direction right = facing.getClockWise();
        return new Vec3(d.x * right.getStepX() + d.z * right.getStepZ(), d.y, d.x * facing.getStepX() + d.z * facing.getStepZ());
    }

    public Vec3 toWorld(Vec3 local) {
        Direction right = facing.getClockWise();
        return startPos().add(right.getStepX() * local.x + facing.getStepX() * local.z, local.y, right.getStepZ() * local.x + facing.getStepZ() * local.z);
    }

    // --- saving ---------------------------------------------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Start", start.asLong());
        tag.putInt("Facing", facing.get2DDataValue());
        ListTag list = new ListTag();
        for (AABB g : gates) {
            CompoundTag t = new CompoundTag();
            t.putDouble("X0", g.minX);
            t.putDouble("Y0", g.minY);
            t.putDouble("Z0", g.minZ);
            t.putDouble("X1", g.maxX);
            t.putDouble("Y1", g.maxY);
            t.putDouble("Z1", g.maxZ);
            list.add(t);
        }
        tag.put("Gates", list);
        tag.putDouble("AX0", area.minX);
        tag.putDouble("AY0", area.minY);
        tag.putDouble("AZ0", area.minZ);
        tag.putDouble("AX1", area.maxX);
        tag.putDouble("AY1", area.maxY);
        tag.putDouble("AZ1", area.maxZ);
        ListTag spots = new ListTag();
        for (Marker m : markers) {
            CompoundTag t = new CompoundTag();
            t.putString("Kind", m.kind());
            t.putLong("Pos", m.pos().asLong());
            t.putInt("Facing", m.facing());
            t.putInt("Vanaf", m.vanaf());
            spots.add(t);
        }
        tag.put("Markers", spots);
        return tag;
    }

    @Nullable
    public static RaceTrack load(CompoundTag tag) {
        if (!tag.contains("Start") || !tag.contains("Gates")) {
            return null;
        }
        List<AABB> gates = new ArrayList<>();
        for (Tag t : tag.getListOrEmpty("Gates")) {
            CompoundTag g = (CompoundTag) t;
            gates.add(new AABB(g.getDoubleOr("X0", 0.0), g.getDoubleOr("Y0", 0.0), g.getDoubleOr("Z0", 0.0), g.getDoubleOr("X1", 0.0), g.getDoubleOr("Y1", 0.0), g.getDoubleOr("Z1", 0.0)));
        }
        if (gates.size() < 2) {
            return null;
        }
        AABB area = new AABB(tag.getDoubleOr("AX0", 0.0), tag.getDoubleOr("AY0", 0.0), tag.getDoubleOr("AZ0", 0.0), tag.getDoubleOr("AX1", 0.0), tag.getDoubleOr("AY1", 0.0), tag.getDoubleOr("AZ1", 0.0));
        List<Marker> markers = new ArrayList<>();
        for (Tag t : tag.getListOrEmpty("Markers")) {
            CompoundTag m = (CompoundTag) t;
            markers.add(new Marker(m.getStringOr("Kind", ""), BlockPos.of(m.getLongOr("Pos", 0L)), m.getIntOr("Facing", 0), m.getIntOr("Vanaf", 0)));
        }
        return new RaceTrack(BlockPos.of(tag.getLongOr("Start", 0L)), Direction.from2DDataValue(tag.getIntOr("Facing", 0)), gates, area, markers);
    }
}
