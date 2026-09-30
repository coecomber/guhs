package nl.juiced.guhs.feature.mewtwo;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * Mieuwguh: a tiny pink floating guhtje with big blue eyes and a long thin tail with a bulb. She giggles at everything, does
 * little somersaults in the air, floats around the kloon-eiland (her {@link #THUIS home}: the island's middle) and around the
 * players who visit her, and once the big knabbel meal is done she is the Guhtwo's best friend: she circles every
 * Guhtwo nearby. Not tameable (she is everybody's), never hurt, no falling. {@link MewSpawner} brings her near the island
 * for players who reached the meal of the questline and takes her away again when nobody like that is around (a Mieuwguh
 * from a spawn egg stays).
 */
public class MewEntity extends AmbientCreature implements GeoEntity {
    /** Persistent keys: her home (BlockPos long), "brought by the spawner" (she leaves when nobody who knows her is near). */
    public static final String THUIS = "guhs_mewtwo_thuis", WILD = "guhs_mewtwo_wild";
    /** How far from home she floats (blocks), how close a player must come to get her attention. */
    public static final int THUIS_STRAAL = 16, SPELER_STRAAL = 7;
    private static final RawAnimation ZWEEF = RawAnimation.begin().thenLoop("animation.mew.zweef");
    private static final RawAnimation GIECHEL = RawAnimation.begin().thenPlay("animation.mew.giechel");
    private static final RawAnimation ZWAAI = RawAnimation.begin().thenPlay("animation.mew.zwaai");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private Vec3 doel;
    private int kies, giechelTijd = 200;

    public MewEntity(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.FLYING_SPEED, 0.4);
    }

    // --- home ------------------------------------------------------------------------------------------------------------

    public void zetThuis(BlockPos pos, boolean wild) {
        getPersistentData().putLong(THUIS, pos.asLong());
        if (wild) {
            getPersistentData().putBoolean(WILD, true);
        }
    }

    public BlockPos thuis() {
        if (!getPersistentData().contains(THUIS)) {
            getPersistentData().putLong(THUIS, blockPosition().asLong());
        }
        return BlockPos.of(getPersistentData().getLongOr(THUIS, 0L));
    }

    public boolean isWild() {
        return getPersistentData().getBooleanOr(WILD, false);
    }

    // --- never hurt, never falls, not pushed -------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (!level().isClientSide() && source.getEntity() instanceof Player) {
            giechel();   // (she thinks it's a game)
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;   // (MewSpawner decides)
    }

    // --- floating around ----------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().scale(0.9));
        if (level().isClientSide() && random.nextInt(14) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 0.2,
                    getZ() + (random.nextDouble() - 0.5) * 0.4, 0, -0.01, 0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 200 == 0 && isWild() && !MewSpawner.welkom(this)) {
            poef();
            return;
        }
        GuhEntity vriendje = vriendje();
        Player speler = level().getNearestPlayer(this, SPELER_STRAAL);
        if (speler != null && speler.isSpectator()) {
            speler = null;
        }
        if (vriendje != null) {
            // round and round her best friend, a little higher than his head
            double t = (tickCount + getId() * 7) * 0.07;
            doel = vriendje.position().add(Math.cos(t) * 2.2, vriendje.getBbHeight() + 0.9 + Math.sin(t * 0.5) * 0.3, Math.sin(t) * 2.2);
            vlieg(doel, 0.5);
            kijkNaar(vriendje.position());
        } else if (speler != null) {
            // close to the player, at head height, bobbing
            if (--kies <= 0 || doel == null || doel.distanceTo(speler.position()) > 4) {
                kies = 40 + random.nextInt(40);
                double a = random.nextDouble() * Math.PI * 2;
                doel = speler.position().add(Math.cos(a) * 2.5, speler.getBbHeight() + 0.2 + random.nextDouble(), Math.sin(a) * 2.5);
            }
            vlieg(doel, 0.35);
            kijkNaar(speler.getEyePosition());
        } else {
            BlockPos thuis = thuis();
            if (--kies <= 0 || doel == null || doel.distanceTo(position()) < 1.2) {
                kies = 60 + random.nextInt(80);
                double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * THUIS_STRAAL;
                int x = thuis.getX() + (int) (Math.cos(a) * r), z = thuis.getZ() + (int) (Math.sin(a) * r);
                int grond = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                doel = new Vec3(x + 0.5, Math.max(grond + 2 + random.nextInt(6), thuis.getY() - 2), z + 0.5);
            }
            vlieg(doel, 0.28);
            Vec3 v = getDeltaMovement();
            if (v.horizontalDistanceSqr() > 1e-4) {
                setYRot((float) (Mth.atan2(v.z, v.x) * 180.0 / Math.PI) - 90f);
                yBodyRot = getYRot();
            }
        }
        if (--giechelTijd <= 0) {
            giechelTijd = 240 + random.nextInt(400);
            if (speler != null || vriendje != null) {
                giechel();
            }
        }
    }

    /** The Guhtwo she circles (after the meal): the nearest one within 16 blocks, when a player nearby did the meal. */
    @Nullable
    private GuhEntity vriendje() {
        List<GuhEntity> mewtwos = level().getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(16),
                g -> g.isAlive() && g.getVariant() == GuhVariant.MEWTWO);
        if (mewtwos.isEmpty()) {
            return null;
        }
        Player p = level().getNearestPlayer(this, 48);
        if (p == null || MewtwoVoortgang.stap(p) < MewtwoVoortgang.KLAAR) {
            return null;
        }
        GuhEntity best = null;
        for (GuhEntity g : mewtwos) {
            if (best == null || distanceToSqr(g) < distanceToSqr(best)) {
                best = g;
            }
        }
        return best;
    }

    private void vlieg(Vec3 to, double speed) {
        Vec3 d = to.subtract(position());
        double len = d.length();
        if (len < 0.05) {
            return;
        }
        Vec3 want = d.scale(Math.min(speed, len * 0.25) / len);
        setDeltaMovement(getDeltaMovement().add(want.subtract(getDeltaMovement()).scale(0.12)));
    }

    private void kijkNaar(Vec3 p) {
        double dx = p.x - getX(), dz = p.z - getZ();
        setYRot((float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90f);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    /** A giggle and a little somersault in the air. */
    public void giechel() {
        if (level() instanceof ServerLevel server) {
            triggerAnim("actie", "giechel");
            playSound(MewtwoFeature.MEW_GIECHEL.get(), 0.9f, 1f + random.nextFloat() * 0.15f);
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.6, getZ(), 2, 0.2, 0.1, 0.2, 0.02);
        }
    }

    /** She says goodbye: a puff of pink sparkles. */
    public void poef() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(MewtwoFeature.GLOED.get(), getX(), getY() + 0.3, getZ(), 16, 0.3, 0.3, 0.3, 0.05);
        }
        discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            triggerAnim("actie", "zwaai");
            playSound(MewtwoFeature.MEW_GIECHEL.get(), 0.9f, 1.2f);
            sp.sendOverlayMessage(Component.translatable("gui.guhs.mewtwo.mew.zwaai"));
            nl.juiced.guhs.quest.GuhDex.zie(sp, GuhVariant.MEW);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? MewtwoFeature.MEW_GIECHEL.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    /** Is there a Mieuwguh within this distance of a spot? */
    public static boolean in(ServerLevel level, Vec3 at, double straal) {
        return !level.getEntitiesOfClass(MewEntity.class, new AABB(at, at).inflate(straal), MewEntity::isAlive).isEmpty();
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "zweef", 4, state -> state.setAndContinue(ZWEEF)));
        controllers.add(new AnimationController<>(this, "actie", 0, state -> PlayState.STOP)
                .triggerableAnim("giechel", GIECHEL).triggerableAnim("zwaai", ZWAAI));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
