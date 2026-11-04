package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.klassen.Fx;

/**
 * The upward blossom-wind columns of zone 3 (ronde 18: "wél windkolommen") and the bookkeeping around riding. z3-wereld places
 * the columns ({@code Z3WereldPlekken.WINDKOLOMMEN}, copied in {@link #KOLOMMEN}); this class moves you:
 * <ul>
 *   <li>on foot: Levitation (fast) while inside the column, set down towards the landing at the top, plus a few seconds Slow
 *   Falling (the server's flying check allows Levitation);</li>
 *   <li>on a mount: the mount gets no gravity while inside (synced, so the server does not see a "floating vehicle"); the riding
 *   client pushes it up and onto the landing ({@code client/Z3WindClient}, which also does the Pluimguh's glide);</li>
 *   <li>every column you ride up counts for the "wind" bounty; blocks ridden in zone 3 count for the "rit" bounties.</li>
 * </ul>
 */
public final class Z3Wind {
    /** One column: inside radius r around (x, z), from y0 up to y1 (the landing's stand y), set down towards (lx, lz). */
    public record Kolom(String id, int x, int z, int y0, int y1, double r, int lx, int lz) {
        public boolean binnen(double px, double py, double pz) {
            double dx = px - (x + 0.5), dz = pz - (z + 0.5);
            return dx * dx + dz * dz <= r * r && py >= y0 - 1 && py <= y1 + 1.5;
        }

        public boolean boven(double py) {
            return py >= y1 - 0.5;
        }
    }

    /** Copied from z3-wereld POI_Z3.md v0 (the integrator may point this at Z3WereldPlekken.WINDKOLOMMEN). */
    public static volatile List<Kolom> KOLOMMEN = List.of(
            new Kolom("hangnest", 2807, 56, 66, 195, 3.0, 2807, 40),
            new Kolom("paddenbloesem_tak", 2715, 349, 69, 198, 3.0, 2705, 361),
            new Kolom("werf_tak", 2734, -390, 71, 197, 3.0, 2724, -401),
            new Kolom("mosvoet_kruin", 2094, 195, 73, 258, 3.5, 2101, 188),
            new Kolom("lichtkever_kruin", 2259, -300, 77, 251, 3.5, 2261, -291),
            new Kolom("honingtak", 1897, 38, 71, 239, 3.0, 1903, 30));

    /** Upward speed in blocks per tick (Levitation amplifier 9 = 0.5). */
    public static final double OMHOOG = 0.55;
    private static final Map<UUID, String> IN = new ConcurrentHashMap<>();
    private static final Set<UUID> ZONDER_ZWAARTE = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Vec3> LAATST = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> RIT = new ConcurrentHashMap<>();

    private Z3Wind() {
    }

    @Nullable
    public static Kolom kolom(double x, double y, double z) {
        for (Kolom k : KOLOMMEN) {
            if (k.binnen(x, y, z)) {
                return k;
            }
        }
        return null;
    }

    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (!Z3Lagen.inZone3(p.getX(), p.getZ()) || p.isSpectator()) {
                if (IN.remove(p.getUUID()) != null) {
                    zwaarte(p, null);
                }
                LAATST.remove(p.getUUID());
                continue;
            }
            Entity v = p.getVehicle();
            Kolom k = kolom(p.getX(), p.getY(), p.getZ());
            String was = IN.get(p.getUUID());
            if (k == null) {
                if (was != null) {
                    IN.remove(p.getUUID());
                    zwaarte(p, null);
                }
            } else {
                if (!k.id().equals(was)) {
                    IN.put(p.getUUID(), k.id());
                    Premies.wind(p);
                }
                if (v != null) {
                    zwaarte(p, v);
                } else if (!k.boven(p.getY())) {
                    p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 6, 9, false, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false, false));
                } else {
                    p.removeEffect(MobEffects.LEVITATION);
                    Vec3 naar = new Vec3(k.lx() + 0.5 - p.getX(), 0, k.lz() + 0.5 - p.getZ());
                    if (naar.lengthSqr() > 0.25 && sl.getGameTime() % 4 == 0) {
                        Fx.duw(p, naar.normalize().scale(0.45).add(0, 0.2, 0));
                    }
                }
                if (sl.getGameTime() % 3 == 0) {
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() - 0.5, p.getZ(), 3, 0.8, 0.4, 0.8, 0.02);
                }
            }
            // ride bookkeeping (blocks ridden on a mount in zone 3)
            Vec3 nu = p.position();
            Vec3 oud = LAATST.put(p.getUUID(), nu);
            if (v != null && oud != null) {
                double d = Math.min(3, Math.sqrt(Math.pow(nu.x - oud.x, 2) + Math.pow(nu.z - oud.z, 2)));
                double t = RIT.merge(p.getUUID(), d, Double::sum);
                if (t >= 20) {
                    RIT.put(p.getUUID(), t - 20);
                    Premies.rit(p, 20);
                }
            }
        }
    }

    /** Mount inside a column: no gravity (restored when it leaves). */
    private static void zwaarte(ServerPlayer p, @Nullable Entity v) {
        if (v != null) {
            if (!v.isNoGravity()) {
                v.setNoGravity(true);
                ZONDER_ZWAARTE.add(v.getUUID());
            }
            return;
        }
        Entity oud = p.getVehicle();
        if (oud != null && ZONDER_ZWAARTE.remove(oud.getUUID())) {
            oud.setNoGravity(false);
        }
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, false));
    }

    static void vergeet(UUID id) {
        IN.remove(id);
        LAATST.remove(id);
        RIT.remove(id);
    }
}
