package nl.juiced.guhs.feature.beroepen;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The Knabbeldief (Inspecteur Vahoegsma's case): a small Mika with a burglar's mask, a striped shirt and a loot sack on
 * its back. It sits by the stolen knabbel stock, munching and giggling; when someone comes within {@link #SCHRIK} blocks
 * it gets a fright, giggles "betrapt!" and runs off (poof). It never does anything else: it can't hurt and can't be hurt.
 * Gone by itself after {@link #LEVEN} ticks.
 */
public class KnabbeldiefMikaEntity extends PathfinderMob implements GeoEntity {
    public static final double SCHRIK = 5.0;
    public static final int LEVEN = 20 * 60 * 15;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guh.walk");
    private static final RawAnimation SMAK = RawAnimation.begin().thenLoop("animation.guh.emote_smakken");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private boolean vlucht;
    private int vluchtTijd;
    private int leeftijd;
    @Nullable
    private BlockPos buit;

    public KnabbeldiefMikaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.translatable("entity.guhs.knabbeldief_mika"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.SCALE, 0.65);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    /** Where its loot lies (it sits next to it). */
    public void buit(BlockPos pos) {
        this.buit = pos;
    }

    public boolean isGevlucht() {
        return vlucht;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        ServerLevel server = (ServerLevel) level();
        if (++leeftijd > LEVEN) {
            poef();
            return;
        }
        if (vlucht) {
            if (tickCount % 4 == 0) {
                server.sendParticles(KnuffeldalFeature.KRUIMEL.get(), getX(), getY() + 0.2, getZ(), 2, 0.1, 0.05, 0.1, 0.01);
            }
            if (++vluchtTijd > 80 || getNavigation().isDone() && vluchtTijd > 30) {
                poef();
            }
            return;
        }
        if (buit != null) {
            getLookControl().setLookAt(buit.getX() + 0.5, buit.getY() + 0.3, buit.getZ() + 0.5);
        }
        if (tickCount % 20 == 0) {
            server.sendParticles(KnuffeldalFeature.KRUIMEL.get(), getX(), getY() + 0.4, getZ(), 3, 0.2, 0.1, 0.2, 0.02);
            if (random.nextInt(4) == 0) {
                giechel(0.6f);
            }
        }
        Player p = server.getNearestPlayer(this, SCHRIK);
        if (p instanceof ServerPlayer sp && !sp.isSpectator()) {
            schrik(sp);
        }
    }

    /** Caught! It giggles and runs away from the player. */
    public void schrik(ServerPlayer player) {
        if (vlucht) {
            return;
        }
        vlucht = true;
        giechel(1.0f);
        for (Player p : level().players()) {
            if (p.distanceTo(this) < 24 && p instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.knabbeldief.betrapt").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        Vec3 weg = DefaultRandomPos.getPosAway(this, 16, 5, player.position());
        if (weg == null) {
            weg = position().add(position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(10));
        }
        getNavigation().moveTo(weg.x, weg.y, weg.z, 1.6);
    }

    private void giechel(float volume) {
        level().playSound(null, this, KnuffeldalFeature.KRUIMEL_GIECHEL.get(), SoundSource.NEUTRAL, volume, 1.1f + random.nextFloat() * 0.2f);
    }

    /** Off it goes: a puff and a giggle. */
    public void poef() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 14, 0.3, 0.2, 0.3, 0.03);
        }
        discard();
    }

    // --- a Mika never fights ---------------------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Vlucht", vlucht);
        tag.putInt("Leeftijd", leeftijd);
        if (buit != null) {
            tag.putLong("Buit", buit.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        vlucht = tag.getBooleanOr("Vlucht", false);
        leeftijd = tag.getIntOr("Leeftijd", 0);
        buit = tag.keySet().contains("Buit") ? BlockPos.of(tag.getLongOr("Buit", 0L)) : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 4, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(vlucht ? IDLE : SMAK);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
