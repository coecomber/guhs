package nl.juiced.guhs.feature.verhaal.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.VerhaalPayloads;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * bbq2 (verhaal engine): the narrator card ({@link Verteller}): a parchment over the whole screen with a drawn map
 * ({@code textures/gui/verhaal/kaart_<id>.png}, 256x160), the chapter's title and its lines, which appear one by one;
 * after the last one the button "Verder". 1.4.4: the next line comes on a click (left or right mouse button, space or
 * enter) once the line before it is on the paper, with the hint "Klik om verder te gaan"; a card read before (and every
 * picture book) is skipped by holding the button. Esc does nothing (the card is part of the story). The same screen shows a
 * cutscene as a picture book ({@code Cutscenes.herbekijk} far from where the scene played): the scene's subtitles as the
 * lines, over the map of its narrator card.
 */
public final class VertelScherm extends Screen {
    private static final int KAART_W = 256, KAART_H = 160;
    private static final int PAPIER = 0xFFF3E4C4, PAPIER_RAND = 0xFF8A6A3A, INKT = 0xFF4A3220, INKT_ZACHT = 0xFF7A5A3A, TITEL = 0xFF7A2848;

    /** The card that is open (so it comes back when something else closed the screen while the story still waits). */
    @Nullable
    private static VertelScherm open;

    private final String soort, id;
    /** 1.4.4: may it be skipped by holding the button; how many lines show; the tick each line appeared; the hold. */
    private final boolean overslaan;
    private int getoond = 1, houd;
    private final List<Integer> verschenen = new ArrayList<>(List.of(0));
    private boolean gemeldWacht;
    private final List<Component> regels = new ArrayList<>();
    private final Component titel;
    @Nullable
    private final Identifier kaart;
    private int ticks;
    private boolean klaar;
    @Nullable
    private Button verder;

    private VertelScherm(String soort, String id, boolean overslaan) {
        super(Component.empty());
        this.soort = soort;
        this.id = id;
        this.overslaan = overslaan;
        if (VerhaalPayloads.BOEK.equals(soort)) {
            Cutscene s = Cutscene.van(id);
            this.titel = Component.translatable(s == null ? "scene.guhs." + id + ".titel" : s.titelKey());
            this.kaart = s == null || s.kaart() == null ? null : plaatje(s.kaart());
            if (s != null) {
                for (Cutscene.Zeg z : s.zinnen()) {
                    regels.add(CutsceneSpeler.regel(s, z));
                }
            }
        } else {
            Verteller.Kaart k = Verteller.van(id);
            this.titel = Component.translatable(k == null ? "gui.guhs.verhaal.kaart." + id + ".titel" : k.titelKey());
            this.kaart = plaatje(id);
            for (int i = 0; k != null && i < k.regels(); i++) {
                regels.add(Component.translatable(k.regelKey(i)));
            }
        }
    }

    /** The map picture of a narrator card. */
    public static Identifier plaatje(String kaartId) {
        return Guhs.id("textures/gui/verhaal/kaart_" + kaartId + ".png");
    }

    static boolean actief() {
        return open != null;
    }

