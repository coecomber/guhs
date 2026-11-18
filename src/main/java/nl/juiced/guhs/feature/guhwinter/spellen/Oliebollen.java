package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.kermiskraam.KermisKraamFeature;
import org.slf4j.Logger;

/**
 * Oliebollen bakken (Oud &amp; Nieuw): a screen game of {@value #DUUR} seconds. Scoop dough into the hot pan at the right
 * moment, turn every oliebol once its underside is golden (not pale, not burnt) and lift it out when the other side is
 * golden too. At the end: a score 0-100 and a cute line about how they turned out.
 * <ul>
 *   <li>The story calls {@link #start(ServerPlayer, Consumer)} (or with {@code airfryer = true} for Lin's scene: a little
 *   airfryer instead of the pan). The callback gets the score exactly once: 0-100, or -1 when the player closed the game
 *   right away (under {@value #MIN_SECONDEN} seconds) or left the server.</li>
 *   <li>Replayable at the oliebollen stall during Oud &amp; Nieuw ({@link #bijKraam}): a few oliebollen to eat for a good
 *   score, and {@value #EERSTE_KRAAM} pepernoten the first time you score {@value #EERSTE_DREMPEL} or more.</li>
 * </ul>
 * The game itself runs on the client (client.OliebollenScherm); the server only accepts a score the time allows: in less
 * than the whole game you can't have baked more than the time that went by.
 */
public final class Oliebollen {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String MINIGAME = "guhwinter_oliebollen";
    public static final String VLAG_KRAAM = "oliebollen_kraam";
    public static final int DUUR = 45, MIN_SECONDEN = 3, EERSTE_KRAAM = 5, EERSTE_DREMPEL = 50;

    private record Sessie(int spel, boolean airfryer, long startMs, Consumer<Integer> klaar) {
    }

    private static final Map<UUID, Sessie> SESSIES = new ConcurrentHashMap<>();
    private static final AtomicInteger TELLER = new AtomicInteger();

    private Oliebollen() {
    }

    static void register() {
        Minigames.registerGame(MINIGAME, p -> SESSIES.containsKey(p.getUUID()));
    }

    /** The story's oliebollen (in the pan). */
    public static void start(ServerPlayer player, Consumer<Integer> score) {
        start(player, false, score);
    }

    /**
     * Opens the oliebollen game for this player; score gets the result once (0-100, -1 = quit at once or left). A game
     * that was still open for this player ends first (its callback gets -1).
     */
    public static void start(ServerPlayer player, boolean airfryer, Consumer<Integer> score) {
        Sessie oud = SESSIES.remove(player.getUUID());
        if (oud != null) {
            roep(oud, -1);
        }
        int spel = TELLER.incrementAndGet();
        SESSIES.put(player.getUUID(), new Sessie(spel, airfryer, System.currentTimeMillis(), score));
        Minigames.startKeeping(player);
        PacketDistributor.sendToPlayer(player, new SpellenPayloads.OlieOpen(spel, airfryer, player.getRandom().nextLong()));
    }

    public static boolean bezig(ServerPlayer p) {
        return SESSIES.containsKey(p.getUUID());
    }

    private static void roep(Sessie s, int score) {
        try {
            s.klaar().accept(score);
        } catch (RuntimeException e) {
            LOG.warn("Guhwinter oliebollen: the score callback failed", e);
        }
    }

    /** The highest score that fits in this much time (a whole game: 100). */
    public static int maxVoorTijd(long ms) {
        if (ms >= (DUUR - 5) * 1000L) {
            return 100;
        }
        return (int) Math.min(100, Math.max(0, ms * 100 / (DUUR * 1000L) + 5));
    }

    static void klaar(ServerPlayer p, int spel, int score, boolean klaar) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s == null || s.spel() != spel) {
            return;
        }
        SESSIES.remove(p.getUUID());
        Minigames.forget(p);
        long ms = System.currentTimeMillis() - s.startMs();
        int telt = ms < MIN_SECONDEN * 1000L ? -1 : Math.max(0, Math.min(score, maxVoorTijd(ms)));
        if (telt >= 0) {
            SpellenData.get(p.level().getServer()).zet(p, SpellenData.OLIEBOLLEN, telt);
        }
        roep(s, telt);
    }

    static void vergeet(ServerPlayer p) {
        Sessie s = SESSIES.remove(p.getUUID());
        if (s != null) {
            roep(s, -1);
        }
    }

    static void vergeetAlles() {
        SESSIES.clear();
    }

    /** The oliebollen stall on the plaza: only from 26 December (Oud &amp; Nieuw). */
    public static void bijKraam(ServerPlayer p) {
        if (!SpelBrug.oudjaar(p.level().getServer())) {
            p.sendSystemMessage(Component.translatable(SpelBrug.AFGELOPEN.equals(SpelBrug.fase(p.level().getServer()))
                    ? "gui.guhs.guhwinter_spellen.voorbij" : "gui.guhs.guhwinter_spellen.olie.nog_niet").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (Minigames.busyElsewhere(p, MINIGAME)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.bezig").withStyle(ChatFormatting.GRAY));
            return;
        }
        start(p, false, score -> beloonKraam(p, score));
    }

    private static void beloonKraam(ServerPlayer p, int score) {
        if (score < 0 || p.hasDisconnected()) {
            return;
        }
        int bollen = score / 25;
        if (bollen > 0) {
            Minigames.give(p, new ItemStack(KermisKraamFeature.OLIEBOLLEN.get(), bollen));
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.olie.kraam", score, bollen).withStyle(ChatFormatting.GOLD));
        } else {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.olie.kraam_niets", score).withStyle(ChatFormatting.GRAY));
        }
        SpellenData data = SpellenData.get(p.level().getServer());
        if (score >= EERSTE_DREMPEL && !data.vlag(p.getUUID(), VLAG_KRAAM)) {
            data.zetVlag(p.getUUID(), VLAG_KRAAM, true);
            if (SpelBrug.geef(p, EERSTE_KRAAM)) {
                p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.olie.noten", EERSTE_KRAAM).withStyle(ChatFormatting.YELLOW));
            }
        }
    }
}
