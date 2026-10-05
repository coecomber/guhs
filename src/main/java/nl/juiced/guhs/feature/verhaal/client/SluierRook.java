package nl.juiced.guhs.feature.verhaal.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.VerhaalPayloads;

/**
 * bbq2 (verhaal engine): the smoke of Guhdalfs sluier ({@link Sluiers}), for the viewer only. The server tells which walls
 * stand near the player and are still closed for them ({@code guhs:verhaal_sluiers}); here thick smoke swirls up along
 * those walls within {@link #ZICHT} blocks (always drawn, also with particles on "minimal": it is what tells you where
 * you can't go yet). A wall that leaves the list (the sluier opened for this player) dissolves in a puff.
 */
public final class SluierRook {
    /** How far from the viewer the wall is drawn (blocks). */
    public static final double ZICHT = 48;
    /** Smoke clouds per block of wall per tick. */
    private static final double DICHTHEID = 0.035;

    private static List<Sluiers.Zone> zones = List.of();
    /** Walls that just opened: {zone, ticks left of the puff}. */
    private static final List<Object[]> LOST = new ArrayList<>();

    private SluierRook() {
    }

    /** The walls drawn now (for checks). */
    public static List<Sluiers.Zone> zones() {
        return zones;
    }

    static void ontvang(VerhaalPayloads.Rook p) {
        List<Sluiers.Zone> nieuw = new ArrayList<>();
        for (Tag t : p.data().getListOrEmpty("Zones")) {
            if (t instanceof CompoundTag c) {
                nieuw.add(new Sluiers.Zone(c.getStringOr("S", ""), c.getIntOr("X0", 0), c.getIntOr("Z0", 0), c.getIntOr("X1", 0), c.getIntOr("Z1", 0)));
            }
        }
        for (Sluiers.Zone oud : zones) {
            if (!nieuw.contains(oud)) {
                LOST.add(new Object[] {oud, 30});
            }
        }
        zones = List.copyOf(nieuw);
    }

    static void wis() {
        zones = List.of();
        LOST.clear();
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            wis();
            return;
        }
        if (mc.isPaused() || zones.isEmpty() && LOST.isEmpty()) {
            return;
        }
        Vec3 oog = mc.gameRenderer.getMainCamera().position();
        RandomSource r = level.getRandom();
        for (Sluiers.Zone z : zones) {
            muur(level, r, z, oog, false);
        }
        for (int i = LOST.size() - 1; i >= 0; i--) {
            Object[] l = LOST.get(i);
            muur(level, r, (Sluiers.Zone) l[0], oog, true);
            int over = (Integer) l[1] - 1;
            if (over <= 0) {
                LOST.remove(i);
            } else {
                l[1] = over;
            }
        }
    }

    /** One tick of smoke along the four sides of a wall, as far as they are near the viewer. */
    private static void muur(ClientLevel level, RandomSource r, Sluiers.Zone z, Vec3 oog, boolean lostOp) {
        double x0 = z.x0(), x1 = z.x1() + 1, z0 = z.z0(), z1 = z.z1() + 1;
        zijde(level, r, oog, x0, x1, z0, true, lostOp);
        zijde(level, r, oog, x0, x1, z1, true, lostOp);
        zijde(level, r, oog, z0, z1, x0, false, lostOp);
        zijde(level, r, oog, z0, z1, x1, false, lostOp);
    }

    /** A side from a to b along x (langsX) or z, at the fixed other coordinate vast. */
    private static void zijde(ClientLevel level, RandomSource r, Vec3 oog, double a, double b, double vast, boolean langsX, boolean lostOp) {
        double langs = langsX ? oog.x : oog.z, dwars = langsX ? oog.z : oog.x;
        if (Math.abs(vast - dwars) > ZICHT) {
            return;
        }
        double van = Math.max(a, langs - ZICHT), tot = Math.min(b, langs + ZICHT);
        if (tot <= van) {
            return;
        }
        double aantal = (tot - van) * DICHTHEID * (lostOp ? 2.5 : 1);
        int n = (int) aantal + (r.nextDouble() < aantal - (int) aantal ? 1 : 0);
        for (int i = 0; i < n; i++) {
            double s = van + r.nextDouble() * (tot - van);
            double y = oog.y - 6 + r.nextDouble() * 18;
            double x = langsX ? s : vast + (r.nextDouble() - 0.5) * 1.2, zz = langsX ? vast + (r.nextDouble() - 0.5) * 1.2 : s;
            // it swirls: along the wall, a little in and out, slowly up
            double draai = (r.nextDouble() - 0.5) * 0.06, wiebel = (r.nextDouble() - 0.5) * 0.015;
            double vx = langsX ? draai : wiebel, vz = langsX ? wiebel : draai;
            ParticleOptions p = lostOp ? (r.nextInt(3) == 0 ? ParticleTypes.CLOUD : ParticleTypes.POOF)
                    : r.nextInt(5) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
            level.addAlwaysVisibleParticle(p, true, x, y, zz, vx, lostOp ? 0.12 : 0.03 + r.nextDouble() * 0.03, vz);
        }
    }
}
