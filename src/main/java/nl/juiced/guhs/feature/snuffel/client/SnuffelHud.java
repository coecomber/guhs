package nl.juiced.guhs.feature.snuffel.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.juiced.guhs.feature.snuffel.GeurSoort;
import nl.juiced.guhs.feature.snuffel.SnuffelPayloads;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * What a dog sees on its screen (GUI layer {@code guhs:snuffel_hud}):
 * <ul>
 *   <li>the SCENT METER while the nose is on the ground: a row of blocks above the hotbar that fills up the closer the
 *   scent is and swings harder the stronger it gets, in the colour of the kind of scent (orange food, blue a thing, green
 *   an animal, purple something strange; grey: nothing in the air), with the kind's name, and "Graaf hier!" on the spot;</li>
 *   <li>the keys, for a while after you became a dog;</li>
 *   <li>the exam's progress while one runs.</li>
 * </ul>
 */
public final class SnuffelHud {
    private static final int VAKJES = 16, ACHTER = 0xB01A1410, RAND = 0xFFE8D9B5, UIT = 0xFF3A322C;
    /** How long the keys stay on the screen after turning into a dog (ticks). */
    private static final int UITLEG_TICKS = 20 * 20;

    private static SnuffelPayloads.Meter meter = SnuffelPayloads.Meter.NIETS;
    private static long meterTick = -1000, hondSinds = -1;
    private static float getoond;

    private SnuffelHud() {
    }

    static void meter(SnuffelPayloads.Meter m) {
        meter = m;
        meterTick = SnuffelClient.ticks();
    }

    static void tick() {
        boolean hond = HondClient.eigenHond();
        if (hond && hondSinds < 0) {
            hondSinds = SnuffelClient.ticks();
        } else if (!hond) {
            hondSinds = -1;
            getoond = 0;
        }
        // (the needle follows the nose a little late, like a real meter)
        float doel = snuffelt() ? meter.sterkte() : 0f;
        getoond += (doel - getoond) * 0.25f;
    }

    private static boolean snuffelt() {
        return SnuffelKeys.SNUFFEL.isDown() && SnuffelClient.ticks() - meterTick < 20;
    }

    static void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || !HondClient.eigenHond() || Cutscenes.bezig(mc.player)) {
            return;
        }
        Font font = mc.font;
        int sw = g.guiWidth(), sh = g.guiHeight();
        float tijd = SnuffelClient.ticks() + delta.getGameTimeDeltaPartialTick(false);
        // --- the scent meter ---
        if (SnuffelKeys.SNUFFEL.isDown()) {
            GeurSoort soort = snuffelt() ? GeurSoort.vanNummer(meter.soort()) : null;
            int kleur = soort == null ? 0xFF9A9088 : soort.kleur();
            int w = VAKJES * 7 + 5, x = (sw - w) / 2, y = sh - 66;
            g.fill(x - 1, y - 1, x + w + 1, y + 13, RAND);
            g.fill(x, y, x + w, y + 12, ACHTER);
            float zwaai = soort == null ? 0f : Mth.sin(tijd * (0.35f + getoond * 0.5f)) * (0.02f + 0.07f * getoond);
            int aan = soort == null ? 0 : Mth.clamp(Math.round((getoond + zwaai) * VAKJES), meter.plek() ? VAKJES : 1, VAKJES);
            for (int i = 0; i < VAKJES; i++) {
                int vx = x + 3 + i * 7;
                int hoog = 4 + i * 4 / VAKJES;   // (the blocks grow a little towards "close")
                g.fill(vx, y + 10 - hoog, vx + 5, y + 10, i < aan ? kleur : UIT);
            }
            Component tekst;
            if (soort == null) {
                tekst = Component.translatable("gui.guhs.snuffel.meter.niets");
            } else if (meter.plek()) {
                tekst = Component.translatable(meter.graven() ? "gui.guhs.snuffel.meter.graaf" : "gui.guhs.snuffel.meter.dichtbij", soort.naam());
            } else {
                tekst = Component.translatable("gui.guhs.snuffel.meter.ruikt", soort.naam());
            }
            int tekstKleur = soort != null && meter.plek() && (int) (tijd / 5) % 2 == 0 ? 0xFFFFFFFF : kleur;
            g.centeredText(font, tekst, sw / 2, y - 11, tekstKleur);
        }
        // --- the keys, for a while ---
        if (hondSinds >= 0 && SnuffelClient.ticks() - hondSinds < UITLEG_TICKS && mc.screen == null) {
            Component[] regels = {
                    Component.translatable("gui.guhs.snuffel.toets.snuffel", SnuffelKeys.SNUFFEL.getTranslatedKeyMessage()),
                    Component.translatable("gui.guhs.snuffel.toets.graaf", mc.options.keyAttack.getTranslatedKeyMessage()),
                    Component.translatable("gui.guhs.snuffel.toets.zit", SnuffelKeys.ZIT.getTranslatedKeyMessage(), SnuffelKeys.KWISPEL.getTranslatedKeyMessage()),
                    Component.translatable("gui.guhs.snuffel.toets.blaf", SnuffelKeys.BLAF.getTranslatedKeyMessage()),
                    Component.translatable("gui.guhs.snuffel.toets.boekje", SnuffelKeys.BOEKJE.getTranslatedKeyMessage()),
                    Component.translatable("gui.guhs.snuffel.toets.kaart")};
            int breed = 0;
            for (Component c : regels) {
                breed = Math.max(breed, font.width(c));
            }
            int x = 6, y = sh / 2 - regels.length * 5;
            g.fill(x - 3, y - 3, x + breed + 3, y + regels.length * 10 + 1, ACHTER);
            for (int i = 0; i < regels.length; i++) {
                g.text(font, regels[i], x, y + i * 10, 0xFFF3E4C4, false);
            }
        }
        // --- the exam ---
        int[] examen = EigenStand.examen();
        if (examen != null) {
            Component tekst = Component.translatable("gui.guhs.snuffel.examen.hud", Component.translatable("gui.guhs.snuffel.examen." + EigenStand.examenId()),
                    examen[0], examen[1]);
            int w = font.width(tekst) + 10, x = (sw - w) / 2;
            g.fill(x, 4, x + w, 18, ACHTER);
            g.centeredText(font, tekst, sw / 2, 7, 0xFFFFD86B);
        }
    }

    /** (For the snuffelboekje) the keys as one line. */
    static Component toetsen() {
        return Component.translatable("gui.guhs.snuffel.toets.kort", SnuffelKeys.SNUFFEL.getTranslatedKeyMessage(),
                Minecraft.getInstance().options.keyAttack.getTranslatedKeyMessage(), SnuffelKeys.ZIT.getTranslatedKeyMessage(),
                SnuffelKeys.KWISPEL.getTranslatedKeyMessage(), SnuffelKeys.BLAF.getTranslatedKeyMessage());
    }
}
