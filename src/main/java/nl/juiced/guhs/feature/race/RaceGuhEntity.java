package nl.juiced.guhs.feature.race;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.spelen.Niveau;

import net.minecraft.world.level.storage.ValueOutput;
/**
 * The rental race guh: a big, fast guh with a saddle that the Raceguh lends you for one race (it is never yours, never
 * saved and poofs away when the race is over). W runs (it picks up speed), S brakes, the mouse steers, shift = get off.
 * A VAHOEG launch pad under its paws (or a rainbow boost ring around it) gives it a boost. It stands still while the
 * countdown runs ("frozen").
 * <p>
 * The rider's own client moves it (like a horse), so the speed and the boosts are worked out here, on that side.
 * <p>
 * 2.9: its level ({@link Niveau}) sets how it runs: makkelijk is a calmer guh (a bit slower, picks up speed quicker),
 * medium the 2.4 race guh, lastig a faster but trickier one (it turns after your mouse instead of at once, and the silver
 * VAHOEG pads don't work for it). The server tells the rider's game about bumps ({@link #schok}: a Mika pinched the
 * boost, a rolling kaasknabbel, a boost) and scripted rides ({@link RaceRit}: the looping) through synced data.
 */
public class RaceGuhEntity extends GuhEntity {
    private static final EntityDataAccessor<Boolean> DATA_FROZEN = SynchedEntityData.defineId(RaceGuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_NIVEAU = SynchedEntityData.defineId(RaceGuhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SCHOK = SynchedEntityData.defineId(RaceGuhEntity.class, EntityDataSerializers.INT);
    /** The scripted ride as SNBT (1.1.0: 26.1 has no CompoundTag entity data serializer). */
    private static final EntityDataAccessor<String> DATA_RIT = SynchedEntityData.defineId(RaceGuhEntity.class, EntityDataSerializers.STRING);
    /** 2.10.1: a track with jumps (the Regenboogbaan): the rainbow jump over a gap (see {@link #setSprongen}). */
    private static final EntityDataAccessor<Boolean> DATA_SPRONGEN = SynchedEntityData.defineId(RaceGuhEntity.class, EntityDataSerializers.BOOLEAN);
    /** Size of a race guh (big enough to ride, small enough for the tunnel). */
    public static final float SCALE = 1.3f;
    /** Movement "speed" (vanilla: ground speed ~ 2.2x this in blocks per tick): cruising, VAHOEG boost, backwards (medium). */
    public static final float TOP_SPEED = 0.22f, BOOST_SPEED = 0.38f, REVERSE_SPEED = 0.06f, ACCELERATION = 0.012f;
    public static final int BOOST_TICKS = 30;
    /** Per level (makkelijk, medium, lastig): cruising speed, boost speed, acceleration, sideways steering. */
    public static final float[] TOP = {0.19f, TOP_SPEED, 0.25f}, BOOST = {0.33f, BOOST_SPEED, 0.43f}, ACCEL = {0.016f, ACCELERATION, 0.011f},
            SIDEWAYS = {0.3f, 0.3f, 0.2f};
    /** Lastig: how many degrees a tick the guh turns after the rider's mouse (the others turn at once). */
    public static final float LASTIG_TURN = 9f;
    /**
     * 2.10.1, the rainbow jump (only on a track with jumps, see {@link #setSprongen}): a race guh that runs off the road
     * over a gap, with road further on, is thrown up and forward (at least {@link #SPRONG_MIN} blocks a tick) and glides
     * over it with a rainbow sparkle: a bit less gravity ({@link #ZWEEF_GRAVITY}) and it keeps its pace in the air
     * ({@link #ZWEEF_AIR}: the same top speed as on the road) until it lands. The faster it goes, the flatter the jump
     * (up: {@link #SPRONG_LIFT} / its speed, between {@link #SPRONG_VY_MIN} and {@link #SPRONG_VY_MAX}): a slow guh still
     * flies about 6 blocks, a boosted lastig one about 10 (the gaps are 4). {@link #SPRONG_AHEAD}: how far ahead the road on
     * the other side may be.
     */
    public static final double SPRONG_LIFT = 0.16, SPRONG_VY_MIN = 0.2, SPRONG_VY_MAX = 0.42, SPRONG_MIN = 0.36, ZWEEF_GRAVITY = 0.8,
            SPRONG_AHEAD = 10;
    public static final float ZWEEF_AIR = 0.2f;
    /** Bumps from the server (see {@link #schok}). */
    public static final int SCHOK_PIK = 1, SCHOK_BOTS = 2, SCHOK_BOOST = 3;

    private float raceSpeed;
    private int boostTicks;
    /** Server: a little pause between two "VAHOEG!"s of the same pad. */
    private int padCooldown;
    private int schokSeen;
    @Nullable
    private RaceRit rit;
    private int ritTick;
    /** The rainbow jump: gliding now; already jumped since the guh last stood on something. */
    private boolean zweeft, gesprongen;

    public RaceGuhEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FROZEN, false);
        builder.define(DATA_NIVEAU, Niveau.MEDIUM.ordinal());
        builder.define(DATA_SCHOK, 0);
        builder.define(DATA_RIT, "");
        builder.define(DATA_SPRONGEN, false);
    }

