package nl.juiced.guhs.feature.vadsrijk.dungeon.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonNet;

/**
 * W-GROT, client: the dungeon door's window and the ready check, in the pink panel style of the Vadsrijk windows (QuestLopenScherm,
 * RozeKnop): a pink title bar with "Dungeon: name", the lines the server sent (headings in dark pink), the buttons in
 * rows of two at the bottom (a tooltip on hover), a time bar for the ready check. The game does not pause; Esc closes (and tells
 * the server, which then stops sending updates). All texts come from the server ({@link DungeonNet}).
 */
public final class DungeonScherm extends Screen {
    private static final int ROZE = 0xFFFF5FAF, ROZE_DONKER = 0xFFC0287C, PANEEL = 0xF0FFF4FA;

    private record Vlak(int x, int y, int w, int h, Runnable doe) {
    }

    private DungeonNet.Scherm data;
    private final List<Vlak> vlakken = new ArrayList<>();

    DungeonScherm(DungeonNet.Scherm data) {
        super(Component.translatable("vadsrijk.dungeon.titel"));
        this.data = data;
    }

    String soort() {
        return data.meta().getStringOr("soort", "deur");
    }

    void zet(DungeonNet.Scherm d) {
        this.data = d;
    }

    /** Client setup: the window receives the server's state. */
    public static void installeer() {
        DungeonNet.ontvanger = sch -> Minecraft.getInstance().execute(() -> ontvang(sch));
    }

    /** Client hook: open / update / close. */
    static void ontvang(DungeonNet.Scherm s) {
        Minecraft mc = Minecraft.getInstance();
        CompoundTag m = s.meta();
        String soort = m.getStringOr("soort", "deur");
        if (mc.screen instanceof DungeonScherm g && g.soort().equals(soort)) {
            if (m.getBooleanOr("sluit", false)) {
                g.stil = true;
                mc.setScreen(null);
            } else {
                g.zet(s);
            }
            return;
        }
        if (m.getBooleanOr("sluit", false)) {
            return;
        }
        if (m.getBooleanOr("open", false) || soort.equals("klaar") && mc.screen == null) {
            mc.setScreen(new DungeonScherm(s));
        }
    }

