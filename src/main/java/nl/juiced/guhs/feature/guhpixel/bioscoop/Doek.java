package nl.juiced.guhs.feature.guhpixel.bioscoop;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The cinema screen a projector shines on: a full rectangle of Bioscoopdoek blocks, at most {@link #MAX_B} wide and
 * {@link #MAX_H} high, that faces the projector.
 *
 * @param hoek    the block in the lower left corner (as the audience sees it)
 * @param breedte blocks wide (1..7)
 * @param hoogte  blocks high (1..4)
 * @param kant    the side the picture is on (towards the audience)
 */
public record Doek(BlockPos hoek, int breedte, int hoogte, Direction kant) {
    public static final int MAX_B = 7, MAX_H = 4;
    /** How far in front of itself a projector looks for a screen. */
    public static final int BEREIK = 16;
    private static final int[] OPZIJ = {0, 1, -1, 2, -2, 3, -3}, OMHOOG = {0, 1, 2, 3, -1, -2};

    /** "Right" as the audience sees it. */
    public Direction rechts() {
        return kant.getCounterClockWise();
    }

    public BlockPos blok(int a, int b) {
        return hoek.relative(rechts(), a).above(b);
    }

    /** The middle of the picture (on the cloth). */
    public Vec3 midden() {
        Direction r = rechts();
        double dx = r.getStepX() * (breedte - 1) / 2.0, dz = r.getStepZ() * (breedte - 1) / 2.0;
        return new Vec3(hoek.getX() + 0.5 + dx - kant.getStepX() * 0.375, hoek.getY() + hoogte / 2.0, hoek.getZ() + 0.5 + dz - kant.getStepZ() * 0.375);
    }

    public boolean bevat(BlockPos pos) {
        for (int a = 0; a < breedte; a++) {
            for (int b = 0; b < hoogte; b++) {
                if (blok(a, b).equals(pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Is every block of the rectangle still screen cloth facing the right way? */
    public boolean heel(BlockGetter level) {
        for (int a = 0; a < breedte; a++) {
            for (int b = 0; b < hoogte; b++) {
                if (!isDoek(level.getBlockState(blok(a, b)), kant)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isDoek(BlockState state, Direction kant) {
        return state.getBlock() instanceof DoekBlock && state.getValue(DoekBlock.FACING) == kant;
    }

    /**
     * The screen in front of a projector at {@code pos} that looks towards {@code kijk}: the first cloth block that faces
     * the projector within {@link #BEREIK} blocks (up to 3 blocks to the side, 3 up, 2 down), and around it the biggest
     * full rectangle (at most 7 x 4). Null: no screen.
     */
    @Nullable
    public static Doek zoek(BlockGetter level, BlockPos pos, Direction kijk) {
        Direction kant = kijk.getOpposite();
        Direction rechts = kant.getCounterClockWise();
        for (int d = 2; d <= BEREIK; d++) {
            BlockPos p = pos.relative(kijk, d);
            for (int dy : OMHOOG) {
                for (int opzij : OPZIJ) {
                    BlockPos q = p.relative(rechts, opzij).above(dy);
                    if (isDoek(level.getBlockState(q), kant)) {
                        return rond(level, q, kant);
                    }
                }
            }
        }
        return null;
    }

    /** The biggest full rectangle of cloth (same side) round this cloth block. */
    public static Doek rond(BlockGetter level, BlockPos raak, Direction kant) {
        Direction rechts = kant.getCounterClockWise();
        // the connected cloth in the plane, as (a, b) offsets from the block that was hit
        Set<Long> veld = new HashSet<>();
        ArrayDeque<int[]> rij = new ArrayDeque<>();
        rij.add(new int[]{0, 0});
        veld.add(sleutel(0, 0));
        while (!rij.isEmpty()) {
            int[] c = rij.poll();
            for (int[] s : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int a = c[0] + s[0], b = c[1] + s[1];
                if (Math.abs(a) >= MAX_B || Math.abs(b) >= MAX_H || veld.contains(sleutel(a, b))) {
                    continue;
                }
                if (isDoek(level.getBlockState(raak.relative(rechts, a).above(b)), kant)) {
                    veld.add(sleutel(a, b));
                    rij.add(new int[]{a, b});
                }
            }
        }
        int besteA = 0, besteB = 0, besteW = 1, besteH = 1;
        for (int a0 = -(MAX_B - 1); a0 <= 0; a0++) {
            for (int b0 = -(MAX_H - 1); b0 <= 0; b0++) {
                for (int w = MAX_B; w >= 1 - a0; w--) {
                    for (int h = MAX_H; h >= 1 - b0; h--) {
                        if (beter(w, h, besteW, besteH) && vol(veld, a0, b0, w, h)) {
                            besteA = a0;
                            besteB = b0;
                            besteW = w;
                            besteH = h;
                        }
                    }
                }
            }
        }
        return new Doek(raak.relative(rechts, besteA).above(besteB), besteW, besteH, kant);
    }

    /** Bigger pictures first: what counts is how big the 7:4 film can be drawn, then the area. */
    private static boolean beter(int w, int h, int bw, int bh) {
        double s = Math.min(w / (double) MAX_B, h / (double) MAX_H), bs = Math.min(bw / (double) MAX_B, bh / (double) MAX_H);
        if (s != bs) {
            return s > bs;
        }
        return w * h > bw * bh;
    }

    private static boolean vol(Set<Long> veld, int a0, int b0, int w, int h) {
        for (int a = a0; a < a0 + w; a++) {
            for (int b = b0; b < b0 + h; b++) {
                if (!veld.contains(sleutel(a, b))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static long sleutel(int a, int b) {
        return ((long) (a + 64) << 8) | (b + 64);
    }
}
