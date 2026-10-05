package nl.juiced.guhs.feature.wereld;

import java.util.UUID;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * bbq2 (CONTRACT_130 §6.3.4): break/place protection of the new quest buildings. THIS IS THE NO-OP STUB of the skeleton
 * (§5.1): the wereld foundation (F3) replaces it with the real thing, keeping these signatures. Until then
 * {@code feature.verhaal.Sluiers} protects its own structures itself (it asks {@link #beschermd} first, so nothing is
 * done twice once the real class is here).
 */
public final class Bescherming {
    /** Nobody breaks/places/explodes/floods inside the pieces' boxes (+ rand) of this structure. */
    public static void registreer(String structuur, int rand) {
    }

    /** Quest blocks a player may change after all. */
    public static void uitzondering(String structuur, BiPredicate<ServerPlayer, BlockPos> mag) {
    }

    public static boolean beschermd(Level level, BlockPos pos) {
        return false;
    }

    /** For machines: may something be changed here on behalf of this player? */
    public static boolean magWijzigen(ServerLevel level, BlockPos pos, @Nullable UUID namens) {
        return true;
    }

    private Bescherming() {
    }
}
