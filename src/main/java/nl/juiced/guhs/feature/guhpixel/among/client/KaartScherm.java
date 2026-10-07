package nl.juiced.guhs.feature.guhpixel.among.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhpixel.among.AmongPayloads;
import nl.juiced.guhs.taal.Tekst;

/**
 * The Mika's map of De Vadsvaarder: every room and corridor drawn to scale, the vents, the three repair panels and where
 * you stand. From the Saboteerkaart: click a room to shut its doors, or press "Licht uit" or "Knabbelalarm". From a vent:
 * click a room with a vent of the same network (gold) to crawl there. The server checks everything again (cooldowns, the
 * role, the distance).
 */
public class KaartScherm extends Screen {
    private static final int W = 300, H = 240, KAART_Y = 34;
    private static final double SCHAAL = 3.5;
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, GANG = 0xFF4A3444,
            KAMER = 0xFF5E3A50, KAMER_OP = 0xFF9A4A72, ROMP = 0xFF1A0E15;

    private final CompoundTag data;
    private final int luik;
    private int afkoel;
    private int left, top, kx, ky;
    /** Vent mode: room (zone index) -> the vent of the network in it. */
    private final Map<Integer, Integer> luikInKamer = new HashMap<>();
    private final Map<Integer, Component> kamerNaam = new HashMap<>();

    public KaartScherm(CompoundTag data) {
        super(Component.translatable(data.getIntOr("Luik", -1) >= 0 ? "gui.guhs.among.kaart.luik" : "gui.guhs.among.kaart.sabotage"));
        this.data = data;
        this.luik = data.getIntOr("Luik", -1);
        this.afkoel = data.getIntOr("Afkoel", 0);
        ListTag kamers = data.getListOrEmpty("Kamers");
        ListTag zones = data.getListOrEmpty("Zones"), luiken = data.getListOrEmpty("Luiken");
        for (int i = 0; i < kamers.size(); i++) {
            CompoundTag k = kamers.getCompoundOrEmpty(i);
            int idx = k.getIntOr("Idx", -1);
            if (luik < 0) {
                kamerNaam.put(idx, Tekst.get(k, "Naam"));
                continue;
            }
            // a vent of the network: find the room it lies in
            for (int l = 0; l < luiken.size(); l++) {
                CompoundTag lk = luiken.getCompoundOrEmpty(l);
                if (lk.getIntOr("Idx", -2) != idx) {
                    continue;
                }
                for (int z = 0; z < zones.size(); z++) {
                    CompoundTag zone = zones.getCompoundOrEmpty(z);
                    int[] vak = zone.getIntArray("Vak").orElse(new int[4]);
                    if (zone.getBooleanOr("Kamer", false) && vak.length == 4 && lk.getIntOr("X", -1) >= vak[0] && lk.getIntOr("X", -1) <= vak[2]
                            && lk.getIntOr("Z", -1) >= vak[1] && lk.getIntOr("Z", -1) <= vak[3]) {
                        luikInKamer.put(zone.getIntOr("Idx", -1), idx);
                        kamerNaam.put(zone.getIntOr("Idx", -1), Tekst.get(k, "Naam"));
                    }
                }
            }
        }
    }

    private int sx(double x) {
        return kx + (int) Math.round(x * SCHAAL);
    }

    private int sy(double z) {
        return ky + (int) Math.round(z * SCHAAL);
    }

    private boolean kan() {
        return luik >= 0 || afkoel == 0;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        kx = left + (W - (int) Math.round(data.getIntOr("Breedte", 75) * SCHAAL)) / 2;
        ky = top + KAART_Y;
        int by = top + H - 24;
        if (luik < 0) {
            Button licht = Button.builder(Component.translatable("gui.guhs.among.kaart.licht"), b -> saboteer(1, -1)).bounds(left + 8, by, 92, 20).build();
            Button alarm = Button.builder(Component.translatable("gui.guhs.among.kaart.alarm"), b -> saboteer(2, -1)).bounds(left + 104, by, 92, 20).build();
            licht.active = alarm.active = kan();
            addRenderableWidget(licht);
            addRenderableWidget(alarm);
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(left + 200, by, 92, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(left + W / 2 - 46, by, 92, 20).build());
        }
    }

