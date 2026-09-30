package nl.juiced.guhs.feature.kapper.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.kapper.KappersShow;
import nl.juiced.guhs.feature.kapper.KapperPayloads;

/**
 * Kapper Krulletje's screen: how the kappersshow works, your record and krulmunten, start a show (or the feest round
 * when the Burgemeester asked for feestkapsels), and his shop. The frame is a striped barber's pole.
 */
public class KapperScreen extends Screen {
    private static final int W = 300, H = 186;
    private static final int[] PAAL = {0xFFFF8FCB, 0xFFFFFFFF, 0xFF8CCBFF, 0xFFFFFFFF};
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public KapperScreen(int npcId, CompoundTag data) {
        super(Component.translatable("entity.guhs.guh_npc.kapperguh"));
        this.npcId = npcId;
        this.data = data;
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new KapperPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int half = (W - 44) / 2;
        boolean feest = data.getBoolean("Feest");
        boolean running = data.getBoolean("Running");
        int y = top + 112 - (feest ? 22 : 0);
        Button start = Button.builder(Component.translatable("gui.guhs.kapper.start"), b -> send(KappersShow.START))
                .bounds(left + 20, y, W - 40, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.kapper.start.tooltip"))).build();
        start.active = !running;
        addRenderableWidget(start);
        if (feest) {
            Button f = Button.builder(Component.translatable("gui.guhs.kapper.feest").withStyle(ChatFormatting.LIGHT_PURPLE), b -> send(KappersShow.FEEST))
                    .bounds(left + 20, y + 22, W - 40, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.kapper.feest.tooltip", KappersShow.FEEST_MIN))).build();
            f.active = !running;
            addRenderableWidget(f);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.kapper.shop"), b -> send(KappersShow.SHOP))
                .bounds(left + 20, top + H - 30, half, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.kapper.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + half, top + H - 30, half, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        long t = System.currentTimeMillis() / 120;
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFFFFFFFF);
        for (int i = -H; i < W + H; i += 12) {                     // a barber's pole frame, the stripes run round
            int c = PAAL[(int) (((i / 12) + t) & 3)];
            for (int k = 0; k < 6; k++) {
                int x = left - 3 + i + k;
                if (x >= left - 3 && x < left + W + 3) {
                    g.fill(x, top - 3, x + 1, top, c);
                    g.fill(x, top + H, x + 1, top + H + 3, c);
                }
            }
        }
        g.fill(left, top, left + W, top + H, 0xF0301A26);
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFFFE6EE);
        g.drawCenteredString(font, Component.translatable("gui.guhs.kapper.ondertitel"), width / 2, top + 20, 0xFFF7B6CB);
        Component text = data.getBoolean("Running") ? Component.translatable("gui.guhs.kapper.bezet", data.getString("Speler"))
                : Component.translatable("gui.guhs.kapper.uitleg");
        int y = top + 36;
        for (var line : font.split(text, W - 30)) {
            g.drawCenteredString(font, line, width / 2, y, 0xFFD8B8E8);
            y += 11;
        }
        int best = data.getInt("Best");
        g.drawCenteredString(font, best > 0 ? Component.translatable("gui.guhs.kapper.best", best) : Component.translatable("gui.guhs.kapper.geen_best"),
                width / 2, top + 138, 0xFFFFD27A);
        g.drawCenteredString(font, Component.translatable(data.getBoolean("Played") ? "gui.guhs.kapper.munten" : "gui.guhs.kapper.eerste",
                data.getInt("Munten")), width / 2, top + 148, 0xFFFFE6EE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
