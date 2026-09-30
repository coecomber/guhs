package nl.juiced.guhs.feature.guhwaiispellen.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.guhwaiispellen.GuhwaiiSpellenPayloads;
import nl.juiced.guhs.feature.guhwaiispellen.SurfGolven;
import nl.juiced.guhs.feature.guhwaiispellen.SurfPlankEntity;
import nl.juiced.guhs.feature.guhwaiispellen.SurfSim;
import nl.juiced.guhs.feature.guhwaiispellen.Surfplek;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * Your own surf ride (3.0): your game runs the same {@link SurfSim} as the server, with your keys, the moment you press
 * them (so the board follows you at once), puts your board there and sends the keys of every step to the server (which
 * counts). Also the panel (score, the multiplier, the wave, what to do now, the tricks you land) and the camera (behind
 * you, over the waves).
 */
public final class SurfClient {
    @Nullable
    private static SurfSim sim;
    @Nullable
    private static Surfplek.Spot spot;
    private static int bordId = -1, liloId = -1, record;
    private static Niveau niveau = Niveau.MEDIUM;
    @Nullable
    private static CameraType camera;
    /** Tricks and happenings floating over the panel: text, colour, ticks left. */
    private record Melding(Component tekst, int kleur, int tot) {
    }

    private static final List<Melding> MELDINGEN = new ArrayList<>();
    private static int ticks;
    @Nullable
    private static CompoundTag uitslag;
    private static int uitslagTot;

    public static void start(CompoundTag data) {
        spot = new Surfplek.Spot(BlockPos.of(data.getLong("Origin")), data.getDouble("Hoek"));
        niveau = Niveau.of(data.getInt("Niveau"));
        sim = new SurfSim(niveau, data.getInt("Seed"));
        bordId = data.getInt("Bord");
        liloId = data.getInt("Lilo");
        record = data.getInt("Record");
        MELDINGEN.clear();
        uitslag = null;
        Minecraft mc = Minecraft.getInstance();
        if (camera == null) {
            camera = mc.options.getCameraType();
        }
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    public static void einde(CompoundTag data) {
        SurfPlankEntity bord = bord();
        if (bord != null) {
            bord.eigen = false;
        }
        sim = null;
        bordId = -1;
        uitslag = data;
        uitslagTot = ticks + 140;
        herstelCamera();
    }

    public static void uit() {
        sim = null;
        bordId = -1;
        uitslag = null;
        MELDINGEN.clear();
        herstelCamera();
    }

    private static void herstelCamera() {
        if (camera != null) {
            Minecraft.getInstance().options.setCameraType(camera);
            camera = null;
        }
    }

    public static boolean surft() {
        return sim != null;
    }

    /** The ride running in this game (for the wave renderer), or null. */
    @Nullable
    public static SurfSim sim() {
        return sim;
    }

    public static int bordId() {
        return bordId;
    }

    @Nullable
    private static SurfPlankEntity bord() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && bordId >= 0 && mc.level.getEntity(bordId) instanceof SurfPlankEntity b ? b : null;
    }

    // --- every client tick -----------------------------------------------------------------------------------------------

