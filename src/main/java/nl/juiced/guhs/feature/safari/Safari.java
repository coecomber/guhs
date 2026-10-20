package nl.juiced.guhs.feature.safari;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.Guhs;

/**
 * Op Safari! (1.6.0): the numbers of Safaripark Vadse Bergen (SAFARI_CONTRACT.md). The dimension {@code guhs:safaripark} holds
 * one stamped area of {@link #MAAT} x {@link #MAAT} blocks from (0, 0, 0): {@link #TEGELS} x {@link #TEGELS} tiles of
 * {@link #TEGEL} blocks, {@link #HOOGTE} high. The park itself (natural outer fence) lies roughly in [128, 640); the ring
 * around it is decor to the horizon. Water stands at {@link #WATER}.
 */
public final class Safari {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("safaripark"));
    public static final int TEGELS = 6, TEGEL = 128, MAAT = TEGELS * TEGEL, HOOGTE = 160, WATER = 62;
    /** The park inside the decor ring (the design maps' 0..512 is world +128). */
    public static final int PARK_MIN = 128, PARK_MAX = 640;

    public static boolean echt(@Nullable Level level) {
        return level != null && level.dimension() == DIM;
    }

    public static boolean in(@Nullable Entity e) {
        return e != null && echt(e.level());
    }

    /** The whole safari dimension is the park (protected). */
    public static boolean in(Level level, BlockPos pos) {
        return echt(level);
    }

    @Nullable
    public static ServerLevel level(@Nullable MinecraftServer server) {
        return server == null ? null : server.getLevel(DIM);
    }

    /** Inside the stamped area? */
    public static boolean binnen(int x, int z) {
        return x >= 0 && z >= 0 && x < MAAT && z < MAAT;
    }

    private Safari() {
    }
}
