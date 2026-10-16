package nl.juiced.guhs.feature.einde2.appa;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2, slice appa: the Appa-guh, a big six-legged flying bison with a guh face (its own model, not the guh's).
 * <ul>
 *   <li><b>Seats.</b> His owner sits on his head and steers; three guests ride in the saddle basket (players who click him,
 *       or the owner's own guhs: click him with a carried guh in your hand or with guhs on your leash); five more seats
 *       ({@link #TEAM_STOELEN}) are only filled by {@link #zetTeam} (the team of the story). Which seat a passenger has
 *       follows from who it is, the same on both sides.</li>
 *   <li><b>Flying.</b> On the ground he ambles. The jump key is "jip jip": he takes off, and from then on he flies where
 *       his rider looks (jump = up, sneak = down), slower than Guhvleugels but for as long as you like, and he hangs still
 *       when you let go. Touching the ground is landing. You can't get off in the air (sneak is "down" there). This runs
 *       on the rider's own client, like the Enderguh.</li>
 *   <li><b>Flying by himself</b> ({@link #vliegNaar}, see {@link Appa#vlieg}): the server steers him to a spot and lands
 *       him there; when the player may steer he only points the way until they let go for three seconds.</li>
 *   <li><b>Never lost.</b> Nothing hurts him, nothing pushes or leashes him, he doesn't fall out of the world. His saddle
 *       chest and his name also live in his owner's saved data ({@link Appa}), so the Appa that the fluitje makes again
 *       is the same Appa; an older copy that turns up later steps aside by itself.</li>
 * </ul>
 * Without an owner (the creative spawn egg, a template) he just stands there and looks cute.
 */
public class AppaGuhEntity extends PathfinderMob implements GeoEntity {
    /** Extra decorative seats that only {@link #zetTeam} fills (the team of the story). */
    public static final int TEAM_STOELEN = 5;
    /** Guests in the saddle basket, next to the owner on his head. */
    public static final int GASTEN = 3;
    public static final int KIST_VAKKEN = 27;
    /** Blocks per tick in the air (an Enderguh does 0.6, Guhvleugels far more), sprinting, and straight up or down. */
    public static final double SNELHEID = 0.46, SPRINT = 0.6, STIJGEN = 0.3;
    /**
     * einde2 fix-reizen: on a journey (a flight to a place: a story flight, a pick from his menu) he goes about twice as fast,
     * whether he flies himself or his rider steers, and climbs faster.
     */
    public static final double REIS_SNELHEID = 0.92, REIS_SPRINT = 1.2, REIS_STIJGEN = 0.55;
    /**
     * Flying by himself: at least this far above the goal; at least {@value #MARGE} blocks above the highest ground or roof
     * he will pass over in the next blocks ({@link #VOORUIT}); and only right above his landing spot (closer than
     * {@value #BOVEN_PAD} blocks) does he go straight down onto it.
     */
    private static final double KRUISHOOGTE = 10, MARGE = 7, BOVEN_PAD = 2.5;
    /** How far ahead (blocks along the way, and 3.5 to either side) he looks at the heightmap. */
    private static final double[] VOORUIT = {0, 4, 8, 14, 22, 32, 44, 58};
    /** Ticks without a key before he takes over a flight the player may steer. */
    public static final int LOSLATEN = 60;

    private static final EntityDataAccessor<String> DATA_EIGENAAR = SynchedEntityData.defineId(AppaGuhEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_ZELF = SynchedEntityData.defineId(AppaGuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LUCHT = SynchedEntityData.defineId(AppaGuhEntity.class, EntityDataSerializers.BOOLEAN);
    /** The entity ids of the passengers on the team seats, "12,40,7" (the client seats them by it). */
    private static final EntityDataAccessor<String> DATA_TEAM = SynchedEntityData.defineId(AppaGuhEntity.class, EntityDataSerializers.STRING);
    /** einde2 fix-reizen: he is on a journey (a flight to a place), so his rider steers at the journey speed. */
    private static final EntityDataAccessor<Boolean> DATA_REIS = SynchedEntityData.defineId(AppaGuhEntity.class, EntityDataSerializers.BOOLEAN);

    /** Seats, in his own space (x = to his left, y = up, z = forward), in blocks. */
    private static final Vec3 KOP = new Vec3(0, 2.44, 2.25);
    private static final Vec3[] MAND = {new Vec3(0.66, 2.5, 0.62), new Vec3(-0.66, 2.5, 0.62), new Vec3(0, 2.5, 0.05)};
    private static final Vec3[] TEAM = {new Vec3(0.82, 2.5, -0.1), new Vec3(-0.82, 2.5, -0.1), new Vec3(0, 2.5, 0.82),
            new Vec3(0.5, 2.5, -0.62), new Vec3(-0.5, 2.5, -0.62)};

    /** How far his owner may stand from him with the saddle chest open (blocks; he is big). */
    public static final double KIST_BEREIK = 10.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final SimpleContainer kist = new SimpleContainer(KIST_VAKKEN) {
        @Override
        public void setChanged() {
            super.setChanged();
            kistVeranderd();
        }

        /** An open chest shuts when he is gone, was made again (a recall, a story flight) or its owner walked off. */
        @Override
        public boolean stillValid(Player player) {
            return kistGeldig(player);
        }
    };
    /** Which Appa of his owner this is ({@link Appa}: an older number than the owner's steps aside). */
    private int gen;
    /** Server: the chest was read from the owner's saved data (or the owner has none yet). */
    private boolean kistGelezen;
    /** Server: don't write the chest back to the owner while it is being filled from there. */
    private boolean kistStil;
    /** Server: {@link #zetTeam} is seating someone (so {@link #canAddPassenger} counts team seats). */
    private boolean teamBezig;
    /** Server: he lets everybody off (a landing by himself, a recall). */
    boolean uitstappen;
    /** Server: the last spot he stood on the ground (not saved; null until he has landed once). */
    @Nullable
    private Vec3 laatsteGrond;
    /** How fast he glides back to that ground when nobody steers him over the void (blocks per tick). */
    public static final double TERUG_SNELHEID = 0.3;
    /** Server: the flight he is on (null: none). */
    @Nullable
    private Vlucht vlucht;
    /** His yaw at the start of this tick (his passengers turn along with him by the difference). */
    private float vorigeYaw;

    /** A flight to a spot: who it is for, whether they may steer, and how it is going. */
    private static final class Vlucht {
        final UUID speler;
        final Vec3 doel;
        final Appa.Landing landing;
        final boolean spelerStuurt;
        int stil;
        int klok;
        double beste = Double.MAX_VALUE;
        int vast;

        Vlucht(UUID speler, Appa.Landing landing, boolean spelerStuurt) {
            this.speler = speler;
            this.doel = landing.appa();
            this.landing = landing;
            this.spelerStuurt = spelerStuurt;
        }
    }

    /** Server: a rider's box hit something on the last move (he climbs over it, as over a wall in his own way). */
    private boolean ruiterBotste;

    public AppaGuhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.STEP_HEIGHT, 1.1).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_EIGENAAR, "");
        builder.define(DATA_ZELF, false);
        builder.define(DATA_LUCHT, false);
        builder.define(DATA_TEAM, "");
        builder.define(DATA_REIS, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    // --- whose he is ----------------------------------------------------------------------------------------------------------

    @Nullable
    public UUID eigenaar() {
        String s = entityData.get(DATA_EIGENAAR);
        if (s.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isEigenaar(@Nullable Entity e) {
        return e != null && e.getUUID().equals(eigenaar());
    }

    void zetEigenaar(UUID eigenaar, int gen) {
        entityData.set(DATA_EIGENAAR, eigenaar.toString());
        this.gen = gen;
    }

    public int gen() {
        return gen;
    }

    public SimpleContainer kist() {
        return kist;
    }

    /** Fills his chest from the owner's saved data (see {@link Appa}); nothing is written back meanwhile. */
    void leesKist(ServerPlayer eigenaar) {
        kistStil = true;
        try {
            Appa.leesKist(eigenaar, kist);
        } finally {
            kistStil = false;
        }
        kistGelezen = true;
    }

    boolean isKistGelezen() {
        return kistGelezen;
    }

    private void kistVeranderd() {
        if (kistStil || level().isClientSide() || isWeggehaald()) {
            return;                                 // (a stale menu of an Appa that was taken away never writes: the new one has the chest)
        }
        ServerPlayer baas = baas();
        if (baas != null && kistGelezen && gen == Appa.gen(baas)) {
            Appa.schrijfKist(baas, kist);
        }
    }

    /** Taken out of the world for good (discarded or dead), not just unloaded with its chunk. */
    private boolean isWeggehaald() {
        return getRemovalReason() != null && getRemovalReason().shouldDestroy();
    }

    /** May this player (still) look into the saddle chest: he is alive and here, theirs, the copy that counts, and near. */
    boolean kistGeldig(Player player) {
        return isAlive() && !isRemoved() && isEigenaar(player) && player.distanceToSqr(this) <= KIST_BEREIK * KIST_BEREIK
                && (!(player instanceof ServerPlayer sp) || gen == Appa.gen(sp));
    }

    /** His owner, when online. */
    @Nullable
    public ServerPlayer baas() {
        UUID e = eigenaar();
        return e == null || level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(e);
    }

    /**
     * einde2 phase B (reizen): a shove while he flies himself (a fireball of Zuka's ship, a dodge): added to his motion,
     * which his own flight smooths away again in about a second. Never hurts anybody.
     */
    public void duw(Vec3 impuls) {
        if (!level().isClientSide()) {
            setDeltaMovement(getDeltaMovement().add(impuls));
            hurtMarked = true;
        }
    }

    // --- seats ----------------------------------------------------------------------------------------------------------------

    /** The passenger on his head (his owner), or null. */
    @Nullable
    public Entity bestuurder() {
        for (Entity p : getPassengers()) {
            if (isEigenaar(p)) {
                return p;
            }
        }
        return null;
    }

    private List<Integer> teamIds() {
        List<Integer> out = new ArrayList<>();
        for (String s : entityData.get(DATA_TEAM).split(",")) {
            if (!s.isEmpty()) {
                try {
                    out.add(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    // (never: we write it ourselves)
                }
            }
        }
        return out;
    }

    public boolean isTeam(Entity e) {
        return teamIds().contains(e.getId());
    }

    /** The guests in the saddle basket (not the owner, not the team). */
    public List<Entity> gasten() {
        List<Integer> team = teamIds();
        List<Entity> out = new ArrayList<>();
        for (Entity p : getPassengers()) {
            if (!isEigenaar(p) && !team.contains(p.getId())) {
                out.add(p);
            }
        }
        return out;
    }

    /** The passengers on the team seats. */
    public List<Entity> team() {
        List<Integer> team = teamIds();
        List<Entity> out = new ArrayList<>();
        for (Entity p : getPassengers()) {
            if (team.contains(p.getId())) {
                out.add(p);
            }
        }
        return out;
    }

    /** Where this passenger sits, in his own space. */
    private Vec3 stoel(Entity passenger) {
        if (isEigenaar(passenger)) {
            return KOP;
        }
        List<Integer> team = teamIds();
        int i = team.indexOf(passenger.getId());
        if (i >= 0) {
            return TEAM[i % TEAM.length];
        }
        return MAND[Math.max(0, gasten().indexOf(passenger)) % MAND.length];
    }

    /** hook fase B: puts a team member on one of the team seats (false: all five are taken, or it can't ride him). */
    public boolean zetTeam(Entity e) {
        if (level().isClientSide() || e == this || isEigenaar(e)) {
            return false;
        }
        if (hasPassenger(e) && isTeam(e)) {
            return true;
        }
        teamOpruimen();
        if (teamIds().size() >= TEAM_STOELEN) {
            return false;
        }
        if (hasPassenger(e)) {                     // a guest moves to a team seat
            schrijfTeam(e.getId(), true);
            return true;
        }
        teamBezig = true;
        try {
            if (!e.startRiding(this, true, false)) {
                return false;
            }
        } finally {
            teamBezig = false;
        }
        schrijfTeam(e.getId(), true);
        return true;
    }

    /** hook fase B: the team steps off (next to him). */
    public void teamAf() {
        if (level().isClientSide()) {
            return;
        }
        uitstappen = true;
        try {
            for (Entity e : team()) {
                e.stopRiding();
            }
        } finally {
            uitstappen = false;
        }
        entityData.set(DATA_TEAM, "");
    }

    private void schrijfTeam(int id, boolean erbij) {
        List<Integer> team = teamIds();
        team.remove((Integer) id);
        if (erbij) {
            team.add(id);
        }
        StringBuilder sb = new StringBuilder();
        for (int t : team) {
            sb.append(sb.length() == 0 ? "" : ",").append(t);
        }
        entityData.set(DATA_TEAM, sb.toString());
    }

    /** Forgets team seats whose passenger is gone. */
    private void teamOpruimen() {
        for (int id : teamIds()) {
            boolean er = false;
            for (Entity p : getPassengers()) {
                er |= p.getId() == id;
            }
            if (!er) {
                schrijfTeam(id, false);
            }
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (teamBezig) {
            return teamIds().size() < TEAM_STOELEN;
        }
        if (isEigenaar(passenger)) {
            return bestuurder() == null;
        }
        return gasten().size() < GASTEN;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return stoel(passenger).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger != getControllingPassenger()) {              // whoever doesn't steer turns along with him
            float turn = Mth.wrapDegrees(getYRot() - vorigeYaw);
            if (Math.abs(turn) < 45) {
                passenger.setYRot(passenger.getYRot() + turn);
                passenger.setYHeadRot(passenger.getYHeadRot() + turn);
            }
        }
    }

    /** Off him up high (a teleport, a log-out): you float down instead of falling. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (level().isClientSide()) {
            return;
        }
        if (isLucht() && passenger instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));
            if (passenger instanceof ServerPlayer sp && !uitstappen) {
                // einde2 fix-reizen: off him in the air (a cutscene took them, a teleport): as soon as they are free again
                // and still hanging there, they are put down on solid ground, never in a wall (Appa#zetNeer)
                Appa.zetNeer(sp, veiligeGrondOnder(sp));
            }
        }
        if (isTeam(passenger)) {
            schrijfTeam(passenger.getId(), false);
        }
    }

    /** Off him: next to his flank (left, right, behind), on the ground when there is ground. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3[] kanten = {new Vec3(2.3, 0, 0), new Vec3(-2.3, 0, 0), new Vec3(0, 0, -3.6), new Vec3(2.3, 0, 1.6), new Vec3(-2.3, 0, 1.6)};
        for (Vec3 kant : kanten) {
            Vec3 at = position().add(kant.yRot(-getYRot() * Mth.DEG_TO_RAD));
            for (int dy = 1; dy >= -2; dy--) {
                Vec3 plek = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), BlockPos.containing(at.x, at.y + dy, at.z), true);
                if (plek != null) {
                    return plek;
                }
            }
        }
        // (in the air: beside him, they float down; einde2 fix-reizen: never into a wall)
        for (Vec3 kant : kanten) {
            Vec3 at = position().add(kant.yRot(-getYRot() * Mth.DEG_TO_RAD));
            if (level().noCollision(passenger, passenger.getDimensions(passenger.getPose()).makeBoundingBox(at))) {
                return at;
            }
        }
        Vec3 vrij = Appa.vrijePlek(level(), passenger.getType(), position(), 6, false);
        return vrij != null ? vrij : position().add(0, getBbHeight(), 0);
    }

    /** Server: where a rider who got off up here lands safely: the ground under them, else the ground he last stood on. */
    private Vec3 veiligeGrondOnder(ServerPlayer p) {
        BlockPos kolom = p.blockPosition();
        if (level().hasChunkAt(kolom)) {
            int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING, kolom.getX(), kolom.getZ());
            if (y > level().getMinY() + 1 && y <= p.getY() + 0.5) {
                Vec3 grond = Appa.vrijePlek(level(), p.getType(), new Vec3(p.getX(), y, p.getZ()), 4, true);
                if (grond != null) {
                    return grond;
                }
            }
        }
        return laatsteGrond != null ? laatsteGrond : p.position();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        if (entityData.get(DATA_ZELF)) {
            return null;       // he flies by himself: the server steers, so everyone sees the same
        }
        return bestuurder() instanceof Player player ? player : null;
    }

    // --- flying with a rider (on the rider's own client) --------------------------------------------------------------------------

    /** In the air (not standing on something, nothing right under him)? Synced; the server decides. */
    public boolean isLucht() {
        return entityData.get(DATA_LUCHT);
    }

    /** Is he flying by himself right now (the server steers)? */
    public boolean vliegtZelf() {
        return entityData.get(DATA_ZELF);
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        // a big animal turns slowly towards where his rider looks
        float yaw = getYRot() + Mth.wrapDegrees(player.getYRot() - getYRot()) * 0.14f;
        setRot(yaw, 0);
        this.yRotO = this.yBodyRot = this.yHeadRot = yaw;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float forward = player.zza <= 0 ? player.zza * 0.4f : player.zza;
        return new Vec3(player.xxa * 0.5f, 0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    /** He hovers for as long as his rider likes: the server must not take that for cheating (allow-flight=false). */
    @Override
    public boolean isFlyingVehicle() {
        return true;
    }

    @Override
    public void travel(Vec3 input) {
        if (getControllingPassenger() instanceof Player rider) {
            stuur(rider, input);
        } else if (!level().isClientSide() && vliegtZelf() && vlucht != null) {
            vliegZelf();
        } else {
            zweef();
        }
    }

    /** His rider steers: an amble on the ground, and from "jip jip" (the jump key) on a flight where the rider looks. */
    private void stuur(Player rider, Vec3 input) {
        boolean op = rider.isJumping();
        boolean neer = rider.isShiftKeyDown();
        if (onGround() && !op) {
            super.travel(input);
            return;
        }
        Vec3 look = rider.getLookAngle();
        Vec3 links = new Vec3(look.z, 0, -look.x);
        links = links.lengthSqr() < 1e-6 ? Vec3.ZERO : links.normalize();
        boolean reis = isReis();
        double speed = reis ? rider.isSprinting() ? REIS_SPRINT : REIS_SNELHEID : rider.isSprinting() ? SPRINT : SNELHEID;
        double stijgen = reis ? REIS_STIJGEN : STIJGEN;
        Vec3 want = look.scale(input.z * speed).add(links.scale(input.x * speed));
        if (op) {
            want = want.add(0, stijgen, 0);
        }
        if (neer) {
            want = want.add(0, -stijgen, 0);
        }
        vlieg(getDeltaMovement().scale(0.86).add(want.scale(0.14)));
    }

    /**
     * One tick of flight with this motion (he never sinks out of the world). einde2 fix-reizen: he never carries a rider
     * into a wall or a roof either. His own box does not cover the seat on his head, so a move that would push a player's
     * box from free space into blocks is taken back (only its climb or sink is tried); a rider already stuck may move out.
     */
    private void vlieg(Vec3 motion) {
        if (getY() + motion.y < level().getMinY() + 2 && motion.y < 0) {
            motion = new Vec3(motion.x, 0, motion.z);
        }
        Vec3 van = position();
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        if (ruiterRaakt(position().subtract(van))) {
            setPos(van);
            move(MoverType.SELF, new Vec3(0, motion.y, 0));
            if (ruiterRaakt(position().subtract(van))) {
                setPos(van);
            }
            setDeltaMovement(new Vec3(0, getDeltaMovement().y * 0.5, 0));
            ruiterBotste = true;
        }
        calculateEntityAnimation(true);
        resetFallDistance();
    }

    /** Would this shift of him push a rider (a player) out of free space into blocks? */
    private boolean ruiterRaakt(Vec3 verschuiving) {
        if (verschuiving.lengthSqr() < 1.0e-8) {
            return false;
        }
        for (Entity e : getPassengers()) {
            if (e instanceof Player speler) {
                var doos = speler.getBoundingBox();
                if (level().noCollision(speler, doos) && !level().noCollision(speler, doos.move(verschuiving))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Nobody at the reins: on the ground he stands; in the air he sinks gently when there is ground below. Over the void he
     * glides back to the last ground he stood on (his owner logged out in the air, a guest is still aboard: nobody is left
     * hanging), and only hangs when he knows no ground.
     */
    private void zweef() {
        if (onGround() || level().isClientSide()) {
            super.travel(Vec3.ZERO);
            return;
        }
        Vec3 m = getDeltaMovement();
        if (grondOnder(64) || laatsteGrond == null) {
            vlieg(new Vec3(m.x * 0.8, grondOnder(64) ? -0.14 : 0, m.z * 0.8));
            return;
        }
        Vec3 naar = new Vec3(laatsteGrond.x - getX(), 0, laatsteGrond.z - getZ());
        double ver = naar.length();
        Vec3 stap = ver < 1.0e-3 ? Vec3.ZERO : naar.scale(Math.min(TERUG_SNELHEID, ver) / ver);
        double dy = Mth.clamp(laatsteGrond.y + 2.0 - getY(), -0.14, 0.14);
        if (ver > 1.0e-3) {
            setYRot(Mth.approachDegrees(getYRot(), (float) (Mth.atan2(naar.z, naar.x) * (180.0 / Math.PI)) - 90f, 4f));
        }
        vlieg(new Vec3(stap.x, dy, stap.z));
    }

    private boolean grondOnder(double diep) {
        return grondOnder(this, diep);
    }

    /** Is there something to land on within {@code diep} blocks under this entity? */
    static boolean grondOnder(Entity e, double diep) {
        Vec3 van = e.position().add(0, 0.2, 0);
        return e.level().clip(new ClipContext(van, van.add(0, -diep, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, e)).getType()
                != HitResult.Type.MISS;
    }

    /** Server: where he last stood on the ground in this level (null: not since he came into the world). */
    @Nullable
    Vec3 laatsteGrond() {
        return laatsteGrond;
    }

    // --- flying by himself (the server) --------------------------------------------------------------------------------------

    /**
     * Server: flies this player (who must sit on him) to {@code doel} and lands there. With {@code spelerStuurt} he only
     * points the way until the player lets go for {@value #LOSLATEN} ticks (and hands the reins back when they steer again);
     * the last few blocks he always does himself. Replaces a flight that was going on.
     */
    void vliegNaar(ServerPlayer speler, Appa.Landing landing, boolean spelerStuurt) {
        vlucht = new Vlucht(speler.getUUID(), landing, spelerStuurt);
        entityData.set(DATA_ZELF, !spelerStuurt);
        entityData.set(DATA_REIS, true);
    }

    /** On a journey (a flight to a place) right now? Synced: his rider steers at the journey speed. */
    public boolean isReis() {
        return entityData.get(DATA_REIS);
    }

    /** Server: the landing of the flight he is on, or null. */
    @Nullable
    public Appa.Landing vluchtLanding() {
        return vlucht == null ? null : vlucht.landing;
    }

    /** Server: the goal of the flight he is on, or null. */
    @Nullable
    public Vec3 vluchtDoel() {
        return vlucht == null ? null : vlucht.doel;
    }

    /** Server: breaks the flight off (he hangs where he is; his rider has the reins again). */
    void stopVlucht() {
        vlucht = null;
        entityData.set(DATA_ZELF, false);
        entityData.set(DATA_REIS, false);
    }

    private void vluchtTick() {
        Vlucht v = vlucht;
        if (v == null) {
            if (vliegtZelf()) {
                entityData.set(DATA_ZELF, false);
            }
            return;
        }
        ServerPlayer p = level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(v.speler);
        if (p == null || p.getVehicle() != this) {
            if (p != null && v.landing.gebied() != null && v.landing.gebied().contains(position())) {
                // einde2 fix-reizen: taken off him on the last stretch (the arrival scene of the place began): he lands on
                // his spot of the landing place at once, and his rider is set down beside him once the scene lets them go
                zetOpLanding(v.landing);
                Appa.zetNeer(p, v.landing.spelerOf(this, p));
                land();
                return;
            }
            stopVlucht();                          // nobody to fly for any more
            return;
        }
        v.klok++;
        double dx = v.doel.x - getX(), dz = v.doel.z - getZ();
        double ver = Math.sqrt(dx * dx + dz * dz);
        boolean zelf = true;
        if (v.spelerStuurt && ver >= 12) {         // (the last blocks and the landing are always his)
            Input in = p.getLastClientInput();
            if (in.forward() || in.backward() || in.left() || in.right() || in.jump() || in.shift()) {
                v.stil = 0;
            } else {
                v.stil++;
            }
            zelf = v.stil >= LOSLATEN;
        }
        if (zelf != vliegtZelf()) {
            entityData.set(DATA_ZELF, zelf);
            v.beste = Double.MAX_VALUE;
            v.vast = 0;
        }
        if (v.klok % 20 == 1) {
            wijs(p, v, ver, zelf);
        }
    }

    /** The line above the hotbar: how far, and (while the player steers) which way, plus a few sparkles from his nose. */
    private void wijs(ServerPlayer p, Vlucht v, double ver, boolean zelf) {
        int blokken = (int) Math.round(ver);
        if (zelf) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_appa.vliegt_zelf", getDisplayName(), blokken).withStyle(ChatFormatting.AQUA));
            return;
        }
        p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_appa.wijst", getDisplayName(), pijl(p, v.doel), blokken)
                .withStyle(ChatFormatting.AQUA));
        Vec3 neus = position().add(new Vec3(0, 1.6, 3.2).yRot(-getYRot() * Mth.DEG_TO_RAD));
        Vec3 richting = v.doel.subtract(neus).normalize();
        if (level() instanceof ServerLevel server) {
            for (int i = 1; i <= 6; i++) {
                Vec3 at = neus.add(richting.scale(i * 1.4));
                server.sendParticles(p, ParticleTypes.END_ROD, true, true, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    /** An arrow for "the goal is that way", seen from where the player looks. */
    static String pijl(Player p, Vec3 doel) {
        double hoek = Math.toDegrees(Math.atan2(-(doel.x - p.getX()), doel.z - p.getZ()));
        int k = Math.floorMod(Math.round(Mth.wrapDegrees((float) hoek - p.getYRot()) / 45f), 8);
        return new String[]{"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"}[k];
    }

    /**
     * One tick of his own flight (einde2 fix-reizen): at journey speed and at a safe height over everything on the way
     * (the heightmap ahead and to both sides, {@link #VOORUIT}), slowing down on the last stretch, then straight down onto
     * his landing spot once he is right above it. Where the chunks ahead are not there yet he slows down, and he waits in
     * the air while the very next ones are missing, so he never flies into the unknown.
     */
    private void vliegZelf() {
        Vlucht v = vlucht;
        double dx = v.doel.x - getX(), dz = v.doel.z - getZ();
        double ver = Math.sqrt(dx * dx + dz * dz);
        double dy = v.doel.y - getY();
        if (ver < 1.0 && (onGround() || Math.abs(dy) < 0.35)) {
            land();
            return;
        }
        Vec3 want;
        float yawDoel;
        if (ver > BOVEN_PAD) {
            double[] h = veiligeHoogte(dx / ver, dz / ver, ver);
            double hoogte = Math.min(level().getMaxY() - 6, Math.max(v.doel.y + KRUISHOOGTE, h[0] + MARGE));
            double tempo = Math.min(REIS_SNELHEID, 0.08 + ver * 0.045);     // (full speed until ~18 blocks out)
            if (h[1] > 1) {
                tempo = 0;                                                  // (the chunks right ahead are not there yet)
            } else if (h[1] > 0) {
                tempo = Math.min(tempo, SNELHEID * 0.6);
            }
            double klim = hoogte - getY();
            double vy = Mth.clamp(klim * 0.12, -REIS_STIJGEN, REIS_STIJGEN);
            if (klim > 3) {
                tempo *= 0.3;                                               // (well below the safe height: mostly up first)
            }
            want = new Vec3(dx / ver * tempo, vy, dz / ver * tempo);
            yawDoel = (float) Math.toDegrees(Math.atan2(-dx, dz));
        } else {
            // right above the landing spot: centre on it and go straight down (the landing spots are free up to the sky)
            want = new Vec3(dx * 0.15, Mth.clamp(dy * 0.1, -0.45, 0.2), dz * 0.15);
            yawDoel = Float.isNaN(v.landing.yaw()) ? getYRot() : v.landing.yaw();
        }
        if (horizontalCollision || ruiterBotste) {
            want = new Vec3(want.x * 0.2, REIS_STIJGEN, want.z * 0.2);   // something in the way: over it
        }
        ruiterBotste = false;
        vlieg(getDeltaMovement().scale(0.8).add(want.scale(0.2)));
        float yaw = Mth.approachDegrees(getYRot(), yawDoel, 5f);
        setRot(yaw, 0);
        this.yBodyRot = this.yHeadRot = yaw;
        // stuck for good (a cave roof, a cage)? then he "knows a shortcut": straight onto his landing spot
        double afstand = position().distanceTo(v.doel);
        if (afstand < v.beste - 0.5) {
            v.beste = afstand;
            v.vast = 0;
        } else if (++v.vast > 160) {
            zetOpLanding(v.landing);
            land();
        }
    }

    /**
     * The highest ground or roof under his way: {heightmap top, unknown}, where unknown is 0 (all there), 1 (chunks further
     * ahead are missing) or 2 (the chunks of the next 8 blocks are missing).
     */
    private double[] veiligeHoogte(double nx, double nz, double ver) {
        double hoogste = level().getMinY();
        int onbekend = 0;
        for (double d : VOORUIT) {
            if (d > ver + 3) {
                break;
            }
            for (double opzij : new double[]{-3.5, 0, 3.5}) {
                int x = Mth.floor(getX() + nx * d - nz * opzij), z = Mth.floor(getZ() + nz * d + nx * opzij);
                if (!level().hasChunkAt(new BlockPos(x, getBlockY(), z))) {
                    onbekend = Math.max(onbekend, d <= 8 ? 2 : 1);
                    continue;
                }
                hoogste = Math.max(hoogste, level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
            }
        }
        return new double[]{hoogste, onbekend};
    }

    /** Server: puts him on his landing spot, looking its way (loads its chunk when needed). */
    private void zetOpLanding(Appa.Landing landing) {
        Vec3 a = landing.appa();
        if (level() instanceof ServerLevel server) {
            server.getChunk(BlockPos.containing(a));
        }
        float yaw = Float.isNaN(landing.yaw()) ? getYRot() : landing.yaw();
        snapTo(a.x, a.y + 0.05, a.z, yaw, 0);
        this.yBodyRot = this.yHeadRot = yaw;
        setDeltaMovement(Vec3.ZERO);
    }

    /** Server: he is there. On a landing that says so, his rider gets off onto its spot (einde2 fix-reizen). */
    private void land() {
        Vlucht v = vlucht;
        stopVlucht();
        setDeltaMovement(Vec3.ZERO);
        brul();
        ServerPlayer p = v == null || level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(v.speler);
        if (p != null && v.landing.afstappen() && p.getVehicle() == this) {
            Vec3 naar = v.landing.spelerOf(this, p);
            uitstappen = true;
            try {
                p.stopRiding();
            } finally {
                uitstappen = false;
            }
            p.teleportTo(naar.x, naar.y, naar.z);
            p.resetFallDistance();
            p.setDeltaMovement(Vec3.ZERO);
        }
        if (p != null) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_appa.aangekomen", getDisplayName()).withStyle(ChatFormatting.AQUA));
            Appa.AANGEKOMEN.forEach(h -> h.accept(p));
        }
    }

    // --- every tick -----------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        vorigeYaw = getYRot();
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        boolean lucht = !onGround() && level().noCollision(this, getBoundingBox().move(0, -0.75, 0));
        if (onGround() && tickCount % 10 == 0) {
            laatsteGrond = position();
        }
        if (lucht != isLucht()) {
            entityData.set(DATA_LUCHT, lucht);
            if (lucht && bestuurder() != null) {
                jipJip();
            }
        }
        vluchtTick();
        if (tickCount % 10 == 3) {
            teamOpruimen();
            Appa.meld(this);
        }
    }

    /** "Jip jip!": the take-off (sound, his little jump, the line for everyone on him). */
    public void jipJip() {
        playSound(E2AppaFeature.JIP_JIP.get(), 1.4f, 0.9f + random.nextFloat() * 0.2f);
        triggerAnim("actie", "jipjip");
        for (Entity e : getPassengers()) {
            if (e instanceof ServerPlayer p) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_appa.jip_jip").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** His friendly roar. */
    public void brul() {
        playSound(E2AppaFeature.BRUL.get(), 1.2f, 0.9f + random.nextFloat() * 0.2f);
        triggerAnim("actie", "brul");
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return E2AppaFeature.BRUL.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 900;
    }

    @Override
    public void playAmbientSound() {
        if (!isVehicle()) {
            brul();
        }
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    // --- clicks ---------------------------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        UUID eigenaar = eigenaar();
        boolean baas = isEigenaar(player);
        if (stack.is(Items.NAME_TAG) && baas) {
            return InteractionResult.PASS;                     // (the name tag names him; Appa.meld remembers the name)
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        if (eigenaar == null) {
            brul();
            zeg(sp, Component.translatable("gui.guhs.einde2_appa.wild"));
            return InteractionResult.SUCCESS;
        }
        if (baas) {
            if (!kistGelezen) {
                leesKist(sp);
            }
            if (guhsErop(sp, stack)) {
                return InteractionResult.SUCCESS;
            }
            if (player.isSecondaryUseActive()) {
                openKist(sp);
            } else if (Appa.KLIK.test(sp)) {
                return InteractionResult.SUCCESS;              // (einde2 fix-reizen: his menu "Waar vliegen we heen?" opened)
            } else if (!hasPassenger(sp) && sp.startRiding(this)) {
                zeg(sp, Component.translatable("gui.guhs.einde2_appa.opstappen", getDisplayName()));
            }
            return InteractionResult.SUCCESS;
        }
        Component naam = Appa.naamVan(level().getServer(), eigenaar);
        if (player.isSecondaryUseActive()) {
            zeg(sp, Component.translatable("gui.guhs.einde2_appa.kist_niet", naam));
        } else if (hasPassenger(sp)) {
            return InteractionResult.SUCCESS;
        } else if (gasten().size() >= GASTEN) {
            zeg(sp, Component.translatable("gui.guhs.einde2_appa.vol"));
        } else if (sp.startRiding(this)) {
            zeg(sp, Component.translatable("gui.guhs.einde2_appa.instappen", getDisplayName(), naam));
        }
        return InteractionResult.SUCCESS;
    }

    private static void zeg(ServerPlayer p, Component tekst) {
        p.sendOverlayMessage(tekst.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Server: opens the saddle chest for his owner. */
    public void openKist(ServerPlayer baas) {
        baas.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, kist),
                Component.translatable("gui.guhs.einde2_appa.kist", getDisplayName())));
    }

    /**
     * The owner's own guhs get into the saddle basket: the carried guh in their hand, or every own guh on their leash
     * (the leads come back). True when the click was about guhs (also when the basket was full).
     */
    private boolean guhsErop(ServerPlayer baas, ItemStack stack) {
        if (stack.is(ModItems.PICKED_UP_GUH.get())) {
            if (gasten().size() >= GASTEN) {
                zeg(baas, Component.translatable("gui.guhs.einde2_appa.vol"));
                return true;
            }
            Entity e = PickedUpGuhItem.release(level(), PickedUpGuhItem.guhData(stack), getX(), getY() + 2.6, getZ(), getYRot());
            if (e != null) {
                stack.shrink(1);
                if (e instanceof GuhEntity guh && guh.isOwnedBy(baas) && guh.startRiding(this, true, false)) {
                    zeg(baas, Component.translatable("gui.guhs.einde2_appa.guh_erop", guh.getDisplayName()));
                    guh.playSound(nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), 1f, 1f);
                }
            }
            return true;
        }
        boolean iemand = false;
        for (GuhEntity guh : level().getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(12),
                g -> g.isLeashed() && g.getLeashHolder() == baas && g.isOwnedBy(baas))) {
            iemand = true;
            if (gasten().size() >= GASTEN) {
                zeg(baas, Component.translatable("gui.guhs.einde2_appa.vol"));
                break;
            }
            guh.removeLeash();
            Minigames.give(baas, new ItemStack(Items.LEAD));
            if (guh.startRiding(this, true, false)) {
                zeg(baas, Component.translatable("gui.guhs.einde2_appa.guh_erop", guh.getDisplayName()));
                guh.playSound(nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), 1f, 1f);
            }
        }
        return iemand;
    }

    /** Server: every guh in the saddle basket hops out (his owner gave him a pat on the flank). False: there was none. */
    public boolean guhsEraf() {
        boolean iemand = false;
        uitstappen = true;
        try {
            for (Entity e : gasten()) {
                if (e instanceof GuhEntity) {
                    e.stopRiding();
                    iemand = true;
                }
            }
        } finally {
            uitstappen = false;
        }
        return iemand;
    }

    // --- he is never hurt, pushed, leashed or lost -----------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);     // (/kill: the fluitje makes him again)
        }
        if (source.getEntity() instanceof ServerPlayer p) {
            if (isEigenaar(p)) {
                if (!isLucht() && guhsEraf()) {
                    zeg(p, Component.translatable("gui.guhs.einde2_appa.guhs_eraf"));
                }
            } else if (eigenaar() == null && p.getAbilities().instabuild && p.isShiftKeyDown()) {
                discard();                                      // (creative: sneak-hit removes one without an owner)
            }
        }
        return false;
    }

    @Override
    protected void onBelowWorld() {
        setPos(getX(), level().getMinY() + 4, getZ());
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isPushedByFluid() {
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
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;                                           // (in another dimension the fluitje brings him)
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 224 * 224;
    }

    @Override
    public void remove(RemovalReason reason) {
        Appa.vergeet(this);
        super.remove(reason);
    }

    // --- saving ---------------------------------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Eigenaar", entityData.get(DATA_EIGENAAR));
        tag.putInt("Gen", gen);
        ContainerHelper.saveAllItems(tag, kist.getItems());
        // (a flight in progress and the team are not saved: after a restart he stands where he was)
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_EIGENAAR, tag.getStringOr("Eigenaar", ""));
        gen = tag.getIntOr("Gen", 0);
        kistStil = true;
        try {
            kist.clearContent();
            ContainerHelper.loadAllItems(tag, kist.getItems());
        } finally {
            kistStil = false;
        }
    }

    // --- GeckoLib -------------------------------------------------------------------------------------------------------------

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation VLIEG = RawAnimation.begin().thenLoop("vlieg");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<AppaGuhEntity>("beweeg", 6,
                state -> state.setAndContinue(isLucht() ? VLIEG : state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<AppaGuhEntity>("actie", 2, state -> PlayState.STOP)
                .triggerableAnim("jipjip", RawAnimation.begin().thenPlay("jipjip"))
                .triggerableAnim("brul", RawAnimation.begin().thenPlay("brul")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
