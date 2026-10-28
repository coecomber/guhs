package nl.juiced.guhs.feature.hamsterbuis.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.hamsterbuis.HamsterPayloads;
import nl.juiced.guhs.feature.hamsterbuis.HamsterbuisFeature;

/**
 * A hamsterbuis station's screen: its name (the owner renames it, and opens it to everyone or not), the owner's buffer and
 * what a trip costs, and every station you may travel to (name, coordinates, dimension in the tooltip: click and woesj).
 */
public class HamsterbuisScreen extends Screen {
    private static final int W = 270, MAX_H = 250, ROW = 22, BOTTOM = 34;
    private final BlockPos station;
    private CompoundTag data;
    private int left, top, h, listY, perPage, page;
    private EditBox naam;
    private String getypt;

    public HamsterbuisScreen(HamsterPayloads.Open open) {
        super(Component.translatable("block.guhs.hamsterbuis_station"));
        this.station = open.station();
        this.data = open.data();
    }

    public BlockPos station() {
        return station;
    }

    public void update(HamsterPayloads.Open open) {
        if (naam != null) {
            getypt = naam.getValue();
        }
        this.data = open.data();
        rebuildWidgets();
    }

    private void stuur(int actie, String tekst) {
        ClientPacketDistributor.sendToServer(new HamsterPayloads.Actie(station, actie, tekst));
    }

    private boolean eigen() {
        return data.getBooleanOr("Eigenaar", false);
    }

    private List<CompoundTag> rijen() {
        List<CompoundTag> uit = new ArrayList<>();
        ListTag l = data.getListOrEmpty("Stations");
        for (int i = 0; i < l.size(); i++) {
            uit.add(l.getCompoundOrEmpty(i));
        }
        return uit;
    }

    @Override
    protected void init() {
        h = Math.min(MAX_H, height - 8);
        left = (width - W) / 2;
        top = (height - h) / 2;
        int y = top + 22;
        if (eigen()) {
            naam = new EditBox(font, left + 32, y, W - 110, 18, Component.translatable("gui.guhs.hamsterbuis.naam"));
            naam.setMaxLength(24);
            naam.setValue(getypt != null ? getypt : data.getStringOr("Naam", ""));
            addRenderableWidget(naam);
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.rename"), b -> stuur(HamsterPayloads.Actie.HERNOEM, naam.getValue()))
                    .bounds(left + W - 74, y - 1, 62, 20).build());
            addRenderableWidget(Button.builder(Component.translatable(data.getBooleanOr("Openbaar", false) ? "gui.guhs.hamsterbuis.openbaar"
                            : "gui.guhs.hamsterbuis.prive"), b -> stuur(HamsterPayloads.Actie.OPENBAAR, ""))
                    .bounds(left + W - 74, y + 21, 62, 16)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.hamsterbuis.openbaar.tooltip"))).build());
        }
        listY = top + 78;
        perPage = Math.max(1, (h - (listY - top) - BOTTOM) / ROW);
        List<CompoundTag> rijen = rijen();
        int pages = Math.max(1, (rijen.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < perPage && page * perPage + i < rijen.size(); i++) {
            CompoundTag r = rijen.get(page * perPage + i);
            String id = r.getStringOr("Id", "");
            Component tekst = Component.translatable("gui.guhs.hamsterbuis.rij", r.getStringOr("Naam", "?"),
                    r.getIntOr("X", 0), r.getIntOr("Y", 0), r.getIntOr("Z", 0));
            Button b = Button.builder(tekst, x -> {
                        stuur(HamsterPayloads.Actie.REIS, id);
                        onClose();
                    }).bounds(left + 32, listY + i * ROW, W - 44, 20)
                    .tooltip(Tooltip.create(Component.translatable(r.getBooleanOr("Draait", true) ? "gui.guhs.hamsterbuis.ga"
                            : "gui.guhs.hamsterbuis.ga_stil", r.getStringOr("Naam", "?"), r.getStringOr("Dim", "")))).build();
            b.active = r.getBooleanOr("Draait", true);
            addRenderableWidget(b);
        }
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left + 12, top + h - 24, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + 36, top + h - 24, 20, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 92, top + h - 24, 80, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + h + 1, 0xFFF0A0C8);
        g.fill(left, top, left + W, top + h, 0xF02A1424);
        g.centeredText(font, (eigen() ? title : Component.literal(data.getStringOr("Naam", ""))).copy().withStyle(ChatFormatting.BOLD), width / 2,
                top + 8, 0xFFFFE0F0);
        if (!eigen()) {
            g.text(font, Component.literal(data.getStringOr("Naam", "")), left + 32, top + 27, 0xFFF0D0E0);
        }
        Component stand = !data.getBooleanOr("Knooppunt", false) ? Component.translatable("gui.guhs.hamsterbuis.geen_knooppunt")
                : Component.translatable("gui.guhs.hamsterbuis.lading", data.getLongOr("Lading", 0L), data.getLongOr("Max", 0L), data.getIntOr("Kost", 0));
        g.text(font, stand, left + 12, top + 64, data.getBooleanOr("Draait", false) ? 0xFF9FE8B0 : 0xFFF0B070);
        if (rijen().isEmpty()) {
            int y = listY + 2;
            for (var regel : font.split(Component.translatable("gui.guhs.hamsterbuis.geen"), W - 24)) {
                g.text(font, regel, left + 12, y, 0xFFE0B0C8);
                y += 11;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        ItemStack icoon = new ItemStack(HamsterbuisFeature.STATION_ITEM.get());
        g.item(icoon, left + 12, top + 23);
        List<CompoundTag> rijen = rijen();
        for (int i = 0; i < perPage && page * perPage + i < rijen.size(); i++) {
            g.item(icoon, left + 12, listY + i * ROW + 2);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
