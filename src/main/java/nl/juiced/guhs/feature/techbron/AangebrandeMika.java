package nl.juiced.guhs.feature.techbron;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.spiesburcht.AangebrandeMikaEntity;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The per-player flag "Aangebrande Mika verslagen" (bbq2; the Gloeister tier of the Guh-technologie comes after him, and
 * other updates ask it too): {@link #verslagen(ServerPlayer)}.
 * <ul>
 *   <li>It is the key {@link #SLEUTEL} in {@link GuhQuests#saved} (survives dying), so it is per player and any number of
 *       players can have it.</li>
 *   <li>It is set for EVERY player within {@link TechbronGetallen#MIKA_BEREIK} blocks of an Aangebrande Mika at the moment
 *       it dies ({@link #dood}), whoever gave the last blow.</li>
 *   <li>Players who beat him before this flag existed get it when they log in ({@link #inloggen}), from the advancements
 *       they already have: at every login from the one that really means "beaten" ({@link #BEWIJS}), and ONCE, at the
 *       player's first login with this update, from {@link #OPGEROEPEN} (the advancement the orchestrator named; the game
 *       grants that one when the Mika is SUMMONED, so it is only trusted for what happened before this flag existed:
 *       afterwards summoning him and running away does not count).</li>
 * </ul>
 * This class listens to the game's events itself; the spiesburcht package is not touched.
 */
public final class AangebrandeMika {
    /** The key in {@code GuhQuests.saved(player)} (a boolean). */
    public static final String SLEUTEL = "guhs_bbq2_aangebrande_mika_verslagen";
    /**
     * The advancements that mean "beaten" and give the flag at any login: {@code guhs:quest/aangebrande_mika_verslagen}
     * (hidden; the Mika itself grants it within 64 blocks when he dies, since 1.0.0). Holding a gloeister is NOT in the
     * list: a friend can hand you one.
     */
    public static final List<String> BEWIJS = List.of("quest/aangebrande_mika_verslagen");
    /**
     * {@code guhs:barbecuether/aangebrande_mika}: granted to everybody within 50 blocks when the Mika is SUMMONED. It gives
     * the flag only at a player's first login with this update (marker {@link #GEKEKEN}).
     */
    public static final String OPGEROEPEN = "barbecuether/aangebrande_mika";
    /** The key in {@code GuhQuests.saved(player)}: the one-time look at {@link #OPGEROEPEN} was done. */
    public static final String GEKEKEN = "guhs_bbq2_aangebrande_mika_gekeken";

    private AangebrandeMika() {
    }

    /** Has this player beaten the Aangebrande Mika (was within 32 blocks when one died, or had beaten him before the flag existed)? */
    public static boolean verslagen(ServerPlayer player) {
        return GuhQuests.saved(player).getBooleanOr(SLEUTEL, false);
    }

    /** Gives this player the flag (it is never taken away again). */
    public static void zet(ServerPlayer player) {
        GuhQuests.saved(player).putBoolean(SLEUTEL, true);
    }

    /** An Aangebrande Mika dies: everybody within {@link TechbronGetallen#MIKA_BEREIK} blocks has beaten him. */
    @SubscribeEvent
    public static void dood(LivingDeathEvent event) {
        if (event.getEntity() instanceof AangebrandeMikaEntity mika && mika.level() instanceof ServerLevel level) {
            double r = TechbronGetallen.MIKA_BEREIK;
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator() && p.distanceToSqr(mika) <= r * r) {
                    zet(p);
                }
            }
        }
    }

    /** Login: a player who has the proof from before gets the flag. */
    @SubscribeEvent
    public static void inloggen(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            vulAan(player);
        }
    }

    /**
     * Gives the flag to a player who has the proof from before (see the class text); returns whether they have the flag now.
     * Safe to call at any time: the look at {@link #OPGEROEPEN} happens only the first time.
     */
    public static boolean vulAan(ServerPlayer player) {
        boolean eersteKeer = !GuhQuests.saved(player).getBooleanOr(GEKEKEN, false);
        GuhQuests.saved(player).putBoolean(GEKEKEN, true);
        if (!verslagen(player) && (BEWIJS.stream().anyMatch(naam -> GidsFeature.heeft(player, naam))
                || eersteKeer && GidsFeature.heeft(player, OPGEROEPEN))) {
            zet(player);
        }
        return verslagen(player);
    }
}
