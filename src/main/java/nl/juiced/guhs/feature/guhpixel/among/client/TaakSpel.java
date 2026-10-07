package nl.juiced.guhs.feature.guhpixel.among.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.among.AmongPayloads;
import nl.juiced.guhs.taal.Tekst;

/**
 * The frame of every task mini-game of Among Guhs ({@link TaakSpellen}): the panel with its title, the room, one line that
 * says what to do, the play field, a line of feedback and "Stoppen". A game draws its field in {@link #teken}, reacts to
 * the mouse ({@link #druk}, {@link #los}; {@link #muisX}/{@link #muisY}/{@link #ingedrukt} for holding and dragging) and
 * calls {@link #gelukt} with its answer; the server checks that answer against the opgave it sent ({@code Taken}).
 * Closing the panel early gives the work up. At home (the Taakjes-paneel as decoration) the same games run for fun.
 */
public abstract class TaakSpel extends Screen {
    protected static final int W = 264, H = 196;
    protected static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, VAK = 0x50000000, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0,
            GROEN = 0xFF68D88A, ROOD = 0xFFFF6B6B, METAAL = 0xFFD8D8E2, METAAL_D = 0xFF8E8E9C, DONKER = 0xFF1A0E15;
    /** The sprite sheet of the task panels (tools/features/guhpixel_among_taken.py): 16 x 16 cells. */
    protected static final Identifier VEL = Guhs.id("textures/gui/among_taken.png");
    protected static final int VEL_W = 64, VEL_H = 32;
    protected static final int KAASKNABBEL = 0, KNABBEL = 1, KRUIMEL = 2, GUH = 3, WOLK = 4, JERRYCAN = 5, WORST = 6, SLAAPGUH = 7;

    protected final CompoundTag data;
    protected final CompoundTag opgave;
    protected final String soort;
    protected final boolean thuis;
    /** The play field. */
    protected int left, top, vx, vy, vw, vh;
    protected int muisX, muisY;
    protected boolean ingedrukt;
    protected int leeftijd;
    private boolean klaar;
    private int sluitTeller = -1;
    private Component melding;
    private int meldingKleur, meldingTeller;

    protected TaakSpel(CompoundTag data) {
        super(Component.translatable("gui.guhs.among.taak." + data.getStringOr("Soort", "")));
        this.data = data;
        this.soort = data.getStringOr("Soort", "");
        this.opgave = data.getCompoundOrEmpty("Opgave");
        this.thuis = data.getBooleanOr("Thuis", false);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        vx = left + 12;
        vy = top + 46;
        vw = W - 24;
        vh = H - 46 - 44;
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.among.taak.stop"), b -> onClose()).bounds(left + W / 2 - 40, top + H - 24, 80, 20).build());
    }

    // --- for the games -------------------------------------------------------------------------------------------------------

    /** Draws the play field (the frame, the texts and the feedback line are done). */
    protected abstract void teken(GuiGraphicsExtractor g, float partialTick);

    /** Every client tick while the task is not done. */
    protected void spelTick() {
    }

    /** The left mouse button went down inside the panel. */
    protected void druk(int x, int y) {
    }

    /** The left mouse button came up. */
    protected void los(int x, int y) {
    }

    protected boolean isKlaar() {
        return klaar;
    }

    /** The task is done: the answer goes to the server, a happy sound, the panel closes a moment later. */
    protected void gelukt(CompoundTag resultaat) {
        if (klaar) {
            return;
        }
        klaar = true;
        ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.TAAK_KLAAR, thuis ? -1 : data.getIntOr("Paneel", -1), 0, resultaat));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f, 0.6f));
        meld(Component.translatable("gui.guhs.among.taak.gelukt"), GROEN, 40);
        sluitTeller = 14;
    }

    /** A line of feedback under the play field. */
    protected void meld(Component tekst, int kleur, int ticks) {
        melding = tekst;
        meldingKleur = kleur;
        meldingTeller = ticks;
    }

    protected void mis(String sleutel) {
        meld(Component.translatable("gui.guhs.among.taak." + sleutel), ROOD, 50);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.5f));
    }

    protected void tik(float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BONE_BLOCK_BREAK, pitch));
    }

    protected static boolean in(int x, int y, int x0, int y0, int x1, int y1) {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    /** A cell of the sprite sheet, drawn {@code maat} pixels big. */
    protected static void plaatje(GuiGraphicsExtractor g, int cel, int x, int y, int maat) {
        g.blit(RenderPipelines.GUI_TEXTURED, VEL, x, y, (cel % 4) * 16, (cel / 4) * 16, maat, maat, 16, 16, VEL_W, VEL_H);
    }

    /** A box with a light edge (a button, a slot, a screen). */
    protected static void vak(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int rand, int vulling) {
        g.fill(x0, y0, x1, y1, rand);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, vulling);
    }

    /** A bar from 0 to 1. */
    protected static void balk(GuiGraphicsExtractor g, int x, int y, int w, int h, float deel, int kleur) {
        vak(g, x - 1, y - 1, x + w + 1, y + h + 1, RAND, DONKER);
        g.fill(x, y, x + Math.round(w * Math.max(0f, Math.min(1f, deel))), y + h, kleur);
    }

    // --- the screen ----------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        leeftijd++;
        if (meldingTeller > 0) {
            meldingTeller--;
        }
        if (sluitTeller >= 0) {
            if (--sluitTeller < 0) {
                minecraft.setScreen(null);
            }
            return;
        }
        if (!klaar) {
            spelTick();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0 && !klaar) {
            muisX = (int) event.x();
            muisY = (int) event.y();
            ingedrukt = true;
            druk(muisX, muisY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && ingedrukt) {
            ingedrukt = false;
            muisX = (int) event.x();
            muisY = (int) event.y();
            if (!klaar) {
                los(muisX, muisY);
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        if (!klaar) {
            ClientPacketDistributor.sendToServer(new AmongPayloads.Actie(AmongPayloads.TAAK_STOP, data.getIntOr("Paneel", -1), 0));
        }
        super.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        muisX = mouseX;
        muisY = mouseY;
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, RAND);
        g.fill(left, top, left + W, top + H, PANEEL);
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, top + 7, TEKST);
        Component waar;
        int stappen = data.getIntOr("Stappen", 1);
        if (thuis) {
            waar = Component.translatable("gui.guhs.among.taak.thuis");
        } else if (stappen > 1) {
            waar = Component.translatable("gui.guhs.among.taak.waar_stap", Tekst.get(data, "Kamer"), data.getIntOr("Stap", 1), stappen);
        } else {
            waar = Component.translatable("gui.guhs.among.taak.waar", Tekst.get(data, "Kamer"));
        }
        g.centeredText(font, waar, width / 2, top + 19, DOF);
        g.centeredText(font, uitleg(), width / 2, top + 32, GOUD);
        g.fill(vx - 1, vy - 1, vx + vw + 1, vy + vh + 1, 0xFF5A3A4C);
        g.fill(vx, vy, vx + vw, vy + vh, VAK);
        teken(g, partialTick);
        if (meldingTeller > 0 && melding != null) {
            g.centeredText(font, melding, width / 2, vy + vh + 5, meldingKleur);
        }
    }

    /** The one line that says what to do. */
    protected Component uitleg() {
        return Component.translatable("gui.guhs.among.taak." + soort + ".bezig");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
