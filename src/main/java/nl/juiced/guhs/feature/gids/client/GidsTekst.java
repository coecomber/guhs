package nl.juiced.guhs.feature.gids.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Text helpers of the gids screens: scaled text, text shrunk to fit a width, wrapped text of a known height. */
public final class GidsTekst {
    static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Text at (x, y) at a scale; rechts: x is where it ends. */
    public static void schaal(GuiGraphics g, Component text, int x, int y, float scale, int colour, boolean rechts) {
        Font font = font();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, rechts ? -font.width(text) : 0, 0, colour, false);
        g.pose().popPose();
    }

    /** Text on one line at most this scale, smaller when it doesn't fit maxWidth; returns the width used. */
    public static int passend(GuiGraphics g, Component text, int x, int y, int maxWidth, float scale, int colour, boolean rechts) {
        int tw = Math.max(1, font().width(text));
        float s = Math.min(scale, maxWidth / (float) tw);
        s = Math.max(s, 0.4f);
        schaal(g, text, x, y + Math.round((8 * scale - 8 * s) / 2), s, colour, rechts);
        return Math.round(tw * s);
    }

    /** The lines of a text wrapped to maxWidth at a scale. */
    public static List<FormattedCharSequence> regels(Component text, int maxWidth, float scale) {
        return font().split(text, (int) Math.floor(maxWidth / scale));
    }

    /** The height of wrapped text (lines of 10 px at scale 1). */
    public static int hoogte(Component text, int maxWidth, float scale) {
        return Math.round(regels(text, maxWidth, scale).size() * 10 * scale);
    }

    /** Wrapped text; returns the height used. */
    public static int alinea(GuiGraphics g, Component text, int x, int y, int maxWidth, float scale, int colour) {
        List<FormattedCharSequence> lines = regels(text, maxWidth, scale);
        Font font = font();
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), 0, i * 10, colour, false);
        }
        g.pose().popPose();
        return Math.round(lines.size() * 10 * scale);
    }

    /** A progress bar (0..1), pink on dark, green when full. */
    public static void balk(GuiGraphics g, int x, int y, int w, int h, float frac) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF7A2848);
        g.fill(x, y, x + w, y + h, 0xFF3A1C30);
        int fw = Math.round(w * Math.max(0f, Math.min(1f, frac)));
        if (fw > 0) {
            g.fill(x, y, x + fw, y + h, frac >= 1f ? 0xFF68D88A : 0xFFF77AB0);
            g.fill(x, y, x + fw, y + 1, 0x60FFFFFF);
        }
    }

    private GidsTekst() {
    }
}
