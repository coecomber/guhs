package nl.juiced.guhs.feature.vadsrijk.menu.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/**
 * 1.10.3: the "Bazen" tab of the Vadsrijk-menu. Left: per open zone its bosses (a green/red dot = can you fight it now, your kill
 * count; "???" until you beat it), the next closed zone with its requirement. Right: the chosen boss: name, area, whether you can
 * fight it now (and why not / what to do), level, respawn, your kills, fastest kill, first kill, total damage/healing, what you got
 * from it, and its rare drops with your dry streak (chance, "zeker na nog N keer", a pity bar). Data: {@code bazenboek.Bazenboek.scherm}.
 */
public class BazenPaneel extends MenuPaneel {
    private static final int REGEL = 11, LIJST_B = 150;
    private static final int PAARS = 0xFF7A4AB0;
    private int scroll, detailScroll;
    @Nullable
    private String gekozen;

    public BazenPaneel(CompoundTag data) {
        super("bazen", Component.translatable("vadsrijk.menu.tab.bazen"), data);
    }

    /** One line of the list: a zone heading (b == null) or a boss. */
    private record Regel(CompoundTag zone, @Nullable CompoundTag b) {
    }

    private List<Regel> regels() {
        List<Regel> l = new ArrayList<>();
        ListTag zl = data.getListOrEmpty("zones");
        for (int i = 0; i < zl.size(); i++) {
            CompoundTag z = zl.getCompoundOrEmpty(i);
            l.add(new Regel(z, null));
            ListTag bl = z.getListOrEmpty("bazen");
            for (int j = 0; j < bl.size(); j++) {
                l.add(new Regel(z, bl.getCompoundOrEmpty(j)));
            }
        }
        return l;
    }

