package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Stal;

/** The stal: your mounts (level, xp, speed, jump), pick the one your rijfluitje calls, buy a new rijfluitje. */
public class StalScherm extends EcoScherm {
    public StalScherm(CompoundTag data, List<ItemStack> items) {
        super(EcoPayloads.STAL, Component.translatable("vadsrijk.economie.stal.titel"), data, items, 300, 200);
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        ListTag l = data.getListOrEmpty("rijdieren");
        int kw = (w - 24) / 2;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            int x = left + 8 + i * (kw + 8), y = top + 24;
            kaart(g, mx, my, t, x, y, kw, 132);
        }
        int y = top + h - 36;
        boolean fluitje = data.getBooleanOr("fluitje", false);
        g.text(font, Component.translatable(fluitje ? "vadsrijk.economie.stal.fluitje_ja" : "vadsrijk.economie.stal.fluitje_nee"), left + 8, y + 5,
                fluitje ? GROEN : ROOD, false);
        knop(g, mx, my, left + w - 128, y, 120, 18, Component.translatable("vadsrijk.economie.stal.koop_fluitje", Stal.FLUITJE_PRIJS), !fluitje,
                () -> doe("fluitje"));
    }

    private void kaart(GuiGraphicsExtractor g, int mx, int my, CompoundTag t, int x, int y, int kw, int kh) {
        boolean heeft = t.getBooleanOr("heeft", false), actief = t.getBooleanOr("actief", false);
        RijdierSoort s = RijdierSoort.van(t.getStringOr("soort", ""));
        g.fill(x, y, x + kw, y + kh, actief ? 0xFFFF7FC0 : 0xFFF0B8D8);
        g.fill(x + 1, y + 1, x + kw - 1, y + kh - 1, heeft ? 0xFFFFF4FA : 0xFFF0E4EC);
        g.pose().pushMatrix();
        g.pose().translate(x + 6, y + 6);
        g.pose().scale(2f, 2f);
        g.item(item(t.getIntOr("item", -1)), 0, 0);
        g.pose().popMatrix();
        Component naam = s == null ? Component.literal("?") : Component.translatable(s.naamKey());
        g.text(font, naam.copy().withStyle(ChatFormatting.BOLD), x + 42, y + 8, heeft ? TEKST : ZACHT, false);
        if (!heeft) {
            for (var line : font.split(Component.translatable("vadsrijk.economie.stal.nog_niet." + t.getStringOr("soort", "")), kw - 12)) {
                y += 10;
                g.text(font, line, x + 6, y + 36, ZACHT, false);
            }
            return;
        }
        int level = t.getIntOr("level", 1);
        g.text(font, Component.translatable("vadsrijk.economie.stal.level", level, RijdierSoort.MAX_LEVEL), x + 42, y + 20, GOUD, false);
        long xp = t.getLongOr("xp", 0), nodig = t.getLongOr("nodig", 0);
        int bw = kw - 12;
        g.fill(x + 6, y + 42, x + 6 + bw, y + 48, 0xFFE8C8DC);
        if (nodig > 0) {
            g.fill(x + 6, y + 42, x + 6 + (int) (bw * Math.min(1.0, xp / (double) nodig)), y + 48, 0xFFFF5FAF);
            g.text(font, Component.translatable("vadsrijk.economie.stal.xp", xp, nodig), x + 6, y + 52, ZACHT, false);
        } else {
            g.fill(x + 6, y + 42, x + 6 + bw, y + 48, 0xFFFFC040);
            g.text(font, Component.translatable("vadsrijk.economie.stal.max"), x + 6, y + 52, GOUD, false);
        }
        double sn = t.getDoubleOr("snelheid", 0), sp = t.getDoubleOr("sprong", 0);
        g.text(font, Component.translatable("vadsrijk.economie.stal.snelheid", Math.round(sn / 0.1 * 100)), x + 6, y + 66, TEKST, false);
        g.text(font, Component.translatable("vadsrijk.economie.stal.sprong", String.format(java.util.Locale.ROOT, "%.1f", sp * sp / 0.16 + 0.5)), x + 6, y + 78,
                TEKST, false);
        if (actief) {
            g.centeredText(font, Component.translatable("vadsrijk.economie.stal.gekozen_label").withStyle(ChatFormatting.BOLD), x + kw / 2, y + kh - 20, GROEN);
        } else {
            String id = t.getStringOr("soort", "");
            knop(g, mx, my, x + 8, y + kh - 24, kw - 16, 18, Component.translatable("vadsrijk.economie.stal.kies"), true, () -> {
                CompoundTag a = new CompoundTag();
                a.putString("soort", id);
                doe("kies", a);
            });
        }
    }
}
