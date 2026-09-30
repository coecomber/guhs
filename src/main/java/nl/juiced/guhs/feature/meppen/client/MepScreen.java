package nl.juiced.guhs.feature.meppen.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.meppen.MepGame;
import nl.juiced.guhs.feature.meppen.MepPayloads;
import nl.juiced.guhs.feature.klassiekers.client.NiveauKeuze;

/**
 * The Mepguh's screen: start a game of Mika meppen (or stop yours / watch someone else's), how it works, your record and
 * the hall's, and the shop. Makkelijk, medium or lastig: three level buttons above the play button.
 */
public class MepScreen extends Screen {
    private static final int W = 300, H = 214;   // (2.9 visual QA: 206 -> 214, the records + coins lines ran into the buttons)
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public MepScreen(MepPayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.mepguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new MepPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bw = (W - 48) / 2;
        if (data.getBoolean("You")) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.mika_mep.stop"), b -> send(MepGame.STOP))
                    .bounds(left + 20, top + 96, W - 40, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.mika_mep.stop.tooltip"))).build());
        } else {
            NiveauKeuze.knoppen(this::addRenderableWidget, "meppen", left + 20, top + 72, W - 40);
            Button play = Button.builder(Component.translatable("gui.guhs.mika_mep.play").withStyle(ChatFormatting.BOLD),
                            b -> send(NiveauKeuze.actie("meppen", MepGame.START)))
                    .bounds(left + 20, top + 96, W - 40, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.mika_mep.play.tooltip"))).build();
            play.active = !data.getBoolean("Running");
            addRenderableWidget(play);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.mika_mep.help"), b -> send(MepGame.HELP))
                .bounds(left + 20, top + H - 54, bw, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.mika_mep.help.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.mika_mep.shop"), b -> send(MepGame.SHOP))
                .bounds(left + 28 + bw, top + H - 54, bw, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.mika_mep.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 20, top + H - 28, W - 40, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFF5C542);
        g.fill(left, top, left + W, top + H, 0xE8301A26);
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 10, 0xFFFFE6EE);
        Component question = data.getBoolean("You") ? Component.translatable("gui.guhs.mika_mep.question.you", data.getInt("Left"))
                : data.getBoolean("Running") ? Component.translatable("gui.guhs.mika_mep.question.busy", data.getString("Player"), data.getInt("Left"))
                : Component.translatable("gui.guhs.mika_mep.question");
        int y = top + 28;
        for (var line : font.split(question, W - 30)) {
            g.drawCenteredString(font, line, width / 2, y, 0xFFD8B8C8);
            y += 11;
        }
        NiveauKeuze.records(g, font, data, width / 2, top + 124, s -> String.valueOf(s), 0);
        g.drawCenteredString(font, Component.translatable("gui.guhs.klassiekers.wereldrecord", data.getInt("HallBest")), width / 2, top + 136, 0xFFFFE6EE);
        g.drawCenteredString(font, Component.translatable("gui.guhs.mika_mep.coins", data.getInt("Coins")), width / 2, top + 147, 0xFFFFE6EE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
