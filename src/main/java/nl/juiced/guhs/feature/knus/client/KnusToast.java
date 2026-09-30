package nl.juiced.guhs.feature.knus.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A pink toast: "Knus: new milestone / new entry", with an icon. */
public class KnusToast implements Toast {
    private static final long SHOW_MS = 5000L;
    private final Component title;
    private final Component text;
    private final ItemStack icon;

    public KnusToast(Component title, Component text, ItemStack icon) {
        this.title = title;
        this.text = text;
        this.icon = icon;
    }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toasts, long timeSinceLastVisible) {
        int w = width(), h = height();
        g.fill(0, 0, w, h, 0xF0301A26);
        g.fill(0, 0, w, 1, 0xFFF7B6CB);
        g.fill(0, h - 1, w, h, 0xFFF7B6CB);
        g.fill(0, 0, 1, h, 0xFFF7B6CB);
        g.fill(w - 1, 0, w, h, 0xFFF7B6CB);
        g.renderItem(icon, 8, 8);
        var font = toasts.getMinecraft().font;
        g.drawString(font, title, 30, 7, 0xFFFFB6D8, false);
        g.drawString(font, font.plainSubstrByWidth(text.getString(), w - 34), 30, 18, 0xFFFFE6EE, false);
        return timeSinceLastVisible >= SHOW_MS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
