package nl.juiced.guhs.feature.guhpixel.grap2.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.gids.client.GidsTekst;
import nl.juiced.guhs.feature.guhpixel.grap2.Grap2Payloads;
import nl.juiced.guhs.feature.guhpixel.grap2.Grap2Slice;
import org.lwjgl.glfw.GLFW;

/**
 * The credits of Boer zoekt Guh: a dark screen, the title, and pairs of lines (a job, who did it) rolling up, like the end
 * of a TV show. It closes by itself at the end (or with a click, space or escape) and tells the server so.
 */
public class BzgAftitelingScherm extends Screen {
    private static final int PAAR_H = 30;
    private final int paren;
    private final boolean demo;
    private int ticks;
    private boolean gemeld;

    public BzgAftitelingScherm(CompoundTag data) {
        super(Component.translatable("gui.guhs.bzg.aftiteling.titel"));
        this.paren = Math.max(1, data.getIntOr("Regels", 1) / 2);
        this.demo = data.getBooleanOr("Demo", false);
    }

    @Override
    protected void init() {
        if (ticks == 0 && minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Grap2Slice.GELUID_TUNE.get(), 1f));
        }
    }

    private float rol(float partialTick) {
        return (ticks + partialTick) * 0.9f;
    }

    private int lengte() {
        return height + 60 + paren * PAAR_H + 70;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (rol(0) > lengte()) {
            onClose();
        }
    }

    @Override
    public void onClose() {
        if (!gemeld && !demo && minecraft != null && minecraft.getConnection() != null) {
            gemeld = true;
            ClientPacketDistributor.sendToServer(new Grap2Payloads.BzgDoe(Grap2Payloads.AFTITELING_KLAAR, 0));
        }
        super.onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ticks > 40 && (event.key() == GLFW.GLFW_KEY_SPACE || event.key() == GLFW.GLFW_KEY_ENTER)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (ticks > 40) {
            onClose();
        }
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, width, height, 0xF0120810);
        float y = height - rol(partialTick);
        int cx = width / 2;
        Component kop = title.copy().withStyle(ChatFormatting.BOLD);
        int kw = font.width(kop);
        GidsTekst.schaal(g, kop, cx - kw, Math.round(y), 2f, 0xFFFFD27A, false);
        g.centeredText(font, Component.translatable("gui.guhs.bzg.aftiteling.onder"), cx, Math.round(y) + 24, 0xFFF7B6CB);
        y += 60;
        for (int i = 1; i <= paren; i++) {
            int py = Math.round(y + (i - 1) * PAAR_H);
            if (py > -PAAR_H && py < height + 4) {
                g.centeredText(font, Component.translatable("gui.guhs.bzg.aftiteling.rol." + i), cx, py, 0xFFB090A0);
                g.centeredText(font, Component.translatable("gui.guhs.bzg.aftiteling.naam." + i), cx, py + 11, 0xFFFFE6EE);
            }
        }
        int ey = Math.round(y + paren * PAAR_H + 20);
        g.centeredText(font, Component.translatable("gui.guhs.bzg.aftiteling.einde").withStyle(ChatFormatting.BOLD), cx, ey, 0xFFFFD27A);
        g.centeredText(font, Component.translatable("gui.guhs.bzg.aftiteling.einde.onder"), cx, ey + 12, 0xFFB090A0);
        if (ticks > 40) {
            int alpha = Mth.clamp((ticks - 40) * 8, 0, 160);
            g.text(font, Component.translatable("gui.guhs.bzg.aftiteling.sluit"), 6, height - 12, (alpha << 24) | 0x907080, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
