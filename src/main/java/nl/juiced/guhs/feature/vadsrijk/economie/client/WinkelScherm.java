package nl.juiced.guhs.feature.vadsrijk.economie.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads;
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;

/** A Vadsmunten shop: buy (items, plain gear per level tier, cosmetics by post, titles) and sell loot. */
public class WinkelScherm extends EcoScherm {
    private static final int RIJ = 22, ZICHT = 7;
    private boolean verkopen;
    private int scroll, niveau = 1;

    public WinkelScherm(CompoundTag data, List<ItemStack> items) {
        super(EcoPayloads.WINKEL, Component.translatable("vadsrijk.economie.winkel." + data.getStringOr("winkel", "algemeen")), data, items, 300, 228);
    }

    @Override
    protected void opNieuweData() {
        int max = Math.max(0, lijst().size() - ZICHT);
        scroll = Math.min(scroll, max);
    }

    private ListTag lijst() {
        return data.getListOrEmpty(verkopen ? "tas" : "waren");
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int y = top + 22;
        boolean koopt = data.getBooleanOr("koopt", false);
        if (koopt) {
            knop(g, mx, my, left + 8, y, 70, 14, Component.translatable("vadsrijk.economie.winkel.tab.kopen"), verkopen, () -> {
                verkopen = false;
                scroll = 0;
            });
            knop(g, mx, my, left + 82, y, 70, 14, Component.translatable("vadsrijk.economie.winkel.tab.verkopen"), !verkopen, () -> {
                verkopen = true;
                scroll = 0;
            });
        }
        boolean gear = !verkopen && data.getStringOr("winkel", "").equals(Winkels.UITRUSTING);
        if (gear) {
            int x = left + w - 8 - 4 * 34;
            for (int n : Winkels.NIVEAUS) {
                final int lv = n;
                knop(g, mx, my, x, y, 32, 14, Component.literal("Lv " + n), niveau != n, () -> niveau = lv);
                x += 34;
            }
        }
        if (verkopen) {
            knop(g, mx, my, left + w - 128, y, 120, 14, Component.translatable("vadsrijk.economie.winkel.verkoop_grijs"), true, () -> doe("verkoopgrijs"));
        }
        y += 20;
        ListTag l = lijst();
        if (l.isEmpty()) {
            g.centeredText(font, Component.translatable(verkopen ? "vadsrijk.economie.winkel.niks_te_verkopen" : "vadsrijk.economie.winkel.leeg"),
                    left + w / 2, y + 30, ZACHT);
        }
        for (int i = scroll; i < Math.min(l.size(), scroll + ZICHT); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            int ry = y + (i - scroll) * RIJ;
            boolean hover = binnen(mx, my, left + 6, ry, w - 12, RIJ - 2);
            g.fill(left + 6, ry, left + w - 6, ry + RIJ - 2, hover ? HOVER : (i % 2 == 0 ? RIJ_A : RIJ_B));
            ItemStack s = item(t.getIntOr("item", -1));
            slot(g, mx, my, left + 8, ry + 1, s, false);
            if (verkopen) {
                rijVerkoop(g, mx, my, t, s, ry);
            } else {
                rijKoop(g, mx, my, t, s, ry);
            }
        }
        if (l.size() > ZICHT) {
            int bh = ZICHT * RIJ - 2;
            int kh = Math.max(12, bh * ZICHT / l.size());
            int ky = y + (bh - kh) * scroll / Math.max(1, l.size() - ZICHT);
            g.fill(left + w - 5, y, left + w - 2, y + bh, 0x40FF5FAF);
            g.fill(left + w - 5, ky, left + w - 2, ky + kh, RAND);
        }
        if (data.getStringOr("winkel", "").equals(Winkels.COSMETICA)) {
            g.centeredText(font, Component.translatable("vadsrijk.economie.winkel.post_uitleg", data.getIntOr("post", 0)), left + w / 2, top + h - 26, ZACHT);
        }
    }

    private void rijKoop(GuiGraphicsExtractor g, int mx, int my, CompoundTag t, ItemStack s, int ry) {
        String soort = t.getStringOr("soort", "ITEM");
        Component naam = soort.equals("TITEL") ? Component.translatable("vadsrijk.economie.winkel.titel", Component.translatable(t.getStringOr("titel", "")))
                : s.getHoverName();
        g.text(font, naam, left + 30, ry + 2, TEKST, false);
        long prijs = t.getLongOr("prijs", 0);
        if (soort.equals("GEAR")) {
            prijs += 25L * niveau;
            g.text(font, Component.translatable("vadsrijk.economie.winkel.gear_uitleg", niveau), left + 30, ry + 11, ZACHT, false);
        } else if (soort.equals("POST")) {
            // (integration) cut off before the price + buy button, so the post line never runs under the price
            String post = Component.translatable("vadsrijk.economie.winkel.per_post_kort").getString();
            long gd = prijs / 10000, zv = prijs / 100 % 100;
            int prijsBreed = 15 + font.width(String.valueOf(prijs % 100)) + (gd > 0 || zv > 0 ? 15 + font.width(String.valueOf(zv)) : 0)
                    + (gd > 0 ? 15 + font.width(String.valueOf(gd)) : 0);
            int ruimte = w - 30 - 10 - 44 - prijsBreed - 6;
            if (font.width(post) > ruimte) {
                post = font.plainSubstrByWidth(post, ruimte - font.width("...")) + "...";
            }
            g.text(font, post, left + 30, ry + 11, ZACHT, false);
        }
        long munten = data.getLongOr("munten", 0);
        int bx = left + w - 10;
        String id = t.getStringOr("id", "");
        if (t.getBooleanOr("al", false)) {
            g.text(font, Component.translatable("vadsrijk.economie.winkel.in_bezit").withStyle(ChatFormatting.ITALIC), bx - 60, ry + 6, GROEN, false);
            return;
        }
        if (soort.equals("ITEM")) {
            long p5 = prijs * 5;
            knop(g, mx, my, bx - 26, ry + 2, 26, 16, Component.literal("x5"), munten >= p5, () -> koop(id, 5));
            bx -= 30;
        }
        long pr = prijs;
        knop(g, mx, my, bx - 40, ry + 2, 40, 16, Component.translatable("vadsrijk.economie.winkel.koop"), munten >= pr, () -> koop(id, 1));
        munten(g, bx - 44, ry + 7, prijs, true, false);
    }

    private void koop(String id, int aantal) {
        CompoundTag a = new CompoundTag();
        a.putString("waar", id);
        a.putInt("aantal", aantal);
        a.putInt("level", niveau);
        doe("koop", a);
    }

    private void rijVerkoop(GuiGraphicsExtractor g, int mx, int my, CompoundTag t, ItemStack s, int ry) {
        g.text(font, s.getHoverName(), left + 30, ry + 6, TEKST, false);
        int bx = left + w - 10;
        int slot = t.getIntOr("slot", -1);
        knop(g, mx, my, bx - 52, ry + 2, 52, 16, Component.translatable("vadsrijk.economie.winkel.verkoop"), true, () -> {
            CompoundTag a = new CompoundTag();
            a.putInt("slot", slot);
            doe("verkoop", a);
        });
        munten(g, bx - 56, ry + 7, t.getLongOr("waarde", 0), true, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, lijst().size() - ZICHT);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        return true;
    }
}
