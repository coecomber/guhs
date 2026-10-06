package nl.juiced.guhs.feature.techbezorg.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.techbezorg.Bezorgnet;
import nl.juiced.guhs.feature.techbezorg.HaltepaaltjeBlock;
import nl.juiced.guhs.feature.techbezorg.StepstationBlockEntity.Fase;
import nl.juiced.guhs.feature.techbezorg.StepstationMenu;

/**
 * The Stepstation's screen: one line that says what the Bezorgguhtje is doing, on the left its backpack (darkened while it
 * is on the road: then it is on the guhtje's back) with the button "Naar huis!", on the right the round: every stop with its
 * number, what happens there (green ophalen / orange afleveren), the chest it stands at, and two little buttons to move it
 * earlier or later. The stop the guhtje is riding to is marked. Drawn with plain fills, like the Bank Guh's screen.
 */
public class StepstationScreen extends AbstractContainerScreen<StepstationMenu> {
    private static final int BG = 0xFF3A1F2C, PANEL = 0xFFE9D6EE, PANEL_DARK = 0xFFC3A0CD, SLOT = 0xFF7A5A86, SLOT_IN = 0xFFAE8FB8;
    private static final int TEXT = 0xFF4A2A56, GRIJS = 0xFF7E6A86, GROEN = 0xFF2E8B4A, ORANJE = 0xFFC2661A, ROOD = 0xFFB02A4A;
    private static final int RIJ_NU = 0xFFFFE9A8, DICHT = 0xA03A1F2C;
    /** The list of stops: its corner, the width of a row and its height. */
    private static final int LIJST_X = 74, LIJST_Y = 53, LIJST_B = 148, RIJ = 12;

    private final Button[] eerder = new Button[Bezorgnet.MAX_HALTES];
    private final Button[] later = new Button[Bezorgnet.MAX_HALTES];

