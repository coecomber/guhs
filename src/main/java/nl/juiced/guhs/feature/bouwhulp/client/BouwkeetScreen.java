package nl.juiced.guhs.feature.bouwhulp.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetPayloads;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetPayloads.Actie;
import nl.juiced.guhs.feature.gids.client.GidsTekst;

/**
 * The Bouwkeet menu (design 2.2): the tabs along the top (each a {@link TabPagina}, registered by its slice with
 * {@link #tab}), the open tab's page in the middle, and along the bottom the keet's project: its size, what is missing,
 * "Bouw!" (with Pof! at level 4), the ghost's control panel, undo, let go.
 * <pre>
 * BouwkeetScreen.tab("vormen", new BouwkeetScreen.TabPagina() {
 *     public void knoppen(BouwkeetScreen s, CompoundTag tab, int x, int y, int w, int h) { s.knop(..., () -> s.doe("vormen", 1, 0, "")); }
 *     public void teken(BouwkeetScreen s, GuiGraphicsExtractor g, CompoundTag tab, int x, int y, int w, int h) { ... }
 * });
 * </pre>
 */
public class BouwkeetScreen extends Screen {
    public static final int W = 340, H = 244;
    /** The page area: x + 10 .. x + W - 10, from {@link #PAGINA_Y} to {@link #PROJECT_Y} - 4 (relative to the panel). */
    public static final int PAGINA_Y = 42, PROJECT_Y = H - 64;
    public static final int PANEEL = 0xF0FFF9EC, RAND = 0xFFE7B04A, KOP = 0xFFFFE9B8, DONKER = 0xFF5A3A1E, TEKST = 0xFF6A4A2A,
            LICHT = 0xFFA0845E, GROEN = 0xFF4E8A3A, ROOD = 0xFFB0484A, ROZE = 0xFFE07AAE;

    /** The client half of a tab (see {@code BouwkeetTabs} for the server half). */
    public interface TabPagina {
        /** Adds the page's buttons (x, y, w, h: the page area on screen). */
        default void knoppen(BouwkeetScreen s, CompoundTag tab, int x, int y, int w, int h) {
        }

        /** Draws the page (behind the buttons). */
        default void teken(BouwkeetScreen s, GuiGraphicsExtractor g, CompoundTag tab, int x, int y, int w, int h) {
        }
    }

    private static final Map<String, TabPagina> PAGINAS = new ConcurrentHashMap<>();

    /** Registers (or replaces) the page of a tab. */
    public static void tab(String id, TabPagina pagina) {
        PAGINAS.put(id, pagina);
    }

    private CompoundTag data;
    private final BlockPos pos;
    private int left, top;

    public BouwkeetScreen(CompoundTag data) {
        super(Component.translatable("block.guhs.bouwkeet"));
        this.data = data;
        this.pos = BlockPos.of(data.getLongOr("Pos", 0L));
    }

    public BlockPos pos() {
        return pos;
    }

    /** New state: rebuild, unless a text box is in use. */
    public void update(CompoundTag nieuw) {
        this.data = nieuw;
        for (var w : children()) {
            if (w instanceof EditBox e && e.isFocused()) {
                return;
            }
        }
        rebuildWidgets();
    }

    // =====================================================================================================================
    // for the pages
    // =====================================================================================================================

    public CompoundTag data() {
        return data;
    }

    public Font lettertype() {
        return font;
    }

    public boolean magBewerken() {
        return data.getBooleanOr("Mag", false);
    }

    /** The level the keet works at now (0..4). */
    public int werkNiveau() {
        return data.getIntOr("WerkNiveau", 0);
    }

    public String openTab() {
        return data.getStringOr("Open", "");
    }

    /** Sends a button of a tab ("" = the menu itself). */
    public void doe(String tab, int actie, int waarde, String tekst) {
        ClientPacketDistributor.sendToServer(new BouwkeetPayloads.Doe(pos, tab, actie, waarde, tekst));
    }

