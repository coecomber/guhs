package nl.juiced.guhs.feature.kermiskraam.client;

import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.kermiskraam.Kraam;
import nl.juiced.guhs.feature.kermiskraam.KraamPayloads;
import nl.juiced.guhs.registry.ModItems;

/**
 * The little games of the game stalls, one click each:
 * <ul>
 *   <li>Guhtjes vissen: rubber guhtjes float past the hook; click when one is under it (3 casts);</li>
 *   <li>Blikkengooien: a marker swings over a bar; click in the middle (3 throws, 3/2/1 tins);</li>
 *   <li>Knabbelschieten: knabbels pop up for a moment; click them (10 knabbels).</li>
 * </ul>
 * Every game gets harder as it goes. The result goes to the server (KraamSpel), which pays out.
 */
public class KraamSpelScherm extends Screen {
    private static final int W = 240, H = 176;
    private static final int RAND = 0xFFE85C84, PANEEL = 0xF02C1C30, VELD = 0xFF4A3248, TEKST = 0xFFFFF0FA, DOF = 0xFFC8A8C0, GOUD = 0xFFFFD65A;
    private static final int GUHTJES = 8, WORPEN = 3, KNABBELS = 10, KNABBEL_TIJD = 26, PAUZE = 5, SLUIT_NA = 50;
    private static final double RING_X = 80, RING_Y = 40, VANG = 0.16, HAAK = -Math.PI / 2;

    private final Kraam kraam;
    private final long seed;
    private final Random rng;
    private int left, top, ticks;
    private int score, beurt;
    private int klaar = -1;                     // the tick the game ended, or -1
    private boolean verstuurd;
    // vissen
    private final boolean[] weg = new boolean[GUHTJES];
    private double hoek, snelheid = 0.055;
    // blikken
    private double marker, fase, armSnelheid = 0.13;
    private int laatsteTreffer = -1;
    // schieten
    private int doelX, doelY, doelTot, pauzeTot;