    /** The server shows a card (or a picture book). */
    static void toon(VerhaalPayloads.Kaart p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        CutsceneSpeler.stop(false);
        open = new VertelScherm(p.soort(), p.id(), p.overslaan());
        VerhaalClient.zetBezig();
        mc.setScreen(open);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 0.9f));
    }

    /** Closes the card without telling the server (the lock was broken off, or a scene starts). */
    static void sluit() {
        VertelScherm s = open;
        if (s == null) {
            return;
        }
        open = null;
        VerhaalClient.zetBezig();
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == s) {
            mc.setScreen(null);
        }
    }

    /** Every client tick: a card that was pushed away while the story still waits for it comes back. */
    static void bewaak() {
        Minecraft mc = Minecraft.getInstance();
        if (open == null) {
            return;
        }
        if (mc.level == null || mc.player == null) {
            open = null;
            VerhaalClient.zetBezig();
        } else if (mc.screen == null) {
            mc.setScreen(open);
        }
    }

    /** How many lines show now. */
    private int zichtbaar() {
        return Math.min(regels.size(), getoond);
    }

    /** 1.4.4: the newest line is on the paper: the card waits for a click (the next line, or "Verder" after the last). */
    private boolean wacht() {
        return !klaar && ticks - verschenen.get(verschenen.size() - 1) >= Verteller.MIN_TICKS_PER_REGEL;
    }

    private boolean alles() {
        return zichtbaar() >= regels.size() && wacht();
    }

    /** 1.4.4: a click: the next line, or after the last one "Verder". */
    private void klik() {
        if (!wacht()) {
            return;
        }
        if (zichtbaar() < regels.size()) {
            getoond++;
            verschenen.add(ticks);
            meld(false);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.3f, 0.4f));
        } else {
            verder();
        }
    }

    /** 1.4.4: tells the server that the card waits for a click (its time-out stands still) or goes on. */
    private void meld(boolean wacht) {
        if (wacht == gemeldWacht) {
            return;
        }
        gemeldWacht = wacht;
        if (Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new VerhaalPayloads.Pauze(soort, id, wacht));
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean dubbel) {
        if (super.mouseClicked(event, dubbel)) {
            return true;   // (the button "Verder")
        }
        if (event.button() == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT || event.button() == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            klik();
            return true;
        }
        return false;
    }

    @Override
    protected void init() {
        verder = Button.builder(Component.translatable("gui.guhs.verhaal.verder"), b -> verder()).bounds(width / 2 - 45, height - 28, 90, 20).build();
        verder.visible = alles();
        addRenderableWidget(verder);
    }

    private void verder() {
        if (klaar) {
            return;
        }
        klaar = true;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }

    @Override
    public void tick() {
        if (klaar) {
            // (the tick after the click: tell the server, which runs what comes after, and close)
            if (open == this) {
                if (Minecraft.getInstance().getConnection() != null) {
                    ClientPacketDistributor.sendToServer(new VerhaalPayloads.Klaar(soort, id));
                }
                sluit();
            }
            return;
        }
        ticks++;
        // 1.4.4: a card read before is skipped by holding the button
        if (overslaan && CutsceneSpeler.knopIngedruktInScherm()) {
            if (++houd >= nl.juiced.guhs.feature.verhaal.Klikklok.HOUD_TICKS) {
                verder();
                return;
            }
        } else {
            houd = 0;
        }
        meld(wacht());
        if (verder != null) {
            verder.visible = alles();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF1A1014);
        // the parchment
        int pw = Math.min(width - 16, 380), ph = height - 12, px = (width - pw) / 2, py = 6;
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, PAPIER_RAND);
        g.fill(px, py, px + pw, py + ph, PAPIER);
        g.fill(px, py, px + pw, py + 2, 0x30FFFFFF);
        g.fill(px, py + ph - 3, px + pw, py + ph, 0x20000000);
        int y = py + 8;
        g.centeredText(font, titel.copy().withStyle(ChatFormatting.BOLD), width / 2, y, TITEL);
        y += 14;
        // the map: as big as leaves room for the lines
        int tekstRuimte = Math.max(60, Math.min(ph / 2, regels.size() * 22 + 30));
        int kh = Mth.clamp(py + ph - tekstRuimte - y - 26, 40, KAART_H), kw = kh * KAART_W / KAART_H;
        if (kw > pw - 16) {
            kw = pw - 16;
            kh = kw * KAART_H / KAART_W;
        }
        if (kaart != null) {
            int kx = (width - kw) / 2;
            g.fill(kx - 2, y - 2, kx + kw + 2, y + kh + 2, PAPIER_RAND);
            g.blit(RenderPipelines.GUI_TEXTURED, kaart, kx, y, 0f, 0f, kw, kh, KAART_W, KAART_H, KAART_W, KAART_H);
            y += kh + 8;
        } else {
            y += 4;
        }
        // the lines, one by one (the newest fades in); when they don't all fit, the oldest make room
        int tw = pw - 28, onder = py + ph - 30;
        int n = zichtbaar();
        List<List<FormattedCharSequence>> blokken = new ArrayList<>();
        int hoogte = 0;
        for (int i = n - 1; i >= 0; i--) {
            List<FormattedCharSequence> blok = font.split(regels.get(i), tw);
            int h = blok.size() * 10 + 4;
            if (hoogte + h > onder - y && !blokken.isEmpty()) {
                break;
            }
            blokken.add(0, blok);
            hoogte += h;
        }
        int eerste = n - blokken.size();
        for (int i = 0; i < blokken.size(); i++) {
            int nr = eerste + i;
            float sinds = ticks + partialTick - verschenen.get(Math.min(nr, verschenen.size() - 1));
            int alpha = (int) (Mth.clamp(sinds / 12f, 0.05f, 1f) * 255);
            int kleur = (alpha << 24) | ((nr == n - 1 ? INKT : INKT_ZACHT) & 0xFFFFFF);
            for (FormattedCharSequence regel : blokken.get(i)) {
                g.text(font, regel, px + 14, y, kleur, false);
                y += 10;
            }
            y += 4;
        }
        // 1.4.4: the hints
        if (wacht() && zichtbaar() < regels.size()) {
            int alpha = 150 + (int) (105 * (0.5 + 0.5 * Math.sin((ticks + partialTick) * 0.25)));
            Component klik = Component.translatable(VerhaalHud.KLIK).append(" ▶");
            g.text(font, klik, px + pw - 10 - font.width(klik), py + ph - 14, (alpha << 24) | (TITEL & 0xFFFFFF), false);
        }
        if (overslaan) {
            Component houdTekst = Component.translatable(VerhaalHud.OVERSLAAN);
            int w = font.width(houdTekst), x = width - 6 - w;
            g.text(font, houdTekst, x, 2, 0xB0FFFFFF, true);
            if (houd > 0) {
                g.fill(x, 12, x + w, 14, 0x60FFFFFF);
                g.fill(x, 12, x + Math.round(w * Math.min(1f, houd / (float) nl.juiced.guhs.feature.verhaal.Klikklok.HOUD_TICKS)), 14, 0xFFF7D27A);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE
                || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
            klik();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