    public Button knop(Component tekst, int x, int y, int w, Runnable actie, boolean actief) {
        Button b = Button.builder(tekst, x2 -> actie.run()).bounds(x, y, w, 18).build();
        b.active = actief;
        return addRenderableWidget(b);
    }

    public <T extends AbstractWidget> T voegToe(T widget) {
        return addRenderableWidget(widget);
    }

    // =====================================================================================================================
    // building the menu
    // =====================================================================================================================

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        ListTag tabs = data.getListOrEmpty("Tabs");
        int n = Math.max(1, tabs.size());
        int tw = (W - 12) / n;
        String open = openTab();
        for (int i = 0; i < tabs.size(); i++) {
            CompoundTag t = tabs.getCompoundOrEmpty(i);
            String id = t.getStringOr("Id", "");
            int vanaf = t.getIntOr("Vanaf", 0);
            Component naam = Component.translatable("gui.guhs.bouwkeet.tab." + id);
            Button b = Button.builder(vanaf > werkNiveau() ? naam.copy().withStyle(ChatFormatting.GRAY) : naam, x -> doe("", Actie.KIES.ordinal(), 0, id))
                    .bounds(left + 6 + i * tw, top + 20, tw - 2, 16).build();
            if (vanaf > werkNiveau()) {
                b.setTooltip(Tooltip.create(Component.translatable("gui.guhs.bouwkeet.tab.vanaf", Component.translatable("gui.guhs.bouwkeet.niveau." + vanaf))));
            }
            b.active = !id.equals(open);
            addRenderableWidget(b);
        }
        TabPagina pagina = PAGINAS.get(open);
        if (pagina != null) {
            pagina.knoppen(this, data.getCompoundOrEmpty("Tab"), left + 10, top + PAGINA_Y, W - 20, PROJECT_Y - PAGINA_Y - 4);
        }
        projectKnoppen();
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bouwkeet.sluit"), b -> onClose()).bounds(left + W - 56, top + H - 22, 48, 18).build());
    }

    private void projectKnoppen() {
        CompoundTag pr = data.getCompoundOrEmpty("Project");
        int y = top + H - 22;
        boolean mag = magBewerken();
        if (pr.isEmpty()) {
            Button terug = knop(Component.translatable("gui.guhs.bouwkeet.terug"), left + 10, y, 70, () -> doe("", Actie.TERUG.ordinal(), 0, ""), true);
            terug.setTooltip(Tooltip.create(Component.translatable("gui.guhs.bouwhulp.spook.terug.uitleg")));
            return;
        }
        boolean bouwt = pr.getIntOr("Status", 0) == 1;
        if (bouwt) {
            knop(Component.translatable("gui.guhs.bouwhulp.spook.pauze"), left + 10, y, 64, () -> doe("", Actie.PAUZE.ordinal(), 0, ""), mag);
        } else {
            Component bouw = pr.getBooleanOr("Pof", false) && pr.getListOrEmpty("Tekort").isEmpty()
                    ? Component.translatable("gui.guhs.bouwkeet.pof").withStyle(ChatFormatting.BOLD)
                    : Component.translatable("gui.guhs.bouwhulp.spook.bouw").withStyle(ChatFormatting.BOLD);
            knop(bouw, left + 10, y, 64, () -> doe("", Actie.BOUW.ordinal(), 0, ""), mag);
        }
        knop(Component.translatable("gui.guhs.bouwkeet.bediening"), left + 78, y, 70, () -> doe("", Actie.BEDIENING.ordinal(), 0, ""), true)
                .setTooltip(Tooltip.create(Component.translatable("gui.guhs.bouwkeet.bediening.uitleg")));
        Button terug = knop(Component.translatable("gui.guhs.bouwhulp.spook.terug", pr.getIntOr("Ongedaan", 0)), left + 152, y, 60,
                () -> doe("", Actie.TERUG.ordinal(), 0, ""), pr.getIntOr("Ongedaan", 0) > 0);
        terug.setTooltip(Tooltip.create(Component.translatable("gui.guhs.bouwhulp.spook.terug.uitleg")));
        knop(Component.translatable("gui.guhs.bouwkeet.loslaten"), left + 216, y, 64, () -> doe("", Actie.LOSLATEN.ordinal(), 0, ""), mag && !bouwt)
                .setTooltip(Tooltip.create(Component.translatable("gui.guhs.bouwkeet.loslaten.uitleg")));
    }

    // =====================================================================================================================
    // drawing
    // =====================================================================================================================

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.fill(left, top, left + W, top + 18, KOP);
        g.fill(left, top + 18, left + W, top + 19, RAND);
        g.fill(left, top + PROJECT_Y - 2, left + W, top + PROJECT_Y - 1, RAND);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 8, top + 5, DONKER, false);
        int niveau = data.getIntOr("Niveau", 0), werk = werkNiveau();
        Component nv = Component.translatable("gui.guhs.bouwkeet.niveau." + niveau);
        if (werk < niveau) {
            nv = nv.copy().append(" ").append(Component.translatable(data.getBooleanOr("Aan", true) ? "gui.guhs.bouwkeet.geen_kracht" : "gui.guhs.bouwkeet.uit")
                    .withStyle(ChatFormatting.ITALIC));
        }
        GidsTekst.passend(g, nv, left + W - 8, top + 5, 200, 0.75f, werk < niveau ? ROOD : LICHT, true);
        TabPagina pagina = PAGINAS.get(openTab());
        if (pagina != null) {
            pagina.teken(this, g, data.getCompoundOrEmpty("Tab"), left + 10, top + PAGINA_Y, W - 20, PROJECT_Y - PAGINA_Y - 4);
        }
        project(g, left + 10, top + PROJECT_Y + 2);
    }

    private void project(GuiGraphicsExtractor g, int x, int y) {
        CompoundTag pr = data.getCompoundOrEmpty("Project");
        if (pr.isEmpty()) {
            GidsTekst.alinea(g, Component.translatable("gui.guhs.bouwkeet.geen_project"), x, y, W - 20, 0.75f, LICHT);
            return;
        }
        String naam = pr.getStringOr("Naam", "");
        int[] gr = pr.getIntArray("Grootte").orElse(new int[] {0, 0, 0});
        Component kop = Component.translatable("gui.guhs.bouwkeet.project", naam.isEmpty() ? Component.translatable("gui.guhs.bouwhulp.project")
                : Component.literal(naam), gr[0], gr[1], gr[2], pr.getIntOr("Totaal", 0));
        GidsTekst.passend(g, kop.copy().withStyle(ChatFormatting.BOLD), x, y, W - 20, 0.75f, DONKER, false);
        int gedaan = pr.getIntOr("Gedaan", 0), rest = pr.getIntOr("Rest", 0);
        if (gedaan + rest > 0) {
            GidsTekst.balk(g, x, y + 10, W - 20, 4, gedaan / (float) (gedaan + rest));
        }
        ListTag tekort = pr.getListOrEmpty("Tekort");
        if (tekort.isEmpty()) {
            GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.spook.alles_er"), x, y + 17, W - 20, 0.75f, GROEN, false);
        } else {
            GidsTekst.passend(g, Component.translatable("gui.guhs.bouwhulp.spook.tekort"), x, y + 17, 60, 0.75f, ROOD, false);
            for (int i = 0; i < Math.min(10, tekort.size()); i++) {
                CompoundTag c = tekort.getCompoundOrEmpty(i);
                var item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(c.getStringOr("Id", "minecraft:air")));
                int ix = x + 62 + i * 24;
                if (item.isPresent()) {
                    g.item(new ItemStack(item.get()), ix, y + 13);
                    GidsTekst.schaal(g, Component.literal(String.valueOf(c.getIntOr("N", 0))), ix + 17, y + 24, 0.6f, ROOD, true);
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
