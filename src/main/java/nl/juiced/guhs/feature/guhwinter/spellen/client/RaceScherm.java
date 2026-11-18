package nl.juiced.guhs.feature.guhwinter.spellen.client;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.guhpixel.guhkade.GuhkadeSlice;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Doek;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Opname;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sprite;
import nl.juiced.guhs.feature.guhwinter.spellen.Pepernotenrace;
import nl.juiced.guhs.feature.guhwinter.spellen.SpellenPayloads;
import nl.juiced.guhs.feature.guhwinter.spellen.race.RaceSim;
import org.lwjgl.glfw.GLFW;

/**
 * De Pepernotenrace on your screen: a "phone" in the middle of a dark winter screen with the runner on it (the same
 * simulation the server plays again, 30 steps a second), the top 10 next to it. A / D or the arrows: change lanes; W,
 * arrow up or space: jump; S or arrow down: duck. When the run is over its input goes to the server, which answers with
 * the score it counted and the pepernoten it gave.
 */
public class RaceScherm extends Screen {
    private static final int LIJST_B = 150, ACHTER = 0xFF0E1428, RAND = 0xFFFFD27A, TEKST = 0xFFFFE6EE, DOF = 0xFF9AA6C8, GOUD = 0xFFFFD27A;

    private enum Fase { TITEL, SPEEL, WACHT, UIT }

    private final BlockPos pos;
    private long seed;
    private List<SpellenPayloads.Regel> top;
    private int best;
    private boolean eersteGehad;
    private Fase fase = Fase.TITEL;
    private RaceSim sim;
    @Nullable
    private Opname opname;
    private final ArrayDeque<Integer> wachtrij = new ArrayDeque<>();
    private long beginMs, faseMs;
    private boolean verstuurd;
    private float schaal = 2f;
    private int telX, telY, telB, telH;
    // the server's answer
    private boolean geldig = true, record;
    private int score = -1, gekregen, plaats;

    public RaceScherm(SpellenPayloads.RaceOpen p) {
        super(Component.translatable("block.guhs.guhwinter_arcadekast"));
        this.pos = p.pos();
        this.seed = p.seed();
        this.top = SpellenPayloads.leesRegels(p.top());
        this.best = p.best();
        this.eersteGehad = p.eersteGehad();
        this.sim = new RaceSim(seed);
        this.faseMs = Util.getMillis();
    }

    public BlockPos pos() {
        return pos;
    }

    public void uitslag(SpellenPayloads.RaceUitslag u) {
        seed = u.seed();
        top = SpellenPayloads.leesRegels(u.top());
        best = u.best();
        geldig = u.geldig();
        score = u.score();
        gekregen = u.gekregen();
        plaats = u.plaats();
        record = u.record();
        eersteGehad = true;
        if (fase == Fase.WACHT) {
            zet(Fase.UIT);
            if (record) {
                geluid(GuhkadeSlice.RECORD.get(), 1f);
            }
        }
    }

    @Override
    protected void init() {
        schaal = 1f;
        for (float s : new float[] {3f, 2.5f, 2f, 1.5f}) {
            if (RaceSim.HOOG * s + 60 <= height && RaceSim.BREED * s + 40 <= width) {
                schaal = s;
                break;
            }
        }
        telB = Math.round(RaceSim.BREED * schaal);
        telH = Math.round(RaceSim.HOOG * schaal);
        boolean lijst = width >= telB + 2 * (LIJST_B + 40);
        telX = (width - telB) / 2;
        if (!lijst && width >= telB + LIJST_B + 60) {
            telX = 20 + 10;
        }
        telY = (height - telH) / 2;
    }

    private void zet(Fase f) {
        fase = f;
        faseMs = Util.getMillis();
    }

