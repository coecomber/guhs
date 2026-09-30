package nl.juiced.guhs.feature.sterrenwacht.client;

import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.sterrenwacht.Sterrenbeeld;
import nl.juiced.guhs.feature.sterrenwacht.SterrenwachtFeature;
import nl.juiced.guhs.feature.sterrenwacht.SterrenwachtPayloads;
import nl.juiced.guhs.feature.sterrenwacht.Sterrenkijken;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * Looking through a guh telescope: a round piece of night sky (with the pink Guhmensie moon) full of stars, and
 * somewhere among them one guh constellation. The card on the right shows its shape; click a star and then the next
 * one to draw a line. A right line turns gold, a wrong one flashes red ("njeg!"). When every line is there: VAHOEG!
 * (the server checks the lines and gives the reward, see Sterrenkijken).
 */
public class TelescoopScherm extends Screen {
    private static final int W = 300, H = 204, R = 90;
    private static final int PANEL = 0xF0140C24, BORDER = 0xFF8C7AD8, GOUD = 0xFFFFD86A, ROOD = 0xFFFF5A6A, TEKST = 0xFFE8E0FF;

    private final Sterrenbeeld beeld;
    private final boolean nieuw;
    private final int vannacht;
    private final List<float[]> sterren = new ArrayList<>();     // {x, y, helderheid, fase} relative to the circle's middle
    private final List<Integer> beeldIndex = new ArrayList<>();  // star -> index in the constellation, or -1
    private final Set<Integer> getrokken = new HashSet<>();       // Sterrenbeeld.sleutel of the lines drawn
    private final List<Integer> volgorde = new ArrayList<>();     // the lines in the order drawn (a, b, a, b...)
    private int gekozen = -1;
    private int fouten;
    private int foutA = -1, foutB = -1, foutTijd;
    private int feestTijd = -1;
    private int tick;
    private boolean verstuurd;
    private int left, top, cx, cy;

    public TelescoopScherm(Sterrenbeeld beeld, int seed, boolean nieuw, int vannacht) {
        super(Component.translatable("gui.guhs.sterrenwacht.telescoop"));
        this.beeld = beeld;
        this.nieuw = nieuw;
        this.vannacht = vannacht;
        maakHemel(new Random(seed));
    }

