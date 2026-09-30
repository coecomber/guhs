package nl.juiced.guhs.feature.guhwaii.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import nl.juiced.guhs.feature.guhwaii.GuhwaiiPayloads;
import nl.juiced.guhs.feature.guhwaii.Scanner;

/**
 * The vadsigheid-scanner's big screen (626's alien tech, in guh colours): the guh on the scan plate with a scan beam going
 * up and down, and the meter VADSIGHEIDSNIVEAU filling up past "een beetje vads", "vads", "heel vads", "VAHOEG"... until the
 * needle breaks right through the top of the frame (cracks, sparks, the screen shakes) and it flashes
 * ONBEREKENBAAR VAHOEG!!! Then the fun little readings pop in one by one, and "Njeg!" closes it.
 */
public class ScannerScherm extends Screen {
    private static final int W = 340, H = 200, PIC = 92;
    private static final int PANEL = 0xF0101A34, RAND = 0xFF5FE0FF, RAND2 = 0xFF2A6EA8, TEKST = 0xFFD8F6FF, ROZE = 0xFFFF8CC8;
    /** The meter's colour steps from bottom to top. */
    private static final int[] KLEUREN = {0xFF5CE08A, 0xFF8BE05C, 0xFFC4E05C, 0xFFF0D85C, 0xFFF8B04C, 0xFFF8884C, 0xFFF86A6A, 0xFFF85C9C,
            0xFFE85CD8, 0xFFB45CF8};
    private static final String[] LABELS = {"gui.guhs.guhwaii.scanner.niveau.1", "gui.guhs.guhwaii.scanner.niveau.2",
            "gui.guhs.guhwaii.scanner.niveau.3", "gui.guhs.guhwaii.scanner.niveau.4"};
    private static final String[] METINGEN = {"gui.guhs.guhwaii.scanner.meting.1", "gui.guhs.guhwaii.scanner.meting.2",
            "gui.guhs.guhwaii.scanner.meting.3", "gui.guhs.guhwaii.scanner.meting.4"};

    private final GuhwaiiPayloads.Scan scan;
    private int t;
    private int left, top;
    private Button njeg;

