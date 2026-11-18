package nl.juiced.guhs.feature.guhwinter.tekenen.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhwinter.tekenen.Doel;
import nl.juiced.guhs.feature.guhwinter.tekenen.TekenPayloads;
import nl.juiced.guhs.feature.guhwinter.tekenen.TekenPixels;

/**
 * The drawing editor: a 64x64 canvas shown big and crisp in a pink guh frame (ears on top, a little face below), the tools on
 * the left (potlood, gum, emmer, pipet, lijn; sizes 1/2/4; undo/redo, 40 steps; wissen with a question inside the screen) and
 * the colours on the right (a full colour wheel + brightness slider, guh-pink swatches, the colours you used last).
 * Left mouse draws, right mouse erases. Keys: Ctrl+Z / Ctrl+Y (or Ctrl+Shift+Z), P G E I L for the tools, 1 2 4 for the size.
 * Opslaan sends the drawing to the server and closes when the server says it's stored; Annuleren (or Esc) asks first when
 * there are changes.
 */
public class TekenScherm extends Screen {
    private static final Identifier CANVAS = Guhs.id("dynamic/guhwinter_teken_canvas");
    private static final Identifier WIEL = Guhs.id("dynamic/guhwinter_kleurwiel");
    private static final int W = TekenPixels.W, WIEL_D = 80, WIEL_TEX = 160, UNDO_MAX = 40, RECENT_MAX = 8;
    private static final int TOOLS_W = 52, KOL_W = 94, KOP = 22, VOET = 26;

    // colours of the screen
    private static final int PAPIER_UI = 0xFFFFF6EC, ROZE = 0xFFF29BC1, ROZE_DONKER = 0xFFD9679A, ROZE_LICHT = 0xFFFFD1E3, INKT = 0xFF5A3A4E,
            GRIJS = 0xFF9A8290, ROOD = 0xFFD8435A, OOR_BINNEN = 0xFFFFB8D2;

    /** The swatches: guh pinks and warm colours, cool colours, greys and browns. */
    private static final int[] KLEURTJES = {
            0xFFFFD1E3, 0xFFF7A8C8, 0xFFFF7EB6, 0xFFE0569B, 0xFFB03A75, 0xFFE53935, 0xFFFF9800, 0xFFFFD54F,
            0xFF8BC34A, 0xFF2E7D32, 0xFF9BE3C8, 0xFF4FC3F7, 0xFF1E5AA8, 0xFFB39DDB, 0xFF7B4FA0, 0xFFFFE0C2,
            0xFFFFFFFF, 0xFFD6D6DE, 0xFF9A9AA8, 0xFF5A5A66, 0xFF16161C, 0xFF8D5A3B, 0xFFC9A26B, 0xFFE8B84A};

    enum Gereedschap {
        POTLOOD("potlood", GLFW.GLFW_KEY_P), GUM("gum", GLFW.GLFW_KEY_G), EMMER("emmer", GLFW.GLFW_KEY_E), PIPET("pipet", GLFW.GLFW_KEY_I),
        LIJN("lijn", GLFW.GLFW_KEY_L);

        final String id;
        final int toets;

        Gereedschap(String id, int toets) {
            this.id = id;
            this.toets = toets;
        }
    }

    private enum Vraag { GEEN, WISSEN, WEG }

    @Nullable
    private static DynamicTexture wiel;

    private final int token;
    private final Doel doel;
    private final int[] px;
    private final int[] begin;
    @Nullable
    private DynamicTexture canvas;
    private boolean texVuil = true;

    private Gereedschap tool = Gereedschap.POTLOOD, vorige = Gereedschap.POTLOOD;
    private int maat = 1;
    private boolean raster;
    private float hue = 0.93f, sat = 0.34f, val = 1f;
    private final Deque<int[]> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private final List<Integer> recent = new ArrayList<>();

    /** The mouse button drawing on the canvas (-1: none), the last pixel of the stroke, the line's start and canvas before it. */
    private int knop = -1, lx, ly, lijnX, lijnY;
    @Nullable
    private int[] lijnBasis;
    private boolean sleepWiel, sleepWaarde;

    private Vraag vraag = Vraag.GEEN;
    private boolean bezig, opgeslagen;
    private int bezigTicks;
    @Nullable
    private Component fout;

    // layout
    private int schaal, cw, pw, ph, px0, py0, toolX, toolY, cx, cy, kolX, kolY;

