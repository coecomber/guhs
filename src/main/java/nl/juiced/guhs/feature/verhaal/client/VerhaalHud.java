package nl.juiced.guhs.feature.verhaal.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.juiced.guhs.client.GuhsClientConfig;
import nl.juiced.guhs.feature.verhaal.VerhaalSync;

/**
 * bbq2 (verhaal engine): what the engine draws over the game:
 * <ul>
 *   <li>{@code guhs:verhaal_cutscene}: the black bars of a cutscene, its subtitles in the lower bar and its fades to black
 *       (every other HUD layer is off while a scene plays, see VerhaalClient);</li>
 *   <li>{@code guhs:verhaal_doel}: the objective line (CONTRACT_130 §6.2.4): small, top left, the short text of the
 *       questline the player follows ({@code gui.guhs.verhalen.<id>.kort.<sleutel>}, else its {@code nu} text). The switch
 *       is in the Guhdex tab Verhalen (client config objectiveLine).</li>
 * </ul>
 */
public final class VerhaalHud {
    private VerhaalHud() {
    }

    static void cutscene(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!CutsceneSpeler.actief()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float pt = delta.getGameTimeDeltaPartialTick(false);
        int sw = g.guiWidth(), sh = g.guiHeight();
        float zwart = CutsceneSpeler.zwart(pt);
        if (zwart > 0.01f) {
            g.fill(0, 0, sw, sh, ((int) (zwart * 255) << 24));
        }
        int balk = Math.round(sh * 0.13f * CutsceneSpeler.balk(pt));
        if (balk > 0) {
            g.fill(0, 0, sw, balk, 0xFF000000);
            g.fill(0, sh - balk, sw, sh, 0xFF000000);
        }
        Component tekst = CutsceneSpeler.ondertitel();
        if (tekst != null) {
            Font font = mc.font;
            List<FormattedCharSequence> regels = font.split(tekst, Math.min(sw - 40, 340));
            int vol = Math.round(sh * 0.13f);
            int y = sh - vol + Math.max(2, (vol - regels.size() * 10) / 2);
            for (FormattedCharSequence r : regels) {
                g.centeredText(font, r, sw / 2, y, 0xFFFFFFFF);
                y += 10;
            }
        }
    }

    /** The lang key of the objective of the followed line (null: nothing to show). */
    static String doelKey() {
        String volg = VerhaalSync.Client.volg();
        if (volg.isEmpty()) {
            return null;
        }
        String sleutel = VerhaalSync.Client.sleutel(volg);
        String kort = "gui.guhs.verhalen." + volg + ".kort." + sleutel;
        return I18n.exists(kort) ? kort : "gui.guhs.verhalen." + volg + ".nu." + sleutel;
    }

    static void doel(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || !GuhsClientConfig.objectiveLine() || CutsceneSpeler.actief() || VertelScherm.actief()
                || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        String key = doelKey();
        if (key == null) {
            return;
        }
        Font font = mc.font;
        Component naam = Component.translatable("gui.guhs.verhalen." + VerhaalSync.Client.volg() + ".naam");
        Component tekst = Component.literal("➜ ").withStyle(ChatFormatting.GOLD).append(Component.translatable(key).withStyle(ChatFormatting.WHITE));
        float schaal = 0.75f;
        int max = (int) (Math.min(g.guiWidth() * 0.42f, 230) / schaal);
        List<FormattedCharSequence> regels = font.split(tekst, max);
        if (regels.size() > 2) {
            regels = regels.subList(0, 2);
        }
        int breed = font.width(naam);
        for (FormattedCharSequence r : regels) {
            breed = Math.max(breed, font.width(r));
        }
        int w = Math.round(breed * schaal) + 8, h = Math.round((regels.size() * 10 + 9) * schaal) + 5;
        int x = 3, y = 3;
        g.fill(x, y, x + w, y + h, 0x70201018);
        g.fill(x, y, x + 1, y + h, 0xFFF7D27A);
        g.pose().pushMatrix();
        g.pose().translate(x + 4, y + 3);
        g.pose().scale(schaal, schaal);
        g.text(font, naam.copy().withStyle(ChatFormatting.BOLD), 0, 0, 0xFFF7D27A, false);
        int ty = 10;
        for (FormattedCharSequence r : regels) {
            g.text(font, r, 0, ty, 0xFFFFFFFF, true);
            ty += 10;
        }
        g.pose().popMatrix();
    }
}
