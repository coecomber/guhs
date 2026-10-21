package nl.juiced.guhs.feature.bouwhulp.blauwdruk;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.vormen.Daken;
import nl.juiced.guhs.feature.bouwhulp.vormen.Familie;
import nl.juiced.guhs.feature.bouwhulp.vormen.Vormen;

/**
 * A little builder for the catalogue's designs: fill boxes, walls, a house shell, doors, windows, a roof from the dakmal,
 * and at the end the right stair corners and the connections of fences, panes and walls (so they look right however the
 * blocks get placed).
 */
final class Bouwdoos {
    static final BlockState LUCHT = Blocks.AIR.defaultBlockState();
    final Map<BlockPos, BlockState> m = new LinkedHashMap<>();

    /** A guhs block (or the fallback when it is not there). */
    static Block g(String id, Block anders) {
        Block b = Familie.blok("guhs:" + id);
        return b != null ? b : anders;
    }

    Bouwdoos zet(int x, int y, int z, BlockState s) {
        m.put(new BlockPos(x, y, z), s);
        return this;
    }

    Bouwdoos zet(int x, int y, int z, Block b) {
        return zet(x, y, z, Vormen.staat(b));
    }

    Bouwdoos vul(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    zet(x, y, z, s);
                }
            }
        }
        return this;
    }

    Bouwdoos vul(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        return vul(x0, y0, z0, x1, y1, z1, Vormen.staat(b));
    }

    /** The four walls of a box (every layer's edge). */
    Bouwdoos ring(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (x == x0 || x == x1 || z == z0 || z == z1) {
                        zet(x, y, z, b);
                    }
                }
            }
        }
        return this;
    }

    /** A house shell: floor at y0, walls (corner posts {@code hoek}) up to y0 + hoogte, the inside empty. */
    Bouwdoos huis(int x0, int z0, int x1, int z1, int y0, int hoogte, Block vloer, Block muur, Block hoek) {
        vul(x0, y0, z0, x1, y0, z1, vloer);
        vul(x0 + 1, y0 + 1, z0 + 1, x1 - 1, y0 + hoogte, z1 - 1, LUCHT);
        ring(x0, y0 + 1, z0, x1, y0 + hoogte, z1, muur);
        for (int y = y0 + 1; y <= y0 + hoogte; y++) {
            zet(x0, y, z0, hoek).zet(x1, y, z0, hoek).zet(x0, y, z1, hoek).zet(x1, y, z1, hoek);
        }
        return this;
    }

    /** A door (two halves) at x, y, z facing out that way. */
    Bouwdoos deur(int x, int y, int z, Direction naar, Block deur) {
        BlockState s = deur.defaultBlockState().setValue(DoorBlock.FACING, naar);
        zet(x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        return zet(x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    Bouwdoos trap(int x, int y, int z, Block trap, Direction op, boolean boven) {
        BlockState s = trap instanceof StairBlock ? trap.defaultBlockState().setValue(StairBlock.FACING, op)
                .setValue(StairBlock.HALF, boven ? Half.TOP : Half.BOTTOM) : trap.defaultBlockState();
        return zet(x, y, z, s);
    }

    Bouwdoos plaat(int x, int y, int z, Block plaat, boolean boven) {
        BlockState s = plaat instanceof SlabBlock ? plaat.defaultBlockState().setValue(SlabBlock.TYPE, boven ? SlabType.TOP : SlabType.BOTTOM)
                : plaat.defaultBlockState();
        return zet(x, y, z, s);
    }

    Bouwdoos ladder(int x, int y0, int y1, int z, Direction naar) {
        for (int y = y0; y <= y1; y++) {
            zet(x, y, z, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, naar));
        }
        return this;
    }

    Bouwdoos lamp(int x, int y, int z, boolean hangend) {
        return zet(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, hangend));
    }

    /** A block that faces a way (a chair, a bench...). */
    Bouwdoos gericht(int x, int y, int z, Block b, Direction naar) {
        BlockState s = b.defaultBlockState();
        if (s.hasProperty(HorizontalDirectionalBlock.FACING)) {
            s = s.setValue(HorizontalDirectionalBlock.FACING, naar);
        }
        return zet(x, y, z, s);
    }

    /** A roof from the dakmal over the house x0..x1 × z0..z1, eaves at y. */
    Bouwdoos dak(Daken.Dak soort, int x0, int z0, int x1, int z1, int y, Block mat, Block gevel, int overstek, boolean gevels, Block oor) {
        Map<BlockPos, BlockState> d = Daken.blokken(soort, x1 - x0 + 1, z1 - z0 + 1, overstek, gevels, mat, gevel, oor);
        d.forEach((p, s) -> m.put(p.offset(x0 - overstek, y, z0 - overstek), s));
        return this;
    }

    /** A filled or hollow round column (a tower), centre cx, cz. */
    Bouwdoos cilinder(double cx, double cz, double r, int y0, int y1, Block b, boolean hol) {
        for (int x = (int) Math.floor(cx - r); x <= (int) Math.ceil(cx + r); x++) {
            for (int z = (int) Math.floor(cz - r); z <= (int) Math.ceil(cz + r); z++) {
                double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                if (d > r + 0.3) {
                    continue;
                }
                for (int y = y0; y <= y1; y++) {
                    zet(x, y, z, hol && d < r - 0.7 ? LUCHT : Vormen.staat(b));
                }
            }
        }
        return this;
    }

    /** Ready: stair corners and the connections of fences, panes and walls, then moved to start at 0, 0, 0. */
    Ontwerp ontwerp() {
        Daken.trapvormen(m);
        verbind(m);
        return Ontwerp.van(m);
    }

    /** Fences, panes and walls connect to their own kind and to full blocks next to them. */
    static void verbind(Map<BlockPos, BlockState> m) {
        for (Map.Entry<BlockPos, BlockState> e : m.entrySet()) {
            BlockState s = e.getValue();
            Block b = s.getBlock();
            if (!(b instanceof CrossCollisionBlock || b instanceof WallBlock)) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockState n = m.get(e.getKey().relative(d));
                boolean aan = n != null && !n.isAir() && (n.isSolidRender() || zelfde(b, n.getBlock()));
                if (b instanceof WallBlock) {
                    s = s.setValue(switch (d) {
                        case NORTH -> WallBlock.NORTH;
                        case SOUTH -> WallBlock.SOUTH;
                        case EAST -> WallBlock.EAST;
                        default -> WallBlock.WEST;
                    }, aan ? WallSide.LOW : WallSide.NONE);
                } else {
                    s = s.setValue(switch (d) {
                        case NORTH -> CrossCollisionBlock.NORTH;
                        case SOUTH -> CrossCollisionBlock.SOUTH;
                        case EAST -> CrossCollisionBlock.EAST;
                        default -> CrossCollisionBlock.WEST;
                    }, aan);
                }
            }
            e.setValue(s);
        }
    }

    private static boolean zelfde(Block a, Block n) {
        return a instanceof FenceBlock && (n instanceof FenceBlock || n instanceof FenceGateBlock)
                || a instanceof IronBarsBlock && n instanceof IronBarsBlock
                || a instanceof WallBlock && n instanceof WallBlock;
    }
}
