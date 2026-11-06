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
import nl.juiced.guhs.feature.vadsrijk.klassen.spec.Spec;
import org.lwjgl.glfw.GLFW;

/**
 * The skill tree screen (key K in the dimension) and, opened at the ordetempel, the class desk (switch class, free respec,
 * the advancement ceremony). Pink, readable: a 4x3 grid of nodes with their item icons, lines to what they need, a cost
 * badge, and a tooltip with what the node really does. Click a node once to select it, again to learn it.
 */
public final class BoomScherm extends Screen {
    private static final int W = 384;
    /** z2-systemen: the height and row distance depend on the rows of the tree (4 in zone 1, 6 with the zone-2 rows). */
    private int H = 240, DY = 38;
    private static final int ROZE = 0xFFFF7AC8, LICHT = 0xFFFFC2E8, DIEP = 0xFF2A1022, PANEEL = 0xF0401A34, GOUD = 0xFFFFD24A, GRIJS = 0xFF8A6A7E;
    /** U-SPECS: 4 columns (spec A, trunk, spec B, side branch), a bit closer together. */
    private static final int KNOOP = 26, DX = 44;
    private CompoundTag t;
    private int left, top;
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

    /** z2-systemen: top of the advancement badges (relative to top). */
    private int badgeY() {
        return rijen() <= Bomen.RIJEN ? H - 34 : 46 + (rijen() - 1) * DY + KNOOP + 20;
    }

