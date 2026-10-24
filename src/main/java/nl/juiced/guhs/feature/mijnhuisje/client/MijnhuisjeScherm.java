package nl.juiced.guhs.feature.mijnhuisje.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import nl.juiced.guhs.feature.mijnhuisje.MijnGebied;
import nl.juiced.guhs.feature.mijnhuisje.MijnhuisjeBlockEntity;
import nl.juiced.guhs.feature.mijnhuisje.MijnhuisjeMenu;

/**
 * The Mijn-guhhuisje's screen: what the guhs are doing, three sliders (blocks to each side on x and z, down to y), "Laat
 * zien" (the pink outline), work on/off, "Rugzak legen", the slots for kaasknabbels and a Banksleutel, the residents (with
 * "uit huis"), what was mined. Drawn with plain fills like the Haltepaaltje's screen.
 */
public class MijnhuisjeScherm extends AbstractContainerScreen<MijnhuisjeMenu> {
    private static final int BG = 0xFF3A1F2C, PANEL = 0xFFF3DCE8, SLOT = 0xFF8A5A76, SLOT_IN = 0xFFC79BB3;
    private static final int TEXT = 0xFF4A2A40, GRIJS = 0xFF85707E, GROEN = 0xFF2E8B4A, ORANJE = 0xFFC2661A, ROOD = 0xFFB02A4A;
    private Button aan;

