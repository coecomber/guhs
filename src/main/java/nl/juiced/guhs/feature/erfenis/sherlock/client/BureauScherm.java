package nl.juiced.guhs.feature.erfenis.sherlock.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.erfenis.sherlock.SherlockPayloads;
import org.lwjgl.glfw.GLFW;

/**
 * The Detectivebureau: Dokter Vadson's Speurboekje (every chapter, its state, a button to start / continue / read back)
 * and the speurbord of one case: a cork board with the cards left and the polaroids of the suspects right. Click a card,
 * then a suspect: a red thread. Right-click a card: loose again. Three threads on one suspect and "Wijs aan!" lights up.
 * The server decides everything (SherlockPayloads); this screen only shows its state and sends what was clicked.
 */
public final class BureauScherm extends Screen {
    private static final int W = 460, H = 270;
    private static final int KURK = 0xFFC4935E, KURK_DONKER = 0xFFA87645, PAPIER = 0xFFFFF8E8, PAPIER_BLAUW = 0xFFE6F2FF, INKT = 0xFF4A3426,
            ROOD = 0xFFD8323C, ROZE = 0xFFF7B6CB, DONKER = 0xFF6A2848, GROEN = 0xFF5FA85A, GRIJS = 0xFFB4A8A0, GOUD = 0xFFE8B83A;

    private CompoundTag staat;
    private int x0, y0;
    /** Speurboekje: the chapter on the right. Board: the card picked to pin. */
    private int gekozen = -1;
    @Nullable
    private String kaart;

    private record Klik(int x0, int y0, int x1, int y1, Runnable wat, boolean rechts) {
        boolean op(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    private final List<Klik> klikken = new ArrayList<>();
    private final List<Klik> rechtsKlikken = new ArrayList<>();

    public BureauScherm(CompoundTag staat) {
        super(Component.translatable("sherlock.ui.bureau"));
        this.staat = staat;
    }

    public void update(CompoundTag nieuw) {
        if (!nieuw.getStringOr("Modus", "").equals(staat.getStringOr("Modus", ""))) {
            gekozen = -1;
            kaart = null;
        }
        this.staat = nieuw;
    }

    private boolean bord() {
        return staat.getStringOr("Modus", "").equals("bord") && staat.contains("Bord");
    }

    private static void stuur(String soort, String a, String b) {
        ClientPacketDistributor.sendToServer(new SherlockPayloads.BordActie(soort, a, b));
    }

    // =====================================================================================================================

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        super.extractBackground(g, mx, my, partialTick);
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        klikken.clear();
        rechtsKlikken.clear();
        if (bord()) {
            tekenBord(g, mx, my);
        } else {
            tekenBoekje(g, mx, my);
        }
    }

    // --- the Speurboekje ---

