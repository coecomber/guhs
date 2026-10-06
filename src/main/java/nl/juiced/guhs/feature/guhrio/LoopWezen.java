package nl.juiced.guhs.feature.guhrio;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A creature that walks up and down its lane (the Guhmba, the Schild-Mika): it stays on the lane's line, falls like
 * anything else, and turns at a wall, at the end of the lane and (if it {@link #draaitBijAfgrond}) at a ledge.
 */
public abstract class LoopWezen extends LevelWezen {
    /** +1 further along the lane, -1 back. */
    protected int teken = -1;
    /** Client: its walk (for the feet). */
    public float loop, loopO;

    protected LoopWezen(EntityType<? extends LoopWezen> type, Level level) {
        super(type, level);
    }

    @Override
    public void zetBaan(GuhrioSpel.Actief actief, Baan baan, BlockPos thuis) {
        super.zetBaan(actief, baan, thuis);
        draai();
    }

    /** Which way it walks along its lane (+1 / -1). */
    public int teken() {
        return teken;
    }

    public void zetTeken(int teken) {
        this.teken = teken < 0 ? -1 : 1;
        draai();
    }

    /** Blocks per tick right now; 0: it stands still (but still falls). */
    protected abstract double snelheid();

    protected boolean draaitBijAfgrond() {
        return true;
    }

    /** It walked into something solid (before it turns round). */
    protected void botst(ServerLevel level, BlockPos tegen) {
    }

    /** After every step on the server. */
    protected void naStap(ServerLevel level) {
    }

    protected Vec3 vooruit() {
        Direction d = baan == null ? Direction.EAST : baan.richting(stuk);
        return new Vec3(d.getStepX() * teken, 0, d.getStepZ() * teken);
    }

    protected void draai() {
        Vec3 v = vooruit();
        this.setYRot((float) Math.toDegrees(Math.atan2(-v.x, v.z)));
    }

    @Override
    protected void clientTick() {
        loopO = loop;
        Vec3 d = this.position().subtract(this.xo, this.yo, this.zo);
        loop += (float) Math.sqrt(d.x * d.x + d.z * d.z) * 6f;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        Vec3 v = this.getDeltaMovement();
        double val = this.onGround() ? -0.08 : v.y - 0.08;
        double snel = snelheid();
        if (snel <= 0) {
            this.move(MoverType.SELF, new Vec3(0, val, 0));
            this.setDeltaMovement(0, this.onGround() ? 0 : val * 0.98, 0);
        } else {
            Vec3 voor = vooruit();
            if (this.onGround() && draaitBijAfgrond() && afgrond(level, voor)) {
                teken = -teken;
                voor = vooruit();
            }
            this.move(MoverType.SELF, new Vec3(voor.x * snel, val, voor.z * snel));
            this.setDeltaMovement(voor.x * snel, this.onGround() ? 0 : val * 0.98, voor.z * snel);
            if (this.horizontalCollision) {
                botst(level, BlockPos.containing(getX() + voor.x * (getBbWidth() / 2 + 0.3), getY() + 0.3, getZ() + voor.z * (getBbWidth() / 2 + 0.3)));
                teken = -teken;
            }
            if (baan != null) {
                Baan.Stap stap = baan.stap(stuk, getX(), getZ());
                stuk = stap.stuk();
                this.setPos(stap.x(), getY(), stap.z());
                if (stap.eind()) {
                    teken = -teken;
                }
            }
        }
        if (baan != null ? getY() < baan.onder - 4 : getY() < level.getMinY() - 8) {
            uitHetLevel();
            return;
        }
        draai();
        if (this.isInLava() || this.isInWater()) {
            this.clearFire();
        }
        naStap(level);
    }

    /** It fell out of the level: gone (its spot makes a new one). */
    protected void uitHetLevel() {
        this.discard();
    }

    /** Is there nothing to stand on one step ahead? */
    protected boolean afgrond(ServerLevel level, Vec3 voor) {
        BlockPos onder = BlockPos.containing(getX() + voor.x * 0.6, getY() - 0.3, getZ() + voor.z * 0.6);
        return level.getBlockState(onder).getCollisionShape(level, onder).isEmpty();
    }
}
