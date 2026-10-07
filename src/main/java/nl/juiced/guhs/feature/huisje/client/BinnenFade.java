package nl.juiced.guhs.feature.huisje.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 1.3.2: the short fade when you step through the door of a Guhhuisje (in or out): the screen goes black with a pink glow,
 * stays so while the server moves you, and clears again. Started by {@code guhs:huisje_binnen_fx}.
 */
public final class BinnenFade {
    /** Ticks: fading in, staying dark, fading out. */
    private static final int IN = 8, DONKER = 8, UIT = 12;
    private static int ticks = -1;

    private BinnenFade() {
    }

    static void start() {
        ticks = 0;
    }

    static void tick() {
        if (ticks >= 0 && ++ticks > IN + DONKER + UIT) {
            ticks = -1;
        }
    }

    static void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (ticks < 0) {
            return;
        }
        float t = ticks + delta.getGameTimeDeltaPartialTick(false);
        float a = t < IN ? t / IN : t < IN + DONKER ? 1f : Math.max(0f, 1f - (t - IN - DONKER) / UIT);
        int alfa = Math.max(0, Math.min(255, Math.round(a * 255)));
        if (alfa <= 0) {
            return;
        }
        // black at the top, a deep pink glow at the bottom
        g.fillGradient(0, 0, g.guiWidth(), g.guiHeight(), alfa << 24 | 0x12060C, alfa << 24 | 0x7A2848);
    }
}
