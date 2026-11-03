package nl.juiced.guhs.feature.erfenis.kaart.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.erfenis.client.MinimapStil;
import nl.juiced.guhs.feature.erfenis.kaart.BestelGoed;
import nl.juiced.guhs.feature.erfenis.kaart.Bestelling;
import nl.juiced.guhs.feature.erfenis.kaart.KaartPayloads;

/**
 * De Erfenis: the ORDERS screen (Hay Day style board). One card per cart: the gold star order first, then the Marktplein's
 * carts. An open order shows the village's funny wish, the goods (have / need, green when enough), the reward (and the help
 * bonus), the deco chance or the star's gift, and Leveren / Weigeren. A cart on its way shows a little progress bar.
 * Opened from the map (with "Terug naar de kaart") or by the server ({@code Bestellingen.open}, e.g. kern's Marktplein).
 */
public final class BestelScherm extends Screen {
    private static final int INKT = 0xFF5A3448, ZACHT = 0xFFB0708A, CREME = 0xFFFFF6FA, ROZE = 0xFFF7B6CB, ROZE_DONKER = 0xFFD27A9C,
            GROEN = 0xFF3F9A62, ROOD = 0xFFD0506A, GOUD = 0xFFF2C14E;
    private static final int KOLOMMEN = 4, KOP = 44;

    @Nullable
    private CompoundTag data;
    private long klokVerschil;
    @Nullable
    private final Runnable terug;
    private int ververs;

    public BestelScherm(@Nullable Runnable terug) {
        super(Component.translatable("erfenis.kaart.bestel.titel"));
        this.terug = terug;
    }

    /** Opens the screen at once ("laden...") and asks the server for the orders. */
    public static void open(@Nullable Runnable terug) {
        Minecraft.getInstance().setScreen(new BestelScherm(terug));
        ClientPacketDistributor.sendToServer(new KaartPayloads.BestelActie(KaartPayloads.BestelActie.OPEN, 0));
    }

