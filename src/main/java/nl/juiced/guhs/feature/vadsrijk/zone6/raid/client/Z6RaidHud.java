package nl.juiced.guhs.feature.vadsrijk.zone6.raid.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;
import nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidPayloads;

/**
 * U-Z6RAID: the raid HUD (only while you are in a raid group): the raid frames for up to 10 players (name, class colour, Vads bar,
 * flauw / weg), the floor + boss, the fight timer counting to the enrage, the wipe counter and the total time; on floor 3 the
 * three constellations' bars + the steek-aan-venster.
 * <p>Drawn through {@code RijkHud.voegToe("z6raid", ...)} on the right edge. When u-hud's editor is merged, the spot "z6raid" can
 * be added to its {@code RijkHud.standaardPlek} (then it is movable); u-hud's party frames (max 4 others) stay as they are, the raid
 * frames are their 10-player big brother and only show inside the tower.
 */
public final class Z6RaidHud {
    @Nullable
    private static CompoundTag staat;
    private static long ontvangen;

    private Z6RaidHud() {
    }

    static void init() {
        Z6RaidPayloads.ontvanger = t -> {
            staat = t;
            ontvangen = System.currentTimeMillis();
        };
        RijkHud.voegToe("z6raid", 140, Z6RaidHud::teken);
    }

    private static int kleur(String klasse) {
        return switch (klasse) {
            case "knuffelridder" -> 0xFFFF8AC8;
            case "pluisjager" -> 0xFF8AE07A;
            case "vadsmagier" -> 0xFF8AC8FF;
            case "nep" -> 0xFF9A9AA6;
            default -> 0xFFE0D0FF;
        };
    }

    private static String tijd(int s) {
        if (s < 0) {
            return "-";
        }
        return String.format("%d:%02d", s / 60, s % 60);
    }

    static void teken(GuiGraphicsExtractor g, Font f, int w, int h, float pt) {
        CompoundTag t = staat;
        if (t == null || System.currentTimeMillis() - ontvangen > 2500 || Minecraft.getInstance().options.hideGui) {
            return;
        }
        int breed = 118, x = w - breed - 4, y = 58;
        int verd = t.getIntOr("verdieping", 1);
        ListTag leden = t.getListOrEmpty("leden");
        ListTag extra = t.getListOrEmpty("extra");
        int hoog = 34 + leden.size() * 11 + (extra.isEmpty() ? 0 : 4 + extra.size() * 9);
        g.fill(x - 3, y - 3, x + breed + 3, y + hoog, 0xA0180E1C);
        g.fill(x - 3, y - 3, x + breed + 3, y - 2, 0xFFC080FF);
        Component kop = verd > 4 ? Component.translatable("vadsrijk.z6raid.hud.klaar")
                : Component.translatable("vadsrijk.z6raid.hud.verdieping", verd, Component.translatable(t.getStringOr("baas", "")));
        g.text(f, kop, x, y, 0xFFFFE27A, true);
        int gevecht = t.getIntOr("gevecht", -1), enrage = t.getIntOr("enrage", 0);
        String klok = gevecht < 0 ? Component.translatable("vadsrijk.z6raid.hud.rust").getString()
                : tijd(gevecht) + (enrage > 0 ? " / " + tijd(enrage) : "");
        int kleurKlok = gevecht >= 0 && enrage > 0 && gevecht >= enrage ? 0xFFFF5A5A : gevecht >= 0 && enrage > 0 && enrage - gevecht < 60 ? 0xFFFFB04A : 0xFFE0E0E0;
        g.text(f, Component.translatable("vadsrijk.z6raid.hud.klok", klok), x, y + 10, kleurKlok, true);
        g.text(f, Component.translatable("vadsrijk.z6raid.hud.wipes", t.getIntOr("wipes", 0), tijd(t.getIntOr("totaal", 0)), t.getIntOr("n", 2)), x, y + 20,
                0xFFC8B8E0, true);
        int ry = y + 32;
        for (Tag tag : leden) {
            if (!(tag instanceof CompoundTag l)) {
                continue;
            }
            boolean weg = l.getBooleanOr("weg", false), flauw = l.getBooleanOr("flauw", false), binnen = l.getBooleanOr("binnen", true);
            float vads = l.getFloatOr("vads", 0), max = Math.max(1, l.getFloatOr("max", 1));
            int k = kleur(l.getStringOr("klasse", ""));
            g.fill(x, ry, x + breed, ry + 10, 0xFF2A1A2E);
            if (!weg) {
                int vol = (int) ((breed - 2) * Math.max(0, Math.min(1, vads / max)));
                g.fill(x + 1, ry + 1, x + 1 + vol, ry + 9, flauw ? 0xFF5A4A5A : (k & 0x00FFFFFF) | 0xC0000000);
            }
            g.fill(x, ry, x + 2, ry + 10, k);
            String naam = l.getStringOr("naam", "?");
            if (f.width(naam) > breed - 40) {
                naam = f.plainSubstrByWidth(naam, breed - 44) + "..";
            }
            g.text(f, Component.literal(naam), x + 4, ry + 1, weg || !binnen ? 0xFF8A8A8A : 0xFFFFFFFF, true);
            String rechts = weg ? Component.translatable("vadsrijk.z6raid.hud.weg").getString()
                    : flauw ? Component.translatable("vadsrijk.z6raid.hud.flauw").getString() : !binnen ? Component.translatable("vadsrijk.z6raid.hud.buiten").getString()
                            : (int) (100 * vads / max) + "%";
            g.text(f, Component.literal(rechts), x + breed - 2 - f.width(rechts), ry + 1, flauw ? 0xFFFF8A8A : 0xFFE0E0E0, true);
            ry += 11;
        }
        if (!extra.isEmpty()) {
            ry += 4;
            for (Tag tag : extra) {
                if (!(tag instanceof CompoundTag e)) {
                    continue;
                }
                float fr = e.getFloatOr("f", 0);
                boolean uit = e.getBooleanOr("u", false);
                g.fill(x, ry, x + breed, ry + 8, 0xFF1A1A2A);
                g.fill(x + 1, ry + 1, x + 1 + (int) ((breed - 2) * Math.max(0, Math.min(1, fr))), ry + 7, uit ? 0xFFFFE27A : 0xFF8A9AC8);
                g.pose().pushMatrix();
                g.pose().translate(x + 3, ry + 1);
                g.pose().scale(0.75f, 0.75f);
                g.text(f, Component.translatable(e.getStringOr("k", "")).append(uit ? " ✦" : ""), 0, 0, 0xFFFFFFFF, true);
                g.pose().popMatrix();
                ry += 9;
            }
        }
    }
}
