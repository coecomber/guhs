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
    /** Flying by himself: this far above the goal and above the ground under him, and from this close he comes down. */
    private static final double KRUISHOOGTE = 10, DAALAFSTAND = 14;
    /** Ticks without a key before he takes over a flight the player may steer. */
    public static final int LOSLATEN = 60;

    private static final EntityDataAccessor<String> DATA_EIGENAAR = SynchedEntityData.defineId(AppaGuhEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_ZELF = SynchedEntityData.defineId(AppaGuhEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LUCHT = SynchedEntityData.defineId(AppaGuhEntity.class, EntityDataSerializers.BOOLEAN);
    /** The entity ids of the passengers on the team seats, "12,40,7" (the client seats them by it). */
    private static final EntityDataAccessor<String> DATA_TEAM = SynchedEntityData.defineId(AppaGuhEntity.class, EntityDataSerializers.STRING);

    /** Seats, in his own space (x = to his left, y = up, z = forward), in blocks. */
    private static final Vec3 KOP = new Vec3(0, 2.44, 2.25);
    private static final Vec3[] MAND = {new Vec3(0.66, 2.5, 0.62), new Vec3(-0.66, 2.5, 0.62), new Vec3(0, 2.5, 0.05)};
    private static final Vec3[] TEAM = {new Vec3(0.82, 2.5, -0.1), new Vec3(-0.82, 2.5, -0.1), new Vec3(0, 2.5, 0.82),
            new Vec3(0.5, 2.5, -0.62), new Vec3(-0.5, 2.5, -0.62)};

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final SimpleContainer kist = new SimpleContainer(KIST_VAKKEN) {
        @Override
        public void setChanged() {
            super.setChanged();
            kistVeranderd();
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
    /** Server: the flight he is on (null: none). */
    @Nullable
    private Vlucht vlucht;
    /** His yaw at the start of this tick (his passengers turn along with him by the difference). */
    private float vorigeYaw;

    /** A flight to a spot: who it is for, whether they may steer, and how it is going. */
    private static final class Vlucht {
        final UUID speler;
        final Vec3 doel;
        final boolean spelerStuurt;
        int stil;
        int klok;
        double beste = Double.MAX_VALUE;
        int vast;

        Vlucht(UUID speler, Vec3 doel, boolean spelerStuurt) {
            this.speler = speler;
            this.doel = doel;
            this.spelerStuurt = spelerStuurt;
        }
    }

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
        if (kistStil || level().isClientSide()) {
            return;
        }
        ServerPlayer baas = baas();
        if (baas != null && kistGelezen) {
            Appa.schrijfKist(baas, kist);
        }
    }

    /** His owner, when online. */
    @Nullable
    ServerPlayer baas() {
        UUID e = eigenaar();
        return e == null || level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(e);
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
        return position().add(kanten[0].yRot(-getYRot() * Mth.DEG_TO_RAD));     // (in the air: beside him, they float down)
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
        double speed = rider.isSprinting() ? SPRINT : SNELHEID;
        Vec3 want = look.scale(input.z * speed).add(links.scale(input.x * speed));
        if (op) {
            want = want.add(0, STIJGEN, 0);
        }
        if (neer) {
            want = want.add(0, -STIJGEN, 0);
        }
        vlieg(getDeltaMovement().scale(0.86).add(want.scale(0.14)));
    }

    /** One tick of flight with this motion (he never sinks out of the world). */
    private void vlieg(Vec3 motion) {
        if (getY() + motion.y < level().getMinY() + 2 && motion.y < 0) {
            motion = new Vec3(motion.x, 0, motion.z);
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        calculateEntityAnimation(true);
        resetFallDistance();
    }

    /** Nobody at the reins: on the ground he stands; in the air he sinks gently when there is ground below, else he hangs. */
    private void zweef() {
        if (onGround() || level().isClientSide()) {
            super.travel(Vec3.ZERO);
            return;
        }
        Vec3 m = getDeltaMovement();
        vlieg(new Vec3(m.x * 0.8, grondOnder(64) ? -0.14 : 0, m.z * 0.8));
    }

    private boolean grondOnder(double diep) {
        Vec3 van = position().add(0, 0.2, 0);
        return level().clip(new ClipContext(van, van.add(0, -diep, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this)).getType()
                != HitResult.Type.MISS;
    }

    // --- flying by himself (the server) --------------------------------------------------------------------------------------

    /**
     * Server: flies this player (who must sit on him) to {@code doel} and lands there. With {@code spelerStuurt} he only
     * points the way until the player lets go for {@value #LOSLATEN} ticks (and hands the reins back when they steer again);
     * the last few blocks he always does himself. Replaces a flight that was going on.
     */
    void vliegNaar(ServerPlayer speler, Vec3 doel, boolean spelerStuurt) {
        vlucht = new Vlucht(speler.getUUID(), doel, spelerStuurt);
        entityData.set(DATA_ZELF, !spelerStuurt);
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
            stopVlucht();                          // nobody to fly for any more
            return;
        }
        v.klok++;
        double dx = v.doel.x - getX(), dz = v.doel.z - getZ();
        double ver = Math.sqrt(dx * dx + dz * dz);
        boolean zelf = true;
        if (v.spelerStuurt && !(ver < 6 && Math.abs(v.doel.y - getY()) < 10)) {
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

    /** One tick of his own flight: up to cruising height, towards the goal, down onto it. */
    private void vliegZelf() {
        Vlucht v = vlucht;
        double dx = v.doel.x - getX(), dz = v.doel.z - getZ();
        double ver = Math.sqrt(dx * dx + dz * dz);
        double dy = v.doel.y - getY();
        if (ver < 0.7 && (onGround() || Math.abs(dy) < 0.35)) {
            land();
            return;
        }
        double hoogte;
        double tempo = SNELHEID;
        if (ver > DAALAFSTAND) {
            hoogte = Math.max(v.doel.y + KRUISHOOGTE, grond() + KRUISHOOGTE * 0.6);
        } else {
            hoogte = v.doel.y + Math.max(0, ver - 1.5) * 0.7;        // the glide down
            tempo = Math.min(SNELHEID, 0.1 + ver * 0.05);
        }
        double vy = Mth.clamp((hoogte - getY()) * 0.12, -STIJGEN, STIJGEN);
        // still far below cruising height: mostly up first (out of a courtyard, over a wall)
        double vooruit = ver > DAALAFSTAND && hoogte - getY() > 4 ? tempo * 0.35 : tempo;
        Vec3 want = ver < 1e-3 ? new Vec3(0, vy, 0) : new Vec3(dx / ver * vooruit, vy, dz / ver * vooruit);
        if (horizontalCollision) {
            want = new Vec3(want.x, STIJGEN, want.z);              // something in the way: over it
        }
        vlieg(getDeltaMovement().scale(0.8).add(want.scale(0.2)));
        if (ver > 0.5) {
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            yaw = Mth.approachDegrees(getYRot(), yaw, 5f);
            setRot(yaw, 0);
            this.yBodyRot = this.yHeadRot = yaw;
        }
        // stuck for good (a cave roof, a cage)? then he "knows a shortcut": straight to the goal
        double afstand = position().distanceTo(v.doel);
        if (afstand < v.beste - 0.5) {
            v.beste = afstand;
            v.vast = 0;
        } else if (++v.vast > 160) {
            setPos(v.doel.x, v.doel.y + 0.1, v.doel.z);
            setDeltaMovement(Vec3.ZERO);
            land();
        }
    }

    /** The ground height under him (the bottom of the world when there is none: the void between the islands). */
    private double grond() {
        if (!level().hasChunkAt(blockPosition())) {
            return level().getMinY();
        }
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
    }

    /** Server: he is there. */
    private void land() {
        Vlucht v = vlucht;
        vlucht = null;
        entityData.set(DATA_ZELF, false);
        setDeltaMovement(Vec3.ZERO);
        brul();
        ServerPlayer p = v == null || level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(v.speler);
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
