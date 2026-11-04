package nl.juiced.guhs.feature.vadsrijk.voltooiing.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;

/**
 * I-VOLTOOIING: the Verkenningsboek. An open pink book: zone bookmarks on top; the left page shows the zone's overall percentage
 * (a big number + a ribbon bar), the 100 % reward, and every category with its icon, x/y and a little bar (click one); the right
 * page lists the chosen category: done = green tick + name, busy = yellow dot, not done = grey (secrets: "???" + a small hint).
 * The collection tabs (Rijdieren / Huisdieren / Verkenningsboek) sit above the panel (i-ui CollectieTabs, via EcoScherm). All data comes from the server.
 */
public class BoekScherm extends EcoScherm {
    private static final int RIJ = 13, REGEL = 11;
    private static final int PAPIER = 0xFFFFF8EC, PAPIER_RAND = 0xFFF2D9C8, LINT = 0xFFFF5FAF, LINT_ZACHT = 0xFFFFD0E6, GRIJS = 0xFFA898A4;
    private int zone, categorie, scroll;
    private boolean eerste = true;

    public BoekScherm(CompoundTag data) {
        super("voltooiing", Component.translatable("vadsrijk.voltooiing.titel"), data, List.of(), 380, 232);
    }

    @Override
    protected void init() {
        super.init();
        if (eerste) {
            eerste = false;
            String hier = data.getStringOr("hier", "");
            ListTag zl = zones();
            for (int i = 0; i < zl.size(); i++) {
                if (zl.getCompoundOrEmpty(i).getStringOr("id", "").equals(hier)) {
                    zone = i;
                }
            }
        }
    }

    /** Its tab next to Rijdieren / Huisdieren (CollectieTabs, drawn and clicked by EcoScherm). */
    @Override
    protected String collectieTab() {
        return VoltooiingClient.TAB;
    }

