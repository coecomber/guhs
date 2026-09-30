package nl.juiced.guhs.feature.creche.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.creche.CrecheGame;
import nl.juiced.guhs.feature.creche.CrechePayloads;

/**
 * Juf Knuffel's screen: she (drawn, looking at you) explains the care round and the minigame; buttons to start either
 * (or stop yours), her shop, and your record and speenmunten. The frame is pastel, with little sleepy stars.
 */
public class CrecheScreen extends Screen {
    private static final int W = 300, H = 196, PIC = 64;
    private static final int PANEL = 0xF02A1A30, BORDER = 0xFFB9D8F7, TEXT = 0xFFFFE6EE;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public CrecheScreen(CrechePayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.juf_knuffel"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new CrechePayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int x = left + 12, w = W - 24;
        int y = top + 22 + PIC + 30;
        boolean running = data.getBoolean("Running"), mine = data.getBoolean("Mine");
        if (mine) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.creche.knop.stop"), b -> send(CrecheGame.STOP))
                    .bounds(x, y, w, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.creche.knop.stop.tooltip"))).build());
        } else {
            boolean feest = data.getBoolean("Feest");
            Button zorg = Button.builder(Component.translatable(feest ? "gui.guhs.creche.knop.knutselen" : "gui.guhs.creche.knop.verzorgen"),
                    b -> send(CrecheGame.VERZORGEN)).bounds(x, y, w / 2 - 2, 20)
                    .tooltip(Tooltip.create(Component.translatable(feest ? "gui.guhs.creche.knop.knutselen.tooltip" : "gui.guhs.creche.knop.verzorgen.tooltip")))
                    .build();
            Button spel = Button.builder(Component.translatable("gui.guhs.creche.knop.terugbrengen"), b -> send(CrecheGame.TERUGBRENGEN))
                    .bounds(x + w / 2 + 2, y, w / 2 - 2, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.creche.knop.terugbrengen.tooltip")))
                    .build();
            zorg.active = spel.active = !running;
            addRenderableWidget(zorg);
            addRenderableWidget(spel);
        }
        int half = (W - 28) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.creche.knop.winkel"), b -> send(CrecheGame.SHOP))
                .bounds(left + 12, top + H - 28, half, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.creche.knop.winkel.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.creche.knop.doei"), b -> onClose())
                .bounds(left + 16 + half, top + H - 28, half, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        long ms = System.currentTimeMillis();
        for (int i = 0; i < 9; i++) {                              // twinkling stars along the top
            int sx = left + 14 + i * 33, sy = top + 4 + (i % 2) * 3;
            int a = (int) (120 + 120 * Math.sin(ms / 400.0 + i));
            g.fill(sx, sy, sx + 2, sy + 2, (a << 24) | 0xFFF3A0);
        }
        g.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), left + 12, top + 10, 0xFFB9D8F7, false);
        Entity npc = minecraft.level == null ? null : minecraft.level.getEntity(npcId);
        g.fill(left + 12, top + 22, left + 12 + PIC, top + 22 + PIC, 0x30B9D8F7);
        if (npc instanceof LivingEntity living) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + 12, top + 22, left + 12 + PIC, top + 22 + PIC, 24, 0.0625f,
                    mouseX, mouseY, living);
        }
        int bx = left + 12 + PIC + 8, by = top + 22, bw = W - (bx - left) - 12;
        g.fill(bx, by, bx + bw, by + PIC, 0xFFF4F8FF);
        g.fill(bx - 4, by + 12, bx, by + 18, 0xFFF4F8FF);
        boolean running = data.getBoolean("Running"), mine = data.getBoolean("Mine");
        Component text = mine ? Component.translatable("gui.guhs.creche.scherm.jij_" + data.getString("Modus").toLowerCase(java.util.Locale.ROOT))
                : running ? Component.translatable("gui.guhs.creche.scherm.bezig", data.getString("Speler"))
                : Component.translatable(data.getBoolean("Feest") ? "gui.guhs.creche.scherm.feest" : "gui.guhs.creche.scherm.uitleg");
        List<FormattedCharSequence> lines = font.split(text, bw - 10);
        for (int i = 0; i < lines.size() && i < 6; i++) {
            g.drawString(font, lines.get(i), bx + 5, by + 5 + i * 10, 0xFF2A2440, false);
        }
        int y = top + 22 + PIC + 6;
        int best = data.getInt("Best");
        g.drawString(font, best > 0 ? Component.translatable("gui.guhs.creche.scherm.best", best) : Component.translatable("gui.guhs.creche.scherm.geen_best"),
                left + 12, y, 0xFFFFD27A, false);
        g.drawString(font, Component.translatable("gui.guhs.creche.scherm.munten", data.getInt("Munten"), data.getInt("Liedjes")), left + 12, y + 11, TEXT, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        Entity npc = minecraft.level == null ? null : minecraft.level.getEntity(npcId);
        if (npc == null || minecraft.player == null || minecraft.player.distanceTo(npc) > 12) {
            onClose();
        }
    }
}
