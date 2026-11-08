package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * U-LEVEN-STAD: one little scene of town life (a scurrying mouse, a guh walking its pet, a terrace of chatting guhs, the
 * playground...). It owns its entities, ticks them, and is taken away as a whole when nobody is near any more (or its time is
 * up). Nothing of it is saved.
 */
abstract class Groep {
    /** The kinds, with their cap per town at VOL (ZACHT: half, rounded up) and the radius (LOD) they live in round a player. */
    enum Soort {
        DIERTJE(9, 34), WANDELAAR(2, 48), KAT(3, 44), EENDJES(1, 44), VOGEL(4, 36), MUZIKANT(1, 48), KAR(1, 56), PICKNICK(1, 48),
        TIKKERTJE(1, 48), TERRAS(2, 44), SPEELTUIN(1, 64);

        final int max;
        final double zicht;

        Soort(int max, double zicht) {
            this.max = max;
            this.zicht = zicht;
        }

        int max(StadsLeven.Niveau n) {
            return n == StadsLeven.Niveau.VOL ? max : n == StadsLeven.Niveau.ZACHT ? (max + 1) / 2 : 0;
        }
    }

    final String sleutel;
    final Soort soort;
    final Steden.Stad stad;
    final List<Entity> leden = new ArrayList<>();
    int t;
    /** Ticks left (-1 = until nobody is near). */
    int duur = -1;
    /** The next tick a bubble may appear (throttle per group). */
    int volgendeBubbel = 60;

    Groep(String sleutel, Soort soort, Steden.Stad stad) {
        this.sleutel = sleutel;
        this.soort = soort;
        this.stad = stad;
    }

    /** Where the scene is (for the LOD). */
    abstract Vec3 midden();

    /** One tick; false = done (taken away). */
    abstract boolean tick(ServerLevel sl);

    /** Every entity still alive in this level? */
    boolean heel(ServerLevel sl) {
        for (Entity e : leden) {
            if (e.isRemoved() || e.level() != sl) {
                return false;
            }
        }
        return !leden.isEmpty();
    }

    int guhs() {
        int n = 0;
        for (Entity e : leden) {
            if (e instanceof LevenGuh) {
                n++;
            }
        }
        return n;
    }

    void weg() {
        for (Entity e : leden) {
            StadsLeven.EIGEN.remove(e.getUUID());
            if (!e.isRemoved()) {
                e.discard();
            }
        }
        leden.clear();
    }

    <E extends Entity> E lid(@Nullable E e) {
        if (e != null) {
            leden.add(e);
            if (e instanceof LevenGuh g) {
                g.groep = this;
            }
        }
        return e;
    }

    /** A line for a click on one of its guhs (null = the general cosy line). */
    @Nullable
    String klikRegel(LevenGuh g) {
        return null;
    }

    // ---- kinematic walking on the real ground -------------------------------------------------------------------------------

    /**
     * Walks an entity over the ground towards a target, never through walls or water: x/z straight on, y follows the ground
     * (smoothed, a ramp over half steps and stairs). Stuck (a wall, a drop) = arrived.
     */
    static final class Loper {
        Vec3 pos;
        @Nullable
        Vec3 doel;
        double snelheid;
        double huppel;   // hop height (blobs, mice dashes), 0 = walking
        private double y;
        private double fase;

        Loper(Vec3 pos, double snelheid) {
            this.pos = pos;
            this.y = pos.y;
            this.snelheid = snelheid;
        }

        boolean beweegt() {
            return doel != null;
        }

        void naar(Vec3 d) {
            this.doel = d;
        }

        /** One step; returns the position for the entity (with the hop added). */
        Vec3 stap(ServerLevel sl) {
            if (doel == null) {
                return pos.add(0, 0, 0);
            }
            double dx = doel.x - pos.x, dz = doel.z - pos.z, d = Math.sqrt(dx * dx + dz * dz);
            if (d <= snelheid) {
                pos = new Vec3(doel.x, pos.y, doel.z);
                doel = null;
            } else {
                double nx = pos.x + dx / d * snelheid, nz = pos.z + dz / d * snelheid;
                double g = Plekken.grondY(sl, nx, y, nz);
                if (Double.isNaN(g) || Math.abs(g - y) > 1.05) {
                    doel = null;   // a wall, water or a drop: stop here
                } else {
                    y = g;
                    pos = new Vec3(nx, pos.y, nz);
                }
            }
            double py = pos.y + Mth.clamp(y - pos.y, -0.25, 0.25);
            pos = new Vec3(pos.x, py, pos.z);
            double hop = 0;
            if (huppel > 0 && doel != null) {
                fase += snelheid * 2.2;
                hop = Math.abs(Math.sin(fase)) * huppel;
            }
            return new Vec3(pos.x, pos.y + hop, pos.z);
        }
    }
}