    private void tekenBoekje(GuiGraphicsExtractor g, int mx, int my) {
        // a little notebook: leather cover, two cream pages
        g.fill(x0 - 2, y0 - 2, x0 + W + 2, y0 + H + 2, 0xFF5A3424);
        g.fill(x0, y0, x0 + W, y0 + H, 0xFF7A4A30);
        int pl = x0 + 8, pr = x0 + W - 8, pt = y0 + 8, pb = y0 + H - 8, mid = x0 + W / 2;
        g.fill(pl, pt, mid - 1, pb, PAPIER);
        g.fill(mid + 1, pt, pr, pb, PAPIER);
        g.fill(mid - 1, pt, mid + 1, pb, 0xFFD8C8A8);
        for (int y = pt + 26; y < pb - 4; y += 12) {
            g.fill(pl + 4, y, mid - 5, y + 1, 0x1A3060A0);
            g.fill(mid + 5, y, pr - 4, y + 1, 0x1A3060A0);
        }
        g.text(font, Component.translatable("sherlock.ui.boekje").withStyle(ChatFormatting.BOLD), pl + 8, pt + 8, DONKER, false);

        List<CompoundTag> zaken = zaken();
        if (gekozen < 0) {
            gekozen = 0;
            for (int i = 0; i < zaken.size(); i++) {
                if (!zaken.get(i).getStringOr("Status", "").equals("OPGELOST")) {
                    gekozen = i;
                    break;
                }
            }
        }
        // the prologue line
        int y = pt + 24;
        boolean sleutel = staat.getBooleanOr("Sleuteltje", false);
        Component proloog = Component.translatable(sleutel ? "sherlock.ui.proloog.ja" : "sherlock.ui.proloog.nee");
        int pw = mid - pl - 20;
        schaalTekst(g, proloog, pl + 8 + Math.min(pw, font.width(proloog)) / 2, y, pw, sleutel ? 0xFF8A6A2A : GRIJS);
        y += 14;
        for (int i = 0; i < zaken.size(); i++) {
            CompoundTag z = zaken.get(i);
            String status = z.getStringOr("Status", "WACHT");
            int ry = y + i * 17;
            boolean op = mx >= pl + 4 && mx < mid - 4 && my >= ry - 3 && my < ry + 13;
            if (i == gekozen) {
                g.fill(pl + 4, ry - 3, mid - 4, ry + 13, 0x40F7B6CB);
            } else if (op) {
                g.fill(pl + 4, ry - 3, mid - 4, ry + 13, 0x20F7B6CB);
            }
            int kleur = switch (status) {
                case "OPGELOST" -> GROEN;
                case "BEZIG" -> ROOD;
                case "KLAAR" -> GOUD;
                default -> GRIJS;
            };
            g.fill(pl + 8, ry + 1, pl + 15, ry + 8, kleur);
            Component naam = status.equals("WACHT") ? Component.translatable("sherlock.ui.geheim", z.getIntOr("Nr", 0))
                    : Component.translatable("sherlock.ui.hoofdstuk", z.getIntOr("Nr", 0), Component.translatable("sherlock.zaak." + z.getStringOr("Id", "") + ".titel"));
            g.text(font, naam, pl + 20, ry, status.equals("WACHT") ? GRIJS : INKT, false);
            if (z.getBooleanOr("EersteKeer", false)) {
                g.text(font, Component.literal("★"), mid - 16, ry, GOUD, false);
            }
            int index = i;
            klikken.add(new Klik(pl + 4, ry - 3, mid - 4, ry + 13, () -> gekozen = index, false));
        }
        // the right page: the chapter picked
        if (gekozen >= 0 && gekozen < zaken.size()) {
            CompoundTag z = zaken.get(gekozen);
            String id = z.getStringOr("Id", "");
            String status = z.getStringOr("Status", "WACHT");
            int rx = mid + 10, rw = pr - rx - 8, ry = pt + 8;
            boolean geheim = status.equals("WACHT");
            Component kop = geheim ? Component.translatable("sherlock.ui.geheim", z.getIntOr("Nr", 0)) : Component.translatable("sherlock.zaak." + id + ".titel");
            g.text(font, kop.copy().withStyle(ChatFormatting.BOLD), rx, ry, DONKER, false);
            ry += 16;
            g.text(font, Component.translatable("sherlock.ui.status." + status.toLowerCase(java.util.Locale.ROOT)), rx, ry, statusKleur(status), false);
            ry += 14;
            Component uitleg = geheim ? Component.translatable("sherlock.ui.wacht_op", Component.translatable("sherlock.zaak." + id + ".wacht"))
                    : Component.translatable("sherlock.zaak." + id + ".kort");
            for (FormattedCharSequence regel : font.split(uitleg, rw)) {
                g.text(font, regel, rx, ry, INKT, false);
                ry += 10;
            }
            if (z.getBooleanOr("EersteKeer", false)) {
                ry += 6;
                sticker(g, rx, ry);
            }
            Component knop = switch (status) {
                case "KLAAR" -> Component.translatable(z.getBooleanOr("Finale", false) ? "sherlock.ui.naar_rechtbank" : "sherlock.ui.begin");
                case "BEZIG" -> Component.translatable(z.getBooleanOr("Finale", false) ? "sherlock.ui.naar_rechtbank" : "sherlock.ui.naar_bord");
                case "OPGELOST" -> z.getBooleanOr("Finale", false) ? null : Component.translatable("sherlock.ui.terugkijken");
                default -> null;
            };
            if (knop != null) {
                int bx0 = rx, bx1 = pr - 14, by0 = pb - 30, by1 = pb - 12;
                Runnable wat = status.equals("OPGELOST") ? () -> stuur("bord", id, "") : () -> stuur("start", id, "");
                knop(g, knop, bx0, by0, bx1, by1, mx, my, true, status.equals("OPGELOST") ? 0xFF8A9AB8 : 0xFFE0607E, wat);
            }
        }
        knop(g, Component.translatable("sherlock.ui.sluiten"), pl + 8, pb - 24, pl + 78, pb - 8, mx, my, true, 0xFF9A7A6A, this::onClose);
    }

