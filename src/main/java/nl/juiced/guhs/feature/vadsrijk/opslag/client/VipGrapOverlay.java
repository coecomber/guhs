package nl.juiced.guhs.feature.vadsrijk.opslag.client;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;

/**
 * U-OPSLAGVIP: the "WORD GUH VIP++" joke, drawn ON TOP of the storage screen (no screen switch, so the storage menu stays open).
 * A parody of mobile-game microtransaction pop-ups: flashing title, struck-through "50 EURO", a pulsing red arrow at a giant
 * "KOOP NU!!!" button, gold borders, sparkles + confetti, a fake countdown, badges, a "97% KORTING" sticker and a jingle.
 * The buttons do nothing (the overlay shakes). After {@link #GRAP_MS} it turns into a normal dialogue in the RijkPraatScherm
 * style (Winkelguh Snoes) with the real coin price: "Kopen" (= the buy confirmation) / "Nee, dank je".
 * <p>Esc during the joke skips to the dialogue; Esc in the dialogue = "Nee, dank je".
 */
public final class VipGrapOverlay {
    static final long GRAP_MS = 10_000;
    private static final int GOUD = 0xFFFFC83A, GOUD_LICHT = 0xFFFFF0A0, GOUD_DONKER = 0xFFB07A10, ROOD = 0xFFFF2A2A;
    private static final int PANEEL = 0xF0301A26, ROZE = 0xFFFFB6D8, BALLON = 0xFFFFF4F8, TEKST = 0xFF3A1C30;
    private static final int[] REGENBOOG = {0xFFFF3B3B, 0xFFFFC83A, 0xFFFFFF55, 0xFF55FF7F, 0xFF55D8FF, 0xFFFF7FE0};
    /** The jingle: (ms after the start, note pitch). A cheesy rising arpeggio, then the cash register. */
    private static final float[][] JINGLE = {{0, 1.0f}, {130, 1.26f}, {260, 1.5f}, {390, 2.0f}, {560, 1.5f}, {690, 2.0f}};

    public enum Fase { GRAP, DIALOOG, KLAAR }

    private final long prijs;
    private final Runnable koop;
    private final long start = Util.getMillis();
    private Fase fase = Fase.GRAP;
    private long schud = -10_000, dialoogStart;
    private int jingle;
    private boolean kaching;
    @Nullable
    private LivingEntity snoes;
    // last layout (for clicks)
    private int koopX, koopY, koopB, koopH, neeX, neeY, neeB, neeH;
    private int dKoopX, dKoopY, dNeeX, dNeeY, dKnopB, dKnopH;

    VipGrapOverlay(long prijs, Runnable koop) {
        this.prijs = prijs;
        this.koop = koop;
    }

    public Fase fase() {
        return fase;
    }

    public boolean actief() {
        return fase != Fase.KLAAR;
    }

