package nl.juiced.guhs.feature.guhwaiispellen.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhwaiispellen.GuhwaiiSpellenPayloads;
import nl.juiced.guhs.feature.guhwaiispellen.HulaKaart;
import nl.juiced.guhs.feature.guhwaiispellen.HulaLiedje;
import nl.juiced.guhs.feature.spelen.Niveau;
import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
/**
 * The hula dance in your game (3.0): the steps slide in from the right towards the hibiscus on the left, and when one
 * reaches it you press its key on the beat (A hips left, D hips right, W arms up, S down the knees, space VAHOEG!). Your
 * game judges it at once against the song's clock (started when the song started) and tells the server which step it was
 * and how many ms after the start; the server counts. While you dance your keys don't walk. Lilo-guh on the podium sways
 * her hips on the beat for everybody who hears the song.
 */
public final class HulaClient {
    /** The step you dance, and the song's start (our clock). */
    private static int npc = -1;
    @Nullable
    private static HulaLiedje liedje;
    private static long startMs;
    private static int record;
    private static final Map<Integer, HulaKaart.Oordeel> GEDANST = new HashMap<>();
    private static int score, combo, maxCombo, serverScore;
    private static boolean serverEinde;
    /** Feedback popups: text, colour, until (ms). */
    private record Popup(Component tekst, int kleur, long tot) {
    }

    private static final List<Popup> POPUPS = new ArrayList<>();
    /** Lilo-guhs that dance (npc id -> song + start), also for the ones who only watch. */
    private record Dans(HulaLiedje liedje, long start) {
    }

    private static final Map<Integer, Dans> DANSEN = new HashMap<>();
    @Nullable
    private static CompoundTag uitslag;
    private static long uitslagTot;

    public static void start(CompoundTag data) {
        HulaLiedje l = HulaLiedje.of(data.getIntOr("Liedje", 0));
        long now = Util.getMillis();
        DANSEN.put(data.getIntOr("Npc", 0), new Dans(l, now));
        if (!data.getBooleanOr("Danser", false)) {
            return;
        }
        npc = data.getIntOr("Npc", 0);
        liedje = l;
        startMs = now;
        record = data.getIntOr("Record", 0);
        GEDANST.clear();
        POPUPS.clear();
        score = combo = maxCombo = serverScore = 0;
        serverEinde = false;
        uitslag = null;
    }

    public static void stand(CompoundTag data) {
        int id = data.getIntOr("Npc", 0);
        if (data.getBooleanOr("Einde", false)) {
            DANSEN.remove(id);
            if (id == npc && liedje != null) {
                uitslag = data;
                uitslagTot = Util.getMillis() + 7000;
                if (data.contains("Score")) {
                    serverScore = data.getIntOr("Score", 0);
                }
                npc = -1;
                liedje = null;
            }
            return;
        }
        if (id == npc) {
            serverScore = data.getIntOr("Score", 0);
        }
    }

    public static void uit() {
        npc = -1;
        liedje = null;
        DANSEN.clear();
        POPUPS.clear();
        uitslag = null;
    }

    public static boolean danst() {
        return liedje != null && npc >= 0;
    }

    private static double songMs() {
        return Util.getMillis() - startMs;
    }

    /** How long a step takes to slide in (ms). */
    private static double aanloop(HulaLiedje l) {
        return switch (l.niveau) {
            case MAKKELIJK -> 2400;
            case MEDIUM -> 1800;
            case LASTIG -> 1350;
        };
    }

    // --- your keys -------------------------------------------------------------------------------------------------------

