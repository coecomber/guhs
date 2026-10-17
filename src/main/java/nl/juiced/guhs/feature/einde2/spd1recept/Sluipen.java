package nl.juiced.guhs.feature.einde2.spd1recept;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.Karen;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Rustpunten;

/**
 * einde2 "Sponsguh", slice sp_d1_recept, step 8 (CONTRACT_E2_C 7.5): the sneak through Plankmika's lab in De Vetemmer, from
 * {@code sluip_start} along the three lanes of {@code sluip_route} to the little safe {@code receptkluis}. Everything is drawn
 * for the sneaking player alone ({@link Schijn#deeltjes}), and it is all visible, never a hidden trigger (playtests 1-3):
 * <ul>
 *   <li><b>Karen's cameras</b> ({@code camera_1..4}, on the ceiling) slowly swing to and fro ({@link #cameraYaw}); what a
 *       camera sees is drawn on the floor as a fan of dust: pale yellow as far as it sees you walking ({@value #ZICHT}
 *       blocks), orange as far as it sees you crouching ({@value #ZICHT_GEBUKT}). Right under a camera it sees nothing (the
 *       blind spot of {@value #DODE_HOEK} blocks). Standing in its sight fills a little meter ("Karen ziet je...!"): only a
 *       full meter ({@value #METER} ticks) counts as seen, so walking through the edge of a fan is forgiven;</li>
 *   <li><b>the lasers</b> ({@code laser_1..4}, a red beam across each lane) blink: {@value #LASER_AAN} ticks on, then
 *       {@value #LASER_UIT} off (a dotted, flickering beam warns {@value #WAARSCHUW} ticks before it comes back). Touching a
 *       beam while it is on counts as seen;</li>
 *   <li><b>seen</b>: Karen beeps, her screen looks cross for a moment, one friendly line, and you are back on the last rest
 *       point you reached ({@code sluip_start}, {@code d1r_rust_2}, {@code d1r_rust_3}), FACING down the corridor (playtest 2
 *       item 5), with {@value #RUST} ticks to breathe. Nothing is lost; there is no clock. The action bar always shows the rest
 *       point you are at and how often Karen saw you.</li>
 * </ul>
 * The pure parts ({@link #laserAan}, {@link #cameraYaw}, {@link #cameraZiet}, {@link #Voortgang}) are what the game test checks.
 */
final class Sluipen {
    /** The rest point kind (main's {@link Rustpunten}). */
    static final String SOORT = "einde2_sp_d1_recept_sluip";
    /** The cameras: marker, the way it looks at rest (Minecraft yaw: 90 = west, -90 = east), its sweep period and phase (ticks). */
    record Cam(String marker, float basis, int periode, int fase) {
    }

    static final List<Cam> CAMS = List.of(new Cam("camera_1", 90f, 120, 0), new Cam("camera_4", 90f, 110, 40),
            new Cam("camera_2", -90f, 130, 20), new Cam("camera_3", 90f, 120, 60));
    /** The lasers: zone marker and phase (ticks). */
    record Laser(String zone, int fase) {
    }

    static final List<Laser> LASERS = List.of(new Laser("laser_1", 0), new Laser("laser_2", 30), new Laser("laser_3", 55), new Laser("laser_4", 15));
    /** The sweep of a camera (degrees each way), the half width of its sight, how far it sees (walking / crouching), its blind spot. */
    static final double ZWAAI = 85, HOEK = 14, ZICHT = 6.5, ZICHT_GEBUKT = 3.0, DODE_HOEK = 1.6;
    /** A laser's beat: on, off, and the warning before it comes back on (ticks). */
    static final int LASER_AAN = 50, LASER_UIT = 50, LASER_PERIODE = LASER_AAN + LASER_UIT, WAARSCHUW = 15;
    /** The meter: one tick in a camera's sight adds one, a tick out of it takes one off; full = seen. */
    static final int METER = 10;
    /** After being sent back nothing sees you for this many ticks. */
    static final int RUST = 60;
    /** The rest points in order (the first is the build's own) and the way you face on each: down the next corridor. */
    static final List<String> RUSTPUNTEN = List.of("sluip_start", "d1r_rust_2", "d1r_rust_3");
    static final float[] RUST_YAW = {-90f, 90f, -90f};
    /** How near (blocks) a rest point counts as reached, and the safe as reached. */
    static final double BIJ_RUST = 1.4, BIJ_KLUIS = 1.9;

