package nl.juiced.guhs.feature.guhrio.client;

import java.util.Set;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.juiced.guhs.feature.guhrio.GuhrioFeature;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;

/**
 * The panel of a Super Guhrio level, a bar where the hotbar normally is: who (GUHRIO and your power-up), your coins of
 * this run, which level, the time. Also the black of going through a pipe, the flash of coming back to your flag,
 * and the banner at the flagpole. The game's own bars (hotbar, hearts, food, the crosshair...) and other mods' panels are
 * hidden in a level ({@link #verborgen}): nothing there can hurt you and your hands do nothing.
 */
public final class GuhrioHud {
    /** The layers a level keeps (and everything of Guhs itself); every other layer, of the game or of another mod, is hidden. */
    private static final Set<Identifier> BLIJFT = Set.of(VanillaGuiLayers.CAMERA_OVERLAYS, VanillaGuiLayers.AFTER_CAMERA_DECORATIONS,
            VanillaGuiLayers.BOSS_OVERLAY, VanillaGuiLayers.SLEEP_OVERLAY, VanillaGuiLayers.DEMO_OVERLAY, VanillaGuiLayers.OVERLAY_MESSAGE,
            VanillaGuiLayers.TITLE, VanillaGuiLayers.CHAT, VanillaGuiLayers.TAB_LIST, VanillaGuiLayers.SUBTITLE_OVERLAY);

    /**
     * Is this layer hidden in a level? The hotbar, hearts, food, crosshair... and the panels of other mods (a minimap, a
     * "what am I looking at" box, which would name whatever happens to be further along the lane): a level is its own
     * little game with its own screen.
     */
    static boolean verborgen(Identifier laag) {
        // (of Guhs itself only this panel and the cutscene's bars stay: no vadskracht readout, no objective line in a level)
        return !BLIJFT.contains(laag) && !laag.equals(LAAG) && !laag.equals(nl.juiced.guhs.feature.verhaal.client.VerhaalClient.LAAG_CUTSCENE);
    }

    /** This panel's own layer. */
    static final Identifier LAAG = nl.juiced.guhs.Guhs.id("guhrio_hud");

    private GuhrioHud() {
    }

    static void teken(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!GuhrioClient.speelt()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        // the black of a pipe, the flash of coming back
        float donker = Math.max(BaanBesturing.pijpDonker(), GuhrioClient.flits / 12f);
        if (donker > 0.01f) {
            g.fill(0, 0, w, h, ((int) (Math.min(1f, donker) * 255) << 24));
        }
        if (mc.options.hideGui || (mc.player != null && nl.juiced.guhs.feature.verhaal.Cutscenes.bezig(mc.player))) {
            return;
        }
        Font font = mc.font;
        // a bar where the hotbar was (hidden in a level): the one place no other mod draws its own panel
        int breed = 288, kolom = breed / 4, links = w / 2 - breed / 2, y = h - 26;
        g.fill(links - 4, y - 4, links + breed + 4, h - 2, 0x90101018);
        g.fill(links - 4, y - 5, links + breed + 4, y - 4, 0xFFFFD24A);
        // GUHRIO + power-up
        kop(g, font, Component.translatable("gui.guhs.guhrio.hud.naam"), links + kolom / 2, y);
        boolean vuur = GuhrioClient.kracht == GuhrioSpel.Kracht.VUUR.ordinal(), groot = GuhrioClient.kracht != GuhrioSpel.Kracht.GEEN.ordinal();
        Component kracht = Component.translatable(GuhrioClient.guhshi != 0 ? "gui.guhs.guhrio.hud.guhshi"
                : vuur ? "gui.guhs.guhrio.hud.vuur" : groot ? "gui.guhs.guhrio.hud.super" : "gui.guhs.guhrio.hud.klein");
        g.centeredText(font, kracht, links + kolom / 2, y + 11, vuur ? 0xFFFF8A4A : 0xFFFFFFFF);
        // the three big vadsmunten of this level, above the bar
        for (int i = 0; i < 3; i++) {
            g.pose().pushMatrix();
            g.pose().translate(w / 2f - 27 + i * 18, y - 22);
            g.item(new ItemStack((GuhrioClient.vads >> i & 1) != 0 ? GuhrioFeature.VADSMUNT.get().asItem() : GuhrioFeature.VADSMUNT_SCHIM.get()), 0, 0);
            g.pose().popMatrix();
        }
        // coins
        kop(g, font, Component.translatable("gui.guhs.guhrio.hud.munten"), links + kolom + kolom / 2, y);
        String munten = "x" + (GuhrioClient.munten < 10 ? "0" : "") + GuhrioClient.munten;
        int mx = links + kolom + kolom / 2 - (font.width(munten) + 12) / 2;
        g.pose().pushMatrix();
        g.pose().translate(mx - 2, y + 8);
        g.pose().scale(0.75f, 0.75f);
        g.item(new ItemStack(GuhrioFeature.MUNT.get()), 0, 0);
        g.pose().popMatrix();
        g.text(font, munten, mx + 12, y + 11, 0xFFFFFFFF, true);
        // which level
        kop(g, font, Component.translatable("gui.guhs.guhrio.hud.wereld"), links + 2 * kolom + kolom / 2, y);
        g.centeredText(font, GuhrioClient.wereld(), links + 2 * kolom + kolom / 2, y + 11, 0xFFFFFFFF);
        // the time
        kop(g, font, Component.translatable("gui.guhs.guhrio.hud.tijd"), links + 3 * kolom + kolom / 2, y);
        g.centeredText(font, GuhrioSpel.tijd(GuhrioClient.klaar > 0 ? GuhrioClient.klaarTijd : GuhrioClient.tijd), links + 3 * kolom + kolom / 2, y + 11, 0xFFFFFFFF);
        // done!
        if (GuhrioClient.klaar > 0) {
            Component klaar = Component.translatable("gui.guhs.guhrio.hud.klaar");
            g.pose().pushMatrix();
            g.pose().translate(w / 2f, h / 3f);
            g.pose().scale(2f, 2f);
            g.centeredText(font, klaar, 0, 0, 0xFFFFE14D);
            g.pose().popMatrix();
        }
    }

    private static void kop(GuiGraphicsExtractor g, Font font, Component tekst, int x, int y) {
        g.centeredText(font, tekst, x, y, 0xFFFFD24A);
    }
}