    public TekenScherm(int token, Doel doel, @Nullable int[] bestaand) {
        super(Component.translatable("guhwinter.tekenen.editor.titel." + doel.id()));
        this.token = token;
        this.doel = doel;
        this.px = bestaand == null ? TekenPixels.leeg() : bestaand.clone();
        this.begin = px.clone();
    }

    static void vergeetWiel() {
        if (wiel != null) {
            Minecraft.getInstance().getTextureManager().release(WIEL);
            wiel = null;
        }
    }

    // =================================================================================================================
    // layout + widgets
    // =================================================================================================================

    @Override
    protected void init() {
        schaal = Mth.clamp(Math.min((height - KOP - VOET - 22) / W, (width - 204) / W), 2, 6);
        cw = W * schaal;
        pw = cw + 204;
        ph = KOP + Math.max(cw + 22, 176) + VOET;   // + room for the frame and the little face below it
        px0 = (width - pw) / 2;
        py0 = Math.max(4, (height - ph) / 2);
        toolX = px0 + 10;
        toolY = py0 + KOP + 6;
        cx = toolX + TOOLS_W + 12 + 6;
        cy = py0 + KOP + 8;
        kolX = cx + cw + 6 + 12;
        kolY = py0 + KOP + 6;
        if (canvas == null) {
            canvas = new DynamicTexture(() -> "guhs guhwinter teken canvas", new NativeImage(W, W, false));
            minecraft.getTextureManager().register(CANVAS, canvas);
            texVuil = true;
        }
        if (wiel == null) {
            wiel = new DynamicTexture(() -> "guhs guhwinter kleurwiel", maakWiel());
            minecraft.getTextureManager().register(WIEL, wiel);
        }
        int by = py0 + ph - VOET + 3;
        Button opslaan = Button.builder(Component.translatable("guhwinter.tekenen.editor.opslaan"), b -> opslaan())
                .bounds(px0 + pw - 10 - 74, by, 74, 20).build();
        opslaan.active = !bezig && vraag == Vraag.GEEN;
        addRenderableWidget(opslaan);
        Button annuleer = Button.builder(Component.translatable("guhwinter.tekenen.editor.annuleren"), b -> onClose())
                .bounds(px0 + pw - 10 - 74 - 4 - 74, by, 74, 20).build();
        annuleer.active = !bezig && vraag == Vraag.GEEN;
        addRenderableWidget(annuleer);
        if (vraag != Vraag.GEEN) {
            int mx = cx + cw / 2, my = cy + cw / 2;
            addRenderableWidget(Button.builder(Component.translatable("guhwinter.tekenen.editor.ja"), b -> {
                if (vraag == Vraag.WISSEN) {
                    bewaarStap();
                    Arrays.fill(px, 0);
                    texVuil = true;
                    vraag = Vraag.GEEN;
                    rebuildWidgets();
                } else {
                    sluit();
                }
            }).bounds(mx - 52, my + 6, 50, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("guhwinter.tekenen.editor.nee"), b -> {
                vraag = Vraag.GEEN;
                rebuildWidgets();
            }).bounds(mx + 2, my + 6, 50, 20).build());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (bezig) {
            return;
        }
        if (vraag == Vraag.GEEN && !Arrays.equals(px, begin)) {
            vraag = Vraag.WEG;
            rebuildWidgets();
            return;
        }
        sluit();
    }

    private void sluit() {
        minecraft.setScreen(null);
    }

    @Override
    public void removed() {
        if (!opgeslagen && minecraft.getConnection() != null) {
            ClientPacketDistributor.sendToServer(new TekenPayloads.Annuleer(token));
        }
        if (canvas != null) {
            minecraft.getTextureManager().release(CANVAS);
            canvas = null;
        }
    }

    private void opslaan() {
        if (bezig) {
            return;
        }
        bezig = true;
        bezigTicks = 0;
        fout = null;
        ClientPacketDistributor.sendToServer(new TekenPayloads.Opslaan(token, TekenPixels.pak(px)));
        rebuildWidgets();
    }

    /** The server's answer to Opslaan. */
    void antwoord(int t, boolean ok, String sleutel) {
        if (t != token) {
            return;
        }
        bezig = false;
        if (ok) {
            opgeslagen = true;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f));
            sluit();
            return;
        }
        fout = Component.translatable(sleutel.isEmpty() ? "guhwinter.tekenen.fout.kapot" : sleutel);
        rebuildWidgets();
    }

    @Override
    public void tick() {
        if (bezig && ++bezigTicks > 200) {   // no answer in 10 s: let them try again
            bezig = false;
            fout = Component.translatable("guhwinter.tekenen.fout.geen_antwoord");
            rebuildWidgets();
        }
    }

    // =================================================================================================================
    // colours
    // =================================================================================================================

    private int kleur() {
        return 0xFF000000 | Mth.hsvToRgb(hue, sat, val);
    }

    private void zetKleur(int argb) {
        float r = (argb >> 16 & 0xFF) / 255f, g = (argb >> 8 & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        val = max;
        sat = max == 0 ? 0 : d / max;
        if (d > 0) {
            float h;
            if (max == r) {
                h = ((g - b) / d) % 6f;
            } else if (max == g) {
                h = (b - r) / d + 2f;
            } else {
                h = (r - g) / d + 4f;
            }
            h /= 6f;
            hue = h < 0 ? h + 1 : h;
        }
    }

    private void gebruikt(int argb) {
        recent.remove((Integer) argb);
        recent.add(0, argb);
        while (recent.size() > RECENT_MAX) {
            recent.remove(recent.size() - 1);
        }
    }

    private static NativeImage maakWiel() {
        NativeImage img = new NativeImage(WIEL_TEX, WIEL_TEX, false);
        float m = WIEL_TEX / 2f;
        for (int y = 0; y < WIEL_TEX; y++) {
            for (int x = 0; x < WIEL_TEX; x++) {
                float dx = x + 0.5f - m, dy = y + 0.5f - m;
                float r = (float) Math.sqrt(dx * dx + dy * dy) / m;
                if (r > 1.02f) {
                    img.setPixel(x, y, 0);
                    continue;
                }
                float h = (float) (Math.atan2(dy, dx) / (2 * Math.PI));
                int rgb = Mth.hsvToRgb(h < 0 ? h + 1 : h, Math.min(1f, r), 1f);
                int a = (int) (Mth.clamp((1.02f - r) * m / 1.5f, 0f, 1f) * 255);
                img.setPixel(x, y, a << 24 | rgb);
            }
        }
        return img;
    }

    // =================================================================================================================
    // drawing on the pixels
    // =================================================================================================================

    private void bewaarStap() {
        undo.push(px.clone());
        while (undo.size() > UNDO_MAX) {
            undo.removeLast();
        }
        redo.clear();
    }

    private void ongedaan() {
        if (!undo.isEmpty()) {
            redo.push(px.clone());
            System.arraycopy(undo.pop(), 0, px, 0, px.length);
            texVuil = true;
        }
    }

    private void opnieuw() {
        if (!redo.isEmpty()) {
            undo.push(px.clone());
            System.arraycopy(redo.pop(), 0, px, 0, px.length);
            texVuil = true;
        }
    }

    private void stip(int x, int y, int argb) {
        int s = x - (maat - 1) / 2, t = y - (maat - 1) / 2;
        for (int j = t; j < t + maat; j++) {
            for (int i = s; i < s + maat; i++) {
                if (i >= 0 && j >= 0 && i < W && j < W) {
                    px[j * W + i] = argb;
                }
            }
        }
        texVuil = true;
    }

    private void lijn(int x0, int y0, int x1, int y1, int argb) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
        while (true) {
            stip(x0, y0, argb);
            if (x0 == x1 && y0 == y1) {
                return;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private void vul(int x, int y, int argb) {
        int doel = px[y * W + x];
        if (doel == argb) {
            return;
        }
        int[] stapel = new int[W * W];
        int n = 0;
        stapel[n++] = y * W + x;
        px[y * W + x] = argb;
        while (n > 0) {
            int i = stapel[--n], ix = i % W, iy = i / W;
            if (ix > 0 && px[i - 1] == doel) {
                px[i - 1] = argb;
                stapel[n++] = i - 1;
            }
            if (ix < W - 1 && px[i + 1] == doel) {
                px[i + 1] = argb;
                stapel[n++] = i + 1;
            }
            if (iy > 0 && px[i - W] == doel) {
                px[i - W] = argb;
                stapel[n++] = i - W;
            }
            if (iy < W - 1 && px[i + W] == doel) {
                px[i + W] = argb;
                stapel[n++] = i + W;
            }
        }
        texVuil = true;
    }

    // =================================================================================================================
    // input
    // =================================================================================================================

    private boolean opCanvas(double mx, double my) {
        return mx >= cx && my >= cy && mx < cx + cw && my < cy + cw;
    }

    private int pixX(double mx) {
        return Mth.clamp((int) Math.floor((mx - cx) / schaal), 0, W - 1);
    }

    private int pixY(double my) {
        return Mth.clamp((int) Math.floor((my - cy) / schaal), 0, W - 1);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (vraag != Vraag.GEEN || bezig) {
            return true;
        }
        double mx = event.x(), my = event.y();
        int b = event.button();
        if (opCanvas(mx, my) && (b == 0 || b == 1)) {
            int x = pixX(mx), y = pixY(my);
            Gereedschap t = b == 1 ? Gereedschap.GUM : tool;
            if (t == Gereedschap.PIPET) {
                int p = px[y * W + x];
                if (p != 0) {
                    zetKleur(p);
                    gebruikt(p);
                }
                tool = vorige;
                klik();
                return true;
            }
            bewaarStap();
            knop = b;
            lx = x;
            ly = y;
            switch (t) {
                case EMMER -> {
                    vul(x, y, kleur());
                    gebruikt(kleur());
                    knop = -1;
                    eindeStreek();
                }
                case LIJN -> {
                    lijnX = x;
                    lijnY = y;
                    lijnBasis = px.clone();
                    stip(x, y, kleur());
                }
                case GUM -> stip(x, y, 0);
                default -> stip(x, y, kleur());
            }
            return true;
        }
        if (in(mx, my, kolX, kolY, WIEL_D, WIEL_D)) {
            sleepWiel = true;
            kiesWiel(mx, my);
            return true;
        }
        if (in(mx, my, kolX + WIEL_D + 4, kolY, 10, WIEL_D)) {
            sleepWaarde = true;
            kiesWaarde(my);
            return true;
        }
        for (int i = 0; i < KLEURTJES.length; i++) {
            if (in(mx, my, kolX + (i % 8) * 11, kolY + 112 + (i / 8) * 11, 10, 10)) {
                zetKleur(KLEURTJES[i]);
                klik();
                return true;
            }
        }
        for (int i = 0; i < recent.size(); i++) {
            if (in(mx, my, kolX + i * 11, kolY + 158, 10, 10)) {
                zetKleur(recent.get(i));
                klik();
                return true;
            }
        }
        return klikTool(mx, my);
    }

    private boolean klikTool(double mx, double my) {
        Gereedschap[] ts = Gereedschap.values();
        for (int i = 0; i < ts.length; i++) {
            if (in(mx, my, toolX + (i % 2) * 28, toolY + (i / 2) * 28, 24, 24)) {
                kies(ts[i]);
                return true;
            }
        }
        if (in(mx, my, toolX + 28, toolY + 56, 24, 24)) {
            raster = !raster;
            klik();
            return true;
        }
        int[] maten = {1, 2, 4};
        for (int i = 0; i < 3; i++) {
            if (in(mx, my, toolX + i * 18, toolY + 86, 16, 16)) {
                maat = maten[i];
                klik();
                return true;
            }
        }
        if (in(mx, my, toolX, toolY + 108, 24, 18)) {
            ongedaan();
            klik();
            return true;
        }
        if (in(mx, my, toolX + 28, toolY + 108, 24, 18)) {
            opnieuw();
            klik();
            return true;
        }
        if (in(mx, my, toolX, toolY + 130, TOOLS_W, 16)) {
            vraag = Vraag.WISSEN;
            klik();
            rebuildWidgets();
            return true;
        }
        return false;
    }

    private void kies(Gereedschap t) {
        if (t == Gereedschap.PIPET && tool != Gereedschap.PIPET) {
            vorige = tool;
        }
        tool = t;
        klik();
    }

    private void klik() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 0.4f));
    }

    private void kiesWiel(double mx, double my) {
        double dx = mx - (kolX + WIEL_D / 2.0), dy = my - (kolY + WIEL_D / 2.0);
        double r = Math.sqrt(dx * dx + dy * dy) / (WIEL_D / 2.0);
        float h = (float) (Math.atan2(dy, dx) / (2 * Math.PI));
        hue = h < 0 ? h + 1 : h;
        sat = (float) Math.min(1, r);
        if (val < 0.08f) {
            val = 1f;   // picking a colour while it is all black: make it visible
        }
    }

    private void kiesWaarde(double my) {
        val = Mth.clamp(1f - (float) ((my - kolY) / WIEL_D), 0f, 1f);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mx = event.x(), my = event.y();
        if (sleepWiel) {
            kiesWiel(mx, my);
            return true;
        }
        if (sleepWaarde) {
            kiesWaarde(my);
            return true;
        }
        if (knop >= 0) {
            int x = pixX(mx), y = pixY(my);
            Gereedschap t = knop == 1 ? Gereedschap.GUM : tool;
            if (t == Gereedschap.LIJN && lijnBasis != null) {
                System.arraycopy(lijnBasis, 0, px, 0, px.length);
                lijn(lijnX, lijnY, x, y, kleur());
            } else if (x != lx || y != ly) {
                lijn(lx, ly, x, y, t == Gereedschap.GUM ? 0 : kleur());
            }
            lx = x;
            ly = y;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        sleepWiel = false;
        sleepWaarde = false;
        if (knop >= 0 && event.button() == knop) {
            if (knop == 0 && tool != Gereedschap.GUM) {
                gebruikt(kleur());
            }
            knop = -1;
            lijnBasis = null;
            eindeStreek();
            return true;
        }
        return super.mouseReleased(event);
    }

    /** A stroke that changed nothing leaves no undo step. */
    private void eindeStreek() {
        if (!undo.isEmpty() && Arrays.equals(undo.peek(), px)) {
            undo.pop();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (vraag != Vraag.GEEN) {
            if (event.isEscape()) {
                vraag = Vraag.GEEN;
                rebuildWidgets();
                return true;
            }
            return super.keyPressed(event);
        }
        int k = event.key();
        if (event.hasControlDown()) {
            if (k == GLFW.GLFW_KEY_Z && !event.hasShiftDown()) {
                ongedaan();
                return true;
            }
            if (k == GLFW.GLFW_KEY_Y || (k == GLFW.GLFW_KEY_Z && event.hasShiftDown())) {
                opnieuw();
                return true;
            }
        } else if (knop < 0) {
            for (Gereedschap t : Gereedschap.values()) {
                if (k == t.toets) {
                    kies(t);
                    return true;
                }
            }
            if (k == GLFW.GLFW_KEY_1 || k == GLFW.GLFW_KEY_2 || k == GLFW.GLFW_KEY_4) {
                maat = k == GLFW.GLFW_KEY_1 ? 1 : k == GLFW.GLFW_KEY_2 ? 2 : 4;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    // =================================================================================================================
    // drawing the screen
    // =================================================================================================================

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (texVuil && canvas != null) {
            NativeImage img = canvas.getPixels();
            for (int i = 0; i < px.length; i++) {
                img.setPixel(i % W, i / W, TekenPixels.toon(px[i]));
            }
            canvas.upload();
            texVuil = false;
        }
        paneel(g);
        lijst(g);
        g.blit(RenderPipelines.GUI_TEXTURED, CANVAS, cx, cy, 0f, 0f, cw, cw, W, W, W, W);
        if (raster && schaal >= 3) {
            for (int i = 1; i < W; i++) {
                g.fill(cx + i * schaal, cy, cx + i * schaal + 1, cy + cw, 0x18000000);
                g.fill(cx, cy + i * schaal, cx + cw, cy + i * schaal + 1, 0x18000000);
            }
        }
        if (vraag == Vraag.GEEN && opCanvas(mouseX, mouseY)) {
            int m = tool == Gereedschap.EMMER || tool == Gereedschap.PIPET ? 1 : maat;
            int x = pixX(mouseX) - (m - 1) / 2, y = pixY(mouseY) - (m - 1) / 2;
            g.outline(cx + x * schaal - 1, cy + y * schaal - 1, m * schaal + 2, m * schaal + 2, 0xCCFFFFFF);
            g.outline(cx + x * schaal, cy + y * schaal, m * schaal, m * schaal, 0xCC000000);
        }
        Component hover = gereedschap(g, mouseX, mouseY);
        kleuren(g, mouseX, mouseY);
        voet(g);
        if (vraag != Vraag.GEEN) {
            g.nextStratum();
            int mx = cx + cw / 2, my = cy + cw / 2;
            g.fill(cx, cy, cx + cw, cy + cw, 0x99402030);
            g.fill(mx - 60, my - 24, mx + 60, my + 32, PAPIER_UI);
            g.outline(mx - 60, my - 24, 120, 56, ROZE_DONKER);
            g.centeredText(font, Component.translatable(vraag == Vraag.WISSEN ? "guhwinter.tekenen.editor.wissen_zeker" : "guhwinter.tekenen.editor.weg_zeker"),
                    mx, my - 14, INKT);
        }
        super.extractRenderState(g, mouseX, mouseY, a);
        if (hover != null && vraag == Vraag.GEEN) {
            g.setTooltipForNextFrame(font, hover, mouseX, mouseY);
        }
    }

    private void paneel(GuiGraphicsExtractor g) {
        g.fill(px0 - 2, py0 - 2, px0 + pw + 2, py0 + ph + 2, ROZE_DONKER);
        g.fill(px0, py0, px0 + pw, py0 + ph, PAPIER_UI);
        g.fill(px0, py0, px0 + pw, py0 + KOP, ROZE);
        g.fill(px0, py0 + KOP - 2, px0 + pw, py0 + KOP, ROZE_DONKER);
        // snowflakes in the header
        for (int i = 0; i < pw / 26; i++) {
            int sx = px0 + 9 + i * 26 + (i * 7 % 5), sy = py0 + 4 + (i * 5 % 11);
            if (Math.abs(sx - (px0 + pw / 2)) < font.width(title) / 2 + 10) {
                continue;
            }
            g.fill(sx, sy - 1, sx + 1, sy + 2, 0xCCFFFFFF);
            g.fill(sx - 1, sy, sx + 2, sy + 1, 0xCCFFFFFF);
        }
        g.centeredText(font, title, px0 + pw / 2, py0 + 7, 0xFFFFFFFF);
    }

    /** The pink guh frame round the canvas: ears on top, a little face below. */
    private void lijst(GuiGraphicsExtractor g) {
        int x0 = cx - 6, y0 = cy - 6, x1 = cx + cw + 6, y1 = cy + cw + 6;
        // ears (stacked rows, narrower to the top)
        for (int oor = 0; oor < 2; oor++) {
            int ox = oor == 0 ? x0 + 6 : x1 - 6 - 22;
            for (int r = 0; r < 8; r++) {   // 8 rows up from the frame, narrower to the top (below the title)
                int ry = y0 - 1 - r, inset = r + 1;
                g.fill(ox + inset, ry, ox + 22 - inset, ry + 1, ROZE_DONKER);
                if (r < 6) {
                    g.fill(ox + inset + 3, ry, ox + 22 - inset - 3, ry + 1, OOR_BINNEN);
                }
            }
        }
        g.fill(x0, y0, x1, y1, ROZE_DONKER);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, ROZE);
        g.fill(cx - 1, cy - 1, cx + cw + 1, cy + cw + 1, ROZE_DONKER);
        // the face on a little tab below the frame
        int fx = cx + cw / 2;
        g.fill(fx - 16, y1 - 1, fx + 16, y1 + 7, ROZE);
        g.fill(fx - 16, y1 + 7, fx + 16, y1 + 8, ROZE_DONKER);
        g.fill(fx - 7, y1 + 1, fx - 5, y1 + 4, INKT);
        g.fill(fx + 5, y1 + 1, fx + 7, y1 + 4, INKT);
        g.fill(fx - 12, y1 + 3, fx - 9, y1 + 5, 0xFFFF8FB8);
        g.fill(fx + 9, y1 + 3, fx + 12, y1 + 5, 0xFFFF8FB8);
        g.fill(fx - 2, y1 + 4, fx + 2, y1 + 5, INKT);
    }

    /** The tool tiles; returns the tooltip of the hovered one. */
    @Nullable
    private Component gereedschap(GuiGraphicsExtractor g, int mx, int my) {
        Component tip = null;
        Gereedschap[] ts = Gereedschap.values();
        for (int i = 0; i < ts.length; i++) {
            int x = toolX + (i % 2) * 28, y = toolY + (i / 2) * 28;
            boolean over = in(mx, my, x, y, 24, 24);
            tegel(g, x, y, 24, 24, tool == ts[i], over);
            icoon(g, ICONEN[i], x + 4, y + 4);
            if (over) {
                tip = Component.translatable("guhwinter.tekenen.editor.tool." + ts[i].id);
            }
        }
        int rx = toolX + 28, ry = toolY + 56;
        boolean overRaster = in(mx, my, rx, ry, 24, 24);
        tegel(g, rx, ry, 24, 24, raster, overRaster);
        icoon(g, RASTER, rx + 4, ry + 4);
        if (overRaster) {
            tip = Component.translatable("guhwinter.tekenen.editor.tool.raster");
        }
        int[] maten = {1, 2, 4};
        for (int i = 0; i < 3; i++) {
            int x = toolX + i * 18, y = toolY + 86;
            boolean over = in(mx, my, x, y, 16, 16);
            tegel(g, x, y, 16, 16, maat == maten[i], over);
            int s = maten[i] == 1 ? 2 : maten[i] == 2 ? 4 : 8;
            g.fill(x + 8 - s / 2, y + 8 - s / 2, x + 8 - s / 2 + s, y + 8 - s / 2 + s, INKT);
            if (over) {
                tip = Component.translatable("guhwinter.tekenen.editor.tool.maat", maten[i]);
            }
        }
        boolean overU = in(mx, my, toolX, toolY + 108, 24, 18), overR = in(mx, my, toolX + 28, toolY + 108, 24, 18);
        tegel(g, toolX, toolY + 108, 24, 18, false, overU && !undo.isEmpty());
        icoon(g, undo.isEmpty() ? UNDO_UIT : UNDO, toolX + 4, toolY + 109);
        tegel(g, toolX + 28, toolY + 108, 24, 18, false, overR && !redo.isEmpty());
        icoon(g, redo.isEmpty() ? REDO_UIT : REDO, toolX + 32, toolY + 109);
        if (overU) {
            tip = Component.translatable("guhwinter.tekenen.editor.tool.ongedaan", undo.size());
        } else if (overR) {
            tip = Component.translatable("guhwinter.tekenen.editor.tool.opnieuw", redo.size());
        }
        boolean overW = in(mx, my, toolX, toolY + 130, TOOLS_W, 16);
        tegel(g, toolX, toolY + 130, TOOLS_W, 16, false, overW);
        g.centeredText(font, Component.translatable("guhwinter.tekenen.editor.wissen"), toolX + TOOLS_W / 2, toolY + 134, ROOD);
        return tip;
    }

    private void tegel(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean aan, boolean over) {
        g.fill(x, y, x + w, y + h, aan ? ROZE_DONKER : over ? ROZE : 0xFFE9C9D6);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, aan ? ROZE_LICHT : over ? 0xFFFFF0F6 : 0xFFFFFFFF);
    }

    private void kleuren(GuiGraphicsExtractor g, int mx, int my) {
        int v = (int) (val * 255);
        if (wiel != null) {
            g.blit(RenderPipelines.GUI_TEXTURED, WIEL, kolX, kolY, 0f, 0f, WIEL_D, WIEL_D, WIEL_TEX, WIEL_TEX, WIEL_TEX, WIEL_TEX,
                    0xFF000000 | v << 16 | v << 8 | v);
        }
        int wx = kolX + WIEL_D / 2 + Math.round(Mth.cos(hue * Mth.TWO_PI) * sat * WIEL_D / 2f);
        int wy = kolY + WIEL_D / 2 + Math.round(Mth.sin(hue * Mth.TWO_PI) * sat * WIEL_D / 2f);
        g.outline(wx - 3, wy - 3, 7, 7, 0xFFFFFFFF);
        g.outline(wx - 2, wy - 2, 5, 5, 0xFF000000);
        // the brightness slider
        int sx = kolX + WIEL_D + 4;
        g.fillGradient(sx, kolY, sx + 10, kolY + WIEL_D, 0xFF000000 | Mth.hsvToRgb(hue, sat, 1f), 0xFF000000);
        g.outline(sx - 1, kolY - 1, 12, WIEL_D + 2, ROZE_DONKER);
        int vy = kolY + Math.round((1f - val) * (WIEL_D - 1));
        g.fill(sx - 2, vy - 1, sx + 12, vy + 2, 0xFFFFFFFF);
        g.fill(sx - 1, vy, sx + 11, vy + 1, 0xFF000000);
        // the current colour
        g.fill(kolX - 1, kolY + 85, kolX + 31, kolY + 99, ROZE_DONKER);
        g.fill(kolX, kolY + 86, kolX + 30, kolY + 98, kleur());
        g.text(font, String.format("#%06X", kleur() & 0xFFFFFF), kolX + 35, kolY + 88, INKT, false);
        g.text(font, Component.translatable("guhwinter.tekenen.editor.kleurtjes"), kolX, kolY + 102, GRIJS, false);
        for (int i = 0; i < KLEURTJES.length; i++) {
            staal(g, kolX + (i % 8) * 11, kolY + 112 + (i / 8) * 11, KLEURTJES[i], mx, my);
        }
        g.text(font, Component.translatable("guhwinter.tekenen.editor.recent"), kolX, kolY + 148, GRIJS, false);
        for (int i = 0; i < RECENT_MAX; i++) {
            if (i < recent.size()) {
                staal(g, kolX + i * 11, kolY + 158, recent.get(i), mx, my);
            } else {
                g.outline(kolX + i * 11, kolY + 158, 10, 10, 0xFFE9C9D6);
            }
        }
    }

    private void staal(GuiGraphicsExtractor g, int x, int y, int argb, int mx, int my) {
        boolean over = in(mx, my, x, y, 10, 10);
        g.fill(x, y, x + 10, y + 10, over ? INKT : 0xFFCFAFBE);
        g.fill(x + 1, y + 1, x + 9, y + 9, argb);
        if (argb == kleur()) {
            g.fill(x + 4, y + 4, x + 6, y + 6, (argb & 0xFF) + (argb >> 8 & 0xFF) + (argb >> 16 & 0xFF) > 380 ? INKT : 0xFFFFFFFF);
        }
    }

    private void voet(GuiGraphicsExtractor g) {
        int y = py0 + ph - VOET + 9;
        Component c = bezig ? Component.translatable("guhwinter.tekenen.editor.bezig") : fout != null ? fout
                : Component.translatable("guhwinter.tekenen.editor.uitleg");
        int max = pw - 20 - 2 * 74 - 10;
        List<net.minecraft.util.FormattedCharSequence> regels = font.split(c, max);
        int ry = regels.size() > 1 ? y - 5 : y;
        for (int i = 0; i < Math.min(2, regels.size()); i++) {
            g.text(font, regels.get(i), px0 + 10, ry + i * 10, fout != null && !bezig ? ROOD : GRIJS, false);
        }
    }

    // =================================================================================================================
    // the little 8x8 icons (drawn 2x)
    // =================================================================================================================

    private static final String[] POTLOOD = {
            "......pp",
            ".....ypp",
            "....yyy.",
            "...yyy..",
            "..yyy...",
            ".hyy....",
            ".kh.....",
            "k......."};
    private static final String[] GUM = {
            "........",
            "...pppp.",
            "..pppppp",
            ".ppppppw",
            ".wwwwww.",
            ".wwwww..",
            "........",
            "........"};
    private static final String[] EMMER = {
            "...g....",
            "..g.g...",
            ".ggggg.b",
            ".gbbbg.b",
            ".ggggg..",
            "..ggg...",
            "..ggg...",
            "........"};
    private static final String[] PIPET = {
            "......kk",
            ".....kkk",
            "....gkk.",
            "...gg...",
            "..gg....",
            ".gb.....",
            ".b......",
            "b......."};
    private static final String[] LIJN = {
            ".......k",
            "......k.",
            ".....k..",
            "....k...",
            "...k....",
            "..k.....",
            ".k......",
            "k......."};
    private static final String[] RASTER = {
            "g.g.g.g.",
            "........",
            "g.g.g.g.",
            "........",
            "g.g.g.g.",
            "........",
            "g.g.g.g.",
            "........"};
    private static final String[] UNDO = {
            "..k.....",
            ".kk.....",
            "kkkkkk..",
            ".kk...k.",
            "..k...k.",
            "......k.",
            "....kk..",
            "........"};
    private static final String[] REDO = spiegel(UNDO);
    private static final String[] UNDO_UIT = kleur(UNDO, 'g');
    private static final String[] REDO_UIT = kleur(REDO, 'g');
    private static final String[][] ICONEN = {POTLOOD, GUM, EMMER, PIPET, LIJN};

    private static String[] spiegel(String[] in) {
        String[] uit = new String[in.length];
        for (int i = 0; i < in.length; i++) {
            uit[i] = new StringBuilder(in[i]).reverse().toString();
        }
        return uit;
    }

    private static String[] kleur(String[] in, char c) {
        String[] uit = new String[in.length];
        for (int i = 0; i < in.length; i++) {
            uit[i] = in[i].replace('k', c);
        }
        return uit;
    }

    private static int icoonKleur(char c) {
        return switch (c) {
            case 'p' -> 0xFFF7A8C8;
            case 'y' -> 0xFFFFD45A;
            case 'h' -> 0xFFE8C9A0;
            case 'k' -> INKT;
            case 'w' -> 0xFFF4F0F2;
            case 'g' -> 0xFFB4A6B0;
            case 'b' -> 0xFF5FAEF0;
            default -> 0;
        };
    }

    private static void icoon(GuiGraphicsExtractor g, String[] rijen, int x, int y) {
        for (int r = 0; r < rijen.length; r++) {
            for (int c = 0; c < rijen[r].length(); c++) {
                int k = icoonKleur(rijen[r].charAt(c));
                if (k != 0) {
                    g.fill(x + c * 2, y + r * 2, x + c * 2 + 2, y + r * 2 + 2, k);
                }
            }
        }
    }
}
