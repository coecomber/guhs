package nl.juiced.guhs.entity;

import net.minecraft.world.entity.Entity;

/**
 * 1.1.1: one central check before anything starts riding anything (called from {@code mixin/EntityRidingMixin} at the top of
 * every {@code Entity#startRiding}, also the forced ones). A rider may never get on itself, nor on something that already
 * (indirectly) rides it: that would make a riding loop, and every loop over the passengers (the server's entity tracker
 * walks them for every player move) would never end. Vanilla 26.1 checks the vehicles above the target, but not
 * {@code rider == vehicle}. The walks here are bounded, so even a broken chain can't hang the check itself.
 */
public final class RijGuard {
    /** Deeper than any real stack of riders (a sled, a guh on a kart, ...). */
    static final int MAX_DIEPTE = 64;

    /** True when rider may get on vehicle without making a loop. */
    public static boolean mag(Entity rider, Entity vehicle) {
        if (rider == null || vehicle == null || rider == vehicle) {
            return false;
        }
        // the vehicle's own chain upwards must not reach the rider (vehicle.getRootVehicle() == rider, bounded)
        Entity e = vehicle;
        for (int i = 0; i < MAX_DIEPTE; i++) {
            Entity boven = e.getVehicle();
            if (boven == null) {
                break;
            }
            if (boven == rider) {
                return false;
            }
            e = boven;
            if (i == MAX_DIEPTE - 1) {
                return false;   // (a chain this long is already broken: don't make it worse)
            }
        }
        // and the vehicle must not already be one of the rider's (indirect) passengers (rider.hasIndirectPassenger(vehicle))
        return !heeftPassagier(rider, vehicle, 0);
    }

    private static boolean heeftPassagier(Entity e, Entity zoek, int diepte) {
        if (diepte >= MAX_DIEPTE) {
            return true;
        }
        for (Entity p : e.getPassengers()) {
            if (p == zoek || p == e || heeftPassagier(p, zoek, diepte + 1)) {
                return true;
            }
        }
        return false;
    }

    /** True when e is part of a riding loop (or a chain deeper than any real one): for tests and checks. */
    public static boolean inLus(Entity e) {
        Entity x = e;
        for (int i = 0; i < MAX_DIEPTE; i++) {
            x = x.getVehicle();
            if (x == null) {
                return heeftPassagier(e, e, 0);
            }
            if (x == e) {
                return true;
            }
        }
        return true;
    }

    private RijGuard() {
    }
}
