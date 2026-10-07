package nl.juiced.guhs.feature.baltoslee.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.juiced.guhs.feature.baltoslee.RitRoute;
import nl.juiced.guhs.feature.baltoslee.SleeBaan;
import nl.juiced.guhs.feature.baltoslee.SleeEntity;
import nl.juiced.guhs.feature.baltoslee.SleeRijden;
import nl.juiced.guhs.feature.baltoslee.SleeRit;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.quest.Highscores;

/**
 * The sled panel on the rider's screen: which ride, the route from the stable to the berghut and back (rest points, ice
 * bridges, avalanches, the dieptepunt; your sled, Steele-Mika's), the clock (the race time, or the time left to reach the
 * hospital), the dogs' warmth and speed; below the crosshair what's coming (an avalanche: which way to steer, a gust, the
 * ice bridge, a vuurkorf), and a soft snowy edge that thickens with the storm.
 */
public final class SleeHud {
    private static final int ACHTER = 0xB0142034, RAND = 0xFFBFE6FF, BALK = 0xFF2A3350;

    private SleeHud() {
    }

    static void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        SleeEntity sled = SleeEffecten.eigenSlee();
        if (mc.options.hideGui) {
            return;
        }
        int sw = g.guiWidth(), sh = g.guiHeight();
        if (SleeEffecten.wit > 0) {
            int a = (int) (Math.min(1, SleeEffecten.wit / 25f) * 210);
            g.fill(0, 0, sw, sh, (a << 24) | 0xF4F8FF);
        }
        if (sled == null || sled.route() == null) {
            return;
        }
        RitRoute r = sled.route();
        Font font = mc.font;
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        // the snowy edge of your view
        int rand = (int) (sled.storm() * 120);
        if (rand > 4) {
            for (int i = 0; i < 12; i++) {
                int a = (int) (rand * (1 - i / 12f)) / 2;
                int c = (a << 24) | 0xEEF4FF;
                int d = i * 6;
                g.fill(0, d, sw, d + 6, c);
                g.fill(0, sh - d - 6, sw, sh - d, c);
                g.fill(d, 0, d + 6, sh, c);
                g.fill(sw - d - 6, 0, sw - d, sh, c);
            }
        }

        int w = 236, x = (sw - w) / 2, y = 4;
        g.fill(x - 1, y - 1, x + w + 1, y + 57, RAND);   // (3.0 QA: 6 higher, so the berghut icon sits under the title)
        g.fill(x, y, x + w, y + 56, ACHTER);
        boolean sprint = sled.soort() != SleeEntity.TOCHT;
        Component titel = sprint ? Component.translatable("gui.guhs.baltoslee.hud.sprint", Niveau.of(sled.niveau()).naam())
                : Component.translatable(sled.kist() ? "gui.guhs.baltoslee.hud.tocht_kist" : "gui.guhs.baltoslee.hud.tocht");
        g.centeredText(font, titel, x + w / 2, y + 3, 0xFFE8F6FF);

        // the route: there (left half), the berghut, back (right half)
        int bx = x + 10, bl = w - 20, by = y + 22;
        g.fill(bx, by, bx + bl, by + 4, BALK);
        g.fill(bx, by, bx + bl / 2, by + 4, 0xFF3A4770);
        for (int been = 0; been < 2; been++) {
            SleeBaan b = r.baan(been);
            for (RitRoute.Zone z : r.zones(been)) {
                int zx0 = bx + (int) (plek(been, z.s0(), b) * bl), zx1 = Math.max(zx0 + 2, bx + (int) (plek(been, z.s1(), b) * bl));
                int kleur = switch (z.soort()) {
                    case RUST -> 0xFFFFA040;
                    case IJSBRUG -> 0xFF9FE8FF;
                    case LAWINE -> 0xFFFFFFFF;
                    case DIEPTEPUNT -> 0xFF9090B0;
                };
                if (z.soort() == RitRoute.Soort.RUST) {
                    int m = (zx0 + zx1) / 2;
                    g.fill(m - 1, by - 2, m + 2, by + 6, kleur);
                } else if (z.soort() == RitRoute.Soort.DIEPTEPUNT) {
                    if (sprint) {
                        continue;
                    }
                    g.fill(zx0 - 1, by - 1, zx0 + 2, by + 5, kleur);
                } else {
                    g.fill(zx0, by + 1, zx1, by + 3, kleur);
                }
            }
        }
        g.fill(bx + bl / 2 - 2, by - 3, bx + bl / 2 + 2, by + 7, 0xFFB07A48);          // the berghut
        g.text(font, "⌂", bx + bl / 2 - 3, by - 11, 0xFFE0C090, true);
        if (sled.tegen() >= 0) {
            int st = bx + (int) (Mth.clamp(sled.tegen() / 2f, 0, 1) * bl);
            g.fill(st - 2, by - 2, st + 2, by + 6, 0xFFC080FF);
        }
        int me = bx + (int) (plek(sled.been, sled.s, r.baan(sled.been)) * bl);
        g.fill(me - 2, by - 3, me + 3, by + 7, 0xFFFFE070);

