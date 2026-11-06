package nl.juiced.guhs.feature.vadsrijk.menu.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.client.BoekScherm;

/**
 * U-TABS: the Puzzels-tab of the Vadsrijk-menu: every puzzle, secret and hidden cuddly of ALL zones in one list (from the
 * Verkenningsboek's data: categories "puzzel", "geheim", "knuffel"), grouped per zone, with a filter row (all / open / done)
 * and the search box. Done = green tick + name; not found yet = "???" + its hint.
 */
public class PuzzelPaneel extends BoekScherm {
    private static final Set<String> CATS = Set.of("puzzel", "geheim", "knuffel", "grotten");   // V-INT: + u-grotten (only its puzzle lines, below)
    private static final int REGEL = 12;
    private int scroll, filter;   // filter: 0 all, 1 still open, 2 done

    public PuzzelPaneel(CompoundTag data) {
        super(data, Component.translatable("vadsrijk.menu.tab.puzzels"));
    }

    @Override
    protected String collectieTab() {
        return "puzzels";
    }

    /** One line of the list: a zone heading (d == null) or an entry. */
    private record Regel(Component kop, CompoundTag d, String icoon) {
    }

    private List<Regel> regels() {
        List<Regel> l = new ArrayList<>();
        ListTag zl = data.getListOrEmpty("zones");
        for (int z = 0; z < zl.size(); z++) {
            CompoundTag zt = zl.getCompoundOrEmpty(z);
            List<Regel> deze = new ArrayList<>();
            int klaar = 0, totaal = 0;
            ListTag cl = zt.getListOrEmpty("c");
            for (int c = 0; c < cl.size(); c++) {
                CompoundTag ct = cl.getCompoundOrEmpty(c);
                if (!CATS.contains(ct.getStringOr("id", ""))) {
                    continue;
                }
                ListTag dl = ct.getListOrEmpty("d");
                for (int i = 0; i < dl.size(); i++) {
                    CompoundTag d = dl.getCompoundOrEmpty(i);
                    if ("grotten".equals(ct.getStringOr("id", "")) && !d.getStringOr("n", "").startsWith("vadsrijk.grotten.boek.puzzel.")) {
                        continue;   // V-INT: of a cave only its puzzle (discovery/elite/treasure stay in the book)
                    }
                    boolean af = d.getByteOr("s", (byte) 0) == 2;
                    totaal++;
                    klaar += af ? 1 : 0;
                    if ((filter == 1 && af) || (filter == 2 && !af)) {
                        continue;
                    }
                    if (!past(tekst(d))) {
                        continue;
                    }
                    deze.add(new Regel(Component.translatable(ct.getStringOr("naam", "")), d, ct.getStringOr("icoon", "")));
                }
            }
            if (totaal > 0) {
                l.add(new Regel(Component.translatable(zt.getStringOr("naam", "")).append("  " + klaar + "/" + totaal), null, ""));
                l.addAll(deze);
            }
        }
        return l;
    }

    private static boolean geheim(CompoundTag d) {
        return d.getBooleanOr("g", false) && d.getByteOr("s", (byte) 0) == 0;
    }

    private static String tekst(CompoundTag d) {
        return (geheim(d) ? "???" : naam(d).getString()) + " " + (d.contains("h") ? hint(d).getString() : "");
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        // the filter row
        String[] keys = {"vadsrijk.menu.puzzels.alles", "vadsrijk.menu.puzzels.open", "vadsrijk.menu.puzzels.klaar"};
        int fx = left + 8;
        for (int i = 0; i < keys.length; i++) {
            Component c = Component.translatable(keys[i]);
            int bw = font.width(c) + 14;
            final int f = i;
            knop(g, mx, my, fx, top + 22, bw, 14, filter == i ? c.copy().withStyle(ChatFormatting.UNDERLINE) : c, filter != i, () -> {
                filter = f;
                scroll = 0;
            });
            fx += bw + 4;
        }
        g.text(font, Component.translatable("vadsrijk.menu.puzzels.uitleg"), fx + 6, top + 25, ZACHT, false);
        int x = left + 10, y0 = top + 42, onder = top + h - 8, lw = w - 26;
        g.fill(x - 4, y0 - 2, x + lw + 4, onder + 2, 0xFFFFF4FA);
        List<Regel> l = regels();
        if (data.getListOrEmpty("zones").isEmpty()) {
            g.text(font, Component.translatable("vadsrijk.menu.laden"), x, y0 + 4, ZACHT, false);
            return;
        }
        if (l.isEmpty()) {
            g.text(font, Component.translatable("vadsrijk.menu.zoek.niks"), x, y0 + 4, ZACHT, false);
            return;
        }
        int zicht = Math.max(1, (onder - y0) / REGEL), max = Math.max(0, l.size() - zicht);
        scroll = Math.max(0, Math.min(scroll, max));
        int y = y0;
        for (int i = scroll; i < l.size() && i < scroll + zicht; i++) {
            Regel r = l.get(i);
            if (r.d() == null) {
                g.fill(x - 2, y + REGEL - 2, x + lw + 2, y + REGEL - 1, 0xFFFFB0D8);
                g.text(font, r.kop().copy().withStyle(ChatFormatting.BOLD), x, y + 2, TEKST, false);
                y += REGEL;
                continue;
            }
            CompoundTag d = r.d();
            int s = d.getByteOr("s", (byte) 0);
            boolean af = s == 2, bezig = s == 1, verborgen = geheim(d);
            g.pose().pushMatrix();
            g.pose().translate(x + 4, y + 1);
            g.pose().scale(0.625f, 0.625f);
            g.item(BoekScherm.icoon(r.icoon()), 0, 0);
            g.pose().popMatrix();
            g.fill(x + 16, y + 2, x + 24, y + 10, af ? GROEN : bezig ? 0xFFE0A020 : 0xFFD8C8D2);
            g.fill(x + 17, y + 3, x + 23, y + 9, af ? 0xFF7AD89A : bezig ? 0xFFFFD86A : 0xFFFFF8EC);
            if (af) {
                g.text(font, "✔", x + 17, y + 2, 0xFF14562E, false);
            }
            Component naam = verborgen ? Component.literal("???") : naam(d);
            String nt = font.plainSubstrByWidth(naam.getString(), lw / 2 - 30);
            g.text(font, nt, x + 28, y + 2, af ? 0xFF1F7A44 : verborgen ? 0xFFA898A4 : TEKST, false);
            if (!af && d.contains("h")) {
                String ht = font.plainSubstrByWidth(hint(d).getString(), lw - (28 + font.width(nt) + 10));
                g.text(font, ht, x + 28 + font.width(nt) + 8, y + 2, ZACHT, false);
            }
            List<Component> tip = new ArrayList<>();
            tip.add(naam.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            tip.add(r.kop().copy().withStyle(ChatFormatting.DARK_GRAY));
            if (d.contains("h") && !af) {
                tip.add(hint(d).copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            }
            tip(mx, my, x, y, lw, REGEL, tip);
            y += REGEL;
        }
        if (max > 0) {
            int sh = onder - y0, bh = Math.max(8, sh * zicht / l.size()), by = y0 + (sh - bh) * scroll / max;
            g.fill(x + lw + 6, y0, x + lw + 8, onder, 0xFFF0D8E4);
            g.fill(x + lw + 6, by, x + lw + 8, by + bh, 0xFFFF5FAF);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = Math.max(0, scroll + (dy > 0 ? -3 : dy < 0 ? 3 : 0));
        return true;
    }
}
