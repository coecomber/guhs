package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.erfenis.Base;
import nl.juiced.guhs.feature.erfenis.Erfenis;
import nl.juiced.guhs.feature.erfenis.ErfenisData;
import nl.juiced.guhs.feature.erfenis.Plot;
import nl.juiced.guhs.feature.erfenis.sherlock.Zaak.Kaart;

/**
 * The clues in the player's own village: where each one lies (a fixed plot tile per case, moved to the nearest free grass
 * when something stands there), the glitter only the owner sees while holding the {@link Vergrootglas}, and finding one by
 * looking at it and using the glass. Visitors never see or find clues of somebody else's village.
 */
public final class Sporen {
    /** How far the glitter shows, how far a clue can be picked up, and the "aim" (radians) it forgives. */
    static final double ZICHT = 12, PAK = 6, MIK = 0.38;
    private static final long VERS = 20 * 30;

    private record Plekken(String zaak, long tijd, Map<String, BlockPos> plekken) {
    }

    private static final Map<UUID, Plekken> CACHE = new ConcurrentHashMap<>();

    private Sporen() {
    }

    /** The min corner (tile 0,0 at the grass top) of the player's own plot when they stand on it, else null. */
    @Nullable
    public static BlockPos eigenPlot(ServerPlayer p) {
        if (!Erfenis.in(p)) {
            return null;
        }
        Base b = ErfenisData.get(p.level().getServer()).van(p.getUUID());
        if (b == null || Plot.cel(p.getBlockX()) != b.cx || Plot.cel(p.getBlockZ()) != b.cz) {
            return null;
        }
        return Plot.oorsprong(b.cx, b.cz);
    }

    /** Where to stand at plot tile (x, z): the free grass nearest to it (feet position). */
    public static BlockPos plek(ServerLevel level, BlockPos oorsprong, int x, int z) {
        for (int r = 0; r <= 7; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int bx = oorsprong.getX() + x + dx, bz = oorsprong.getZ() + z + dz;
                    int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                    if (top == Plot.Y + 1) {
                        return new BlockPos(bx, top, bz);
                    }
                }
            }
        }
        int bx = oorsprong.getX() + x, bz = oorsprong.getZ() + z;
        return new BlockPos(bx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz), bz);
    }

    /** The spots of the glass clues of this case in this player's village (cached a little while). */
    static Map<String, BlockPos> plekken(ServerPlayer p, BlockPos oorsprong, Zaak z) {
        long nu = p.level().getGameTime();
        Plekken c = CACHE.get(p.getUUID());
        if (c != null && c.zaak().equals(z.id()) && nu - c.tijd() < VERS && nu >= c.tijd()) {
            return c.plekken();
        }
        Map<String, BlockPos> m = new HashMap<>();
        for (Kaart k : z.glasKaarten()) {
            m.put(k.id(), plek(p.level(), oorsprong, k.x(), k.z()));
        }
        CACHE.put(p.getUUID(), new Plekken(z.id(), nu, m));
        return m;
    }

    static void vergeet(UUID speler) {
        CACHE.remove(speler);
    }

    /** The clues still to find (glass ones) of the running case, with their spots; empty when not on the own plot. */
    static List<Map.Entry<Kaart, BlockPos>> open(ServerPlayer p, Voortgang v) {
        List<Map.Entry<Kaart, BlockPos>> uit = new ArrayList<>();
        Zaak z = v.actief == null ? null : Zaken.van(v.actief);
        BlockPos o = eigenPlot(p);
        if (z == null || o == null) {
            return uit;
        }
        Map<String, BlockPos> m = plekken(p, o, z);
        for (Kaart k : z.glasKaarten()) {
            if (!v.gevonden.contains(k.id()) && m.containsKey(k.id())) {
                uit.add(Map.entry(k, m.get(k.id())));
            }
        }
        return uit;
    }

    /** Twice a second while the owner holds the glass: the glitter of the clues nearby (only they see it). */
    static void glinster(ServerPlayer p, Voortgang v, long tick) {
        for (Map.Entry<Kaart, BlockPos> e : open(p, v)) {
            BlockPos b = e.getValue();
            Vec3 m = Vec3.atBottomCenterOf(b);
            if (p.position().distanceToSqr(m) > ZICHT * ZICHT) {
                continue;
            }
            int kleur = e.getKey().kleur();
            DustParticleOptions stof = new DustParticleOptions(kleur, 0.9f);
            if (e.getKey().id().contains("afdruk")) {
                // little footprints: three toes in a row towards the north
                for (int i = 0; i < 3; i++) {
                    p.level().sendParticles(p, stof, true, false, m.x - 0.35 + i * 0.35, m.y + 0.05, m.z - 0.1 - i * 0.25, 1, 0.02, 0, 0.02, 0);
                }
            } else {
                p.level().sendParticles(p, stof, true, false, m.x, m.y + 0.15, m.z, 3, 0.18, 0.06, 0.18, 0);
            }
            if (tick % 20 == 0) {
                p.level().sendParticles(p, ParticleTypes.END_ROD, true, false, m.x, m.y + 0.5, m.z, 1, 0.1, 0.15, 0.1, 0.005);
            }
        }
    }

    /** The clue the player is looking at (or standing on), within reach; null when none. */
    @Nullable
    static Kaart gezien(ServerPlayer p, Voortgang v) {
        Vec3 oog = p.getEyePosition();
        Vec3 kijk = p.getLookAngle();
        Kaart beste = null;
        double besteHoek = MIK;
        for (Map.Entry<Kaart, BlockPos> e : open(p, v)) {
            Vec3 m = Vec3.atBottomCenterOf(e.getValue()).add(0, 0.1, 0);
            Vec3 naar = m.subtract(oog);
            double afstand = naar.length();
            if (afstand > PAK) {
                continue;
            }
            double plat = Math.hypot(m.x - p.getX(), m.z - p.getZ());
            double hoek = plat < 1.6 ? 0 : Math.acos(Math.max(-1, Math.min(1, naar.normalize().dot(kijk))));
            if (hoek <= besteHoek) {
                besteHoek = hoek;
                beste = e.getKey();
            }
        }
        return beste;
    }

    /** How far the nearest clue still to find is (blocks), or -1. */
    static double dichtstbij(ServerPlayer p, Voortgang v) {
        double d = -1;
        for (Map.Entry<Kaart, BlockPos> e : open(p, v)) {
            double a = Math.sqrt(p.position().distanceToSqr(Vec3.atBottomCenterOf(e.getValue())));
            if (d < 0 || a < d) {
                d = a;
            }
        }
        return d;
    }

    /** The line in the corner: "Sporen 2/4". */
    static void hud(ServerPlayer p, Zaak z, Voortgang v) {
        int glas = z.glasKaarten().size();
        long gevonden = z.glasKaarten().stream().filter(k -> v.gevonden.contains(k.id())).count();
        p.sendOverlayMessage(Component.translatable("sherlock.hud.sporen", gevonden, glas, Component.translatable("sherlock.zaak." + z.id() + ".titel")));
    }
}
