package nl.juiced.guhs.feature.ringvlucht;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * 1.4.2 (rookguh): where the Rookguh puts a player down: about {@code afstand} blocks in front of an entrance of the
 * destination, on solid ground, and only where they can WALK to that entrance (so never in a cave pocket behind the rock or
 * on a ledge over the sauce). Works the same in the Guhmensie and under the roof of the Guhbarbecuether: nothing here asks
 * for the sky or a heightmap (1.4.1's lesson of {@code ringh6.Thuis}: an unloaded column reads as the bottom of the world),
 * the chunks are loaded first ({@link #laad}).
 * <p>
 * How: a walk (breadth first) from the entrance over cells one can stand in ({@link #staan}: a solid block under it, room
 * for a player, no fluid), a step up of one block or down of at most three, at most {@link #RAND} blocks further than
 * {@code afstand} sideways and {@link #HOOGTE} up or down. Of the cells reached, the one nearest the point {@code afstand}
 * blocks out along the way out wins, preferring a cell outside the structure's own box, then any cell in front of the
 * entrance (a mountain on an island in the sauce sea: its ash plain), then the entrance itself. Cells in a fire, on magma,
 * a campfire, a cactus or leaves, and cells the caller refuses (a closed sluier), are never chosen.
 */
public final class Landing {
    /** How much further than the wanted distance the walk may look (blocks sideways), and how far up or down. */
    public static final int RAND = 10, HOOGTE = 20;
    /** At most this many cells are walked. */
    public static final int MAX_CELLEN = 40000;

    /** An entrance in the world: the cell you stand in, and the way out (one of the four directions). */
    public record Ingang(BlockPos plek, int dx, int dz) {
    }

    /**
     * The best landing cell (the block the player's feet are in) for these entrances (tried in order: the first that gives a
     * spot outside the box wins; else the best spot in front of any of them; else the first entrance that is a place to
     * stand), or null when there is none at all. {@code mag}: may the player be put down in this cell? Loads chunks.
     */
    @Nullable
    public static BlockPos zoek(ServerLevel level, List<Ingang> ingangen, int afstand, @Nullable BoundingBox doos, Predicate<BlockPos> mag) {
        BlockPos beste = null;
        double besteD = Double.MAX_VALUE;
        for (Ingang ingang : ingangen) {
            laad(level, ingang.plek(), afstand + RAND);
            if (afstand <= 0) {
                BlockPos hier = dichtbij(level, ingang.plek(), mag);
                if (hier != null) {
                    return hier;
                }
                continue;
            }
            Keuze k = loop(level, ingang, afstand, doos, mag);
            if (k.buiten != null) {
                return k.buiten;
            }
            if (k.voor != null && k.voorD < besteD) {
                beste = k.voor;
                besteD = k.voorD;
            }
        }
        if (beste != null) {
            return beste;
        }
        for (Ingang ingang : ingangen) {
            BlockPos hier = dichtbij(level, ingang.plek(), mag);
            if (hier != null) {
                return hier;
            }
        }
        return null;
    }

    private static final class Keuze {
        @Nullable
        BlockPos buiten, voor;
        double buitenD = Double.MAX_VALUE, voorD = Double.MAX_VALUE;
    }

    private static Keuze loop(ServerLevel level, Ingang ingang, int afstand, @Nullable BoundingBox doos, Predicate<BlockPos> mag) {
        Keuze k = new Keuze();
        BlockPos start = dichtbij(level, ingang.plek(), p -> true);
        if (start == null) {
            return k;
        }
        BlockPos e = ingang.plek();
        double tx = e.getX() + ingang.dx() * (double) afstand, tz = e.getZ() + ingang.dz() * (double) afstand;
        int bereik = afstand + RAND;
        Set<Long> gezien = new HashSet<>();
        ArrayDeque<BlockPos> rij = new ArrayDeque<>();
        rij.add(start);
        gezien.add(start.asLong());
        int[][] richtingen = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!rij.isEmpty() && gezien.size() < MAX_CELLEN) {
            BlockPos c = rij.poll();
            // (is this cell a candidate?)
            int vx = c.getX() - e.getX(), vz = c.getZ() - e.getZ();
            int vooruit = vx * ingang.dx() + vz * ingang.dz();
            if (vooruit >= 2 && landbaar(level, c) && mag.test(c)) {
                double d = Math.sqrt((c.getX() - tx) * (c.getX() - tx) + (c.getZ() - tz) * (c.getZ() - tz)) + 0.5 * Math.abs(c.getY() - e.getY())
                        + (ruim(level, c) ? 0 : 3);
                if (doos == null || !doos.isInside(c)) {
                    if (d < k.buitenD) {
                        k.buiten = c;
                        k.buitenD = d;
                    }
                } else if (d < k.voorD) {
                    k.voor = c;
                    k.voorD = d;
                }
            }
            for (int[] r : richtingen) {
                int nx = c.getX() + r[0], nz = c.getZ() + r[1];
                if (Math.abs(nx - e.getX()) > bereik || Math.abs(nz - e.getZ()) > bereik) {
                    continue;
                }
                BlockPos n = stap(level, c, nx, nz);
                if (n != null && Math.abs(n.getY() - e.getY()) <= HOOGTE && gezien.add(n.asLong())) {
                    rij.add(n);
                }
            }
        }
        return k;
    }

    /** From this cell one step sideways to column nx, nz: the cell you end up standing in (same height, one up, up to three down), or null. */
    @Nullable
    private static BlockPos stap(ServerLevel level, BlockPos c, int nx, int nz) {
        BlockPos gelijk = new BlockPos(nx, c.getY(), nz);
        if (staan(level, gelijk)) {
            return gelijk;
        }
        BlockPos op = gelijk.above();
        if (!level.getBlockState(c.above(2)).blocksMotion() && staan(level, op)) {
            return op;                                       // (a jump: room over your head where you stand)
        }
        if (level.getBlockState(gelijk).blocksMotion() || level.getBlockState(gelijk.above()).blocksMotion()) {
            return null;                                     // (a wall)
        }
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos neer = gelijk.below(dy);
            if (!level.getFluidState(neer.above()).isEmpty()) {
                return null;                                 // (into the sauce: no)
            }
            if (staan(level, neer)) {
                return neer;
            }
            if (level.getBlockState(neer).blocksMotion()) {
                return null;
            }
        }
        return null;
    }

    /** A place to stand at or right around this cell (the cell itself first, then within 2 blocks), or null. */
    @Nullable
    static BlockPos dichtbij(ServerLevel level, BlockPos rond, Predicate<BlockPos> mag) {
        BlockPos beste = null;
        double besteD = Double.MAX_VALUE;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos c = rond.offset(dx, dy, dz);
                    double d = dx * dx + dz * dz + 2.0 * dy * dy;
                    if (d < besteD && staan(level, c) && landbaar(level, c) && mag.test(c)) {
                        beste = c;
                        besteD = d;
                    }
                }
            }
        }
        return beste;
    }

    /**
     * Loads (the very first time: generates) the chunks within {@code straal} blocks of this spot now, so their blocks can be
     * read. Server thread (as ringh6.Thuis does it; the flight scene asks for them in the background first: {@code Vlucht.warm}).
     */
    static void laad(ServerLevel level, BlockPos rond, int straal) {
        for (int cx = (rond.getX() - straal) >> 4; cx <= (rond.getX() + straal) >> 4; cx++) {
            for (int cz = (rond.getZ() - straal) >> 4; cz <= (rond.getZ() + straal) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }

    /** Is this a cell to stand in: inside the world, a solid block under it, room for a player, no fluid? */
    public static boolean staan(ServerLevel level, BlockPos voet) {
        if (voet.getY() <= level.getMinY() || voet.getY() + 1 > level.getMaxY() || !level.isLoaded(voet)) {
            return false;
        }
        BlockState onder = level.getBlockState(voet.below());
        return onder.blocksMotion() && onder.getFluidState().isEmpty() && !level.getBlockState(voet).blocksMotion() && level.getFluidState(voet).isEmpty()
                && !level.getBlockState(voet.above()).blocksMotion() && level.getFluidState(voet.above()).isEmpty();
    }

    /** May somebody be put down here: not in a fire, a bush that pricks or a cobweb, not on magma, a campfire, a cactus or leaves. */
    public static boolean landbaar(ServerLevel level, BlockPos voet) {
        BlockState hier = level.getBlockState(voet), hoofd = level.getBlockState(voet.above()), onder = level.getBlockState(voet.below());
        if (hier.is(BlockTags.FIRE) || hoofd.is(BlockTags.FIRE) || hier.is(Blocks.SWEET_BERRY_BUSH) || hier.is(Blocks.COBWEB) || hier.is(Blocks.WITHER_ROSE)
                || hier.is(Blocks.POWDER_SNOW) || hier.is(Blocks.CACTUS)) {
            return false;
        }
        return !onder.is(BlockTags.CAMPFIRES) && !onder.is(Blocks.MAGMA_BLOCK) && !onder.is(Blocks.CACTUS) && !onder.is(BlockTags.LEAVES)
                && !onder.is(Blocks.POWDER_SNOW);
    }

    /** Is there room around this cell (at least three of its four neighbours are places to stand at about this height)? */
    private static boolean ruim(ServerLevel level, BlockPos c) {
        int n = 0;
        for (BlockPos b : new BlockPos[]{c.east(), c.west(), c.north(), c.south()}) {
            if (staan(level, b) || staan(level, b.below()) || staan(level, b.above())) {
                n++;
            }
        }
        return n >= 3;
    }

    private Landing() {
    }
}
