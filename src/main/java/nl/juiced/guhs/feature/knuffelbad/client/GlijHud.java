package nl.juiced.guhs.feature.knuffelbad.client;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.juiced.guhs.feature.knuffelbad.GlijPad;
import nl.juiced.guhs.feature.knuffelbad.Glijbaan;
import nl.juiced.guhs.feature.knuffelbad.ZwembandjeEntity;

/**
 * The panel on a rider's screen: which slide, the score (big), the ducks picked up, the combo, and two little bars - how
 * fast you go and how far down the slide you are. A "+points" pops up for every duck.
 */
public final class GlijHud {
    @Nullable
    private static CompoundTag data;
    private static int laatstGepakt;
    private static long popTot;
    private static int popPunten;

    private GlijHud() {
    }

    static void zet(CompoundTag d) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        if (!d.getBoolean("Actief")) {
            data = null;
            laatstGepakt = 0;
            return;
        }
        if (data != null && d.getInt("Gepakt") > laatstGepakt) {
            popTot = now + 30;
            popPunten = d.getInt("Punten");
        }
        laatstGepakt = d.getInt("Gepakt");
        data = d;
    }

    static void uit() {
        data = null;
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (data == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        Glijbaan baan = Glijbaan.byIndex(data.getInt("Baan"));
        long now = mc.level.getGameTime();
        int w = 190, x = (g.guiWidth() - w) / 2, y = 4;
        g.fill(x - 1, y - 1, x + w + 1, y + 55, baan.kleur);
        g.fill(x, y, x + w, y + 54, 0xD8201430);
        // little waves along the top
        for (int i = 0; i < w; i += 6) {
            int h = (int) (2 + Math.sin((i + now * 2) * 0.25) * 1.5);
            g.fill(x + i, y, x + i + 5, y + h, baan.licht);
        }
        g.drawCenteredString(font, baan.naam().copy().withStyle(ChatFormatting.BOLD), x + w / 2, y + 5, baan.licht);
        // the score, big
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + 16, 0);
        g.pose().scale(2f, 2f, 1f);
        String score = String.valueOf(data.getInt("Score"));
        g.drawString(font, score, -font.width(score) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
        Component eend = Component.translatable("gui.guhs.knuffelbad.hud.eendjes", data.getInt("Gepakt"), data.getInt("Totaal"));
        g.drawString(font, eend, x + 6, y + 21, 0xFFFFE27A);
        int combo = data.getInt("Combo");
        if (combo >= 2) {
            Component c = Component.translatable("gui.guhs.knuffelbad.hud.combo", combo);
            int col = (now / 3) % 2 == 0 ? 0xFFFF8FC8 : 0xFFFFFFFF;
            g.drawString(font, c, x + w - 6 - font.width(c), y + 21, col);
        }
        int best = data.getInt("Best"), record = data.getInt("Record");
        Component rec = Component.translatable("gui.guhs.knuffelbad.hud.record", best, record);
        g.drawCenteredString(font, rec, x + w / 2, y + 34, 0xFFB8A8C8);
        // speed and how far down the slide
        ZwembandjeEntity ring = KnuffelbadClient.eigenRing();
        if (ring != null) {
            GlijPad pad = ring.baan().pad();
            float pt = delta.getGameTimeDeltaPartialTick(false);
            double tau = ring.tekenTau(pt);
            float snel = (float) Mth.clamp(pad.snelheid(tau) / 1.2, 0, 1);
            float ver = (float) Mth.clamp(pad.sAt(tau) / pad.lengte, 0, 1);
            int bw = w - 12;
            g.fill(x + 6, y + 45, x + 6 + bw, y + 47, 0x60FFFFFF);
            g.fill(x + 6, y + 45, x + 6 + (int) (bw * ver), y + 47, baan.licht);
            g.fill(x + 6, y + 49, x + 6 + bw, y + 51, 0x60FFFFFF);
            int sc = snel > 0.75f ? 0xFFFF6E9A : snel > 0.45f ? 0xFFFFD27A : 0xFF7CE0FF;
            g.fill(x + 6, y + 49, x + 6 + (int) (bw * snel), y + 51, sc);
        }
        // "+10!" for a duck
        if (now < popTot) {
            float f = (popTot - now) / 30f;
            String pop = "+" + popPunten;
            g.pose().pushPose();
            g.pose().translate(g.guiWidth() / 2f + 60, y + 70 + (1 - f) * -12, 0);
            g.pose().scale(1.6f, 1.6f, 1f);
            int a = (int) (Mth.clamp(f * 2, 0, 1) * 255);
            g.drawString(font, pop, -font.width(pop) / 2, 0, (a << 24) | 0xFFE27A, true);
            g.pose().popPose();
        }
    }
}
