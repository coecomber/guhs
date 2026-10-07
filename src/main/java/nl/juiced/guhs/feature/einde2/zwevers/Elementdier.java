package nl.juiced.guhs.feature.einde2.zwevers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;

/**
 * One of the four element animals of the Guheinde (the hybrid animals of the Guhvatar story, guh-ified): the
 * {@link VleermuisLemurguhEntity vleermuis-lemurguh} (lucht), the {@link OtterPinguinguhEntity otter-pinguïnguh} (water),
 * the {@link DasMolguhEntity das-molguh} (aarde) and the {@link SchildpadEendguhEntity schildpad-eendguh} (vuur). They are
 * there for the mood: they live in and around the story structures and the side places (placed by the templates), and a
 * rare wild one walks in the biome of its element ({@link Elementdieren}).
 * <p>
 * What they share: lief (nothing hurts them; a tap only makes them {@link #schrik}), a favourite snack ({@link #isVoer}:
 * they follow whoever holds it and eat it from your hand), petting with an empty hand, and <b>{@code Honk}</b>: an animal
 * placed by a building (NBT {@code Honk}, blocks) stays within that many blocks of where it was put (the spot is taken on
 * its first tick and saved). GeckoLib animations: idle, walk, blij, eet + the animal's own.
 */
public abstract class Elementdier extends PathfinderMob implements GeoEntity {
    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** How far it may go from {@link #honkThuis} (blocks; 0 or less: it is free). */
    private int honk;
    @Nullable
    private BlockPos honkThuis;
    private long laatstGeaaid = Long.MIN_VALUE / 2;

    protected Elementdier(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    // --- what each animal says about itself ---------------------------------------------------------------------------------------

    /** The element it belongs to (its wild ones live in that element's biome). */
    public abstract Einde2Ids.Element element();

    /** Its favourite snack. */
    public abstract boolean isVoer(ItemStack stack);

    /** Its own sound. */
    protected abstract SoundEvent geluid();

    /** The animal's own goals (priority 1-3). */
    protected void eigenDoelen() {
    }

    /** Something startled it (a tap). */
    protected void schrik() {
    }

    /** It ate a snack from this player's hand. */
    protected void gegeten(ServerPlayer player, ItemStack snack) {
    }

    /** Is it busy with something of its own right now (no petting animation, no luring)? */
    public boolean isBezig() {
        return false;
    }

    // --- Honk -------------------------------------------------------------------------------------------------------------------------

    /** Keeps it within this many blocks of this spot (0 or less: free again). */
    public void setHonk(@Nullable BlockPos thuis, int blokken) {
        honk = blokken;
        honkThuis = blokken > 0 ? thuis : null;
        if (honkThuis != null) {
            setHomeTo(honkThuis, honk);
        } else {
            clearHome();
        }
    }

    public int honk() {
        return honk;
    }

    @Nullable
    public BlockPos honkThuis() {
        return honkThuis;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && honk > 0 && honkThuis == null) {
            setHonk(blockPosition(), honk);                        // (placed by a building: this is where it lives)
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (honk > 0) {
            tag.putInt("Honk", honk);
            if (honkThuis != null) {
                tag.putLong("HonkThuis", honkThuis.asLong());
            }
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        int blokken = tag.getIntOr("Honk", 0);
        setHonk(tag.getLong("HonkThuis").map(BlockPos::of).orElse(null), blokken);
        honk = blokken;                                            // (no saved spot yet: the first tick takes it)
    }

    // --- goals ------------------------------------------------------------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        eigenDoelen();
        goalSelector.addGoal(4, new TemptGoal(this, 1.15, this::isVoer, false) {
            @Override
            public boolean canUse() {
                return !isBezig() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        zwerfDoelen();
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** How it wanders and looks around (priority 6-7). */
    protected abstract void zwerfDoelen();

    // --- being lief -------------------------------------------------------------------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean voer = isVoer(stack);
        boolean leeg = stack.isEmpty() && hand == InteractionHand.MAIN_HAND;
        if (!voer && !leeg) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (voer) {
            voer(sp, stack);
        } else {
            aaien(sp);
        }
        return InteractionResult.SUCCESS;
    }

    /** A snack from a player's hand: it eats it, hearts. */
    public void voer(ServerPlayer player, ItemStack stack) {
        ItemStack hap = stack.copyWithCount(1);
        stack.consume(1, player);
        triggerAnim("actie", "eet");
        playSound(geluid(), 0.8f, 1.15f + random.nextFloat() * 0.2f);
        ServerLevel level = (ServerLevel) level();
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, hap.getItem()), getX(), getY() + getBbHeight() * 0.6, getZ(), 6, 0.12, 0.05, 0.12, 0.03);
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.1, getZ(), 2, 0.2, 0.1, 0.2, 0);
        heal(4);
        gegeten(player, hap);
    }

    /** Petting: hearts and a happy little animation. */
    public void aaien(ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.1, getZ(), 2, 0.2, 0.1, 0.2, 0);
        if (!isBezig()) {
            triggerAnim("actie", "blij");
        }
        if (level.getGameTime() - laatstGeaaid >= 20) {
            laatstGeaaid = level.getGameTime();
            playSound(geluid(), 0.8f, 1.1f + random.nextFloat() * 0.3f);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        if (source.getEntity() != null) {
            playSound(geluid(), 0.8f, 1.5f);
            schrik();
        }
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;                                             // (a wild one of the top-up comes and goes: WildeDieren)
    }

    // --- sounds -----------------------------------------------------------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return isBezig() ? null : geluid();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 260;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------------------

    /** The looping animation right now. */
    protected RawAnimation beweging(AnimationTest<Elementdier> state) {
        return state.isMoving() ? WALK : IDLE;
    }

    /** More one-shot animations of this animal (triggerableAnim on the "actie" controller). */
    protected void extraActies(AnimationController<Elementdier> actie) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<Elementdier>("beweeg", 4, state -> state.setAndContinue(beweging(state))));
        AnimationController<Elementdier> actie = new AnimationController<Elementdier>("actie", 1, state -> PlayState.STOP)
                .triggerableAnim("blij", RawAnimation.begin().thenPlay("blij"))
                .triggerableAnim("eet", RawAnimation.begin().thenPlay("eet"));
        extraActies(actie);
        controllers.add(actie);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
