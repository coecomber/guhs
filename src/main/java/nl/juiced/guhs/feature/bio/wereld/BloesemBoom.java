package nl.juiced.guhs.feature.bio.wereld;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bio.Bio;

/**
 * biomes3 wereld, the Bloesemmeertje: the guhbloesem tree of the lake, in four sizes ({@link MeerTerrein.Boom#maat}):
 * 0 small, 1 middle, 2 large, 3 the rare giant. A broad, flat, ragged crown of a few overlapping blobs (cut off flat
 * underneath, so you look in under it); a trunk that can lean out over the water, the crown hanging further out still;
 * and a FEW loose hanging strands of blossom at the crown's edge (one edge block in ten, one to three long) instead of a
 * curtain. An island's big tree ({@link MeerTerrein.Boom#vast}) also keeps a wing of crown back over its own foot, so
 * what stands under it (the picnic) is in its shade while most of it hangs over the water.
 * <p>
 * The shape is a pure function of the {@link MeerTerrein.Boom} ({@link #cellen}): tests and the petals on the water read
 * it without a world. Leaves get their real distance to the wood, so they behave like a grown tree's (they stay while
 * the trunk stands and decay when it is felled); a leaf further than 6 from any wood is persistent.
 */
public final class BloesemBoom {
    static final double[] STRAAL = {2.4, 3.2, 4.2, 5.6};
    static final int[] HOOG = {3, 4, 6, 8};
    /** Cell values of {@link #cellen}: wood along y / x / z; a leaf is BLAD + its distance to wood (1-6), BLAD + 7 = persistent. */
    public static final int STAM_Y = 1, STAM_X = 2, STAM_Z = 3, BLAD = 10;

    public static int hoogte(MeerTerrein.Boom b) {
        return HOOG[b.maat()] + (int) ((b.zaad() >>> 9) & 1);
    }

    private static boolean schuif(MeerTerrein.Boom b, int i) {
        return (b.leunX() != 0 || b.leunZ() != 0) && i >= 2 && (b.maat() == 3 ? i % 2 == 1 : i % 2 == 0);
    }

    /** The crown from above: {middle x, middle z, radius}. */
    public static double[] kroon(MeerTerrein.Boom b) {
        int n = 0, h = hoogte(b);
        for (int i = 0; i < h; i++) {
            n += schuif(b, i) ? 1 : 0;
        }
        double r = STRAAL[b.maat()];
        return new double[]{b.x() + b.leunX() * (n + r * 0.2), b.z() + b.leunZ() * (n + r * 0.2), r};
    }

