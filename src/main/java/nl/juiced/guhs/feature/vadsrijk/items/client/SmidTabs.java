package nl.juiced.guhs.feature.vadsrijk.items.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.SterRegels;
import nl.juiced.guhs.feature.vadsrijk.items.Erfstukken;
import nl.juiced.guhs.feature.vadsrijk.items.ItemsBalans;
import nl.juiced.guhs.feature.vadsrijk.items.ItemsRegistry;
import nl.juiced.guhs.feature.vadsrijk.items.Vangnet;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonus;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * T-ITEMS: what the smid screen ({@code economie.client.SmidScherm}) draws for itemisatie 2.0, through a small bridge so the
 * screen keeps its own look. Tabs: <b>Sterren</b> (the old star part + pity meter, talisman switch, previews), <b>Bonussen</b>
 * (the old reroll + polijsten), <b>Doorgroeien</b>, <b>Overvadsen</b>. Every tab shows before/after and the chances before you
 * click (R2: previews).
 */
public final class SmidTabs {
    public static final int STERREN = 0, BONUSSEN = 1, DOORGROEIEN = 2, OVERVADSEN = 3;
    private static final String[] TABS = {"sterren", "bonussen", "doorgroeien", "overvadsen"};
    public static final int TEKST = 0xFF4A1E46, ZACHT = 0xFF8A5A84, GOUD = 0xFFB07800, ROOD = 0xFFD0306A, GROEN = 0xFF2E9E5A, PAARS = 0xFF8A3AC0;

    /** Overvadsen state: the inventory slot of the offered piece, its bonus, the target bonus (-1 = add). */
    private static int offer = -1, offerBonus = -1, doelBonus = -1, voorKies = -2;

    /** What SmidScherm lends us (its protected drawing helpers). */
    public interface Brug {
        Font font();

        CompoundTag data();

        ItemStack item(int i);

        int kies();

        void knop(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int h, Component label, boolean actief, Runnable actie);

        void klikvlak(int x, int y, int w, int h, Runnable actie);

        void slot(GuiGraphicsExtractor g, int mx, int my, int x, int y, ItemStack s, boolean gekozen);

        int munten(GuiGraphicsExtractor g, int x, int y, long brons);

        void doe(String actie, CompoundTag args);
    }

    private SmidTabs() {
    }

    /** 1.10.24: the height of a tab in the row (was 11: the text touched the edges). */
    public static final int TAB_H = 15;

    /** The tab row; returns the y under it. 1.10.24: taller tabs, a label that does not fit is cut off (never over the next tab). */
    public static int tabRij(Brug b, GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int tab, IntConsumer kies) {
        int bw = w / TABS.length;
        Font f = b.font();
        for (int i = 0; i < TABS.length; i++) {
            int tx = x + i * bw;
            boolean aan = i == tab;
            boolean hover = mx >= tx && mx < tx + bw - 2 && my >= y && my < y + TAB_H;
            g.fill(tx, y, tx + bw - 2, y + TAB_H, aan ? 0xFFFF5FAF : hover ? 0xFFFFB8DC : 0xFFFFD6EB);
            if (aan) {
                g.fill(tx, y + TAB_H - 2, tx + bw - 2, y + TAB_H, 0xFFC0287C);
            }
            Component c = Component.translatable("vadsrijk.items.smid.tab." + TABS[i]);
            var r = kort(f, c, bw - 6);
            g.text(f, r, tx + (bw - 2 - f.width(r)) / 2, y + (TAB_H - 8) / 2, aan ? 0xFFFFFFFF : TEKST, false);
            final int t = i;
            b.klikvlak(tx, y, bw - 2, TAB_H, () -> kies.accept(t));
        }
        return y + TAB_H + 5;
    }

