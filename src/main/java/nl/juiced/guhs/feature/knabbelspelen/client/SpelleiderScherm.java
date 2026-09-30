package nl.juiced.guhs.feature.knabbelspelen.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.knabbelspelen.KnabbelspelenPayloads;
import nl.juiced.guhs.feature.knabbelspelen.Onderdeel;
import nl.juiced.guhs.feature.knabbelspelen.Wedstrijd;
import nl.juiced.guhs.quest.Highscores;

/**
 * Juf Vahoegsakee's screen: the six events (each on its own) and the Grote Zeskamp, who's playing now and "Doe mee!",
 * "Start nu!" for the one who opened the round, the score table of the zeskamp, your records, and her shop.
 */
public class SpelleiderScherm extends Screen {
    private static final int W = 360, H = 250;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;
    private boolean tabel;

    public SpelleiderScherm(int npcId, CompoundTag data) {
        super(Component.translatable("entity.guhs.guh_npc.spelleiderguh"));
        this.npcId = npcId;
        this.data = data;
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new KnabbelspelenPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        clearWidgets();
        boolean running = data.getBoolean("Running");
        boolean vrij = !running && data.getBoolean("Anker");
        if (!tabel) {
            int bw = (W - 40) / 3;
            for (Onderdeel o : Onderdeel.values()) {
                int i = o.ordinal();
                int best = data.getInt("Best" + i);
                MutableComponent tip = Component.translatable("gui.guhs.knabbelspelen.uitleg." + o.id()).copy().append("\n\n")
                        .append(best < 0 ? Component.translatable("gui.guhs.knabbelspelen.nog_geen_record")
                                : Component.translatable("gui.guhs.knabbelspelen.jouw_record", o.tijd ? Component.literal(Highscores.tijd(best))
                                        : Component.translatable("quest.guhs.knabbelspelen.punten", best)));
                Component naam = o.naam();
                if (font.width(naam) > bw - 6) {           // (2.9 visual QA: too long for the button: the short name, the full one is in the tooltip)
                    naam = Component.translatableWithFallback("gui.guhs.knabbelspelen.onderdeel." + o.id() + ".kort", naam.getString());
                    tip = o.naam().copy().withStyle(ChatFormatting.BOLD).append("\n").append(tip);
                }
                Button b = Button.builder(naam, x -> send(Wedstrijd.ONDERDEEL + i))
                        .bounds(left + 12 + (i % 3) * (bw + 8), top + 104 + (i / 3) * 24, bw, 20).tooltip(Tooltip.create(tip)).build();
                b.active = vrij;
                addRenderableWidget(b);
            }
            Button zes = Button.builder(Component.translatable("gui.guhs.knabbelspelen.zeskamp").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD),
                            x -> send(Wedstrijd.ZESKAMP)).bounds(left + 12, top + 154, W - 24, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.knabbelspelen.zeskamp.tooltip", data.getInt("BestZeskamp")))).build();
            zes.active = vrij;
            addRenderableWidget(zes);
            if (running && data.getInt("Fase") == Wedstrijd.Fase.INSCHRIJVEN.ordinal()) {
                Button mee = Button.builder(Component.translatable("gui.guhs.knabbelspelen.mee").withStyle(ChatFormatting.BOLD), x -> send(Wedstrijd.MEEDOEN))
                        .bounds(left + 12, top + 180, (W - 32) / 2, 20).build();
                mee.active = !data.getBoolean("Mee") && data.getList("Namen", Tag.TAG_STRING).size() < Wedstrijd.MAX;
                addRenderableWidget(mee);
                Button nu = Button.builder(Component.translatable("gui.guhs.knabbelspelen.nu"), x -> send(Wedstrijd.NU))
                        .bounds(left + 20 + (W - 32) / 2, top + 180, (W - 32) / 2, 20)
                        .tooltip(Tooltip.create(Component.translatable("gui.guhs.knabbelspelen.nu.tooltip"))).build();
                nu.active = data.getBoolean("Host");
                addRenderableWidget(nu);
            }
        }
        int kw = (W - 40) / 3;
        addRenderableWidget(Button.builder(Component.translatable(tabel ? "gui.guhs.knabbelspelen.tabel.dicht" : "gui.guhs.knabbelspelen.tabel"),
                x -> {
                    tabel = !tabel;
                    init();
                }).bounds(left + 12, top + H - 28, kw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.knabbelspelen.shop"), x -> send(Wedstrijd.SHOP))
                .bounds(left + 20 + kw, top + H - 28, kw, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.knabbelspelen.shop.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), x -> onClose()).bounds(left + 28 + 2 * kw, top + H - 28, kw, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFE0406C);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFFFFF0F6);
        g.fill(left, top, left + W, top + H, 0xEC2A1036);
        for (int x = left; x < left + W; x += 20) {                 // circus stripes at the top
            g.fill(x, top, Math.min(x + 10, left + W), top + 5, 0xFFE0406C);
        }
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 10, 0xFFFFE6F0);
        int y = top + 26;
        if (tabel) {
            for (var line : font.split(Component.translatable("gui.guhs.knabbelspelen.tabel.uitleg"), W - 30)) {
                g.drawString(font, line, left + 14, y, 0xFFF6DDEA);
                y += 10;
            }
            return;
        }
        for (var line : font.split(Component.translatable("gui.guhs.knabbelspelen.rules"), W - 30)) {
            g.drawCenteredString(font, line, width / 2, y, 0xFFF6DDEA);
            y += 10;
        }
        y = Math.max(y + 4, top + 74);
        Component status;
        if (!data.getBoolean("Anker")) {
            status = Component.translatable("gui.guhs.knabbelspelen.kapot");
        } else if (data.getBoolean("Running")) {
            StringBuilder namen = new StringBuilder();
            for (Tag t : data.getList("Namen", Tag.TAG_STRING)) {
                namen.append(namen.isEmpty() ? "" : ", ").append(t.getAsString());
            }
            Component wat = data.getBoolean("Zeskamp") ? Component.translatable("gui.guhs.knabbelspelen.zeskamp") : Onderdeel.of(data.getInt("Onderdeel")).naam();
            status = data.getInt("Fase") == Wedstrijd.Fase.INSCHRIJVEN.ordinal()
                    ? Component.translatable("gui.guhs.knabbelspelen.inschrijven", wat, namen.toString(), data.getInt("Nog"))
                    : Component.translatable("gui.guhs.knabbelspelen.bezig", wat, namen.toString());
        } else {
            status = Component.translatable("gui.guhs.knabbelspelen.free");
        }
        for (var line : font.split(status, W - 30)) {
            g.drawCenteredString(font, line, width / 2, y, data.getBoolean("Running") ? 0xFFFFD0A0 : 0xFFB8F0C8);
            y += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