    public KraamSpelScherm(Kraam kraam, long seed) {
        super(Component.translatable("gui.guhs.kermiskraam." + kraam.id + ".titel"));
        this.kraam = kraam;
        this.seed = seed;
        this.rng = new Random(seed);
        for (int i = 0; i < GUHTJES; i++) {
            weg[i] = rng.nextInt(4) == 0;       // some gaps in the ring, so it is not just "click whenever"
        }
        pauzeTot = 20;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static double gat(double a, double b) {
        return Math.abs(Mth.wrapDegrees(Math.toDegrees(a - b))) * Math.PI / 180;
    }

    @Override
    public void tick() {
        ticks++;
        if (klaar >= 0) {
            if (ticks - klaar >= SLUIT_NA && minecraft != null) {
                minecraft.setScreen(null);
            }
            return;
        }
        switch (kraam) {
            case VISSEN -> hoek += snelheid;
            case BLIKKEN -> {
                fase += armSnelheid;
                marker = Math.sin(fase);
            }
            case SCHIETEN -> tickSchieten();
            default -> { }
        }
    }

    private void tickSchieten() {
        if (doelTot > 0 && ticks >= doelTot) {
            volgendeKnabbel();
        } else if (doelTot == 0 && ticks >= pauzeTot) {
            doelX = 16 + rng.nextInt(W - 52);
            doelY = 40 + rng.nextInt(H - 88);
            doelTot = ticks + KNABBEL_TIJD - beurt;
        }
    }

    private void volgendeKnabbel() {
        doelTot = 0;
        pauzeTot = ticks + PAUZE;
        if (++beurt >= KNABBELS) {
            einde();
        }
    }

    private void einde() {
        klaar = ticks;
        verstuurd = true;
        ClientPacketDistributor.sendToServer(new KraamPayloads.Klaar(seed, score));
    }

    private void geluid(float toon) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, toon));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (klaar >= 0 || event.button() != 0) {
            return false;
        }
        switch (kraam) {
            case VISSEN -> klikVissen();
            case BLIKKEN -> klikBlikken();
            case SCHIETEN -> klikSchieten(event.x() - left, event.y() - top);
            default -> { }
        }
        return true;
    }

    private void klikVissen() {
        boolean raak = false;
        for (int i = 0; i < GUHTJES && !raak; i++) {
            if (!weg[i] && gat(hoek + i * Math.PI * 2 / GUHTJES, HAAK) < VANG) {
                weg[i] = true;
                raak = true;
            }
        }
        score += raak ? 1 : 0;
        geluid(raak ? 1.6f : 0.7f);
        snelheid += 0.015;
        if (++beurt >= WORPEN) {
            einde();
        }
    }

    private void klikBlikken() {
        double af = Math.abs(marker);
        int blikken = af < 0.08 ? 3 : af < 0.25 ? 2 : af < 0.5 ? 1 : 0;
        laatsteTreffer = blikken;
        score = Math.min(kraam.hoogste, score + blikken);
        geluid(0.7f + blikken * 0.3f);
        armSnelheid += 0.03;
        if (++beurt >= WORPEN || score >= kraam.hoogste) {
            einde();
        }
    }

    private void klikSchieten(double x, double y) {
        if (doelTot > 0 && Math.hypot(x - doelX - 8, y - doelY - 8) <= 12) {
            score++;
            geluid(1.5f);
            volgendeKnabbel();
        }
    }

    /** Closing early gives up the round. */
    @Override
    public void removed() {
        if (!verstuurd) {
            verstuurd = true;
            ClientPacketDistributor.sendToServer(new KraamPayloads.Klaar(seed, -1));
        }
        super.removed();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        for (int x = 0; x < W; x += 16) {                                       // a striped awning
            g.fill(left + x, top, left + Math.min(W, x + 8), top + 6, 0xFFFFF4E0);
            g.fill(left + x + 8, top, left + Math.min(W, x + 16), top + 6, RAND);
        }
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 10, TEKST);
        g.fill(left + 8, top + 24, left + W - 8, top + H - 22, VELD);
        g.centeredText(font, Component.translatable("gui.guhs.kermiskraam." + kraam.id + ".uitleg"), width / 2, top + H - 16, DOF);
        float t = klaar >= 0 ? 0 : partialTick;
        switch (kraam) {
            case VISSEN -> tekenVissen(g, t);
            case BLIKKEN -> tekenBlikken(g, t);
            case SCHIETEN -> tekenSchieten(g);
            default -> { }
        }
        g.text(font, Component.translatable("gui.guhs.kermiskraam.score", score, kraam.hoogste), left + 12, top + 28, GOUD, false);
        if (klaar >= 0) {
            g.fill(left + 40, top + 70, left + W - 40, top + 104, 0xE0000000);
            String uit = score >= kraam.hoogste ? "perfect" : score * 2 >= kraam.hoogste ? "goed" : "jammer";
            g.centeredText(font, Component.translatable("gui.guhs.kermiskraam.einde." + uit), width / 2, top + 83, GOUD);
        }
    }

    private void guhtje(GuiGraphicsExtractor g, int x, int y, int kleur) {
        g.fill(x - 5, y - 4, x + 5, y + 4, kleur);
        g.fill(x - 5, y - 7, x - 2, y - 4, kleur);                              // the ears
        g.fill(x + 2, y - 7, x + 5, y - 4, kleur);
        g.fill(x - 3, y - 2, x - 1, y, 0xFF201018);                             // the eyes
        g.fill(x + 1, y - 2, x + 3, y, 0xFF201018);
    }

    private void tekenVissen(GuiGraphicsExtractor g, float t) {
        int cx = left + W / 2, cy = top + 92;
        g.fill(cx - 96, cy - 50, cx + 96, cy + 50, 0xFF3C78C8);                 // the water
        for (int i = 0; i < GUHTJES; i++) {
            if (!weg[i]) {
                double a = hoek + snelheid * t + i * Math.PI * 2 / GUHTJES;
                guhtje(g, cx + (int) (Math.cos(a) * RING_X), cy + (int) (Math.sin(a) * RING_Y), 0xFFFFB0CC);
            }
        }
        int haakY = cy - (int) RING_Y;
        g.fill(cx, top + 24, cx + 1, haakY - 2, 0xFFE0E0E0);                    // the line and the hook
        g.fill(cx - 2, haakY - 2, cx + 3, haakY, GOUD);
        g.text(font, Component.translatable("gui.guhs.kermiskraam.worpen", WORPEN - beurt), left + W - 70, top + 28, TEKST, false);
    }

    private void tekenBlikken(GuiGraphicsExtractor g, float t) {
        int cx = left + W / 2;
        int staan = kraam.hoogste - score, i = 0;
        for (int rij = 3; rij >= 1; rij--) {                                    // the pyramid: the top falls first
            for (int k = 0; k < rij; k++, i++) {
                if (i < staan) {
                    int x = cx - rij * 10 + k * 20, y = top + 118 - (3 - rij) * 22 - 26;
                    g.fill(x + 2, y, x + 18, y + 20, 0xFFC0C8D0);
                    g.fill(x + 2, y + 6, x + 18, y + 14, RAND);
                }
            }
        }
        int balk = 90, y = top + 132;
        g.fill(cx - balk, y, cx + balk, y + 10, 0xFF803040);
        g.fill(cx - (int) (balk * .5), y, cx + (int) (balk * .5), y + 10, 0xFFC08030);
        g.fill(cx - (int) (balk * .25), y, cx + (int) (balk * .25), y + 10, 0xFFD8C040);
        g.fill(cx - (int) (balk * .08), y, cx + (int) (balk * .08), y + 10, 0xFF60D080);
        double m = klaar >= 0 ? marker : Math.sin(fase + armSnelheid * t);
        g.fill(cx + (int) (m * balk) - 1, y - 2, cx + (int) (m * balk) + 1, y + 12, 0xFFFFFFFF);
        g.text(font, Component.translatable("gui.guhs.kermiskraam.worpen", WORPEN - beurt), left + W - 70, top + 28, TEKST, false);
        if (laatsteTreffer >= 0) {
            g.text(font, Component.translatable("gui.guhs.kermiskraam.blikken_om", laatsteTreffer), left + 12, top + 40, DOF, false);
        }
    }

    private void tekenSchieten(GuiGraphicsExtractor g) {
        if (doelTot > 0) {
            g.fill(left + doelX - 3, top + doelY - 3, left + doelX + 19, top + doelY + 19, RAND);
            g.fill(left + doelX - 1, top + doelY - 1, left + doelX + 17, top + doelY + 17, 0xFFFFF4E0);
            g.item(new ItemStack(ModItems.KAAS_KNABBELS.get()), left + doelX, top + doelY);
        }
        g.text(font, Component.translatable("gui.guhs.kermiskraam.knabbels_over", KNABBELS - beurt), left + W - 90, top + 28, TEKST, false);
    }
}
