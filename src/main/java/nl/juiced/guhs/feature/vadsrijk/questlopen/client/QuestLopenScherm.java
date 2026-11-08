package nl.juiced.guhs.feature.vadsrijk.questlopen.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.vadsrijk.questlopen.QlNet;

/**
 * U-QUESTLOPEN: the admin screen. A small panel (drag the pink title bar to move it; the world stays visible and the game does
 * not pause), with the current step (zone, quest, what / where / who, min. and expected level), the whole main line as a list
 * (done = ✔ grey, current = pink, click a row = choose it for "Spring naar"), and the buttons. At a boss step "Volgende" becomes
 * "Zelf vechten" + "Laat hem verslaan". Enter = Volgende, Esc = close ({@code /guhs vadsrijk questlopen verder} opens it again).
 */
public final class QuestLopenScherm extends Screen {
    private static final String K = "vadsrijk.questlopen.";
    private static final int ROZE = 0xFFFF5FAF, ROZE_DONKER = 0xFFC0287C, PANEEL = 0xF0FFF4FA, TEKST = 0xFF5A3A56, ZACHT = 0xFF9A8094, GOUD = 0xFFB07A10;
    private static final int RIJ = 10;
    /** Where the panel was dragged to (kept while the game runs). */
    private static int px = -1, py = -1;

    private record Vlak(int x, int y, int w, int h, Runnable doe) {
    }

    private final List<Vlak> vlakken = new ArrayList<>();
    private int pw, ph;
    private boolean sleept;
    private double grijpX, grijpY;
    private int scroll;
    private int keuze = -1;
    private boolean resetZeker;
    private long resetTot;
    private boolean eersteKeer = true;

    public QuestLopenScherm() {
        super(Component.translatable(K + "titel"));
    }

    void nieuw() {
        eersteKeer = true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        // (no blur, no darkening: the world and the scene stay visible)
    }

    private static CompoundTag meta() {
        QlNet.Stand s = QuestLopenClient.laatste;
        return s == null ? new CompoundTag() : s.meta();
    }