    public MijnhuisjeScherm(MijnhuisjeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MijnhuisjeMenu.BREED, MijnhuisjeMenu.HOOG);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = MijnhuisjeMenu.INV_X;
        this.inventoryLabelY = MijnhuisjeMenu.INV_Y - 10;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 8, w = imageWidth - 16;
        addRenderableWidget(new Schuif(x, topPos + 28, w, "gui.guhs.mijnhuisje.x", 0, MijnGebied.MAX_ZIJ, menu.startRx, MijnhuisjeMenu.KNOP_RX));
        addRenderableWidget(new Schuif(x, topPos + 46, w, "gui.guhs.mijnhuisje.z", 0, MijnGebied.MAX_ZIJ, menu.startRz, MijnhuisjeMenu.KNOP_RZ));
        int boven = menu.pos.getY() - 1, min = MijnGebied.MIN_Y;
        Schuif bodem = new Schuif(x, topPos + 64, w, "gui.guhs.mijnhuisje.bodem", Math.min(min, boven), Math.max(min, boven),
                menu.startBodem, MijnhuisjeMenu.KNOP_BODEM + MijnhuisjeMenu.BODEM_VERSCHUIF);
        bodem.active = boven > min;
        addRenderableWidget(bodem);
        int bw = (w - 8) / 3;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.mijnhuisje.laat_zien"), b -> klik(MijnhuisjeMenu.KNOP_TOON))
                .bounds(x, topPos + 83, bw, 16).tooltip(Tooltip.create(Component.translatable("gui.guhs.mijnhuisje.laat_zien.tooltip"))).build());
        aan = addRenderableWidget(Button.builder(aanTekst(), b -> klik(MijnhuisjeMenu.KNOP_AAN)).bounds(x + bw + 4, topPos + 83, bw, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.mijnhuisje.rugzak_legen"), b -> klik(MijnhuisjeMenu.KNOP_RUGZAK))
                .bounds(x + 2 * (bw + 4), topPos + 83, bw, 16).tooltip(Tooltip.create(Component.translatable("gui.guhs.mijnhuisje.rugzak_legen.tooltip"))).build());
        for (int i = 0; i < menu.namen.size(); i++) {
            final int knop = MijnhuisjeMenu.KNOP_UIT + i;
            addRenderableWidget(Button.builder(Component.literal("x"), b -> klik(knop)).bounds(leftPos + imageWidth - 20, topPos + 103 + i * 11, 12, 10)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.mijnhuisje.uit_huis", menu.namen.get(i)))).build());
        }
    }

    private Component aanTekst() {
        return Component.translatable(menu.vlag(8) ? "gui.guhs.mijnhuisje.pauze" : "gui.guhs.mijnhuisje.verder");
    }

    private void klik(int knop) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, knop);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (aan != null) {
            aan.setMessage(aanTekst());
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < 2; i++) {
                if (isHovering(MijnhuisjeMenu.VAK_X, MijnhuisjeMenu.VAK_Y + i * 20, 16, 16, mouseX, mouseY) && !menu.getSlot(i).hasItem()) {
                    g.setTooltipForNextFrame(font, font.split(Component.translatable(i == 0 ? "gui.guhs.mijnhuisje.vak.knabbels" : "gui.guhs.mijnhuisje.vak.sleutel"), 170),
                            mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, BG);
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        for (int i = 0; i < 2; i++) {
            vak(g, x + MijnhuisjeMenu.VAK_X - 1, y + MijnhuisjeMenu.VAK_Y - 1 + i * 20);
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                vak(g, x + MijnhuisjeMenu.INV_X - 1 + c * 18, y + MijnhuisjeMenu.INV_Y - 1 + r * 18);
            }
        }
        for (int c = 0; c < 9; c++) {
            vak(g, x + MijnhuisjeMenu.INV_X - 1 + c * 18, y + MijnhuisjeMenu.INV_Y + 57);
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
        int s = menu.status();
        int kleur = s == MijnhuisjeBlockEntity.BEZIG ? GROEN : s == MijnhuisjeBlockEntity.KLAAR ? GROEN
                : s == MijnhuisjeBlockEntity.VOL || s == MijnhuisjeBlockEntity.GEEN_OPSLAG ? ROOD : ORANJE;
        g.text(font, Component.translatable("gui.guhs.mijnhuisje.status." + s, menu.werkers(), menu.aantal()), 8, 17, kleur, false);
        // the slots' little labels
        g.text(font, Component.translatable("gui.guhs.mijnhuisje.knabbels" + (menu.vlag(16) ? ".aan" : "")), 28, MijnhuisjeMenu.VAK_Y + 4,
                menu.vlag(16) ? GROEN : GRIJS, false);
        g.text(font, Component.translatable("gui.guhs.mijnhuisje.bank" + (menu.vlag(2) ? ".aan" : "")), 28, MijnhuisjeMenu.VAK_Y + 24,
                menu.vlag(2) ? GROEN : GRIJS, false);
        // the residents
        int rx = 92;
        g.text(font, Component.translatable("gui.guhs.mijnhuisje.bewoners", menu.namen.size(), MijnhuisjeBlockEntity.MAX_BEWONERS), rx, 92 + 3, TEXT, false);
        if (menu.namen.isEmpty()) {
            for (var regel : font.split(Component.translatable("gui.guhs.mijnhuisje.geen_bewoners"), imageWidth - rx - 6)) {
                g.text(font, regel, rx, 104 + 0, GRIJS, false);
                break;
            }
        }
        for (int i = 0; i < menu.namen.size(); i++) {
            String n = menu.namen.get(i);
            g.text(font, font.plainSubstrByWidth(n, imageWidth - rx - 24), rx, 104 + i * 11, TEXT, false);
        }
        // what was mined, where the work is, what the bags hold, whether there is a chest
        Component info = Component.translatable("gui.guhs.mijnhuisje.info", menu.gemijnd(), menu.laag());
        g.text(font, info, 8, 141, GRIJS, false);
        Component opslag = Component.translatable("gui.guhs.mijnhuisje.opslag", Component.translatable(menu.vlag(1) ? "gui.guhs.mijnhuisje.ja" : "gui.guhs.mijnhuisje.nee"),
                menu.vlag(4) ? Component.translatable("gui.guhs.mijnhuisje.rugzak", menu.rugzak()) : Component.empty());
        g.text(font, opslag, 8, 130, menu.vlag(1) ? GRIJS : ORANJE, false);
    }

    /** A slider for one number, sent as a menu button when it changes. */
    private class Schuif extends AbstractSliderButton {
        private final String sleutel;
        private final int min, max, knop;
        private int laatst;

        Schuif(int x, int y, int w, String sleutel, int min, int max, int waarde, int knop) {
            super(x, y, w, 16, Component.empty(), max == min ? 0 : (Math.clamp(waarde, min, max) - min) / (double) (max - min));
            this.sleutel = sleutel;
            this.min = min;
            this.max = max;
            this.knop = knop;
            this.laatst = Math.clamp(waarde, min, max);
            updateMessage();
        }

        private int getal() {
            return min + (int) Math.round(value * (max - min));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(sleutel, getal()).withStyle(ChatFormatting.WHITE));
        }

        @Override
        protected void applyValue() {
            int n = getal();
            if (n != laatst) {
                laatst = n;
                klik(knop + n);
            }
        }
    }
}
