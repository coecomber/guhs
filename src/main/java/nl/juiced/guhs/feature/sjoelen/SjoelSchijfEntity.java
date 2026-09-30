package nl.juiced.guhs.feature.sjoelen;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A sjoelschijf: a round wooden puck with a guh face on top. It has no physics of its own: the {@link SjoelGame} of Opoe
 * Njegschuif slides it over the bak ({@link SjoelBak}) and moves it every tick; the client glides along smoothly and
 * lets it spin. A puck that no game owns any more disappears by itself; pucks are never saved.
 */
public class SjoelSchijfEntity extends Entity {
    public static final float SIZE = (float) (SjoelBak.R * 2), HEIGHT = 0.12f;
    /** Its colour: 0 natural wood, 1 pink (the 20th, the last one: a little present from Opoe). */
    private static final EntityDataAccessor<Integer> KLEUR = SynchedEntityData.defineId(SjoelSchijfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DRAAI = SynchedEntityData.defineId(SjoelSchijfEntity.class, EntityDataSerializers.FLOAT);

    @Nullable
    private SjoelGame game;
    private int puck = -1;

    // client: smooth movement
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;

    public SjoelSchijfEntity(EntityType<? extends SjoelSchijfEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    static SjoelSchijfEntity create(Level level, SjoelGame game, int puck, double x, double y, double z, int kleur) {
        SjoelSchijfEntity e = new SjoelSchijfEntity(SjoelenFeature.SCHIJF.get(), level);
        e.game = game;
        e.puck = puck;
        e.moveTo(x, y, z, 0, 0);
        e.entityData.set(KLEUR, kleur);
        level.addFreshEntity(e);
        return e;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KLEUR, 0);
        builder.define(DRAAI, 0f);
    }

    public int kleur() {
        return entityData.get(KLEUR);
    }

    /** How far it has turned (radians), for the renderer. */
    public float draai() {
        return entityData.get(DRAAI);
    }

    void draai(float spin) {
        entityData.set(DRAAI, spin);
    }

    public int puck() {
        return puck;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                double d = 1.0 / lerpSteps;
                setPos(getX() + (lerpX - getX()) * d, getY() + (lerpY - getY()) * d, getZ() + (lerpZ - getZ()) * d);
                lerpSteps--;
            }
            return;
        }
        if (tickCount % 20 == 0 && (game == null || !game.owns(this))) {
            discard();
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpSteps = steps;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    // --- not a normal entity -----------------------------------------------------------------------------------------

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
