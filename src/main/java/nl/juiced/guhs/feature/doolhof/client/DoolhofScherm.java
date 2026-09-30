package nl.juiced.guhs.feature.doolhof.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.doolhof.DoolhofGame;
import nl.juiced.guhs.feature.doolhof.DoolhofKaart;
import nl.juiced.guhs.feature.doolhof.DoolhofPayloads;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.Highscores;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * Meneer Vadskronkel's screen: how the maze works, three level buttons (makkelijk / medium / lastig, each with its
 * knabbels, Mika's and your best time), who is in the maze right now, and his shop.
 */
public class DoolhofScherm extends Screen {
    private static final int W = 330, H = 232;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public DoolhofScherm(int npcId, CompoundTag data) {
        super(Component.translatable("entity.guhs.guh_npc.doolhofguh"));
        this.npcId = npcId;
        this.data = data;
    }

    private void send(int action) {
        ClientPacketDistributor.sendToServer(new DoolhofPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        boolean vrij = !data.getBooleanOr("Running", false) && data.getBooleanOr("Anker", false);
        int bw = (W - 48) / 3;
        for (Niveau n : Niveau.values()) {
            int i = n.ordinal();
            Button b = Button.builder(n.naam().copy().withStyle(ChatFormatting.BOLD), x -> send(DoolhofGame.START + i))
                    .bounds(left + 16 + i * (bw + 8), top + H - 74, bw, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.doolhof.niveau.tooltip", DoolhofKaart.aantalKnabbels(n),
                            DoolhofKaart.aantalMikas(n), Component.translatable("gui.guhs.doolhof.niveau." + n.id())))).build();
            b.active = vrij;
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.doolhof.shop"), b -> send(DoolhofGame.SHOP))
                .bounds(left + 16, top + H - 30, (W - 40) / 2, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.doolhof.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + (W - 40) / 2, top + H - 30, (W - 40) / 2, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF3F8A3A);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFB8E8A0);
        g.fill(left, top, left + W, top + H, 0xEA1C3218);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFE8FFD8);
        int y = top + 26;
        for (var line : font.split(Component.translatable("gui.guhs.doolhof.rules"), W - 30)) {
            g.centeredText(font, line, width / 2, y, 0xFFDDF2D0);
            y += 10;
        }
        y += 4;
        Component status;
        if (data.getBooleanOr("Running", false)) {
            status = Component.translatable("gui.guhs.doolhof.busy", data.getStringOr("Player", ""), Niveau.of(data.getIntOr("Niveau", 0)).naam(),
                    data.getIntOr("Zak", 0), data.getIntOr("Wil", 0), Highscores.tijd(data.getIntOr("Tijd", 0)));
        } else if (!data.getBooleanOr("Anker", false)) {
            status = Component.translatable("gui.guhs.doolhof.kapot");
        } else {
            status = Component.translatable(data.getIntOr("Games", 0) == 0 ? "gui.guhs.doolhof.first" : "gui.guhs.doolhof.free");
        }
        for (var line : font.split(status, W - 30)) {
            g.centeredText(font, line, width / 2, y, data.getBooleanOr("Running", false) ? 0xFFFFB0B0 : 0xFFB8F0C8);
            y += 10;
        }
        int bw = (W - 48) / 3;
        for (Niveau n : Niveau.values()) {
            int best = data.getIntOr("Best" + n.ordinal(), 0);
            Component rec = best < 0 ? Component.translatable("gui.guhs.doolhof.geen_tijd") : Component.translatable("gui.guhs.doolhof.jouw_tijd", Highscores.tijd(best));
            g.centeredText(font, rec, left + 16 + n.ordinal() * (bw + 8) + bw / 2, top + H - 50, 0xFFFFE27A);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
