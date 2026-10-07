package nl.juiced.guhs.feature.guhpixel.guhkade.client;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.guhkade.Guhkade;
import nl.juiced.guhs.feature.guhpixel.guhkade.GuhkadePayloads;
import nl.juiced.guhs.feature.guhpixel.guhkade.GuhkadeSlice;
import nl.juiced.guhs.feature.guhpixel.guhkade.KastBlockEntity;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Doek;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Opname;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.PongSim;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Schermen;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Spel;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sprite;
import org.lwjgl.glfw.GLFW;

/**
 * The screen of a Guhkade cabinet: the game itself on the left (the same little simulation the server will play again,
 * 30 steps a second), the cabinet's top 5 with names on the right. Flappy Guh: space bar, W, arrow up or a click = flap.
 * Mika-Pong: W / S or the arrow keys move your paddle. When the game is over the input of every step goes to the server,
 * and the server answers with the score it counted, your place and the new list.
 */
public class KastScherm extends Screen {
    public static final Identifier BLAD = Guhs.id("textures/guhkade/sprites.png");
    private static final int RAND = 0xFFF7B6CB, PANEEL = 0xF0301A26, TEKST = 0xFFFFE6EE, GOUD = 0xFFFFD27A, DOF = 0xFFB090A0, ROZE = 0xFFFF9AC8;
    private static final int LIJST_B = 132;

    private enum Fase { TITEL, SPEEL, WACHT, UIT }

    private final BlockPos pos;
    private final Spel spel;
    private long seed;
    private List<KastBlockEntity.Regel> top;
    private int best;
    private float schaal = 1.5f;
    private int left, top0, panelB, panelH, veldB, veldH;
    private Fase fase = Fase.TITEL;
    private Sim sim;
    @Nullable
    private Opname opname;
    private long beginMs, faseMs;
    private boolean flap, omhoog, omlaag, verstuurd;
    // the server's answer
    private int score = -1, plaats, verslagen;
    private boolean record;
    @Nullable
    private Button startKnop;

    public KastScherm(GuhkadePayloads.Open p) {
        super(Component.translatable("block.guhs.guhkade_kast_" + Spel.op(p.spel()).id));
        this.pos = p.pos();
        this.spel = Spel.op(p.spel());
        this.seed = p.seed();
        this.top = KastBlockEntity.leesRegels(p.scherm());
        this.best = p.best();
        this.sim = spel.nieuw(seed);
        this.faseMs = Util.getMillis();
    }

    public BlockPos pos() {
        return pos;
    }

    /** The server counted the game. */
    public void uitslag(GuhkadePayloads.Uitslag u) {
        seed = u.seed();
        top = KastBlockEntity.leesRegels(u.scherm());
        best = u.best();
        score = u.score();
        plaats = u.plaats();
        record = u.record();
        verslagen = u.verslagen();
        if (fase == Fase.WACHT) {
            zet(Fase.UIT);
            if (record) {
                geluid(GuhkadeSlice.RECORD.get(), 1.0f);
            }
        }
    }

    @Override
    protected void init() {
        // the biggest game picture that fits next to the list
        schaal = 1f;
        for (float s : new float[] {2f, 1.75f, 1.5f, 1.25f}) {
            if (Sim.B * s + LIJST_B + 40 <= width && Sim.H * s + 56 <= height) {
                schaal = s;
                break;
            }
        }
        veldB = Math.round(Sim.B * schaal);
        veldH = Math.round(Sim.H * schaal);
        panelB = veldB + LIJST_B + 28;
        panelH = Math.max(veldH + 44, 190);
        left = (width - panelB) / 2;
        top0 = (height - panelH) / 2;
        int kx = left + veldB + 20;
        startKnop = addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhkade.knop.start"), b -> begin())
                .bounds(kx, top0 + panelH - 50, LIJST_B, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.guhs.guhkade.knop.doei"), b -> onClose())
                .bounds(kx, top0 + panelH - 26, LIJST_B, 20).build());
        knoppen();
    }

    private void knoppen() {
        if (startKnop != null) {
            startKnop.visible = fase == Fase.TITEL || fase == Fase.UIT;
            startKnop.setMessage(Component.translatable(fase == Fase.UIT ? "gui.guhs.guhkade.knop.opnieuw" : "gui.guhs.guhkade.knop.start"));
        }
    }

    private void zet(Fase f) {
        fase = f;
        faseMs = Util.getMillis();
        knoppen();
    }

    private int veldX() {
        return left + 10;
    }

    private int veldY() {
        return top0 + 24;
    }

    // --- the game ------------------------------------------------------------------------------------------------------------

