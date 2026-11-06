package nl.juiced.guhs.feature.vadsrijk.vfx;

import net.minecraft.world.phys.Vec3;

/**
 * A telegraph shape on the ground (roadmap 3.1, the bouwdoos): where it is drawn (as a decal, {@link Vfx#decal}) is exactly where
 * it hits ({@link #raakt}). All shapes are flat (horizontal); the height check (|dy| &lt; 3) is the caller's.
 * <ul>
 *   <li>{@link #cirkel}: everything within r of the anchor;</li>
 *   <li>{@link #kegel}: a cone from the anchor in the direction, {@code hoek} degrees wide, r long;</li>
 *   <li>{@link #lijn}: a strip from the anchor in the direction, {@code lengte} long, {@code breedte} wide;</li>
 *   <li>{@link #donut}: between r (safe inside) and r2;</li>
 *   <li>{@link #veilig}: EVERYTHING except within r (the pink "safe spot"; the boss's whole fight is the danger zone);</li>
 *   <li>{@link #kruis}: two strips through the anchor (direction and direction + 90 degrees), {@code lengte} each way.</li>
 * </ul>
 */
public record Vorm(Soort soort, double r, double r2, double hoek, double lengte, double breedte) {
    public enum Soort {
        CIRKEL, KEGEL, LIJN, DONUT, VEILIG, KRUIS
    }

    public static Vorm cirkel(double r) {
        return new Vorm(Soort.CIRKEL, r, 0, 0, 0, 0);
    }

    /** A cone {@code graden} wide (full angle) and r long. */
    public static Vorm kegel(double r, double graden) {
        return new Vorm(Soort.KEGEL, r, 0, graden, 0, 0);
    }

    public static Vorm lijn(double lengte, double breedte) {
        return new Vorm(Soort.LIJN, 0, 0, 0, lengte, breedte);
    }

    /** Safe within {@code binnen}, hit from there to {@code buiten}. */
    public static Vorm donut(double binnen, double buiten) {
        return new Vorm(Soort.DONUT, binnen, buiten, 0, 0, 0);
    }

    /** The safe spot: everyone OUTSIDE r is hit. */
    public static Vorm veilig(double r) {
        return new Vorm(Soort.VEILIG, r, 0, 0, 0, 0);
    }

    public static Vorm kruis(double lengte, double breedte) {
        return new Vorm(Soort.KRUIS, 0, 0, 0, lengte, breedte);
    }

    /** Is a point (feet) hit by this shape laid at {@code anker} facing {@code richting} (radians, atan2(dz, dx))? Horizontal only. */
    public boolean raakt(Vec3 anker, double richting, Vec3 p) {
        double dx = p.x - anker.x, dz = p.z - anker.z;
        double d2 = dx * dx + dz * dz;
        return switch (soort) {
            case CIRKEL -> d2 <= r * r;
            case DONUT -> d2 >= r * r && d2 <= r2 * r2;
            case VEILIG -> d2 > r * r;
            case KEGEL -> {
                if (d2 > r * r) {
                    yield false;
                }
                if (d2 < 0.36) {
                    yield true;   // standing right on the anchor (inside the boss) counts
                }
                double a = Math.atan2(dz, dx) - richting;
                a = Math.atan2(Math.sin(a), Math.cos(a));
                yield Math.abs(a) <= Math.toRadians(hoek) / 2;
            }
            case LIJN -> strip(dx, dz, richting, 0, lengte);
            case KRUIS -> strip(dx, dz, richting, -lengte, lengte) || strip(dx, dz, richting + Math.PI / 2, -lengte, lengte);
        };
    }

    private boolean strip(double dx, double dz, double richting, double van, double tot) {
        double ux = Math.cos(richting), uz = Math.sin(richting);
        double langs = dx * ux + dz * uz, dwars = -dx * uz + dz * ux;
        return langs >= van && langs <= tot && Math.abs(dwars) <= breedte / 2;
    }

    /** How far the shape reaches from the anchor (for the particle LOD and the client's decal culling). */
    public double bereik() {
        return switch (soort) {
            case CIRKEL, KEGEL, VEILIG -> r;
            case DONUT -> r2;
            case LIJN, KRUIS -> lengte;
        };
    }
}