    static net.minecraft.world.item.ItemStack icoon(String id) {
        try {
            return new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id)));
        } catch (RuntimeException e) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }

    @Override
    protected void opNieuweData() {
        zone = Math.min(zone, Math.max(0, zones().size() - 1));
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        // (nothing goes back: the book only shows; EcoScherm's "dicht" is not for us)
    }

    private ListTag zones() {
        return data.getListOrEmpty("zones");
    }

    private CompoundTag z() {
        return zones().getCompoundOrEmpty(zone);
    }

    private ListTag cats() {
        return z().getListOrEmpty("c");
    }

    // ---- drawing --------------------------------------------------------------------------------------------------------------

    @Override
    protected void inhoud(GuiGraphicsExtractor g, int mx, int my, float pt) {
        ListTag zl = zones();
        if (zl.isEmpty()) {
            return;
        }
        // ---- zone bookmarks in the title band (right-aligned) ------------------------------------------------------------------
        int bx = left + w - 6;
        for (int i = zl.size() - 1; i >= 0; i--) {
            CompoundTag zt = zl.getCompoundOrEmpty(i);
            Component naam = Component.translatable(zt.getStringOr("naam", ""));
            int bw = font.width(naam) + 10;
            bx -= bw;
            boolean sel = i == zone, hover = binnen(mx, my, bx, top + 2, bw, 15);
            g.fill(bx, top + 2, bx + bw, top + 18, sel ? PANEEL : hover ? 0xFFFFB0D8 : 0xFFFF9ACC);
            g.text(font, naam, bx + 5, top + 6, sel ? TEKST : WIT, !sel);
            final int idx = i;
            if (!sel) {
                klikvlak(bx, top + 2, bw, 16, () -> {
                    zone = idx;
                    categorie = 0;
                    scroll = 0;
                });
            }
            bx -= 3;
        }

        // ---- the open book -----------------------------------------------------------------------------------------------------
        int bl = left + 6, bt = top + 22, br = left + w - 6, bb = top + h - 6, mid = (bl + br) / 2;
        g.fill(bl - 1, bt - 1, br + 1, bb + 1, 0xFFE7B9CF);
        g.fill(bl, bt, mid - 2, bb, PAPIER);
        g.fill(mid + 2, bt, br, bb, PAPIER);
        g.fill(mid - 2, bt, mid + 2, bb, 0xFFE89CC4);   // the spine
        g.fill(mid - 1, bt, mid, bb, 0xFFD078A8);
        g.fill(bl, bb - 2, mid - 2, bb, PAPIER_RAND);
        g.fill(mid + 2, bb - 2, br, bb, PAPIER_RAND);
        // a ribbon bookmark hanging from the top of the right page
        g.fill(br - 16, bt, br - 10, bt + 14, LINT);
        g.fill(br - 16, bt + 14, br - 13, bt + 17, LINT);
        g.fill(br - 13, bt + 14, br - 10, bt + 17, LINT);

        linkerPagina(g, mx, my, bl + 7, bt + 6, mid - 2 - bl - 14, bb - 6);
        rechterPagina(g, mx, my, mid + 9, bt + 6, br - mid - 2 - 14 - 10, bb - 6);

    }

    private void linkerPagina(GuiGraphicsExtractor g, int mx, int my, int x, int y, int pw, int onder) {
        CompoundTag zt = z();
        int k = zt.getIntOr("k", 0), t = zt.getIntOr("t", 0), pct = t == 0 ? 100 : (int) Math.floor(k * 100.0 / t);
        // big percentage + zone name
        String p = pct + "%";
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(2f, 2f);
        g.text(font, p, 0, 0, pct >= 100 ? GOUD : LINT, false);
        g.pose().popMatrix();
        int nx = x + font.width(p) * 2 + 6;
        g.text(font, Component.translatable(zt.getStringOr("naam", "")).withStyle(ChatFormatting.BOLD), nx, y, TEKST, false);
        g.text(font, Component.translatable("vadsrijk.voltooiing.verkend", k, t), nx, y + 9, ZACHT, false);
        y += 21;
        balk(g, x, y, pw, 5, t == 0 ? 1 : k / (float) t, pct >= 100);
        y += 9;
        // the reward line
        Component beloning = zt.getBooleanOr("beloond", false)
                ? Component.translatable("vadsrijk.voltooiing.beloning.behaald", titel(zt))
                : Component.translatable("vadsrijk.voltooiing.beloning.nog", titel(zt), munten(zt.getLongOr("beloning", 0)));
        for (FormattedCharSequence line : font.split(beloning, (int) (pw / 0.75f))) {
            klein(g, line, x, y, zt.getBooleanOr("beloond", false) ? GROEN : GOUD);
            y += 7;
        }
        y += 4;
        // the categories
        ListTag cl = cats();
        categorie = Math.min(categorie, Math.max(0, cl.size() - 1));
        for (int i = 0; i < cl.size() && y + RIJ <= onder; i++) {
            CompoundTag c = cl.getCompoundOrEmpty(i);
            int ck = c.getIntOr("k", 0), ct = c.getIntOr("t", 0);
            boolean sel = i == categorie, hover = binnen(mx, my, x - 2, y, pw + 4, RIJ), vol = ck >= ct;
            if (sel || hover) {
                g.fill(x - 2, y, x + pw + 2, y + RIJ, sel ? LINT_ZACHT : 0xFFFFEAF4);
            }
            if (sel) {
                g.fill(x - 2, y, x, y + RIJ, LINT);
            }
            g.pose().pushMatrix();
            g.pose().translate(x + 1, y + 1);
            g.pose().scale(0.6875f, 0.6875f);
            g.item(icoon(c.getStringOr("icoon", "")), 0, 0);
            g.pose().popMatrix();
            g.text(font, Component.translatable(c.getStringOr("naam", "")), x + 15, y + 3, vol ? GROEN : TEKST, false);
            String teller = ck + "/" + ct;
            int tx = x + pw - 34 - font.width(teller);
            g.text(font, teller, tx, y + 3, vol ? GROEN : ZACHT, false);
            balk(g, x + pw - 30, y + 5, 30, 3, ct == 0 ? 1 : ck / (float) ct, vol);
            final int idx = i;
            klikvlak(x - 2, y, pw + 4, RIJ, () -> {
                categorie = idx;
                scroll = 0;
            });
            y += RIJ;
        }
    }

    private void rechterPagina(GuiGraphicsExtractor g, int mx, int my, int x, int y, int pw, int onder) {
        ListTag cl = cats();
        if (cl.isEmpty()) {
            return;
        }
        CompoundTag c = cl.getCompoundOrEmpty(categorie);
        g.text(font, Component.translatable(c.getStringOr("naam", "")).withStyle(ChatFormatting.BOLD), x, y, TEKST, false);
        y += 11;
        g.fill(x, y, x + pw, y + 1, LINT_ZACHT);
        y += 4;
        ListTag dl = c.getListOrEmpty("d");
        int zichtbaar = Math.max(1, (onder - y) / REGEL), max = Math.max(0, dl.size() - zichtbaar);
        scroll = Math.max(0, Math.min(scroll, max));
        int lijstTop = y;
        for (int i = scroll; i < dl.size() && i < scroll + zichtbaar; i++) {
            CompoundTag d = dl.getCompoundOrEmpty(i);
            int s = d.getByteOr("s", (byte) 0);
            boolean klaar = s == 2, bezig = s == 1, geheim = d.getBooleanOr("g", false) && !klaar && !bezig;
            // the tick box
            g.fill(x, y + 1, x + 8, y + 9, klaar ? GROEN : bezig ? 0xFFE0A020 : 0xFFD8C8D2);
            g.fill(x + 1, y + 2, x + 7, y + 8, klaar ? 0xFF7AD89A : bezig ? 0xFFFFD86A : PAPIER);
            if (klaar) {
                g.text(font, "✔", x + 1, y + 1, 0xFF14562E, false);
            } else if (bezig) {
                g.fill(x + 3, y + 4, x + 5, y + 6, 0xFF9A6A00);
            }
            int tx = x + 12;
            Component naam = geheim ? Component.literal("???") : naam(d);
            int kleur = klaar ? 0xFF1F7A44 : bezig ? 0xFF8A6000 : geheim ? GRIJS : ZACHT;
            String tekst = font.plainSubstrByWidth(naam.getString(), pw - 12);
            g.text(font, tekst, tx, y + 1, kleur, false);
            // a hint after the name for what isn't done yet (secrets: only the hint)
            if (!klaar && d.contains("h")) {
                Component hint = hint(d);
                int over = pw - 12 - font.width(tekst) - 4;
                String ht = font.plainSubstrByWidth(hint.getString(), (int) (over / 0.75f));
                if (over > 20 && !ht.isEmpty()) {
                    klein(g, Component.literal(ht).withStyle(ChatFormatting.ITALIC).getVisualOrderText(), tx + font.width(tekst) + 4, y + 2, GRIJS);
                }
                List<Component> tip = new ArrayList<>();
                tip.add(naam.copy().withStyle(geheim ? ChatFormatting.GRAY : ChatFormatting.LIGHT_PURPLE));
                tip.add(hint.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
                if (bezig) {
                    tip.add(Component.translatable("vadsrijk.voltooiing.bezig").withStyle(ChatFormatting.GOLD));
                }
                tip(mx, my, x, y, pw, REGEL, tip);
            } else if (bezig) {
                tip(mx, my, x, y, pw, REGEL, List.of(naam, Component.translatable("vadsrijk.voltooiing.bezig").withStyle(ChatFormatting.GOLD)));
            }
            y += REGEL;
        }
        if (max > 0) {
            // a little scroll bar
            int top0 = lijstTop, sh = onder - lijstTop, bh = Math.max(8, sh * zichtbaar / dl.size()), by = top0 + (sh - bh) * scroll / max;
            g.fill(x + pw + 4, top0, x + pw + 6, onder, 0xFFF0D8E4);
            g.fill(x + pw + 4, by, x + pw + 6, by + bh, LINT);
        }
    }

    private void balk(GuiGraphicsExtractor g, int x, int y, int bw, int bh, float f, boolean vol) {
        g.fill(x, y, x + bw, y + bh, 0xFFF0D8E4);
        int v = Math.round(bw * Math.max(0, Math.min(1, f)));
        if (v > 0) {
            g.fill(x, y, x + v, y + bh, vol ? 0xFFF0B800 : LINT);
            g.fill(x, y, x + v, y + 1, 0x60FFFFFF);
        }
    }

    private void klein(GuiGraphicsExtractor g, FormattedCharSequence s, int x, int y, int kleur) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.75f, 0.75f);
        g.text(font, s, 0, 0, kleur, false);
        g.pose().popMatrix();
    }

    private static Component titel(CompoundTag zt) {
        return Component.translatable("gui.guhs.titels.naam." + zt.getStringOr("titel", "")).withStyle(ChatFormatting.BOLD);
    }

    private static String munten(long brons) {
        return nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.muntTekst(brons);
    }

    /** The name: key + optional arg; "key#n" = a lang key and a literal number (campfires: "Kampvuur: Sporenwoud 2"). */
    static MutableComponent naam(CompoundTag d) {
        return tekst(d.getStringOr("n", ""), d.getStringOr("na", ""));
    }

    static MutableComponent hint(CompoundTag d) {
        return tekst(d.getStringOr("h", ""), d.getStringOr("ha", ""));
    }

    private static MutableComponent tekst(String key, String arg) {
        if (arg.isEmpty()) {
            return Component.translatable(key);
        }
        int h = arg.indexOf('#');
        if (h >= 0) {
            return Component.translatable(key, Component.translatable(arg.substring(0, h)), arg.substring(h + 1));
        }
        return Component.translatable(key, Component.translatable(arg));
    }

    // ---- input -------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll += dy > 0 ? -2 : dy < 0 ? 2 : 0;
        if (scroll < 0) {
            scroll = 0;
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (VoltooiingClient.BOEK.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