    private void begin() {
        if (fase != Fase.TITEL && fase != Fase.UIT) {
            return;
        }
        sim = new RaceSim(seed);
        opname = new Opname(sim.bits());
        wachtrij.clear();
        beginMs = Util.getMillis();
        verstuurd = false;
        score = -1;
        ClientPacketDistributor.sendToServer(new SpellenPayloads.RaceStart(pos, seed));
        geluid(GuhkadeSlice.MUNTJE.get(), 1.2f);
        zet(Fase.SPEEL);
    }

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
            int invoer = wachtrij.isEmpty() ? 0 : wachtrij.poll();
            opname.schrijf(invoer);
            sim.stap(invoer);
            int g = sim.geluid();
            if ((g & Sim.GELUID_AF) != 0) {
                geluid(GuhkadeSlice.AF.get(), 1f);
            } else if ((g & Sim.GELUID_PUNT) != 0) {
                geluid(GuhkadeSlice.PUNT.get(), 1.3f);
            } else if ((g & Sim.GELUID_BONS) != 0) {
                geluid(GuhkadeSlice.PIEP.get(), 0.8f);
            } else if ((g & Sim.GELUID_TIK) != 0) {
                geluid(GuhkadeSlice.PIEP.get(), 1.6f);
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
            ClientPacketDistributor.sendToServer(new SpellenPayloads.RaceKlaar(pos, seed, opname.stappen(), opname.bytes()));
        }
    }

    private void geluid(SoundEvent s, float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, 0.45f));
        }
    }

    // --- input -------------------------------------------------------------------------------------------------------------

    private void druk(int invoer) {
        if (wachtrij.size() < 3) {
            wachtrij.add(invoer);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (fase == Fase.SPEEL) {
            switch (key) {
                case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> druk(RaceSim.LINKS);
                case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> druk(RaceSim.RECHTS);
                case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_SPACE -> druk(RaceSim.SPRING);
                case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> druk(RaceSim.BUK);
                default -> {
                    return super.keyPressed(event);
                }
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) {
            if (fase == Fase.TITEL || fase == Fase.UIT && Util.getMillis() - faseMs > 600) {
                begin();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.x() >= telX && event.x() < telX + telB && event.y() >= telY && event.y() < telY + telH) {
            if (fase == Fase.TITEL || fase == Fase.UIT && Util.getMillis() - faseMs > 600) {
                begin();
            } else if (fase == Fase.SPEEL) {
                druk(RaceSim.SPRING);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null
                || minecraft.player.distanceToSqr(Vec3.atCenterOf(pos)) > Pepernotenrace.BEREIK * Pepernotenrace.BEREIK) {
            onClose();
        }
    }

    @Override
    public void removed() {
        if (fase == Fase.SPEEL) {
            verstuur();      // (closing mid-run: it counts as far as it got)
        }
        ClientPacketDistributor.sendToServer(new SpellenPayloads.RaceStop(pos));
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- drawing -----------------------------------------------------------------------------------------------------------

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
        }
    }

    static String woord(String key) {
        return I18n.get("gui.guhs.guhwinter_spellen.race.scherm." + key).toUpperCase(Locale.ROOT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        loop();
        // a dark winter night all round, with a few still stars
        g.fill(0, 0, width, height, ACHTER);
        for (int i = 0; i < 40; i++) {
            int sx = (int) ((i * 7919L) % Math.max(1, width)), sy = (int) ((i * 104729L) % Math.max(1, height));
            g.fill(sx, sy, sx + 1, sy + 1, i % 3 == 0 ? 0xFF8090C0 : 0xFF404C78);
        }
        // the phone
        int r = Math.max(6, Math.round(5 * schaal));
        g.fill(telX - r - 2, telY - r * 3, telX + telB + r + 2, telY + telH + r * 3, 0xFF2A1430);
        g.fill(telX - r, telY - r * 3 + 2, telX + telB + r, telY + telH + r * 3 - 2, 0xFFFF9EC4);
        g.fill(telX - 2, telY - 2, telX + telB + 2, telY + telH + 2, 0xFF2A1430);
        g.fill(telX + telB / 2 - 12, telY - r * 2, telX + telB / 2 + 12, telY - r * 2 + 3, 0xFF2A1430);            // speaker
        g.fill(telX + telB / 2 - 16, telY + telH + r + 2, telX + telB / 2 + 16, telY + telH + r + 6, 0xFFFFD6E8);  // home bar
        g.enableScissor(telX, telY, telX + telB, telY + telH);
        g.pose().pushMatrix();
        g.pose().translate(telX, telY);
        g.pose().scale(schaal, schaal);
        GuiDoek d = new GuiDoek(g);
        long nu = Util.getMillis();
        switch (fase) {
            case TITEL -> sim.tekenTitel(d, woord("naam"), woord("start"), woord("uitleg"), (nu - faseMs) / 500 % 2 == 0);
            case SPEEL -> sim.teken(d);
            case WACHT -> {
                sim.teken(d);
                if (nu - faseMs > 350) {
                    RaceSim.tekenAf(d, woord("af"), sim.score(), "", false);
                }
            }
            case UIT -> {
                sim.teken(d);
                String extra = !geldig ? woord("ongeldig") : gekregen > 0 ? "+" + gekregen + " " + woord("noten") : "";
                RaceSim.tekenAf(d, woord(record ? "record" : "af"), Math.max(0, score), extra, record);
            }
        }
        g.pose().popMatrix();
        g.disableScissor();
        // the list (and the keys) next to the phone, when there is room
        int lx = telX + telB + r + 24;
        if (lx + LIJST_B <= width - 8) {
            lijst(g, lx, telY);
        }
        Component onder = switch (fase) {
            case TITEL, SPEEL -> Component.translatable("gui.guhs.guhwinter_spellen.race.toetsen");
            case WACHT -> Component.translatable(nu - faseMs > 4000 ? "gui.guhs.guhwinter_spellen.race.geen_antwoord" : "gui.guhs.guhwinter_spellen.race.tellen");
            case UIT -> !geldig ? Component.translatable("gui.guhs.guhwinter_spellen.race.uit.ongeldig")
                    : plaats > 0 ? Component.translatable("gui.guhs.guhwinter_spellen.race.uit.plaats", plaats)
                    : Component.translatable("gui.guhs.guhwinter_spellen.race.uit.gewoon");
        };
        List<FormattedCharSequence> regels = font.split(onder, Math.max(120, telB + 60));
        int ty = telY + telH + r * 3 + 4;
        for (FormattedCharSequence regel : regels) {
            if (ty + 9 > height) {
                break;
            }
            g.text(font, regel, (width - font.width(regel)) / 2, ty, fase == Fase.UIT && plaats > 0 ? GOUD : DOF, false);
            ty += 10;
        }
    }

    private void lijst(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 6, y - 6, x + LIJST_B + 6, y + 12 + 10 * 13 + 64, 0xC0202848);
        g.text(font, Component.translatable("gui.guhs.guhwinter_spellen.race.top10").withStyle(ChatFormatting.BOLD), x, y, GOUD, false);
        g.fill(x, y + 11, x + LIJST_B, y + 12, RAND);
        for (int i = 0; i < 10; i++) {
            int ry = y + 17 + i * 13;
            g.text(font, (i + 1) + ".", x, ry, DOF, false);
            if (i >= top.size()) {
                g.text(font, "---", x + 18, ry, DOF, false);
                continue;
            }
            SpellenPayloads.Regel rg = top.get(i);
            String punten = Integer.toString(rg.score());
            int ruimte = LIJST_B - 18 - font.width(punten) - 4;
            g.text(font, font.plainSubstrByWidth(rg.naam(), ruimte), x + 18, ry, i == 0 ? GOUD : TEKST, false);
            g.text(font, punten, x + LIJST_B - font.width(punten), ry, i == 0 ? GOUD : TEKST, false);
        }
        int by = y + 17 + 10 * 13 + 4;
        g.fill(x, by - 3, x + LIJST_B, by - 2, 0xFF5A6488);
        g.text(font, Component.translatable("gui.guhs.guhwinter_spellen.race.jouw_best", best), x, by, TEKST, false);
        Component tip = eersteGehad ? Component.translatable("gui.guhs.guhwinter_spellen.race.tip", Pepernotenrace.MAX_PER_RUN)
                : Component.translatable("gui.guhs.guhwinter_spellen.race.tip_eerste", Pepernotenrace.EERSTE_BONUS);
        int ty = by + 13;
        for (FormattedCharSequence regel : font.split(tip, LIJST_B)) {
            g.text(font, regel, x, ty, DOF, false);
            ty += 10;
        }
    }
}
