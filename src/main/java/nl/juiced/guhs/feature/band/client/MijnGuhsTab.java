package nl.juiced.guhs.feature.band.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.BandNiveau;
import nl.juiced.guhs.feature.band.BandPayloads;
import nl.juiced.guhs.feature.band.DagboekStat;
import nl.juiced.guhs.feature.band.FavorietSoort;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Roepen;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTekst;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The Guhdex tab "Mijn guhs" (2.10): the list of all your tamed guhs (a little picture, name, variant, hearts level and a
 * bar), and per guh its dagboekje: a 3D preview you turn by dragging, name / variant / personality, level and progress, and
 * a scrolling page with its favourites ("???" until discovered), friends, chores, where it is, statistics, eerste keren
 * and wist-je-datjes. Everything comes from {@link MijnGuhsCache} (the guhs themselves don't have to be loaded).
 */
public final class MijnGuhsTab {
    /** The guh whose page is open (null: the list); remembered while the game runs. */
    @Nullable
    private static UUID open;
    private static double lijstScroll, paginaScroll;
    private static final int RIJ = 30, KOP = 15;
    private static final int TEKST = 0xFF5A3A4A, DONKER = 0xFF3A1C30, ROZE = 0xFF7A2848, LICHT = 0xFFB0708A;

    private final GidsLijst lijst = new GidsLijst();
    private final Map<UUID, LivingEntity> poppen = new HashMap<>();
    private Font font;
    private int left, top, w, h;
    private float yaw = 25, pitch = -8;
    private boolean draaien;
    private Runnable herbouw = () -> {
    };
    /** The cache version the pictures were made for (new data: new clothes, so new pictures). */
    private int versie = -1;
    /** 1.2.5: "Roep naar mij" on a guh's page (null on the list) and its tooltip. */
    @Nullable
    private Button roepKnop;
    private List<Component> roepTip = List.of();
    private static final int ROEP_Y = 188, ROEP_H = 14;

    /** (AutoCheck) open this guh's page (null: the list). */
    public static void open(@Nullable UUID id) {
        open = id;
        paginaScroll = 0;
    }

    /** Builds the tab inside the Guhdex body; knop adds a button to the screen, herbouw rebuilds the screen. */
    public void init(Font font, int left, int top, int w, int h, Consumer<AbstractWidget> knop, Runnable herbouw) {
        this.font = font;
        this.left = left;
        this.top = top;
        this.w = w;
        this.h = h;
        this.herbouw = herbouw;
        if (versie != MijnGuhsCache.versie()) {
            versie = MijnGuhsCache.versie();
            poppen.clear();
        }
        UUID f = MijnGuhsCache.pakFocus();
        if (f != null) {
            open(f);
        }
        MijnGuhsCache.Guh guh = open == null ? null : MijnGuhsCache.van(open);
        if (open != null && guh == null) {
            open = null;
        }
        roepKnop = null;
        if (guh == null) {
            lijst.plaats(left + 8, top + 28, w - 14, h - 34);
            lijst.zet(lijstRegels());
            lijst.scrollNaar(lijstScroll);
        } else {
            knop.accept(Button.builder(Component.translatable("gui.guhs.mijnguhs.terug"), b -> {
                paginaScroll = lijst.scroll();
                open = null;
                this.herbouw.run();
            }).bounds(left + 8, top + 28, 98, 14).build());
            // 1.2.5: call this guh over, wherever it is (grey when it is picked up, in a Guh Wheel or in the wolkjes)
            Roepen.Uitkomst niet = Roepen.nietRoepbaar(PlekSoort.byId(guh.plekSoort()), guh.dood());
            roepKnop = Button.builder(Component.translatable("gui.guhs.mijnguhs.roep"),
                    b -> ClientPacketDistributor.sendToServer(new BandPayloads.Roep(guh.id()))).bounds(left + 8, top + ROEP_Y, 98, ROEP_H).build();
            roepKnop.active = niet == null;
            Component uitleg = Component.translatable(niet == null ? "gui.guhs.mijnguhs.roep.tip" : switch (niet) {
                case DOOD -> "gui.guhs.mijnguhs.roep.dood";
                case GUHWIEL -> "gui.guhs.mijnguhs.roep.guhwiel";
                default -> "gui.guhs.mijnguhs.roep.opgepakt";
            }, guh.naam());
            roepTip = regels(uitleg, niet == null ? ChatFormatting.GRAY : ChatFormatting.GOLD);
            knop.accept(roepKnop);
            lijst.plaats(left + 112, top + 28, w - 118, h - 34);
            lijst.zet(pagina(guh));
            lijst.scrollNaar(paginaScroll);
        }
    }

    /** Asks the server for fresh data. */
    public static void vraag() {
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new BandPayloads.Vraag(""));
        }
    }

    public void bewaarScroll() {
        if (open == null) {
            lijstScroll = lijst.scroll();
        } else {
            paginaScroll = lijst.scroll();
        }
    }

    // =====================================================================================================================
    // drawing
    // =====================================================================================================================

    public void teken(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        MijnGuhsCache.Guh guh = open == null ? null : MijnGuhsCache.van(open);
        if (guh == null) {
            if (MijnGuhsCache.guhs().isEmpty()) {
                GidsTekst.alinea(g, Component.translatable("gui.guhs.mijnguhs.leeg"), left + 20, top + 60, w - 40, 1f, TEKST);
            } else {
                String n = String.valueOf(MijnGuhsCache.guhs().size());
                g.text(font, n + " ♥", left + w - 8 - font.width(n + " ♥"), top + 9, ROZE, false);
            }
            lijst.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
            return;
        }
        // the picture (drag to turn it)
        int px1 = left + 8, py1 = top + 45, px2 = left + 106, py2 = top + 140;
        g.fill(px1, py1, px2, py2, 0x30F7B6CB);
        g.fill(px1, py2 - 1, px2, py2, 0x60D27A9C);
        LivingEntity pop = pop(guh);
        if (pop != null) {
            GuhPop.teken(g, px1, py1, px2, py2, pop, yaw, pitch);
        }
        GidsTekst.schaal(g, Component.translatable("gui.guhs.mijnguhs.draai"), px1 + 2, py2 - 8, 0.5f, LICHT, false);
        // name, variant, personality, level + bar
        int y = py2 + 3;
        GidsTekst.passend(g, guh.naam().copy().withStyle(ChatFormatting.BOLD), px1, y, px2 - px1, 1f, DONKER, false);
        y += 10;
        GidsTekst.passend(g, soortRegel(guh), px1, y, px2 - px1, 0.75f, TEKST, false);
        y += 8;
        BandNiveau niveau = BandNiveau.byIndex(guh.niveau());
        GidsTekst.passend(g, hartje(niveau).append(" ").append(niveauNaam(niveau)), px1, y, px2 - px1, 0.75f, kleur(niveau), false);
        y += 9;
        float frac = guh.volgende() <= 0 ? 1f : (float) (guh.hartjes() - niveau.drempel()) / Math.max(1, guh.volgende() - niveau.drempel());
        GidsTekst.balk(g, px1 + 1, y, px2 - px1 - 2, 4, frac);
        y += 7;
        Component hart = guh.volgende() <= 0 ? Component.translatable("gui.guhs.mijnguhs.hartjes_max", guh.hartjes())
                : Component.translatable("gui.guhs.mijnguhs.hartjes", guh.hartjes(), guh.volgende());
        GidsTekst.passend(g, hart, px1, y, px2 - px1, 0.625f, LICHT, false);
        lijst.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
    }

    @Nullable
    public List<Component> tip(double mx, double my) {
        if (roepKnop != null && mx >= roepKnop.getX() && mx < roepKnop.getX() + roepKnop.getWidth() && my >= roepKnop.getY()
                && my < roepKnop.getY() + roepKnop.getHeight()) {
            return roepTip;
        }
        return lijst.tip(mx, my);
    }

    /** A tooltip text cut into lines of at most 180 pixels (the tooltip doesn't wrap by itself). */
    private List<Component> regels(Component tekst, ChatFormatting kleur) {
        List<Component> out = new ArrayList<>();
        for (net.minecraft.network.chat.FormattedText r : font.getSplitter().splitLines(tekst, 180, net.minecraft.network.chat.Style.EMPTY)) {
            out.add(Component.literal(r.getString()).withStyle(kleur));
        }
        return out;
    }

    public boolean wiel(double mx, double my, double delta) {
        if (lijst.wiel(mx, my, delta)) {
            bewaarScroll();
            return true;
        }
        return false;
    }

    public boolean klik(double mx, double my, int button) {
        if (open != null && button == 0 && mx >= left + 8 && mx < left + 106 && my >= top + 45 && my < top + 140) {
            draaien = true;
            return true;
        }
        if (lijst.klik(mx, my, button)) {
            bewaarScroll();
            return true;
        }
        return false;
    }

    public boolean sleep(double mx, double my, double dx, double dy) {
        if (draaien) {
            yaw = (yaw + (float) dx * 2f) % 360f;
            pitch = Mth.clamp(pitch + (float) dy * 0.8f, -35f, 35f);
            return true;
        }
        if (lijst.sleep(my)) {
            bewaarScroll();
            return true;
        }
        return false;
    }

    public void los() {
        draaien = false;
        lijst.los();
    }

    @Nullable
    private LivingEntity pop(MijnGuhsCache.Guh guh) {
        return poppen.computeIfAbsent(guh.id(), k -> GuhPop.van(guh.looks()));
    }

    // =====================================================================================================================
    // rows
    // =====================================================================================================================

    private List<GidsLijst.Regel> lijstRegels() {
        List<GidsLijst.Regel> out = new ArrayList<>();
        for (MijnGuhsCache.Guh guh : MijnGuhsCache.guhs()) {
            out.add(new GuhRij(guh));
        }
        return out;
    }

    /** One guh in the list: click to open its dagboekje. */
    private final class GuhRij implements GidsLijst.Regel {
        private final MijnGuhsCache.Guh guh;

        GuhRij(MijnGuhsCache.Guh guh) {
            this.guh = guh;
        }

        @Override
        public int hoogte() {
            return RIJ;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            g.fill(x, y, x + w, y + RIJ - 2, hover ? 0x40F77AB0 : 0x20F7B6CB);
            LivingEntity pop = pop(guh);
            if (pop != null) {
                GuhPop.teken(g, x + 1, y + 1, x + 33, y + RIJ - 3, pop, 30, -5);
            }
            BandNiveau niveau = BandNiveau.byIndex(guh.niveau());
            int tx = x + 38, rechts = x + w - 4;
            int breed = Math.min(110, w / 3);
            Component naamRegel = guh.dood() ? Component.literal("☁ ").append(guh.naam().copy().withStyle(ChatFormatting.BOLD))
                    : guh.naam().copy().withStyle(ChatFormatting.BOLD);   // (3.0: in de wolkjes)
            GidsTekst.passend(g, naamRegel, tx, y + 4, rechts - breed - tx - 6, 1f, DONKER, false);
            // 1.2.6: where the guh is (short), instead of its personality (that's on its page)
            GidsTekst.passend(g, plekRegel(guh), tx, y + 16, rechts - breed - tx - 6, 0.75f, TEKST, false);
            GidsTekst.passend(g, hartje(niveau).append(" ").append(niveauNaam(niveau)), rechts, y + 5, breed, 0.625f, kleur(niveau), true);
            float frac = guh.volgende() <= 0 ? 1f : (float) (guh.hartjes() - niveau.drempel()) / Math.max(1, guh.volgende() - niveau.drempel());
            GidsTekst.balk(g, rechts - breed, y + 16, breed, 4, frac);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            lijstScroll = lijst.scroll();
            open(guh.id());
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.2f));
            herbouw.run();
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            return List.of(guh.naam().copy().withStyle(ChatFormatting.BOLD), guh.plek().copy().withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.guhs.mijnguhs.open_tip"));
        }
    }

    /** The dagboekje page of one guh. */
    private List<GidsLijst.Regel> pagina(MijnGuhsCache.Guh guh) {
        List<GidsLijst.Regel> out = new ArrayList<>();
        if (guh.dood()) {   // 3.0: a guh that went to the wolkjes keeps its dagboekje
            out.add(new Tekst(Component.translatable("gui.guhs.wolkjes.tab", guh.naam()), 0xFF8A6AB8, 0.875f));
            out.add(new Tekst(Component.translatable("gui.guhs.hemel.mijnguhs_hint"), 0xFFB05A8A, 0.75f));   // 3.0 hemel: the Knuffelhart
        }
        // favourites
        out.add(new Kop(Component.translatable("gui.guhs.mijnguhs.kop.favorietjes")));
        int gevonden = 0;
        for (MijnGuhsCache.Fav f : guh.fav()) {
            FavorietSoort soort = FavorietSoort.byId(f.soort());
            Component naam = soort == null ? Component.literal(f.soort()) : soort.naam();
            Component waarde = f.naam() == null ? Component.literal("???").withStyle(ChatFormatting.GRAY) : f.naam().copy().withStyle(ChatFormatting.LIGHT_PURPLE);
            out.add(new Paar(naam, waarde));
            gevonden += f.naam() == null ? 0 : 1;
        }
        if (gevonden == 0) {
            out.add(new Tekst(Component.translatable("gui.guhs.mijnguhs.favorietjes_hint"), LICHT, 0.75f));
        }
        // friends
        out.add(new Kop(Component.translatable("gui.guhs.mijnguhs.kop.vriendjes")));
        if (guh.vrienden().isEmpty()) {
            out.add(new Tekst(Component.translatable("gui.guhs.mijnguhs.geen_vriendjes"), LICHT, 0.75f));
        }
        for (MijnGuhsCache.Vriend v : guh.vrienden()) {
            out.add(new Tekst(v.bestie() ? Component.translatable("gui.guhs.mijnguhs.bestie", v.naam()) : Component.literal("♥ ").append(v.naam()),
                    v.bestie() ? 0xFFD89A10 : TEKST, 0.875f));
        }
        // chores
        out.add(new Kop(Component.translatable("gui.guhs.mijnguhs.kop.klusjes")));
        if (guh.huisje().getString().isEmpty()) {
            out.add(new Tekst(Component.translatable("gui.guhs.mijnguhs.geen_huisje"), LICHT, 0.75f));
        } else {
            out.add(new Tekst(Component.translatable("gui.guhs.mijnguhs.woont_in", guh.huisje()), TEKST, 0.875f));
            if (guh.klussen().isEmpty()) {
                out.add(new Tekst(Component.translatable("gui.guhs.mijnguhs.geen_klusjes"), LICHT, 0.75f));
            }
            for (Component k : guh.klussen()) {
                out.add(new Tekst(Component.literal("• ").append(k), TEKST, 0.875f));
            }
        }
        // where it is
        out.add(new Kop(Component.translatable("gui.guhs.mijnguhs.kop.waar", guh.naam())));
        out.add(new Tekst(guh.plek(), TEKST, 0.875f));
        // statistics
        out.add(new Kop(Component.translatable("gui.guhs.mijnguhs.kop.statistieken")));
        for (DagboekStat st : DagboekStat.values()) {
            long n = guh.stats().getOrDefault(st.id(), 0L);
            out.add(new Paar(st.naam(), Component.literal(String.valueOf(n)).withStyle(ChatFormatting.BOLD)));
        }
        if (guh.sinds() >= 0) {
            out.add(new Paar(Component.translatable("gui.guhs.mijnguhs.sinds"),
                    Component.translatable("gui.guhs.mijnguhs.dagen", Math.max(0, MijnGuhsCache.dag() - guh.sinds()) + 1)));
        }
        // eerste keren
        out.add(new Kop(Component.translatable("gui.guhs.mijnguhs.kop.eerste")));
        if (guh.eerste().isEmpty()) {
            out.add(new Tekst(Component.translatable("gui.guhs.mijnguhs.nog_niks"), LICHT, 0.75f));
        }
        for (MijnGuhsCache.Eerste e : guh.eerste()) {
            out.add(new Tekst(Component.translatable("gui.guhs.dagboek.eerste." + e.id()).withStyle(ChatFormatting.BOLD)
                    .append(Component.literal("  · ").append(Component.translatable("gui.guhs.mijnguhs.dag", e.dag() + 1)).withStyle(ChatFormatting.GRAY)),
                    DONKER, 0.875f));
            out.add(new Tekst(Component.translatable("gui.guhs.dagboek.eerste." + e.id() + ".tekst"), TEKST, 0.75f));
        }
        // wist-je-datjes
        out.add(new Kop(Component.translatable("gui.guhs.mijnguhs.kop.wistjedat")));
        if (guh.wist().isEmpty()) {
            out.add(new Tekst(Component.translatable("gui.guhs.mijnguhs.nog_niks"), LICHT, 0.75f));
        }
        for (MijnGuhsCache.Wist wd : guh.wist()) {
            out.add(new Tekst(Component.literal("“").append(wd.tekst()).append("”")
                    .append(Component.literal("  · ").append(Component.translatable("gui.guhs.mijnguhs.dag", wd.dag() + 1)).withStyle(ChatFormatting.GRAY)),
                    TEKST, 0.75f));
        }
        return out;
    }

    /** A section header. */
    private final class Kop implements GidsLijst.Regel {
        private final Component tekst;

        Kop(Component tekst) {
            this.tekst = tekst;
        }

        @Override
        public int hoogte() {
            return KOP;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            GidsTekst.passend(g, tekst.copy().withStyle(ChatFormatting.BOLD), x + 1, y + 4, w - 2, 1f, ROZE, false);
            g.fill(x, y + KOP - 2, x + w, y + KOP - 1, 0x60D27A9C);
        }
    }

    /** A wrapped text. */
    private final class Tekst implements GidsLijst.Regel {
        private final Component tekst;
        private final int kleur;
        private final float schaal;

        Tekst(Component tekst, int kleur, float schaal) {
            this.tekst = tekst;
            this.kleur = kleur;
            this.schaal = schaal;
        }

        @Override
        public int hoogte() {
            return GidsTekst.hoogte(tekst, lijst.rijBreedte() - 4, schaal) + 2;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            GidsTekst.alinea(g, tekst, x + 2, y + 1, w - 4, schaal, kleur);
        }
    }

    /** "name ....... value" on one line. */
    private final class Paar implements GidsLijst.Regel {
        private final Component naam, waarde;

        Paar(Component naam, Component waarde) {
            this.naam = naam;
            this.waarde = waarde;
        }

        @Override
        public int hoogte() {
            return 10;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY, boolean hover) {
            int vw = GidsTekst.passend(g, waarde, x + w - 2, y + 1, (w - 4) / 2, 0.875f, TEKST, true);
            GidsTekst.passend(g, naam, x + 2, y + 1, w - vw - 8, 0.875f, LICHT, false);
        }
    }

    // =====================================================================================================================
    // bits
    // =====================================================================================================================

    /** 1.2.6, the list row: the variant and where the guh is, short ("Mint Guh · Guh House Villa Chonk"). */
    static Component plekRegel(MijnGuhsCache.Guh guh) {
        Component soort = GuhVariant.byId(guh.looks().getStringOr("Variant", "")).displayName();
        return soort.copy().append(" · ").append(kortePlek(guh.plek()));
    }

    /** The short text of a place (gui.guhs.band.plek.X.kort with the same args), or the long one when there is no short one. */
    static Component kortePlek(Component plek) {
        if (plek.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && net.minecraft.client.resources.language.I18n.exists(t.getKey() + ".kort")) {
            return Component.translatable(t.getKey() + ".kort", t.getArgs());
        }
        return plek;
    }

    static Component soortRegel(MijnGuhsCache.Guh guh) {
        GuhVariant v = GuhVariant.byId(guh.looks().getStringOr("Variant", ""));
        GuhPersonality p = GuhPersonality.byId(guh.looks().getStringOr("Personality", ""));
        Component c = v.displayName().copy();
        return p == null ? c : c.copy().append(" · ").append(p.displayName());
    }

    public static Component niveauNaam(BandNiveau n) {
        return n == BandNiveau.GEEN ? Component.translatable("gui.guhs.band.niveau.geen") : n.naam();
    }

    public static net.minecraft.network.chat.MutableComponent hartje(BandNiveau n) {
        return Component.literal(switch (n) {
            case GEEN -> "♡";
            case LIEF -> "♥";
            case MEGA -> "♥♥";
            case ZIELSGUH -> "♥♥♥";
        });
    }

    public static int kleur(BandNiveau n) {
        return switch (n) {
            case GEEN -> 0xFFB0708A;
            case LIEF -> 0xFFE0508A;
            case MEGA -> 0xFFD02060;
            case ZIELSGUH -> 0xFFD89A10;
        };
    }
}