    /** 1.10.24: a text cut to width {@code w} with an ellipsis. */
    public static net.minecraft.util.FormattedCharSequence kort(Font f, Component c, int w) {
        if (f.width(c) <= w) {
            return c.getVisualOrderText();
        }
        var deel = f.substrByWidth(c, Math.max(4, w - f.width("…")));
        return net.minecraft.util.FormattedCharSequence.composite(net.minecraft.locale.Language.getInstance().getVisualOrder(deel),
                Component.literal("…").getVisualOrderText());
    }

    /**
     * 1.10.15: a text wrapped to {@code w} (the Dutch lines of the smid were longer than the column and ran over the prices and the
     * edge). 9 px per line + 1 under it, so one line takes the old 10. Returns the y under it.
     */
    public static int regels(Font f, GuiGraphicsExtractor g, Component c, int x, int y, int w, int kleur) {
        for (var r : f.split(c, Math.max(20, w))) {
            g.text(f, r, x, y, kleur, false);
            y += 9;
        }
        return y + 1;
    }

    private static String pct(double d) {
        return Math.round(d * 1000) / 10.0 + "%";
    }

    private static String getal(Stat s, double v) {
        return s.procent ? String.format(Locale.ROOT, "%.1f%%", v) : String.format(Locale.ROOT, "%.0f", v);
    }

    // ---- Sterren ----------------------------------------------------------------------------------------------------------

    /** The chance line with pity, the patience meter, the talisman switch + buy button. Returns the new y. */
    public static int sterBoven(Brug b, GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, Uitrusting u) {
        Font f = b.font();
        double basis = SterRegels.KANS[u.sterren()], kans = Vangnet.kans(u);
        MutableComponent k = Component.translatable("vadsrijk.economie.smid.kans", pct(kans));
        if (kans > basis) {
            k.append(Component.translatable("vadsrijk.items.smid.met_geduld", pct(kans - basis)).withStyle(ChatFormatting.GOLD));
        }
        y = regels(f, g, k, x, y, w, GROEN);
        int tot = (int) ItemsBalans.get("pity_tot_ster");
        if (u.sterren() < tot) {
            // "Smidguh wordt ongeduldig..." meter: one notch per failure, full at +50 %
            int n = u.erf().pity();
            int mw = 50, mx0 = x + w - mw;
            g.fill(mx0, y + 1, mx0 + mw, y + 7, 0xFFFFD6EB);
            g.fill(mx0, y + 1, mx0 + Math.min(mw, n * mw / 10), y + 7, 0xFFF0A800);
            y = regels(f, g, Component.translatable(n > 0 ? "vadsrijk.items.smid.geduld" : "vadsrijk.items.smid.geduld_leeg"), x, y, w - mw - 6, ZACHT);
        } else {
            y = regels(f, g, Component.translatable("vadsrijk.items.smid.geen_geduld", tot), x, y, w, ZACHT);
        }
        CompoundTag d = b.data();
        int heb = d.getIntOr("talismannen", 0);
        boolean aan = d.getBooleanOr("talisman_aan", false);
        double[] risico = SterRegels.risico(u.sterren());
        if (risico[0] > 0 || heb > 0) {
            Component vink = Component.literal(aan ? "☑ " : "☐ ").append(Component.translatable("vadsrijk.items.smid.talisman", heb));
            // 1.10.24: the buy button gets its own room on the right; the line is cut before it (it ran under the button)
            int kw = Math.max(50, f.width(Component.translatable("vadsrijk.items.smid.koop")) + 12);
            var vr = kort(f, vink, w - kw - 6);
            int vw = f.width(vr);
            g.text(f, vr, x, y + 3, aan ? PAARS : ZACHT, false);
            b.klikvlak(x, y, vw, 14, () -> {
                CompoundTag t = new CompoundTag();
                t.putInt("aan", aan ? 0 : 1);
                b.doe("talisman", t);
            });
            long prijs = Vangnet.talismanPrijs(u.level());
            b.knop(g, mx, my, x + w - kw, y, kw, 14, Component.translatable("vadsrijk.items.smid.koop"), d.getLongOr("munten", 0) >= prijs,
                    () -> b.doe("koop_talisman", slotArgs(b)));
            y += 17;
            if (aan && heb > 0 && risico[0] > 0) {
                y = regels(f, g, Component.translatable("vadsrijk.items.smid.talisman_aan"), x, y, w, PAARS);
            }
        }
        return y;
    }

