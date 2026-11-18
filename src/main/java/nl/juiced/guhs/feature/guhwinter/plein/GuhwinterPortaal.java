package nl.juiced.guhs.feature.guhwinter.plein;

import java.util.List;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.block.GuhPortalBlock;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.ReisguhPortalen;
import nl.juiced.guhs.registry.ModBlocks;
import org.slf4j.Logger;

/**
 * The one-time moves of {@code /guhs winter inrichten} (guhswinter/TERREIN.md §6, §9): the spawn portal from the peak
 * (frame x -273..-270, y 174..178, z -621) down to the valley (frame x -301..-298, y 30..34, z -655, same orientation), the
 * Reisguh "Spawn" along with it (same entity), and the old peak back to nature: the pink pad, the floating wool bridge, the
 * ring platform, the ~140 deep water column and the ledge along z -625 go. The tamed Blub and its chair on the peak (and the
 * blocks under them) are left alone.
 */
final class GuhwinterPortaal {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The old portal: frame corners. */
    static final BlockPos OUD_MIN = new BlockPos(-273, 174, -621), OUD_MAX = new BlockPos(-270, 178, -621);
    /** Where the Reisguh "Spawn" stood. */
    static final BlockPos OUD_REISGUH = new BlockPos(-269, 174, -623);
    /** Built things on the peak at y 173: pad, bridge, ring, ledge (x1, z1, x2, z2). */
    private static final int[][] TOP_VAKKEN = {{-276, -625, -267, -618}, {-280, -657, -271, -626}, {-289, -661, -280, -652}, {-266, -626, -233, -624}};
    private static final int TOP_Y = 173;
    /** The pad's rows that lie on the peak itself: they get the peak's own crust. */
    private static final int[] PAD_OP_GROND = {-276, -621, -267, -618};
    /** Never touched: the tamed Blub "Snoes gids" and its guh_stoel (-271, 174, -616), with a block around. */
    static final AABB BLUB = new AABB(-274, 170, -619, -268, 180, -613);
    /** The water column (all water here goes) and the box where all flowing water of the old waterfall goes. */
    private static final int[] KOLOM = {-289, -661, -280, -652};
    private static final int[] STROOM = {-312, -690, -262, -628};

    private GuhwinterPortaal() {
    }

    /** The new portal's frame base (= GuhPortalForcer's base: the first portal column, the frame's bottom). */
    static BlockPos basis() {
        return GuhwinterPlein.PORTAAL;
    }

    static boolean nieuwStaat(ServerLevel level) {
        BlockPos b = basis();
        return level.getBlockState(b.offset(0, 1, 0)).is(ModBlocks.GUH_PORTAL.get()) && level.getBlockState(b.offset(-1, 0, 0)).is(ModBlocks.BLOCK_OF_KAASKNABBELS.get());
    }

