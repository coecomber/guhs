package nl.juiced.guhs.feature.vissen.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.vissen.VisSoort;
import nl.juiced.guhs.feature.vissen.VisWedstrijd;
import nl.juiced.guhs.feature.vissen.VissenPayloads;
import nl.juiced.guhs.feature.klassiekers.Klassiekers;
import nl.juiced.guhs.feature.klassiekers.client.NiveauKeuze;
import nl.juiced.guhs.feature.spelen.Niveau;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The Visguh's screen: start a Guhvis-wedstrijd (or join / stop the one that's running), your own records, the pond's
 * record board and her stall.
 */
public class VissenScreen extends Screen {
    private static final int W = 320, H = 244;
    /** Everything below the buttons moves down this much (the level buttons, 2.9). */
    private static final int D = 26;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public VissenScreen(VissenPayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.visguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action) {
        ClientPacketDistributor.sendToServer(new VissenPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        Button main;
        if (data.getBooleanOr("Playing", false)) {
            main = Button.builder(Component.translatable("gui.guhs.vissen.stop"), b -> send(VisWedstrijd.STOP))
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.vissen.stop.tooltip"))).build();
        } else if (data.getBooleanOr("Running", false)) {
            main = Button.builder(Component.translatable("gui.guhs.vissen.join"), b -> send(VisWedstrijd.JOIN))
                    .tooltip(Tooltip.create(Component.translatable(data.getBooleanOr("Joinable", false) ? "gui.guhs.vissen.join.tooltip"
                            : "gui.guhs.vissen.join.closed"))).build();
            main.active = data.getBooleanOr("Joinable", false);
        } else {
            NiveauKeuze.knoppen(this::addRenderableWidget, "vissen", left + 30, top + 76, W - 60);
            main = Button.builder(Component.translatable("gui.guhs.vissen.start"), b -> send(NiveauKeuze.actie("vissen", VisWedstrijd.START)))
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.vissen.start.tooltip", VisWedstrijd.DURATION / 20 / 60))).build();
        }
        main.setRectangle(W - 60, 20, left + 30, top + 76 + D);
        addRenderableWidget(main);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.vissen.shop"), b -> send(VisWedstrijd.SHOP))
                .bounds(left + 20, top + H - 28, (W - 44) / 2, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.vissen.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + (W - 44) / 2, top + H - 28, (W - 44) / 2, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF7FD6E8);
        g.fill(left, top, left + W, top + H, 0xE8142A36);
        g.fill(left, top + 102 + D, left + W, top + 103 + D, 0x557FD6E8);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFE6FAFF);
        Component text;
        if (data.getBooleanOr("Playing", false)) {
            text = Component.translatable("gui.guhs.vissen.playing", VisWedstrijd.time(data.getIntOr("Seconds", 0) * 20));
        } else if (data.getBooleanOr("Running", false)) {
            text = Component.translatable("gui.guhs.vissen.running", data.getIntOr("Players", 0), VisWedstrijd.time(data.getIntOr("Seconds", 0) * 20))
                    .append(" ").append(Component.translatable("gui.guhs.klassiekers.vissen.niveau", Klassiekers.naam(Niveau.byId(data.getStringOr("Niveau", "")))));
        } else {
            text = Component.translatable("gui.guhs.vissen.question", VisWedstrijd.DURATION / 20 / 60);
        }
        int y = top + 24;
        for (var line : font.split(text, W - 30)) {
            g.centeredText(font, line, width / 2, y, 0xFFB8DCE8);
            y += 10;
        }

        // your records (left: per level) and the pond's record board (right: the level you picked, or the one being fished)
        int lx = left + 14, rx = left + W / 2 + 6, y0 = top + 110 + D;
        g.text(font, Component.translatable("gui.guhs.vissen.mine").withStyle(ChatFormatting.BOLD), lx, y0, 0xFFFFD27A);
        for (Niveau n : Niveau.values()) {
            int best = data.getIntOr("Best_" + n.id(), 0);
            g.text(font, Component.literal(" ").append(Klassiekers.naam(n)).append(Component.literal(": " + (best > 0 ? best + " pt" : "-"))
                    .withStyle(ChatFormatting.WHITE)), lx, y0 + 13 + n.ordinal() * 10, 0xFFE6FAFF);
        }
        VisSoort heavy = VisSoort.byId(data.getStringOr("HeaviestSoort", ""));
        g.text(font, Component.translatable("gui.guhs.vissen.mine.heaviest"), lx, y0 + 46, 0xFFE6FAFF);
        g.text(font, heavy == null ? Component.literal("  -") : Component.literal("  ").append(Component.translatable("item.guhs." + heavy.id()))
                .append(" " + VisSoort.kg(data.getIntOr("Heaviest", 0))), lx, y0 + 57, 0xFFB8DCE8);
        g.text(font, Component.translatable("gui.guhs.vissen.mine.games", data.getIntOr("Games", 0)), lx, y0 + 69, 0xFFE6FAFF);

        Niveau shown = data.getBooleanOr("Running", false) ? Niveau.byId(data.getStringOr("Niveau", "")) : NiveauKeuze.gekozen("vissen");
        // (2.9 visual QA: "Top 3 van de wereld Medium" ran out of the frame; now shrunk to fit the right column)
        Component board = Component.translatable("gui.guhs.vissen.board").withStyle(ChatFormatting.BOLD).append(" ")
                .append(Klassiekers.naam(shown));
        float bs = Math.min(1f, (left + W - 10 - rx) / (float) Math.max(1, font.width(board)));
        g.pose().pushMatrix();
        g.pose().translate(rx, y0 + (1 - bs) * 4);
        g.pose().scale(bs, bs);
        g.text(font, board, 0, 0, 0xFFFFD27A);
        g.pose().popMatrix();
        ListTag topList = data.getListOrEmpty("Top_" + shown.id());
        if (topList.isEmpty()) {
            g.text(font, Component.translatable("gui.guhs.vissen.board.empty"), rx, y0 + 13, 0xFFB8DCE8);
        }
        for (int i = 0; i < Math.min(3, topList.size()); i++) {
            CompoundTag e = topList.getCompoundOrEmpty(i);
            String line = (i + 1) + ". " + e.getStringOr("Name", "");
            g.text(font, font.plainSubstrByWidth(line, W / 2 - 50), rx, y0 + 13 + i * 10, i == 0 ? 0xFFFFE08A : 0xFFE6FAFF);
            String pts = String.valueOf(e.getIntOr("Points", 0));
            g.text(font, pts, left + W - 14 - font.width(pts), y0 + 13 + i * 10, 0xFFB8DCE8);
        }
        ListTag heaviestList = data.getListOrEmpty("Zwaarste_" + shown.id());
        if (!heaviestList.isEmpty()) {
            CompoundTag z = heaviestList.getCompoundOrEmpty(0);
            g.text(font, Component.translatable("gui.guhs.vissen.board.heaviest", z.getStringOr("Name", "")), rx, y0 + 47, 0xFFFFD27A);
            g.text(font, Component.literal("  " + VisSoort.kg(z.getIntOr("Points", 0))), rx, y0 + 58, 0xFFB8DCE8);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