    private void begin() {
        if (fase != Fase.TITEL && fase != Fase.UIT) {
            return;
        }
        sim = spel.nieuw(seed);
        opname = new Opname(sim.bits());
        beginMs = Util.getMillis();
        flap = verstuurd = false;
        score = -1;
        ClientPacketDistributor.sendToServer(new GuhkadePayloads.Start(pos, seed));
        geluid(GuhkadeSlice.MUNTJE.get(), 1.0f);
        zet(Fase.SPEEL);
        if (spel == Spel.FLAPPY) {
            flap = true;                // (the click that starts the game is the first flap)
        }
    }

    /** Plays the steps that are due (30 a second, by the clock; after a hiccup the game simply goes on from where it was). */
    private void loop() {
        if (fase != Fase.SPEEL || opname == null) {
            return;
        }
        long nu = Util.getMillis();
        int doel = (int) ((nu - beginMs) * Sim.HZ / 1000L);
        if (doel - sim.stappen() > 6) {
            beginMs = nu - (sim.stappen() + 1) * 1000L / Sim.HZ;
            doel = sim.stappen() + 1;
        }
        while (sim.stappen() < doel && !sim.af()) {
            int invoer;
            if (spel == Spel.FLAPPY) {
                invoer = flap ? 1 : 0;
                flap = false;
            } else {
                invoer = omhoog == omlaag ? 0 : omhoog ? PongSim.OMHOOG : PongSim.OMLAAG;
            }
            opname.schrijf(invoer);
            sim.stap(invoer);
            int g = sim.geluid();
            if ((g & Sim.GELUID_AF) != 0) {
                geluid(GuhkadeSlice.AF.get(), 1.0f);
            } else if ((g & Sim.GELUID_PUNT) != 0) {
                geluid(GuhkadeSlice.PUNT.get(), 1.0f);
            } else if ((g & Sim.GELUID_TIK) != 0) {
                geluid(GuhkadeSlice.PIEP.get(), spel == Spel.FLAPPY ? 1.5f : 1.0f);
            } else if ((g & Sim.GELUID_BONS) != 0) {
                geluid(GuhkadeSlice.PIEP.get(), 0.7f);
            }
        }
        if (sim.af()) {
            verstuur();
            zet(Fase.WACHT);
        }
    }

    private void verstuur() {
        if (!verstuurd && opname != null) {
            verstuurd = true;
            ClientPacketDistributor.sendToServer(new GuhkadePayloads.Klaar(pos, seed, opname.stappen(), opname.bytes()));
        }
    }