    /** The constellation (scaled, turned a little, somewhere in the view) among 40-55 other stars. */
    private void maakHemel(Random rng) {
        float maat = 88 + rng.nextFloat() * 36;
        double hoek = Math.toRadians(rng.nextFloat() * 30 - 15);
        float ox = (rng.nextFloat() - 0.5f) * (2 * R - maat - 28) * 0.7f;
        float oy = (rng.nextFloat() - 0.5f) * (2 * R - maat - 28) * 0.7f;
        for (int i = 0; i < beeld.sterren(); i++) {
            float[] s = beeld.ster(i);
            double x = (s[0] - 0.5) * maat, y = (s[1] - 0.5) * maat;
            double rx = x * Math.cos(hoek) - y * Math.sin(hoek), ry = x * Math.sin(hoek) + y * Math.cos(hoek);
            sterren.add(new float[]{(float) rx + ox, (float) ry + oy, 0.75f + rng.nextFloat() * 0.25f, rng.nextFloat() * 6.28f});
            beeldIndex.add(i);
        }
        int extra = 40 + rng.nextInt(16);
        for (int tries = 0; extra > 0 && tries < 4000; tries++) {
            float x = (rng.nextFloat() * 2 - 1) * (R - 8), y = (rng.nextFloat() * 2 - 1) * (R - 8);
            if (x * x + y * y > (R - 8) * (R - 8)) {
                continue;
            }
            boolean vrij = true;
            for (float[] s : sterren) {
                if ((s[0] - x) * (s[0] - x) + (s[1] - y) * (s[1] - y) < 11 * 11) {
                    vrij = false;
                    break;
                }
            }
            if (vrij) {
                sterren.add(new float[]{x, y, 0.35f + rng.nextFloat() * 0.65f, rng.nextFloat() * 6.28f});
                beeldIndex.add(-1);
                extra--;
            }
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        cx = left + 8 + R;
        cy = top + 12 + R;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.sterrenwacht.opnieuw"), b -> opnieuw())
                .bounds(left + W - 96, top + H - 50, 88, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.sterrenwacht.stoppen"), b -> onClose())
                .bounds(left + W - 96, top + H - 28, 88, 18).build());
    }

    private void opnieuw() {
        if (feestTijd < 0) {
            getrokken.clear();
            volgorde.clear();
            gekozen = -1;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (button != 0 || feestTijd >= 0) {
            return false;
        }
        int ster = sterBij(mouseX, mouseY);
        if (ster < 0) {
            return false;
        }
        if (gekozen < 0 || gekozen == ster) {
            gekozen = gekozen == ster ? -1 : ster;
            geluid(1.4f);
            return true;
        }
        int a = beeldIndex.get(gekozen), b = beeldIndex.get(ster);
        if (a >= 0 && b >= 0 && beeld.isLijn(a, b)) {
            if (getrokken.add(Sterrenbeeld.sleutel(a, b))) {
                volgorde.add(a);
                volgorde.add(b);
                geluid(1.0f + 0.08f * getrokken.size());
            }
            gekozen = ster;
            if (getrokken.size() == beeld.sleutels().size()) {
                klaar();
            }
        } else {
            fouten++;
            foutA = gekozen;
            foutB = ster;
            foutTijd = 16;
            gekozen = -1;
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7f, 0.6f));
            }
        }
        return true;
    }

    private void klaar() {
        feestTijd = 0;
        gekozen = -1;
        verstuurd = true;
        ClientPacketDistributor.sendToServer(new SterrenwachtPayloads.Klaar(beeld.ordinal(), List.copyOf(volgorde), fouten));
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SterrenwachtFeature.STERRENBEELD_GELUID.get(), 1.2f, 0.8f));
        }
    }

    private void geluid(float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SterrenwachtFeature.STER_KLIK.get(), pitch, 0.7f));
        }
    }

    private int sterBij(double mx, double my) {
        int best = -1;
        double bestD = 7 * 7;
        for (int i = 0; i < sterren.size(); i++) {
            float[] s = sterren.get(i);
            double d = (cx + s[0] - mx) * (cx + s[0] - mx) + (cy + s[1] - my) * (cy + s[1] - my);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    @Override
    public void tick() {
        tick++;
        if (foutTijd > 0) {
            foutTijd--;
        }
        if (feestTijd >= 0 && ++feestTijd > 60) {
            onClose();
        } else if (feestTijd < 0 && tick >= Sterrenkijken.KIJK_TICKS) {
            // the look took too long: the stars have turned away (the server forgets the look too)
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendSystemMessage(Component.translatable("gui.guhs.sterrenwacht.te_laat")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            onClose();
        }
    }

    /** Closed any way (the button, Esc, another screen, the autocheck): the server ends the look too, if it wasn't done. */
    @Override
    public void removed() {
        if (!verstuurd) {
            verstuurd = true;
            if (minecraft != null && minecraft.getConnection() != null) {
                ClientPacketDistributor.sendToServer(new SterrenwachtPayloads.Stop());
            }
        }
        super.removed();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        g.fill(left, top, left + W, top + H, PANEL);
        float t = tick + partialTick;
        // the round view: a night sky getting a little lighter towards the horizon, the pink moon
        for (int dy = -R; dy <= R; dy++) {
            int half = (int) Math.sqrt(Math.max(0, R * R - dy * dy));
            float f = (dy + R) / (2f * R);
            int r = (int) Mth.lerp(f, 18, 52), gr = (int) Mth.lerp(f, 14, 26), b = (int) Mth.lerp(f, 48, 86);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, 0xFF000000 | (r << 16) | (gr << 8) | b);
        }
        int mx = cx + R / 2 + 10, my = cy - R / 2 - 6;
        for (int dy = -7; dy <= 7; dy++) {
            int half = (int) Math.sqrt(49 - dy * dy);
            g.fill(mx - half, my + dy, mx + half + 1, my + dy + 1, 0xFFF7B6D8);
        }
        g.fill(mx - 3, my - 2, mx - 1, my, 0xFFE89AC0);
        g.fill(mx + 2, my + 2, mx + 4, my + 4, 0xFFE89AC0);
        // the lines drawn
        for (int i = 0; i + 1 < volgorde.size(); i += 2) {
            float[] a = sterVanBeeld(volgorde.get(i)), b = sterVanBeeld(volgorde.get(i + 1));
            int kleur = feestTijd >= 0 && (feestTijd / 4) % 2 == 0 ? 0xFFFFFFFF : GOUD;
            lijn(g, cx + a[0], cy + a[1], cx + b[0], cy + b[1], kleur);
        }
        if (foutTijd > 0 && foutA >= 0 && foutB >= 0) {
            float[] a = sterren.get(foutA), b = sterren.get(foutB);
            lijn(g, cx + a[0], cy + a[1], cx + b[0], cy + b[1], (foutTijd * 12 << 24) | (ROOD & 0xFFFFFF));
        }
        // the stars (twinkling)
        for (int i = 0; i < sterren.size(); i++) {
            float[] s = sterren.get(i);
            float glans = s[2] * (0.75f + 0.25f * Mth.sin(t * 0.12f + s[3]));
            int x = Math.round(cx + s[0]), y = Math.round(cy + s[1]);
            int a = (int) (255 * Mth.clamp(glans, 0.2f, 1f));
            int kleur = (a << 24) | 0xFFF6D8;
            g.fill(x, y, x + 1, y + 1, kleur);
            if (glans > 0.55f) {
                int zacht = ((a / 2) << 24) | 0xFFF6D8;
                g.fill(x - 1, y, x, y + 1, zacht);
                g.fill(x + 1, y, x + 2, y + 1, zacht);
                g.fill(x, y - 1, x + 1, y, zacht);
                g.fill(x, y + 1, x + 1, y + 2, zacht);
            }
            if (i == gekozen) {
                ring(g, x, y, 4, 0xFFFFD86A);
            } else if (Math.abs(mouseX - x) <= 5 && Math.abs(mouseY - y) <= 5 && feestTijd < 0) {
                ring(g, x, y, 4, 0x80FFFFFF);
            }
        }
        // the eyepiece's rim
        ringDik(g, cx, cy, R + 1, 0xFF8C7AD8);
        renderKaart(g);
    }

    private float[] sterVanBeeld(int beeldI) {
        for (int i = 0; i < sterren.size(); i++) {
            if (beeldIndex.get(i) == beeldI) {
                return sterren.get(i);
            }
        }
        return new float[]{0, 0};
    }

    /** The card on the right: which constellation, its shape, tonight's count and the mistakes. */
    private void renderKaart(GuiGraphicsExtractor g) {
        int x = left + 8 + 2 * R + 10, w = left + W - 8 - x;
        int y = top + 10;
        g.text(font, Component.translatable("gui.guhs.sterrenwacht.zoek").withStyle(ChatFormatting.ITALIC), x, y, 0xFFB8A8E8, false);
        y += 11;
        List<FormattedCharSequence> naam = font.split(beeld.naam().copy().withStyle(ChatFormatting.BOLD), w);
        for (FormattedCharSequence line : naam) {
            g.text(font, line, x, y, beeld.zeldzaam ? GOUD : TEKST, false);
            y += 10;
        }
        if (nieuw) {
            g.text(font, Component.translatable("gui.guhs.sterrenwacht.nieuw"), x, y, 0xFFFF9AD0, false);
            y += 10;
        }
        // the shape
        int box = Math.min(w, 58), bx = x + (w - box) / 2, by = y + 3;
        g.fill(bx - 1, by - 1, bx + box + 1, by + box + 1, 0xFF3A2E6A);
        g.fill(bx, by, bx + box, by + box, 0xFF1A1236);
        for (int[] l : beeld.lijnen()) {
            float[] a = beeld.ster(l[0]), b = beeld.ster(l[1]);
            boolean al = getrokken.contains(Sterrenbeeld.sleutel(l[0], l[1]));
            lijn(g, bx + 4 + a[0] * (box - 8), by + 4 + a[1] * (box - 8), bx + 4 + b[0] * (box - 8), by + 4 + b[1] * (box - 8),
                    al ? GOUD : 0xFF7A70B0);
        }
        for (int i = 0; i < beeld.sterren(); i++) {
            float[] s = beeld.ster(i);
            int sx = Math.round(bx + 4 + s[0] * (box - 8)), sy = Math.round(by + 4 + s[1] * (box - 8));
            g.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xFFFFF6D8);
        }
        y = by + box + 6;
        g.text(font, Component.translatable("gui.guhs.sterrenwacht.lijnen", getrokken.size(), beeld.sleutels().size()), x, y, TEKST, false);
        y += 10;
        g.text(font, Component.translatable("gui.guhs.sterrenwacht.fouten", fouten), x, y, fouten > 0 ? 0xFFFFA0A8 : TEKST, false);
        y += 10;
        g.text(font, Component.translatable("gui.guhs.sterrenwacht.vannacht", vannacht, Sterrenkijken.MAX_PER_NACHT), x, y, 0xFFB8A8E8, false);
        if (feestTijd >= 0) {
            Component vahoeg = Component.translatable("gui.guhs.sterrenwacht.vahoeg").withStyle(ChatFormatting.BOLD);
            int tw = font.width(vahoeg);
            g.fill(cx - tw / 2 - 6, cy + R - 30, cx + tw / 2 + 6, cy + R - 16, 0xC0301A40);
            g.text(font, vahoeg, cx - tw / 2, cy + R - 27, GOUD, false);
        } else {
            List<FormattedCharSequence> hint = font.split(Component.translatable("gui.guhs.sterrenwacht.hint"), 2 * R - 20);
            int hy = top + H - 4 - hint.size() * 9;
            for (FormattedCharSequence line : hint) {
                g.text(font, line, left + 8 + R - font.width(line) / 2, hy, 0xFF9A90C8, false);
                hy += 9;
            }
        }
    }

    private static void lijn(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int kleur) {
        int steps = (int) Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= steps; i++) {
            float f = i / (float) steps;
            int x = Math.round(Mth.lerp(f, x0, x1)), y = Math.round(Mth.lerp(f, y0, y1));
            g.fill(x, y, x + 1, y + 1, kleur);
        }
    }

    private static void ring(GuiGraphicsExtractor g, int x, int y, int r, int kleur) {
        for (int a = 0; a < 24; a++) {
            double h = a * Math.PI * 2 / 24;
            int px = x + (int) Math.round(Math.cos(h) * r), py = y + (int) Math.round(Math.sin(h) * r);
            g.fill(px, py, px + 1, py + 1, kleur);
        }
    }

    private static void ringDik(GuiGraphicsExtractor g, int x, int y, int r, int kleur) {
        for (int a = 0; a < 360; a++) {
            double h = Math.toRadians(a);
            int px = x + (int) Math.round(Math.cos(h) * r), py = y + (int) Math.round(Math.sin(h) * r);
            g.fill(px - 1, py - 1, px + 1, py + 1, kleur);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