    private boolean stil;

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!stil) {
            doe(soort().equals("klaar") ? "dicht_klaar" : "dicht");
        }
        super.onClose();
    }

    private Component tekst(int i) {
        List<Component> t = data.teksten();
        return i < 0 || i >= t.size() ? Component.empty() : t.get(i);
    }

    void doe(String wat) {
        ClientPacketDistributor.sendToServer(new DungeonNet.Actie(wat, data.meta().getStringOr("dungeon", "")));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        vlakken.clear();
        CompoundTag m = data.meta();
        ListTag lijnen = m.getListOrEmpty("lijnen"), knoppen = m.getListOrEmpty("knoppen");
        boolean klaar = soort().equals("klaar");
        int pw = Math.min(klaar ? 240 : 300, width - 8);
        int tw = pw - 14;
        // measure
        List<List<FormattedCharSequence>> gesplitst = new ArrayList<>();
        int hoog = 0;
        for (int i = 0; i < lijnen.size(); i++) {
            CompoundTag l = lijnen.getCompoundOrEmpty(i);
            List<FormattedCharSequence> s = font.split(tekst(l.getIntOr("t", -1)), tw);
            gesplitst.add(s);
            hoog += s.size() * 10 + (l.getBooleanOr("h", false) ? 5 : 0);
        }
        int rijen = (knoppen.size() + 1) / 2;
        Component onder = tekst(1);
        List<FormattedCharSequence> onderRegels = klaar ? font.split(onder, tw) : List.of();
        int ph = 20 + (klaar ? onderRegels.size() * 10 + 4 : 0) + hoog + (m.contains("tijd") ? 10 : 0) + 6 + rijen * 18 + 4;
        ph = Math.min(ph, height - 8);
        int x = (width - pw) / 2, y = Math.max(4, (height - ph) / 2);
        g.fill(x - 1, y - 1, x + pw + 1, y + ph + 1, ROZE_DONKER);
        g.fill(x, y, x + pw, y + ph, PANEEL);
        g.fill(x, y, x + pw, y + 15, ROZE);
        g.text(font, tekst(0), x + 6, y + 4, 0xFFFFFFFF, true);
        if (!klaar) {
            Component z = onder;
            g.text(font, z, x + pw - 18 - font.width(z), y + 4, 0xFFFFE8F4, false);
        }
        knop(g, mx, my, x + pw - 13, y + 2, 11, 11, Component.literal("×"), true, this::onClose);
        int cy = y + 19;
        for (FormattedCharSequence s : onderRegels) {
            g.text(font, s, x + 7, cy, 0xFF9A8094, false);
            cy += 10;
        }
        if (klaar) {
            cy += 4;
        }
        int bodem = y + ph - rijen * 18 - 4;
        for (int i = 0; i < lijnen.size(); i++) {
            CompoundTag l = lijnen.getCompoundOrEmpty(i);
            boolean kop = l.getBooleanOr("h", false);
            if (kop) {
                cy += 3;
                g.fill(x + 6, cy - 1, x + pw - 6, cy, 0x40C0287C);
                cy += 2;
            }
            int kleur = l.getIntOr("k", 0xFF5A3A56);
            for (FormattedCharSequence s : gesplitst.get(i)) {
                if (cy + 9 > bodem - (m.contains("tijd") ? 10 : 0)) {
                    break;
                }
                g.text(font, s, x + 7, cy, kleur, l.getBooleanOr("v", false));
                cy += 10;
            }
        }
        if (m.contains("tijd")) {
            int tijd = m.getIntOr("tijd", 0), duur = Math.max(1, m.getIntOr("duur", 1));
            int by = bodem - 9;
            g.fill(x + 7, by, x + pw - 7, by + 5, 0xFFE8C8DC);
            g.fill(x + 7, by, x + 7 + (pw - 14) * tijd / duur, by + 5, tijd < duur / 4 ? 0xFFE04848 : ROZE);
            Component t = Component.translatable("vadsrijk.dungeon.klaar.tijd", (tijd + 19) / 20);
            g.text(font, t, x + pw - 7 - font.width(t), by - 10, 0xFF9A8094, false);
        }
        // the buttons, two per row
        int bw = (pw - 18) / 2;
        Component tip = null;
        for (int i = 0; i < knoppen.size(); i++) {
            CompoundTag k = knoppen.getCompoundOrEmpty(i);
            int bx = x + 6 + (i % 2) * (bw + 6), by = bodem + 2 + (i / 2) * 18;
            int w = knoppen.size() % 2 == 1 && i == knoppen.size() - 1 ? pw - 12 : bw;
            String id = k.getStringOr("id", "");
            knop(g, mx, my, bx, by, w, 15, tekst(k.getIntOr("t", -1)), k.getBooleanOr("aan", false), () -> doe(id));
            int ti = k.getIntOr("tip", -1);
            if (ti >= 0 && mx >= bx && mx < bx + w && my >= by && my < by + 15) {
                tip = tekst(ti);
            }
        }
        if (tip != null) {
            g.setTooltipForNextFrame(font, font.split(tip, 200), mx, my);
        }
    }

    private void knop(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int h, Component c, boolean actief, Runnable doe) {
        boolean over = actief && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, actief ? (over ? 0xFFC0287C : 0xFFE0489A) : 0xFFC8A8BC);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, actief ? (over ? 0xFFFF8CC8 : 0xFFFF6FB5) : 0xFFE4D0DC);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, actief ? 0x60FFFFFF : 0x30FFFFFF);
        String s = font.plainSubstrByWidth(c.getString(), w - 4);
        Component t = s.length() < c.getString().length() ? Component.literal(s).withStyle(c.getStyle()) : c;
        g.centeredText(font, t, x + w / 2, y + (h - 8) / 2, actief ? 0xFFFFFFFF : 0xFF9A8094);
        if (actief) {
            vlakken.add(new Vlak(x, y, w, h, doe));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dubbel) {
        if (e.button() == 0) {
            for (int i = vlakken.size() - 1; i >= 0; i--) {
                Vlak v = vlakken.get(i);
                if (e.x() >= v.x() && e.x() < v.x() + v.w() && e.y() >= v.y() && e.y() < v.y() + v.h()) {
                    v.doe().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(e, dubbel);
    }
}
