package nl.juiced.guhs.feature.safari.quests.client;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.kern.SafariSync;
import nl.juiced.guhs.feature.safari.quests.QuestTracker;

/**
 * Test round 4 (design ronde 49): the small quest tracker in the top left corner (test round 5: was top right, under the
 * effect icons) while you are in the safari park:
 * a pink title and one line per quest that is busy ({@link QuestTracker#regels}, from the synced safari state). It hides
 * while a screen is open, with F1, and when no quest is busy.
 */
final class QuestTrackerHud {
    private static final int MAX_BREED = 190, TITEL = 0xFFF7B6CB, TEKST = 0xFFF4F0E8, ACHTER = 0x70201018;

    static void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null || mc.level == null || mc.player == null || !Safari.echt(mc.level)) {
            return;
        }
        List<Component> regels = QuestTracker.regels(SafariSync.stand);
        if (regels.isEmpty()) {
            return;
        }
        Font font = mc.font;
        Component titel = Component.translatable("gui.guhs.safari.tracker.titel");
        int breed = font.width(titel);
        for (Component r : regels) {
            breed = Math.max(breed, Math.min(MAX_BREED, font.width(r)));
        }
        int x = 8, y = 6;
        int hoog = 12;
        for (Component r : regels) {
            hoog += font.split(r, MAX_BREED).size() * 10;
        }
        g.fill(x - 4, y - 3, x + breed + 4, y + hoog + 1, ACHTER);
        g.text(font, titel, x, y, TITEL, true);
        y += 12;
        for (Component r : regels) {
            for (var lijn : font.split(r, MAX_BREED)) {
                g.text(font, lijn, x, y, TEKST, true);
                y += 10;
            }
        }
    }

    private QuestTrackerHud() {
    }
}
