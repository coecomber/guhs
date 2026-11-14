package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.Opruimen;

/**
 * 1.10.1 "Opruimen": every piece of Vadsrijk gear in your bag with its verdict (better/worse than what you wear), what it is worth,
 * what salvaging gives, a lock button, and Ontmantel (+ Verkoop at the smid). On top the two "alle slechtere" buttons (with a
 * confirm window that lists the counts); at the smid also the melt recipes at the bottom. Blue and up asks before salvaging.
 */
public class OpruimScherm extends EcoScherm {
    private static final int RIJ = 22;
    private int scroll;

    public OpruimScherm(CompoundTag data, List<ItemStack> items) {
        super(EcoPayloads.OPRUIM, Component.translatable(Opruimen.K + (data.getBooleanOr("verkoopt", false) ? "titel_smid" : "titel")), data, items, 330, data.getBooleanOr("smelt", false) ? 250 : 236);
    }

    private boolean smid() {
        return data.getBooleanOr("smelt", false);
    }

    private int zicht() {
        return smid() ? 5 : 7;
    }

    @Override
    protected void opNieuweData() {
        scroll = Math.min(scroll, Math.max(0, data.getListOrEmpty("gear").size() - zicht()));
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        boolean verkoopt = data.getBooleanOr("verkoopt", false);
        int y = top + 22;
        CompoundTag sam = data.getCompoundOrEmpty("slechter");
        int n = sam.getIntOr("aantal", 0);
        int bw = verkoopt ? (w - 20) / 2 : w - 16;
        knop(g, mx, my, left + 8, y, bw, 14, Component.translatable("vadsrijk.economie.winkel.ontmantel_slechter", n), n > 0,
                () -> vraag(vraagSlechter(sam, false), () -> doe("ontmantelslechter")));
        if (verkoopt) {
            knop(g, mx, my, left + 12 + bw, y, bw, 14, Component.translatable("vadsrijk.economie.winkel.verkoop_slechter", n), n > 0,
                    () -> vraag(vraagSlechter(sam, true), () -> doe("verkoopslechter")));
        }
        y += 18;
        g.text(font, Component.translatable(Opruimen.K + "uitleg"), left + 8, y, ZACHT, false);
        y += 11;
        ListTag l = data.getListOrEmpty("gear");
        if (l.isEmpty()) {
            g.centeredText(font, Component.translatable(Opruimen.K + "leeg"), left + w / 2, y + 30, ZACHT);
        }
        int zicht = zicht();
        for (int i = scroll; i < Math.min(l.size(), scroll + zicht); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            int ry = y + (i - scroll) * RIJ;
            g.fill(left + 6, ry, left + w - 6, ry + RIJ - 2, binnen(mx, my, left + 6, ry, w - 12, RIJ - 2) ? HOVER : (i % 2 == 0 ? RIJ_A : RIJ_B));
            rij(g, mx, my, t, ry, verkoopt);
        }
        if (l.size() > zicht) {
            int bh = zicht * RIJ - 2;
            int kh = Math.max(12, bh * zicht / l.size());
            int ky = y + (bh - kh) * scroll / Math.max(1, l.size() - zicht);
            g.fill(left + w - 5, y, left + w - 2, y + bh, 0x40FF5FAF);
            g.fill(left + w - 5, ky, left + w - 2, ky + kh, RAND);
        }
        // your salvage material (+ at the smid: what it melts into)
        int my0 = top + h - (smid() ? 84 : 30);
        kop(g, Component.translatable(Opruimen.K + "jouw_materiaal"), left + 8, my0 + 4);
        int x = left + 8 + font.width(Component.translatable(Opruimen.K + "jouw_materiaal")) + 10;
        x = materiaal(g, mx, my, x, my0, new ItemStack(EcoSpul.SMEEDSTOFJE.get()), data.getIntOr("stofjes", 0));
        x = materiaal(g, mx, my, x, my0, new ItemStack(EcoSpul.SMEEDSPLINTER.get()), data.getIntOr("splinters", 0));
        materiaal(g, mx, my, x, my0, new ItemStack(EcoSpul.SMEEDKRISTAL.get()), data.getIntOr("kristallen", 0));
        if (smid()) {
            int sy = my0 + 20;
            kop(g, Component.translatable(Opruimen.K + "omsmelten"), left + 8, sy + 5);
            ListTag sm = data.getListOrEmpty("omsmelten");
            int sx = left + 70;
            for (int i = 0; i < sm.size(); i++) {
                CompoundTag t = sm.getCompoundOrEmpty(i);
                int cx = sx + (i % 3) * 84, cy = sy + (i / 3) * 20;
                g.text(font, t.getIntOr("n", 1) + "x", cx, cy + 5, TEKST, false);
                slot(g, mx, my, cx + 12, cy, item(t.getIntOr("in", -1)), false);
                g.text(font, "→", cx + 32, cy + 5, TEKST, false);
                slot(g, mx, my, cx + 40, cy, item(t.getIntOr("uit", -1)), false);
                String id = t.getStringOr("id", "");
                knop(g, mx, my, cx + 60, cy + 2, 20, 14, Component.literal("✓"), t.getBooleanOr("kan", false), () -> {
                    CompoundTag a = new CompoundTag();
                    a.putString("id", id);
                    doe("omsmelt", a);
                });
            }
            knop(g, mx, my, left + w - 100, my0 + 3, 90, 12, Component.translatable(Opruimen.K + "knop.terug"), true, () -> doe("terug"));
        }
    }

