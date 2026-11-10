package nl.juiced.guhs.feature.vadsrijk.ftb;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Y-FTB (user decision 2026-11-10): FTB Quests rewards can NEVER be claimed inside the Vadsrijk dimension ({@code guhs:vadsrijk});
 * everywhere else (the overworld, the Guhmensie, the Nether, the Knuffelkwartier with its home inventory...) they can. Ticking off a
 * task in the Vadsrijk is fine; only the claim waits. This holds for every reward of every chapter: in the Vadsrijk you carry the
 * Vadsrijk bag, and nothing from outside may land in it.
 * <p>
 * The FTB side is {@code mixin/FtbClaimMixin}: every claim of FTB Quests 26.1.2 ends in {@code TeamData.claimReward(ServerPlayer,
 * Reward, boolean, long)}: the claim button (ClaimRewardMessage), "claim all" (ClaimAllRewardsMessage), auto-claim when a quest
 * completes (TeamData.checkAutoCompletion), team rewards (once per online member, each with his own dimension) and an admin's
 * change-progress. The one path past it, a choice reward (ClaimChoiceRewardMessage), is covered by {@code FtbKeuzeClaimMixin}. A
 * refused claim is simply not done: the reward stays claimable and is claimed outside the Vadsrijk later (auto-claim rewards too:
 * the claim button is there for them then).
 * <p>
 * This class has no FTB types, so it loads (and is tested) without FTB Quests.
 */
public final class FtbClaimRegel {
    public static final String MELDING = "vadsrijk.ftb.claim.vadsrijk";
    /** One message per player per this many ticks ("claim all" refuses many rewards at once). */
    static final int STIL = 40;
    private static final Map<UUID, Long> GEMELD = new ConcurrentHashMap<>();
    /** Gametests / dev: how many claims were refused. */
    public static int geweigerd;

    private FtbClaimRegel() {
    }

    /** May this player claim an FTB reward where he is now? */
    public static boolean magClaimen(@Nullable ServerPlayer p) {
        return p == null || !Vadsrijk.in(p);
    }

    /**
     * Called by the mixins at the head of a claim: true = refuse it (the player hears why, at most once per {@link #STIL} ticks).
     */
    public static boolean weiger(@Nullable ServerPlayer p) {
        if (magClaimen(p)) {
            return false;
        }
        geweigerd++;
        long nu = p.level().getGameTime();
        Long vorig = GEMELD.get(p.getUUID());
        if (vorig == null || nu - vorig >= STIL || nu < vorig) {
            GEMELD.put(p.getUUID(), nu);
            p.sendSystemMessage(Component.translatable(MELDING).withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.6f, 0.8f);
        }
        return true;
    }

    /** Tests: forget the message throttle. */
    public static void vergeet() {
        GEMELD.clear();
    }
}
