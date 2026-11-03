package nl.juiced.guhs.feature.erfenis.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.erfenis.Base;
import nl.juiced.guhs.feature.erfenis.DecoWinkel;
import nl.juiced.guhs.feature.erfenis.ErfenisDeco;
import nl.juiced.guhs.feature.erfenis.ErfenisPayloads.Actie;
import nl.juiced.guhs.feature.erfenis.Goed;
import nl.juiced.guhs.feature.erfenis.Paden;

/**
 * De Erfenis, the overview's deco parts (deco slice): the decowinkel in the shop bar (tab "Deco": a row of categories, the
 * cards of the open category, the gum and the Decobon switch) and the side panel of a placed deco piece (Verplaatsen,
 * Draaien, Opbergen). The screen ({@link OverzichtScherm}) keeps the modes; this only draws and calls back.
 */
final class DecoBalk {
    static final int KAART_W = 52, KAART_H = 34;
    private static final Map<String, ItemStack> ICONEN = new HashMap<>();

    private DecoBalk() {
    }

    static ItemStack icoon(String id) {
        return ICONEN.computeIfAbsent(id, k -> {
            DecoWinkel.Stuk s = DecoWinkel.stuk(k);
            var i = s == null ? null : Goed.item(s.icoon());
            return new ItemStack(i == null || i == Items.AIR ? Items.FLOWER_POT : i);
        });
    }

    /** How wide the deco cards need the shop to be. */
    static int breedte() {
        int n = 0;
        for (DecoWinkel.Categorie c : DecoWinkel.Categorie.values()) {
            n = Math.max(n, DecoWinkel.van(c).size());
        }
        return n * (KAART_W + 4) - 4 + 12;
    }

    /** The decowinkel inside the shop panel (x0, y0)-(x0 + breed, y0 + hoog). */
    static void kaarten(Ui ui, OverzichtScherm s, Base b, int x0, int y0, int breed) {
        int h = b.hoeveLevel();
        // the categories
        int cx = x0 + 6, cy = y0 + 4;
        for (DecoWinkel.Categorie c : DecoWinkel.Categorie.values()) {
            Component naam = Component.translatable(c.naamKey());
            int w = ui.font.width(naam) + 8;
            boolean open = c == OverzichtScherm.decoCat;
            boolean nieuw = false;
            for (DecoWinkel.Stuk st : DecoWinkel.van(c)) {
                nieuw |= st.vanaf() == h && h > 1;
            }
            Teken.rond(ui.g, cx, cy, cx + w, cy + 11, open ? Teken.ROZE_DONKER : ui.op(cx, cy, cx + w, cy + 11) ? Teken.ROZE : 0xFFF3E4EB, 3);
            ui.tekst(naam, cx + 4, cy + 2, open ? Teken.WIT : Teken.ZACHT);
            if (nieuw) {
                ui.g.fill(cx + w - 3, cy + 1, cx + w - 1, cy + 3, Teken.GROEN);
            }
            ui.klik(cx, cy, cx + w, cy + 11, () -> OverzichtScherm.decoCat = c, nieuw ? List.of(Component.translatable("erfenis.decowinkel.cat_nieuw")) : null);
            cx += w + 2;
        }
        // right: the gum (paths and hekjes) and the Decobon switch
        int rx = x0 + breed - 6;
        if (b.decobonnen > 0) {
            Component t = Component.translatable("erfenis.decowinkel.bon", b.decobonnen);
            int w = ui.font.width(t) + 8;
            boolean aan = OverzichtScherm.bonModus;
            Teken.rond(ui.g, rx - w, cy, rx, cy + 11, aan ? Teken.GOUD : ui.op(rx - w, cy, rx, cy + 11) ? 0xFFFFF0C0 : 0xFFF6EAD0, 3);
            ui.tekst(t, rx - w + 4, cy + 2, aan ? Teken.DONKER : 0xFFA07A20);
            ui.klik(rx - w, cy, rx, cy + 11, () -> OverzichtScherm.bonModus = !OverzichtScherm.bonModus,
                    List.of(Component.translatable("erfenis.decowinkel.bon_tip").withColor(Teken.ZACHT)));
            rx -= w + 3;
        } else {
            OverzichtScherm.bonModus = false;
        }
        if (OverzichtScherm.decoCat == DecoWinkel.Categorie.PADEN || OverzichtScherm.decoCat == DecoWinkel.Categorie.HEKJES) {
            Component t = Component.translatable("erfenis.decowinkel.gum");
            int w = ui.font.width(t) + 8;
            boolean aan = s.gumAan();
            Teken.rond(ui.g, rx - w, cy, rx, cy + 11, aan ? Teken.ROZE_DONKER : ui.op(rx - w, cy, rx, cy + 11) ? Teken.ROZE : 0xFFF3E4EB, 3);
            ui.tekst(t, rx - w + 4, cy + 2, aan ? Teken.WIT : Teken.DONKER);
            ui.klik(rx - w, cy, rx, cy + 11, () -> s.trekStart(null), List.of(Component.translatable("erfenis.decowinkel.gum_tip").withColor(Teken.ZACHT)));
        }
        // the cards
        int x = x0 + 6, y = y0 + 18;
        for (DecoWinkel.Stuk st : DecoWinkel.van(OverzichtScherm.decoCat)) {
            kaart(ui, s, b, st, x, y);
            x += KAART_W + 4;
        }
    }

