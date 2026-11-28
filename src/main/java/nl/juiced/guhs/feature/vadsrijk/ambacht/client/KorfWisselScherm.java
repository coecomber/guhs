package nl.juiced.guhs.feature.vadsrijk.ambacht.client;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPayloads;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Imkeren;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;

/**
 * "Korf verplaatsen" (the user: "als je rechtermuisknop klikt op een honingraat maar je hebt al 4/4 in die zone: een dialoog waarmee
 * je een andere raat kan verwijderen, maar met een waarschuwing"): you clicked a free hive stand while your zone is full. The window
 * lists your hives in that zone (meadow, x/z, the combs in them now); "Weghalen" on one asks first, in red, with the combs you lose;
 * "Ja, weghalen" sends {@code korf_wissel} (the server takes that hive away and puts the new one on the clicked stand). Closing or
 * "Laat maar" changes nothing. Server side: {@link Imkeren#stuurWissel} / {@link Imkeren#wissel}.
 */
public class KorfWisselScherm extends EcoScherm {
    private static final String K = Imkeren.K_WISSEL;
    private static final int RIJ = 24, ZICHT = 4;
    private int scroll;
    /** The hive the player picked (the warning page), or null (the list). */
    @Nullable
    private CompoundTag weg;

    public KorfWisselScherm(CompoundTag data, List<ItemStack> items) {
        super("ambacht_korf", Component.translatable(K + "titel"), data, items, 360, 190);
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        ClientPacketDistributor.sendToServer(new AmbachtPayloads.Doe(actie, args));
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        if (weg != null) {
            vraag(g, mx, my, weg);
            return;
        }
        ListTag l = data.getListOrEmpty("korven");
        int y = top + 24;
        for (var regel : font.split(Component.translatable(K + "uitleg", l.size(), data.getIntOr("max", Imkeren.PER_ZONE)), w - 16)) {
            g.text(font, regel, left + 8, y, TEKST, false);
            y += 10;
        }
        int y0 = top + 60;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, l.size() - ZICHT)));
        for (int i = scroll; i < Math.min(l.size(), scroll + ZICHT); i++) {
            CompoundTag t = l.getCompoundOrEmpty(i);
            int ry = y0 + (i - scroll) * RIJ;
            g.fill(left + 6, ry, left + w - 6, ry + RIJ - 2, binnen(mx, my, left + 6, ry, w - 12, RIJ - 2) ? HOVER : (i % 2 == 0 ? RIJ_A : RIJ_B));
            slot(g, mx, my, left + 8, ry + 2, item(t.getIntOr("item", -1)), false);
            int ruimte = w - 30 - 76;
            g.text(font, kort(naam(t), ruimte), left + 30, ry + 2, TEKST, false);
            Component onder = t.getBooleanOr("waar", false) ? Component.empty().append(raten(t)).append(" · ").append(waar(t)) : raten(t);   // (the combs first: they must never be cut off)
            g.text(font, kort(onder.getString(), ruimte), left + 30, ry + 12, ZACHT, false);
            knop(g, mx, my, left + w - 74, ry + 3, 64, 16, Component.translatable(K + "knop.weg"), true, () -> weg = t);
        }
        if (l.size() > ZICHT) {
            int bh = ZICHT * RIJ - 2, kh = Math.max(12, bh * ZICHT / l.size());
            int ky = y0 + (bh - kh) * scroll / Math.max(1, l.size() - ZICHT);
            g.fill(left + w - 5, y0, left + w - 2, y0 + bh, 0x40FF5FAF);
            g.fill(left + w - 5, ky, left + w - 2, ky + kh, RAND);
        }
        knop(g, mx, my, left + (w - 90) / 2, top + h - 24, 90, 16, Component.translatable(K + "knop.laat"), true, this::onClose);
    }

    /** The warning page for the picked hive. */
    private void vraag(GuiGraphicsExtractor g, int mx, int my, CompoundTag t) {
        int mid = left + w / 2, y = top + 30;
        midden(g, Component.translatable(K + "vraag.titel").withStyle(ChatFormatting.BOLD), mid, y, TEKST);
        y += 16;
        midden(g, Component.literal(kort(naam(t), w - 24)), mid, y, TEKST);
        y += 11;
        if (t.getBooleanOr("waar", false)) {
            midden(g, waar(t), mid, y, ZACHT);
            y += 11;
        }
        y += 7;
        int n = t.getIntOr("raten", 0);
        Component verlies = n > 0 ? Component.translatable(K + "vraag.verlies", n, honing(t.getStringOr("honing", ""))) : Component.translatable(K + "vraag.leeg");
        for (var regel : font.split(verlies, w - 40)) {
            g.text(font, regel, mid - font.width(regel) / 2, y, n > 0 ? ROOD : TEKST, false);
            y += 10;
        }
        y += 7;
        for (var regel : font.split(Component.translatable(K + "vraag.daarna", honing(data.getStringOr("honing", ""))), w - 40)) {
            g.text(font, regel, mid - font.width(regel) / 2, y, ZACHT, false);
            y += 10;
        }
        int ky = top + h - 28;
        String id = t.getStringOr("id", "");
        knop(g, mx, my, mid - 104, ky, 100, 18, Component.translatable(K + "knop.ja"), true, () -> {
            CompoundTag a = new CompoundTag();
            a.putString("weg", id);
            a.putString("spot", data.getStringOr("spot", ""));
            doe("korf_wissel", a);
            Minecraft.getInstance().setScreen(null);   // (no "dicht": the server already closed the offer with the swap)
        });
        knop(g, mx, my, mid + 4, ky, 100, 18, Component.translatable(K + "knop.nee"), true, () -> weg = null);
    }

    /** "De bijenwei in het Paddenbloesemdal" (the group names start in lower case: they are written for the middle of a sentence). */
    private static String naam(CompoundTag t) {
        String groep = t.getStringOr("groep", "");
        String s = groep.isEmpty() ? t.getStringOr("id", "") : Component.translatable("vadsrijk.ambacht.groep." + groep).getString();
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Component waar(CompoundTag t) {
        return Component.translatable(K + "plek", t.getIntOr("x", 0), t.getIntOr("z", 0));
    }

    private static Component raten(CompoundTag t) {
        int n = t.getIntOr("raten", 0);
        return n > 0 ? Component.translatable(K + "raten", n, honing(t.getStringOr("honing", ""))) : Component.translatable(K + "geen_raten");
    }

    private static Component honing(String id) {
        return id.isEmpty() ? Component.empty() : Component.translatable("item.guhs.vr_amb_" + id);
    }

    /** Centred text without the drop shadow (dark text on the light panel). */
    private void midden(GuiGraphicsExtractor g, Component c, int x, int y, int kleur) {
        g.text(font, c, x - font.width(c) / 2, y, kleur, false);
    }

    private String kort(String s, int ruimte) {
        return font.width(s) <= ruimte ? s : font.plainSubstrByWidth(s, ruimte - font.width("...")) + "...";
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (weg == null) {
            int max = Math.max(0, data.getListOrEmpty("korven").size() - ZICHT);
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        }
        return true;
    }
}