    public ScannerScherm(GuhwaiiPayloads.Scan scan) {
        super(Component.translatable("gui.guhs.guhwaii.scanner.titel"));
        this.scan = scan;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        njeg = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhwaii.scanner.njeg"), b -> onClose())
                .bounds(left + W - 10 - 80, top + H - 26, 80, 20).build());
        njeg.visible = t > Scanner.METING_TICKS + 20;
    }

    @Override
    public void tick() {
        t++;
        if (njeg != null) {
            njeg.visible = t > Scanner.METING_TICKS + 20;
        }
    }

    /** 0..1 how full the meter is (easing in, and a last rush). */
    private float vulling(float nu) {
        float x = Mth.clamp(nu / Scanner.METING_TICKS, 0f, 1f);
        return x < 0.8f ? 0.85f * (x / 0.8f) * (x / 0.8f) : 0.85f + 0.15f * (x - 0.8f) / 0.2f;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float pt) {
        super.extractBackground(g, mouseX, mouseY, pt);
        float nu = t + pt;
        boolean kapot = nu >= Scanner.METING_TICKS;
        float na = nu - Scanner.METING_TICKS;
        // the screen shakes for a moment when the meter breaks
        int sx = 0, sy = 0;
        if (kapot && na < 14) {
            sx = (int) (Math.sin(nu * 3.1) * 3 * (1 - na / 14));
            sy = (int) (Math.cos(nu * 2.3) * 2 * (1 - na / 14));
        }
        int l = left + sx, o = top + sy;
        g.fill(l - 2, o - 2, l + W + 2, o + H + 2, RAND2);
        g.fill(l - 1, o - 1, l + W + 1, o + H + 1, RAND);
        g.fill(l, o, l + W, o + H, PANEL);
        // scan lines
        for (int y = o + 2; y < o + H - 2; y += 3) {
            g.fill(l + 1, y, l + W - 1, y + 1, 0x14FFFFFF);
        }
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), l + 10, o + 7, RAND, false);
        String exp = "EXPERIMENT 626";
        g.text(font, exp, l + W - 10 - font.width(exp), o + 7, 0xFF7FA8D8, false);
        // the guh on the plate
        int px = l + 10, py = o + 22;
        g.fill(px - 1, py - 1, px + PIC + 1, py + PIC + 1, RAND2);
        g.fill(px, py, px + PIC, py + PIC, 0xFF0A1226);
        Entity e = minecraft.level == null ? null : minecraft.level.getEntity(scan.guhId());
        if (e instanceof LivingEntity living) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, px, py, px + PIC, py + PIC, 30, 0.0625f, mouseX, mouseY, living);
        }
        if (!kapot) {
            int beam = py + (int) ((Math.sin(nu * 0.22) * 0.5 + 0.5) * (PIC - 4));
            g.fill(px, beam, px + PIC, beam + 2, 0xC05FE0FF);
            g.fill(px, beam - 3, px + PIC, beam, 0x405FE0FF);
        } else if (((int) nu / 4) % 2 == 0) {
            g.fill(px, py, px + PIC, py + PIC, 0x30FF8CC8);
        }
        g.text(font, font.plainSubstrByWidth(scan.naam(), PIC), px, py + PIC + 4, TEKST, false);
        g.text(font, font.plainSubstrByWidth(scan.soort(), PIC), px, py + PIC + 14, 0xFF8FB8E0, false);
        // the meter
        int mx = l + 10 + PIC + 14, my = o + 22;
        g.text(font, Component.translatable("gui.guhs.guhwaii.scanner.niveau").withStyle(ChatFormatting.BOLD), mx, my, TEKST, false);
        int bx = mx, by = my + 14, bw = 22, bh = 118;
        g.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, RAND);
        g.fill(bx, by, bx + bw, by + bh, 0xFF06101E);
        float vol = kapot ? 1f : vulling(nu);
        int segs = KLEUREN.length, segH = bh / segs;
        for (int i = 0; i < segs; i++) {
            float grens = (i + 1f) / segs;
            if (vol + 0.0001f >= grens || vol > (float) i / segs) {
                int y1 = by + bh - (i + 1) * segH + 1, y0 = by + bh - i * segH - 1;
                float deel = Mth.clamp((vol - (float) i / segs) * segs, 0f, 1f);
                int yTop = y0 - (int) ((y0 - y1) * deel);
                g.fill(bx + 2, yTop, bx + bw - 2, y0, KLEUREN[i]);
            }
        }
        // the level labels next to the meter
        for (int i = 0; i < LABELS.length; i++) {
            int ly = by + bh - (int) ((i + 1f) / LABELS.length * bh * 0.95f);
            g.fill(bx + bw + 2, ly, bx + bw + 6, ly + 1, RAND);
            boolean bereikt = vol >= (i + 1f) / LABELS.length * 0.95f;
            g.text(font, Component.translatable(LABELS[i]), bx + bw + 9, ly - 4, bereikt ? KLEUREN[Math.min(segs - 1, (i + 1) * segs / LABELS.length - 1)]
                    : 0xFF5A7090, false);
        }
        // the needle going up... and through the top
        int naald = kapot ? by - 10 - (int) Math.min(18, na * 3) : by + bh - (int) (vol * bh);
        g.fill(bx - 6, naald - 1, bx + bw + 6, naald + 1, 0xFFFFFFFF);
        if (kapot) {
            // the frame broke: cracks and sparks at the top
            int cx = bx + bw / 2;
            for (int k = -2; k <= 2; k++) {
                for (int s = 0; s < 8; s++) {
                    g.fill(cx + k * 3 + (s % 2 == 0 ? s / 2 : -s / 2) * (k == 0 ? 1 : k), by - 2 + s, cx + k * 3 + 1 + (s % 2 == 0 ? s / 2 : -s / 2) * (k == 0 ? 1 : k),
                            by - 1 + s, 0xFFFFFFFF);
                }
            }
            for (int k = 0; k < 10; k++) {
                double a = k * 0.63 + nu * 0.05;
                int r = (int) (6 + (na * 1.6 + k * 3) % 24);
                g.fill(cx + (int) (Math.cos(a) * r), by - 8 - (int) (Math.abs(Math.sin(a)) * r), cx + (int) (Math.cos(a) * r) + 2,
                        by - 6 - (int) (Math.abs(Math.sin(a)) * r), KLEUREN[k % segs]);
            }
        }
        // the result
        int rx = bx + bw + 68, ry = by + 2, rw = l + W - 10 - rx;
        if (!kapot) {
            int pct = (int) (vol * 100);
            g.text(font, pct + "%", rx, ry, TEKST, false);
            String dots = ".".repeat(1 + ((int) nu / 5) % 3);
            g.text(font, Component.translatable("gui.guhs.guhwaii.scanner.meten").getString() + dots, rx, ry + 12, 0xFF8FB8E0, false);
        } else {
            boolean flits = ((int) nu / 3) % 2 == 0;
            Component groot = Component.translatable("gui.guhs.guhwaii.scanner.onberekenbaar.kort").withStyle(ChatFormatting.BOLD);
            g.pose().pushMatrix();
            float schaal = 1.3f + (na < 8 ? (8 - na) * 0.08f : 0f);
            g.pose().translate(rx, ry);
            g.pose().scale(schaal, schaal);
            for (var line : font.split(groot, (int) (rw / schaal))) {
                g.text(font, line, 0, 0, flits ? ROZE : 0xFFFFE070, true);
                g.pose().translate(0, 11);
            }
            g.pose().popMatrix();
            int y = ry + 42;
            for (int i = 0; i < METINGEN.length; i++) {
                if (na > 14 + i * 9) {
                    for (var line : font.split(Component.translatable(METINGEN[i]), rw)) {
                        g.text(font, line, rx, y, TEKST, false);
                        y += 10;
                    }
                    y += 2;
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
