package nl.juiced.guhs.feature.vadsrijk.ambacht.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;

/**
 * The Ambachten book (key Y, the collection tab, the board guhs): tabs Overzicht (9 tiles with level + xp bar, the food buff and
 * your rest), Recepten (per ambacht: what you know, what comes next as grey lines), Bestellingen (the Bestelbord), Ranglijst (the
 * Ambachtsbord: top 10 + your place) and Plekken (found spots per zone, fish, honey, recipes made).
 */
public class AmbachtScherm extends EcoScherm {
    private static final String[] TABS = {"overzicht", "recepten", "bestel", "opdracht", "winkel", "rang", "plekken"};
    private int scroll;
    /** U-AMBACHT2: the new-order panel of the Opdrachten tab (client-side until "Plaats"). */
    private int kies, aantal = 1;
    private long beloning = -1;

    public AmbachtScherm(CompoundTag data, List<ItemStack> items) {
        // (U-AMBACHT2: bigger, 7 tabs: + Opdrachten and the Knutselwinkel)
        super("ambacht_boek", Component.translatable("vadsrijk.ambacht.titel"), data, items, 400, 250);
    }

    @Override
    protected String collectieTab() {
        return AmbachtClient.TAB;
    }

    private String tab() {
        return data.getStringOr("tab", "overzicht");
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        ClientPacketDistributor.sendToServer(new AmbachtPayloads.Doe(actie, args));
    }

    private void open(String tab, String ambacht, String lijst) {
        CompoundTag a = new CompoundTag();
        a.putString("tab", tab);
        a.putString("ambacht", ambacht);
        a.putString("lijst", lijst);
        scroll = 0;
        doe("boek", a);
    }

    static ItemStack icoon(String id) {
        var i = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return i == null ? new ItemStack(Items.BOOK) : new ItemStack(i);
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int tx = left + 8, som = 0;
        for (String t : TABS) {
            som += font.width(Component.translatable("vadsrijk.ambacht.tab." + t)) + 3;
        }
        int pad = Math.max(4, Math.min(12, (w - 16 - som) / TABS.length));   // (all tabs always fit)
        for (String t : TABS) {
            boolean nu = t.equals(tab());
            Component naam = Component.translatable("vadsrijk.ambacht.tab." + t);
            int bw = font.width(naam) + pad;
            knop(g, mx, my, tx, top + 22, bw, 14, nu ? naam.copy().withStyle(ChatFormatting.UNDERLINE) : naam, !nu,
                    () -> open(t, data.getStringOr("ambacht", "mijnen"), ""));
            tx += bw + 3;
        }
        switch (tab()) {
            case "recepten" -> recepten(g, mx, my);
            case "bestel" -> bestel(g, mx, my);
            case "rang" -> rang(g, mx, my);
            case "plekken" -> plekken(g);
            case "opdracht" -> opdracht(g, mx, my);
            case "winkel" -> winkel(g, mx, my);
            default -> overzicht(g, mx, my);
        }
    }

