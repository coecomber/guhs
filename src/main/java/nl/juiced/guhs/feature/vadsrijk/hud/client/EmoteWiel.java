package nl.juiced.guhs.feature.vadsrijk.hud.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.hud.Emote;
import nl.juiced.guhs.feature.vadsrijk.hud.UHudPayloads;

/**
 * U-HUD: the emote wheel. Hold the emote key (default {@code ;}), point at an emote, let go = play it. A quick tap keeps the
 * wheel open: then click one. Esc / right click = close without an emote. The middle shows the hovered emote's name.
 */
public final class EmoteWiel extends Screen {
    private static final Emote[] RAD = Emote.values();
    private int open;
    private boolean vast, los;
    private int gekozen = -1;

    EmoteWiel() {
        super(Component.translatable("vadsrijk.uhud.emote.wiel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int straal() {
        return Math.max(40, Math.min(72, height / 2 - 34));
    }

    @Override
    public void tick() {
        open++;
        // the key may have been let go before this screen saw it
        if (!vast && !los && open > 1 && minecraft != null && !UHudClient.EMOTES.isUnbound()
                && UHudClient.EMOTES.getKey().getType() == InputConstants.Type.KEYSYM
                && !InputConstants.isKeyDown(minecraft.getWindow(), UHudClient.EMOTES.getKey().getValue())) {
            loslaten();
        }
    }

    private void loslaten() {
        if (los) {
            return;
        }
        los = true;
        if (gekozen >= 0) {
            kies(gekozen);
        } else if (open < 6) {
            vast = true;   // (a quick tap: stay open, click an emote)
        } else {
            onClose();
        }
    }

    private void kies(int i) {
        ClientPacketDistributor.sendToServer(new UHudPayloads.EmoteVraag(RAD[i].id));
        onClose();
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (UHudClient.EMOTES.matches(event)) {
            loslaten();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            onClose();
            return true;
        }
        int i = onder(event.x(), event.y());
        if (i >= 0) {
            kies(i);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** The slot under the mouse (by direction from the middle), or -1 in the middle. */
    private int onder(double mx, double my) {
        double dx = mx - width / 2.0, dy = my - height / 2.0;
        if (dx * dx + dy * dy < 16 * 16) {
            return -1;
        }
        double hoek = Math.toDegrees(Math.atan2(dx, -dy));   // 0 = up, clockwise
        double stap = 360.0 / RAD.length;
        return Math.floorMod((int) Math.round(hoek / stap), RAD.length);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, height, 0x30000000);   // (no blur: you see yourself / the world)
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        gekozen = onder(mouseX, mouseY);
        int cx = width / 2, cy = height / 2, r = straal();
        // the middle
        g.fill(cx - 30, cy - 9, cx + 30, cy + 9, 0xC0201418);
        Component mid = gekozen >= 0 ? Component.translatable(RAD[gekozen].naamKey()) : Component.translatable("vadsrijk.uhud.emote.kies");
        g.centeredText(font, mid, cx, cy - 4, gekozen >= 0 ? 0xFFFFE27A : 0xFFBFAFB8);
        for (int i = 0; i < RAD.length; i++) {
            double h = Math.toRadians(i * 360.0 / RAD.length);
            int x = cx + (int) Math.round(Math.sin(h) * r), y = cy - (int) Math.round(Math.cos(h) * r);
            boolean aan = i == gekozen;
            int z = aan ? 15 : 13;
            g.fill(x - z - 1, y - z - 1, x + z + 1, y + z + 1, aan ? 0xFFFFD24A : 0xFF6A3A5A);
            g.fill(x - z, y - z, x + z, y + z, aan ? 0xF0402A38 : 0xE0281A24);
            g.pose().pushMatrix();
            g.pose().translate(x, y - 5);
            float s = aan ? 1.8f : 1.5f;
            g.pose().scale(s, s);
            g.centeredText(font, Component.literal(RAD[i].teken), 0, -4, 0xFFFFC2E8);
            g.pose().popMatrix();
            String n = Component.translatable(RAD[i].naamKey()).getString();
            g.pose().pushMatrix();
            g.pose().translate(x, y + 5);
            g.pose().scale(0.6f, 0.6f);
            g.centeredText(font, Component.literal(n), 0, 0, aan ? 0xFFFFFFFF : 0xFFD8C8D0);
            g.pose().popMatrix();
        }
        // a line from the middle towards the mouse
        double dx = mouseX - cx, dy = mouseY - cy, l = Math.sqrt(dx * dx + dy * dy);
        if (l > 16) {
            for (int k = 12; k < Math.min(l, r - 16); k += 3) {
                int px = cx + (int) (dx / l * k), py = cy + (int) (dy / l * k);
                g.fill(px, py, px + 1, py + 1, 0xA0FFD24A);
            }
        }
        if (vast) {
            g.centeredText(font, Component.translatable("vadsrijk.uhud.emote.klik"), cx, Mth.clamp(cy + r + 20, 0, height - 10), 0xFFBFAFB8);
        }
    }
}
