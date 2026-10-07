package nl.juiced.guhs.feature.gids.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.gids.VerhalenVoortgang;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Reiskaart;
import nl.juiced.guhs.feature.verhaal.Reiskaarten;
import nl.juiced.guhs.feature.verhaal.VerhaalPayloads;
import nl.juiced.guhs.feature.verhaal.VerhaalSync;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * The Guhdex tab "Verhalen": every questline (the Guhverhalen first, then the older adventures) as a row with its icon,
 * name, status ("nog niet begonnen" / "stap 3 van 7: ..." / "klaar!") and a bar; click a row for its page: what to do
 * now, whom to go to and where, what you still need (with counts), every step ticked off, and the rewards. Both scroll
 * (wheel or bar). Everything comes from {@link VerhalenCache}.
 * <p>
 * bbq2 (verhaal engine): the registered questlines ({@link Verhaallijn}) come first, under the heading of their group; a
 * group with a travel map ({@link Reiskaart}) has a "Reiskaart" row that opens the map page ({@link GidsReiskaart}); a
 * line whose predecessor isn't done yet shows as "???"; a page has "Volg dit verhaal" (the objective line and the
 * Superkompas follow it) and "Opnieuw bekijken" for its scenes and narrator cards; the list starts with the switch of the
 * objective line.
 */
public final class GidsVerhalenTab {
    /** The page id of a travel map: this prefix + the map's id. */
    public static final String KAART = "@kaart:";
    /** The questline whose page is open (null: the list); remembered while the game runs. */
    @Nullable
    private static String open;
    private static double lijstScroll, paginaScroll;
    private static final int RIJ = 28, KOP = 14;
    static final int TEKST = 0xFF5A3A4A, DONKER = 0xFF3A1C30, ROZE = 0xFF7A2848, LICHT = 0xFFB0708A, GROEN = 0xFF3E9A5A,
            GRIJS = 0xFF9A8090;

    /**
     * guhpad: another layout of the LIST (set by a feature's client init; null: the plain list of {@link #lijstRegels}).
     * It builds its rows from the public pieces of this tab ({@link #rij}, {@link #kaartRij}, {@link #doelSchakelaar},
     * {@link #kop}, {@link #tekst}, {@link #losseHerbekijk}) and asks for a redraw with {@link #verbouw}.
     */
    public interface Indeling {
        List<GidsLijst.Regel> lijst(GidsVerhalenTab tab, List<VerhaalStand> alle);
    }

    @Nullable
    public static volatile Indeling indeling;

    private final GidsLijst lijst = new GidsLijst();
    private Runnable herbouw = () -> {
    };
    private int left, top, w, h;

    /** (AutoCheck) open this questline's page (null: the list; {@link #KAART} + id: a travel map). */
    public static void open(@Nullable String id) {
        open = id;
        paginaScroll = 0;
    }

    @Nullable
    public static String open() {
        return open;
    }

    /** The travel map that is open (null: none). */
    @Nullable
    private static Reiskaart kaart() {
        return open != null && open.startsWith(KAART) ? Reiskaarten.van(open.substring(KAART.length())) : null;
    }

    public void init(int left, int top, int w, int h, Consumer<AbstractWidget> knop, Runnable herbouw) {
        this.left = left;
        this.top = top;
        this.w = w;
        this.h = h;
        this.herbouw = herbouw;
        Reiskaart k = kaart();
        VerhaalStand v = open == null || k != null ? null : VerhalenCache.van(open);
        if (v == null && k == null) {
            lijst.plaats(left + 8, top + 40, w - 14, h - 46);
            lijst.zet(lijstRegels());
            lijst.scrollNaar(lijstScroll);
        } else {
            knop.accept(Button.builder(Component.translatable("gui.guhs.verhalen.terug"), b -> {
                paginaScroll = 0;
                open = null;
                this.herbouw.run();
            }).bounds(left + 8, top + 28, 70, 14).build());
            lijst.plaats(left + 8, top + 46, w - 14, h - 52);
            lijst.zet(k != null ? GidsReiskaart.pagina(this, k) : pagina(v));
            lijst.scrollNaar(paginaScroll);
        }
    }

    public void bewaarScroll() {
        if (open == null) {
            lijstScroll = lijst.scroll();
        } else {
            paginaScroll = lijst.scroll();
        }
    }

    /** (AutoCheck) screenfuls of the current list, and scroll to one. */
    public int paginas() {
        return 1 + (int) Math.ceil(lijst.max() / (double) Math.max(20, h - 80));
    }

