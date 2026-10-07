package nl.juiced.guhs.feature.einde2.biomeguhs;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.feature.verhaal.VariantGedrag;

/**
 * einde2, slice biomeguhs: the Korstguh rolls up into a ball ({@link E2Vlaggen#TRUC}).
 * <ul>
 *   <li>A tamed one rolls up by itself when its owner sneaks next to it (within {@link #BUK_AFSTAND} blocks), and when it
 *       naps in a kaasgat ({@link KaasgatDutje}). It stays rolled up for {@link #ROL_TICKS} ticks after the last reason.</li>
 *   <li>The ball is pushed by walking into it and by a left-click (a tik: no damage, {@link #onTik}). It rolls on with
 *       little friction, bounces off walls, goes up a step, and stops at the edge of the void (a guh never rolls off its
 *       island). Rolling is its own movement ({@link #travel}).</li>
 *   <li>The client draws the ball from the flag and turns it by the distance it rolled ({@link #rolHoek}).</li>
 * </ul>
 */
public final class Korstbal implements VariantGedrag {
    /** Persistent keys: the game time until it stays rolled up; the last time the "tik" sound played. */
    public static final String ROL_TOT = "guhs_einde2_biomeguhs_rol_tot", TIK = "guhs_einde2_biomeguhs_tik";
    /** How long it stays rolled up after its owner stopped sneaking / after the last push. */
    public static final int ROL_TICKS = 100, DUW_TICKS = 80;
    /** Its owner sneaks within this many blocks: it rolls up. */
    public static final double BUK_AFSTAND = 3.0;
    /** The fastest a ball rolls (blocks per tick), the push of a left-click, how much speed is left after a tick on the ground. */
    public static final double MAX_SNELHEID = 0.7, TIK_KRACHT = 0.55, WRIJVING = 0.965;

    /** Client: per Korstguh {angle, angle of the tick before, last x, last z} (radians; see {@link #rolHoek}). */
    private static final Map<GuhEntity, float[]> ROL = Collections.synchronizedMap(new WeakHashMap<>());

    public static boolean opgerold(GuhEntity guh) {
        return guh.getVariant() == GuhVariant.KORSTGUH && Trucs.doet(guh);
    }

    /** Rolls up (when it wasn't yet) and stays so for at least this many ticks. */
    public static void rolOp(GuhEntity guh, int ticks) {
        long tot = guh.level().getGameTime() + ticks;
        if (tot > guh.getPersistentData().getLongOr(ROL_TOT, 0L)) {
            guh.getPersistentData().putLong(ROL_TOT, tot);
        }
        if (!Trucs.doet(guh)) {
            Trucs.zet(guh, true);
            guh.getNavigation().stop();
            Trucs.geluid(guh, E2BiomeguhsFeature.KORSTGUH_ROL.get(), 0.9f, guh.getVoicePitch());
        }
    }

