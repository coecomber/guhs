package nl.juiced.guhs.feature.vadsrijk.menu.client;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.menu.MenuServer;

/**
 * U-TABS: the Karakter-tab of the Vadsrijk-menu. Left: who you are (name, game mode, level + xp bar, class + job title, all class
 * levels, Vadsmunten, weapon damage, extra lines from other slices such as the specialisation). Middle: the gear, weapon + armour
 * and the virtual slots (rings, necklace, earring, bracelet, gloves, cape), each with its tooltip (empty = the slot name).
 * Right: all stats with clear names (main stats first, % where it is a percentage).
 */
public class KarakterPaneel extends MenuPaneel {
    public KarakterPaneel(CompoundTag data, List<ItemStack> items) {
        super("karakter", Component.translatable("vadsrijk.menu.tab.karakter"), data);
        this.items = items;
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        if (!data.contains("level")) {
            g.text(font, Component.translatable("vadsrijk.menu.laden"), left + 10, top + 26, ZACHT, false);
            return;
        }
        int kolom = (w - 24) / 3;
        int x1 = left + 8, x2 = x1 + kolom + 4, x3 = x2 + kolom + 4, y0 = top + 24;
        // ---- left: who you are -------------------------------------------------------------------------------------------------
        int y = y0;
        kop(g, Component.literal(data.getStringOr("naam", "?")), x1, y);
        y += 11;
        g.text(font, Component.translatable(data.getStringOr("modus", "")), x1, y, ZACHT, false);
        y += 13;
        int lv = data.getIntOr("level", 1);
        g.text(font, Component.translatable("vadsrijk.menu.karakter.level", lv).withStyle(ChatFormatting.BOLD), x1, y, TEKST, false);
        y += 11;
        long xp = data.getLongOr("xp", 0), nodig = data.getLongOr("xpNodig", 0);
        balk(g, x1, y, kolom - 8, 6, nodig <= 0 ? 1f : xp / (float) nodig);
        tip(mx, my, x1, y - 2, kolom - 8, 10, List.of(nodig <= 0 ? Component.translatable(data.getStringOr("maxKey", "vadsrijk.menu.karakter.max"),   // X-ZONE1: soft cap
                        Component.translatable("vadsrijk.zonepoort.zone." + data.getIntOr("volgendeZone", 2)), data.getLongOr("gespaard", 0))
                : Component.translatable("vadsrijk.menu.karakter.xp", xp, nodig)));
        y += 12;
        g.text(font, Component.translatable(data.getStringOr("titel", "")).withStyle(ChatFormatting.BOLD), x1, y, 0xFFD0307A, false);
        y += 11;
        g.text(font, Component.translatable("vadsrijk.menu.karakter.klasse", Component.translatable(data.getStringOr("klasse", "")),
                data.getIntOr("klasseLevel", 1)), x1, y, TEKST, false);
        y += 14;
        g.text(font, Component.translatable("vadsrijk.menu.karakter.klassen").withStyle(ChatFormatting.BOLD), x1, y, TEKST, false);
        y += 11;
        ListTag kl = data.getListOrEmpty("klassen");
        for (int i = 0; i < kl.size(); i++) {
            CompoundTag k = kl.getCompoundOrEmpty(i);
            boolean actief = k.getBooleanOr("actief", false);
            g.text(font, Component.literal(actief ? "⚔ " : "  ").append(Component.translatable(k.getStringOr("naam", ""))), x1, y, actief ? TEKST : ZACHT, false);
            String l = "lv " + k.getIntOr("lv", 1);
            g.text(font, l, x1 + kolom - 8 - font.width(l), y, actief ? TEKST : ZACHT, false);
            y += 10;
        }
        y += 4;
        g.text(font, Component.translatable("vadsrijk.menu.karakter.schade", String.format(Locale.ROOT, "%.1f", data.getDoubleOr("wapenSchade", 0))), x1, y, TEKST,
                false);
        tip(mx, my, x1, y - 1, kolom - 8, 10, List.of(Component.translatable("vadsrijk.menu.karakter.schade.uitleg")));
        y += 12;
        munten(g, x1, y, data.getLongOr("munten", 0), false, false);
        y += 13;
        ListTag extra = data.getListOrEmpty("extra");
        for (int i = 0; i < extra.size() && y < top + h - 10; i++) {
            CompoundTag e = extra.getCompoundOrEmpty(i);
            Component c = e.contains("a") ? Component.translatable(e.getStringOr("k", ""), Component.translatable(e.getStringOr("a", "")))
                    : Component.translatable(e.getStringOr("k", ""));
            for (var regel : font.split(c, kolom - 8)) {
                g.text(font, regel, x1, y, TEKST, false);
                y += 10;
            }
        }

        // ---- middle: gear --------------------------------------------------------------------------------------------------------
        g.fill(x2 - 2, y0 - 2, x2 + kolom, top + h - 6, 0xFFFFF4FA);
        kop(g, Component.translatable("vadsrijk.menu.karakter.uitrusting"), x2 + 2, y0);
        int gy = y0 + 13, half = (kolom - 4) / 2;
        for (int i = 0; i < MenuServer.GEAR.length; i++) {
            Slot s = MenuServer.GEAR[i];
            boolean rechts = i >= 5;
            int sx = x2 + 2 + (rechts ? half : 0), sy = gy + (rechts ? (i - 5) : i) * 20;
            ItemStack st = item(i);
            slot(g, mx, my, sx, sy, st, false);
            Component naam = Component.translatable(s.key());
            g.text(font, font.plainSubstrByWidth(naam.getString(), half - 24), sx + 21, sy + 5, st.isEmpty() ? 0xFFB8A0B4 : ZACHT, false);
            if (st.isEmpty()) {
                tip(mx, my, sx, sy, 18, 18, List.of(naam.copy().withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.translatable("vadsrijk.menu.karakter.leeg").withStyle(ChatFormatting.GRAY)));
            }
        }
        int ty = gy + 7 * 20 + 2;
        for (var regel : font.split(Component.translatable("vadsrijk.menu.karakter.uitrusting.uitleg"), kolom - 8)) {
            if (ty > top + h - 16) {
                break;
            }
            g.text(font, regel, x2 + 2, ty, ZACHT, false);
            ty += 10;
        }

        // ---- right: stats --------------------------------------------------------------------------------------------------------
        kop(g, Component.translatable("vadsrijk.menu.karakter.stats"), x3, y0);
        int sy = y0 + 13;
        ListTag sl = data.getListOrEmpty("stats");
        boolean wasHoofd = true;
        for (int i = 0; i < sl.size() && sy < top + h - 8; i++) {
            CompoundTag s = sl.getCompoundOrEmpty(i);
            boolean hoofd = s.getBooleanOr("h", false);
            if (wasHoofd && !hoofd) {
                g.fill(x3, sy + 1, x3 + kolom - 6, sy + 2, 0xFFFFB0D8);
                sy += 5;
            }
            wasHoofd = hoofd;
            double v = s.getDoubleOr("v", 0);
            boolean p = s.getBooleanOr("p", false);
            String waarde = p ? String.format(Locale.ROOT, "%.1f%%", v) : String.format(Locale.ROOT, "%.0f", v);
            g.fill(x3 - 2, sy - 1, x3 + kolom - 4, sy + 10, i % 2 == 0 ? RIJ_A : RIJ_B);
            Component naam = Component.translatable(s.getStringOr("k", ""));
            g.text(font, font.plainSubstrByWidth(naam.getString(), kolom - 12 - font.width(waarde)), x3, sy, TEKST, false);
            g.text(font, waarde, x3 + kolom - 8 - font.width(waarde), sy, hoofd ? TEKST : GOUD, false);
            String uitleg = s.getStringOr("k", "") + ".uitleg";
            if (net.minecraft.locale.Language.getInstance().has(uitleg)) {
                tip(mx, my, x3 - 2, sy - 1, kolom - 2, 11, List.of(naam.copy().withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.translatable(uitleg).withStyle(ChatFormatting.GRAY)));
            }
            sy += 11;
        }
    }

    private void balk(GuiGraphicsExtractor g, int x, int y, int bw, int bh, float f) {
        g.fill(x, y, x + bw, y + bh, 0xFFF0D8E4);
        int v = Math.round(bw * Math.max(0, Math.min(1, f)));
        if (v > 0) {
            g.fill(x, y, x + v, y + bh, 0xFFFF5FAF);
            g.fill(x, y, x + v, y + 1, 0x60FFFFFF);
        }
    }
}
