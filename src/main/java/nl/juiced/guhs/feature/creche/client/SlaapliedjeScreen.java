package nl.juiced.guhs.feature.creche.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.creche.CrechePayloads;
import nl.juiced.guhs.feature.creche.Slaapliedje;
import org.lwjgl.glfw.GLFW;

/**
 * The lullaby: little stars float towards the moon; tap (space or a click) when a star is in the moon and its note
 * sounds. The babyguhtje's eyes close a bit with every note you hit. At the end the server hears how many you hit
 * ({@link Slaapliedje#NODIG}% and it sleeps). Juf Knuffel hums along softly, so you hear the beat.
 */
public class SlaapliedjeScreen extends Screen {
    private static final int W = 300, H = 160;
    private static final long VENSTER = 200;          // ms early/late that still counts
    private static final double SNELHEID = 0.09;       // pixels per ms
    private final int npcId;
    private final BlockPos pos;
    private final Slaapliedje liedje;
    private final boolean[] raak, gemist, geneuried;
    private long start = -1;
    private int hits;
    private long klaarOp = -1;
    private boolean verstuurd;
    private int left, top;

    public SlaapliedjeScreen(CrechePayloads.Liedje payload) {
        super(Component.translatable("gui.guhs.creche.liedje.titel"));
        this.npcId = payload.npcId();
        this.pos = payload.pos();
        Slaapliedje s = Slaapliedje.byIndex(payload.liedje());
        this.liedje = s == null ? Slaapliedje.STERRETJES : s;
        this.raak = new boolean[liedje.aantal()];
        this.gemist = new boolean[liedje.aantal()];
        this.geneuried = new boolean[liedje.aantal()];
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        if (start < 0) {
            start = System.currentTimeMillis();
        }
    }

    private long nu() {
        return System.currentTimeMillis() - start;
    }

    private long tijd(int i) {
        return liedje.tick(i) * 50L;
    }