    private static void geluid(net.minecraft.sounds.SoundEvent s, float toon, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, toon, volume));
    }

    private void tik(long t) {
        while (jingle < JINGLE.length && t >= JINGLE[jingle][0]) {
            geluid(SoundEvents.NOTE_BLOCK_BELL.value(), JINGLE[jingle][1], 0.7f);
            geluid(SoundEvents.NOTE_BLOCK_CHIME.value(), JINGLE[jingle][1], 0.4f);
            jingle++;
        }
        if (!kaching && t >= 900) {
            kaching = true;
            kassa();
        }
        if (fase == Fase.GRAP && t >= GRAP_MS) {
            naarDialoog();
        }
    }

    private static void kassa() {
        geluid(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.6f, 0.8f);
        geluid(SoundEvents.NOTE_BLOCK_BELL.value(), 2.0f, 0.6f);
        try {
            geluid(nl.juiced.guhs.feature.guhpixel.GuhpixelFeature.MUNTJE.get(), 1.0f, 0.9f);
        } catch (RuntimeException e) {
            // (no coin sound: the bell is enough)
        }
    }

    private void naarDialoog() {
        fase = Fase.DIALOOG;
        dialoogStart = Util.getMillis();
        geluid(SoundEvents.BUBBLE_POP, 1.3f, 0.6f);
    }

    // ---- drawing -------------------------------------------------------------------------------------------------------------

    void teken(GuiGraphicsExtractor g, Font font, int w, int h, int mx, int my) {
        long t = Util.getMillis() - start;
        tik(t);
        g.nextStratum();
        if (fase == Fase.GRAP) {
            tekenGrap(g, font, w, h, mx, my, t);
        } else if (fase == Fase.DIALOOG) {
            tekenDialoog(g, font, w, h, mx, my);
        }
    }

    private void tekenGrap(GuiGraphicsExtractor g, Font font, int w, int h, int mx, int my, long t) {
        float sec = t / 1000f;
        long sinds = Util.getMillis() - schud;
        float dx = sinds < 450 ? (float) Math.sin(sinds * 0.09) * 9 * (1 - sinds / 450f) : 0;
        g.pose().pushMatrix();
        g.pose().translate(dx, 0);
        // background: a dark purple "casino" with flashing stripes
        g.fill(-20, -20, w + 20, h + 20, 0xF2200A2E);
        int streep = (int) (t / 90) % 2;
        for (int y = -20 + streep * 10; y < h + 20; y += 20) {
            g.fill(-20, y, w + 20, y + 10, 0x182A0F40);
        }
        // the gold border (thick, with blinking "light bulbs")
        int r = 6;
        g.fill(0, 0, w, r, GOUD);
        g.fill(0, h - r, w, h, GOUD);
        g.fill(0, 0, r, h, GOUD);
        g.fill(w - r, 0, w, h, GOUD);
        g.fill(r, r, w - r, r + 1, GOUD_DONKER);
        g.fill(r, h - r - 1, w - r, h - r, GOUD_DONKER);
        int lamp = (int) (t / 160) % 2;
        for (int x = 8; x < w - 8; x += 14) {
            boolean aan = ((x / 14) + lamp) % 2 == 0;
            g.fill(x, 1, x + 4, 5, aan ? GOUD_LICHT : GOUD_DONKER);
            g.fill(x, h - 5, x + 4, h - 1, aan ? GOUD_LICHT : GOUD_DONKER);
        }
        for (int y = 8; y < h - 8; y += 14) {
            boolean aan = ((y / 14) + lamp) % 2 == 1;
            g.fill(1, y, 5, y + 4, aan ? GOUD_LICHT : GOUD_DONKER);
            g.fill(w - 5, y, w - 1, y + 4, aan ? GOUD_LICHT : GOUD_DONKER);
        }
        // confetti (falling) + sparkles (twinkling)
        for (int i = 0; i < 70; i++) {
            float val = (sec * (40 + (i * 37) % 50) + i * 53) % (h + 20) - 10;
            int kleur = REGENBOOG[i % REGENBOOG.length];
            int x = (int) ((i * 97 + 13) % Math.max(1, w) + Math.sin(sec * 1.7f + i) * 8);
            int y = (int) val;
            boolean breed = (i + (int) (sec * 6)) % 2 == 0;
            g.fill(x, y, x + (breed ? 4 : 2), y + (breed ? 2 : 4), kleur);
            if (i % 3 == 0) {
                int fx = (int) ((i * 151 + 7) % Math.max(1, w)), fy = (int) ((i * 89 + 31) % Math.max(1, h));
                float fonk = (float) Math.sin(sec * 6 + i * 1.3f);
                if (fonk > 0.2f) {
                    int s = fonk > 0.8f ? 3 : 2;
                    g.fill(fx - s, fy, fx + s + 1, fy + 1, 0xFFFFFFFF);
                    g.fill(fx, fy - s, fx + 1, fy + s + 1, 0xFFFFFFFF);
                }
            }
        }
        int cx = w / 2;
        float ruimte = Math.min(1f, h / 300f);
        // badges in the corners
        Component badge = Component.translatable("vadsrijk.opslag.vip.badge").withStyle(ChatFormatting.BOLD);
        tekenBadge(g, font, badge, 14, 14, t, 0xFFE0301E);
        tekenBadge(g, font, badge, w - 14 - font.width(badge) - 12, 14, t + 300, 0xFFE0301E);
        // the title: huge, flashing, pulsing
        float puls = 1f + 0.06f * (float) Math.sin(sec * 9);
        float titelSchaal = Math.min(4.2f, (w - 40) / (float) Math.max(1, font.width(Component.translatable("vadsrijk.opslag.vip.titel")))) * puls * Math.max(0.6f, ruimte);
        int titelKleur = REGENBOOG[(int) (t / 120) % REGENBOOG.length];
        int ty = (int) (h * 0.13f);
        midden(g, font, Component.translatable("vadsrijk.opslag.vip.titel").withStyle(ChatFormatting.BOLD), cx, ty, titelSchaal, titelKleur);
        // VAN ~~50 EURO~~ VOOR MAAR 34,99!!!
        Component oud = Component.translatable("vadsrijk.opslag.vip.oud").withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.GRAY);
        Component van = Component.translatable("vadsrijk.opslag.vip.van", oud).withStyle(ChatFormatting.BOLD);
        float vanSchaal = Math.min(2.2f, (w - 30) / (float) Math.max(1, font.width(van))) * Math.max(0.7f, ruimte);
        int vy = ty + (int) (9 * titelSchaal) + 8;
        midden(g, font, van, cx, vy, vanSchaal, (t / 250) % 2 == 0 ? 0xFFFFFF55 : 0xFFFFFFFF);
        // the benefits (useless)
        int ly = vy + (int) (9 * vanSchaal) + 8;
        for (int i = 1; i <= 3; i++) {
            Component c = Component.translatable("vadsrijk.opslag.vip.voordeel" + i);
            g.text(font, c, cx - font.width(c) / 2, ly, 0xFF9CFFB0, true);
            ly += 11;
        }
        // the countdown
        int rest = (int) Math.max(0, 9 - sec);
        Component klok = Component.translatable("vadsrijk.opslag.vip.klok", String.format("00:0%d", rest)).withStyle(ChatFormatting.BOLD);
        midden(g, font, klok, cx, ly + 4, 1.6f, (t / 200) % 2 == 0 ? ROOD : 0xFFFFFFFF);
        // the giant button
        koopB = Math.min(220, w - 120);
        koopH = 34;
        koopX = cx - koopB / 2;
        koopY = Math.min(h - 70, ly + 26);
        float knopPuls = 1f + 0.05f * (float) Math.sin(sec * 11);
        boolean boven = in(mx, my, koopX, koopY, koopB, koopH);
        g.pose().pushMatrix();
        g.pose().translate(cx, koopY + koopH / 2f);
        g.pose().scale(knopPuls, knopPuls);
        int bx = -koopB / 2, by = -koopH / 2;
        g.fill(bx - 3, by - 3, bx + koopB + 3, by + koopH + 3, GOUD);
        g.fill(bx - 1, by - 1, bx + koopB + 1, by + koopH + 1, GOUD_DONKER);
        g.fill(bx, by, bx + koopB, by + koopH, boven ? 0xFF3BE05A : 0xFF20B23E);
        g.fill(bx, by, bx + koopB, by + 4, 0x60FFFFFF);
        g.pose().popMatrix();
        midden(g, font, Component.translatable("vadsrijk.opslag.vip.koop").withStyle(ChatFormatting.BOLD), cx, koopY + koopH / 2 - (int) (4.5f * 2.2f * knopPuls),
                2.2f * knopPuls, 0xFFFFFFFF);
        // the pulsing red arrow pointing at it (from the left)
        int bob = (int) (Math.abs(Math.sin(sec * 7)) * 12);
        int punt = koopX - 10 - bob, ay = koopY + koopH / 2;
        int staart = Math.max(14, punt - 70);
        g.fill(staart, ay - 5, punt - 14, ay + 6, ROOD);
        for (int i = 0; i < 16; i++) {
            g.fill(punt - 16 + i, ay - 15 + i, punt - 15 + i, ay + 16 - i, ROOD);
        }
        g.fill(staart, ay - 5, staart + 3, ay + 6, 0xFFB01010);
        // the fake sticker, tilted, on the button's corner
        g.pose().pushMatrix();
        g.pose().translate(koopX + koopB - 6, koopY - 4);
        g.pose().rotate((float) Math.toRadians(-14 + Math.sin(sec * 5) * 4));
        Component sticker = Component.translatable("vadsrijk.opslag.vip.sticker").withStyle(ChatFormatting.BOLD);
        int sw = font.width(sticker) + 10;
        g.fill(-sw / 2 - 1, -9, sw / 2 + 1, 9, 0xFFFFFFFF);
        g.fill(-sw / 2, -8, sw / 2, 8, 0xFFFF2A6A);
        g.text(font, sticker, -font.width(sticker) / 2, -4, 0xFFFFFFFF, false);
        g.pose().popMatrix();
        // "nee bedankt" (guilt trip, small)
        Component nee = Component.translatable("vadsrijk.opslag.vip.nee").withStyle(ChatFormatting.UNDERLINE);
        neeB = font.width(nee);
        neeH = 10;
        neeX = cx - neeB / 2;
        neeY = koopY + koopH + 10;
        g.text(font, nee, neeX, neeY, in(mx, my, neeX, neeY, neeB, neeH) ? 0xFFDDDDDD : 0xFF9A8AA8, false);
        // social proof + "only today"
        Component kopers = Component.translatable("vadsrijk.opslag.vip.kopers", String.valueOf(9000 + (t / 37) % 999));
        g.text(font, kopers, cx - font.width(kopers) / 2, neeY + 14, 0xFFFFA0A0, true);
        Component alleen = Component.translatable("vadsrijk.opslag.vip.alleen").withStyle(ChatFormatting.ITALIC);
        g.text(font, alleen, cx - font.width(alleen) / 2, neeY + 26, GOUD_LICHT, true);
        // the disclaimer: clearly a joke
        Component parodie = Component.translatable("vadsrijk.opslag.vip.parodie");
        g.text(font, parodie, cx - font.width(parodie) / 2, h - 18, 0xFFCFC0DA, false);
        g.pose().popMatrix();
    }

    private static void tekenBadge(GuiGraphicsExtractor g, Font font, Component badge, int x, int y, long t, int kleur) {
        boolean flits = (t / 180) % 2 == 0;
        int bw = font.width(badge) + 12;
        g.fill(x - 1, y - 1, x + bw + 1, y + 15, GOUD);
        g.fill(x, y, x + bw, y + 14, flits ? kleur : 0xFFFF7A1E);
        g.text(font, badge, x + 6, y + 3, 0xFFFFFFFF, true);
    }

    /** Text centred on cx at y, scaled. */
    private static void midden(GuiGraphicsExtractor g, Font font, Component c, int cx, int y, float schaal, int kleur) {
        g.pose().pushMatrix();
        g.pose().translate(cx, y);
        g.pose().scale(schaal, schaal);
        g.text(font, c, -font.width(c) / 2, 0, kleur, true);
        g.pose().popMatrix();
    }

    private void tekenDialoog(GuiGraphicsExtractor g, Font font, int w, int h, int mx, int my) {
        g.fill(-20, -20, w + 20, h + 20, 0x90000000);
        int sw = Math.min(330, w - 20), sh = 112;
        int sx = (w - sw) / 2, sy = h - sh - 14;
        g.fill(sx - 1, sy - 1, sx + sw + 1, sy + sh + 1, ROZE);
        g.fill(sx, sy, sx + sw, sy + sh, PANEEL);
        // the name plate
        Component naam = Component.translatable("vadsrijk.opslag.vip.naam").withStyle(ChatFormatting.BOLD);
        int nw = font.width(naam) + 10;
        g.fill(sx + 8, sy + 4, sx + 8 + nw, sy + 17, ROZE);
        g.text(font, naam, sx + 13, sy + 7, 0xFF2A1420, false);
        g.text(font, Component.translatable("vadsrijk.opslag.vip.plek"), sx + 8 + nw + 5, sy + 7, 0xC0FFB6D8, false);
        // the portrait: a sweet shop guh with a bow tie
        int pic = 56, px = sx + 8, py = sy + 22;
        g.fill(px - 1, py - 1, px + pic + 1, py + pic + 1, 0xA0FFB6D8);
        g.fill(px, py, px + pic, py + pic, 0x30FFB6D8);
        LivingEntity guh = snoes();
        if (guh != null) {
            float tijd = (Util.getMillis() - dialoogStart) / 1000f;
            int hop = -(int) Math.round(Math.abs(Math.sin(tijd * 4.5f)) * 2);
            float hoog = guh.getBbHeight() / Math.max(0.01f, guh.getScale());
            int maat = Math.max(8, Math.round(pic / 1.65f));
            float xHoek = Mth.clamp((float) Math.atan((px + pic / 2f - mx) / 40f), -0.55f, 0.55f);
            float yHoek = Mth.clamp((float) Math.atan((py + pic / 2f - my) / 40f), -0.35f, 0.35f);
            g.enableScissor(px, py, px + pic, py + pic);
            InventoryScreen.renderEntityInInventoryFollowsAngle(g, px, py + hop, px + pic, py + pic + hop, maat, 0.62f - hoog / 2f, xHoek, yHoek, guh);
            g.disableScissor();
        }
        // the balloon, the text types itself
        int bx = px + pic + 8, by = py, bw = sx + sw - 8 - bx, bh = sh - 22 - 30;
        g.fill(bx, by, bx + bw, by + bh, BALLON);
        g.fill(bx - 4, by + 12, bx, by + 18, BALLON);
        String tekst = Component.translatable("vadsrijk.opslag.vip.tekst", nl.juiced.guhs.feature.vadsrijk.kern.Spelers.munten(prijs)).getString();
        int zicht = (int) Math.min(tekst.length(), (Util.getMillis() - dialoogStart) / 22);
        int y = by + 5;
        for (FormattedCharSequence regel : font.split(Component.literal(tekst.substring(0, zicht)), bw - 10)) {
            g.text(font, regel, bx + 5, y, TEKST, false);
            y += 10;
        }
        // the two answers
        dKnopB = (sw - 16 - pic - 8 - 6) / 2;
        dKnopH = 20;
        dKoopX = bx;
        dNeeX = bx + dKnopB + 6;
        dKoopY = dNeeY = sy + sh - 8 - dKnopH;
        knop(g, font, Component.literal("1  ").append(Component.translatable("vadsrijk.opslag.vip.kopen")), dKoopX, dKoopY, dKnopB, dKnopH,
                in(mx, my, dKoopX, dKoopY, dKnopB, dKnopH), 0xFF6BD68A);
        knop(g, font, Component.literal("2  ").append(Component.translatable("vadsrijk.opslag.vip.nee_dank")), dNeeX, dNeeY, dKnopB, dKnopH,
                in(mx, my, dNeeX, dNeeY, dKnopB, dKnopH), 0xFFC8A8B8);
    }

    private static void knop(GuiGraphicsExtractor g, Font font, Component c, int x, int y, int b, int h, boolean boven, int streep) {
        g.fill(x - 1, y - 1, x + b + 1, y + h + 1, boven ? 0xFFFFFFFF : 0xFF7A4A66);
        g.fill(x, y, x + b, y + h, boven ? 0xFF5A2E48 : 0xFF3E2034);
        g.fill(x + 1, y + 1, x + 3, y + h - 1, streep);
        g.text(font, c, x + 8, y + (h - 8) / 2, 0xFFFFFFFF, false);
    }

    @Nullable
    private LivingEntity snoes() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        if (snoes != null && snoes.level() == mc.level) {
            return snoes;
        }
        try {
            if (nl.juiced.guhs.registry.ModEntities.GUH.get().create(mc.level, EntitySpawnReason.LOAD) instanceof nl.juiced.guhs.entity.GuhEntity guh) {
                guh.wear(nl.juiced.guhs.entity.GuhClothes.RED_BOWTIE);
                guh.wear(nl.juiced.guhs.entity.GuhClothes.STRAW_HAT);
                guh.setYRot(180f);
                guh.yBodyRot = 180f;
                guh.yHeadRot = 180f;
                snoes = guh;
            }
        } catch (RuntimeException e) {
            snoes = null;   // (no portrait rather than a crash)
        }
        return snoes;
    }

    private static boolean in(double mx, double my, int x, int y, int b, int h) {
        return mx >= x && mx < x + b && my >= y && my < y + h;
    }

    // ---- input (the screen forwards everything while the overlay is active) --------------------------------------------------

    void klik(double mx, double my) {
        if (fase == Fase.GRAP) {
            if (in(mx, my, koopX - 6, koopY - 6, koopB + 12, koopH + 12) || in(mx, my, neeX, neeY, neeB, neeH)) {
                schud = Util.getMillis();
                geluid(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, 0.9f);
                geluid(SoundEvents.VILLAGER_NO, 1.4f, 0.5f);
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.sendOverlayMessage(Component.translatable("vadsrijk.opslag.vip.niks").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            return;
        }
        if (fase == Fase.DIALOOG) {
            if (in(mx, my, dKoopX, dKoopY, dKnopB, dKnopH)) {
                kopen();
            } else if (in(mx, my, dNeeX, dNeeY, dKnopB, dKnopH)) {
                nee();
            }
        }
    }

    /** A key while active: Esc skips the joke / says no; 1 = buy, 2 = no in the dialogue. Always swallowed. */
    void toets(int key) {
        if (fase == Fase.GRAP) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                naarDialoog();
            }
            return;
        }
        if (fase == Fase.DIALOOG) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_1 || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                kopen();
            } else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_2 || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                nee();
            }
        }
    }

    private void kopen() {
        fase = Fase.KLAAR;
        koop.run();
    }

    private void nee() {
        fase = Fase.KLAAR;
        geluid(SoundEvents.BOOK_PAGE_TURN, 1.2f, 0.8f);
    }
}
