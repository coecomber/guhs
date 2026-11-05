package nl.juiced.guhs.feature.vadsrijk.rijdier.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;
import nl.juiced.guhs.feature.vadsrijk.economie.client.RijdierScherm;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.rijdier.RijdierExtraRegistry;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.As;

/**
 * T-RIJDIER, client: the "Aankleden" panel in the mount screen (N): three rows of swatches (Vacht for every mount but the Pluimguh,
 * whose colours stay under the preview; Patroon; Tuig with little item icons; "geen" = take it off). Clicking a swatch tries it on in
 * the turning preview; below: its name, whether it's yours or where it comes from (+ progress), and the button: "Aantrekken" /
 * "Kopen" (at the stal) / "Ruilen (n Gouden Veren)".
 */
public final class AankleedPaneel {
    private static final int VAK = 12, STAP = 14;
    public boolean open;
    @Nullable
    private As keuzeAs;
    private String keuzeId = "";

    /** Another mount was picked: forget the try-on. */
    public void wis() {
        keuzeAs = null;
        keuzeId = "";
    }

    /** What the preview wears: the mount's own look with the tried-on piece swapped in. */
    public String pasAan(RijdierSoort s, CompoundTag t) {
        String[] d = Uiterlijk.delen(t.getStringOr("uiterlijk", ""));
        if (open && keuzeAs != null) {
            d[keuzeAs.ordinal()] = keuzeId;
        }
        return Uiterlijk.maak(d[0], d[1], d[2]);
    }

    private static List<CompoundTag> looks(CompoundTag t, As as) {
        List<CompoundTag> l = new ArrayList<>();
        ListTag lt = t.getListOrEmpty("looks");
        for (int i = 0; i < lt.size(); i++) {
            CompoundTag c = lt.getCompoundOrEmpty(i);
            if (c.getStringOr("as", "").equals(as.id())) {
                l.add(c);
            }
        }
        return l;
    }

    /** "How to get it" for a look's source (with its numbers). */
    public static Component bronTekst(CompoundTag c, int veren) {
        String bron = c.getStringOr("bron", ""), tekst = c.getStringOr("tekst", "");
        Component t = switch (bron) {
            case "stal" -> Component.translatable(tekst, Spelers.munten(c.getLongOr("waarde", 0)));
            case "veren" -> Component.translatable(tekst, c.getLongOr("waarde", 0), veren);
            case "baas" -> Component.translatable(tekst, Component.translatable("entity." + c.getStringOr("sleutel", "").replace(':', '.')));
            default -> Component.translatable(tekst);
        };
        if (c.getIntOr("nodig", 0) > 0) {
            t = t.copy().append(Component.translatable("vadsrijk.rijdier.voortgang_kort", c.getIntOr("heb", 0), c.getIntOr("nodig", 0)));
        }
        return t;
    }

