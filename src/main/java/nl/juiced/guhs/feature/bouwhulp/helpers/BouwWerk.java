package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The building templates (design 2.8): the orangutan's treehouse and bridges between two trees, the penguin's igloo, ice
 * slide and ice sculpture.
 */
final class BouwWerk {
    static final BlockState LUCHT = Blocks.AIR.defaultBlockState();

    private static void ruim(Klus k, Map<BlockPos, BlockState> m, BlockPos q) {
        BlockState s = k.staat(q);
        if (!m.containsKey(q) && (s.is(BlockTags.LEAVES) || !s.isAir() && s.canBeReplaced())) {
            m.put(q, LUCHT);
        }
    }

    /** The trunk (its bottom log) at or near p. */
    private static BlockPos stam(Klus k, BlockPos p) {
        BlockPos s = TerreinWerk.boomBij(k, p, 4);
        if (s == null) {
            throw Klus.nee("geen_bomen");
        }
        return s;
    }

    /** A treehouse: a platform round the trunk with a railing, a little roof on posts, a ladder up the trunk to the board side. */
    static Map<BlockPos, BlockState> boomhut(Klus k, BlockPos p) {
        BlockPos stam = stam(k, p);
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int half = 2 + k.grootte() / 2 + (k.grootte() == 2 ? 1 : 0);
        int vloer = stam.getY() + 3 + k.grootte();
        Direction trap = Direction.getApproximateNearest(k.bord().getX() - stam.getX(), 0, k.bord().getZ() - stam.getZ());
        BlockState plank = Blocks.JUNGLE_PLANKS.defaultBlockState(), hek = Blocks.JUNGLE_FENCE.defaultBlockState();
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                BlockPos kol = stam.offset(dx, 0, dz);
                if (dx == 0 && dz == 0) {
                    continue;
                }
                m.put(kol.atY(vloer), plank);
                for (int y = vloer + 1; y <= vloer + 3; y++) {
                    ruim(k, m, kol.atY(y));
                }
                boolean rand = Math.abs(dx) == half || Math.abs(dz) == half;
                boolean hoek = Math.abs(dx) == half && Math.abs(dz) == half;
                BlockPos trapGat = stam.relative(trap, 1).relative(trap, half - 1);
                if (rand && !(kol.getX() == trapGat.getX() && kol.getZ() == trapGat.getZ())) {
                    m.put(kol.atY(vloer + 1), hek);
                }
                if (hoek) {
                    m.put(kol.atY(vloer + 2), hek);
                    m.put(kol.atY(vloer + 3), hek);
                }
            }
        }
        // the roof: slabs one above the posts, a little wider
        for (int dx = -half - 1; dx <= half + 1; dx++) {
            for (int dz = -half - 1; dz <= half + 1; dz++) {
                BlockPos q = stam.offset(dx, 0, dz).atY(vloer + 4);
                if (dx == 0 && dz == 0) {
                    continue;
                }
                m.put(q, (Math.abs(dx) > half || Math.abs(dz) > half) ? Blocks.JUNGLE_SLAB.defaultBlockState()
                        : Blocks.JUNGLE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP));
            }
        }
        m.put(stam.relative(trap, 1).atY(vloer + 3), Blok.met(Blocks.LANTERN.defaultBlockState(), "hanging", "true"));
        // the ladder on the trunk, facing away from it
        for (int y = stam.getY(); y < vloer + 1; y++) {
            BlockPos q = stam.relative(trap).atY(y);
            m.put(q, Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, trap));
            if (!k.staat(stam.atY(y)).is(BlockTags.LOGS)) {
                m.put(stam.atY(y), Blocks.JUNGLE_LOG.defaultBlockState());
            }
        }
        m.put(stam.relative(trap).atY(vloer), m.get(stam.relative(trap).atY(vloer - 1)));
        Blok.verbind(m);
        return m;
    }

    /** Two trees for a bridge: the one at p and the nearest other one 5..20 away. */
    static BlockPos[] tweeBomen(Klus k, BlockPos p) {
        BlockPos a = stam(k, p);
        BlockPos b = null;
        int d = Integer.MAX_VALUE;
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                int dd = dx * dx + dz * dz;
                if (dd < 25 || dd > 400 || dd >= d) {
                    continue;
                }
                int x = a.getX() + dx, z = a.getZ() + dz;
                int g = k.grond(x, z);
                if (k.staat(new BlockPos(x, g + 1, z)).is(BlockTags.LOGS) && k.staat(new BlockPos(x, g + 3, z)).is(BlockTags.LOGS)) {
                    d = dd;
                    b = new BlockPos(x, g + 1, z);
                }
            }
        }
        if (b == null) {
            throw Klus.nee("geen_tweede_boom");
        }
        return new BlockPos[]{a, b};
    }

    /**
     * A bridge between two trees, a little platform round each trunk: {@code hang} = a suspension bridge that sags in the
     * middle (slabs and chains), otherwise a straight rope bridge with fence railings.
     */
    static Map<BlockPos, BlockState> brug(Klus k, BlockPos a, BlockPos b, boolean hang) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int hoogte = Math.max(a.getY(), b.getY()) + 4 + k.grootte();
        for (BlockPos stam : new BlockPos[]{a, b}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        m.put(stam.offset(dx, 0, dz).atY(hoogte), Blocks.JUNGLE_PLANKS.defaultBlockState());
                        ruim(k, m, stam.offset(dx, 0, dz).atY(hoogte + 1));
                        ruim(k, m, stam.offset(dx, 0, dz).atY(hoogte + 2));
                    }
                }
            }
        }
        int dx = b.getX() - a.getX(), dz = b.getZ() - a.getZ();
        int stappen = Math.max(Math.abs(dx), Math.abs(dz));
        boolean langsX = Math.abs(dx) >= Math.abs(dz);
        double zak = hang ? Math.max(1, stappen / 7.0) : 0;
        for (int s = 2; s <= stappen - 2; s++) {
            double t = s / (double) stappen;
            int x = a.getX() + (int) Math.round(dx * t), z = a.getZ() + (int) Math.round(dz * t);
            double hy = hoogte - zak * 4 * t * (1 - t);
            int y = (int) Math.floor(hy);
            boolean half = hang && hy - y >= 0.5;
            BlockPos q = new BlockPos(x, half ? y : y, z);
            BlockState dek = hang ? (half ? Blocks.JUNGLE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP)
                    : Blocks.JUNGLE_SLAB.defaultBlockState()) : Blocks.JUNGLE_PLANKS.defaultBlockState();
            if (hang && !half) {
                q = q.above();
            }
            m.put(q, dek);
            ruim(k, m, q.above());
            ruim(k, m, q.above(2));
            for (int j : new int[]{-1, 1}) {
                BlockPos zij = langsX ? q.offset(0, 0, j) : q.offset(j, 0, 0);
                m.put(zij.above(), hang ? Blok.as(Blocks.IRON_CHAIN.defaultBlockState(), Direction.Axis.Y) : Blocks.JUNGLE_FENCE.defaultBlockState());
                if (!hang) {
                    m.put(zij, Blocks.JUNGLE_PLANKS.defaultBlockState());
                }
            }
        }
        Blok.verbind(m);
        return m;
    }

    /** An igloo: a dome of snow blocks, a little ice window, an entrance tunnel toward the board, a carpet and a lantern inside. */
    static Map<BlockPos, BlockState> iglo(Klus k, BlockPos p) {
        int r = 3 + k.grootte();
        int g = GraafWerkVlak.vlak(k, p, r);
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        Direction deur = Direction.getApproximateNearest(k.bord().getX() - p.getX(), 0, k.bord().getZ() - p.getZ());
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = 0; dy <= r; dy++) {
                    double d = Math.sqrt(dx * dx + dz * dz + dy * dy * 1.2);
                    BlockPos q = p.offset(dx, 0, dz).atY(g + 1 + dy);
                    if (d > r + 0.4) {
                        continue;
                    }
                    if (d > r - 0.6) {
                        boolean raam = dy == 1 && (dx == 0 && Math.abs(dz) >= r - 1 || dz == 0 && Math.abs(dx) >= r - 1);
                        m.put(q, raam ? Blocks.ICE.defaultBlockState() : Blocks.SNOW_BLOCK.defaultBlockState());
                    } else {
                        m.put(q, dy == 0 ? Blocks.LIGHT_BLUE_CARPET.defaultBlockState() : LUCHT);
                    }
                }
            }
        }
        m.put(p.atY(g + r), Blok.met(Blocks.LANTERN.defaultBlockState(), "hanging", "true"));
        for (int i = r - 1; i <= r + 2; i++) {
            BlockPos gang = p.relative(deur, i);
            Direction zij = deur.getClockWise();
            for (int y = 1; y <= 2; y++) {
                m.put(gang.atY(g + y), LUCHT);
                m.put(gang.relative(zij).atY(g + y), Blocks.SNOW_BLOCK.defaultBlockState());
                m.put(gang.relative(zij.getOpposite()).atY(g + y), Blocks.SNOW_BLOCK.defaultBlockState());
            }
            m.put(gang.atY(g + 3), Blocks.SNOW_BLOCK.defaultBlockState());
        }
        return m;
    }

    /** An ice slide: a snow tower with a ladder, a ramp of packed ice going down toward the water (or away from the board). */
    static Map<BlockPos, BlockState> ijsglijbaan(Klus k, BlockPos p) {
        int hoog = 4 + k.grootte();
        int g = k.grond(p.getX(), p.getZ());
        BlockPos water = WaterWerk.naasteWater(k, p, 14);
        Direction d = water != null ? Direction.getApproximateNearest(water.getX() - p.getX(), 0, water.getZ() - p.getZ())
                : Direction.getApproximateNearest(p.getX() - k.bord().getX(), 0, p.getZ() - k.bord().getZ());
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        Direction zij = d.getClockWise();
        for (int dx = 0; dx <= 1; dx++) {
            for (int y = g + 1; y <= g + hoog; y++) {
                m.put(p.relative(zij, dx).atY(y), Blocks.SNOW_BLOCK.defaultBlockState());
            }
        }
        for (int dx = 0; dx <= 1; dx++) {
            BlockPos l = p.relative(zij, dx).relative(d.getOpposite());
            for (int y = g + 1; y <= g + hoog; y++) {
                m.put(l.atY(y), Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, d.getOpposite()));
            }
        }
        int y = g + hoog;
        for (int i = 1; y > g; i++) {
            y = g + hoog - (i + 1) / 2;
            for (int dx = 0; dx <= 1; dx++) {
                BlockPos kol = p.relative(d, i).relative(zij, dx);
                int gg = k.grond(kol.getX(), kol.getZ());
                for (int yy = gg + 1; yy <= y; yy++) {
                    m.put(kol.atY(yy), yy == y ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.SNOW_BLOCK.defaultBlockState());
                }
                for (int yy = y + 1; yy <= y + 2; yy++) {
                    ruim(k, m, kol.atY(yy));
                }
            }
            for (int j : new int[]{-1, 2}) {
                BlockPos rand = p.relative(d, i).relative(zij, j);
                if (k.grond(rand.getX(), rand.getZ()) < y) {
                    m.put(rand.atY(y + 1), Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 4));
                }
            }
            if (i > 40) {
                break;
            }
        }
        return m;
    }

    /** An ice sculpture of a guh: a round body of packed ice, a head of ice, blue-ice ears and two dark eyes. */
    static Map<BlockPos, BlockState> ijssculptuur(Klus k, BlockPos p) {
        int g = k.grond(p.getX(), p.getZ());
        double rb = 1.6 + k.grootte() * 0.6, rk = 1.2 + k.grootte() * 0.4;
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int lijf = (int) Math.ceil(rb);
        double kopY = 2 * rb + rk * 0.6;
        Direction kijk = Direction.getApproximateNearest(k.bord().getX() - p.getX(), 0, k.bord().getZ() - p.getZ());
        for (int dx = -lijf - 1; dx <= lijf + 1; dx++) {
            for (int dz = -lijf - 1; dz <= lijf + 1; dz++) {
                for (int dy = 0; dy <= (int) (kopY + rk + 2); dy++) {
                    BlockPos q = p.offset(dx, 0, dz).atY(g + 1 + dy);
                    double yb = dy + 0.5 - rb, yk = dy + 0.5 - kopY;
                    if (Math.sqrt(dx * dx + dz * dz + yb * yb) <= rb) {
                        m.put(q, Blocks.PACKED_ICE.defaultBlockState());
                    } else if (Math.sqrt(dx * dx + dz * dz + yk * yk) <= rk) {
                        m.put(q, Blocks.ICE.defaultBlockState());
                    }
                }
            }
        }
        int top = (int) Math.round(kopY + rk);
        Direction zij = kijk.getClockWise();
        int oor = Math.max(1, (int) Math.round(rk * 0.8));
        m.put(p.relative(zij, oor).atY(g + 1 + top), Blocks.BLUE_ICE.defaultBlockState());
        m.put(p.relative(zij, -oor).atY(g + 1 + top), Blocks.BLUE_ICE.defaultBlockState());
        int ooghoogte = g + 1 + (int) Math.round(kopY);
        int voor = (int) Math.floor(rk);
        m.put(p.relative(kijk, voor).relative(zij, 1).atY(ooghoogte), Blocks.BLACK_CONCRETE.defaultBlockState());
        m.put(p.relative(kijk, voor).relative(zij, -1).atY(ooghoogte), Blocks.BLACK_CONCRETE.defaultBlockState());
        return m;
    }

    /** (A small stand-in so the igloo shares the "flat enough" rule of the digging templates.) */
    static final class GraafWerkVlak {
        static int vlak(Klus k, BlockPos p, int r) {
            int g = k.grond(p.getX(), p.getZ());
            for (int dx = -r; dx <= r; dx += Math.max(1, r)) {
                for (int dz = -r; dz <= r; dz += Math.max(1, r)) {
                    if (Math.abs(k.grond(p.getX() + dx, p.getZ() + dz) - g) > 2) {
                        throw Klus.nee("te_scheef");
                    }
                }
            }
            return g;
        }
    }

    private BouwWerk() {
    }
}
