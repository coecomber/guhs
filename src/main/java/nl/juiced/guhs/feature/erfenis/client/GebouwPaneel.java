package nl.juiced.guhs.feature.erfenis.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.erfenis.Base;
import nl.juiced.guhs.feature.erfenis.ErfenisPayloads.Actie;
import nl.juiced.guhs.feature.erfenis.Gebouw;
import nl.juiced.guhs.feature.erfenis.GebouwType;
import nl.juiced.guhs.feature.erfenis.Goed;
import nl.juiced.guhs.feature.erfenis.Plot;
import nl.juiced.guhs.feature.erfenis.Prijzen;
import nl.juiced.guhs.feature.erfenis.Prijzen.Doel;
import nl.juiced.guhs.feature.erfenis.Prijzen.Prijs;
import nl.juiced.guhs.feature.erfenis.Productie;
import nl.juiced.guhs.feature.erfenis.Recept;

/**
 * De Erfenis, the overview's side panel (right): the selected building (what it is, what it makes and how fast, its
 * workers, bonus and vadskracht, its basket with Ophalen, its choices, the next level with every cost and the lock reason,
 * and the buttons Verbeteren / Verplaatsen / Draaien / Verven and the building's own one), or a plukje (clear it) or a
 * kavel (buy it). Scrolls with the mouse wheel when it doesn't fit.
 */
final class GebouwPaneel {
    static final int B = 168;
    /** The palettes' swatch colours (T1 hout & stro, T2 baksteen & roze, T3 vakwerk, T4 vads & goud, T5 pastel-kristal). */
    static final int[] PALET = {0xFFC79A5B, 0xFFD8735F, 0xFFEFE2C8, 0xFFE8B830, 0xFFB5DDF5};
    static final int[] PALET_RAND = {0xFF7A5A30, 0xFF9A3F35, 0xFF6B4A35, 0xFF9A7410, 0xFFC08AD8};

    int scroll;
    boolean verven;
    private int inhoud;
    private int vorigeKeuze = Integer.MIN_VALUE;

    /** A new selection: back to the top. */
    void reset(int keuze) {
        if (keuze != vorigeKeuze) {
            scroll = 0;
            verven = false;
            vorigeKeuze = keuze;
        }
    }

    void scrol(double stappen) {
        scroll = (int) Math.max(0, scroll - stappen * 18);
    }

    // =====================================================================================================================