    private static int statusKleur(String status) {
        return switch (status) {
            case "OPGELOST" -> GROEN;
            case "BEZIG" -> ROOD;
            case "KLAAR" -> 0xFFB8881A;
            default -> GRIJS;
        };
    }

    private void sticker(GuiGraphicsExtractor g, int x, int y) {
        Component s = Component.translatable("sherlock.ui.sticker");
        int w = font.width(s) + 10;
        g.fill(x, y, x + w, y + 14, 0xFFFFE070);
        g.fill(x, y + 14, x + w, y + 15, 0xFFC8A030);
        g.text(font, s, x + 5, y + 3, 0xFF8A5A10, false);
    }

    private List<CompoundTag> zaken() {
        List<CompoundTag> l = new ArrayList<>();
        for (Tag t : staat.getListOrEmpty("Zaken")) {
            if (t instanceof CompoundTag c) {
                l.add(c);
            }
        }
        return l;
    }

    // --- the speurbord ---

    private void tekenBord(GuiGraphicsExtractor g, int mx, int my) {
        CompoundTag b = staat.getCompoundOrEmpty("Bord");
        String zaak = b.getStringOr("Zaak", "");
        boolean klaar = b.getBooleanOr("Opgelost", false), verzamel = b.getBooleanOr("Verzamel", false);
        // the frame and the cork with its little speckles
        g.fill(x0 - 3, y0 - 3, x0 + W + 3, y0 + H + 3, 0xFF6A4428);
        g.fill(x0, y0, x0 + W, y0 + H, KURK);
        long zaad = zaak.hashCode();
        for (int i = 0; i < 260; i++) {
            zaad = zaad * 6364136223846793005L + 1442695040888963407L;
            int sx = x0 + 2 + (int) ((zaad >>> 33) % (W - 4) + (W - 4)) % (W - 4);
            int sy = y0 + 2 + (int) ((zaad >>> 13) % (H - 4) + (H - 4)) % (H - 4);
            g.fill(sx, sy, sx + 1, sy + 1, KURK_DONKER);
        }
        // the title on a strip of paper
        Component titel = Component.translatable("sherlock.ui.bord", Component.translatable("sherlock.zaak." + zaak + ".titel"));
        int tw = font.width(titel) + 16;
        g.fill(x0 + 10, y0 + 7, x0 + 10 + tw, y0 + 23, PAPIER);
        g.text(font, titel, x0 + 18, y0 + 11, DONKER, false);
        List<CompoundTag> kaarten = lijst(b, "Kaarten");
        Component teller = Component.translatable("sherlock.ui.kaartjes", kaarten.size(), b.getIntOr("Totaal", 0));
        g.text(font, teller, x0 + W - 12 - font.width(teller), y0 + 11, 0xFF3A2416, false);

        Map<String, String> draadjes = new LinkedHashMap<>();
        CompoundTag d = b.getCompoundOrEmpty("Draadjes");
        for (String k : d.keySet()) {
            draadjes.put(k, d.getStringOr(k, ""));
        }
        String rood = b.getStringOr("Rood", "");

        // the suspects (or, chapter 10, Opa Knabbel's testament): polaroids on the right
        List<CompoundTag> verdachten = lijst(b, "Verdachten");
        Map<String, int[]> spelden = new LinkedHashMap<>();
        int px = x0 + 200, pw = 58, ph = 74, pgap = verdachten.size() <= 3 ? 26 : 10, py = y0 + 46;
        if (verzamel) {
            int tx = x0 + 250, ty = y0 + 60;
            g.fill(tx, ty, tx + 150, ty + 110, PAPIER);
            g.fill(tx, ty, tx + 150, ty + 3, 0xFFE0C890);
            g.centeredText(font, Component.translatable("sherlock.ui.testament").withStyle(ChatFormatting.BOLD), tx + 75, ty + 12, DONKER);
            int ly = ty + 28;
            for (FormattedCharSequence r : font.split(Component.translatable("sherlock.ui.testament.tekst"), 136)) {
                g.text(font, r, tx + 7, ly, INKT, false);
                ly += 10;
            }
            spelden.put("testament", new int[] {tx + 75, ty + 2});
            speld(g, tx + 75, ty + 2);
        }
        for (int i = 0; i < verdachten.size(); i++) {
            CompoundTag v = verdachten.get(i);
            String id = v.getStringOr("Id", "");
            int x = px + i * (pw + pgap), y = py + (i % 2) * 8;
            boolean dader = klaar && id.equals(b.getStringOr("Dader", ""));
            boolean op = mx >= x && mx < x + pw && my >= y && my < y + ph;
            g.fill(x + 2, y + 2, x + pw + 2, y + ph + 2, 0x50000000);
            g.fill(x, y, x + pw, y + ph, op && kaart != null && !klaar ? 0xFFFFF4C0 : 0xFFFDFDF8);
            g.fill(x + 4, y + 4, x + pw - 4, y + pw - 4, dader ? 0xFF6A9A5A : 0xFF3A3A4A);
            g.pose().pushMatrix();
            g.pose().translate(x + pw / 2f - 16, y + pw / 2f - 16 - 2);
            g.pose().scale(2f, 2f);
            g.item(item(v.getStringOr("Icoon", "")), 0, 0);
            g.pose().popMatrix();
            Component naam = Component.translatable("sherlock.wie." + id);
            schaalTekst(g, naam, x + pw / 2, y + pw - 1, pw - 4, INKT);
            if (dader) {
                g.centeredText(font, Component.translatable("sherlock.ui.dader").withStyle(ChatFormatting.BOLD), x + pw / 2, y + 6, 0xFFFFFFFF);
            }
            spelden.put(id, new int[] {x + pw / 2, y + 2});
            speld(g, x + pw / 2, y + 2);
            if (!klaar) {
                klikken.add(new Klik(x, y, x + pw, y + ph, () -> {
                    if (kaart != null) {
                        stuur("pin", kaart, id);
                        kaart = null;
                    }
                }, false));
                long aantal = draadjes.values().stream().filter(id::equals).count();
                knop(g, Component.translatable("sherlock.ui.wijs"), x, y + ph + 6, x + pw, y + ph + 22, mx, my, aantal >= 3, 0xFFD8323C,
                        () -> stuur("wijs", id, ""));
                g.centeredText(font, Component.literal(aantal + "/3"), x + pw / 2, y + ph + 26, aantal >= 3 ? 0xFFFFFFFF : 0xFF5A3A26);
            }
        }

        // the cards on the left
        Map<String, int[]> randen = new LinkedHashMap<>();
        int kx = x0 + 14, ky = y0 + 34, kw = 168, kh = 24;
        CompoundTag hover = null;
        for (int i = 0; i < kaarten.size(); i++) {
            CompoundTag k = kaarten.get(i);
            String id = k.getStringOr("Id", "");
            int y = ky + i * (kh + 6);
            boolean op = mx >= kx && mx < kx + kw && my >= y && my < y + kh;
            boolean isRood = id.equals(rood);
            g.fill(kx + 2, y + 2, kx + kw + 2, y + kh + 2, 0x50000000);
            int papier = isRood ? 0xFFFFC8C8 : k.getBooleanOr("Getuige", false) ? PAPIER_BLAUW : PAPIER;
            if (id.equals(kaart)) {
                g.fill(kx - 2, y - 2, kx + kw + 2, y + kh + 2, GOUD);
            } else if (isRood) {
                g.fill(kx - 1, y - 1, kx + kw + 1, y + kh + 1, ROOD);
            }
            g.fill(kx, y, kx + kw, y + kh, op ? lichter(papier) : papier);
            g.fill(kx + kw / 2 - 6, y - 2, kx + kw / 2 + 6, y + 3, 0xB0F4F0D0);   // a little tape
            g.item(item(k.getStringOr("Icoon", "")), kx + 4, y + 4);
            Component naam = Component.translatable("sherlock.zaak." + zaak + ".kaart." + id);
            List<FormattedCharSequence> regels = font.split(naam, kw - 28);
            int ty = regels.size() > 1 ? y + 3 : y + 8;
            for (int r = 0; r < Math.min(2, regels.size()); r++) {
                g.text(font, regels.get(r), kx + 24, ty + r * 9, isRood ? 0xFF9A1820 : INKT, false);
            }
            randen.put(id, new int[] {kx + kw, y + kh / 2});
            if (op) {
                hover = k;
            }
            if (!klaar && !verzamel) {
                klikken.add(new Klik(kx, y, kx + kw, y + kh, () -> kaart = id.equals(kaart) ? null : id, false));
                rechtsKlikken.add(new Klik(kx, y, kx + kw, y + kh, () -> stuur("los", id, ""), true));
            }
        }
        // the red threads (drawn over everything; from a card's right edge to the suspect's pin)
        for (Map.Entry<String, String> e : draadjes.entrySet()) {
            int[] van = randen.get(e.getKey());
            int[] naar = spelden.get(e.getValue());
            if (van != null && naar != null) {
                draad(g, van[0], van[1], naar[0], naar[1], e.getKey().equals(rood) ? 0xFF7A1018 : ROOD);
            }
        }
        if (verzamel && !klaar) {
            int[] naar = spelden.get("testament");
            for (int[] van : randen.values()) {
                draad(g, van[0], van[1], naar[0], naar[1], ROOD);
            }
        }
        for (int[] s : spelden.values()) {
            speld(g, s[0], s[1]);
        }
        // the line at the bottom: what to do (or who did it)
        Component onder = klaar ? (b.contains("Dader") ? Component.translatable("sherlock.ui.opgelost", Component.translatable("sherlock.wie." + b.getStringOr("Dader", "")))
                : Component.translatable("sherlock.ui.opgelost_rond"))
                : verzamel ? Component.translatable("sherlock.ui.uitleg_verzamel")
                : kaart != null ? Component.translatable("sherlock.ui.uitleg_kies", Component.translatable("sherlock.zaak." + zaak + ".kaart." + kaart))
                : !rood.isEmpty() ? Component.translatable("sherlock.ui.uitleg_rood")
                : Component.translatable("sherlock.ui.uitleg");
        int oy = y0 + H - 46;
        g.fill(x0 + 10, oy - 3, x0 + W - 10, oy + 19, 0xC0FFF8E8);
        int ly = oy;
        for (FormattedCharSequence r : font.split(onder, W - 30)) {
            g.text(font, r, x0 + 15, ly, INKT, false);
            ly += 9;
        }
        knop(g, Component.translatable("sherlock.ui.terug_boekje"), x0 + 10, y0 + H - 22, x0 + 130, y0 + H - 6, mx, my, true, 0xFF9A7A6A,
                () -> stuur("boekje", "", ""));
        knop(g, Component.translatable("sherlock.ui.sluiten"), x0 + W - 80, y0 + H - 22, x0 + W - 10, y0 + H - 6, mx, my, true, 0xFF9A7A6A, this::onClose);
        if (hover != null) {
            String id = hover.getStringOr("Id", "");
            List<Component> tip = List.of(Component.translatable("sherlock.zaak." + zaak + ".kaart." + id).withStyle(ChatFormatting.GOLD),
                    Component.translatable("sherlock.ui.vadson_zegt", Component.translatable("sherlock.zaak." + zaak + ".kaart." + id + ".tekst")));
            List<FormattedCharSequence> regels = new ArrayList<>();
            for (Component c : tip) {
                regels.addAll(font.split(c, 220));
            }
            g.setTooltipForNextFrame(font, regels, mx, my);
        }
    }