    public static void tick() {
        ticks++;
        MELDINGEN.removeIf(m -> m.tot < ticks);
        if (sim == null || spot == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        SurfPlankEntity bord = bord();
        if (p == null || bord == null || p.getVehicle() != bord || mc.isPaused()) {
            return;                                          // (the board isn't here yet: wait for it; or the game is paused)
        }
        bord.eigen = true;
        int bits = mc.screen != null ? 0 : invoer(p);
        List<SurfSim.Gebeurtenis> events = sim.stap(bits);
        PacketDistributor.sendToServer(new GuhwaiiSpellenPayloads.SurfStuur(bordId, sim.step(), bits));
        // the board goes where the ride is (and you with it, this very tick)
        Vec3 w = spot.wereld(sim.u(), sim.v(), sim.hoogte());
        bord.setPos(w.x, w.y, w.z);
        bord.eigenYawO = bord.eigenYaw;
        bord.eigenHellingO = bord.eigenHelling;
        float yaw = spot.yaw(sim);
        bord.eigenYaw = bord.eigenYawO + Mth.wrapDegrees(yaw - bord.eigenYawO);
        bord.eigenHelling = (float) Math.toDegrees(sim.helling());
        bord.positionRider(p);
        for (SurfSim.Gebeurtenis e : events) {
            melding(e);
        }
        spetters(mc, bord);
    }

    /** Your keys in the beach's frame: A/D along the beach, W down the face, S up to the lip, space: jump. */
    private static int invoer(LocalPlayer p) {
        int bits = 0;
        if (p.input.leftImpulse > 0) {
            bits |= SurfSim.LINKS;
        } else if (p.input.leftImpulse < 0) {
            bits |= SurfSim.RECHTS;
        }
        if (p.input.forwardImpulse > 0) {
            bits |= SurfSim.VOORUIT;
        } else if (p.input.forwardImpulse < 0) {
            bits |= SurfSim.ACHTERUIT;
        }
        if (p.input.jumping) {
            bits |= SurfSim.SPRING;
        }
        return bits;
    }

    private static void melding(SurfSim.Gebeurtenis e) {
        Component t = switch (e.soort()) {
            case VANG -> Component.translatable("gui.guhs.guhwaiispellen.surf.vang");
            case MIS -> Component.translatable("gui.guhs.guhwaiispellen.surf.mis");
            case SCHUIM -> Component.translatable("gui.guhs.guhwaiispellen.surf.schuim");
            case TRUC, CUTBACK, TUBE -> Component.translatable("gui.guhs.guhwaiispellen.truc." + e.naam()).append(" +" + e.punten());
            case TUBE_IN -> Component.translatable("gui.guhs.guhwaiispellen.surf.tube_in");
            case PLONS -> Component.translatable("gui.guhs.guhwaiispellen.surf.plons." + e.naam());
            case GOLF_KLAAR -> Component.translatable("gui.guhs.guhwaiispellen.surf.golf_" + e.naam()).append(" +" + e.punten());
            case KLAAR -> Component.translatable("gui.guhs.guhwaiispellen.surf.klaar");
        };
        int kleur = switch (e.soort()) {
            case TRUC, CUTBACK, TUBE -> 0xFFFFD84D;
            case PLONS, MIS, SCHUIM -> 0xFF9FD8FF;
            case TUBE_IN -> 0xFF5CF0E0;
            default -> 0xFFFFFFFF;
        };
        MELDINGEN.add(new Melding(t, kleur, ticks + 50));
        if (MELDINGEN.size() > 4) {
            MELDINGEN.remove(0);
        }
    }

    /** Spray behind the board when you carve fast, a splash when you land or fall. */
    private static void spetters(Minecraft mc, SurfPlankEntity bord) {
        if (mc.level == null || sim == null) {
            return;
        }
        var r = mc.level.getRandom();
        if (sim.fase() == SurfSim.Fase.RIJDEN && sim.vaart() > 0.35 && r.nextFloat() < sim.vaart()) {
            mc.level.addParticle(ParticleTypes.SPLASH, bord.getX() + (r.nextDouble() - 0.5) * 0.6, bord.getY() + 0.15,
                    bord.getZ() + (r.nextDouble() - 0.5) * 0.6, 0, 0.1, 0);
        }
        if (sim.inTube() && r.nextInt(2) == 0) {
            mc.level.addParticle(ParticleTypes.BUBBLE_POP, bord.getX() + (r.nextDouble() - 0.5) * 2, bord.getY() + 1.2 + r.nextDouble(),
                    bord.getZ() + (r.nextDouble() - 0.5) * 2, 0, 0, 0);
        }
    }

    // --- the panel -------------------------------------------------------------------------------------------------------

    public static void hud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        int w = g.guiWidth();
        if (sim == null) {
            if (uitslag != null && ticks < uitslagTot) {
                uitslagPaneel(g, font, w);
            }
            return;
        }
        int x0 = w / 2 - 110, y0 = 6;
        g.fill(x0, y0, x0 + 220, y0 + 44, 0xB0103A4A);
        g.fill(x0, y0 + 44, x0 + 220, y0 + 46, 0xFF3CE0C8);
        SurfGolven golven = sim.golven();
        int golf = Math.min(golven.golven().size(), Math.max(1, sim.volgende() + 1));
        g.drawCenteredString(font, Component.translatable("gui.guhs.guhwaiispellen.surf.paneel", niveau.naam(), golf, golven.golven().size())
                .withStyle(ChatFormatting.AQUA), w / 2, y0 + 4, 0xFFFFFFFF);
        String score = String.valueOf(sim.score());
        g.pose().pushPose();
        g.pose().translate(w / 2f - font.width(score), y0 + 15, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(font, score, 0, 0, 0xFFFFE6A0, true);
        g.pose().popPose();
        if (sim.mult() > 1) {
            g.drawString(font, "x" + sim.mult(), w / 2 + font.width(score) + 6, y0 + 20, 0xFFFF7EB8, true);
        }
        g.drawString(font, Component.translatable("gui.guhs.guhwaiispellen.surf.record", record), x0 + 4, y0 + 34, 0xFFB8E8F0, false);
        // speed
        if (sim.fase() == SurfSim.Fase.RIJDEN || sim.fase() == SurfSim.Fase.LUCHT) {
            int len = (int) (80 * (sim.vaart() - SurfSim.MIN_VAART) / (SurfSim.MAX_VAART - SurfSim.MIN_VAART));
            g.fill(x0 + 134, y0 + 36, x0 + 216, y0 + 41, 0x80000000);
            g.fill(x0 + 135, y0 + 37, x0 + 135 + Math.max(1, len), y0 + 40, sim.inTube() ? 0xFF5CF0E0 : 0xFFFF9EC8);
        }
        // what to do now
        Component hint = switch (sim.fase()) {
            case PEDDELEN -> sim.kanVangen() ? Component.translatable(niveau == Niveau.MAKKELIJK ? "gui.guhs.guhwaiispellen.surf.hint.komt"
                    : "gui.guhs.guhwaiispellen.surf.hint.nu") : Component.translatable("gui.guhs.guhwaiispellen.surf.hint.wacht");
            case RIJDEN -> Component.translatable(sim.inTube() ? "gui.guhs.guhwaiispellen.surf.hint.tube" : "gui.guhs.guhwaiispellen.surf.hint.rijden");
            case LUCHT -> Component.translatable("gui.guhs.guhwaiispellen.surf.hint.lucht", Math.round(Math.abs(sim.spin())));
            case PLONS -> Component.translatable("gui.guhs.guhwaiispellen.surf.hint.plons");
            case TERUG -> Component.translatable("gui.guhs.guhwaiispellen.surf.hint.terug");
            case KLAAR -> Component.empty();
        };
        boolean knipper = sim.fase() == SurfSim.Fase.PEDDELEN && sim.kanVangen() && (ticks / 4) % 2 == 0;
        int hy = g.guiHeight() - 72;
        g.drawCenteredString(font, hint, w / 2, hy, knipper ? 0xFFFFE14D : 0xFFE8FFFF);
        g.drawCenteredString(font, Component.translatable("gui.guhs.guhwaiispellen.surf.hint.stoppen").withStyle(ChatFormatting.GRAY), w / 2, hy + 11, 0xFFAAAAAA);
        int y = y0 + 54;
        for (Melding m : MELDINGEN) {
            int left = m.tot - ticks;
            int a = Mth.clamp(left * 12, 30, 255);
            g.drawCenteredString(font, m.tekst, w / 2, y, (a << 24) | (m.kleur & 0xFFFFFF));
            y += 11;
        }
    }

    private static void uitslagPaneel(GuiGraphics g, Font font, int w) {
        int x0 = w / 2 - 100, y0 = 6;
        g.fill(x0, y0, x0 + 200, y0 + 34, 0xB0103A4A);
        g.drawCenteredString(font, Component.translatable("gui.guhs.guhwaiispellen.surf.einde", uitslag.getInt("Score"),
                Niveau.of(uitslag.getInt("Niveau")).naam()), w / 2, y0 + 5, 0xFFFFE6A0);
        Component tweede = uitslag.getBoolean("Record") ? Component.translatable("gui.guhs.guhwaiispellen.nieuw_record")
                : Component.translatable("gui.guhs.guhwaiispellen.munten", uitslag.getInt("Munten"));
        g.drawCenteredString(font, tweede, w / 2, y0 + 19, 0xFFFF9EC8);
    }

    /** (for the renderer: the Lilo-guh board of your own game) */
    public static int liloId() {
        return liloId;
    }

    private SurfClient() {
    }
}
