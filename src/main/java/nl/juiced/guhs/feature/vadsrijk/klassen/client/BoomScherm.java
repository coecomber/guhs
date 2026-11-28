package nl.juiced.guhs.feature.vadsrijk.klassen.client;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.klassen.KlassenPayloads;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.BoomIndeling;
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec;
import org.lwjgl.glfw.GLFW;

/**
 * The skill tree screen (key K in the dimension) and, opened at the ordetempel, the class desk (switch class, free respec,
 * the advancement ceremony). Pink, readable: a grid of nodes with their item icons, lines to what they need, a cost
 * badge, and a tooltip with what the node really does. Click a node once to select it, again to learn it.
 */
public final class BoomScherm extends Screen implements nl.juiced.guhs.feature.vadsrijk.menu.client.Geschaald {   // U-TABS: in the Vadsrijk-menu
    private static final int ROZE = 0xFFFF7AC8, LICHT = 0xFFFFC2E8, DIEP = 0xFF2A1022, PANEEL = 0xF0401A34, GOUD = 0xFFFFD24A, GRIJS = 0xFF8A6A7E;
    /** U-SPECS: 4 columns (spec A, trunk, spec B, side branch), a bit closer together. */
    private static final int KNOOP = BoomIndeling.KNOOP, DX = BoomIndeling.DX;
    private CompoundTag t;
    /** The dark tree panel: it fills the common menu frame (W x H follow the frame, DY the rows that fit). */
    private int left, top, W = 384, H = 240, DY = 38;
    /** U-TABS: the Vadsrijk-menu frame around the tree: the SAME frame as every other tab (the tree scrolls inside it). */
    private nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling.Kader kader;
    private BoomIndeling in;
    /** The scroll of the tree column, and the class it was last aimed for (a new class = show its next node again). */
    private double scroll;
    @Nullable
    private String scrollKlasse;
    /** A press in the tree view: a short one clicks the node under it, a long one drags the tree. */
    private boolean druk, balkSleep;
    private double sleep;
    @Nullable
    private String drukKnoop;
    /** The advancement badges sit on the right side when there is room, else below the tree (in the scrolled view). */
    private boolean badgesRechts;
    /** Top of the badges on the right side (relative to top), and how many lines of the hint text fit. */
    private int badgesRechtsY, hintRegels;
    @Nullable
    private String gekozen;

    public BoomScherm(CompoundTag t) {
        super(Component.translatable("vadsrijk.klassen.scherm.boom"));
        this.t = t;
        SkillInfo.bekend(knopen());   // fix round 2 (g-ui): for the HUD skill tooltips
    }

    public void zet(CompoundTag t) {
        this.t = t;
        SkillInfo.bekend(knopen());   // fix round 2 (g-ui): for the HUD skill tooltips
        gekozen = null;
        rebuildWidgets();
    }

    private boolean balie() {
        return t.getStringOr("modus", "boom").equals("balie");
    }

    private Klasse klasse() {
        return Klassen.ofRidder(t.getStringOr("klasse", Klassen.RIDDER));
    }

    private Set<String> knopen() {
        String s = t.getStringOr("knopen", "");
        return s.isEmpty() ? new LinkedHashSet<>() : new LinkedHashSet<>(List.of(s.split(",")));
    }

    private static void stuur(String soort, String arg) {
        ClientPacketDistributor.sendToServer(new KlassenPayloads.Actie(soort, arg));
    }

    /** z2-systemen: rows in this class's tree. */
    private int rijen() {
        int r = Bomen.RIJEN;
        for (BoomKnoop b : klasse().boom()) {
            r = Math.max(r, b.rij() + 1);
        }
        return r;
    }

    /** The skills listed on the right side: the class's first four and the Knuffelsprong. */
    private int skillRegels() {
        return Math.min(4, klasse().vaardigheden().size()) + 1;
    }

    private int skillStap() {
        return in.krap() ? 17 : 19;
    }

    /** The y positions (relative to top) of the class desk on the right side; a low panel packs them tighter. */
    private record Balie(int rijY, int rijStap, int rijH, int respecTekstY, int respecY, int advY, int specY, int advTekstY, int knopH) {
    }

