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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.erfenis.Base;
import nl.juiced.guhs.feature.erfenis.ErfenisPayloads;
import nl.juiced.guhs.feature.erfenis.Gebouw;
import nl.juiced.guhs.feature.erfenis.GebouwType;
import nl.juiced.guhs.feature.erfenis.Plot;
import nl.juiced.guhs.feature.erfenis.Vormen;
import org.lwjgl.glfw.GLFW;

/**
 * De Erfenis (POC): the overview (bovenaanzicht). A screen without background over the real world, seen through the
 * {@link Overzicht} camera; the mouse is free.
 * <ul>
 *   <li><b>Top:</b> whose erfenis (and "alleen kijken" for a visitor), Bouwspul (amount/cap with a bar), Knusheid, stadhuis
 *   level, the builder (vrij, or a timer).</li>
 *   <li><b>World:</b> hover outline, selection, a floating timer above a building under construction, a bubble above a
 *   molen with something to collect (click it). Drag the ground or use WASD to pan, scroll to zoom, Q/E to turn.</li>
 *   <li><b>Right:</b> the selected building: what it is and does, the next level (cost, time, or the lock reason) and the
 *   buttons Verbeteren, Verplaatsen, Draaien, Ophalen. Drag a selected building to move it.</li>
 *   <li><b>Bottom:</b> the shop: every type with cost, count/max and lock reason. A choice gives a ghost on the grid: green
 *   = may, red = may not; click to place; walls are dragged as a line and stay in placing mode.</li>
 * </ul>
 * Read-only (somebody else's base): no shop and no buttons, only looking. The server checks everything again.
 */
public final class OverzichtScherm extends Screen {
    private enum Modus { KIJKEN, PLAATSEN, VERPLAATSEN }

    private record Klik(int x0, int y0, int x1, int y1, @Nullable Runnable actie, @Nullable List<Component> tip) {
        boolean op(double mx, double my) {
            return mx >= x0 && mx < x1 && my >= y0 && my < y1;
        }
    }

    private record Toast(Component tekst, boolean goed, long tot) {
    }

    private static final int SHOP_W = 64, SHOP_H = 50, PANEEL_W = 152;
    private static final ItemStack BOUWSPUL = new ItemStack(Items.BRICK), KNUS = new ItemStack(Items.PINK_TULIP),
            BOUWER = new ItemStack(Items.GOLDEN_SHOVEL);

    @Nullable
    private static OverzichtScherm huidig;

