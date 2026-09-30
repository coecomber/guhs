package nl.juiced.guhs.feature.theehuis.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import nl.juiced.guhs.feature.theehuis.TheehuisPayloads;
import nl.juiced.guhs.feature.theehuis.Theekransje;

/**
 * Mevrouw Theelepel's screen: she explains the theekransje (and how to make tea in a theepotje), how many of your tamed
 * guhs are around to come along, a button to start (or stop) a kransje, and during one the gezelligheid so far.
 */
public class TheehuisScreen extends Screen {
    private static final int W = 300, H = 190, PIC = 64;
    private static final int PANEL = 0xF0302018, BORDER = 0xFFF2D7A6, TEXT = 0xFFFFF0DC;
    private final int npcId;
    private final CompoundTag data;
    private int left, top;

    public TheehuisScreen(TheehuisPayloads.Open open) {
        super(Component.translatable("entity.guhs.guh_npc.theeguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new TheehuisPayloads.Action(npcId, action));
        onClose();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int y = top + 22 + PIC + 30;
        if (data.getBooleanOr("Mine", false)) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.theehuis.knop.stop"), b -> send(Theekransje.STOP))
                    .bounds(left + 12, y, W - 24, 20).build());
        } else {
            Button start = Button.builder(Component.translatable("gui.guhs.theehuis.knop.start"), b -> send(Theekransje.START))
                    .bounds(left + 12, y, W - 24, 20).tooltip(Tooltip.create(Component.translatable("gui.guhs.theehuis.knop.start.tooltip"))).build();
            start.active = !data.getBooleanOr("Running", false) && data.getIntOr("Gasten", 0) > 0;
            addRenderableWidget(start);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.theehuis.knop.doei"), b -> onClose())
                .bounds(left + W - 12 - 100, top + H - 28, 100, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        for (int i = 0; i < 12; i++) {                             // a flowery porcelain border
            int fx = left + 8 + i * 24;
            g.fill(fx, top + H - 5, fx + 4, top + H - 2, i % 2 == 0 ? 0xFFF7A6C8 : 0xFFA6D8F7);
        }
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 12, top + 9, 0xFFF2D7A6, false);
        Entity npc = minecraft.level == null ? null : minecraft.level.getEntity(npcId);
        g.fill(left + 12, top + 22, left + 12 + PIC, top + 22 + PIC, 0x30F2D7A6);
        if (npc instanceof LivingEntity living) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + 12, top + 22, left + 12 + PIC, top + 22 + PIC, 24, 0.0625f,
                    mouseX, mouseY, living);
        }
        int bx = left + 12 + PIC + 8, by = top + 22, bw = W - (bx - left) - 12;
        g.fill(bx, by, bx + bw, by + PIC, 0xFFFFF8EE);
        g.fill(bx - 4, by + 12, bx, by + 18, 0xFFFFF8EE);
        Component text;
        if (data.getBooleanOr("Mine", false)) {
            text = Component.translatable("gui.guhs.theehuis.scherm.bezig_jij", data.getIntOr("Gezelligheid", 0), data.getIntOr("Doel", 0));
        } else if (data.getBooleanOr("Running", false)) {
            text = Component.translatable("gui.guhs.theehuis.scherm.bezig", data.getStringOr("Gastheer", ""));
        } else if (data.getIntOr("Gasten", 0) == 0) {
            text = Component.translatable("gui.guhs.theehuis.scherm.geen_guhs");
        } else {
            text = Component.translatable(data.getBooleanOr("Feest", false) ? "gui.guhs.theehuis.scherm.feest" : "gui.guhs.theehuis.scherm.uitleg",
                    data.getIntOr("Gasten", 0));
        }
        List<FormattedCharSequence> lines = font.split(text, bw - 10);
        for (int i = 0; i < lines.size() && i < 6; i++) {
            g.text(font, lines.get(i), bx + 5, by + 5 + i * 10, 0xFF3A2418, false);
        }
        int y = top + 22 + PIC + 6;
        g.text(font, Component.translatable("gui.guhs.theehuis.scherm.kransjes", data.getIntOr("Kransjes", 0), data.getIntOr("Soorten", 0)), left + 12, y,
                0xFFFFD27A, false);
        g.text(font, Component.translatable("gui.guhs.theehuis.scherm.tip"), left + 12, y + 11, TEXT, false);
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