    private static void kaart(Ui ui, OverzichtScherm s, Base b, DecoWinkel.Stuk st, int x, int y) {
        int h = b.hoeveLevel();
        boolean slot = h < st.vanaf();
        boolean bon = OverzichtScherm.bonModus && st.soort() == DecoWinkel.Soort.STUK;
        boolean gekozen = s.gekozenDeco(st.id());
        boolean op = ui.op(x, y, x + KAART_W, y + KAART_H);
        int voorraad = st.getrokken() ? Paden.voorraad(b, st.id()) : b.decoOpslag.getOrDefault(st.id(), 0);
        boolean kan = !slot && (bon || voorraad > 0 || b.guhlden >= st.prijs());
        Teken.rond(ui.g, x, y, x + KAART_W, y + KAART_H, gekozen ? Teken.GOUD : op ? Teken.ROZE_DONKER : Teken.ROZE, 3);
        Teken.rond(ui.g, x + 1, y + 1, x + KAART_W - 1, y + KAART_H - 1, gekozen ? 0xFFFFF7DC : kan ? (op ? 0xFFFFFFFF : 0xFFFFF0F5) : 0xFFEDE3E8, 3);
        ui.item(icoon(st.id()), x + 2, y + 2);
        if (slot) {
            ui.g.fill(x + 2, y + 2, x + 18, y + 18, 0xA0EDE3E8);
            ui.item(Ui.SLOT, x + 6, y + 6, 0.5f);
        }
        String prijs;
        int kleur;
        if (slot) {
            prijs = "H" + st.vanaf();
            kleur = Teken.ROOD;
        } else if (bon) {
            prijs = "bon";
            kleur = 0xFFA07A20;
        } else {
            prijs = "Ƒ" + Ui.getal(st.prijs()) + (st.getrokken() ? "/t" : "");
            kleur = b.guhlden >= st.prijs() ? Teken.DONKER : Teken.ROOD;
        }
        ui.passend(Component.literal(prijs), x + 20, y + 3, KAART_W - 22, kleur);
        if (voorraad > 0) {
            ui.tekst("x" + voorraad, x + 20, y + 12, Teken.GROEN_DONKER);
        }
        kort(ui, Component.translatable(st.naamKey()), x + 3, y + 23, KAART_W - 6, kan ? Teken.DONKER : Teken.ZACHT);
        // the tooltip
        List<Component> tip = new ArrayList<>();
        tip.add(Component.translatable(st.naamKey()).withStyle(ChatFormatting.BOLD));
        if (st.soort() == DecoWinkel.Soort.STUK) {
            int[] m = Base.decoMaat(st.id(), 0);
            tip.add(Component.translatable("erfenis.ui.maat", m[0], m[1]).withColor(Teken.ZACHT));
            tip.add(Component.translatable("erfenis.decowinkel.knus", ErfenisDeco.knusheid(st.id())).withColor(Teken.ZACHT));
        } else {
            tip.add(Component.translatable(st.soort() == DecoWinkel.Soort.PAD ? "erfenis.decowinkel.knus_pad"
                    : "knuffelhaag".equals(st.id()) || "vadshekje".equals(st.id()) ? "erfenis.decowinkel.knus_hek4" : "erfenis.decowinkel.knus_hek")
                    .withColor(Teken.ZACHT));
        }
        if (slot) {
            tip.add(Component.translatable("erfenis.fout.slot", st.vanaf()).withColor(Teken.ROOD));
        } else {
            tip.add(Component.translatable(st.getrokken() ? "erfenis.decowinkel.prijs_tegel" : "erfenis.decowinkel.prijs", Ui.getal(st.prijs())));
            if (voorraad > 0) {
                tip.add(Component.translatable(st.getrokken() ? "erfenis.decowinkel.voorraad_tegels" : "erfenis.decowinkel.voorraad", voorraad)
                        .withColor(Teken.GROEN_DONKER));
            }
            for (FormattedCharSequence r : ui.font.split(Component.translatable(st.getrokken() ? "erfenis.decowinkel.klik_trek"
                    : bon ? "erfenis.decowinkel.klik_bon" : "erfenis.decowinkel.klik_koop"), 190)) {
                tip.add(Component.literal(stuk(r)).withColor(Teken.ZACHT));
            }
        }
        String id = st.id();
        ui.klik(x, y, x + KAART_W, y + KAART_H, () -> s.kiesDeco(id), tip);
    }