    private void speld(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 2, y - 2, x + 3, y + 3, 0xFF8A1018);
        g.fill(x - 1, y - 1, x + 2, y + 2, 0xFFE84A54);
        g.fill(x - 1, y - 1, x, y, 0xFFFFC0C0);
    }

    /** A red thread: a slightly sagging line of little squares. */
    private static void draad(GuiGraphicsExtractor g, int xa, int ya, int xb, int yb, int kleur) {
        double lengte = Math.hypot(xb - xa, yb - ya);
        int stappen = Math.max(1, (int) lengte);
        for (int i = 0; i <= stappen; i++) {
            double t = (double) i / stappen;
            double x = xa + (xb - xa) * t;
            double y = ya + (yb - ya) * t + Math.sin(Math.PI * t) * Math.min(14, lengte / 14);
            g.fill((int) x, (int) y, (int) x + 1, (int) y + 2, kleur);
        }
    }

    private void schaalTekst(GuiGraphicsExtractor g, Component tekst, int midden, int y, int breed, int kleur) {
        int w = font.width(tekst);
        float s = w > breed ? (float) breed / w : 1f;
        g.pose().pushMatrix();
        g.pose().translate(midden - w * s / 2f, y + (1 - s) * 4);
        g.pose().scale(s, s);
        g.text(font, tekst, 0, 0, kleur, false);
        g.pose().popMatrix();
    }

