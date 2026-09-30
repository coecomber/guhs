package nl.juiced.guhs.feature.smul.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.smul.SmulGame;
import nl.juiced.guhs.feature.klassiekers.client.NiveauKeuze;
import nl.juiced.guhs.feature.smul.SmulPayloads;

/**
 * The Smulguh's screen: how the eetfestijn game works, "Smullen!" (when the arena is free), your record, and her shop.
 */
public class SmulScreen extends Screen {
    private static final int W = 310, H = 236;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public SmulScreen(int npcId, CompoundTag data) {
        super(Component.translatable("entity.guhs.guh_npc.smulguh"));
        this.npcId = npcId;
        this.data = data;
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new SmulPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        if (!data.getBooleanOr("Running", false)) {
            NiveauKeuze.knoppen(this::addRenderableWidget, "smul", left + 20, top + H - 78, W - 40);
        }
        Button play = Button.builder(Component.translatable("gui.guhs.smul.play").withStyle(ChatFormatting.BOLD),
                        b -> send(NiveauKeuze.actie("smul", SmulGame.START)))
                .bounds(left + 20, top + H - 56, W - 40, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.smul.play.tooltip"))).build();
        play.active = !data.getBooleanOr("Running", false);
        addRenderableWidget(play);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.smul.shop"), b -> send(SmulGame.SHOP))
                .bounds(left + 20, top + H - 30, (W - 44) / 2, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.smul.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + (W - 44) / 2, top + H - 30, (W - 44) / 2, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFFFD27A);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFF7B6CB);
        g.fill(left, top, left + W, top + H, 0xEA3A1A2A);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFFFE6EE);
        int y = top + 26;
        for (var line : font.split(Component.translatable("gui.guhs.smul.rules"), W - 30)) {
            g.centeredText(font, line, width / 2, y, 0xFFE8C8D8);
            y += 10;
        }
        y += 4;
        Component status = data.getBooleanOr("Running", false)
                ? Component.translatable("gui.guhs.smul.busy", data.getStringOr("Player", ""), data.getIntOr("Left", 0), data.getIntOr("Score", 0))
                : data.getBooleanOr("First", false) ? Component.translatable("gui.guhs.smul.first") : Component.translatable("gui.guhs.smul.free");
        for (var line : font.split(status, W - 30)) {
            g.centeredText(font, line, width / 2, y, data.getBooleanOr("Running", false) ? 0xFFFFA0A0 : 0xFFB8F0C8);
            y += 10;
        }
        NiveauKeuze.records(g, font, data, width / 2, top + H - 106, s -> s + " pt", 0);
        g.centeredText(font, Component.translatable("gui.guhs.klassiekers.gespeeld", data.getIntOr("Games", 0)), width / 2, top + H - 94, 0xFFE8C8D8);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
