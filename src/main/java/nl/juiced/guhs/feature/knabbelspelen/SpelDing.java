package nl.juiced.guhs.feature.knabbelspelen;

import java.util.UUID;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * A thing of De Knabbelspelen that the game moves around: a kaasknabbel swinging on its string (Knabbelhappen; bite it
 * by hitting or right-clicking it), the knabbelspijker dangling behind your guh belt (Spijkerpoepen) or a pinned tail
 * on the guh board (Guhguhtje prik). The client draws the item and its string (to the beam or to the player's belt).
 * Never saved: after a restart the game is over and its things are gone.
 */
public class SpelDing extends Entity {
    public static final int HANGKNABBEL = 0, SPIJKER = 1, STAARTJE = 2;
    private static final EntityDataAccessor<Integer> SOORT = SynchedEntityData.defineId(SpelDing.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GOUD = SynchedEntityData.defineId(SpelDing.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Vector3f> TOUW = SynchedEntityData.defineId(SpelDing.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> EIGENAAR = SynchedEntityData.defineId(SpelDing.class, EntityDataSerializers.INT);

    @Nullable
    UUID spel;
    int baan;
    /** A swinging knabbel: string length, amplitude (rad), speed (rad/tick), phase, swing direction (world, horizontal). */
    double lengte, amp, omega, fase;
    Vec3 as = new Vec3(1, 0, 0);
    int leeftijd;

    public SpelDing(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SOORT, HANGKNABBEL);
        builder.define(GOUD, false);
        builder.define(TOUW, new Vector3f());
        builder.define(EIGENAAR, -1);
    }

    public int soort() {
        return entityData.get(SOORT);
    }

    void soort(int s) {
        entityData.set(SOORT, s);
    }

    public boolean goud() {
        return entityData.get(GOUD);
    }

    void goud(boolean g) {
        entityData.set(GOUD, g);
    }

    /** Where its string is tied (the beam; for the spijker: unused, the client uses the owner's belt). */
    public Vec3 touw() {
        Vector3f v = entityData.get(TOUW);
        return new Vec3(v.x(), v.y(), v.z());
    }

    void touw(Vec3 p) {
        entityData.set(TOUW, new Vector3f((float) p.x, (float) p.y, (float) p.z));
    }

    /** The entity id of the player whose belt the spijker hangs from (-1: none). */
    public int eigenaar() {
        return entityData.get(EIGENAAR);
    }

    void eigenaar(int id) {
        entityData.set(EIGENAAR, id);
    }

    /** A swinging knabbel's spot at this moment. */
    Vec3 slinger(int t) {
        double hoek = amp * Math.sin(omega * t + fase);
        Vec3 p = touw();
        return p.add(as.scale(Math.sin(hoek) * lengte)).add(0, -Math.cos(hoek) * lengte, 0);
    }

    @Override
    public void tick() {
        super.tick();
        leeftijd++;
        if (level().isClientSide()) {
            return;
        }
        if (soort() == HANGKNABBEL && lengte > 0) {
            Vec3 p = slinger(leeftijd);
            setPos(p.x, p.y, p.z);
        }
        if (Wedstrijd.byNpc(spel) == null && leeftijd > 20) {
            discard();                                        // (its game is over)
        }
    }

    // --- biting ------------------------------------------------------------------------------------------------------------

    @Override
    public boolean isPickable() {
        return soort() == HANGKNABBEL;
    }

    @Override
    public boolean isAttackable() {
        return soort() == HANGKNABBEL;
    }

    /** Hitting it = a bite (no damage, nothing breaks). */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!level().isClientSide() && attacker instanceof ServerPlayer p && soort() == HANGKNABBEL) {
            Knabbelhappen.hap(p, this);
        }
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (soort() != HANGKNABBEL) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer p && hand == InteractionHand.MAIN_HAND) {
            Knabbelhappen.hap(p, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    // --- what it looks like (for the renderer) ---------------------------------------------------------------------------

    public ItemStack stack() {
        return switch (soort()) {
            case SPIJKER -> new ItemStack(KnabbelspelenFeature.KNABBELSPIJKER.get());
            case STAARTJE -> new ItemStack(KnabbelspelenFeature.GUHGUHTJE_STAARTJE.get());
            default -> new ItemStack(goud() ? net.minecraft.core.registries.BuiltInRegistries.ITEM.get(nl.juiced.guhs.Guhs.id("gouden_kaasknabbel"))
                    : nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get());
        };
    }

    // --- never saved --------------------------------------------------------------------------------------------------------

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64 * 64;
    }
}
