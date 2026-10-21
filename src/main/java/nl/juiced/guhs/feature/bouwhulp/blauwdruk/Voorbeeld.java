package nl.juiced.guhs.feature.bouwhulp.blauwdruk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;

/**
 * The little picture of a blueprint (design 2.6: "elke knop toont een preview-render"): an isometric view from the
 * south-east, each block a small cube in its map colour (top light, the sides darker), fitted into N × N pixels. Made on
 * the server once (when a blueprint is saved), sent as ARGB ints, drawn by the client pixel by pixel.
 */
public final class Voorbeeld {
    public static final int N = 48;

    public static int[] maak(Ontwerp o) {
        int[] uit = new int[N * N];
        Vec3i g = o.grootte();
        if (o.isLeeg()) {
            return uit;
        }
        // each block: a 4 px wide cube; screen x = (x - z) * 2, screen y = (x + z) - 2y (plus offsets)
        int bw = (g.getX() + g.getZ()) * 2 + 4, bh = (g.getX() + g.getZ()) + 2 * g.getY() + 4;
        int[] groot = new int[bw * bh];
        List<Map.Entry<BlockPos, BlockState>> blokken = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> e : o.blokken().entrySet()) {
            if (!e.getValue().isAir() && kleur(e.getValue()) != 0) {
                blokken.add(e);
            }
        }
        // far first: small x + z, low y
        blokken.sort((a, b) -> Integer.compare(a.getKey().getX() + a.getKey().getZ() + a.getKey().getY(),
                b.getKey().getX() + b.getKey().getZ() + b.getKey().getY()));
        int ox = g.getZ() * 2, oy = 2 * g.getY();
        for (Map.Entry<BlockPos, BlockState> e : blokken) {
            BlockPos p = e.getKey();
            int c = kleur(e.getValue());
            int sx = (p.getX() - p.getZ()) * 2 + ox, sy = p.getX() + p.getZ() - 2 * p.getY() + oy;
            int top = tint(c, 1.15), links = tint(c, 0.85), rechts = tint(c, 0.65);
            for (int dy = 0; dy < 4; dy++) {
                for (int dx = 0; dx < 4; dx++) {
                    int px = sx + dx, py = sy + dy;
                    if (px < 0 || py < 0 || px >= bw || py >= bh) {
                        continue;
                    }
                    groot[py * bw + px] = dy < 2 ? top : dx < 2 ? links : rechts;
                }
            }
        }
        // fit (keep the aspect), centred
        double f = Math.min(N / (double) bw, N / (double) bh);
        int w = Math.max(1, (int) Math.round(bw * f)), h = Math.max(1, (int) Math.round(bh * f));
        int x0 = (N - w) / 2, y0 = (N - h) / 2;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sx = Math.min(bw - 1, (int) (x / f)), sy = Math.min(bh - 1, (int) (y / f));
                uit[(y0 + y) * N + x0 + x] = groot[sy * bw + sx];
            }
        }
        return uit;
    }

    private static int kleur(BlockState s) {
        MapColor m;
        try {
            m = s.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException e) {
            return 0xFF888888;
        }
        if (m == null || m == MapColor.NONE) {
            return s.getFluidState().isEmpty() ? 0 : 0xFF4060E0;
        }
        return 0xFF000000 | m.col;
    }

    private static int tint(int c, double f) {
        int r = Math.min(255, (int) ((c >> 16 & 255) * f)), g = Math.min(255, (int) ((c >> 8 & 255) * f)), b = Math.min(255, (int) ((c & 255) * f));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private Voorbeeld() {
    }
}
