package nl.juiced.guhs.feature.vadsrijk.zone3.systemen.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Premiebord;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Spul;

/**
 * The Premiebord / Jachtbord / Penningwinkel screen (economie pink style): three tabs, the Jachtpenningen + Gouden Veren at the
 * top, the time to the 06:00 reset at the bottom.
 */
public class PremieScherm extends EcoScherm {
    public static final String SCHERM = "z3_premiebord";
    private static final String[] TABS = {Premiebord.PREMIES, Premiebord.JACHT, Premiebord.WINKEL};

    public PremieScherm(CompoundTag data, List<ItemStack> items) {
        super(SCHERM, Component.translatable("vadsrijk.z3.systemen.bord.titel"), data, items, 320, 216);
    }

    private String tab() {
        return data.getStringOr("tab", Premiebord.PREMIES);
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        args.putString("tab", tab());
        ClientPacketDistributor.sendToServer(new Premiebord.Doe(actie, args));
    }

    @Override
    public void onClose() {
        super.onClose();   // ("dicht" is ignored by the server)
    }

    private void tabKnop(String t) {
        CompoundTag a = new CompoundTag();
        a.putString("tab", t);
        ClientPacketDistributor.sendToServer(new Premiebord.Doe("tab", a));
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        // tabs
        int tx = left + 8;
        for (String t : TABS) {
            boolean nu = t.equals(tab());
            Component naam = Component.translatable("vadsrijk.z3.systemen.bord.tab." + t);
            int bw = font.width(naam) + 14;
            knop(g, mx, my, tx, top + 22, bw, 14, nu ? naam.copy().withStyle(ChatFormatting.UNDERLINE) : naam, !nu, () -> tabKnop(t));
            tx += bw + 4;
        }
        // counters
        int rx = left + w - 8;
        Component pen = Component.translatable("vadsrijk.z3.systemen.bord.penningen", data.getIntOr("penningen", 0));
        g.text(font, pen, rx - font.width(pen), top + 25, GOUD, false);
        switch (tab()) {
            case Premiebord.JACHT -> jacht(g, mx, my);
            case Premiebord.WINKEL -> winkel(g, mx, my);
            default -> premies(g, mx, my);
        }
        long s = data.getLongOr("reset", 0);
        Component reset = Component.translatable("vadsrijk.z3.systemen.bord.reset", s / 3600, s / 60 % 60);
        g.text(font, reset, left + 8, top + h - 26, ZACHT, false);
        Component veren = Component.translatable("vadsrijk.z3.systemen.bord.veren", data.getIntOr("veren", 0), data.getIntOr("verenNodig", 25));
        g.text(font, veren, left + w - 8 - font.width(veren), top + h - 26, GOUD, false);
    }

