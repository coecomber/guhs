package nl.juiced.guhs.feature.einde2.sponsguh;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;

/**
 * einde2, slice sponsguh_wezens: a player's own Sponsguh buddy ({@link SponsguhEntity} with an owner): exactly one per
 * player, never lost (the bookkeeping: {@link Maatjes}).
 * <p>
 * hook fase B: nothing in phase A gives one in survival; the Sponsguh story calls {@link #geef} as its reward.
 */
public final class Sponsguh {
    static final String SOORT = "sponsguh";

    public static boolean heeft(ServerPlayer p) {
        return Maatjes.heeft(p, SOORT);
    }

    /** Gives this player theirs, once: when they have one already, that one comes. Null only when no entity could be made. */
    @Nullable
    public static SponsguhEntity geef(ServerPlayer p) {
        SponsguhEntity e = Maatjes.geef(p, SOORT, () -> E2SponsguhFeature.SPONSGUH.get());
        if (e != null) {
            GuhDex.zie(p, GuhVariant.SPONSGUH);
        }
        return e;
    }

    /** Brings the player's own one to them (null: they have none); it follows again. */
    @Nullable
    public static SponsguhEntity roep(ServerPlayer p) {
        return Maatjes.roep(p, SOORT, () -> E2SponsguhFeature.SPONSGUH.get());
    }

    /** The buddy stays at home where it stands (true) or follows its owner again (false). */
    public static void zetThuis(ServerPlayer p, boolean thuis) {
        Maatjes.zetThuis(p, SOORT, thuis);
    }

    private Sponsguh() {
    }
}