    private void overzicht(GuiGraphicsExtractor g, int mx, int my) {
        ListTag l = data.getListOrEmpty("tegels");
        int bw = (w - 16) / 3, bh = 44;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            int x = left + 8 + (i % 3) * bw, y = top + 42 + (i / 3) * (bh + 3);
            g.fill(x, y, x + bw - 3, y + bh, (i % 2 == 0) ? RIJ_A : RIJ_B);
            slot(g, mx, my, x + 3, y + 3, icoon(t.getStringOr("icoon", "minecraft:book")), false);
            String id = t.getStringOr("id", "");
            g.text(font, Component.translatable("vadsrijk.ambacht.naam." + id).withStyle(ChatFormatting.BOLD), x + 24, y + 4, TEKST, false);
            int lv = t.getIntOr("level", 1);
            g.text(font, Component.translatable("vadsrijk.ambacht.lv", lv, data.getIntOr("cap", 60)), x + 24, y + 15, ZACHT, false);
            if (t.getBooleanOr("meester", false)) {
                Component m = Component.translatable("vadsrijk.ambacht.meester.kort").withStyle(ChatFormatting.BOLD);
                g.text(font, m, x + bw - 8 - font.width(m), y + 4, 0xFFB040C8, false);
            }
            long in = t.getLongOr("in", 0), nodig = t.getLongOr("nodig", 1);
            int bx = x + 4, bbw = bw - 12;
            g.fill(bx, y + 30, bx + bbw, y + 36, 0xFFE8C8DC);
            g.fill(bx, y + 30, bx + (int) (bbw * (nodig <= 0 ? 1 : Math.min(1.0, in / (double) nodig))), y + 36, t.getIntOr("kleur", 0xFFFF6FB5));
            tip(mx, my, bx, y + 28, bbw, 10, List.of(Component.translatable("vadsrijk.ambacht.xp_balk", in, nodig)));
            klikvlak(x, y, bw - 3, bh, () -> open("recepten", id, ""));
        }
        int y = top + 42 + 3 * (bh + 3) + 2;
        String buff = data.getStringOr("buff", "");
        Component b = buff.isEmpty() ? Component.translatable("vadsrijk.ambacht.boek.geen_buff")
                : Component.translatable("vadsrijk.ambacht.boek.buff", Component.translatable("item.guhs.vr_amb_eten_" + buff), data.getLongOr("buffOver", 0) / 60);
        g.text(font, b, left + 8, y, TEKST, false);
        long u = data.getLongOr("uitgerust", 0);
        g.text(font, Component.translatable("vadsrijk.ambacht.boek.uitgerust", u / 60, 120), left + 8, y + 11, ZACHT, false);
        g.text(font, Component.translatable("vadsrijk.ambacht.penning.stand", data.getIntOr("penningen", 0)), left + 8, y + 22, GOUD, false);
    }

    private void ambachtKeuze(GuiGraphicsExtractor g, int mx, int my, String tab, boolean totaal) {
        String nu = tab.equals("rang") ? data.getStringOr("lijst", "mijnen") : data.getStringOr("ambacht", "mijnen");
        int x = left + 8;
        for (Ambacht a : Ambacht.values()) {
            boolean gekozen = a.id.equals(nu);
            slot(g, mx, my, x, top + 40, icoon(a.icoon), gekozen);
            String id = a.id;
            klikvlak(x, top + 40, 18, 18, () -> open(tab, id, tab.equals("rang") ? id : ""));
            tip(mx, my, x, top + 40, 18, 18, List.of(Component.translatable(a.naamKey())));
            x += 20;
        }
        if (totaal) {
            boolean gekozen = nu.equals("totaal");
            knop(g, mx, my, x + 4, top + 42, 50, 14, Component.translatable("vadsrijk.ambacht.rang.totaal"), !gekozen, () -> open("rang", "mijnen", "totaal"));
        }
    }

    private void recepten(GuiGraphicsExtractor g, int mx, int my) {
        ambachtKeuze(g, mx, my, "recepten", false);
        ListTag l = data.getListOrEmpty("recepten");
        int y = top + 62, rij = 0;
        int max = 7;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, l.size() - max)));
        for (int i = scroll; i < l.size() && rij < max; i++, rij++) {
            CompoundTag r = l.getCompoundOrEmpty(i);
            boolean kent = r.getBooleanOr("kent", false);
            g.fill(left + 6, y, left + w - 6, y + 20, rij % 2 == 0 ? RIJ_A : RIJ_B);
            slot(g, mx, my, left + 8, y + 1, kent ? item(r.getIntOr("item", -1)) : new ItemStack(Items.GRAY_DYE), false);
            g.text(font, Component.translatable(r.getStringOr("naam", "")), left + 30, y + 2, kent ? TEKST : ZACHT, false);
            g.text(font, Component.translatable("vadsrijk.ambacht.recept.lv", r.getIntOr("level", 1), r.getIntOr("tier", 1)), left + 30, y + 11, ZACHT, false);
            ListTag in = r.getListOrEmpty("in");
            int ix = left + w - 8 - in.size() * 20;
            for (int k = 0; k < in.size(); k++) {
                CompoundTag c = in.getCompoundOrEmpty(k);
                int heeft = c.getIntOr("heeft", 0), nodig = c.getIntOr("nodig", 1);
                slot(g, mx, my, ix + k * 20, y + 1, item(c.getIntOr("item", -1)), heeft >= nodig);
                tip(mx, my, ix + k * 20, y + 1, 18, 18, List.of(Component.translatable(c.getStringOr("naam", "")),
                        Component.translatable(c.getBooleanOr("of", false) ? "vadsrijk.ambacht.recept.of" : "vadsrijk.ambacht.recept.heeft", heeft < 0 ? "?" : heeft, nodig)));
            }
            y += 21;
        }
        if (l.size() > max) {
            g.text(font, (scroll + 1) + "-" + Math.min(l.size(), scroll + max) + " / " + l.size(), left + 8, top + h - 26, ZACHT, false);
        }
    }

    private void bestel(GuiGraphicsExtractor g, int mx, int my) {
        ListTag l = data.getListOrEmpty("bestel");
        boolean geruild = data.getBooleanOr("geruild", false);
        int y = top + 42;
        if (l.isEmpty()) {
            g.centeredText(font, Component.translatable("vadsrijk.ambacht.bestel.leeg"), left + w / 2, top + 90, ZACHT);
        }
        for (int i = 0; i < l.size(); i++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            int slot = i, n = e.getIntOr("aantal", 1), v = e.getIntOr("voortgang", 0);
            boolean claimd = e.getBooleanOr("geclaimd", false), klaar = e.getBooleanOr("klaar", false);
            g.fill(left + 6, y, left + w - 6, y + 40, i % 2 == 0 ? RIJ_A : RIJ_B);
            slot(g, mx, my, left + 10, y + 11, claimd ? new ItemStack(Items.LIME_DYE) : icoon(Ambacht.van(e.getStringOr("ambacht", "mijnen")).icoon), false);
            Component tekst = Component.translatable(e.getStringOr("key", ""), Component.translatable(e.getStringOr("gever", "")), n,
                    Component.translatable(e.getStringOr("ding", "")));
            int ty = y + 4;
            for (var regel : font.split(tekst, w - 130)) {
                g.text(font, regel, left + 34, ty, claimd ? ZACHT : TEKST, false);
                ty += 9;
            }
            int bx = left + 34, bw = 110;
            g.fill(bx, y + 30, bx + bw, y + 35, 0xFFE8C8DC);
            g.fill(bx, y + 30, bx + (int) (bw * Math.min(1.0, v / (double) n)), y + 35, klaar ? 0xFF4AC87A : 0xFFFF6FB5);
            g.text(font, Math.min(v, n) + "/" + n, bx + bw + 4, y + 28, ZACHT, false);
            if (claimd) {
                g.text(font, Component.translatable("vadsrijk.ambacht.bestel.gedaan"), left + w - 70, y + 15, GROEN, false);
            } else {
                knop(g, mx, my, left + w - 84, y + 5, 74, 13, Component.translatable("vadsrijk.ambacht.bestel.lever"), klaar, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putInt("slot", slot);
                    doe("claim", a);
                });
                knop(g, mx, my, left + w - 84, y + 21, 74, 13, Component.translatable("vadsrijk.ambacht.bestel.ruil"), !geruild, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putInt("slot", slot);
                    doe("ruil", a);
                });
            }
            y += 44;
        }
        Component week = Component.translatable(data.getBooleanOr("weekKist", false) ? "vadsrijk.ambacht.bestel.week_klaar" : "vadsrijk.ambacht.bestel.week",
                Math.min(data.getIntOr("weekN", 0), data.getIntOr("weekDoel", 12)), data.getIntOr("weekDoel", 12));
        g.text(font, week, left + 8, top + h - 36, ZACHT, false);
        long s = data.getLongOr("reset", 0);
        g.text(font, Component.translatable("vadsrijk.ambacht.bestel.reset", s / 3600, s / 60 % 60), left + 8, top + h - 26, ZACHT, false);
    }

    private void rang(GuiGraphicsExtractor g, int mx, int my) {
        ambachtKeuze(g, mx, my, "rang", true);
        ListTag l = data.getListOrEmpty("top");
        int y = top + 64;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            boolean jij = e.getBooleanOr("jij", false);
            g.fill(left + 6, y, left + w - 6, y + 12, jij ? HOVER : i % 2 == 0 ? RIJ_A : RIJ_B);
            g.text(font, (i + 1) + ".", left + 10, y + 2, i < 3 ? GOUD : TEKST, false);
            g.text(font, e.getStringOr("naam", "?"), left + 30, y + 2, TEKST, false);
            g.text(font, Component.translatable("vadsrijk.ambacht.rang.lv", e.getIntOr("level", 1), e.getLongOr("xp", 0)), left + 170, y + 2, ZACHT, false);
            y += 13;
        }
        int eigen = data.getIntOr("eigen", -1);
        g.text(font, eigen > 0 ? Component.translatable("vadsrijk.ambacht.rang.eigen", eigen, data.getIntOr("aantal", 0))
                : Component.translatable("vadsrijk.ambacht.rang.geen"), left + 8, top + h - 26, ZACHT, false);
    }

    private void plekken(GuiGraphicsExtractor g) {
        // (U-AMBACHT2: every zone, 3 columns x 2 rows; the zone-4 fishing spots were missing here)
        ListTag l = data.getListOrEmpty("plekken");
        int kolom = (w - 16) / 3, rijH = 64;
        int zone = 0, x = left + 8, y = top + 42;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            int z = e.getIntOr("zone", 1);
            if (z != zone) {
                zone = z;
                x = left + 8 + ((z - 1) % 3) * kolom;
                y = top + 42 + ((z - 1) / 3) * rijH;
                g.text(font, Component.translatable("vadsrijk.ambacht.boek.zone", z).withStyle(ChatFormatting.BOLD), x, y, TEKST, false);
                y += 12;
            }
            int gev = e.getIntOr("gevonden", 0), tot = e.getIntOr("totaal", 1);
            g.text(font, Component.translatable("vadsrijk.ambacht.naam." + e.getStringOr("ambacht", "")).append(": " + gev + "/" + tot), x, y,
                    gev >= tot ? GROEN : TEKST, false);
            y += 10;
        }
        int b = top + 42 + 2 * rijH + 4;
        g.text(font, Component.translatable("vadsrijk.ambacht.boek.vissen", data.getIntOr("vissen", 0), data.getIntOr("vissenTot", 9)), left + 8, b, TEKST, false);
        g.text(font, Component.translatable("vadsrijk.ambacht.boek.honing", data.getIntOr("honing", 0), data.getIntOr("honingTot", 3)), left + 8, b + 11, TEKST, false);
        g.text(font, Component.translatable("vadsrijk.ambacht.boek.recepten", data.getIntOr("recepten", 0), data.getIntOr("receptenTot", 1)), left + 8, b + 22, TEKST,
                false);
        g.text(font, Component.translatable("vadsrijk.ambacht.boek.meer"), left + 8, b + 36, ZACHT, false);
    }

    // ---- U-AMBACHT2: the Opdrachtenbord (player crafting orders) -------------------------------------------------------------------

    private void opdracht(GuiGraphicsExtractor g, int mx, int my) {
        ListTag l = data.getListOrEmpty("opdrachten");
        int lw = 250, y = top + 40, max = 8;
        kop(g, Component.translatable("vadsrijk.ambacht.opdracht.kop"), left + 8, y);
        y += 12;
        if (l.isEmpty()) {
            g.text(font, Component.translatable("vadsrijk.ambacht.opdracht.leeg"), left + 8, y + 4, ZACHT, false);
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, l.size() - max)));
        for (int i = scroll, rij = 0; i < l.size() && rij < max; i++, rij++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            boolean eigen = e.getBooleanOr("eigen", false), geleverd = e.getBooleanOr("geleverd", false);
            int n = e.getIntOr("aantal", 1), heeft = e.getIntOr("heeft", 0);
            long id = e.getLongOr("id", -1L);
            g.fill(left + 6, y, left + 6 + lw, y + 20, eigen ? HOVER : rij % 2 == 0 ? RIJ_A : RIJ_B);
            slot(g, mx, my, left + 7, y + 1, item(e.getIntOr("item", -1)), false);
            Component naam = Component.translatable(e.getStringOr("naam", "")).append(" x" + n);
            g.text(font, font.plainSubstrByWidth(naam.getString(), 128), left + 28, y + 2, TEKST, false);
            Component wie = geleverd ? Component.translatable("vadsrijk.ambacht.opdracht.door", e.getStringOr("maker", ""))
                    : Component.translatable("vadsrijk.ambacht.opdracht.van", eigen ? Component.translatable("vadsrijk.ambacht.opdracht.jij")
                            : Component.literal(e.getStringOr("vrager", "?")));
            g.text(font, font.plainSubstrByWidth(wie.getString(), 128), left + 28, y + 11, ZACHT, false);
            munten(g, left + 6 + lw - 64, y + 6, e.getLongOr("beloning", 0), true, false);
            int bx = left + 6 + lw - 60;
            if (eigen && geleverd) {
                g.text(font, Component.translatable("vadsrijk.ambacht.opdracht.klaar"), bx + 4, y + 6, GROEN, false);
            } else if (eigen) {
                knop(g, mx, my, bx, y + 3, 56, 14, Component.translatable("vadsrijk.ambacht.opdracht.trek"), true, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putLong("id", id);
                    doe("opdracht_trek", a);
                });
            } else {
                boolean kent = e.getBooleanOr("kent", false);
                knop(g, mx, my, bx, y + 3, 56, 14, Component.translatable("vadsrijk.ambacht.opdracht.lever"), kent && heeft >= n, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putLong("id", id);
                    doe("opdracht_lever", a);
                });
                tip(mx, my, bx, y + 3, 56, 14, List.of(Component.translatable(kent ? "vadsrijk.ambacht.opdracht.heeft" : "vadsrijk.ambacht.opdracht.kent_niet_tip",
                        heeft, n)));
            }
            y += 21;
        }
        if (l.size() > max) {
            g.text(font, (scroll + 1) + "-" + Math.min(l.size(), scroll + max) + " / " + l.size(), left + 8, top + h - 26, ZACHT, false);
        }
        nieuweOpdracht(g, mx, my, left + 266, top + 40);
    }

    private void nieuweOpdracht(GuiGraphicsExtractor g, int mx, int my, int x, int y) {
        int pw = left + w - 8 - x;
        g.fill(x - 4, y - 2, x + pw, top + h - 16, 0xFFFFF4FA);
        kop(g, Component.translatable("vadsrijk.ambacht.opdracht.nieuw"), x, y);
        ListTag k = data.getListOrEmpty("kiesbaar");
        if (k.isEmpty()) {
            return;
        }
        kies = Math.floorMod(kies, k.size());
        CompoundTag r = k.getCompoundOrEmpty(kies);
        int max = r.getIntOr("max", 1);
        aantal = Math.max(1, Math.min(max, aantal));
        long min = data.getLongOr("minPerStuk", 25) * aantal;
        if (beloning < min) {
            beloning = min;
        }
        y += 13;
        knop(g, mx, my, x, y + 2, 12, 14, Component.literal("<"), true, () -> kies--);
        slot(g, mx, my, x + 15, y, item(r.getIntOr("item", -1)), true);
        knop(g, mx, my, x + 36, y + 2, 12, 14, Component.literal(">"), true, () -> kies++);
        y += 21;
        int regels = 0;
        for (var regel : font.split(Component.translatable(r.getStringOr("naam", "")), pw - 4)) {
            if (regels++ < 2) {
                g.text(font, regel, x, y, TEKST, false);
                y += 9;
            }
        }
        g.text(font, Component.translatable("vadsrijk.ambacht.opdracht.kan_maken", Component.translatable("vadsrijk.ambacht.naam." + r.getStringOr("ambacht", "")),
                r.getIntOr("level", 1)), x, y, ZACHT, false);
        y += 12;
        g.text(font, Component.translatable("vadsrijk.ambacht.opdracht.aantal_n", aantal), x, y + 3, TEKST, false);
        knop(g, mx, my, x + pw - 34, y, 14, 14, Component.literal("-"), aantal > 1, () -> aantal = aantal > 8 ? aantal - 8 : aantal - 1);
        knop(g, mx, my, x + pw - 18, y, 14, 14, Component.literal("+"), aantal < max, () -> aantal = aantal >= 8 ? aantal + 8 : aantal + 1);
        y += 16;
        g.text(font, Component.translatable("vadsrijk.ambacht.opdracht.beloning_label"), x, y + 3, TEKST, false);
        y += 12;
        munten(g, x, y + 3, beloning, false, false);
        long stap = beloning < 500 ? 25 : beloning < 5000 ? 250 : 2500;
        knop(g, mx, my, x + pw - 34, y, 14, 14, Component.literal("-"), beloning - stap >= min, () -> beloning -= stap);
        knop(g, mx, my, x + pw - 18, y, 14, 14, Component.literal("+"), true, () -> beloning += stap);
        y += 18;
        boolean vrij = data.getIntOr("mijnOpen", 0) < data.getIntOr("maxOpen", 5);
        String id = r.getStringOr("id", "");
        knop(g, mx, my, x, y, pw - 4, 15, Component.translatable("vadsrijk.ambacht.opdracht.plaats"), vrij && data.getLongOr("munten", 0) >= beloning, () -> {
            CompoundTag a = new CompoundTag();
            a.putString("recept", id);
            a.putInt("aantal", aantal);
            a.putLong("beloning", beloning);
            doe("opdracht_plaats", a);
        });
        y += 18;
        g.text(font, Component.translatable("vadsrijk.ambacht.opdracht.open_n", data.getIntOr("mijnOpen", 0), data.getIntOr("maxOpen", 5)), x, y, ZACHT, false);
        y += 11;
        int op = data.getIntOr("ophalen", 0);
        knop(g, mx, my, x, y, pw - 4, 15, Component.translatable("vadsrijk.ambacht.opdracht.ophalen", op), op > 0, () -> doe("opdracht_haal", new CompoundTag()));
    }

    // ---- U-AMBACHT2: the Knutselwinkel ------------------------------------------------------------------------------------------

    private void winkel(GuiGraphicsExtractor g, int mx, int my) {
        int y = top + 40;
        kop(g, Component.translatable("vadsrijk.ambacht.winkel.kop"), left + 8, y);
        Component stand = Component.translatable("vadsrijk.ambacht.penning.stand", data.getIntOr("penningen", 0));
        g.text(font, stand, left + w - 8 - font.width(stand), y, GOUD, false);
        y += 11;
        g.text(font, Component.translatable("vadsrijk.ambacht.winkel.uitleg"), left + 8, y, ZACHT, false);
        y += 12;
        ListTag l = data.getListOrEmpty("winkel");
        int kolom = (w - 16) / 2;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            int x = left + 8 + (i % 2) * kolom, ry = y + (i / 2) * 21;
            g.fill(x, ry, x + kolom - 4, ry + 20, (i / 2) % 2 == 0 ? RIJ_A : RIJ_B);
            slot(g, mx, my, x + 1, ry + 1, item(e.getIntOr("item", -1)), false);
            Component naam = Component.translatable(e.getStringOr("naam", ""));
            if (e.getStringOr("soort", "").equals("post")) {
                naam = naam.copy().append(Component.translatable("vadsrijk.ambacht.winkel.naar_huis"));
            }
            g.text(font, font.plainSubstrByWidth(naam.getString(), kolom - 74), x + 22, ry + 2, TEKST, false);
            g.text(font, Component.translatable("vadsrijk.ambacht.winkel.prijs", e.getIntOr("prijs", 0)), x + 22, ry + 11, GOUD, false);
            String id = e.getStringOr("id", "");
            knop(g, mx, my, x + kolom - 48, ry + 3, 40, 14, Component.translatable("vadsrijk.ambacht.winkel.koop"), e.getBooleanOr("kan", false), () -> {
                CompoundTag a = new CompoundTag();
                a.putString("id", id);
                doe("koop", a);
            });
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = Math.max(0, scroll - (int) Math.signum(dy));
        return true;
    }
}
