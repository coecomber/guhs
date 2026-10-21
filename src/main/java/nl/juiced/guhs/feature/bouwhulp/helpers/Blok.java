package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Small block helpers for the helper templates: a block by id (a guhs block that is missing falls back to a vanilla one),
 * a property by name, and the fence/wall connections of a template (blocks placed with setBlock do not connect themselves
 * to neighbours that were already there, and the ghost shows what the map says).
 */
final class Blok {
    static BlockState van(String id) {
        return van(id, Blocks.OAK_PLANKS);
    }

    static BlockState van(String id, Block anders) {
        Optional<Block> b = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id.contains(":") ? id : "minecraft:" + id));
        return b.map(Block::defaultBlockState).orElse(anders.defaultBlockState());
    }

    /** {@code s} with property {@code naam} = {@code waarde} (unchanged when the block has no such property or value). */
    static BlockState met(BlockState s, String naam, String waarde) {
        Property<?> p = s.getBlock().getStateDefinition().getProperty(naam);
        return p == null ? s : zet(s, p, waarde);
    }

    private static <T extends Comparable<T>> BlockState zet(BlockState s, Property<T> p, String waarde) {
        return p.getValue(waarde).map(v -> s.setValue(p, v)).orElse(s);
    }

    static BlockState kant(BlockState s, Direction d) {
        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && d.getAxis().isHorizontal()) {
            return s.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
        }
        if (s.hasProperty(BlockStateProperties.FACING)) {
            return s.setValue(BlockStateProperties.FACING, d);
        }
        return s;
    }

    static BlockState as(BlockState s, Direction.Axis a) {
        if (s.hasProperty(BlockStateProperties.AXIS)) {
            return s.setValue(BlockStateProperties.AXIS, a);
        }
        if (s.hasProperty(BlockStateProperties.HORIZONTAL_AXIS) && a.isHorizontal()) {
            return s.setValue(BlockStateProperties.HORIZONTAL_AXIS, a);
        }
        return s;
    }

    static BlockState blad(String id) {
        return met(van(id, Blocks.OAK_LEAVES), "persistent", "true");
    }

    static BlockState kussen(DyeColor c) {
        return van("guhs:" + c.getName() + "_kussen", Blocks.WHITE_CARPET);
    }

    static BlockState zitzak(DyeColor c) {
        return van("guhs:" + c.getName() + "_zitzak", Blocks.WHITE_WOOL);
    }

    static BlockState lampion(DyeColor c) {
        String k = switch (c) {
            case YELLOW, ORANGE -> "geel";
            case LIME, GREEN, CYAN, LIGHT_BLUE -> "mint";
            default -> "roze";
        };
        return met(van("guhs:lampion_" + k, Blocks.LANTERN), "hanging", "false");
    }

    /**
     * Fences, walls, bars and the guh hekjes in the map get their north/east/south/west set toward neighbours of the same
     * kind in the map (and fence gates: in line with fences).
     */
    static void verbind(Map<BlockPos, BlockState> m) {
        for (Map.Entry<BlockPos, BlockState> e : m.entrySet()) {
            BlockState s = e.getValue();
            if (!s.hasProperty(BlockStateProperties.NORTH) || !s.hasProperty(BlockStateProperties.EAST)
                    || s.getBlock() instanceof net.minecraft.world.level.block.VineBlock || s.getBlock() instanceof net.minecraft.world.level.block.MultifaceBlock) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockState n = m.get(e.getKey().relative(d));
                boolean ja = n != null && (n.getBlock() == s.getBlock() || n.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock
                        || n.isSolid() && !(n.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock));
                Property<?> p = switch (d) {
                    case NORTH -> BlockStateProperties.NORTH;
                    case EAST -> BlockStateProperties.EAST;
                    case SOUTH -> BlockStateProperties.SOUTH;
                    default -> BlockStateProperties.WEST;
                };
                if (s.hasProperty(p)) {
                    s = zet(s, p, String.valueOf(ja));
                }
            }
            e.setValue(s);
        }
    }

    private Blok() {
    }
}