    /** The server's data (KaartClient routes it here, or opens a fresh screen when {@code open}). */
    static void ontvang(KaartPayloads.BestelLijst l) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BestelScherm s) {
            s.update(l.data());
        } else if (l.open()) {
            BestelScherm s = new BestelScherm(null);
            mc.setScreen(s);
            s.update(l.data());
        }
    }

    void update(CompoundTag t) {
        data = t;
        klokVerschil = t.getLongOr("Nu", System.currentTimeMillis()) - System.currentTimeMillis();
        rebuildWidgets();
    }

    private List<CompoundTag> kaarten() {
        List<CompoundTag> l = new ArrayList<>();
        if (data != null) {
            for (Tag e : data.getListOrEmpty("Karren")) {
                if (e instanceof CompoundTag c) {
                    l.add(c);
                }
            }
        }
        return l;
    }

    private int[] vak(int i) {
        int n = Math.max(1, kaarten().size());
        int rijen = (n + KOLOMMEN - 1) / KOLOMMEN;
        int w = (width - 16 - (KOLOMMEN - 1) * 6) / KOLOMMEN;
        int h = Math.min(170, (height - KOP - 8 - (rijen - 1) * 6) / Math.max(1, rijen));
        int x = 8 + i % KOLOMMEN * (w + 6), y = KOP + i / KOLOMMEN * (h + 6);
        return new int[] {x, y, x + w, y + h};
    }

    @Override
    protected void init() {
        MinimapStil.uit();
        int bw = 86;
        addRenderableWidget(Button.builder(Component.translatable("erfenis.kaart.bestel.sluiten"), b -> onClose())
                .bounds(width - 8 - bw, 8, bw, 18).build());
        if (terug != null) {
            addRenderableWidget(Button.builder(Component.translatable("erfenis.kaart.bestel.naar_kaart"), b -> {
                onClose();
                terug.run();
            }).bounds(width - 8 - 2 * bw - 4, 8, bw, 18).build());
        }
        List<CompoundTag> l = kaarten();
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.get(i);
            if (c.getIntOr("Fase", 0) != Bestelling.OPEN) {
                continue;
            }
            int[] v = vak(i);
            int slot = c.getIntOr("Slot", 0);
            boolean kan = genoeg(c);
            int by = v[3] - 22, half = (v[2] - v[0] - 12) * 3 / 5;
            Button lever = Button.builder(Component.translatable("erfenis.kaart.bestel.leveren"),
                    b -> ClientPacketDistributor.sendToServer(new KaartPayloads.BestelActie(KaartPayloads.BestelActie.LEVER, slot)))
                    .bounds(v[0] + 4, by, half, 18).tooltip(Tooltip.create(Component.translatable(kan
                            ? "erfenis.kaart.bestel.leveren.uitleg" : "erfenis.kaart.bestel.leveren.tekort"))).build();
            lever.active = kan;
            addRenderableWidget(lever);
            addRenderableWidget(Button.builder(Component.translatable("erfenis.kaart.bestel.weigeren"),
                    b -> ClientPacketDistributor.sendToServer(new KaartPayloads.BestelActie(KaartPayloads.BestelActie.WEIGER, slot)))
                    .bounds(v[0] + 8 + half, by, v[2] - v[0] - 12 - half, 18)
                    .tooltip(Tooltip.create(Component.translatable("erfenis.kaart.bestel.weigeren.uitleg"))).build());
        }
    }

    private static boolean genoeg(CompoundTag c) {
        CompoundTag heeft = c.getCompoundOrEmpty("Heeft");
        for (Tag e : c.getListOrEmpty("Regels")) {
            if (e instanceof CompoundTag r && heeft.getIntOr(r.getStringOr("G", ""), 0) < r.getIntOr("N", 1)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void tick() {
        if (++ververs % 100 == 0) {
            ClientPacketDistributor.sendToServer(new KaartPayloads.BestelActie(KaartPayloads.BestelActie.VERVERS, 0));
        }
    }

    @Override
    public void removed() {
        MinimapStil.terug();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // =====================================================================================================================
    // drawing
    // =====================================================================================================================

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, height, 0xFFFDEFF4);
        for (int y = 0; y < height; y += 16) {   // soft gingham
            for (int x = (y / 16) % 2 * 16; x < width; x += 32) {
                g.fill(x, y, x + 16, y + 16, 0xFFFBE4EC);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, 34, CREME);
        g.fill(0, 34, width, 36, ROZE_DONKER);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), 12, 7, INKT, false);
        if (data == null) {
            g.text(font, Component.translatable("erfenis.kaart.bestel.laden"), 12, 20, ZACHT, false);
        } else {
            int markt = data.getIntOr("Marktplein", 0), n = 0;
            for (CompoundTag c : kaarten()) {
                n += c.getIntOr("Slot", 0) >= 0 ? 1 : 0;
            }
            Component sub = markt == 0 ? Component.translatable("erfenis.kaart.bestel.sub_geen", n, data.getIntOr("Geleverd", 0))
                    : Component.translatable("erfenis.kaart.bestel.sub", markt, n, data.getIntOr("Geleverd", 0));
            g.text(font, sub, 12, 20, ZACHT, false);
            long nu = System.currentTimeMillis() + klokVerschil;
            List<CompoundTag> l = kaarten();
            if (l.isEmpty()) {
                g.centeredText(font, Component.translatable("erfenis.kaart.bestel.leeg"), width / 2, height / 2, INKT);
            }
            for (int i = 0; i < l.size(); i++) {
                kaart(g, l.get(i), vak(i), nu, mouseX, mouseY);
            }
        }
        super.extractRenderState(g, mouseX, mouseY, a);
    }

    private void kaart(GuiGraphicsExtractor g, CompoundTag c, int[] v, long nu, int mx, int my) {
        int x0 = v[0], y0 = v[1], x1 = v[2], y1 = v[3];
        boolean ster = c.getBooleanOr("Ster", false);
        int fase = c.getIntOr("Fase", 0), kleur = c.getIntOr("Kleur", 0xFFF29BB8);
        // the card
        g.fill(x0 + 1, y0 + 2, x1 + 1, y1 + 2, 0x30501830);
        g.fill(x0, y0, x1, y1, ster ? 0xFFC0902A : ROZE_DONKER);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, ster ? 0xFFFFF8E0 : CREME);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 15, ster ? GOUD : kleur | 0xFF000000);
        g.fill(x0 + 1, y0 + 15, x1 - 1, y0 + 16, 0x40000000);
        String naam = c.getStringOr("DorpNaam", "?");
        Component tag = ster ? Component.translatable("erfenis.kaart.bestel.ster")
                : Component.translatable("erfenis.kaart.bestel.relatie." + (c.getIntOr("Relatie", 0) + 1));
        int tw = font.width(tag);
        String kop = naam;   // (bold text is wider: shorten until the bold name fits next to the tag)
        while (kop.length() > 1 && font.width(Component.literal(kop).withStyle(ChatFormatting.BOLD)) > x1 - x0 - tw - 16) {
            kop = naam.substring(0, kop.endsWith("..") ? kop.length() - 3 : kop.length() - 1) + "..";
        }
        g.text(font, Component.literal(kop).withStyle(ChatFormatting.BOLD), x0 + 5, y0 + 4, 0xFFFFFFFF, true);
        g.text(font, tag, x1 - 5 - tw, y0 + 4, 0xFFFFFFFF, true);
        int y = y0 + 20, breed = x1 - x0 - 10;
        if (fase == Bestelling.WACHT) {
            g.text(font, Component.translatable("erfenis.kaart.bestel.rust"), x0 + 5, y, ZACHT, false);
            g.text(font, Component.translatable("erfenis.kaart.bestel.nieuw_over", tijd(c.getLongOr("Eind", 0L) - nu)), x0 + 5, y + 11, INKT, false);
            KarIcoon.teken(g, (x0 + x1) / 2, (y0 + y1) / 2 + 10, 3, ster, false, false, nu);
            return;
        }
        if (fase == Bestelling.TERUG) {
            boolean leeg = c.getBooleanOr("Geweigerd", false);
            for (FormattedCharSequence s : font.split(Component.translatable(leeg ? "erfenis.kaart.bestel.terug_leeg" : "erfenis.kaart.bestel.terug_vol",
                    naam), breed)) {
                g.text(font, s, x0 + 5, y, ZACHT, false);
                y += 10;
            }
            balk(g, x0 + 5, y + 2, x1 - 5, c, nu);
            g.text(font, tijd(c.getLongOr("Eind", 0L) - nu), x0 + 5, y + 10, INKT, false);
            KarIcoon.teken(g, (x0 + x1) / 2, (y0 + y1) / 2 + 16, 3, ster, true, true, nu);
            return;
        }
        // the wish
        MutableComponent wens = wens(c);
        List<FormattedCharSequence> regels = font.split(wens, breed);
        for (int i = 0; i < Math.min(4, regels.size()); i++) {
            g.text(font, regels.get(i), x0 + 5, y, INKT, false);
            y += 10;
        }
        y += 3;
        // the goods
        CompoundTag heeft = c.getCompoundOrEmpty("Heeft");
        for (Tag e : c.getListOrEmpty("Regels")) {
            if (!(e instanceof CompoundTag r)) {
                continue;
            }
            String id = r.getStringOr("G", "");
            int n = r.getIntOr("N", 1), h = heeft.getIntOr(id, 0);
            BestelGoed bg = BestelGoed.van(id);
            if (bg != null) {
                g.item(bg.icoon(), x0 + 5, y);
            }
            String telling = Math.min(h, 99999) + "/" + n;
            boolean ok = h >= n;
            g.text(font, telling, x0 + 24, y + 4, fase == Bestelling.OPEN ? (ok ? GROEN : ROOD) : ZACHT, false);
            Component gn = bg == null ? Component.literal(id) : Component.translatable(bg.naamKey());
            g.text(font, font.plainSubstrByWidth(gn.getString(), x1 - x0 - 34 - font.width(telling)), x0 + 28 + font.width(telling), y + 4,
                    ZACHT, false);
            if (ok && fase == Bestelling.OPEN) {
                g.text(font, "✔", x1 - 12, y + 4, GROEN, false);
            }
            y += 17;
        }
        // reward
        y += 1;
        int uit = c.getIntOr("Uitbetaling", 0);
        String hulp = c.getStringOr("Hulp", "");
        g.text(font, Component.translatable("erfenis.kaart.bestel.beloning", uit).withStyle(ChatFormatting.BOLD), x0 + 5, y, 0xFFB07A10, false);
        if (!hulp.isEmpty()) {
            Component hb = Component.translatable("erfenis.kaart.bestel.hulp_bonus");
            g.text(font, hb, x1 - 5 - font.width(hb), y, GROEN, false);
            if (mx >= x1 - 5 - font.width(hb) && mx < x1 - 5 && my >= y && my < y + 9) {
                g.setTooltipForNextFrame(font, List.of(Component.translatable("erfenis.kaart.bestel.hulp_van", hulp)), java.util.Optional.empty(), mx, my);
            }
        }
        y += 11;
        Component deco = ster && !c.getStringOr("Deco", "").isEmpty()
                ? Component.translatable("erfenis.kaart.bestel.cadeau_zeker", Component.translatable("erfenis.kaart.deco." + c.getStringOr("Deco", "")))
                : Component.translatable("erfenis.kaart.bestel.cadeau_kans", c.getIntOr("DecoKans", 0));
        g.text(font, font.plainSubstrByWidth(deco.getString(), breed), x0 + 5, y, ster ? 0xFFB07A10 : ZACHT, false);
        if (fase == Bestelling.HEEN) {
            int by = y1 - 20;
            g.text(font, Component.translatable("erfenis.kaart.bestel.onderweg", tijd(c.getLongOr("Eind", 0L) - nu)), x0 + 5, by - 1, INKT, false);
            balk(g, x0 + 5, by + 10, x1 - 5, c, nu);
        }
    }

    /** "Knabbeldorp wil 12 eieren voor de vadse paasbrunch." from the first goods line and the order's reason. */
    private static MutableComponent wens(CompoundTag c) {
        String naam = c.getStringOr("DorpNaam", "?");
        boolean ster = c.getBooleanOr("Ster", false);
        int reden = c.getIntOr("Reden", 0);
        Component wat = Component.literal("...");
        for (Tag e : c.getListOrEmpty("Regels")) {
            if (e instanceof CompoundTag r) {
                BestelGoed bg = BestelGoed.van(r.getStringOr("G", ""));
                wat = Component.translatable("erfenis.kaart.bestel.aantal", r.getIntOr("N", 1),
                        bg == null ? Component.literal(r.getStringOr("G", "")) : Component.translatable(bg.naamKey()));
                break;
            }
        }
        return Component.translatable(ster ? "erfenis.kaart.bestel.wens_ster" : "erfenis.kaart.bestel.wens", naam, wat,
                Component.translatable((ster ? "erfenis.kaart.bestel.ster_reden." : "erfenis.kaart.bestel.reden.") + reden));
    }

    private void balk(GuiGraphicsExtractor g, int x0, int y, int x1, CompoundTag c, long nu) {
        long s = c.getLongOr("Start", 0L), e = c.getLongOr("Eind", 0L);
        float deel = e <= s ? 1 : Math.max(0, Math.min(1, (nu - s) / (float) (e - s)));
        g.fill(x0, y, x1, y + 5, 0xFFEAD3DD);
        g.fill(x0 + 1, y + 1, x0 + 1 + Math.round((x1 - x0 - 2) * deel), y + 4, ROZE_DONKER);
    }

    private static String tijd(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format("%d:%02d", s / 60, s % 60);
    }
}
