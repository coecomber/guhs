package nl.juiced.guhs.feature.snuffel.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.snuffel.HondEntity;
import nl.juiced.guhs.feature.snuffel.Hondvorm;
import nl.juiced.guhs.feature.snuffel.Rang;
import nl.juiced.guhs.feature.snuffel.SnuffelPayloads;
import org.lwjgl.glfw.GLFW;

/**
 * The little window of the Guhstation (not fullscreen: the room stays visible around it): a grey-black console with the
 * guh snoet as its logo and a WHITE field. On the white, under our own title "HET SNUFFEL EILAND", your dog and your
 * little brother or sister run in from the sides and play: they greet each other wagging, the puppy dashes off and back,
 * your dog barks, sniffs, the puppy sits. Under them "Druk op start" blinks (click it, or Enter / Space): off to the last
 * spot you stood on the island. Below the white: your sniffing rank and your number of scents, small; and the button "Nee
 * ik wil even niet snuffelen, njeg" that closes the window.
 */
public final class GuhstationScherm extends Screen {
    private static final int W = 244, H = 196, VELD_W = 220, VELD_H = 112;
    private static final int KAST = 0xFF2A2B33, KAST_LICHT = 0xFF4A4C58, KAST_DONKER = 0xFF17181D, WIT = 0xFFFFFFFF, TITEL = 0xFF2E7F86, START = 0xFFE0632F,
            KLEIN = 0xFFC9CBD6;
    private static final Identifier LOGO = Guhs.id("textures/gui/snuffel/guhstation_logo.png");
    private static final int IN = 44, RONDE = 150;

    private final Rang rang;
    private final int geuren;
    private final String ras, kleur;
    @Nullable
    private HondEntity hond, pup;
    private int ticks;
    private boolean gestart;
    private int left, top;

