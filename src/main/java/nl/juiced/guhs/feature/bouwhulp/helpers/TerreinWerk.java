package nl.juiced.guhs.feature.bouwhulp.helpers;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The terrain templates (design 2.8): the elephant (clearing fallen trees, stamping flat), the beaver felling trees, the
 * zebras' paths, the planters (koala, toucan, bumblebee), the demolition crew (rhino, buffalo) and the mammoth's snow.
 */
final class TerreinWerk {
    static final BlockState LUCHT = Blocks.AIR.defaultBlockState();

    /** Fallen trees (logs lying on their side) and loose leaves around the spot go. */
    static Map<BlockPos, BlockState> opruimen(Klus k, BlockPos p) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int r = 6 + 2 * k.grootte();
        int g = k.grond(p.getX(), p.getZ());
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-r, 0, -r).atY(g - 2), p.offset(r, 0, r).atY(g + 5))) {
            BlockState s = k.staat(q);
            if (s.is(BlockTags.LOGS) && s.hasProperty(BlockStateProperties.AXIS) && s.getValue(BlockStateProperties.AXIS) != Direction.Axis.Y) {
                m.put(q.immutable(), LUCHT);
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("geen_omgevallen");
        }
        return m;
    }

    /** The ground around the spot stamped flat at the spot's height: humps go, holes fill with earth and grass. */
    static Map<BlockPos, BlockState> stampen(Klus k, BlockPos p) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int r = 3 + k.grootte();
        int g = k.grond(p.getX(), p.getZ());
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + r) {
                    continue;
                }
                BlockPos kol = p.offset(dx, 0, dz);
                int h = k.grond(kol.getX(), kol.getZ());
                if (k.isWater(kol.getX(), kol.getZ())) {
                    continue;
                }
                for (int y = g + 1; y <= Math.max(h, g) + 2; y++) {
                    BlockState s = k.staat(kol.atY(y));
                    if (!s.isAir() && Klus.natuur(s) && !s.is(BlockTags.LEAVES)) {
                        m.put(kol.atY(y), LUCHT);
                    }
                }
                for (int y = h + 1; y <= g; y++) {
                    m.put(kol.atY(y), y == g ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                }
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("al_vlak");
        }
        return m;
    }

    /** The trunk columns of trees near p: logs standing on the ground with leaves above. */
    @Nullable
    static BlockPos boomBij(Klus k, BlockPos p, int r) {
        BlockPos beste = null;
        int d = Integer.MAX_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int x = p.getX() + dx, z = p.getZ() + dz;
                int g = k.grond(x, z);
                BlockState s = k.staat(new BlockPos(x, g + 1, z));
                if (s.is(BlockTags.LOGS) && k.staat(new BlockPos(x, g + 2, z)).is(BlockTags.LOGS) && dx * dx + dz * dz < d) {
                    d = dx * dx + dz * dz;
                    beste = new BlockPos(x, g + 1, z);
                }
            }
        }
        return beste;
    }

    static BlockPos boomZoek(Klus k) {
        for (BlockPos c : k.rondBord(k.zoekStraal())) {
            if ((c.getX() ^ c.getZ()) % 2 != 0 || !k.level().isLoaded(c)) {
                continue;
            }
            int g = k.grond(c.getX(), c.getZ());
            if (k.staat(c.atY(g + 1)).is(BlockTags.LOGS) && k.staat(c.atY(g + 2)).is(BlockTags.LOGS)) {
                return c.atY(g + 1);
            }
        }
        throw Klus.nee("geen_bomen");
    }

    /** The beaver fells the trees around the spot: logs and their leaves go, a gnawed stump stays. */
    static Map<BlockPos, BlockState> kappen(Klus k, BlockPos p) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int r = 4 + 2 * k.grootte();
        int g = k.grond(p.getX(), p.getZ());
        Set<Long> stronken = new HashSet<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                int x = p.getX() + dx, z = p.getZ() + dz;
                int gg = k.grond(x, z);
                if (k.staat(new BlockPos(x, gg + 1, z)).is(BlockTags.LOGS)) {
                    stronken.add(BlockPos.asLong(x, gg + 1, z));
                }
            }
        }
        if (stronken.isEmpty()) {
            throw Klus.nee("geen_bomen");
        }
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-r - 3, 0, -r - 3).atY(g - 1), p.offset(r + 3, 0, r + 3).atY(g + 30))) {
            BlockState s = k.staat(q);
            if (stronken.contains(q.asLong())) {
                m.put(q.immutable(), Blocks.STRIPPED_OAK_LOG.defaultBlockState());   // the gnawed stump
                continue;
            }
            int dx = q.getX() - p.getX(), dz = q.getZ() - p.getZ();
            boolean binnen = dx * dx + dz * dz <= r * r;
            if (s.is(BlockTags.LOGS) && binnen && q.getY() > g - 1) {
                m.put(q.immutable(), LUCHT);
            } else if (s.is(BlockTags.LEAVES) && !s.getValueOrElse(BlockStateProperties.PERSISTENT, false) && dx * dx + dz * dz <= (r + 3) * (r + 3)) {
                m.put(q.immutable(), LUCHT);
            }
        }
        return m;
    }

    /** The zebras walk a path from the board to the spot, following the terrain (variant 1: a real striped zebra crossing). */
    static Map<BlockPos, BlockState> zebrapad(Klus k, BlockPos van, BlockPos naar) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int dx = naar.getX() - van.getX(), dz = naar.getZ() - van.getZ();
        int stappen = Math.max(Math.abs(dx), Math.abs(dz));
        if (stappen < 3) {
            throw Klus.nee("te_kort");
        }
        stappen = Math.min(stappen, Klus.MAX_AFSTAND + 8);
        boolean gestreept = k.variant() == 1;
        boolean langsX = Math.abs(dx) >= Math.abs(dz);
        int breed = gestreept ? 1 : k.grootte() >= 1 ? 1 : 0;
        for (int s = 0; s <= stappen; s++) {
            int x = van.getX() + Math.round(dx * s / (float) stappen), z = van.getZ() + Math.round(dz * s / (float) stappen);
            for (int j = -breed; j <= breed; j++) {
                int xx = langsX ? x : x + j, zz = langsX ? z + j : z;
                if (k.isWater(xx, zz)) {
                    continue;
                }
                int g = k.grond(xx, zz);
                BlockPos q = new BlockPos(xx, g, zz);
                BlockState nu = k.staat(q);
                if (!Klus.natuur(nu)) {
                    continue;
                }
                BlockState pad = gestreept ? ((s / 2) % 2 == 0 ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState())
                        : (s + j) % 5 == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
                m.put(q, pad);
                BlockPos boven = q.above();
                BlockState b = k.staat(boven);
                if (!b.isAir() && b.canBeReplaced()) {
                    m.put(boven, LUCHT);
                }
            }
        }
        return m;
    }

    static BlockPos padZoek(Klus k) {
        // toward the nearest door (the player's house), else 16 blocks in front of the board
        for (BlockPos c : k.rondBord(Math.min(40, k.zoekStraal()))) {
            if (!k.level().isLoaded(c)) {
                continue;
            }
            int g = k.grond(c.getX(), c.getZ());
            for (int y = g + 1; y <= g + 2; y++) {
                if (k.staat(c.atY(y)).is(BlockTags.DOORS)) {
                    return c.atY(g);
                }
            }
        }
        Direction voor = k.level().getBlockState(k.bord()).getValueOrElse(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        BlockPos p = k.bord().relative(voor, Math.min(16, k.zoekStraal()));
        return k.grondPos(p.getX(), p.getZ());
    }

    /** Planting (variant: bos, struiken, bloemen, boomgaard) on the grass around the spot. */
    static Map<BlockPos, BlockState> planten(Klus k, BlockPos p) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int r = 4 + 2 * k.grootte();
        BlockState[] bomen = {Blocks.OAK_SAPLING.defaultBlockState(), Blocks.BIRCH_SAPLING.defaultBlockState(), Blocks.SPRUCE_SAPLING.defaultBlockState(),
                Blocks.CHERRY_SAPLING.defaultBlockState()};
        BlockState[] bloemen = {Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
                Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(), Blocks.PINK_TULIP.defaultBlockState(),
                Blocks.AZURE_BLUET.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState()};
        int variant = Math.floorMod(k.variant(), 4);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                int x = p.getX() + dx, z = p.getZ() + dz;
                if (k.isWater(x, z)) {
                    continue;
                }
                int g = k.grond(x, z);
                BlockPos op = new BlockPos(x, g + 1, z);
                if (!Klus.aarde(k.staat(op.below())) || !(k.staat(op).isAir() || k.staat(op).canBeReplaced())) {
                    continue;
                }
                BlockState zet = switch (variant) {
                    case 0 -> Math.floorMod(x * 7 + z * 13, 11) == 0 ? bomen[k.r().nextInt(3)] : k.r().nextInt(9) == 0 ? Blocks.FERN.defaultBlockState() : null;
                    case 1 -> k.r().nextInt(5) == 0 ? switch (k.r().nextInt(4)) {
                        case 0 -> Blocks.AZALEA.defaultBlockState();
                        case 1 -> Blocks.FLOWERING_AZALEA.defaultBlockState();
                        case 2 -> Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(BlockStateProperties.AGE_3, 2);
                        default -> Blok.blad("minecraft:oak_leaves");
                    } : null;
                    case 2 -> k.r().nextInt(3) != 0 ? bloemen[k.r().nextInt(bloemen.length)] : null;
                    default -> Math.floorMod(dx, 4) == 0 && Math.floorMod(dz, 4) == 0 ? (k.r().nextBoolean() ? bomen[0] : bomen[3])
                            : k.r().nextInt(6) == 0 ? bloemen[k.r().nextInt(bloemen.length)] : null;
                };
                if (zet != null) {
                    m.put(op, zet);
                }
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("geen_gras");
        }
        return m;
    }

    /**
     * The demolition crew: the clicked building (every block connected to it that is not terrain) or the clicked boulder
     * (connected stone sticking out above the ground around it) goes; the blocks come back through the undo list.
     */
    static Map<BlockPos, BlockState> slopen(Klus k, BlockPos p) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        BlockState start = k.staat(p);
        if (start.isAir() || !start.getFluidState().isEmpty()) {
            throw Klus.nee("wijs_aan");
        }
        boolean rots = Klus.natuur(start);
        int grondRond = Integer.MAX_VALUE;
        if (rots) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos v = p.relative(d, 6);
                grondRond = Math.min(grondRond, k.grond(v.getX(), v.getZ()));
            }
        }
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        Set<Long> gezien = new HashSet<>();
        rij.add(p);
        gezien.add(p.asLong());
        int max = rots ? 600 : 3000;
        while (!rij.isEmpty() && m.size() < max) {
            BlockPos c = rij.poll();
            BlockState s = k.staat(c);
            boolean mee = rots ? s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE) || s.is(BlockTags.DIRT)
                    && c.getY() > grondRond : !s.isAir() && s.getFluidState().isEmpty() && !Klus.natuur(s);
            if (!mee || rots && c.getY() <= grondRond || Math.abs(c.getX() - p.getX()) > 24 || Math.abs(c.getZ() - p.getZ()) > 24) {
                continue;
            }
            m.put(c, LUCHT);
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) {
                if (gezien.add(q.asLong())) {
                    rij.add(q.immutable());
                }
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("niets_te_slopen");
        }
        return m;
    }

    /** The mammoth pushes the snow away (layers and snow blocks on top) into little heaps around the edge. */
    static Map<BlockPos, BlockState> sneeuwschuiven(Klus k, BlockPos p) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int r = 6 + 2 * k.grootte();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                int x = p.getX() + dx, z = p.getZ() + dz;
                int top = k.top(x, z) + 1;
                for (int y = top; y >= top - 3; y--) {
                    BlockPos q = new BlockPos(x, y, z);
                    BlockState s = k.staat(q);
                    if (s.is(Blocks.SNOW) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.SNOW_BLOCK) && y >= top - 2) {
                        m.put(q, LUCHT);
                    }
                }
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("geen_sneeuw");
        }
        for (int i = 0; i < 6 + 2 * k.grootte(); i++) {
            double hoek = i * Math.PI * 2 / (6 + 2 * k.grootte());
            int x = p.getX() + (int) Math.round(Math.cos(hoek) * (r + 2)), z = p.getZ() + (int) Math.round(Math.sin(hoek) * (r + 2));
            int g = k.grond(x, z);
            m.put(new BlockPos(x, g + 1, z), Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 4 + k.r().nextInt(4)));
        }
        return m;
    }

    /** A snow cover on every free top around the spot. */
    static Map<BlockPos, BlockState> sneeuwlaag(Klus k, BlockPos p) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        int r = 5 + 2 * k.grootte();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) {
                    continue;
                }
                int x = p.getX() + dx, z = p.getZ() + dz;
                if (k.isWater(x, z)) {
                    continue;
                }
                int top = k.top(x, z) + 1;
                BlockPos q = new BlockPos(x, top, z);
                BlockState onder = k.staat(q.below());
                if (k.staat(q).isAir() && onder.isFaceSturdy(k.level(), q.below(), Direction.UP) && !onder.is(Blocks.ICE)) {
                    m.put(q, Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 1 + k.r().nextInt(2)));
                }
            }
        }
        if (m.isEmpty()) {
            throw Klus.nee("niets_te_doen");
        }
        return m;
    }

    private TerreinWerk() {
    }
}
