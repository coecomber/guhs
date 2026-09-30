package nl.juiced.guhs.feature.huisje;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * A kind of toy (implementations: the speelgoed feature, registered with {@link Speelgoed#registreer}; ids knabbelbal,
 * glijbaantje, tunnel, wip_schommel). Residents of a huisje sometimes play with one near their home at random.
 */
public interface Speeltje {
    String id();

    /** A toy of this kind within bereik of rond for this mob to play with, or null. */
    @Nullable
    KlusTaak zoek(ServerLevel level, Mob speler, BlockPos rond, int bereik);
}
