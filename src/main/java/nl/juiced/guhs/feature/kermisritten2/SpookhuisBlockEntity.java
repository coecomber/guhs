package nl.juiced.guhs.feature.kermisritten2;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.safari.rit.Pad;

/**
 * Het Spookguhhuis: the anchor lies in the paving in front of the two mouths. The design's house is 18 by 23 blocks with
 * two partitions. The track goes in by the north mouth, along one hand of every corridor to the far end and back along the
 * other, so it never crosses itself, out by the south mouth and round outside to the station. Two carts run; the anchor
 * makes them while a player is near. "rel" below = blocks from the house's north-west corner; the anchor is at rel (19, 11),
 * facing east.
 */
public class SpookhuisBlockEntity extends AnkerBlockEntity {
    public static final int KARREN = 2;
    static final double NABIJ = 48;
    static final int LEEG_WEG = 600;
    /** Rel of the anchor. */
    private static final int AX = 19, AZ = 11;
    /** The track, rel (x, z), as a loop from the station. The stop at the way out is point {@link #UIT_I}. */
    static final int[][] SPOOR = {
            {22, 8}, {13, 8}, {13, 2}, {7, 2}, {7, 19}, {4, 19}, {4, 9}, {2, 9}, {2, 21}, {9, 21}, {9, 3}, {11, 3}, {11, 14}, {22, 14},
            {24, 14}, {24, 8}};
    static final int UIT_I = 13;
    /** Where a ghost pops up: (track point it comes after, rel x, rel z, its line). */
    static final double[][] SCHRIK = {{1, 15, 5, 0}, {3, 9, 9, 1}, {5, 3, 15, 2}, {7, 3, 13, 3}, {8, 6, 20, 4}, {11, 14, 9, 5}};
    public static final int REGELS = 6;

    private int leeg;

    public SpookhuisBlockEntity(BlockPos pos, BlockState state) {
        super(Ritten2Feature.SPOOKHUIS_BE.get(), pos, state);
    }

    @Override
    protected Direction ontwerp() {
        return Direction.EAST;
    }

    public Pad pad() {
        List<Vec3> punten = new ArrayList<>();
        for (int[] p : SPOOR) {
            punten.add(rel(p[0], p[1], 1));
        }
        return new Pad(punten, true);
    }

    /** A rel point (whole numbers: the middle of that block) in the world. */
    Vec3 rel(double x, double z, double dy) {
        return Anker.punt(getBlockPos(), draai(), x - AX, dy, z - AZ);
    }

    /** Where you stand after the ride: beside the south mouth's track. */
    public Vec3 uitgang() {
        return rel(21, 16, 1);
    }

    AABB huis() {
        return new AABB(rel(-2, -2, 0), rel(26, 24, 12));
    }

    @Override
    protected void rit(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        Vec3 midden = huis().getCenter();
        List<SpookkarEntity> karren = level.getEntitiesOfClass(SpookkarEntity.class, huis().inflate(4));
        if (level.hasNearbyAlivePlayer(midden.x, midden.y, midden.z, NABIJ)) {
            leeg = 0;
            veegEenmaal(level);
            for (int k = karren.size(); k < KARREN; k++) {
                SpookkarEntity kar = Ritten2Feature.SPOOKKAR.get().create(level, EntitySpawnReason.TRIGGERED);
                if (kar == null) {
                    return;
                }
                kar.zetSpoor(this, k == 0);
                level.addFreshEntity(kar);
            }
        } else if ((leeg += 20) >= LEEG_WEG) {
            karren.stream().filter(k -> !k.isVehicle()).forEach(SpookkarEntity::discard);
        }
    }

    /** The wool ghosts of the design hang in the track's way: the carts' own ghosts take over. The lanterns stay. */
    @Override
    protected void veeg(ServerLevel level) {
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(rel(1, 1, 1)), BlockPos.containing(rel(16, 21, 7)))) {
            if (level.getBlockState(p).is(Blocks.WHITE_WOOL)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}