    private static Component tekst(int i) {
        QlNet.Stand s = QuestLopenClient.laatste;
        return s == null || i < 0 || i >= s.teksten().size() ? Component.empty() : s.teksten().get(i);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        vlakken.clear();
        pw = Math.min(340, width - 8);
        ph = Math.min(250, height - 8);
        if (px < 0 || py < 0) {
            px = width - pw - 6;
            py = 6;
        }
        px = Math.max(0, Math.min(width - pw, px));
        py = Math.max(0, Math.min(height - ph, py));
        int x = px, y = py;
        g.fill(x - 1, y - 1, x + pw + 1, y + ph + 1, ROZE_DONKER);
        g.fill(x, y, x + pw, y + ph, PANEEL);
        g.fill(x, y, x + pw, y + 14, ROZE);
        CompoundTag m = meta();
        g.text(font, Component.translatable(K + "titel"), x + 5, y + 3, 0xFFFFFFFF, true);
        String kar = m.getStringOr("karakter", "");
        if (!kar.isEmpty()) {
            Component c = Component.translatable(K + "karakter", kar);
            g.text(font, c, x + pw - 18 - font.width(c), y + 3, 0xFFFFE8F4, false);
        }
        knop(g, mx, my, x + pw - 13, y + 2, 11, 10, Component.literal("×"), true, this::onClose);
        if (!m.getBooleanOr("begrepen", false)) {
            waarschuwing(g, mx, my, m, x, y);
            return;
        }
        ListTag st = m.getListOrEmpty("stappen");
        int huidig = m.getIntOr("huidig", -1);
        String fase = m.getStringOr("fase", "RUST");
        int cy = y + 18;
        // ---- the current step
        if (huidig >= 0 && huidig < st.size()) {
            CompoundTag c = st.getCompoundOrEmpty(huidig);
            int t = c.getIntOr("t", 0);
            g.text(font, Component.translatable(K + "zone", c.getIntOr("z", 1)).append(" · ").append(tekst(t)).withStyle(ChatFormatting.BOLD),
                    x + 6, cy, TEKST, false);
            cy += 11;
            Component soort = Component.translatable(K + "soort." + c.getIntOr("s", 0));
            cy = regels(g, Component.empty().append(soort).append(": ").append(tekst(t + 1)), x + 6, cy, pw - 12, TEKST, 2);
            Component waar = tekst(t + 2), wie = tekst(t + 3);
            Component w = Component.translatable(K + "waar").append(" ").append(waar);
            if (!wie.getString().isEmpty()) {
                w = w.copy().append("  ").append(Component.translatable(K + "wie")).append(" ").append(wie);
            }
            cy = regels(g, w, x + 6, cy, pw - 12, ZACHT, 2);
            boolean kl = c.getBooleanOr("kl", false);
            Component lv = Component.translatable(K + (kl ? "lv.klasse" : "lv"), c.getIntOr("lv", 1), c.getIntOr("vl", 1),
                    m.getIntOr("level", 1), m.getIntOr("klasseLevel", 1));
            g.text(font, lv, x + 6, cy, GOUD, false);
            cy += 11;
            if (c.getBooleanOr("b", false)) {
                g.text(font, Component.translatable(K + "baas").withStyle(ChatFormatting.BOLD), x + 6, cy, 0xFFC23A3A, false);
                cy += 10;
            }
        } else {
            g.text(font, Component.translatable(K + "melding.klaar"), x + 6, cy, GOUD, false);
            cy += 11;
        }
        if (m.contains("melding")) {
            cy = regels(g, tekst(m.getIntOr("melding", -1)), x + 6, cy, pw - 12, 0xFF7A4AB0, 2);
        }
        if (!fase.equals("RUST")) {
            g.text(font, Component.translatable(K + "fase." + fase.toLowerCase(java.util.Locale.ROOT)), x + 6, cy, 0xFF2F8A5A, false);
            cy += 10;
        }
        // ---- the buttons (bottom up)
        int by = y + ph - 17;
        int bw3 = (pw - 16) / 3;
        boolean verwacht = m.getBooleanOr("verwacht", false), adv = m.getBooleanOr("adv", false), rij = m.getBooleanOr("rijdier", false);
        knop(g, mx, my, x + 4, by, bw3, 14, Component.translatable(K + "knop.level." + (verwacht ? "verwacht" : "min")), true,
                () -> QuestLopenClient.doe("level", verwacht ? 0 : 1));
        knop(g, mx, my, x + 8 + bw3, by, bw3, 14, Component.translatable(K + "knop.adv." + (adv ? "aan" : "uit")), true,
                () -> QuestLopenClient.doe("adv", adv ? 0 : 1));
        knop(g, mx, my, x + 12 + 2 * bw3, by, bw3, 14, Component.translatable(K + "knop.rijdier." + (rij ? "aan" : "uit")), true,
                () -> QuestLopenClient.doe("rijdier", rij ? 0 : 1));
        tipNa(g, mx, my, x + 4, by, bw3, 14, K + "tip.level");
        tipNa(g, mx, my, x + 8 + bw3, by, bw3, 14, K + "tip.adv");
        tipNa(g, mx, my, x + 12 + 2 * bw3, by, bw3, 14, K + "tip.rijdier");
        by -= 17;
        // W-SPEL: the way out of any stuck step (also while a fight or a scene wait runs): skip it, or back to the last safe spot
        int bw2 = (pw - 12) / 2;
        boolean nietSpoel = !fase.equals("SPOEL");
        knop(g, mx, my, x + 4, by, bw2, 14, Component.translatable(K + "knop.overslaan"), nietSpoel && huidig >= 0, () -> QuestLopenClient.doe("overslaan", 0));
        knop(g, mx, my, x + 8 + bw2, by, bw2, 14, Component.translatable(K + "knop.veilig"), nietSpoel, () -> QuestLopenClient.doe("veilig", 0));
        tipNa(g, mx, my, x + 4, by, bw2, 14, K + "tip.overslaan");
        tipNa(g, mx, my, x + 8 + bw2, by, bw2, 14, K + "tip.veilig");
        by -= 17;
        boolean rust = fase.equals("RUST") || fase.equals("VECHT");
        if (resetZeker && System.currentTimeMillis() > resetTot) {
            resetZeker = false;
        }
        knop(g, mx, my, x + 4, by, bw3, 14, Component.translatable(K + "knop.herhaal"), rust && m.getBooleanOr("scenes", false),
                () -> QuestLopenClient.doe("herhaal", 0));
        knop(g, mx, my, x + 8 + bw3, by, bw3, 14, Component.translatable(K + "knop.teleport"), rust, () -> QuestLopenClient.doe("teleport", 0));
        knop(g, mx, my, x + 12 + 2 * bw3, by, bw3, 14, Component.translatable(K + (resetZeker ? "knop.reset.zeker" : "knop.reset")), rust, () -> {
            if (resetZeker) {
                resetZeker = false;
                QuestLopenClient.doe("reset", 0);
            } else {
                resetZeker = true;
                resetTot = System.currentTimeMillis() + 4000;
            }
        });
        by -= 17;
        boolean baas = huidig >= 0 && huidig < st.size() && st.getCompoundOrEmpty(huidig).getBooleanOr("b", false);
        knop(g, mx, my, x + 4, by, bw3 - 20, 14, Component.translatable(K + "knop.terug"), rust, () -> QuestLopenClient.doe("terug", 0));
        int vx = x + bw3 - 12, vw = bw3 + 40;
        if (fase.equals("SPOEL")) {
            knop(g, mx, my, vx, by, vw, 14, Component.translatable(K + "knop.stop"), true, () -> QuestLopenClient.doe("stop", 0));
        } else if (baas) {
            int h = (vw - 4) / 2;
            knop(g, mx, my, vx, by, h, 14, Component.translatable(K + "knop.zelf"), rust, () -> QuestLopenClient.doe("zelf", 0));
            knop(g, mx, my, vx + h + 4, by, h, 14, Component.translatable(K + "knop.versla"), rust, () -> QuestLopenClient.doe("versla", 0));
        } else {
            knop(g, mx, my, vx, by, vw, 14, Component.translatable(K + "knop.volgende").withStyle(ChatFormatting.BOLD), rust && huidig >= 0,
                    () -> QuestLopenClient.doe("volgende", 0));
        }
        int sx = vx + vw + 4, sw = x + pw - 4 - sx;
        int kz = keuze;
        knop(g, mx, my, sx, by, sw, 14, kz >= 0 ? Component.translatable(K + "knop.spring.naar", kz + 1) : Component.translatable(K + "knop.spring"),
                rust && kz >= 0, () -> {
                    QuestLopenClient.doe("spring", kz);
                    keuze = -1;
                });
        tipNa(g, mx, my, sx, by, sw, 14, K + "tip.spring");
        // ---- the list
        int ly0 = cy + 3, ly1 = by - 4;
        lijst(g, mx, my, st, huidig, x + 4, ly0, pw - 8, ly1 - ly0);
    }

