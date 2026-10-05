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
 * after the last one the button "Verder". Esc does nothing (the card is part of the story). The same screen shows a
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
    private final List<Component> regels = new ArrayList<>();
    private final Component titel;
    @Nullable
    private final Identifier kaart;
    private int ticks;
    private boolean klaar;
    @Nullable
    private Button verder;

    private VertelScherm(String soort, String id) {
        super(Component.empty());
        this.soort = soort;
        this.id = id;
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
        open = new VertelScherm(p.soort(), p.id());
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
        return Math.min(regels.size(), 1 + ticks / Verteller.TICKS_PER_REGEL);
    }

    private boolean alles() {
        return zichtbaar() >= regels.size() && ticks >= (regels.size() - 1) * Verteller.TICKS_PER_REGEL + 12;
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
        int voor = zichtbaar();
        ticks++;
        if (zichtbaar() > voor) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.3f, 0.4f));
        }
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
            float sinds = ticks + partialTick - nr * Verteller.TICKS_PER_REGEL;
            int alpha = (int) (Mth.clamp(sinds / 12f, 0.05f, 1f) * 255);
            int kleur = (alpha << 24) | ((nr == n - 1 ? INKT : INKT_ZACHT) & 0xFFFFFF);
            for (FormattedCharSequence regel : blokken.get(i)) {
                g.text(font, regel, px + 14, y, kleur, false);
                y += 10;
            }
            y += 4;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (alles() && (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE
                || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)) {
            verder();
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
