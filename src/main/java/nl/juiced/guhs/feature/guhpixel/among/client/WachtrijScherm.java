package nl.juiced.guhs.feature.guhpixel.among.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhpixel.among.AmongPayloads;
import nl.juiced.guhs.taal.Tekst;

/**
 * The queue at the Kapitein-guh: who waits, who is ready, the difficulty (the leader picks it). The round starts when
 * everybody in the list pressed "klaar". Closing the screen keeps your place (talk to the Kapitein again to get it back);
 * "Verlaten" gives it up.
 */
public class WachtrijScherm extends Screen {
    private static final int W = 250, H = 214;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, GROEN = 0xFF68D88A;

    private CompoundTag data;
    private int left, top;

    public WachtrijScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.among.wachtrij.titel"));
        this.data = data;
    }

    public void update(CompoundTag nieuw) {
        this.data = nieuw;
        if (width > 0) {
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        boolean klaar = data.getBooleanOr("Klaar", false), lastig = data.getBooleanOr("Lastig", false);
        addRenderableWidget(Button.builder(Component.translatable(klaar ? "gui.guhs.among.wachtrij.niet_klaar" : "gui.guhs.among.wachtrij.klaar"),
                b -> ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.WACHTRIJ_KLAAR, 0, 0))).bounds(left + 12, top + H - 50, 110, 20).build());
        Button niveau = Button.builder(Component.translatable("gui.guhs.among.wachtrij.niveau",
                        Component.translatable(lastig ? "gui.guhs.among.niveau.lastig" : "gui.guhs.among.niveau.normaal")),
                b -> ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.WACHTRIJ_NIVEAU, 0, 0))).bounds(left + 128, top + H - 50, 110, 20).build();
        niveau.active = data.getBooleanOr("Leider", false);
        addRenderableWidget(niveau);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.among.wachtrij.verlaten"), b -> {
            ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.WACHTRIJ_WEG, 0, 0));
            onClose();
        }).bounds(left + 12, top + H - 26, 110, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.among.wachtrij.sluiten"), b -> onClose()).bounds(left + 128, top + H - 26, 110, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 8, TEKST);
        boolean lastig = data.getBooleanOr("Lastig", false);
        g.centeredText(font, Component.translatable(lastig ? "gui.guhs.among.wachtrij.regels.lastig" : "gui.guhs.among.wachtrij.regels.normaal"),
                width / 2, top + 21, lastig ? GOUD : DOF);
        ListTag spelers = data.getListOrEmpty("Spelers");
        int y = top + 38;
        for (int i = 0; i < spelers.size(); i++) {
            CompoundTag s = spelers.getCompoundOrEmpty(i);
            boolean klaar = s.getBooleanOr("Klaar", false);
            Component naam = s.getBooleanOr("Leider", false) ? Component.translatable("gui.guhs.among.wachtrij.leider", Tekst.get(s, "Naam")) : Tekst.get(s, "Naam");
            g.text(font, naam, left + 16, y, TEKST, false);
            Component stand = Component.translatable(klaar ? "gui.guhs.among.wachtrij.is_klaar" : "gui.guhs.among.wachtrij.wacht");
            g.text(font, stand, left + W - 16 - font.width(stand), y, klaar ? GROEN : DOF, false);
            y += 11;
        }
        int npcs = Math.max(0, data.getIntOr("Deelnemers", 9) - spelers.size());
        g.text(font, Component.translatable("gui.guhs.among.wachtrij.guhs", npcs), left + 16, y + 2, DOF, false);
        int klaar = 0;
        for (int i = 0; i < spelers.size(); i++) {
            klaar += spelers.getCompoundOrEmpty(i).getBooleanOr("Klaar", false) ? 1 : 0;
        }
        g.centeredText(font, Component.translatable("gui.guhs.among.wachtrij.stand", klaar, spelers.size()), width / 2, top + H - 63, GOUD);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