    /** No wandering, fleeing, following or eating: it only runs where its rider wants. */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public boolean isFrozen() {
        return this.entityData.get(DATA_FROZEN);
    }

    public void setFrozen(boolean frozen) {
        this.entityData.set(DATA_FROZEN, frozen);
    }

    public Niveau niveau() {
        return Niveau.of(this.entityData.get(DATA_NIVEAU));
    }

    public void setNiveau(Niveau niveau) {
        this.entityData.set(DATA_NIVEAU, niveau.ordinal());
    }

    /** Does this track have jumps (the rainbow jump over its gaps)? */
    public boolean sprongen() {
        return this.entityData.get(DATA_SPRONGEN);
    }

    /** Server: the track has jumps (the Regenboogbaan: CircuitExtra). */
    public void setSprongen(boolean sprongen) {
        this.entityData.set(DATA_SPRONGEN, sprongen);
    }

    /** Gliding over a gap after a rainbow jump (on the side that moves the guh). */
    public boolean zweeft() {
        return zweeft;
    }

    /** Makes it a race guh: size, saddle, a bit of step height for the bumps and ramps. */
    public void setUpForRace() {
        setGuhScale(SCALE);
        equipSaddle(new ItemStack(net.minecraft.world.item.Items.SADDLE), null);
        this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.1);
        this.setPersonality(nl.juiced.guhs.entity.GuhPersonality.BRAVE);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
    }

    // --- bumps and rides from the server --------------------------------------------------------------------------------

    /** Server: a bump the rider's game should feel (SCHOK_PIK: a Mika pinched the boost; SCHOK_BOTS: a rolling knabbel; SCHOK_BOOST). */
    public void schok(int kind) {
        int seq = (this.entityData.get(DATA_SCHOK) >> 3) + 1;
        this.entityData.set(DATA_SCHOK, (seq << 3) | (kind & 7));
    }

    /** The last bump the server sent (SCHOK_PIK, SCHOK_BOTS, SCHOK_BOOST; 0: none yet). */
    public int lastSchok() {
        return this.entityData.get(DATA_SCHOK) & 7;
    }

    /** Server: start a scripted ride (the looping). */
    public void startRit(RaceRit ride) {
        this.entityData.set(DATA_RIT, ride.save().toString());
    }

    /** In a scripted ride right now? */
    public boolean inRit() {
        return rit != null;
    }

    @Nullable
    public RaceRit rit() {
        return rit;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SCHOK.equals(key)) {
            int value = this.entityData.get(DATA_SCHOK);
            if (value != schokSeen) {
                schokSeen = value;
                feel(value & 7);
            }
        } else if (DATA_RIT.equals(key)) {
            RaceRit ride = RaceRit.load(snbt(this.entityData.get(DATA_RIT)));
            if (ride != null) {
                rit = ride;
                ritTick = 0;
                this.noPhysics = true;
                this.setNoGravity(true);
            } else if (rit != null) {
                endRit();
            }
        }
    }

    /** What a bump does to the guh's run (on the side that moves it). */
    void feel(int kind) {
        int n = niveau().ordinal();
        switch (kind) {
            case SCHOK_PIK -> {
                boostTicks = 0;
                raceSpeed = Math.min(raceSpeed, TOP[n]) * 0.45f;
            }
            case SCHOK_BOTS -> {
                boostTicks = 0;
                raceSpeed = Math.min(raceSpeed, 0.03f);
                if (this.onGround()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1, 0.2).add(0, 0.45, 0));
                }
            }
            case SCHOK_BOOST -> {
                boostTicks = BOOST_TICKS;
                raceSpeed = Math.max(raceSpeed, BOOST[n]);
            }
            default -> {
            }
        }
    }

    private void endRit() {
        RaceRit ride = rit;
        rit = null;
        this.noPhysics = false;
        this.setNoGravity(false);
        if (ride != null) {
            Vec3 out = ride.exit();
            this.setPos(out.x, out.y, out.z);
            float yaw = ride.yaw();
            this.setYRot(yaw);
            this.yRotO = this.yBodyRot = this.yHeadRot = yaw;
            this.setDeltaMovement(Vec3.directionFromRotation(0, yaw).scale(0.5));
            feel(SCHOK_BOOST);                       // out of the looping with a VAHOEG!
        }
    }

    // --- riding -------------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    public boolean isRideable() {
        return true;
    }

    /**
     * 2.10 (samen): behind the racer there is room for one more: the racer's own guh rides along. The racer stays
     * passenger 0 (the one who steers); the second seat only takes a guh, and only while someone is in front.
     */
    @Override
    protected boolean canAddPassenger(net.minecraft.world.entity.Entity passenger) {
        if (this.getPassengers().isEmpty()) {
            return passenger instanceof Player;   // (only a player steers a race guh)
        }
        return this.getPassengers().size() == 1 && this.getFirstPassenger() instanceof Player
                && passenger instanceof GuhEntity && !(passenger instanceof RaceGuhEntity);
    }

    /** The second seat (the guh riding along) is behind the racer, a little lower, turned with the race guh. */
    @Override
    protected Vec3 getPassengerAttachmentPoint(net.minecraft.world.entity.Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions,
                                               float partialTick) {
        Vec3 seat = super.getPassengerAttachmentPoint(passenger, dimensions, partialTick);
        if (this.getPassengers().indexOf(passenger) < 1) {
            return seat;
        }
        float scale = this.getScale();
        return seat.add(new Vec3(0, -0.12 * scale, -0.62 * scale).yRot(-this.yBodyRot * ((float) Math.PI / 180F)));
    }

    /**
     * 2.10 (visual QA): the guh on the second seat looks where the kart goes (its own look goals used to turn it sideways
     * on the seat). Runs after the rider's own AI tick, on both sides.
     */
    @Override
    protected void positionRider(net.minecraft.world.entity.Entity passenger, net.minecraft.world.entity.Entity.MoveFunction move) {
        super.positionRider(passenger, move);
        if (passenger instanceof GuhEntity guh && this.getPassengers().indexOf(passenger) >= 1) {
            float yaw = this.yBodyRot;
            guh.setYRot(yaw);
            guh.yRotO = yaw;
            guh.setYBodyRot(yaw);
            guh.yBodyRotO = this.yBodyRotO;
            guh.setYHeadRot(yaw);
            guh.yHeadRotO = this.yBodyRotO;
        }
    }

    /** A race guh never launches (right-click while riding is for your own guh). */
    @Override
    public void onLaunchPressed(Player rider) {
    }

    /** The racer can hop back on (after getting off by accident); nobody else can do anything with it. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.isVehicle() && !player.isPassenger()) {
            if (!this.level().isClientSide() && player instanceof ServerPlayer racer && RaceGame.isRacerOf(racer, this)) {
                racer.startRiding(this, true, true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onOwnerTap(Player player) {
    }

    /** Medium and makkelijk turn with the mouse at once; lastig turns after it a bit at a time (trickier). */
    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        if (niveau() != Niveau.LASTIG || inRit()) {
            super.tickRidden(player, travelVector);
            return;
        }
        float yaw = turnTowards(this.getYRot(), player.getYRot());
        this.setRot(yaw, player.getXRot() * 0.5f);
        this.yRotO = this.yBodyRot = this.yHeadRot = yaw;
    }

    /** Lastig: the new yaw, at most {@link #LASTIG_TURN} degrees closer to where the rider looks. */
    static float turnTowards(float yaw, float want) {
        return yaw + Mth.clamp(Mth.wrapDegrees(want - yaw), -LASTIG_TURN, LASTIG_TURN);
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        if (isFrozen() || inRit()) {
            if (isFrozen()) {
                raceSpeed = 0;
                boostTicks = 0;
            }
            return Vec3.ZERO;
        }
        int n = niveau().ordinal();
        if (boostHere() && boostTicks < BOOST_TICKS - 5) {
            boostTicks = BOOST_TICKS;
            raceSpeed = Math.max(raceSpeed, BOOST[n]);
            if (this.onGround() && onPad()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.3, 0)); // a little VAHOEG hop
            }
        }
        float target = player.zza > 0 ? (boostTicks > 0 ? BOOST[n] : TOP[n]) : player.zza < 0 ? -REVERSE_SPEED : 0;
        if (boostTicks > 0) {
            boostTicks--;
        }
        if (raceSpeed < target) {
            raceSpeed = Math.min(target, raceSpeed + (player.zza > 0 ? ACCEL[n] : ACCEL[n] * 2));
        } else if (raceSpeed > target) {
            raceSpeed = Math.max(target, raceSpeed - (player.zza < 0 ? ACCEL[n] * 3 : ACCEL[n] * 0.7f));
        }
        if (Math.abs(raceSpeed) < 0.005f) {
            return Vec3.ZERO;
        }
        return new Vec3(player.xxa * SIDEWAYS[n], 0, raceSpeed >= 0 ? 1 : -1);
    }

    /** (Tests) one step of the rider's input, as the rider's game would work it out. */
    public Vec3 riddenInputForTest(Player player) {
        return getRiddenInput(player, Vec3.ZERO);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return Math.abs(raceSpeed);
    }

    /** (Tests) as if it already runs this fast. */
    public void setRaceSpeedForTest(float speed) {
        raceSpeed = speed;
    }

    /** (Tests) one tick of the rider's game moving the guh, as travelRidden does it: the input, the speed, the move. */
    public void rideTickForTest(Player player) {
        Vec3 input = getRiddenInput(player, Vec3.ZERO);
        this.setSpeed(getRiddenSpeed(player));
        travel(input);
    }

    /** In a scripted ride the path moves the guh (on the rider's side; the server follows in {@link #tick}). */
    @Override
    public void travel(Vec3 input) {
        if (rit != null) {
            Vec3 p = rit.at(ritTick);
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(p.x, p.y, p.z);
            this.calculateEntityAnimation(true);
            return;
        }
        if (sprongen() && !gesprongen && !this.onGround() && this.getDeltaMovement().y <= 0.05 && raceSpeed > 0.005f) {
            sprong();
        }
        super.travel(input);
        if (this.onGround() || this.isInWater() || !sprongen()) {
            zweeft = false;
            gesprongen = false;
        }
    }

    /** The rainbow jump: off the road over a gap with road further on? Up, and glide over it. */
    private void sprong() {
        Vec3 v = this.getDeltaMovement();
        Vec3 dir = new Vec3(v.x, 0, v.z);
        if (dir.lengthSqr() < 0.01) {
            dir = Vec3.directionFromRotation(0, this.getYRot());
        }
        dir = dir.normalize();
        if (!gatOnder() || !wegVerderop(dir)) {
            return;
        }
        double forward = Math.max(SPRONG_MIN, Math.sqrt(v.x * v.x + v.z * v.z));
        this.setDeltaMovement(dir.x * forward, Mth.clamp(SPRONG_LIFT / forward, SPRONG_VY_MIN, SPRONG_VY_MAX), dir.z * forward);
        this.needsSync = true;
        zweeft = true;
        gesprongen = true;
        this.resetFallDistance();
        if (this.level().isClientSide()) {
            this.level().playLocalSound(getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.5f, 1.4f, false);
        }
    }

    /** Nothing to stand on for 4 blocks under the guh's middle (a gap, not a step down the road). */
    private boolean gatOnder() {
        BlockPos feet = BlockPos.containing(getX(), getY() - 0.01, getZ());
        for (int dy = 0; dy < 4; dy++) {
            BlockPos p = feet.below(dy);
            if (!this.level().getBlockState(p).getCollisionShape(this.level(), p).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Road on the other side: something to land on ahead (at most {@link #SPRONG_AHEAD} blocks), no higher than the guh. */
    private boolean wegVerderop(Vec3 dir) {
        int top = Mth.floor(getY() - 0.01);
        for (double d = 1; d <= SPRONG_AHEAD; d += 0.5) {
            double x = getX() + dir.x * d, z = getZ() + dir.z * d;
            for (int y = top; y >= top - 5; y--) {
                BlockPos p = BlockPos.containing(x, y, z);
                if (!this.level().getBlockState(p).getCollisionShape(this.level(), p).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Gliding: less gravity. */
    @Override
    protected double getDefaultGravity() {
        return zweeft ? super.getDefaultGravity() * ZWEEF_GRAVITY : super.getDefaultGravity();
    }

    /** In the air a race guh keeps a bit of its speed (as vanilla does for a ridden mob); gliding it keeps its whole pace. */
    @Override
    protected float getFlyingSpeed() {
        return this.getSpeed() * (zweeft ? ZWEEF_AIR : 0.1f);
    }

    /** Is there a working VAHOEG pad under its paws? (On lastig the silver pads don't work.) */
    public boolean onPad() {
        BlockPos pos = this.blockPosition();
        return workingPad(this.level().getBlockState(pos)) || workingPad(this.level().getBlockState(pos.below()));
    }

    private boolean workingPad(BlockState state) {
        return state.is(RaceFeature.RACE_PAD.get()) && (niveau() != Niveau.LASTIG || state.getValue(RaceBlocks.LASTIG));
    }

    /** Is it in a boost ring (a block of the tag guhs:race_boost, e.g. the Regenboogbaan's rainbow rings)? */
    public boolean inBoostRing() {
        var box = getBoundingBox();
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (this.level().getBlockState(p).is(RaceGame.BOOST_BLOCKS)) {
                return true;
            }
        }
        return false;
    }

    /** A pad or a boost ring. */
    public boolean boostHere() {
        return onPad() || inBoostRing();
    }

    public float getRaceSpeed() {
        return raceSpeed;
    }

    public int boostTicks() {
        return boostTicks;
    }

    @Override
    public void tick() {
        if (rit != null) {
            ritTick++;
            if (ritTick >= rit.ticks()) {
                endRit();
                if (!this.level().isClientSide()) {
                    this.entityData.set(DATA_RIT, "");
                }
            } else if (!this.level().isClientSide() || !this.isLocalInstanceAuthoritative()) {
                Vec3 p = rit.at(ritTick);
                this.setDeltaMovement(Vec3.ZERO);
                this.setPos(p.x, p.y, p.z);
            }
        }
        super.tick();
        if (zweeft && this.level().isClientSide()) {
            regenboogGlitter();
        }
        if (!this.level().isClientSide()) {
            if (padCooldown > 0) {
                padCooldown--;
            } else if (this.isVehicle() && boostHere() && !inRit()) {
                padCooldown = 20;
                RaceGame.onPad(this);
            }
            if (!this.isVehicle() && !RaceGame.isRaceGuh(this) && this.tickCount > 40) {
                this.discard(); // a leftover without a race (a race that ended while it wasn't loaded)
                return;
            }
            RaceGame.tickFrom(this);
        }
    }

    private static CompoundTag snbt(String s) {
        if (s.isEmpty()) {
            return new CompoundTag();
        }
        try {
            return net.minecraft.nbt.TagParser.parseCompoundFully(s);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return new CompoundTag();
        }
    }

    private static final int[] GLITTER = {0xFF4B5C, 0xFF9F3B, 0xFFE14B, 0x6BE36B, 0x5BC8FF, 0x6B72FF, 0xC06BFF};

    /** Client: rainbow sparkles under a gliding race guh. */
    private void regenboogGlitter() {
        for (int i = 0; i < 3; i++) {
            int rgb = GLITTER[(this.tickCount * 3 + i) % GLITTER.length];
            var dust = new net.minecraft.core.particles.DustParticleOptions(rgb, 1.4f);
            this.level().addParticle(dust, getX() + (random.nextDouble() - 0.5) * 1.4, getY() + random.nextDouble() * 0.4,
                    getZ() + (random.nextDouble() - 0.5) * 1.4, 0, -0.05, 0);
        }
        if (this.tickCount % 2 == 0) {
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 0, -0.02, 0);
        }
    }

    // --- never yours, never hurt, never saved ---------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(net.minecraft.server.level.ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    protected void dropEquipment(net.minecraft.server.level.ServerLevel level) {
        // the saddle and the clothes belong to the Raceguh
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /**
     * 1.2.7: the racer leaves the kart for whatever reason (gets off, is teleported away, logs out...): their own guh on the
     * back seat hops off at once. The race guh is never saved, so a guh still sitting on it when its chunk unloads would be
     * lost for good.
     */
    @Override
    protected void removePassenger(net.minecraft.world.entity.Entity passenger) {
        super.removePassenger(passenger);
        if (!this.level().isClientSide() && passenger instanceof ServerPlayer racer) {
            nl.juiced.guhs.feature.samen.SamenMee.uitDeKart(racer);
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("RaceGuh", true);
    }
}