    /** Every block of the tree with its foot's lowest log at y: position ({@link BlockPos#asLong}) -> kind. */
    public static Map<Long, Integer> cellen(MeerTerrein.Boom b, int y) {
        Random rnd = new Random(b.zaad());
        Map<Long, Integer> uit = new LinkedHashMap<>();
        int maat = b.maat(), h = hoogte(b), lx = b.leunX(), lz = b.leunZ();
        double r = STRAAL[maat];
        boolean leunt = lx != 0 || lz != 0;
        int cx = b.x(), cz = b.z();
        List<long[]> stam = new ArrayList<>();
        if (maat == 3) {
            // a thick foot
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int n = 1 + rnd.nextInt(3);
                for (int i = 0; i < n; i++) {
                    stam.add(new long[]{cx + d.getStepX(), y + i, cz + d.getStepZ()});
                }
            }
        }
        for (int i = 0; i < h; i++) {
            if (schuif(b, i)) {
                stam.add(new long[]{cx, y + i, cz});
                cx += lx;
                cz += lz;
            }
            stam.add(new long[]{cx, y + i, cz});
            if (maat == 3 && i < h - 2) {
                stam.add(new long[]{cx - (lx != 0 ? lx : 1), y + i, cz - (lx != 0 ? 0 : lz)});
            }
        }
        int ty = y + h;
        stam.add(new long[]{cx, ty, cz});
        double ccx = cx + lx * r * 0.2, ccz = cz + lz * r * 0.2;
        // the blobs of the crown: {x, y, z, radius, half height}
        List<double[]> bollen = new ArrayList<>();
        bollen.add(new double[]{ccx, ty, ccz, r, r * 0.5});
        for (int i = 0; i < 1 + Math.min(maat, 2); i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            bollen.add(new double[]{ccx + Math.cos(a) * r * 0.5, ty - rnd.nextInt(2), ccz + Math.sin(a) * r * 0.5, r * 0.6, r * 0.42});
        }
        if (leunt) {
            bollen.add(new double[]{ccx + lx * r * 0.65, ty - 1 - (maat >= 2 ? 1 : 0), ccz + lz * r * 0.65, r * 0.66, r * 0.4});
        }
        if (b.vast()) {
            // back over the foot
            bollen.add(new double[]{b.x() - lx, ty - 1, b.z() - lz, r * 0.6, r * 0.4});
            bollen.add(new double[]{(b.x() + ccx) / 2, ty - 1, (b.z() + ccz) / 2, r * 0.62, r * 0.42});
        }
        int vloer = ty - 2 - (maat >= 2 ? 1 : 0);
        for (double[] bol : bollen) {
            for (int xx = (int) Math.floor(bol[0] - bol[3]); xx <= Math.ceil(bol[0] + bol[3]); xx++) {
                for (int yy = Math.max(vloer, (int) Math.floor(bol[1] - bol[4])); yy <= Math.ceil(bol[1] + bol[4]); yy++) {
                    for (int zz = (int) Math.floor(bol[2] - bol[3]); zz <= Math.ceil(bol[2] + bol[3]); zz++) {
                        double dx = (xx - bol[0]) / bol[3], dy = (yy - bol[1]) / bol[4], dz = (zz - bol[2]) / bol[3], dd = dx * dx + dy * dy + dz * dz;
                        if (dd <= 1 && !(dd > 0.78 && rnd.nextFloat() < 0.35f)) {
                            uit.put(BlockPos.asLong(xx, yy, zz), BLAD);
                        }
                    }
                }
            }
        }
        // a few loose hanging strands at the edge
        int extra = maat == 3 ? 2 : maat == 2 ? 1 : 0;
        float kans = maat == 3 ? 0.12f : 0.10f;
        List<long[]> slierten = new ArrayList<>();
        for (long pos : uit.keySet()) {
            int xx = BlockPos.getX(pos), yy = BlockPos.getY(pos), zz = BlockPos.getZ(pos);
            if (!uit.containsKey(BlockPos.asLong(xx, yy - 1, zz)) && Math.hypot(xx - ccx, zz - ccz) > r * 0.5 && rnd.nextFloat() < kans) {
                int n = 1 + (extra == 0 ? 0 : rnd.nextInt(extra + 1));
                for (int k = 1; k <= n && yy - k >= y + 2; k++) {
                    slierten.add(new long[]{xx, yy - k, zz});
                }
            }
        }
        for (long[] s : slierten) {
            uit.putIfAbsent(BlockPos.asLong((int) s[0], (int) s[1], (int) s[2]), BLAD);
        }
        // branches inside the crown (wood only where leaves were), so the leaves are near wood
        if (maat >= 2) {
            int lang = maat == 3 ? 3 : 2;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                for (int i = 1; i <= lang; i++) {
                    long pos = BlockPos.asLong(cx + d.getStepX() * i, ty - 1, cz + d.getStepZ() * i);
                    if (uit.containsKey(pos)) {
                        uit.put(pos, d.getAxis() == Direction.Axis.X ? STAM_X : STAM_Z);
                    }
                }
            }
            if (b.vast()) {
                // one long bough back towards the foot
                for (int i = 1; i <= 4; i++) {
                    long pos = BlockPos.asLong(cx - lx * i, ty - 1, cz - lz * i);
                    if (uit.containsKey(pos)) {
                        uit.put(pos, lx != 0 ? STAM_X : STAM_Z);
                    }
                }
            }
        }
        // and a knot of wood in the heart of every blob that is far from the trunk
        for (int i = 1; i < bollen.size(); i++) {
            double[] bol = bollen.get(i);
            long pos = BlockPos.asLong((int) Math.round(bol[0]), (int) Math.round(bol[1]), (int) Math.round(bol[2]));
            if (uit.containsKey(pos) && Math.hypot(bol[0] - cx, bol[2] - cz) > 2.5) {
                uit.put(pos, lx != 0 ? STAM_X : STAM_Z);
            }
        }
        for (long[] s : stam) {
            uit.put(BlockPos.asLong((int) s[0], (int) s[1], (int) s[2]), STAM_Y);
        }
        // the distance of every leaf to wood
        ArrayDeque<Long> rij = new ArrayDeque<>();
        for (Map.Entry<Long, Integer> e : uit.entrySet()) {
            if (e.getValue() < BLAD) {
                rij.add(e.getKey());
            } else {
                e.setValue(BLAD + 7);
            }
        }
        while (!rij.isEmpty()) {
            long pos = rij.poll();
            int v = uit.get(pos), afstand = v < BLAD ? 0 : v - BLAD;
            if (afstand >= 6) {
                continue;
            }
            for (Direction d : Direction.values()) {
                long buur = BlockPos.asLong(BlockPos.getX(pos) + d.getStepX(), BlockPos.getY(pos) + d.getStepY(), BlockPos.getZ(pos) + d.getStepZ());
                Integer bv = uit.get(buur);
                if (bv != null && bv >= BLAD && bv - BLAD > afstand + 1) {
                    uit.put(buur, BLAD + afstand + 1);
                    rij.add(buur);
                }
            }
        }
        return uit;
    }

    /** Builds the tree with its lowest log at y; leaves only go into air, wood replaces air, leaves and plants. Returns the blocks set. */
    public static int bouw(WorldGenLevel level, MeerTerrein.Boom b, int y) {
        BlockState stam = Bio.blok("guhbloesem_log", Blocks.CHERRY_LOG).defaultBlockState();
        BlockState blad = Bio.blok("guhbloesem_leaves", Blocks.CHERRY_LEAVES).defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int gezet = 0;
        for (Map.Entry<Long, Integer> e : cellen(b, y).entrySet()) {
            p.set(e.getKey());
            int v = e.getValue();
            BlockState er = level.getBlockState(p);
            if (v < BLAD) {
                if (er.isAir() || er.getBlock() instanceof LeavesBlock || er.canBeReplaced() && er.getFluidState().isEmpty()) {
                    BlockState s = stam;
                    if (s.hasProperty(RotatedPillarBlock.AXIS)) {
                        s = s.setValue(RotatedPillarBlock.AXIS, v == STAM_X ? Direction.Axis.X : v == STAM_Z ? Direction.Axis.Z : Direction.Axis.Y);
                    }
                    level.setBlock(p, s, 2);
                    gezet++;
                }
            } else if (er.isAir()) {
                BlockState s = blad;
                int afstand = v - BLAD;
                if (s.hasProperty(LeavesBlock.DISTANCE)) {
                    s = s.setValue(LeavesBlock.DISTANCE, Math.min(7, afstand));
                }
                if (afstand >= 7 && s.hasProperty(LeavesBlock.PERSISTENT)) {
                    s = s.setValue(LeavesBlock.PERSISTENT, true);
                }
                level.setBlock(p, s, 2);
                gezet++;
            }
        }
        return gezet;
    }

    private BloesemBoom() {
    }
}
