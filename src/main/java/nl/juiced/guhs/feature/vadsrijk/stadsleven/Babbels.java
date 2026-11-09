package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * U-LEVEN-STAD: the speech bubbles of town guhs (Dutch, lang {@code vadsrijk.stadsleven.<pool>.<n>}, written by
 * tools/features/vadsrijk_u_leven_stad.py: keep the counts in sync). Cosy chit-chat, "njeg!", ~10 lines of gossip per zone (the
 * dragon, the Keizer rumours, the Knuffeltij, Moeder Wolkwal, the caravan...), children's shouts, the cart's call.
 * Throttled: a bubble only when a player is within {@link #HOOR} blocks, at most {@link #MAX_TEGELIJK} at once per town.
 */
final class Babbels {
    static final int NJEG = 6, KLETS = 14, ROD = 10, KIND = 8, IJS = 3, SOEP = 3, MUZIEK = 4, PICKNICK = 5, WANDEL = 5, KAT = 3, SPEEL = 6;
    static final double HOOR = 20;
    static final int MAX_TEGELIJK = 3;

    private Babbels() {
    }

    static MutableComponent regel(String pool, int n, RandomSource r) {
        return Component.translatable("vadsrijk.stadsleven." + pool + "." + r.nextInt(n));
    }

    /** Terrace/picnic talk: gossip of this zone, cosy chit-chat or a njeg. */
    static MutableComponent kletsregel(Steden.Stad stad, RandomSource r) {
        int k = r.nextInt(20);
        if (k < 9) {
            return regel("roddel.z" + Math.max(1, Math.min(6, stad.zone())), ROD, r);
        }
        if (k < 16) {
            return regel("klets", KLETS, r);
        }
        return regel("njeg", NJEG, r);
    }

    /** The bubble look: white text on the name tag background, between little quote marks. */
    static Component bubbel(MutableComponent tekst) {
        return Component.literal("“").append(tekst).append("”").withStyle(ChatFormatting.WHITE);
    }

    /** Is a player (with Stadsleven on) close enough to read it? */
    /**
     * X-INT: gametests only (null = the real players). The gametest server never removes the mock players other tests made
     * (GuhMockPlayer stays in the player list at its test's origin), so one of them could "hear" a test town 13 blocks away and
     * keep the playground's children up at night (order-dependent StadslevenGameTests.speeltuin).
     */
    static Boolean testHoort;

    static boolean iemandHoort(ServerLevel sl, Vec3 p) {
        if (testHoort != null) {
            return testHoort;
        }
        for (ServerPlayer sp : sl.players()) {
            if (StadsLeven.ziet(sp) && sp.distanceToSqr(p) < HOOR * HOOR) {
                return true;
            }
        }
        return false;
    }

    /** Let one of these guhs say something (if allowed now); returns true when a bubble appeared. */
    static boolean praat(ServerLevel sl, Groep g, List<LevenGuh> sprekers, MutableComponent tekst, int ticks) {
        if (g.t < g.volgendeBubbel || sprekers.isEmpty()) {
            return false;
        }
        g.volgendeBubbel = g.t + 70 + sl.getRandom().nextInt(110);
        LevenGuh s = sprekers.get(sl.getRandom().nextInt(sprekers.size()));
        if (s.praat() || !iemandHoort(sl, s.position()) || StadsLeven.bubbelsIn(g.stad) >= MAX_TEGELIJK) {
            return false;
        }
        s.zeg(bubbel(tekst), ticks);
        return true;
    }
}
