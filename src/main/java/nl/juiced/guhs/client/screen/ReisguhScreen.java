package nl.juiced.guhs.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.network.MaagPayloads;
import nl.juiced.guhs.quest.Reisguh;

/** A Reisguh's menu: its name (rename it) and every other Reisguh you've discovered (click to travel there). */
public class ReisguhScreen extends Screen {
    private static final int W = 280, H = 236, PER_PAGE = 7;
    private final int npcId;
    private CompoundTag data;
    private int left, top, page;
    private EditBox name;

    public ReisguhScreen(MaagPayloads.ReisOpen open) {
        super(Component.translatable("entity.guhs.guh_npc.reisguh"));
        this.npcId = open.npcId();
        this.data = open.data();
    }

    public void update(MaagPayloads.ReisOpen open) {
        this.data = open.data();
        rebuildWidgets();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        name = new EditBox(font, left + 12, top + 26, W - 90, 18, Component.translatable("gui.guhs.reis.name"));
        name.setMaxLength(Reisguh.MAX_NAME);
        name.setValue(data.getString("Name"));
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.rename"),
                        b -> PacketDistributor.sendToServer(new MaagPayloads.ReisAction(npcId, Reisguh.RENAME, name.getValue())))
                .bounds(left + W - 74, top + 25, 62, 20).tooltip(GuhScreen.tip("gui.guhs.reis.rename.tooltip")).build());
        ListTag points = data.getList("Points", Tag.TAG_COMPOUND);
        int pages = Math.max(1, (points.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < PER_PAGE && page * PER_PAGE + i < points.size(); i++) {
            CompoundTag p = points.getCompound(page * PER_PAGE + i);
            String id = p.getUUID("Id").toString();
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.reis.to", p.getString("Name"), p.getInt("Distance")),
                            b -> {
                                PacketDistributor.sendToServer(new MaagPayloads.ReisAction(npcId, Reisguh.TRAVEL, id));
                                onClose();
                            })
                    .bounds(left + 12, top + 66 + i * 22, W - 24, 20).tooltip(GuhScreen.tip("gui.guhs.reis.to.tooltip")).build());
        }
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left + 12, top + H - 28, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + 36, top + H - 28, 20, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 92, top + H - 28, 80, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (name.isFocused() && (keyCode == 257 || keyCode == 335)) {
            PacketDistributor.sendToServer(new MaagPayloads.ReisAction(npcId, Reisguh.RENAME, name.getValue()));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF8FD3F5);
        g.fill(left, top, left + W, top + H, 0xE81A2638);
        g.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFE6F6FF);
        g.drawString(font, Component.translatable("gui.guhs.reis.where"), left + 12, top + 53, 0xFF9FD8F0);
        if (data.getList("Points", Tag.TAG_COMPOUND).isEmpty()) {
            for (var line : font.split(Component.translatable("gui.guhs.reis.none"), W - 24)) {
                g.drawString(font, line, left + 12, top + 70, 0xFFB0C8D8);
                break;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
