package nl.juiced.guhs.feature.vadsrijk.zone2.systemen.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Juwelier;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.JuwelierPayloads;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;

/**
 * The juweliersguh's screen (same pink style as the economie screens): left your gear with sockets and your crystals, right
 * the chosen piece with its sockets. Empty socket + a chosen crystal: "Inzetten" (free). A gem: "Uithalen" for a small fee, the
 * crystal comes back.
 */
public class JuwelierScherm extends EcoScherm {
    public static final String SCHERM = "z2_juwelier";
    private int kies = -1, kristal = -1;
    private int glinster = -1;

    public JuwelierScherm(CompoundTag data, List<ItemStack> items) {
        super(SCHERM, Component.translatable("vadsrijk.z2.systemen.juwelier.titel"), data, items, 330, 244);   // (z3-systemen: room for a 3rd socket)
        kies = data.getIntOr("kies", -1);
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        ClientPacketDistributor.sendToServer(new JuwelierPayloads.Doe(actie, args));
    }

    @Override
    protected void opNieuweData() {
        int k = data.getIntOr("kies", -1);
        if (k >= 0) {
            kies = k;
        }
        if (vind("kristallen", kristal) == null) {
            kristal = -1;
        }
        String m = data.getStringOr("melding", "");
        if (m.endsWith(".gezet") || m.endsWith(".gehaald")) {
            glinster = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (glinster >= 0 && ++glinster > 20) {
            glinster = -1;
        }
    }

    private CompoundTag vind(String lijst, int slot) {
        ListTag l = data.getListOrEmpty(lijst);
        for (int i = 0; i < l.size(); i++) {
            if (l.getCompoundOrEmpty(i).getIntOr("slot", -2) == slot) {
                return l.getCompoundOrEmpty(i);
            }
        }
        return null;
    }

    private ItemStack stapel(String lijst, int slot) {
        CompoundTag t = vind(lijst, slot);
        return t == null ? ItemStack.EMPTY : item(t.getIntOr("item", -1));
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        // left: gear with sockets
        kop(g, Component.translatable("vadsrijk.z2.systemen.juwelier.gear"), left + 8, top + 24);
        ListTag gear = data.getListOrEmpty("gear");
        for (int i = 0; i < gear.size(); i++) {
            CompoundTag t = gear.getCompoundOrEmpty(i);
            int x = left + 8 + (i % 6) * 19, y = top + 36 + (i / 6) * 19;
            int slot = t.getIntOr("slot", -1);
            slot(g, mx, my, x, y, item(t.getIntOr("item", -1)), slot == kies);
            klikvlak(x, y, 18, 18, () -> kies = slot);
        }
        if (gear.isEmpty()) {
            for (var r : font.split(Component.translatable("vadsrijk.z2.systemen.juwelier.geen_gear"), 112)) {
                g.text(font, r, left + 8, top + 40 + 0, ZACHT, false);
                break;
            }
        }
        // left bottom: crystals
        int ky = top + 118;
        kop(g, Component.translatable("vadsrijk.z2.systemen.juwelier.kristallen"), left + 8, ky);
        ListTag kr = data.getListOrEmpty("kristallen");
        for (int i = 0; i < kr.size(); i++) {
            CompoundTag t = kr.getCompoundOrEmpty(i);
            int x = left + 8 + (i % 6) * 19, y = ky + 12 + (i / 6) * 19;
            int slot = t.getIntOr("slot", -1);
            slot(g, mx, my, x, y, item(t.getIntOr("item", -1)), slot == kristal);
            klikvlak(x, y, 18, 18, () -> kristal = slot);
        }
        if (kr.isEmpty()) {
            g.text(font, Component.translatable("vadsrijk.z2.systemen.juwelier.geen_kristal"), left + 8, ky + 14, ZACHT, false);
        }

        // right: the piece and its sockets
        int rx = left + 128, rw = w - 136;
        g.fill(rx, top + 22, rx + rw, top + h - 16, 0xFFFFF6FB);
        ItemStack s = stapel("gear", kies);
        Uitrusting u = Rollen.van(s);
        if (u == null) {
            int yy = top + 80;
            for (var r : font.split(Component.translatable("vadsrijk.z2.systemen.juwelier.kies"), rw - 16)) {
                g.centeredText(font, r, rx + rw / 2, yy, ZACHT);
                yy += 10;
            }
            return;
        }
        float bob = Mth.sin((System.currentTimeMillis() % 2400) / 2400f * Mth.TWO_PI) * 1.5f;
        g.pose().pushMatrix();
        g.pose().translate(rx + 10, top + 28 + bob);
        g.pose().scale(2f, 2f);
        g.item(s, 0, 0);
        g.pose().popMatrix();
        g.text(font, s.getHoverName(), rx + 48, top + 32, TEKST, false);
        g.text(font, Component.translatable("vadsrijk.z2.systemen.juwelier.aantal", u.sokjes().size()), rx + 48, top + 44, ZACHT, false);
        if (glinster >= 0) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 + glinster * 0.2;
                int px = rx + 26 + (int) (Math.cos(a) * (8 + glinster)), py = top + 44 + (int) (Math.sin(a) * (8 + glinster));
                g.fill(px, py, px + 2, py + 2, 0xFFFFE070);
            }
        }
        ItemStack kiesKristal = stapel("kristallen", kristal);
        int y = top + 70;
        for (int i = 0; i < u.sokjes().size(); i++) {
            String gem = u.sokjes().get(i);
            int sokje = i;
            g.fill(rx + 6, y - 3, rx + rw - 6, y + 31, i % 2 == 0 ? RIJ_A : RIJ_B);
            slot(g, mx, my, rx + 10, y + 4, gem.isEmpty() ? ItemStack.EMPTY : Kristallen.stack(gem), false);
            if (gem.isEmpty()) {
                g.text(font, Component.translatable("vadsrijk.z2.systemen.sokje.leeg"), rx + 34, y + 2, ZACHT, false);
                boolean kan = !kiesKristal.isEmpty();
                knop(g, mx, my, rx + 34, y + 13, 90, 14, Component.translatable("vadsrijk.z2.systemen.juwelier.zet"), kan, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putInt("slot", kies);
                    a.putInt("sokje", sokje);
                    a.putInt("kristal", kristal);
                    doe("zet", a);
                    geluid(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
                });
                if (!kan) {
                    tip(mx, my, rx + 34, y + 13, 90, 14, List.of(Component.translatable("vadsrijk.z2.systemen.juwelier.kies_kristal")));
                }
            } else {
                g.text(font, Kristallen.regel(gem), rx + 34, y + 2, TEKST, false);
                long prijs = Juwelier.prijs(gem);
                boolean kan = data.getLongOr("munten", 0) >= prijs;
                knop(g, mx, my, rx + 34, y + 13, 90, 14, Component.translatable("vadsrijk.z2.systemen.juwelier.haal"), kan, () -> {
                    CompoundTag a = new CompoundTag();
                    a.putInt("slot", kies);
                    a.putInt("sokje", sokje);
                    doe("haal", a);
                });
                munten(g, rx + 130, y + 16, prijs, false, false);
            }
            y += 36;
        }
        int yy = top + h - 40;
        for (var r : font.split(Component.translatable("vadsrijk.z2.systemen.juwelier.uitleg").withStyle(ChatFormatting.ITALIC), rw - 12)) {
            g.text(font, r, rx + 6, yy, ZACHT, false);
            yy += 9;
        }
    }
}