    private void geluid(SoundEvent s, float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, 0.5f));
        }
    }

    // --- input ---------------------------------------------------------------------------------------------------------------

    private boolean knop() {
        if (fase == Fase.SPEEL) {
            flap = true;
            return true;
        }
        if (fase == Fase.TITEL || (fase == Fase.UIT && Util.getMillis() - faseMs > 500)) {
            begin();
            return true;
        }
        return fase == Fase.WACHT || fase == Fase.UIT;
    }

    private boolean stuur(int key, boolean neer) {
        switch (key) {
            case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> omhoog = neer;
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> omlaag = neer;
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            return knop();
        }
        if (stuur(key, true)) {
            if (spel == Spel.FLAPPY && (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) && fase == Fase.SPEEL) {
                flap = true;
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (stuur(event.key(), false)) {
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.x() >= veldX() && event.x() < veldX() + veldB && event.y() >= veldY() && event.y() < veldY() + veldH) {
            return knop();
        }
        return false;
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || minecraft.player.distanceToSqr(Vec3.atCenterOf(pos)) > Guhkade.BEREIK * Guhkade.BEREIK) {
            onClose();
            return;
        }
        // the list stays fresh while you look at it (a guh's new score, somebody else's)
        if (fase == Fase.TITEL && minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof KastBlockEntity kast && !kast.top().isEmpty()) {
            top = kast.top();
        }
    }

    @Override
    public void removed() {
        if (fase == Fase.SPEEL) {
            verstuur();     // (walking away in the middle of a game: it counts as far as it got)
        }
        ClientPacketDistributor.sendToServer(new GuhkadePayloads.Stop(pos));
        super.removed();
    }

    // --- drawing -------------------------------------------------------------------------------------------------------------

    /** The game's picture drawn with the GUI's own rectangles and the sprite sheet. */
    private static final class GuiDoek implements Doek {
        private final GuiGraphicsExtractor g;

        GuiDoek(GuiGraphicsExtractor g) {
            this.g = g;
        }

        @Override
        public void rect(int x, int y, int b, int h, int argb) {
            g.fill(x, y, x + b, y + h, argb);
        }

        @Override
        public void sprite(Sprite s, int x, int y) {
            g.blit(RenderPipelines.GUI_TEXTURED, BLAD, x, y, s.u, s.v, s.b, s.h, Sprite.BLAD, Sprite.BLAD);
        }
    }

    /** A word for the pixel font: the language's own, in capitals. */
    static String woord(String key) {
        return I18n.get("gui.guhs.guhkade.scherm." + key).toUpperCase(Locale.ROOT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        loop();
        g.fill(left - 1, top0 - 1, left + panelB + 1, top0 + panelH + 1, RAND);
        g.fill(left, top0, left + panelB, top0 + panelH, PANEEL);
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + 10, top0 + 9, ROZE, false);
        // the screen in its bezel
        int x = veldX(), y = veldY();
        g.fill(x - 4, y - 4, x + veldB + 4, y + veldH + 4, 0xFF1A0C18);
        g.fill(x - 2, y - 2, x + veldB + 2, y + veldH + 2, 0xFF5A2E44);
        g.enableScissor(x, y, x + veldB, y + veldH);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(schaal, schaal);
        GuiDoek d = new GuiDoek(g);
        long nu = Util.getMillis();
        int topScore = top.isEmpty() ? 0 : top.get(0).score();
        switch (fase) {
            case TITEL -> Schermen.titel(d, sim, woord("naam." + spel.id), woord("top"), topScore, woord("start"), (nu - faseMs) / 500 % 2 == 0);
            case SPEEL -> sim.teken(d);
            case WACHT -> {
                sim.teken(d);
                if (nu - faseMs > 350) {
                    Schermen.af(d, woord(sim.stappen() >= Sim.MAX_STAPPEN ? "tijd" : "af"), sim.score(), false);
                }
            }
            case UIT -> {
                sim.teken(d);
                Schermen.af(d, woord(record && score > 0 ? "record" : sim.stappen() >= Sim.MAX_STAPPEN ? "tijd" : "af"), Math.max(0, score), record && score > 0);
            }
        }
        g.pose().popMatrix();
        g.disableScissor();
        lijst(g, left + veldB + 20, top0 + 24);
        // one line under the screen: what to press, or what happened
        Component onder = switch (fase) {
            case TITEL -> Component.translatable("gui.guhs.guhkade.uitleg." + spel.id);
            case SPEEL -> Component.translatable("gui.guhs.guhkade.uitleg." + spel.id);
            case WACHT -> Component.translatable(nu - faseMs > 4000 ? "gui.guhs.guhkade.geen_antwoord" : "gui.guhs.guhkade.tellen");
            case UIT -> verslagen > 0 ? Component.translatable("gui.guhs.guhkade.uit.verslagen", verslagen)
                    : plaats > 0 ? Component.translatable("gui.guhs.guhkade.uit.plaats", plaats)
                    : Component.translatable("gui.guhs.guhkade.uit.gewoon");
        };
        List<FormattedCharSequence> regels = font.split(onder, veldB + 4);
        if (!regels.isEmpty()) {
            g.text(font, regels.get(0), x - 2, y + veldH + 8, fase == Fase.UIT && (verslagen > 0 || plaats > 0) ? GOUD : DOF, false);
        }
    }

    /** The cabinet's top 5: place, name (a guh's in pink with its little face) and score; then your own best ever. */
    private void lijst(GuiGraphicsExtractor g, int x, int y) {
        g.text(font, Component.translatable("gui.guhs.guhkade.top5").withStyle(ChatFormatting.BOLD), x, y, GOUD, false);
        g.fill(x, y + 11, x + LIJST_B, y + 12, RAND);
        for (int i = 0; i < KastBlockEntity.TOP; i++) {
            int ry = y + 17 + i * 13;
            g.text(font, (i + 1) + ".", x, ry, DOF, false);
            if (i >= top.size()) {
                g.text(font, "---", x + 14, ry, DOF, false);
                continue;
            }
            KastBlockEntity.Regel r = top.get(i);
            String punten = Integer.toString(r.score());
            int naamX = x + 14;
            if (r.guh()) {
                g.blit(RenderPipelines.GUI_TEXTURED, BLAD, naamX, ry - 2, Sprite.GUH_OP.u, Sprite.GUH_OP.v, Sprite.GUH_OP.b, Sprite.GUH_OP.h, Sprite.BLAD, Sprite.BLAD);
                naamX += Sprite.GUH_OP.b + 2;
            }
            int ruimte = x + LIJST_B - font.width(punten) - 4 - naamX;
            String naam = font.plainSubstrByWidth(r.naam().getString(), ruimte);
            g.text(font, naam, naamX, ry, r.guh() ? ROZE : r.huis() ? DOF : TEKST, false);
            g.text(font, punten, x + LIJST_B - font.width(punten), ry, r.huis() ? DOF : GOUD, false);
        }
        int by = y + 17 + KastBlockEntity.TOP * 13 + 4;
        g.fill(x, by - 3, x + LIJST_B, by - 2, 0xFF5A2E44);
        g.text(font, Component.translatable("gui.guhs.guhkade.jouw_best", best), x, by, TEKST, false);
        int ty = by + 13;
        for (FormattedCharSequence regel : font.split(Component.translatable("gui.guhs.guhkade.guh_tip"), LIJST_B)) {
            if (ty + 9 > top0 + panelH - 54) {
                break;
            }
            g.text(font, regel, x, ty, DOF, false);
            ty += 10;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
