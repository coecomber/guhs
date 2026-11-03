package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;

/** The two-sided trade window: both put things in, both press "Klaar", then both press "Ruilen!". Any change resets both. */
public class HandelScherm extends EcoScherm {
    private EditBox munten;
    private boolean hunKlaarWas;

    public HandelScherm(CompoundTag data, List<ItemStack> items) {
        super(EcoPayloads.HANDEL, Component.translatable("vadsrijk.economie.handel.titel", data.getStringOr("ander", "?")), data, items, 330, 262);
    }

    @Override
    protected void init() {
        super.init();
        String oud = munten == null ? "" : munten.getValue();
        munten = new EditBox(font, left + 8, top + 112, 70, 14, Component.translatable("vadsrijk.economie.handel.munten_veld"));
        munten.setHint(Component.literal("bv. 1z 20b").withStyle(ChatFormatting.GRAY));
        munten.setValue(oud);
        addRenderableWidget(munten);
    }

    @Override
    protected void opNieuweData() {
        boolean hk = data.getBooleanOr("hunKlaar", false);
        if (hk && !hunKlaarWas) {
            geluid(SoundEvents.NOTE_BLOCK_BELL, 1.6f);
        }
        hunKlaarWas = hk;
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        String ander = data.getStringOr("ander", "?");
        int lx = left + 8, rx = left + w / 2 + 8;
        kop(g, Component.translatable("vadsrijk.economie.handel.jij_geeft"), lx, top + 24);
        kop(g, Component.translatable("vadsrijk.economie.handel.hij_geeft", ander), rx, top + 24);
        g.fill(left + w / 2, top + 22, left + w / 2 + 1, top + 150, 0x60FF5FAF);
        aanbod(g, mx, my, data.getListOrEmpty("mijn"), lx, top + 36, true);
        aanbod(g, mx, my, data.getListOrEmpty("hun"), rx, top + 36, false);
        // munten
        munten(g, lx + 60, top + 98, data.getLongOr("mijnMunten", 0), false, false);
        g.text(font, Component.translatable("vadsrijk.economie.handel.munten"), lx, top + 98, ZACHT, false);
        knop(g, mx, my, lx + 74, top + 112, 30, 14, Component.translatable("vadsrijk.economie.handel.zet"), true, () -> {
            CompoundTag a = new CompoundTag();
            a.putLong("n", Math.max(0, MarktScherm.leesPrijs(munten.getValue())));
            doe("munten", a);
        });
        g.text(font, Component.translatable("vadsrijk.economie.handel.munten"), rx, top + 98, ZACHT, false);
        munten(g, rx + 60, top + 98, data.getLongOr("hunMunten", 0), false, false);
        // status
        status(g, lx, top + 132, data.getBooleanOr("mijnKlaar", false), data.getBooleanOr("mijnBevestigd", false));
        status(g, rx, top + 132, data.getBooleanOr("hunKlaar", false), data.getBooleanOr("hunBevestigd", false));
        // your bag
        kop(g, Component.translatable("vadsrijk.economie.handel.tas"), lx, top + 154);
        ListTag tas = data.getListOrEmpty("tas");
        for (int i = 0; i < tas.size(); i++) {
            CompoundTag t = tas.getCompoundOrEmpty(i);
            int x = lx + (i % 12) * 19, y = top + 165 + (i / 12) * 19;
            if (y > top + h - 40) {
                break;
            }
            slot(g, mx, my, x, y, item(t.getIntOr("item", -1)), false);
            if (t.getBooleanOr("gebonden", false)) {
                g.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
                tip(mx, my, x, y, 18, 18, List.of(Component.translatable("vadsrijk.economie.handel.gebonden").withStyle(ChatFormatting.RED)));
            } else {
                int slot = t.getIntOr("slot", -1);
                klikvlak(x, y, 18, 18, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putInt("slot", slot);
                    doe("leg", a);
                });
            }
        }
        // buttons
        int by = top + h - 34;
        boolean mk = data.getBooleanOr("mijnKlaar", false), hk = data.getBooleanOr("hunKlaar", false);
        knop(g, mx, my, left + 8, by, 90, 18, Component.translatable(mk ? "vadsrijk.economie.handel.niet_klaar" : "vadsrijk.economie.handel.klaar"), true,
                () -> doe("klaar"));
        knop(g, mx, my, left + 104, by, 120, 18, Component.translatable("vadsrijk.economie.handel.ruilen").withStyle(ChatFormatting.BOLD),
                mk && hk && !data.getBooleanOr("mijnBevestigd", false), () -> doe("bevestig"));
        knop(g, mx, my, left + w - 98, by, 90, 18, Component.translatable("vadsrijk.economie.handel.stop"), true, () -> doe("annuleer"));
    }

    private void aanbod(GuiGraphicsExtractor g, int mx, int my, ListTag l, int x0, int y0, boolean mijn) {
        for (int i = 0; i < 9; i++) {
            int x = x0 + (i % 3) * 19, y = y0 + (i / 3) * 19;
            ItemStack s = i < l.size() ? item(l.getCompoundOrEmpty(i).getIntOr("item", -1)) : ItemStack.EMPTY;
            slot(g, mx, my, x, y, s, false);
            if (mijn && !s.isEmpty()) {
                final int idx = i;
                klikvlak(x, y, 18, 18, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putInt("index", idx);
                    doe("pak", a);
                });
            }
        }
        if (mijn) {
            g.text(font, Component.translatable("vadsrijk.economie.handel.klik_terug"), x0 + 62, y0 + 4, ZACHT, false);
        }
    }

    private void status(GuiGraphicsExtractor g, int x, int y, boolean klaar, boolean bevestigd) {
        Component c = bevestigd ? Component.translatable("vadsrijk.economie.handel.status.bevestigd")
                : klaar ? Component.translatable("vadsrijk.economie.handel.status.klaar") : Component.translatable("vadsrijk.economie.handel.status.bezig");
        g.fill(x - 2, y - 2, x + 140, y + 10, bevestigd ? 0xFFB8F0C8 : klaar ? 0xFFFFF0B0 : 0xFFF4DCEA);
        g.text(font, c, x, y, bevestigd ? GROEN : klaar ? GOUD : ZACHT, false);
    }
}
