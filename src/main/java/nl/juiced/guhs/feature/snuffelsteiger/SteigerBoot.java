package nl.juiced.guhs.feature.snuffelsteiger;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.SynchedEntityData;
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

/**
 * Kapitein Zoutsnoet's boat, "De Natte Neus" (entity {@code guhs:steiger_boot}): a little wooden sloop with a mast, a
 * cream sail, a pennant with a paw and a lantern on its bow. Model, texture, animation:
 * tools/features/snuffel_steiger_modellen.py; the renderer: client.SnuffelsteigerClient.
 * <ul>
 *   <li><b>Moored</b> at every steigerhuisje (kept there by {@code Bezetting}): it never moves, nobody rides it, you can
 *   stand on it, and a click on it is a word with the captain ({@link SteigerVerhaal#klikBoot}).</li>
 *   <li><b>In the cutscenes</b> (the crossing, the trip home) it is the actor that sails: the scene's script moves it, and
 *   {@code animatie("boot", t, "storm")} makes it roll on the waves ({@link #stormt}).</li>
 * </ul>
 * Nothing about it is per player and nothing is ever taken from it: the real trip is the cutscene.
 */
public class SteigerBoot extends Entity implements GeoEntity {
    /** The cutscene animation: heavy weather. */
    public static final String STORM = "storm";
    private static final RawAnimation DOBBER = RawAnimation.begin().thenLoop("dobber"), WAPPER = RawAnimation.begin().thenLoop("storm");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SteigerBoot(EntityType<? extends SteigerBoot> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Client: is this the boat of a scene in heavy weather? */
    public boolean stormt() {
        return level().isClientSide() && STORM.equals(Cutscenes.animatie(this));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!level().isClientSide() && player instanceof ServerPlayer p && hand == InteractionHand.MAIN_HAND) {
            SteigerVerhaal.klikBoot(this, p);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild && player.isShiftKeyDown()) {
            discard();                         // (creative: a sneak-hit removes it; Bezetting brings a new one)
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    /** Solid, like a vanilla boat: you can step from the pier onto its deck. */
    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SteigerBoot>("zeil", 6, state -> state.setAndContinue(stormt() ? WAPPER : DOBBER)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
