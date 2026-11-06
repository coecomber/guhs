package nl.juiced.guhs.feature.vadsrijk.indeling;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * T-WERELD: the barriers between the zones of the island sea (WERELDINDELING §3, W-1: passable ONLY by airship). The terrain does
 * most of it (sheer spires of the Splinterwand, the Wolkenkloof channel, the open sea with currents, the grey mist); this is the
 * soft, invisible push-back on top: a player (or the mount / boat / glider he is on) outside every zone's free area is pushed
 * back towards the nearest zone, with bubbles/cloud puffs and one line on the action bar. Never a hard wall: only when someone
 * gets far into a gap (a fast glider, a dev teleport into the sea) is he put back at his last free spot.
 * <p>Exempt: creative/spectator, scenes ({@link RijkScenes}), entities/vehicles tagged {@link #TAG_VRIJ}, and every
 * {@link #VRIJSTELLING} predicate (t-luchtschip registers its airship ride there).
 */
public final class Barrieres {
    private Barrieres() {
    }

    /** Players or vehicles with this tag are never pushed (the airship, a scripted ride). */
    public static final String TAG_VRIJ = "guhs_luchtschip";
    /** Extra exemptions (t-luchtschip: "is this player on an airship flight?"). */
    public static final List<Predicate<ServerPlayer>> VRIJSTELLING = new CopyOnWriteArrayList<>();
    /** Dev switch ({@code /guhs vadsrijk indeling barriere uit}). */
    public static volatile boolean aan = true;

    /** A free area: superellipse (p 4) round (cx, cz) with half axes rx/rz, optionally only from y {@code vanafY} up. */
    public record Vrij(String zone, double cx, double cz, double rx, double rz, int vanafY) {
        public double maat(double x, double z) {
            double a = Math.abs(x - cx) / rx, b = Math.abs(z - cz) / rz;
            return Math.pow(a * a * a * a + b * b * b * b, 0.25);
        }

        public boolean bevat(double x, double y, double z) {
            return y >= vanafY && maat(x, z) <= 1.0;
        }
    }

    /**
     * The free areas (land + a swimming margin). Between them lie the barriers: zone 1 / zone 2 meet only across the Splinterwand
     * band (z ~ -680..-790), zone 2 / zone 3 across the Wolkenkloof channel (x ~ 1034..1104), zone 3 / zone 4 across the Wolkenval
     * (x ~ 1704..1765), Knuffelkamp / zone 1 across the Zoete-Zeestroom.
     */
    public static final List<Vrij> VRIJ = List.of(
            new Vrij("kamp", -373, 996, 205, 200, -64),
            new Vrij("z1", 0, -20, 715, 660, -64),
            new Vrij("z1_zw", -560, 600, 120, 110, -64),          // the south-west peninsula
            new Vrij("z1_schildpad", 520, 720, 90, 90, -64),      // the turtle island off the south-east coast
            new Vrij("z2", 704, -1143, 330, 395, -64),
            new Vrij("z3", 1404, -576, 300, 300, -64),
            new Vrij("z3_werf", 1655, -827, 115, 115, -64),
            new Vrij("z4", 2184, -491, 426, 270, -64),           // (T-INT: fitted to t-z4wereld's real islands, clouds excluded: max 0.947; the Wolkenval stays a barrier)
            new Vrij("z5", 2795, -520, 420, 200, 150),
            new Vrij("proeftuin", 3096, 3096, 140, 140, -64));

    /** The whole island sea: outside it nothing is pushed (far dev spots, gametests). */
    public static boolean inZeegebied(double x, double z) {
        return x > -1300 && x < 3500 && z > -1900 && z < 1700;
    }

    private static final Map<UUID, Vec3> LAATST_VRIJ = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> MELDING = new ConcurrentHashMap<>();

    public static boolean vrij(double x, double y, double z) {
        if (!inZeegebied(x, z)) {
            return true;
        }
        for (Vrij v : VRIJ) {
            if (v.bevat(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /** The nearest free area to x/y/z (by how far outside it you are, relative to its size). */
    public static Vrij dichtste(double x, double y, double z) {
        Vrij best = VRIJ.get(1);
        double bd = Double.MAX_VALUE;
        for (Vrij v : VRIJ) {
            double d = (v.maat(x, z) - 1) * Math.min(v.rx(), v.rz()) + (y < v.vanafY() ? v.vanafY() - y : 0);
            if (d < bd) {
                bd = d;
                best = v;
            }
        }
        return best;
    }

    /** Which barrier you are in (for the message): by the two nearest zones and the height. */
    public static String soort(double x, double y, double z, boolean inWater) {
        if (!inWater && y > 100) {
            return "wind";
        }
        if (x > 2380 && z > -420) {
            return "mist";
        }
        if (z < -640 && z > -820 && x < 1000) {
            return "wand";
        }
        if (x > 1000 && x < 1130 && z < -300) {
            return "kloof";
        }
        if (x > 1650 && x < 1800) {
            return "wolkenval";
        }
        return "stroming";
    }

    static boolean vrijgesteld(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator() || RijkScenes.bezig(p) || p.entityTags().contains(TAG_VRIJ)) {
            return true;
        }
        Entity v = p.getRootVehicle();
        if (v != p && v.entityTags().contains(TAG_VRIJ)) {
            return true;
        }
        for (Predicate<ServerPlayer> f : VRIJSTELLING) {
            if (f.test(p)) {
                return true;
            }
        }
        return false;
    }

    /** Every 4 ticks from the server tick. */
    static void tik(MinecraftServer server, long t) {
        if (!aan || t % 4 != 0) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (vrijgesteld(p)) {
                continue;
            }
            double x = p.getX(), y = p.getY(), z = p.getZ();
            if (vrij(x, y, z)) {
                if (t % 20 == 0) {
                    LAATST_VRIJ.put(p.getUUID(), p.position());
                }
                continue;
            }
            duw(sl, p, t);
        }
    }

    private static void duw(ServerLevel sl, ServerPlayer p, long t) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        Vrij v = dichtste(x, y, z);
        double buiten = (v.maat(x, z) - 1) * Math.min(v.rx(), v.rz());
        Vec3 terug = LAATST_VRIJ.get(p.getUUID());
        Entity rijdt = p.getRootVehicle();
        if (buiten > 60 && terug != null) {          // far into a gap (fast glider, teleport): back to the last free spot
            if (rijdt != p) {
                p.stopRiding();
            }
            p.teleport(new TeleportTransition(sl, terug, Vec3.ZERO, p.getYRot(), p.getXRot(), TeleportTransition.DO_NOTHING));
            meld(p, soort(x, y, z, p.isInWater()), t, true);
            return;
        }
        Vec3 naar = new Vec3(v.cx() - x, 0, v.cz() - z);
        if (naar.lengthSqr() < 1e-4) {
            return;
        }
        double kracht = 0.32 + Math.min(0.5, buiten / 60.0);
        Vec3 duw = naar.normalize().scale(kracht);
        Entity e = rijdt != p ? rijdt : p;
        Vec3 dm = e.getDeltaMovement();
        double vy = dm.y;
        if (!p.isInWater() && y > 70 && (p.isFallFlying() || rijdt != p)) {
            vy = Math.min(vy, -0.08);                   // the wind muur pushes gliders and flying mounts down and back
        }
        e.setDeltaMovement(dm.x * 0.4 + duw.x, vy, dm.z * 0.4 + duw.z);
        e.hurtMarked = true;
        p.hurtMarked = true;
        String s = soort(x, y, z, p.isInWater());
        if (t % 8 == 0) {
            if (p.isInWater()) {
                sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, x, y + 0.3, z, 14, 0.8, 0.4, 0.8, 0.05);
                sl.sendParticles(ParticleTypes.SPLASH, x, y + 1, z, 10, 0.8, 0.2, 0.8, 0.1);
            } else if (s.equals("mist")) {
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1, z, 6, 1.5, 0.6, 1.5, 0.01);
            } else {
                sl.sendParticles(ParticleTypes.CLOUD, x, y + 1, z, 8, 1.0, 0.5, 1.0, 0.05);
            }
        }
        meld(p, s, t, false);
    }

    private static void meld(ServerPlayer p, String soort, long t, boolean nu) {
        Long vorig = MELDING.get(p.getUUID());
        if (!nu && vorig != null && t - vorig < 60) {
            return;
        }
        MELDING.put(p.getUUID(), t);
        p.sendOverlayMessage(Component.translatable("vadsrijk.indeling.barriere." + soort).withStyle(ChatFormatting.AQUA));
    }

    static void vergeet(UUID id) {
        LAATST_VRIJ.remove(id);
        MELDING.remove(id);
    }
}
