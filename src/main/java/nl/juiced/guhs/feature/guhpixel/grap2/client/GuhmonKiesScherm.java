package nl.juiced.guhs.feature.guhpixel.grap2.client;

import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.client.GuhKiezerLijst;
import nl.juiced.guhs.feature.guhpixel.grap2.Grap2Payloads;
import nl.juiced.guhs.feature.guhpixel.grap2.Guhmon;

/**
 * "Kies je Guhmon": the list of your own tamed guhs (a copy fights; the real guh stays home) with the gym's leenguh as
 * the last row, a turnable stand-in on the right and the button "Ik kies jou!".
 */
public class GuhmonKiesScherm extends Screen {
    private static final int W = 300, H = 206;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, DOF = 0xFFB090A0;

    private final CompoundTag data;
    private final GuhKiezerLijst kiezer;
    private int left, top;
    private Button kies;

    public GuhmonKiesScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.guhmon.kies.titel"));
        this.data = data;
        this.kiezer = new GuhKiezerLijst(id -> ververs());
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        kiezer.plaats(left + 8, top + 44, W - 16, H - 44 - 34);
        kiezer.zet(data.getListOrEmpty("Guhs"));
        if (kiezer.gekozen() == null && data.getIntOr("Eigen", 0) == 0) {
            kiezer.kies(Guhmon.LEENGUH_ID);   // (no tamed guhs yet: the leenguh is the only choice)
        }
        kies = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhmon.kies.knop"), b -> {
            UUID id = kiezer.gekozen();
            if (id != null && minecraft != null && minecraft.getConnection() != null) {
                ClientPacketDistributor.sendToServer(new Grap2Payloads.GuhmonDoe(Grap2Payloads.KIES, id.toString()));
                onClose();
            }
        }).bounds(left + W / 2 - 60, top + H - 26, 120, 20).build());
        ververs();
    }

    private void ververs() {
        if (kies != null) {
            kies.active = kiezer.gekozen() != null;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, TEKST);
        Component uitleg = Component.translatable(data.getIntOr("Eigen", 0) == 0 ? "gui.guhs.guhmon.kies.geen_eigen" : "gui.guhs.guhmon.kies.uitleg");
        GidsTekst.alinea(g, uitleg, left + 8, top + 20, W - 16, 0.75f, DOF);
        kiezer.teken(g, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        List<Component> tip = kiezer.tip(mouseX, mouseY);
        if (tip != null && !tip.isEmpty()) {
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return kiezer.wiel(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick) || kiezer.klik(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return kiezer.sleep(event.x(), event.y(), dragX, dragY) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        kiezer.los();
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