    /** The panel at (x0, y0) with at most this height. Returns its bottom. */
    int gebouw(Ui ui, OverzichtScherm s, Base b, Gebouw geb, long nu, int x0, int y0, int maxH, boolean eigen) {
        int x1 = x0 + B;
        int hoogte = Math.min(maxH, inhoud + 10);
        Teken.paneel(ui.g, x0, y0, x1, y0 + hoogte);
        ui.vlak(x0, y0, x1, y0 + hoogte, null);
        ui.g.enableScissor(x0 + 2, y0 + 2, x1 - 2, y0 + hoogte - 2);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, inhoud + 10 - hoogte)));
        int top = y0 + 6 - scroll;
        Regels r = new Regels(ui, x0 + 8, top, B - 16, y0, y0 + hoogte);
        // head
        ui.item(geb.type.icoon(), x0 + 6, r.y);
        ui.tekst(Component.translatable(geb.type.naamKey()).withStyle(ChatFormatting.BOLD), x0 + 26, r.y, Teken.DONKER);
        Component sub = geb.level == 0 ? Component.translatable("erfenis.ui.in_aanbouw")
                : Component.translatable("erfenis.ui.level_palet", geb.level, geb.palet);
        ui.tekst(sub, x0 + 26, r.y + 10, Teken.ROZE_DONKER);
        r.y += 22;
        Base.Bouw bouw = b.bouwVan(Base.Bouw.GEBOUW, geb.id);
        if (bouw != null) {
            r.regel(Component.translatable(geb.level == 0 ? "erfenis.ui.wordt_gebouwd" : "erfenis.ui.wordt_verbeterd", bouw.naar())
                    .append(" · " + Teken.tijd(bouw.eind() - nu)), Teken.DONKER);
            if (r.zichtbaar(r.y, r.y + 5)) {
                Teken.balk(ui.g, r.x, r.y, r.x + r.w, r.y + 5, bouw.voortgang(nu), Teken.GOUD);
            }
            r.y += 9;
        }
        List<FormattedCharSequence> uitleg = ui.font.split(Component.translatable(geb.type.uitlegKey()), r.w);
        for (int i = 0; i < Math.min(6, uitleg.size()); i++) {
            r.regel(uitleg.get(i), Teken.ZACHT);
        }
        r.y += 3;
        int lvl = Math.max(1, geb.level);
        // ---- production
        if (geb.type.maakt() && geb.level > 0) {
            r.streep();
            productie(ui, s, r, b, geb, eigen);
        }
        // ---- what it does
        List<Component> effect = effecten(b, geb, lvl);
        if (!effect.isEmpty()) {
            r.streep();
            for (Component e : effect) {
                r.regelWrap(Component.literal("• ").append(e), Teken.TEKST);
            }
        }
        if (geb.type == GebouwType.KAARSENMAKERIJ && geb.level > 0) {
            kleuren(ui, r, geb, eigen);
        }
        // ---- the next level
        r.streep();
        volgende(ui, r, b, geb, bouw != null);
        // ---- buttons
        if (eigen) {
            r.y += 2;
            knoppen(ui, s, r, b, geb);
        }
        inhoud = r.y + scroll - (y0 + 6);
        ui.g.disableScissor();
        scrolBalk(ui, x1, y0, hoogte);
        return y0 + hoogte;
    }

    private void scrolBalk(Ui ui, int x1, int y0, int hoogte) {
        if (inhoud + 10 > hoogte) {
            int h = Math.max(12, hoogte * hoogte / (inhoud + 10));
            int y = y0 + 3 + (int) ((hoogte - 6 - h) * (scroll / (float) Math.max(1, inhoud + 10 - hoogte)));
            ui.g.fill(x1 - 4, y, x1 - 2, y + h, 0x80D27A9C);
        }
    }

    private void productie(Ui ui, OverzichtScherm s, Regels r, Base b, Gebouw geb, boolean eigen) {
        List<Recept> recepten = Productie.recepten(geb.type);
        Recept nu = Productie.recept(geb);
        if (recepten.size() > 1) {
            r.regel(Component.translatable(geb.type == GebouwType.AKKER ? "erfenis.ui.gewas" : "erfenis.ui.recept")
                    .append(": ").append(Component.translatable(nu.naamKey()).withStyle(ChatFormatting.BOLD)), Teken.TEKST);
            int x = r.x;
            for (int i = 0; i < recepten.size(); i++) {
                Recept rc = recepten.get(i);
                if (!Productie.magRecept(b, rc)) {   // a secret recipe (Sherlock's reward) stays hidden until you have it
                    continue;
                }
                boolean gekozen = rc == nu, mag = rc.minLevel() <= Math.max(1, geb.level);
                if (r.zichtbaar(r.y, r.y + 20)) {
                    Teken.rond(ui.g, x, r.y, x + 20, r.y + 20, gekozen ? Teken.GOUD : ui.op(x, r.y, x + 20, r.y + 20) ? Teken.ROZE_DONKER : Teken.ROZE, 3);
                    Teken.rond(ui.g, x + 1, r.y + 1, x + 19, r.y + 19, mag ? Teken.CREME_VOL : 0xFFE2D6DC, 2);
                    ui.item(Ui.icoon(rc.uit()), x + 2, r.y + 2);
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.translatable(rc.naamKey()).withStyle(ChatFormatting.BOLD));
                    tip.add(receptRegel(rc));
                    tip.add(Component.translatable("erfenis.ui.per_uur_basis", Ui.getal(Math.round(Productie.perUur(geb.type, rc, Math.max(1, geb.level)))))
                            .withColor(Teken.ZACHT));
                    if (!mag) {
                        tip.add(Component.translatable("erfenis.fout.recept_level", rc.minLevel()).withColor(Teken.ROOD));
                    }
                    int keuze = i;
                    ui.klik(x, r.y, x + 20, r.y + 20, eigen && mag && !gekozen ? () -> ErfenisClient.stuur(new Actie(Actie.KIES, geb.id, keuze, 0, 0, 0, 0, "")) : null, tip);
                }
                x += 22;
            }
            r.y += 23;
        } else if (!nu.in().isEmpty()) {
            r.regel(receptRegel(nu), Teken.TEKST);
        }
        double perUur = Productie.nuPerUur(b, geb);
        MutableComponent maakt = Component.translatable("erfenis.ui.maakt", Ui.getal(Math.round(perUur)), Component.translatable(nu.uit().naamKey()));
        for (Map.Entry<Goed, Double> e : nu.bij().entrySet()) {
            maakt.append(Component.translatable("erfenis.ui.en_bij", Component.translatable(e.getKey().naamKey())));
        }
        r.regelWrap(maakt, Teken.DONKER);
        int plekken = Productie.plekken(geb.type, geb.level), bemand = Productie.bemanning(b).getOrDefault(geb.id, 0);
        r.regel(Component.translatable("erfenis.ui.werkers", bemand, plekken).withColor(bemand < plekken ? Teken.ROOD : Teken.TEKST), Teken.TEKST);
        int bonus = (int) Math.round(Productie.bonus(b, geb) * 100);
        if (bonus > 0) {
            r.regel(Component.translatable("erfenis.ui.bonus", bonus, b.logesBij(geb.id)), Teken.GROEN_DONKER);
        }
        int vk = Productie.vkNodig(geb.type, geb.level);
        if (vk > 0) {
            Boolean genoeg = Productie.vadskracht(b).get(geb.id);
            boolean ok = genoeg == null || genoeg;
            r.regelWrap(Component.translatable(ok ? "erfenis.ui.vk_ok" : Productie.slaaptZonderVk(geb.type) ? "erfenis.ui.vk_slaapt" : "erfenis.ui.vk_traag",
                    vk, Productie.ZONDER_VK_LEVEL), ok ? Teken.GROEN_DONKER : Teken.ROOD);
        }
        if (!nu.in().isEmpty()) {
            boolean wacht = false;
            for (Map.Entry<Goed, Integer> e : nu.in().entrySet()) {
                wacht |= b.goed(e.getKey()) < e.getValue();
            }
            if (wacht && perUur > 0) {
                r.regelWrap(Component.translatable("erfenis.ui.wacht_op_invoer"), Teken.ROOD);
            }
        }
        // the basket
        double cap = Productie.bufferCap(geb), in = geb.inBuffer(nu.uit());
        r.regel(Component.translatable("erfenis.ui.mandje", Ui.getal((long) Math.floor(in)), Ui.getal(Math.round(cap)), Productie.bufferUren(geb.level)),
                geb.vol() ? Teken.ROOD : Teken.TEKST);
        if (r.zichtbaar(r.y, r.y + 5)) {
            Teken.balk(ui.g, r.x, r.y, r.x + r.w, r.y + 5, cap <= 0 ? 0 : (float) (in / cap), geb.vol() ? Teken.GOUD : Teken.ROZE_DONKER);
        }
        r.y += 8;
        if (eigen) {
            Base.Fout f = b.kanOphalen(geb.id);
            MutableComponent t = Component.translatable("erfenis.ui.ophalen").append(" (" + geb.klaar() + ")");
            r.knop(t, r.x, r.x + r.w, f == null, Teken.GOUD, () -> ErfenisClient.stuur(Actie.op(Actie.OPHALEN, geb.id)),
                    f == null ? null : List.of(ErfenisClient.tekst(f.key(), List.of(f.args()))));
        }
        if (geb.type == GebouwType.BOUWBLOKKENWERKPLAATS) {
            r.knop(Component.translatable("erfenis.ui.blokken_kiezen", geb.selectie.size()), r.x, r.x + r.w, eigen, 0xFF9C7BC8,
                    () -> s.open(new Vensters.Bouwkeet(geb.id)), null);
        }
    }

    /** "2 Graan + 1 Ei -> 1 Brood". */
    static Component receptRegel(Recept r) {
        MutableComponent t = Component.empty();
        boolean eerste = true;
        for (Map.Entry<Goed, Integer> e : r.in().entrySet()) {
            if (!eerste) {
                t.append(" + ");
            }
            eerste = false;
            t.append(e.getValue() + " ").append(Component.translatable(e.getKey().naamKey()));
        }
        if (r.in().isEmpty()) {
            return Component.translatable(r.uit().naamKey());
        }
        return t.append(" → " + r.uitN() + " ").append(Component.translatable(r.uit().naamKey()));
    }

    /** What a non-production building does (and the production ones' extras). */
    static List<Component> effecten(Base b, Gebouw g, int lvl) {
        List<Component> l = new ArrayList<>();
        switch (g.type) {
            case HOEVE -> {
                l.add(Component.translatable("erfenis.ui.e.guhlden", Ui.getal(Productie.guhldenCap(lvl))));
                l.add(Component.translatable("erfenis.ui.e.opslag", Productie.HOEVE_OPSLAG));
                l.add(Component.translatable("erfenis.ui.e.paletten", b.maxPalet()));
            }
            case OPSLAGSCHUUR -> l.add(Component.translatable("erfenis.ui.e.opslag", Ui.getal(Productie.opslagschuur(lvl))));
            case TIMMERSCHUUR -> l.add(Component.translatable("erfenis.ui.e.bouwers", Productie.bouwers(lvl)));
            case GUHHUTJE -> l.add(Component.translatable("erfenis.ui.e.bewoners", Productie.hutjeBewoners(lvl)));
            case SPEELTUIN -> {
                l.add(Component.translatable("erfenis.ui.e.speeltuin", Productie.speeltuinBewoners(lvl)));
                l.add(Component.translatable("erfenis.ui.e.intrek_sneller"));
            }
            case GUHPENSION -> l.add(Component.translatable("erfenis.ui.e.pension", b.loges.size(), Productie.pensionPlekken(lvl)));
            case MARKTPLEIN -> l.add(Component.translatable("erfenis.ui.e.markt", Productie.marktKarren(lvl), b.bestellingen));
            case DORPSCAFE, MUZIEKTENT -> l.add(Component.translatable("erfenis.ui.e.bonus", Productie.bonusProcent(lvl)));
            case VADSMAST -> l.add(Component.translatable("erfenis.ui.e.vadsmast", b.vkInDorp(), Productie.vadsmastCap(lvl), b.vk));
            case DETECTIVEBUREAU -> l.add(Component.translatable("erfenis.ui.e.bureau"));
            case LOKET -> l.add(Component.translatable("erfenis.ui.e.loket", b.loket.values().stream().mapToInt(Integer::intValue).sum()));
            default -> {
            }
        }
        return l;
    }

    private void kleuren(Ui ui, Regels r, Gebouw g, boolean eigen) {
        r.regel(Component.translatable("erfenis.ui.kaarskleur"), Teken.TEKST);
        int x = r.x;
        for (int k = 0; k <= 16; k++) {
            int kleur = k == 0 ? 0xFFF3E7C6 : 0xFF000000 | DyeColor.byId(k - 1).getTextureDiffuseColor();
            if (r.zichtbaar(r.y, r.y + 8)) {
                ui.g.fill(x, r.y, x + 8, r.y + 8, g.kleur == k ? Teken.GOUD : 0xFF8A6A7A);
                ui.g.fill(x + 1, r.y + 1, x + 7, r.y + 7, kleur);
                int kk = k;
                ui.klik(x, r.y, x + 8, r.y + 8, eigen ? () -> ErfenisClient.stuur(new Actie(Actie.KLEUR, g.id, kk, 0, 0, 0, 0, "")) : null,
                        List.of(Component.translatable(k == 0 ? "erfenis.ui.kaars_naturel" : "color.minecraft." + DyeColor.byId(k - 1).getSerializedName())));
            }
            x += 9;
        }
        r.y += 11;
    }

    private void volgende(Ui ui, Regels r, Base b, Gebouw g, boolean bezig) {
        if (bezig) {
            return;
        }
        if (g.level >= g.type.topLevel()) {
            r.regel(Component.translatable("erfenis.ui.max").withColor(Teken.GROEN_DONKER), Teken.GROEN_DONKER);
            return;
        }
        int naar = g.level + 1;
        r.regel(Component.translatable("erfenis.ui.volgende", naar).withStyle(ChatFormatting.BOLD), Teken.DONKER);
        if (g.type == GebouwType.HOEVE) {
            for (Doel d : Prijzen.doelen(naar)) {
                int stand = b.doelStand(d.soort());
                boolean ok = stand >= d.n();
                r.regel(Component.literal(ok ? "✔ " : "✘ ").append(Component.translatable("erfenis.doel." + d.soort(), Math.min(stand, d.n()), d.n())),
                        ok ? Teken.GROEN_DONKER : Teken.ROOD);
            }
        } else if (naar > g.type.maxLevel(b.hoeveLevel())) {
            r.regelWrap(Component.translatable("erfenis.fout.hoeve_level", g.type.ontgrendeltLevel(naar)), Teken.ROOD);
        }
        prijs(ui, r, b, b.prijsVolgende(g));
    }

    /** A price: Guhlden, goods and main-world items as little chips (red when short), and the time. */
    static void prijs(Ui ui, Regels r, Base b, Prijs p) {
        List<Object[]> chips = new ArrayList<>();
        if (p.guhlden() > 0) {
            chips.add(new Object[] {Ui.GUHLDEN, Ui.getal(p.guhlden()), b.guhlden >= p.guhlden(), Component.translatable("erfenis.ui.guhlden")});
        }
        for (Map.Entry<Goed, Integer> e : p.goederen().entrySet()) {
            chips.add(new Object[] {Ui.icoon(e.getKey()), Ui.getal(e.getValue()), b.heeft(e.getKey()) >= e.getValue(),
                    Component.translatable(e.getKey().naamKey()).append(" (" + b.heeft(e.getKey()) + "/" + e.getValue() + ")")});
        }
        for (Map.Entry<String, Integer> e : p.hoofdwereld().entrySet()) {
            if (Goed.item(e.getKey()) == null) {
                continue;
            }
            int heeft = b.loket.getOrDefault(e.getKey(), 0);
            chips.add(new Object[] {Ui.item(e.getKey()), heeft + "/" + e.getValue(), heeft >= e.getValue(),
                    Component.translatable(Goed.item(e.getKey()).getDescriptionId()).append(" ").append(Component.translatable("erfenis.ui.via_loket"))});
        }
        int x = r.x;
        for (Object[] c : chips) {
            String t = (String) c[1];
            int w = 12 + ui.font.width(t) + 4;
            if (x + w > r.x + r.w) {
                x = r.x;
                r.y += 12;
            }
            if (r.zichtbaar(r.y, r.y + 11)) {
                boolean ok = (Boolean) c[2];
                Teken.rond(ui.g, x, r.y, x + w, r.y + 11, ok ? 0xFFF6E9EF : 0xFFFBD9DE, 2);
                ui.item((ItemStack) c[0], x + 1, r.y + 1, 0.56f);
                ui.tekst(t, x + 12, r.y + 2, ok ? Teken.DONKER : Teken.ROOD);
                ui.klik(x, r.y, x + w, r.y + 11, null, List.of((Component) c[3]));
            }
            x += w + 2;
        }
        r.y += chips.isEmpty() ? 0 : 13;
        r.regel(ErfenisClient.snel || p.tijdMs() == 0 ? Component.translatable("erfenis.ui.direct")
                : Component.translatable("erfenis.ui.tijd", Ui.duur(p.tijdMs())), Teken.ZACHT);
    }

    private void knoppen(Ui ui, OverzichtScherm s, Regels r, Base b, Gebouw g) {
        int id = g.id;
        Base.Fout f = b.kanVerbeteren(id);
        boolean kanV = f == null;
        if (g.level < g.type.topLevel() && !b.inAanbouw(g)) {
            r.knop(Component.translatable("erfenis.ui.verbeteren"), r.x, r.x + r.w, kanV, Teken.GROEN, () -> ErfenisClient.stuur(Actie.op(Actie.VERBETER, id)),
                    f == null ? null : List.of(ErfenisClient.tekst(f.key(), List.of(f.args()))));
        }
        int mid = r.x + r.w / 2;
        {
            if (r.zichtbaar(r.y, r.y + 14)) {
                ui.knop(Component.translatable("erfenis.ui.verplaatsen"), r.x, r.y, mid - 2, r.y + 14, true, Teken.ROZE_DONKER, () -> s.verplaatsStart(id), null);
                Base.Fout fd = b.kanDraaien(id);
                ui.knop(Component.translatable("erfenis.ui.draaien"), mid + 2, r.y, r.x + r.w, r.y + 14, fd == null, Teken.ROZE_DONKER,
                        () -> ErfenisClient.stuur(Actie.op(Actie.DRAAI, id)), fd == null ? null : List.of(ErfenisClient.tekst(fd.key(), List.of(fd.args()))));
            }
            r.y += 17;
        }
        r.knop(Component.translatable(verven ? "erfenis.ui.verven_dicht" : "erfenis.ui.verven"), r.x, r.x + r.w, g.level > 0, 0xFFB98AA6, () -> verven = !verven, null);
        if (verven) {
            int x = r.x;
            for (int p = 1; p <= 5; p++) {
                boolean mag = p <= b.maxPalet();
                if (r.zichtbaar(r.y, r.y + 16)) {
                    int pp = p;
                    boolean op = ui.op(x, r.y, x + 24, r.y + 16);
                    Teken.rond(ui.g, x, r.y, x + 24, r.y + 16, g.palet == p ? Teken.GOUD : op && mag ? Teken.DONKER : PALET_RAND[p - 1], 3);
                    Teken.rond(ui.g, x + 1, r.y + 1, x + 23, r.y + 15, mag ? PALET[p - 1] : 0xFFD9D0D4, 2);
                    ui.tekst("T" + p, x + 7, r.y + 4, mag ? Teken.DONKER : 0xFF9A8A92);
                    ui.klik(x, r.y, x + 24, r.y + 16, mag && g.palet != p ? () -> ErfenisClient.stuur(new Actie(Actie.VERF, id, pp, 0, 0, 0, 0, "")) : null,
                            List.of(Component.translatable("erfenis.palet." + p).withStyle(ChatFormatting.BOLD),
                                    mag ? Component.translatable("erfenis.ui.verven_gratis") : Component.translatable("erfenis.fout.palet", p * 2 - 1)
                                            .withColor(Teken.ROOD)));
                }
                x += 27;
            }
            r.y += 19;
            r.knop(Component.translatable("erfenis.ui.heel_dorp", g.palet), r.x, r.x + r.w, true, 0xFF9C7BC8,
                    () -> ErfenisClient.stuur(new Actie(Actie.VERF, -1, g.palet, 0, 0, 0, 0, "")), List.of(Component.translatable("erfenis.ui.heel_dorp_uitleg")));
        }
        if (g.level > 0) {
            switch (g.type) {
                case DETECTIVEBUREAU -> r.knop(Component.translatable("erfenis.ui.naar_bureau"), r.x, r.x + r.w, true, 0xFF7F6A55,
                        () -> {
                            ErfenisClient.stuur(Actie.op(Actie.BUREAU, id));
                            OverzichtScherm.sluitAlles();
                        }, null);
                case MARKTPLEIN -> r.knop(Component.translatable("erfenis.ui.bestellingen"), r.x, r.x + r.w, true, 0xFF7FA8D8,
                        () -> ErfenisClient.stuur(Actie.op(Actie.MARKT, id)), null);
                case GUHPENSION -> r.knop(Component.translatable("erfenis.ui.loges"), r.x, r.x + r.w, true, 0xFFE07FA8,
                        () -> ErfenisClient.stuur(new Actie(Actie.PENSION, 0, Actie.PENSION_OPEN, 0, 0, 0, 0, "")), null);
                case LOKET -> r.knop(Component.translatable("erfenis.ui.loket"), r.x, r.x + r.w, true, 0xFFC8A060, () -> s.open(new Vensters.Loket()), null);
                case OPSLAGSCHUUR, HOEVE -> r.knop(Component.translatable("erfenis.ui.opslag_bekijken"), r.x, r.x + r.w, true, 0xFFC8A060,
                        () -> s.open(new Vensters.Opslag()), null);
                default -> {
                }
            }
        }
    }

    // =====================================================================================================================
    // a plukje and a kavel
    // =====================================================================================================================

    int plukje(Ui ui, Base b, int n, long nu, int x0, int y0, boolean eigen) {
        int x1 = x0 + B;
        int ring = Plot.ring(n);
        List<Component> regels = new ArrayList<>();
        Base.Bouw bouw = b.bouwVan(Base.Bouw.PLUKJE, n);
        Prijs p = Prijzen.plukje(ring);
        int h = 112 + (bouw != null ? 10 : 0);
        Teken.paneel(ui.g, x0, y0, x1, y0 + h);
        ui.vlak(x0, y0, x1, y0 + h, null);
        Regels r = new Regels(ui, x0 + 8, y0 + 6, B - 16, y0, y0 + h);
        ui.item(PLUKJE_ICOON[b.plukjeSoort(n)], x0 + 6, r.y);
        ui.tekst(Component.translatable("erfenis.plukje.soort." + b.plukjeSoort(n)).withStyle(ChatFormatting.BOLD), x0 + 26, r.y, Teken.DONKER);
        ui.tekst(Component.translatable("erfenis.ui.ring", ring), x0 + 26, r.y + 10, Teken.ROZE_DONKER);
        r.y += 22;
        r.regelWrap(Component.translatable("erfenis.ui.plukje_uitleg"), Teken.ZACHT);
        if (bouw != null) {
            r.regel(Component.translatable("erfenis.ui.wordt_opgeruimd").append(" · " + Teken.tijd(bouw.eind() - nu)), Teken.DONKER);
            Teken.balk(ui.g, r.x, r.y, r.x + r.w, r.y + 5, bouw.voortgang(nu), Teken.GOUD);
            r.y += 9;
        } else {
            if (b.hoeveLevel() < Prijzen.ringOpent(ring)) {
                r.regel(Component.translatable("erfenis.fout.slot", Prijzen.ringOpent(ring)), Teken.ROOD);
            }
            prijs(ui, r, b, p);
            if (eigen) {
                Base.Fout f = b.kanOpruimen(n);
                r.knop(Component.translatable("erfenis.ui.opruimen"), r.x, r.x + r.w, f == null, Teken.GROEN,
                        () -> ErfenisClient.stuur(new Actie(Actie.OPRUIMEN, 0, n, 0, 0, 0, 0, "")),
                        f == null ? null : List.of(ErfenisClient.tekst(f.key(), List.of(f.args()))));
            }
        }
        return y0 + h;
    }

    private static final ItemStack[] PLUKJE_ICOON = {new ItemStack(net.minecraft.world.item.Items.OAK_LEAVES), new ItemStack(net.minecraft.world.item.Items.OAK_LOG),
            new ItemStack(net.minecraft.world.item.Items.MOSSY_COBBLESTONE), new ItemStack(net.minecraft.world.item.Items.TALL_GRASS),
            new ItemStack(net.minecraft.world.item.Items.BARREL), new ItemStack(net.minecraft.world.item.Items.ROOTED_DIRT)};

    int kavel(Ui ui, Base b, int k, int x0, int y0, boolean eigen) {
        int x1 = x0 + B, h = 104;
        Teken.paneel(ui.g, x0, y0, x1, y0 + h);
        ui.vlak(x0, y0, x1, y0 + h, null);
        Regels r = new Regels(ui, x0 + 8, y0 + 6, B - 16, y0, y0 + h);
        ui.item(new ItemStack(net.minecraft.world.item.Items.OAK_SIGN), x0 + 6, r.y);
        ui.tekst(Component.translatable("erfenis.ui.kavel").withStyle(ChatFormatting.BOLD), x0 + 26, r.y, Teken.DONKER);
        int[] v = Plot.kavelVak(k);
        ui.tekst(v[2] + " x " + v[3], x0 + 26, r.y + 10, Teken.ROZE_DONKER);
        r.y += 22;
        r.regelWrap(Component.translatable("erfenis.ui.kavel_uitleg", b.kavelsGekocht(), Prijzen.maxKavels(b.hoeveLevel())), Teken.ZACHT);
        prijs(ui, r, b, Prijzen.kavel(b.kavelsGekocht() + 1));
        if (eigen) {
            Base.Fout f = b.kanKavel(k);
            r.knop(Component.translatable("erfenis.ui.kavel_kopen"), r.x, r.x + r.w, f == null, Teken.GROEN,
                    () -> ErfenisClient.stuur(new Actie(Actie.KAVEL, 0, k, 0, 0, 0, 0, "")),
                    f == null ? null : List.of(ErfenisClient.tekst(f.key(), List.of(f.args()))));
        }
        return y0 + h;
    }

    // =====================================================================================================================

    /** A cursor down a panel: lines, wrapped lines, separators and buttons, only drawn (and clickable) where visible. */
    static final class Regels {
        final Ui ui;
        final int x, w, zichtTop, zichtBodem;
        int y;

        Regels(Ui ui, int x, int y, int w, int zichtTop, int zichtBodem) {
            this.ui = ui;
            this.x = x;
            this.y = y;
            this.w = w;
            this.zichtTop = zichtTop;
            this.zichtBodem = zichtBodem;
        }

        boolean zichtbaar(int a, int b) {
            return b > zichtTop + 2 && a < zichtBodem - 2;
        }

        void regel(Component t, int kleur) {
            if (zichtbaar(y, y + 9)) {
                ui.passend(t, x, y, w, kleur);
            }
            y += 10;
        }

        void regel(FormattedCharSequence t, int kleur) {
            if (zichtbaar(y, y + 9)) {
                ui.tekst(t, x, y, kleur);
            }
            y += 9;
        }

        void regelWrap(Component t, int kleur) {
            for (FormattedCharSequence s : ui.font.split(t, w)) {
                regel(s, kleur);
            }
            y += 1;
        }

        void streep() {
            y += 1;
            if (zichtbaar(y, y + 1)) {
                ui.g.fill(x, y, x + w, y + 1, 0x40D27A9C);
            }
            y += 4;
        }

        void knop(Component t, int x0, int x1, boolean kan, int kleur, Runnable actie, List<Component> tip) {
            if (zichtbaar(y, y + 14)) {
                ui.knop(t, x0, y, x1, y + 14, kan, kleur, actie, tip);
            }
            y += 17;
        }
    }
}