    private Balie balieIndeling() {
        boolean krap = in.krap(), adv = t.getBooleanOr("advBalie", true);
        int stap = krap ? 24 : 30, rijH = krap ? 22 : 26, knopH = krap ? 16 : 18, gat = krap ? 2 : 4;
        int y = 48 + 3 * stap;
        int respecY = krap ? y + 2 : y + 6;
        int advY = adv ? respecY + knopH + gat : -1;
        int specY = (adv ? advY : respecY) + knopH + gat;
        return new Balie(48, stap, rijH, krap ? -1 : y - 3, respecY, advY, specY, adv ? specY + knopH + 3 : -1, knopH);
    }

    @Override
    protected void init() {
        // U-TABS: inside the common menu frame: band, then the class tabs (21 px), then the tree panel, which fills the frame. The
        // tree column scrolls when its rows do not fit (the frame is never made taller for it: that made the frame jump between tabs).
        kader = nl.juiced.guhs.feature.vadsrijk.menu.client.MenuClient.kader();
        width = kader.lw();
        height = kader.lh();
        boolean specKnop = !balie() && t.getBooleanOr("specOpen", false);
        // first as if the badges sit below the tree: the right side then tells whether they fit there instead
        in = BoomIndeling.van(kader, rijen(), true);
        badgesRechts = false;
        hintRegels = 0;
        if (!balie()) {
            int onder = in.h() - 6 - (specKnop ? 22 : 0);
            int hintY = 50 + skillRegels() * skillStap() + 4;
            int regels = font.split(Component.translatable("vadsrijk.klassen.scherm.ordetempel"), in.rw()).size();
            hintRegels = Math.max(0, Math.min(regels, (onder - hintY) / 9));
            int vrij = hintY + hintRegels * 9 + 4;
            badgesRechts = hintRegels == regels && in.rw() >= BoomIndeling.BADGES * 80 + (BoomIndeling.BADGES - 1) * 4
                    && vrij + 10 + BoomIndeling.BADGE_H <= onder;
            badgesRechtsY = vrij + 10;
            if (badgesRechts) {
                in = BoomIndeling.van(kader, rijen(), false);
            }
        }
        left = in.left();
        top = in.top();
        W = in.w();
        H = in.h();
        DY = in.dy();
        if (!klasse().id().equals(scrollKlasse)) {
            scrollKlasse = klasse().id();
            scroll = in.scrollVoor(volgendeRij());
        }
        scroll = in.klem(scroll);
        int rx = in.rx(), rw = in.rw();
        // g-klassen: a tab per class (you are always all three; the active one = the weapon you last attacked with)
        int tx = left;
        for (Klasse k : Klassen.alle()) {
            boolean nu = k.id().equals(klasse().id());
            boolean actief = k.id().equals(t.getStringOr("actief", ""));
            Component naam = Component.translatable(k.naamKey());
            Button b = Button.builder(actief ? Component.literal("⚔ ").append(naam) : naam, x -> stuur("tab", k.id()))
                    .bounds(tx, top - 21, 100, 18).tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.scherm.tab.tip",
                            naam, t.getIntOr("lv_" + k.id(), 1)))).build();
            b.active = !nu;
            addRenderableWidget(b);
            tx += 104;
        }
        if (balie()) {
            Balie bl = balieIndeling();
            int y = top + bl.rijY();
            long prijs = t.getLongOr("prijs", 30);
            for (Klasse k : Klassen.alle()) {
                boolean had = t.getBooleanOr("start_" + k.id(), false);
                Button b = Button.builder(had ? Component.translatable("vadsrijk.klassen.scherm.wapen.koop", nl.juiced.guhs.feature.vadsrijk.kern.Spelers.munten(prijs))
                        : Component.translatable("vadsrijk.klassen.scherm.wapen.gratis"), x -> stuur("wapen", k.id())).bounds(rx + rw - 74, y + (bl.rijH() - 16) / 2, 72, 16)
                        .tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.scherm.wapen.tip", Component.translatable(k.naamKey())))).build();
                addRenderableWidget(b);
                y += bl.rijStap();
            }
            long respec = t.getLongOr("respec", 0);
            Button r = Button.builder(respec > 0 ? Component.translatable("vadsrijk.klassen.scherm.respec.wacht", tijd(respec))
                    : Component.translatable("vadsrijk.klassen.scherm.respec"), x -> stuur("respec", ""))
                    .bounds(rx, top + bl.respecY(), rw, bl.knopH()).tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.scherm.respec.tip"))).build();
            r.active = respec <= 0 && !knopen().stream().allMatch(id -> id.contains("_adv") || id.contains("_spec_"));
            addRenderableWidget(r);
            if (bl.advY() >= 0) {
                Button a = Button.builder(Component.translatable("vadsrijk.klassen.scherm.adv").withStyle(ChatFormatting.GOLD), x -> {
                    stuur("adv", "");
                    onClose();
                }).bounds(rx, top + bl.advY(), rw, bl.knopH()).tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.scherm.adv.tip"))).build();
                a.active = t.getBooleanOr("kanAdv", false);
                addRenderableWidget(a);
            }
            // U-SPECS: the spec screen (first choice or switch) from the desk
            addRenderableWidget(Button.builder(Component.translatable(t.getBooleanOr("specOpen", false) ? "vadsrijk.klassen.spec.knop.kies"
                    : "vadsrijk.klassen.spec.knop").withStyle(ChatFormatting.GOLD), x -> stuur("spec_scherm", ""))
                    .bounds(rx, top + bl.specY(), rw, bl.knopH())
                    .tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.spec.knop.tip"))).build());
        } else if (specKnop) {
            // U-SPECS: the first spec choice is open (after the step-1 quest): free, from anywhere
            addRenderableWidget(Button.builder(Component.translatable("vadsrijk.klassen.spec.knop.kies").withStyle(ChatFormatting.GOLD),
                    x -> stuur("spec_scherm", "")).bounds(rx, top + H - 24, rw, 18).build());
        }
        // (the Done button sits in the title band: the panel itself is full with the tree and the right side)
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(kader.left() + kader.w() - 56, kader.top() + 2, 52, 14).build());
    }

    /** The row to show when the tab opens: the first node you can learn now, else the last one you have, else the top. */
    private int volgendeRij() {
        Klasse k = klasse();
        Set<String> heeft = knopen();
        int level = t.getIntOr("level", 1);
        int kan = -1, had = -1;
        for (BoomKnoop b : k.boom()) {
            if (b.rij() < 0) {
                continue;
            }
            if (heeft.contains(b.id())) {
                had = Math.max(had, b.rij());
            } else if (Boom.waaromNiet(k, heeft, level, b.id()) == null && (kan < 0 || b.rij() < kan)) {
                kan = b.rij();
            }
        }
        return kan >= 0 ? kan : Math.max(0, had);
    }

    private static String tijd(long sec) {
        return sec >= 60 ? (sec / 60 + 1) + " min" : sec + " s";
    }

    // ---- drawing ----------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractBackground(g, mx, my, pt);
        nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.tekenKader(g, font, kader.left(), kader.top(), kader.w(), kader.h(), title, "skillboom", mx, my);   // U-TABS
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, ROZE);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, LICHT);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fillGradient(left, top, left + W, top + 34, 0xF0C2457E, 0xF0401A34);
        g.fill(left + BoomIndeling.SCHEIDING, top + 38, left + BoomIndeling.SCHEIDING + 1, top + H - 8, 0x60FFC2E8);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        Klasse k = klasse();
        Set<String> heeft = knopen();
        int level = t.getIntOr("level", 1);
        // header
        g.pose().pushMatrix();
        g.pose().translate(left + 12, top + 8);
        g.pose().scale(1.5f, 1.5f);
        g.text(font, Component.translatable(t.getStringOr("titel", k.naamKey())).withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFFFF, true);
        g.pose().popMatrix();
        g.text(font, Component.translatable("vadsrijk.klassen.scherm.niveau2", Component.translatable(k.naamKey()), level, t.getIntOr("generiek", 1)),
                left + 12, top + 23, LICHT, false);
        int punten = t.getIntOr("punten", 0);
        Component pc = Component.translatable("vadsrijk.klassen.scherm.punten", punten);
        g.text(font, pc, left + W - 12 - font.width(pc), top + 10, punten > 0 ? GOUD : LICHT, true);
        if (punten > 0) {
            Component h = Component.translatable("vadsrijk.klassen.scherm.punten.tip");
            g.text(font, h, left + W - 12 - font.width(h), top + 22, 0xFFFFE7A0, false);
        }
        // the tree: a clipped view that scrolls (wheel, drag, the bar); the column names above it stay put
        int sc = in.klem(scroll);
        int gx = in.knoopX(0), gy = in.vy0() + in.knoopY(0) - sc;
        boolean inKijk = in.inKijk(mx, my);
        List<Component> tip = takken(g, k, gx, sc, mx, my);   // U-SPECS: column strips + headers (spec A, trunk, spec B, side)
        g.enableScissor(in.vx0(), in.vy0(), in.vx1() - 6, in.vy1());
        for (BoomKnoop b : k.boom()) {
            if (b.rij() < 0) {
                continue;
            }
            int x = gx + b.kolom() * DX, y = gy + b.rij() * DY;
            for (String v : b.vereist()) {
                for (String alt : v.split("\\|")) {
                    BoomKnoop o = Boom.knoop(k, alt);
                    if (o != null) {
                        lijn(g, gx + o.kolom() * DX + KNOOP / 2, gy + o.rij() * DY + KNOOP, x + KNOOP / 2, y, heeft.contains(alt) ? ROZE : 0xFF5A3A4E);
                    }
                }
            }
        }
        for (int r = 0; r < rijen(); r++) {
            int kost = r == 0 ? 1 : r == 3 ? 3 : 2;
            int minLv = 0;
            for (BoomKnoop b : k.boom()) {   // z2-systemen: the cost and the level gate from the row's own nodes
                if (b.rij() == r) {
                    kost = b.kosten();
                    minLv = Math.max(minLv, Boom.MIN_LEVEL.getOrDefault(b.id(), 0));
                }
            }
            g.text(font, Component.literal(String.valueOf(kost)).withStyle(ChatFormatting.BOLD), gx - 12, gy + r * DY + 9, 0x90FFC2E8, false);
            if (minLv > 0) {
                g.text(font, Component.translatable("vadsrijk.klassen.scherm.rij_lv", minLv), gx + 3 * DX + KNOOP + 7, gy + r * DY + 9,
                        level >= minLv ? 0x90FFC2E8 : 0xFFFF9A9A, false);
            }
        }
        for (BoomKnoop b : k.boom()) {
            if (b.rij() < 0) {
                continue;
            }
            int x = gx + b.kolom() * DX, y = gy + b.rij() * DY;
            if (y + KNOOP + 2 < in.vy0() || y - 4 >= in.vy1()) {
                continue;   // (scrolled out of the view)
            }
            String nee = Boom.waaromNiet(k, heeft, level, b.id());
            boolean al = heeft.contains(b.id()), kan = nee == null, uit = "uitgesloten".equals(nee);
            boolean hover = inKijk && mx >= x && mx < x + KNOOP && my >= y && my < y + KNOOP;
            boolean sel = b.id().equals(gekozen);
            int rand = al ? GOUD : sel ? 0xFFFFFFFF : kan ? (System.currentTimeMillis() / 400 % 2 == 0 ? 0xFFFFFFFF : LICHT) : 0xFF6A4A5A;
            int vul = al ? ROZE : kan ? 0xFF9A3C7A : uit ? 0xFF22121C : 0xFF3A2433;
            g.fill(x - 2, y - 2, x + KNOOP + 2, y + KNOOP + 2, rand);
            g.fill(x - 1, y - 1, x + KNOOP + 1, y + KNOOP + 1, DIEP);
            g.fill(x, y, x + KNOOP, y + KNOOP, hover ? (vul | 0x00202020) : vul);
            g.item(icoon(b.id()), x + 5, y + 5);
            if (!al && !kan) {
                g.fill(x, y, x + KNOOP, y + KNOOP, 0x90200A18);
            }
            if (uit) {
                for (int i = 2; i < KNOOP - 2; i++) {
                    g.fill(x + i, y + i, x + i + 1, y + i + 1, 0xFFFF5A5A);
                    g.fill(x + KNOOP - 1 - i, y + i, x + KNOOP - i, y + i + 1, 0xFFFF5A5A);
                }
            }
            if (al) {
                g.text(font, "✔", x + KNOOP - 6, y - 4, GOUD, true);
            }
            if (hover) {
                tip = knoopTip(b, nee, al, sel);
            }
        }
        // advancement badges: below the tree (scrolled along) or, with room, on the right side
        if (!badgesRechts) {
            g.text(font, Component.translatable("vadsrijk.klassen.scherm.adv.kop").withStyle(ChatFormatting.BOLD), in.badgeX(0), in.vy0() + in.badgeKopY() - sc + 1, LICHT, false);
            for (int s = 1; s <= BoomIndeling.BADGES; s++) {
                List<Component> bt = badge(g, k, s, in.badgeX(s - 1), in.vy0() + in.badgeY(s - 1) - sc, BoomIndeling.BADGE_W, mx, my, inKijk);
                tip = bt != null ? bt : tip;
            }
        }
        g.disableScissor();
        if (in.maxScroll() > 0) {
            int x0 = in.vx0(), x1 = in.vx1() - 6, kleur = PANEEL & 0xFFFFFF;
            if (sc > 0) {
                g.fillGradient(x0, in.vy0(), x1, in.vy0() + 8, 0xF0000000 | kleur, kleur);   // (more above)
            }
            if (sc < in.maxScroll()) {
                g.fillGradient(x0, in.vy1() - 8, x1, in.vy1(), kleur, 0xF0000000 | kleur);   // (more below)
            }
            int hoog = in.kijkH(), duim = Math.max(14, hoog * hoog / in.inhoudH());
            int dy0 = in.vy0() + (hoog - duim) * sc / in.maxScroll();
            g.fill(in.vx1() - 5, in.vy0(), in.vx1() - 2, in.vy1(), 0x40FFC2E8);
            g.fill(in.vx1() - 5, dy0, in.vx1() - 2, dy0 + duim, balkSleep || (mx >= in.vx1() - 7 && mx < in.vx1() && inKijk) ? 0xFFFFFFFF : ROZE);
        }
        // right side
        int stap = t.getIntOr("adv_" + k.id(), 0);
        int rx = in.rx(), rw = in.rw();
        if (balie()) {
            Balie bl = balieIndeling();
            g.text(font, Component.translatable("vadsrijk.klassen.scherm.balie").withStyle(ChatFormatting.BOLD), rx, top + 38, GOUD, false);
            int y = top + bl.rijY();
            for (Klasse x : Klassen.alle()) {
                boolean nu = x.id().equals(t.getStringOr("actief", k.id()));
                g.fill(rx, y, rx + rw, y + bl.rijH(), nu ? 0x70FF7AC8 : 0x40FFFFFF);
                g.text(font, Component.translatable(t.getStringOr("titel_" + x.id(), x.naamKey())), rx + 4, y + 2, nu ? 0xFFFFFFFF : LICHT, false);
                g.text(font, Component.translatable("vadsrijk.klassen.scherm.nv", t.getIntOr("lv_" + x.id(), 1)), rx + 4, y + bl.rijH() - 10, GOUD, false);
                y += bl.rijStap();
            }
            long respec = t.getLongOr("respec", 0);
            if (bl.respecTekstY() >= 0) {
                g.text(font, Component.translatable(respec > 0 ? "vadsrijk.klassen.scherm.respec.uitleg_wacht" : "vadsrijk.klassen.scherm.respec.uitleg"),
                        rx, top + bl.respecTekstY(), GRIJS, false);
            }
            if (bl.advTekstY() >= 0) {
                Component a = t.getBooleanOr("kanAdv", false) ? Component.translatable("vadsrijk.klassen.scherm.adv.klaar")
                        : stap >= nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.MAX_STAP ? Component.translatable("vadsrijk.klassen.scherm.adv.alles")
                        : Component.translatable("vadsrijk.klassen.scherm.adv.nog", nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.nodigLevel(stap));
                int yy = top + bl.advTekstY();
                for (var l : font.split(a, rw)) {
                    if (yy + 9 > top + H - 2) {
                        break;
                    }
                    g.text(font, l, rx, yy, LICHT, false);
                    yy += 9;
                }
            }
        } else {
            g.text(font, Component.translatable("vadsrijk.klassen.scherm.skills").withStyle(ChatFormatting.BOLD), rx, top + 38, GOUD, false);
            // fix round 2 (g-ui): each skill with its icon; hover = the full skill info with the tree improvements
            String[] toets = {"R", "V", "G", "Z", "C"};
            int y = top + 50, ss = skillStap();
            List<Vaardigheid> vs = k.vaardigheden();
            List<String> ids = new ArrayList<>();
            List<String> namen = new ArrayList<>();
            for (int i = 0; i < vs.size() && i < 4; i++) {
                ids.add(vs.get(i).id());
                namen.add(vs.get(i).naamKey());
            }
            ids.add(nl.juiced.guhs.feature.vadsrijk.kern.Knuffelsprong.ID);
            namen.add("vadsrijk.kern.skill.knuffelsprong");
            for (int i = 0; i < ids.size(); i++) {
                int slot = i < ids.size() - 1 ? i : 4;
                boolean hover = mx >= rx && mx < rx + rw && my >= y - 1 && my < y + ss - 2;
                if (hover) {
                    g.fill(rx - 2, y - 1, rx + rw, y + ss - 2, 0x40FFC2E8);
                }
                g.fill(rx, y + 1, rx + 12, y + 15, slot == 3 ? GOUD : slot == 4 ? 0xFF8FD4FF : ROZE);
                g.centeredText(font, toets[slot], rx + 6, y + 4, 0xFF2A1022);
                Identifier ic = nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud.skillIcoon(ids.get(i));
                if (ic != null) {
                    nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud.tekenIcoon(g, ic, rx + 14, y, 16);
                }
                g.text(font, Component.translatable(namen.get(i)), rx + 34, y + 4, 0xFFFFFFFF, false);
                if (hover) {
                    tip = SkillInfo.tip(ids.get(i), slot, font, heeft::contains);
                }
                y += ss;
            }
            int yy = y + 4, n = 0;
            for (var l : font.split(Component.translatable("vadsrijk.klassen.scherm.ordetempel"), rw)) {
                if (n++ >= hintRegels) {
                    break;
                }
                g.text(font, l, rx, yy, GRIJS, false);
                yy += 9;
            }
            if (badgesRechts) {
                int by = top + badgesRechtsY, bw = (rw - (BoomIndeling.BADGES - 1) * 4) / BoomIndeling.BADGES;
                g.text(font, Component.translatable("vadsrijk.klassen.scherm.adv.kop").withStyle(ChatFormatting.BOLD), rx, by - 10, LICHT, false);
                for (int s = 1; s <= BoomIndeling.BADGES; s++) {
                    List<Component> bt = badge(g, k, s, rx + (s - 1) * (bw + 4), by, bw, mx, my, true);
                    tip = bt != null ? bt : tip;
                }
            }
        }
        super.extractRenderState(g, mx, my, pt);
        if (tip != null) {
            g.setComponentTooltipForNextFrame(font, tip, mx, my);
        }
    }

    /** One advancement badge (step s) at (bx, by), bw wide; returns its tooltip while hovered ({@code raakbaar}: not clipped away). */
    @Nullable
    private List<Component> badge(GuiGraphicsExtractor g, Klasse k, int s, int bx, int by, int bw, int mx, int my, boolean raakbaar) {
        boolean open = t.getIntOr("adv_" + k.id(), 0) >= s;
        int bh = BoomIndeling.BADGE_H;
        g.fill(bx, by, bx + bw, by + bh, open ? 0xFFB8436F : 0xFF3A2433);
        g.outline(bx, by, bw, bh, open ? GOUD : 0xFF6A4A5A);
        g.item(new ItemStack(open ? Items.NETHER_STAR : Items.GRAY_DYE), bx + 3, by + 3);
        Component titel = Component.translatable(t.getStringOr("adv_titel_" + s, "vadsrijk.klassen.titel." + k.id() + "." + s));
        float ts = Math.min(0.75f, (bw - 24) / (float) Math.max(1, font.width(titel)));   // (a long title shrinks to fit)
        g.pose().pushMatrix();
        g.pose().translate(bx + 22, by + 4);
        g.pose().pushMatrix();
        g.pose().scale(ts, ts);
        g.text(font, titel, 0, 0, open ? 0xFFFFFFFF : GRIJS, false);
        g.pose().popMatrix();
        g.pose().scale(0.75f, 0.75f);
        g.text(font, Component.translatable("vadsrijk.klassen.scherm.adv.lv", nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.levelVan(s)), 0, 11, open ? GOUD : GRIJS, false);
        g.pose().popMatrix();
        if (!raakbaar || mx < bx || mx >= bx + bw || my < by || my >= by + bh) {
            return null;
        }
        List<Component> tip = new ArrayList<>();
        tip.add(titel.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tip.addAll(regels(t.getStringOr("adv_uitleg_" + s, "vadsrijk.klassen.adv.uitleg.geen." + s), ChatFormatting.WHITE));
        tip.add(Component.translatable(open ? "vadsrijk.klassen.scherm.adv.open" : "vadsrijk.klassen.scherm.adv.dicht", nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.levelVan(s))
                .withStyle(open ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        return tip;
    }

    /**
     * U-SPECS: a soft strip behind every column with its name on top (spec A, Stam, spec B, Tank/Steun/Heel). The spec you don't
     * have is dimmed and marked; hovering a header tells what the branch is. Returns a tooltip or null. The names sit above the
     * scrolled view (they stay while the rows scroll); the strips run down to the last row.
     */
    @Nullable
    private List<Component> takken(GuiGraphicsExtractor g, Klasse k, int gx, int sc, int mx, int my) {
        List<Spec> specs = Spec.vanKlasse(k.id());
        String mijn = t.getStringOr("spec", "");
        int kopY = top + BoomIndeling.KOP, onder = Math.min(in.vy1(), in.vy0() + in.boomOnder() - sc);
        List<Component> tip = null;
        for (int kol = 0; kol < 4; kol++) {
            Spec sp = kol == 0 && !specs.isEmpty() ? specs.get(0) : kol == 2 && specs.size() > 1 ? specs.get(1) : null;
            int kleur = sp != null ? 0xFF000000 | sp.kleur : kol == 1 ? 0xFFFFC2E8 : 0xFF8FD4FF;
            boolean dicht = sp != null && !sp.id.equals(mijn);
            int x0 = gx + kol * DX - 6, x1 = x0 + KNOOP + 12;
            g.fill(x0, kopY, x1, Math.max(in.vy0(), onder), (dicht ? 0x10000000 : 0x26000000) | (kleur & 0xFFFFFF));
            g.fill(x0, kopY, x1, kopY + 1, (kleur & 0xFFFFFF) | 0xA0000000);
            String sleutel = kol == 1 ? "stam" : "zij." + k.id();
            Component kop = sp != null ? Component.translatable(sp.naamKey()) : Component.translatable("vadsrijk.klassen.tak." + sleutel);
            g.pose().pushMatrix();
            g.pose().translate(x0 + (x1 - x0) / 2f, kopY + 3);
            g.pose().scale(0.6f, 0.6f);
            g.centeredText(font, dicht ? Component.literal("× ").append(kop) : kop, 0, 0, dicht ? GRIJS : kleur);
            g.pose().popMatrix();
            if (mx >= x0 && mx < x1 && my >= kopY && my < kopY + 10) {
                tip = new ArrayList<>();
                tip.add(kop.copy().withStyle(ChatFormatting.BOLD));
                tip.addAll(regels(sp != null ? "vadsrijk.klassen.spec." + sp.id + ".kort" : "vadsrijk.klassen.tak." + sleutel + ".uitleg", ChatFormatting.WHITE));
                if (dicht) {
                    tip.add(Component.translatable(mijn.isEmpty() ? "vadsrijk.klassen.tak.dicht_nog" : "vadsrijk.klassen.tak.dicht_ander")
                            .withStyle(ChatFormatting.RED));
                }
            }
        }
        return tip;
    }

    private static void lijn(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int kleur) {
        int midY = (y0 + y1) / 2;
        g.fill(x0 - 1, y0 + 2, x0 + 1, midY + 1, kleur);
        g.fill(Math.min(x0, x1) - 1, midY - 1, Math.max(x0, x1) + 1, midY + 1, kleur);
        g.fill(x1 - 1, midY - 1, x1 + 1, y1 - 2, kleur);
    }

    private static ItemStack icoon(String id) {
        String s = Bomen.ICOON.get(id);
        Item it = s == null ? null : BuiltInRegistries.ITEM.getValue(Identifier.parse(s));
        return new ItemStack(it == null ? Items.PINK_DYE : it);
    }

    private List<Component> knoopTip(BoomKnoop b, @Nullable String nee, boolean al, boolean sel) {
        List<Component> l = new ArrayList<>();
        l.add(Component.translatable(b.naamKey()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        l.add(Component.translatable("vadsrijk.klassen.scherm.kost", b.kosten()).withStyle(ChatFormatting.GOLD));
        l.addAll(SkillInfo.verbetert(b));   // fix round 2 (g-ui)
        if (b.bonus() != null) {
            b.bonus().alles().forEach((st, v) -> l.add(Component.literal((st.procent ? String.format(java.util.Locale.ROOT, "+%.0f%% ", v)
                    : String.format(java.util.Locale.ROOT, "+%.0f ", v))).withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable(st.key()).withStyle(ChatFormatting.GRAY))));
        }
        l.addAll(regels(b.naamKey() + ".uitleg", ChatFormatting.WHITE));
        if (al) {
            l.add(Component.translatable("vadsrijk.klassen.scherm.geleerd").withStyle(ChatFormatting.GREEN));
        } else if (nee == null) {
            l.add(Component.translatable(sel ? "vadsrijk.klassen.scherm.bevestig" : "vadsrijk.klassen.scherm.klik").withStyle(ChatFormatting.YELLOW));
        } else {
            MutableComponent w = Component.translatable("vadsrijk.klassen.boom.nee." + nee);
            if (nee.equals("level")) {   // z2-systemen
                w = Component.translatable("vadsrijk.klassen.scherm.pas_op", Boom.MIN_LEVEL.getOrDefault(b.id(), 0));
            } else if (nee.equals("vereist")) {
                List<Component> namen = new ArrayList<>();
                for (String v : b.vereist()) {
                    for (String alt : v.split("\\|")) {
                        namen.add(Component.translatable("vadsrijk.klassen.knoop." + alt));
                    }
                }
                w = Component.translatable("vadsrijk.klassen.scherm.eerst", Component.literal(String.join(" / ", namen.stream().map(Component::getString).toList())));
            } else if (nee.equals("uitgesloten")) {
                w = Component.translatable("vadsrijk.klassen.scherm.uitgesloten",
                        Component.literal(String.join(" / ", Bomen.uitgesloten(b.id()).stream().map(x -> Component.translatable("vadsrijk.klassen.knoop." + x).getString()).toList())));
            }
            l.add(w.withStyle(ChatFormatting.RED));
        }
        return l;
    }

    private List<Component> regels(String key, ChatFormatting kleur) {
        List<Component> l = new ArrayList<>();
        String tekst = Component.translatable(key).getString();
        for (var r : font.getSplitter().splitLines(tekst, 200, net.minecraft.network.chat.Style.EMPTY)) {
            l.add(Component.literal(r.getString()).withStyle(kleur));
        }
        return l;
    }

    /** The node under a point in the tree view (the scroll counted in), or null. */
    @Nullable
    private BoomKnoop knoopOp(double mx, double my) {
        if (!in.inKijk(mx, my)) {
            return null;
        }
        int sc = in.klem(scroll);
        for (BoomKnoop b : klasse().boom()) {
            if (b.rij() < 0) {
                continue;
            }
            int x = in.knoopX(b.kolom()), y = in.vy0() + in.knoopY(b.rij()) - sc;
            if (mx >= x && mx < x + KNOOP && my >= y && my < y + KNOOP) {
                return b;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && kader != null && nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.klik(font, event.x(), event.y(),
                kader.left(), kader.top(), kader.w(), font.width(title.copy().withStyle(ChatFormatting.BOLD)), "skillboom")) {   // U-TABS
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && in != null && in.inKijk(event.x(), event.y())) {
            // a press in the tree view: released without dragging = a click on the node under it, dragged = the tree moves
            balkSleep = in.maxScroll() > 0 && event.x() >= in.vx1() - 7;
            druk = !balkSleep;
            sleep = 0;
            BoomKnoop b = druk ? knoopOp(event.x(), event.y()) : null;
            drukKnoop = b == null ? null : b.id();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (druk) {
            sleep += Math.abs(dx) + Math.abs(dy);
            scroll = Math.max(0, Math.min(in.maxScroll(), scroll - dy));
            return true;
        }
        if (balkSleep) {
            scroll = Math.max(0, Math.min(in.maxScroll(), scroll + dy * in.inhoudH() / Math.max(1, in.kijkH())));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (balkSleep) {
            balkSleep = false;
            scroll = in.klem(scroll);
            return true;
        }
        if (druk) {
            druk = false;
            scroll = in.klem(scroll);
            BoomKnoop b = sleep < 4 ? knoopOp(event.x(), event.y()) : null;
            if (b != null && b.id().equals(drukKnoop)) {
                klikKnoop(b);
            }
            drukKnoop = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Click a node once to select it, again to learn it. */
    private void klikKnoop(BoomKnoop b) {
        if (Boom.waaromNiet(klasse(), knopen(), t.getIntOr("level", 1), b.id()) == null) {
            if (b.id().equals(gekozen)) {
                stuur("koop", b.id());
                gekozen = null;
            } else {
                gekozen = b.id();
            }
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2f));
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (in != null && in.maxScroll() > 0 && mx >= left && mx < left + BoomIndeling.SCHEIDING && my >= top && my < top + H) {
            scroll = in.klem(in.klem(scroll) - dy * DY);
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- U-TABS: the Vadsrijk-menu -----------------------------------------------------------------------------------------------

    @Override
    public float menuSchaal() {
        return kader == null ? 1f : kader.schaal();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (nl.juiced.guhs.feature.vadsrijk.menu.client.VadsrijkMenu.toetsInMenu(this, event, "skillboom")) {
            return true;
        }
        return super.keyPressed(event);
    }
}
