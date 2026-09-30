package nl.juiced.guhs.feature.beauty.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.beauty.BeautyPayloads;
import nl.juiced.guhs.feature.beauty.BeautyShow;
import nl.juiced.guhs.feature.beauty.ShowTheme;
import nl.juiced.guhs.feature.klassiekers.client.NiveauKeuze;

/**
 * The Showguh's screen: how the beauty contest works, your record, start a show (with a new model or your own guh), or
 * the Showster shop. While someone else is performing: watch from the benches (and shop).
 */
public class ShowguhScreen extends Screen {
    private static final int W = 320, H = 224;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public ShowguhScreen(int npcId, CompoundTag data) {
        super(Component.translatable("gui.guhs.beauty.title"));
        this.npcId = npcId;
        this.data = data;
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new BeautyPayloads.Action(npcId, action, 0));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bw = (W - 44) / 2;
        if (!data.getBooleanOr("Running", false)) {
            NiveauKeuze.knoppen(this::addRenderableWidget, "beauty", left + 20, top + H - 80, W - 40);
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.beauty.start"), b -> send(NiveauKeuze.actie("beauty", BeautyShow.START)))
                    .bounds(left + 20, top + H - 56, bw, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.beauty.start.tooltip"))).build());
            String own = data.getStringOr("OwnGuh", "");
            Button withOwn = Button.builder(own.isEmpty() ? Component.translatable("gui.guhs.beauty.start_own.none")
                                    : Component.translatable("gui.guhs.beauty.start_own", own), b -> send(NiveauKeuze.actie("beauty", BeautyShow.START_OWN)))
                    .bounds(left + 24 + bw, top + H - 56, bw, 20)
                    .tooltip(Tooltip.create(Component.translatable(own.isEmpty() ? "gui.guhs.beauty.start_own.none.tooltip"
                            : "gui.guhs.beauty.start_own.tooltip"))).build();
            withOwn.active = !own.isEmpty();
            addRenderableWidget(withOwn);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.beauty.shop"), b -> send(BeautyShow.SHOP))
                .bounds(left + 20, top + H - 30, bw, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.beauty.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + 24 + bw, top + H - 30, bw, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        DressScreen.frame(g, left, top, W, H);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 10, 0xFFFFE6EE);
        Component text = data.getBooleanOr("Running", false)
                ? Component.translatable("gui.guhs.beauty.running", data.getStringOr("Performer", ""), data.getIntOr("Round", 0), BeautyShow.ROUNDS,
                BeautyShow.themeName(ShowTheme.byId(data.getStringOr("Theme", ""))))
                : Component.translatable("gui.guhs.beauty.explain", BeautyShow.ROUNDS);
        int y = top + 28;
        for (var line : font.split(text, W - 30)) {
            g.centeredText(font, line, width / 2, y, 0xFFD8B8C8);
            y += 11;
        }
        int shows = data.getIntOr("Shows", 0);
        if (shows == 0) {
            g.centeredText(font, Component.translatable("gui.guhs.beauty.best.none"), width / 2, top + H - 96, 0xFFFFD27A);
        } else {
            NiveauKeuze.records(g, font, data, width / 2, top + H - 102, s -> s + "/" + BeautyShow.ROUNDS * BeautyShow.MAX_ROUND, 0);
            g.centeredText(font, Component.translatable("gui.guhs.klassiekers.beauty.shows", shows), width / 2, top + H - 91, 0xFFD8B8C8);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