        // the clock, the warmth, the speed
        int ly = y + 32;
        int tijd = sled.tijd();
        Component klok;
        if (sprint) {
            klok = Component.translatable("gui.guhs.baltoslee.hud.tijd", Highscores.tijd(Math.max(0, tijd)));
        } else if (tijd >= 0) {
            klok = Component.translatable("gui.guhs.baltoslee.hud.nog", Highscores.tijd(tijd).replaceAll("\\.\\d$", ""));
        } else {
            klok = Component.translatable("gui.guhs.baltoslee.hud.heen");
        }
        int klokKleur = !sprint && tijd >= 0 && tijd < 20 * 30 ? (now / 5 % 2 == 0 ? 0xFFFF7070 : 0xFFFFFFFF) : 0xFFFFFFFF;
        g.text(font, klok, x + 8, ly, klokKleur, true);
        float warmte = sled.warmte();
        int wbl = 60, wbx = x + w - 8 - wbl;
        Component wl = Component.translatable("gui.guhs.baltoslee.hud.warmte");
        g.text(font, wl, wbx - 4 - font.width(wl), ly, 0xFFFFD8A0, true);
        g.fill(wbx, ly + 1, wbx + wbl, ly + 7, BALK);
        int wk = warmte < SleeRijden.WARMTE_KOUD ? (now / 5 % 2 == 0 ? 0xFF7FB8FF : 0xFFCFE4FF)
                : warmte < SleeRijden.WARMTE_WAARSCHUWING ? 0xFFFFC060 : 0xFFFF8A3A;   // (1.3.1: blinks blue when the dogs are slow)
        g.fill(wbx, ly + 1, wbx + (int) (wbl * warmte / 100f), ly + 7, wk);
        int vy = y + 44;
        g.text(font, Component.translatable("gui.guhs.baltoslee.hud.vaart"), x + 8, vy, 0xFFCFE8FF, true);
        int vx = x + 8 + font.width(Component.translatable("gui.guhs.baltoslee.hud.vaart")) + 4;
        int pootjes = (int) Math.round(Mth.clamp(sled.v / SleeRijden.TOP, 0, 1.2) * 8);
        for (int i = 0; i < 8; i++) {
            g.text(font, "•", vx + i * 7, vy, i < pootjes ? 0xFFFFF0A0 : 0xFF45507A, false);
        }
        if (!sprint && sled.kist()) {
            g.text(font, Component.translatable("gui.guhs.baltoslee.hud.kist"), x + w - 8 - font.width(Component.translatable("gui.guhs.baltoslee.hud.kist")),
                    vy, 0xFFFFB0D0, true);
        } else if (sprint && sled.tegen() >= 0) {
            Component st = Component.translatable("gui.guhs.baltoslee.hud.steele");
            g.text(font, st, x + w - 8 - font.width(st), vy, 0xFFD8A8FF, true);
        }

