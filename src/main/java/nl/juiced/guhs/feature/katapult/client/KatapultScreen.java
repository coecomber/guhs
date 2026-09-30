package nl.juiced.guhs.feature.katapult.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.katapult.KatapultGame;
import nl.juiced.guhs.feature.katapult.KatapultPayloads;
import nl.juiced.guhs.feature.klassiekers.Klassiekers;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * Kapitein Floepguh's screen: pick a level (makkelijk 5 pluisballen per fort and an aiming line, medium 4, lastig 3 and
 * wind) and start with the play button, or stop your run, your record and the castle record per level, and his little
 * shop. Like the klassiekers' screens, the level buttons only choose (the chosen one framed "» Lastig «", remembered while
 * the game runs, medium at first); "Floepen maar!" starts on that level.
 */
public class KatapultScreen extends Screen {
    private static final int W = 320, H = 226;
    private static Niveau gekozen = Niveau.MEDIUM;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public KatapultScreen(KatapultPayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.katapultguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new KatapultPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        boolean mine = data.getBoolean("Mine"), running = data.getBoolean("Running");
        if (mine) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.katapult.stop"), b -> send(KatapultGame.STOP))
                    .bounds(left + 20, top + 62, W - 40, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.katapult.stop.tooltip"))).build());
        } else {
            int gap = 4, bw = (W - 40 - 2 * gap) / 3;
            List<Button> levels = new ArrayList<>();
            for (Niveau n : Niveau.values()) {
                Button b = Button.builder(label(n), x -> {
                            gekozen = n;
                            for (Niveau m : Niveau.values()) {
                                levels.get(m.ordinal()).setMessage(label(m));
                            }
                        }).bounds(left + 20 + n.ordinal() * (bw + gap), top + 58, bw, 18)
                        .tooltip(Tooltip.create(Component.translatable("gui.guhs.katapult.play." + n.id()))).build();
                b.active = !running;
                levels.add(b);
                addRenderableWidget(b);
            }
            Button play = Button.builder(Component.translatable("gui.guhs.katapult.play").withStyle(ChatFormatting.BOLD),
                            b -> send(KatapultGame.START + gekozen.ordinal()))
                    .bounds(left + 20, top + 80, W - 40, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.katapult.play.tooltip"))).build();
            play.active = !running;
            addRenderableWidget(play);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.katapult.shop"), b -> send(KatapultGame.SHOP))
                .bounds(left + 20, top + H - 30, (W - 44) / 2, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.katapult.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + (W - 44) / 2, top + H - 30, (W - 44) / 2, 20).build());
    }

    /** "Medium" in the level's colour; the chosen level framed and bold: "» Medium «". */
    private static Component label(Niveau n) {
        MutableComponent name = n.naam().copy().withStyle(Klassiekers.kleur(n));
        return gekozen == n ? Component.literal("» ").append(name.withStyle(ChatFormatting.BOLD)).append(" «").withStyle(Klassiekers.kleur(n)) : name;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFF7B6CB);
        g.fill(left, top, left + W, top + H, 0xEA2A1A30);
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 10, 0xFFFFE6EE);
        Component question = data.getBoolean("Mine") ? Component.translatable("gui.guhs.katapult.mine", data.getInt("Fort"))
                : data.getBoolean("Running") ? Component.translatable("gui.guhs.katapult.busy", data.getString("Player"), data.getInt("Fort"))
                : Component.translatable("gui.guhs.katapult.question");
        int y = top + 26;
        for (var line : font.split(question, W - 30)) {
            g.drawCenteredString(font, line, width / 2, y, 0xFFD8B8C8);
            y += 11;
        }
        y = top + 108;
        for (var line : font.split(Component.translatable("gui.guhs.katapult.rules"), W - 30)) {
            g.drawCenteredString(font, line, width / 2, y, 0xFFFFE6EE);
            y += 11;
        }
        y = Math.max(y + 4, top + 142);
        g.drawCenteredString(font, Component.translatable("gui.guhs.katapult.records", data.getInt("Runs")), width / 2, y, 0xFFFFD27A);
        for (Niveau n : Niveau.values()) {
            y += 12;
            int best = data.getInt("Best_" + n.id()), rec = data.getInt("RecordScore_" + n.id());
            Component castle = rec < 0 ? Component.translatable("gui.guhs.katapult.kasteel_none")
                    : Component.translatable("gui.guhs.katapult.kasteel", data.getString("RecordName_" + n.id()), rec);
            g.drawCenteredString(font, Component.translatable("gui.guhs.katapult.best", n.naam(), best < 0 ? "-" : String.valueOf(best), castle),
                    width / 2, y, 0xFFFFE6EE);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
