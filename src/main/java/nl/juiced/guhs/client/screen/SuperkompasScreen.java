package nl.juiced.guhs.client.screen;

import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.gids.GidsData;
import nl.juiced.guhs.feature.gids.client.GidsLijst;
import nl.juiced.guhs.feature.gids.client.GidsTabs;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen.Wereld;
import nl.juiced.guhs.feature.guhpad.GuhpadPayloads;
import nl.juiced.guhs.feature.guhpad.KompasVerhalen;
import nl.juiced.guhs.feature.guhpad.client.GuhpadTab;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.network.MaagPayloads;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The super compass menu (2.9, the gids slice): a row of icon tabs on top (one per category, like the creative
 * inventory; the name on hover), under it the chosen category's name and what it's about, and a scrolling list of its
 * places in two columns, with subheadings where a category has them (Minigames: Klassiekers, Knuffeldal, De Grote
 * Guhspelen). A minigame place shows its game's icon; every place shows a green tick once you've been there (1.3.1). Click a place: that's what
 * the compass looks for. The first option of every tab is "Mijn verhaal" (guhpad: a normal option next to the places, with
 * its explanation on hover): the compass then points to the next goal of the questline you follow, or else to the nearest
 * story you have not done yet.
 * <p>
 * 1.4.1: the tab Verhalen is split by world and progress like the Guhdex tab Verhalen ({@code feature.guhpad.KompasVerhalen},
 * the one source is GroteVerhalen): a heading per world of the Guhpad that folds open and shut, locked with what is still
 * missing where the player may not go yet, under it every big story's places (the Knabbelring: a place per chapter). A place
 * the player's story has not reached is "???" with a lock and can not be chosen. "Het echte Guheinde" is only its locked line.
 */
public class SuperkompasScreen extends Screen {
    private static final int W = 300, H = 232;
    private static final int TABS_Y = 22, LIST_TOP = 70, KNOP_H = 20, KOPJE_H = 14;
    private static final int GOUD = 0xFFF7D27A, LICHT = 0xFFFFE6EE, ZACHT = 0xFFB8A0B0;
    /** The last tab you looked at (remembered while the game runs). */
    private static int laatste = -1;
    private final InteractionHand hand;
    @Nullable
    private final String chosen;
    private final GidsLijst lijst = new GidsLijst();
    /** (1.4.1, the tab Verhalen) the worlds the player folded shut; remembered while the game runs. */
    private static final java.util.Map<Wereld, Boolean> GEVOUWEN = new java.util.EnumMap<>(Wereld.class);
    /** (1.4.1, the tab Verhalen) the places that show as "???" now, and the big story of each story place. */
    private final java.util.Set<String> geheim = new java.util.HashSet<>();
    private final java.util.Map<String, String> verhaalVan = new java.util.HashMap<>();
    private int left, top, tab;

    public SuperkompasScreen(InteractionHand hand, @Nullable String chosen) {
        super(Component.translatable("item.guhs.guhmensie_superkompas"));
        this.hand = hand;
        this.chosen = chosen;
        // open on the tab you had last, when it has what the compass looks for; else the first tab that has it
        int of = SuperkompasItem.categoryOf(chosen);
        if (laatste >= 0 && laatste < SuperkompasItem.CATEGORIES.size()
                && (chosen == null || SuperkompasItem.CATEGORIES.get(laatste).structures().contains(chosen))) {
            tab = laatste;
        } else {
            tab = Math.max(0, of);
        }
        tab = nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.start(hand, chosen, tab, laatste);   // biomes3: the tab Biomes
    }

    /** (AutoCheck) the tab that shows. */
    public void showTab(int index) {
        tab = Math.floorMod(index, SuperkompasItem.CATEGORIES.size() + 1);   // biomes3: + the tab Biomes
        laatste = tab;
        rebuildWidgets();
    }

