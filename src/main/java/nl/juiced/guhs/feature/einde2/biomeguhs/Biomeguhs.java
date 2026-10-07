package nl.juiced.guhs.feature.einde2.biomeguhs;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.einde2.Einde2Ids;

/**
 * einde2, slice biomeguhs: the guh variant of each biome of the outer islands and the wild ones that come around players.
 * Skeleton stub: the biome table is real, no wild guh comes until the biomeguhs slice fills it in.
 */
public final class Biomeguhs {
    /** The variant that lives in this biome (null: none, e.g. the main island and every chunk made before einde2). */
    @Nullable
    public static GuhVariant variantVoor(ResourceKey<Biome> biome) {
        if (biome == Einde2Ids.GATENKAASHEUVELS) {
            return GuhVariant.KORSTGUH;
        } else if (biome == Einde2Ids.KORSTWOESTIJN) {
            return GuhVariant.DUINGUH;
        } else if (biome == Einde2Ids.KRUIMELWOLKEN) {
            return GuhVariant.KRUIMELGUH;
        } else if (biome == Einde2Ids.SCHIMMELKAASVLAKTE) {
            return GuhVariant.SCHIMMELGUH;
        } else if (biome == Einde2Ids.KRISTALKAASKLIPPEN) {
            return GuhVariant.KRISTALGUH;
        } else if (biome == Einde2Ids.KNABBELKOORBOS) {
            return GuhVariant.KOORGUH;
        }
        return null;
    }

    /** The per-player top-up: maybe a wild biome guh near this player (null: none came). */
    @Nullable
    public static GuhEntity aanvullen(ServerLevel level, ServerPlayer player, RandomSource random) {
        return null;
    }

    /** hook fase C: how much this guh counts on the Disco-dynamo (2 for a Koorguh, else 1). */
    public static int discoFactor(GuhEntity guh) {
        return guh.getVariant() == GuhVariant.KOORGUH ? 2 : 1;
    }

    private Biomeguhs() {
    }
}
