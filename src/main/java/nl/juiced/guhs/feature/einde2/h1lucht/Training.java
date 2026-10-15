package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * einde2 phase B, slice h1_lucht, steps 4 and 5 (DESIGN 5 h1: Guhatso's training): the luchtscooter-baan and the
 * zweefringen.
 * <ul>
 *   <li>The track: at {@code scooter_start} a countdown (the first time after wink 1, Sjokkel on the track), then the wind
 *       pushes you (fast feet, a cloud under you) along {@code scooter_baan}; pass every gate (its points, in order) and reach
 *       {@code scooter_finish} within {@value #BAAN_SECONDEN} seconds. Too slow, or blown off the track: back to the start.</li>
 *   <li>The rings: jump off {@code zweef_start} (the south balcony of the great tower) and you glide: you go where you look,
 *       looking down is faster, looking up slower. Fly through the rings {@code zweefring_01..14}; ten of them are enough.
 *       Landing ends the flight; too few: climb up and try again.</li>
 * </ul>
 * Never a fall hurts in the temple ({@link Tempel#opVal}).
 */
public final class Training {
    public static final int BAAN_SECONDEN = 60;
    /** How many rings make it. */
    public static final int RINGEN_NODIG = 10;

    private static final class Baan {
        int aftellen = 60, poort = 1;
        long tot;
        boolean bezig;
    }

    private static final class Zweef {
        boolean vliegt;
        int volgende, door, ticks;
        long opBalkon = -1000;
        Vec3 vorige;
    }

    private static final Map<UUID, Baan> BANEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Zweef> ZWEVEN = new ConcurrentHashMap<>();

    private Training() {
    }

    private static net.minecraft.network.chat.MutableComponent tekst(String key, Object... args) {
        return Component.translatable(E2H1Lucht.P + key, args);
    }

    /** Every tick, for a player in the temple at step 4 or 5. */
    static void tick(ServerPlayer p) {
        int stap = E2H1Lucht.LIJN.stap(p);
        if (stap == E2H1Lucht.SCOOTER) {
            baan(p);
        } else if (BANEN.remove(p.getUUID()) != null) {
            p.removeEffect(MobEffects.SPEED);
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
        if (pad.size() < 2 || start == null || finish == null || Cutscenes.bezig(p)) {
            return;
        }
        Baan b = BANEN.get(p.getUUID());
        if (b == null) {
            if (horizontaal(p.position(), start) < 2.5 && Math.abs(p.getY() - start.y) < 2) {
                if (E2H1Lucht.LIJN.eenmalig(p, "knipoog_sjokkel")) {
                    BlockPos anker = knipoogAnker(g);
                    if (anker != null && Cutscenes.speel(p, H1Scenes.KNIPOOG_SJOKKEL, anker, Rotation.NONE, s -> BANEN.put(s.getUUID(), new Baan()))) {
                        return;
                    }
                }
                BANEN.put(p.getUUID(), new Baan());
                p.sendOverlayMessage(tekst("scooter.klaar").withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        long nu = g.getGameTime();
        if (!b.bezig) {
            if (horizontaal(p.position(), start) > 4) {
                BANEN.remove(p.getUUID());              // (walked away before the start)
                return;
            }
            if (b.aftellen % 20 == 0 && b.aftellen > 0) {
                p.sendOverlayMessage(tekst("scooter.tel", b.aftellen / 20).withStyle(ChatFormatting.YELLOW));
                Schijn.geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), p.blockPosition(), 1.0f, 1.0f);
            }
            if (--b.aftellen <= 0) {
                b.bezig = true;
                b.tot = nu + BAAN_SECONDEN * 20L;
                p.addEffect(new MobEffectInstance(MobEffects.SPEED, BAAN_SECONDEN * 20 + 40, 2, false, false, true));
                p.sendOverlayMessage(tekst("scooter.zoef").withStyle(ChatFormatting.AQUA));
                Schijn.geluid(p, SoundEvents.FIREWORK_ROCKET_LAUNCH, p.blockPosition(), 1.0f, 1.2f);
            }
            return;
        }
        // on the way: a cloud under the feet (the scooter), the gates in order, the clock
        g.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 2, 0.25, 0.05, 0.25, 0.01);
        if (b.poort < pad.size()) {
            Vec3 poort = pad.get(b.poort);
            if (horizontaal(p.position(), poort) < 5.5 && Math.abs(p.getY() - poort.y) < 4) {
                b.poort++;
            }
            Schijn.deeltjes(p, ParticleTypes.END_ROD, pad.get(Math.min(b.poort, pad.size() - 1)).add(0, 1.2, 0), 2, 0.3, 0.0);
        }
        long over = Math.max(0, (b.tot - nu) / 20);
        if (nu % 10 == 0) {
            p.sendOverlayMessage(tekst("scooter.stand", Math.min(b.poort, pad.size()), pad.size(), over).withStyle(ChatFormatting.YELLOW));
        }
        if (b.poort >= pad.size() && horizontaal(p.position(), finish) < 4) {
            long sec = (BAAN_SECONDEN * 20L - (b.tot - nu)) / 20;
            BANEN.remove(p.getUUID());
            p.removeEffect(MobEffects.SPEED);
            Schijn.deeltjes(p, ParticleTypes.FIREWORK, p.position().add(0, 1.5, 0), 30, 0.5, 0.1);
            Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, p.blockPosition(), 1.0f, 1.2f);
            p.sendSystemMessage(tekst("scooter.gehaald", sec).withStyle(ChatFormatting.GREEN));
            E2H1Lucht.LIJN.verder(p, E2H1Lucht.SCOOTER);
            return;
        }
        if (nu > b.tot) {
            mis(p, "scooter.te_laat");
        } else if (afstandTotPad(p.position(), pad) > 12) {
            mis(p, "scooter.van_de_baan");
        }
    }

    private static void mis(ServerPlayer p, String key) {
        BANEN.remove(p.getUUID());
        p.removeEffect(MobEffects.SPEED);
        p.sendSystemMessage(tekst(key).withStyle(ChatFormatting.YELLOW));
        E2H1Lucht.zetNaar(p, "scooter_start", p.getYRot());
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

    // =====================================================================================================================
    // the zweefringen
    // =====================================================================================================================

    /** Is this player gliding right now? */
    public static boolean zweeft(ServerPlayer p) {
        Zweef z = ZWEVEN.get(p.getUUID());
        return z != null && z.vliegt;
    }

    private static void zweef(ServerPlayer p) {
        ServerLevel g = p.level();
        Vec3 balkon = E2H1Lucht.midden(g, "zweef_start");
        if (balkon == null) {
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
            boolean op = horizontaal(p.position(), balkon) < 3.0 && Math.abs(p.getY() - balkon.y) < 1.5;
            if (op) {
                if (nu - z.opBalkon > 60) {
                    p.sendOverlayMessage(tekst("zweef.balkon").withStyle(ChatFormatting.AQUA));
                }
                z.opBalkon = nu;
                toonRing(p, oorsprong, ringen.get(0));
            } else if (nu - z.opBalkon < 40 && p.getY() < balkon.y - 0.8 && !p.onGround()) {
                z.vliegt = true;
                z.volgende = 0;
                z.door = 0;
                z.ticks = 0;
                z.vorige = p.position();
                Schijn.geluid(p, SoundEvents.ENDER_DRAGON_FLAP, p.blockPosition(), 0.6f, 1.6f);
            }
            return;
        }
        // in the air: where you look, gently down
        z.ticks++;
        Vec3 kijk = p.getLookAngle();
        Vec3 vlak = new Vec3(kijk.x, 0, kijk.z);
        vlak = vlak.lengthSqr() < 1e-4 ? Vec3.ZERO : vlak.normalize();
        double omlaag = -0.07 + Math.min(0.0, kijk.y) * 0.45 + Math.max(0.0, kijk.y) * 0.05;
        double snel = 0.55 + Math.max(0.0, -kijk.y) * 0.25;
        p.setDeltaMovement(vlak.x * snel, Math.max(-0.6, omlaag), vlak.z * snel);
        p.hurtMarked = true;
        p.resetFallDistance();
        if (z.ticks % 2 == 0) {
            g.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.4, p.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
        }
        // the rings: the next one, or one of the two after it (a missed ring is missed)
        Vec3 nuPlek = p.position().add(0, 0.9, 0);
        for (int k = z.volgende; k < Math.min(ringen.size(), z.volgende + 3); k++) {
            if (door(z.vorige.add(0, 0.9, 0), nuPlek, oorsprong, ringen.get(k))) {
                z.door++;
                z.volgende = k + 1;
                Schijn.geluid(p, SoundEvents.EXPERIENCE_ORB_PICKUP, p.blockPosition(), 1.0f, 0.8f + z.door * 0.06f);
                Schijn.deeltjes(p, ParticleTypes.FIREWORK, nuPlek, 16, 0.6, 0.05);
                p.sendOverlayMessage(tekst("zweef.ring", z.door, ringen.size()).withStyle(ChatFormatting.AQUA));
                break;
            }
        }
        if (z.volgende < ringen.size() && z.ticks % 8 == 0) {
            toonRing(p, oorsprong, ringen.get(z.volgende));
        }
        z.vorige = p.position();
        if (p.onGround() || p.isInWater() || z.ticks > 1200 || p.getY() < balkon.y - 90) {
            geland(p, z, ringen.size());
        }
    }

    private static void geland(ServerPlayer p, Zweef z, int totaal) {
        z.vliegt = false;
        p.resetFallDistance();
        if (z.door >= RINGEN_NODIG) {
            p.sendSystemMessage(tekst("zweef.goed", z.door).withStyle(ChatFormatting.GREEN));
            Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, p.blockPosition(), 1.0f, 1.0f);
            if (E2H1Lucht.LIJN.verder(p, E2H1Lucht.ZWEVEN)) {
                ZWEVEN.remove(p.getUUID());
                MomoJacht.jat(p);
            }
        } else {
            p.sendSystemMessage(tekst("zweef.te_weinig", z.door).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** Did the line from a to b go through this ring (its plane crossed within its radius)? */
    static boolean door(Vec3 a, Vec3 b, BlockPos oorsprong, Plekken.Plek ring) {
        Vec3 c = Vec3.atCenterOf(oorsprong.offset(ring.pos()));
        boolean x = ring.as() == net.minecraft.core.Direction.Axis.X;
        double da = x ? a.x - c.x : a.z - c.z, db = x ? b.x - c.x : b.z - c.z;
        if (Math.signum(da) == Math.signum(db) && Math.abs(db) > 0.6) {
            return false;
        }
        double t = Math.abs(da - db) < 1e-6 ? 1 : da / (da - db);
        Vec3 snij = a.add(b.subtract(a).scale(Math.max(0, Math.min(1, t))));
        return snij.distanceTo(c) <= ring.straal() + 0.7;
    }

    private static void toonRing(ServerPlayer p, BlockPos oorsprong, Plekken.Plek ring) {
        Vec3 c = Vec3.atCenterOf(oorsprong.offset(ring.pos()));
        boolean x = ring.as() == net.minecraft.core.Direction.Axis.X;
        for (int i = 0; i < 12; i++) {
            double h = i * Math.PI / 6;
            double a = Math.cos(h) * ring.straal(), b = Math.sin(h) * ring.straal();
            Schijn.deeltjes(p, ParticleTypes.END_ROD, x ? c.add(0, b, a) : c.add(a, b, 0), 1, 0.0, 0.0);
        }
    }

    /** The rings in flying order (zweefring_01 ..). */
    static List<Plekken.Plek> ringen() {
        return Plekken.van(E2H1Lucht.STRUCTUUR).values().stream().filter(q -> q.id().startsWith("zweefring_"))
                .sorted(java.util.Comparator.comparing(Plekken.Plek::id)).toList();
    }

    static void vergeet(ServerPlayer p) {
        if (BANEN.remove(p.getUUID()) != null) {
            p.removeEffect(MobEffects.SPEED);
        }
        ZWEVEN.remove(p.getUUID());
    }

    static void vergeet(UUID speler) {
        BANEN.remove(speler);
        ZWEVEN.remove(speler);
    }
}
