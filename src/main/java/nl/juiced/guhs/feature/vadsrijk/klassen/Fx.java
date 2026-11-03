package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;

/** Shared helpers of the klassen slice: particles in element colours, sounds, aiming, area queries. */
public final class Fx {
    public static final int ROZE = 0xFF7AC8, LICHTROZE = 0xFFC2E8, GOUD = 0xFFD24A;

    private Fx() {
    }

    public static int kleur(Element e) {
        return e == null || e == Element.GEEN ? LICHTROZE : e.rgb;
    }

    public static DustParticleOptions stof(int rgb, float grootte) {
        return new DustParticleOptions(rgb, grootte);
    }

    public static void deeltjes(ServerLevel sl, ParticleOptions p, Vec3 v, int n, double spreid, double snel) {
        sl.sendParticles(p, v.x, v.y, v.z, n, spreid, spreid, spreid, snel);
    }

    public static void geluid(ServerLevel sl, Vec3 v, SoundEvent s, float volume, float toon) {
        sl.playSound(null, BlockPos.containing(v), s, SoundSource.PLAYERS, volume, toon);
    }

    /** A dotted line of particles from a to b. */
    public static void lijn(ServerLevel sl, ParticleOptions p, Vec3 a, Vec3 b, double stap) {
        Vec3 d = b.subtract(a);
        double l = d.length();
        int n = (int) Math.max(1, l / stap);
        for (int i = 0; i <= n; i++) {
            Vec3 v = a.add(d.scale(i / (double) n));
            sl.sendParticles(p, v.x, v.y, v.z, 1, 0, 0, 0, 0);
        }
    }

    /** A ring of particles on the ground. */
    public static void ring(ServerLevel sl, ParticleOptions p, Vec3 c, double r, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            sl.sendParticles(p, c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 1, 0, 0.02, 0, 0);
        }
    }

    /** An element-coloured explosion puff. */
    public static void plof(ServerLevel sl, Vec3 c, double r, Element e) {
        int k = kleur(e);
        sl.sendParticles(stof(k, 2.2f), c.x, c.y + 0.3, c.z, (int) (18 * r), r * 0.5, 0.4, r * 0.5, 0.02);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y + 0.5, c.z, (int) (10 * r), r * 0.5, 0.5, r * 0.5, 0.05);
        sl.sendParticles(e == Element.VUUR ? ParticleTypes.FLAME : e == Element.WATER ? ParticleTypes.SPLASH : e == Element.NATUUR
                ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.END_ROD, c.x, c.y + 0.5, c.z, (int) (8 * r), r * 0.4, 0.4, r * 0.4, 0.04);
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.4, c.z, 1, 0, 0, 0, 0);
    }

    /** The living Vadsrijk mobs within r of c (not plofd). */
    public static List<LivingEntity> mobsBij(ServerLevel sl, Vec3 c, double r) {
        List<LivingEntity> l = new ArrayList<>();
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, r * 0.6 + 1.5, r),
                x -> x instanceof VadsrijkMob m && !m.geplofd())) {
            if (e.getBoundingBox().getCenter().multiply(1, 0, 1).distanceTo(c.multiply(1, 0, 1)) <= r + e.getBbWidth() * 0.5) {
                l.add(e);
            }
        }
        return l;
    }

    /** The players that count as "friends" of p within r of c: everyone (PvP is off in the dimension) who isn't a spectator. */
    public static List<ServerPlayer> vriendenBij(ServerPlayer p, Vec3 c, double r) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer s : ((ServerLevel) p.level()).players()) {
            if (!s.isSpectator() && s.position().distanceTo(c) <= r) {
                l.add(s);
            }
        }
        return l;
    }

    /** p's party within r (always includes p). */
    public static List<ServerPlayer> party(ServerPlayer p, double r) {
        return Partijen.ledenInBuurt(p, r);
    }

    /** Where p aims: the mob under the crosshair, else the block looked at (max), else that far ahead on the ground. */
    public static Vec3 mikpunt(ServerPlayer p, LivingEntity doel, double max) {
        if (doel != null) {
            return grond((ServerLevel) p.level(), doel.position());
        }
        Vec3 oog = p.getEyePosition();
        Vec3 eind = oog.add(p.getLookAngle().scale(max));
        BlockHitResult r = p.level().clip(new ClipContext(oog, eind, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (r.getType() != HitResult.Type.MISS) {
            return grond((ServerLevel) p.level(), r.getLocation());
        }
        return grond((ServerLevel) p.level(), p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(Math.min(max, 10))));
    }

    /** The ground under (or just above) v. */
    public static Vec3 grond(ServerLevel sl, Vec3 v) {
        BlockPos b = BlockPos.containing(v.x, v.y + 1.5, v.z);
        for (int i = 0; i < 24 && b.getY() > sl.getMinY(); i++) {
            if (!sl.getBlockState(b.below()).getCollisionShape(sl, b.below()).isEmpty()) {
                return new Vec3(v.x, b.getY(), v.z);
            }
            b = b.below();
        }
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(v.x), (int) Math.floor(v.z));
        return new Vec3(v.x, Math.min(v.y, y), v.z);
    }

    /** Pushes the player (server-side motion that the client really gets). */
    public static void duw(ServerPlayer p, Vec3 v) {
        p.setDeltaMovement(v);
        p.hurtMarked = true;
        p.connection.send(new ClientboundSetEntityMotionPacket(p));
    }
}
