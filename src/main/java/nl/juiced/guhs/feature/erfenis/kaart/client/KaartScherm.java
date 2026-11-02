package nl.juiced.guhs.feature.erfenis.kaart.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.erfenis.client.MinimapStil;
import nl.juiced.guhs.feature.erfenis.kaart.KaartDorp;
import nl.juiced.guhs.feature.erfenis.kaart.KaartNieuws;
import nl.juiced.guhs.feature.erfenis.kaart.KaartPayloads;

/**
 * De Wereldkaart (Grepolis / Tribal Wars style): a pastel sea with land islets, every base a little icon that grows with
 * its stadhuis level (hutje, huisje, dorpje, torendorp, kasteel). Drag to pan, scroll to zoom (3 levels; names appear when
 * zoomed in), hover for a tooltip, click for a panel (Bezoeken / Bovenaanzicht / Aanvallen-komt-later). On the right the
 * Ranglijst (by Knusheid) and the Nieuws of the NPC villages. Refreshes itself every 5 seconds while open.
 */
public final class KaartScherm extends Screen {
    private static final float[] ZOOMS = {7f, 14f, 26f};
    private static final int SIDE = 172, PANEL_W = 252, PANEL_H = 96, LIJST_Y = 30, RIJ = 12;
    /** Marker colours: your own village, other real players, NPC villages. */
    static final int EIGEN = 0xFFFFD54A, SPELER = 0xFF5FAEF0, NPC = 0xFFF29BB8;
    private static final int INKT = 0xFF4A3A55, GRIJS = 0xFF8A7A90, PAPIER = 0xF2FFF6EC, ROZE = 0xFFF2A9C4;

    @Nullable
    private KaartPayloads.Kaart kaart;
    @Nullable
    private KaartLand land;
    private long klokVerschil;
    private List<KaartDorp> ranglijst = List.of();

    private float camX, camZ, zoom = ZOOMS[1];
    private int zoomIdx = 1;
    private boolean eersteKeer = true;
    /** Flying to a village (after a click in the list or "Naar mijn dorp"). */
    private boolean vliegen;
    private float doelX, doelZ;
    /** Zooming around a point: that world point stays under that screen point. */
    private boolean anker;
    private float ankerSx, ankerSy, ankerWx, ankerWz;
    private long laatsteFrame;

    @Nullable
    private UUID gekozen;
    @Nullable
    private KaartDorp hover;
    private int tab;
    private final float[] scroll = new float[2];
    private boolean drukOpKaart;
    private double sleepAfstand;
    private int ververs;

    /** Where the "Terug" button goes (the overview this map was opened from), or null: no such button. */
    @Nullable
    private final Runnable terug;

    public KaartScherm(@Nullable Runnable terug) {
        super(Component.translatable("erfenis.kaart.titel"));
        this.terug = terug;
    }

    // =====================================================================================================================
    // data
    // =====================================================================================================================

    void update(KaartPayloads.Kaart k) {
        kaart = k;
        klokVerschil = k.serverNu() - System.currentTimeMillis();
        List<KaartDorp> r = new ArrayList<>(k.dorpen());
        r.sort(Comparator.comparingInt(KaartDorp::punten).reversed().thenComparing(KaartDorp::naam));
        ranglijst = r;
        long sleutel = KaartLand.sleutel(k.seed(), k.dorpen());
        if (land == null || land.sleutel != sleutel) {
            if (land != null) {
                land.sluit();
            }
            land = KaartLand.maak(k.seed(), k.dorpen());
        }
        if (eersteKeer) {
            eersteKeer = false;
            KaartDorp eigen = eigen();
            camX = eigen != null ? eigen.cx() + 0.5f : 0.5f;
            camZ = eigen != null ? eigen.cz() + 0.5f : 0.5f;
        }
        if (gekozen != null && dorp(gekozen) == null) {
            gekozen = null;
        }
        rebuildWidgets();
    }