    /** The side panel of a placed piece: name, Knusheid, size, and Verplaatsen / Draaien / Opbergen. Returns its bottom. */
    static int paneel(Ui ui, OverzichtScherm s, Base b, Base.Deco d, int px, int py, boolean eigen) {
        int w = GebouwPaneel.B, h = eigen ? 98 : 50;
        Teken.paneel(ui.g, px, py, px + w, py + h);
        ui.vlak(px, py, px + w, py + h, null);
        ui.item(icoonVoor(d.id()), px + 6, py + 6);
        ui.passend(Component.translatable("erfenis.deco." + d.id()).withStyle(ChatFormatting.BOLD), px + 26, py + 6, w - 32, Teken.DONKER);
        int[] m = Base.decoMaat(d.id(), d.rot());
        int knus = ErfenisDeco.knusheid(d.id());
        ui.tekst(Component.translatable("erfenis.ui.maat", m[0], m[1]), px + 26, py + 16, Teken.ZACHT);
        ui.tekst(knus > 0 ? Component.translatable("erfenis.decowinkel.knus", knus) : Component.translatable("erfenis.decowinkel.knus_hek"),
                px + 8, py + 30, Teken.TEKST);
        if (!eigen) {
            return py + h;
        }
        int by = py + 44, bw = (w - 20) / 2;
        ui.knop(Component.translatable("erfenis.ui.verplaatsen"), px + 8, by, px + 8 + bw, by + 14, true, Teken.ROZE_DONKER,
                () -> s.decoVerplaatsStart(d), List.of(Component.translatable("erfenis.decowinkel.verplaats_tip").withColor(Teken.ZACHT)));
        ui.knop(Component.translatable("erfenis.ui.draaien"), px + 12 + bw, by, px + w - 8, by + 14, true, Teken.ROZE_DONKER,
                () -> ErfenisClient.stuur(new Actie(Actie.DECO, 0, d.x(), d.z(), Actie.DECO_DRAAI, 0, 0, "")), null);
        by += 18;
        ui.knop(Component.translatable("erfenis.ui.deco_weg"), px + 8, by, px + w - 8, by + 14, true, 0xFFB98AA6, () -> {
            ErfenisClient.stuur(new Actie(Actie.DECO, 0, d.x(), d.z(), Actie.DECO_WEG, 0, 0, ""));
            s.decoLos();
        }, List.of(Component.translatable("erfenis.decowinkel.weg_tip").withColor(Teken.ZACHT)));
        by += 18;
        ui.passend(Component.translatable("erfenis.decowinkel.sleep_tip"), px + 8, by + 2, w - 16, Teken.ZACHT);
        return py + h;
    }

    /** The shop icon, or (a special piece) a flower pot. */
    static ItemStack icoonVoor(String id) {
        return DecoWinkel.stuk(id) != null ? icoon(id) : SPECIAAL;
    }

    private static final ItemStack SPECIAAL = new ItemStack(Items.FLOWER_POT);

    /** A name in a card: squeezed a little (to 70%), and beyond that cut off with "..." (the tooltip has it whole). */
    private static void kort(Ui ui, Component t, int x, int y, int breed, int kleur) {
        int w = ui.font.width(t);
        if (w <= breed / 0.7f) {
            ui.passend(t, x, y, breed, kleur);
            return;
        }
        String s = t.getString();
        int max = (int) (breed / 0.7f) - ui.font.width("...");
        while (!s.isEmpty() && ui.font.width(s) > max) {
            s = s.substring(0, s.length() - 1);
        }
        ui.passend(Component.literal(s.trim() + "..."), x, y, breed, kleur);
    }

    static boolean shift() {
        return Minecraft.getInstance().hasShiftDown();
    }

    private static String stuk(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((i, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }
}
