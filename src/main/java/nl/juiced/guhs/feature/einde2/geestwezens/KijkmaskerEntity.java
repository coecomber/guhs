package nl.juiced.guhs.feature.einde2.geestwezens;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * einde2 phase B, slice geest_modellen: a kijkmasker (DESIGN_EINDE2 5 "Maskers die kijken"): one of the masks of Koh's cave on a
 * little wooden hook, with small guh ears on top; Soort 0..11 = the twelve faces of the block {@code geestenmasker} (noh, guh,
 * mika, guhdalf, sponsguh, vos, aap, lach, verdriet, blanco, blauwe_geest, uil). It hangs where it was put, facing the way it
 * was put (its body never turns), and its face (bone {@code head}) turns SLOWLY towards the nearest player within
 * {@link #ZIET} blocks, at most {@link #DRAAI} degrees a tick and {@link #MAX_YAW} / {@link #MAX_PITCH} degrees from straight
 * ahead. Now and then a mask flickers ({@code knipper}) when you look away. It never does anything else.
 */
public class KijkmaskerEntity extends GeestWezen {
    public static final double ZIET = 16.0;
    public static final float DRAAI = 1.6f, MAX_YAW = 70f, MAX_PITCH = 35f;

    private float richting = Float.NaN;

    public KijkmaskerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.lookControl = new LookControl(this) {
            @Override
            public void tick() {                                     // (its face turns in tick, slowly: nothing pulls it back)
            }
        };
    }

    /** The way it hangs (its body yaw, fixed). */
    public float richting() {
        return Float.isNaN(richting) ? getYRot() : richting;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
            }
        };
    }

    @Override
    protected boolean kijktNaarSpelers() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (Float.isNaN(richting)) {
            richting = getYRot();
        }
        setYRot(richting);
        yBodyRot = richting;
        yBodyRotO = richting;
        if (level().isClientSide() || isNoAi()) {
            return;
        }
        Player p = level().getNearestPlayer(this, ZIET);
        float wilYaw = richting, wilPitch = 0f;
        if (p != null && !p.isSpectator()) {
            Vec3 d = p.getEyePosition().subtract(getEyePosition());
            float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            wilYaw = richting + Mth.clamp(Mth.wrapDegrees(yaw - richting), -MAX_YAW, MAX_YAW);
            wilPitch = Mth.clamp((float) (-Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG), -MAX_PITCH, MAX_PITCH);
            if (random.nextInt(900) == 0) {
                zetAnimatie("knipper");
            }
        }
        float stapYaw = Mth.clamp(Mth.wrapDegrees(wilYaw - yHeadRot), -DRAAI, DRAAI);
        setYHeadRot(yHeadRot + stapYaw);
        setXRot(getXRot() + Mth.clamp(wilPitch - getXRot(), -DRAAI, DRAAI));
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Richting", richting());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        float r = tag.getFloatOr("Richting", Float.NaN);
        if (!Float.isNaN(r)) {
            richting = r;
        }
    }
}