    private void rij(GuiGraphicsExtractor g, int mx, int my, CompoundTag t, int ry, boolean verkoopt) {
        ItemStack s = item(t.getIntOr("item", -1));
        slot(g, mx, my, left + 8, ry + 1, s, false);
        String naam = s.getHoverName().getString();
        int ruimte = 100;
        if (font.width(naam) > ruimte) {
            naam = font.plainSubstrByWidth(naam, ruimte - font.width("...")) + "...";
        }
        g.text(font, naam, left + 30, ry + 2, TEKST, false);
        int o = t.getIntOr("oordeel", Opruimen.GEEN_OORDEEL);
        boolean hand = t.getBooleanOr("hand", false);
        String ok = hand ? "hand" : o == 1 ? "beter" : o == -1 ? "slechter" : o == 0 ? "gelijk" : o == Opruimen.LEEG ? "leeg" : "geen";
        int kleur = hand ? ZACHT : o == 1 || o == Opruimen.LEEG ? GROEN : o == -1 ? ROOD : ZACHT;
        g.text(font, Component.translatable(Opruimen.K + "oordeel." + ok), left + 30, ry + 11, kleur, false);
        // what it is worth / gives
        munten(g, left + 186, ry + 2, t.getLongOr("waarde", 0), true, false);
        ItemStack uit = item(t.getIntOr("opbrengst", -1));
        g.pose().pushMatrix();
        g.pose().translate(left + 160, ry + 11);
        g.pose().scale(0.5f, 0.5f);
        g.item(uit, 0, 0);
        g.pose().popMatrix();
        g.text(font, "x" + uit.getCount(), left + 170, ry + 11, TEKST, false);
        tip(mx, my, left + 158, ry + 10, 30, 10, List.of(Component.translatable(Opruimen.K + "tooltip.ontmantel", uit.getCount() + "x " + uit.getHoverName().getString())));
        // the buttons: lock, salvage, (sell)
        int slot = t.getIntOr("slot", -1);
        boolean op = t.getBooleanOr("slot_op", false);
        int bx = left + w - 10;
        if (verkoopt) {
            knop(g, mx, my, bx - 44, ry + 2, 44, 16, Component.translatable(Opruimen.K + "knop.verkoop"), !op && !hand, () -> actie("verkoop", slot, false));
            bx -= 48;
        }
        boolean kostbaar = t.getBooleanOr("kostbaar", false);
        knop(g, mx, my, bx - 56, ry + 2, 56, 16, Component.translatable(Opruimen.K + "knop.ontmantel"), !op && !hand, () -> {
            if (kostbaar) {
                vraag(List.of(Component.translatable(Opruimen.K + "vraag.een", s.getHoverName()).withStyle(ChatFormatting.BOLD),
                        Component.translatable(Opruimen.K + "tooltip.ontmantel", uit.getCount() + "x " + uit.getHoverName().getString())),
                        () -> actie("ontmantel", slot, true));
            } else {
                actie("ontmantel", slot, false);
            }
        });
        bx -= 60;
        knop(g, mx, my, bx - 18, ry + 2, 18, 16, Component.literal(op ? "🔒" : "🔓"), true, () -> actie("vergrendel", slot, false));
        tip(mx, my, bx - 18, ry + 2, 18, 16, List.of(Component.translatable(Opruimen.K + (op ? "knop.van_slot" : "knop.op_slot"))));
    }

    private void actie(String a, int slot, boolean ok) {
        CompoundTag t = new CompoundTag();
        t.putInt("slot", slot);
        t.putBoolean("ok", ok);
        doe(a, t);
    }

    private int materiaal(GuiGraphicsExtractor g, int mx, int my, int x, int y, ItemStack s, int n) {
        slot(g, mx, my, x, y, s, false);
        g.text(font, "x" + n, x + 20, y + 5, TEKST, false);
        return x + 20 + font.width("x" + n) + 8;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, data.getListOrEmpty("gear").size() - zicht());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        return true;
    }
}
