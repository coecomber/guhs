package nl.juiced.guhs.client.screen;

import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.KnusPayloads;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;

import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The Guhdex, the all-in-one guide (2.9: icon tabs along the top like the creative inventory's, the name on hover, the
 * same look as the Superkompas; see feature.gids):
 * <ul>
 *   <li><b>Guhs</b>: one page per guh (variant) or guh character. Seen = the page is filled in, tamed = a star. Unseen
 *   guhs are a dark silhouette with "???". The rewards for the milestones are claimed at the bottom.</li>
 *   <li><b>Knus</b>: the progress of the cozy 2.8 features ({@link KnusVoortgang}): an overview of every section with a
 *   bar; a section shows its milestones (bar, claim) and its collection pages; a collection page shows its entries
 *   (found ones with icon and name, the rest "?"), click one for its text.</li>
 *   <li><b>Minigames</b> (2.9, replaces the Highscores tab): every minigame building per era, folding open to where it is,
 *   whether you've been there, its clothing and per level / track / event your best and the server record
 *   (feature.gids.client.GidsMinigamesTab).</li>
 *   <li><b>Kleding</b> (2.9): every clothing piece per source, grey = locked, colourful with a green border = unlocked
 *   (feature.gids.client.GidsKledingTab).</li>
 *   <li><b>Mijn guhs</b> (2.10): your own tamed guhs, each with its dagboekje: hearts, favourites, friends, chores, where
 *   it is, statistics, eerste keren and wist-je-datjes (feature.band.client.MijnGuhsTab).</li>
 *   <li><b>Verhalen</b>: every questline with your step, what to do now, whom to visit, what you need and the rewards
 *   (feature.gids.client.GidsVerhalenTab, data feature.gids.VerhalenVoortgang).</li>
 *   <li><b>Titels</b> (1.2.6): every title; pick the one that shows behind your name (feature.titels.client.GidsTitelsTab).</li>
 * </ul>
 * The Minigames and Kleding tabs scroll (mouse wheel or the bar); what is folded open is remembered while the game runs.
 */
public class GuhDexScreen extends Screen {
    private static final int W = 300, H = 210;
    private MaagPayloads.GuhDexData data;
    private int page;
    private int left, top;
    private GuhEntity preview;
    private nl.juiced.guhs.entity.GuhNpcEntity character;
    /** Pages of creatures that aren't guhs or guh characters (the kikkerguh...): the entity with the page's id. */
    private final java.util.Map<GuhVariant, net.minecraft.world.entity.LivingEntity> creatures = new java.util.EnumMap<>(GuhVariant.class);
    /** The Highscores page (sent by the server when the Guhdex opens and whenever a score changes). */
    public static java.util.List<MaagPayloads.HighscoreRow> highscores = java.util.List.of();
    /** The tab bar: the tabs start at (left + TABS_X, top + TABS_Y); the page below it starts at top + BODY. */
    private static final int TABS_X = 6, TABS_Y = 2, BODY = 25, LIST_TOP = 28;
    /** 1.2.6: seven tabs, so each a little narrower than the Superkompas's (the title still fits next to them). */
    private static final int TAB_W = 22, TAB_GAP = 1;
    private final nl.juiced.guhs.feature.gids.client.GidsLijst lijst = new nl.juiced.guhs.feature.gids.client.GidsLijst();
    /** 2.10: the Mijn guhs tab (its own list, preview and pages). */
    private final nl.juiced.guhs.feature.band.client.MijnGuhsTab mijnGuhs = new nl.juiced.guhs.feature.band.client.MijnGuhsTab();
    /** The Verhalen tab (its list and the page of a questline). */
    private final nl.juiced.guhs.feature.gids.client.GidsVerhalenTab verhalen = new nl.juiced.guhs.feature.gids.client.GidsVerhalenTab();
    /** 1.2.6: the Titels tab (pick the title behind your name). */
    private final nl.juiced.guhs.feature.titels.client.GidsTitelsTab titels = new nl.juiced.guhs.feature.titels.client.GidsTitelsTab();
    /**
     * Layout of a guh page: the picture (with the page arrows and the page number under it) on the left, the text column
     * on the right down to TEXT_BOTTOM, then the rewards. Everything stays inside its own box: long texts are wrapped
     * and, when they still don't fit, drawn smaller (see {@link #fitText}).
     */
    private static final int PIC_TOP = 28, PIC_BOTTOM = 118, NAV_Y = 121, TEXT_X = 130, TEXT_W = W - TEXT_X - 8,
            TEXT_BOTTOM = 137, REWARDS_Y = 142, MILESTONE_Y = 154;

