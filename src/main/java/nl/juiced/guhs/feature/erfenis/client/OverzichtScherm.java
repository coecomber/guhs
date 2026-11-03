package nl.juiced.guhs.feature.erfenis.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.erfenis.Base;
import nl.juiced.guhs.feature.erfenis.ErfenisPayloads.Actie;
import nl.juiced.guhs.feature.erfenis.Gebouw;
import nl.juiced.guhs.feature.erfenis.GebouwType;
import nl.juiced.guhs.feature.erfenis.Plot;
import nl.juiced.guhs.feature.erfenis.Prijzen;
import nl.juiced.guhs.feature.erfenis.Productie;
import nl.juiced.guhs.feature.erfenis.Recept;
import nl.juiced.guhs.feature.erfenis.kaart.client.KaartClient;
import org.lwjgl.glfw.GLFW;

/**
 * De Erfenis: the overview (bovenaanzicht). A screen without background over the real world, seen through the
 * {@link Overzicht} camera; the mouse is free.
 * <ul>
 *   <li><b>Top left:</b> whose erfenis, Sluiten / Terug naar de wereld / Wereldkaart (/ Terug naar mijn erfenis); under it
 *   (own village) Opslag, Naar huis, Loket, Alles ophalen, Logés.</li>
 *   <li><b>Top right:</b> Guhlden (in the Hoeve, with the cap), storage, residents, Knusheid, Hoeve level, builders (with the
 *   jobs and their timers in the tooltip).</li>
 *   <li><b>World:</b> hover outlines (buildings, overgrown plukjes, kavels for sale), the selection, timers above jobs, a
 *   bubble above every building with something to collect (click = Ophalen). Drag / WASD pans, scroll zooms, Q/E turns.</li>
 *   <li><b>Right:</b> the side panel ({@link GebouwPaneel}): a building, a plukje or a kavel.</li>
 *   <li><b>Bottom:</b> the shop: one tab per family, every type with cost, count/max and lock; a choice gives a ghost.</li>
 *   <li><b>Windows</b> ({@link Vensters}): storage, Naar huis sturen, Loket, Bouwkeet choice, Guhpension.</li>
 * </ul>
 * Read-only (somebody else's base): no shop and no buttons, only looking. The server checks everything again.
 */
public final class OverzichtScherm extends Screen {
    private enum Modus { KIJKEN, PLAATSEN, VERPLAATSEN }

    private record Toast(Component tekst, boolean goed, long tot) {
    }

    private static final int SHOP_W = 62, SHOP_H = 48, TAB_H = 14;

    @Nullable
    private static OverzichtScherm huidig;
    /** The shop tab last open (kept between openings). */
    private static GebouwType.Familie tab = GebouwType.Familie.KERN;

    private Modus modus = Modus.KIJKEN;
    @Nullable
    private GebouwType plaatsType;
    private int plaatsRot;
    private int verplaatsId = -1;
    private int geselecteerd = -1, selPlukje = -1, selKavel = -1;
    // the mouse
    private boolean ingedrukt, gesleept, sleepKandidaat, sleepVerplaats;
    private double startX, startY, vorigX, vorigY;
    private int muisX, muisY;
    // what is under the mouse (updated every frame)
    private int hoverX = -1000, hoverZ = -1000;
    private boolean hoverOpPlot;
    @Nullable
    private Gebouw hoverGebouw;
    private final List<Ui.Klik> klikken = new ArrayList<>();
    private final List<Toast> toasts = new ArrayList<>();
    private final GebouwPaneel paneel = new GebouwPaneel();
    @Nullable
    private Vensters.Venster venster;
    private int paneelX0 = -1, paneelY1 = -1;
    private long laatsteSim;

    OverzichtScherm() {
        super(Component.translatable("erfenis.ui.titel"));
        huidig = this;
    }

    @Nullable
    static OverzichtScherm huidig() {
        return huidig;
    }

