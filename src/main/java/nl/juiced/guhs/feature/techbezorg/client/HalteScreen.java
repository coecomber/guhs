package nl.juiced.guhs.feature.techbezorg.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import nl.juiced.guhs.feature.techbezorg.Bezorgnet;
import nl.juiced.guhs.feature.techbezorg.HalteMenu;

/**
 * The Haltepaaltje's screen: which Stepstation it belongs to and which stop it is, the big button ophalen / afleveren (it
 * also turns the sign on the pole), "Ander station", and the filter: nine ghost slots (click with an item; nothing is
 * taken) with "Leegmaken". Drawn with plain fills, like the Stepstation's screen.
 */
public class HalteScreen extends AbstractContainerScreen<HalteMenu> {
    private static final int BG = 0xFF3A1F2C, PANEL = 0xFFE9D6EE, SLOT = 0xFF7A5A86, SLOT_IN = 0xFFAE8FB8, SPOOK = 0xFFD9C2E0;
    private static final int TEXT = 0xFF4A2A56, GRIJS = 0xFF7E6A86, GROEN = 0xFF2E8B4A, ORANJE = 0xFFC2661A, ROOD = 0xFFB02A4A;

    private Button soort;
    private boolean getoond;

    public HalteScreen(HalteMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, HalteMenu.BREED, HalteMenu.HOOG);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = HalteMenu.INV_X;
        this.inventoryLabelY = HalteMenu.INV_Y - 10;
    }

    @Override
    protected void init() {
        super.init();
        soort = addRenderableWidget(Button.builder(soortTekst(), b -> klik(HalteMenu.KNOP_SOORT)).bounds(leftPos + 8, topPos + 30, 78, 16).build());
        soort.setTooltip(Tooltip.create(soortUitleg()));
        getoond = menu.ophalen();
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.techbezorg.ander_station"), b -> klik(HalteMenu.KNOP_STATION))
                .bounds(leftPos + 90, topPos + 30, 78, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.guhs.techbezorg.ander_station.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.techbezorg.wis"), b -> klik(HalteMenu.KNOP_WIS))
                .bounds(leftPos + 110, topPos + 51, 58, 13).build());
    }

    private void klik(int knop) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, knop);
        }
    }

    private Component soortTekst() {
        return Component.translatable(menu.ophalen() ? "gui.guhs.techbezorg.ophalen" : "gui.guhs.techbezorg.afleveren")
                .withColor(menu.ophalen() ? 0x7CE08A : 0xFFB866);
    }

    private Component soortUitleg() {
        return Component.translatable(menu.ophalen() ? "gui.guhs.techbezorg.ophalen.tooltip" : "gui.guhs.techbezorg.afleveren.tooltip");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (soort != null && getoond != menu.ophalen()) {   // (the server answered the click)
            getoond = menu.ophalen();
            soort.setMessage(soortTekst());
            soort.setTooltip(Tooltip.create(soortUitleg()));
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (menu.getCarried().isEmpty()) {
            if (menu.gekoppeld() && isHovering(8, 17, 160, 10, mouseX, mouseY)) {
                BlockPos s = menu.station();
                g.setTooltipForNextFrame(font, Component.translatable("gui.guhs.techbezorg.halte.station.waar", s.getX(), s.getY(), s.getZ()), mouseX, mouseY);
            } else if (isHovering(HalteMenu.FILTER_X - 1, HalteMenu.FILTER_Y - 13, 100, 12, mouseX, mouseY)) {
                g.setTooltipForNextFrame(font, font.split(Component.translatable("gui.guhs.techbezorg.filter.tooltip"), 170), mouseX, mouseY);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, BG);
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        for (int i = 0; i < Bezorgnet.FILTER; i++) {            // the ghost slots: lighter, they hold nothing
            int sx = x + HalteMenu.FILTER_X - 1 + i * 18, sy = y + HalteMenu.FILTER_Y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, SLOT);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SPOOK);
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                vak(g, x + HalteMenu.INV_X - 1 + c * 18, y + HalteMenu.INV_Y - 1 + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            vak(g, x + HalteMenu.INV_X - 1 + c * 18, y + HalteMenu.INV_Y + 57);
        }
    }

    private void vak(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_IN);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        if (!menu.heeftKist()) {
            g.text(font, Component.translatable("gui.guhs.techbezorg.halte.geen_kist.kort"), 8, 18, ROOD, false);
        } else if (menu.gekoppeld()) {
            g.text(font, Component.translatable("gui.guhs.techbezorg.halte.station", menu.nummer(), menu.aantal()), 8, 18,
                    menu.ophalen() ? GROEN : ORANJE, false);
        } else {
            g.text(font, Component.translatable("gui.guhs.techbezorg.halte.los"), 8, 18, GRIJS, false);
        }
        Component filter = Component.translatable("gui.guhs.techbezorg.filter");
        g.text(font, filter, HalteMenu.FILTER_X, HalteMenu.FILTER_Y - 11, TEXT, false);
        if (menu.filterLeeg()) {
            g.text(font, Component.translatable("gui.guhs.techbezorg.filter.alles"), HalteMenu.FILTER_X + font.width(filter) + 4,
                    HalteMenu.FILTER_Y - 11, GRIJS, false);
        }
    }
}