    /** The four tabs (2.9): icon (item id, vanilla stand-in) and name (gui.guhs.guhdex.tab.&lt;id&gt;). */
    public enum Tab {
        GUHS("guhs:guh_spawn_egg", net.minecraft.world.item.Items.PINK_DYE),
        KNUS("guhs:knuffel_normal", net.minecraft.world.item.Items.PINK_BED),
        MINIGAMES("guhs:discomunt", net.minecraft.world.item.Items.JUKEBOX),
        KLEDING("guhs:party_hat", net.minecraft.world.item.Items.LEATHER_HELMET),
        /** 2.10: your own tamed guhs, each with its dagboekje (feature.band.client.MijnGuhsTab). */
        MIJN_GUHS("guhs:guhhuisje_klein", net.minecraft.world.item.Items.RED_BED),
        /** Every questline: where you are, what to do now, whom to visit (feature.gids.client.GidsVerhalenTab). */
        VERHALEN("minecraft:writable_book", net.minecraft.world.item.Items.WRITABLE_BOOK),
        /** 1.2.6: your titles; pick the one behind your name (feature.titels.client.GidsTitelsTab). */
        TITELS("minecraft:name_tag", net.minecraft.world.item.Items.NAME_TAG);

        private final String icon;
        private final net.minecraft.world.item.Item standIn;

        Tab(String icon, net.minecraft.world.item.Item standIn) {
            this.icon = icon;
            this.standIn = standIn;
        }

        public String id() {
            return lower(this);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.guhdex.tab." + id());
        }

