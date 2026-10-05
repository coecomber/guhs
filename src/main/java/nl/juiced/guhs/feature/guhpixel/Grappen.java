package nl.juiced.guhs.feature.guhpixel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The per-player mini questlines of the joke games. The saved step is the furthest step a player EVER reached (it only
 * grows; the step inside a replay lives in the session). The first {@link #voltooi} pays 100 muntjes (key
 * {@code grap:<id>}), gives the keepsake, unlocks the film and grants quest/&lt;id&gt;_klaar; replays pay nothing and give
 * no second keepsake. Everything is per player: any number of players can finish every game.
 */
public final class Grappen {
    public static final int BELONING = 100;
    private static final Map<String, Grap> GRAPPEN = new LinkedHashMap<>();
    private static final String DATA = "Grappen", STAP = "Stap", KLAAR = "Klaar", KEREN = "Keren";

    public static void registreer(Grap grap) {
        GRAPPEN.put(grap.id(), grap);
    }

    @Nullable
    public static Grap van(String id) {
        return GRAPPEN.get(id);
    }

    public static List<Grap> alle() {
        return List.copyOf(GRAPPEN.values());
    }

    private static CompoundTag data(ServerPlayer p, String id) {
        return PxData.sub(PxData.sub(Muntjes.data(p), DATA), id);
    }

    /** The furthest step ever reached, 0..stappen. */
    public static int stap(ServerPlayer p, String id) {
        return Muntjes.data(p).getCompoundOrEmpty(DATA).getCompoundOrEmpty(id).getIntOr(STAP, 0);
    }

    /**
     * The player is at this step now (1..stappen): shows "Stap i/n: text" and keeps the maximum; a step reached for the
     * first time grants the hidden advancement quest/&lt;id&gt;_stap_&lt;i&gt;.
     */
    public static void zetStap(ServerPlayer p, String id, int stap) {
        Grap g = GRAPPEN.get(id);
        int n = g == null ? stap : g.stappen();
        int i = Math.max(0, Math.min(stap, n));
        if (i > 0) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.guhpixel.grap.stap", i, n, Component.translatable("gui.guhs." + id + ".grap.stap." + i))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        CompoundTag d = data(p, id);
        int oud = d.getIntOr(STAP, 0);
        if (i > oud) {
            d.putInt(STAP, i);
            PxData.vuil(p.level().getServer());
            for (int k = oud + 1; k <= i; k++) {
                GuhAdvancements.grant(p, id + "_stap_" + k);
            }
        }
    }

    /**
     * The player finished the game. True the FIRST time (100 muntjes, the keepsake, the film, quest/&lt;id&gt;_klaar); always:
     * the run is counted and the punchline shows as a title.
     */
    public static boolean voltooi(ServerPlayer p, String id) {
        Grap g = GRAPPEN.get(id);
        CompoundTag d = data(p, id);
        boolean eerste = !d.getBooleanOr(KLAAR, false);
        d.putBoolean(KLAAR, true);
        d.putInt(KEREN, d.getIntOr(KEREN, 0) + 1);
        if (g != null && d.getIntOr(STAP, 0) < g.stappen()) {
            int oud = d.getIntOr(STAP, 0);
            d.putInt(STAP, g.stappen());
            for (int k = oud + 1; k <= g.stappen(); k++) {
                GuhAdvancements.grant(p, id + "_stap_" + k);
            }
        }
        PxData.vuil(p.level().getServer());
        PxGeluid.titel(p, Component.translatable("gui.guhs." + id + ".grap.clou").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable(eerste ? "gui.guhs.guhpixel.grap.klaar" : "gui.guhs.guhpixel.grap.opnieuw").withStyle(ChatFormatting.LIGHT_PURPLE), 80);
        if (eerste) {
            Muntjes.verdienEens(p, "grap:" + id, BELONING);
            if (g != null) {
                try {
                    g.aandenken().accept(p);
                } catch (RuntimeException e) {
                    LogUtils.getLogger().error("Guhpixel: the keepsake of {} failed", id, e);
                }
            }
            Films.ontgrendel(p, id);
            GuhAdvancements.grant(p, id + "_klaar");
        }
        return eerste;
    }

    public static boolean isKlaar(ServerPlayer p, String id) {
        return Muntjes.data(p).getCompoundOrEmpty(DATA).getCompoundOrEmpty(id).getBooleanOr(KLAAR, false);
    }

    /** How often the player finished it. */
    public static int keren(ServerPlayer p, String id) {
        return Muntjes.data(p).getCompoundOrEmpty(DATA).getCompoundOrEmpty(id).getIntOr(KEREN, 0);
    }

    /** (Dev) forgets this player's progress of one game (the 100 muntjes stay paid). */
    public static void reset(ServerPlayer p, String id) {
        PxData.sub(Muntjes.data(p), DATA).remove(id);
        PxData.vuil(p.level().getServer());
    }

    private Grappen() {
    }
}
