package nl.juiced.guhs.feature.bakkerij.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.bakkerij.BakkerijGame;
import nl.juiced.guhs.feature.bakkerij.BakkerijPayloads;

/**
 * Bakker Korstje's screen: how his order game works, "Bakken!" (when the bakery is free), your record, the
 * receptenboek count, the Knusfeest's feesttaart (when the Burgemeester asked for it), and his shop.
 */
public class KorstjeScherm extends Screen {
    private static final int W = 320, H = 222;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public KorstjeScherm(int npcId, CompoundTag data) {
        super(Component.translatable("entity.guhs.guh_npc.bakkerguh"));
        this.npcId = npcId;
        this.data = data;
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new BakkerijPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        Button play = Button.builder(Component.translatable("gui.guhs.bakkerij.play").withStyle(ChatFormatting.BOLD), b -> send(BakkerijGame.START))
                .bounds(left + 20, top + H - 56, W - 40, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.bakkerij.play.tooltip"))).build();
        play.active = !data.getBoolean("Running");
        addRenderableWidget(play);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.bakkerij.shop"), b -> send(BakkerijGame.SHOP))
                .bounds(left + 20, top + H - 30, (W - 44) / 2, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.bakkerij.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + (W - 44) / 2, top + H - 30, (W - 44) / 2, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFE0A050);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFFFE2A8);
        g.fill(left, top, left + W, top + H, 0xEA3A2418);
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFFFE9C4);
        int y = top + 26;
        for (var line : font.split(Component.translatable("gui.guhs.bakkerij.rules"), W - 30)) {
            g.drawCenteredString(font, line, width / 2, y, 0xFFF3DCC0);
            y += 10;
        }
        y += 4;
        Component status = data.getBoolean("Running")
                ? Component.translatable("gui.guhs.bakkerij.busy", data.getString("Player"), data.getInt("Left"), data.getInt("Score"))
                : data.getBoolean("First") ? Component.translatable("gui.guhs.bakkerij.first") : Component.translatable("gui.guhs.bakkerij.free");
        for (var line : font.split(status, W - 30)) {
            g.drawCenteredString(font, line, width / 2, y, data.getBoolean("Running") ? 0xFFFFA0A0 : 0xFFB8F0C8);
            y += 10;
        }
        if (data.getBoolean("Feest")) {
            y += 2;
            for (var line : font.split(Component.translatable("gui.guhs.bakkerij.feest", data.getInt("FeestMin")), W - 30)) {
                g.drawCenteredString(font, line, width / 2, y, 0xFFFF9FD8);
                y += 10;
            }
        }
        g.drawCenteredString(font, Component.translatable("gui.guhs.bakkerij.record", data.getInt("Best"), data.getInt("Games"), data.getInt("Recepten")),
                width / 2, top + H - 72, 0xFFFFD27A);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
