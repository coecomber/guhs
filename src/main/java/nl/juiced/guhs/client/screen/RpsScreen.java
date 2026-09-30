package nl.juiced.guhs.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.quest.GuhQuests;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/** Rock-paper-scissors-VADS against the Mika-baas: win three times in a row. */
public class RpsScreen extends Screen {
    private static final int W = 240, H = 150;
    private MaagPayloads.RpsState state;
    private int left, top;

    public RpsScreen(MaagPayloads.RpsState state) {
        super(Component.translatable("gui.guhs.rps.title"));
        this.state = state;
    }

    public void update(MaagPayloads.RpsState newState) {
        boolean newMove = newState.vads() != state.vads();
        this.state = newState;
        if (!newState.open()) {
            onClose(); // Guhbert is free!
        } else if (newMove) {
            rebuildWidgets(); // VADS!?
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        // VADS only shows up once the Mika-baas let it slip
        GuhQuests.Rps[] choices = state.vads() ? GuhQuests.Rps.values()
                : new GuhQuests.Rps[]{GuhQuests.Rps.STEEN, GuhQuests.Rps.PAPIER, GuhQuests.Rps.SCHAAR};
        int bw = (W - 20 - 4 * (choices.length - 1)) / choices.length;
        for (int i = 0; i < choices.length; i++) {
            GuhQuests.Rps choice = choices[i];
            Component label = Component.translatable("gui.guhs.rps." + choice.name().toLowerCase(java.util.Locale.ROOT));
            addRenderableWidget(Button.builder(choice == GuhQuests.Rps.VADS ? label.copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD) : label,
                    b -> ClientPacketDistributor.sendToServer(new MaagPayloads.RpsChoice(state.mikaId(), choice.ordinal())))
                    .bounds(left + 10 + i * (bw + 4), top + H - 36, bw, 20).build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF8C1428);
        g.fill(left, top, left + W, top + H, 0xF0241018);
        g.centeredText(font, title, width / 2, top + 10, 0xFFFFD0D8);
        g.centeredText(font, Component.translatable("gui.guhs.rps.rules"), width / 2, top + 26, 0xFFB08090);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < GuhQuests.RPS_WINS_NEEDED; i++) {
            stars.append(i < state.streak() ? "★ " : "☆ ");
        }
        g.centeredText(font, Component.literal(stars.toString().trim()).withStyle(ChatFormatting.GOLD), width / 2, top + 48, 0xFFFFD27A);
        if (state.mikaChoice() >= 0) {
            Component mika = Component.translatable("gui.guhs.rps." + GuhQuests.Rps.values()[state.mikaChoice()].name().toLowerCase(java.util.Locale.ROOT));
            g.centeredText(font, state.won() ? Component.translatable("gui.guhs.rps.win") : Component.translatable("gui.guhs.rps.lose", mika),
                    width / 2, top + 72, state.won() ? 0xFF7CFF8A : 0xFFFF7C7C);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