    /** "Bij succes: ..." under the star button: what the next star changes. Returns the new y. */
    public static int sterVoorbeeld(Brug b, GuiGraphicsExtractor g, int x, int y, int w, Uitrusting u) {
        if (u.sterren() >= SterRegels.MAX || u.verpluisd()) {
            return y;
        }
        Uitrusting na = u.metSterren(u.sterren() + 1);
        List<Component> l = verschil(u, na);
        y = regels(b.font(), g, Component.translatable("vadsrijk.items.smid.bij_succes"), x, y, w, ZACHT);
        for (Component c : l.subList(0, Math.min(3, l.size()))) {
            g.text(b.font(), c, x + 6, y, GROEN, false);
            y += 9;
        }
        return y;
    }

    /** "Kracht 12 -> 14" lines for every stat that changes between two versions of a piece (+ weapon damage). */
    public static List<Component> verschil(Uitrusting voor, Uitrusting na) {
        List<Component> l = new ArrayList<>();
        if (voor.basisType() != null && voor.basisType().slot() == Slot.WAPEN) {
            double a = Rollen.klapSchade(voor), c = Rollen.klapSchade(na);
            if (Math.abs(c - a) >= 0.5) {
                l.add(Component.translatable("vadsrijk.items.smid.schade_van_naar", String.format(Locale.ROOT, "%.0f", a), String.format(Locale.ROOT, "%.0f", c)));
            }
        }
        Stats a = Rollen.stats(voor), c = Rollen.stats(na);
        for (Stat s : Stat.values()) {
            double va = a.get(s), vc = c.get(s);
            if (Math.abs(vc - va) >= (s.procent ? 0.05 : 0.5)) {
                l.add(Component.translatable(s.key()).append(" " + getal(s, va) + " → " + getal(s, vc)));
            }
        }
        return l;
    }

    // ---- Bonussen: polijsten ----------------------------------------------------------------------------------------------

    /** Under the bonus list: polish the selected bonus (range + price). Returns the new y. */
    public static int polijst(Brug b, GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, Uitrusting u, int gekozen) {
        Font f = b.font();
        if (gekozen < 0 || gekozen >= u.bonussen().size()) {
            return regels(f, g, Component.translatable("vadsrijk.items.smid.polijst_kies"), x, y, w, ZACHT);
        }
        Bonus bo = u.bonussen().get(gekozen);
        double[] r = Bonussen.bereik(bo.stat(), u.level());
        double budget = Rollen.budget(u);
        y = regels(f, g, Component.translatable("vadsrijk.items.smid.polijst_bereik", Component.translatable(bo.stat().key()),
                getal(bo.stat(), r[0] * budget), getal(bo.stat(), r[1] * budget), getal(bo.stat(), bo.waarde() * budget)), x, y, w, TEKST) + 1;
        long prijs = Erfstukken.polijstPrijs(u);
        int mb = b.munten(g, x + w, y + 5, prijs);   // 1.10.24: the price first; the button stops before it (it ran under the coins)
        b.knop(g, mx, my, x, y, w - mb - 8, 17, Component.translatable("vadsrijk.items.smid.knop.polijst"), b.data().getLongOr("munten", 0) >= prijs, () -> {
            CompoundTag t = slotArgs(b);
            t.putInt("bonus", gekozen);
            b.doe("polijst", t);
        });
        return y + 21;
    }

    // ---- Doorgroeien ------------------------------------------------------------------------------------------------------

