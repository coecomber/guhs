package nl.juiced.guhs.client.screen;

import java.util.ArrayList;
import java.util.List;

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
import nl.juiced.guhs.world.GuhWorldData;

import net.minecraft.core.UUIDUtil;
/** The Maagenzym-guh's menu: who may come into your stomach (and build), and how big it is / can get. */
public class MaagSettingsScreen extends Screen {
    private static final int W = 260, H = 230;
    private static final int BG = 0xF0301A26, BORDER = 0xFFF7B6CB, TEXT = 0xFFFFE6EE;
    private CompoundTag data;
    private EditBox nameBox;
    private int left, top;

    public MaagSettingsScreen(CompoundTag data) {
        super(Component.translatable("gui.guhs.maag.settings"));
        this.data = data;
    }

    public void update(CompoundTag newData) {
        this.data = newData;
        String typed = nameBox == null ? "" : nameBox.getValue();
        rebuildWidgets();
        nameBox.setValue(typed);
    }

    private static void send(int action, String text) {
        PacketDistributor.sendToServer(new MaagPayloads.MaagSettingsAction(action, text));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        String access = data.getStringOr("Access", "");
        int x = left + 10;
        int y = top + 26;
        GuhWorldData.Access[] modes = GuhWorldData.Access.values();
        int bw = (W - 20 - 8) / 3;
        for (int i = 0; i < modes.length; i++) {
            GuhWorldData.Access mode = modes[i];
            boolean current = mode.name().equals(access);
            Component label = Component.translatable("gui.guhs.maag.access." + mode.name().toLowerCase(java.util.Locale.ROOT));
            Button b = Button.builder(current ? label.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD) : label,
                    btn -> send(MaagPayloads.MaagSettingsAction.SET_ACCESS, mode.name())).bounds(x + i * (bw + 4), y, bw, 20).build();
            b.active = !current;
            addRenderableWidget(b);
        }
        y += 58;
        ListTag list = data.getListOrEmpty("Whitelist");
        for (int i = 0; i < list.size() && i < 5; i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            String id = e.read("Id", UUIDUtil.CODEC).orElseThrow().toString();
            boolean build = e.getBooleanOr("Build", false);
            addRenderableWidget(Button.builder(Component.translatable(build ? "gui.guhs.maag.build_on" : "gui.guhs.maag.build_off"),
                    b -> send(MaagPayloads.MaagSettingsAction.TOGGLE_BUILD, id)).bounds(left + W - 110, y + i * 22, 70, 20).build());
            addRenderableWidget(Button.builder(Component.literal("✖"), b -> send(MaagPayloads.MaagSettingsAction.REMOVE, id))
                    .bounds(left + W - 36, y + i * 22, 24, 20).build());
        }
        int ay = top + H - 32;
        nameBox = new EditBox(font, left + 11, ay + 1, W - 100, 18, Component.translatable("gui.guhs.maag.add_name"));
        nameBox.setHint(Component.translatable("gui.guhs.maag.add_name").withStyle(ChatFormatting.GRAY));
        nameBox.setMaxLength(16);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.maag.add"), b -> {
            if (!nameBox.getValue().isBlank()) {
                send(MaagPayloads.MaagSettingsAction.ADD, nameBox.getValue());
                nameBox.setValue("");
            }
        }).bounds(left + W - 84, ay, 74, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, BG);
        g.text(font, title, left + 10, top + 8, TEXT);
        int size = data.getIntOr("Size", 0), next = data.getIntOr("NextSize", 0);
        g.text(font, Component.translatable("gui.guhs.maag.size", size, size), left + 10, top + 52, BORDER);
        g.text(font, next > 0 ? Component.translatable("gui.guhs.maag.grow", next, next) : Component.translatable("gui.guhs.maag.max"),
                left + 10, top + 64, 0xFFD8B8C8);
        List<Component> names = new ArrayList<>();
        ListTag list = data.getListOrEmpty("Whitelist");
        for (int i = 0; i < list.size() && i < 5; i++) {
            g.text(font, list.getCompoundOrEmpty(i).getStringOr("Name", ""), left + 12, top + 84 + i * 22 + 6, TEXT);
        }
        if (list.isEmpty()) {
            g.text(font, Component.translatable("gui.guhs.maag.whitelist_empty").withStyle(ChatFormatting.GRAY), left + 12, top + 90, 0xFF9A8090);
        }
        if (!"WHITELIST".equals(data.getStringOr("Access", "")) && !list.isEmpty()) {
            g.text(font, Component.translatable("gui.guhs.maag.whitelist_off").withStyle(ChatFormatting.ITALIC), left + 12, top + H - 46, 0xFF9A8090);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
