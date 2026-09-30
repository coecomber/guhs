package nl.juiced.guhs.feature.sjoelen.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.sjoelen.SjoelGame;
import nl.juiced.guhs.feature.sjoelen.SjoelenPayloads;

/**
 * Opoe Njegschuif's screen: play a turn of 20 pucks (or stop yours, or see who's playing), the rules of the gates, your
 * record and the house record, and her little shop.
 */
public class SjoelenScreen extends Screen {
    private static final int W = 300, H = 196;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public SjoelenScreen(SjoelenPayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.sjoelguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new SjoelenPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        boolean mine = data.getBooleanOr("Mine", false);
        Button play = Button.builder(Component.translatable(mine ? "gui.guhs.sjoelen.stop" : "gui.guhs.sjoelen.play"),
                        b -> send(mine ? SjoelGame.STOP : SjoelGame.START))
                .bounds(left + 20, top + 62, W - 40, 20)
                .tooltip(Tooltip.create(Component.translatable(mine ? "gui.guhs.sjoelen.stop.tooltip" : "gui.guhs.sjoelen.play.tooltip"))).build();
        play.active = mine || !data.getBooleanOr("Running", false);
        addRenderableWidget(play);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.sjoelen.shop"), b -> send(SjoelGame.SHOP))
                .bounds(left + 20, top + H - 30, (W - 44) / 2, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.sjoelen.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + (W - 44) / 2, top + H - 30, (W - 44) / 2, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFB98A5A);
        g.fill(left, top, left + W, top + H, 0xEA3A2616);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 10, 0xFFFFE6CC);
        Component question = data.getBooleanOr("Mine", false) ? Component.translatable("gui.guhs.sjoelen.mine", data.getIntOr("Left", 0))
                : data.getBooleanOr("Running", false) ? Component.translatable("gui.guhs.sjoelen.busy", data.getStringOr("Player", ""))
                : Component.translatable("gui.guhs.sjoelen.question");
        int y = top + 26;
        for (var line : font.split(question, W - 30)) {
            g.centeredText(font, line, width / 2, y, 0xFFE8D2B8);
            y += 11;
        }
        y = top + 90;
        for (var line : font.split(Component.translatable("gui.guhs.sjoelen.rules"), W - 30)) {
            g.centeredText(font, line, width / 2, y, 0xFFFFE6CC);
            y += 11;
        }
        int best = data.getIntOr("Best", 0);
        g.centeredText(font, Component.translatable("gui.guhs.sjoelen.records"), width / 2, top + 126, 0xFFFFD27A);
        g.centeredText(font, Component.translatable("gui.guhs.sjoelen.best", best < 0 ? "-" : String.valueOf(best), data.getIntOr("Games", 0)),
                width / 2, top + 138, 0xFFFFE6CC);
        int house = data.getIntOr("RecordScore", 0);
        g.centeredText(font, house < 0 ? Component.translatable("gui.guhs.sjoelen.huis_none")
                : Component.translatable("gui.guhs.sjoelen.huis", data.getStringOr("RecordName", ""), house), width / 2, top + 150, 0xFFFFE6CC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