    /** Returns the y under it (1.10.24: the smid scrolls a tab that is taller than its panel). */
    public static int doorgroei(Brug b, GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, Uitrusting u) {
        Font f = b.font();
        y = regels(f, g, Component.translatable("vadsrijk.items.smid.doorgroei_uitleg"), x, y, w, ZACHT);
        y = regels(f, g, Component.translatable("vadsrijk.items.smid.vertrouwd", Erfstukken.vertrouwd(u), Erfstukken.MAX_VERTROUWD), x, y, w,
                Erfstukken.vertrouwd(u) >= Erfstukken.MAX_VERTROUWD ? GROEN : ROOD);
        String waarom = Erfstukken.kanDoorgroeien(u);
        if (!waarom.isEmpty()) {
            for (var r : f.split(Component.translatable(waarom), w)) {
                g.text(f, r, x, y, ROOD, false);
                y += 9;
            }
            return y;
        }
        Uitrusting na = Erfstukken.doorgroei(u);
        g.text(f, Component.translatable("vadsrijk.items.smid.level_van_naar", u.level(), na.level()).withStyle(ChatFormatting.BOLD), x, y, GOUD, false);
        y += 11;
        for (Component c : verschil(u, na).subList(0, Math.min(5, verschil(u, na).size()))) {
            g.text(f, c, x + 6, y, GROEN, false);
            y += 9;
        }
        y = regels(f, g, Component.translatable("vadsrijk.items.smid.doorgroei_gevolg"), x, y, w, ZACHT) + 1;
        int pluis = Erfstukken.groeipluis(u), heb = b.data().getIntOr("groeipluis", 0);
        g.item(new ItemStack(ItemsRegistry.GROEIPLUIS.get()), x, y - 2);
        g.text(f, Component.literal(heb + " / " + pluis), x + 18, y + 2, heb >= pluis ? GROEN : ROOD, false);
        long prijs = Erfstukken.doorgroeiPrijs(u);
        b.munten(g, x + w, y + 2, prijs);
        y += 18;
        b.knop(g, mx, my, x, y, w, 18, Component.translatable("vadsrijk.items.smid.knop.doorgroei").withStyle(ChatFormatting.BOLD),
                heb >= pluis && b.data().getLongOr("munten", 0) >= prijs, () -> b.doe("doorgroei", slotArgs(b)));
        return y + 22;
    }

    // ---- Overvadsen -------------------------------------------------------------------------------------------------------