        public ItemStack icoon() {
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(icon));
            return new ItemStack(item == net.minecraft.world.item.Items.AIR ? standIn : item);
        }
    }

    /** Which tab (remembered while the game runs). */
    private static Tab tab = Tab.GUHS;
    /** The scroll position per list tab (remembered while the game runs). */
    private static final java.util.EnumMap<Tab, Double> SCROLL = new java.util.EnumMap<>(Tab.class);
    /** The Knus tab: null = the overview, else a section; with a collection open inside it. */
    private static String knusOnderdeel;
    private static String knusVerzameling;
    private static String knusGekozen;
    private static int knusPage;
    private static final int KNUS_TOP = 27, KNUS_ROW = 12, MIJLPAAL_ROW = 19, MIJLPALEN_PER_PAGE = 4, CELL = 20, CELLS_PER_ROW = 13,
            CELL_ROWS = 4;

    public GuhDexScreen(MaagPayloads.GuhDexData data) {
        super(Component.translatable("gui.guhs.guhdex.title"));
        this.data = data;
    }

    public void update(MaagPayloads.GuhDexData newData) {
        this.data = newData;
        rebuildWidgets();
    }

    /** The Knus data changed (a claim, a new entry): redraw the buttons. */
    public void knusChanged() {
        if (tab == Tab.KNUS) {
            rebuildWidgets();
        }
    }

    /** (AutoCheck) which tab is showing. */
    public static Tab tab() {
        return tab;
    }

    /** (AutoCheck) switch tabs. */
    public void showTab(Tab newTab) {
        kies(newTab);
    }

    /** 2.10: new Mijn guhs data arrived while that tab shows. */
    public void mijnGuhsVernieuwd() {
        if (tab == Tab.MIJN_GUHS) {
            rebuildWidgets();
        }
    }

    /** New story progress arrived: redraw when the Verhalen tab shows. */
    public void verhalenVernieuwd() {
        if (tab == Tab.VERHALEN) {
            verhalen.bewaarScroll();
            rebuildWidgets();
        }
    }

    /** (AutoCheck) the Verhalen tab. */
    public nl.juiced.guhs.feature.gids.client.GidsVerhalenTab verhalenTab() {
        return verhalen;
    }

    /** 2.10.1: a guh's dagboekje should open (the Guh menu's "Dagboekje"): switch to "Mijn guhs", its page, scrolled to the top. */
    public void naarDagboek() {
        if (tab == Tab.MINIGAMES || tab == Tab.KLEDING) {
            SCROLL.put(tab, lijst.scroll());
        }
        tab = Tab.MIJN_GUHS;
        rebuildWidgets();   // (init takes the focus: MijnGuhsTab.open, page scroll 0)
    }

    private void kies(Tab newTab) {
        if (tab == Tab.MINIGAMES || tab == Tab.KLEDING) {
            SCROLL.put(tab, lijst.scroll());
        }
        if (tab == Tab.MIJN_GUHS) {
            mijnGuhs.bewaarScroll();
        }
        if (tab == Tab.VERHALEN) {
            verhalen.bewaarScroll();
        }
        tab = newTab;
        if (tab == Tab.MIJN_GUHS) {
            nl.juiced.guhs.feature.band.client.MijnGuhsTab.vraag();   // (fresh hearts and places)
        }
        if (tab == Tab.VERHALEN) {
            nl.juiced.guhs.feature.gids.client.VerhalenCache.vraag(); // (fresh steps and counts)
        }
        if (tab == Tab.TITELS) {
            nl.juiced.guhs.feature.titels.client.TitelsCache.vraag(); // (titles earned since the Guhdex opened)
        }
        rebuildWidgets();
    }

    /** (AutoCheck) folds everything of the Minigames or Kleding tab open (or shut). */
    public void openAlles(boolean open) {
        if (tab == Tab.MINIGAMES) {
            nl.juiced.guhs.feature.gids.client.GidsMinigamesTab.alles(open);
        } else if (tab == Tab.KLEDING) {
            nl.juiced.guhs.feature.gids.client.GidsKledingTab.alles(open);
        }
        rebuildWidgets();
    }

    /** (AutoCheck) how many screenfuls the list of this tab has. */
    public int lijstPaginas() {
        return (tab == Tab.MINIGAMES || tab == Tab.KLEDING) ? 1 + (int) Math.ceil(lijst.max() / (double) LIJST_STAP) : 1;
    }

    /** (AutoCheck) scrolls the list to a screenful. */
    public void lijstPagina(int p) {
        lijst.scrollNaar(p * LIJST_STAP);
        SCROLL.put(tab, lijst.scroll());
    }

    /** (AutoCheck) one screenful of the list, minus a little overlap. */
    private static final int LIJST_STAP = H - LIST_TOP - 6 - 24;

    private GuhVariant current() {
        return GuhDex.ENTRIES.get(page);
    }

    private boolean seen(GuhVariant v) {
        return data.seen().contains(v.id());
    }

    private boolean tamed(GuhVariant v) {
        return data.tamed().contains(v.id());
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        preview = ModEntities.GUH.get().create(minecraft.level, EntitySpawnReason.TRIGGERED);
        character = ModEntities.GUH_NPC.get().create(minecraft.level, EntitySpawnReason.TRIGGERED);
        if (tab != Tab.MIJN_GUHS && nl.juiced.guhs.feature.band.client.MijnGuhsCache.heeftFocus()) {
            tab = Tab.MIJN_GUHS;   // 2.10: the menu's "Dagboekje" opens a guh's page
        }
        // 1.2.0: the language of the Guhs texts (Auto / NL / EN), under the book at the right (above it when the window is low)
        int taalY = top + H + 18 <= height ? top + H + 3 : Math.max(0, top - 17);
        addRenderableWidget(nl.juiced.guhs.client.GuhsTaal.knop(left + W, taalY, 84, 14));
        // (2.9) the tabs are drawn and clicked by hand (icon tabs, see renderBackground / mouseClicked)
        switch (tab) {
            case MINIGAMES, KLEDING -> initLijst();
            case KNUS -> initKnus();
            case MIJN_GUHS -> mijnGuhs.init(font, left, top, W, H, this::addRenderableWidget, this::rebuildWidgets);
            case VERHALEN -> verhalen.init(left, top, W, H, this::addRenderableWidget, this::rebuildWidgets);
            case TITELS -> titels.init(left, top, W, H);
            default -> initGuhs();
        }
    }

    /** The Minigames or Kleding tab: a scrolling list of rows that fold open and shut. */
    private void initLijst() {
        lijst.plaats(left + 8, top + LIST_TOP, W - 14, H - LIST_TOP - 6);
        vulLijst();
        lijst.scrollNaar(SCROLL.getOrDefault(tab, 0.0));
    }

    private void vulLijst() {
        lijst.zet(tab == Tab.KLEDING ? nl.juiced.guhs.feature.gids.client.GidsKledingTab.regels(this::vulLijst)
                : nl.juiced.guhs.feature.gids.client.GidsMinigamesTab.regels(this::vulLijst));
    }

    private void initGuhs() {
        // the page arrows sit under the picture, with the page number between them (clear of the text and the rewards)
        addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(left + 10, top + NAV_Y, 20, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(left + 100, top + NAV_Y, 20, 14).build());
        for (int i = 0; i < GuhDex.MILESTONES.size(); i++) {
            GuhDex.Milestone m = GuhDex.MILESTONES.get(i);
            int index = i;
            boolean claimed = data.claimed().contains(i);
            boolean reached = GuhDex.geteldIds(data.seen()) >= m.seen() && data.tamed().size() >= m.tamed();
            Button b = Button.builder(Component.translatable(claimed ? "gui.guhs.guhdex.claimed" : "gui.guhs.guhdex.claim"),
                    btn -> ClientPacketDistributor.sendToServer(new MaagPayloads.GuhDexClaim(index))).bounds(left + W - 70, top + MILESTONE_Y + i * 12, 60, 11).build();
            b.active = reached && !claimed;
            addRenderableWidget(b);
        }
    }

    private void turn(int by) {
        page = Math.floorMod(page + by, GuhDex.ENTRIES.size());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFF7B6CB);
        g.fill(left, top, left + W, top + H, 0xF0FFF4F8);
        // the tab bar: a slightly darker band with the icon tabs, the title (and the tab's name) next to them
        g.fill(left, top, left + W, top + BODY - 1, 0xFFFBE0EA);
        g.fill(left, top + BODY - 1, left + W, top + BODY, 0xFFD27A9C);
        List<ItemStack> icons = java.util.Arrays.stream(Tab.values()).map(Tab::icoon).toList();
        nl.juiced.guhs.feature.gids.client.GidsTabs.teken(g, left + TABS_X, top + TABS_Y, icons, tab.ordinal(), mouseX, mouseY,
                nl.juiced.guhs.feature.gids.client.GidsTabs.GUHDEX, TAB_W, TAB_GAP);
        int tx = left + TABS_X + nl.juiced.guhs.feature.gids.client.GidsTabs.breedte(Tab.values().length, TAB_W, TAB_GAP) + 6;
        Component bold = title.copy().withStyle(ChatFormatting.BOLD);
        g.text(font, bold, tx, top + 9, 0xFF7A2848, false);
        scaled(g, Component.literal("· ").append(tab.naam()), tx + font.width(bold) + 4, top + 10, 0.875f, 0xFFB0708A, false);
        switch (tab) {
            case MINIGAMES, KLEDING -> lijst.teken(g, mouseX, mouseY, 0xFFD27A9C, 0x30D27A9C);
            case KNUS -> renderKnus(g, mouseX, mouseY);
            case MIJN_GUHS -> mijnGuhs.teken(g, mouseX, mouseY);
            case VERHALEN -> verhalen.teken(g, mouseX, mouseY);
            case TITELS -> titels.teken(g, mouseX, mouseY);
            default -> renderGuhs(g, mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        // tooltips last, above everything: the tab names, and the rows of the list tabs
        int hover = nl.juiced.guhs.feature.gids.client.GidsTabs.onder(left + TABS_X, top + TABS_Y, Tab.values().length, mouseX, mouseY, TAB_W, TAB_GAP);
        if (hover >= 0) {
            g.setTooltipForNextFrame(font, Tab.values()[hover].naam(), mouseX, mouseY);
        } else if (tab == Tab.MINIGAMES || tab == Tab.KLEDING || tab == Tab.MIJN_GUHS || tab == Tab.VERHALEN || tab == Tab.TITELS) {
            List<Component> tip = tab == Tab.MIJN_GUHS ? mijnGuhs.tip(mouseX, mouseY) : tab == Tab.VERHALEN ? verhalen.tip(mouseX, mouseY)
                    : tab == Tab.TITELS ? titels.tip(mouseX, mouseY) : lijst.tip(mouseX, mouseY);
            if (tip != null && !tip.isEmpty()) {
                g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if ((tab == Tab.MINIGAMES || tab == Tab.KLEDING) && lijst.wiel(mouseX, mouseY, scrollY)) {
            SCROLL.put(tab, lijst.scroll());
            return true;
        }
        if (tab == Tab.MIJN_GUHS && mijnGuhs.wiel(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (tab == Tab.VERHALEN && verhalen.wiel(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (tab == Tab.TITELS && titels.wiel(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (tab == Tab.GUHS && mouseX >= left && mouseX < left + W && mouseY >= top + BODY && mouseY < top + H && scrollY != 0) {
            turn(scrollY > 0 ? -1 : 1);   // (the wheel turns the guh pages)
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if ((tab == Tab.MINIGAMES || tab == Tab.KLEDING) && lijst.sleep(mouseY)) {
            SCROLL.put(tab, lijst.scroll());
            return true;
        }
        if (tab == Tab.MIJN_GUHS && mijnGuhs.sleep(mouseX, mouseY, dragX, dragY)) {
            return true;
        }
        if (tab == Tab.VERHALEN && verhalen.sleep(mouseY)) {
            return true;
        }
        if (tab == Tab.TITELS && titels.sleep(mouseY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        lijst.los();
        mijnGuhs.los();
        verhalen.los();
        titels.los();
        return super.mouseReleased(event);
    }

    private void renderGuhs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        String count = GuhDex.geteldIds(data.seen()) + " / " + GuhDex.TELLEND.size();   // (2.10.1: without bonus pages)
        g.text(font, count, left + W - 8 - font.width(count), top + 9, 0xFF7A2848, false);
        GuhVariant v = current();
        boolean seen = seen(v);
        // the guh itself (a dark silhouette when not seen yet)
        g.fill(left + 10, top + PIC_TOP, left + 120, top + PIC_BOTTOM, seen ? 0x30F7B6CB : 0xFF2A1420);
        if (preview != null) {
            preview.setVariant(v);
            net.minecraft.world.entity.LivingEntity shown = preview;
            if (v.isCharacter() && v.npcKind() == null) {
                shown = creature(v);
            } else if (v.isCharacter() && character != null) {
                character.setKind(v.npcKind());
                shown = character;
            }
            if (seen && shown != null) {
                InventoryScreen.extractEntityInInventoryFollowsMouse(g, left + 10, top + PIC_TOP, left + 120, top + PIC_BOTTOM, 32, 0.0625f, mouseX, mouseY, shown);
            } else if (seen) {
                // 3.0: a page whose creature isn't in the game (yet): a big pink flower instead of a picture
                scaled(g, Component.literal("✿"), left + 65 - 12, top + (PIC_TOP + PIC_BOTTOM) / 2 - 14, 3f, 0xFFF7A8CC, false);
            } else {
                g.centeredText(font, "???", left + 65, top + (PIC_TOP + PIC_BOTTOM) / 2 - 4, 0xFF8A6A7A);
            }
        }
        int tx = left + TEXT_X, bottom = top + TEXT_BOTTOM;
        String id = v.id();
        Component display = !v.isCharacter() ? v.displayName()
                : v.npcKind() == null ? Component.translatable("entity.guhs." + v.id()) : Component.translatable("entity.guhs.guh_npc." + v.id());
        int y = top + PIC_TOP + 2;
        y += fitText(g, seen ? display.copy().withStyle(ChatFormatting.BOLD) : Component.literal("???"), tx, y, TEXT_W, 10, 0xFF3A1C30) + 2;
        if (tamed(v)) {
            y += fitText(g, Component.literal("★ ").append(Component.translatable("gui.guhs.guhdex.tamed")), tx, y, TEXT_W, 10, 0xFFD89A10) + 2;
        }
        if (seen && v == GuhVariant.ROOKGUH) {   // 2.10.1: your saved Rookguhs (was above the whole Guhdex)
            y += fitText(g, Component.literal("♥ ").append(Component.translatable("gui.guhs.guhdex.rookguhs",
                    nl.juiced.guhs.feature.spiesburcht.SpiesburchtStats.clientRookguhs)), tx, y, TEXT_W, 10, 0xFFC0407A) + 2;
        }
        y += 3;
        if (seen) {
            // the rarity: at most two lines, then the description gets all the room that's left
            y += fitText(g, Component.translatable("gui.guhs.guhdex.rarity." + id), tx, y, TEXT_W, 20, 0xFF7A2848) + 5;
            fitText(g, Component.translatable("gui.guhs.guhdex.info." + id), tx, y, TEXT_W, bottom - y, 0xFF5A3A4A);
        } else {
            fitText(g, Component.translatable("gui.guhs.guhdex.unknown"), tx, y, TEXT_W, bottom - y, 0xFF9A8090);
        }
        g.centeredText(font, (page + 1) + " / " + GuhDex.ENTRIES.size(), left + 65, top + NAV_Y + 3, 0xFF7A2848);
        // milestones
        g.fill(left + 10, top + REWARDS_Y - 4, left + W - 10, top + REWARDS_Y - 3, 0x60F7B6CB);
        g.text(font, Component.translatable("gui.guhs.guhdex.rewards"), left + 10, top + REWARDS_Y, 0xFF7A2848, false);
        for (int i = 0; i < GuhDex.MILESTONES.size(); i++) {
            GuhDex.Milestone m = GuhDex.MILESTONES.get(i);
            Component text = m.tamed() > 0 ? Component.translatable("gui.guhs.guhdex.milestone_tamed", m.seen(), m.tamed(), Component.translatable(m.reward().get().getDescriptionId()))
                    : Component.translatable("gui.guhs.guhdex.milestone", m.seen(), Component.translatable(m.reward().get().getDescriptionId()));
            fitText(g, text, left + 10, top + MILESTONE_Y + 2 + i * 12, W - 84, 10, 0xFF5A3A4A);
        }
    }

    // =================================================================================================================
    // The Knus tab
    // =================================================================================================================

    private void initKnus() {
        if (knusOnderdeel != null && KnusVoortgang.onderdelen().stream().noneMatch(o -> o.id().equals(knusOnderdeel))) {
            knusOnderdeel = null;
        }
        if (knusOnderdeel == null) {
            return;   // the overview: rows are clicked (mouseClicked)
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knus.terug"), b -> {
            if (knusVerzameling != null) {
                knusVerzameling = null;
                knusGekozen = null;
            } else {
                knusOnderdeel = null;
            }
            knusPage = 0;
            rebuildWidgets();
        }).bounds(left + 8, top + 27, 44, 14).build());
        if (knusVerzameling != null) {
            return;   // the entries are clicked (mouseClicked)
        }
        List<KnusVoortgang.Mijlpaal> mijlpalen = KnusVoortgang.mijlpalen(knusOnderdeel);
        int pages = Math.max(1, (mijlpalen.size() + MIJLPALEN_PER_PAGE - 1) / MIJLPALEN_PER_PAGE);
        knusPage = Math.min(knusPage, pages - 1);
        int y = top + 56;
        for (int i = knusPage * MIJLPALEN_PER_PAGE; i < Math.min(mijlpalen.size(), (knusPage + 1) * MIJLPALEN_PER_PAGE); i++) {
            KnusVoortgang.Mijlpaal m = mijlpalen.get(i);
            boolean claimed = KnusVoortgang.Client.geclaimd(m.id());
            boolean reached = KnusVoortgang.Client.bereikt(m);
            ItemStack reward = m.beloning().get();
            boolean nothing = reward == null || reward.isEmpty();
            Button b = Button.builder(Component.translatable(claimed || reached && nothing ? "gui.guhs.guhdex.claimed" : "gui.guhs.guhdex.claim"),
                    btn -> ClientPacketDistributor.sendToServer(new KnusPayloads.KnusClaim(m.id()))).bounds(left + W - 58, y + 3, 50, 12).build();
            b.active = reached && !claimed;
            addRenderableWidget(b);
            y += MIJLPAAL_ROW;
        }
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                knusPage = Math.floorMod(knusPage - 1, pages);
                rebuildWidgets();
            }).bounds(left + W - 58, top + 42, 16, 12).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                knusPage = Math.floorMod(knusPage + 1, pages);
                rebuildWidgets();
            }).bounds(left + W - 24, top + 42, 16, 12).build());
        }
        // the collection pages of this section
        List<KnusVoortgang.Verzameling> verzamelingen = KnusVoortgang.verzamelingen(knusOnderdeel);
        int bw = (W - 16 - 4) / 2;
        int by = top + 56 + MIJLPALEN_PER_PAGE * MIJLPAAL_ROW + 16;
        for (int i = 0; i < verzamelingen.size() && i < 4; i++) {
            KnusVoortgang.Verzameling v = verzamelingen.get(i);
            Set<String> found = KnusVoortgang.Client.ontdekt(v.id());
            long n = v.items().stream().filter(found::contains).count();
            Component label = v.naam().copy().append(" (" + n + "/" + v.items().size() + ")");
            addRenderableWidget(Button.builder(label, btn -> {
                knusVerzameling = v.id();
                knusGekozen = null;
                knusPage = 0;
                rebuildWidgets();
            }).bounds(left + 8 + (i % 2) * (bw + 4), by + (i / 2) * 18, bw, 16).build());
        }
    }

    private void renderKnus(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (knusOnderdeel == null) {
            renderKnusOverzicht(g, mouseX, mouseY);
        } else if (knusVerzameling == null) {
            renderKnusOnderdeel(g);
        } else {
            renderKnusVerzameling(g, mouseX, mouseY);
        }
    }

    /** Every section: its icon, name and a bar (reached milestones + found entries). */
    private void renderKnusOverzicht(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<KnusVoortgang.Onderdeel> all = KnusVoortgang.onderdelen();
        int x = left + 10, w = W - 20;
        for (int i = 0; i < all.size(); i++) {
            KnusVoortgang.Onderdeel o = all.get(i);
            int y = top + KNUS_TOP + i * KNUS_ROW;
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + KNUS_ROW - 1;
            g.fill(x, y, x + w, y + KNUS_ROW - 1, hover ? 0x50F7B6CB : 0x28F7B6CB);
            g.pose().pushMatrix();
            g.pose().translate(x + 1, y);
            g.pose().scale(0.75f, 0.75f);
            g.item(o.icoon().get(), 0, 0);
            g.pose().popMatrix();
            int[] p = KnusVoortgang.Client.voortgang(o.id());
            boolean empty = p[1] == 0;
            fitText(g, o.naam(), x + 16, y + 2, 110, 9, 0xFF3A1C30);
            int bx = x + 132, bw = w - 132 - 44;
            bar(g, bx, y + 3, bw, 6, empty ? 0 : p[0] / (float) p[1]);
            String t = empty ? "" : p[0] + "/" + p[1];
            scaled(g, empty ? Component.translatable("gui.guhs.knus.binnenkort").withStyle(ChatFormatting.ITALIC) : Component.literal(t),
                    x + w - 3, y + 3, 0.75f, empty ? 0xFFB09AA6 : 0xFF7A2848, true);
        }
        // (2.9 visual QA: the hint was wider than the frame; now small and always inside it)
        Component kies = Component.translatable("gui.guhs.knus.kies");
        float ks = Math.min(0.75f, (W - 16) / (float) Math.max(1, font.width(kies)));
        int kw = Math.round(font.width(kies) * ks);
        scaled(g, kies, left + (W - kw) / 2, top + H - 9, ks, 0xFF9A8090, false);
    }

    /** A section: its milestones (name, bar, claim) and its collection pages (buttons, see initKnus). */
    private void renderKnusOnderdeel(GuiGraphicsExtractor g) {
        KnusVoortgang.Onderdeel o = KnusVoortgang.onderdelen().stream().filter(x -> x.id().equals(knusOnderdeel)).findFirst().orElse(null);
        if (o == null) {
            return;
        }
        g.item(o.icoon().get(), left + 58, top + 26);
        fitText(g, o.naam().copy().withStyle(ChatFormatting.BOLD), left + 78, top + 30, W - 78 - 70, 10, 0xFF3A1C30);
        List<KnusVoortgang.Mijlpaal> mijlpalen = KnusVoortgang.mijlpalen(knusOnderdeel);
        g.text(font, Component.translatable("gui.guhs.knus.mijlpalen"), left + 10, top + 44, 0xFF7A2848, false);
        if (mijlpalen.size() > MIJLPALEN_PER_PAGE) {
            int pages = (mijlpalen.size() + MIJLPALEN_PER_PAGE - 1) / MIJLPALEN_PER_PAGE;
            scaled(g, Component.literal((knusPage + 1) + "/" + pages), left + W - 33, top + 44, 0.75f, 0xFF7A2848, true);
        }
        int y = top + 56;
        if (mijlpalen.isEmpty()) {
            fitText(g, Component.translatable("gui.guhs.knus.binnenkort_lang"), left + 12, y + 2, W - 24, 30, 0xFF9A8090);
        }
        for (int i = knusPage * MIJLPALEN_PER_PAGE; i < Math.min(mijlpalen.size(), (knusPage + 1) * MIJLPALEN_PER_PAGE); i++) {
            KnusVoortgang.Mijlpaal m = mijlpalen.get(i);
            int have = Math.min(KnusVoortgang.Client.teller(m.teller()), m.doel());
            boolean reached = have >= m.doel();
            g.fill(left + 8, y, left + W - 8, y + MIJLPAAL_ROW - 2, reached ? 0x3868D88A : 0x22F7B6CB);
            ItemStack reward = m.beloning().get();
            if (reward != null && !reward.isEmpty()) {
                g.pose().pushMatrix();
                g.pose().translate(left + 10, y + 2);
                g.pose().scale(0.8f, 0.8f);
                g.item(reward, 0, 0);
                g.pose().popMatrix();
            }
            fitText(g, (reached ? Component.literal("✔ ") : Component.empty()).copy().append(m.naam()), left + 26, y + 1, W - 26 - 70 - 60, 16,
                    reached ? 0xFF2A7A48 : 0xFF3A1C30);
            int bx = left + W - 58 - 64;
            bar(g, bx, y + 5, 58, 6, have / (float) m.doel());
            scaled(g, Component.literal(have + "/" + m.doel()), bx + 29, y + 12, 0.6f, 0xFF7A2848, false);
            y += MIJLPAAL_ROW;
        }
        List<KnusVoortgang.Verzameling> verzamelingen = KnusVoortgang.verzamelingen(knusOnderdeel);
        if (!verzamelingen.isEmpty()) {
            g.text(font, Component.translatable("gui.guhs.knus.verzamelingen"), left + 10, top + 56 + MIJLPALEN_PER_PAGE * MIJLPAAL_ROW + 5,
                    0xFF7A2848, false);
        }
    }

    /** A collection page: a grid of entries (found: icon, else "?"), the chosen entry's name and text underneath. */
    private void renderKnusVerzameling(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        KnusVoortgang.Verzameling v = KnusVoortgang.verzameling(knusVerzameling);
        if (v == null) {
            return;
        }
        Set<String> found = KnusVoortgang.Client.ontdekt(v.id());
        long n = v.items().stream().filter(found::contains).count();
        fitText(g, v.naam().copy().withStyle(ChatFormatting.BOLD).append(" (" + n + "/" + v.items().size() + ")"), left + 58, top + 30, W - 66, 10,
                0xFF3A1C30);
        int gx = left + (W - CELLS_PER_ROW * CELL) / 2, gy = top + 44;
        int perPage = CELLS_PER_ROW * CELL_ROWS;
        int pageCount = Math.max(1, (v.items().size() + perPage - 1) / perPage);
        knusPage = Math.min(knusPage, pageCount - 1);
        String hovered = null;
        for (int i = knusPage * perPage; i < Math.min(v.items().size(), (knusPage + 1) * perPage); i++) {
            int k = i - knusPage * perPage;
            int cx = gx + (k % CELLS_PER_ROW) * CELL, cy = gy + (k / CELLS_PER_ROW) * CELL;
            String item = v.items().get(i);
            boolean has = found.contains(item);
            boolean chosen = item.equals(knusGekozen);
            g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, chosen ? 0xFFF7B6CB : has ? 0x40F7B6CB : 0xFF2A1420);
            if (has) {
                g.item(v.icoon().apply(item), cx + 1, cy + 1);
            } else {
                g.centeredText(font, "?", cx + (CELL - 2) / 2, cy + 5, 0xFF8A6A7A);
            }
            if (mouseX >= cx && mouseX < cx + CELL - 2 && mouseY >= cy && mouseY < cy + CELL - 2) {
                hovered = item;
            }
        }
        if (pageCount > 1) {
            scaled(g, Component.literal((knusPage + 1) + "/" + pageCount + "  ▶"), left + W - 10, top + 44 + CELL_ROWS * CELL + 1, 0.75f,
                    0xFF7A2848, true);
        }
        int ty = top + 44 + CELL_ROWS * CELL + 10;
        g.fill(left + 10, ty - 3, left + W - 10, ty - 2, 0x60F7B6CB);
        String show = knusGekozen != null ? knusGekozen : hovered;
        if (show == null) {
            fitText(g, Component.translatable("gui.guhs.knus.klik_item"), left + 12, ty, W - 24, 20, 0xFF9A8090);
        } else if (!found.contains(show)) {
            fitText(g, Component.translatable("gui.guhs.knus.nog_niet"), left + 12, ty, W - 24, 20, 0xFF9A8090);
        } else {
            int used = fitText(g, v.item(show).copy().withStyle(ChatFormatting.BOLD), left + 12, ty, W - 24, 10, 0xFF3A1C30);
            if (net.minecraft.client.resources.language.I18n.exists(v.infoKey(show))) {
                fitText(g, Component.translatable(v.infoKey(show)), left + 12, ty + used + 2, W - 24, top + H - 8 - (ty + used + 2), 0xFF5A3A4A);
            }
        }
    }

    /** A progress bar (0..1), pink on dark. */
    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF7A2848);
        g.fill(x, y, x + w, y + h, 0xFF3A1C30);
        int fw = Math.round(w * Math.max(0f, Math.min(1f, frac)));
        if (fw > 0) {
            g.fill(x, y, x + fw, y + h, frac >= 1f ? 0xFF68D88A : 0xFFF77AB0);
            g.fill(x, y, x + fw, y + 1, 0x60FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        int hit = nl.juiced.guhs.feature.gids.client.GidsTabs.onder(left + TABS_X, top + TABS_Y, Tab.values().length, mouseX, mouseY, TAB_W, TAB_GAP);
        if (hit >= 0 && button == 0) {
            if (Tab.values()[hit] != tab) {
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0f));
                kies(Tab.values()[hit]);
            }
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if ((tab == Tab.MINIGAMES || tab == Tab.KLEDING) && lijst.klik(mouseX, mouseY, button)) {
            SCROLL.put(tab, lijst.scroll());
            return true;
        }
        if (tab == Tab.MIJN_GUHS) {
            return mijnGuhs.klik(mouseX, mouseY, button);
        }
        if (tab == Tab.VERHALEN) {
            return verhalen.klik(mouseX, mouseY, button);
        }
        if (tab == Tab.TITELS) {
            return titels.klik(mouseX, mouseY, button);
        }
        if (tab != Tab.KNUS || button != 0) {
            return false;
        }
        if (knusOnderdeel == null) {
            List<KnusVoortgang.Onderdeel> all = KnusVoortgang.onderdelen();
            int x = left + 10, w = W - 20;
            for (int i = 0; i < all.size(); i++) {
                int y = top + KNUS_TOP + i * KNUS_ROW;
                if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + KNUS_ROW - 1) {
                    knusOnderdeel = all.get(i).id();
                    knusVerzameling = null;
                    knusPage = 0;
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.2f));
                    rebuildWidgets();
                    return true;
                }
            }
        } else if (knusVerzameling != null) {
            KnusVoortgang.Verzameling v = KnusVoortgang.verzameling(knusVerzameling);
            if (v == null) {
                return false;
            }
            int gx = left + (W - CELLS_PER_ROW * CELL) / 2, gy = top + 44, perPage = CELLS_PER_ROW * CELL_ROWS;
            for (int i = knusPage * perPage; i < Math.min(v.items().size(), (knusPage + 1) * perPage); i++) {
                int k = i - knusPage * perPage;
                int cx = gx + (k % CELLS_PER_ROW) * CELL, cy = gy + (k / CELLS_PER_ROW) * CELL;
                if (mouseX >= cx && mouseX < cx + CELL - 2 && mouseY >= cy && mouseY < cy + CELL - 2) {
                    knusGekozen = v.items().get(i).equals(knusGekozen) ? null : v.items().get(i);
                    return true;
                }
            }
            // the page indicator: next page
            int pageCount = Math.max(1, (v.items().size() + perPage - 1) / perPage);
            if (pageCount > 1 && mouseY >= top + 44 + CELL_ROWS * CELL && mouseY < top + 44 + CELL_ROWS * CELL + 8 && mouseX > left + W - 50) {
                knusPage = (knusPage + 1) % pageCount;
                return true;
            }
        }
        return false;
    }

    /** Text scales we try, biggest first: fractions that still land on (half) pixels, so the letters stay crisp enough. */
    private static final float[] SCALES = {1f, 0.875f, 0.75f, 0.625f, 0.5f};

    /**
     * Draws the text wrapped to maxWidth inside a box of maxHeight, at the biggest scale where every line fits (lines of
     * 10 px at scale 1). Returns the height used. If even the smallest scale doesn't fit, the last lines are left out
     * rather than drawn over something else.
     */
    private int fitText(GuiGraphicsExtractor g, Component text, int x, int y, int maxWidth, int maxHeight, int colour) {
        if (maxWidth <= 0 || maxHeight <= 0) {
            return 0;
        }
        float scale = SCALES[SCALES.length - 1];
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = null;
        for (float s : SCALES) {
            lines = font.split(text, (int) Math.floor(maxWidth / s));
            float lineH = s == 1f ? 10f : 9.5f * s;
            if (lines.size() * lineH - (lineH - 8 * s) <= maxHeight) {
                scale = s;
                break;
            }
            scale = s;
        }
        float lineH = scale == 1f ? 10f : 9.5f * scale;
        int fits = Math.max(1, (int) ((maxHeight + (lineH - 8 * scale)) / lineH));
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        int n = Math.min(lines.size(), fits);
        for (int i = 0; i < n; i++) {
            g.text(font, lines.get(i), 0, Math.round(i * lineH / scale), colour, false);
        }
        g.pose().popMatrix();
        return Math.round(n * lineH);
    }

    /** Text at (x, y) at a scale; rightAligned: x is where it ends. */
    private void scaled(GuiGraphicsExtractor g, Component text, int x, int y, float scale, int colour, boolean rightAligned) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.text(font, text, rightAligned ? -font.width(text) : 0, 0, colour, false);
        g.pose().popMatrix();
    }

    @javax.annotation.Nullable
    private net.minecraft.world.entity.LivingEntity creature(GuhVariant v) {
        return creatures.computeIfAbsent(v, page -> net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getOptional(nl.juiced.guhs.Guhs.id(page.id())).map(type -> type.create(minecraft.level, EntitySpawnReason.TRIGGERED))
                .filter(e -> e instanceof net.minecraft.world.entity.LivingEntity).map(e -> (net.minecraft.world.entity.LivingEntity) e).orElse(null));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }
}
