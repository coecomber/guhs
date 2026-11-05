package nl.juiced.guhs.feature.vadsrijk.kleding.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.kleding.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.kleding.KledingPayloads;
import nl.juiced.guhs.feature.vadsrijk.kleding.Kledingkast;

/**
 * T-KLEDING: the Kledingkast screen (the pink economie style). Left: you, turning with the mouse, wearing your choices at once
 * (every click is sent and comes straight back), plus the two effects sliders. Middle: the categories. Right: the list of the
 * category: what you have (click = put on / take off), what is still locked (hover = where it comes from; secrets stay "???").
 */
public class KledingScherm extends EcoScherm {
    private static final String[] TABS = {"rug", "hoofd", "harnas", "verf", "naamkleur", "titel", "plof", "spoor", "aura"};
    private static final int RIJ = 14;
    private String tab = "rug";
    private int scroll;

    public KledingScherm(CompoundTag data) {
        super("kledingkast", Component.translatable("vadsrijk.kleding.titel"), data, List.of(), 400, 232);
    }

    @Override
    protected String collectieTab() {
        return KledingClient.TAB;
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        ClientPacketDistributor.sendToServer(new KledingPayloads.Actie(actie, args.getStringOr("a", ""), args.getStringOr("b", "")));
    }

    private void stuur(String actie, String a, String b) {
        CompoundTag t = new CompoundTag();
        t.putString("a", a);
        t.putString("b", b);
        doe(actie, t);
    }

    private Set<String> heeft() {
        Set<String> s = new HashSet<>();
        for (Tag t : data.getListOrEmpty("heeft")) {
            t.asString().ifPresent(s::add);
        }
        return s;
    }

