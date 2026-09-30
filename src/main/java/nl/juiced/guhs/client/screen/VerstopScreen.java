package nl.juiced.guhs.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.quest.VerstopGame;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * Verstopguhtje's screen: play verstopguh on makkelijk / medium / moeilijk (or join the game that's already running),
 * your best times, and her little shop.
 */
public class VerstopScreen extends Screen {
    private static final int W = 300, H = 170;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public VerstopScreen(MaagPayloads.VerstopOpen open) {
        super(Component.translatable("entity.guhs.guh_npc.verstopguhtje"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action) {
        ClientPacketDistributor.sendToServer(new MaagPayloads.VerstopAction(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        if (data.getBooleanOr("Running", false)) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.verstop.join"), b -> send(VerstopGame.JOIN))
                    .bounds(left + 20, top + 70, W - 40, 20).tooltip(GuhScreen.tip("gui.guhs.verstop.join.tooltip")).build());
        } else {
            VerstopGame.Level[] levels = VerstopGame.Level.values();
            int bw = (W - 40 - 8) / levels.length;
            for (int i = 0; i < levels.length; i++) {
                VerstopGame.Level level = levels[i];
                int action = VerstopGame.START + i;
                addRenderableWidget(Button.builder(Component.translatable("gui.guhs.verstop." + level.id()), b -> send(action))
                        .bounds(left + 20 + i * (bw + 4), top + 70, bw, 20)
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.guhs.verstop.level.tooltip",
                                level.guhs, String.valueOf(level.length).replace('.', ','), level.tickets, VerstopGame.QUICK_BONUS))).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.verstop.shop"), b -> send(VerstopGame.SHOP))
                .bounds(left + 20, top + H - 30, (W - 44) / 2, 20).tooltip(GuhScreen.tip("gui.guhs.verstop.shop.tooltip")).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + (W - 44) / 2, top + H - 30, (W - 44) / 2, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFF7B6CB);
        g.fill(left, top, left + W, top + H, 0xE8301A26);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 10, 0xFFFFE6EE);
        Component question = data.getBooleanOr("Running", false)
                ? Component.translatable("gui.guhs.verstop.running", Component.translatable("gui.guhs.verstop." + data.getStringOr("Level", "")),
                data.getIntOr("Found", 0), data.getIntOr("Total", 0), data.getIntOr("Players", 0))
                : Component.translatable("gui.guhs.verstop.question");
        int y = top + 28;
        for (var line : font.split(question, W - 30)) {
            g.centeredText(font, line, width / 2, y, 0xFFD8B8C8);
            y += 11;
        }
        // best times (only games played alone count)
        StringBuilder best = new StringBuilder();
        for (VerstopGame.Level level : VerstopGame.Level.values()) {
            int ticks = data.getIntOr("Best_" + level.id(), 0);
            if (best.length() > 0) {
                best.append("   ");
            }
            best.append(Component.translatable("gui.guhs.verstop." + level.id()).getString()).append(": ")
                    .append(ticks < 0 ? "-" : VerstopGame.time(ticks));
        }
        g.centeredText(font, Component.translatable("gui.guhs.verstop.best"), width / 2, top + 100, 0xFFFFD27A);
        g.centeredText(font, best.toString(), width / 2, top + 112, 0xFFFFE6EE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