    /** Returns the y under it (1.10.24: the smid scrolls a tab that is taller than its panel). */
    public static int overvads(Brug b, GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, Uitrusting u) {
        Font f = b.font();
        if (voorKies != b.kies()) {
            voorKies = b.kies();
            offer = -1;
            offerBonus = -1;
            doelBonus = -1;
        }
        y = regels(f, g, Component.translatable("vadsrijk.items.smid.overvads_uitleg"), x, y, w, ZACHT);
        // 1. pick the old piece (any other piece in your bag with bonuses)
        ListTag l = b.data().getListOrEmpty("gear");
        int n = 0;
        Uitrusting o = null;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            int slot = t.getIntOr("slot", -1);
            ItemStack s = b.item(t.getIntOr("item", -1));
            Uitrusting x2 = Rollen.van(s);
            if (slot == b.kies() || x2 == null || x2.bonussen().isEmpty()) {
                continue;
            }
            if (n < Math.max(1, w / 20)) {
                int sx = x + n * 20;
                b.slot(g, mx, my, sx, y, s, slot == offer);
                b.klikvlak(sx, y, 18, 18, () -> {
                    offer = slot;
                    offerBonus = -1;
                });
            }
            if (slot == offer) {
                o = x2;
            }
            n++;
        }
        if (n == 0) {
            return regels(f, g, Component.translatable("vadsrijk.items.smid.overvads_geen"), x, y + 4, w, ROOD);
        }
        y += 22;
        if (o == null) {
            return regels(f, g, Component.translatable("vadsrijk.items.smid.overvads_kies_offer"), x, y, w, ZACHT);
        }
        // 2. which bonus of the old piece
        int half = w / 2 - 2;
        g.text(f, kort(f, Component.translatable("vadsrijk.items.smid.overvads_van"), half), x, y, TEKST, false);
        g.text(f, kort(f, Component.translatable("vadsrijk.items.smid.overvads_naar"), half), x + half + 4, y, TEKST, false);
        y += 12;
        for (int i = 0; i < o.bonussen().size(); i++) {
            Bonus bo = o.bonussen().get(i);
            int by = y + i * 12;
            final int idx = i;
            g.text(f, kort(f, Component.literal(i == offerBonus ? "» " : "• ").append(Component.translatable(bo.stat().key())), half), x, by, i == offerBonus ? GOUD : TEKST, false);
            b.klikvlak(x, by - 2, half, 12, () -> offerBonus = idx);
        }
        // 3. which bonus of the target it replaces (or a new one)
        int ty = y;
        for (int j = -1; j < u.bonussen().size(); j++) {
            if (j < 0 && u.bonussen().size() >= u.zeldzaamheid().maxBonus) {
                continue;
            }
            Component c = j < 0 ? Component.translatable("vadsrijk.items.smid.overvads_erbij") : Component.translatable(u.bonussen().get(j).stat().key());
            final int idx = j;
            g.text(f, kort(f, Component.literal(j == doelBonus ? "» " : "• ").append(c), half), x + half + 4, ty, j == doelBonus ? GOUD : TEKST, false);
            b.klikvlak(x + half + 4, ty - 2, half, 12, () -> doelBonus = idx);
            ty += 12;
        }
        y = Math.max(y + o.bonussen().size() * 12, ty) + 3;
        if (offerBonus < 0) {
            return y;
        }
        String waarom = Erfstukken.kanOvervadsen(u, o, offerBonus, doelBonus);
        if (!waarom.isEmpty()) {
            return regels(f, g, Component.translatable(waarom), x, y, w, ROOD);
        }
        Uitrusting na = Erfstukken.overvads(u, o, offerBonus, doelBonus, false), lief = Erfstukken.overvads(u, o, offerBonus, doelBonus, true);
        Bonus nb = doelBonus < 0 ? na.bonussen().get(na.bonussen().size() - 1) : na.bonussen().get(doelBonus);
        Bonus lb = doelBonus < 0 ? lief.bonussen().get(lief.bonussen().size() - 1) : lief.bonussen().get(doelBonus);
        double budget = Rollen.budget(u);
        y = regels(f, g, Component.translatable("vadsrijk.items.smid.overvads_wordt", Component.translatable(nb.stat().key()), getal(nb.stat(), nb.waarde() * budget),
                pct(ItemsBalans.get("overvads_mislukking")), getal(lb.stat(), lb.waarde() * budget)), x, y, w, GROEN);
        y = regels(f, g, Component.translatable("vadsrijk.items.smid.overvads_weg"), x, y, w, ROOD) + 1;
        long prijs = Erfstukken.overvadsPrijs(u);
        int mb = b.munten(g, x + w, y + 5, prijs);   // 1.10.24: the price first; the button stops before it
        b.knop(g, mx, my, x, y, w - mb - 8, 17, Component.translatable("vadsrijk.items.smid.knop.overvads"), b.data().getLongOr("munten", 0) >= prijs, () -> {
            CompoundTag t = slotArgs(b);
            t.putInt("offer", offer);
            t.putInt("bonus", offerBonus);
            t.putInt("doel", doelBonus);
            b.doe("overvads", t);
            offer = -1;
            offerBonus = -1;
            doelBonus = -1;
        });
        return y + 21;
    }

    private static CompoundTag slotArgs(Brug b) {
        CompoundTag t = new CompoundTag();
        t.putInt("slot", b.kies());
        return t;
    }
}
