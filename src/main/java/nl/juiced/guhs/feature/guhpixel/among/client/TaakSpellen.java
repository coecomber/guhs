package nl.juiced.guhs.feature.guhpixel.among.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.guhpixel.among.TaakSoorten;
import nl.juiced.guhs.feature.guhpixel.among.Taken;

/**
 * The eight task mini-games of Among Guhs and the two repair panels, client side (the classics, in guh style):
 * worstjes knopen (wires), pasje door de lezer (not too fast, not too slow), kruimelbak legen (hold the lever),
 * pindasaus tanken (stop at the line; two panels), knabbels sorteren, dromen downloaden/uploaden (a wait bar in two
 * rooms), wegen (keep still: "Resultaat: vads"), schakelaars goedzetten; and for sabotage: the light switches in Elektra
 * and the code of the Knabbelalarm. What a panel asks comes from the server ({@link Taken}); the answer goes back and is
 * checked there.
 */
public final class TaakSpellen {
    static void registreer() {
        AmongClient.taakScherm(Taken.WORSTJES, Worstjes::new);
        AmongClient.taakScherm(Taken.PASJE, Pasje::new);
        AmongClient.taakScherm(Taken.KRUIMELBAK, Kruimelbak::new);
        AmongClient.taakScherm(Taken.PINDASAUS, Pindasaus::new);
        AmongClient.taakScherm(Taken.SORTEREN, Sorteren::new);
        AmongClient.taakScherm(Taken.DROMEN, Dromen::new);
        AmongClient.taakScherm(Taken.WEGEN, Wegen::new);
        AmongClient.taakScherm(Taken.SCHAKELAARS, Schakelaars::new);
        AmongClient.taakScherm(TaakSoorten.HERSTEL_LICHT, Schakelaars::new);
        AmongClient.taakScherm(TaakSoorten.HERSTEL_ALARM, Alarmcode::new);
    }

    private static int[] reeks(CompoundTag tag, String sleutel, int lengte) {
        int[] a = tag.getIntArray(sleutel).orElse(null);
        return a != null && a.length == lengte ? a.clone() : new int[lengte];
    }

