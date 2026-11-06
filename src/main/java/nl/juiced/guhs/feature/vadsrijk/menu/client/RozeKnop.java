package nl.juiced.guhs.feature.vadsrijk.menu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;

/**
 * U-TABS: a vanilla Button drawn in the pink guh style of the Vadsrijk-menu (same look as EcoScherm's {@code knop}).
 * {@code Button.builder(tekst, b -> ...).bounds(x, y, w, h).build(RozeKnop::new)}.
 */
public class RozeKnop extends Button {
    public RozeKnop(Button.Builder b) {
        super(b);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x = getX(), y = getY(), bw = getWidth(), bh = getHeight();
        boolean hover = active && isHoveredOrFocused();
        g.fill(x, y, x + bw, y + bh, active ? (hover ? 0xFFC0287C : 0xFFE0489A) : 0xFFC8A8BC);
        g.fill(x + 1, y + 1, x + bw - 1, y + bh - 1, active ? (hover ? 0xFFFF8CC8 : 0xFFFF6FB5) : 0xFFE4D0DC);
        g.fill(x + 1, y + 1, x + bw - 1, y + 2, active ? 0x60FFFFFF : 0x30FFFFFF);
        var font = Minecraft.getInstance().font;
        String s = font.plainSubstrByWidth(getMessage().getString(), bw - 4);
        var c = s.length() < getMessage().getString().length() ? net.minecraft.network.chat.Component.literal(s).withStyle(getMessage().getStyle()) : getMessage();
        g.centeredText(font, c, x + bw / 2, y + (bh - 8) / 2, active ? 0xFFFFFFFF : 0xFF9A8094);
    }
}