    public static void rolUit(GuhEntity guh) {
        guh.getPersistentData().remove(ROL_TOT);
        if (Trucs.doet(guh)) {
            Trucs.zet(guh, false);
            GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, false);
            Trucs.geluid(guh, E2BiomeguhsFeature.KORSTGUH_UITROL.get(), 0.9f, guh.getVoicePitch());
        }
    }

    /**
     * Pushes a rolled-up Korstguh this way (a horizontal direction; kracht in blocks per tick, added to what it has).
     * Returns whether it was pushed.
     */
    public static boolean duw(GuhEntity guh, Vec3 richting, double kracht, @Nullable ServerPlayer door) {
        if (!opgerold(guh) || richting.horizontalDistanceSqr() < 1.0e-6) {
            return false;
        }
        Vec3 r = new Vec3(richting.x, 0, richting.z).normalize();
        Vec3 v = guh.getDeltaMovement().add(r.x * kracht, 0, r.z * kracht);
        double h = v.horizontalDistance();
        if (h > MAX_SNELHEID) {
            v = new Vec3(v.x * MAX_SNELHEID / h, v.y, v.z * MAX_SNELHEID / h);
        }
        guh.setDeltaMovement(v);
        guh.hurtMarked = true;   // (tell the clients its new speed)
        long nu = guh.level().getGameTime();
        guh.getPersistentData().putLong(ROL_TOT, Math.max(guh.getPersistentData().getLongOr(ROL_TOT, 0L), nu + DUW_TICKS));
        if (nu - guh.getPersistentData().getLongOr(TIK, 0L) >= 8) {
            guh.getPersistentData().putLong(TIK, nu);
            Trucs.geluid(guh, E2BiomeguhsFeature.KORSTGUH_TIK.get(), 0.8f, 0.9f + guh.getRandom().nextFloat() * 0.3f);
        }
        if (door != null && guh.isOwnedBy(door)) {
            if (GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT)) {
                GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, false);   // (that wakes it up)
            }
            GidsFeature.grant(door, "guheinde/einde2_biomeguhs_bal");
            Trucs.meld(door, guh, "rolt");
        }
        return true;
    }

    /** (GuhHooks.tick, server) */
    static void serverTick(GuhEntity guh) {
        boolean rol = Trucs.doet(guh);
        if (!Trucs.tam(guh) || guh.isVehicle() || guh.isPassenger() || guh.isLeashed()) {
            if (rol) {
                rolUit(guh);
            }
            return;
        }
        if ((guh.tickCount + guh.getId()) % 5 == 0) {
            ServerPlayer baas = Trucs.baasBij(guh, BUK_AFSTAND);
            if (baas != null && baas.isShiftKeyDown()) {
                rolOp(guh, ROL_TICKS);
            }
        }
        if (!Trucs.doet(guh)) {
            return;
        }
        guh.getNavigation().stop();
        // whoever walks into the ball pushes it along
        for (Player p : guh.level().getEntitiesOfClass(Player.class, guh.getBoundingBox().inflate(0.3, 0, 0.3), p -> !p.isSpectator())) {
            Vec3 weg = new Vec3(guh.getX() - p.getX(), 0, guh.getZ() - p.getZ());
            Vec3 loop = p.getKnownMovement();
            double snel = loop.horizontalDistance();
            if (snel > 0.05 && weg.lengthSqr() > 1.0e-4 && loop.x * weg.x + loop.z * weg.z > 0) {
                duw(guh, weg, Math.min(0.4, 0.1 + snel * 1.2), p instanceof ServerPlayer sp ? sp : null);
            }
        }
        if (guh.level().getGameTime() > guh.getPersistentData().getLongOr(ROL_TOT, 0L) && guh.onGround()
                && guh.getDeltaMovement().horizontalDistanceSqr() < 0.0009) {
            rolUit(guh);
        }
    }

    /** A left-click on a rolled-up Korstguh is a tik: it rolls away, nobody gets hurt. Both sides. */
    static void onTik(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof GuhEntity guh) || !opgerold(guh)) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer p) {
            duw(guh, p.getLookAngle(), TIK_KRACHT, p);
        }
    }

    /** The ball's own movement: it keeps its speed, bounces, and never leaves its island. */
    @Override
    public boolean travel(GuhEntity guh, Vec3 input) {
        if (!opgerold(guh) || guh.isPassenger()) {
            return false;
        }
        Vec3 v = guh.getDeltaMovement();
        if (!guh.isNoGravity()) {
            v = v.add(0, -guh.getGravity(), 0);
        }
        if (guh.onGround() && v.horizontalDistanceSqr() > 1.0e-4 && randVanLeegte(guh, v)) {
            v = new Vec3(0, v.y, 0);
        }
        Vec3 voor = guh.position();
        guh.move(MoverType.SELF, v);
        Vec3 na = guh.position();
        double vx = v.x, vy = v.y, vz = v.z;
        if (guh.horizontalCollision) {   // a wall: it bounces back, a little slower
            if (Math.abs(na.x - voor.x - v.x) > 1.0e-4) {
                vx = -vx * 0.45;
            }
            if (Math.abs(na.z - voor.z - v.z) > 1.0e-4) {
                vz = -vz * 0.45;
            }
        }
        double wrijving = guh.isInWater() ? 0.8 : guh.onGround() ? WRIJVING : 0.99;
        vx *= wrijving;
        vz *= wrijving;
        vy = guh.onGround() ? (vy < -0.3 ? -vy * 0.3 : 0) : vy * 0.98;
        if (vx * vx + vz * vz < 1.0e-5) {
            vx = 0;
            vz = 0;
        } else if (vx * vx + vz * vz > 4.0e-4) {
            float yaw = (float) (Mth.atan2(vz, vx) * Mth.RAD_TO_DEG) - 90f;   // it faces the way it rolls
            guh.setYRot(yaw);
            guh.yBodyRot = yaw;
            guh.yHeadRot = yaw;
        }
        guh.setDeltaMovement(vx, vy, vz);
        guh.calculateEntityAnimation(false);
        return true;
    }

    /** Would the ball roll off into the void here (no ground within 8 blocks under the spot just ahead)? */
    private static boolean randVanLeegte(GuhEntity guh, Vec3 v) {
        double h = v.horizontalDistance();
        Level level = guh.level();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x = Mth.floor(guh.getX() + v.x / h * 0.9), z = Mth.floor(guh.getZ() + v.z / h * 0.9), y = Mth.floor(guh.getY());
        for (int dy = 0; dy <= 8; dy++) {
            p.set(x, y - dy, z);
            if (p.getY() < level.getMinY()) {
                return true;
            }
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Client: count how far the ball rolled (the angle the renderer turns it by). */
    @Override
    public void tick(GuhEntity guh) {
        if (!guh.level().isClientSide()) {
            return;
        }
        float[] r = ROL.computeIfAbsent(guh, g -> new float[] {0f, 0f, (float) g.getX(), (float) g.getZ()});
        r[1] = r[0];
        if (Trucs.doet(guh)) {
            double dx = guh.getX() - r[2], dz = guh.getZ() - r[3];
            r[0] += (float) (Math.sqrt(dx * dx + dz * dz) / (0.42 * Math.max(0.3f, guh.getScale() * guh.getAgeScale())));
        } else {
            r[0] = 0f;
            r[1] = 0f;
        }
        r[2] = (float) guh.getX();
        r[3] = (float) guh.getZ();
    }

    /** Client: how far this ball has turned (radians). */
    public static float rolHoek(GuhEntity guh, float partialTick) {
        float[] r = ROL.get(guh);
        return r == null ? 0f : Mth.lerp(partialTick, r[1], r[0]);
    }
}
