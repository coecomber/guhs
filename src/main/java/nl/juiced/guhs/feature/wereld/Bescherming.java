package nl.juiced.guhs.feature.wereld;

import java.util.UUID;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * bbq2: protection of the new quest buildings and "may this be changed here" for the machines. This is the F0 no-op stub with
 * the signatures of CONTRACT_130 6.3.4: F3 fills it in.
 */
public final class Bescherming {
    /** Nobody breaks, places, explodes or floods inside the pieces' boxes (+ rand blocks) of this structure. */
    public static void registreer(String structuur, int rand) {
    }

    /** The quest blocks of this structure a player may change after all. */
    public static void uitzondering(String structuur, BiPredicate<ServerPlayer, BlockPos> mag) {
    }

    public static boolean beschermd(Level level, BlockPos pos) {
        return false;
    }

    /** For machines (Knabbelaar, Neerzetter, Oogster): may the block here be changed on behalf of this player? */
    public static boolean magWijzigen(ServerLevel level, BlockPos pos, @Nullable UUID namens) {
        return true;
    }

    private Bescherming() {
    }
}
