package nl.juiced.guhs.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhSleeEntity;
import nl.juiced.guhs.network.SledControlPayload;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/** The sled's control panel: start/stop, 3 speeds, and turn around. */
public class SledPanelScreen extends Screen {
    private static final int W = 220, H = 96;
    private final GuhSleeEntity sled;
    private int left, top;
    private Button startStop;
    private final Button[] speeds = new Button[3];

    public SledPanelScreen(GuhSleeEntity sled) {
        super(Component.translatable("gui.guhs.sled.title"));
        this.sled = sled;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        startStop = addRenderableWidget(Button.builder(Component.empty(), b -> send(sled.isRunning() ? SledControlPayload.STOP : SledControlPayload.START))
                .bounds(left + 10, top + 24, 98, 20).tooltip(GuhScreen.tip("gui.guhs.sled.start.tooltip")).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.sled.reverse"), b -> send(SledControlPayload.REVERSE))
                .bounds(left + 112, top + 24, 98, 20).tooltip(GuhScreen.tip("gui.guhs.sled.reverse.tooltip")).build());
        for (int i = 0; i < 3; i++) {
            int speed = i + 1;
            speeds[i] = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.sled.speed." + speed),
                    b -> send(SledControlPayload.SPEED + speed)).bounds(left + 10 + i * 68, top + 64, 64, 20)
                    .tooltip(GuhScreen.tip("gui.guhs.sled.speed.tooltip")).build());
        }
    }

    private void send(int action) {
        ClientPacketDistributor.sendToServer(new SledControlPayload(action));
    }

    @Override
    public void tick() {
        if (minecraft.player == null || minecraft.player.getVehicle() != sled) {
            onClose();
            return;
        }
        startStop.setMessage(Component.translatable(sled.isRunning() ? "gui.guhs.sled.stop" : "gui.guhs.sled.start"));
        for (int i = 0; i < 3; i++) {
            speeds[i].active = sled.getSpeed() != i + 1;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFF7B6CB);
        g.fill(left, top, left + W, top + H, 0xE0FFF4F8);
        g.centeredText(font, title, width / 2, top + 8, 0xFF7A2848);
        g.centeredText(font, Component.translatable("gui.guhs.sled.speed"), width / 2, top + 52, 0xFF7A2848);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