        // what's coming, under the crosshair
        Component waarschuwing = null;
        int kleur = 0xFFFFFFFF;
        boolean knipper = now / 4 % 2 == 0;
        SleeBaan b = r.baan(sled.been);
        double s = sled.s;
        switch (sled.fase()) {
            case SleeEntity.VAST -> {
                waarschuwing = Component.translatable("gui.guhs.baltoslee.hud.vast");
                kleur = 0xFFE0F0FF;
            }
            case SleeEntity.PAUZE -> {
                String key = switch (sled.pauze()) {
                    case SleeEntity.BERGHUT -> "gui.guhs.baltoslee.hud.berghut";
                    case SleeEntity.DIEPTEPUNT -> "gui.guhs.baltoslee.hud.dieptepunt";
                    default -> "gui.guhs.baltoslee.hud.rust";
                };
                waarschuwing = Component.translatable(key, Math.max(0, sled.wacht() / 20));
                kleur = sled.pauze() == SleeEntity.RUST ? 0xFFFFC070 : 0xFFE8E8FF;
            }
            case SleeEntity.RIJDT -> {
                RitRoute.Zone lawine = null;
                for (RitRoute.Zone z : r.zones(sled.been, RitRoute.Soort.LAWINE)) {
                    if (z.midden() - s < 22 && z.midden() - s > -1) {
                        lawine = z;
                    }
                }
                int vlaag = SleeRijden.windKomt(sled.seed(), sled.been, s, b.lengte);
                if (lawine != null) {
                    double w0 = b.breedte(lawine.midden());
                    boolean veilig = sled.lat * -lawine.kant() >= SleeRit.LAWINE_VEILIG * w0;
                    if (veilig) {
                        waarschuwing = Component.translatable("gui.guhs.baltoslee.hud.lawine_veilig");
                        kleur = 0xFF80FF90;
                    } else {
                        waarschuwing = Component.translatable(lawine.kant() < 0 ? "gui.guhs.baltoslee.hud.lawine_rechts" : "gui.guhs.baltoslee.hud.lawine_links");
                        kleur = knipper ? 0xFFFF6060 : 0xFFFFFFFF;
                    }
                } else if (r.zone(sled.been, RitRoute.Soort.IJSBRUG, s) != null) {
                    waarschuwing = Component.translatable("gui.guhs.baltoslee.hud.ijsbrug");
                    kleur = 0xFFA8ECFF;
                } else if (vlaag != 0) {
                    waarschuwing = Component.translatable(vlaag > 0 ? "gui.guhs.baltoslee.hud.vlaag_links" : "gui.guhs.baltoslee.hud.vlaag_rechts");
                    kleur = 0xFFD8E8FF;
                } else if (r.zone(sled.been, RitRoute.Soort.RUST, s) != null && warmte < 90) {   // (a used rest point shows it too: harmless)
                    waarschuwing = Component.translatable("gui.guhs.baltoslee.hud.vuurkorf");
                    kleur = 0xFFFFB060;
                } else if (Math.abs(sled.lat) > b.breedte(s) + 0.05) {
                    waarschuwing = Component.translatable("gui.guhs.baltoslee.hud.diep");
                    kleur = 0xFFD0E0FF;
                } else if (warmte < SleeRijden.WARMTE_KOUD) {
                    waarschuwing = Component.translatable("gui.guhs.baltoslee.hud.koud");
                    kleur = knipper ? 0xFF9FC8FF : 0xFFFFFFFF;
                } else if (warmte < SleeRijden.WARMTE_WAARSCHUWING) {   // 1.3.1: the paws are getting cold
                    waarschuwing = Component.translatable("gui.guhs.baltoslee.hud.koud_bijna");
                    kleur = 0xFFFFC870;
                }
            }
            default -> {
            }
        }
        if (waarschuwing != null) {
            g.centeredText(font, waarschuwing, sw / 2, sh / 2 + 26, kleur);
        }
        if (sled.tickCount < 260 || sled.fase() == SleeEntity.WACHT) {
            g.centeredText(font, Component.translatable("gui.guhs.baltoslee.hud.toetsen"), sw / 2, sh - 62, 0xFFDDE8FF);
        }
    }

    /** Where s on leg been is on the panel's route line (0 = the stable, 0.5 = the berghut, 1 = back). */
    private static float plek(int been, double s, SleeBaan b) {
        float f = (float) Mth.clamp(s / Math.max(1, b.lengte), 0, 1) * 0.5f;
        return been == 0 ? f : 0.5f + f;
    }
}
