package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers;

/**
 * The eight 7x7 plots of the Surprisestraatje (plein's anchors {@code bouwveld_<i>} = min corner, 7 high,
 * {@code bouwveld_poort_<i>} = the gate in the little fence, {@code bouwveld_bord_<i>} = the name sign on the street side).
 */
public final class Bouwvelden {
    public static final int AANTAL = 8, MAAT = Inhoud.N;

    /** One plot: its room (min corner, 7x7x7), where you stand when you're put out (on the street), the sign. */
    public record Veld(int i, BlockPos min, Vec3 uitgang, float uitgangYaw, @Nullable BlockPos bord, Rotation naarVoorkant) {
        /** Is this block inside the 7x7x7 room? */
        public boolean bevat(BlockPos p) {
            return p.getX() >= min.getX() && p.getX() < min.getX() + MAAT && p.getY() >= min.getY() && p.getY() < min.getY() + MAAT
                    && p.getZ() >= min.getZ() && p.getZ() < min.getZ() + MAAT;
        }

        /** Are these feet inside the room (x/z in the 7x7, from the floor to the top)? */
        public boolean bevatVoeten(Vec3 v) {
            return v.x >= min.getX() && v.x < min.getX() + MAAT && v.z >= min.getZ() && v.z < min.getZ() + MAAT
                    && v.y >= min.getY() - 0.5 && v.y < min.getY() + MAAT + 1;
        }

        public AABB ruimte() {
            return new AABB(min.getX(), min.getY(), min.getZ(), min.getX() + MAAT, min.getY() + MAAT, min.getZ() + MAAT);
        }

        public Vec3 midden() {
            return new Vec3(min.getX() + MAAT / 2.0, min.getY(), min.getZ() + MAAT / 2.0);
        }
    }

    private static List<Veld> cache;

    private Bouwvelden() {
    }

    /** The plots that have their anchor (normally all eight), in order. */
    public static List<Veld> alle() {
        if (cache == null) {
            List<Veld> l = new ArrayList<>();
            for (int i = 0; i < AANTAL; i++) {
                GuhwinterAnkers.Anker a = GuhwinterAnkers.zoek("bouwveld_" + i);
                if (a == null) {
                    continue;
                }
                BlockPos min = a.pos();
                Vec3 mid = new Vec3(min.getX() + MAAT / 2.0, min.getY(), min.getZ() + MAAT / 2.0);
                GuhwinterAnkers.Anker poort = GuhwinterAnkers.zoek("bouwveld_poort_" + i);
                GuhwinterAnkers.Anker bord = GuhwinterAnkers.zoek("bouwveld_bord_" + i);
                Vec3 uit;
                float yaw;
                Rotation voor;
                if (poort != null) {
                    // the gate looks into the plot: one and a half step back from it is the street
                    Vec3 kijk = Vec3.directionFromRotation(0, poort.yaw());
                    uit = poort.vec().subtract(kijk.scale(1.5));
                    yaw = poort.yaw();
                    voor = naarZuid(poort.x() - mid.x, poort.z() - mid.z);
                } else {
                    uit = new Vec3(mid.x, min.getY(), min.getZ() - 1.5);
                    yaw = 0;
                    voor = Rotation.CLOCKWISE_180;
                }
                l.add(new Veld(i, min, uit, yaw, bord == null ? null : bord.pos(), voor));
            }
            cache = List.copyOf(l);
        }
        return cache;
    }

    /** The turn that brings the side (dx, dz) from the middle (the gate's side, the plot's "front") to the south. */
    static Rotation naarZuid(double dx, double dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;   // east -> south / west -> south
        }
        return dz > 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
    }

    @Nullable
    public static Veld veld(int i) {
        for (Veld v : alle()) {
            if (v.i() == i) {
                return v;
            }
        }
        return null;
    }

    /** The plot whose room holds this block, or null. */
    @Nullable
    public static Veld op(BlockPos p) {
        for (Veld v : alle()) {
            if (v.bevat(p)) {
                return v;
            }
        }
        return null;
    }

    /** The plot whose room these feet are in, or null. */
    @Nullable
    public static Veld voeten(Vec3 p) {
        for (Veld v : alle()) {
            if (v.bevatVoeten(p)) {
                return v;
            }
        }
        return null;
    }

    /** The plot of this sign position, or null. */
    @Nullable
    public static Veld vanBord(BlockPos p) {
        for (Veld v : alle()) {
            if (p.equals(v.bord())) {
                return v;
            }
        }
        return null;
    }

    /** The whole street around the plots (to look for players cheaply), or null when there are no anchors. */
    @Nullable
    public static AABB straat() {
        AABB s = null;
        for (Veld v : alle()) {
            s = s == null ? v.ruimte() : s.minmax(v.ruimte());
        }
        return s == null ? null : s.inflate(4, 3, 4);
    }

    /** (after /reload or new anchors) read them again. */
    static void vergeet() {
        cache = null;
    }
}