    /** A key went down: the step it is (the nearest one of that move not danced yet), judged at once. */
    public static void toets(int key, int scan, int action) {
        if (!danst() || action != GLFW.GLFW_PRESS || Minecraft.getInstance().screen != null) {
            return;
        }
        var o = Minecraft.getInstance().options;
        HulaKaart.Pas pas = matcht(o.keyLeft, key, scan) ? HulaKaart.Pas.LINKS : matcht(o.keyRight, key, scan) ? HulaKaart.Pas.RECHTS
                : matcht(o.keyUp, key, scan) ? HulaKaart.Pas.OMHOOG : matcht(o.keyDown, key, scan) ? HulaKaart.Pas.OMLAAG
                : matcht(o.keyJump, key, scan) ? HulaKaart.Pas.VAHOEG : null;
        if (pas == null) {
            return;
        }
        double nu = songMs();
        int venster = HulaKaart.venster(liedje, HulaKaart.Oordeel.GUH);
        HulaKaart.Noot best = null;
        double bestD = Double.MAX_VALUE;
        for (HulaKaart.Noot n : HulaKaart.van(liedje)) {
            if (n.pas() != pas || GEDANST.containsKey(n.index())) {
                continue;
            }
            double d = Math.abs(nu - liedje.ms(n.beat()));
            if (d <= venster + 40 && d < bestD) {
                bestD = d;
                best = n;
            }
        }
        if (best == null) {
            return;                                                  // (a key between the steps: nothing happens)
        }
        HulaKaart.Oordeel oordeel = HulaKaart.oordeel(liedje, nu - liedje.ms(best.beat()));
        oordeel(best, oordeel);
        ClientPacketDistributor.sendToServer(new GuhwaiiSpellenPayloads.HulaTik(npc, best.index(), (int) Math.round(nu), pas.ordinal()));
    }

    private static boolean matcht(KeyMapping m, int key, int scan) {
        return m.matches(new net.minecraft.client.input.KeyEvent(key, scan, 0));
    }

    private static void oordeel(HulaKaart.Noot n, HulaKaart.Oordeel o) {
        GEDANST.put(n.index(), o);
        if (o == HulaKaart.Oordeel.MIS) {
            combo = 0;
        } else {
            combo++;
            maxCombo = Math.max(maxCombo, combo);
            score += HulaKaart.punten(o, combo);
        }
        int kleur = switch (o) {
            case VAHOEG -> 0xFFFFD84D;
            case NJEG -> 0xFF7CF0A0;
            case GUH -> 0xFFFFFFFF;
            case MIS -> 0xFFFF7070;
        };
        POPUPS.add(new Popup(Component.translatable("gui.guhs.guhwaiispellen.hula.oordeel." + o.id()), kleur, Util.getMillis() + 450));
        if (POPUPS.size() > 3) {
            POPUPS.remove(0);
        }
    }