    public void teken(RijdierScherm sch, GuiGraphicsExtractor g, int mx, int my, RijdierSoort s, CompoundTag t, CompoundTag data, int x, int y, int bw,
            int onder) {
        Font font = sch.paneelFont();
        boolean stal = data.getBooleanOr("stal", false);
        int veren = data.getIntOr("veren", 0);
        String[] aan = Uiterlijk.delen(t.getStringOr("uiterlijk", ""));
        CompoundTag gekozen = null;
        for (As as : As.values()) {
            List<CompoundTag> l = looks(t, as);
            if (l.isEmpty()) {
                continue;
            }
            g.text(font, Component.translatable("vadsrijk.rijdier.as." + as.id()).withStyle(ChatFormatting.BOLD), x, y, EcoScherm.TEKST, false);
            y += 10;
            int kol = Math.max(1, bw / STAP), i = 0;
            if (as != As.VACHT) {   // "geen": take it off
                int sx = x;
                boolean sel = keuzeAs == as && keuzeId.isEmpty(), draagt = aan[as.ordinal()].isEmpty();
                g.fill(sx, y, sx + VAK, y + VAK, sel ? 0xFFFF2F8F : draagt ? 0xFF2E9E5A : 0xFFB890AC);
                g.fill(sx + 1, y + 1, sx + VAK - 1, y + VAK - 1, 0xFFFFF4FA);
                g.text(font, "×", sx + 3, y + 2, 0xFF9A7090, false);
                sch.paneelTip(mx, my, sx, y, VAK, VAK, List.of(Component.translatable("vadsrijk.rijdier.geen")));
                sch.paneelKlik(sx, y, VAK, VAK, () -> {
                    keuzeAs = as;
                    keuzeId = "";
                });
                i++;
            }
            for (CompoundTag c : l) {
                String id = c.getStringOr("id", "");
                Uiterlijk.Variant v = Uiterlijk.van(s, as, id);
                if (v == null) {
                    continue;
                }
                int sx = x + (i % kol) * STAP, sy = y + (i / kol) * STAP;
                boolean heeft = c.getBooleanOr("heeft", false);
                boolean draagt = as == As.VACHT ? (aan[0].isEmpty() ? id.equals(Uiterlijk.NATUREL) : aan[0].equals(id)) : aan[as.ordinal()].equals(id);
                boolean sel = keuzeAs == as && keuzeId.equals(id);
                if (sel) {
                    gekozen = c;
                }
                g.fill(sx, sy, sx + VAK, sy + VAK, sel ? 0xFFFF2F8F : draagt ? 0xFF2E9E5A : 0xFFB890AC);
                int kleur = heeft ? v.kleur() : (v.kleur() & 0x00FFFFFF) | 0x50000000;
                g.fill(sx + 1, sy + 1, sx + VAK - 1, sy + VAK - 1, as == As.TUIG ? (heeft ? 0xFFFFF4FA : 0xFFE6DCE4) : kleur);
                if (as == As.TUIG && RijdierExtraRegistry.TUIG.containsKey(id)) {
                    g.pose().pushMatrix();
                    g.pose().translate(sx + 1, sy + 1);
                    g.pose().scale(0.625f, 0.625f);
                    g.item(new ItemStack(RijdierExtraRegistry.TUIG.get(id).get()), 0, 0);
                    g.pose().popMatrix();
                } else if (as == As.PATROON) {
                    g.text(font, symbool(id), sx + 3, sy + 2, heeft ? 0xFF4A1E46 : 0xFF9A8094, false);
                }
                if (!heeft) {
                    g.fill(sx + VAK - 5, sy, sx + VAK, sy + 5, 0xC0FFFFFF);
                    g.text(font, "?", sx + VAK - 5, sy - 1, 0xFF6A4A64, false);
                }
                List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable(v.naamKey()).withStyle(ChatFormatting.BOLD));
                tip.add(heeft ? Component.translatable("vadsrijk.economie.rijdieren.van_jou").withStyle(ChatFormatting.GREEN)
                        : bronTekst(c, veren).copy().withStyle(ChatFormatting.GRAY));
                sch.paneelTip(mx, my, sx, sy, VAK, VAK, tip);
                sch.paneelKlik(sx, sy, VAK, VAK, () -> {
                    keuzeAs = as;
                    keuzeId = id;
                });
                i++;
            }
            y += ((i + kol - 1) / kol) * STAP + 3;
        }
        // ---- the picked look -------------------------------------------------------------------------------------------------------
        g.text(font, Component.translatable("vadsrijk.rijdier.veren", veren), x, onder - 34, EcoScherm.GOUD, false);
        if (keuzeAs == null) {
            for (var line : font.split(Component.translatable("vadsrijk.rijdier.paneel_uitleg"), bw)) {
                if (y > onder - 44) {
                    break;
                }
                g.text(font, line, x, y, EcoScherm.ZACHT, false);
                y += 9;
            }
            return;
        }
        final As as = keuzeAs;
        final String id = keuzeId;
        if (gekozen == null && !id.isEmpty()) {
            return;
        }
        if (gekozen != null) {
            Uiterlijk.Variant v = Uiterlijk.van(s, as, id);
            if (v != null) {
                g.text(font, Component.translatable(v.naamKey()).withStyle(ChatFormatting.BOLD), x, y, EcoScherm.TEKST, false);
                y += 10;
            }
            if (!gekozen.getBooleanOr("heeft", false)) {
                for (var line : font.split(bronTekst(gekozen, veren), bw)) {
                    if (y > onder - 44) {
                        break;
                    }
                    g.text(font, line, x, y, 0xFF9A3A7A, false);
                    y += 9;
                }
            }
        }
        boolean heeft = gekozen == null || gekozen.getBooleanOr("heeft", false);
        int by = onder - 22;
        CompoundTag a = new CompoundTag();
        a.putString("soort", s.id());
        a.putString("as", as.id());
        a.putString("id", id);
        if (heeft) {
            String[] d = Uiterlijk.delen(t.getStringOr("uiterlijk", ""));
            boolean draagt = as == As.VACHT ? (d[0].isEmpty() ? id.isEmpty() || id.equals(Uiterlijk.NATUREL) : d[0].equals(id)) : d[as.ordinal()].equals(id);
            sch.paneelKnop(g, mx, my, x, by, bw, 16, Component.translatable(draagt ? "vadsrijk.rijdier.draagt" : "vadsrijk.rijdier.aantrekken"), !draagt,
                    () -> sch.paneelDoe("kleed", a));
        } else {
            String bron = gekozen.getStringOr("bron", "");
            long waarde = gekozen.getLongOr("waarde", 0);
            if (bron.equals("stal")) {
                sch.paneelKnop(g, mx, my, x, by, bw, 16, Component.translatable("vadsrijk.economie.stal.koop", Spelers.munten(waarde)),
                        stal && data.getLongOr("munten", 0) >= waarde, () -> sch.paneelDoe("koop_look", a));
            } else if (bron.equals("veren")) {
                sch.paneelKnop(g, mx, my, x, by, bw, 16, Component.translatable("vadsrijk.rijdier.ruil_knop", waarde), veren >= waarde,
                        () -> sch.paneelDoe("ruil", a));
            }
        }
    }

    private static String symbool(String patroon) {
        return switch (patroon) {
            case "vlekjes" -> "•";
            case "streepjes" -> "≡";
            case "sterretjes" -> "★";
            case "hartjes" -> "♥";
            case "glitter" -> "✦";
            default -> "?";
        };
    }
}
