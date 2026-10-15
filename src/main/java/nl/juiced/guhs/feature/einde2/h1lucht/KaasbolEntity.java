package nl.juiced.guhs.feature.einde2.h1lucht;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * einde2 phase B, slice h1_lucht: the glowing kaasbol at the Knabbelberg (DESIGN 5 "Start"). One per player who may begin the
 * story and has not opened theirs yet, seen by that player only ({@link Kaasbol} puts it down and takes it away; it is never
 * saved). A click opens it: {@link Kaasbol#open}. As an actor of the scenes kaasbol and intro it plays "open" when the scene
 * says so ({@link Cutscenes#animatie}). Model: geckolib/models/entity/kaasbol.geo.json (+ the glow, kaasbol_gloed.png).
 */
public class KaasbolEntity extends Entity implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.kaasbol.idle");
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("animation.kaasbol.open");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public KaasbolEntity(EntityType<? extends KaasbolEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && tickCount % 10 == 0) {
            // the glow and a thin column of light, so the player sees it from afar
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 2, 0.5, 0.5, 0.5, 0.01);
            if (tickCount % 40 == 0) {
                server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 4.0, getZ(), 6, 0.05, 2.5, 0.05, 0.0);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer sp) {
            Kaasbol.open(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof ServerPlayer sp) {
            Kaasbol.open(sp, this);
        }
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;            // (Kaasbol puts a new one down whenever its player is near: never kept in the world)
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
    }

    // --- GeckoLib -------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, state -> {
            if (level().isClientSide() && "open".equals(Cutscenes.animatie(this))) {
                open = true;             // (a scene actor: once open it stays open, also while the scene waits for a click)
            }
            return state.setAndContinue(open ? OPEN : IDLE);
        }));
    }

    /** Client: this actor was told to open (a scene actor lives only as long as its scene). */
    private boolean open;

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
