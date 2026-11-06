package nl.juiced.guhs.feature.vadsrijk.toetsen.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.vadsrijk.toetsen.ToetsenIndeling;

/**
 * U-TOETSEN: draws the key overview into a box (pink guh panel, semi-transparent) and remembers where each key line landed, for
 * the click (= open the vanilla key screen). Used by the pause menu (box = right of the buttons) and by {@link ToetsenScherm}.
 */
final class ToetsenPaneel {
    static final int ACHTER = 0xC8301726;      // panel fill (dark pink, semi-transparent)
    static final int RANDKLEUR = 0xFFE88AC0;   // pink border
    static final int KOP = 0xFFFFB6DA;         // group header text
    static final int TITEL = 0xFFFFD6EC;
    static final int TEKST = 0xFFF4E8EF;
    static final int GRIJS = 0xFF8A7A84;
    static final int ROOD = 0xFFFF5A5A;
    static final int TOETS_ACHTER = 0xFF5A2E4C;
    static final int TOETS_RAND = 0xFFB0628E;
    static final int HOVER = 0x40FFB6DA;
    private static final int TOETS_MAX = 64;   // the key column never gets wider (very long names are cut)

    private record Vak(VadsrijkToetsen.Toets toets, int x0, int y0, int x1, int y1) {
    }

    private final List<Vak> vakken = new ArrayList<>();
    private ToetsenIndeling.Uitkomst laatste;
    private int px, py;

    /** Layout for this box; null when it does not fit (the caller shows a button instead). */
    ToetsenIndeling.Uitkomst plan(List<VadsrijkToetsen.Toets> toetsen, int maxB, int maxH, float minSchaal) {
        Font f = Minecraft.getInstance().font;
        int tk = toetsKolom(f, toetsen);
        return ToetsenIndeling.kies(regels(f, toetsen, tk), maxB, maxH, minSchaal, f.width(titel()));
    }

    static Component titel() {
        return Component.translatable("vadsrijk.toetsen.titel");
    }

    private static int toetsKolom(Font f, List<VadsrijkToetsen.Toets> toetsen) {
        int tk = 14;
        for (VadsrijkToetsen.Toets t : toetsen) {
            tk = Math.max(tk, f.width(toetsTekst(t)) + 6);
        }
        return Math.min(tk, TOETS_MAX);
    }

    private static Component toetsTekst(VadsrijkToetsen.Toets t) {
        return t.los() ? Component.literal("—") : t.toets();
    }

    /** The lines in order: per group a header, then its keys. */
    private static List<ToetsenIndeling.Regel> regels(Font f, List<VadsrijkToetsen.Toets> toetsen, int tk) {
        List<ToetsenIndeling.Regel> uit = new ArrayList<>();
        VadsrijkToetsen.Groep vorige = null;
        for (VadsrijkToetsen.Toets t : toetsen) {
            if (t.groep() != vorige) {
                uit.add(new ToetsenIndeling.Regel(true, f.width(t.groep().naam()) + 4));
                vorige = t.groep();
            }
            uit.add(new ToetsenIndeling.Regel(false, tk + 4 + f.width(t.beschrijving()) + (t.conflictMet() != null ? 8 : 0)));
        }
        return uit;
    }

