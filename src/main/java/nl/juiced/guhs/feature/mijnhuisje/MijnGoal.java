package nl.juiced.guhs.feature.mijnhuisje;

import java.util.EnumSet;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.Huisjes;

/**
 * A resident of a Mijn-guhhuisje walks to where the work is (priority 3, MOVE + LOOK; added to every guh through
 * GuhHooks.doelen): to the block mined last ({@link MijnhuisjeBlockEntity#werkplek}), looking at it. When it can't get there
 * on foot (down the ladder shaft, a deep pit) it hops there after a few tries, if there is room to stand. Sitting,
 * riding or leashed guhs and babies stay where they are. A guh whose Mijn-guhhuisje is gone (or that moved into a
 * Guhhuisje) is set free here.
 */
public class MijnGoal extends Goal {
    private final GuhEntity guh;
    @Nullable
    private MijnhuisjeBlockEntity thuis;
    private long thuisTot;
    private int vast, tik;
    @Nullable
    private BlockPos doel;

    public MijnGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Nullable
    private MijnhuisjeBlockEntity thuis() {
        long nu = guh.level().getGameTime();
        if (nu >= thuisTot) {
            thuisTot = nu + 40;
            thuis = MijnhuisjeBlockEntity.thuisVan(guh);
            if (thuis == null && (MijnhuisjeBlockEntity.thuisWeg(guh) || MijnhuisjeBlockEntity.teVer(guh) || Huisjes.isBewoner(guh))) {
                MijnhuisjeBlockEntity.ontruim(guh);            // (its house is gone, or it was carried far away / to another world)
            } else if (thuis != null && thuis.status() != MijnhuisjeBlockEntity.BEZIG && guh.getY() < thuis.getBlockPos().getY() - 4
                    && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isLeashed() && guh.getNavigation().isDone()) {
                BlockPos deur = staanBij(guh.level(), thuis.getBlockPos().relative(thuis.voor()));   // (work done or paused: up out of the pit)
                if (deur != null) {
                    guh.snapTo(deur.getX() + 0.5, deur.getY(), deur.getZ() + 0.5, guh.getYRot(), guh.getXRot());
                    guh.fallDistance = 0;
                }
            }
        }
        return thuis;
    }

    private boolean mag() {
        if (guh.level().isClientSide() || !MijnhuisjeBlockEntity.isBewoner(guh) || guh.isOrderedToSit() || guh.isPassenger() || guh.isVehicle()
                || guh.isLeashed() || guh.isBaby()) {
            return false;
        }
        MijnhuisjeBlockEntity be = thuis();
        return be != null && be.status() == MijnhuisjeBlockEntity.BEZIG && be.werkplek() != null
                && guh.distanceToSqr(be.getBlockPos().getCenter()) <= (double) MijnhuisjeBlockEntity.BEREIK * MijnhuisjeBlockEntity.BEREIK;
    }

    @Override
    public boolean canUse() {
        return (guh.tickCount + guh.getId()) % 20 < 2 && mag();
    }

    @Override
    public boolean canContinueToUse() {
        return mag();
    }

    @Override
    public void start() {
        vast = 0;
        tik = 0;
        doel = null;
    }

    @Override
    public void stop() {
        guh.getNavigation().stop();
        doel = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return false;
    }

    @Override
    public void tick() {
        MijnhuisjeBlockEntity be = thuis;
        BlockPos plek = be == null ? null : be.werkplek();
        if (plek == null) {
            return;
        }
        guh.getLookControl().setLookAt(plek.getX() + 0.5, plek.getY() + 0.5, plek.getZ() + 0.5);
        if (++tik % 10 != 0) {
            return;
        }
        double d = guh.distanceToSqr(plek.getCenter());
        if (d <= 6.25) {
            vast = 0;
            guh.getNavigation().stop();
            return;
        }
        if (!plek.equals(doel) || guh.getNavigation().isDone()) {
            doel = plek;
            boolean pad = guh.getNavigation().moveTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 1.0);
            vast = pad ? Math.max(0, vast - 1) : vast + 2;
        } else {
            vast++;
        }
        if (vast >= 8) {
            BlockPos staan = staanBij(guh.level(), plek);
            if (staan != null) {
                guh.getNavigation().stop();
                guh.snapTo(staan.getX() + 0.5, staan.getY(), staan.getZ() + 0.5, guh.getYRot(), guh.getXRot());
                guh.fallDistance = 0;
            }
            vast = 0;
        }
    }

    /** A spot to stand near the work: the mined cell or a cell beside it, with a floor and room for a guh. */
    @Nullable
    static BlockPos staanBij(Level level, BlockPos plek) {
        if (kanStaan(level, plek)) {
            return plek;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = plek.relative(d);
            if (kanStaan(level, n)) {
                return n;
            }
            if (kanStaan(level, n.above())) {
                return n.above();
            }
        }
        return null;
    }

    static boolean kanStaan(Level level, BlockPos voet) {
        return level.getBlockState(voet.below()).isFaceSturdy(level, voet.below(), Direction.UP)
                && level.getBlockState(voet).getCollisionShape(level, voet).isEmpty() && level.getFluidState(voet).isEmpty()
                && level.getBlockState(voet.above()).getCollisionShape(level, voet.above()).isEmpty();
    }
}
