package nl.juiced.guhs.feature.beauty.client;

import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.beauty.BeautyPayloads;
import nl.juiced.guhs.feature.beauty.BeautyShow;
import nl.juiced.guhs.feature.beauty.ShowTheme;
import nl.juiced.guhs.registry.ModItems;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The loaner wardrobe: the model on the left (turn it with the mouse), the theme and the clock on top, and every piece
 * you may borrow per slot (hat, eyes, neck, body). Click a piece to put it on the model, click the worn one to take it
 * off. Everything happens on the model (the server does the dressing): nothing ever ends up in your inventory.
 */
public class DressScreen extends Screen {
    private static final int W = 330, H = 236, CELL = 18, PER_ROW = 11;
    private static final int GRID_X = 124;
    private final int npcId;
    private final int modelId;
    private final ShowTheme theme;
    private final int round, rounds;
    private final List<GuhClothes> own = new ArrayList<>();
    private final Map<GuhClothes.Slot, List<GuhClothes>> wardrobe = new EnumMap<>(GuhClothes.Slot.class);
    private final Map<GuhClothes, ItemStack> icons = new java.util.HashMap<>();
    private int ticksLeft;
    private int left, top;

    public DressScreen(int npcId, CompoundTag data) {
        super(Component.translatable("gui.guhs.beauty.wardrobe"));
        this.npcId = npcId;
        this.modelId = data.getIntOr("Model", 0);
        this.theme = ShowTheme.byId(data.getStringOr("Theme", ""));
        this.round = data.getIntOr("Round", 0);
        this.rounds = data.getIntOr("Rounds", 0);
        this.ticksLeft = data.getIntOr("Ticks", 0);
        for (int i : data.getIntArray("Own").orElse(new int[0])) {
            GuhClothes c = GuhClothes.byIndex(i);
            if (c != null) {
                own.add(c);
            }
        }
        for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
            wardrobe.put(slot, new ArrayList<>());
        }
        for (int i : data.getIntArray("Wardrobe").orElse(new int[0])) {
            GuhClothes c = GuhClothes.byIndex(i);
            if (c != null && wardrobe.containsKey(c.slot)) {
                wardrobe.get(c.slot).add(c);
                icons.put(c, new ItemStack(ModItems.clothingItem(c)));
            }
        }
    }

    private void send(int action, int value) {
        ClientPacketDistributor.sendToServer(new BeautyPayloads.Action(npcId, action, value));
    }

    @Nullable
    private GuhEntity model() {
        return Minecraft.getInstance().level != null && Minecraft.getInstance().level.getEntity(modelId) instanceof GuhEntity guh ? guh : null;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.beauty.ready").withStyle(ChatFormatting.GOLD), b -> {
            send(BeautyShow.READY, 0);
            onClose();
        }).bounds(left + GRID_X, top + H - 26, 108, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.guhs.beauty.ready.tooltip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.beauty.undress_all"), b -> {
            for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
                send(BeautyShow.UNDRESS, slot.ordinal());
            }
        }).bounds(left + GRID_X + 112, top + H - 26, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.beauty.quit"), b -> {
            send(BeautyShow.QUIT, 0);
            onClose();
        }).bounds(left + 8, top + H - 26, 108, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.guhs.beauty.quit.tooltip"))).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (--ticksLeft <= 0 || model() == null) {
            onClose();              // time's up: the model is on its way to the catwalk
        }
    }

    /** The pink frame all beauty screens share. */
    static void frame(GuiGraphicsExtractor g, int left, int top, int w, int h) {
        g.fill(left - 2, top - 2, left + w + 2, top + h + 2, 0xFFF2C14E);
        g.fill(left - 1, top - 1, left + w + 1, top + h + 1, 0xFFF7B6CB);
        g.fill(left, top, left + w, top + h, 0xEE301A26);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        frame(g, left, top, W, H);
        // the stage: the model on a little pink spotlight circle
        g.fill(left + 6, top + 6, left + 118, top + H - 32, 0xFF4A2338);
        g.fill(left + 22, top + H - 60, left + 102, top + H - 50, 0xFFF7B6CB);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        GuhEntity model = model();
        if (model != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, left + 12, top + 30, left + 112, top + H - 52, 38, 0.0625f,
                    mouseX, mouseY, model);
            g.centeredText(font, model.getDisplayName(), left + 62, top + 12, 0xFFFFE6EE);
        }
        // the theme and the clock
        g.text(font, Component.translatable("gui.guhs.beauty.round", round, rounds), left + GRID_X, top + 8, 0xFFD8B8C8);
        g.pose().pushMatrix();
        g.pose().translate(left + GRID_X, top + 18);
        g.pose().scale(1.4f, 1.4f);
        g.text(font, BeautyShow.themeName(theme).withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFD27A);
        g.pose().popMatrix();
        String clock = BeautyShow.time(Math.max(0, ticksLeft));
        g.text(font, clock, left + W - 8 - font.width(clock), top + 8, ticksLeft <= 200 ? 0xFFFF6060 : 0xFFFFE6EE);
        g.text(font, Component.translatable("gui.guhs.beauty.hint." + theme.id()).withStyle(ChatFormatting.ITALIC),
                left + GRID_X, top + 33, 0xFFB898A8);
        // the wardrobe, slot by slot
        int y = top + 46;
        GuhClothes hovered = null;
        for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
            g.text(font, Component.translatable("gui.guhs.menu.clothes." + slot.name().toLowerCase(java.util.Locale.ROOT)),
                    left + GRID_X, y, 0xFFF7B6CB);
            y += 10;
            List<GuhClothes> pieces = wardrobe.get(slot);
            GuhClothes worn = model == null ? null : model.getClothes(slot);
            for (int i = 0; i < pieces.size(); i++) {
                GuhClothes c = pieces.get(i);
                int x = left + GRID_X + (i % PER_ROW) * CELL, cy = y + (i / PER_ROW) * CELL;
                boolean over = mouseX >= x && mouseX < x + CELL && mouseY >= cy && mouseY < cy + CELL;
                int bg = c == worn ? 0xFFF2C14E : own.contains(c) ? 0xFF8A3A66 : over ? 0xFF7A4A62 : 0xFF4A2338;
                g.fill(x, cy, x + CELL - 1, cy + CELL - 1, bg);
                g.item(icons.get(c), x + 1, cy + 1);
                if (over) {
                    hovered = c;
                }
            }
            y += Math.max(1, (pieces.size() + PER_ROW - 1) / PER_ROW) * CELL + 2;
        }
        if (hovered != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(icons.get(hovered).getHoverName());
            if (own.contains(hovered)) {
                tip.add(Component.translatable("gui.guhs.beauty.own_piece").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            tip.add(Component.translatable(model != null && model.getClothes(hovered.slot) == hovered ? "gui.guhs.beauty.click_off"
                    : "gui.guhs.beauty.click_on").withStyle(ChatFormatting.GRAY));
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        GuhEntity model = model();
        int y = top + 46;
        for (GuhClothes.Slot slot : ShowTheme.SLOTS) {
            y += 10;
            List<GuhClothes> pieces = wardrobe.get(slot);
            for (int i = 0; i < pieces.size(); i++) {
                int x = left + GRID_X + (i % PER_ROW) * CELL, cy = y + (i / PER_ROW) * CELL;
                if (mouseX >= x && mouseX < x + CELL && mouseY >= cy && mouseY < cy + CELL) {
                    GuhClothes c = pieces.get(i);
                    if (model != null && model.getClothes(slot) == c) {
                        send(BeautyShow.UNDRESS, slot.ordinal());
                    } else {
                        send(BeautyShow.DRESS, c.ordinal());
                    }
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.3f));
                    return true;
                }
            }
            y += Math.max(1, (pieces.size() + PER_ROW - 1) / PER_ROW) * CELL + 2;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
