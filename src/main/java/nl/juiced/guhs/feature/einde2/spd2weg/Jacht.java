package nl.juiced.guhs.feature.einde2.spd2weg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.sponsguh.DennisMikaEntity;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Duwtje;

/**
 * einde2 "Sponsguh", slice sp_d2_weg (CONTRACT_E2_C 7.7): Dennis-Mika's chase (step 7, variant {@code 7_b}). The player runs
 * along {@code dennis_route} (the road down the hill from the east rim of the loopgraaf, through the town gate) with his own
 * Dennis-Mika on the motorbike behind him ({@code Zicht.alleenVoor}), a little slower than a sprint.
 * <ul>
 *   <li>START: a title and a one-line rule, Dennis revs for {@value #START_WACHT} ticks before he sets off (a visible rev,
 *       not a timer: there is no limit at all);</li>
 *   <li>COUNTER: "Schelpenstad: n blokken" above the hotbar (the way to the gate along the road);</li>
 *   <li>CAUGHT (Dennis within {@value #GEPAKT} blocks): a gentle shove back to the last of two checkpoints (the start, or
 *       halfway at route point {@value #HALF_PUNT}), facing the town, Dennis {@value #VOORSPRONG} blocks behind; nothing
 *       lost, the step stays;</li>
 *   <li>FINISH: the town gate (route point {@value E2SpD2Weg#POORT_PUNT}): a title, then the scene {@code laars};</li>
 *   <li>a player who cannot sprint (hungry) gets a slower Dennis; one who walks off the road (more than {@value #KWIJT}
 *       blocks), dies, logs out or leaves stops the chase: back on the road it goes on from the checkpoint.</li>
 * </ul>
 * {@link Rit} is the pure state machine (the game test drives it without a world); this class puts it in the world.
 */
final class Jacht {
    /** Dennis' speed (blocks per tick): a sprint is 0.28, walking 0.216. Hungry players (no sprint) get {@link #TRAAG}. */
    static final double SNEL = 0.245, TRAAG = 0.185;
    /** Caught within this many blocks; Dennis starts this far behind the checkpoint; off the road from this far. */
    static final double GEPAKT = 1.5, VOORSPRONG = 8.0, KWIJT = 14;
    /** Further than this beside the road, Dennis cannot reach the player: he waits on the road level with him. */
    static final double NAAST_DE_WEG = 4.5;
    /** Ticks Dennis revs before he goes (after the start and after a catch). */
    static final int START_WACHT = 30;
    /** The route points: where the player starts, the halfway checkpoint. */
    static final int START_PUNT = 2, HALF_PUNT = 6;

    // =====================================================================================================================
    // the pure state machine
    // =====================================================================================================================

    /** A route: points and their running lengths along it (only x/z count for "where along"). */
    static final class Route {
        final List<Vec3> p;
        final double[] tot;

        Route(List<Vec3> punten) {
            this.p = List.copyOf(punten);
            this.tot = new double[p.size()];
            for (int i = 1; i < p.size(); i++) {
                tot[i] = tot[i - 1] + vlak(p.get(i - 1), p.get(i));
            }
        }

        double lengte() {
            return tot[tot.length - 1];
        }

        /** The running length at point i. */
        double bij(int i) {
            return tot[Math.max(0, Math.min(tot.length - 1, i))];
        }

        /** The spot at running length s (clamped). */
        Vec3 op(double s) {
            s = Math.max(0, Math.min(lengte(), s));
            for (int i = 1; i < p.size(); i++) {
                if (s <= tot[i] || i == p.size() - 1) {
                    double d = tot[i] - tot[i - 1];
                    double f = d <= 1e-6 ? 0 : (s - tot[i - 1]) / d;
                    return p.get(i - 1).lerp(p.get(i), Math.max(0, Math.min(1, f)));
                }
            }
            return p.get(0);
        }

        /** The direction of the route at s (x/z, unit). */
        Vec3 richting(double s) {
            Vec3 a = op(s - 0.5), b = op(s + 0.5);
            Vec3 d = new Vec3(b.x - a.x, 0, b.z - a.z);
            return d.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : d.normalize();
        }

        /** The running length of the route point nearest to v (x/z). */
        double naast(Vec3 v) {
            double beste = 0, d = Double.MAX_VALUE;
            for (int i = 1; i < p.size(); i++) {
                Vec3 a = p.get(i - 1), b = p.get(i);
                double dx = b.x - a.x, dz = b.z - a.z, len2 = dx * dx + dz * dz;
                double f = len2 <= 1e-9 ? 0 : Math.max(0, Math.min(1, ((v.x - a.x) * dx + (v.z - a.z) * dz) / len2));
                double qx = a.x + dx * f - v.x, qz = a.z + dz * f - v.z, dd = qx * qx + qz * qz;
                if (dd < d) {
                    d = dd;
                    beste = tot[i - 1] + f * (tot[i] - tot[i - 1]);
                }
            }
            return beste;
        }