    private void tik() {
        if (klaarOp >= 0) {
            return;
        }
        long t = nu();
        int best = -1;
        long bestD = VENSTER + 1;
        for (int i = 0; i < liedje.aantal(); i++) {
            if (raak[i] || gemist[i]) {
                continue;
            }
            long d = Math.abs(tijd(i) - t);
            if (d < bestD) {
                best = i;
                bestD = d;
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (best >= 0) {
            raak[best] = true;
            hits++;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HARP.value(), Slaapliedje.pitch(liedje.noot(best)), 0.9f));
        } else {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WOOL_STEP, 1.2f, 0.4f));
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            tik();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            tik();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public void tick() {
        long t = nu();
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < liedje.aantal(); i++) {
            if (!geneuried[i] && t >= tijd(i)) {                    // the Juf hums the beat, softly
                geneuried[i] = true;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), Slaapliedje.pitch(liedje.noot(i)), 0.12f));
            }
            if (!raak[i] && !gemist[i] && t > tijd(i) + VENSTER) {
                gemist[i] = true;
            }
        }
        if (klaarOp < 0 && t > liedje.lengte() * 50L + 400) {
            klaarOp = t;
            if (!verstuurd) {
                verstuurd = true;
                PacketDistributor.sendToServer(new CrechePayloads.LiedjeKlaar(npcId, pos, liedje.ordinal(), hits));
            }
        }
        if (klaarOp >= 0 && t - klaarOp > 1800) {
            onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        long t = nu();
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFB9D8F7);
        g.fill(left, top, left + W, top + H, 0xF0161A38);
        for (int i = 0; i < 24; i++) {                              // the night sky
            int sx = left + 6 + (i * 53) % (W - 12), sy = top + 6 + (i * 29) % 40;
            int a = (int) (90 + 90 * Math.sin(t / 500.0 + i * 1.7));
            g.fill(sx, sy, sx + 1, sy + 1, (a << 24) | 0xFFFFFF);
        }
        g.drawCenteredString(font, Component.translatable("gui.guhs.creche.liedje.kop", Component.translatable("gui.guhs.knus.slaapliedjes." + liedje.id()))
                .withStyle(ChatFormatting.BOLD), width / 2, top + 8, 0xFFFFE6A0);
        // the babyguhtje (its eyes close as you sing)
        float slaap = liedje.aantal() == 0 ? 0 : hits / (float) liedje.aantal();
        int fx = left + 16, fy = top + 50, fw = 56, fh = 48;
        g.fill(fx + 4, fy - 8, fx + 14, fy + 2, 0xFFF59AC0);                 // ears
        g.fill(fx + fw - 14, fy - 8, fx + fw - 4, fy + 2, 0xFFF59AC0);
        g.fill(fx, fy, fx + fw, fy + fh, 0xFFFFB6D2);
        g.fill(fx + 2, fy - 2, fx + fw - 2, fy, 0xFFFFB6D2);
        g.fill(fx + 2, fy + fh, fx + fw - 2, fy + fh + 2, 0xFFFFB6D2);
        int eye = Math.max(1, Math.round(8 * (1 - slaap)));
        for (int ex : new int[]{fx + 12, fx + fw - 20}) {
            g.fill(ex, fy + 18 + (8 - eye), ex + 8, fy + 26, 0xFF201830);
            if (eye > 3) {
                g.fill(ex + 1, fy + 19 + (8 - eye), ex + 3, fy + 21 + (8 - eye), 0xFFFFFFFF);
            }
        }
        g.fill(fx + 6, fy + 30, fx + 12, fy + 34, 0xFFF77AA8);                // cheeks
        g.fill(fx + fw - 12, fy + 30, fx + fw - 6, fy + 34, 0xFFF77AA8);
        g.fill(fx + 22, fy + 34, fx + fw - 22, fy + 40, 0xFF8FD0F5);          // the pacifier
        g.fill(fx + 25, fy + 40, fx + fw - 25, fy + 44, 0xFFFFE08A);
        if (slaap > 0.6f) {
            g.drawString(font, "z", fx + fw + 2, fy - 4 - (int) ((t / 90) % 8), 0xFFB9D8F7, false);
        }
        // the lane: the moon on the left, the stars float in from the right
        int laneL = left + 90, laneR = left + W - 12, laneY = top + 74;
        g.fill(laneL, laneY - 10, laneR, laneY + 10, 0x40B9D8F7);
        int mx = laneL + 22;
        g.fill(mx - 9, laneY - 9, mx + 9, laneY + 9, 0xFFFFE08A);          // the moon
        g.fill(mx - 3, laneY - 9, mx + 9, laneY + 3, 0xFF161A38);
        g.fill(mx - 9, laneY - 11, mx + 9, laneY - 9, 0x60FFE08A);
        for (int i = 0; i < liedje.aantal(); i++) {
            if (raak[i]) {
                continue;
            }
            double x = mx + (tijd(i) - t) * SNELHEID;
            if (x < laneL - 4 || x > laneR) {
                continue;
            }
            int sx = (int) x, sy = laneY + (gemist[i] ? (int) Math.min(30, (t - tijd(i) - VENSTER) / 20) : 0);
            int c = gemist[i] ? 0x80909090 : 0xFFFFF3A0;
            g.fill(sx - 1, sy - 4, sx + 2, sy + 5, c);                      // a little star
            g.fill(sx - 4, sy - 1, sx + 5, sy + 2, c);
        }
        g.drawCenteredString(font, Component.translatable("gui.guhs.creche.liedje.raak", hits, liedje.aantal()), laneL + (laneR - laneL) / 2, top + 100, 0xFFFFE6EE);
        Component hint = klaarOp >= 0
                ? Component.translatable(liedje.gelukt(hits) ? "gui.guhs.creche.liedje.gelukt" : "gui.guhs.creche.liedje.mislukt")
                : Component.translatable("gui.guhs.creche.liedje.uitleg");
        int y = top + 116;
        for (var line : font.split(hint, W - 24)) {
            g.drawCenteredString(font, line, width / 2, y, klaarOp >= 0 ? 0xFFFFD27A : 0xFFB9D8F7);
            y += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
