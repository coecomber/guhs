package nl.juiced.guhs.client.screen;

import net.minecraft.client.input.KeyEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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
        name.setValue(data.getStringOr("Name", ""));
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.rename"),
                        b -> ClientPacketDistributor.sendToServer(new MaagPayloads.ReisAction(npcId, Reisguh.RENAME, name.getValue())))
                .bounds(left + W - 74, top + 25, 62, 20).tooltip(GuhScreen.tip("gui.guhs.reis.rename.tooltip")).build());
        ListTag points = data.getListOrEmpty("Points");
        int pages = Math.max(1, (points.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < PER_PAGE && page * PER_PAGE + i < points.size(); i++) {
            CompoundTag p = points.getCompoundOrEmpty(page * PER_PAGE + i);
            String id = p.read("Id", UUIDUtil.CODEC).orElseThrow().toString();
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.reis.to", p.getStringOr("Name", ""), p.getIntOr("Distance", 0)),
                            b -> {
                                ClientPacketDistributor.sendToServer(new MaagPayloads.ReisAction(npcId, Reisguh.TRAVEL, id));
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
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
        if (name.isFocused() && (keyCode == 257 || keyCode == 335)) {
            ClientPacketDistributor.sendToServer(new MaagPayloads.ReisAction(npcId, Reisguh.RENAME, name.getValue()));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, 0xFF8FD3F5);
        g.fill(left, top, left + W, top + H, 0xE81A2638);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 9, 0xFFE6F6FF);
        g.text(font, Component.translatable("gui.guhs.reis.where"), left + 12, top + 53, 0xFF9FD8F0);
        if (data.getListOrEmpty("Points").isEmpty()) {
            for (var line : font.split(Component.translatable("gui.guhs.reis.none"), W - 24)) {
                g.text(font, line, left + 12, top + 70, 0xFFB0C8D8);
                break;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