    public StepstationScreen(StepstationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, StepstationMenu.BREED, StepstationMenu.HOOG);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = StepstationMenu.INV_X;
        this.inventoryLabelY = StepstationMenu.INV_Y - 10;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < Bezorgnet.MAX_HALTES; i++) {
            int halte = i;
            int y = topPos + LIJST_Y + i * RIJ;
            eerder[i] = addRenderableWidget(Button.builder(Component.literal("▲"), b -> klik(halte * 4 + StepstationMenu.EERDER))
                    .bounds(leftPos + LIJST_X + LIJST_B - 25, y, 12, RIJ - 1)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.techbezorg.omhoog"))).build());
            later[i] = addRenderableWidget(Button.builder(Component.literal("▼"), b -> klik(halte * 4 + StepstationMenu.LATER))
                    .bounds(leftPos + LIJST_X + LIJST_B - 12, y, 12, RIJ - 1)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.techbezorg.omlaag"))).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.techbezorg.naar_huis"), b -> klik(StepstationMenu.KNOP_NAAR_HUIS))
                .bounds(leftPos + 8, topPos + 112, 56, 14)
                .tooltip(Tooltip.create(Component.translatable("gui.guhs.techbezorg.naar_huis.tooltip"))).build());
        knoppen();
    }

    private void klik(int knop) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, knop);
        }
    }

    /** Only the buttons of stops that exist, and no "earlier" on the first or "later" on the last. */
    private void knoppen() {
        int n = menu.aantal();
        for (int i = 0; i < Bezorgnet.MAX_HALTES; i++) {
            if (eerder[i] != null) {
                eerder[i].visible = i < n && i > 0;
                later[i].visible = i < n - 1;
            }
        }
    }

    // --- what the numbers mean ---

    private Component stand() {
        if (!menu.koerier()) {
            return Component.translatable("gui.guhs.techbezorg.stand.zoek");
        }
        Fase fase = menu.fase();
        return switch (fase) {
            case RIJDT -> Component.translatable("gui.guhs.techbezorg.stand.rijdt", menu.doel() + 1, menu.aantal());
            case LAADT -> Component.translatable("gui.guhs.techbezorg.stand.laadt", menu.doel() + 1);
            default -> Component.translatable("gui.guhs.techbezorg.stand." + fase.id());
        };
    }

    /** The name of the block the pole of stop i stands at (as far as this client has that chunk). */
    private Component kist(int i) {
        if (minecraft == null || minecraft.level == null) {
            return Component.literal("?");
        }
        BlockPos paal = menu.halte(i);
        BlockState state = minecraft.level.getBlockState(paal);
        if (!state.hasProperty(HaltepaaltjeBlock.FACING)) {
            return Component.literal("?");
        }
        BlockState kist = minecraft.level.getBlockState(paal.relative(state.getValue(HaltepaaltjeBlock.FACING)));
        return kist.isAir() ? Component.literal("-") : kist.getBlock().getName();
    }

    private int rijOnder(double mouseX, double mouseY) {
        double x = mouseX - leftPos - LIJST_X, y = mouseY - topPos - LIJST_Y;
        if (x < 0 || x >= LIJST_B - 26 || y < 0) {
            return -1;
        }
        int i = (int) y / RIJ;
        return i < menu.aantal() ? i : -1;
    }

    // --- drawing ---

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        knoppen();
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        int rij = rijOnder(mouseX, mouseY);
        if (rij >= 0 && menu.getCarried().isEmpty()) {
            List<Component> regels = new ArrayList<>();
            regels.add(Component.translatable("gui.guhs.techbezorg.halte.rij." + (menu.ophalen(rij) ? "ophalen" : "afleveren"), rij + 1)
                    .withStyle(menu.ophalen(rij) ? ChatFormatting.GREEN : ChatFormatting.GOLD));
            regels.add(kist(rij));
            BlockPos halte = menu.halte(rij);
            regels.add(Component.translatable("gui.guhs.techbezorg.halte.rij.afstand", (int) Math.round(Math.sqrt(halte.distSqr(menu.pos()))))
                    .withStyle(ChatFormatting.GRAY));
            if (!menu.bereikbaar(rij)) {
                regels.add(Component.translatable("gui.guhs.techbezorg.halte.rij.te_ver").withStyle(ChatFormatting.RED));
            } else if (!menu.heeftKist(rij)) {
                regels.add(Component.translatable("gui.guhs.techbezorg.halte.rij.geen_kist").withStyle(ChatFormatting.RED));
            }
            g.setComponentTooltipForNextFrame(font, regels, mouseX, mouseY);
        } else if (!menu.open() && isHovering(StepstationMenu.RUGZAK_X - 1, StepstationMenu.RUGZAK_Y - 1, 54, 54, mouseX, mouseY)
                && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            g.setTooltipForNextFrame(font, font.split(Component.translatable("gui.guhs.techbezorg.rugzak.dicht"), 170), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, BG);
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        // the backpack
        for (int i = 0; i < Bezorgnet.RUGZAK; i++) {
            vak(g, x + StepstationMenu.RUGZAK_X - 1 + (i % 3) * 18, y + StepstationMenu.RUGZAK_Y - 1 + (i / 3) * 18);
        }
        // the round
        g.fill(x + LIJST_X - 1, y + LIJST_Y - 1, x + LIJST_X + LIJST_B + 1, y + LIJST_Y + Bezorgnet.MAX_HALTES * RIJ, PANEL_DARK);
        int doel = menu.doel();
        for (int i = 0; i < menu.aantal(); i++) {
            int ry = y + LIJST_Y + i * RIJ;
            g.fill(x + LIJST_X, ry, x + LIJST_X + LIJST_B, ry + RIJ - 1, i == doel ? RIJ_NU : PANEL);
        }
        // the inventory
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                vak(g, x + StepstationMenu.INV_X - 1 + c * 18, y + StepstationMenu.INV_Y - 1 + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            vak(g, x + StepstationMenu.INV_X - 1 + c * 18, y + StepstationMenu.INV_Y + 57);
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
        // what the guhtje is doing (at most two lines)
        List<FormattedCharSequence> stand = font.split(stand(), imageWidth - 16);
        for (int i = 0; i < Math.min(2, stand.size()); i++) {
            g.text(font, stand.get(i), 8, 18 + i * 10, menu.kracht() ? TEXT : GRIJS, false);
        }
        g.text(font, Component.translatable("gui.guhs.techbezorg.rugzak"), StepstationMenu.RUGZAK_X, 42, menu.vast() ? ROOD : TEXT, false);
        g.text(font, Component.translatable("gui.guhs.techbezorg.haltes", menu.aantal(), Bezorgnet.MAX_HALTES), LIJST_X, 42, TEXT, false);
        if (menu.aantal() == 0) {
            int ty = LIJST_Y + 4;
            for (FormattedCharSequence regel : font.split(Component.translatable("gui.guhs.techbezorg.geen_haltes"), LIJST_B - 8)) {
                g.text(font, regel, LIJST_X + 4, ty, GRIJS, false);
                ty += 10;
            }
        }
        for (int i = 0; i < menu.aantal(); i++) {
            int ry = LIJST_Y + i * RIJ + 2;
            boolean goed = menu.bereikbaar(i) && menu.heeftKist(i);
            g.text(font, (i + 1) + ".", LIJST_X + 2, ry, TEXT, false);
            Component soort = Component.translatable(menu.ophalen(i) ? "gui.guhs.techbezorg.ophalen" : "gui.guhs.techbezorg.afleveren");
            g.text(font, soort, LIJST_X + 14, ry, !goed ? ROOD : menu.ophalen(i) ? GROEN : ORANJE, false);
            int naamX = LIJST_X + 68, ruimte = LIJST_B - 68 - 28;
            String naam = font.plainSubstrByWidth(kist(i).getString(), ruimte);
            g.text(font, naam, naamX, ry, goed ? GRIJS : ROOD, false);
        }
        if (!menu.open()) {
            // on the road: the backpack is on its back
            g.fill(StepstationMenu.RUGZAK_X - 1, StepstationMenu.RUGZAK_Y - 1, StepstationMenu.RUGZAK_X + 53, StepstationMenu.RUGZAK_Y + 53, DICHT);
        }
    }
}
