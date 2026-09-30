package nl.juiced.guhs.feature.elftocht.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.elftocht.ElftochtPayloads;
import nl.juiced.guhs.feature.elftocht.SchaatsmeesterRole;
import nl.juiced.guhs.quest.Highscores;

/**
 * Schaatsmeester Guhglij's screen: how the Elf-Guhjestocht works, your best time and the record, and the buttons
 * "Start de Elf-Guhjestocht!", "Vrij schaatsen", "Stoppen" (while you skate) and his shop. Winter colours: ice blue with
 * an orange edge.
 */
public class SchaatsmeesterScherm extends Screen {
    private static final int W = 320, H = 214;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public SchaatsmeesterScherm(int npcId, CompoundTag data) {
        super(Component.translatable("entity.guhs.guh_npc.schaatsmeesterguh"));
        this.npcId = npcId;
        this.data = data;
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new ElftochtPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bezig = data.getInt("Bezig");
        Button start = Button.builder(Component.translatable("gui.guhs.elftocht.knop.start").withStyle(ChatFormatting.BOLD),
                        b -> send(SchaatsmeesterRole.START))
                .bounds(left + 20, top + H - 80, W - 40, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.elftocht.knop.start.tooltip"))).build();
        start.active = bezig != 1;
        addRenderableWidget(start);
        Button vrij = Button.builder(Component.translatable(bezig == 0 ? "gui.guhs.elftocht.knop.vrij" : "gui.guhs.elftocht.knop.stop"),
                        b -> send(bezig == 0 ? SchaatsmeesterRole.VRIJ : SchaatsmeesterRole.STOP))
                .bounds(left + 20, top + H - 55, W - 40, 20)
                .tooltip(Tooltip.create(Component.translatable(bezig == 0 ? "gui.guhs.elftocht.knop.vrij.tooltip" : "gui.guhs.elftocht.knop.stop.tooltip"))).build();
        addRenderableWidget(vrij);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.elftocht.knop.winkel"), b -> send(SchaatsmeesterRole.SHOP))
                .bounds(left + 20, top + H - 30, (W - 44) / 2, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.elftocht.knop.winkel.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + (W - 44) / 2, top + H - 30, (W - 44) / 2, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFFF9A2E);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFFFFFFF);
        g.fill(left, top, left + W, top + H, 0xEE1C3A5C);
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFFFE2B8);
        int y = top + 26;
        for (var line : font.split(Component.translatable("gui.guhs.elftocht.regels"), W - 30)) {
            g.drawCenteredString(font, line, width / 2, y, 0xFFD8ECFF);
            y += 10;
        }
        y += 4;
        int best = data.getInt("Best");
        Component jij = best >= 0 ? Component.translatable("gui.guhs.elftocht.jouw_best", Highscores.tijd(best), data.getInt("Ritten"))
                : Component.translatable("gui.guhs.elftocht.nog_nooit");
        Component record = data.contains("Record")
                ? Component.translatable("gui.guhs.elftocht.server_best", data.getString("RecordNaam"), Highscores.tijd(data.getInt("Record")))
                : Component.translatable("gui.guhs.elftocht.geen_record");
        // (2.9 visual QA: "nog nooit uitgereden..." was wider than the frame; both lines now wrap, above the buttons)
        var jijRegels = font.split(jij, W - 20);
        var recordRegels = font.split(record, W - 20);
        int ry = Math.max(y, top + H - 83 - 10 * (jijRegels.size() + recordRegels.size()));
        for (var line : jijRegels) {
            g.drawCenteredString(font, line, width / 2, ry, 0xFFB8F0C8);
            ry += 10;
        }
        for (var line : recordRegels) {
            g.drawCenteredString(font, line, width / 2, ry, 0xFFFFD27A);
            ry += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
