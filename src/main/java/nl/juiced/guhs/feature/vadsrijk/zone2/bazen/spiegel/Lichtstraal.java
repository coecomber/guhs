package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A light beam on the block grid (one horizontal layer): from the cell next to a source it goes straight on through air (and
 * anything without collision), turns at mirrors ({@link SpiegelBlok}: "/" swaps north/east and south/west, "\" swaps north/west and
 * south/east, flat lets it pass) and stops at the first other block. Ending on a crystal lock = {@link Pad#slot}. Pure on a
 * {@link BlockGetter}, so the gametests can check it.
 */
public final class Lichtstraal {
    public static final int MAX = 80;

    /** The cells the beam passes (in order), where it stopped (the blocking block, or null if it ran out) and whether that's a lock. */
    public record Pad(List<BlockPos> cellen, @Nullable BlockPos eind, boolean slot) {
    }

    private Lichtstraal() {
    }

    public static int dx(int r) {
        return r == 1 ? 1 : r == 3 ? -1 : 0;
    }

    public static int dz(int r) {
        return r == 2 ? 1 : r == 0 ? -1 : 0;
    }

    /** The new direction after a mirror of this stand (0 "/", 1 "\", 2 flat). */
    public static int kaats(int richting, int stand) {
        return switch (stand) {
            case 0 -> richting ^ 1;
            case 1 -> 3 - richting;
            default -> richting;
        };
    }

    /** Traces from the source at {@code bron} shining in {@code richting} (0 N, 1 E, 2 S, 3 W). */
    public static Pad volg(BlockGetter level, BlockPos bron, int richting) {
        List<BlockPos> cellen = new ArrayList<>();
        BlockPos p = bron;
        int r = richting;
        for (int i = 0; i < MAX; i++) {
            p = p.offset(dx(r), 0, dz(r));
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof SpiegelBlok) {
                cellen.add(p);
                r = kaats(r, s.getValue(SpiegelBlok.STAND));
                continue;
            }
            if (s.getBlock() instanceof KristalBlokken.Slot) {
                return new Pad(cellen, p, true);
            }
            if (s.isAir() || s.getCollisionShape(level, p).isEmpty()) {
                cellen.add(p);
                continue;
            }
            return new Pad(cellen, p, false);
        }
        return new Pad(cellen, null, false);
    }

    private static final DustParticleOptions LICHT = new DustParticleOptions(0xFFF4FC, 0.9f);

    /** The beam's look (particles, a dot every half block). */
    public static void toon(ServerLevel sl, Pad pad, DustParticleOptions stof) {
        for (BlockPos c : pad.cellen()) {
            sl.sendParticles(stof, c.getX() + 0.5, c.getY() + 0.55, c.getZ() + 0.5, 1, 0.15, 0.03, 0.15, 0);
        }
    }

    public static void toon(ServerLevel sl, Pad pad) {
        toon(sl, pad, LICHT);
    }
}
