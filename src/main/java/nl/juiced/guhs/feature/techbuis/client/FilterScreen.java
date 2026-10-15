package nl.juiced.guhs.feature.techbuis.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.techbuis.FilterMenu;

/**
 * The screen of a Filterstuk (nine examples, "alleen deze / alles behalve", "precies", "bewaar minstens N") and of a
 * Voorraadmeter (one example, "minstens / minder dan", "precies", the number N). The examples are ghost slots (click one
 * with an item; your item stays in your hand). The - and + buttons count by 1, with shift by 8, with ctrl by 64. All
 * changes go to the server as menu buttons ({@link FilterMenu#clickMenuButton}). Background: textures/gui/techbuis_filter.png
 * and techbuis_meter.png (tools/features/tech_buizen.py).
 */
public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
    private static final Identifier FILTER = Guhs.id("textures/gui/techbuis_filter.png");
    private static final Identifier METER = Guhs.id("textures/gui/techbuis_meter.png");
    /** The column of buttons on the right. */
    private static final int KOLOM_X = 68, KOLOM_B = 100, KNOP_H = 16, LIJST_Y = 17, PRECIES_Y = 35, LABEL_Y = 56, GETAL_Y = 66;
    private static final int TEKST = 0xFF404040;

    private final boolean meter;
    private Button lijstKnop, preciesKnop;

    public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, FilterMenu.HOOGTE);
        this.meter = menu.vakken() == FilterMenu.VAKKEN_METER;
        this.inventoryLabelY = FilterMenu.INV_Y - 11;
    }

    private String sleutel(String naam) {
        return "gui.guhs.techbuis.scherm." + (meter ? "meter." : "filter.") + naam;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + KOLOM_X, y = topPos;
        lijstKnop = addRenderableWidget(Button.builder(Component.empty(), b -> druk(FilterMenu.KNOP_BEHALVE))
                .bounds(x, y + LIJST_Y, KOLOM_B, KNOP_H).tooltip(Tooltip.create(Component.translatable(sleutel("lijst.uitleg")))).build());
        preciesKnop = addRenderableWidget(Button.builder(Component.empty(), b -> druk(FilterMenu.KNOP_PRECIES))
                .bounds(x, y + PRECIES_Y, KOLOM_B, KNOP_H).tooltip(Tooltip.create(Component.translatable("gui.guhs.techbuis.scherm.precies.uitleg"))).build());
        Tooltip stappen = Tooltip.create(Component.translatable("gui.guhs.techbuis.scherm.getal.uitleg"));
        addRenderableWidget(Button.builder(Component.literal("-"), b -> druk(stap(false))).bounds(x, y + GETAL_Y, 20, KNOP_H).tooltip(stappen).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> druk(stap(true))).bounds(x + KOLOM_B - 20, y + GETAL_Y, 20, KNOP_H).tooltip(stappen).build());
        werkBij();
    }

    /** The button id of one step up or down: by 1, with shift by 8, with ctrl by 64. */
    private int stap(boolean erbij) {
        if (minecraft != null && minecraft.hasControlDown()) {
            return erbij ? FilterMenu.KNOP_PLUS_64 : FilterMenu.KNOP_MIN_64;
        }
        if (minecraft != null && minecraft.hasShiftDown()) {
            return erbij ? FilterMenu.KNOP_PLUS_8 : FilterMenu.KNOP_MIN_8;
        }
        return erbij ? FilterMenu.KNOP_PLUS_1 : FilterMenu.KNOP_MIN_1;
    }

    private void druk(int knop) {
        if (minecraft != null && minecraft.player != null && minecraft.gameMode != null && menu.clickMenuButton(minecraft.player, knop)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, knop);
            werkBij();
        }
    }

    private void werkBij() {
        if (lijstKnop != null) {
            lijstKnop.setMessage(Component.translatable(sleutel(menu.behalve() ? "lijst.behalve" : "lijst.alleen")));
            preciesKnop.setMessage(Component.translatable("gui.guhs.techbuis.scherm.precies." + (menu.precies() ? "aan" : "uit")));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        werkBij();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.blit(RenderPipelines.GUI_TEXTURED, meter ? METER : FILTER, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        g.text(font, Component.translatable(sleutel("getal")), KOLOM_X, LABEL_Y, TEKST, false);
        g.centeredText(font, Component.literal(String.valueOf(menu.getal())), KOLOM_X + KOLOM_B / 2, GETAL_Y + 4, 0xFFFFFFFF);
    }
}
