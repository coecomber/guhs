package nl.juiced.guhs.feature.guhwinter.spellen.client;

import java.util.List;
import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhwinter.spellen.Oliebollen;
import nl.juiced.guhs.feature.guhwinter.spellen.SpellenPayloads;
import org.lwjgl.glfw.GLFW;

/**
 * Oliebollen bakken on the screen (see {@link Oliebollen}): a ladle with dough swings over the pan (or over the basket of
 * Lin's airfryer); space or a click on the ladle drops a scoop in the spot under it. Each oliebol cooks from pale to golden
 * to brown (and then, oh no, black): click it (or press 1-4) to turn it once its underside is golden, and click it again
 * to lift it out when the other side is golden too. Every oliebol scores up to {@value #PER_BOL} points for how golden its
 * two sides came out; a game lasts {@value Oliebollen#DUUR} seconds and scores at most 100.
 */
public class OliebollenScherm extends Screen {
    static final int B = 200, H = 140, PLEKKEN = 4, PER_BOL = 8;
    static final int[] PLEK_X = {52, 82, 112, 142};
    static final int PLEK_Y = 88, STRAAL = 9;
    private static final int TICKS = Oliebollen.DUUR * 20;

    private final int spel;
    private final boolean airfryer;
    private final Random dobbel;
    private final Bol[] pan = new Bol[PLEKKEN];
    private final float[] vuur = new float[PLEKKEN];
    private int tick, gebakken, verbrand, misser;
    private float punten;
    private int valX = -1, valT, spetterX = -1, spetterT;
    private boolean klaar, verstuurd;
    private float schaal = 3f;
    private int ox, oy;
    private Button knop;

    /** One oliebol in the pan: which side is down (0 first, 1 turned) and how done the side that's down is. */
    private static final class Bol {
        int kant;
        float gaar, eerste;
        int rook;
    }

    public OliebollenScherm(SpellenPayloads.OlieOpen p) {
        super(Component.translatable(p.airfryer() ? "gui.guhs.guhwinter_spellen.olie.titel_airfryer" : "gui.guhs.guhwinter_spellen.olie.titel"));
        this.spel = p.spel();
        this.airfryer = p.airfryer();
        this.dobbel = new Random(p.seed());
        for (int i = 0; i < PLEKKEN; i++) {
            vuur[i] = 1f / (20f * (4.5f + dobbel.nextFloat() * 2f));      // 4.5-6.5 seconds a side
        }
    }

    @Override
    protected void init() {
        schaal = 1f;
        for (float s : new float[] {4f, 3.5f, 3f, 2.5f, 2f, 1.5f}) {
            if (B * s + 20 <= width && H * s + 70 <= height) {
                schaal = s;
                break;
            }
        }
        ox = (width - Math.round(B * schaal)) / 2;
        oy = (height - Math.round(H * schaal)) / 2 - 10;
        knop = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhwinter_spellen.olie.klaar_knop"), b -> onClose())
                .bounds(width / 2 - 50, oy + Math.round(H * schaal) + 28, 100, 20).build());
        knop.visible = klaar;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- the game ----------------------------------------------------------------------------------------------------------

    /** Where the ladle is now (game pixels). */
    private float lepelX() {
        return B / 2f - 5 + Mth.sin(tick * 0.13f) * 66f;
    }

    /** 0..1: how golden a side is (1 = just right, 0 = still pale or too dark). */
    static float kwaliteit(float g) {
        if (g >= 0.58f && g <= 0.80f) {
            return 1f;
        }
        if (g < 0.58f) {
            return Mth.clamp((g - 0.30f) / 0.28f, 0f, 1f);
        }
        return Mth.clamp((1.05f - g) / 0.25f, 0f, 1f);
    }

    public int score() {
        return Math.min(100, Math.round(punten));
    }