    @Nullable
    private KaartDorp dorp(@Nullable UUID id) {
        if (kaart == null || id == null) {
            return null;
        }
        for (KaartDorp d : kaart.dorpen()) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }

    @Nullable
    private KaartDorp eigen() {
        return kaart == null ? null : dorp(kaart.eigen());
    }

    private boolean isEigen(KaartDorp d) {
        return kaart != null && d.id().equals(kaart.eigen());
    }

    private int soortKleur(KaartDorp d) {
        return isEigen(d) ? EIGEN : d.npc() ? NPC : SPELER;
    }

    private Component naam(KaartDorp d) {
        if (isEigen(d)) {
            return Component.translatable("erfenis.kaart.jouw_erfenis");
        }
        return d.npc() ? Component.literal(d.naam()) : Component.translatable("erfenis.kaart.erfenis_van", d.eigenaar());
    }

    private static Component grootte(int level) {
        return Component.translatable("erfenis.kaart.grootte." + Math.max(1, Math.min(5, level)));
    }

    private int rang(KaartDorp d) {
        return ranglijst.indexOf(d) + 1;
    }

    // =====================================================================================================================
    // widgets
    // =====================================================================================================================

    private int kaartW() {
        return width - SIDE;
    }

    @Override
    protected void init() {
        MinimapStil.uit();   // (the JourneyMap minimap draws behind every screen, see MinimapStil)
        int mw = kaartW();
        Button rang = Button.builder(Component.translatable("erfenis.kaart.ranglijst"), b -> {
            tab = 0;
            rebuildWidgets();
        }).bounds(mw + 8, 7, 76, 18).build();
        Button nieuws = Button.builder(Component.translatable("erfenis.kaart.nieuws"), b -> {
            tab = 1;
            rebuildWidgets();
        }).bounds(mw + 88, 7, 76, 18).build();
        rang.active = tab != 0;
        nieuws.active = tab != 1;
        addRenderableWidget(rang);
        addRenderableWidget(nieuws);
        Button thuis = Button.builder(Component.translatable("erfenis.kaart.naar_mijn_dorp"), b -> {
            KaartDorp e = eigen();
            if (e != null) {
                vlieg(e);
                kies(e);
            }
        }).bounds(mw + 8, height - 25, SIDE - 16, 18).build();
        thuis.active = eigen() != null;
        addRenderableWidget(thuis);
        if (terug != null) {
            addRenderableWidget(Button.builder(Component.translatable("erfenis.kaart.terug"), b -> {
                onClose();
                terug.run();
            }).bounds(mw + 8, height - 47, SIDE - 16, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("+"), b -> zoomRond(1, mw / 2f, height / 2f)).bounds(mw - 26, 8, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("erfenis.kaart.zoom_in"))).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> zoomRond(-1, mw / 2f, height / 2f)).bounds(mw - 26, 28, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("erfenis.kaart.zoom_uit"))).build());

        KaartDorp d = dorp(gekozen);
        if (d != null) {
            int px = 8, py = height - PANEL_H - 8, by = py + PANEL_H - 24, bw = 78;
            boolean eigen = isEigen(d);
            addRenderableWidget(Button.builder(Component.translatable(eigen ? "erfenis.kaart.naar_huis" : "erfenis.kaart.bezoeken"),
                    b -> actie(KaartPayloads.Actie.BEZOEK, d)).bounds(px + 6, by, bw, 18)
                    .tooltip(Tooltip.create(Component.translatable("erfenis.kaart.bezoeken.uitleg"))).build());
            addRenderableWidget(Button.builder(Component.translatable("erfenis.kaart.bovenaanzicht"),
                    b -> actie(KaartPayloads.Actie.BOVENAANZICHT, d)).bounds(px + 6 + bw + 3, by, bw, 18)
                    .tooltip(Tooltip.create(Component.translatable("erfenis.kaart.bovenaanzicht.uitleg"))).build());
            Button aanval = Button.builder(Component.translatable("erfenis.kaart.aanvallen"), b -> {
            }).bounds(px + 6 + 2 * (bw + 3), by, bw, 18).tooltip(Tooltip.create(Component.translatable("erfenis.kaart.aanvallen.later"))).build();
            aanval.active = false;
            addRenderableWidget(aanval);
            addRenderableWidget(Button.builder(Component.literal("x"), b -> kies(null)).bounds(px + PANEL_W - 15, py + 3, 12, 12).build());
        }
    }

    private void actie(int soort, KaartDorp d) {
        ClientPacketDistributor.sendToServer(new KaartPayloads.Actie(soort, d.id()));
        onClose();
    }

    private void kies(@Nullable KaartDorp d) {
        UUID nieuw = d == null ? null : d.id();
        if (nieuw == null ? gekozen != null : !nieuw.equals(gekozen)) {
            gekozen = nieuw;
            rebuildWidgets();
        }
    }

    private void vlieg(KaartDorp d) {
        vliegen = true;
        anker = false;
        doelX = d.cx() + 0.5f;
        doelZ = d.cz() + 0.5f;
    }

    private void zoomRond(int stap, float sx, float sy) {
        int idx = Math.max(0, Math.min(ZOOMS.length - 1, zoomIdx + stap));
        if (idx == zoomIdx) {
            return;
        }
        float mx = kaartW() / 2f, my = height / 2f;
        ankerSx = sx;
        ankerSy = sy;
        ankerWx = camX + (sx - mx) / zoom;
        ankerWz = camZ + (sy - my) / zoom;
        anker = true;
        vliegen = false;
        zoomIdx = idx;
    }

    @Override
    public void tick() {
        if (++ververs % 100 == 0) {
            ClientPacketDistributor.sendToServer(new KaartPayloads.Vraag(false));
        }
    }

    @Override
    public void removed() {
        MinimapStil.terug();
        if (land != null) {
            land.sluit();
            land = null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // =====================================================================================================================
    // drawing: the map (background stratum)
    // =====================================================================================================================

    private void beweeg() {
        long nu = System.currentTimeMillis();
        float dt = laatsteFrame == 0 ? 0 : Math.min(0.1f, (nu - laatsteFrame) / 1000f);
        laatsteFrame = nu;
        float k = 1 - (float) Math.exp(-dt * 12);
        float doel = ZOOMS[zoomIdx];
        zoom += (doel - zoom) * k;
        if (Math.abs(doel - zoom) < 0.02f) {
            zoom = doel;
        }
        float mx = kaartW() / 2f, my = height / 2f;
        if (anker) {
            camX = ankerWx - (ankerSx - mx) / zoom;
            camZ = ankerWz - (ankerSy - my) / zoom;
            if (zoom == doel) {
                anker = false;
            }
        } else if (vliegen) {
            camX += (doelX - camX) * k;
            camZ += (doelZ - camZ) * k;
            if (Math.abs(doelX - camX) + Math.abs(doelZ - camZ) < 0.01f) {
                vliegen = false;
            }
        }
        if (land != null) {
            camX = Math.max(land.minCx, Math.min(land.minCx + land.breedte, camX));
            camZ = Math.max(land.minCz, Math.min(land.minCz + land.hoogte, camZ));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int mw = kaartW();
        beweeg();
        g.fill(0, 0, mw, height, KaartLand.DIEP);
        if (kaart == null || land == null) {
            g.centeredText(font, Component.translatable("erfenis.kaart.laden"), mw / 2, height / 2 - 4, 0xFFFFFFFF);
            return;
        }
        float mx = mw / 2f, my = height / 2f;
        g.enableScissor(0, 0, mw, height);
        float schaal = zoom / land.px;
        g.pose().pushMatrix();
        g.pose().translate(mx + (land.minCx - camX) * zoom, my + (land.minCz - camZ) * zoom);
        g.pose().scale(schaal, schaal);
        g.blit(RenderPipelines.GUI_TEXTURED, KaartLand.ID, 0, 0, 0f, 0f, land.texW(), land.texH(), land.texW(), land.texH());
        g.pose().popMatrix();

        // hover: the nearest village under the mouse (not through the panels)
        hover = null;
        boolean opKaart = mouseX < mw && !overPaneel(mouseX, mouseY) && !(mouseX < 8 + titelBreedte() && mouseY < 44);
        float beste = Float.MAX_VALUE;
        long tijd = System.currentTimeMillis();
        for (KaartDorp d : kaart.dorpen()) {
            float sx = mx + (d.cx() + 0.5f - camX) * zoom, sy = my + (d.cz() + 0.5f - camZ) * zoom;
            if (sx < -40 || sy < -40 || sx > mw + 40 || sy > height + 40) {
                continue;
            }
            float r = Math.max(5, Math.min(zoom * 0.5f, 14));
            float dx = mouseX - sx, dy = mouseY - sy, dd = dx * dx + dy * dy;
            if (opKaart && Math.abs(dx) <= r && Math.abs(dy) <= r && dd < beste) {
                beste = dd;
                hover = d;
            }
        }
        // draw back to front (north first), the chosen and own on top
        List<KaartDorp> volgorde = new ArrayList<>(kaart.dorpen());
        volgorde.sort(Comparator.comparingInt(KaartDorp::cz).thenComparingInt(KaartDorp::cx));
        for (KaartDorp d : volgorde) {
            float sx = mx + (d.cx() + 0.5f - camX) * zoom, sy = my + (d.cz() + 0.5f - camZ) * zoom;
            if (sx < -40 || sy < -40 || sx > mw + 40 || sy > height + 40) {
                continue;
            }
            DorpIcoon.teken(g, d, Math.round(sx), Math.round(sy), zoom, soortKleur(d), isEigen(d), d.id().equals(gekozen), d == hover, tijd);
        }
        // names: the important ones first (own, chosen, hovered, players, then by level); a label that would overlap one
        // already placed is left out (hover shows it anyway)
        List<KaartDorp> namen = new ArrayList<>(kaart.dorpen());
        namen.sort(Comparator.comparingInt((KaartDorp d) -> isEigen(d) || d.id().equals(gekozen) || d == hover ? 0 : d.npc() ? 2 : 1)
                .thenComparing(Comparator.comparingInt(KaartDorp::stadhuisLevel).reversed()));
        List<int[]> geplaatst = new ArrayList<>();
        for (KaartDorp d : namen) {
            float sx = mx + (d.cx() + 0.5f - camX) * zoom, sy = my + (d.cz() + 0.5f - camZ) * zoom;
            if (sx < -60 || sy < -20 || sx > mw + 60 || sy > height + 20) {
                continue;
            }
            boolean moet = isEigen(d) || d.id().equals(gekozen) || d == hover;
            boolean toon = moet || zoom >= 20 || (zoom >= 10 && (!d.npc() || d.stadhuisLevel() >= 4));
            if (!toon) {
                continue;
            }
            int x = Math.round(sx), y = Math.round(sy + DorpIcoon.halveHoogte(d, zoom) + 3), w = font.width(labelTekst(d)) / 2 + 4;
            int[] r = {x - w, y - 2, x + w, y + 10};
            boolean vrij = true;
            for (int[] o : geplaatst) {
                vrij &= r[2] <= o[0] || r[0] >= o[2] || r[3] <= o[1] || r[1] >= o[3];
            }
            if (vrij || moet) {
                geplaatst.add(r);
                naamLabel(g, d, x, y);
            }
        }
        g.disableScissor();
        titel(g);
        legenda(g, mw);
    }

    private Component labelTekst(KaartDorp d) {
        MutableComponent t = naam(d).copy();
        if (zoom >= 20) {
            t.append(Component.literal(" " + d.stadhuisLevel()).withStyle(ChatFormatting.YELLOW));
        }
        return t;
    }

    private void naamLabel(GuiGraphicsExtractor g, KaartDorp d, int x, int y) {
        Component t = labelTekst(d);
        int w = font.width(t);
        g.fill(x - w / 2 - 3, y - 2, x + w / 2 + 3, y + 9, 0xA0403050);
        g.fill(x - w / 2 - 3, y + 9, x + w / 2 + 3, y + 10, soortKleur(d));
        g.text(font, t, x - w / 2, y, isEigen(d) ? 0xFFFFE27A : d.npc() ? 0xFFFFFFFF : 0xFFCDE9FF, true);
    }

    private int titelBreedte() {
        return Math.max(font.width(title) + 4, kaart == null ? 0 : font.width(ondertitel())) + 16;
    }

    private Component ondertitel() {
        int spelers = 0;
        for (KaartDorp d : kaart.dorpen()) {
            spelers += d.npc() ? 0 : 1;
        }
        return Component.translatable("erfenis.kaart.ondertitel", kaart.dorpen().size(), spelers);
    }

    private void titel(GuiGraphicsExtractor g) {
        int w = titelBreedte();
        g.fill(8, 8, 8 + w, 40, PAPIER);
        g.fill(8, 38, 8 + w, 40, ROZE);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), 16, 13, INKT, false);
        if (kaart != null) {
            g.text(font, ondertitel(), 16, 26, GRIJS, false);
        }
    }

    private void legenda(GuiGraphicsExtractor g, int mw) {
        Component[] t = {Component.translatable("erfenis.kaart.legenda.jij"), Component.translatable("erfenis.kaart.legenda.speler"),
                Component.translatable("erfenis.kaart.legenda.npc")};
        int[] k = {EIGEN, SPELER, NPC};
        int w = 0;
        for (Component c : t) {
            w = Math.max(w, font.width(c));
        }
        int x0 = mw - w - 26, y0 = height - 44;
        g.fill(x0, y0, mw - 6, height - 6, 0xC8FFF6EC);
        for (int i = 0; i < 3; i++) {
            g.fill(x0 + 5, y0 + 5 + i * 11, x0 + 12, y0 + 12 + i * 11, k[i]);
            g.text(font, t[i], x0 + 16, y0 + 5 + i * 11, INKT, false);
        }
    }

    // =====================================================================================================================
    // drawing: panels, list, tooltip (front stratum)
    // =====================================================================================================================

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int mw = kaartW();
        g.fill(mw, 0, width, height, 0xFFFFF6EC);
        g.fill(mw, 0, mw + 2, height, ROZE);
        if (kaart != null) {
            if (tab == 0) {
                ranglijst(g, mouseX, mouseY);
            } else {
                nieuwsLijst(g);
            }
        }
        KaartDorp d = dorp(gekozen);
        if (d != null) {
            paneel(g, d);
        }
        super.extractRenderState(g, mouseX, mouseY, a);
        if (hover != null) {
            g.setTooltipForNextFrame(font, tooltip(hover), Optional.empty(), mouseX, mouseY);
        }
    }

    private int lijstOnder() {
        return height - (terug != null ? 64 : 42);
    }

    private void ranglijst(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int mw = kaartW(), hoog = lijstOnder() - LIJST_Y;
        scroll[0] = Math.max(0, Math.min(scroll[0], Math.max(0, ranglijst.size() * RIJ - hoog)));
        g.enableScissor(mw + 2, LIJST_Y, width, lijstOnder());
        for (int i = 0; i < ranglijst.size(); i++) {
            int y = LIJST_Y + i * RIJ - Math.round(scroll[0]);
            if (y + RIJ < LIJST_Y || y > lijstOnder()) {
                continue;
            }
            KaartDorp d = ranglijst.get(i);
            if (isEigen(d)) {
                g.fill(mw + 2, y, width, y + RIJ, 0x66FFD54A);
            } else if (!d.npc()) {
                g.fill(mw + 2, y, width, y + RIJ, 0x335FAEF0);
            }
            if (d.id().equals(gekozen) || (mouseX >= mw + 2 && mouseY >= y && mouseY < y + RIJ && mouseY >= LIJST_Y && mouseY < lijstOnder())) {
                g.fill(mw + 2, y, width, y + RIJ, 0x22403050);
            }
            g.text(font, "#" + (i + 1), mw + 6, y + 2, GRIJS, false);
            g.fill(mw + 30, y + 3, mw + 36, y + 9, d.kleur());
            String p = Integer.toString(d.punten());
            int pw = font.width(p);
            String n = font.plainSubstrByWidth(naam(d).getString(), width - 8 - pw - 4 - (mw + 40));
            g.text(font, n, mw + 40, y + 2, d.npc() ? INKT : 0xFF2F6FAF, false);
            g.text(font, p, width - 8 - pw, y + 2, 0xFF7A5A20, false);
        }
        g.disableScissor();
        g.text(font, Component.translatable("erfenis.kaart.punten_uitleg"), mw + 8, lijstOnder() + 1, GRIJS, false);
    }

    private void nieuwsLijst(GuiGraphicsExtractor g) {
        int mw = kaartW(), breed = SIDE - 16;
        long nu = System.currentTimeMillis() + klokVerschil;
        List<Object[]> regels = new ArrayList<>();
        int totaal = 0;
        for (KaartNieuws n : kaart.nieuws()) {
            List<FormattedCharSequence> l = font.split(nieuwsTekst(n), breed);
            regels.add(new Object[] {l, leeftijd(nu - n.tijd())});
            totaal += l.size() * 10 + 14;
        }
        int hoog = lijstOnder() - LIJST_Y;
        scroll[1] = Math.max(0, Math.min(scroll[1], Math.max(0, totaal - hoog)));
        g.enableScissor(mw + 2, LIJST_Y, width, lijstOnder());
        int y = LIJST_Y - Math.round(scroll[1]);
        if (regels.isEmpty()) {
            g.text(font, Component.translatable("erfenis.kaart.nieuws.leeg"), mw + 8, y + 2, GRIJS, false);
        }
        for (Object[] r : regels) {
            @SuppressWarnings("unchecked")
            List<FormattedCharSequence> l = (List<FormattedCharSequence>) r[0];
            g.fill(mw + 6, y + 1, mw + 8, y + l.size() * 10 - 1, ROZE);
            for (FormattedCharSequence s : l) {
                g.text(font, s, mw + 11, y, INKT, false);
                y += 10;
            }
            g.text(font, (Component) r[1], mw + 11, y, GRIJS, false);
            y += 14;
        }
        g.disableScissor();
    }

    static Component nieuwsTekst(KaartNieuws n) {
        Object[] args = new Object[n.args().size()];
        for (int i = 0; i < args.length; i++) {
            String s = n.args().get(i);
            args[i] = s.startsWith("#") ? Component.translatable(s.substring(1)) : s;
        }
        return Component.translatable(n.key(), args);
    }

    private static Component leeftijd(long ms) {
        long s = Math.max(0, ms / 1000);
        if (s < 60) {
            return Component.translatable("erfenis.kaart.tijd.zojuist");
        }
        if (s < 3600) {
            return Component.translatable("erfenis.kaart.tijd.min", s / 60);
        }
        if (s < 86400) {
            return Component.translatable("erfenis.kaart.tijd.uur", s / 3600);
        }
        return Component.translatable("erfenis.kaart.tijd.dag", s / 86400);
    }

    private boolean overPaneel(double x, double y) {
        return gekozen != null && x >= 8 && x < 8 + PANEL_W && y >= height - PANEL_H - 8 && y < height - 8;
    }

    private void paneel(GuiGraphicsExtractor g, KaartDorp d) {
        int x = 8, y = height - PANEL_H - 8;
        g.fill(x, y, x + PANEL_W, y + PANEL_H, PAPIER);
        g.fill(x, y, x + PANEL_W, y + 3, d.kleur());
        g.fill(x, y + PANEL_H - 2, x + PANEL_W, y + PANEL_H, soortKleur(d));
        g.fill(x + 7, y + 9, x + 15, y + 17, 0xFF5A4660);
        g.fill(x + 8, y + 10, x + 14, y + 16, d.kleur());
        String n = font.plainSubstrByWidth(naam(d).getString(), PANEL_W - 40);
        g.text(font, Component.literal(n).withStyle(ChatFormatting.BOLD), x + 19, y + 9, INKT, false);
        Component wie = isEigen(d) ? Component.translatable("erfenis.kaart.soort.eigen")
                : d.npc() ? Component.translatable("erfenis.kaart.soort.npc", d.eigenaar())
                : Component.translatable("erfenis.kaart.soort.speler", d.eigenaar());
        g.text(font, wie, x + 8, y + 22, d.npc() ? GRIJS : 0xFF2F6FAF, false);
        g.text(font, Component.translatable("erfenis.kaart.paneel.level", d.stadhuisLevel(), grootte(d.stadhuisLevel()), d.punten()),
                x + 8, y + 34, INKT, false);
        g.text(font, Component.translatable("erfenis.kaart.paneel.rang", rang(d), ranglijst.size(), d.cx(), d.cz()), x + 8, y + 46, GRIJS,
                false);
        if (d.nieuw()) {
            g.text(font, Component.translatable("erfenis.kaart.paneel.nieuw"), x + 8, y + 58, 0xFFC07A10, false);
        }
    }

    private List<Component> tooltip(KaartDorp d) {
        List<Component> l = new ArrayList<>();
        l.add(naam(d).copy().withColor(soortKleur(d) == SPELER ? 0x8FCBFF : soortKleur(d) == EIGEN ? 0xFFE27A : 0xFFC4D6));
        l.add(Component.translatable("erfenis.kaart.tip.eigenaar", d.npc() ? d.eigenaar() : d.eigenaar()).withStyle(ChatFormatting.GRAY));
        l.add(Component.translatable("erfenis.kaart.tip.level", d.stadhuisLevel(), grootte(d.stadhuisLevel())));
        l.add(Component.translatable("erfenis.kaart.tip.punten", d.punten(), rang(d)));
        l.add(Component.translatable(isEigen(d) ? "erfenis.kaart.tip.eigen" : d.npc() ? "erfenis.kaart.tip.npc" : "erfenis.kaart.tip.speler")
                .withStyle(d.npc() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
        if (d.nieuw()) {
            l.add(Component.translatable("erfenis.kaart.paneel.nieuw").withStyle(ChatFormatting.GOLD));
        }
        l.add(Component.translatable("erfenis.kaart.tip.klik").withStyle(ChatFormatting.DARK_GRAY));
        return l;
    }

    // =====================================================================================================================
    // input
    // =====================================================================================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        double x = event.x(), y = event.y();
        int mw = kaartW();
        if (x >= mw) {
            if (tab == 0 && kaart != null && y >= LIJST_Y && y < lijstOnder()) {
                int i = (int) ((y - LIJST_Y + scroll[0]) / RIJ);
                if (i >= 0 && i < ranglijst.size()) {
                    KaartDorp d = ranglijst.get(i);
                    kies(d);
                    vlieg(d);
                }
            }
            return true;
        }
        if (overPaneel(x, y)) {
            return true;
        }
        if (event.button() == 0) {
            drukOpKaart = true;
            sleepAfstand = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (drukOpKaart) {
            camX -= (float) (dragX / zoom);
            camZ -= (float) (dragY / zoom);
            sleepAfstand += Math.abs(dragX) + Math.abs(dragY);
            vliegen = false;
            anker = false;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (drukOpKaart) {
            drukOpKaart = false;
            if (sleepAfstand < 4) {
                kies(hover);
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= kaartW()) {
            scroll[tab] -= (float) (scrollY * 24);
            return true;
        }
        if (scrollY != 0) {
            zoomRond(scrollY > 0 ? 1 : -1, (float) mouseX, (float) mouseY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (KaartClient.TOETS.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
