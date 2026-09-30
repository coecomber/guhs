package nl.juiced.guhs.feature.disco.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.disco.DiscoGame;
import nl.juiced.guhs.feature.disco.DiscoLiedje;
import nl.juiced.guhs.feature.disco.DiscoPayloads;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The DJ-guh's screen (2.9): how Guhdisco works, pick a song (the song is the level: Vadsige Tango, the Ze-hangen
 * remix, Mika-Mambo, or the bonus Njeg-Njeg Boogie) with your record on each, or stop dancing, and his shop. The frame
 * blinks on the beat of the song under your mouse, in that song's colours.
 */
public class DiscoScreen extends Screen {
    private static final int W = 320, H = 206;
    private final int npcId;
    private final CompoundTag data;
    private final List<Button> songs = new ArrayList<>();
    private int left, top;

    public DiscoScreen(DiscoPayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.djguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action) {
        ClientPacketDistributor.sendToServer(new DiscoPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        songs.clear();
        int half = (W - 44) / 2;
        if (data.getBooleanOr("Mine", false)) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.disco.stop"), b -> send(DiscoGame.STOP))
                    .bounds(left + 20, top + 92, W - 40, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.disco.stop.tooltip"))).build());
        } else {
            DiscoLiedje[] all = DiscoLiedje.values();
            for (int i = 0; i < all.length; i++) {
                DiscoLiedje l = all[i];
                int best = data.getIntOr("Best_" + l.id, 0);
                Component label = Component.translatable("gui.guhs.disco.lied.knop", l.naam(), l.niveauNaam());
                if (font.width(label) > half - 8) {        // (too long for the button: the short name, the full one is in the tooltip)
                    label = Component.translatable("gui.guhs.disco.lied.knop",
                            Component.translatableWithFallback("gui.guhs.disco.lied." + l.id + ".kort", l.naam().getString()), l.niveauNaam());
                }
                Component tip = l.naam().copy().withStyle(ChatFormatting.BOLD).append("\n")
                        .append(Component.translatable("gui.guhs.disco.lied." + l.id + ".tooltip"))
                        .append("\n").append(Component.translatable("gui.guhs.disco.lied.bpm", (int) Math.round(l.bpm)))
                        .append("\n").append(best > 0 ? Component.translatable("gui.guhs.disco.best", best) : Component.translatable("gui.guhs.disco.no_best"));
                Button b = Button.builder(label, x -> send(DiscoGame.START_LIED + l.ordinal()))
                        .bounds(left + 20 + (i % 2) * (half + 4), top + 84 + (i / 2) * 26, half, 20).tooltip(Tooltip.create(tip)).build();
                b.active = !data.getBooleanOr("Running", false);
                songs.add(addRenderableWidget(b));
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.disco.shop"), b -> send(DiscoGame.SHOP))
                .bounds(left + 20, top + H - 30, half, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.disco.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 24 + half, top + H - 30, half, 20).build());
    }

    /** The song under the mouse (or the one being danced, or the standard song): its colours and beat light the frame. */
    private DiscoLiedje shown(int mouseX, int mouseY) {
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).isMouseOver(mouseX, mouseY)) {
                return DiscoLiedje.of(i);
            }
        }
        return data.getBooleanOr("Running", false) ? DiscoLiedje.of(data.getIntOr("Liedje", 0)) : DiscoLiedje.DISCO70;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        DiscoLiedje l = shown(mouseX, mouseY);
        long ms = System.currentTimeMillis();
        int beat = (int) ((ms * l.bpm / 60000.0) % 4);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF000000 | l.kleur(beat));
        g.fill(left, top, left + W, top + H, 0xF01A1026);
        for (int i = 0; i < 4; i++) {                              // four little tiles under the title, on the beat
            int x = width / 2 - 34 + i * 18;
            int c = l.kleur(i);
            g.fill(x, top + 22, x + 14, top + 26, i == beat ? 0xFF000000 | c : 0x66000000 | c);
        }
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFFFE6EE);
        Component text = data.getBooleanOr("Mine", false) ? Component.translatable("gui.guhs.disco.mine", data.getIntOr("Round", 0))
                : data.getBooleanOr("Running", false) ? Component.translatable("gui.guhs.disco.running", data.getStringOr("Dancer", ""), data.getIntOr("Round", 0),
                DiscoLiedje.of(data.getIntOr("Liedje", 0)).naam())
                : Component.translatable("gui.guhs.disco.question");
        int y = top + 32;
        for (var line : font.split(text, W - 30)) {
            g.centeredText(font, line, width / 2, y, 0xFFD8B8E8);
            y += 10;
        }
        if (!data.getBooleanOr("Mine", false)) {
            g.centeredText(font, Component.translatable("gui.guhs.disco.kies").withStyle(ChatFormatting.BOLD), width / 2, top + 72, 0xFFFFE14D);
            for (int i = 0; i < songs.size(); i++) {               // a stripe in each song's colours under its button
                Button b = songs.get(i);
                DiscoLiedje s = DiscoLiedje.of(i);
                for (int k = 0; k < 4; k++) {
                    int x0 = b.getX() + k * b.getWidth() / 4;
                    g.fill(x0, b.getY() + 21, x0 + b.getWidth() / 4, b.getY() + 23, 0xFF000000 | s.kleur(k));
                }
            }
        }
        g.centeredText(font, Component.translatable(data.getBooleanOr("Played", false) ? "gui.guhs.disco.munten" : "gui.guhs.disco.first",
                data.getIntOr("Munten", 0)), width / 2, top + H - 44, 0xFFFFE6EE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