    @Override
    public void tick() {
        if (klaar) {
            return;
        }
        tick++;
        if (valT > 0 && --valT == 0) {
            landen();
        }
        if (spetterT > 0) {
            spetterT--;
        }
        for (int i = 0; i < PLEKKEN; i++) {
            Bol b = pan[i];
            if (b == null) {
                continue;
            }
            b.gaar += vuur[i];
            if (b.gaar > 1.0f) {
                b.rook++;
            }
            if (b.gaar >= 1.3f) {                   // black: gone, oh no
                pan[i] = null;
                verbrand++;
                geluid(SoundEvents.FIRE_EXTINGUISH, 1.4f);
            }
        }
        if (tick % 15 == 0) {
            geluid(SoundEvents.FURNACE_FIRE_CRACKLE, 1.5f);
        }
        if (tick >= TICKS) {
            einde(true);
        }
    }

    private void schep() {
        if (klaar || valT > 0) {
            return;
        }
        valX = Math.round(lepelX());
        valT = 6;
        geluid(SoundEvents.HONEY_BLOCK_STEP, 1.2f);
    }

    private void landen() {
        for (int i = 0; i < PLEKKEN; i++) {
            if (Math.abs(valX - PLEK_X[i]) <= 11) {
                if (pan[i] == null) {
                    pan[i] = new Bol();
                    geluid(SoundEvents.GENERIC_SPLASH, 1.8f);
                    return;
                }
                break;
            }
        }
        misser++;
        spetterX = valX;
        spetterT = 12;
        geluid(SoundEvents.SLIME_SQUISH_SMALL, 1.3f);
    }

    private void klikBol(int i) {
        Bol b = pan[i];
        if (klaar || b == null) {
            return;
        }
        if (b.kant == 0) {
            b.eerste = kwaliteit(b.gaar);
            b.kant = 1;
            b.gaar = 0f;
            b.rook = 0;
            geluid(SoundEvents.ITEM_PICKUP, 0.7f);
        } else {
            float q = (b.eerste + kwaliteit(b.gaar)) / 2f;
            punten += q * PER_BOL;
            gebakken++;
            pan[i] = null;
            geluid(q > 0.9f ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.ITEM_PICKUP, q > 0.9f ? 1.4f : 1.0f);
        }
    }

    private void einde(boolean opTijd) {
        if (klaar) {
            return;
        }
        klaar = true;
        if (knop != null) {
            knop.visible = true;
        }
        verstuur(opTijd);
        geluid(SoundEvents.NOTE_BLOCK_BELL.value(), 1.2f);
    }

    private void verstuur(boolean opTijd) {
        if (!verstuurd) {
            verstuurd = true;
            ClientPacketDistributor.sendToServer(new SpellenPayloads.OlieKlaar(spel, score(), opTijd));
        }
    }

    @Override
    public void removed() {
        verstuur(false);
        super.removed();
    }

