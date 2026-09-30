package nl.juiced.guhs.feature.race.client;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.race.RaceFeature;
import nl.juiced.guhs.feature.race.RacePayloads;
import nl.juiced.guhs.feature.race.RaceRecords;

/**
 * Client side of the guhrace: the race guh (a normal guh renderer) and the see-through ghost, the Raceguh's screen, and
 * the race panel at the top of the screen (lap, race clock, lap clock, next checkpoint, the difference with your record).
 */
public final class RaceClient {
    /** The last race panel from the server, and the client time it came in (the clock keeps running from there). */
    @Nullable
    private static CompoundTag hud;
    private static long hudAt;
    private static long deltaUntil;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(RaceFeature.RACE_GUH.get(), GuhRenderer::new);
            event.registerEntityRenderer(RaceFeature.RACE_GHOST.get(), RaceGhostRenderer::new);
        });
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(Guhs.id("race_hud"), RaceClient::renderHud));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> hud = null);
    }

    public static void open(RacePayloads.Open payload) {
        Minecraft.getInstance().setScreen(new RaceScreen(payload));
    }

    public static void hud(CompoundTag data) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (data.getBoolean("Active") && data.getInt("Delta") != Integer.MIN_VALUE
                && (hud == null || hud.getInt("Delta") != data.getInt("Delta") || hud.getInt("Next") != data.getInt("Next"))) {
            deltaUntil = now + 60;
        }
        hud = data.getBoolean("Active") ? data : null;
        hudAt = now;
    }

    private static void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (hud == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        long now = mc.level.getGameTime();
        boolean racing = hud.getBoolean("Racing");
        int ticks = hud.getInt("Ticks") + (racing ? (int) Math.max(0, now - hudAt) : 0);
        int lapTicks = ticks - hud.getInt("LapStart");
        int goud = hud.contains("Goud") ? hud.getInt("Goud") : -1;
        int w = 200, x = (g.guiWidth() - w) / 2, y = 4, h = goud >= 0 ? 68 : 57;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFFF7B6CB);
        g.fill(x, y, x + w, y + h, 0xD8301A26);
        if (goud >= 0) {
            g.drawCenteredString(font, Component.translatable("gui.guhs.race.hud.goud", RaceRecords.time(goud)), x + w / 2, y + 57, 0xFFFFC940);
        }
        String baan = hud.getString("Baan");
        String niveau = hud.getString("Niveau");
        Component head = baan.isEmpty() || baan.equals("racebaan") && (niveau.isEmpty() || niveau.equals("medium"))
                ? Component.translatable("gui.guhs.race.hud.lap", hud.getInt("Lap"), hud.getInt("Laps"))
                : Component.translatable("gui.guhs.race.hud.lap.baan", Component.translatable("gui.guhs.race.baan." + baan),
                Component.translatable("gui.guhs.niveau." + niveau), hud.getInt("Lap"), hud.getInt("Laps"));
        g.drawCenteredString(font, head.copy().withStyle(ChatFormatting.BOLD), x + w / 2, y + 3, 0xFFFFD27A);
        // the big race clock
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + 14, 0);
        g.pose().scale(2f, 2f, 1f);
        String clock = RaceRecords.time(ticks);
        g.drawString(font, clock, -font.width(clock) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
        g.drawString(font, Component.translatable("gui.guhs.race.hud.this_lap", RaceRecords.time(lapTicks)), x + 6, y + 34, 0xFFD8B8C8);
        int bestLap = hud.getInt("BestLap");
        Component best = Component.translatable("gui.guhs.race.hud.best", RaceRecords.time(hud.getInt("Best")));
        g.drawString(font, best, x + w - 6 - font.width(best), y + 34, 0xFFD8B8C8);
        int next = hud.getInt("Next");
        Component nextText = next == 0 ? Component.translatable("gui.guhs.race.hud.next_finish")
                : Component.translatable("gui.guhs.race.hud.next", next, hud.getInt("Gates") - 1);
        g.drawString(font, nextText, x + 6, y + 45, 0xFFFFE6EE);
        int d = hud.getInt("Delta");
        if (d != Integer.MIN_VALUE && now < deltaUntil) {
            String text = RaceRecords.delta(d);
            g.drawString(font, text, x + w - 6 - font.width(text), y + 45, d <= 0 ? 0xFF7CFF8A : 0xFFFF6E6E);
        } else if (bestLap >= 0) {
            Component lap = Component.translatable("gui.guhs.race.hud.best_lap", RaceRecords.time(bestLap));
            g.drawString(font, lap, x + w - 6 - font.width(lap), y + 45, 0xFFB89AAA);
        }
    }

    private RaceClient() {
    }
}