    private void knop(GuiGraphicsExtractor g, Component tekst, int bx0, int by0, int bx1, int by1, int mx, int my, boolean kan, int kleur, Runnable wat) {
        boolean op = mx >= bx0 && mx < bx1 && my >= by0 && my < by1;
        int basis = kan ? kleur : 0xFFB8ACA4;
        g.fill(bx0, by0 + 1, bx1, by1 + 1, donkerder(basis));
        g.fill(bx0, by0, bx1, by1, op && kan ? lichter(basis) : basis);
        g.fill(bx0 + 2, by0 + 1, bx1 - 2, by0 + 2, 0x50FFFFFF);
        g.centeredText(font, tekst, (bx0 + bx1) / 2, (by0 + by1 - 8) / 2 + 1, kan ? 0xFFFFFFFF : 0xFFF4EEF1);
        if (kan) {
            klikken.add(new Klik(bx0, by0, bx1, by1, wat, false));
        }
    }

    private static int lichter(int argb) {
        int r = Math.min(255, ((argb >> 16) & 255) + 22), gr = Math.min(255, ((argb >> 8) & 255) + 22), b = Math.min(255, (argb & 255) + 22);
        return (argb & 0xFF000000) | r << 16 | gr << 8 | b;
    }

    private static int donkerder(int argb) {
        int r = ((argb >> 16) & 255) * 3 / 4, gr = ((argb >> 8) & 255) * 3 / 4, b = (argb & 255) * 3 / 4;
        return (argb & 0xFF000000) | r << 16 | gr << 8 | b;
    }

    private static List<CompoundTag> lijst(CompoundTag t, String naam) {
        List<CompoundTag> l = new ArrayList<>();
        for (Tag x : t.getListOrEmpty(naam)) {
            if (x instanceof CompoundTag c) {
                l.add(c);
            }
        }
        return l;
    }

    private static ItemStack item(String id) {
        Identifier i = Identifier.tryParse(id);
        var it = i == null ? Items.PAPER : BuiltInRegistries.ITEM.getValue(i);
        return new ItemStack(it == Items.AIR ? Items.PAPER : it);
    }

    // =====================================================================================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean rechts = event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        List<Klik> lijst = rechts ? rechtsKlikken : klikken;
        for (int i = lijst.size() - 1; i >= 0; i--) {
            Klik k = lijst.get(i);
            if (k.op(event.x(), event.y())) {
                k.wat().run();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.5f));
                return true;
            }
        }
        if (!rechts && bord()) {
            kaart = null;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && bord() && kaart != null) {
            kaart = null;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