    private void geluid(SoundEvent s, float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, 0.4f));
        }
    }

    // --- input -------------------------------------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (!klaar) {
            if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
                schep();
                return true;
            }
            if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_4) {
                klikBol(key - GLFW.GLFW_KEY_1);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (klaar) {
            return false;
        }
        double gx = (event.x() - ox) / schaal, gy = (event.y() - oy) / schaal;
        for (int i = 0; i < PLEKKEN; i++) {
            if (pan[i] != null && Math.abs(gx - PLEK_X[i]) <= STRAAL + 3 && Math.abs(gy - PLEK_Y) <= STRAAL + 3) {
                klikBol(i);
                return true;
            }
        }
        if (gx >= 0 && gx < B && gy >= 0 && gy < 70) {
            schep();
            return true;
        }
        return false;
    }

    // --- drawing -----------------------------------------------------------------------------------------------------------

    private GuiGraphicsExtractor g;

    private void r(float x, float y, float b, float h, int argb) {
        g.fill(Math.round(x), Math.round(y), Math.round(x + b), Math.round(y + h), argb);
    }

    private void bol(float cx, float cy, int straal, int boven, int onder) {
        for (int dy = -straal; dy <= straal; dy++) {
            int w = (int) Math.round(Math.sqrt(straal * straal - dy * dy + 0.5));
            r(cx - w, cy + dy, w * 2, 1, dy < 1 ? boven : onder);
        }
    }

    private static int mix(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
    }

    /** The colour of a side this done: pale dough, golden, brown, black. */
    static int kleur(float g) {
        if (g < 0.68f) {
            return mix(0xFFF4E2B8, 0xFFE0A030, g / 0.68f);
        }
        if (g < 1.0f) {
            return mix(0xFFE0A030, 0xFF8A5020, (g - 0.68f) / 0.32f);
        }
        return mix(0xFF8A5020, 0xFF2A1A10, (g - 1.0f) / 0.3f);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        this.g = graphics;
        float t = tick + (klaar ? 0 : partialTick);
        int gb = Math.round(B * schaal), gh = Math.round(H * schaal);
        g.fill(ox - 3, oy - 3, ox + gb + 3, oy + gh + 3, 0xFF3A1E12);
        g.enableScissor(ox, oy, ox + gb, oy + gh);
        g.pose().pushMatrix();
        g.pose().translate(ox, oy);
        g.pose().scale(schaal, schaal);
        // the stall: a striped awning, a warm wall, the wooden counter
        r(0, 0, B, H, 0xFFFFE9D0);
        for (int x = 0; x < B; x += 20) {
            r(x, 0, 10, 14, 0xFFE0384C);
            r(x + 10, 0, 10, 14, 0xFFFFFFFF);
        }
        for (int x = 0; x < B; x += 10) {
            r(x, 14, 10, 3, (x / 10) % 2 == 0 ? 0xFFE0384C : 0xFFFFFFFF);
        }
        r(0, 112, B, 28, 0xFFB07A44);
        r(0, 112, B, 2, 0xFF7A5232);
        for (int x = 6; x < B; x += 24) {
            r(x, 120, 12, 1, 0xFF9A6838);
        }
        if (airfryer) {
            tekenAirfryer(t);
        } else {
            tekenPan(t);
        }
        // the oliebollen
        for (int i = 0; i < PLEKKEN; i++) {
            Bol b = pan[i];
            if (b == null) {
                continue;
            }
            // the top is the other side: still pale dough, or (turned) the side that cooked first, as golden as it got
            int boven = b.kant == 0 ? 0xFFF4E2B8 : mix(0xFFF4E2B8, 0xFFE0A030, b.eerste);
            bol(PLEK_X[i], PLEK_Y, STRAAL, boven, kleur(b.gaar));
            r(PLEK_X[i] - 4, PLEK_Y - 6, 2, 2, 0x60FFFFFF);
            if (b.rook > 0) {
                float h = (b.rook % 20) * 0.8f;
                r(PLEK_X[i] - 2 + Mth.sin(t * 0.3f) * 2, PLEK_Y - 14 - h, 4, 3, 0xFF707070);
                r(PLEK_X[i] + 1, PLEK_Y - 20 - h, 3, 3, 0xFF909090);
            }
            if (!klaar) {      // the key to press
                String n = Integer.toString(i + 1);
                g.pose().pushMatrix();
                g.pose().translate(PLEK_X[i] - 2, PLEK_Y + STRAAL + 4);
                g.pose().scale(0.5f, 0.5f);
                g.text(font, n, 0, 0, 0xFF5A3420, false);
                g.pose().popMatrix();
            }
        }
        // the falling scoop and a splash next to the spots
        if (valT > 0) {
            float y = 40 + (6 - valT) * 7.5f;
            bol(valX, y, 6, 0xFFFFF0D0, 0xFFF4E2B8);
        }
        if (spetterT > 0) {
            int s = 12 - spetterT;
            r(spetterX - s, PLEK_Y - 2 - s / 2f, 2, 2, 0xFFF4E2B8);
            r(spetterX + s, PLEK_Y - 2 - s / 2f, 2, 2, 0xFFF4E2B8);
            r(spetterX - 1, PLEK_Y - 6 - s, 2, 2, 0xFFF4E2B8);
        }
        // the ladle swinging over the pan
        if (!klaar) {
            float lx = lepelX();
            r(lx - 1, 18, 2, 18, 0xFF8C95A3);
            bol(lx, 40, 7, 0xFFFFF0D0, 0xFFB8BCC8);
            if (valT == 0) {
                r(lx - 5, 36, 10, 3, 0xFFFFF0D0);
            }
        }
        // the plate with what you baked, with powdered sugar
        r(162, 104, 34, 6, 0xFFFFFFFF);
        r(164, 110, 30, 2, 0xFFD0D8E8);
        for (int i = 0; i < Math.min(gebakken, 9); i++) {
            float bx = 170 + (i % 3) * 9, by = 99 - (i / 3) * 7;
            bol(bx, by, 4, 0xFFE0A030, 0xFFC08030);
            r(bx - 2, by - 3, 1, 1, 0xFFFFFFFF);
            r(bx + 1, by - 2, 1, 1, 0xFFFFFFFF);
        }
        // the time bar
        r(6, 20, B - 12, 4, 0xFF5A3420);
        r(6, 20, (B - 12) * Math.max(0, TICKS - tick) / (float) TICKS, 4, tick > TICKS - 200 ? 0xFFE0384C : 0xFFFFD34E);
        g.pose().popMatrix();
        g.disableScissor();
        // the texts round it
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), ox, oy - 12, 0xFFFFD27A, true);
        String stand = Component.translatable("gui.guhs.guhwinter_spellen.olie.stand", score(), gebakken).getString();
        g.text(font, stand, ox + gb - font.width(stand), oy - 12, 0xFFFFFFFF, true);
        Component onder = klaar ? Component.translatable(regel(), score()) : Component.translatable(airfryer
                ? "gui.guhs.guhwinter_spellen.olie.uitleg_airfryer" : "gui.guhs.guhwinter_spellen.olie.uitleg");
        List<FormattedCharSequence> regels = font.split(onder, Math.max(160, gb));
        int ty = oy + gh + 6;
        for (FormattedCharSequence rg : regels) {
            g.text(font, rg, (width - font.width(rg)) / 2, ty, klaar ? 0xFFFFD27A : 0xFFE0E0E0, true);
            ty += 10;
        }
    }

    /** The cute line at the end. */
    private String regel() {
        int s = score();
        String trap = s >= 100 ? "100" : s >= 80 ? "80" : s >= 50 ? "50" : s >= 20 ? "20" : "0";
        return "gui.guhs.guhwinter_spellen.olie.einde." + (airfryer ? "airfryer." : "pan.") + trap;
    }

    private void tekenPan(float t) {
        // the burner, the pan and its handle, the hot oil with bubbles
        r(30, 104, 140, 8, 0xFF404652);
        r(60, 108, 80, 4, 0xFFE07020);
        r(26, 70, 148, 34, 0xFF2E2E34);
        r(22, 74, 156, 26, 0xFF2E2E34);
        r(174, 82, 22, 5, 0xFF3A2416);
        r(30, 74, 140, 26, 0xFFC08A2A);
        r(26, 78, 148, 18, 0xFFC08A2A);
        for (int i = 0; i < 9; i++) {
            float bx = 34 + ((i * 37 + (int) (t * 0.7f) * 3) % 132), by = 78 + (i * 13) % 18;
            if (((int) t + i * 5) % 12 < 8) {
                r(bx, by, 2, 2, 0xFFE8B860);
            }
        }
    }

    private void tekenAirfryer(float t) {
        // a little white airfryer: the display, the basket with its window, the handle
        r(32, 30, 136, 82, 0xFFF0F0F2);
        r(32, 30, 136, 3, 0xFFFFFFFF);
        r(32, 109, 136, 3, 0xFFC8C8D0);
        r(40, 36, 30, 10, 0xFF20242C);
        g.pose().pushMatrix();
        g.pose().translate(43, 38);
        g.pose().scale(0.6f, 0.6f);
        g.text(font, "200°", 0, 0, (int) t / 10 % 2 == 0 ? 0xFF60FF90 : 0xFF40D070, false);
        g.pose().popMatrix();
        r(140, 36, 20, 10, 0xFFFF9EC4);
        r(36, 70, 128, 34, 0xFF30343C);
        r(40, 74, 120, 26, 0xFF50565F);
        for (int x = 44; x < 158; x += 6) {
            r(x, 74, 1, 26, 0xFF3A3F48);
        }
        r(80, 104, 40, 5, 0xFF20242C);
        // the warm air going round
        for (int i = 0; i < 5; i++) {
            float ax = 44 + ((i * 29 + t * 1.5f) % 112);
            r(ax, 72 + Mth.sin(t * 0.2f + i) * 1.5f, 3, 1, 0x80FFD0A0);
        }
    }
}
