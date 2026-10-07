package nl.juiced.guhs.feature.guhpixel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;

/**
 * Guhpixel (the minigame-server parody): where am I? The void dimension {@code guhs:guhpixel} holds ONE lobby island
 * around 0,0 and, far away on a grid, the arenas of the running games ({@link Arenas}).
 * <p>
 * Fixed geometry (CONTRACT_PX section 2): the lobby template (97 x max 96 x 97) is stamped with its min corner at
 * {@link #LOBBY_MIN}, so the spawn point (feet) is (0, 100, 0) and the plaza floor is y 99. Arena cell k has its min corner
 * at (4096 + 512 * (k % 64), 64, 512 * (k / 64)). A player below y {@link #VOID_Y} is put back.
 * <p>
 * The game test server has no datapack dimensions: there {@link #level} is null and tests mark their own box as
 * "guhpixel" with {@link PxTest#gebied}, so the rules apply there and nowhere else.
 */
public final class Guhpixel {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("guhpixel"));

    /** The lobby box: min corner and size of the template's place. */
    public static final BlockPos LOBBY_MIN = new BlockPos(-48, 68, -48);
    public static final int LOBBY_BREEDTE = 97, LOBBY_HOOGTE = 96;
    /** The arena grid. */
    public static final int ARENA_X0 = 4096, ARENA_Y = 64, CEL = 512, CELLEN_PER_RIJ = 64, ARENA_MAX = 160;
    /** Below this height a player is put back on the plaza (or the arena start). */
    public static final int VOID_Y = 40;

    /** (Game tests) boxes that count as guhpixel. */
    private record TestGebied(ResourceKey<Level> dim, AABB doos) {
    }

    private static final List<TestGebied> TEST = new CopyOnWriteArrayList<>();

    /** The dimension (null on the game test server, which has no datapack dimensions). */
    @Nullable
    public static ServerLevel level(@Nullable MinecraftServer server) {
        return server == null ? null : server.getLevel(DIM);
    }

    /** In the dimension, or inside a registered test area. */
    public static boolean in(@Nullable Entity e) {
        return e != null && in(e.level(), e.blockPosition());
    }

    public static boolean in(Level level, BlockPos pos) {
        if (level.dimension() == DIM) {
            return true;
        }
        if (!TEST.isEmpty() && !level.isClientSide()) {
            for (TestGebied g : TEST) {
                if (g.dim() == level.dimension() && g.doos().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Really the dimension (not a test area). */
    public static boolean echt(Level level) {
        return level.dimension() == DIM;
    }

    /** Inside the lobby box (the real dimension only). */
    public static boolean inLobby(@Nullable Entity e) {
        return e != null && e.level().dimension() == DIM && lobbyDoos().contains(e.position());
    }

    public static boolean inLobby(Level level, BlockPos pos) {
        return level.dimension() == DIM && lobbyDoos().contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /** The lobby box (a little air above, below and beside the template counts too). */
    public static AABB lobbyDoos() {
        return new AABB(LOBBY_MIN.getX() - 4, VOID_Y, LOBBY_MIN.getZ() - 4, LOBBY_MIN.getX() + LOBBY_BREEDTE + 4,
                LOBBY_MIN.getY() + LOBBY_HOOGTE + 24, LOBBY_MIN.getZ() + LOBBY_BREEDTE + 4);
    }

    /** The min corner of arena cell k (k >= 0). */
    public static BlockPos celOorsprong(int k) {
        return new BlockPos(ARENA_X0 + CEL * (k % CELLEN_PER_RIJ), ARENA_Y, CEL * (k / CELLEN_PER_RIJ));
    }

    /** The cell whose arena space (160 x 160 x 160 from the cell's min corner) holds this position, or -1. */
    public static int celVan(BlockPos pos) {
        int dx = pos.getX() - ARENA_X0, dz = pos.getZ();
        if (dx < 0 || dz < 0 || dx >= CEL * CELLEN_PER_RIJ) {
            return -1;
        }
        if (dx % CEL >= ARENA_MAX || dz % CEL >= ARENA_MAX || pos.getY() < ARENA_Y || pos.getY() >= ARENA_Y + ARENA_MAX) {
            return -1;
        }
        return (dz / CEL) * CELLEN_PER_RIJ + dx / CEL;
    }

    /** (Tests) marks a box as guhpixel. */
    static void testGebied(ResourceKey<Level> dim, AABB doos) {
        TEST.add(new TestGebied(dim, doos));
    }

    /** (Tests) forgets a test box. */
    static void testGebiedWeg(ResourceKey<Level> dim, AABB doos) {
        TEST.removeIf(g -> g.dim() == dim && g.doos().equals(doos));
    }

    private Guhpixel() {
    }
}