    private String keuze(String k) {
        return data.getCompoundOrEmpty("keuze").getStringOr(k, "");
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        Minecraft mc = Minecraft.getInstance();
        // ---- left: the preview + sliders
        int px = left + 8, py = top + 24;
        g.fill(px, py, px + 82, py + 150, 0xFFFFF4FA);
        if (mc.player != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, px, py, px + 82, py + 146, 52, 0.0625f, mx, my, mc.player);
        }
        knop(g, mx, my, px, py + 154, 82, 16, Component.translatable("vadsrijk.kleding.anderen",
                Component.translatable("vadsrijk.kleding.niveau." + KledingCache.anderen().name().toLowerCase(Locale.ROOT))), true,
                () -> KledingCache.zetAnderen(KledingCache.anderen().volgende()));
        tip(mx, my, px, py + 154, 82, 16, List.of(Component.translatable("vadsrijk.kleding.anderen.tip")));
        knop(g, mx, my, px, py + 173, 82, 16, Component.translatable("vadsrijk.kleding.eigen",
                Component.translatable("vadsrijk.kleding.niveau." + KledingCache.eigen().name().toLowerCase(Locale.ROOT))), true,
                () -> KledingCache.zetEigen(KledingCache.eigen().volgende()));
        tip(mx, my, px, py + 173, 82, 16, List.of(Component.translatable("vadsrijk.kleding.eigen.tip")));
        // ---- middle: the categories
        int tx = left + 98;
        for (int i = 0; i < TABS.length; i++) {
            String t = TABS[i];
            boolean aan = t.equals(tab);
            int y = top + 24 + i * 20;
            if (aan) {
                g.fill(tx - 1, y - 1, tx + 75, y + 18, 0xFFFF4FA8);
            }
            knop(g, mx, my, tx, y, 74, 17, Component.translatable("vadsrijk.kleding.tab." + t), !aan, () -> {
                tab = t;
                scroll = 0;
            });
            if (aan) {
                g.centeredText(font, Component.translatable("vadsrijk.kleding.tab." + t).withStyle(ChatFormatting.BOLD), tx + 37, y + 5, WIT);
            }
        }
        // ---- right: the list
        int lx = left + 182, ly = top + 24, lw = w - 190;
        g.fill(lx - 2, ly - 2, lx + lw, top + h - 4, 0xFFFFF4FA);
        List<Rij> rijen = rijen();
        int zicht = (top + h - 8 - ly) / RIJ;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rijen.size() - zicht)));
        for (int i = 0; i < zicht && i + scroll < rijen.size(); i++) {
            rijen.get(i + scroll).teken(g, mx, my, lx, ly + i * RIJ, lw - 2, i % 2 == 0);
        }
        if (rijen.size() > zicht) {
            g.text(font, Component.literal((scroll + 1) + "-" + Math.min(rijen.size(), scroll + zicht) + "/" + rijen.size()), lx + lw - 40, top + 6, WIT, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll -= (int) Math.signum(dy);
        return true;
    }

    // ---- rows -----------------------------------------------------------------------------------------------------------------------

    /** One row of the list. */
    private interface Rij {
        void teken(GuiGraphicsExtractor g, int mx, int my, int x, int y, int bw, boolean even);
    }

    private List<Rij> rijen() {
        List<Rij> l = new ArrayList<>();
        Set<String> heeft = heeft();
        switch (tab) {
            case "harnas" -> {
                if (!data.getBooleanOr("binnen", true)) {
                    l.add(tekst(Component.translatable("vadsrijk.kleding.harnas.thuis")));
                }
                for (EquipmentSlot s : Kledingkast.HARNAS) {
                    l.add(harnasRij(s));
                }
                l.add(tekst(Component.translatable("vadsrijk.kleding.harnas.uitleg")));
            }
            case "titel" -> {
                String nu = data.getStringOr("titel", "");
                l.add(keuzeRij(Component.translatable("vadsrijk.kleding.geen_titel"), nu.isEmpty(), () -> stuur("titel", "", "")));
                for (Tag t : data.getListOrEmpty("titels")) {
                    if (t instanceof CompoundTag c) {
                        String id = c.getStringOr("id", "");
                        l.add(keuzeRij(Component.translatable(c.getStringOr("naam", "")).withColor(c.getIntOr("kleur", 0xFFFFFF)), id.equals(nu),
                                () -> stuur("titel", "", id)));
                    }
                }
                if (data.getListOrEmpty("titels").isEmpty()) {
                    l.add(tekst(Component.translatable("vadsrijk.kleding.geen_titels")));
                }
            }
            default -> {
                List<Cosmetica.Soort> soorten = switch (tab) {
                    case "rug" -> List.of(Cosmetica.Soort.VLEUGEL, Cosmetica.Soort.KAAP);
                    case "hoofd" -> List.of(Cosmetica.Soort.ACCESSOIRE);
                    default -> List.of(Cosmetica.Soort.van(tab));
                };
                Cosmetica.Soort eerste = soorten.get(0);
                String gekozen = keuze(eerste.keuze());
                l.add(keuzeRij(Component.translatable("vadsrijk.kleding.niets." + tab), gekozen.isEmpty(), () -> {
                    Cosmetica.Ding nu = Cosmetica.van(gekozen);
                    stuur("kies", (nu == null ? eerste : nu.soort()).id, "");
                }));
                if (tab.equals("rug")) {
                    l.add(tekst(Component.translatable(data.getBooleanOr("zweven", false) ? "vadsrijk.kleding.rug.zweven" : "vadsrijk.kleding.rug.nogniet")
                            .withStyle(ChatFormatting.ITALIC)));
                }
                for (Cosmetica.Soort s : soorten) {
                    for (Cosmetica.Ding d : Cosmetica.van(s)) {
                        l.add(dingRij(d, heeft.contains(d.id()), d.id().equals(gekozen)));
                    }
                }
            }
        }
        return l;
    }

    private Rij tekst(Component c) {
        return (g, mx, my, x, y, bw, even) -> g.text(font, font.plainSubstrByWidth(c.getString(), bw - 6), x + 3, y + 3, ZACHT, false);
    }

    private Rij keuzeRij(Component naam, boolean aan, Runnable kies) {
        return (g, mx, my, x, y, bw, even) -> {
            g.fill(x, y, x + bw, y + RIJ - 1, even ? RIJ_A : RIJ_B);
            g.text(font, naam, x + 4, y + 3, TEKST, false);
            if (aan) {
                g.text(font, Component.translatable("vadsrijk.kleding.aan").withStyle(ChatFormatting.BOLD), x + bw - 34, y + 3, GROEN, false);
            } else {
                knop(g, mx, my, x + bw - 40, y + 1, 38, RIJ - 3, Component.translatable("vadsrijk.kleding.kies"), true, kies);
            }
        };
    }

    private Rij dingRij(Cosmetica.Ding d, boolean heb, boolean aan) {
        return (g, mx, my, x, y, bw, even) -> {
            g.fill(x, y, x + bw, y + RIJ - 1, even ? RIJ_A : RIJ_B);
            int tx = x + 4;
            if (d.kleur() != 0) {
                g.fill(x + 3, y + 2, x + 12, y + 11, 0xFF4A1E46);
                g.fill(x + 4, y + 3, x + 11, y + 10, 0xFF000000 | d.kleur());
                tx = x + 15;
            }
            Component naam = heb || !d.geheim() ? Component.translatable(d.naamKey()) : Component.literal("???");
            g.text(font, naam, tx, y + 3, heb ? TEKST : ZACHT, false);
            List<Component> tip = new ArrayList<>();
            tip.add(naam.copy().withStyle(ChatFormatting.BOLD));
            if (d.kracht() != null) {
                tip.add(Component.translatable("vadsrijk.kleding.kracht." + d.kracht().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.AQUA));
                tip.add(Component.translatable("vadsrijk.kleding.kracht.alleen_rijk").withStyle(ChatFormatting.GRAY));
            }
            tip.add(heb ? Component.translatable("vadsrijk.kleding.heb").withStyle(ChatFormatting.GREEN)
                    : Component.translatable(d.later() ? "vadsrijk.kleding.later" : "vadsrijk.kleding.slot", Component.translatable(d.bronKey()))
                            .withStyle(ChatFormatting.GOLD));
            tip(mx, my, x, y, bw - 44, RIJ, tip);
            if (aan) {
                g.text(font, Component.translatable("vadsrijk.kleding.aan").withStyle(ChatFormatting.BOLD), x + bw - 34, y + 3, GROEN, false);
            } else if (heb) {
                knop(g, mx, my, x + bw - 40, y + 1, 38, RIJ - 3, Component.translatable("vadsrijk.kleding.kies"), true,
                        () -> stuur("kies", d.soort().id, d.id()));
            } else {
                g.text(font, Component.translatable(d.later() ? "vadsrijk.kleding.later.kort" : "vadsrijk.kleding.dicht"), x + bw - 40, y + 3, ZACHT, false);
            }
        };
    }

    /** A transmog row: the slot, the look now, and &lt; &gt; through [own piece, hidden, every collected look]. */
    private Rij harnasRij(EquipmentSlot s) {
        List<String> opties = new ArrayList<>();
        opties.add("");
        opties.add(Kledingkast.VERBORGEN);
        ListTag looks = data.getCompoundOrEmpty("looks").getListOrEmpty(s.getName());
        for (Tag t : looks) {
            t.asString().ifPresent(opties::add);
        }
        String nu = keuze("tm." + s.getName());
        int i = Math.max(0, opties.indexOf(nu));
        return (g, mx, my, x, y, bw, even) -> {
            g.fill(x, y, x + bw, y + RIJ - 1, even ? RIJ_A : RIJ_B);
            g.text(font, Component.translatable("vadsrijk.kleding.slot." + s.getName()), x + 4, y + 3, TEKST, false);
            String o = opties.get(i);
            Component wat;
            if (o.isEmpty()) {
                wat = Component.translatable("vadsrijk.kleding.tm.eigen");
            } else if (o.equals(Kledingkast.VERBORGEN)) {
                wat = Component.translatable("vadsrijk.kleding.tm.verborgen");
            } else {
                ItemStack st = KledingLaag.look(o);
                wat = st.isEmpty() ? Component.literal(o) : st.getHoverName();
                g.pose().pushMatrix();
                g.pose().translate(x + 58, y);
                g.pose().scale(0.75f, 0.75f);
                g.item(st, 0, 0);
                g.pose().popMatrix();
            }
            g.text(font, font.plainSubstrByWidth(wat.getString(), bw - 120), x + 72, y + 3, TEKST, false);
            knop(g, mx, my, x + bw - 38, y + 1, 17, RIJ - 3, Component.literal("<"), opties.size() > 1,
                    () -> stuur("tm", s.getName(), opties.get((i - 1 + opties.size()) % opties.size())));
            knop(g, mx, my, x + bw - 19, y + 1, 17, RIJ - 3, Component.literal(">"), opties.size() > 1,
                    () -> stuur("tm", s.getName(), opties.get((i + 1) % opties.size())));
        };
    }
}
