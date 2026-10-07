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
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.network.MaagPayloads;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The super compass menu (2.9, the gids slice): a row of icon tabs on top (one per category, like the creative
 * inventory; the name on hover), under it the chosen category's name and what it's about, and a scrolling list of its
 * places in two columns, with subheadings where a category has them (Minigames: Klassiekers, Knuffeldal, De Grote
 * Guhspelen). A minigame place shows its game's icon; every place shows a green tick once you've been there (1.3.1). Click a place: that's what
 * the compass looks for.
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
        List<GidsLijst.Regel> out = new ArrayList<>();
        SuperkompasItem.Category c = SuperkompasItem.CATEGORIES.get(tab);
        for (SuperkompasItem.Kopje k : c.kopjes()) {
            if (k.id() != null) {
                out.add(new Kopje(k.naam()));
            }
            List<String> s = k.structures();
            for (int i = 0; i < s.size(); i += 2) {
                out.add(new Paar(s.get(i), i + 1 < s.size() ? s.get(i + 1) : null));
            }
        }
        return out;
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
            boolean gekozen = id.equals(chosen);
            int rand = gekozen ? GOUD : on ? 0xFFE8B8CC : 0xFF6A4A5A;
            g.fill(x, y, x + w, y + KNOP_H, rand);
            g.fill(x + 1, y + 1, x + w - 1, y + KNOP_H - 1, on ? 0xFF6A3E54 : gekozen ? 0xFF5A3A2A : 0xFF462838);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x20FFFFFF);
            SpelGroepen.Groep groep = GidsData.groepVanStructuur(id);
            int tx = x + 5;
            if (groep != null) {
                g.pose().pushMatrix();
                g.pose().translate(x + 3, y + 3);
                g.pose().scale(0.875f, 0.875f);
                g.item(groep.icoon().get(), 0, 0);
                g.pose().popMatrix();
                tx = x + 20;
            }
            int rechtsRuimte = 4;
            if (isBezocht(id, groep)) {
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
                kies(id);
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
            out.add(Component.translatable("structure.guhs." + id).withStyle(ChatFormatting.BOLD));
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
