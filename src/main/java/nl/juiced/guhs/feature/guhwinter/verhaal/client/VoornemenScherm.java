package nl.juiced.guhs.feature.guhwinter.verhaal.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhwinter.verhaal.WinterVerhaalPayloads;

/**
 * The writing card: one line (at most 80 characters) with your goede voornemen; "Ophangen" pins it on the prikbord (the
 * server cleans and checks it and answers with the board).
 */
public class VoornemenScherm extends Screen {
    private static final int W = 280, H = 132, PAPIER = 0xFFFFF8E8, RAND = 0xFFE0384C, INKT = 0xFF3A1E2E, DOF = 0xFF8A6A5A;

    private final CompoundTag data;
    private final int max;
    private EditBox tekst;
    private int left, top;

    public VoornemenScherm(CompoundTag data) {
        super(Component.translatable("guhwinter.verhaal.kaartje.titel"));
        this.data = data;
        this.max = Math.max(10, data.getIntOr("Max", 80));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        String oud = tekst != null ? tekst.getValue() : data.getStringOr("Eigen", "");
        tekst = new EditBox(font, left + 12, top + 52, W - 24, 18, Component.translatable("guhwinter.verhaal.kaartje.titel"));
        tekst.setMaxLength(max);
        tekst.setValue(oud);
        addRenderableWidget(tekst);
        setInitialFocus(tekst);
        addRenderableWidget(Button.builder(Component.translatable("guhwinter.verhaal.kaartje.ophangen"), b -> stuur())
                .bounds(left + 12, top + H - 30, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("guhwinter.verhaal.kaartje.terug"), b -> minecraft.setScreen(new PrikbordScherm(data)))
                .bounds(left + W - 92, top + H - 30, 80, 20).build());
    }

    private void stuur() {
        String t = tekst.getValue().strip();
        if (t.isEmpty()) {
            return;
        }
        ClientPacketDistributor.sendToServer(new WinterVerhaalPayloads.Voornemen(t));
        onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (tekst.isFocused() && (key == 257 || key == 335)) {
            stuur();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, RAND);
        g.fill(left, top, left + W, top + H, PAPIER);
        g.fill(left + W / 2 - 3, top - 4, left + W / 2 + 3, top + 2, RAND);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 12, top + 10, RAND, false);
        List<FormattedCharSequence> uitleg = font.split(Component.translatable("guhwinter.verhaal.kaartje.uitleg"), W - 24);
        for (int i = 0; i < Math.min(2, uitleg.size()); i++) {
            g.text(font, uitleg.get(i), left + 12, top + 24 + i * 10, DOF, false);
        }
        String teller = (tekst == null ? 0 : tekst.getValue().length()) + "/" + max;
        g.text(font, teller, left + W - 12 - font.width(teller), top + 74, DOF, false);
        g.text(font, Component.translatable("guhwinter.verhaal.kaartje.iedereen"), left + 12, top + 74, INKT, false);
    }
}