    static void sluitAlles() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof OverzichtScherm) {
            mc.setScreen(null);
        }
        Overzicht.stop();
        huidig = null;
    }

    boolean toetsenVrij() {
        return venster == null;
    }

    boolean toontRaster() {
        return modus != Modus.KIJKEN;
    }

    int muisX() {
        return muisX;
    }

    void open(Vensters.Venster v) {
        venster = v;
    }

    void sluitVenster() {
        venster = null;
    }

    @Nullable
    Vensters.Venster venster() {
        return venster;
    }

    void verplaatsStart(int id) {
        modus = Modus.VERPLAATSEN;
        verplaatsId = id;
    }

    /** Dev (/guhs erfenis ui, AutoCheck): select something, open a tab or a window, point the camera. */
    void devUi(String wat, String arg) {
        Base b = base();
        if (b == null) {
            return;
        }
        BlockPos o = ErfenisClient.oorsprong();
        switch (wat) {
            case "kies" -> {
                for (Gebouw g : b.gebouwen) {
                    if (g.type.id.equals(arg)) {
                        kies(g.id, -1, -1);
                        Overzicht.richt(o.getX() + g.x + g.b() / 2.0 + 8, o.getZ() + g.z + g.d() / 2.0, 46);
                        return;
                    }
                }
            }
            case "plukje" -> {
                int n = Integer.parseInt(arg);
                kies(-1, n, -1);
                int[] h = Plot.plukjeHoek(n);
                Overzicht.richt(o.getX() + h[0] + 4 + 8, o.getZ() + h[1] + 4, 46);
            }
            case "kavel" -> kies(-1, -1, Integer.parseInt(arg));
            case "tab" -> tab = GebouwType.Familie.valueOf(arg.toUpperCase(java.util.Locale.ROOT));
            case "verven" -> paneel.verven = true;
            case "dicht" -> venster = null;
            case "camera" -> {
                String[] d = arg.split(",");
                Overzicht.richt(o.getX() + Double.parseDouble(d[0]), o.getZ() + Double.parseDouble(d[1]), d.length > 2 ? Double.parseDouble(d[2]) : -1);
            }
            case "venster" -> {
                switch (arg) {
                    case "opslag" -> open(new Vensters.Opslag());
                    case "naarhuis" -> open(new Vensters.NaarHuis());
                    case "loket" -> open(new Vensters.Loket());
                    case "bouwkeet" -> {
                        for (Gebouw g : b.gebouwen) {
                            if (g.type == GebouwType.BOUWBLOKKENWERKPLAATS) {
                                open(new Vensters.Bouwkeet(g.id));
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
            default -> {
            }
        }
    }

    private static boolean eigen() {
        return ErfenisClient.eigen;
    }

    @Nullable
    private static Base base() {
        return ErfenisClient.base;
    }

    /** A new copy of the base arrived. */
    void vernieuwd() {
        Base b = base();
        if (b == null) {
            return;
        }
        if (geselecteerd >= 0 && b.gebouw(geselecteerd) == null) {
            geselecteerd = -1;
        }
        if (selPlukje >= 0 && b.opgeruimd[selPlukje] && b.bouwVan(Base.Bouw.PLUKJE, selPlukje) == null) {
            selPlukje = -1;
        }
        if (selKavel >= 0 && b.kavelGekocht(selKavel)) {
            selKavel = -1;
        }
        if (modus == Modus.VERPLAATSEN && b.gebouw(verplaatsId) == null) {
            modus = Modus.KIJKEN;
        }
        if (!eigen()) {
            modus = Modus.KIJKEN;
        }
    }

    void toast(Component tekst, boolean goed) {
        toasts.add(new Toast(tekst, goed, System.currentTimeMillis() + 3200));
        while (toasts.size() > 4) {
            toasts.remove(0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        Overzicht.stop();
        if (huidig == this) {
            huidig = null;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // (nothing: the world is the background)
    }

    private void kies(int gebouw, int plukje, int kavel) {
        geselecteerd = gebouw;
        selPlukje = plukje;
        selKavel = kavel;
        paneel.reset(gebouw >= 0 ? gebouw : plukje >= 0 ? 1000 + plukje : kavel >= 0 ? 2000 + kavel : -1);
    }

    // =====================================================================================================================
    // the world under the mouse
    // =====================================================================================================================

    private void kijkOnderMuis(double mx, double my) {
        Base b = base();
        hoverGebouw = null;
        hoverOpPlot = false;
        if (b == null) {
            return;
        }
        BlockPos o = ErfenisClient.oorsprong();
        Vec3[] s = Overzicht.straal(mx, my, width, height);
        Vec3 eind = s[0].add(s[1].scale(400));
        double beste = Double.MAX_VALUE;
        if (modus == Modus.KIJKEN) {
            for (Gebouw g : b.gebouwen) {
                double y0 = Overzicht.grondY() - (g.level == 0 ? 0.2 : 0);
                AABB doos = new AABB(o.getX() + g.x, y0, o.getZ() + g.z, o.getX() + g.x + g.b(), Overzicht.grondY() + Math.max(1, g.type.hoogte(g.level)),
                        o.getZ() + g.z + g.d());
                Optional<Vec3> raak = doos.clip(s[0], eind);
                if (raak.isPresent() && raak.get().distanceToSqr(s[0]) < beste) {
                    beste = raak.get().distanceToSqr(s[0]);
                    hoverGebouw = g;
                }
            }
        }
        Vec3 grond = Overzicht.grond(mx, my, width, height);
        if (grond != null) {
            hoverX = (int) Math.floor(grond.x - o.getX());
            hoverZ = (int) Math.floor(grond.z - o.getZ());
            hoverOpPlot = Plot.opPlot(hoverX, hoverZ);
            if (hoverGebouw == null && hoverOpPlot && modus == Modus.KIJKEN) {
                hoverGebouw = b.op(hoverX, hoverZ);
            }
        } else {
            hoverX = hoverZ = -1000;
        }
    }

    /** The plukje under the mouse that is still overgrown (or being cleared), or -1. */
    private int hoverPlukje(Base b) {
        if (!hoverOpPlot || hoverGebouw != null) {
            return -1;
        }
        int p = Plot.plukjeOp(hoverX, hoverZ);
        return p >= 0 && (!b.opgeruimd[p] || b.bouwVan(Base.Bouw.PLUKJE, p) != null) ? p : -1;
    }

    private int hoverKavel(Base b) {
        if (!hoverOpPlot || hoverGebouw != null) {
            return -1;
        }
        int k = Plot.kavelOp(hoverX, hoverZ);
        return k >= 0 && !b.kavelGekocht(k) ? k : -1;
    }

    private int voetX(int w) {
        return hoverX - (w - 1) / 2;
    }

    private int voetZ(int h) {
        return hoverZ - (h - 1) / 2;
    }

    /** What the world drawing shows this frame: ghosts, hover and selection outlines. */
    List<OverzichtWereld.Doos> dozen() {
        List<OverzichtWereld.Doos> l = new ArrayList<>();
        Base b = base();
        if (b == null) {
            return l;
        }
        BlockPos o = ErfenisClient.oorsprong();
        double gy = Overzicht.grondY();
        Gebouw sel = geselecteerd >= 0 ? b.gebouw(geselecteerd) : null;
        if (sel != null && modus == Modus.KIJKEN) {
            l.add(new OverzichtWereld.Doos(o.getX() + sel.x, gy, o.getZ() + sel.z, o.getX() + sel.x + sel.b(), gy + sel.type.hoogte(sel.level) + 0.05,
                    o.getZ() + sel.z + sel.d(), 0x30F7B6CB, 0xF0F07CB4, 0.12f));
        }
        if (selPlukje >= 0) {
            int[] h = Plot.plukjeHoek(selPlukje);
            l.add(vlak(o, h[0], h[1], Plot.PLUK, Plot.PLUK, 0x30F7B6CB, 0xF0F07CB4));
        }
        if (selKavel >= 0) {
            int[] v = Plot.kavelVak(selKavel);
            l.add(vlak(o, v[0], v[1], v[2], v[3], 0x30F7B6CB, 0xF0F07CB4));
        }
        if (modus == Modus.KIJKEN && hoverGebouw != null && hoverGebouw != sel) {
            Gebouw g = hoverGebouw;
            l.add(new OverzichtWereld.Doos(o.getX() + g.x, gy, o.getZ() + g.z, o.getX() + g.x + g.b(), gy + g.type.hoogte(g.level) + 0.05,
                    o.getZ() + g.z + g.d(), 0, 0xC0FFFFFF, 0.08f));
        } else if (modus == Modus.KIJKEN && hoverOpPlot && hoverGebouw == null) {
            int p = hoverPlukje(b), k = hoverKavel(b);
            if (p >= 0 && p != selPlukje) {
                int[] h = Plot.plukjeHoek(p);
                l.add(vlak(o, h[0], h[1], Plot.PLUK, Plot.PLUK, 0x20FFFFFF, 0xB0FFFFFF));
            } else if (k >= 0 && k != selKavel) {
                int[] v = Plot.kavelVak(k);
                l.add(vlak(o, v[0], v[1], v[2], v[3], 0x20FFFFFF, 0xB0FFE08A));
            } else if (p < 0 && k < 0) {
                l.add(new OverzichtWereld.Doos(o.getX() + hoverX, gy, o.getZ() + hoverZ, o.getX() + hoverX + 1, gy + 0.04, o.getZ() + hoverZ + 1,
                        0x40FFFFFF, 0, 0));
            }
        }
        if (modus == Modus.PLAATSEN && plaatsType != null && hoverX > -100) {
            int w = plaatsType.b(plaatsRot), h = plaatsType.d(plaatsRot);
            int x = voetX(w), z = voetZ(h);
            l.add(spook(o, x, z, w, h, plaatsType.hoogte(1), b.kanPlaatsen(plaatsType, x, z, plaatsRot) == null));
        }
        if (modus == Modus.VERPLAATSEN) {
            Gebouw g = b.gebouw(verplaatsId);
            if (g != null) {
                l.add(vlak(o, g.x, g.z, g.b(), g.d(), 0x30FFFFFF, 0x80FFFFFF));
                int x = voetX(g.b()), z = voetZ(g.d());
                l.add(spook(o, x, z, g.b(), g.d(), g.type.hoogte(Math.max(1, g.level)), b.kanVerplaatsen(g.id, x, z) == null));
            }
        }
        return l;
    }

    private static OverzichtWereld.Doos vlak(BlockPos o, int x, int z, int w, int h, int vul, int rand) {
        double gy = Overzicht.grondY();
        return new OverzichtWereld.Doos(o.getX() + x, gy, o.getZ() + z, o.getX() + x + w, gy + 0.06, o.getZ() + z + h, vul, rand, 0.1f);
    }

    private static OverzichtWereld.Doos spook(BlockPos o, int x, int z, int w, int d, int h, boolean mag) {
        double gy = Overzicht.grondY();
        return new OverzichtWereld.Doos(o.getX() + x + 0.02, gy + 0.01, o.getZ() + z + 0.02, o.getX() + x + w - 0.02, gy + h, o.getZ() + z + d - 0.02,
                mag ? 0x5066D98A : 0x60E8505E, mag ? 0xE046C070 : 0xE0E0404E, 0.1f);
    }

    // =====================================================================================================================
    // drawing
    // =====================================================================================================================

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        klikken.clear();
        muisX = mx;
        muisY = my;
        Base b = base();
        if (b == null) {
            return;
        }
        long nu = ErfenisClient.nu();
        if (System.currentTimeMillis() - laatsteSim > 500 && !b.npc) {   // (the same catch-up as the server: live numbers)
            laatsteSim = System.currentTimeMillis();
            b.bijwerken(nu);
            b.momenten.clear();
            b.klaar.clear();
            b.meldingen.clear();
        }
        Ui ui = new Ui(g, font, mx, venster != null ? -1000 : my, klikken);
        if (venster == null) {
            kijkOnderMuis(mx, my);
        }
        zwevers(ui, b, nu);
        bovenbalk(ui, b, nu);
        paneelX0 = -1;
        int onder = height - (eigen() ? SHOP_H + TAB_H + 34 : 24);
        int px = width - GebouwPaneel.B - 6, py = 34;
        if (modus == Modus.KIJKEN) {
            Gebouw sel = geselecteerd >= 0 ? b.gebouw(geselecteerd) : null;
            if (sel != null) {
                paneelY1 = paneel.gebouw(ui, this, b, sel, nu, px, py, onder - py, eigen());
                paneelX0 = px;
            } else if (selPlukje >= 0) {
                paneelY1 = paneel.plukje(ui, b, selPlukje, nu, px, py, eigen());
                paneelX0 = px;
            } else if (selKavel >= 0) {
                paneelY1 = paneel.kavel(ui, b, selKavel, px, py, eigen());
                paneelX0 = px;
            }
        }
        if (eigen()) {
            winkel(ui, b);
        }
        hulp(ui);
        toasts(ui);
        if (venster != null) {
            Ui vui = new Ui(g, font, mx, my, klikken);
            venster.teken(vui, this, b, width, height, nu);
        }
        // tooltips last
        for (int i = klikken.size() - 1; i >= 0; i--) {
            Ui.Klik k = klikken.get(i);
            if (k.op(mx, my)) {
                if (k.tip() != null && !k.tip().isEmpty()) {
                    g.setComponentTooltipForNextFrame(font, k.tip(), mx, my);
                }
                return;
            }
        }
        if (venster == null && modus == Modus.KIJKEN && hoverGebouw != null && hoverGebouw.id != geselecteerd) {
            Gebouw h = hoverGebouw;
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(h.type.naamKey()).withStyle(ChatFormatting.BOLD));
            tip.add(h.level == 0 ? Component.translatable("erfenis.ui.in_aanbouw") : Component.translatable("erfenis.ui.level", h.level));
            if (h.type.maakt() && h.level > 0) {
                tip.add(Component.translatable("erfenis.ui.maakt_kort", Ui.getal(Math.round(Productie.nuPerUur(b, h))),
                        Component.translatable(Productie.recept(h).uit().naamKey())).withColor(Teken.ZACHT));
            }
            g.setComponentTooltipForNextFrame(font, tip, mx, my);
        } else if (venster == null && modus == Modus.KIJKEN && hoverPlukje(b) >= 0) {
            int p = hoverPlukje(b);
            g.setComponentTooltipForNextFrame(font, List.of(Component.translatable("erfenis.plukje.soort." + b.plukjeSoort(p)).withStyle(ChatFormatting.BOLD),
                    Component.translatable("erfenis.ui.ring", Plot.ring(p)), Component.translatable("erfenis.ui.klik_opruimen").withColor(Teken.ZACHT)), mx, my);
        } else if (venster == null && modus == Modus.KIJKEN && hoverKavel(b) >= 0) {
            g.setComponentTooltipForNextFrame(font, List.of(Component.translatable("erfenis.ui.kavel").withStyle(ChatFormatting.BOLD),
                    Component.translatable("erfenis.ui.klik_kavel").withColor(Teken.ZACHT)), mx, my);
        }
    }

    /** Above the world: job timers (buildings and plukjes) and the collect bubbles. */
    private void zwevers(Ui ui, Base b, long nu) {
        BlockPos o = ErfenisClient.oorsprong();
        for (Base.Bouw bouw : b.bouwen) {
            double cx, cz, top;
            if (bouw.soort() == Base.Bouw.PLUKJE) {
                int[] h = Plot.plukjeHoek(bouw.ref());
                cx = o.getX() + h[0] + 4;
                cz = o.getZ() + h[1] + 4;
                top = Overzicht.grondY() + 3;
            } else {
                Gebouw g = b.gebouw(bouw.ref());
                if (g == null) {
                    continue;
                }
                cx = o.getX() + g.x + g.b() / 2.0;
                cz = o.getZ() + g.z + g.d() / 2.0;
                top = Overzicht.grondY() + g.type.hoogte(g.level) + 1.8;
            }
            double[] p = Overzicht.projecteer(new Vec3(cx, top, cz), width, height);
            if (p != null && p[1] > 66) {
                String t = Teken.tijd(bouw.eind() - nu);
                int w = Math.max(44, font.width(t) + 24), x = (int) p[0] - w / 2, y = (int) p[1] - 9;
                Teken.chip(ui.g, x, y, x + w, y + 17);
                ui.item(Ui.BOUWER, x + 1, y);
                ui.tekst(t, x + 18, y + 3, Teken.DONKER);
                Teken.balk(ui.g, x + 18, y + 12, x + w - 3, y + 15, bouw.voortgang(nu), Teken.GOUD);
            }
        }
        for (Gebouw geb : b.gebouwen) {
            if (!geb.type.maakt() || geb.level <= 0 || geb.klaar() < 1) {
                continue;
            }
            Recept r = Productie.recept(geb);
            double cx = o.getX() + geb.x + geb.b() / 2.0, cz = o.getZ() + geb.z + geb.d() / 2.0;
            double top = Overzicht.grondY() + geb.type.hoogte(geb.level) + 0.8;
            double[] p = Overzicht.projecteer(new Vec3(cx, top, cz), width, height);
            if (p == null || p[1] <= 66) {
                continue;
            }
            boolean vol = geb.vol();
            String t = Ui.getal((long) Math.floor(geb.inBuffer(r.uit())));
            int w = font.width(t) + 24, x = (int) p[0] - w / 2, y = (int) p[1] - 10;
            boolean op = ui.op(x, y, x + w, y + 20);
            Teken.rond(ui.g, x, y + 1, x + w, y + 19, 0x50501830, 4);
            Teken.rond(ui.g, x, y, x + w, y + 18, vol ? Teken.GOUD : Teken.ROZE_DONKER, 4);
            Teken.rond(ui.g, x + 1, y + 1, x + w - 1, y + 17, op && eigen() ? Teken.WIT : Teken.CREME_VOL, 3);
            ui.g.fill(x + w / 2 - 2, y + 18, x + w / 2 + 2, y + 20, vol ? Teken.GOUD : Teken.ROZE_DONKER);
            ui.item(Ui.icoon(r.uit()), x + 2, y + 1);
            ui.tekst(t, x + 19, y + 5, Teken.DONKER);
            int id = geb.id;
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("erfenis.ui.ophalen").withStyle(ChatFormatting.BOLD));
            tip.add(Component.translatable("erfenis.ui.mandje", Ui.getal(geb.klaar()), Ui.getal(Math.round(Productie.bufferCap(geb))),
                    Productie.bufferUren(geb.level)));
            if (vol) {
                tip.add(Component.translatable("erfenis.ui.mandje_vol").withColor(Teken.ROOD));
            }
            ui.klik(x, y, x + w, y + 20, eigen() ? () -> ErfenisClient.stuur(Actie.op(Actie.OPHALEN, id)) : null, tip);
        }
    }

    private void bovenbalk(Ui ui, Base b, long nu) {
        Component titel = b.npc ? Component.translatable("erfenis.ui.dorp", b.naam) : Component.translatable("erfenis.ui.van", b.naam);
        Component sub = eigen() ? (ErfenisClient.snel ? Component.translatable("erfenis.ui.snel") : null) : Component.translatable("erfenis.ui.kijken");
        int w = Math.max(ui.font.width(titel) + 16, sub == null ? 0 : ui.font.width(sub) + 16);
        int h = sub == null ? 20 : 30;
        Teken.paneel(ui.g, 6, 6, 6 + w, 6 + h);
        ui.tekst(titel.copy().withStyle(ChatFormatting.BOLD), 14, 12, Teken.DONKER);
        if (sub != null) {
            ui.tekst(sub, 14, 23, eigen() ? Teken.ROOD : Teken.ZACHT);
        }
        ui.vlak(6, 6, 6 + w, 6 + h, null);
        int by = 6 + h + 4;
        Component sluit = Component.translatable("erfenis.ui.sluiten"), terug = Component.translatable("erfenis.ui.terug");
        int w1 = ui.font.width(sluit) + 12, w2 = ui.font.width(terug) + 12;
        ui.knop(sluit, 6, by, 6 + w1, by + 14, true, Teken.ROZE_DONKER, this::onClose, null);
        ui.knop(terug, 10 + w1, by, 10 + w1 + w2, by + 14, true, 0xFFB98AA6, () -> {
            sluitAlles();
            ErfenisClient.stuur(Actie.simpel(Actie.TERUG));
        }, null);
        Component kaartKnop = Component.translatable("erfenis.ui.wereldkaart");
        int kx = 14 + w1 + w2, w3 = ui.font.width(kaartKnop) + 12;
        ui.knop(kaartKnop, kx, by, kx + w3, by + 14, true, 0xFF7FA8D8, () -> {
            sluitAlles();
            KaartClient.open(() -> ErfenisClient.stuur(Actie.simpel(Actie.OPEN)));
        }, null);
        if (!eigen()) {
            Component naarEigen = Component.translatable("erfenis.ui.naar_eigen");
            int ex = kx + w3 + 4, w4 = ui.font.width(naarEigen) + 12;
            ui.knop(naarEigen, ex, by, ex + w4, by + 14, true, Teken.GROEN_DONKER, () -> {
                sluitAlles();
                ErfenisClient.stuur(Actie.op(Actie.GA, Actie.GA_EN_OPEN));
            }, null);
        } else {
            linkerKnoppen(ui, b, by + 20);
        }
        // right: the chips
        int x = width - 6;
        List<Component> bouwerTip = new ArrayList<>();
        bouwerTip.add(Component.translatable("erfenis.ui.bouwers", b.bouwen.size(), b.bouwers()).withStyle(ChatFormatting.BOLD));
        long eerste = Long.MAX_VALUE;
        Base.Bouw eersteBouw = null;
        for (Base.Bouw bouw : b.bouwen) {
            Component wat;
            if (bouw.soort() == Base.Bouw.PLUKJE) {
                wat = Component.translatable("erfenis.ui.job_plukje");
            } else {
                Gebouw bg = b.gebouw(bouw.ref());
                wat = bg == null ? Component.literal("?") : Component.translatable(bg.type.naamKey()).append(" → ")
                        .append(Component.translatable("erfenis.ui.level", bouw.naar()));
            }
            bouwerTip.add(Component.literal("• ").append(wat).append(" · " + Teken.tijd(bouw.eind() - nu)));
            if (bouw.eind() < eerste) {
                eerste = bouw.eind();
                eersteBouw = bouw;
            }
        }
        if (b.vrijeBouwers() > 0) {
            bouwerTip.add(Component.translatable("erfenis.ui.bouwers_vrij", b.vrijeBouwers()).withColor(Teken.GROEN_DONKER));
        }
        Component bouwer = b.bouwen.isEmpty() ? Component.translatable("erfenis.ui.bouwer_vrij")
                : Component.literal(b.bouwen.size() + "/" + b.bouwers() + " · " + Teken.tijd(eerste - nu));
        x = chip(ui, x, Ui.BOUWER, bouwer, b.vrijeBouwers() > 0 ? Teken.GROEN_DONKER : Teken.DONKER,
                eersteBouw != null ? eersteBouw.voortgang(nu) : -1, Teken.GOUD, bouwerTip, null);
        Gebouw hoeve = b.hoeve();
        x = chip(ui, x, GebouwType.HOEVE.icoon(), Component.translatable("erfenis.ui.level", b.hoeveLevel()), Teken.DONKER, -1, 0,
                List.of(Component.translatable("erfenis.gebouw.hoeve").withStyle(ChatFormatting.BOLD), Component.translatable("erfenis.ui.klik_hoeve")
                        .withColor(Teken.ZACHT)), hoeve == null ? null : () -> kies(hoeve.id, -1, -1));
        x = chip(ui, x, Ui.KNUS, Component.literal(Ui.getal(b.knusheid())), Teken.DONKER, -1, 0,
                List.of(Component.translatable("erfenis.ui.knusheid").withStyle(ChatFormatting.BOLD), Component.translatable("erfenis.ui.knus_uitleg")
                        .withColor(Teken.ZACHT)), null);
        List<Component> bewTip = new ArrayList<>();
        bewTip.add(Component.translatable("erfenis.ui.bewoners").withStyle(ChatFormatting.BOLD));
        bewTip.add(Component.translatable("erfenis.ui.werkplekken", b.werkplekken()));
        if (b.volgendeIntrek > 0) {
            bewTip.add(Component.translatable("erfenis.ui.intrek_over", Teken.tijd(b.volgendeIntrek - nu)).withColor(Teken.ZACHT));
        } else if (b.bewoners >= b.maxBewoners()) {
            bewTip.add(Component.translatable("erfenis.ui.bouw_hutjes").withColor(Teken.ZACHT));
        }
        x = chip(ui, x, Ui.BEWONER, Component.literal(b.bewoners + "/" + b.maxBewoners()), Teken.DONKER, -1, 0, bewTip, null);
        int cap = b.opslag(), vol = b.opslagGebruikt();
        x = chip(ui, x, Ui.OPSLAG, Component.literal(Ui.getal(vol) + "/" + Ui.getal(cap)), vol >= cap ? Teken.ROOD : Teken.DONKER,
                cap <= 0 ? 0 : vol / (float) cap, Teken.ROZE_DONKER, List.of(Component.translatable("erfenis.ui.opslag").withStyle(ChatFormatting.BOLD),
                        Component.translatable("erfenis.ui.klik_opslag").withColor(Teken.ZACHT)), () -> open(new Vensters.Opslag()));
        int gcap = b.guhldenCap();
        chip(ui, x, Ui.GUHLDEN, Component.literal("Ƒ " + Ui.getal(b.guhlden) + "/" + Ui.getal(gcap)), b.guhlden >= gcap ? Teken.ROOD : Teken.DONKER,
                gcap <= 0 ? 0 : b.guhlden / (float) gcap, Teken.GOUD, List.of(Component.translatable("erfenis.ui.guhlden").withStyle(ChatFormatting.BOLD),
                        Component.translatable("erfenis.ui.guhlden_uitleg", Ui.getal(gcap)).withColor(Teken.ZACHT)), null);
    }

    /** The own village's tool buttons under the top-left buttons. */
    private void linkerKnoppen(Ui ui, Base b, int y) {
        int x0 = 6, x1 = 86;
        ui.knop(Component.translatable("erfenis.ui.opslag"), x0, y, x1, y + 14, true, 0xFFC8A060, () -> open(new Vensters.Opslag()), null);
        y += 17;
        ui.knop(Component.translatable("erfenis.ui.naar_huis"), x0, y, x1, y + 14, true, 0xFF7FA8D8, () -> open(new Vensters.NaarHuis()),
                List.of(Component.translatable("erfenis.ui.naar_huis_tip")));
        y += 17;
        int klaar = 0;
        for (Gebouw g : b.gebouwen) {
            klaar += g.type.maakt() ? g.klaar() : 0;
        }
        ui.knop(Component.translatable("erfenis.ui.alles_ophalen"), x0, y, x1, y + 14, klaar > 0, Teken.GOUD,
                () -> ErfenisClient.stuur(Actie.simpel(Actie.OPHALEN_ALLES)), List.of(Component.translatable("erfenis.ui.alles_ophalen_tip", klaar)));
        y += 17;
        if (b.besteLevel(GebouwType.LOKET) > 0) {
            ui.knop(Component.translatable("erfenis.ui.loket"), x0, y, x1, y + 14, true, 0xFFC8A060, () -> open(new Vensters.Loket()), null);
            y += 17;
        }
        if (b.besteLevel(GebouwType.GUHPENSION) > 0) {
            ui.knop(Component.translatable("erfenis.ui.loges"), x0, y, x1, y + 14, true, 0xFFE07FA8,
                    () -> ErfenisClient.stuur(new Actie(Actie.PENSION, 0, Actie.PENSION_OPEN, 0, 0, 0, 0, "")), null);
        }
    }

    /** One chip of the top bar, right-aligned at x; returns the x left of it. A bar under the text when deel >= 0. */
    private int chip(Ui ui, int rechts, net.minecraft.world.item.ItemStack icoon, Component tekst, int kleur, float deel, int balkKleur,
                     List<Component> tip, @Nullable Runnable klik) {
        int w = font.width(tekst) + 26, x = rechts - w, y = 6;
        Teken.chip(ui.g, x, y, rechts, y + 20);
        if (klik != null && ui.op(x, y, rechts, y + 20)) {
            Teken.rond(ui.g, x + 1, y + 1, rechts - 1, y + 19, 0xFFFFFFFF, 3);
        }
        ui.item(icoon, x + 3, y + 2);
        ui.tekst(tekst, x + 21, y + (deel >= 0 ? 4 : 6), kleur);
        if (deel >= 0) {
            Teken.balk(ui.g, x + 21, y + 14, rechts - 4, y + 17, deel, balkKleur);
        }
        ui.klik(x, y, rechts, y + 20, klik, tip);
        return x - 4;
    }

    /** The shop at the bottom: family tabs and the cards of the open family. */
    private void winkel(Ui ui, Base b) {
        int h = b.hoeveLevel();
        List<GebouwType> types = new ArrayList<>();
        for (GebouwType t : GebouwType.values()) {
            if (t.familie == tab && t != GebouwType.HOEVE) {
                types.add(t);
            }
        }
        int gap = 4, n = Math.max(types.size(), 5);
        int totaal = Math.max(n * SHOP_W + (n - 1) * gap + 12, GebouwType.Familie.values().length * 66 + 12);
        int x0 = (width - totaal) / 2, y0 = height - SHOP_H - 12;
        // tabs
        int tx = x0 + 6;
        for (GebouwType.Familie f : GebouwType.Familie.values()) {
            Component naam = Component.translatable(f.naamKey());
            int tw = 62;
            boolean open = f == tab;
            int ty = y0 - TAB_H + (open ? 0 : 2);
            Teken.rond(ui.g, tx, ty, tx + tw, y0 + 4, open ? Teken.ROZE_DONKER : Teken.ROZE, 3);
            Teken.rond(ui.g, tx + 1, ty + 1, tx + tw - 1, y0 + 4, open ? Teken.CREME_VOL : ui.op(tx, ty, tx + tw, y0) ? 0xFFFFF6FA : 0xFFF3E4EB, 3);
            boolean nieuw = false;
            for (GebouwType t : GebouwType.values()) {
                nieuw |= t.familie == f && t != GebouwType.HOEVE && b.aantal(t) < t.maxAantal(h);
            }
            ui.passend(naam, tx + 5, ty + 3, tw - (nieuw ? 14 : 8), open ? Teken.DONKER : Teken.ZACHT);
            if (nieuw) {
                ui.g.fill(tx + tw - 8, ty + 4, tx + tw - 4, ty + 8, Teken.GROEN);
            }
            ui.klik(tx, ty, tx + tw, y0, () -> tab = f, nieuw ? List.of(Component.translatable("erfenis.ui.tab_nieuw")) : null);
            tx += tw + 2;
        }
        Teken.paneel(ui.g, x0, y0, x0 + totaal, y0 + SHOP_H + 8);
        ui.vlak(x0, y0, x0 + totaal, y0 + SHOP_H + 8, null);
        int x = x0 + 6;
        for (GebouwType t : types) {
            Base.Fout f = b.kanKopen(t);
            boolean slot = h < t.opent;
            boolean gekozen = modus == Modus.PLAATSEN && plaatsType == t;
            boolean op = ui.op(x, y0 + 4, x + SHOP_W, y0 + 4 + SHOP_H);
            int y = y0 + 4;
            Teken.rond(ui.g, x, y, x + SHOP_W, y + SHOP_H, gekozen ? Teken.GOUD : op ? Teken.ROZE_DONKER : Teken.ROZE, 3);
            Teken.rond(ui.g, x + 1, y + 1, x + SHOP_W - 1, y + SHOP_H - 1, f == null ? (op ? 0xFFFFFFFF : 0xFFFFF0F5) : 0xFFEDE3E8, 3);
            ui.item(t.icoon(), x + 4, y + 3);
            if (slot) {
                ui.g.fill(x + 4, y + 3, x + 20, y + 19, 0xA0EDE3E8);
                ui.item(Ui.SLOT, x + 8, y + 7, 0.5f);
            }
            String telling = b.aantal(t) + "/" + t.maxAantal(h);
            ui.tekst(telling, x + SHOP_W - 4 - font.width(telling), y + 7, Teken.ZACHT);
            ui.passend(Component.translatable(t.naamKey()), x + 3, y + 22, SHOP_W - 6, f == null ? Teken.DONKER : Teken.ZACHT);
            String regel;
            int kleur;
            Prijzen.Prijs p = b.prijsNieuw(t);
            if (slot) {
                regel = "H" + t.opent;
                kleur = Teken.ROOD;
            } else if (f != null && ("erfenis.fout.slot".equals(f.key()) || "erfenis.fout.limiet".equals(f.key()))) {
                regel = "erfenis.fout.slot".equals(f.key()) ? "H" + f.args()[0] : "max";
                kleur = Teken.ZACHT;
            } else {
                regel = p.guhlden() == 0 ? "gratis!" : "Ƒ" + Ui.getal(p.guhlden());
                kleur = b.guhlden >= p.guhlden() ? Teken.DONKER : Teken.ROOD;
            }
            ui.tekst(regel, x + (SHOP_W - font.width(regel)) / 2, y + 34, kleur);
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable(t.naamKey()).withStyle(ChatFormatting.BOLD));
            for (FormattedCharSequence r : font.split(Component.translatable(t.uitlegKey()), 190)) {
                tip.add(Component.literal(stuk(r)).withColor(Teken.ZACHT));
            }
            tip.add(Component.translatable("erfenis.ui.maat", t.breedte, t.diepte));
            if (slot) {
                tip.add(Component.translatable("erfenis.fout.slot", t.opent).withColor(Teken.ROOD));
            } else {
                tip.add(prijsTekst(p));
                tip.add(Component.translatable("erfenis.ui.tijd", Ui.duur(ErfenisClient.snel ? 0 : p.tijdMs())));
                if (f != null) {
                    tip.add(ErfenisClient.tekst(f.key(), List.of(f.args())).copy().withColor(Teken.ROOD));
                }
            }
            final GebouwType type = t;
            ui.klik(x, y, x + SHOP_W, y + SHOP_H, () -> kiesType(type), tip);
            x += SHOP_W + gap;
        }
    }

    /** "Ƒ 15 · 10 Planken" for a tooltip. */
    static Component prijsTekst(Prijzen.Prijs p) {
        net.minecraft.network.chat.MutableComponent t = Component.translatable("erfenis.ui.kost").append(" ");
        boolean iets = false;
        if (p.guhlden() > 0) {
            t.append("Ƒ" + Ui.getal(p.guhlden()));
            iets = true;
        }
        for (var e : p.goederen().entrySet()) {
            t.append(iets ? ", " : "").append(e.getValue() + " ").append(Component.translatable(e.getKey().naamKey()));
            iets = true;
        }
        if (!p.hoofdwereld().isEmpty()) {
            t.append(iets ? " + " : "").append(Component.translatable("erfenis.ui.hoofdwereld_spul"));
            iets = true;
        }
        return iets ? t : Component.translatable("erfenis.ui.gratis");
    }

    private static String stuk(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((i, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }

    private void kiesType(GebouwType t) {
        Base b = base();
        if (b == null) {
            return;
        }
        if (modus == Modus.PLAATSEN && plaatsType == t) {
            modus = Modus.KIJKEN;
            return;
        }
        Base.Fout f = b.kanKopen(t);
        if (f != null) {
            toast(ErfenisClient.tekst(f.key(), List.of(f.args())), false);
            return;
        }
        modus = Modus.PLAATSEN;
        plaatsType = t;
        plaatsRot = 0;
        kies(-1, -1, -1);
    }

    private void hulp(Ui ui) {
        String key = switch (modus) {
            case PLAATSEN -> "erfenis.ui.hulp_plaats";
            case VERPLAATSEN -> "erfenis.ui.hulp_verplaats";
            default -> "erfenis.ui.hulp";
        };
        Component t = Component.translatable(key);
        int w = font.width(t) + 12;
        int y = eigen() ? height - SHOP_H - TAB_H - 30 : height - 18;
        int x = (width - w) / 2;
        Teken.rond(ui.g, x, y, x + w, y + 13, modus == Modus.KIJKEN ? 0x90402030 : 0xC0D27A9C, 3);
        ui.tekst(t, x + 6, y + 3, Teken.WIT);
    }

    private void toasts(Ui ui) {
        long nu = System.currentTimeMillis();
        toasts.removeIf(t -> t.tot() < nu);
        int y = 34;
        for (Toast t : toasts) {
            int w = Math.min(width - 200, font.width(t.tekst()) + 16), x = (width - w) / 2;
            Teken.rond(ui.g, x, y + 1, x + w, y + 17, 0x50501830, 3);
            Teken.rond(ui.g, x, y, x + w, y + 16, t.goed() ? Teken.GROEN_DONKER : Teken.ROOD, 3);
            Teken.rond(ui.g, x + 1, y + 1, x + w - 1, y + 15, Teken.CREME_VOL, 3);
            ui.passend(t.tekst(), x + 8, y + 4, w - 16, t.goed() ? Teken.GROEN_DONKER : Teken.ROOD);
            y += 19;
        }
    }

    // =====================================================================================================================
    // input
    // =====================================================================================================================

    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        if (k == GLFW.GLFW_KEY_ESCAPE) {
            if (venster != null) {
                venster = null;
                return true;
            }
            if (modus != Modus.KIJKEN) {
                modus = Modus.KIJKEN;
                return true;
            }
            if (geselecteerd >= 0 || selPlukje >= 0 || selKavel >= 0) {
                kies(-1, -1, -1);
                return true;
            }
            onClose();
            return true;
        }
        if (venster != null) {
            return true;
        }
        if (k == GLFW.GLFW_KEY_Q) {
            Overzicht.draai(-1);
            return true;
        }
        if (k == GLFW.GLFW_KEY_E) {
            Overzicht.draai(1);
            return true;
        }
        if (k == GLFW.GLFW_KEY_R && modus == Modus.PLAATSEN) {
            plaatsRot = (plaatsRot + 1) & 3;
            return true;
        }
        if (ErfenisClient.TOETS.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (venster != null) {
                venster = null;
            } else if (modus != Modus.KIJKEN) {
                modus = Modus.KIJKEN;
            } else {
                kies(-1, -1, -1);
            }
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return true;
        }
        for (int i = klikken.size() - 1; i >= 0; i--) {
            Ui.Klik k = klikken.get(i);
            if (k.op(mx, my)) {
                if (k.actie() != null) {
                    k.actie().run();
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.5f));
                }
                return true;
            }
        }
        if (venster != null) {
            return true;
        }
        ingedrukt = true;
        gesleept = false;
        startX = vorigX = mx;
        startY = vorigY = my;
        kijkOnderMuis(mx, my);
        sleepKandidaat = modus == Modus.KIJKEN && eigen() && hoverGebouw != null && hoverGebouw.id == geselecteerd;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!ingedrukt || event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return true;
        }
        double mx = event.x(), my = event.y();
        if (!gesleept && (Math.abs(mx - startX) > 4 || Math.abs(my - startY) > 4)) {
            gesleept = true;
            if (sleepKandidaat) {
                verplaatsStart(geselecteerd);
                sleepVerplaats = true;
            }
        }
        if (gesleept && !sleepVerplaats) {
            Vec3 a = Overzicht.grond(vorigX, vorigY, width, height), b = Overzicht.grond(mx, my, width, height);
            if (a != null && b != null) {
                Overzicht.schuif(a.x - b.x, a.z - b.z);
            }
        }
        vorigX = mx;
        vorigY = my;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !ingedrukt) {
            return true;
        }
        ingedrukt = false;
        Base b = base();
        if (b == null) {
            return true;
        }
        kijkOnderMuis(event.x(), event.y());
        if (sleepVerplaats) {
            sleepVerplaats = false;
            if (!verplaats(b)) {
                modus = Modus.KIJKEN;
            }
            return true;
        }
        if (gesleept) {
            return true;
        }
        switch (modus) {
            case KIJKEN -> {
                if (hoverGebouw != null) {
                    kies(hoverGebouw.id, -1, -1);
                } else if (hoverPlukje(b) >= 0) {
                    kies(-1, hoverPlukje(b), -1);
                } else if (hoverKavel(b) >= 0) {
                    kies(-1, -1, hoverKavel(b));
                } else {
                    kies(-1, -1, -1);
                }
            }
            case PLAATSEN -> {
                if (plaatsType != null && hoverOpPlot) {
                    int w = plaatsType.b(plaatsRot), h = plaatsType.d(plaatsRot);
                    int x = voetX(w), z = voetZ(h);
                    Base.Fout f = b.kanPlaatsen(plaatsType, x, z, plaatsRot);
                    if (f != null) {
                        toast(ErfenisClient.tekst(f.key(), List.of(f.args())), false);
                    } else {
                        ErfenisClient.stuur(new Actie(Actie.PLAATS, 0, x, z, 0, 0, plaatsRot, plaatsType.id));
                        modus = Modus.KIJKEN;
                    }
                }
            }
            case VERPLAATSEN -> verplaats(b);
        }
        return true;
    }

    /** Drops the building being moved on the hovered spot. True: sent. */
    private boolean verplaats(Base b) {
        Gebouw g = b.gebouw(verplaatsId);
        if (g == null || !hoverOpPlot) {
            return false;
        }
        int x = voetX(g.b()), z = voetZ(g.d());
        if (x == g.x && z == g.z) {
            modus = Modus.KIJKEN;
            return true;
        }
        Base.Fout f = b.kanVerplaatsen(g.id, x, z);
        if (f != null) {
            toast(ErfenisClient.tekst(f.key(), List.of(f.args())), false);
            return false;
        }
        ErfenisClient.stuur(new Actie(Actie.VERPLAATS, g.id, x, z, 0, 0, 0, ""));
        modus = Modus.KIJKEN;
        kies(g.id, -1, -1);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (venster != null) {
            venster.scrol(scrollY);
            return true;
        }
        if (paneelX0 >= 0 && mx >= paneelX0 && my >= 34 && my < paneelY1) {
            paneel.scrol(scrollY);
            return true;
        }
        for (Ui.Klik k : klikken) {
            if (k.op(mx, my)) {
                return true;
            }
        }
        Overzicht.zoom(scrollY);
        return true;
    }
}