    /** Draws at (x, y) = the panel's top-left on screen. */
    void teken(GuiGraphicsExtractor g, List<VadsrijkToetsen.Toets> toetsen, ToetsenIndeling.Uitkomst u, int x, int y, int mx, int my) {
        Font f = Minecraft.getInstance().font;
        laatste = u;
        px = x;
        py = y;
        vakken.clear();
        int tk = toetsKolom(f, toetsen);
        float s = u.schaal();
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(s, s);
        int b = u.breedte(), h = u.hoogte();
        g.fill(0, 0, b, h, ACHTER);
        g.outline(0, 0, b, h, RANDKLEUR);
        g.fill(1, 1, b - 1, 2, 0x60FFFFFF);   // a soft highlight along the top
        g.text(f, titel(), ToetsenIndeling.RAND, ToetsenIndeling.RAND, TITEL, true);
        // virtual mouse position
        float vmx = (mx - x) / s, vmy = (my - y) / s;
        VadsrijkToetsen.Groep vorige = null;
        int i = 0;
        VadsrijkToetsen.Toets hover = null;
        for (VadsrijkToetsen.Toets t : toetsen) {
            if (t.groep() != vorige) {
                ToetsenIndeling.Plek p = u.plekken().get(i++);
                g.text(f, t.groep().naam(), p.x(), p.y() + 3, KOP, true);
                g.fill(p.x(), p.y() + 12, p.x() + u.kolomBreedte(), p.y() + 13, 0x50E88AC0);
                vorige = t.groep();
            }
            ToetsenIndeling.Plek p = u.plekken().get(i++);
            int x0 = p.x(), y0 = p.y(), x1 = p.x() + u.kolomBreedte(), y1 = p.y() + ToetsenIndeling.REGEL_H;
            boolean over = vmx >= x0 && vmx < x1 && vmy >= y0 && vmy < y1;
            if (over) {
                g.fill(x0 - 1, y0, x1 + 1, y1, HOVER);
                hover = t;
            }
            // the key as a small key cap
            int kleur = t.los() ? GRIJS : t.conflictMet() != null ? ROOD : TEKST;
            g.fill(x0, y0, x0 + tk, y0 + 10, t.conflictMet() != null ? 0xFF6A2030 : TOETS_ACHTER);
            g.outline(x0, y0, tk, 10, t.conflictMet() != null ? ROOD : TOETS_RAND);
            String ts = f.plainSubstrByWidth(toetsTekst(t).getString(), tk - 4);
            g.text(f, ts, x0 + (tk - f.width(ts)) / 2, y0 + 1, kleur, false);
            // the description (cut when the column is too narrow)
            int dx = x0 + tk + 4;
            int ruimte = x1 - dx - (t.conflictMet() != null ? 8 : 0);
            String d = t.beschrijving().getString();
            if (f.width(d) > ruimte) {
                d = f.plainSubstrByWidth(d, Math.max(0, ruimte - f.width("…"))) + "…";
            }
            g.text(f, d, dx, y0 + 1, t.los() ? GRIJS : TEKST, true);
            if (t.conflictMet() != null) {
                g.text(f, "!", x1 - 6, y0 + 1, ROOD, true);
            }
            vakken.add(new Vak(t, x + Math.round(x0 * s), y + Math.round(y0 * s), x + Math.round(x1 * s), y + Math.round(y1 * s)));
        }
        String voet = Component.translatable("vadsrijk.toetsen.voet").getString();
        g.text(f, f.plainSubstrByWidth(voet, b - 2 * ToetsenIndeling.RAND), ToetsenIndeling.RAND, h - ToetsenIndeling.RAND - 8, GRIJS, false);
        pose.popMatrix();
        if (hover != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(hover.mapping().getName()));
            if (hover.los()) {
                tip.add(Component.translatable("vadsrijk.toetsen.tip.los").withColor(GRIJS));
            }
            if (hover.conflictMet() != null) {
                tip.add(Component.translatable("vadsrijk.toetsen.tip.conflict", hover.conflictMet()).withColor(ROOD));
            }
            tip.add(Component.translatable("vadsrijk.toetsen.tip.klik").withColor(KOP));
            g.setTooltipForNextFrame(f, tip, java.util.Optional.empty(), mx, my);
        }
    }

    /** The key line under the mouse in the last drawn frame, or null. */
    VadsrijkToetsen.Toets onder(double mx, double my) {
        for (Vak v : vakken) {
            if (mx >= v.x0() && mx < v.x1() && my >= v.y0() && my < v.y1()) {
                return v.toets();
            }
        }
        return null;
    }

    /** Is the point inside the last drawn panel? */
    boolean binnen(double mx, double my) {
        return laatste != null && mx >= px && my >= py && mx < px + laatste.schermBreedte() && my < py + laatste.schermHoogte();
    }

    void vergeet() {
        vakken.clear();
        laatste = null;
    }
}