    private int tabsX() {
        return nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.tabsX(left, W);   // biomes3: one more tab, narrower tabs when they no longer fit
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        laatste = tab;
        lijst.plaats(left + 8, top + LIST_TOP, W - 14, H - LIST_TOP - 34);
        lijst.zet(regels());
        lijst.scrollNaar(0);
        // scroll the chosen place into view
        int y = 0;
        for (GidsLijst.Regel r : lijst.regels()) {
            if (r instanceof Paar p && (p.links().equals(chosen) || chosen != null && chosen.equals(p.rechts()))) {
                lijst.scrollNaar(y - 20);
                break;
            }
            y += r.hoogte();
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 88, top + H - 28, 80, 20).build());
        // 1.2.0: the language of the Guhs texts (Auto / NL / EN), under the panel at the right like the Guhdex's (above it when
        // the window is low)
        int taalY = top + H + 18 <= height ? top + H + 3 : Math.max(0, top - 17);
        addRenderableWidget(nl.juiced.guhs.client.GuhsTaal.knop(left + W, taalY, 84, 14));
    }

    /** The rows: subheadings and pairs of places. */
    private List<GidsLijst.Regel> regels() {
        if (nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.is(tab)) {   // biomes3
            return nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.regels(hand, lijst, this::onClose);   // biomes3
        }   // biomes3
        geheim.clear();
        verhaalVan.clear();
        List<GidsLijst.Regel> out = new ArrayList<>();
        SuperkompasItem.Category c = SuperkompasItem.CATEGORIES.get(tab);
        if (c.id().equals(KompasVerhalen.TAB)) {   // 1.4.1
            return verhalenRegels();
        }
        // guhpad: "Mijn verhaal" is a normal option, the first one next to the places of every tab (a tab that starts with
        // a subheading gets it on a row of its own above that heading)
        boolean mijnVerhaal = c.kopjes().isEmpty() || c.kopjes().get(0).id() != null;
        if (mijnVerhaal) {
            out.add(new Paar(SuperkompasItem.DOEL, null));
        }
        for (SuperkompasItem.Kopje k : c.kopjes()) {
            // (bbq2: what Guhdalfs sluier still hides for this player is not listed)
            List<String> s = new ArrayList<>(k.structures().stream().filter(id -> !nl.juiced.guhs.feature.verhaal.VerhaalSync.Client.verborgen(id)).toList());
            if (!mijnVerhaal) {
                s.add(0, SuperkompasItem.DOEL);
                mijnVerhaal = true;
            }
            if (s.isEmpty()) {
                continue;
            }
            if (k.id() != null) {
                out.add(new Kopje(k.naam()));
            }
            for (int i = 0; i < s.size(); i += 2) {
                out.add(new Paar(s.get(i), i + 1 < s.size() ? s.get(i + 1) : null));
            }
        }
        return out;
    }

    /**
     * 1.4.1: the rows of the tab Verhalen: "Mijn verhaal" on top, then per world of the Guhpad its heading, what it still
     * asks when it is locked, and its places (see the class text).
     */
    private List<GidsLijst.Regel> verhalenRegels() {
        List<GidsLijst.Regel> out = new ArrayList<>();
        out.add(new Paar(SuperkompasItem.DOEL, null));
        GuhpadPayloads.Stand stand = GuhpadPayloads.Client.stand();
        for (KompasVerhalen.Groep groep : KompasVerhalen.groepen(stand, nl.juiced.guhs.feature.verhaal.VerhaalSync.Client::verborgen)) {
            out.add(new WereldKop(groep, stand));
            if (groep.wereld() == Wereld.ECHT || isGevouwen(groep.wereld())) {
                continue;
            }
            if (!groep.mist().isEmpty()) {
                out.add(new Tekst(Component.translatable("gui.guhs.guhpad.nog_nodig", GuhpadTab.lijst(groep.mist())), 0xFFF0A8C0));
            }
            for (KompasVerhalen.Blok blok : groep.blokken()) {
                if (blok.kop() != null) {
                    out.add(new Kopje(Component.translatable("gui.guhs.guhpad.verhaal." + blok.kop())));
                }
                List<KompasVerhalen.Plek> plekken = blok.plekken();
                for (KompasVerhalen.Plek plek : plekken) {
                    if (plek.geheim()) {
                        geheim.add(plek.structuur());
                    }
                    if (plek.verhaal() != null) {
                        verhaalVan.put(plek.structuur(), plek.verhaal());
                    }
                }
                for (int i = 0; i < plekken.size(); i += 2) {
                    out.add(new Paar(plekken.get(i).structuur(), i + 1 < plekken.size() ? plekken.get(i + 1).structuur() : null));
                }
            }
            if (groep.blokken().isEmpty()) {
                out.add(new Tekst(Component.translatable("gui.guhs.guhpad.leeg").withStyle(ChatFormatting.ITALIC), ZACHT));
            }
        }
        return out;
    }

    private static boolean isGevouwen(Wereld w) {
        return GEVOUWEN.getOrDefault(w, false);
    }

    /** (AutoCheck) folds a world of the tab Verhalen open or shut. */
    public void vouw(Wereld w, boolean dicht) {
        GEVOUWEN.put(w, dicht);
        lijst.zet(regels());
    }

    /** (AutoCheck) scrolls the list this many screenfuls down from the top. */
    public void scrollLijst(double schermen) {
        lijst.scrollNaar(schermen * (H - LIST_TOP - 34));
    }

    private void kies(String id) {
        ClientPacketDistributor.sendToServer(new MaagPayloads.SuperkompasChoice(hand == InteractionHand.MAIN_HAND, id));
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.LODESTONE_COMPASS_LOCK, 1.2f));
        onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, GOUD);
        g.fill(left, top, left + W, top + H, 0xE8301A26);
        g.fill(left, top, left + W, top + TABS_Y + GidsTabs.H, 0xF0241320);
        g.fill(left, top + TABS_Y + GidsTabs.H, left + W, top + TABS_Y + GidsTabs.H + 1, GOUD);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, LICHT);
        List<SuperkompasItem.Category> tabs = nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.tabs();   // biomes3: the categories and the tab Biomes
        List<ItemStack> icons = tabs.stream().map(SuperkompasItem.Category::icoon).toList();   // biomes3
        GidsTabs.teken(g, tabsX(), top + TABS_Y, icons, tab, mouseX, mouseY, GidsTabs.SUPERKOMPAS,
                nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.tabW(tabs.size()), nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.tabGap(tabs.size()));   // biomes3
        // the category: its name and what it is about
        SuperkompasItem.Category c = tabs.get(tab);   // biomes3
        int y = top + TABS_Y + GidsTabs.H + 5;
        g.text(font, c.naam().copy().withStyle(ChatFormatting.BOLD), left + 10, y, GOUD, false);
        GidsTekst.passend(g, Component.translatable("gui.guhs.superkompas." + c.id() + ".tooltip"), left + 10, y + 11, W - 20, 0.75f, ZACHT, false);
        lijst.teken(g, mouseX, mouseY, GOUD, 0x40F7D27A);
        // (wrapped at 0.75 so it never runs under the Done button)
        GidsTekst.alinea(g, Component.translatable("gui.guhs.superkompas.pick"), left + 10, top + H - 22, W - 20 - 92, 0.75f, ZACHT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        List<SuperkompasItem.Category> tabs = nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.tabs();   // biomes3
        int hover = GidsTabs.onder(tabsX(), top + TABS_Y, tabs.size(), mouseX, mouseY, nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.tabW(tabs.size()), nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.tabGap(tabs.size()));   // biomes3
        if (hover >= 0) {
            SuperkompasItem.Category c = tabs.get(hover);   // biomes3
            g.setComponentTooltipForNextFrame(font, List.of(c.naam().copy().withStyle(ChatFormatting.BOLD),
                    Component.translatable("gui.guhs.superkompas." + c.id() + ".tooltip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else {
            List<Component> tip = lijst.tip(mouseX, mouseY);
            if (tip != null) {
                g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        int tabs = SuperkompasItem.CATEGORIES.size() + 1;   // biomes3: + the tab Biomes
        int hit = GidsTabs.onder(tabsX(), top + TABS_Y, tabs, mouseX, mouseY, nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.tabW(tabs), nl.juiced.guhs.feature.bio.kompas.client.BiomesTab.tabGap(tabs));   // biomes3
        if (hit >= 0 && button == 0) {
            if (hit != tab) {
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.2f));
                showTab(hit);
            }
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        return lijst.klik(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return lijst.wiel(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        return lijst.sleep(mouseY) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        lijst.los();
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- the rows ---------------------------------------------------------------------------------------------------------

    /** (1.3.1) Been there: the place itself was found, or (saves from before 1.3.1) its minigame building was. */
    private static boolean isBezocht(String id, @Nullable SpelGroepen.Groep groep) {
        return SpelGroepen.Client.structuurBezocht(id) || groep != null && SpelGroepen.Client.bezocht(groep.id());
    }

    /** A subheading (gold, with a line). */
    private record Kopje(Component naam) implements GidsLijst.Regel {
        @Override
        public int hoogte() {
            return KOPJE_H;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, boolean hover) {
            int tw = GidsTekst.passend(g, Component.literal("✦ ").append(naam).withStyle(ChatFormatting.BOLD), x + 1, y + 4, w - 20, 0.875f, GOUD, false);
            g.fill(x + tw + 6, y + 8, x + w - 1, y + 9, 0x80F7D27A);
        }
    }

    /** 1.4.1 (the tab Verhalen): the heading of a world of the Guhpad; click to fold its places open or shut. Dark, like this menu. */
    private final class WereldKop implements GidsLijst.Regel {
        private final KompasVerhalen.Groep groep;
        private final GuhpadPayloads.Stand stand;
        private final ItemStack icoon;

        WereldKop(KompasVerhalen.Groep groep, GuhpadPayloads.Stand stand) {
            this.groep = groep;
            this.stand = stand;
            this.icoon = GuhpadTab.stack(groep.wereld().icoon());
        }

        private boolean echt() {
            return groep.wereld() == Wereld.ECHT;
        }

        @Override
        public int hoogte() {
            return 22;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, boolean hover) {
            Wereld wereld = groep.wereld();
            boolean echt = echt();
            g.fill(x, y + 2, x + w, y + 20, echt ? 0xFF2A1C3A : hover ? 0xFF6A3E54 : 0xFF54304A);
            g.fill(x, y + 2, x + w, y + 3, 0x20FFFFFF);
            g.fill(x, y + 19, x + w, y + 20, echt ? 0xFF6A4E9A : GOUD);
            int kleur = echt ? 0xFFD8C8F0 : GOUD;
            if (echt) {
                g.text(font, Component.literal("?").withStyle(ChatFormatting.BOLD), x + 17, y + 7, 0xFFB090E0, false);
            } else {
                GidsTekst.schaal(g, Component.literal(isGevouwen(wereld) ? "\u25B6" : "\u25BC"), x + 4, y + 8, 0.75f, kleur, false);
                g.item(icoon, x + 13, y + 3);
            }
            // on the right, as in the Guhdex: locked, or how many of its big stories are done
            int rechts = x + w - 5;
            if (groep.opSlot()) {
                GuhpadTab.slotje(g, rechts - 7, y + 7, echt ? 0xFFB090E0 : 0xFFF0A8C0, echt ? 0xFF2A1C3A : 0xFF54304A);
                Component slot = Component.translatable("gui.guhs.guhpad.op_slot");
                GidsTekst.schaal(g, slot, rechts - 10, y + 9, 0.625f, echt ? 0xFFB090E0 : 0xFFF0A8C0, true);
                rechts -= 14 + Math.round(font.width(slot) * 0.625f);
            } else if (!stand.eisen().isEmpty()) {
                List<GuhpadPayloads.Verhaal> verhalen = stand.verhalen(wereld);
                long klaar = verhalen.stream().filter(GuhpadPayloads.Verhaal::klaar).count();
                boolean alles = GuhpadTab.allesGedaan(stand, wereld);
                Component telling = verhalen.isEmpty() ? Component.translatable("gui.guhs.guhpad.open")
                        : Component.literal((alles ? "\u2714 " : "") + klaar + " / " + verhalen.size() + " \u2605");
                GidsTekst.schaal(g, telling, rechts, y + 8, 0.75f, alles || verhalen.isEmpty() ? 0xFF68D88A : LICHT, true);
                rechts -= 6 + Math.round(font.width(telling) * 0.75f);
            }
            GidsTekst.passend(g, wereld.naam().withStyle(ChatFormatting.BOLD), x + 33, y + 7, rechts - x - 35, 1f, kleur, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            if (echt()) {
                return true;   // (nothing to fold open: locked for everybody)
            }
            GEVOUWEN.put(groep.wereld(), !isGevouwen(groep.wereld()));
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.2f));
            lijst.zet(regels());
            return true;
        }

        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            List<Component> tip = new ArrayList<>(GuhpadTab.uitleg(stand, groep.wereld()));
            if (!echt()) {
                tip.add(Component.translatable(isGevouwen(groep.wereld()) ? "gui.guhs.guhpad.kompas.vouw_open" : "gui.guhs.guhpad.kompas.vouw_dicht")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
            return tip;
        }
    }

    /** 1.4.1 (the tab Verhalen): a small wrapped line under a world's heading (what it still asks; "no stories here yet"). */
    private final class Tekst implements GidsLijst.Regel {
        private final Component tekst;
        private final int kleur;

        Tekst(Component tekst, int kleur) {
            this.tekst = tekst;
            this.kleur = kleur;
        }

        @Override
        public int hoogte() {
            return GidsTekst.hoogte(tekst, lijst.rijBreedte() - 12, 0.75f) + 4;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, boolean hover) {
            GidsTekst.alinea(g, tekst, x + 6, y + 2, w - 12, 0.75f, kleur);
        }
    }

    /** guhpad: is this option "Mijn verhaal" (SuperkompasItem.DOEL: no place, the compass follows your story by itself)? */
    private static boolean isMijnVerhaal(String id) {
        return SuperkompasItem.DOEL.equals(id);
    }

    /** guhpad: the questline "Mijn verhaal" follows now (null: none, it looks for the nearest story you have not done). */
    @Nullable
    private static Component gevolgd() {
        String volg = nl.juiced.guhs.feature.verhaal.VerhaalSync.Client.volg();
        return volg.isEmpty() ? null : Component.translatable("gui.guhs.verhalen." + volg + ".naam");
    }

    /** Two places side by side (the right one may be missing). */
    private final class Paar implements GidsLijst.Regel {
        private final String links;
        @Nullable
        private final String rechts;

        Paar(String links, @Nullable String rechts) {
            this.links = links;
            this.rechts = rechts;
        }

        String links() {
            return links;
        }

        @Nullable
        String rechts() {
            return rechts;
        }

        @Override
        public int hoogte() {
            return KNOP_H + 3;
        }

        private int bw(int w) {
            return (w - 4) / 2;
        }

        @Nullable
        private String onder(double mx, double my, int x, int y, int w) {
            if (my < y + 1 || my >= y + 1 + KNOP_H) {
                return null;
            }
            if (mx >= x && mx < x + bw(w)) {
                return links;
            }
            if (rechts != null && mx >= x + bw(w) + 4 && mx < x + w) {
                return rechts;
            }
            return null;
        }

        @Override
        public void teken(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my, boolean hover) {
            knop(g, links, x, y + 1, bw(w), hover && links.equals(onder(mx, my, x, y, w)));
            if (rechts != null) {
                knop(g, rechts, x + bw(w) + 4, y + 1, bw(w), hover && rechts.equals(onder(mx, my, x, y, w)));
            }
        }

        private void knop(GuiGraphicsExtractor g, String id, int x, int y, int w, boolean on) {
            if (geheim.contains(id)) {
                // 1.4.1: a place the player's story has not reached: "???" with a lock, not a button
                g.fill(x, y, x + w, y + KNOP_H, 0xFF4A3442);
                g.fill(x + 1, y + 1, x + w - 1, y + KNOP_H - 1, 0xFF2E1C28);
                GuhpadTab.slotje(g, x + 6, y + 6, ZACHT, 0xFF2E1C28);
                g.text(font, Component.translatable("gui.guhs.guhpad.kompas.geheim"), x + 20, y + 6, ZACHT, false);
                return;
            }
            boolean gekozen = id.equals(chosen);
            int rand = gekozen ? GOUD : on ? 0xFFE8B8CC : 0xFF6A4A5A;
            g.fill(x, y, x + w, y + KNOP_H, rand);
            g.fill(x + 1, y + 1, x + w - 1, y + KNOP_H - 1, on ? 0xFF6A3E54 : gekozen ? 0xFF5A3A2A : 0xFF462838);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x20FFFFFF);
            boolean verhaal = isMijnVerhaal(id);
            SpelGroepen.Groep groep = verhaal ? null : GidsData.groepVanStructuur(id);
            int tx = x + 5;
            if (groep != null || verhaal) {
                g.pose().pushMatrix();
                g.pose().translate(x + 3, y + 3);
                g.pose().scale(0.875f, 0.875f);
                g.item(verhaal ? new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK) : groep.icoon().get(), 0, 0);
                g.pose().popMatrix();
                tx = x + 20;
            }
            int rechtsRuimte = 4;
            if (!verhaal && isBezocht(id, groep)) {
                GidsTekst.schaal(g, Component.literal("✔"), x + w - 4, y + 6, 1f, 0xFF68D88A, true);
                rechtsRuimte = 14;
            }
            Component label = Component.translatable("structure.guhs." + id);
            if (gekozen) {
                label = Component.literal("▶ ").append(label);
            }
            GidsTekst.passend(g, label, tx, y + 6, x + w - rechtsRuimte - tx, 1f, gekozen ? GOUD : LICHT, false);
        }

        @Override
        public boolean klik(double mx, double my, int x, int y, int w) {
            String id = onder(mx, my, x, y, w);
            if (id != null) {
                if (!geheim.contains(id)) {   // (1.4.1: a "???" place can not be chosen)
                    kies(id);
                }
                return true;
            }
            return false;
        }

        @Nullable
        @Override
        public List<Component> tip(double mx, double my, int x, int y, int w) {
            String id = onder(mx, my, x, y, w);
            if (id == null) {
                return null;
            }
            List<Component> out = new ArrayList<>();
            String verhaal = verhaalVan.get(id);
            if (geheim.contains(id)) {
                // 1.4.1: no name, no description: only which story it is a part of
                out.add(Component.translatable("gui.guhs.guhpad.kompas.geheim").withStyle(ChatFormatting.BOLD));
                out.add(Component.translatable("gui.guhs.guhpad.kompas.geheim.uitleg",
                        Component.translatable("gui.guhs.guhpad.verhaal." + verhaal)).withStyle(ChatFormatting.GRAY));
                return out;
            }
            out.add(Component.translatable("structure.guhs." + id).withStyle(ChatFormatting.BOLD));
            if (verhaal != null) {
                out.add(Component.translatable("gui.guhs.guhpad.kompas.verhaal", Component.translatable("gui.guhs.guhpad.verhaal." + verhaal))
                        .withStyle(ChatFormatting.GOLD));
            }
            if (isMijnVerhaal(id)) {
                // guhpad: the short explanation, and what it follows now
                out.add(Component.translatable("gui.guhs.guhpad.kompas.uitleg").withStyle(ChatFormatting.GRAY));
                Component volg = gevolgd();
                out.add(volg == null ? Component.translatable("gui.guhs.guhpad.kompas.dichtstbij").withStyle(ChatFormatting.GOLD)
                        : Component.translatable("gui.guhs.guhpad.kompas.volgt", volg).withStyle(ChatFormatting.GOLD));
                if (id.equals(chosen)) {
                    out.add(Component.translatable("gui.guhs.superkompas.zoekt_al").withStyle(ChatFormatting.GOLD));
                }
                return out;
            }
            out.add(Component.translatable("structure.guhs." + id + ".tooltip").withStyle(ChatFormatting.GRAY));
            SpelGroepen.Groep groep = GidsData.groepVanStructuur(id);
            // (1.3.1) every place tells whether you have been there, not only the minigame buildings
            boolean bezocht = isBezocht(id, groep);
            out.add(Component.translatable(bezocht ? "gui.guhs.spelgroep.bezocht" : "gui.guhs.spelgroep.niet_bezocht")
                    .withStyle(bezocht ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            if (id.equals(chosen)) {
                out.add(Component.translatable("gui.guhs.superkompas.zoekt_al").withStyle(ChatFormatting.GOLD));
            }
            return out;
        }
    }
}