    static Component json(String s) {
        if (s == null || s.isEmpty()) {
            return Component.empty();
        }
        try {
            var mc = Minecraft.getInstance();
            net.minecraft.core.HolderLookup.Provider reg = mc.level != null ? mc.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
            return net.minecraft.network.chat.ComponentSerialization.CODEC
                    .parse(reg.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), com.google.gson.JsonParser.parseString(s)).getOrThrow();
        } catch (RuntimeException e) {
            return Component.literal(s);
        }
    }

    static Component naam(CompoundTag b) {
        return b.getBooleanOr("bekend", false) ? Component.translatable(b.getStringOr("naam", "")) : Component.literal("???");
    }

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int x = left + 8, y0 = top + 24, onder = top + h - 8;
        g.fill(x - 2, y0 - 2, x + LIJST_B + 2, onder + 2, 0xFFFFF4FA);
        List<Regel> l = regels();
        if (l.isEmpty()) {
            g.text(font, Component.translatable("vadsrijk.menu.laden"), x + 2, y0 + 2, ZACHT, false);
            return;
        }
        if (gekozen == null) {
            for (Regel r : l) {
                if (r.b() != null) {
                    gekozen = r.b().getStringOr("id", "");
                    break;
                }
            }
        }
        // ---- the list ---------------------------------------------------------------------------------------------------------------
        int zicht = Math.max(1, (onder - y0) / REGEL), max = Math.max(0, l.size() - zicht);
        scroll = Math.max(0, Math.min(scroll, max));
        int y = y0;
        CompoundTag keuze = null;
        for (Regel r : l) {
            if (r.b() != null && r.b().getStringOr("id", "").equals(gekozen)) {
                keuze = r.b();
            }
        }
        for (int i = scroll; i < l.size() && i < scroll + zicht; i++) {
            Regel r = l.get(i);
            if (r.b() == null) {
                CompoundTag z = r.zone();
                boolean open = z.getBooleanOr("open", false);
                g.fill(x, y + REGEL - 2, x + LIJST_B, y + REGEL - 1, 0xFFFFB0D8);
                Component kop = Component.translatable(z.getStringOr("naam", ""));
                if (!open) {
                    kop = Component.literal("🔒 ").append(kop);
                }
                g.text(font, font.plainSubstrByWidth(kop.getString(), LIJST_B - 4), x + 2, y + 1, open ? TEKST : ZACHT, false);
                if (!open) {
                    tip(mx, my, x, y, LIJST_B, REGEL, List.of(Component.translatable("vadsrijk.bazenboek.zone.dicht").withStyle(ChatFormatting.RED),
                            json(z.getStringOr("eis", "")).copy().withStyle(ChatFormatting.GRAY)));
                }
                y += REGEL;
                continue;
            }
            CompoundTag b = r.b();
            boolean sel = b.getStringOr("id", "").equals(gekozen), hover = binnen(mx, my, x, y, LIJST_B, REGEL);
            if (sel || hover) {
                g.fill(x, y, x + LIJST_B, y + REGEL, sel ? 0xFFFFD0EA : 0xFFFFE4F2);
            }
            boolean ja = b.getBooleanOr("toegang", false);
            g.fill(x + 3, y + 3, x + 8, y + 8, ja ? GROEN : ROOD);
            int kills = b.getIntOr("kills", 0);
            String k = kills > 0 ? kills + (b.getBooleanOr("oud", false) ? "+" : "") + "×" : "";
            int kw = font.width(k);
            Component n = naam(b);
            g.text(font, font.plainSubstrByWidth(n.getString(), LIJST_B - 16 - kw), x + 11, y + 1, b.getBooleanOr("bekend", false) ? TEKST : 0xFFA898A4, false);
            if (!k.isEmpty()) {
                g.text(font, k, x + LIJST_B - 2 - kw, y + 1, GOUD, false);
            }
            tip(mx, my, x, y, LIJST_B, REGEL, List.of(n.copy().withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.translatable(b.getStringOr("gebied", "")).withStyle(ChatFormatting.GRAY),
                    json(b.getStringOr("reden", "")).copy().withStyle(ja ? ChatFormatting.GREEN : ChatFormatting.RED)));
            String id = b.getStringOr("id", "");
            klikvlak(x, y, LIJST_B, REGEL, () -> {
                gekozen = id;
                detailScroll = 0;
            });
            y += REGEL;
        }
        if (max > 0) {
            int sh = onder - y0, bh = Math.max(8, sh * zicht / l.size()), by = y0 + (sh - bh) * scroll / max;
            g.fill(x + LIJST_B + 3, y0, x + LIJST_B + 5, onder, 0xFFF0D8E4);
            g.fill(x + LIJST_B + 3, by, x + LIJST_B + 5, by + bh, 0xFFFF5FAF);
        }
        if (keuze != null) {
            detail(g, mx, my, keuze, x + LIJST_B + 12, y0, left + w - 8, onder);
        }
    }

    // ---- the chosen boss ---------------------------------------------------------------------------------------------------------------

    private int dy, dTop, dOnder;

    /** One wrapped text block in the detail column (clipped to the column, scrolled by detailScroll). */
    private void tekst(GuiGraphicsExtractor g, Component c, int x, int bw, int kleur) {
        for (FormattedCharSequence line : font.split(c, bw)) {
            if (dy >= dTop && dy + 9 <= dOnder) {
                g.text(font, line, x, dy, kleur, false);
            }
            dy += 10;
        }
    }

    private void paar(GuiGraphicsExtractor g, Component label, Component waarde, int x, int bw) {
        if (dy >= dTop && dy + 9 <= dOnder) {
            g.text(font, label, x, dy, ZACHT, false);
            g.text(font, waarde, x + bw - font.width(waarde), dy, TEKST, false);
        }
        dy += 10;
    }

    private void detail(GuiGraphicsExtractor g, int mx, int my, CompoundTag b, int x0, int y0, int x1, int onder) {
        int bw = x1 - x0;
        g.fill(x0 - 2, y0 - 2, x1 + 2, onder + 2, 0xFFFFF8FC);
        dTop = y0;
        dOnder = onder;
        dy = y0 - detailScroll;
        boolean bekend = b.getBooleanOr("bekend", false);
        // name + where
        Component n = naam(b).copy().withStyle(ChatFormatting.BOLD);
        tekst(g, n, x0, bw, bekend ? TEKST : 0xFFA898A4);
        tekst(g, Component.translatable("vadsrijk.bazenboek.waar", Component.translatable(b.getStringOr("gebied", "")),
                Component.translatable("vadsrijk.bazenboek.soort." + b.getStringOr("soort", "veld"))), x0, bw, ZACHT);
        dy += 2;
        // can I fight it now?
        boolean ja = b.getBooleanOr("toegang", false);
        int boxTop = dy;
        Component status = Component.translatable(ja ? "vadsrijk.bazenboek.toegang.kop.ja" : "vadsrijk.bazenboek.toegang.kop.nee").withStyle(ChatFormatting.BOLD);
        int regels = 1 + font.split(json(b.getStringOr("reden", "")), bw - 8).size();
        if (boxTop >= dTop && boxTop + regels * 10 + 4 <= dOnder) {
            g.fill(x0, boxTop, x1, boxTop + regels * 10 + 4, ja ? 0xFFD8F4E2 : 0xFFFBDDE4);
            g.fill(x0, boxTop, x0 + 2, boxTop + regels * 10 + 4, ja ? GROEN : ROOD);
        }
        dy += 3;
        tekst(g, status, x0 + 6, bw - 8, ja ? 0xFF1F7A44 : 0xFFB02050);
        tekst(g, json(b.getStringOr("reden", "")), x0 + 6, bw - 8, TEKST);
        dy += 5;
        // the numbers
        int lv = b.getIntOr("lv", 0), kills = b.getIntOr("kills", 0);
        paar(g, Component.translatable("vadsrijk.bazenboek.lv"), Component.literal(lv > 0 ? String.valueOf(lv) : "?"), x0, bw);
        if (b.getIntOr("respawn", 0) > 0) {
            paar(g, Component.translatable("vadsrijk.bazenboek.respawn"), Component.literal(duur(b.getIntOr("respawn", 0))), x0, bw);
        }
        paar(g, Component.translatable("vadsrijk.bazenboek.kills"),
                Component.literal(kills <= 0 ? "0" : kills + (b.getBooleanOr("oud", false) ? "+" : "")), x0, bw);
        if (b.getBooleanOr("oud", false)) {
            klein(g, Component.translatable("vadsrijk.bazenboek.oud"), x0, bw);
        }
        if (b.getDoubleOr("snelst", 0) > 0) {
            paar(g, Component.translatable("vadsrijk.bazenboek.snelst"), Component.literal(duur((int) Math.round(b.getDoubleOr("snelst", 0)))), x0, bw);
        }
        if (b.contains("eerste")) {
            paar(g, Component.translatable("vadsrijk.bazenboek.eerste"),
                    Component.literal(new java.text.SimpleDateFormat("d-M-yyyy", Locale.ROOT).format(new java.util.Date(b.getLongOr("eerste", 0)))), x0, bw);
        }
        if (b.getDoubleOr("schade", 0) > 0) {
            paar(g, Component.translatable("vadsrijk.bazenboek.schade"), Component.literal(getal(b.getDoubleOr("schade", 0))), x0, bw);
        }
        if (b.getDoubleOr("heal", 0) >= 1) {
            paar(g, Component.translatable("vadsrijk.bazenboek.heal"), Component.literal(getal(b.getDoubleOr("heal", 0))), x0, bw);
        }
        if (b.getDoubleOr("tank", 0) >= 1) {
            paar(g, Component.translatable("vadsrijk.bazenboek.tank"), Component.literal(getal(b.getDoubleOr("tank", 0))), x0, bw);
        }
        // what you got from it
        dy += 3;
        tekst(g, Component.translatable("vadsrijk.bazenboek.buit").withStyle(ChatFormatting.BOLD), x0, bw, GOUD);
        CompoundTag r = b.getCompoundOrEmpty("r");
        long munten = b.getLongOr("munten", 0);
        int items = b.getIntOr("items", 0);
        if (munten <= 0 && items <= 0 && r.isEmpty() && b.getListOrEmpty("sp").isEmpty()) {
            tekst(g, Component.translatable("vadsrijk.bazenboek.buit.niks"), x0, bw, ZACHT);
        } else {
            if (munten > 0) {
                paar(g, Component.translatable("vadsrijk.bazenboek.buit.munten"), Component.literal(nl.juiced.guhs.feature.vadsrijk.kern.Spelers.munten(munten)), x0, bw);
            }
            if (!r.isEmpty()) {
                // gear per rarity, in the rarity colours
                net.minecraft.network.chat.MutableComponent rz = Component.empty();
                boolean eerst = true;
                for (Zeldzaamheid z : Zeldzaamheid.values()) {
                    int c = r.getIntOr(z.name(), 0);
                    if (c <= 0) {
                        continue;
                    }
                    if (!eerst) {
                        rz.append(Component.literal("  "));
                    }
                    eerst = false;
                    rz.append(Component.literal(c + "× ").append(Component.translatable(z.key())).withColor(z.rgb));
                }
                tekst(g, Component.translatable("vadsrijk.bazenboek.buit.stukken").append(" ").append(rz), x0, bw, TEKST);
            }
            if (items > 0) {
                paar(g, Component.translatable("vadsrijk.bazenboek.buit.items"), Component.literal(String.valueOf(items)), x0, bw);
            }
            ListTag sp = b.getListOrEmpty("sp");
            for (int i = sp.size() - 1; i >= 0 && i >= sp.size() - 6; i--) {
                tekst(g, Component.literal("★ ").append(json(sp.getStringOr(i, ""))), x0 + 4, bw - 4, 0xFFB0307A);
            }
        }
        // the rare drops with your dry streak
        ListTag zl = b.getListOrEmpty("zeldzaam");
        if (!zl.isEmpty()) {
            dy += 3;
            tekst(g, Component.translatable("vadsrijk.bazenboek.zeldzaam").withStyle(ChatFormatting.BOLD), x0, bw, PAARS);
            for (int i = 0; i < zl.size(); i++) {
                CompoundTag d = zl.getCompoundOrEmpty(i);
                boolean heeft = d.getBooleanOr("heeft", false);
                tekst(g, json(d.getStringOr("n", "")), x0, bw, heeft ? 0xFF1F7A44 : TEKST);
                tekst(g, json(d.getStringOr("regel", "")), x0 + 6, bw - 6, heeft ? 0xFF1F7A44 : PAARS);
                int pech = d.getIntOr("pech", 0), pity = d.getIntOr("pity", 0);
                if (!heeft && pity > 0) {
                    if (dy >= dTop && dy + 6 <= dOnder) {
                        int bx = x0 + 6, bb = bw - 6 - 40;
                        g.fill(bx, dy + 1, bx + bb, dy + 5, 0xFFE8C8DC);
                        g.fill(bx, dy + 1, bx + bb * Math.min(pech, pity) / pity, dy + 5, pech >= pity ? 0xFFFFC040 : 0xFFB070E0);
                        String t = pech + " / " + pity;
                        g.text(font, t, x1 - font.width(t), dy - 1, ZACHT, false);
                        tip(mx, my, bx, dy, bw - 6, 7, List.of(Component.translatable("vadsrijk.vint.pech.balk", pech, pity).withStyle(ChatFormatting.LIGHT_PURPLE),
                                Component.translatable("vadsrijk.dungeon.pech.uitleg").withStyle(ChatFormatting.GRAY)));
                    }
                    dy += 9;
                }
                dy += 2;
            }
        }
        // (scroll the detail when it is longer than the column)
        int hoog = dy + detailScroll - y0;
        detailMax = Math.max(0, hoog - (onder - y0));
        detailScroll = Math.min(detailScroll, detailMax);
        detailX = x0;
        if (detailMax > 0) {
            int sh = onder - y0, bh = Math.max(8, sh * sh / hoog), by = y0 + (sh - bh) * detailScroll / detailMax;
            g.fill(x1 + 1, y0, x1 + 2, onder, 0xFFF0D8E4);
            g.fill(x1 + 1, by, x1 + 2, by + bh, 0xFFFF5FAF);
        }
    }

    private int detailMax, detailX = Integer.MAX_VALUE;

    private void klein(GuiGraphicsExtractor g, Component c, int x, int bw) {
        for (FormattedCharSequence line : font.split(c, (int) (bw / 0.75f))) {
            if (dy >= dTop && dy + 7 <= dOnder) {
                g.pose().pushMatrix();
                g.pose().translate(x, dy);
                g.pose().scale(0.75f, 0.75f);
                g.text(font, line, 0, 0, ZACHT, false);
                g.pose().popMatrix();
            }
            dy += 7;
        }
    }

    static String duur(int s) {
        if (s >= 3600) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
        }
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    static String getal(double v) {
        return String.format(Locale.GERMANY, "%,d", Math.round(v));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int stap = dy > 0 ? -1 : dy < 0 ? 1 : 0;
        if (mx >= detailX) {
            detailScroll = Math.max(0, Math.min(detailMax, detailScroll + stap * 20));
        } else {
            scroll = Math.max(0, scroll + stap * 3);
        }
        return true;
    }
}
