package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guhwinter.spellen.race.RaceSim;
import org.slf4j.Logger;

/**
 * A player's Pepernotenrace, from the arcade cabinet to the pepernoten in their pocket:
 * <ol>
 *   <li>{@link #open}: the screen opens with a fresh seed, the top 10 and your best;</li>
 *   <li>{@link #start}: the server notes the time the run begins;</li>
 *   <li>{@link #klaar}: the client sends the input of every step; the server plays it again ({@link RaceSim#speelAf}),
 *   checks that so many steps fit in the time that went by ({@link RaceSim#geloofwaardig}), and only then counts it: the
 *   top 10, your best, and real pepernoten ({@link #beloning}: one per {@value #NOTEN_PER_BELONING} picked up, at most
 *   {@value #MAX_PER_RUN} a run, plus {@value #EERSTE_BONUS} for your very first run). No dailies: every run counts the same.</li>
 * </ol>
 * While a run goes on you are in a minigame ({@link Minigames}): nothing can hurt you and you can't hurt anyone.
 */
public final class Pepernotenrace {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String MINIGAME = "guhwinter_race";
    public static final String VLAG_EERSTE = "race_eerste";
    public static final int BEREIK = 8;
    public static final int MAX_PER_RUN = 10, EERSTE_BONUS = 25, NOTEN_PER_BELONING = 10, MIN_SECONDEN = 10;

    private static final class Sessie {
        final BlockPos pos;
        long seed;
        long startMs = -1;

        Sessie(BlockPos pos, long seed) {
            this.pos = pos;
            this.seed = seed;
        }
    }

    private static final Map<UUID, Sessie> SESSIES = new ConcurrentHashMap<>();

    private Pepernotenrace() {
    }

    static void register() {
        Minigames.registerGame(MINIGAME, p -> {
            Sessie s = SESSIES.get(p.getUUID());
            return s != null && s.startMs >= 0;
        });
    }

    static void vergeet(UUID id) {
        SESSIES.remove(id);
    }

    static void vergeetAlles() {
        SESSIES.clear();
    }

    private static long nieuwSeed(ServerPlayer p) {
        return p.getRandom().nextLong() ^ System.nanoTime();
    }

    /** The cabinet's screen opens for this player. */
    public static void open(ServerPlayer p, BlockPos pos) {
        if (!SpelBrug.open(p.level().getServer())) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.voorbij").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (Minigames.busyElsewhere(p, MINIGAME)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.bezig").withStyle(ChatFormatting.GRAY));
            return;
        }
        Sessie s = new Sessie(pos, nieuwSeed(p));
        SESSIES.put(p.getUUID(), s);
        SpellenData data = SpellenData.get(p.level().getServer());
        PacketDistributor.sendToPlayer(p, new SpellenPayloads.RaceOpen(pos, s.seed, SpellenPayloads.regels(data.top(SpellenData.RACE)),
                Math.max(0, data.beste(p.getUUID(), SpellenData.RACE)), data.vlag(p.getUUID(), VLAG_EERSTE)));
    }

    static void start(ServerPlayer p, BlockPos pos, long seed) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s == null || s.seed != seed || !s.pos.equals(pos) || p.distanceToSqr(Vec3.atCenterOf(pos)) > BEREIK * BEREIK * 4) {
            return;
        }
        s.startMs = System.currentTimeMillis();
        Minigames.startKeeping(p);
    }

    static void klaar(ServerPlayer p, BlockPos pos, long seed, int stappen, byte[] invoer) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s == null || s.seed != seed || s.startMs < 0 || !s.pos.equals(pos)) {
            return;
        }
        long ms = System.currentTimeMillis() - s.startMs;
        RaceSim.Uitkomst u = RaceSim.speelAf(seed, invoer, stappen);
        s.seed = nieuwSeed(p);
        s.startMs = -1;
        Minigames.forget(p);
        SpellenData data = SpellenData.get(p.level().getServer());
        if (!RaceSim.geloofwaardig(u.stappen(), ms)) {
            LOG.warn("Guhwinter Pepernotenrace: {} sent {} steps after {} ms; not counted", p.getGameProfile().name(), u.stappen(), ms);
            PacketDistributor.sendToPlayer(p, new SpellenPayloads.RaceUitslag(pos, false, u.score(), 0, 0, false, s.seed,
                    SpellenPayloads.regels(data.top(SpellenData.RACE)), Math.max(0, data.beste(p.getUUID(), SpellenData.RACE))));
            return;
        }
        Verwerkt v = verwerk(p, u);
        PacketDistributor.sendToPlayer(p, new SpellenPayloads.RaceUitslag(pos, true, u.score(), v.gekregen(), v.plaats(), v.record(), s.seed,
                SpellenPayloads.regels(data.top(SpellenData.RACE)), Math.max(0, data.beste(p.getUUID(), SpellenData.RACE))));
    }

    static void stop(ServerPlayer p) {
        Sessie s = SESSIES.remove(p.getUUID());
        if (s != null && s.startMs >= 0) {
            Minigames.forget(p);
        }
    }

    /** The pepernoten a run gives (without the first-run bonus): one per ten picked up, at most ten; nothing for a run under ten seconds. */
    public static int beloning(RaceSim.Uitkomst u) {
        if (u.seconden() < MIN_SECONDEN) {
            return 0;
        }
        return Math.min(MAX_PER_RUN, Math.max(1, u.pepernoten() / NOTEN_PER_BELONING));
    }

    /** What a counted run did. */
    public record Verwerkt(int gekregen, int plaats, boolean record) {
    }

    /** A counted run: the board, your best, the pepernoten (also used by the dev command and the test). */
    public static Verwerkt verwerk(ServerPlayer p, RaceSim.Uitkomst u) {
        SpellenData data = SpellenData.get(p.level().getServer());
        int oudRecord = data.top(SpellenData.RACE).isEmpty() ? -1 : data.top(SpellenData.RACE).get(0).score();
        SpellenData.Inzending in = data.zet(p, SpellenData.RACE, u.score());
        int noten = beloning(u);
        if (!data.vlag(p.getUUID(), VLAG_EERSTE)) {
            data.zetVlag(p.getUUID(), VLAG_EERSTE, true);
            noten += EERSTE_BONUS;
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.race.eerste", EERSTE_BONUS).withStyle(ChatFormatting.GOLD));
        }
        if (noten > 0 && SpelBrug.geef(p, noten)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.race.noten", u.score(), noten).withStyle(ChatFormatting.YELLOW));
        }
        boolean record = in.plaats() == 1 && u.score() > oudRecord;
        if (record) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.4f);
        }
        if (in.plaats() > 0) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.race.plaats", in.plaats()).withStyle(ChatFormatting.GOLD));
        }
        HighscorebordBlock.verversAlles(p.level().getServer());
        return new Verwerkt(noten, in.plaats(), record);
    }
}