        static double vlak(Vec3 a, Vec3 b) {
            double dx = b.x - a.x, dz = b.z - a.z;
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    enum Uit {
        NIETS, GEPAKT, HALF, POORT, KWIJT
    }

    /** One chase: where Dennis is along the route, the checkpoint, the rev wait. Pure: no world. */
    static final class Rit {
        final Route r;
        final double poort;
        double dennis, checkpoint, speler;
        boolean half;
        int wacht = START_WACHT;
        int gepakt;

        Rit(Route r, double vanaf) {
            this.r = r;
            this.poort = r.bij(E2SpD2Weg.POORT_PUNT);
            this.checkpoint = r.bij(START_PUNT);
            if (vanaf >= r.bij(HALF_PUNT)) {
                checkpoint = r.bij(HALF_PUNT);
                half = true;
            }
            this.speler = vanaf;
            this.dennis = Math.max(0, Math.min(vanaf, checkpoint) - VOORSPRONG);
        }

        /** One tick: the player stands at v (sprinting possible or not). */
        Uit stap(Vec3 v, boolean kanSprinten) {
            speler = r.naast(v);
            Vec3 op = r.op(speler);
            if (Route.vlak(op, v) > KWIJT) {
                return Uit.KWIJT;
            }
            if (speler >= poort - 0.8) {
                return Uit.POORT;
            }
            Uit uit = Uit.NIETS;
            if (!half && speler >= r.bij(HALF_PUNT)) {
                half = true;
                checkpoint = r.bij(HALF_PUNT);
                uit = Uit.HALF;
            }
            if (wacht > 0) {
                wacht--;
            } else {
                dennis = Math.min(poort, dennis + (kanSprinten ? SNEL : TRAAG));
            }
            if (dennis >= speler - GEPAKT) {
                if (Route.vlak(op, v) <= NAAST_DE_WEG) {
                    gepakt++;
                    dennis = Math.max(0, checkpoint - VOORSPRONG);
                    wacht = START_WACHT;
                    return Uit.GEPAKT;
                }
                dennis = Math.max(0, speler - GEPAKT - 0.01);          // (beside the road: he waits on it, he never drives past)
            }
            return uit;
        }

        /** The blocks still to go to the gate. */
        int nogTeGaan() {
            return (int) Math.ceil(Math.max(0, poort - speler));
        }
    }

    // =====================================================================================================================
    // in the world
    // =====================================================================================================================

    private record Actief(Rit rit, DennisMikaEntity dennis, List<Vec3> punten) {
    }

    private static final Map<UUID, Actief> ACTIEF = new ConcurrentHashMap<>();

    private Jacht() {
    }

    static boolean actief(ServerPlayer p) {
        return ACTIEF.containsKey(p.getUUID());
    }

    /** The route in the world, every point on the ground (null: Schelpenstad not known). */
    @Nullable
    static Route route(ServerLevel g) {
        List<Vec3> ruw = E2SpD2Weg.pad(g, "dennis_route");
        if (ruw.size() <= E2SpD2Weg.POORT_PUNT) {
            return null;
        }
        List<Vec3> op = new ArrayList<>();
        for (Vec3 v : ruw) {
            op.add(E2SpD2Weg.grond(g, v));
        }
        return new Route(op);
    }

    /** Starts the chase. {@code vooraan}: from the start (the player is put there after the scene), else from where he is. */
    static boolean start(ServerPlayer p, boolean vooraan) {
        ServerLevel g = E2SpD2Weg.guheinde(p);
        Route r = g == null ? null : route(g);
        if (r == null) {
            return false;
        }
        stop(p);
        vooraan |= r.naast(p.position()) < r.bij(START_PUNT);           // (behind the start, by the rim: put on the start)
        double vanaf = vooraan ? r.bij(START_PUNT) : r.naast(p.position());
        Rit rit = new Rit(r, vanaf);
        if (vooraan) {
            zet(p, r, r.bij(START_PUNT));
        }
        DennisMikaEntity d = E2SponsguhFeature.DENNIS_MIKA.get().create(g, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return false;
        }
        Vec3 dp = r.op(rit.dennis);
        d.snapTo(dp.x, dp.y, dp.z, yaw(r.richting(rit.dennis)), 0f);
        d.setOpMotor(true);
        d.zetStil(true);
        d.setNoAi(true);
        d.zetAnimatie("rij");
        E2SpD2Weg.eigen(d, p);
        g.addFreshEntity(d);
        ACTIEF.put(p.getUUID(), new Actief(rit, d, r.p));
        SpAudio.moment(p, "dennis");
        E2SpD2Weg.titel(p, "jacht.titel", "jacht.regel");
        E2SpD2Weg.zeg(p, "jacht.uitleg");
        Schijn.geluid(p, E2SponsguhFeature.MOBIEL_MOTOR.get(), BlockPos.containing(dp), 1.0f, 1.2f);
        return true;
    }

    /** The chase was on before (flag 7_b): it goes on once the player is back on the road (within reach of the route). */
    static void hervat(ServerPlayer p) {
        ServerLevel g = E2SpD2Weg.guheinde(p);
        Route r = g == null ? null : route(g);
        if (r == null || actief(p)) {
            return;
        }
        double s = r.naast(p.position());
        if (Route.vlak(r.op(s), p.position()) > 6 || Math.abs(r.op(s).y - p.getY()) > 4) {
            return;
        }
        if (s >= r.bij(E2SpD2Weg.POORT_PUNT) - 0.8) {
            E2SpD2Weg.naPoort(p);                                   // (already through the gate: the boot)
            return;
        }
        if (start(p, false)) {
            E2SpD2Weg.zeg(p, "jacht.weer");
        }
    }

    /** Every tick of a player in a chase. */
    static void tick(ServerPlayer p) {
        Actief a = ACTIEF.get(p.getUUID());
        if (a == null) {
            return;
        }
        if (a.dennis().isRemoved() || p.level() != a.dennis().level() || !p.isAlive() || E2SpD2Weg.lijn().stap(p) != E2SpD2Weg.DENNIS) {
            stop(p);
            return;
        }
        Rit rit = a.rit();
        boolean sprint = p.isCreative() || p.getFoodData().getFoodLevel() > 6;
        Uit uit = rit.stap(p.position(), sprint);
        Vec3 dp = rit.r.op(rit.dennis);
        DennisMikaEntity d = a.dennis();
        float yaw = yaw(rit.r.richting(rit.dennis));
        d.teleportTo(dp.x, dp.y, dp.z);
        d.setYRot(yaw);
        d.setYHeadRot(yaw);
        d.setYBodyRot(yaw);
        ServerLevel g = p.level();
        long t = g.getServer().getTickCount();
        if (t % 4 == 0 && rit.wacht <= 0) {
            Vec3 r = rit.r.richting(rit.dennis);
            Schijn.deeltjes(p, ParticleTypes.SMOKE, dp.add(-r.x * 0.9, 0.5, -r.z * 0.9), 2, 0.1, 0.01);
        }
        if (t % 16 == 0) {
            Schijn.geluid(p, E2SponsguhFeature.MOBIEL_MOTOR.get(), BlockPos.containing(dp), 0.8f, rit.wacht > 0 ? 0.8f : 1.1f);
        }
        if (t % 5 == 0) {
            p.sendOverlayMessage(Component.translatable(E2SpD2Weg.G + "jacht.balk", rit.nogTeGaan()).withStyle(ChatFormatting.GOLD));
        }
        switch (uit) {
            case GEPAKT -> {
                SpAudio.stinger(p, "stinger.mislukt");
                zet(p, rit.r, rit.checkpoint);
                E2SpD2Weg.titel(p, "jacht.gepakt", rit.half ? "jacht.gepakt_half" : "jacht.gepakt_start");
                Vec3 nd = rit.r.op(rit.dennis);
                d.teleportTo(nd.x, nd.y, nd.z);
            }
            case HALF -> {
                E2SpD2Weg.balk(p, "jacht.half");
                SpAudio.stinger(p, "stinger.opgelost");
            }
            case POORT -> {
                stop(p);
                E2SpD2Weg.titel(p, "jacht.poort", null);
                E2SpD2Weg.naPoort(p);
            }
            case KWIJT -> {
                stop(p);
                E2SpD2Weg.zeg(p, "jacht.kwijt");
            }
            default -> {
            }
        }
    }

    /** Puts the player at running length s of the route, facing along it (a gentle shove: Duwtje). */
    private static void zet(ServerPlayer p, Route r, double s) {
        Vec3 v = r.op(s);
        Duwtje.terug(p, GuheindeFeature.GUHEINDE, v.add(0, 0.05, 0), yaw(r.richting(s)));
    }

    /** The Minecraft yaw of a direction (0 = south, -90 = east). */
    static float yaw(Vec3 d) {
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    /** Stops this player's chase (Dennis goes; the flag 7_b stays: the step is still on). */
    static void stop(ServerPlayer p) {
        Actief a = ACTIEF.remove(p.getUUID());
        if (a != null) {
            a.dennis().discard();
            SpAudio.stopMoment(p, 20);
        }
    }

    /** (dev) the chase as if the player reached the gate. */
    static boolean win(ServerPlayer p) {
        if (!actief(p)) {
            return false;
        }
        stop(p);
        E2SpD2Weg.naPoort(p);
        return true;
    }
}
