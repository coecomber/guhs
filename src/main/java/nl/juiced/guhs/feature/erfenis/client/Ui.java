package nl.juiced.guhs.feature.erfenis.client;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.erfenis.Goed;

/**
 * De Erfenis, the overview's drawing context for one frame: the graphics, the font, the mouse, and the list of clickable
 * areas (later entries lie on top). Panels and windows draw through this, so the screen handles every click the same way.
 */
final class Ui {
    /** A clickable (or just mouse-blocking) area, with an optional tooltip. */
    record Klik(int x0, int y0, int x1, int y1, @Nullable Runnable actie, @Nullable List<Component> tip) {
        boolean op(double mx, double my) {
            return mx >= x0 && mx < x1 && my >= y0 && my < y1;
        }
    }

    final GuiGraphicsExtractor g;
    final Font font;
    final int mx, my;
    final List<Klik> klikken;

    Ui(GuiGraphicsExtractor g, Font font, int mx, int my, List<Klik> klikken) {
        this.g = g;
        this.font = font;
        this.mx = mx;
        this.my = my;
        this.klikken = klikken;
    }

    boolean op(int x0, int y0, int x1, int y1) {
        return mx >= x0 && mx < x1 && my >= y0 && my < y1;
    }

    /** An area that swallows clicks (a panel), with an optional tooltip. */
    void vlak(int x0, int y0, int x1, int y1, @Nullable List<Component> tip) {
        klikken.add(new Klik(x0, y0, x1, y1, null, tip));
    }

    /** A button; {@code actie} only runs when {@code kan}. */
    void knop(Component tekst, int x0, int y0, int x1, int y1, boolean kan, int kleur, @Nullable Runnable actie, @Nullable List<Component> tip) {
        Teken.knop(g, font, tekst, x0, y0, x1, y1, mx, my, kan, kleur);
        klikken.add(new Klik(x0, y0, x1, y1, kan ? actie : null, tip));
    }

    void klik(int x0, int y0, int x1, int y1, @Nullable Runnable actie, @Nullable List<Component> tip) {
        klikken.add(new Klik(x0, y0, x1, y1, actie, tip));
    }

    void tekst(Component t, int x, int y, int kleur) {
        g.text(font, t, x, y, kleur, false);
    }

    void tekst(String t, int x, int y, int kleur) {
        g.text(font, t, x, y, kleur, false);
    }

    void tekst(FormattedCharSequence t, int x, int y, int kleur) {
        g.text(font, t, x, y, kleur, false);
    }

    /** Text squeezed into a width (smaller, never cut). */
    void passend(Component t, int x, int y, int breed, int kleur) {
        int w = font.width(t);
        if (w <= breed) {
            g.text(font, t, x, y, kleur, false);
            return;
        }
        float s = breed / (float) w;
        g.pose().pushMatrix();
        g.pose().translate(x, y + (1 - s) * 4);
        g.pose().scale(s, s);
        g.text(font, t, 0, 0, kleur, false);
        g.pose().popMatrix();
    }

    void item(ItemStack st, int x, int y) {
        g.item(st, x, y);
    }

    /** An item at a scale (0.5 = 8 px), top-left at x, y. */
    void item(ItemStack st, int x, int y, float schaal) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(schaal, schaal);
        g.item(st, 0, 0);
        g.pose().popMatrix();
    }

    // =====================================================================================================================

    private static final Map<Goed, ItemStack> GOED = new EnumMap<>(Goed.class);
    private static final Map<String, ItemStack> ITEMS = new HashMap<>();

    static ItemStack icoon(Goed g) {
        return GOED.computeIfAbsent(g, Goed::icoon);
    }

    static ItemStack item(String id) {
        return ITEMS.computeIfAbsent(id, k -> {
            var i = Goed.item(k);
            return new ItemStack(i == null ? Items.BARRIER : i);
        });
    }

    static final ItemStack GUHLDEN = new ItemStack(Items.GOLD_NUGGET), BEWONER = new ItemStack(Items.TOTEM_OF_UNDYING),
            OPSLAG = new ItemStack(Items.CHEST), KNUS = new ItemStack(Items.PINK_TULIP), BOUWER = new ItemStack(Items.GOLDEN_SHOVEL),
            KLOK = new ItemStack(Items.CLOCK), VK = new ItemStack(Items.REDSTONE), SLOT = new ItemStack(Items.IRON_BARS);

    /** 1234 -> "1.234", 12345 -> "12,3k", 1234567 -> "1,2M" (Dutch style). */
    static String getal(long n) {
        if (n >= 1_000_000) {
            return String.format(java.util.Locale.ROOT, "%.1fM", n / 1e6).replace('.', ',');
        }
        if (n >= 10_000) {
            return String.format(java.util.Locale.ROOT, "%.1fk", n / 1e3).replace('.', ',');
        }
        if (n >= 1000) {
            return n / 1000 + "." + String.format("%03d", n % 1000);
        }
        return Long.toString(n);
    }

    /** "1 u 15 m" / "12 m" / "45 s" for a duration. */
    static String duur(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        if (s >= 3600) {
            return s / 3600 + " u" + (s / 60 % 60 > 0 ? " " + s / 60 % 60 + " m" : "");
        }
        if (s >= 60) {
            return s / 60 + " m";
        }
        return s + " s";
    }
}
