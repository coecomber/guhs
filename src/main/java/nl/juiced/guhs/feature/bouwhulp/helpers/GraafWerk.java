package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The digging templates (design 2.8): the graafploeg (meerkat, badger, wombat: cellar, tunnel, pond, burrow network,
 * stream), the elephant's mud pool and the capybara's baths. They only dig natural ground; a wall that would be loose
 * (air, sand, water) gets planks or stone, so nothing caves in or floods.
 */
final class GraafWerk {
    static final BlockState LUCHT = Blocks.AIR.defaultBlockState();

    private static void graaf(Klus k, Map<BlockPos, BlockState> m, BlockPos q) {
        BlockState nu = k.staat(q);
        if (!nu.isAir() && Klus.natuur(nu)) {
            m.put(q, LUCHT);
        }
    }

    /** A wall/ceiling spot of a dug room: kept when it is firm ground, otherwise lined with {@code bekleding}. */
    private static void wand(Klus k, Map<BlockPos, BlockState> m, BlockPos q, BlockState bekleding) {
        if (m.containsKey(q)) {
            return;
        }
        BlockState nu = k.staat(q);
        if (nu.isAir() || !nu.getFluidState().isEmpty() || nu.is(net.minecraft.tags.BlockTags.SAND) || nu.is(Blocks.GRAVEL) || nu.canBeReplaced()) {
            m.put(q, bekleding);
        }
    }

