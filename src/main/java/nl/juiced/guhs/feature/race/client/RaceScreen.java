package nl.juiced.guhs.feature.race.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.race.RacePayloads;
import nl.juiced.guhs.feature.race.RaceRecords;
import nl.juiced.guhs.feature.race.RaceRole;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * The Raceguh's screen: race on makkelijk, medium or lastig (or wait: someone is racing), your records and the track
 * record per level, your ghost and the golden record ghost on/off, and her shop with the jockey outfit.
 */
public class RaceScreen extends Screen {
    private static final int W = 320, H = 214;
    private static final int[] START = {RaceRole.START_MAKKELIJK, RaceRole.START, RaceRole.START_LASTIG};
    private static final int[] COLOUR = {0xFF9CF0B0, 0xFFFFD27A, 0xFFFF8AA8};
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public RaceScreen(RacePayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.raceguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action, boolean close) {
        PacketDistributor.sendToServer(new RacePayloads.Action(npcId, action));
        if (close) {
            onClose();
        }
    }

    private static Tooltip tip(String key) {
        return Tooltip.create(Component.translatable(key));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bw3 = (W - 40 - 8) / 3;
        for (Niveau n : Niveau.values()) {
            int i = n.ordinal();
            Button race = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.race.start.niveau", n.naam()), b -> send(START[i], true))
                    .bounds(left + 20 + i * (bw3 + 4), top + 132, bw3, 20).tooltip(tip("gui.guhs.race.start.tooltip." + n.id())).build());
            race.active = !data.getBoolean("Busy");
        }
        int bw = (W - 40 - 12) / 4;
        Button ghost = addRenderableWidget(Button.builder(Component.translatable(data.getBoolean("GhostOn") ? "gui.guhs.race.ghost.on" : "gui.guhs.race.ghost.off"),
                b -> send(RaceRole.GHOST, true)).bounds(left + 20, top + H - 30, bw, 20).tooltip(tip("gui.guhs.race.ghost.tooltip")).build());
        ghost.active = data.getBoolean("HasGhost");
        addRenderableWidget(Button.builder(Component.translatable(data.getBoolean("GoudOn") ? "gui.guhs.race.goud.on" : "gui.guhs.race.goud.off"),
                b -> send(RaceRole.GOUD, true)).bounds(left + 24 + bw, top + H - 30, bw, 20).tooltip(tip("gui.guhs.race.goud.tooltip")).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.race.shop"), b -> send(RaceRole.SHOP, true))
                .bounds(left + 28 + bw * 2, top + H - 30, bw, 20).tooltip(tip("gui.guhs.race.shop.tooltip")).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 32 + bw * 3, top + H - 30, bw, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFF7B6CB);
        g.fill(left, top, left + W, top + H, 0xE8301A26);
        // a chequered flag strip along the top
        for (int i = 0; i < W / 6; i++) {
            g.fill(left + i * 6, top, left + i * 6 + 6, top + 3, (i % 2 == 0) ? 0xFFFFFFFF : 0xFFE0569A);
        }
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 10, 0xFFFFE6EE);
        Component text = data.getBoolean("Busy")
                ? Component.translatable("gui.guhs.race.busy", data.getString("Racer"), data.getInt("Lap"))
                : Component.translatable("gui.guhs.race.question");
        int y = top + 26;
        for (var line : font.split(text, W - 30)) {
            g.drawCenteredString(font, line, width / 2, y, 0xFFD8B8C8);
            y += 11;
        }
        g.drawCenteredString(font, Component.translatable("gui.guhs.race.records"), width / 2, top + 62, 0xFFFFD27A);
        for (Niveau n : Niveau.values()) {
            int row = top + 74 + n.ordinal() * 18;
            g.drawString(font, n.naam().copy().withStyle(ChatFormatting.BOLD), left + 16, row, COLOUR[n.ordinal()]);
            g.drawString(font, Component.translatable("gui.guhs.race.best", RaceRecords.time(data.getInt("Best_" + n.id())),
                    RaceRecords.time(data.getInt("BestLap_" + n.id())), data.getInt("Races_" + n.id())), left + 80, row, 0xFFFFE6EE);
            int record = data.getInt("Record_" + n.id());
            g.drawString(font, record < 0 ? Component.translatable("gui.guhs.race.track_record.none")
                    : Component.translatable("gui.guhs.race.track_record", data.getString("RecordName_" + n.id()), RaceRecords.time(record)),
                    left + 80, row + 9, 0xFFB89AAA);
        }
        // (2.9 visual QA: the prizes line was wider than the frame; now wrapped)
        int py = top + 157;
        for (var line : font.split(Component.translatable("gui.guhs.race.prizes"), W - 30)) {
            g.drawCenteredString(font, line, width / 2, py, 0xFFD8B8C8);
            py += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