    public GuhstationScherm(CompoundTag stand) {
        super(Component.translatable("block.guhs.guhstation"));
        this.rang = Rang.metNummer(stand.getIntOr("Rang", 1));
        this.geuren = stand.getIntOr("Aantal", 0);
        this.ras = stand.getStringOr("Ras", "shiba");
        this.kleur = stand.getStringOr("Kleur", "rood");
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.snuffel.guhstation.nee"), b -> onClose()).bounds(left + 12, top + H - 28, W - 24, 20)
                .build());
    }

    private int veldX() {
        return left + (W - VELD_W) / 2;
    }

    private int veldY() {
        return top + 22;
    }

    private void start() {
        if (gestart) {
            return;
        }
        gestart = true;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f));
        ClientPacketDistributor.sendToServer(new SnuffelPayloads.Knop(SnuffelPayloads.Knop.START));
        onClose();
    }

    @Override
    public void tick() {
        ticks++;
        if (hond == null) {
            hond = HondClient.maak(ras, kleur, false);
            pup = HondClient.maak(ras, kleur, true);
        }
        if (hond == null || pup == null) {
            return;
        }
        hond.tickCount++;
        pup.tickCount++;
        int t = ticks;
        if (t < IN) {
            hond.loopt = true;
            pup.loopt = true;
            hond.zetHouding(0);
            pup.zetHouding(0);
            return;
        }
        int r = (t - IN) % RONDE;
        // the big dog: wags, barks once, sniffs, wags
        hond.loopt = false;
        hond.zetHouding(r < 44 ? Hondvorm.KWISPELT : r >= 96 && r < 132 ? Hondvorm.SNUFFELT : r >= 132 ? Hondvorm.KWISPELT : 0);
        if (r == 52) {
            hond.blaf(Hondvorm.BLAF_TICKS);
        }
        // the puppy: wags, dashes off and back, sits
        boolean rent = r >= 44 && r < 96;
        pup.loopt = rent;
        pup.zetHouding(r < 44 ? Hondvorm.KWISPELT : r >= 100 && r < 140 ? Hondvorm.ZIT : 0);
        if (r == 96) {
            pup.blaf(Hondvorm.BLAF_TICKS);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // (no dimmed world: this is a small window) the console
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, KAST_DONKER);
        g.fill(left, top, left + W, top + H, KAST);
        g.fill(left, top, left + W, top + 2, KAST_LICHT);
        g.fill(left, top, left + 2, top + H, KAST_LICHT);
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, left + 8, top + 3, 0f, 0f, 16, 16, 16, 16, 16, 16);
        g.text(font, Component.translatable("block.guhs.guhstation"), left + 28, top + 8, KLEIN, false);
        // the white field
        int vx = veldX(), vy = veldY();
        g.fill(vx - 2, vy - 2, vx + VELD_W + 2, vy + VELD_H + 2, KAST_DONKER);
        g.fill(vx, vy, vx + VELD_W, vy + VELD_H, WIT);
        // our own title, big
        Component titel = Component.translatable("gui.guhs.snuffel.guhstation.titel");
        g.pose().pushMatrix();
        g.pose().translate(vx + VELD_W / 2f, vy + 8);
        g.pose().scale(1.5f, 1.5f);
        g.text(font, titel, -font.width(titel) / 2, 0, TITEL, false);
        g.pose().popMatrix();
        g.fill(vx + 40, vy + 24, vx + VELD_W - 40, vy + 25, 0xFFBFE3E6);
        // "Druk op start", blinking
        if ((ticks / 10) % 2 == 0 || startOnder(mouseX, mouseY)) {
            g.centeredText(font, Component.translatable("gui.guhs.snuffel.guhstation.start"), vx + VELD_W / 2, vy + VELD_H - 14, START);
        }
        // under the white: the rank and the scents, small
        g.pose().pushMatrix();
        g.pose().translate(left + W / 2f, vy + VELD_H + 8);
        g.pose().scale(0.75f, 0.75f);
        Component regel = rang.regel();
        g.text(font, regel, -font.width(regel) / 2, 0, KLEIN, false);
        Component aantal = Component.translatable("gui.guhs.snuffel.guhstation.geuren", geuren);
        g.text(font, aantal, -font.width(aantal) / 2, 11, KLEIN, false);
        g.pose().popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        if (hond == null || pup == null) {
            return;
        }
        int vx = veldX(), vy = veldY();
        float t = ticks + partialTick;
        // where they stand on the white (pixels from the field's left edge) and which way they look (yaw: 0 = at you)
        float hx, px, hYaw, pYaw, pHop = 0f;
        if (t < IN) {
            float f = t / IN;
            hx = Mth.lerp(f, -36f, 74f);
            px = Mth.lerp(f, VELD_W + 30f, 146f);
            hYaw = -80f;
            pYaw = 80f;
        } else {
            float r = (t - IN) % RONDE;
            hx = 74f;
            hYaw = r >= 96 && r < 132 ? -40f : -62f;
            if (r >= 44 && r < 70) {            // the puppy dashes to the right edge...
                px = Mth.lerp((r - 44) / 26f, 146f, 196f);
                pYaw = -80f;
            } else if (r >= 70 && r < 96) {     // ...and back
                px = Mth.lerp((r - 70) / 26f, 196f, 146f);
                pYaw = 80f;
            } else {
                px = 146f;
                pYaw = 62f;
                if (r < 44) {
                    pHop = Math.abs(Mth.sin(r * 0.35f)) * 5f;   // (it bounces with joy)
                }
            }
        }
        int grond = vy + VELD_H - 26;
        g.enableScissor(vx, vy + 26, vx + VELD_W, vy + VELD_H - 16);
        teken(g, hond, vx + hx, grond, hYaw, 44f);
        teken(g, pup, vx + px, grond - pHop, pYaw, 44f);
        g.disableScissor();
    }

    /** A dog with its feet on this point of the field. */
    private static void teken(GuiGraphicsExtractor g, HondEntity e, float x, float voetY, float yaw, float schaal) {
        int half = 40, hoog = 72;
        int x1 = Math.round(x) - half, x2 = Math.round(x) + half, y2 = Math.round(voetY) + 8, y1 = y2 - hoog;
        float midden = (y1 + y2) / 2f;
        Tekenaar.teken(g, x1, y1, x2, y2, e, yaw, -6f, schaal, voetY - midden);
    }

    private boolean startOnder(double mx, double my) {
        int vx = veldX(), vy = veldY();
        return mx >= vx && mx <= vx + VELD_W && my >= vy + VELD_H - 22 && my <= vy + VELD_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && startOnder(event.x(), event.y())) {
            start();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER || event.key() == GLFW.GLFW_KEY_SPACE) {
            start();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
