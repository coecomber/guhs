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

    /** The tab row; returns the y under it. */
    public static int tabRij(Brug b, GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int tab, IntConsumer kies) {
        int bw = w / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int tx = x + i * bw;
            boolean aan = i == tab;
            boolean hover = mx >= tx && mx < tx + bw - 1 && my >= y && my < y + 11;
            g.fill(tx, y, tx + bw - 1, y + 11, aan ? 0xFFFF7FC0 : hover ? 0xFFFFB8DC : 0xFFFFD6EB);
            Component c = Component.translatable("vadsrijk.items.smid.tab." + TABS[i]);
            g.centeredText(b.font(), c, tx + bw / 2, y + 2, aan ? 0xFFFFFFFF : TEKST);
            final int t = i;
            b.klikvlak(tx, y, bw - 1, 11, () -> kies.accept(t));
        }
        return y + 14;
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
        g.text(f, k, x, y, GROEN, false);
        y += 10;
        int tot = (int) ItemsBalans.get("pity_tot_ster");
        if (u.sterren() < tot) {
            // "Smidguh wordt ongeduldig..." meter: one notch per failure, full at +50 %
            int n = u.erf().pity();
            g.text(f, Component.translatable(n > 0 ? "vadsrijk.items.smid.geduld" : "vadsrijk.items.smid.geduld_leeg"), x, y, ZACHT, false);
            int mw = 50, mx0 = x + w - mw;
            g.fill(mx0, y + 1, mx0 + mw, y + 7, 0xFFFFD6EB);
            g.fill(mx0, y + 1, mx0 + Math.min(mw, n * mw / 10), y + 7, 0xFFF0A800);
            y += 10;
        } else {
            g.text(f, Component.translatable("vadsrijk.items.smid.geen_geduld", tot), x, y, ZACHT, false);
            y += 10;
        }
        CompoundTag d = b.data();
        int heb = d.getIntOr("talismannen", 0);
        boolean aan = d.getBooleanOr("talisman_aan", false);
        double[] risico = SterRegels.risico(u.sterren());
        if (risico[0] > 0 || heb > 0) {
            Component vink = Component.literal(aan ? "☑ " : "☐ ").append(Component.translatable("vadsrijk.items.smid.talisman", heb));
            int vw = f.width(vink);
            g.text(f, vink, x, y, aan ? PAARS : ZACHT, false);
            b.klikvlak(x, y - 1, vw, 10, () -> {
                CompoundTag t = new CompoundTag();
                t.putInt("aan", aan ? 0 : 1);
                b.doe("talisman", t);
            });
            long prijs = Vangnet.talismanPrijs(u.level());
            b.knop(g, mx, my, x + w - 46, y - 2, 46, 11, Component.translatable("vadsrijk.items.smid.koop"), d.getLongOr("munten", 0) >= prijs,
                    () -> b.doe("koop_talisman", slotArgs(b)));
            y += 11;
            if (aan && heb > 0 && risico[0] > 0) {
                g.text(f, Component.translatable("vadsrijk.items.smid.talisman_aan"), x, y, PAARS, false);
                y += 10;
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
        g.text(b.font(), Component.translatable("vadsrijk.items.smid.bij_succes"), x, y, ZACHT, false);
        y += 10;
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
            g.text(f, Component.translatable("vadsrijk.items.smid.polijst_kies"), x, y, ZACHT, false);
            return y + 10;
        }
        Bonus bo = u.bonussen().get(gekozen);
        double[] r = Bonussen.bereik(bo.stat(), u.level());
        double budget = Rollen.budget(u);
        g.text(f, Component.translatable("vadsrijk.items.smid.polijst_bereik", Component.translatable(bo.stat().key()),
                getal(bo.stat(), r[0] * budget), getal(bo.stat(), r[1] * budget), getal(bo.stat(), bo.waarde() * budget)), x, y, TEKST, false);
        y += 11;
        long prijs = Erfstukken.polijstPrijs(u);
        b.munten(g, x + w, y + 3, prijs);
        b.knop(g, mx, my, x, y, w - 50, 14, Component.translatable("vadsrijk.items.smid.knop.polijst"), b.data().getLongOr("munten", 0) >= prijs, () -> {
            CompoundTag t = slotArgs(b);
            t.putInt("bonus", gekozen);
            b.doe("polijst", t);
        });
        return y + 17;
    }

    // ---- Doorgroeien ------------------------------------------------------------------------------------------------------

    public static void doorgroei(Brug b, GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, Uitrusting u) {
        Font f = b.font();
        g.text(f, Component.translatable("vadsrijk.items.smid.doorgroei_uitleg"), x, y, ZACHT, false);
        y += 10;
        g.text(f, Component.translatable("vadsrijk.items.smid.vertrouwd", Erfstukken.vertrouwd(u), Erfstukken.MAX_VERTROUWD), x, y,
                Erfstukken.vertrouwd(u) >= Erfstukken.MAX_VERTROUWD ? GROEN : ROOD, false);
        y += 10;
        String waarom = Erfstukken.kanDoorgroeien(u);
        if (!waarom.isEmpty()) {
            for (var r : f.split(Component.translatable(waarom), w)) {
                g.text(f, r, x, y, ROOD, false);
                y += 9;
            }
            return;
        }
        Uitrusting na = Erfstukken.doorgroei(u);
        g.text(f, Component.translatable("vadsrijk.items.smid.level_van_naar", u.level(), na.level()).withStyle(ChatFormatting.BOLD), x, y, GOUD, false);
        y += 11;
        for (Component c : verschil(u, na).subList(0, Math.min(5, verschil(u, na).size()))) {
            g.text(f, c, x + 6, y, GROEN, false);
            y += 9;
        }
        g.text(f, Component.translatable("vadsrijk.items.smid.doorgroei_gevolg"), x, y, ZACHT, false);
        y += 11;
        int pluis = Erfstukken.groeipluis(u), heb = b.data().getIntOr("groeipluis", 0);
        g.item(new ItemStack(ItemsRegistry.GROEIPLUIS.get()), x, y - 2);
        g.text(f, Component.literal(heb + " / " + pluis), x + 18, y + 2, heb >= pluis ? GROEN : ROOD, false);
        long prijs = Erfstukken.doorgroeiPrijs(u);
        b.munten(g, x + w, y + 2, prijs);
        y += 16;
        b.knop(g, mx, my, x, y, w, 16, Component.translatable("vadsrijk.items.smid.knop.doorgroei").withStyle(ChatFormatting.BOLD),
                heb >= pluis && b.data().getLongOr("munten", 0) >= prijs, () -> b.doe("doorgroei", slotArgs(b)));
    }

    // ---- Overvadsen -------------------------------------------------------------------------------------------------------

    public static void overvads(Brug b, GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, Uitrusting u) {
        Font f = b.font();
        if (voorKies != b.kies()) {
            voorKies = b.kies();
            offer = -1;
            offerBonus = -1;
            doelBonus = -1;
        }
        g.text(f, Component.translatable("vadsrijk.items.smid.overvads_uitleg"), x, y, ZACHT, false);
        y += 10;
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
            if (n < 10) {
                int sx = x + n * 19;
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
            g.text(f, Component.translatable("vadsrijk.items.smid.overvads_geen"), x, y + 4, ROOD, false);
            return;
        }
        y += 21;
        if (o == null) {
            g.text(f, Component.translatable("vadsrijk.items.smid.overvads_kies_offer"), x, y, ZACHT, false);
            return;
        }
        // 2. which bonus of the old piece
        int half = w / 2 - 2;
        g.text(f, Component.translatable("vadsrijk.items.smid.overvads_van"), x, y, TEKST, false);
        g.text(f, Component.translatable("vadsrijk.items.smid.overvads_naar"), x + half + 4, y, TEKST, false);
        y += 10;
        for (int i = 0; i < o.bonussen().size(); i++) {
            Bonus bo = o.bonussen().get(i);
            int by = y + i * 10;
            final int idx = i;
            g.text(f, Component.literal(i == offerBonus ? "» " : "• ").append(Component.translatable(bo.stat().key())), x, by, i == offerBonus ? GOUD : TEKST, false);
            b.klikvlak(x, by - 1, half, 10, () -> offerBonus = idx);
        }
        // 3. which bonus of the target it replaces (or a new one)
        int ty = y;
        for (int j = -1; j < u.bonussen().size(); j++) {
            if (j < 0 && u.bonussen().size() >= u.zeldzaamheid().maxBonus) {
                continue;
            }
            Component c = j < 0 ? Component.translatable("vadsrijk.items.smid.overvads_erbij") : Component.translatable(u.bonussen().get(j).stat().key());
            final int idx = j;
            g.text(f, Component.literal(j == doelBonus ? "» " : "• ").append(c), x + half + 4, ty, j == doelBonus ? GOUD : TEKST, false);
            b.klikvlak(x + half + 4, ty - 1, half, 10, () -> doelBonus = idx);
            ty += 10;
        }
        y = Math.max(y + o.bonussen().size() * 10, ty) + 3;
        if (offerBonus < 0) {
            return;
        }
        String waarom = Erfstukken.kanOvervadsen(u, o, offerBonus, doelBonus);
        if (!waarom.isEmpty()) {
            g.text(f, Component.translatable(waarom), x, y, ROOD, false);
            return;
        }
        Uitrusting na = Erfstukken.overvads(u, o, offerBonus, doelBonus, false), lief = Erfstukken.overvads(u, o, offerBonus, doelBonus, true);
        Bonus nb = doelBonus < 0 ? na.bonussen().get(na.bonussen().size() - 1) : na.bonussen().get(doelBonus);
        Bonus lb = doelBonus < 0 ? lief.bonussen().get(lief.bonussen().size() - 1) : lief.bonussen().get(doelBonus);
        double budget = Rollen.budget(u);
        g.text(f, Component.translatable("vadsrijk.items.smid.overvads_wordt", Component.translatable(nb.stat().key()), getal(nb.stat(), nb.waarde() * budget),
                pct(ItemsBalans.get("overvads_mislukking")), getal(lb.stat(), lb.waarde() * budget)), x, y, GROEN, false);
        y += 10;
        g.text(f, Component.translatable("vadsrijk.items.smid.overvads_weg"), x, y, ROOD, false);
        y += 11;
        long prijs = Erfstukken.overvadsPrijs(u);
        b.munten(g, x + w, y + 3, prijs);
        b.knop(g, mx, my, x, y, w - 50, 15, Component.translatable("vadsrijk.items.smid.knop.overvads"), b.data().getLongOr("munten", 0) >= prijs, () -> {
            CompoundTag t = slotArgs(b);
            t.putInt("offer", offer);
            t.putInt("bonus", offerBonus);
            t.putInt("doel", doelBonus);
            b.doe("overvads", t);
            offer = -1;
            offerBonus = -1;
            doelBonus = -1;
        });
    }

    private static CompoundTag slotArgs(Brug b) {
        CompoundTag t = new CompoundTag();
        t.putInt("slot", b.kies());
        return t;
    }
}
