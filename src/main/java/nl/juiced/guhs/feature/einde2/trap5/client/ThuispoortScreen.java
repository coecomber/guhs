package nl.juiced.guhs.feature.einde2.trap5.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.einde2.trap5.T5Payloads;

/**
 * A Thuispoort's screen: its name (the owner renames it), its battery and what a jump costs, every poort you may jump to
 * (an icon, a name, where it stands: click to go), and for the owner who is let in (a name + Toelaten / Weigeren).
 */
public class ThuispoortScreen extends Screen {
    private static final int W = 270, MAX_H = 250, ROW = 22, BOTTOM = 56;
    private final BlockPos poort;
    private CompoundTag data;
    private int left, top, h, listY, perPage, page;
    private EditBox naam, wie;
    private String getypt, wieGetypt = "";

    public ThuispoortScreen(T5Payloads.PoortOpen open) {
        super(Component.translatable("block.guhs.thuispoort"));
        this.poort = open.poort();
        this.data = open.data();
    }

    public BlockPos poort() {
        return poort;
    }

    public void update(T5Payloads.PoortOpen open) {
        if (naam != null) {
            getypt = naam.getValue();
        }
        if (wie != null) {
            wieGetypt = wie.getValue();
        }
        this.data = open.data();
        rebuildWidgets();
    }

    private void stuur(int actie, String tekst) {
        ClientPacketDistributor.sendToServer(new T5Payloads.PoortActie(poort, actie, tekst));
    }

    private boolean eigen() {
        return data.getBooleanOr("Eigenaar", false);
    }

    private static ItemStack icoon(String id) {
        Identifier rl = Identifier.tryParse(id);
        return rl != null && BuiltInRegistries.ITEM.containsKey(rl) ? new ItemStack(BuiltInRegistries.ITEM.getValue(rl)) : ItemStack.EMPTY;
    }

    private List<CompoundTag> rijen() {
        List<CompoundTag> uit = new ArrayList<>();
        ListTag l = data.getListOrEmpty("Poorten");
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
            naam = new EditBox(font, left + 32, y, W - 110, 18, Component.translatable("gui.guhs.einde2_trap5_machines.poort.naam"));
            naam.setMaxLength(24);
            naam.setValue(getypt != null ? getypt : data.getStringOr("Naam", ""));
            addRenderableWidget(naam);
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.menu.rename"), b -> stuur(T5Payloads.PoortActie.HERNOEM, naam.getValue()))
                    .bounds(left + W - 74, y - 1, 62, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_trap5_machines.poort.icoon"), b -> stuur(T5Payloads.PoortActie.ICOON, ""))
                    .bounds(left + W - 74, y + 21, 62, 16)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.einde2_trap5_machines.poort.icoon.tooltip"))).build());
        }
        listY = top + 78;
        perPage = Math.max(1, (h - (listY - top) - BOTTOM) / ROW);
        List<CompoundTag> rijen = rijen();
        int pages = Math.max(1, (rijen.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < perPage && page * perPage + i < rijen.size(); i++) {
            CompoundTag r = rijen.get(page * perPage + i);
            String id = r.getStringOr("Id", "");
            Component tekst = Component.translatable("gui.guhs.einde2_trap5_machines.poort.rij", r.getStringOr("Naam", "?"),
                    r.getIntOr("X", 0), r.getIntOr("Y", 0), r.getIntOr("Z", 0));
            addRenderableWidget(Button.builder(tekst, b -> {
                        stuur(T5Payloads.PoortActie.SPRING, id);
                        onClose();
                    }).bounds(left + 32, listY + i * ROW, W - 44, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.guhs.einde2_trap5_machines.poort.ga", r.getStringOr("Naam", "?"),
                            r.getStringOr("Dim", "")))).build());
        }
        int by = top + h - BOTTOM + 4;
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuildWidgets();
            }).bounds(left + 12, by - 24, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuildWidgets();
            }).bounds(left + 36, by - 24, 20, 20).build());
        }
        if (eigen()) {
            wie = new EditBox(font, left + 12, by + 4, 110, 18, Component.translatable("gui.guhs.einde2_trap5_machines.poort.wie"));
            wie.setMaxLength(16);
            wie.setValue(wieGetypt);
            addRenderableWidget(wie);
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_trap5_machines.poort.toelaten"),
                    b -> stuur(T5Payloads.PoortActie.TOELATEN, wie.getValue())).bounds(left + 126, by + 3, 64, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.guhs.einde2_trap5_machines.poort.weigeren"),
                    b -> stuur(T5Payloads.PoortActie.WEIGEREN, wie.getValue())).bounds(left + 194, by + 3, 64, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(left + W - 92, top + h - 24, 80, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + h + 1, 0xFFB07CE8);
        g.fill(left, top, left + W, top + h, 0xF01C1230);
        g.centeredText(font, (eigen() ? title : Component.literal(data.getStringOr("Naam", ""))).copy().withStyle(ChatFormatting.BOLD), width / 2,
                top + 8, 0xFFF0E0FF);
        if (!eigen()) {
            g.text(font, Component.literal(data.getStringOr("Naam", "")), left + 32, top + 27, 0xFFE0D0F0);
        }
        g.text(font, Component.translatable("gui.guhs.einde2_trap5_machines.poort.lading", data.getLongOr("Lading", 0L), data.getLongOr("Max", 0L),
                data.getIntOr("Kost", 0)), left + 12, top + 64, data.getLongOr("Lading", 0L) >= data.getIntOr("Kost", 0) ? 0xFF9FE8B0 : 0xFFF0B070);
        if (rijen().isEmpty()) {
            int y = listY + 2;
            for (var regel : font.split(Component.translatable("gui.guhs.einde2_trap5_machines.poort.geen"), W - 24)) {
                g.text(font, regel, left + 12, y, 0xFFC0B0D8);
                y += 11;
            }
        }
        if (eigen()) {
            ListTag toe = data.getListOrEmpty("Toegelaten");
            List<String> namen = new ArrayList<>();
            for (int i = 0; i < toe.size(); i++) {
                namen.add(toe.getCompoundOrEmpty(i).getStringOr("Naam", ""));
            }
            Component regel = namen.isEmpty() ? Component.translatable("gui.guhs.einde2_trap5_machines.poort.alleen_jij")
                    : Component.translatable("gui.guhs.einde2_trap5_machines.poort.ook", String.join(", ", namen));
            g.text(font, regel, left + 12, top + h - BOTTOM - 4 + 34, 0xFFC0B0D8);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.item(icoon(data.getStringOr("Icoon", "guhs:thuispoort")), left + 12, top + 23);
        List<CompoundTag> rijen = rijen();
        for (int i = 0; i < perPage && page * perPage + i < rijen.size(); i++) {
            g.item(icoon(rijen.get(page * perPage + i).getStringOr("Icoon", "guhs:thuispoort")), left + 12, listY + i * ROW + 2);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
