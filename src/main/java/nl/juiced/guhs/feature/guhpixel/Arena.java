package nl.juiced.guhs.feature.guhpixel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** One built arena: a cell of the grid (or, in tests, a spot in the test level; then {@link #cel} is -1). */
public final class Arena {
    private final int cel;
    private final ArenaSoort soort;
    private final ServerLevel level;
    private final BlockPos oorsprong;

    Arena(int cel, ArenaSoort soort, ServerLevel level, BlockPos oorsprong) {
        this.cel = cel;
        this.soort = soort;
        this.level = level;
        this.oorsprong = oorsprong.immutable();
    }

    public int cel() {
        return cel;
    }

    public ArenaSoort soort() {
        return soort;
    }

    public ServerLevel level() {
        return level;
    }

    /** The min corner (template 0,0,0). */
    public BlockPos oorsprong() {
        return oorsprong;
    }

    /** Template coordinates to world coordinates. */
    public BlockPos wereld(int x, int y, int z) {
        return oorsprong.offset(x, y, z);
    }

    public Vec3 wereld(Vec3 lokaal) {
        return lokaal.add(oorsprong.getX(), oorsprong.getY(), oorsprong.getZ());
    }

    /** World coordinates to template coordinates. */
    public BlockPos lokaal(BlockPos wereld) {
        return wereld.subtract(oorsprong);
    }

    /** The whole box of the arena. */
    public AABB doos() {
        return Stempel.doos(oorsprong, soort.maat());
    }

    /** Where the players start (world, feet). */
    public Vec3 start() {
        return wereld(soort.startLokaal());
    }
}
