package nl.juiced.guhs.feature.vadsrijk.voltooiing.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.VoltooiingPayloads;

/**
 * 1.10.3 Kampvuurreizen: the small travel screen at a campfire of a fully explored zone. The pink panel of the other Vadsrijk
 * screens; on top "Je bent bij Kampvuur: Sporenwoud 2"; below the lit campfires of every 100 % zone, under a ribbon per zone,
 * named like the Verkenningsboek. The one you stand at is greyed out ("hier"); a click on another one asks the server to travel
 * (everything is checked there; a refusal comes back as the message line at the bottom). Mouse wheel scrolls a long list.
 */
public class ReisScherm extends EcoScherm {
    private static final int RIJ = 14, KOP = 13, LIJST_Y = 36, ONDER = 16, MAX_H = 216;
    private static final int LINT = 0xFFFF5FAF, LINT_ZACHT = 0xFFFFD0E6;
    private static final ItemStack VUUR = new ItemStack(Items.CAMPFIRE);
    private int scroll;

    /** One line of the list: a zone ribbon (vuur == null) or a campfire. */
    private record Regel(Component tekst, CompoundTag vuur) {
    }

    public ReisScherm(CompoundTag data) {
        super("kampvuurreis", Component.translatable("vadsrijk.voltooiing.reis.titel"), data, List.of(), 236, hoogte(data));
    }

    private static int hoogte(CompoundTag data) {
        int px = 0;
        ListTag zl = data.getListOrEmpty("zones");
        for (int i = 0; i < zl.size(); i++) {
            px += KOP + zl.getCompoundOrEmpty(i).getListOrEmpty("v").size() * RIJ + 3;
        }
        return Math.max(96, Math.min(MAX_H, LIJST_Y + px + ONDER + 2));
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        if (actie.equals("reis")) {   // (EcoScherm's "dicht" on close is not for us: the server forgets the screen by itself)
            ClientPacketDistributor.sendToServer(new VoltooiingPayloads.ReisKies(args.getStringOr("id", "")));
        }
    }

    private List<Regel> regels() {
        List<Regel> l = new ArrayList<>();
        ListTag zl = data.getListOrEmpty("zones");
        for (int i = 0; i < zl.size(); i++) {
            CompoundTag zt = zl.getCompoundOrEmpty(i);
            l.add(new Regel(Component.translatable(zt.getStringOr("naam", "")), null));
            ListTag vl = zt.getListOrEmpty("v");
            for (int j = 0; j < vl.size(); j++) {
                CompoundTag vt = vl.getCompoundOrEmpty(j);
                l.add(new Regel(BoekScherm.naam(vt), vt));
            }
        }
        return l;
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        // where you are
        CompoundTag hierTag = new CompoundTag();
        hierTag.putString("n", data.getStringOr("hn", ""));
        hierTag.putString("na", data.getStringOr("hna", ""));
        icoon(g, left + 8, top + 22);
        Component hier = Component.translatable("vadsrijk.voltooiing.reis.hier", BoekScherm.naam(hierTag).withStyle(ChatFormatting.BOLD));
        g.text(font, Component.literal(font.plainSubstrByWidth(hier.getString(), w - 30)), left + 22, top + 23, ZACHT, false);
        g.fill(left + 6, top + LIJST_Y - 4, left + w - 6, top + LIJST_Y - 3, LINT_ZACHT);

        // the list (rows fully inside the area only; the wheel scrolls)
        List<Regel> rl = regels();
        int y0 = top + LIJST_Y, y1 = top + h - ONDER;
        int totaal = 0;
        for (Regel r : rl) {
            totaal += r.vuur() == null ? KOP + 3 : RIJ;
        }
        int maxScroll = Math.max(0, totaal - (y1 - y0));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        String hierId = data.getStringOr("hier", "");
        int y = y0 - scroll, wissel = 0;
        for (int i = 0; i < rl.size(); i++) {
            Regel r = rl.get(i);
            boolean kop = r.vuur() == null;
            int rh = kop ? KOP : RIJ;
            if (kop && i > 0) {
                y += 3;
            }
            if (y >= y0 && y + rh <= y1) {
                if (kop) {
                    wissel = 0;
                    g.fill(left + 6, y, left + 9, y + rh - 1, LINT);
                    g.text(font, r.tekst().copy().withStyle(ChatFormatting.BOLD), left + 13, y + 2, LINT, false);
                    int tw = font.width(r.tekst().copy().withStyle(ChatFormatting.BOLD));
                    g.fill(left + 17 + tw, y + 6, left + w - 6, y + 7, LINT_ZACHT);
                } else {
                    vuurRij(g, mx, my, r, y, hierId, wissel++);
                }
            } else if (!kop) {
                wissel++;
            }
            y += rh;
        }
        if (maxScroll > 0) {   // a thin scroll bar on the right
            int bh = y1 - y0, kh = Math.max(12, bh * bh / (bh + maxScroll));
            int ky = y0 + (bh - kh) * scroll / maxScroll;
            g.fill(left + w - 4, y0, left + w - 2, y1, 0x30FF5FAF);
            g.fill(left + w - 4, ky, left + w - 2, ky + kh, LINT);
        }
        // the footer
        if (data.getStringOr("melding", "").isEmpty()) {
            g.centeredText(font, Component.translatable("vadsrijk.voltooiing.reis.onder"), left + w / 2, top + h - 12, GRIJS);
        }
    }

    private static final int GRIJS = 0xFFA898A4;

    private void vuurRij(GuiGraphicsExtractor g, int mx, int my, Regel r, int y, String hierId, int nr) {
        String id = r.vuur().getStringOr("id", "");
        boolean isHier = id.equals(hierId);
        int x0 = left + 8, x1 = left + w - 8;
        boolean hover = !isHier && binnen(mx, my, x0, y, x1 - x0, RIJ - 1);
        g.fill(x0, y, x1, y + RIJ - 1, hover ? HOVER : nr % 2 == 0 ? RIJ_A : RIJ_B);
        icoon(g, x0 + 2, y + 1);
        String naam = font.plainSubstrByWidth(r.tekst().getString(), x1 - x0 - 60);
        g.text(font, naam, x0 + 15, y + 3, isHier ? GRIJS : TEKST, false);
        if (isHier) {
            Component h = Component.translatable("vadsrijk.voltooiing.reis.hier.label").withStyle(ChatFormatting.ITALIC);
            g.text(font, h, x1 - 4 - font.width(h), y + 3, GRIJS, false);
            return;
        }
        Component pijl = Component.translatable("vadsrijk.voltooiing.reis.ga");
        g.text(font, pijl, x1 - 4 - font.width(pijl), y + 3, hover ? WIT : LINT, hover);
        tip(mx, my, x0, y, x1 - x0, RIJ - 1, List.of(r.tekst().copy().withStyle(ChatFormatting.BOLD),
                Component.translatable("vadsrijk.voltooiing.reis.tip").withStyle(ChatFormatting.GRAY)));
        klikvlak(x0, y, x1 - x0, RIJ - 1, () -> {
            CompoundTag a = new CompoundTag();
            a.putString("id", id);
            doe("reis", a);
        });
    }

    /** A small campfire icon (10x10). */
    private void icoon(GuiGraphicsExtractor g, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.625f, 0.625f);
        g.item(VUUR, 0, 0);
        g.pose().popMatrix();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll += dy > 0 ? -RIJ : dy < 0 ? RIJ : 0;
        if (scroll < 0) {
            scroll = 0;
        }
        return true;
    }
}
