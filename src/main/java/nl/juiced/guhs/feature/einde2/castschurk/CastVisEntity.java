package nl.juiced.guhs.feature.einde2.castschurk;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice cast_schurk: Tui and La, the maanvis and the oceaanvis of the Geestenoase (CONTRACT_E2 11.6). They circle
 * each other: a fish that finds the other kind within {@link #ZOEK} blocks takes the midpoint of the two as its centre and
 * swims a ring around it, half a turn from the other (the yin and yang of the pond: both keep to the line through the
 * midpoint, so they stay opposite); alone it circles its own spot. The radius is half their distance, at most {@link #MAX_STRAAL};
 * it never leaves the water (a step into a block that is not water is skipped) and with {@code stil} it lies still. No bucket,
 * no flopping on land, no drops.
 */
public class CastVisEntity extends CastWezen {
    static final double ZOEK = 8.0, MAX_STRAAL = 3.0, ALLEEN_STRAAL = 1.5;
    /** Radians a tick along the ring, and the most it moves in one tick. */
    static final double HOEKSNELHEID = 0.045, STAP = 0.2;

    @Nullable
    private Vec3 thuis;

    public CastVisEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /** The fish of the other kind (the partner), or null. */
    @Nullable
    public CastVisEntity partner() {
        Wezens ander = wezen == Wezens.MAANVIS ? Wezens.OCEAANVIS : Wezens.MAANVIS;
        CastVisEntity beste = null;
        double bd = ZOEK * ZOEK;
        for (CastVisEntity v : level().getEntitiesOfClass(CastVisEntity.class, getBoundingBox().inflate(ZOEK))) {
            double d = v.distanceToSqr(this);
            if (v != this && v.wezen == ander && d <= bd) {
                beste = v;
                bd = d;
            }
        }
        return beste;
    }

    private boolean water(Vec3 p) {
        return level().getFluidState(BlockPos.containing(p)).is(FluidTags.WATER);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        boolean nat = water(position());
        if (isNoGravity() != nat) {
            setNoGravity(nat);                                      // (in the water it floats; on land it just lies there)
        }
        if (thuis == null) {
            thuis = position();
        }
        if (!nat || stil()) {
            if (nat) {
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        CastVisEntity ander = partner();
        Vec3 midden;
        double straal;
        if (ander != null) {
            midden = position().add(ander.position()).scale(0.5);
            straal = Math.min(MAX_STRAAL, Math.sqrt(horizontaal(position(), ander.position())) / 2);
        } else {
            midden = thuis;
            straal = ALLEEN_STRAAL;
        }
        double hoek = Math.atan2(getZ() - midden.z, getX() - midden.x) + HOEKSNELHEID;
        Vec3 doel = new Vec3(midden.x + Math.cos(hoek) * straal, getY(), midden.z + Math.sin(hoek) * straal);
        Vec3 stap = doel.subtract(position());
        if (stap.length() > STAP) {
            stap = stap.normalize().scale(STAP);
        }
        Vec3 naar = position().add(stap);
        setDeltaMovement(Vec3.ZERO);
        if (stap.lengthSqr() > 1e-6 && water(naar)) {
            setPos(naar.x, naar.y, naar.z);
            float yaw = hoek(stap.x, stap.z);
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
        }
    }

    private static double horizontaal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    /** It swims while it is in the water and not on its mark. */
    @Override
    protected boolean isBezig() {
        return isInWater() && !stil();
    }
}