    public void scrollNaar(int pagina) {
        lijst.scrollNaar(pagina * Math.max(20, h - 80));
        bewaarScroll();
    }

    public int rijBreedte() {
        return lijst.rijBreedte();
    }

    /** guhpad: builds the list again where it is scrolled to (a layout folded something open or shut). */
    public void verbouw() {
        if (open == null) {
            lijstScroll = lijst.scroll();
        }
        herbouw.run();
    }

    /** guhpad: scrolls the list so that the first row that fits (of the rows that show now) is at the top. */
    public void scrollNaarRegel(java.util.function.Predicate<GidsLijst.Regel> welke) {
        int y = 0;
        for (GidsLijst.Regel r : lijst.regels()) {
            if (welke.test(r)) {
                lijst.scrollNaar(y);
                bewaarScroll();
                return;
            }
            y += r.hoogte();
        }
    }

    /** guhpad: a questline's row of the list ("???" while it is still a secret). */
    public GidsLijst.Regel rij(VerhaalStand v) {
        return geheim(v.id()) ? new Geheim() : new Rij(v);
    }

    /** guhpad: the row that opens a group's travel map. */
    public GidsLijst.Regel kaartRij(Reiskaart k) {
        return new KaartRij(k);
    }

    /** guhpad: the switch of the objective line (the first row of the list). */
    public GidsLijst.Regel doelSchakelaar() {
        return new DoelSchakelaar();
    }

    /** guhpad: the heading and the rows of the seen scenes and cards that belong to no questline (empty: none). */
    public List<GidsLijst.Regel> losseHerbekijk() {
        List<GidsLijst.Regel> out = new ArrayList<>();
        List<GidsLijst.Regel> los = herbekijkRegels(null);
        if (!los.isEmpty()) {
            out.add(kop(Component.translatable("gui.guhs.verhalen.kop.herbekijk")));
            out.addAll(los);
        }
        return out;
    }

