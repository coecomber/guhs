package nl.juiced.guhs.feature.guhpixel.reisbureau;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * The guh visibly leaves: for {@value #DUUR} ticks it walks away from the balie with its little suitcase (the synced flag
 * {@link PxVlaggen#KOFFER}, drawn by client.ReisbureauClient), then it is stored ({@link Reizen#opslaan}).
 * <p>
 * The countdown lives in the guh's own persistent data ({@value #TICKS}), so it survives a chunk unload or a restart in
 * the middle of the walk: the guh simply finishes leaving when it is loaded again, or, when its owner's trip was dropped
 * meanwhile, puts its suitcase down and stays home.
 */
public final class Uitzwaaien {
    public static final int DUUR = 60;
    public static final String TICKS = "guhs_px_reisbureau_vertrek", DOEL_X = "guhs_px_reisbureau_doel_x", DOEL_Y = "guhs_px_reisbureau_doel_y",
            DOEL_Z = "guhs_px_reisbureau_doel_z";
    private static final String NS = "reisbureau";

    /** Starts the walk: away from the front of the balie (or away from the player). */
    static void begin(GuhEntity guh, ServerPlayer p, @Nullable BlockPos balie) {
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
        GuhKiezer.claim(guh, NS);
        GuhHooks.zet(guh, PxVlaggen.KOFFER, true);
        Vec3 weg;
        BlockState s = balie == null ? null : guh.level().getBlockState(balie);
        if (s != null && s.getBlock() instanceof BalieBlock) {
            Direction voor = s.getValue(BalieBlock.FACING);
            weg = new Vec3(voor.getStepX(), 0, voor.getStepZ());
        } else {
            weg = guh.position().subtract(p.position()).multiply(1, 0, 1);
            weg = weg.lengthSqr() < 0.01 ? Vec3.directionFromRotation(0, p.getYRot()) : weg.normalize();
        }
        Vec3 doel = guh.position().add(weg.scale(6));
        CompoundTag pd = guh.getPersistentData();
        pd.putInt(TICKS, DUUR);
        pd.putDouble(DOEL_X, doel.x);
        pd.putDouble(DOEL_Y, doel.y);
        pd.putDouble(DOEL_Z, doel.z);
        guh.triggerAnim("action", "happy");
    }

    public static boolean vertrekt(GuhEntity guh) {
        return guh.getPersistentData().contains(TICKS);
    }

    /** (GuhHooks.tick) the walk; at the end the guh is stored. */
    static void tick(GuhEntity guh) {
        CompoundTag pd = guh.getPersistentData();
        if (!pd.contains(TICKS) || guh.level().isClientSide()) {
            return;
        }
        GuhHooks.bezig(guh, 40);
        int t = pd.getIntOr(TICKS, 0) - 1;
        if (t <= 0) {
            Reizen.opslaan(guh);   // (it clears the marks, also when there is no trip for this guh any more)
            return;
        }
        pd.putInt(TICKS, t);
        if (t % 10 == 9) {
            guh.getNavigation().moveTo(pd.getDoubleOr(DOEL_X, guh.getX()), pd.getDoubleOr(DOEL_Y, guh.getY()), pd.getDoubleOr(DOEL_Z, guh.getZ()), 1.0);
        }
    }

    /** The suitcase goes down: the marks, the flag and the claim are removed (the guh itself stays as it is). */
    static void wis(GuhEntity guh) {
        CompoundTag pd = guh.getPersistentData();
        boolean had = pd.contains(TICKS);
        pd.remove(TICKS);
        pd.remove(DOEL_X);
        pd.remove(DOEL_Y);
        pd.remove(DOEL_Z);
        if (GuhHooks.heeft(guh, PxVlaggen.KOFFER)) {
            GuhHooks.zet(guh, PxVlaggen.KOFFER, false);
        }
        if (NS.equals(GuhKiezer.geclaimd(guh))) {
            GuhKiezer.los(guh);
        }
        if (had) {
            guh.getNavigation().stop();
        }
    }

    private Uitzwaaien() {
    }
}