    private static int vlakGenoeg(Klus k, BlockPos p, int r) {
        int g = k.grond(p.getX(), p.getZ());
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r && Math.abs(k.grond(p.getX() + dx, p.getZ() + dz) - g) > 2) {
                    throw Klus.nee("te_scheef");
                }
            }
        }
        return g;
    }

    /** A cellar under the spot: a room with plank walls where the ground is loose, a ladder shaft with a trapdoor, a lantern. */
    static Map<BlockPos, BlockState> kelder(Klus k, BlockPos p) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int g = k.grond(p.getX(), p.getZ());
        int half = 2 + k.grootte();
        int vloer = g - 5, plafond = g - 1;
        BlockState plank = Blocks.SPRUCE_PLANKS.defaultBlockState();
        for (int dx = -half - 1; dx <= half + 1; dx++) {
            for (int dz = -half - 1; dz <= half + 1; dz++) {
                boolean rand = Math.abs(dx) == half + 1 || Math.abs(dz) == half + 1;
                for (int y = vloer; y <= plafond; y++) {
                    BlockPos q = p.offset(dx, 0, dz).atY(y);
                    if (rand || y == vloer || y == plafond) {
                        wand(k, m, q, plank);
                    } else {
                        m.put(q, LUCHT);
                    }
                }
            }
        }
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                m.put(p.offset(dx, 0, dz).atY(vloer), (dx + dz & 1) == 0 ? plank : Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
            }
        }
        // the shaft in a corner, the ladder against its north wall
        BlockPos schacht = p.offset(-half, 0, -half + 1);
        for (int y = vloer + 1; y <= g; y++) {
            BlockPos q = schacht.atY(y);
            m.put(q, Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
            m.put(q.north(), y > plafond ? Blocks.SPRUCE_PLANKS.defaultBlockState() : m.getOrDefault(q.north(), plank));
        }
        m.put(schacht.atY(g + 1), Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.OPEN, true)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        m.put(p.atY(plafond - 1), Blok.met(Blocks.LANTERN.defaultBlockState(), "hanging", "true"));
        m.put(p.offset(half, 0, half).atY(vloer + 1), Blocks.BARREL.defaultBlockState());
        return m;
    }

    /** A tunnel into the hillside, three wide and three high, with a wooden frame and a lantern every four blocks. */
    static Map<BlockPos, BlockState> tunnel(Klus k, BlockPos p) {
        Direction d = k.helling(p, 6, 3);
        if (d == null) {
            throw Klus.nee("geen_heuvel");
        }
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int g = k.grond(p.getX(), p.getZ());
        int lengte = 8 + 4 * k.grootte();
        Direction zij = d.getClockWise();
        for (int i = 0; i < lengte; i++) {
            BlockPos rij = p.relative(d, i).atY(g);
            boolean frame = i % 4 == 2;
            for (int j = -2; j <= 2; j++) {
                for (int y = g; y <= g + 4; y++) {
                    BlockPos q = rij.relative(zij, j).atY(y);
                    boolean binnen = Math.abs(j) <= 1 && y >= g + 1 && y <= g + 3;
                    if (binnen) {
                        graaf(k, m, q);
                        if (y == g + 3 && j == 0 && frame) {
                            m.put(q, Blok.met(Blocks.LANTERN.defaultBlockState(), "hanging", "true"));
                        }
                    } else if (frame && (Math.abs(j) == 2 && y >= g + 1 && y <= g + 3)) {
                        m.put(q, Blocks.SPRUCE_LOG.defaultBlockState());
                    } else if (frame && y == g + 4 && Math.abs(j) <= 2) {
                        m.put(q, Blok.as(Blocks.SPRUCE_LOG.defaultBlockState(), zij.getAxis()));
                    } else if (y == g) {
                        wand(k, m, q, Blocks.COBBLESTONE.defaultBlockState());
                    } else {
                        wand(k, m, q, Blocks.SPRUCE_PLANKS.defaultBlockState());
                    }
                }
            }
        }
        return m;
    }

    static BlockPos tunnelZoek(Klus k) {
        for (BlockPos c : k.rondBord(k.zoekStraal())) {
            if ((c.getX() + c.getZ()) % 3 != 0 || !k.level().isLoaded(c) || k.isWater(c.getX(), c.getZ())) {
                continue;
            }
            if (k.helling(c, 6, 4) != null) {
                return k.grondPos(c.getX(), c.getZ());
            }
        }
        throw Klus.nee("geen_heuvel");
    }

    /**
     * A pond, flush with the ground: water 1 deep at its edge and 2 in the middle, a sand/clay bed, water lilies. {@code soort}:
     * "vijver", "vijvertje" (small, round), "modderpoel" (mud with a puddle).
     */
    static Map<BlockPos, BlockState> vijver(Klus k, BlockPos p, String soort) {
        int rx = switch (soort) {
            case "vijvertje" -> 2 + k.grootte() / 2;
            case "modderpoel" -> 2 + k.grootte();
            default -> 3 + k.grootte();
        };
        int rz = soort.equals("vijver") ? rx - 1 : rx;
        int g = vlakGenoeg(k, p, rx);
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        boolean modder = soort.equals("modderpoel");
        for (int dx = -rx - 1; dx <= rx + 1; dx++) {
            for (int dz = -rz - 1; dz <= rz + 1; dz++) {
                double d = Math.sqrt(dx * dx / (double) (rx * rx) + dz * dz / (double) (rz * rz));
                BlockPos kol = p.offset(dx, 0, dz);
                if (d > 1.0) {
                    // the rim: no hole where the water would run out
                    if (d < 1.0 + 1.5 / rx && k.grond(kol.getX(), kol.getZ()) < g) {
                        for (int y = k.grond(kol.getX(), kol.getZ()) + 1; y <= g; y++) {
                            m.put(kol.atY(y), y == g ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                        }
                    }
                    continue;
                }
                int diep = d < 0.55 ? 2 : 1;
                for (int y = g - diep; y <= g; y++) {
                    BlockPos q = kol.atY(y);
                    if (modder) {
                        m.put(q, d < 0.4 && y == g ? WaterWerk.WATER : Blocks.MUD.defaultBlockState());
                    } else {
                        m.put(q, WaterWerk.WATER);
                    }
                }
                m.put(kol.atY(g - diep - 1), modder ? Blocks.MUD.defaultBlockState() : (dx + dz & 1) == 0 ? Blocks.SAND.defaultBlockState() : Blocks.CLAY.defaultBlockState());
                for (int y = g + 1; y <= g + 2; y++) {
                    graaf(k, m, kol.atY(y));
                }
                if (!modder && d > 0.5 && k.r().nextInt(7) == 0) {
                    m.put(kol.atY(g + 1), Blocks.LILY_PAD.defaultBlockState());
                }
            }
        }
        return m;
    }

    /** The capybara's warm bath: a square pool with a stone rim and a crackling campfire next to it. */
    static Map<BlockPos, BlockState> badje(Klus k, BlockPos p) {
        int half = 1 + k.grootte() / 2;
        int g = vlakGenoeg(k, p, half + 1);
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        for (int dx = -half - 1; dx <= half + 1; dx++) {
            for (int dz = -half - 1; dz <= half + 1; dz++) {
                BlockPos kol = p.offset(dx, 0, dz);
                boolean rand = Math.abs(dx) == half + 1 || Math.abs(dz) == half + 1;
                if (rand) {
                    m.put(kol.atY(g), Blocks.POLISHED_ANDESITE.defaultBlockState());
                    m.put(kol.atY(g + 1), Blocks.ANDESITE_SLAB.defaultBlockState());
                } else {
                    m.put(kol.atY(g - 1), Blocks.SMOOTH_STONE.defaultBlockState());
                    m.put(kol.atY(g), WaterWerk.WATER);
                    m.put(kol.atY(g + 1), WaterWerk.WATER);
                }
            }
        }
        BlockPos vuur = p.offset(half + 3, 0, 0);
        m.put(vuur.atY(k.grond(vuur.getX(), vuur.getZ()) + 1), Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        return m;
    }

    /** The hot tub: round, a raised rim of smooth stone, a step inside, lanterns on the rim. */
    static Map<BlockPos, BlockState> hottub(Klus k, BlockPos p) {
        int r = 2 + k.grootte();
        int g = vlakGenoeg(k, p, r + 1);
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                BlockPos kol = p.offset(dx, 0, dz);
                if (d > r + 0.7) {
                    continue;
                }
                if (d > r - 0.3) {
                    m.put(kol.atY(g), Blocks.SMOOTH_STONE.defaultBlockState());
                    m.put(kol.atY(g + 1), Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
                    if ((dx == 0 || dz == 0) && Math.abs(dx) + Math.abs(dz) >= r) {
                        m.put(kol.atY(g + 2), Blok.met(Blocks.LANTERN.defaultBlockState(), "hanging", "false"));
                    }
                } else {
                    m.put(kol.atY(g - 1), d > r - 1.3 ? Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE,
                            net.minecraft.world.level.block.state.properties.SlabType.TOP).setValue(BlockStateProperties.WATERLOGGED, true)
                            : WaterWerk.WATER);
                    m.put(kol.atY(g - 2), Blocks.SMOOTH_STONE.defaultBlockState());
                    m.put(kol.atY(g), WaterWerk.WATER);
                    m.put(kol.atY(g + 1), WaterWerk.WATER);
                }
            }
        }
        return m;
    }

    /** The burrow network: three little rooms underground, tunnels between them, two holes up with a mound of earth. */
    static Map<BlockPos, BlockState> holletjesnet(Klus k, BlockPos p) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int g = k.grond(p.getX(), p.getZ());
        int y0 = g - 4;
        int[][] kamers = k.grootte() >= 1 ? new int[][]{{0, 0}, {6, 2}, {-5, 4}, {2, -6}} : new int[][]{{0, 0}, {6, 2}, {-5, 4}};
        for (int[] c : kamers) {
            BlockPos kamer = p.offset(c[0], 0, c[1]).atY(y0);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int y = 0; y <= 1; y++) {
                        BlockPos q = kamer.offset(dx, y, dz);
                        m.put(q, y == 0 && (dx + dz & 1) == 0 ? Blocks.MOSS_CARPET.defaultBlockState() : LUCHT);
                    }
                    wand(k, m, kamer.offset(dx, -1, dz), Blocks.PACKED_MUD.defaultBlockState());
                    wand(k, m, kamer.offset(dx, 2, dz), Blocks.PACKED_MUD.defaultBlockState());
                }
            }
        }
        for (int i = 1; i < kamers.length; i++) {
            int x0 = p.getX(), z0 = p.getZ(), x1 = p.getX() + kamers[i][0], z1 = p.getZ() + kamers[i][1];
            int stappen = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
            for (int s = 0; s <= stappen; s++) {
                int x = x0 + Math.round((x1 - x0) * s / (float) stappen), z = z0 + Math.round((z1 - z0) * s / (float) stappen);
                for (int y = 0; y <= 1; y++) {
                    BlockPos q = new BlockPos(x, y0 + y, z);
                    if (!m.containsKey(q) || m.get(q) != Blocks.MOSS_CARPET.defaultBlockState()) {
                        m.put(q, LUCHT);
                    }
                }
                wand(k, m, new BlockPos(x, y0 + 2, z), Blocks.PACKED_MUD.defaultBlockState());
            }
        }
        // two ways up, each with a little mound
        for (int i = 1; i <= 2; i++) {
            BlockPos gat = p.offset(kamers[i][0], 0, kamers[i][1]);
            int gg = k.grond(gat.getX(), gat.getZ());
            for (int y = y0; y <= gg + 1; y++) {
                m.put(gat.atY(y), LUCHT);
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos heuvel = gat.relative(d);
                m.put(heuvel.atY(k.grond(heuvel.getX(), heuvel.getZ()) + 1), Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        return m;
    }

    /** A stream from the spot downhill: a channel of water that never climbs, a gravel bed, little banks where it would spill. */
    static Map<BlockPos, BlockState> beekje(Klus k, BlockPos p) {
        Direction omlaag = null;
        int laagst = Integer.MAX_VALUE;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int h = k.grond(p.getX() + d.getStepX() * 10, p.getZ() + d.getStepZ() * 10);
            if (h < laagst) {
                laagst = h;
                omlaag = d;
            }
        }
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int lengte = 10 + 5 * k.grootte();
        int breed = k.grootte() >= 1 ? 2 : 1;
        Direction zij = omlaag.getClockWise();
        int peil = k.grond(p.getX(), p.getZ());
        for (int i = 0; i < lengte; i++) {
            BlockPos rij = p.relative(omlaag, i);
            peil = Math.min(peil, k.grond(rij.getX(), rij.getZ()));
            for (int j = 0; j < breed; j++) {
                BlockPos kol = rij.relative(zij, j);
                int g = k.grond(kol.getX(), kol.getZ());
                for (int y = peil; y <= g; y++) {
                    m.put(kol.atY(y), y == peil ? WaterWerk.WATER : LUCHT);
                }
                for (int y = g + 1; y <= g + 2; y++) {
                    graaf(k, m, kol.atY(y));
                }
                m.put(kol.atY(peil - 1), (i + j) % 3 == 0 ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState());
            }
            for (int j : new int[]{-1, breed}) {
                BlockPos kol = rij.relative(zij, j);
                int g = k.grond(kol.getX(), kol.getZ());
                for (int y = g + 1; y <= peil; y++) {
                    m.put(kol.atY(y), y == peil ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                }
            }
        }
        return m;
    }

    private GraafWerk() {
    }
}
