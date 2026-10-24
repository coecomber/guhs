package nl.juiced.guhs.feature.onderwol.bouw;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * A natural hall carved around (a piece of) an underground building ({@code guhs:grot_bouw} with {@code "zaal"}): a rounded
 * room, wider than the build by {@code rand} blocks, with a domed ceiling {@code boven} blocks above the build's top, its wall
 * and ceiling wobbling a few blocks (a 3D noise, strength {@code ruw}); the floor rises a little towards the wall, like a
 * cave. It is placed before the building's own pieces, so the template's blocks always win.
 * <p>
 * Rules: the rock is only carved, never added in the room; under the floor holes (another cave, a lake) are filled with
 * the layer stone of the spot ({@link GrotSchilProcessor#laagsteen}), and a block that would open the room into a fluid
 * becomes layer stone too (a dam), so no lake or saus sea drains into it. The ceiling stays at least {@code dekking} blocks
 * under the surface (the WG heightmap of that column): a hall never breaks into the sky or into the sea floor.
 */
public class GrotZaalPiece extends StructurePiece {
    private final int cx, cz, vloer, rx, rz, hoog, dekking;
    private final float ruw;
    private final long zaad;

    /**
     * @param middenX middle of the hall (x, z), vloer = the y you stand on (the first air above the floor), rx / rz = the
     *                half widths, hoog = the height of the dome above the floor in the middle
     */
    public GrotZaalPiece(int middenX, int middenZ, int vloer, int rx, int rz, int hoog, float ruw, int dekking, long zaad) {
        super(OnderWolBouw.ZAAL.get(), 0, doos(middenX, middenZ, vloer, rx, rz, hoog, ruw));
        this.cx = middenX;
        this.cz = middenZ;
        this.vloer = vloer;
        this.rx = Math.max(2, rx);
        this.rz = Math.max(2, rz);
        this.hoog = Math.max(3, hoog);
        this.ruw = ruw;
        this.dekking = dekking;
        this.zaad = zaad;
    }

    public GrotZaalPiece(CompoundTag tag) {
        super(OnderWolBouw.ZAAL.get(), tag);
        this.cx = tag.getIntOr("cx", 0);
        this.cz = tag.getIntOr("cz", 0);
        this.vloer = tag.getIntOr("vloer", 0);
        this.rx = tag.getIntOr("rx", 4);
        this.rz = tag.getIntOr("rz", 4);
        this.hoog = tag.getIntOr("hoog", 6);
        this.ruw = tag.getFloatOr("ruw", 1.0f);
        this.dekking = tag.getIntOr("dekking", 6);
        this.zaad = tag.getLongOr("zaad", 0L);
    }

    /** The box the hall can reach (the wobble included). */
    static BoundingBox doos(int cx, int cz, int vloer, int rx, int rz, int hoog, float ruw) {
        int w = (int) Math.ceil(3 * ruw) + 1;
        return new BoundingBox(cx - rx - w, vloer - 3, cz - rz - w, cx + rx + w, vloer + hoog + w, cz + rz + w);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("cx", cx);
        tag.putInt("cz", cz);
        tag.putInt("vloer", vloer);
        tag.putInt("rx", rx);
        tag.putInt("rz", rz);
        tag.putInt("hoog", hoog);
        tag.putFloat("ruw", ruw);
        tag.putInt("dekking", dekking);
        tag.putLong("zaad", zaad);
    }

    /**
     * How far (x, z) lies from the middle in "hall radii" (1 = the wall, before the wobble): a rounded rectangle (a
     * superellipse), so a square build fits with its corners inside.
     */
    public static double afstand(double dx, double dz, double rx, double rz) {
        double p = 3.0;
        return Math.pow(Math.pow(Math.abs(dx) / rx, p) + Math.pow(Math.abs(dz) / rz, p), 1.0 / p);
    }

    /** The y of the ceiling above (x, z) at this distance (exclusive): a dome, lower towards the wall. */
    public int plafond(double d, double golf) {
        double t = Math.max(0.0, 1.0 - Math.pow(Math.min(1.0, d), 4));
        return vloer + 3 + (int) Math.round((hoog - 3) * Math.sqrt(t) + golf * 2 * ruw);
    }

    /** The y of the floor at this distance: flat in the middle, rising towards the wall. */
    public int vloerOp(double d) {
        return d < 0.8 ? vloer : vloer + (int) Math.round((d - 0.8) * 8);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunk,
                            ChunkPos chunkPos, BlockPos reference) {
        SimplexNoise ruis = new SimplexNoise(new LegacyRandomSource(zaad));
        BoundingBox box = getBoundingBox();
        int x0 = Math.max(box.minX(), chunk.minX()), x1 = Math.min(box.maxX(), chunk.maxX());
        int z0 = Math.max(box.minZ(), chunk.minZ()), z1 = Math.min(box.maxZ(), chunk.maxZ());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                double golf = ruis.getValue(x / 11.0, z / 11.0);
                // (the wall wobbles: up to ~2 blocks per ruw in and out)
                double d = afstand(x - cx, z - cz, rx + golf * 2 * ruw, rz + golf * 2 * ruw);
                if (d > 1.0) {
                    continue;
                }
                int top = Math.min(plafond(d, ruis.getValue(x / 7.0 + 31, z / 7.0 - 17)),
                        level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - dekking);
                int bodem = vloerOp(d);
                // the floor: holes under it are filled (two layers)
                for (int y = bodem - 2; y < bodem; y++) {
                    p.set(x, y, z);
                    if (chunk.isInside(p) && !GrotSchilProcessor.vast(level.getBlockState(p))) {
                        level.setBlock(p, GrotSchilProcessor.laagsteen(level, p, chunk), Block.UPDATE_CLIENTS);
                    }
                }
                for (int y = bodem; y < top; y++) {
                    p.set(x, y, z);
                    if (!chunk.isInside(p)) {
                        continue;
                    }
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) {
                        continue;
                    }
                    if (!s.getFluidState().isEmpty() || naastVloeistof(level, p, chunk)) {
                        level.setBlock(p, GrotSchilProcessor.laagsteen(level, p, chunk), Block.UPDATE_CLIENTS);   // (a dam)
                    } else {
                        level.setBlock(p, Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /** Is there a fluid right next to (or above) this spot? Then carving it would let the fluid in. */
    private static boolean naastVloeistof(WorldGenLevel level, BlockPos pos, BoundingBox chunk) {
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) {
                continue;
            }
            q.setWithOffset(pos, dir);
            if (chunk.isInside(q) && !level.getFluidState(q).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
