package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 phase B, slice h1_lucht, steps 4 and 5 (DESIGN 5 h1: Guhatso's training): the luchtscooter-baan and the
 * zweefringen. fix round 1 (playtest 1: "where is the start", "I just walked", "nothing happens at the finish", "no
 * explanation of the rings, too hard"):
 * <ul>
 *   <li><b>The track.</b> The start ({@code scooter_start}, the orange-yellow arch in the south-west corner) shines with a
 *       column of light and wind while you are at step 4. Step on the black-and-white line under the arch: you are put on a
 *       luchtscooter (a ball of air you stand on, {@link LuchtbalEntity#scooter}), a big 3-2-1 counts down on the screen, and
 *       off you go. The scooter goes where you look; W is faster, S brakes, space hops. Every gate (the points of
 *       {@code scooter_baan}, the next one shines) gives a burst of speed. Blown off the track: the wind puts you back on
 *       it, the clock runs on. The finish shows a big title with your time, then the wind carries you to the balcony of the
 *       rings. Too slow ({@value #BAAN_SECONDEN} s): back to the start, try again. Stepping off (sneak) stops the run.</li>
 *   <li><b>The rings.</b> The start is {@code zweef_start}, the south balcony at the top of the great tower on the roof of
 *       the hall. You get there with the wind: right after the track, or from the column of wind in the middle of the
 *       square in front of the hall ({@code zweef_landing}). Jump off and you glide: flat and slow, you go where you look,
 *       looking down is faster. Six rings ({@code zweefring_01..06}) in a short arc to the west; the next one is lit, the
 *       wind helps you towards it, and every ring gives a little lift and a little speed. One may be missed. Miss more, or
 *       land too early: a moment later you stand on the balcony again.</li>
 * </ul>
 * Never a fall hurts in the temple ({@link Tempel#opVal}).
 */
public final class Training {
    public static final int BAAN_SECONDEN = 60;
    /** How many rings may be missed. */
    public static final int RINGEN_MAG_MISSEN = 1;
    /** The countdown on the scooter, in ticks. */
    static final int AFTELLEN = 60;
    /** The glide: speed and sink (blocks per tick) looking straight ahead; the lift (blocks per tick, ticks) of a ring. */
    static final double ZWEEF_SNEL = 0.5, ZWEEF_ZAK = 0.045, RING_LIFT = 0.13;
    static final int RING_LIFT_TICKS = 4;

    private static final class Baan {
        LuchtbalEntity scooter;
        int aftellen = AFTELLEN, poort = 1, vanBaan;
        long tot;
        boolean bezig;
    }

    private static final class Zweef {
        boolean vliegt;
        int volgende, door, gemist, ticks, lift, boost;
        long opBalkon = -1000;
        Vec3 vorige;
    }

    private static final Map<UUID, Baan> BANEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Zweef> ZWEVEN = new ConcurrentHashMap<>();
    /** Players the wind carries to the balcony at this game time (after the track, after a failed glide). */
    private static final Map<UUID, Long> NAAR_BALKON = new ConcurrentHashMap<>();
    /** Players the wind sets down on the square in front of the hall at this game time (after the last ring). */
    private static final Map<UUID, Long> NAAR_PLEIN = new ConcurrentHashMap<>();
    /** Players rising in the column of wind (ticks left). */
    private static final Map<UUID, Integer> OPWAAI = new ConcurrentHashMap<>();
    private static final DustParticleOptions RING_LICHT = new DustParticleOptions(0x66F0FF, 2.2f);
    private static final DustParticleOptions RING_LATER = new DustParticleOptions(0xFFE07A, 1.2f);

    private Training() {
    }

    private static net.minecraft.network.chat.MutableComponent tekst(String key, Object... args) {
        return Component.translatable(E2H1Lucht.P + key, args);
    }

    /** Every tick, for a player in the Guheinde at a step of the chapter. */
    static void tick(ServerPlayer p) {
        int stap = E2H1Lucht.LIJN.stap(p);
        if (stap == E2H1Lucht.SCOOTER) {
            baan(p);
        } else if (BANEN.containsKey(p.getUUID())) {
            stopBaan(p);
        }
        Long wanneer = NAAR_BALKON.get(p.getUUID());
        if (wanneer != null && p.level().getGameTime() >= wanneer && !Cutscenes.bezig(p)) {
            NAAR_BALKON.remove(p.getUUID());
            if (E2H1Lucht.LIJN.stap(p) == E2H1Lucht.ZWEVEN) {
                naarBalkon(p);
            }
        }
        Long neer = NAAR_PLEIN.get(p.getUUID());
        if (neer != null && (p.onGround() || p.level().getGameTime() >= neer)) {
            NAAR_PLEIN.remove(p.getUUID());
            if (p.onGround()) {
                p.removeEffect(MobEffects.SLOW_FALLING);
            } else if (!Cutscenes.bezig(p)) {
                p.removeEffect(MobEffects.SLOW_FALLING);
                Schijn.deeltjes(p, ParticleTypes.CLOUD, p.position().add(0, 1, 0), 20, 0.5, 0.08);
                if (E2H1Lucht.zetNaar(p, "zweef_landing", 0f)) {
                    Schijn.deeltjes(p, ParticleTypes.CLOUD, p.position().add(0, 0.5, 0), 20, 0.6, 0.05);
                    p.sendOverlayMessage(tekst("zweef.neergezet").withStyle(ChatFormatting.AQUA));
                }
            }
        }
        if (stap == E2H1Lucht.ZWEVEN || zweeft(p)) {
            zweef(p);
        }
    }

    // =====================================================================================================================
    // the luchtscooter-baan
    // =====================================================================================================================
    private static void baan(ServerPlayer p) {
        ServerLevel g = p.level();
        List<Vec3> pad = E2H1Lucht.pad(g, "scooter_baan");
        Vec3 start = E2H1Lucht.midden(g, "scooter_start"), finish = E2H1Lucht.midden(g, "scooter_finish");
        if (pad.size() < 2 || start == null || finish == null || !E2H1Lucht.inTempel(p) || Cutscenes.bezig(p)) {
            return;
        }
        Baan b = BANEN.get(p.getUUID());
        long nu = g.getGameTime();
        if (b == null) {
            // the start shines: a column of light and a swirl of wind on the line under the arch
            if (nu % 5 == 0) {
                baken(p, start, nu);
            }
            if (horizontaal(p.position(), start) < 12 && nu % 60 == 0) {
                p.sendOverlayMessage(tekst("scooter.stap_op").withStyle(ChatFormatting.GOLD));
            }
            if (horizontaal(p.position(), start) < 2.6 && Math.abs(p.getY() - start.y) < 2 && !p.isPassenger()) {
                if (E2H1Lucht.LIJN.eenmalig(p, "knipoog_sjokkel")) {
                    BlockPos anker = knipoogAnker(g);
                    if (anker != null && Cutscenes.speel(p, H1Scenes.KNIPOOG_SJOKKEL, anker, Rotation.NONE, Training::opScooter)) {
                        return;
                    }
                }
                opScooter(p);
            }
            return;
        }
        LuchtbalEntity sc = b.scooter;
        if (sc == null || sc.isRemoved() || p.getVehicle() != sc) {
            stopBaan(p);
            p.sendSystemMessage(tekst("scooter.afgestapt").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (!b.bezig) {
            // the countdown: big on the screen; the scooter holds still
            if (b.aftellen % 20 == 0 && b.aftellen > 0) {
                E2H1Lucht.titel(p, Component.literal(String.valueOf(b.aftellen / 20)).withStyle(ChatFormatting.YELLOW),
                        tekst("scooter.uitleg").withStyle(ChatFormatting.AQUA), 18);
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), p.blockPosition(), 1.0f, 1.0f);
            }
            if (--b.aftellen <= 0) {
                b.bezig = true;
                b.tot = nu + BAAN_SECONDEN * 20L;
                sc.boost(20);
                E2H1Lucht.titel(p, tekst("scooter.zoef").withStyle(ChatFormatting.AQUA), tekst("scooter.uitleg").withStyle(ChatFormatting.AQUA), 20);
                Schijn.geluid(p, SoundEvents.FIREWORK_ROCKET_LAUNCH, p.blockPosition(), 1.0f, 1.2f);
                nl.juiced.guhs.feature.einde2.audio.E2Audio.stinger(p, "deuntje.luchtscooter");   // (integratie_b: audio's tune of the track)
            }
            return;
        }
        // on the way: the gates in order (each one a burst of speed), the next one shines, the clock
        if (b.poort < pad.size()) {
            Vec3 poort = pad.get(b.poort);
            if (horizontaal(p.position(), poort) < 5.5 && Math.abs(p.getY() - poort.y) < 4) {
                b.poort++;
                sc.boost(18);
                Schijn.geluid(p, SoundEvents.EXPERIENCE_ORB_PICKUP, p.blockPosition(), 0.7f, 0.9f + b.poort * 0.02f);
                Schijn.deeltjes(p, ParticleTypes.FIREWORK, p.position().add(0, 1.0, 0), 6, 0.4, 0.05);
            }
            if (b.poort < pad.size() && nu % 3 == 0) {
                Vec3 volgende = pad.get(b.poort);
                E2H1Lucht.ver(p, ParticleTypes.END_ROD, volgende.add(0, 1.0 + (nu / 3 % 4) * 0.6, 0), 2, 0.25, 0.0);
            }
        }
        long over = Math.max(0, (b.tot - nu) / 20);
        if (nu % 10 == 0) {
            p.sendOverlayMessage(tekst("scooter.stand", Math.min(b.poort, pad.size()), pad.size(), over).withStyle(ChatFormatting.YELLOW));
        }
        if (b.poort >= pad.size() || (b.poort >= pad.size() - 1 && horizontaal(p.position(), finish) < 5)) {
            finish(p, b, nu);
            return;
        }
        if (nu > b.tot) {
            mis(p, "scooter.te_laat");
            return;
        }
        // off the track (over the kerb, onto the square): the wind puts you back on it, the clock runs on
        if (afstandTotPad(sc.position(), pad) > 7 || sc.getY() < start.y - 5) {
            if (++b.vanBaan > 15) {
                b.vanBaan = 0;
                Vec3 terug = pad.get(Math.max(0, b.poort - 1));
                Vec3 naar = pad.get(Math.min(pad.size() - 1, b.poort));
                float yaw = (float) Math.toDegrees(Math.atan2(-(naar.x - terug.x), naar.z - terug.z));
                sc.snapTo(terug.x, terug.y + 0.05, terug.z, yaw, 0f);
                sc.setDeltaMovement(Vec3.ZERO);
                Schijn.deeltjes(p, ParticleTypes.CLOUD, terug.add(0, 0.8, 0), 14, 0.5, 0.05);
                p.sendOverlayMessage(tekst("scooter.terug_op_baan").withStyle(ChatFormatting.AQUA));
            }
        } else {
            b.vanBaan = 0;
        }
    }

    /** The column of light over the start line, and a swirl of wind on it. */
    private static void baken(ServerPlayer p, Vec3 start, long nu) {
        for (int i = 0; i < 6; i++) {
            E2H1Lucht.ver(p, ParticleTypes.END_ROD, start.add(0, 0.6 + i * 1.4 + (nu / 5 % 3) * 0.45, 0), 1, 0.15, 0.0);
        }
        for (int i = 0; i < 8; i++) {
            double h = nu * 0.25 + i * Math.PI / 4;
            E2H1Lucht.ver(p, ParticleTypes.CLOUD, start.add(Math.cos(h) * 1.6, 0.3, Math.sin(h) * 1.6), 1, 0.02, 0.0);
        }
    }

    /** Stepped on the start line (after wink 1 the first time): on the scooter, the countdown begins. */
    static void opScooter(ServerPlayer p) {
        ServerLevel g = E2H1Lucht.guheinde(p.level().getServer());
        if (g == null || p.level() != g || E2H1Lucht.LIJN.stap(p) != E2H1Lucht.SCOOTER || BANEN.containsKey(p.getUUID())) {
            return;
        }
        List<Vec3> pad = E2H1Lucht.pad(g, "scooter_baan");
        Vec3 start = E2H1Lucht.midden(g, "scooter_start");
        LuchtbalEntity sc = E2H1LuchtFeature.LUCHTBAL.get().create(g, EntitySpawnReason.TRIGGERED);
        if (pad.size() < 2 || start == null || sc == null) {
            return;
        }
        Vec3 naar = pad.get(1);
        float yaw = (float) Math.toDegrees(Math.atan2(-(naar.x - start.x), naar.z - start.z));
        sc.scooter(AFTELLEN);
        sc.snapTo(start.x, start.y + 0.05, start.z, yaw, 0f);
        Zicht.alleenVoor(sc, p.getUUID());
        Tijdelijk.merk(sc);
        g.addFreshEntity(sc);
        p.teleportTo(g, start.x, start.y + 0.8, start.z, Set.of(), yaw, 10f, true);
        if (!p.startRiding(sc, true, true)) {
            Tijdelijk.weg(sc);
            return;
        }
        Baan b = new Baan();
        b.scooter = sc;
        BANEN.put(p.getUUID(), b);
        Schijn.deeltjes(p, ParticleTypes.CLOUD, start.add(0, 0.5, 0), 24, 0.6, 0.08);
        Schijn.geluid(p, SoundEvents.ENDER_DRAGON_FLAP, p.blockPosition(), 0.6f, 1.8f);
        p.sendSystemMessage(tekst("scooter.klaar").withStyle(ChatFormatting.GOLD));
    }

    private static void finish(ServerPlayer p, Baan b, long nu) {
        long sec = (BAAN_SECONDEN * 20L - (b.tot - nu)) / 20;
        stopBaan(p);
        Schijn.deeltjes(p, ParticleTypes.FIREWORK, p.position().add(0, 1.5, 0), 40, 0.6, 0.12);
        Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, p.blockPosition(), 1.0f, 1.2f);
        Schijn.geluid(p, SoundEvents.FIREWORK_ROCKET_TWINKLE, p.blockPosition(), 1.0f, 1.0f);
        E2H1Lucht.titel(p, tekst("scooter.finish").withStyle(ChatFormatting.GOLD), tekst("scooter.tijd", sec).withStyle(ChatFormatting.YELLOW), 60);
        p.sendSystemMessage(tekst("scooter.gehaald", sec).withStyle(ChatFormatting.GREEN));
        if (E2H1Lucht.LIJN.verder(p, E2H1Lucht.SCOOTER)) {
            p.sendSystemMessage(tekst("zweef.wind_komt").withStyle(ChatFormatting.AQUA));
            NAAR_BALKON.put(p.getUUID(), nu + 80);
        }
    }

    private static void mis(ServerPlayer p, String key) {
        stopBaan(p);
        p.sendSystemMessage(tekst(key).withStyle(ChatFormatting.YELLOW));
        E2H1Lucht.zetNaar(p, "scooter_start", 6, 0, 0, 90f);
    }

    /** The run ends: off the scooter, the scooter gone. */
    private static void stopBaan(ServerPlayer p) {
        Baan b = BANEN.remove(p.getUUID());
        if (b != null && b.scooter != null) {
            if (p.getVehicle() == b.scooter) {
                p.stopRiding();
            }
            Tijdelijk.weg(b.scooter);
        }
    }

    /** The anchor of wink 1: the look-at point of knipoog_sjokkel_camera. */
    static BlockPos knipoogAnker(ServerLevel g) {
        List<Vec3> cam = E2H1Lucht.pad(g, "knipoog_sjokkel_camera");
        return cam.isEmpty() ? null : BlockPos.containing(cam.get(cam.size() - 1));
    }

    private static double afstandTotPad(Vec3 p, List<Vec3> pad) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i + 1 < pad.size(); i++) {
            Vec3 a = pad.get(i), b = pad.get(i + 1);
            Vec3 ab = b.subtract(a);
            double t = ab.lengthSqr() < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / ab.lengthSqr()));
            best = Math.min(best, a.add(ab.scale(t)).distanceTo(p));
        }
        return best;
    }

    private static double horizontaal(Vec3 a, Vec3 b) {
        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    /** (test) is this player on a running scooter? */
    static boolean opBaan(ServerPlayer p) {
        return BANEN.containsKey(p.getUUID());
    }

    // =====================================================================================================================
    // the zweefringen
    // =====================================================================================================================

    /** Is this player gliding right now? */
    public static boolean zweeft(ServerPlayer p) {
        Zweef z = ZWEVEN.get(p.getUUID());
        return z != null && z.vliegt;
    }

    /** The wind puts the player on the balcony of the rings, looking south over the first ring. */
    static void naarBalkon(ServerPlayer p) {
        OPWAAI.remove(p.getUUID());
        Zweef z = ZWEVEN.get(p.getUUID());
        if (z != null) {
            z.vliegt = false;
        }
        p.removeEffect(MobEffects.SLOW_FALLING);
        Schijn.deeltjes(p, ParticleTypes.CLOUD, p.position().add(0, 1, 0), 20, 0.5, 0.08);
        if (E2H1Lucht.zetNaar(p, "zweef_start", 0, 0, -1, 0f)) {
            Schijn.deeltjes(p, ParticleTypes.CLOUD, p.position().add(0, 1, 0), 20, 0.5, 0.08);
            Schijn.geluid(p, SoundEvents.ENDER_DRAGON_FLAP, p.blockPosition(), 0.6f, 1.6f);
            p.sendSystemMessage(tekst("zweef.op_balkon").withStyle(ChatFormatting.AQUA));
            E2H1Lucht.titel(p, tekst("zweef.titel").withStyle(ChatFormatting.AQUA), tekst("zweef.titel_onder").withStyle(ChatFormatting.WHITE), 70);
        }
    }

    private static void zweef(ServerPlayer p) {
        ServerLevel g = p.level();
        Vec3 balkon = E2H1Lucht.midden(g, "zweef_start");
        if (balkon == null || Cutscenes.bezig(p)) {
            return;
        }
        Zweef z = ZWEVEN.computeIfAbsent(p.getUUID(), u -> new Zweef());
        long nu = g.getGameTime();
        List<Plekken.Plek> ringen = ringen();
        BlockPos oorsprong = Plekken.oorsprong(g, E2H1Lucht.STRUCTUUR);
        if (oorsprong == null || ringen.isEmpty()) {
            return;
        }
        if (!z.vliegt) {
            if (E2H1Lucht.LIJN.stap(p) != E2H1Lucht.ZWEVEN) {
                return;
            }
            windzuil(p, g, nu);
            boolean op = horizontaal(p.position(), balkon) < 3.2 && Math.abs(p.getY() - balkon.y) < 1.5;
            if (op) {
                if (nu - z.opBalkon > 80) {
                    p.sendOverlayMessage(tekst("zweef.balkon").withStyle(ChatFormatting.AQUA));
                }
                z.opBalkon = nu;
                if (nu % 3 == 0) {
                    toonRing(p, oorsprong, ringen.get(0), RING_LICHT, 28);
                }
            } else if (nu - z.opBalkon < 40 && p.getY() < balkon.y - 0.8 && !p.onGround()) {
                z.vliegt = true;
                z.volgende = 0;
                z.door = 0;
                z.gemist = 0;
                z.ticks = 0;
                z.lift = 0;
                z.boost = 0;
                z.vorige = p.position();
                Schijn.geluid(p, SoundEvents.ENDER_DRAGON_FLAP, p.blockPosition(), 0.6f, 1.6f);
                p.sendOverlayMessage(tekst("zweef.je_zweeft").withStyle(ChatFormatting.AQUA));
            }
            return;
        }
        // in the air: where you look, gently down; the wind helps towards the next ring
        z.ticks++;
        Vec3 kijk = p.getLookAngle();
        Vec3 vlak = new Vec3(kijk.x, 0, kijk.z);
        vlak = vlak.lengthSqr() < 1e-4 ? Vec3.ZERO : vlak.normalize();
        Vec3 midden = p.position().add(0, 0.9, 0);
        double omlaag = -ZWEEF_ZAK + Math.min(0.0, kijk.y) * 0.35 + Math.max(0.0, kijk.y) * 0.03;
        if (z.volgende < ringen.size()) {
            Vec3 ring = Vec3.atCenterOf(oorsprong.offset(ringen.get(z.volgende).pos()));
            Vec3 naar = ring.subtract(midden);
            Vec3 naarVlak = new Vec3(naar.x, 0, naar.z);
            if (naarVlak.lengthSqr() > 1e-4 && vlak.lengthSqr() > 0 && vlak.dot(naarVlak.normalize()) > 0.2 && naarVlak.length() < 30) {
                vlak = vlak.scale(0.72).add(naarVlak.normalize().scale(0.28)).normalize();
                omlaag += Math.max(-0.03, Math.min(0.025, naar.y * 0.012));
            }
        }
        double snel = ZWEEF_SNEL + Math.max(0.0, -kijk.y) * 0.25 + (z.boost > 0 ? 0.2 * z.boost / 20.0 : 0);
        if (z.boost > 0) {
            z.boost--;
        }
        double vy = Math.max(-0.6, omlaag);
        if (z.lift > 0) {
            z.lift--;
            vy = RING_LIFT;
        }
        p.setDeltaMovement(vlak.x * snel, vy, vlak.z * snel);
        p.hurtMarked = true;
        p.resetFallDistance();
        if (z.ticks % 2 == 0) {
            g.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.4, p.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
        }
        // the rings: the next one (or the one after it); a ring whose plane you cross beside it is missed
        Vec3 van = z.vorige.add(0, 0.9, 0);
        for (int k = z.volgende; k < Math.min(ringen.size(), z.volgende + 2); k++) {
            if (door(van, midden, oorsprong, ringen.get(k))) {
                z.gemist += k - z.volgende;
                z.door++;
                z.volgende = k + 1;
                z.lift = RING_LIFT_TICKS;
                z.boost = 20;
                Schijn.geluid(p, SoundEvents.EXPERIENCE_ORB_PICKUP, p.blockPosition(), 1.0f, 0.8f + z.door * 0.08f);
                Schijn.deeltjes(p, ParticleTypes.FIREWORK, midden, 16, 0.6, 0.05);
                E2H1Lucht.titel(p, Component.literal(""), tekst("zweef.ring", z.door, ringen.size()).withStyle(ChatFormatting.AQUA), 20);
                break;
            }
            if (k == z.volgende && langs(van, midden, oorsprong, ringen.get(k))) {
                z.gemist++;
                z.volgende++;
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_BASS.value(), p.blockPosition(), 0.8f, 0.7f);
                p.sendOverlayMessage(tekst("zweef.gemist").withStyle(ChatFormatting.YELLOW));
                break;
            }
        }
        if (z.volgende < ringen.size()) {
            if (z.ticks % 3 == 0) {
                toonRing(p, oorsprong, ringen.get(z.volgende), RING_LICHT, 28);
            }
            if (z.volgende + 1 < ringen.size() && z.ticks % 10 == 0) {
                toonRing(p, oorsprong, ringen.get(z.volgende + 1), RING_LATER, 12);
            }
        }
        z.vorige = p.position();
        int nodig = Math.max(1, ringen.size() - RINGEN_MAG_MISSEN);
        if (z.door >= nodig && z.volgende >= ringen.size()) {
            geland(p, z, true);                           // (all rings behind you: done, a soft way down)
        } else if (z.gemist > RINGEN_MAG_MISSEN) {
            geland(p, z, false);
        } else if (p.onGround() || p.isInWater() || z.ticks > 900) {
            geland(p, z, z.door >= nodig);
        } else if (p.getY() < balkon.y - 70 || !E2H1Lucht.inTempel(p)) {
            boolean goed = z.door >= nodig;               // (off the island: never into the void; the wind takes you back)
            geland(p, z, goed);
            (goed ? NAAR_PLEIN : NAAR_BALKON).put(p.getUUID(), nu);
        }
    }

    /** The column of wind in the middle of the square in front of the hall: it lifts you to the balcony. */
    private static void windzuil(ServerPlayer p, ServerLevel g, long nu) {
        Vec3 zuil = E2H1Lucht.midden(g, "zweef_landing");
        if (zuil == null) {
            return;
        }
        Integer op = OPWAAI.get(p.getUUID());
        if (op != null) {
            p.setDeltaMovement(0, 0.9, 0);
            p.hurtMarked = true;
            p.resetFallDistance();
            Schijn.deeltjes(p, ParticleTypes.CLOUD, p.position(), 4, 0.4, 0.05);
            if (op <= 0) {
                naarBalkon(p);
            } else {
                OPWAAI.put(p.getUUID(), op - 1);
            }
            return;
        }
        if (nu % 4 == 0) {
            for (int i = 0; i < 5; i++) {
                double h = nu * 0.3 + i * Math.PI * 0.4;
                E2H1Lucht.ver(p, ParticleTypes.CLOUD, zuil.add(Math.cos(h) * 0.9, 0.2 + ((nu / 4 + i * 3) % 14) * 0.7, Math.sin(h) * 0.9), 1, 0.02, 0.01);
            }
            E2H1Lucht.ver(p, ParticleTypes.END_ROD, zuil.add(0, 0.3 + (nu / 4 % 10) * 1.0, 0), 1, 0.1, 0.0);
        }
        if (horizontaal(p.position(), zuil) < 1.4 && Math.abs(p.getY() - zuil.y) < 2 && !p.isPassenger()) {
            OPWAAI.put(p.getUUID(), 25);
            Schijn.geluid(p, SoundEvents.ENDER_DRAGON_FLAP, p.blockPosition(), 0.7f, 1.4f);
            p.sendOverlayMessage(tekst("zweef.zuil").withStyle(ChatFormatting.AQUA));
        }
    }

    private static void geland(ServerPlayer p, Zweef z, boolean gehaald) {
        z.vliegt = false;
        p.resetFallDistance();
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false, true));
        long nu = p.level().getGameTime();
        if (gehaald) {
            p.sendSystemMessage(tekst("zweef.goed", z.door).withStyle(ChatFormatting.GREEN));
            E2H1Lucht.titel(p, tekst("zweef.gehaald").withStyle(ChatFormatting.GREEN), tekst("zweef.gehaald_onder", z.door).withStyle(ChatFormatting.YELLOW), 50);
            Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, p.blockPosition(), 1.0f, 1.0f);
            if (E2H1Lucht.LIJN.verder(p, E2H1Lucht.ZWEVEN)) {
                ZWEVEN.remove(p.getUUID());
                NAAR_PLEIN.put(p.getUUID(), nu + 60);       // (a moment of floating, then the wind sets you down)
                MomoJacht.jat(p);
            }
        } else {
            p.sendSystemMessage(tekst("zweef.te_weinig", z.door).withStyle(ChatFormatting.YELLOW));
            NAAR_BALKON.put(p.getUUID(), nu + 40);
        }
    }

    /** Did the line from a to b go through this ring (its plane crossed within its radius)? */
    static boolean door(Vec3 a, Vec3 b, BlockPos oorsprong, Plekken.Plek ring) {
        Vec3 snij = snijpunt(a, b, oorsprong, ring);
        return snij != null && snij.distanceTo(Vec3.atCenterOf(oorsprong.offset(ring.pos()))) <= ring.straal() + 0.7;
    }

    /** Did the line from a to b cross the plane of this ring beside it (not through it, but not far off either: 12 blocks)? */
    static boolean langs(Vec3 a, Vec3 b, BlockPos oorsprong, Plekken.Plek ring) {
        Vec3 snij = snijpunt(a, b, oorsprong, ring);
        if (snij == null) {
            return false;
        }
        double d = snij.distanceTo(Vec3.atCenterOf(oorsprong.offset(ring.pos())));
        return d > ring.straal() + 0.7 && d < 12;
    }

    /** Where the line from a to b crosses the plane of this ring (null: it does not). */
    @Nullable
    private static Vec3 snijpunt(Vec3 a, Vec3 b, BlockPos oorsprong, Plekken.Plek ring) {
        Vec3 c = Vec3.atCenterOf(oorsprong.offset(ring.pos()));
        boolean x = ring.as() == net.minecraft.core.Direction.Axis.X;
        double da = x ? a.x - c.x : a.z - c.z, db = x ? b.x - c.x : b.z - c.z;
        if (Math.signum(da) == Math.signum(db) && Math.abs(db) > 0.6) {
            return null;
        }
        double t = Math.abs(da - db) < 1e-6 ? 1 : da / (da - db);
        return a.add(b.subtract(a).scale(Math.max(0, Math.min(1, t))));
    }

    private static void toonRing(ServerPlayer p, BlockPos oorsprong, Plekken.Plek ring, DustParticleOptions licht, int punten) {
        Vec3 c = Vec3.atCenterOf(oorsprong.offset(ring.pos()));
        boolean x = ring.as() == net.minecraft.core.Direction.Axis.X;
        double r = ring.straal() - 0.3;
        for (int i = 0; i < punten; i++) {
            double h = i * Math.PI * 2 / punten;
            double a = Math.cos(h) * r, b = Math.sin(h) * r;
            E2H1Lucht.ver(p, licht, x ? c.add(0, b, a) : c.add(a, b, 0), 1, 0.0, 0.0);
        }
    }

    /** The rings in flying order (zweefring_01 ..). */
    static List<Plekken.Plek> ringen() {
        return Plekken.van(E2H1Lucht.STRUCTUUR).values().stream().filter(q -> q.id().startsWith("zweefring_"))
                .sorted(java.util.Comparator.comparing(Plekken.Plek::id)).toList();
    }

    static void vergeet(ServerPlayer p) {
        stopBaan(p);
        ZWEVEN.remove(p.getUUID());
        NAAR_BALKON.remove(p.getUUID());
        NAAR_PLEIN.remove(p.getUUID());
        OPWAAI.remove(p.getUUID());
    }

    static void vergeet(UUID speler) {
        Baan b = BANEN.remove(speler);
        if (b != null) {
            Tijdelijk.weg(b.scooter);
        }
        ZWEVEN.remove(speler);
        NAAR_BALKON.remove(speler);
        NAAR_PLEIN.remove(speler);
        OPWAAI.remove(speler);
    }
}
