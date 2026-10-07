package nl.juiced.guhs.feature.guhpixel;

import java.util.List;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

/**
 * Stamping templates in the void: load the chunks, place without neighbour updates (flag 2) and without waterlogging,
 * clear a box, and remove everything in a box that is not a player. Entities of a template are spawned at every stamp, so
 * a box is always {@link #ruim cleaned} before it is stamped again.
 */
public final class Stempel {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Loads every chunk the box touches. */
    public static void laad(ServerLevel level, BlockPos min, Vec3i maat) {
        for (int cx = min.getX() >> 4; cx <= (min.getX() + maat.getX() - 1) >> 4; cx++) {
            for (int cz = min.getZ() >> 4; cz <= (min.getZ() + maat.getZ() - 1) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }

    /** The size of a template (null: it does not exist). */
    public static Vec3i maat(ServerLevel level, Identifier template) {
        return level.getStructureManager().get(template).map(StructureTemplate::getSize).orElse(null);
    }

    /** Places the template with its min corner at {@code min}; false when the template is missing. */
    public static boolean plaats(ServerLevel level, Identifier template, BlockPos min) {
        StructureTemplate t = level.getStructureManager().get(template).orElse(null);
        if (t == null) {
            LOGGER.error("Guhpixel: template {} is missing", template);
            return false;
        }
        laad(level, min, t.getSize());
        StructurePlaceSettings settings = new StructurePlaceSettings().setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        t.placeInWorld(level, min, min, settings, level.getRandom(), 2);
        return true;
    }

    /** Sets everything in the box to air (no neighbour updates, no drops). */
    public static void leeg(ServerLevel level, BlockPos min, Vec3i maat) {
        laad(level, min, maat);
        BlockState lucht = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int y0 = Math.max(level.getMinY(), min.getY()), y1 = Math.min(level.getMaxY(), min.getY() + maat.getY() - 1);
        for (int x = min.getX(); x < min.getX() + maat.getX(); x++) {
            for (int z = min.getZ(); z < min.getZ() + maat.getZ(); z++) {
                for (int y = y1; y >= y0; y--) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.removeBlockEntity(pos);
                        level.setBlock(pos, lucht, 2 | 16);
                    }
                }
            }
        }
    }

    /** Removes every loaded entity in the box that is not a player; returns how many. */
    public static int ruim(ServerLevel level, AABB doos) {
        List<Entity> weg = level.getEntities((Entity) null, doos, e -> !(e instanceof Player));
        for (Entity e : weg) {
            e.ejectPassengers();
            e.discard();
        }
        return weg.size();
    }

    public static AABB doos(BlockPos min, Vec3i maat) {
        return new AABB(min.getX(), min.getY(), min.getZ(), min.getX() + maat.getX(), min.getY() + maat.getY(), min.getZ() + maat.getZ());
    }

    private Stempel() {
    }
}
