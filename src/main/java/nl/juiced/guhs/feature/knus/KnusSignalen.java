package nl.juiced.guhs.feature.knus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Signals between the cozy features (2.8). {@link #zang}: guhs are singing here (the koortje, the xylofoon, the fluitje,
 * a guh doing the ZINGEN emote); the tuintjes listen ({@link #opZang}) and grow faster in that radius.
 */
public final class KnusSignalen {
    @FunctionalInterface
    public interface Zang {
        void gezongen(ServerLevel level, BlockPos pos, int radius);
    }

    private static final List<Zang> ZANG = new CopyOnWriteArrayList<>();

    /** Guhs sing at pos (within radius blocks). */
    public static void zang(ServerLevel level, BlockPos pos, int radius) {
        for (Zang z : ZANG) {
            z.gezongen(level, pos, radius);
        }
    }

    public static void opZang(Zang listener) {
        ZANG.add(listener);
    }

    private KnusSignalen() {
    }
}
