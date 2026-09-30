package nl.juiced.guhs.feature.golf.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.chat.MutableComponent;
import nl.juiced.guhs.feature.golf.GolfBanen;
import nl.juiced.guhs.feature.golf.GolfGame;
import nl.juiced.guhs.feature.klassiekers.Klassiekers;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.feature.golf.GolfPayloads;
import nl.juiced.guhs.feature.klassiekers.client.NiveauKeuze;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The Golfguh's screen: play a round of 9 holes (or stop yours, or see who's playing), your records and the club record,
 * the par of every hole, and her little shop.
 */
public class GolfScreen extends Screen {
    private static final int W = 300, H = 214;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public GolfScreen(GolfPayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.golfguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action) {
        ClientPacketDistributor.sendToServer(new GolfPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        boolean mine = data.getBooleanOr("Mine", false);
        if (!mine && !data.getBooleanOr("Running", false)) {
            NiveauKeuze.knoppen(this::addRenderableWidget, "golf", left + 20, top + 60, W - 40);
        }
        Button play = Button.builder(Component.translatable(mine ? "gui.guhs.golf.stop" : "gui.guhs.golf.play"),
                        b -> send(mine ? GolfGame.STOP : NiveauKeuze.actie("golf", GolfGame.START)))
                .bounds(left + 20, top + 82, W - 40, 20)
                .tooltip(Tooltip.create(Component.translatable(mine ? "gui.guhs.golf.stop.tooltip" : "gui.guhs.golf.play.tooltip"))).build();
        play.active = mine || !data.getBooleanOr("Running", false);
        addRenderableWidget(play);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.golf.shop"), b -> send(GolfGame.SHOP))
                .bounds(left + 20, top + H - 30, (W - 44) / 2, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.golf.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + (W - 44) / 2, top + H - 30, (W - 44) / 2, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFF7B6CB);
        g.fill(left, top, left + W, top + H, 0xE8301A26);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 10, 0xFFFFE6EE);
        Component question = data.getBooleanOr("Mine", false) ? Component.translatable("gui.guhs.golf.mine", data.getIntOr("Hole", 0))
                : data.getBooleanOr("Running", false) ? Component.translatable("gui.guhs.golf.busy", data.getStringOr("Player", ""), data.getIntOr("Hole", 0))
                : Component.translatable("gui.guhs.golf.question");
        int y = top + 26;
        for (var line : font.split(question, W - 30)) {
            g.centeredText(font, line, width / 2, y, 0xFFD8B8C8);
            y += 11;
        }
        // the par of every hole, from the tees of the chosen level (2.10: every level has its own tees)
        Niveau gekozen = NiveauKeuze.gekozen("golf");
        int[] pars = pars(gekozen);
        StringBuilder par = new StringBuilder();
        int total = 0;
        for (int i = 0; i < GolfGame.HOLES; i++) {
            par.append(i == 0 ? "" : " ").append(pars[i]);
            total += pars[i];
        }
        g.centeredText(font, Component.translatable("gui.guhs.golf.par_line_niveau", Klassiekers.naam(gekozen), par.toString(), total),
                width / 2, top + 110, 0xFFFFE6EE);
        // your records (per level, against that level's par) and the club record
        MutableComponent line = Component.translatable("gui.guhs.klassiekers.records").withStyle(ChatFormatting.GOLD);
        for (Niveau n : Niveau.values()) {
            int best = data.getIntOr("Best_" + n.id(), 0), nPar = java.util.Arrays.stream(pars(n)).sum();
            line.append(Component.literal(n == Niveau.MAKKELIJK ? " " : "  ·  ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Klassiekers.naam(n))
                    .append(Component.literal(" " + (best == -1 ? "-" : best + " (" + GolfGame.rel(best - nPar) + ")")).withStyle(ChatFormatting.WHITE));
        }
        g.centeredText(font, line, width / 2, top + 126, 0xFFFFD27A);
        g.centeredText(font, Component.translatable("gui.guhs.klassiekers.golf.rondjes", data.getIntOr("Rounds", 0), data.getIntOr("Aces", 0)),
                width / 2, top + 138, 0xFFFFE6EE);
        int club = data.getIntOr("RecordScore", 0);
        g.centeredText(font, club < 0 ? Component.translatable("gui.guhs.golf.club_none")
                : Component.translatable("gui.guhs.golf.club", data.getStringOr("RecordName", ""), club), width / 2, top + 152, 0xFFFFE6EE);
    }

    /** The par per hole of a level on this course (sent by the Golfguh; the plan's pars when she didn't). */
    private int[] pars(Niveau n) {
        int[] p = data.getIntArray("Par_" + n.id()).orElse(new int[0]);
        return p.length == GolfGame.HOLES ? p : GolfBanen.PARS[n.ordinal()];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