    /** Opens another page of the tab (a questline, or a map). */
    void ga(@Nullable String id) {
        if (open == null) {
            lijstScroll = lijst.scroll();
        }
        open(id);
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.2f));
        herbouw.run();
    }

    // =====================================================================================================================
    // drawing / input
    // =====================================================================================================================

    public void teken(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Reiskaart k = kaart();
        VerhaalStand v = open == null || k != null ? null : VerhalenCache.van(open);
        if (k != null) {
            GidsTekst.passend(g, Component.translatable(k.naamKey()).withStyle(ChatFormatting.BOLD), left + 84, top + 31, w - 92, 1f, DONKER, false);
        } else if (v == null) {
            List<VerhaalStand> alle = VerhalenCache.verhalen();
            long klaar = alle.stream().filter(VerhaalStand::klaar).count();
            // (the count sits on the line under the tab bar: the tab bar's title needs all its room)
            Component n = Component.literal(klaar + " / " + alle.size() + " ✔");
            GidsTekst.schaal(g, n, left + w - 10, top + 29, 0.75f, ROZE, true);
            int nw = Math.round(GidsTekst.font().width(n) * 0.75f) + 8;
            GidsTekst.passend(g, Component.translatable(alle.isEmpty() ? "gui.guhs.verhalen.laden" : "gui.guhs.verhalen.klik"),
                    left + 10, top + 29, w - 20 - nw, 0.75f, LICHT, false);
        } else {
            // next to the back button: the name of the story
            GidsTekst.passend(g, v.naam().copy().withStyle(ChatFormatting.BOLD), left + 84, top + 31, w - 92, 1f, DONKER, false);
        }
        lijst.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
    }

    public boolean wiel(double mx, double my, double delta) {
        boolean r = lijst.wiel(mx, my, delta);
        if (r) {
            bewaarScroll();
        }
        return r;
    }

    public boolean sleep(double my) {
        boolean r = lijst.sleep(my);
        if (r) {
            bewaarScroll();
        }
        return r;
    }

    public void los() {
        lijst.los();
    }

    public boolean klik(double mx, double my, int button) {
        boolean r = lijst.klik(mx, my, button);
        if (r) {
            bewaarScroll();
        }
        return r;
    }

    @Nullable
    public List<Component> tip(double mx, double my) {
        return lijst.tip(mx, my);
    }

    // =====================================================================================================================
    // the list
    // =====================================================================================================================

    /** The heading a questline stands under: its group (a registered line), else "nieuw" (the Guhverhalen) or "oud". */
    public static String groepVan(String id) {
        Verhaallijn l = Verhaallijnen.van(id);
        return l != null ? l.groep() : VerhalenVoortgang.NIEUW.contains(id) ? "nieuw" : "oud";
    }

    /** bbq2: is this line still a secret for the player (the line before it isn't done: no spoilers)? */
    static boolean geheim(String id) {
        Verhaallijn l = Verhaallijnen.van(id);
        Verhaallijn voor = l == null ? null : Verhaallijnen.van(l.na());
        return voor != null && VerhaalSync.Client.stap(voor.id()) < voor.stappen();
    }

    private List<GidsLijst.Regel> lijstRegels() {
        List<GidsLijst.Regel> out = new ArrayList<>();
        List<VerhaalStand> alle = VerhalenCache.verhalen();
        Indeling eigen = indeling;
        if (eigen != null && !alle.isEmpty()) {
            return eigen.lijst(this, alle);   // (guhpad: the path map on top, the stories folded per world)
        }
        if (!alle.isEmpty()) {
            out.add(new DoelSchakelaar());
        }
        String kop = null;
        for (VerhaalStand v : alle) {
            String nieuw = groepVan(v.id());
            if (!nieuw.equals(kop)) {
                kop = nieuw;
                out.add(kop(Component.translatable("gui.guhs.verhalen.kop." + nieuw)));
                Reiskaart k = Reiskaarten.vanGroep(nieuw);
                if (k != null) {
                    out.add(new KaartRij(k));
                }
            }
            out.add(geheim(v.id()) ? new Geheim() : new Rij(v));
        }
        // the scenes and cards seen that belong to no questline
        List<GidsLijst.Regel> los = herbekijkRegels(null);
        if (!los.isEmpty()) {
            out.add(kop(Component.translatable("gui.guhs.verhalen.kop.herbekijk")));
            out.addAll(los);
        }
        return out;
    }

    public static GidsLijst.Regel kop(Component tekst) {
        return new GidsLijst.Regel() {
            @Override
            public int hoogte() {
                return KOP;
            }

            @Override
            public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
                GidsTekst.passend(g, tekst.copy().withStyle(ChatFormatting.BOLD), x + 2, y + 4, w - 4, 0.875f, ROZE, false);
                g.fill(x + 2, y + KOP - 2, x + w - 2, y + KOP - 1, 0x60D27A9C);
            }
        };
    }

    /** One questline in the list. */
    private final class Rij implements GidsLijst.Regel {
        private final VerhaalStand v;
        private final ItemStack icoon;

        Rij(VerhaalStand v) {
            this.v = v;
            this.icoon = stack(v.icoon());
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            boolean volg = v.id().equals(VerhaalSync.Client.volg());
            g.fill(x + 1, y + 1, x + w - 1, y + RIJ - 1, hover ? 0x40F7B6CB : 0x18F7B6CB);
            if (volg) {
                g.fill(x + 1, y + 1, x + 3, y + RIJ - 1, 0xFFF7D27A);   // (bbq2: the line you follow)
            }
            g.item(icoon, x + 5, y + 6);
            if (v.klaar()) {
                GidsTekst.schaal(g, Component.literal("✔"), x + 16, y + 17, 0.75f, GROEN, false);
            }
            int tx = x + 26, bar = 44, tw = w - 26 - bar - 10;
            GidsTekst.passend(g, v.naam().copy().withStyle(ChatFormatting.BOLD), tx, y + 4, tw, 1f, v.status() == VerhaalStand.Status.NIET_BEGONNEN ? GRIJS : DONKER, false);
            GidsTekst.passend(g, statusRegel(v), tx, y + 16, tw, 0.75f, kleur(v), false);
            GidsTekst.balk(g, x + w - bar - 6, y + 11, bar, 5, v.stap() / (float) Math.max(1, v.stappen()));
            GidsTekst.schaal(g, Component.literal(v.stap() + "/" + v.stappen()), x + w - 6, y + 19, 0.5f, LICHT, true);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            ga(v.id());
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> tip = new ArrayList<>();
            tip.add(v.naam().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            tip.add(v.nu().copy().withStyle(ChatFormatting.GRAY));
            if (v.id().equals(VerhaalSync.Client.volg())) {
                tip.add(Component.translatable("gui.guhs.verhalen.volgt").withStyle(ChatFormatting.GOLD));
            }
            tip.add(Component.translatable("gui.guhs.verhalen.klik_rij").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return tip;
        }
    }

    /** bbq2: a questline that is still a secret: "???". */
    private static final class Geheim implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 18;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x + 1, y + 1, x + w - 1, y + 17, 0x10F7B6CB);
            GidsTekst.schaal(g, Component.literal("?"), x + 10, y + 5, 1f, GRIJS, false);
            GidsTekst.passend(g, Component.translatable("gui.guhs.verhalen.geheim"), x + 26, y + 5, w - 32, 0.875f, GRIJS, false);
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(Component.translatable("gui.guhs.verhalen.geheim.tooltip").withStyle(ChatFormatting.GRAY));
        }
    }

    /** bbq2: the row that opens a group's travel map. */
    private final class KaartRij implements GidsLijst.Regel {
        private final Reiskaart k;

        KaartRij(Reiskaart k) {
            this.k = k;
        }

        @Override
        public int hoogte() {
            return 20;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x + 1, y + 1, x + w - 1, y + 19, hover ? 0x60F7D27A : 0x30F7D27A);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x40FFFFFF);
            g.item(new ItemStack(Items.FILLED_MAP), x + 5, y + 2);
            GidsTekst.passend(g, Component.translatable("gui.guhs.verhalen.reiskaart", Component.translatable(k.naamKey())).withStyle(ChatFormatting.BOLD),
                    x + 26, y + 6, w - 44, 0.875f, DONKER, false);
            GidsTekst.schaal(g, Component.literal("»"), x + w - 8, y + 6, 1f, ROZE, true);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            ga(KAART + k.id());
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(Component.translatable("gui.guhs.verhalen.reiskaart.tooltip").withStyle(ChatFormatting.GRAY));
        }
    }

    /** bbq2: the switch of the objective line on the screen (saved in the client config). */
    private final class DoelSchakelaar implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return 16;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            boolean aan = nl.juiced.guhs.client.GuhsClientConfig.objectiveLine();
            g.fill(x + 1, y + 1, x + w - 1, y + 15, hover ? 0x50F7B6CB : 0x28F7B6CB);
            g.fill(x + 5, y + 4, x + 13, y + 12, 0xFF7A2848);
            g.fill(x + 6, y + 5, x + 12, y + 11, aan ? 0xFF68D88A : 0xFF3A1C30);
            GidsTekst.passend(g, Component.translatable(aan ? "gui.guhs.verhalen.doelregel.aan" : "gui.guhs.verhalen.doelregel.uit"), x + 18, y + 5,
                    w - 24, 0.75f, DONKER, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            nl.juiced.guhs.client.GuhsClientConfig.objectiveLine(!nl.juiced.guhs.client.GuhsClientConfig.objectiveLine());
            klikGeluid();
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(Component.translatable("gui.guhs.verhalen.doelregel.tooltip").withStyle(ChatFormatting.GRAY));
        }
    }

    static void klikGeluid() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.2f));
    }

    /** "Nog niet begonnen" / "Stap 3 van 7: ..." / "Klaar!". */
    public static Component statusRegel(VerhaalStand v) {
        return switch (v.status()) {
            case NIET_BEGONNEN -> Component.translatable("gui.guhs.verhalen.status.niet_begonnen");
            case KLAAR -> Component.translatable("gui.guhs.verhalen.status.klaar");
            case BEZIG -> Component.translatable("gui.guhs.verhalen.status.stap", v.stap() + 1, v.stappen(), v.stapNaam(v.stap()));
        };
    }

    static int kleur(VerhaalStand v) {
        return switch (v.status()) {
            case NIET_BEGONNEN -> GRIJS;
            case KLAAR -> GROEN;
            case BEZIG -> ROZE;
        };
    }

    // =====================================================================================================================
    // a questline's page
    // =====================================================================================================================

    private List<GidsLijst.Regel> pagina(VerhaalStand v) {
        List<GidsLijst.Regel> out = new ArrayList<>();
        out.add(new Kopregel(v));
        out.add(tekst(v.uitleg().copy().withStyle(ChatFormatting.ITALIC), 0.75f, LICHT, 4));
        out.add(kop(Component.translatable(v.klaar() ? "gui.guhs.verhalen.kop.klaar" : "gui.guhs.verhalen.kop.nu")));
        out.add(tekst(v.nu(), 0.875f, DONKER, 4));
        out.add(tekst(Component.literal("➜ ").append(v.waar()), 0.75f, ROZE, 4));
        if (Verhaallijnen.van(v.id()) != null && !v.klaar()) {
            out.add(new Volg(v.id()));
        }
        if (!v.nodig().isEmpty()) {
            out.add(kop(Component.translatable("gui.guhs.verhalen.kop.nodig")));
            for (VerhaalStand.Nodig n : v.nodig()) {
                out.add(new Ding(stack(n.item()), n.naam(), Component.literal(n.heb() + " / " + n.nodig()), n.genoeg()));
            }
        }
        out.add(kop(Component.translatable("gui.guhs.verhalen.kop.stappen")));
        out.addAll(stappen(v));
        if (!v.beloningen().isEmpty()) {
            out.add(kop(Component.translatable("gui.guhs.verhalen.kop.beloningen")));
            for (VerhaalStand.Beloning b : v.beloningen()) {
                out.add(new Ding(stack(b.item()), b.tekst(), Component.translatable(b.binnen() ? "gui.guhs.verhalen.binnen" : "gui.guhs.verhalen.nog_niet"),
                        b.binnen()));
            }
        }
        List<GidsLijst.Regel> herbekijk = herbekijkRegels(v.id());
        if (!herbekijk.isEmpty()) {
            out.add(kop(Component.translatable("gui.guhs.verhalen.kop.herbekijk")));
            out.addAll(herbekijk);
        }
        return out;
    }

    /** Every step of a questline with its tick. */
    List<GidsLijst.Regel> stappen(VerhaalStand v) {
        List<GidsLijst.Regel> out = new ArrayList<>();
        for (int i = 0; i < v.stappen(); i++) {
            boolean gedaan = i < v.stap(), nu = i == v.stap() && v.status() != VerhaalStand.Status.NIET_BEGONNEN;
            String teken = gedaan ? "✔ " : nu ? "➜ " : "○ ";
            int kleur = gedaan ? GROEN : nu ? ROZE : GRIJS;
            out.add(tekst(Component.literal(teken).append(v.stapNaam(i)), 0.75f, kleur, 6));
        }
        return out;
    }

    /**
     * bbq2: the "watch again" rows of the scenes and narrator cards this player saw that belong to this questline (null:
     * the ones that belong to none).
     */
    List<GidsLijst.Regel> herbekijkRegels(@Nullable String lijn) {
        List<GidsLijst.Regel> out = new ArrayList<>();
        for (String id : Verteller.ids()) {
            Verteller.Kaart k = Verteller.van(id);
            if (k != null && java.util.Objects.equals(k.lijn(), lijn) && VerhaalSync.Client.kaartGezien(id)) {
                out.add(new Herbekijk(VerhaalPayloads.KAART, id, Component.translatable(k.titelKey()), Items.MAP));
            }
        }
        for (Cutscene s : Cutscene.alle()) {
            if (java.util.Objects.equals(s.lijn(), lijn) && VerhaalSync.Client.sceneGezien(s.id())) {
                out.add(new Herbekijk(VerhaalPayloads.SCENE, s.id(), Component.translatable(s.titelKey()), Items.SPYGLASS));
            }
        }
        return out;
    }

    /** Wrapped text at a scale, indented. */
    public GidsLijst.Regel tekst(Component c, float scale, int kleur, int in) {
        return new GidsLijst.Regel() {
            @Override
            public int hoogte() {
                return GidsTekst.hoogte(c, lijst.rijBreedte() - in - 4, scale) + 3;
            }

            @Override
            public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
                GidsTekst.alinea(g, c, x + in, y + 2, w - in - 4, scale, kleur);
            }
        };
    }

    /** bbq2: "Volg dit verhaal": the objective line and the Superkompas entry "Mijn verhaal" follow this line (click again: by itself). */
    private static final class Volg implements GidsLijst.Regel {
        private final String id;

        Volg(String id) {
            this.id = id;
        }

        @Override
        public int hoogte() {
            return 18;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            boolean gekozen = id.equals(VerhaalSync.Client.gekozen()), volgt = id.equals(VerhaalSync.Client.volg());
            g.fill(x + 4, y + 2, x + w - 4, y + 16, hover ? 0xFFF7D27A : 0xFFD9B56A);
            g.fill(x + 5, y + 3, x + w - 5, y + 15, hover ? 0xFFFFF4D8 : 0xFFFBEBC4);
            Component t = Component.translatable(gekozen ? "gui.guhs.verhalen.volg.gekozen" : volgt ? "gui.guhs.verhalen.volg.vanzelf" : "gui.guhs.verhalen.volg");
            int tw = Math.min(w - 20, Math.round(GidsTekst.font().width(t) * 0.75f));
            GidsTekst.passend(g, t, x + (w - tw) / 2, y + 6, w - 20, 0.75f, DONKER, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            boolean gekozen = id.equals(VerhaalSync.Client.gekozen());
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new VerhaalSync.Volg(gekozen ? "" : id));
            klikGeluid();
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(Component.translatable("gui.guhs.verhalen.volg.tooltip").withStyle(ChatFormatting.GRAY));
        }
    }

    /** bbq2: watch a seen scene or narrator card again (the Guhdex closes and it plays). */
    private static final class Herbekijk implements GidsLijst.Regel {
        private final String soort, id;
        private final Component titel;
        private final ItemStack icoon;

        Herbekijk(String soort, String id, Component titel, net.minecraft.world.item.Item icoon) {
            this.soort = soort;
            this.id = id;
            this.titel = titel;
            this.icoon = new ItemStack(icoon);
        }

        @Override
        public int hoogte() {
            return 18;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x + 1, y + 1, x + w - 1, y + 17, hover ? 0x40F7B6CB : 0x18F7B6CB);
            g.item(icoon, x + 6, y + 1);
            Component rechts = Component.translatable("gui.guhs.verhalen.herbekijk");
            int rw = Math.round(GidsTekst.font().width(rechts) * 0.75f);
            GidsTekst.passend(g, titel, x + 26, y + 5, w - 26 - rw - 12, 0.875f, DONKER, false);
            GidsTekst.schaal(g, rechts, x + w - 6, y + 6, 0.75f, ROZE, true);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            mc.setScreen(null);
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new VerhaalPayloads.Herbekijk(soort, id));
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(Component.translatable(VerhaalPayloads.KAART.equals(soort) ? "gui.guhs.verhalen.herbekijk.kaart" : "gui.guhs.verhalen.herbekijk.scene")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** The top of a page: big icon, status, bar. */
    private static final class Kopregel implements GidsLijst.Regel {
        private final VerhaalStand v;
        private final ItemStack icoon;

        Kopregel(VerhaalStand v) {
            this.v = v;
            this.icoon = stack(v.icoon());
        }

        @Override
        public int hoogte() {
            return 24;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x + 1, y + 1, x + w - 1, y + 23, 0x20F7B6CB);
            g.item(icoon, x + 4, y + 4);
            int tx = x + 26, bar = 60;
            GidsTekst.passend(g, statusRegel(v), tx, y + 5, w - 26 - bar - 12, 0.875f, kleur(v), false);
            GidsTekst.passend(g, Component.translatable("gui.guhs.verhalen.stappen_telling", v.stap(), v.stappen()), tx, y + 15, w - 26 - bar - 12, 0.625f, LICHT, false);
            GidsTekst.balk(g, x + w - bar - 6, y + 9, bar, 6, v.stap() / (float) Math.max(1, v.stappen()));
        }
    }

    /** An item row: icon, name, and on the right a count or "binnen" (green when done). */
    private static final class Ding implements GidsLijst.Regel {
        private final ItemStack icoon;
        private final Component naam, rechts;
        private final boolean ok;

        Ding(ItemStack icoon, Component naam, Component rechts, boolean ok) {
            this.icoon = icoon;
            this.naam = naam;
            this.rechts = (ok ? Component.literal("✔ ") : Component.empty()).append(rechts);
            this.ok = ok;
        }

        @Override
        public int hoogte() {
            return 18;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.item(icoon, x + 6, y + 1);
            int rw = Math.round(GidsTekst.font().width(rechts) * 0.75f);
            GidsTekst.passend(g, naam, x + 26, y + 5, w - 26 - rw - 12, 0.875f, ok ? TEKST : DONKER, false);
            GidsTekst.schaal(g, rechts, x + w - 6, y + 6, 0.75f, ok ? GROEN : ROZE, true);
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return mx < x + 24 ? List.of(icoon.getHoverName()) : null;
        }
    }

    static ItemStack stack(String id) {
        var item = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(id) == null ? Identifier.withDefaultNamespace("book") : Identifier.tryParse(id));
        return new ItemStack(item == Items.AIR ? Items.BOOK : item);
    }
}