    private static final DustParticleOptions FAN_VER = new DustParticleOptions(0xF2E07A, 0.8f);
    private static final DustParticleOptions FAN_DICHT = new DustParticleOptions(0xF08A30, 0.9f);
    private static final DustParticleOptions LASER = new DustParticleOptions(0xFF2A2A, 0.7f);
    private static final DustParticleOptions LASER_ZWAK = new DustParticleOptions(0x7A1A1A, 0.45f);
    private static final DustParticleOptions LENS = new DustParticleOptions(0xFF4040, 1.0f);

    /** The progress of one player's sneak (pure: no Minecraft state). */
    static final class Voortgang {
        int rust;                 // the last rest point reached (index in RUSTPUNTEN)
        int meter;                // how full Karen's meter is
        int gezien;               // how often Karen saw you this time
        long rustTot;             // nothing sees you before this tick

        /** A rest point reached: only forward. True = a new one. */
        boolean bereikt(int nr) {
            if (nr > rust) {
                rust = nr;
                return true;
            }
            return false;
        }

        /** One tick: in a camera's sight or not, touching a laser that is on or not, at this tick. True = seen (now sent back). */
        boolean tik(long tick, boolean inZicht, boolean laser) {
            if (tick < rustTot) {
                meter = 0;
                return false;
            }
            meter = inZicht ? meter + 1 : Math.max(0, meter - 1);
            if (laser || meter >= METER) {
                gezien++;
                meter = 0;
                rustTot = tick + RUST;
                return true;
            }
            return false;
        }
    }

    private static final Map<UUID, Voortgang> SPELERS = new ConcurrentHashMap<>();

    private Sluipen() {
    }

    // =====================================================================================================================
    // the pure rules
    // =====================================================================================================================

    /** Is laser nr (0..3) on at this tick? */
    static boolean laserAan(int nr, long tick) {
        return Math.floorMod(tick + LASERS.get(nr).fase(), LASER_PERIODE) < LASER_AAN;
    }

    /** Is laser nr off but about to come back on (the flicker)? */
    static boolean laserWaarschuwt(int nr, long tick) {
        return Math.floorMod(tick + LASERS.get(nr).fase(), LASER_PERIODE) >= LASER_PERIODE - WAARSCHUW;
    }

    /** Which way camera nr (0..3) looks at this tick. */
    static float cameraYaw(int nr, long tick) {
        Cam c = CAMS.get(nr);
        return c.basis() + (float) (ZWAAI * Math.sin(2 * Math.PI * Math.floorMod(tick + c.fase(), c.periode()) / c.periode()));
    }

    /** Does a camera hanging over {@code cam} (any height) that looks along yaw see a player standing at {@code voet}? */
    static boolean cameraZiet(Vec3 cam, float yaw, Vec3 voet, boolean gebukt) {
        double dx = voet.x - cam.x, dz = voet.z - cam.z;
        double ver = Math.sqrt(dx * dx + dz * dz);
        if (ver < DODE_HOEK || ver > (gebukt ? ZICHT_GEBUKT : ZICHT) || Math.abs(voet.y - cam.y) > 5) {
            return false;
        }
        return Math.abs(Mth.wrapDegrees(Cutscene.yawVan(new Vec3(dx, 0, dz)) - yaw)) <= HOEK;
    }

    // =====================================================================================================================
    // the game in the world
    // =====================================================================================================================

    static boolean bezig(ServerPlayer p) {
        return SPELERS.containsKey(p.getUUID());
    }

    @Nullable
    static Voortgang van(ServerPlayer p) {
        return SPELERS.get(p.getUUID());
    }

    /** Is the player in the sneak part of the lab (east of the wall with the lab room)? */
    static boolean opDeRoute(ServerPlayer p) {
        BlockPos o = E2SpD1Recept.oorsprong(p.level());
        Plekken.Plek lab = Plekken.plek(E2SpD1Recept.S, "lab"), start = Plekken.plek(E2SpD1Recept.S, "sluip_start");
        if (o == null || lab == null || lab.tot() == null || start == null) {
            return false;
        }
        double x = p.getX() - o.getX(), y = p.getY() - o.getY(), z = p.getZ() - o.getZ();
        return x >= start.pos().getX() - 1.0 && x <= lab.tot().getX() + 1 && z >= lab.pos().getZ() && z <= lab.tot().getZ() + 1
                && y >= lab.pos().getY() - 1 && y <= lab.tot().getY() + 1;
    }

