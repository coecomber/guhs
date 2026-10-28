package nl.juiced.guhs.feature.droomsfeer.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.droomsfeer.DroomGeluid;
import nl.juiced.guhs.feature.droomsfeer.DroomsfeerFeature.Fade;
import nl.juiced.guhs.feature.droomsfeer.DroomsfeerFeature.Soort;

/**
 * Client side of the dream's feel: the dream sky ({@link DroomLucht}), the fade overlay and the island music.
 * <ul>
 *   <li><b>Fade:</b> starts fully covered (the server sends it before the teleport, so the jump is never seen), shows a
 *       sweet line and the island's name, then clears slowly. Into the dream a deep purple, waking up a warm dusky pink,
 *       floating to another island a short lavender blink.</li>
 *   <li><b>Music:</b> in Guhdromenland the situational music is the one of the island you are closest to
 *       ({@link DroomEilanden#dichtstbij}); it replaces the current track (no overlap: vanilla stops the old one first).</li>
 * </ul>
 */
public final class DroomsfeerClient {
    /** The fade now showing (null: none) and when it started (Util.getMillis). */
    private static Fade fade;
    private static long start;

    private DroomsfeerClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterCustomEnvironmentEffectRendererEvent event) ->
                event.registerSkyboxRenderer(Guhs.id("guhdromenland"), new DroomLucht()));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("droom_fade"), DroomsfeerClient::tekenFade));
        NeoForge.EVENT_BUS.addListener(DroomsfeerClient::muziek);
        Fade.ontvanger = f -> Minecraft.getInstance().execute(() -> {
            fade = f;
            start = Util.getMillis();
        });
    }

    // =====================================================================================================================
    // the fade

    /** {hold, clear} in milliseconds per kind: fully covered for {@code hold}, then clearing over {@code clear}. */
    private static int[] duur(Soort soort) {
        int[] d = switch (soort) {
            case INSLAPEN -> new int[]{2200, 1800};
            case WAKKER -> new int[]{1600, 1600};
            case ZWEEF -> new int[]{250, 700};
        };
        if (soort != Soort.ZWEEF && nl.juiced.guhs.feature.slaapkleding.Droompyjama.draagtSet(Minecraft.getInstance().player)) {
            float f = nl.juiced.guhs.feature.slaapkleding.Droompyjama.FADE_FACTOR;   // (the droompyjama's set bonus)
            d = new int[]{(int) (d[0] * f), (int) (d[1] * f)};
        }
        return d;
    }

    private static int kleur(Soort soort) {
        return switch (soort) {
            case INSLAPEN -> 0x24143A;      // deep night purple
            case WAKKER -> 0x4A2A44;        // dusky morning pink
            case ZWEEF -> 0x9C80D0;         // a soft lavender blink
        };
    }

    private static void tekenFade(GuiGraphicsExtractor g, DeltaTracker delta) {
        Fade f = fade;
        if (f == null) {
            return;
        }
        Soort soort = Soort.values()[Mth.clamp(f.soort(), 0, Soort.values().length - 1)];
        int[] d = duur(soort);
        long t = Util.getMillis() - start;
        if (t > d[0] + d[1]) {
            fade = null;
            return;
        }
        float alpha = t < d[0] ? 1f : 1f - (t - d[0]) / (float) d[1];
        alpha = alpha * alpha * (3f - 2f * alpha);                    // smoothstep: soft at both ends
        int a = Mth.clamp(Math.round(alpha * 255), 0, 255);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (a << 24) | kleur(soort));
        // a soft vignette of stars on the purple ones (a few dim dots, same every time)
        if (soort != Soort.ZWEEF) {
            long seed = 1808L;
            for (int i = 0; i < 40; i++) {
                seed = seed * 6364136223846793005L + 1442695040888963407L;
                int x = (int) ((seed >>> 33) % Math.max(1, g.guiWidth()));
                int y = (int) ((seed >>> 17) % Math.max(1, g.guiHeight()));
                float knipper = 0.5f + 0.5f * Mth.sin((t + i * 137) * 0.004f);
                int sa = Mth.clamp(Math.round(a * 0.55f * knipper), 0, 255);
                g.fill(x, y, x + 1, y + 1, (sa << 24) | 0xFFE6F6);
            }
        }
        // the sweet line (fades in a little after the start) and the island's name under it
        float tekstA = Math.min(alpha, Mth.clamp(t / 400f, 0f, 1f));
        int ta = Mth.clamp(Math.round(tekstA * 255), 0, 255);
        if (ta > 8) {
            Minecraft mc = Minecraft.getInstance();
            int mid = g.guiWidth() / 2, y = g.guiHeight() / 2 - 10;
            if (!f.regel().isEmpty()) {
                g.pose().pushMatrix();
                g.pose().translate(mid, y);
                g.pose().scale(1.5f, 1.5f);
                g.centeredText(mc.font, Component.translatable(f.regel()), 0, -4, (ta << 24) | 0xFFE3F4);
                g.pose().popMatrix();
            }
            if (!f.naam().isEmpty()) {
                g.centeredText(mc.font, Component.translatable(f.naam()), mid, y + (f.regel().isEmpty() ? 0 : 16), (ta << 24) | 0xD8C4FF);
            }
        }
    }

    // =====================================================================================================================
    // the island music

    private static void muziek(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.level.dimension() != DromenFeature.DIM) {
            return;
        }
        String eiland = DroomEilanden.dichtstbij(mc.player.position());
        event.setMusic(new Music(DroomGeluid.muziek(eiland).holder(), 20, 400, true));
    }
}