    private void saboteer(int soort, int kamer) {
        ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.SABOTEER, soort, kamer));
        onClose();
    }

    /** The room (zone index) under the pointer that can be clicked now; -1: none. */
    private int kamerOp(int mouseX, int mouseY) {
        ListTag zones = data.getListOrEmpty("Zones");
        for (int i = 0; i < zones.size(); i++) {
            CompoundTag z = zones.getCompoundOrEmpty(i);
            int[] vak = z.getIntArray("Vak").orElse(new int[4]);
            int idx = z.getIntOr("Idx", -1);
            if (z.getBooleanOr("Kamer", false) && vak.length == 4 && mouseX >= sx(vak[0]) && mouseX < sx(vak[2] + 1) && mouseY >= sy(vak[1])
                    && mouseY < sy(vak[3] + 1) && (luik < 0 || luikInKamer.containsKey(idx))) {
                return idx;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int kamer = event.button() == 0 ? kamerOp((int) event.x(), (int) event.y()) : -1;
        if (kamer < 0 || !kan()) {
            return false;
        }
        if (luik >= 0) {
            int naar = luikInKamer.get(kamer);
            if (naar != luik) {
                ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.LUIK, luik, naar));
                onClose();
            }
        } else {
            saboteer(3, kamer);
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (afkoel > 0 && --afkoel == 0) {
            rebuildWidgets();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, TEKST);
        int op = kan() ? kamerOp(mouseX, mouseY) : -1;
        Component stand;
        if (luik >= 0) {
            stand = op >= 0 && luikInKamer.get(op) != luik ? Component.translatable("gui.guhs.among.kaart.naar", kamerNaam.getOrDefault(op, Component.empty()))
                    : Component.translatable("gui.guhs.among.kaart.luik.uitleg");
        } else if (afkoel < 0) {
            stand = Component.translatable("gui.guhs.among.kaart.bezig");
        } else if (afkoel > 0) {
            stand = Component.translatable("gui.guhs.among.kaart.afkoel", AmongClient.seconden(afkoel));
        } else {
            stand = op >= 0 ? Component.translatable("gui.guhs.among.kaart.deuren_van", kamerNaam.getOrDefault(op, Component.empty()))
                    : Component.translatable("gui.guhs.among.kaart.kies");
        }
        g.centeredText(font, stand, width / 2, top + 20, kan() ? GOUD : DOF);
        // the hull
        g.fill(sx(0) - 2, sy(0) - 2, sx(data.getIntOr("Breedte", 75)) + 2, sy(data.getIntOr("Diepte", 51)) + 2, ROMP);
        ListTag zones = data.getListOrEmpty("Zones");
        for (int pass = 0; pass < 2; pass++) {       // corridors first, the rooms over them
            for (int i = 0; i < zones.size(); i++) {
                CompoundTag z = zones.getCompoundOrEmpty(i);
                boolean kamer = z.getBooleanOr("Kamer", false);
                int[] vak = z.getIntArray("Vak").orElse(new int[4]);
                if (kamer != (pass == 1) || vak.length != 4) {
                    continue;
                }
                int x0 = sx(vak[0]), y0 = sy(vak[1]), x1 = sx(vak[2] + 1), y1 = sy(vak[3] + 1), idx = z.getIntOr("Idx", -1);
                if (!kamer) {
                    g.fill(x0, y0, x1, y1, GANG);
                    continue;
                }
                boolean doel = luik >= 0 && luikInKamer.containsKey(idx) && luikInKamer.get(idx) != luik;
                g.fill(x0, y0, x1, y1, doel ? GOUD : 0xFF8A6A7C);
                g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, idx == op && (luik < 0 || doel) ? KAMER_OP : KAMER);
                g.centeredText(font, Component.translatable("gui.guhs.among.kaart.kamer." + z.getStringOr("Id", "")), (x0 + x1) / 2, (y0 + y1) / 2 - 4,
                        luik >= 0 && !doel ? DOF : TEKST);
            }
        }
        // the repair panels (yellow: the light switches; red: the two alarm codes)
        ListTag herstel = data.getListOrEmpty("Herstel");
        for (int i = 0; i < herstel.size(); i++) {
            CompoundTag h = herstel.getCompoundOrEmpty(i);
            int x = sx(h.getIntOr("X", 0) + 0.5), y = sy(h.getIntOr("Z", 0) + 0.5);
            g.fill(x - 3, y - 3, x + 3, y + 3, ROMP);
            g.fill(x - 2, y - 2, x + 2, y + 2, h.getBooleanOr("Licht", false) ? 0xFFFFE27A : 0xFFFF6B6B);
        }
        // the vents
        ListTag luiken = data.getListOrEmpty("Luiken");
        for (int i = 0; i < luiken.size(); i++) {
            CompoundTag l = luiken.getCompoundOrEmpty(i);
            int x = sx(l.getIntOr("X", 0) + 0.5), y = sy(l.getIntOr("Z", 0) + 0.5), idx = l.getIntOr("Idx", -1);
            boolean net = luikInKamer.containsValue(idx);
            g.fill(x - 4, y - 4, x + 4, y + 4, idx == luik ? 0xFF68D88A : net ? GOUD : 0xFFB8B8C4);
            g.fill(x - 3, y - 3, x + 3, y + 3, 0xFF2A2430);
            g.fill(x - 3, y - 1, x + 3, y, 0xFF8E8E9C);
            g.fill(x - 3, y + 1, x + 3, y + 2, 0xFF8E8E9C);
        }
        // you
        if (data.contains("X") && minecraft != null && minecraft.level != null && minecraft.level.getGameTime() / 6 % 2 == 0) {
            int x = sx(data.getDoubleOr("X", 0)), y = sy(data.getDoubleOr("Z", 0));
            g.fill(x - 3, y - 3, x + 3, y + 3, 0xFFFFFFFF);
            g.fill(x - 2, y - 2, x + 2, y + 2, 0xFFFF6B6B);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