    private void premies(GuiGraphicsExtractor g, int mx, int my) {
        int lv = data.getIntOr("level", 1), min = data.getIntOr("minLevel", 40);
        if (lv < min) {
            g.centeredText(font, Component.translatable("vadsrijk.z3.systemen.premie.te_laag_lv", min), left + w / 2, top + 80, ZACHT);
            return;
        }
        ListTag l = data.getListOrEmpty("premies");
        boolean geruild = data.getBooleanOr("geruild", false);
        int y = top + 42;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            int slot = i, n = e.getIntOr("aantal", 1), v = e.getIntOr("voortgang", 0);
            boolean claimd = e.getBooleanOr("geclaimd", false), klaar = v >= n;
            g.fill(left + 6, y, left + w - 6, y + 36, i % 2 == 0 ? RIJ_A : RIJ_B);
            slot(g, mx, my, left + 10, y + 9, new ItemStack(claimd ? Items.LIME_DYE : e.getBooleanOr("baas", false) ? Items.WITHER_SKELETON_SKULL : Items.PAPER), false);
            g.text(font, Component.translatable(e.getStringOr("key", ""), n), left + 34, y + 5, claimd ? ZACHT : TEKST, false);
            // progress bar
            int bx = left + 34, bw = 120;
            g.fill(bx, y + 20, bx + bw, y + 26, 0xFFE8C8DC);
            g.fill(bx, y + 20, bx + (int) (bw * Math.min(1.0, v / (double) n)), y + 26, klaar ? 0xFF4AC87A : 0xFFFF6FB5);
            g.text(font, v + "/" + n, bx + bw + 4, y + 19, ZACHT, false);
            if (claimd) {
                g.text(font, Component.translatable("vadsrijk.z3.systemen.bord.geclaimd"), left + w - 70, y + 13, GROEN, false);
            } else {
                knop(g, mx, my, left + w - 82, y + 4, 70, 13, Component.translatable("vadsrijk.z3.systemen.bord.claim"), klaar, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putInt("slot", slot);
                    doe("claim", a);
                });
                knop(g, mx, my, left + w - 82, y + 19, 70, 13, Component.translatable("vadsrijk.z3.systemen.bord.ruil"), !geruild, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putInt("slot", slot);
                    doe("ruil", a);
                });
            }
            y += 40;
        }
        Component week = Component.translatable(data.getBooleanOr("weekKist", false) ? "vadsrijk.z3.systemen.bord.week_klaar" : "vadsrijk.z3.systemen.bord.week",
                Math.min(data.getIntOr("weekN", 0), data.getIntOr("weekDoel", 15)), data.getIntOr("weekDoel", 15));
        g.text(font, week, left + 8, y + 2, ZACHT, false);
    }

    private void jacht(GuiGraphicsExtractor g, int mx, int my) {
        int lv = data.getIntOr("level", 1), min = data.getIntOr("contractLevel", 45);
        g.text(font, Component.translatable("vadsrijk.z3.systemen.bord.jacht_uitleg"), left + 8, top + 42, ZACHT, false);
        if (lv < min) {
            g.centeredText(font, Component.translatable("vadsrijk.z3.systemen.contract.te_laag_lv", min), left + w / 2, top + 90, ZACHT);
            return;
        }
        String actief = data.getStringOr("actief", "");
        ListTag l = data.getListOrEmpty("contracten");
        int y = top + 56;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            String id = e.getStringOr("id", "");
            g.fill(left + 6, y, left + w - 6, y + 40, i % 2 == 0 ? RIJ_A : RIJ_B);
            slot(g, mx, my, left + 10, y + 11, new ItemStack(Items.TARGET), false);
            g.text(font, Component.translatable(e.getStringOr("key", "")).withStyle(ChatFormatting.BOLD), left + 34, y + 6, TEKST, false);
            g.text(font, Component.translatable(e.getStringOr("key", "") + ".uitleg"), left + 34, y + 18, ZACHT, false);
            boolean bezig = id.equals(actief);
            knop(g, mx, my, left + w - 82, y + 24, 70, 13, Component.translatable(bezig ? "vadsrijk.z3.systemen.bord.bezig" : "vadsrijk.z3.systemen.bord.neem"),
                    !bezig, () -> {
                        CompoundTag a = new CompoundTag();
                        a.putString("id", id);
                        doe("contract", a);
                    });
            y += 44;
        }
    }

    private void winkel(GuiGraphicsExtractor g, int mx, int my) {
        ListTag l = data.getListOrEmpty("winkel");
        int pen = data.getIntOr("penningen", 0);
        int y = top + 42;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag e = l.getCompoundOrEmpty(i);
            int col = i % 2, x = left + 6 + col * ((w - 12) / 2), bw = (w - 12) / 2 - 2;
            if (col == 0 && i > 0) {
                y += 24;
            }
            String id = e.getStringOr("id", "");
            int prijs = e.getIntOr("prijs", 0);
            g.fill(x, y, x + bw, y + 22, (i / 2) % 2 == 0 ? RIJ_A : RIJ_B);
            slot(g, mx, my, x + 2, y + 2, item(e.getIntOr("item", -1)), false);
            g.text(font, Component.translatable(e.getStringOr("key", "")), x + 23, y + 3, TEKST, false);
            knop(g, mx, my, x + bw - 46, y + 11, 44, 10, Component.translatable("vadsrijk.z3.systemen.bord.prijs", prijs), pen >= prijs, () -> {
                CompoundTag a = new CompoundTag();
                a.putString("id", id);
                doe("koop", a);
            });
        }
        if (l.isEmpty()) {
            slot(g, mx, my, left + 10, y, new ItemStack(Z3Spul.VAHOEGZEGEL.get()), false);
        }
    }

    @SuppressWarnings("unused")
    private static boolean isCompound(Tag t) {
        return t instanceof CompoundTag;
    }
}
