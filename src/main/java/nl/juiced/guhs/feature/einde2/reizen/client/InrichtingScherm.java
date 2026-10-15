package nl.juiced.guhs.feature.einde2.reizen.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.einde2.reizen.E2ReizenPayloads;
import nl.juiced.guhs.feature.einde2.reizen.Walvis;

/**
 * einde2 phase B, slice reizen: "De rug van je walvis": one switch per part of the fixed set ({@link Walvis#DELEN}: little
 * house, chests, bed, garden, lanterns, flag) and the flag's colour. Every click is sent at once (the server saves it and the
 * whale wears it); the chests keep their items when they are put away.
 */
public class InrichtingScherm extends Screen {
    private static final int W = 240, RIJ = 22, KOP = 48;
    private static final int PANEL = 0xF01E2440, BORDER = 0xFFB89CF0, TEXT = 0xFFF0E8FF;
    /** The colours of the six flags (the model's inr_vlag_<n> bones), for the little square on the button. */
    static final int[] KLEUREN = {0xFFD8343A, 0xFF3A6CD8, 0xFFF2C43A, 0xFF4CAE4C, 0xFF9A50D8, 0xFFF29AC4};
    private int delen, vlag;
    private int left, top;

    public InrichtingScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.einde2_reizen.inrichting.titel"));
        delen = data.getIntOr("Delen", 0);
        vlag = data.getIntOr("Vlag", 0);
    }

    private int hoogte() {
        return KOP + (Walvis.DELEN.size() + 1) * RIJ + 32;
    }

    @Override
    protected void init() {
        clearWidgets();
        left = (width - W) / 2;
        top = Math.max(4, (height - hoogte()) / 2);
        int y = top + KOP;
        List<String> lijst = Walvis.DELEN;
        for (int i = 0; i < lijst.size(); i++) {
            String deel = lijst.get(i);
            boolean aan = (delen & (1 << i)) != 0;
            MutableComponent tekst = Component.translatable("gui.guhs.einde2_reizen.inrichting.deel." + deel).copy()
                    .append(Component.literal(": "))
                    .append(Component.translatable(aan ? "gui.guhs.einde2_reizen.inrichting.aan" : "gui.guhs.einde2_reizen.inrichting.uit")
                            .withStyle(aan ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            int bit = i;
            addRenderableWidget(Button.builder(tekst, b -> zet(deel, bit)).bounds(left + 10, y, W - 20, 20).build());
            y += RIJ;
        }
        MutableComponent kleur = Component.translatable("gui.guhs.einde2_reizen.inrichting.vlag_kleur").copy().append(Component.literal(": "))
                .append(Component.translatable("gui.guhs.einde2_reizen.inrichting.kleur." + vlag));
        addRenderableWidget(Button.builder(kleur, b -> {
            vlag = (vlag + 1) % Walvis.VLAGGEN;
            ClientPacketDistributor.sendToServer(new E2ReizenPayloads.InrichtingZet("vlag_kleur", vlag));
            init();
        }).bounds(left + 10, y, W - 34, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_reizen.inrichting.klaar"), b -> onClose())
                .bounds(left + W - 10 - 96, top + hoogte() - 26, 96, 20).build());
    }

    private void zet(String deel, int bit) {
        boolean aan = (delen & (1 << bit)) == 0;
        delen = aan ? delen | (1 << bit) : delen & ~(1 << bit);
        ClientPacketDistributor.sendToServer(new E2ReizenPayloads.InrichtingZet(deel, aan ? 1 : 0));
        init();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + hoogte() + 1, BORDER);
        g.fill(left, top, left + W, top + hoogte(), PANEL);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 10, top + 8, 0xFFD8C4FF, false);
        List<FormattedCharSequence> regels = font.split(Component.translatable("gui.guhs.einde2_reizen.inrichting.uitleg"), W - 20);
        for (int i = 0; i < regels.size() && i < 2; i++) {
            g.text(font, regels.get(i), left + 10, top + 21 + i * 10, TEXT, false);
        }
        int y = top + KOP + Walvis.DELEN.size() * RIJ;
        g.fill(left + W - 22, y + 4, left + W - 10, y + 16, 0xFF000000);
        g.fill(left + W - 21, y + 5, left + W - 11, y + 15, KLEUREN[Math.floorMod(vlag, KLEUREN.length)]);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