    /** Builds the new 4x5 portal (2x3 inside, axis X) on a stone floor. Returns a line for the log. */
    static String bouwNieuw(ServerLevel level) {
        BlockPos base = basis();
        if (nieuwStaat(level)) {
            return "new portal already stands at " + base.toShortString();
        }
        BlockState frame = ModBlocks.BLOCK_OF_KAASKNABBELS.get().defaultBlockState();
        BlockState portal = ModBlocks.GUH_PORTAL.get().defaultBlockState().setValue(GuhPortalBlock.AXIS, Direction.Axis.X);
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos floor = base.offset(dx, -1, dz);
                if (!level.getBlockState(floor).isSolid()) {
                    level.setBlockAndUpdate(floor, Blocks.STONE_BRICKS.defaultBlockState());
                }
                for (int dy = 0; dy <= 4; dy++) {
                    level.setBlockAndUpdate(base.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                if (dx == -1 || dx == 2 || dy == 0 || dy == 4) {
                    level.setBlockAndUpdate(base.offset(dx, dy, 0), frame);
                }
            }
        }
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 1; dy <= 3; dy++) {
                level.setBlock(base.offset(dx, dy, 0), portal, 18);
            }
        }
        return "new portal built at " + base.toShortString() + " (frame x " + (base.getX() - 1) + ".." + (base.getX() + 2) + ", y "
                + base.getY() + ".." + (base.getY() + 4) + ", z " + base.getZ() + ")";
    }

    /**
     * Moves the Reisguh "Spawn" (same entity, same UUID) next to the new portal, and remembers the new portal as one that has
     * its Reisguh (ReisguhPortalen), so nobody gets a "Guhportaal (x, z)" Reisguh there. Null when she is not found (yet).
     */
    static String verhuisReisguh(ServerLevel level) {
        BlockPos doel = GuhwinterPlein.REISGUH;
        AABB nieuw = new AABB(doel).inflate(6);
        List<GuhNpcEntity> al = level.getEntitiesOfClass(GuhNpcEntity.class, nieuw, GuhwinterPortaal::isSpawn);
        if (!al.isEmpty()) {
            ReisguhPortalen.van(level).onthoud(basis().above());
            return "Reisguh 'Spawn' already stands by the new portal (" + al.get(0).getUUID() + ")";
        }
        List<GuhNpcEntity> oud = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(OUD_REISGUH).inflate(32, 24, 32), GuhwinterPortaal::isSpawn);
        if (oud.isEmpty()) {
            return null;
        }
        GuhNpcEntity npc = oud.get(0);
        BlockPos van = npc.blockPosition();
        float yaw = GuhwinterPlein.REISGUH_YAW;
        npc.snapTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.getNavigation().stop();
        ReisguhPortalen.van(level).onthoud(basis().above());
        return "Reisguh 'Spawn' (" + npc.getUUID() + ") moved from " + van.toShortString() + " to " + doel.toShortString();
    }

    static boolean isSpawn(GuhNpcEntity n) {
        return n.getKind() == GuhNpcEntity.Kind.REISGUH && !n.isRemoved() && "Spawn".equals(n.getReisName().getString().trim());
    }

    /** True when the old portal is (still) there. */
    static boolean oudStaat(ServerLevel level) {
        for (BlockPos p : BlockPos.betweenClosed(OUD_MIN, OUD_MAX)) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.GUH_PORTAL.get()) || s.is(ModBlocks.BLOCK_OF_KAASKNABBELS.get())) {
                return true;
            }
        }
        return false;
    }

    /** Removes the old portal (portal blocks first, so its POI goes) and gives the peak back. Returns a line for the log. */
    static String herstelTop(ServerLevel level) {
        int portaal = 0, top = 0, korst = 0, water = 0;
        for (BlockPos p : BlockPos.betweenClosed(OUD_MIN, OUD_MAX)) {
            if (level.getBlockState(p).is(ModBlocks.GUH_PORTAL.get())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                portaal++;
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(OUD_MIN, OUD_MAX)) {
            if (level.getBlockState(p).is(ModBlocks.BLOCK_OF_KAASKNABBELS.get())) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                portaal++;
            }
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int[] v : TOP_VAKKEN) {
            for (int x = v[0]; x <= v[2]; x++) {
                for (int z = v[1]; z <= v[3]; z++) {
                    m.set(x, TOP_Y, z);
                    if (BLUB.contains(x + 0.5, TOP_Y + 0.5, z + 0.5)) {
                        continue;
                    }
                    BlockState s = level.getBlockState(m);
                    if (!(s.is(Blocks.PINK_WOOL) || s.is(Blocks.PINK_TERRACOTTA) || s.is(Blocks.WHITE_WOOL))) {
                        continue;
                    }
                    if (level.getBlockState(m.below()).isAir()) {
                        level.setBlock(m, Blocks.AIR.defaultBlockState(), GuhwinterGrond.VLAG);
                        top++;
                    } else if (x >= PAD_OP_GROND[0] && x <= PAD_OP_GROND[2] && z >= PAD_OP_GROND[1] && z <= PAD_OP_GROND[3]
                            && !s.is(Blocks.PINK_TERRACOTTA)) {
                        level.setBlock(m, Blocks.PINK_TERRACOTTA.defaultBlockState(), GuhwinterGrond.VLAG);
                        korst++;
                    }
                }
            }
        }
        // the water column (every water block in it) and the flowing water of the old waterfall around it
        for (int x = STROOM[0]; x <= STROOM[2]; x++) {
            for (int z = STROOM[1]; z <= STROOM[3]; z++) {
                boolean kolom = x >= KOLOM[0] && x <= KOLOM[2] && z >= KOLOM[1] && z <= KOLOM[3];
                for (int y = 29; y <= 177; y++) {
                    m.set(x, y, z);
                    BlockState s = level.getBlockState(m);
                    FluidState f = s.getFluidState();
                    if (f.isEmpty() || !s.is(Blocks.WATER)) {
                        continue;
                    }
                    if ((kolom && y >= 31) || !f.isSource()) {
                        level.setBlock(m, Blocks.AIR.defaultBlockState(), GuhwinterGrond.VLAG);
                        water++;
                    }
                }
            }
        }
        String uit = "old portal: " + portaal + " blocks removed; old peak: " + top + " floating blocks of pad/bridge/ring/ledge removed, "
                + korst + " pad blocks on the peak got its own crust; " + water + " water blocks of the old waterfall removed";
        LOGGER.info("Guhwinter: {}", uit);
        return uit;
    }
}
