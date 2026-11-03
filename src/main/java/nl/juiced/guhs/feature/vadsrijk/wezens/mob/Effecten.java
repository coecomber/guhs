package nl.juiced.guhs.feature.vadsrijk.wezens.mob;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Light-weight server-side "projectiles" and lingering clouds of the wezens (no extra entity types to register or render):
 * a {@link Kogel} flies in a straight line drawing particles and hits the first player it touches (or a wall), a
 * {@link Wolk} hangs in the air for a while and does something to the players inside every second. Ticked every server tick.
 */
public final class Effecten {
    /** A flying blob: on a hit {@code raak} gets the player. */
    public static final class Kogel {
        final ServerLevel level;
        Vec3 pos;
        final Vec3 snelheid;
        int leven;
        final ParticleOptions spoor;
        final Consumer<Player> raak;

        Kogel(ServerLevel level, Vec3 pos, Vec3 snelheid, int leven, ParticleOptions spoor, Consumer<Player> raak) {
            this.level = level;
            this.pos = pos;
            this.snelheid = snelheid;
            this.leven = leven;
            this.spoor = spoor;
            this.raak = raak;
        }
    }

    /** A cloud: {@code binnen} every 20 ticks for every player inside. */
    public static final class Wolk {
        final ServerLevel level;
        final Vec3 midden;
        final double straal;
        int leven;
        final ParticleOptions deeltje;
        final Consumer<Player> binnen;

        Wolk(ServerLevel level, Vec3 midden, double straal, int leven, ParticleOptions deeltje, Consumer<Player> binnen) {
            this.level = level;
            this.midden = midden;
            this.straal = straal;
            this.leven = leven;
            this.deeltje = deeltje;
            this.binnen = binnen;
        }
    }

    private static final List<Kogel> KOGELS = new ArrayList<>();
    private static final List<Wolk> WOLKEN = new ArrayList<>();

    private Effecten() {
    }

    /** Shoots a blob from {@code van} at {@code naar} with this speed (blocks per tick); it flies at most 40 ticks. */
    public static void schiet(ServerLevel level, Vec3 van, Vec3 naar, double snelheid, ParticleOptions spoor, Consumer<Player> raak) {
        Vec3 v = naar.subtract(van);
        if (v.lengthSqr() < 1.0E-4) {
            return;
        }
        KOGELS.add(new Kogel(level, van, v.normalize().scale(snelheid), 40, spoor, raak));
    }

    public static void wolk(ServerLevel level, Vec3 midden, double straal, int ticks, ParticleOptions deeltje, Consumer<Player> binnen) {
        WOLKEN.add(new Wolk(level, midden, straal, ticks, deeltje, binnen));
    }

    public static void tick() {
        Iterator<Kogel> it = KOGELS.iterator();
        while (it.hasNext()) {
            Kogel k = it.next();
            if (--k.leven < 0 || !k.level.isLoaded(BlockPos.containing(k.pos))) {
                it.remove();
                continue;
            }
            Vec3 volgende = k.pos.add(k.snelheid);
            k.level.sendParticles(k.spoor, k.pos.x, k.pos.y, k.pos.z, 2, 0.06, 0.06, 0.06, 0);
            Player p = geraakt(k.level, k.pos, volgende);
            if (p != null) {
                k.raak.accept(p);
                k.level.sendParticles(k.spoor, p.getX(), p.getY() + 1, p.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                it.remove();
                continue;
            }
            if (!k.level.getBlockState(BlockPos.containing(volgende)).getCollisionShape(k.level, BlockPos.containing(volgende)).isEmpty()) {
                k.level.sendParticles(k.spoor, k.pos.x, k.pos.y, k.pos.z, 8, 0.2, 0.2, 0.2, 0.02);
                it.remove();
                continue;
            }
            k.pos = volgende;
        }
        Iterator<Wolk> wi = WOLKEN.iterator();
        while (wi.hasNext()) {
            Wolk w = wi.next();
            if (--w.leven < 0) {
                wi.remove();
                continue;
            }
            if (w.leven % 4 == 0) {
                w.level.sendParticles(w.deeltje, w.midden.x, w.midden.y + 0.6, w.midden.z, 10, w.straal * 0.5, 0.5, w.straal * 0.5, 0);
            }
            if (w.leven % 20 == 0) {
                for (Player p : w.level.getEntitiesOfClass(Player.class, new AABB(w.midden, w.midden).inflate(w.straal, 2, w.straal),
                        p -> !p.isCreative() && !p.isSpectator() && p.position().distanceTo(w.midden) <= w.straal + 0.5)) {
                    w.binnen.accept(p);
                }
            }
        }
    }

    @Nullable
    private static Player geraakt(ServerLevel level, Vec3 a, Vec3 b) {
        AABB box = new AABB(a, b).inflate(0.4);
        for (Player p : level.getEntitiesOfClass(Player.class, box, p -> !p.isCreative() && !p.isSpectator() && p.isAlive())) {
            if (p.getBoundingBox().inflate(0.3).clip(a, b).isPresent() || p.getBoundingBox().inflate(0.3).contains(a)) {
                return p;
            }
        }
        return null;
    }

    /** Dev/test: forget everything in flight (gametests). */
    public static void wis() {
        KOGELS.clear();
        WOLKEN.clear();
    }

    public static int aantal() {
        return KOGELS.size() + WOLKEN.size();
    }
}