    /** Starts the sneak for this player (they just stepped onto the route): the title, the rule, the first rest point. */
    static void start(ServerPlayer p) {
        if (bezig(p)) {
            return;
        }
        SPELERS.put(p.getUUID(), new Voortgang());
        zetRust(p, 0);
        E2SpD1Recept.titel(p, Component.translatable(E2SpD1Recept.P + "sluip.titel").withStyle(ChatFormatting.GREEN),
                Component.translatable(E2SpD1Recept.P + "sluip.onder").withStyle(ChatFormatting.YELLOW), 80);
        p.sendSystemMessage(Component.translatable(E2SpD1Recept.Q + "sluip.regel").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        SpAudio.stinger(p, "deuntje.sluipen");
    }

    /** Stops the sneak (left the lab, another step, the safe reached): the rest point is forgotten. */
    static void stop(ServerPlayer p) {
        if (SPELERS.remove(p.getUUID()) != null) {
            Rustpunten.wis(p, SOORT);
        }
    }

    static void vergeet(UUID u) {
        SPELERS.remove(u);
    }

    private static void zetRust(ServerPlayer p, int nr) {
        Vec3 v = E2SpD1Recept.voet(p.level(), RUSTPUNTEN.get(nr));
        if (v != null) {
            Rustpunten.zet(p, SOORT, GuheindeFeature.GUHEINDE, v, RUST_YAW[nr]);
        }
    }

    /**
     * Every tick while the player sneaks (E2SpD1Recept): the rest points, the safe, what the cameras and lasers do and draw.
     * True = the safe is reached (the caller plays the scene).
     */
    static boolean tik(ServerPlayer p) {
        Voortgang v = SPELERS.get(p.getUUID());
        ServerLevel level = p.level();
        if (v == null) {
            return false;
        }
        long tick = level.getServer().getTickCount();
        // the rest points and the safe
        for (int nr = 1; nr < RUSTPUNTEN.size(); nr++) {
            if (E2SpD1Recept.afstand(p, RUSTPUNTEN.get(nr)) <= BIJ_RUST && v.bereikt(nr)) {
                zetRust(p, nr);
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_PLING.value(), p.blockPosition(), 0.6f, 1.4f);
                p.sendOverlayMessage(Component.translatable(E2SpD1Recept.P + "sluip.rustpunt", nr + 1, RUSTPUNTEN.size()).withStyle(ChatFormatting.GREEN));
            }
        }
        Vec3 kluis = E2SpD1Recept.voet(level, "receptkluis");
        if (kluis != null && Math.hypot(p.getX() - kluis.x, p.getZ() - kluis.z) <= BIJ_KLUIS && Math.abs(p.getY() - (kluis.y - 1)) < 1.6) {
            stop(p);
            return true;
        }
        // the cameras and the lasers
        boolean gebukt = p.isShiftKeyDown();
        boolean inZicht = false, laser = false;
        Vec3 voet = p.position();
        for (int nr = 0; nr < CAMS.size(); nr++) {
            Vec3 cam = E2SpD1Recept.voet(level, CAMS.get(nr).marker());
            if (cam == null) {
                continue;
            }
            float yaw = cameraYaw(nr, tick);
            if (cameraZiet(cam, yaw, voet, gebukt)) {
                inZicht = true;
            }
            if ((tick + nr) % 3 == 0 && cam.distanceToSqr(voet) < 20 * 20) {
                waaier(p, cam, yaw);
            }
        }
        BlockPos o = E2SpD1Recept.oorsprong(level);
        for (int nr = 0; nr < LASERS.size() && o != null; nr++) {
            Plekken.Plek z = Plekken.plek(E2SpD1Recept.S, LASERS.get(nr).zone());
            if (z == null || z.tot() == null) {
                continue;
            }
            AABB doos = new AABB(o.getX() + z.pos().getX(), o.getY() + z.pos().getY(), o.getZ() + z.pos().getZ(),
                    o.getX() + z.tot().getX() + 1, o.getY() + z.tot().getY() + 1, o.getZ() + z.tot().getZ() + 1);
            boolean aan = laserAan(nr, tick);
            if (aan && doos.intersects(p.getBoundingBox().deflate(0.05))) {
                laser = true;
            }
            if (tick % 2 == 0 && doos.getCenter().distanceToSqr(voet) < 22 * 22) {
                straal(p, doos, aan, laserWaarschuwt(nr, tick), tick);
            }
        }
        if (!Duwtje.mag(p)) {
            return false;
        }
        if (v.tik(tick, inZicht, laser)) {
            betrapt(p, v, laser);
        } else if (v.meter > 0) {
            p.sendOverlayMessage(Component.translatable(E2SpD1Recept.P + "sluip.bijna", "!".repeat(1 + v.meter * 4 / METER)).withStyle(ChatFormatting.GOLD));
            if (v.meter == 1) {
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_BIT.value(), p.blockPosition(), 0.5f, 1.8f);
            }
        } else if (tick % 20 == 0) {
            p.sendOverlayMessage(Component.translatable(E2SpD1Recept.P + "sluip.stand", v.rust + 1, RUSTPUNTEN.size(), v.gezien).withStyle(ChatFormatting.YELLOW));
        }
        if (kluis != null && tick % 10 == 0) {                       // (the goal: a soft column of light on the safe)
            Schijn.deeltjes(p, ParticleTypes.END_ROD, kluis.add(0, 1.2, 0), 3, 0.08, 0.01);
        }
        return false;
    }

    /** Seen: Karen beeps, a line, back to the last rest point facing the corridor. */
    private static void betrapt(ServerPlayer p, Voortgang v, boolean laser) {
        SpAudio.stinger(p, "stinger.karen");
        Schijn.geluid(p, SoundEvents.NOTE_BLOCK_BELL.value(), p.blockPosition(), 0.8f, 0.5f);
        Schijn.geluid(p, SoundEvents.NOTE_BLOCK_BELL.value(), p.blockPosition(), 0.8f, 0.6f);
        E2SpD1Recept.karenGezicht(p, Karen.BOOS, 50);
        E2SpD1Recept.karenZegt(p, (laser ? "sluip.laser." : "sluip.gezien.") + Math.floorMod(v.gezien - 1, 3));
        if (v.gezien == 1) {
            p.sendSystemMessage(Component.translatable(E2SpD1Recept.Q + "sluip.troost").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        Rustpunten.terug(p, SOORT);
    }

    /** What a camera sees, on the floor: the middle and both edges of its sight, orange as far as it sees you crouching. */
    private static void waaier(ServerPlayer p, Vec3 cam, float yaw) {
        Vec3 vloer = new Vec3(cam.x, Math.floor(p.getY()) + 0.15, cam.z);
        for (double hoek : new double[] {-HOEK, 0, HOEK}) {
            Vec3 r = Vec3.directionFromRotation(0, yaw + (float) hoek);
            for (double d = DODE_HOEK; d <= ZICHT; d += hoek == 0 ? 0.8 : 1.1) {
                Vec3 punt = vloer.add(r.scale(d));
                BlockPos b = BlockPos.containing(punt.x, punt.y + 0.5, punt.z);
                if (!p.level().getBlockState(b).getCollisionShape(p.level(), b).isEmpty()) {
                    break;
                }
                Schijn.deeltjes(p, d <= ZICHT_GEBUKT ? FAN_DICHT : FAN_VER, punt, 1, 0.02, 0);
            }
        }
        Schijn.deeltjes(p, LENS, cam.add(0, 0.4, 0).add(Vec3.directionFromRotation(20, yaw).scale(0.45)), 1, 0, 0);
    }

    /** A laser beam: a bright red curtain while it is on, a faint flickering one just before it comes back, a few dots when off. */
    private static void straal(ServerPlayer p, AABB doos, boolean aan, boolean waarschuw, long tick) {
        boolean langsX = doos.getXsize() > doos.getZsize();
        double lengte = langsX ? doos.getXsize() : doos.getZsize();
        double stap = aan ? 0.3 : waarschuw ? 0.6 : 1.2;
        if (!aan && !waarschuw && tick % 8 != 0) {
            return;
        }
        if (waarschuw && tick % 4 >= 2) {
            return;                                                   // (the flicker)
        }
        for (double h : aan ? new double[] {0.35, 0.9, 1.45, 2.0, 2.55} : new double[] {0.9, 1.9}) {
            for (double s = 0.15; s <= lengte - 0.15; s += stap) {
                double x = langsX ? doos.minX + s : doos.getCenter().x;
                double z = langsX ? doos.getCenter().z : doos.minZ + s;
                Schijn.deeltjes(p, aan ? LASER : LASER_ZWAK, new Vec3(x, doos.minY + h, z), 1, 0, 0);
            }
        }
    }
}