    private Modus modus = Modus.KIJKEN;
    @Nullable
    private GebouwType plaatsType;
    private int plaatsRot;
    private int verplaatsId = -1;
    private int geselecteerd = -1;
    // the mouse
    private boolean ingedrukt, gesleept, sleepKandidaat, sleepVerplaats;
    private double startX, startY, vorigX, vorigY;
    @Nullable
    private int[] muurStart;
    // what is under the mouse (updated every frame)
    private int hoverX = -1, hoverZ = -1;
    private boolean hoverOpPlot;
    @Nullable
    private Gebouw hoverGebouw;
    private final List<Klik> klikken = new ArrayList<>();
    private final List<Toast> toasts = new ArrayList<>();

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
        return true;
    }

    boolean toontRaster() {
        return modus != Modus.KIJKEN;
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
        if (modus == Modus.VERPLAATSEN && b.gebouw(verplaatsId) == null) {
            modus = Modus.KIJKEN;
        }
        if (!eigen()) {
            modus = Modus.KIJKEN;
        }
    }

    void toast(Component tekst, boolean goed) {
        toasts.add(new Toast(tekst, goed, System.currentTimeMillis() + 2800));
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

    // =====================================================================================================================
    // the world under the mouse
    // =====================================================================================================================

    private static int hoogte(GebouwType t, int level) {
        return Vormen.hoogte(t, level, false);
    }

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
                AABB doos = new AABB(o.getX() + g.x, y0, o.getZ() + g.z, o.getX() + g.x + g.type.maat,
                        Overzicht.grondY() + Math.max(1, hoogte(g.type, g.level)), o.getZ() + g.z + g.type.maat);
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
            hoverOpPlot = hoverX >= 0 && hoverZ >= 0 && hoverX < Plot.MAAT && hoverZ < Plot.MAAT;
            if (hoverGebouw == null && hoverOpPlot && modus == Modus.KIJKEN) {
                hoverGebouw = b.op(hoverX, hoverZ);
            }
        }
    }

    /** The min corner of a footprint of this size centred on the hovered tile. */
    private int voetX(int maat) {
        return hoverX - (maat - 1) / 2;
    }

    private int voetZ(int maat) {
        return hoverZ - (maat - 1) / 2;
    }

    /** The tiles of the wall line being dragged (the longer axis wins, like the server). */
    private List<int[]> muurLijn() {
        List<int[]> l = new ArrayList<>();
        if (muurStart == null) {
            l.add(new int[] {hoverX, hoverZ});
            return l;
        }
        int dx = hoverX - muurStart[0], dz = hoverZ - muurStart[1];
        boolean langsX = Math.abs(dx) >= Math.abs(dz);
        int n = Math.min(Plot.MAAT, langsX ? Math.abs(dx) : Math.abs(dz));
        int stap = (langsX ? dx : dz) >= 0 ? 1 : -1;
        for (int i = 0; i <= n; i++) {
            l.add(langsX ? new int[] {muurStart[0] + i * stap, muurStart[1]} : new int[] {muurStart[0], muurStart[1] + i * stap});
        }
        return l;
    }

    /** What the world drawing shows this frame: ghosts, hover and selection. */
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
            l.add(new OverzichtWereld.Doos(o.getX() + sel.x, gy, o.getZ() + sel.z, o.getX() + sel.x + sel.type.maat,
                    gy + hoogte(sel.type, sel.level) + 0.05, o.getZ() + sel.z + sel.type.maat, 0x30F7B6CB, 0xF0F07CB4, 0.12f));
        }
        if (modus == Modus.KIJKEN && hoverGebouw != null && hoverGebouw != sel) {
            Gebouw g = hoverGebouw;
            l.add(new OverzichtWereld.Doos(o.getX() + g.x, gy, o.getZ() + g.z, o.getX() + g.x + g.type.maat,
                    gy + hoogte(g.type, g.level) + 0.05, o.getZ() + g.z + g.type.maat, 0, 0xC0FFFFFF, 0.08f));
        } else if (modus == Modus.KIJKEN && hoverOpPlot && hoverGebouw == null) {
            l.add(new OverzichtWereld.Doos(o.getX() + hoverX, gy, o.getZ() + hoverZ, o.getX() + hoverX + 1, gy + 0.04, o.getZ() + hoverZ + 1,
                    0x40FFFFFF, 0, 0));
        }
        if (modus == Modus.PLAATSEN && plaatsType != null && hoverX > -100) {
            if (plaatsType.maat == 1) {
                int over = Math.min(plaatsType.maxAantal(b.stadhuisLevel()) - b.aantal(plaatsType),
                        plaatsType.kosten(1) <= 0 ? 999 : b.bouwspul / plaatsType.kosten(1));
                for (int[] t : muurLijn()) {
                    boolean vrij = b.vrij(t[0], t[1], 1, -1);
                    boolean mag = vrij && over-- > 0;
                    l.add(spook(o, t[0], t[1], 1, hoogte(plaatsType, 1), mag, !vrij));
                }
            } else {
                int x = voetX(plaatsType.maat), z = voetZ(plaatsType.maat);
                l.add(spook(o, x, z, plaatsType.maat, hoogte(plaatsType, 1), b.kanPlaatsen(plaatsType, x, z) == null, false));
            }
        }
        if (modus == Modus.VERPLAATSEN) {
            Gebouw g = b.gebouw(verplaatsId);
            if (g != null) {
                l.add(new OverzichtWereld.Doos(o.getX() + g.x, gy, o.getZ() + g.z, o.getX() + g.x + g.type.maat, gy + 0.06,
                        o.getZ() + g.z + g.type.maat, 0x30FFFFFF, 0x80FFFFFF, 0.06f));
                int x = voetX(g.type.maat), z = voetZ(g.type.maat);
                l.add(spook(o, x, z, g.type.maat, hoogte(g.type, Math.max(1, g.level)), b.kanVerplaatsen(g.id, x, z) == null, false));
            }
        }
        return l;
    }

    private static OverzichtWereld.Doos spook(BlockPos o, int x, int z, int maat, int h, boolean mag, boolean zacht) {
        double gy = Overzicht.grondY();
        int vul = zacht ? 0x18E06C7E : mag ? 0x5066D98A : 0x60E8505E;
        int rand = zacht ? 0x60E06C7E : mag ? 0xE046C070 : 0xE0E0404E;
        return new OverzichtWereld.Doos(o.getX() + x + 0.02, gy + 0.01, o.getZ() + z + 0.02, o.getX() + x + maat - 0.02, gy + h,
                o.getZ() + z + maat - 0.02, vul, rand, 0.1f);
    }

    // =====================================================================================================================
    // drawing
    // =====================================================================================================================

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        klikken.clear();
        Base b = base();
        if (b == null) {
            return;
        }
        kijkOnderMuis(mx, my);
        long nu = ErfenisClient.nu();
        zwevers(g, b, nu, mx, my);
        bovenbalk(g, b, nu, mx, my);
        if (geselecteerd >= 0 && b.gebouw(geselecteerd) != null && modus == Modus.KIJKEN) {
            paneel(g, b, b.gebouw(geselecteerd), nu, mx, my);
        }
        if (eigen()) {
            winkel(g, b, mx, my);
        }
        hulp(g);
        toasts(g);
        // tooltips last
        for (int i = klikken.size() - 1; i >= 0; i--) {
            Klik k = klikken.get(i);
            if (k.op(mx, my)) {
                if (k.tip() != null && !k.tip().isEmpty()) {
                    g.setComponentTooltipForNextFrame(font, k.tip(), mx, my);
                }
                return;
            }
        }
        if (modus == Modus.KIJKEN && hoverGebouw != null) {
            Gebouw h = hoverGebouw;
            g.setComponentTooltipForNextFrame(font, List.of(naam(h).copy().withStyle(ChatFormatting.BOLD),
                    h.level == 0 ? Component.translatable("erfenis.ui.in_aanbouw") : Component.translatable("erfenis.ui.level", h.level)), mx, my);
        }
    }

    private static Component naam(Gebouw g) {
        return Component.translatable(g.type.naamKey());
    }

    /** Above the buildings: the construction timer, the molen's collect bubble. */
    private void zwevers(GuiGraphicsExtractor g, Base b, long nu, int mx, int my) {
        BlockPos o = ErfenisClient.oorsprong();
        for (Gebouw geb : b.gebouwen) {
            double cx = o.getX() + geb.x + geb.type.maat / 2.0, cz = o.getZ() + geb.z + geb.type.maat / 2.0;
            double top = Overzicht.grondY() + hoogte(geb.type, Math.max(geb.level, 0)) + 1.2;
            if (b.inAanbouw(geb)) {
                double[] p = Overzicht.projecteer(new Vec3(cx, top + 0.6, cz), width, height);
                if (p != null) {
                    String t = Teken.tijd(b.bouw.eind() - nu);
                    int w = Math.max(44, font.width(t) + 24), x = (int) p[0] - w / 2, y = (int) p[1] - 9;
                    Teken.chip(g, x, y, x + w, y + 17);
                    g.item(BOUWER, x + 1, y);   // (the shovel, a bit cut by the chip: cute)
                    g.text(font, t, x + 18, y + 3, Teken.DONKER, false);
                    Teken.balk(g, x + 18, y + 12, x + w - 3, y + 15, b.bouw.voortgang(nu), Teken.GOUD);
                }
            }
            if (geb.type == GebouwType.MOLEN && geb.level > 0) {
                double v = geb.voorraadOp(nu);
                if (v >= 1) {
                    double[] p = Overzicht.projecteer(new Vec3(cx, top, cz), width, height);
                    if (p != null) {
                        boolean vol = v >= GebouwType.molenOpslag(geb.level);
                        String t = Integer.toString((int) v);
                        int w = font.width(t) + 24, x = (int) p[0] - w / 2, y = (int) p[1] - 10;
                        boolean op = mx >= x && mx < x + w && my >= y && my < y + 18;
                        Teken.rond(g, x, y + 1, x + w, y + 19, 0x50501830, 4);
                        Teken.rond(g, x, y, x + w, y + 18, vol ? Teken.GOUD : Teken.ROZE_DONKER, 4);
                        Teken.rond(g, x + 1, y + 1, x + w - 1, y + 17, op && eigen() ? Teken.WIT : Teken.CREME_VOL, 3);
                        g.fill(x + w / 2 - 2, y + 18, x + w / 2 + 2, y + 20, vol ? Teken.GOUD : Teken.ROZE_DONKER);   // (the bubble's tail)
                        g.item(BOUWSPUL, x + 2, y + 1);
                        g.text(font, t, x + 19, y + 5, Teken.DONKER, false);
                        int id = geb.id;
                        klikken.add(new Klik(x, y, x + w, y + 20, eigen() ? () -> ErfenisClient.stuur(ErfenisPayloads.Actie.op(ErfenisPayloads.Actie.OPHALEN, id)) : null,
                                List.of(Component.translatable("erfenis.ui.ophalen").withStyle(ChatFormatting.BOLD),
                                        Component.translatable("erfenis.ui.binnen", (int) v, GebouwType.molenOpslag(geb.level)))));
                    }
                }
            }
        }
    }

    private void bovenbalk(GuiGraphicsExtractor g, Base b, long nu, int mx, int my) {
        // left: whose erfenis, with the close and back buttons under it
        Component titel = b.npc ? Component.translatable("erfenis.ui.dorp", b.naam) : Component.translatable("erfenis.ui.van", b.naam);
        Component sub = eigen() ? (ErfenisClient.snel ? Component.translatable("erfenis.ui.snel") : null) : Component.translatable("erfenis.ui.kijken");
        int w = Math.max(font.width(titel) + 16, sub == null ? 0 : font.width(sub) + 16);
        int h = sub == null ? 20 : 30;
        Teken.paneel(g, 6, 6, 6 + w, 6 + h);
        g.text(font, titel.copy().withStyle(ChatFormatting.BOLD), 14, 12, Teken.DONKER, false);
        if (sub != null) {
            g.text(font, sub, 14, 23, eigen() ? Teken.ROOD : Teken.ZACHT, false);
        }
        klikken.add(new Klik(6, 6, 6 + w, 6 + h, null, null));
        int by = 6 + h + 4;
        Component sluit = Component.translatable("erfenis.ui.sluiten"), terug = Component.translatable("erfenis.ui.terug");
        int w1 = font.width(sluit) + 12, w2 = font.width(terug) + 12;
        Teken.knop(g, font, sluit, 6, by, 6 + w1, by + 14, mx, my, true, Teken.ROZE_DONKER);
        klikken.add(new Klik(6, by, 6 + w1, by + 14, this::onClose, null));
        Teken.knop(g, font, terug, 10 + w1, by, 10 + w1 + w2, by + 14, mx, my, true, 0xFFB98AA6);
        klikken.add(new Klik(10 + w1, by, 10 + w1 + w2, by + 14, () -> {
            sluitAlles();
            ErfenisClient.stuur(ErfenisPayloads.Actie.simpel(ErfenisPayloads.Actie.TERUG));
        }, null));
        // right: the chips
        int x = width - 6;
        // the builder
        Component bouwer;
        int bouwerKleur;
        List<Component> bouwerTip = new ArrayList<>();
        bouwerTip.add(Component.translatable("erfenis.ui.bouwer").withStyle(ChatFormatting.BOLD));
        if (b.bouw != null) {
            bouwer = Component.literal(Teken.tijd(b.bouw.eind() - nu));
            bouwerKleur = Teken.DONKER;
            Gebouw bg = b.gebouw(b.bouw.gebouw());
            if (bg != null) {
                bouwerTip.add(Component.translatable("erfenis.ui.bouwer_bezig").append(": ").append(naam(bg)).append(" ")
                        .append(Component.translatable("erfenis.ui.level", b.bouw.naar())));
            }
        } else {
            bouwer = Component.translatable("erfenis.ui.bouwer_vrij");
            bouwerKleur = Teken.GROEN_DONKER;
        }
        x = chip(g, x, BOUWER, bouwer, bouwerKleur, b.bouw != null ? b.bouw.voortgang(nu) : -1, Teken.GOUD, bouwerTip);
        x = chip(g, x, GebouwType.STADHUIS.icoon(), Component.translatable("erfenis.ui.level", b.stadhuisLevel()), Teken.DONKER, -1, 0,
                List.of(Component.translatable("erfenis.ui.stadhuis").withStyle(ChatFormatting.BOLD)));
        x = chip(g, x, KNUS, Component.literal(Integer.toString(b.knusheid())), Teken.DONKER, -1, 0,
                List.of(Component.translatable("erfenis.ui.knusheid").withStyle(ChatFormatting.BOLD)));
        int cap = b.opslag();
        chip(g, x, BOUWSPUL, Component.literal(b.bouwspul + "/" + cap), b.bouwspul >= cap ? Teken.ROOD : Teken.DONKER,
                cap <= 0 ? 0 : b.bouwspul / (float) cap, Teken.ROZE_DONKER,
                List.of(Component.translatable("erfenis.ui.bouwspul").withStyle(ChatFormatting.BOLD)));
    }

    /** One chip of the top bar, right-aligned at x; returns the x left of it. A bar under the text when deel >= 0. */
    private int chip(GuiGraphicsExtractor g, int rechts, ItemStack icoon, Component tekst, int kleur, float deel, int balkKleur, List<Component> tip) {
        int w = font.width(tekst) + 26, x = rechts - w, y = 6;
        Teken.chip(g, x, y, rechts, y + 20);
        g.item(icoon, x + 3, y + 2);
        g.text(font, tekst, x + 21, y + (deel >= 0 ? 4 : 6), kleur, false);
        if (deel >= 0) {
            Teken.balk(g, x + 21, y + 14, rechts - 4, y + 17, deel, balkKleur);
        }
        klikken.add(new Klik(x, y, rechts, y + 20, null, tip));
        return x - 4;
    }

    /** The panel of the selected building. */
    private void paneel(GuiGraphicsExtractor g, Base b, Gebouw geb, long nu, int mx, int my) {
        int x0 = width - PANEEL_W - 6, x1 = width - 6, y0 = 34;
        int tw = PANEEL_W - 16;
        List<FormattedCharSequence> uitleg = font.split(Component.translatable(geb.type.uitlegKey()), tw);
        List<Component> stats = new ArrayList<>();
        int lvl = Math.max(1, geb.level);
        switch (geb.type) {
            case MOLEN -> {
                stats.add(Component.translatable("erfenis.ui.produceert", GebouwType.productiePerMinuut(lvl)));
                stats.add(Component.translatable("erfenis.ui.binnen", (int) geb.voorraadOp(nu), GebouwType.molenOpslag(lvl)));
            }
            case SCHUURTJE -> stats.add(Component.translatable("erfenis.ui.opslag", GebouwType.schuurOpslag(lvl)));
            case STADHUIS -> stats.add(Component.translatable("erfenis.ui.opslag", GebouwType.stadhuisOpslag(lvl)));
            default -> {
            }
        }
        if (geb.type.knus(lvl) > 0) {
            stats.add(Component.translatable("erfenis.ui.knus", geb.type.knus(lvl)));
        }
        // the next level
        List<Component> volgende = new ArrayList<>();
        Base.Fout fout = b.kanVerbeteren(geb.id);
        boolean bezig = b.inAanbouw(geb);
        if (!bezig) {
            if (geb.level >= geb.type.topLevel()) {
                volgende.add(Component.translatable("erfenis.ui.max").withColor(Teken.GROEN_DONKER));
            } else {
                int naar = geb.level + 1;
                volgende.add(Component.translatable("erfenis.ui.volgende", naar).withStyle(ChatFormatting.BOLD));
                volgende.add(Component.translatable("erfenis.ui.kosten", geb.type.kosten(naar))
                        .withColor(b.bouwspul >= geb.type.kosten(naar) ? Teken.TEKST : Teken.ROOD));
                int sec = geb.type.tijdSec(naar);
                volgende.add(ErfenisClient.snel || sec == 0 ? Component.translatable("erfenis.ui.direct")
                        : Component.translatable("erfenis.ui.tijd", Teken.tijd(sec * 1000L)));
                if (fout != null && !"erfenis.fout.bouwspul".equals(fout.key())) {
                    volgende.add(ErfenisClient.tekst(fout.key(), List.of(fout.args())).copy().withColor(Teken.ROOD));
                }
            }
        }
        boolean eigen = eigen();
        int knoppen = !eigen ? 0 : (geb.type == GebouwType.MOLEN ? 3 : 2);
        int h = 8 + 20 + uitleg.size() * 9 + 4 + stats.size() * 10 + 4 + (bezig ? 22 : volgende.size() * 10) + 4 + knoppen * 17 + 4;
        int ruimte = height - (eigen ? SHOP_H + 26 : 24) - y0;
        if (h > ruimte) {
            uitleg = uitleg.subList(0, Math.max(1, uitleg.size() - (h - ruimte + 8) / 9));
            h = Math.min(h, ruimte);
        }
        int y1 = y0 + h;
        Teken.paneel(g, x0, y0, x1, y1);
        klikken.add(new Klik(x0, y0, x1, y1, null, null));
        int y = y0 + 6;
        g.item(geb.type.icoon(), x0 + 6, y);
        g.text(font, naam(geb).copy().withStyle(ChatFormatting.BOLD), x0 + 26, y, Teken.DONKER, false);
        g.text(font, geb.level == 0 ? Component.translatable("erfenis.ui.in_aanbouw") : Component.translatable("erfenis.ui.level", geb.level),
                x0 + 26, y + 10, Teken.ROZE_DONKER, false);
        y += 22;
        for (FormattedCharSequence r : uitleg) {
            g.text(font, r, x0 + 8, y, Teken.ZACHT, false);
            y += 9;
        }
        y += 4;
        for (Component s : stats) {
            g.text(font, Component.literal("• ").append(s), x0 + 8, y, Teken.TEKST, false);
            y += 10;
        }
        y += 2;
        g.fill(x0 + 8, y, x1 - 8, y + 1, 0x40D27A9C);
        y += 4;
        if (bezig) {
            g.text(font, Component.translatable("erfenis.ui.bouwer_bezig").append(" · ").append(Teken.tijd(b.bouw.eind() - nu)), x0 + 8, y,
                    Teken.DONKER, false);
            Teken.balk(g, x0 + 8, y + 11, x1 - 8, y + 16, b.bouw.voortgang(nu), Teken.GOUD);
            y += 22;
        } else {
            for (Component v : volgende) {
                g.text(font, v, x0 + 8, y, Teken.TEKST, false);
                y += 10;
            }
        }
        y += 2;
        if (!eigen) {
            return;
        }
        int id = geb.id;
        int bx0 = x0 + 8, bx1 = x1 - 8;
        boolean kanV = fout == null;
        List<Component> tipV = fout == null ? null : List.of(ErfenisClient.tekst(fout.key(), List.of(fout.args())));
        Teken.knop(g, font, Component.translatable("erfenis.ui.verbeteren"), bx0, y, bx1, y + 14, mx, my, kanV, Teken.GROEN);
        klikken.add(new Klik(bx0, y, bx1, y + 14, kanV ? () -> ErfenisClient.stuur(ErfenisPayloads.Actie.op(ErfenisPayloads.Actie.VERBETER, id)) : null, tipV));
        y += 17;
        int mid = (bx0 + bx1) / 2;
        Teken.knop(g, font, Component.translatable("erfenis.ui.verplaatsen"), bx0, y, mid - 2, y + 14, mx, my, true, Teken.ROZE_DONKER);
        klikken.add(new Klik(bx0, y, mid - 2, y + 14, () -> {
            modus = Modus.VERPLAATSEN;
            verplaatsId = id;
        }, null));
        Teken.knop(g, font, Component.translatable("erfenis.ui.draaien"), mid + 2, y, bx1, y + 14, mx, my, true, Teken.ROZE_DONKER);
        klikken.add(new Klik(mid + 2, y, bx1, y + 14, () -> ErfenisClient.stuur(ErfenisPayloads.Actie.op(ErfenisPayloads.Actie.DRAAI, id)), null));
        y += 17;
        if (geb.type == GebouwType.MOLEN) {
            Base.Fout f = b.kanOphalen(id, nu);
            Component t = Component.translatable("erfenis.ui.ophalen").append(" (" + (int) geb.voorraadOp(nu) + ")");
            Teken.knop(g, font, t, bx0, y, bx1, y + 14, mx, my, f == null, Teken.GOUD);
            klikken.add(new Klik(bx0, y, bx1, y + 14, f == null ? () -> ErfenisClient.stuur(ErfenisPayloads.Actie.op(ErfenisPayloads.Actie.OPHALEN, id)) : null,
                    f == null ? null : List.of(ErfenisClient.tekst(f.key(), List.of(f.args())))));
        }
    }

    /** The shop bar at the bottom. */
    private void winkel(GuiGraphicsExtractor g, Base b, int mx, int my) {
        GebouwType[] types = GebouwType.values();
        int n = 0;
        for (GebouwType t : types) {
            if (t != GebouwType.STADHUIS) {
                n++;
            }
        }
        int gap = 5, totaal = n * SHOP_W + (n - 1) * gap + 12;
        int x0 = (width - totaal) / 2, y0 = height - SHOP_H - 12;
        Teken.paneel(g, x0, y0, x0 + totaal, y0 + SHOP_H + 8);
        klikken.add(new Klik(x0, y0, x0 + totaal, y0 + SHOP_H + 8, null, null));
        int th = b.stadhuisLevel();
        int x = x0 + 6;
        for (GebouwType t : types) {
            if (t == GebouwType.STADHUIS) {
                continue;
            }
            Base.Fout f = b.kanKopen(t);
            boolean gekozen = modus == Modus.PLAATSEN && plaatsType == t;
            boolean op = mx >= x && mx < x + SHOP_W && my >= y0 + 4 && my < y0 + 4 + SHOP_H;
            int y = y0 + 4;
            int rand = gekozen ? Teken.GOUD : op ? Teken.ROZE_DONKER : Teken.ROZE;
            Teken.rond(g, x, y, x + SHOP_W, y + SHOP_H, rand, 3);
            Teken.rond(g, x + 1, y + 1, x + SHOP_W - 1, y + SHOP_H - 1, f == null ? (op ? 0xFFFFFFFF : 0xFFFFF0F5) : 0xFFEDE3E8, 3);
            g.item(t.icoon(), x + (SHOP_W - 16) / 2, y + 3);
            if (f != null) {
                g.fill(x + (SHOP_W - 16) / 2, y + 3, x + (SHOP_W + 16) / 2, y + 19, 0x80EDE3E8);   // (greyed icon)
            }
            String telling = b.aantal(t) + "/" + t.maxAantal(th);
            g.text(font, telling, x + SHOP_W - 3 - font.width(telling), y + 3, Teken.ZACHT, false);
            Component nm = Component.translatable(t.naamKey());
            String naamTekst = font.plainSubstrByWidth(nm.getString(), SHOP_W - 4);
            g.text(font, naamTekst, x + (SHOP_W - font.width(naamTekst)) / 2, y + 21, f == null ? Teken.DONKER : Teken.ZACHT, false);
            // the cost (or the lock)
            String regel;
            int kleur;
            if (f != null && ("erfenis.fout.slot".equals(f.key()) || "erfenis.fout.limiet".equals(f.key()))) {
                regel = "erfenis.fout.slot".equals(f.key()) ? "SH-lvl " + f.args()[0] : "max";
                kleur = Teken.ROOD;
            } else {
                regel = Integer.toString(t.kosten(1));
                kleur = b.bouwspul >= t.kosten(1) ? Teken.DONKER : Teken.ROOD;
            }
            int rw = font.width(regel) + ("max".equals(regel) || regel.startsWith("SH") ? 0 : 11);
            int rx = x + (SHOP_W - rw) / 2;
            if (!("max".equals(regel) || regel.startsWith("SH"))) {
                g.pose().pushMatrix();
                g.pose().translate(rx, y + 32);
                g.pose().scale(0.6f, 0.6f);
                g.item(BOUWSPUL, 0, 0);
                g.pose().popMatrix();
                rx += 11;
            }
            g.text(font, regel, rx, y + 34, kleur, false);
            List<Component> tip = new ArrayList<>();
            tip.add(nm.copy().withStyle(ChatFormatting.BOLD));
            for (FormattedCharSequence r : font.split(Component.translatable(t.uitlegKey()), 180)) {
                tip.add(Component.literal(stuk(r)).withColor(Teken.ZACHT));
            }
            tip.add(Component.translatable("erfenis.ui.kosten", t.kosten(1)));
            tip.add(t.direct ? Component.translatable("erfenis.ui.direct")
                    : Component.translatable("erfenis.ui.tijd", Teken.tijd((ErfenisClient.snel ? 0 : t.tijdSec(1)) * 1000L)));
            if (f != null) {
                tip.add(ErfenisClient.tekst(f.key(), List.of(f.args())).copy().withColor(Teken.ROOD));
            }
            final GebouwType type = t;
            klikken.add(new Klik(x, y, x + SHOP_W, y + SHOP_H, () -> kies(type), tip));
            x += SHOP_W + gap;
        }
    }

    private static String stuk(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((i, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }

    private void kies(GebouwType t) {
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
        geselecteerd = -1;
    }

    private void hulp(GuiGraphicsExtractor g) {
        String key = switch (modus) {
            case PLAATSEN -> plaatsType != null && plaatsType.maat == 1 ? "erfenis.ui.hulp_muur" : "erfenis.ui.hulp_plaats";
            case VERPLAATSEN -> "erfenis.ui.hulp_verplaats";
            default -> "erfenis.ui.hulp";
        };
        Component t = Component.translatable(key);
        int w = font.width(t) + 12;
        int y = eigen() ? height - SHOP_H - 28 : height - 18;
        int x = (width - w) / 2;
        Teken.rond(g, x, y, x + w, y + 13, modus == Modus.KIJKEN ? 0x90402030 : 0xC0D27A9C, 3);
        g.text(font, t, x + 6, y + 3, Teken.WIT, false);
    }

    private void toasts(GuiGraphicsExtractor g) {
        long nu = System.currentTimeMillis();
        toasts.removeIf(t -> t.tot() < nu);
        int y = 34;
        for (Toast t : toasts) {
            int w = font.width(t.tekst()) + 16, x = (width - w) / 2;
            Teken.rond(g, x, y + 1, x + w, y + 17, 0x50501830, 3);
            Teken.rond(g, x, y, x + w, y + 16, t.goed() ? Teken.GROEN_DONKER : Teken.ROOD, 3);
            Teken.rond(g, x + 1, y + 1, x + w - 1, y + 15, Teken.CREME_VOL, 3);
            g.text(font, t.tekst(), x + 8, y + 4, t.goed() ? Teken.GROEN_DONKER : Teken.ROOD, false);
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
            if (modus != Modus.KIJKEN) {
                modus = Modus.KIJKEN;
                muurStart = null;
                return true;
            }
            if (geselecteerd >= 0) {
                geselecteerd = -1;
                return true;
            }
            onClose();
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
            if (modus != Modus.KIJKEN) {
                modus = Modus.KIJKEN;
                muurStart = null;
            } else {
                geselecteerd = -1;
            }
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return true;
        }
        for (int i = klikken.size() - 1; i >= 0; i--) {
            Klik k = klikken.get(i);
            if (k.op(mx, my)) {
                if (k.actie() != null) {
                    k.actie().run();
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.5f));
                }
                return true;
            }
        }
        ingedrukt = true;
        gesleept = false;
        startX = vorigX = mx;
        startY = vorigY = my;
        kijkOnderMuis(mx, my);
        sleepKandidaat = modus == Modus.KIJKEN && eigen() && hoverGebouw != null && hoverGebouw.id == geselecteerd;
        muurStart = modus == Modus.PLAATSEN && plaatsType != null && plaatsType.maat == 1 && hoverOpPlot ? new int[] {hoverX, hoverZ} : null;
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
                modus = Modus.VERPLAATSEN;
                verplaatsId = geselecteerd;
                sleepVerplaats = true;
            }
        }
        if (gesleept && muurStart == null && !sleepVerplaats) {
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
        if (muurStart != null) {
            if (hoverX > -100) {
                ErfenisClient.stuur(new ErfenisPayloads.Actie(ErfenisPayloads.Actie.LIJN, 0, muurStart[0], muurStart[1], hoverX, hoverZ, 0,
                        plaatsType.id));
            }
            muurStart = null;
            return true;
        }
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
            case KIJKEN -> geselecteerd = hoverGebouw == null ? -1 : hoverGebouw.id;
            case PLAATSEN -> {
                if (plaatsType != null && hoverOpPlot) {
                    int x = voetX(plaatsType.maat), z = voetZ(plaatsType.maat);
                    Base.Fout f = b.kanPlaatsen(plaatsType, x, z);
                    if (f != null) {
                        toast(ErfenisClient.tekst(f.key(), List.of(f.args())), false);
                    } else {
                        ErfenisClient.stuur(new ErfenisPayloads.Actie(ErfenisPayloads.Actie.PLAATS, 0, x, z, 0, 0, plaatsRot, plaatsType.id));
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
        int x = voetX(g.type.maat), z = voetZ(g.type.maat);
        if (x == g.x && z == g.z) {
            modus = Modus.KIJKEN;
            return true;
        }
        Base.Fout f = b.kanVerplaatsen(g.id, x, z);
        if (f != null) {
            toast(ErfenisClient.tekst(f.key(), List.of(f.args())), false);
            return false;
        }
        ErfenisClient.stuur(new ErfenisPayloads.Actie(ErfenisPayloads.Actie.VERPLAATS, g.id, x, z, 0, 0, 0, ""));
        modus = Modus.KIJKEN;
        geselecteerd = g.id;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        for (Klik k : klikken) {
            if (k.op(mx, my)) {
                return true;
            }
        }
        Overzicht.zoom(scrollY);
        return true;
    }
}