    /**
     * While dancing your movement keys are the dance: you stay on the mat. 1.1.0: the input's move vector can't be set
     * any more, so while you dance the player gets an input that never moves (no keys, no jump, no sneak) and gets its
     * keyboard input back afterwards.
     */
    public static void stilStaan(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.client.player.LocalPlayer p)) {
            return;
        }
        if (danst()) {
            if (!(p.input instanceof StilInput)) {
                p.input = new StilInput();
            }
        } else if (p.input instanceof StilInput) {
            p.input = new net.minecraft.client.player.KeyboardInput(Minecraft.getInstance().options);
        }
    }

    /** An input that stands still (the base ClientInput never reads the keys). */
    private static final class StilInput extends net.minecraft.client.player.ClientInput {
    }

    private static long laatsteTick;

    public static void tick() {
        long nu0 = Util.getMillis();
        long dt = laatsteTick == 0 ? 0 : nu0 - laatsteTick;
        laatsteTick = nu0;
        if (Minecraft.getInstance().isPaused() && dt > 0) {
            // (the game and its music stand still: so does our clock)
            startMs += dt;
            DANSEN.replaceAll((id, d) -> new Dans(d.liedje, d.start + dt));
        }
        if (!danst()) {
            return;
        }
        double nu = songMs();
        for (HulaKaart.Noot n : HulaKaart.van(liedje)) {
            if (!GEDANST.containsKey(n.index()) && liedje.ms(n.beat()) + HulaKaart.venster(liedje, HulaKaart.Oordeel.GUH) + 40 < nu) {
                oordeel(n, HulaKaart.Oordeel.MIS);
            }
        }
        POPUPS.removeIf(p -> p.tot < Util.getMillis());
    }

    // --- Lilo-guh sways ------------------------------------------------------------------------------------------------------

    /** Lilo-guh on the podium sways her hips, ears and head on the beat while her song plays (on top of her own animator). */
    public static void liloDanst() {
        SittingGuhRenderers.NpcAnimator eigen = SittingGuhRenderers.NPC_ANIMATORS.get(GuhNpcEntity.Kind.LILO_GUH);
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.LILO_GUH, (n, tick) -> {
            SittingGuhRenderers.NpcAnimator al = eigen;
            nl.juiced.guhs.client.GuhRenderFrame.BoneMove eigenBeweging = al == null ? null : al.animeer(n, tick);
            Dans d = DANSEN.get(n.getId());
            if (d == null) {
                return eigenBeweging;
            }
            double beat = (Util.getMillis() - d.start) / d.liedje.msPerBeat();
            float sway = (float) Math.sin(beat * Math.PI);
            float bob = (float) Math.abs(Math.sin(beat * Math.PI));
            // (from the bones' rest pose: GeckoLib 5 snapshots are relative to it and fresh every frame)
            return bones -> {
                if (eigenBeweging != null) {
                    eigenBeweging.apply(bones);
                }
                bones.ifPresent("body", b -> {
                    b.setRotZ(sway * 0.22f);
                    b.setTranslateY(bob * 0.6f);
                });
                bones.ifPresent("head", b -> b.setRotZ(-sway * 0.15f));
                bones.ifPresent("ear_left", b -> b.setRotZ(sway * 0.3f));
                bones.ifPresent("ear_right", b -> b.setRotZ(sway * 0.3f));
            };
        });
    }

    /** Does this Lilo-guh dance now (in our game)? */
    public static boolean danstNu(int npcId) {
        return DANSEN.containsKey(npcId);
    }

    // --- the panel ---------------------------------------------------------------------------------------------------------

    private static final int[] KLEUR = {0xFFFF7EB8, 0xFF3CE0C8, 0xFFFFD84D, 0xFFB98CFF, 0xFFFFA630};

    public static void hud(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        if (!danst()) {
            if (uitslag != null && Util.getMillis() < uitslagTot) {
                int x0 = w / 2 - 110, y0 = 6;
                g.fill(x0, y0, x0 + 220, y0 + 34, 0xB0401040);
                g.centeredText(font, Component.translatable("gui.guhs.guhwaiispellen.hula.einde", serverScore, uitslag.getIntOr("MaxCombo", 0)),
                        w / 2, y0 + 5, 0xFFFFE6A0);
                g.centeredText(font, Component.translatable("gui.guhs.guhwaiispellen.hula.telling", uitslag.getIntOr("vahoeg", 0), uitslag.getIntOr("njeg", 0),
                        uitslag.getIntOr("guh", 0), uitslag.getIntOr("mis", 0)), w / 2, y0 + 19, 0xFFFF9EC8);
            }
            return;
        }
        double nu = songMs();
        double aanloop = aanloop(liedje);
        // the lane
        int laneW = Math.min(360, w - 40), x0 = w / 2 - laneW / 2, y0 = h - 104, laneH = 30;
        g.fill(x0 - 2, y0 - 2, x0 + laneW + 2, y0 + laneH + 2, 0xC0FFFFFF & 0x80FFFFFF);
        g.fill(x0, y0, x0 + laneW, y0 + laneH, 0xB02A1030);
        int hitX = x0 + 28, cy = y0 + laneH / 2;
        // the beat lines sliding along
        double mpb = liedje.msPerBeat();
        int firstBeat = (int) Math.floor(nu / mpb);
        for (int b = firstBeat; b < firstBeat + 12; b++) {
            double dt = b * mpb - nu;
            int x = hitX + (int) (dt / aanloop * (laneW - 40));
            if (x > x0 && x < x0 + laneW) {
                g.fill(x, y0 + 3, x + 1, y0 + laneH - 3, b % 4 == 0 ? 0x70FFFFFF : 0x30FFFFFF);
            }
        }
        // the hibiscus you hit on (it pulses on the beat)
        double fase = (nu / mpb) % 1.0;
        int puls = (int) (3 * Math.max(0, 1 - fase * 3));
        bloem(g, hitX, cy, 11 + puls);
        // the steps
        for (HulaKaart.Noot n : HulaKaart.van(liedje)) {
            double dt = liedje.ms(n.beat()) - nu;
            if (dt > aanloop + 200) {
                break;
            }
            if (GEDANST.containsKey(n.index()) || dt < -400) {
                continue;
            }
            int x = hitX + (int) (dt / aanloop * (laneW - 40));
            if (x > x0 + laneW - 8) {
                continue;
            }
            int c = KLEUR[n.pas().ordinal()];
            g.fill(x - 9, cy - 9, x + 9, cy + 9, 0xFF000000 | 0x301020);
            g.fill(x - 8, cy - 8, x + 8, cy + 8, c);
            g.centeredText(font, n.pas().pijl, x, cy - 4, 0xFF2A1030);
            g.centeredText(font, toets(n.pas()), x, y0 + laneH + 3, 0xFFEEDDEE);
        }
        // popups, score, combo, the song
        int py = y0 - 14;
        for (Popup p : POPUPS) {
            g.centeredText(font, p.tekst, hitX + 40, py, p.kleur);
            py -= 10;
        }
        g.text(font, Component.translatable("gui.guhs.guhwaiispellen.hula.score", score), x0 + laneW - 120, y0 - 24, 0xFFFFE6A0, true);
        if (combo > 1) {
            g.text(font, Component.translatable("gui.guhs.guhwaiispellen.hula.combo", combo), x0 + laneW - 120, y0 - 13, 0xFFFF9EC8, true);
        }
        g.text(font, liedje.naam().copy().withStyle(ChatFormatting.ITALIC), x0, y0 - 24, 0xFFFFFFFF, true);
        g.text(font, Component.translatable("gui.guhs.guhwaiispellen.hula.record", record), x0, y0 - 13, 0xFFD8B8E8, true);
        List<HulaKaart.Noot> kaart = HulaKaart.van(liedje);
        double eind = liedje.ms(kaart.get(kaart.size() - 1).beat());
        int voortgang = (int) (laneW * Mth.clamp(nu / eind, 0, 1));
        g.fill(x0, y0 + laneH + 13, x0 + laneW, y0 + laneH + 15, 0x60FFFFFF);
        g.fill(x0, y0 + laneH + 13, x0 + voortgang, y0 + laneH + 15, 0xFFFF7EB8);
        if (nu < liedje.ms(kaart.get(0).beat()) - 600) {
            g.centeredText(font, Component.translatable("gui.guhs.guhwaiispellen.hula.klaar_" + (liedje.niveau == Niveau.MAKKELIJK ? "makkelijk"
                    : liedje.niveau == Niveau.MEDIUM ? "medium" : "lastig")), w / 2, y0 - 40, 0xFFFFFFFF);
        }
    }

    private static String toets(HulaKaart.Pas pas) {
        var o = Minecraft.getInstance().options;
        KeyMapping m = switch (pas) {
            case LINKS -> o.keyLeft;
            case RECHTS -> o.keyRight;
            case OMHOOG -> o.keyUp;
            case OMLAAG -> o.keyDown;
            case VAHOEG -> o.keyJump;
        };
        String s = m.getTranslatedKeyMessage().getString();
        return s.length() > 5 ? s.substring(0, 5) : s;
    }

    /** A little pink hibiscus of squares (five petals round a yellow heart). */
    private static void bloem(GuiGraphicsExtractor g, int x, int y, int r) {
        int p = Math.max(3, r / 2);
        for (int k = 0; k < 5; k++) {
            double a = -Math.PI / 2 + k * Math.PI * 2 / 5;
            int px = x + (int) Math.round(Math.cos(a) * r * 0.55), py = y + (int) Math.round(Math.sin(a) * r * 0.55);
            g.fill(px - p, py - p, px + p, py + p, 0xFFFF6FA8);
        }
        g.fill(x - 3, y - 3, x + 3, y + 3, 0xFFFFE14D);
    }

    private HulaClient() {
    }
}