    private void waarschuwing(GuiGraphicsExtractor g, int mx, int my, CompoundTag m, int x, int y) {
        int cy = y + 22;
        g.text(font, Component.translatable(K + "let_op").withStyle(ChatFormatting.BOLD), x + 8, cy, 0xFFC23A3A, false);
        cy += 14;
        cy = regels(g, Component.translatable(K + "waarschuwing", m.getStringOr("karakter", "?")), x + 8, cy, pw - 16, TEKST, 12);
        knop(g, mx, my, x + pw / 2 - 50, Math.min(cy + 10, y + ph - 22), 100, 16, Component.translatable(K + "knop.begrepen"), true,
                () -> QuestLopenClient.doe("begrepen", 0));
    }

    private void lijst(GuiGraphicsExtractor g, int mx, int my, ListTag st, int huidig, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFFFFFFF);
        g.fill(x, y, x + w, y + 1, 0xFFE8C8DC);
        int zicht = Math.max(1, h / RIJ);
        if (eersteKeer && huidig >= 0) {
            scroll = Math.max(0, huidig - zicht / 3);
            eersteKeer = false;
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, st.size() - zicht)));
        int laatsteZone = -1;
        for (int i = scroll; i < st.size() && (i - scroll) < zicht; i++) {
            CompoundTag c = st.getCompoundOrEmpty(i);
            int ry = y + 1 + (i - scroll) * RIJ;
            boolean klaar = c.getBooleanOr("k", false), cur = i == huidig, gekozen = i == keuze;
            boolean over = mx >= x && mx < x + w && my >= ry && my < ry + RIJ;
            if (cur) {
                g.fill(x + 1, ry, x + w - 1, ry + RIJ, 0xFFFFC8E4);
            } else if (gekozen) {
                g.fill(x + 1, ry, x + w - 1, ry + RIJ, 0xFFC8E0FF);
            } else if (over) {
                g.fill(x + 1, ry, x + w - 1, ry + RIJ, 0xFFF4F0F8);
            }
            int z = c.getIntOr("z", 1);
            if (z != laatsteZone && i > 0 && st.getCompoundOrEmpty(i - 1).getIntOr("z", 1) != z) {
                g.fill(x + 1, ry, x + w - 1, ry + 1, ROZE);
            }
            laatsteZone = z;
            int t = c.getIntOr("t", 0);
            String teken = klaar ? "✔" : cur ? "▶" : c.getBooleanOr("b", false) ? "☠" : "·";
            int kleur = klaar ? 0xFFB0A0AC : cur ? ROZE_DONKER : TEKST;
            String nr = (i + 1) + "";
            g.pose().pushMatrix();
            g.pose().translate(x + 3, ry + 2);
            g.pose().scale(0.75f, 0.75f);
            g.text(font, teken + " " + nr + " Z" + z, 0, 0, kleur, false);
            Component regel = Component.empty().append(tekst(t)).append(" — ")
                    .append(Component.translatable(K + "soort." + c.getIntOr("s", 0))).append(": ").append(tekst(t + 1));
            if (c.getBooleanOr("x", false)) {
                regel = Component.literal("+ ").append(regel);
            }
            int bruikbaar = (int) ((w - 40) / 0.75f) - 40;
            g.text(font, font.plainSubstrByWidth(regel.getString(), bruikbaar - 30), 44, 0, kleur, false);
            String lv = "lv " + c.getIntOr("lv", 1);
            g.text(font, lv, (int) ((w - 6) / 0.75f) - font.width(lv), 0, klaar ? 0xFFC8B8C4 : GOUD, false);
            g.pose().popMatrix();
            int fi = i;
            vlakken.add(new Vlak(x, ry, w, RIJ, () -> keuze = keuze == fi ? -1 : fi));
        }
        if (st.size() > zicht) {
            int bh = Math.max(8, h * zicht / st.size());
            int bt = y + (h - bh) * scroll / Math.max(1, st.size() - zicht);
            g.fill(x + w - 3, bt, x + w - 1, bt + bh, 0xFFE0A0C8);
        }
    }

    private int regels(GuiGraphicsExtractor g, Component c, int x, int y, int w, int kleur, int max) {
        var l = font.split(c, w);
        for (int i = 0; i < l.size() && i < max; i++) {
            g.text(font, l.get(i), x, y, kleur, false);
            y += 10;
        }
        return y;
    }

    private void knop(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int h, Component c, boolean actief, Runnable doe) {
        boolean over = actief && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, actief ? (over ? 0xFFC0287C : 0xFFE0489A) : 0xFFC8A8BC);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, actief ? (over ? 0xFFFF8CC8 : 0xFFFF6FB5) : 0xFFE4D0DC);
        String s = font.plainSubstrByWidth(c.getString(), w - 4);
        Component t = s.length() < c.getString().length() ? Component.literal(s).withStyle(c.getStyle()) : c;
        g.centeredText(font, t, x + w / 2, y + (h - 8) / 2, actief ? 0xFFFFFFFF : 0xFF9A8094);
        if (actief) {
            vlakken.add(new Vlak(x, y, w, h, doe));
        }
    }

    private void tipNa(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int h, String key) {
        if (mx >= x && mx < x + w && my >= y && my < y + h) {
            g.setTooltipForNextFrame(font, font.split(Component.translatable(key), 220), mx, my);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dubbel) {
        double mx = e.x(), my = e.y();
        if (e.button() == 0) {
            for (int i = vlakken.size() - 1; i >= 0; i--) {
                Vlak v = vlakken.get(i);
                if (mx >= v.x() && mx < v.x() + v.w() && my >= v.y() && my < v.y() + v.h()) {
                    v.doe().run();
                    return true;
                }
            }
            if (mx >= px && mx < px + pw && my >= py && my < py + 14) {
                sleept = true;
                grijpX = mx - px;
                grijpY = my - py;
                return true;
            }
        }
        return super.mouseClicked(e, dubbel);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (sleept) {
            px = (int) (e.x() - grijpX);
            py = (int) (e.y() - grijpY);
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        sleept = false;
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll -= (int) Math.signum(sy) * 3;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            CompoundTag m = meta();
            String fase = m.getStringOr("fase", "RUST");
            ListTag st = m.getListOrEmpty("stappen");
            int h = m.getIntOr("huidig", -1);
            if (m.getBooleanOr("begrepen", false) && fase.equals("RUST") && h >= 0 && h < st.size() && !st.getCompoundOrEmpty(h).getBooleanOr("b", false)) {
                QuestLopenClient.doe("volgende", 0);
            }
            return true;
        }
        return super.keyPressed(event);
    }
}
