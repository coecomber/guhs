package nl.juiced.guhs.feature.doolhof;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * A Heg-Mika of Het Guhdoolhof: a Mika with twigs and leaves on its head, sneaking through the hedges. It never hurts
 * anyone and can't be hurt. It wanders the maze; when it sees the player it comes after them, and when it touches
 * them it giggles, pinches one kaasknabbel back and hides it somewhere else in the maze ({@link DoolhofGame#gepikt}),
 * then runs off giggling. Without knabbels to pinch it only sticks out its tongue. It belongs to one game (its NPC):
 * without a running game it goes poof.
 */
public class DoolhofMikaEntity extends PathfinderMob implements GeoEntity {
    /** How far it sees the player (in the maze: only with a clear line of sight). */
    public static final double ZICHT = 12;
    /** After a pinch: this long it runs away and can't pinch again. */
    public static final int VLUCHT_TICKS = 60, AFKOEL_TICKS = 120;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guh.walk");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID spel;
    private int afkoel, vlucht, zonderSpel, zoekTimer;

    public DoolhofMikaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.SCALE, 0.75).add(Attributes.STEP_HEIGHT, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    void spel(UUID npc, double snelheid) {
        this.spel = npc;
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(snelheid);
        }
        setPersistenceRequired();
    }

    @Nullable
    public UUID spel() {
        return spel;
    }

    public boolean kanPikken() {
        return afkoel <= 0;
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        DoolhofGame game = DoolhofGame.byNpc(spel);
        if (game == null || !game.bezig()) {
            if (++zonderSpel > 40) {
                poef();
            }
            return;
        }
        zonderSpel = 0;
        if (afkoel > 0) {
            afkoel--;
        }
        ServerLevel level = (ServerLevel) level();
        ServerPlayer p = game.speler(level);
        if (!game.binnen(this)) {
            game.zetTerug(this);
            return;
        }
        if (vlucht > 0) {
            vlucht--;
            if (getNavigation().isDone()) {
                zwerf(game, p, true);
            }
            return;
        }
        if (p != null && distanceTo(p) < ZICHT && hasLineOfSight(p) && game.speelt()) {
            if (tickCount % 5 == 0) {
                getNavigation().moveTo(p, 1.2);
            }
            getLookControl().setLookAt(p, 30, 30);
            if (kanPikken() && getBoundingBox().inflate(0.35).intersects(p.getBoundingBox())) {
                game.gepikt(this, p);
            }
            return;
        }
        if (getNavigation().isDone() || ++zoekTimer > 200) {
            zoekTimer = 0;
            zwerf(game, p, false);
        }
    }

    /** Off to a random spot of the maze (running away from the player: a far one). */
    void zwerf(DoolhofGame game, @Nullable ServerPlayer weg, boolean ver) {
        Vec3 doel = game.willekeurigeCel(getRandom(), weg != null && ver ? weg.position() : null, ver ? 8 : 0, ver ? 0 : 7, position());
        if (doel != null) {
            getNavigation().moveTo(doel.x, doel.y, doel.z, ver ? 1.35 : 0.9);
        }
    }

    /** After a pinch (or nothing to pinch): off it runs. */
    void wegrennen(DoolhofGame game, @Nullable ServerPlayer p) {
        afkoel = AFKOEL_TICKS;
        vlucht = VLUCHT_TICKS;
        getNavigation().stop();
        zwerf(game, p, true);
    }

    void giechel() {
        level().playSound(null, this, DoolhofFeature.GIECHEL.get(), SoundSource.NEUTRAL, 1f, 1.3f + getRandom().nextFloat() * 0.3f);
    }

    void poef() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 10, 0.3, 0.2, 0.3, 0.02);
            server.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 0.5, getZ(), 8, 0.3, 0.2, 0.3, 0.02);
        }
        discard();
    }

    // --- never hurt, never hurting -------------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(ServerLevel serverLevel, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer player && isInvulnerableTo(serverLevel, source)) {
            giechel();
            player.sendOverlayMessage(Component.translatable("gui.guhs.doolhof.niet_meppen"));
            return false;
        }
        return super.hurtServer(serverLevel, source, amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel serverLevel, net.minecraft.world.entity.Entity target) {
        return false;                                           // (a Mika only pinches knabbels)
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return DoolhofFeature.GIECHEL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    public float getVoicePitch() {
        return 1.4f + getRandom().nextFloat() * 0.2f;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (spel != null) {
            tag.store("Spel", UUIDUtil.CODEC, spel);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        spel = tag.read("Spel", UUIDUtil.CODEC).isPresent() ? tag.read("Spel", UUIDUtil.CODEC).orElseThrow() : null;
    }

    // --- animations (the guh ones: its model is the Mika's) -----------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 3, state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** (Tests) a Mika belonging to no game. */
    @Nullable
    static DoolhofMikaEntity maak(ServerLevel level, UUID npc, Vec3 at, double snelheid) {
        DoolhofMikaEntity mika = DoolhofFeature.MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
        if (mika == null) {
            return null;
        }
        mika.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360f, 0);
        mika.spel(npc, snelheid);
        level.addFreshEntity(mika);
        return mika;
    }

    /** Is this player the one it plays with? (for the tests) */
    static boolean isSpeler(Player p, DoolhofGame game) {
        return p.getUUID().equals(game.spelerId());
    }
}
