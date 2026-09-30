package nl.juiced.guhs.feature.bakkerij;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

/**
 * A customer guh of Bakker Korstje's order game (a prop: no Guhdex page, never saved). It comes in at the door, walks to
 * a spot at the counter and waits there with an order bubble over its head (the pastry it wants and a patience bar;
 * drawn by the client renderer). Served: hearts, a happy hop, and home it goes. Waited too long: a sad little "njeg"
 * and it leaves - guhs are never angry. Right-click it to serve ({@link BakkerijGame#serveer}).
 */
public class BakkerijKlant extends PathfinderMob implements GeoEntity {
    public enum Staat { KOMT, WACHT, BLIJ, VERDRIETIG }

    /** How many different guh looks the customers have (textures, client side). */
    public static final int UITERLIJKEN = 7;
    /** Entity event: shakes its head (a safe custom id, see entity.EntiteitEvents). */
    public static final byte EV_NEE = 70;
    /** A customer who can't get to its spot or the door in this many ticks is helped along (teleported). */
    static final int VAST = 20 * 7;

    private static final EntityDataAccessor<Integer> RECEPT = SynchedEntityData.defineId(BakkerijKlant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GEDULD = SynchedEntityData.defineId(BakkerijKlant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> UITERLIJK = SynchedEntityData.defineId(BakkerijKlant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STAAT = SynchedEntityData.defineId(BakkerijKlant.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guh.walk");
    private static final RawAnimation BLIJ_ANIM = RawAnimation.begin().thenLoop("animation.guh.happy");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID spel;
    private BlockPos doel = BlockPos.ZERO, uitgang = BlockPos.ZERO;
    private int geduldMax = 1, geduldOver = 1, onderweg, neeTimer;
    /** (Tests) how often it got stuck on the way and had to be helped along (teleported). */
    int geholpen;

    public BakkerijKlant(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SCALE, 0.8);
    }

    /** A new customer at the door, on its way to its spot, wanting this. */
    static BakkerijKlant maak(ServerLevel level, UUID spel, BlockPos ingang, BlockPos plek, Recept recept, int geduld) {
        BakkerijKlant k = new BakkerijKlant(BakkerijFeature.KLANT.get(), level);
        k.spel = spel;
        k.doel = plek.immutable();
        k.uitgang = ingang.immutable();
        k.geduldMax = k.geduldOver = Math.max(1, geduld);
        k.entityData.set(RECEPT, recept.ordinal());
        k.entityData.set(GEDULD, 1000);
        k.entityData.set(UITERLIJK, recept == Recept.FEESTTAART ? UITERLIJKEN : level.getRandom().nextInt(UITERLIJKEN));
        k.entityData.set(STAAT, Staat.KOMT.ordinal());
        k.snapTo(ingang.getX() + 0.5, ingang.getY(), ingang.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
        k.setCustomName(Component.translatable(recept == Recept.FEESTTAART ? "entity.guhs.bakkerij_klant.feest" : "entity.guhs.bakkerij_klant"));
        level.addFreshEntity(k);
        level.sendParticles(BakkerijFeature.MEELSTOFJE.get(), k.getX(), k.getY() + 0.5, k.getZ(), 6, 0.3, 0.3, 0.3, 0.01);
        return k;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RECEPT, -1);
        builder.define(GEDULD, 1000);
        builder.define(UITERLIJK, 0);
        builder.define(STAAT, 0);
    }

    /**
     * A customer from /summon (the autocheck and merge QA): {@code {Recept:"vadsdonut",Geduld:6000,GeduldOver:2000,Wacht:1b}} waits
     * right where it's summoned with its order bubble and patience bar, without a game (it's never saved, so this is only read on /summon).
     */
    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        Recept r = Recept.byId(tag.getStringOr("Recept", ""));
        if (r != null) {
            entityData.set(RECEPT, r.ordinal());
            entityData.set(UITERLIJK, r == Recept.FEESTTAART ? UITERLIJKEN : getRandom().nextInt(UITERLIJKEN));
        }
        if (tag.contains("Geduld")) {
            geduldMax = geduldOver = Math.max(1, tag.getIntOr("Geduld", 0));
        }
        if (tag.contains("GeduldOver")) {
            geduldOver = Math.max(1, Math.min(geduldMax, tag.getIntOr("GeduldOver", 0)));
        }
        entityData.set(GEDULD, Math.max(0, geduldOver * 1000 / geduldMax));
        if (tag.getBooleanOr("Wacht", false)) {
            entityData.set(STAAT, Staat.WACHT.ordinal());
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    // --- state --------------------------------------------------------------------------------------------------------

    @Nullable
    public Recept recept() {
        return Recept.byIndex(entityData.get(RECEPT));
    }

    /** How much patience is left (1 = full, 0 = gone). */
    public float geduld() {
        return entityData.get(GEDULD) / 1000f;
    }

    public int uiterlijk() {
        return entityData.get(UITERLIJK);
    }

    public Staat staat() {
        int i = entityData.get(STAAT);
        return i >= 0 && i < Staat.values().length ? Staat.values()[i] : Staat.KOMT;
    }

    /** Waiting at the counter. */
    public boolean wacht() {
        return staat() == Staat.WACHT;
    }

    /** Still wants something (on its way in or waiting): its spot is taken. */
    public boolean bezet() {
        return staat() == Staat.KOMT || staat() == Staat.WACHT;
    }

    @Nullable
    public UUID spel() {
        return spel;
    }

    /** (Tests) where it wants to be, and how it's doing. */
    String debug() {
        return staat() + " at " + blockPosition() + " -> " + doel + " onderweg " + onderweg + " nav " + getNavigation().isDone()
                + " path " + (getNavigation().getPath() == null ? "none" : getNavigation().getPath().getTarget());
    }

    /** (Tests) only this many ticks of patience left. */
    void geduldOver(int ticks) {
        geduldOver = Math.max(1, ticks);
    }

    /** (Client) shakes its head for a moment: that's not what it ordered. */
    public int neeTimer() {
        return neeTimer;
    }

    private void zet(Staat staat) {
        entityData.set(STAAT, staat.ordinal());
        onderweg = 0;
    }

    /** Served: happy, and home. */
    void blij() {
        zet(Staat.BLIJ);
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().add(0, 0.35, 0));             // a happy hop
    }

    /** Wrong pastry: shakes its head (it keeps waiting). */
    void nee() {
        neeTimer = 20;
        level().broadcastEntityEvent(this, EV_NEE);
    }

    /** The game is over: everyone goes home (the waiting ones a bit sad). */
    void naarHuis() {
        if (bezet()) {
            zet(Staat.VERDRIETIG);
        }
        spel = null;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EV_NEE) {
            neeTimer = 20;
        } else {
            super.handleEntityEvent(id);
        }
    }

    // --- ticking ------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (neeTimer > 0) {
            neeTimer--;
        }
        if (!(level() instanceof ServerLevel world)) {
            return;
        }
        if (spel != null && BakkerijGame.byNpc(spel) == null && bezet()) {
            naarHuis();
        }
        if (doel.equals(BlockPos.ZERO)) {
            doel = uitgang = blockPosition();                                  // (summoned: it stays where it is)
        }
        onderweg++;
        switch (staat()) {
            case KOMT -> {
                if (loop(doel)) {
                    zet(Staat.WACHT);
                    getNavigation().stop();
                }
            }
            case WACHT -> {
                geduldOver--;
                entityData.set(GEDULD, Math.max(0, geduldOver * 1000 / geduldMax));
                if (geduldOver % 20 == 0) {
                    Player p = world.getNearestPlayer(this, 8);
                    if (p != null) {
                        getLookControl().setLookAt(p, 30f, 30f);
                    }
                }
                if (geduldOver <= 0) {
                    BakkerijGame game = BakkerijGame.byNpc(spel);
                    zet(Staat.VERDRIETIG);
                    world.sendParticles(ParticleTypes.SPLASH, getX(), getY() + getBbHeight(), getZ(), 6, 0.2, 0.1, 0.2, 0);
                    if (game != null) {
                        game.gemist(this);
                    }
                }
            }
            case BLIJ, VERDRIETIG -> {
                if (staat() == Staat.BLIJ && tickCount % 6 == 0) {
                    world.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 1, 0.2, 0.1, 0.2, 0);
                }
                if (loop(uitgang) || onderweg > VAST * 2) {
                    world.sendParticles(BakkerijFeature.KNABBELWOLKJE.get(), getX(), getY() + 0.5, getZ(), 6, 0.3, 0.3, 0.3, 0.01);
                    discard();
                }
            }
        }
    }

    /** Walks to pos; true when there. Stuck for too long: hop, there it is. */
    private boolean loop(BlockPos pos) {
        Vec3 target = Vec3.atBottomCenterOf(pos);
        if (position().distanceToSqr(target.x, getY(), target.z) < 0.45 && Math.abs(getY() - target.y) < 1.2) {
            return true;
        }
        boolean dichtbij = position().distanceToSqr(target.x, getY(), target.z) < 1.7 && Math.abs(getY() - target.y) < 1.2;
        if (onderweg > VAST || (dichtbij && getNavigation().isDone() && onderweg > 10)) {
            if (!dichtbij) {
                geholpen++;
            }
            moveTo(target.x, target.y, target.z, getYRot(), 0);     // (the last little step, or helped along when stuck)
            getNavigation().stop();
            return true;
        }
        if (getNavigation().isDone() || tickCount % 20 == 0) {
            getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        }
        return false;
    }

    // --- interaction and protection -------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer p) {
            BakkerijGame.serveer(p, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** A game isn't saved, so neither are its customers. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    // --- animations (the guh's) -----------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            if (staat() == Staat.BLIJ && !state.isMoving()) {
                return state.setAndContinue(BLIJ_ANIM);
            }
            return state.setAndContinue(state.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
