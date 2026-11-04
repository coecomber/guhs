package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;

/** Small shared helpers of the zone-3 creatures. */
final class Hulp {
    private Hulp() {
    }

    /** A floating line of text above a creature (a key under vadsrijk.z3wezens.). */
    static void zeg(ServerLevel sl, LivingEntity e, String key) {
        Getallen.tekst(sl, e.getX(), e.getY() + e.getBbHeight() + 0.5, e.getZ(), Component.translatable("vadsrijk.z3wezens." + key), 0.85f);
    }

    /** Pushes a player (sent to the client). */
    static void duw(LivingEntity p, Vec3 v) {
        p.setDeltaMovement(v);
        p.hurtMarked = true;
    }

    static void status(LivingEntity p, Status s, int ticks, double sterkte, LivingEntity bron) {
        Statussen.geef(p, s, ticks, sterkte, bron);
    }

    /** Is a point inside a flat strip from {@code van} along {@code richting} (normalised, flat), this long and wide? */
    static boolean inStrook(Vec3 van, Vec3 richting, double lengte, double breedte, Vec3 punt) {
        Vec3 d = punt.subtract(van).multiply(1, 0, 1);
        double langs = d.dot(richting);
        double dwars = Math.abs(d.x * -richting.z + d.z * richting.x);
        return langs > -0.5 && langs < lengte + 0.5 && dwars < breedte / 2 + 0.4 && Math.abs(punt.y - van.y) < 3;
    }

    /** The flat direction from a to b (or +z). */
    static Vec3 richting(Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a).multiply(1, 0, 1);
        return d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
    }

    static boolean speler(LivingEntity e) {
        return e instanceof Player p && !p.isCreative() && !p.isSpectator() && p.isAlive();
    }
}
