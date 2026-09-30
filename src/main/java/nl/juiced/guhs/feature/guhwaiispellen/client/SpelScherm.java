package nl.juiced.guhs.feature.guhwaiispellen.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.guhwaiispellen.GuhwaiiSpellenPayloads;
import nl.juiced.guhs.feature.guhwaiispellen.HulaLiedje;
import nl.juiced.guhs.feature.guhwaiispellen.HulaSpel;
import nl.juiced.guhs.feature.guhwaiispellen.SurfSpel;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * Lilo-guh's screen at the surf beach (3.0): surfing (at the surf shack) or the hula (on the podium). How it works, the
 * three levels (makkelijk / medium / lastig, for the hula: the three songs) with your record and the beach's best on each,
 * a stop button while you play, and your schelpjesmunten. A sunset frame with waves; for the hula it pulses on the beat
 * of the song under your mouse.
 */
public class SpelScherm extends Screen {
    private static final int W = 320, H = 214;
    private final int npcId;
    private final CompoundTag data;
    private final boolean surf;
    private final List<Button> niveaus = new ArrayList<>();
    private int left, top;

    public SpelScherm(int npcId, CompoundTag data) {
        super(Component.translatable("gui.guhs.guhwaiispellen." + ("surf".equals(data.getString("Modus")) ? "surf" : "hula") + ".scherm"));
        this.npcId = npcId;
        this.data = data;
        this.surf = "surf".equals(data.getString("Modus"));
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new GuhwaiiSpellenPayloads.Actie(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        niveaus.clear();
        String m = surf ? "surf" : "hula";
        if (data.getBoolean("Mine")) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhwaiispellen." + m + ".stop"), b -> send(surf ? SurfSpel.STOP : HulaSpel.STOP))
                    .bounds(left + 20, top + 100, W - 40, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.guhwaiispellen." + m + ".stop.tooltip")))
                    .build());
        } else {
            int bw = (W - 48) / 3;
            for (Niveau n : Niveau.values()) {
                int best = data.getInt("Best_" + n.id());
                String top3 = data.getString("Top_" + n.id());
                Component label = surf ? n.naam() : HulaLiedje.of(n).naam();
                Component tip = Component.empty().append((surf ? Component.translatable("gui.guhs.guhwaiispellen.surf.niveau." + n.id())
                                : Component.translatable("gui.guhs.guhwaiispellen.hula.niveau." + n.id(), (int) Math.round(HulaLiedje.of(n).bpm))))
                        .append("\n").append(best > 0 ? Component.translatable("gui.guhs.guhwaiispellen.jouw_best", best)
                                : Component.translatable("gui.guhs.guhwaiispellen.nog_geen_best"))
                        .append("\n").append(top3.isEmpty() ? Component.translatable("gui.guhs.guhwaiispellen.strand_best_geen")
                                : Component.translatable("gui.guhs.guhwaiispellen.strand_best", top3));
                Button b = Button.builder(label, x -> send((surf ? SurfSpel.START : HulaSpel.START) + n.ordinal()))
                        .bounds(left + 16 + n.ordinal() * (bw + 8), top + 104, bw, 20).tooltip(Tooltip.create(tip)).build();
                b.active = surf || !data.getBoolean("Running");
                niveaus.add(addRenderableWidget(b));
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W / 2 - 50, top + H - 28, 100, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        long ms = System.currentTimeMillis();
        int kleurA = 0xFF3CE0C8, kleurB = 0xFFFF7EB8;
        double beat = ms / 600.0;
        if (!surf) {
            HulaLiedje l = HulaLiedje.GUHLA_HULA_ROCK;
            for (int i = 0; i < niveaus.size(); i++) {
                if (niveaus.get(i).isMouseOver(mouseX, mouseY)) {
                    l = HulaLiedje.of(Niveau.of(i));
                }
            }
            kleurA = 0xFF000000 | l.kleur;
            kleurB = 0xFF000000 | l.kleur2;
            beat = ms / l.msPerBeat();
        }
        boolean aan = (beat % 1.0) < 0.35;
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, aan ? kleurB : kleurA);
        g.fillGradient(left, top, left + W, top + H, 0xF0203860, 0xF0602848);
        // the sun and the waves at the bottom of the frame
        // (3.0 QA: a small round sun in the top corner, above the text; the waves under the coins line, above Done)
        int zx = left + W - 22, zy = top + 13;
        for (int dy = -7; dy <= 7; dy++) {
            int half = (int) Math.round(Math.sqrt(49.5 - dy * dy));
            g.fill(zx - half, zy + dy, zx + half, zy + dy + 1, 0xC0FFB84D);
        }
        for (int x = left; x < left + W; x += 2) {
            int y = top + H - 33 + (int) Math.round(Math.sin((x - left) / 11.0 + ms / 400.0) * 2.0);
            g.fill(x, y, x + 2, top + H - 30, 0x803CE0C8);
        }
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFFFF0D8);
        String m = surf ? "surf" : "hula";
        Component text = data.getBoolean("Mine") ? Component.translatable("gui.guhs.guhwaiispellen." + m + ".mine")
                : !surf && data.getBoolean("Running") ? Component.translatable("gui.guhs.guhwaiispellen.hula.druk", data.getString("Danser"))
                : Component.translatable("gui.guhs.guhwaiispellen." + m + ".vraag");
        int y = top + 26;
        for (var line : font.split(text, W - 34)) {
            g.drawCenteredString(font, line, width / 2, y, 0xFFE8F4FF);
            y += 10;
        }
        if (!data.getBoolean("Mine")) {
            g.drawCenteredString(font, Component.translatable("gui.guhs.guhwaiispellen." + m + ".kies").withStyle(ChatFormatting.BOLD), width / 2,
                    top + 90, 0xFFFFE14D);
            for (int i = 0; i < niveaus.size(); i++) {
                Button b = niveaus.get(i);
                int best = data.getInt("Best_" + Niveau.of(i).id());
                g.drawCenteredString(font, best > 0 ? Component.translatable("gui.guhs.guhwaiispellen.best_kort", best)
                        : Component.literal("-"), b.getX() + b.getWidth() / 2, b.getY() + 23, 0xFFD8B8E8);
            }
            int ty = top + 140;
            for (var line : font.split(Component.translatable("gui.guhs.guhwaiispellen." + m + ".toetsen"), W - 34)) {
                g.drawCenteredString(font, line, width / 2, ty, 0xFFB8E8F0);
                ty += 10;
            }
        }
        g.drawCenteredString(font, Component.translatable(data.getBoolean("Played") ? "gui.guhs.guhwaiispellen.munten_nu"
                : "gui.guhs.guhwaiispellen.eerste", data.getInt("Munten")), width / 2, top + H - 44, 0xFFFFF0D8);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
