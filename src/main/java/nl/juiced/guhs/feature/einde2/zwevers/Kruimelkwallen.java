package nl.juiced.guhs.feature.einde2.zwevers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * einde2, slice zwevers: where the swarms of Kruimelkwallen come. By themselves only in the Kruimelwolken; another slice
 * names more areas here (the Kwallenvelden of Bikinibodem, whatever biome that town lies in).
 * Skeleton stub: the list and the question are real, the top-up that asks it comes with the zwevers slice.
 */
public final class Kruimelkwallen {
    private static final List<BiPredicate<ServerLevel, BlockPos>> OOK = new CopyOnWriteArrayList<>();

    /** Also where this rule says yes the top-up brings swarms (same caps as in the Kruimelwolken). */
    public static void ookIn(BiPredicate<ServerLevel, BlockPos> waar) {
        OOK.add(waar);
    }

    /** Does one of the registered rules name this spot? */
    public static boolean ook(ServerLevel level, BlockPos pos) {
        for (BiPredicate<ServerLevel, BlockPos> waar : OOK) {
            if (waar.test(level, pos)) {
                return true;
            }
        }
        return false;
    }

    private Kruimelkwallen() {
    }
}
