package nl.juiced.guhs.feature.circuit.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.circuit.CircuitBanen;
import nl.juiced.guhs.feature.circuit.CircuitPayloads;
import nl.juiced.guhs.feature.circuit.CircuitRole;
import nl.juiced.guhs.feature.race.RaceBaan;
import nl.juiced.guhs.feature.race.RaceGame;
import nl.juiced.guhs.feature.race.RaceRecords;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * Coach Vahoegvroem's screen, the "kiesscherm" of the Guh-Circuit: pick a track (three big coloured buttons) and a level,
 * see what the track has, your records, the track record and the medal times of that level, then RACE (or wait: someone
 * is racing). Below: your ghost and the golden record ghost on/off, the shop. The last choice is remembered.
 */
public class CircuitScreen extends Screen {
    private static final int W = 340, H = 236;
    /** Per track: the button colour and the text colour of its panel. */
    private static final int[] BAAN_KLEUR = {0xFFB06CE0, 0xFFE8B838, 0xFF6FB8E8};
    private static final int[] NIVEAU_KLEUR = {0xFF9CF0B0, 0xFFFFD27A, 0xFFFF8AA8};
    private static int gekozenBaan = 0, gekozenNiveau = 1;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public CircuitScreen(CircuitPayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.circuitguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private RaceBaan baan() {
        return CircuitBanen.BANEN.get(gekozenBaan);
    }

    private void send(int action, boolean close) {
        PacketDistributor.sendToServer(new CircuitPayloads.Action(npcId, action, baan().id, gekozenNiveau));
        if (close) {
            onClose();
        }
    }

    private static Tooltip tip(Component text) {
        return Tooltip.create(text);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bw = (W - 40 - 8) / 3;
        for (int i = 0; i < 3; i++) {
            int k = i;
            RaceBaan b = CircuitBanen.BANEN.get(i);
            Button knop = addRenderableWidget(Button.builder(b.naam().copy().withStyle(i == gekozenBaan ? ChatFormatting.BOLD : ChatFormatting.RESET),
                    x -> {
                        gekozenBaan = k;
                        rebuildWidgets();
                    }).bounds(left + 20 + i * (bw + 4), top + 24, bw, 20).tooltip(tip(Component.translatable("gui.guhs.circuit.baan." + b.id + ".kort"))).build());
            knop.active = i != gekozenBaan;
        }
        for (Niveau n : Niveau.values()) {
            int k = n.ordinal();
            Button knop = addRenderableWidget(Button.builder(n.naam().copy().withStyle(k == gekozenNiveau ? ChatFormatting.BOLD : ChatFormatting.RESET),
                    x -> {
                        gekozenNiveau = k;
                        rebuildWidgets();
                    }).bounds(left + 20 + k * (bw + 4), top + 48, bw, 20).tooltip(tip(Component.translatable("gui.guhs.circuit.niveau." + n.id()))).build());
            knop.active = k != gekozenNiveau;
        }
        Button race = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.circuit.start", baan().naam(), Niveau.of(gekozenNiveau).naam())
                .withStyle(ChatFormatting.GOLD), x -> send(CircuitRole.START, true)).bounds(left + 20, top + 176, W - 40, 20)
                .tooltip(tip(Component.translatable("gui.guhs.circuit.start.tooltip"))).build());
        race.active = !data.getBooleanOr("Busy", false);
        int sw = (W - 40 - 12) / 4;
        addRenderableWidget(Button.builder(Component.translatable(data.getBooleanOr("GhostOn", false) ? "gui.guhs.race.ghost.on" : "gui.guhs.race.ghost.off"),
                x -> send(CircuitRole.GHOST, true)).bounds(left + 20, top + H - 30, sw, 20).tooltip(tip(Component.translatable("gui.guhs.race.ghost.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable(data.getBooleanOr("GoudOn", false) ? "gui.guhs.race.goud.on" : "gui.guhs.race.goud.off"),
                x -> send(CircuitRole.GOUD, true)).bounds(left + 24 + sw, top + H - 30, sw, 20).tooltip(tip(Component.translatable("gui.guhs.race.goud.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.race.shop"), x -> send(CircuitRole.SHOP, true))
                .bounds(left + 28 + sw * 2, top + H - 30, sw, 20).tooltip(tip(Component.translatable("gui.guhs.circuit.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), x -> onClose()).bounds(left + 32 + sw * 3, top + H - 30, sw, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int kleur = BAAN_KLEUR[gekozenBaan];
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, kleur);
        g.fill(left, top, left + W, top + H, 0xE82A1830);
        // a chequered flag strip along the top and a rainbow one along the bottom
        for (int i = 0; i < W / 6; i++) {
            g.fill(left + i * 6, top, left + i * 6 + 6, top + 3, (i % 2 == 0) ? 0xFFFFFFFF : 0xFF202020);
            int rgb = java.awt.Color.HSBtoRGB(i / (float) (W / 6), 0.55f, 1f);
            g.fill(left + i * 6, top + H - 3, left + i * 6 + 6, top + H, 0xFF000000 | rgb);
        }
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFFFE6EE);
        RaceBaan b = baan();
        Niveau n = Niveau.of(gekozenNiveau);
        int y = top + 74;
        Component info = data.getBooleanOr("Busy", false)
                ? Component.translatable("gui.guhs.circuit.busy", data.getStringOr("Racer", ""),
                Component.translatable("gui.guhs.race.baan." + data.getStringOr("RaceBaan", "")), data.getIntOr("Lap", 0))
                : Component.translatable("gui.guhs.circuit.baan." + b.id);
        List<FormattedCharSequence> lines = font.split(info, W - 30);
        for (FormattedCharSequence line : lines.subList(0, Math.min(4, lines.size()))) {
            g.centeredText(font, line, width / 2, y, data.getBooleanOr("Busy", false) ? 0xFFFFB0C8 : 0xFFE8D8F0);
            y += 10;
        }
        String key = b.id + "_" + n.id();
        int ry = top + 120;
        g.text(font, Component.translatable("gui.guhs.circuit.jouw", n.naam()).withStyle(ChatFormatting.BOLD), left + 16, ry, NIVEAU_KLEUR[n.ordinal()]);
        g.text(font, Component.translatable("gui.guhs.race.best", RaceRecords.time(data.getIntOr("Best_" + key, 0)), RaceRecords.time(data.getIntOr("BestLap_" + key, 0)),
                data.getIntOr("Races_" + key, 0)), left + 16, ry + 11, 0xFFFFE6EE);
        int record = data.getIntOr("Record_" + key, 0);
        g.text(font, record < 0 ? Component.translatable("gui.guhs.race.track_record.none")
                : Component.translatable("gui.guhs.race.track_record", data.getStringOr("RecordName_" + key, ""), RaceRecords.time(record)), left + 16, ry + 22, 0xFFFFD27A);
        g.text(font, Component.translatable("gui.guhs.circuit.medailles", RaceRecords.time(b.medalTicks(0, n)), RaceRecords.time(b.medalTicks(1, n)),
                RaceRecords.time(b.medalTicks(2, n)), b.laps), left + 16, ry + 33, 0xFFC8B0D8);
        g.text(font, Component.translatable("gui.guhs.circuit.munten", n.munten(RaceGame.Medal.GOUD.prizes), n.munten(RaceGame.Medal.FINISH.prizes)),
                left + 16, ry + 44, 0xFFB89AAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