    @Override
    protected void init() {
        int rijen = rijen();
        DY = rijen <= Bomen.RIJEN ? 38 : rijen <= 6 ? 32 : 29;   // (z3-systemen: 8 rows with the zone-3 rows 6/7)
        H = rijen <= Bomen.RIJEN ? 240 : 46 + (rijen - 1) * DY + KNOOP + 20 + 22 + 26 + 8;
        left = (width - W) / 2;
        top = Math.max(24, (height - H) / 2);   // (g-klassen: room for the class tabs above)
        int rx = left + 214, rw = W - 214 - 10;
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
            int y = top + 46;
            long prijs = t.getLongOr("prijs", 30);
            for (Klasse k : Klassen.alle()) {
                boolean had = t.getBooleanOr("start_" + k.id(), false);
                Button b = Button.builder(had ? Component.translatable("vadsrijk.klassen.scherm.wapen.koop", nl.juiced.guhs.feature.vadsrijk.kern.Spelers.munten(prijs))
                        : Component.translatable("vadsrijk.klassen.scherm.wapen.gratis"), x -> stuur("wapen", k.id())).bounds(rx + rw - 74, y + 4, 72, 16)
                        .tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.scherm.wapen.tip", Component.translatable(k.naamKey())))).build();
                addRenderableWidget(b);
                y += 30;
            }
            long respec = t.getLongOr("respec", 0);
            Button r = Button.builder(respec > 0 ? Component.translatable("vadsrijk.klassen.scherm.respec.wacht", tijd(respec))
                    : Component.translatable("vadsrijk.klassen.scherm.respec"), x -> stuur("respec", ""))
                    .bounds(rx, top + 144, rw, 18).tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.scherm.respec.tip"))).build();
            r.active = respec <= 0 && !knopen().stream().allMatch(id -> id.contains("_adv") || id.contains("_spec_"));
            addRenderableWidget(r);
            if (t.getBooleanOr("advBalie", true)) {
                Button a = Button.builder(Component.translatable("vadsrijk.klassen.scherm.adv").withStyle(ChatFormatting.GOLD), x -> {
                    stuur("adv", "");
                    onClose();
                }).bounds(rx, top + 166, rw, 18).tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.scherm.adv.tip"))).build();
                a.active = t.getBooleanOr("kanAdv", false);
                addRenderableWidget(a);
            }
            // U-SPECS: the spec screen (first choice or switch) from the desk
            addRenderableWidget(Button.builder(Component.translatable(t.getBooleanOr("specOpen", false) ? "vadsrijk.klassen.spec.knop.kies"
                    : "vadsrijk.klassen.spec.knop").withStyle(ChatFormatting.GOLD), x -> stuur("spec_scherm", ""))
                    .bounds(rx, top + (t.getBooleanOr("advBalie", true) ? 188 : 166), rw, 18)
                    .tooltip(Tooltip.create(Component.translatable("vadsrijk.klassen.spec.knop.tip"))).build());
        } else if (t.getBooleanOr("specOpen", false)) {
            // U-SPECS: the first spec choice is open (after the step-1 quest): free, from anywhere
            addRenderableWidget(Button.builder(Component.translatable("vadsrijk.klassen.spec.knop.kies").withStyle(ChatFormatting.GOLD),
                    x -> stuur("spec_scherm", "")).bounds(rx, top + H - 50, rw, 18).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 70, top + H - 26, 60, 18).build());
    }

    private static String tijd(long sec) {
        return sec >= 60 ? (sec / 60 + 1) + " min" : sec + " s";
    }

    // ---- drawing ----------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractBackground(g, mx, my, pt);
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, ROZE);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, LICHT);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fillGradient(left, top, left + W, top + 34, 0xF0C2457E, 0xF0401A34);
        g.fill(left + 210, top + 38, left + 211, top + H - 8, 0x60FFC2E8);
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
        // the tree
        int gx = left + 20, gy = top + 46;
        List<Component> tip = takken(g, k, gx, gy, mx, my);   // U-SPECS: column strips + headers (spec A, trunk, spec B, side)
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
            String nee = Boom.waaromNiet(k, heeft, level, b.id());
            boolean al = heeft.contains(b.id()), kan = nee == null, uit = "uitgesloten".equals(nee);
            boolean hover = mx >= x && mx < x + KNOOP && my >= y && my < y + KNOOP;
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
        // advancement badges
        int stap = t.getIntOr("adv_" + k.id(), 0);
        int by = top + badgeY();
        g.text(font, Component.translatable("vadsrijk.klassen.scherm.adv.kop").withStyle(ChatFormatting.BOLD), gx - 8, by - 12, LICHT, false);
        for (int s = 1; s <= nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.MAX_STAP; s++) {
            int bx = gx - 8 + ((s - 1) % 2) * 94;   // z2-systemen: step 3 on a second line
            if (s > 2) {
                by = top + badgeY() + 26;
            }
            boolean open = stap >= s;
            boolean hover = mx >= bx && mx < bx + 90 && my >= by && my < by + 22;
            g.fill(bx, by, bx + 90, by + 22, open ? 0xFFB8436F : 0xFF3A2433);
            g.outline(bx, by, 90, 22, open ? GOUD : 0xFF6A4A5A);
            g.item(new ItemStack(open ? Items.NETHER_STAR : Items.GRAY_DYE), bx + 3, by + 3);
            g.pose().pushMatrix();
            g.pose().translate(bx + 22, by + 4);
            g.pose().scale(0.75f, 0.75f);
            g.text(font, Component.translatable(t.getStringOr("adv_titel_" + s, "vadsrijk.klassen.titel." + k.id() + "." + s)), 0, 0, open ? 0xFFFFFFFF : GRIJS, false);
            g.text(font, Component.translatable("vadsrijk.klassen.scherm.adv.lv", nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.levelVan(s)), 0, 11, open ? GOUD : GRIJS, false);
            g.pose().popMatrix();
            if (hover) {
                tip = new ArrayList<>();
                tip.add(Component.translatable(t.getStringOr("adv_titel_" + s, "vadsrijk.klassen.titel." + k.id() + "." + s)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                tip.addAll(regels(t.getStringOr("adv_uitleg_" + s, "vadsrijk.klassen.adv.uitleg.geen." + s), ChatFormatting.WHITE));
                tip.add(Component.translatable(open ? "vadsrijk.klassen.scherm.adv.open" : "vadsrijk.klassen.scherm.adv.dicht", nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.levelVan(s))
                        .withStyle(open ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
        }
        // right side
        int rx = left + 214, rw = W - 214 - 10;
        if (balie()) {
            g.text(font, Component.translatable("vadsrijk.klassen.scherm.balie").withStyle(ChatFormatting.BOLD), rx, top + 38, GOUD, false);
            int y = top + 46;
            for (Klasse x : Klassen.alle()) {
                boolean nu = x.id().equals(t.getStringOr("actief", k.id()));
                g.fill(rx, y, rx + rw, y + 26, nu ? 0x70FF7AC8 : 0x40FFFFFF);
                g.text(font, Component.translatable(t.getStringOr("titel_" + x.id(), x.naamKey())), rx + 4, y + 3, nu ? 0xFFFFFFFF : LICHT, false);
                g.text(font, Component.translatable("vadsrijk.klassen.scherm.nv", t.getIntOr("lv_" + x.id(), 1)), rx + 4, y + 14, GOUD, false);
                y += 30;
            }
            long respec = t.getLongOr("respec", 0);
            g.text(font, Component.translatable(respec > 0 ? "vadsrijk.klassen.scherm.respec.uitleg_wacht" : "vadsrijk.klassen.scherm.respec.uitleg"),
                    rx, top + 135, GRIJS, false);
            if (t.getBooleanOr("advBalie", true)) {
                Component a = t.getBooleanOr("kanAdv", false) ? Component.translatable("vadsrijk.klassen.scherm.adv.klaar")
                        : stap >= nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.MAX_STAP ? Component.translatable("vadsrijk.klassen.scherm.adv.alles")
                        : Component.translatable("vadsrijk.klassen.scherm.adv.nog", nl.juiced.guhs.feature.vadsrijk.klassen.Advancement.nodigLevel(stap));
                int yy = top + 188;
                for (var l : font.split(a, rw)) {
                    g.text(font, l, rx, yy, LICHT, false);
                    yy += 9;
                }
            }
        } else {
            g.text(font, Component.translatable("vadsrijk.klassen.scherm.skills").withStyle(ChatFormatting.BOLD), rx, top + 38, GOUD, false);
            // fix round 2 (g-ui): each skill with its icon; hover = the full skill info with the tree improvements
            String[] toets = {"R", "V", "G", "Z", "C"};
            int y = top + 50;
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
                boolean hover = mx >= rx && mx < rx + rw && my >= y - 1 && my < y + 17;
                if (hover) {
                    g.fill(rx - 2, y - 2, rx + rw, y + 18, 0x40FFC2E8);
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
                y += 19;
            }
            int yy = y + 6;
            for (var l : font.split(Component.translatable("vadsrijk.klassen.scherm.ordetempel"), rw)) {
                g.text(font, l, rx, yy, GRIJS, false);
                yy += 9;
            }
        }
        super.extractRenderState(g, mx, my, pt);
        if (tip != null) {
            g.setComponentTooltipForNextFrame(font, tip, mx, my);
        }
    }

    /**
     * U-SPECS: a soft strip behind every column with its name on top (spec A, Stam, spec B, Tank/Steun/Heel). The spec you don't
     * have is dimmed and marked; hovering a header tells what the branch is. Returns a tooltip or null.
     */
    @Nullable
    private List<Component> takken(GuiGraphicsExtractor g, Klasse k, int gx, int gy, int mx, int my) {
        List<Spec> specs = Spec.vanKlasse(k.id());
        String mijn = t.getStringOr("spec", "");
        int onder = gy + (rijen() - 1) * DY + KNOOP + 3;
        List<Component> tip = null;
        for (int kol = 0; kol < 4; kol++) {
            Spec sp = kol == 0 && !specs.isEmpty() ? specs.get(0) : kol == 2 && specs.size() > 1 ? specs.get(1) : null;
            int kleur = sp != null ? 0xFF000000 | sp.kleur : kol == 1 ? 0xFFFFC2E8 : 0xFF8FD4FF;
            boolean dicht = sp != null && !sp.id.equals(mijn);
            int x0 = gx + kol * DX - 6, x1 = x0 + KNOOP + 12;
            g.fill(x0, gy - 12, x1, onder, (dicht ? 0x10000000 : 0x26000000) | (kleur & 0xFFFFFF));
            g.fill(x0, gy - 12, x1, gy - 11, (kleur & 0xFFFFFF) | 0xA0000000);
            String sleutel = kol == 1 ? "stam" : "zij." + k.id();
            Component kop = sp != null ? Component.translatable(sp.naamKey()) : Component.translatable("vadsrijk.klassen.tak." + sleutel);
            g.pose().pushMatrix();
            g.pose().translate(x0 + (x1 - x0) / 2f, gy - 9);
            g.pose().scale(0.6f, 0.6f);
            g.centeredText(font, dicht ? Component.literal("× ").append(kop) : kop, 0, 0, dicht ? GRIJS : kleur);
            g.pose().popMatrix();
            if (mx >= x0 && mx < x1 && my >= gy - 12 && my < gy - 2) {
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            double mx = event.x(), my = event.y();
            Klasse k = klasse();
            int gx = left + 20, gy = top + 46;
            for (BoomKnoop b : k.boom()) {
                if (b.rij() < 0) {
                    continue;
                }
                int x = gx + b.kolom() * DX, y = gy + b.rij() * DY;
                if (mx >= x && mx < x + KNOOP && my >= y && my < y + KNOOP) {
                    if (Boom.waaromNiet(k, knopen(), t.getIntOr("level", 1), b.id()) == null) {
                        if (b.id().equals(gekozen)) {
                            stuur("koop", b.id());
                            gekozen = null;
                        } else {
                            gekozen = b.id();
                        }
                        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2f));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