    private static int donkerder(int kleur) {
        int r = (kleur >> 16 & 0xFF) * 2 / 3, gr = (kleur >> 8 & 0xFF) * 2 / 3, b = (kleur & 0xFF) * 2 / 3;
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    // =====================================================================================================================
    /** Worstjes knopen: drag every sausage from the left to the hook of its own colour on the right. */
    static final class Worstjes extends TaakSpel {
        private static final int[] KLEUREN = {0xFFE0524A, 0xFFF2D53C, 0xFF5B8CF0, 0xFFF28CC8};
        private final int[] rechts;
        private final int[] paren = {-1, -1, -1, -1};
        private int sleep = -1;

        Worstjes(CompoundTag data) {
            super(data);
            rechts = reeks(opgave, "Rechts", Taken.WORSTEN);
        }

        private int rijY(int i) {
            return vy + 16 + i * 25;
        }

        private void worst(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int kleur) {
            double lengte = Math.max(1, Math.hypot(x1 - x0, y1 - y0));
            int n = Math.max(1, (int) (lengte / 9));
            int donker = donkerder(kleur);
            for (int k = 0; k <= n; k++) {
                int cx = x0 + (x1 - x0) * k / n, cy = y0 + (y1 - y0) * k / n;
                g.fill(cx - 4, cy - 3, cx + 4, cy + 3, donker);
                g.fill(cx - 3, cy - 2, cx + 3, cy + 2, kleur);
                g.fill(cx - 2, cy - 2, cx, cy - 1, 0x60FFFFFF);
            }
        }

        @Override
        protected void teken(GuiGraphicsExtractor g, float partialTick) {
            int lx = vx + 22, rx = vx + vw - 22;
            for (int i = 0; i < Taken.WORSTEN; i++) {
                int y = rijY(i);
                vak(g, vx + 4, y - 8, lx, y + 8, METAAL, KLEUREN[i]);
                int kleurRechts = KLEUREN[rechts[i]];
                vak(g, rx, y - 8, vx + vw - 4, y + 8, METAAL, kleurRechts);
                g.fill(rx - 4, y - 2, rx, y + 2, METAAL_D);            // the hook
            }
            for (int i = 0; i < Taken.WORSTEN; i++) {
                if (paren[i] >= 0) {
                    worst(g, lx + 2, rijY(i), rx - 4, rijY(paren[i]), KLEUREN[i]);
                } else if (sleep == i) {
                    worst(g, lx + 2, rijY(i), muisX, muisY, KLEUREN[i]);
                } else {
                    worst(g, lx + 2, rijY(i), lx + 18, rijY(i) + 5, KLEUREN[i]);
                }
            }
        }

        @Override
        protected void druk(int x, int y) {
            for (int i = 0; i < Taken.WORSTEN; i++) {
                if (paren[i] < 0 && in(x, y, vx, rijY(i) - 12, vx + 60, rijY(i) + 12)) {
                    sleep = i;
                    tik(1.4f);
                }
            }
        }

        @Override
        protected void los(int x, int y) {
            if (sleep < 0) {
                return;
            }
            int i = sleep;
            sleep = -1;
            for (int haak = 0; haak < Taken.WORSTEN; haak++) {
                if (in(x, y, vx + vw - 60, rijY(haak) - 12, vx + vw, rijY(haak) + 12)) {
                    if (rechts[haak] != i) {
                        mis("worstjes.fout");
                        return;
                    }
                    paren[i] = haak;
                    tik(1.8f);
                    for (int p : paren) {
                        if (p < 0) {
                            return;
                        }
                    }
                    CompoundTag r = new CompoundTag();
                    r.putIntArray("Paren", paren.clone());
                    gelukt(r);
                    return;
                }
            }
        }
    }

    // =====================================================================================================================
    /** Pasje door de lezer: drag the card through the reader in one go, not too fast and not too slow. */
    static final class Pasje extends TaakSpel {
        private static final int KAART_W = 44, KAART_H = 28;
        private final int min, max;
        private boolean sleept;
        private int kaartX, greep;
        private long begin;

        Pasje(CompoundTag data) {
            super(data);
            min = opgave.getIntOr("Min", 550);
            max = opgave.getIntOr("Max", 950);
        }

        private int x0() {
            return vx + 10;
        }

        private int x1() {
            return vx + vw - 10 - KAART_W;
        }

        private int kaartY() {
            return vy + 20;
        }

        @Override
        protected void init() {
            super.init();
            if (!sleept) {
                kaartX = x0();
            }
        }

        @Override
        protected void teken(GuiGraphicsExtractor g, float partialTick) {
            int y = kaartY();
            // the reader: a slot with a little light
            vak(g, vx + 6, y + KAART_H - 8, vx + vw - 6, y + KAART_H + 22, METAAL, 0xFF3A2A44);
            g.fill(vx + 10, y + KAART_H - 4, vx + vw - 10, y + KAART_H - 1, DONKER);
            g.fill(vx + vw - 22, y + KAART_H + 6, vx + vw - 14, y + KAART_H + 14, isKlaar() ? GROEN : ROOD);
            g.text(font, Component.translatable("gui.guhs.among.taak.pasje.lezer"), vx + 12, y + KAART_H + 8, DOF, false);
            if (sleept) {
                kaartX = Math.max(x0(), Math.min(x1(), muisX - greep));
            }
            // the card: a Vadsvaarder crew pass with a little guh on it
            vak(g, kaartX, y, kaartX + KAART_W, y + KAART_H, 0xFF7A4A66, 0xFFFFF4F8);
            g.fill(kaartX + 1, y + 1, kaartX + KAART_W - 1, y + 7, 0xFFF28CC8);
            plaatje(g, GUH, kaartX + 3, y + 9, 16);
            g.fill(kaartX + 22, y + 12, kaartX + 40, y + 14, 0xFFB090A0);
            g.fill(kaartX + 22, y + 17, kaartX + 36, y + 19, 0xFFB090A0);
            g.fill(kaartX + 1, y + KAART_H - 5, kaartX + KAART_W - 1, y + KAART_H - 2, DONKER);
            // the arrow
            g.centeredText(font, Component.literal("»  »  »"), vx + vw / 2, y + KAART_H + 30, DOF);
        }

        @Override
        protected void druk(int x, int y) {
            if (in(x, y, kaartX, kaartY(), kaartX + KAART_W, kaartY() + KAART_H) && kaartX <= x0() + 6) {
                sleept = true;
                greep = x - kaartX;
                begin = System.currentTimeMillis();
            }
        }

        @Override
        protected void spelTick() {
            if (sleept && kaartX >= x1()) {
                sleept = false;
                int ms = (int) Math.min(60_000, System.currentTimeMillis() - begin);
                if (ms < min) {
                    kaartX = x0();
                    mis("pasje.te_vlug");
                } else if (ms > max) {
                    kaartX = x0();
                    mis("pasje.te_traag");
                } else {
                    CompoundTag r = new CompoundTag();
                    r.putInt("Ms", ms);
                    gelukt(r);
                }
            }
        }

        @Override
        protected void los(int x, int y) {
            if (sleept) {
                sleept = false;
                if (kaartX < x1()) {
                    kaartX = x0();
                    mis("pasje.half");
                }
            }
        }
    }

    // =====================================================================================================================
    /** Kruimelbak legen: hold the lever until every crumb has flown out. */
    static final class Kruimelbak extends TaakSpel {
        private final int houd;
        private int vast;

        Kruimelbak(CompoundTag data) {
            super(data);
            houd = Math.max(1, opgave.getIntOr("Houd", 60));
        }

        private boolean opHendel() {
            return in(muisX, muisY, vx + vw - 70, vy + 6, vx + vw - 8, vy + vh - 6);
        }

        @Override
        protected void spelTick() {
            if (ingedrukt && opHendel()) {
                if (++vast >= houd) {
                    CompoundTag r = new CompoundTag();
                    r.putInt("Vast", vast);
                    gelukt(r);
                } else if (vast % 6 == 0) {
                    tik(0.7f + 0.6f * vast / houd);
                }
            } else if (vast > 0) {
                vast = Math.max(0, vast - 2);       // the lever springs back
            }
        }

        @Override
        protected void teken(GuiGraphicsExtractor g, float partialTick) {
            float leeg = Math.min(1f, vast / (float) houd);
            // the bin with its crumbs
            int bx = vx + 14, by = vy + 8, bw = 110, bh = vh - 30;
            vak(g, bx, by, bx + bw, by + bh, METAAL, 0xFF3A2A44);
            int peil = Math.round((bh - 4) * (1f - leeg));
            g.fill(bx + 2, by + bh - 2 - peil, bx + bw - 2, by + bh - 2, 0xFFD9A857);
            for (int i = 0; i < 26; i++) {
                int kx = bx + 4 + (i * 37) % (bw - 10), ky = by + bh - 4 - (i * 23) % Math.max(1, peil);
                if (peil > 6) {
                    g.fill(kx, ky, kx + 3, ky + 2, i % 3 == 0 ? 0xFFF4D48A : 0xFFB8843A);
                }
            }
            // the hatch under it, and the crumbs flying into space
            boolean open = ingedrukt && opHendel() && !isKlaar();
            g.fill(bx + 30, by + bh, bx + bw - 30, by + bh + 4, open ? DONKER : METAAL_D);
            if (open) {
                for (int i = 0; i < 9; i++) {
                    int val = (leeftijd * 5 + i * 11) % 18;
                    g.fill(bx + 36 + (i * 7) % 40, by + bh + 4 + val, bx + 39 + (i * 7) % 40, by + bh + 6 + val, 0xFFF4D48A);
                }
            }
            // the lever
            int hx = vx + vw - 44, boven = vy + 12, onder = vy + vh - 22;
            g.fill(hx - 3, boven, hx + 3, onder, METAAL_D);
            int knop = boven + Math.round((onder - boven - 16) * leeg);
            vak(g, hx - 16, knop, hx + 16, knop + 16, 0xFF7A1F1F, open ? 0xFFFF8A80 : 0xFFE0524A);
            balk(g, vx + 14, vy + vh - 12, vw - 28, 6, leeg, GROEN);
        }
    }

    // =====================================================================================================================
    /** Pindasaus tanken: hold the button and let go at the line. Too much = spilled, and the tank runs empty again. */
    static final class Pindasaus extends TaakSpel {
        private final int doel, marge;
        private final boolean gieten;
        private float peil;
        private boolean tankt;
        private int morsTeller;

        Pindasaus(CompoundTag data) {
            super(data);
            doel = opgave.getIntOr("Doel", 75);
            marge = opgave.getIntOr("Marge", 5);
            gieten = data.getIntOr("Stap", 1) > 1;
        }

        private boolean opKnop() {
            return in(muisX, muisY, vx + vw - 96, vy + vh - 40, vx + vw - 12, vy + vh - 10);
        }

        @Override
        protected Component uitleg() {
            return Component.translatable(gieten ? "gui.guhs.among.taak.pindasaus.gieten" : "gui.guhs.among.taak.pindasaus.bezig");
        }

        @Override
        protected void spelTick() {
            if (morsTeller > 0) {
                morsTeller--;
                peil = Math.max(0f, peil - 4f);
                return;
            }
            boolean nu = ingedrukt && opKnop();
            if (nu) {
                peil += 1.15f;
                if (leeftijd % 4 == 0) {
                    tik(0.6f + peil / 120f);
                }
                if (peil > doel + marge) {
                    morsTeller = 30;
                    mis("pindasaus.gemorst");
                }
            } else if (tankt) {
                if (Math.abs(Math.round(peil) - doel) <= marge) {
                    CompoundTag r = new CompoundTag();
                    r.putInt("Peil", Math.round(peil));
                    gelukt(r);
                } else {
                    meld(Component.translatable("gui.guhs.among.taak.pindasaus.te_weinig"), GOUD, 40);
                }
            }
            tankt = nu;
        }

        @Override
        protected void teken(GuiGraphicsExtractor g, float partialTick) {
            // the tank (step 1: the jerrycan in the Voorraadkamer; step 2: the engine's tank in the Machinekamer)
            int tx = vx + 30, ty = vy + 8, tw = 70, th = vh - 16;
            vak(g, tx, ty, tx + tw, ty + th, METAAL, 0xFF3A2A44);
            int hoogte = Math.round((th - 4) * Math.min(100f, peil) / 100f);
            g.fill(tx + 2, ty + th - 2 - hoogte, tx + tw - 2, ty + th - 2, morsTeller > 0 ? 0xFFB06A2A : 0xFFC98A3C);
            if (hoogte > 2) {
                g.fill(tx + 2, ty + th - 2 - hoogte, tx + tw - 2, ty + th - hoogte, 0xFFE8B66A);
            }
            int lijn = ty + th - 2 - Math.round((th - 4) * doel / 100f);
            int band = Math.max(1, Math.round((th - 4) * marge / 100f));
            g.fill(tx - 6, lijn - band, tx + tw + 6, lijn + band, 0x5068D88A);
            g.fill(tx - 8, lijn, tx + tw + 8, lijn + 1, GROEN);
            g.text(font, Component.translatable("gui.guhs.among.taak.pindasaus.streep"), tx + tw + 12, lijn - 4, GROEN, false);
            plaatje(g, JERRYCAN, vx + 4, vy + vh - 26, 16);
            // the button
            boolean aan = ingedrukt && opKnop() && morsTeller == 0 && !isKlaar();
            vak(g, vx + vw - 96, vy + vh - 40, vx + vw - 12, vy + vh - 10, 0xFF7A4A1F, aan ? 0xFFE8B66A : 0xFFC98A3C);
            g.centeredText(font, Component.translatable(gieten ? "gui.guhs.among.taak.pindasaus.knop_gieten" : "gui.guhs.among.taak.pindasaus.knop"),
                    vx + vw - 54, vy + vh - 29, DONKER);
            g.text(font, Component.literal(Math.round(Math.min(100f, peil)) + "%"), vx + vw - 96, vy + 12, TEKST, false);
        }
    }

    // =====================================================================================================================
    /** Knabbels sorteren: drag every piece into its own bin: kaasknabbels, plain knabbels, crumbs. */
    static final class Sorteren extends TaakSpel {
        private final int[] soorten;
        private final int[] bakken = new int[Taken.KNABBELS];
        private int sleep = -1;

        Sorteren(CompoundTag data) {
            super(data);
            soorten = reeks(opgave, "Soorten", Taken.KNABBELS);
            java.util.Arrays.fill(bakken, -1);
        }

        private int stukX(int i) {
            return vx + 14 + i * 37;
        }

        private int bakX(int b) {
            return vx + 4 + b * 80;
        }

        @Override
        protected void teken(GuiGraphicsExtractor g, float partialTick) {
            int by = vy + vh - 44;
            for (int b = 0; b < Taken.BAKKEN; b++) {
                int x = bakX(b), n = 0;
                for (int i = 0; i < Taken.KNABBELS; i++) {
                    n += bakken[i] == b ? 1 : 0;
                }
                boolean boven = sleep >= 0 && in(muisX, muisY, x, by - 6, x + 72, vy + vh);
                vak(g, x, by, x + 72, by + 40, boven ? GOUD : METAAL, 0xFF3A2A44);
                plaatje(g, b, x + 4, by + 4, 16);
                g.text(font, Component.translatable("gui.guhs.among.taak.sorteren.bak." + b), x + 23, by + 5, TEKST, false);
                for (int k = 0; k < n; k++) {
                    plaatje(g, b, x + 4 + k * 12, by + 21, 16);
                }
            }
            for (int i = 0; i < Taken.KNABBELS; i++) {
                if (bakken[i] >= 0) {
                    continue;
                }
                if (sleep == i) {
                    plaatje(g, soorten[i], muisX - 12, muisY - 12, 24);
                } else {
                    plaatje(g, soorten[i], stukX(i), vy + 8 + (i % 2) * 6, 24);
                }
            }
        }

        @Override
        protected void druk(int x, int y) {
            for (int i = 0; i < Taken.KNABBELS; i++) {
                if (bakken[i] < 0 && in(x, y, stukX(i) - 2, vy + 4, stukX(i) + 28, vy + 42)) {
                    sleep = i;
                    tik(1.5f);
                }
            }
        }

        @Override
        protected void los(int x, int y) {
            if (sleep < 0) {
                return;
            }
            int i = sleep;
            sleep = -1;
            for (int b = 0; b < Taken.BAKKEN; b++) {
                if (in(x, y, bakX(b), vy + vh - 50, bakX(b) + 72, vy + vh)) {
                    if (soorten[i] != b) {
                        mis("sorteren.fout");
                        return;
                    }
                    bakken[i] = b;
                    tik(1.9f);
                    for (int bak : bakken) {
                        if (bak < 0) {
                            return;
                        }
                    }
                    CompoundTag r = new CompoundTag();
                    r.putIntArray("Bakken", bakken.clone());
                    gelukt(r);
                    return;
                }
            }
        }
    }

    // =====================================================================================================================
    /** Dromen downloaden (in a room) and uploaden (in the Kantine): press start and wait for the bar. It takes a while, njeg. */
    static final class Dromen extends TaakSpel {
        private final int wacht, droom;
        private final boolean upload;
        private int teller = -1;

        Dromen(CompoundTag data) {
            super(data);
            wacht = Math.max(1, opgave.getIntOr("Wacht", 170));
            upload = opgave.getBooleanOr("Upload", false);
            droom = opgave.getIntOr("Droom", 0);
        }

        @Override
        protected Component uitleg() {
            return Component.translatable(upload ? "gui.guhs.among.taak.dromen.upload" : "gui.guhs.among.taak.dromen.bezig");
        }

        private boolean opKnop(int x, int y) {
            return in(x, y, vx + vw / 2 - 45, vy + vh - 30, vx + vw / 2 + 45, vy + vh - 8);
        }

        @Override
        protected void druk(int x, int y) {
            if (teller < 0 && opKnop(x, y)) {
                teller = 0;
                tik(1.2f);
            }
        }

        @Override
        protected void spelTick() {
            if (teller >= 0 && ++teller >= wacht) {
                gelukt(new CompoundTag());
            }
        }

        @Override
        protected void teken(GuiGraphicsExtractor g, float partialTick) {
            float deel = teller < 0 ? 0f : Math.min(1f, (teller + partialTick) / wacht);
            // a sleeping guh on one side, the ship's computer on the other, the dream travelling between them
            int gx = upload ? vx + vw - 50 : vx + 18, cx = upload ? vx + 18 : vx + vw - 50;
            plaatje(g, SLAAPGUH, upload ? cx : gx, vy + 8, 32);
            vak(g, (upload ? gx : cx), vy + 8, (upload ? gx : cx) + 32, vy + 34, METAAL, 0xFF24303E);
            g.fill((upload ? gx : cx) + 10, vy + 34, (upload ? gx : cx) + 22, vy + 38, METAAL_D);
            if (teller >= 0 && !isKlaar()) {
                int van = vx + 54, tot = vx + vw - 70;
                int wx = van + Math.round((tot - van) * ((leeftijd * 3 % 100) / 100f));
                plaatje(g, WOLK, wx, vy + 12 + (leeftijd / 4 % 2), 16);
            }
            balk(g, vx + 16, vy + 50, vw - 32, 10, deel, upload ? 0xFF7FDBFF : GROEN);
            g.centeredText(font, Component.literal(Math.round(deel * 100) + "%"), vx + vw / 2, vy + 51, TEKST);
            if (teller < 0) {
                vak(g, vx + vw / 2 - 45, vy + vh - 30, vx + vw / 2 + 45, vy + vh - 8, RAND, opKnop(muisX, muisY) ? 0xFF7A4A66 : 0xFF5A3A4C);
                g.centeredText(font, Component.translatable(upload ? "gui.guhs.among.taak.dromen.knop_upload" : "gui.guhs.among.taak.dromen.knop"),
                        vx + vw / 2, vy + vh - 23, TEKST);
            } else {
                // the file name changes now and then; the estimate is never right
                int stuk = Math.min(3, teller * 4 / wacht);
                g.centeredText(font, Component.translatable("gui.guhs.among.taak.dromen.bestand", Component.translatable("gui.guhs.among.droom." + (droom + stuk) % 8)),
                        vx + vw / 2, vy + 66, GOUD);
                int[] dutjes = {3, 47, 2, 1};
                g.centeredText(font, Component.translatable("gui.guhs.among.taak.dromen.schatting", dutjes[stuk]), vx + vw / 2, vy + 78, DOF);
            }
        }
    }

    // =====================================================================================================================
    /** Wegen in de Ziekenboeg: keep the pointer still on the scale. Moving makes the needle swing and the count start over. */
    static final class Wegen extends TaakSpel {
        private final int nodig, gewicht;
        private int stil, vorigX, vorigY, zwaai;

        Wegen(CompoundTag data) {
            super(data);
            nodig = Math.max(1, opgave.getIntOr("Stil", 90));
            gewicht = opgave.getIntOr("Gewicht", 5);
        }

        private boolean opSchaal() {
            return in(muisX, muisY, vx + vw / 2 - 56, vy + 22, vx + vw / 2 + 56, vy + vh - 8);
        }

        @Override
        protected void spelTick() {
            int beweging = Math.abs(muisX - vorigX) + Math.abs(muisY - vorigY);
            vorigX = muisX;
            vorigY = muisY;
            if (!opSchaal() || beweging > 2) {
                if (stil > 8) {
                    mis("wegen.bewoog");
                }
                stil = 0;
                zwaai = 20;
                return;
            }
            if (zwaai > 0) {
                zwaai--;
            }
            if (++stil >= nodig) {
                CompoundTag r = new CompoundTag();
                r.putInt("Stil", stil);
                gelukt(r);
                meld(Component.translatable("gui.guhs.among.taak.wegen.resultaat"), GROEN, 60);
            }
        }

        @Override
        protected void teken(GuiGraphicsExtractor g, float partialTick) {
            int mx = vx + vw / 2;
            // the dial
            vak(g, mx - 40, vy + 4, mx + 40, vy + 22, METAAL, 0xFFFFF4F8);
            for (int i = 0; i <= 10; i++) {
                g.fill(mx - 35 + i * 7, vy + 6, mx - 34 + i * 7, vy + (i % 5 == 0 ? 12 : 9), DONKER);
            }
            float deel = Math.min(1f, stil / (float) nodig);
            int uitslag = Math.round(deel * gewicht * 7) + (zwaai > 0 ? (int) (Math.sin(leeftijd * 1.3) * zwaai) : 0);
            int nx = Math.max(mx - 36, Math.min(mx + 36, mx - 35 + uitslag));
            g.fill(nx, vy + 6, nx + 2, vy + 20, ROOD);
            // the scale with a guh on it
            vak(g, mx - 56, vy + vh - 26, mx + 56, vy + vh - 12, METAAL, METAAL_D);
            g.fill(mx - 46, vy + vh - 12, mx + 46, vy + vh - 6, 0xFF5A5A68);
            int wiebel = zwaai > 0 ? (leeftijd / 2 % 2) * 2 - 1 : 0;
            plaatje(g, isKlaar() ? SLAAPGUH : GUH, mx - 16 + wiebel, vy + vh - 58, 32);
            balk(g, vx + 14, vy + vh - 4, vw - 28, 3, deel, GROEN);
            if (isKlaar()) {
                g.centeredText(font, Component.translatable("gui.guhs.among.taak.wegen.resultaat"), mx, vy + 26, GROEN);
            }
        }
    }

    // =====================================================================================================================
    /**
     * Schakelaars goedzetten: every switch like the little lamp above it. The repair panel of "Licht uit" in Elektra is the
     * same panel with five switches that all have to go up.
     */
    static final class Schakelaars extends TaakSpel {
        private final int aantal, doel;
        private final boolean licht;
        private int stand;

        Schakelaars(CompoundTag data) {
            super(data);
            licht = TaakSoorten.HERSTEL_LICHT.equals(soort);
            aantal = licht ? Taken.LICHTEN : Taken.SCHAKELS;
            doel = licht ? (1 << Taken.LICHTEN) - 1 : opgave.getIntOr("Doel", 0);
            stand = opgave.getIntOr("Begin", 0);
        }

        private int sx(int i) {
            int stap = (vw - 20) / aantal;
            return vx + 10 + i * stap + stap / 2;
        }

        @Override
        protected void teken(GuiGraphicsExtractor g, float partialTick) {
            for (int i = 0; i < aantal; i++) {
                int x = sx(i);
                boolean aan = (stand >> i & 1) == 1, moet = (doel >> i & 1) == 1;
                // the lamp: what the switch should be (the light panel: whether this lamp burns)
                boolean lamp = licht ? aan : moet;
                vak(g, x - 7, vy + 8, x + 7, vy + 22, DONKER, lamp ? 0xFFFFE27A : 0xFF4A3A30);
                if (!licht) {
                    g.centeredText(font, Component.literal(moet ? "▲" : "▼"), x, vy + 11, lamp ? DONKER : DOF);
                }
                // the switch
                vak(g, x - 9, vy + 30, x + 9, vy + vh - 10, METAAL, 0xFF3A2A44);
                int ky = aan ? vy + 33 : vy + vh - 33;
                vak(g, x - 7, ky, x + 7, ky + 20, DONKER, aan == moet ? GROEN : ROOD);
                g.fill(x - 4, ky + 9, x + 4, ky + 11, 0x80FFFFFF);
            }
        }

        @Override
        protected void druk(int x, int y) {
            for (int i = 0; i < aantal; i++) {
                if (in(x, y, sx(i) - 12, vy + 26, sx(i) + 12, vy + vh - 6)) {
                    stand ^= 1 << i;
                    tik((stand >> i & 1) == 1 ? 1.7f : 1.1f);
                    if (stand == doel) {
                        CompoundTag r = new CompoundTag();
                        r.putInt("Stand", stand);
                        gelukt(r);
                    }
                    return;
                }
            }
        }
    }

    // =====================================================================================================================
    /** The code panel of the Knabbelalarm: type the four digits of the note. */
    static final class Alarmcode extends TaakSpel {
        private final int code;
        private String getypt = "";

        Alarmcode(CompoundTag data) {
            super(data);
            code = opgave.getIntOr("Code", 0);
        }

        private int toetsX(int i) {
            return vx + 96 + (i % 3) * 30;
        }

        private int toetsY(int i) {
            return vy + 4 + (i / 3) * 25;
        }

        /** The label of key i: 1..9, wis, 0, ok. */
        private static String toets(int i) {
            return i < 9 ? String.valueOf(i + 1) : i == 9 ? "×" : i == 10 ? "0" : "✔";
        }

        @Override
        protected void teken(GuiGraphicsExtractor g, float partialTick) {
            // the note with the code, stuck beside the keypad
            vak(g, vx + 8, vy + 10, vx + 84, vy + 52, 0xFFB8A04A, 0xFFFFF2A8);
            g.centeredText(font, Component.translatable("gui.guhs.among.taak.herstel_alarm.briefje"), vx + 46, vy + 16, 0xFF5A4A1A);
            g.centeredText(font, Component.literal(String.format("%04d", code)).withStyle(net.minecraft.ChatFormatting.BOLD), vx + 46, vy + 32, 0xFF3A2A0A);
            // what was typed
            vak(g, vx + 8, vy + 62, vx + 84, vy + 82, METAAL, DONKER);
            StringBuilder toon = new StringBuilder(getypt);
            while (toon.length() < Taken.CODE_LENGTE) {
                toon.append('_');
            }
            g.centeredText(font, Component.literal(toon.toString()), vx + 46, vy + 68, GROEN);
            for (int i = 0; i < 12; i++) {
                int x = toetsX(i), y = toetsY(i);
                boolean boven = in(muisX, muisY, x, y, x + 27, y + 22);
                vak(g, x, y, x + 27, y + 22, METAAL, boven ? 0xFF7A4A66 : 0xFF5A3A4C);
                g.centeredText(font, Component.literal(toets(i)), x + 14, y + 7, i == 9 ? ROOD : i == 11 ? GROEN : TEKST);
            }
        }

        private void klaarOfFout() {
            if (getypt.length() == Taken.CODE_LENGTE && Integer.parseInt(getypt) == code) {
                CompoundTag r = new CompoundTag();
                r.putInt("Code", code);
                gelukt(r);
            } else {
                getypt = "";
                mis("herstel_alarm.fout");
            }
        }

        @Override
        protected void druk(int x, int y) {
            for (int i = 0; i < 12; i++) {
                if (in(x, y, toetsX(i), toetsY(i), toetsX(i) + 27, toetsY(i) + 22)) {
                    tik(1.3f + i * 0.04f);
                    if (i == 9) {
                        getypt = "";
                    } else if (i == 11) {
                        klaarOfFout();
                    } else if (getypt.length() < Taken.CODE_LENGTE) {
                        getypt += i == 10 ? "0" : String.valueOf(i + 1);
                        if (getypt.length() == Taken.CODE_LENGTE) {
                            klaarOfFout();
                        }
                    }
                    return;
                }
            }
        }
    }

    private TaakSpellen() {
    }
}
