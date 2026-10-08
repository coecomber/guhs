package nl.juiced.guhs.feature.bio.bouwwolk1;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ballon.BallonFeature;
import nl.juiced.guhs.feature.ballon.BallonRoute;
import nl.juiced.guhs.feature.ballon.LuchtballonEntity;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The balloon of the Luchtballon-haven ({@code guhs:luchtballon_haven_ballon}): the guh balloon of the Ballonfestival
 * (same model, same colours, drawn by the same renderer) with another journey. It does not fly Kapitein Wolkje's round
 * routes: it takes ONE passenger from its mooring on the jetty down to a landing spot on the meadow below
 * ({@link Landing}), lets the passenger out and floats back up empty.
 * <p>
 * The path is one smooth curve from the mooring (out over the edge, then down) to the landing spot; both sides follow
 * it themselves from the synced mooring, landing spot and progress, as the festival balloon does.
 * <p>
 * What can go wrong, and what happens then (nobody is ever dropped, nothing is left behind):
 * <ul>
 *   <li>the landing spot got blocked while flying: the balloon carries its passenger back up and lets them out on the
 *       jetty ({@link #TERUG_MET}); the day's ride is given back;</li>
 *   <li>the passenger leaves the basket some other way (a teleport, a kill): slow falling, and the balloon goes home
 *       empty; sneaking does not get you out while it flies;</li>
 *   <li>the passenger logs out or the server stops ({@link Ballonvaarder#uitloggen}): they stand on the jetty again
 *       before they are saved, the balloon is home, the ride is given back;</li>
 *   <li>the balloon is removed in flight: its passenger floats down with slow falling; the ballonvaarder moors a new
 *       balloon when his is gone ({@link Ballonvaarder#tick});</li>
 *   <li>a crash in flight: the flight is not saved. A balloon that is loaded is at rest at its mooring (the parent
 *       snaps it home), with or without the player in the basket.</li>
 * </ul>
 */
public class HavenBallonEntity extends LuchtballonEntity {
    /** At its mooring / flying down with the passenger / floating back up empty / carrying the passenger back up. */
    public static final int RUST = 0, OMLAAG = 1, TERUG = 2, TERUG_MET = 3;
    private static final EntityDataAccessor<Integer> DATA_FASE = SynchedEntityData.defineId(HavenBallonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3fc> DATA_DOEL = SynchedEntityData.defineId(HavenBallonEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> DATA_VOORTGANG = SynchedEntityData.defineId(HavenBallonEntity.class, EntityDataSerializers.FLOAT);
    /** Blocks per tick at the fastest point of the curve. */
    public static final double SNELHEID = 0.2;
    /** (Tests) how much faster than normal a ride goes. */
    static double tempo = 1.0;

    /** How far along the curve, 0 (the mooring) to 1 (the landing spot). */
    private double voortgang;
    /** Server: the exact landing spot (the synced copy is floats). */
    private Vec3 doel = Vec3.ZERO;
    /** Server: true only while the passengers are let out. */
    boolean magUit;

    public HavenBallonEntity(EntityType<? extends HavenBallonEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FASE, RUST);
        builder.define(DATA_DOEL, new Vector3f());
        builder.define(DATA_VOORTGANG, 0f);
    }

    public int fase() {
        return entityData.get(DATA_FASE);
    }

    public boolean onderweg() {
        return fase() != RUST;
    }

    public double voortgang() {
        return voortgang;
    }

    public Vec3 doel() {
        if (!level().isClientSide()) {
            return doel;
        }
        Vector3fc d = entityData.get(DATA_DOEL);
        return new Vec3(d.x(), d.y(), d.z());
    }

    /** The way it leaves its mooring: where it looked when it was moored (out over the edge of the island). */
    public Vec3 uit() {
        return Vec3.directionFromRotation(0, routeYaw());
    }

    /** Where a passenger stands after stepping out at the mooring: on the jetty behind the basket. */
    public Vec3 steiger() {
        return thuis().add(uit().scale(-1.6)).add(0, 0.1, 0);
    }

    // --- the curve --------------------------------------------------------------------------------------------------

    /** The point of the curve from {@code van} (leaving towards {@code uit}) down to {@code naar}, at u in 0..1. */
    public static Vec3 punt(Vec3 van, Vec3 uit, Vec3 naar, double u) {
        double val = Math.max(4, van.y - naar.y);
        Vec3 b = van.add(uit.scale(9)).add(0, 2.5, 0);
        Vec3 c = naar.add(0, Math.min(16, val * 0.55), 0);
        double v = 1 - u;
        return van.scale(v * v * v).add(b.scale(3 * v * v * u)).add(c.scale(3 * v * u * u)).add(naar.scale(u * u * u));
    }

    /** The length of that curve (blocks). */
    public static double lengte(Vec3 van, Vec3 uit, Vec3 naar) {
        double l = 0;
        Vec3 vorig = van;
        for (int i = 1; i <= 24; i++) {
            Vec3 p = punt(van, uit, naar, i / 24.0);
            l += p.distanceTo(vorig);
            vorig = p;
        }
        return l;
    }

    /** How much of the curve one tick covers at u: slow at the mooring and at the landing, {@link #SNELHEID} in between. */
    static double stap(double u, double lengte) {
        return SNELHEID * tempo * (0.28 + 0.72 * Math.sin(Math.PI * Mth.clamp(u, 0, 1))) / Math.max(1, lengte);
    }

    // --- the ride ---------------------------------------------------------------------------------------------------

    /** Server: off with this player to this landing spot. False when it is not at rest, taken, or the player cannot get in. */
    public boolean vaar(ServerPlayer speler, Vec3 landing) {
        if (level().isClientSide() || onderweg() || isDeco() || !getPassengers().isEmpty() || speler.isPassenger()) {
            return false;
        }
        doel = landing;
        entityData.set(DATA_DOEL, new Vector3f((float) landing.x, (float) landing.y, (float) landing.z));
        zetVoortgang(0);
        if (!speler.startRiding(this, true, true)) {
            return false;
        }
        entityData.set(DATA_FASE, OMLAAG);
        level().playSound(null, this, BallonFeature.BRANDER.get(), SoundSource.NEUTRAL, 0.8f, 1.1f);
        return true;
    }

    private void zetVoortgang(double u) {
        voortgang = u;
        entityData.set(DATA_VOORTGANG, (float) u);
    }

    /** The festival's round flights are not for this balloon. */
    @Override
    public boolean stijgOp(ServerPlayer player, BallonRoute route, @Nullable GuhNpcEntity kapiteinNpc) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();            // (not "vliegt" for the parent: on the server it puts the balloon on its mooring, we move it from there)
        int fase = fase();
        if (fase == RUST) {
            return;
        }
        Vec3 van = thuis(), naar = doel();
        double stap = stap(voortgang, lengte(van, uit(), naar));
        voortgang = fase == OMLAAG ? Math.min(1, voortgang + stap) : Math.max(0, voortgang - stap * 1.5);
        setPos(punt(van, uit(), naar, voortgang));
        if (level().isClientSide()) {
            if (random.nextInt(fase == OMLAAG ? 10 : 4) == 0) {           // the burner: rarely going down, more going up
                level().addParticle(net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 2.6,
                        getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0.05, 0);
            }
            return;
        }
        if (tickCount % 10 == 0) {
            entityData.set(DATA_VOORTGANG, (float) voortgang);
        }
        if (tickCount % 90 == 0) {
            level().playSound(null, this, BallonFeature.WIND.get(), SoundSource.AMBIENT, 0.4f, 1f);
        }
        if (fase == OMLAAG) {
            if (getPassengers().isEmpty()) {
                entityData.set(DATA_FASE, TERUG);                       // nobody in it any more: home
            } else if (voortgang >= 1) {
                aankomst();
            }
        } else if (voortgang <= 0) {
            thuis(fase == TERUG_MET);
        }
    }

    /** Server: at the landing spot. Free: everybody out, the balloon goes back up. Blocked: back up with everybody in it. */
    private void aankomst() {
        ServerLevel level = (ServerLevel) level();
        if (!Landing.vrij(level, doel)) {
            entityData.set(DATA_FASE, TERUG_MET);
            return;
        }
        for (Entity e : java.util.List.copyOf(getPassengers())) {
            eruit(e, doel);
            if (e instanceof ServerPlayer speler) {
                Ballonvaarder.geland(speler);
            }
        }
        entityData.set(DATA_FASE, TERUG);
    }

    /** Server: back at the mooring; {@code met}: with the passengers it could not set down. */
    private void thuis(boolean met) {
        zetVoortgang(0);
        entityData.set(DATA_FASE, RUST);
        setPos(thuis());
        if (met) {
            for (Entity e : java.util.List.copyOf(getPassengers())) {
                eruit(e, steiger());
                if (e instanceof ServerPlayer speler) {
                    Ballonvaarder.terugGebracht(speler, "quest.guhs.luchtballon_haven.terug");
                }
            }
        }
    }

    private void eruit(Entity e, Vec3 naar) {
        magUit = true;
        try {
            e.stopRiding();
        } finally {
            magUit = false;
        }
        e.teleportTo(naar.x, naar.y, naar.z);
        e.setDeltaMovement(Vec3.ZERO);
        e.resetFallDistance();
    }

    /**
     * Server: stop the ride at once (the passenger logs out, the server stops): everybody stands on the jetty, the balloon
     * is at its mooring. Returns whether a ride was going on.
     */
    public boolean breekAf() {
        if (level().isClientSide() || !onderweg()) {
            return false;
        }
        zetVoortgang(0);
        entityData.set(DATA_FASE, RUST);
        setPos(thuis());
        for (Entity e : java.util.List.copyOf(getPassengers())) {
            eruit(e, steiger());
        }
        return true;
    }

    /** Out of the basket in the air after all (a teleport, a kill, the balloon removed): floating down, never falling. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && onderweg() && !magUit && passenger instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0, false, false));
            living.resetFallDistance();
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!level().isClientSide()) {
            return;
        }
        if (key == DATA_FASE) {
            if (fase() == RUST) {
                voortgang = 0;
            } else if (Math.abs(entityData.get(DATA_VOORTGANG) - voortgang) > 0.2) {
                voortgang = entityData.get(DATA_VOORTGANG);
            }
        } else if (key == DATA_VOORTGANG && onderweg() && Math.abs(entityData.get(DATA_VOORTGANG) - voortgang) > 0.06) {
            voortgang = entityData.get(DATA_VOORTGANG);
        }
    }

    /** While it is under way the client follows the curve itself; at rest it takes the server's position like the parent. */
    private final net.minecraft.world.entity.InterpolationHandler eigenInterpolatie = new net.minecraft.world.entity.InterpolationHandler(this, 0) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            if (!onderweg()) {
                setPos(position);
                setYRot(yRot % 360.0F);
                setXRot(xRot % 360.0F);
            }
        }
    };

    @Override
    public net.minecraft.world.entity.InterpolationHandler getInterpolation() {
        return eigenInterpolatie;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!level().isClientSide() && !onderweg() && player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.luchtballon_haven.praat").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    // --- where it may land --------------------------------------------------------------------------------------------

    /** The landing spot of a ride, and whether the way there is free. */
    public static final class Landing {
        /** The landing spot lies this far out from the mooring, and is looked for this far around that point. */
        public static final int UIT = 6, ROND = 3;
        /** A landing spot lies at least this far below the mooring (it is a ride DOWN, not onto something beside the jetty). */
        public static final int LAGER = 8;

        /**
         * The landing spot for a balloon moored at {@code thuis} that leaves towards {@code uit}: the nearest column
         * around the point {@link #UIT} blocks out with loaded, solid, dry ground well below the mooring and room for
         * the basket and its passenger. Null when there is none.
         */
        @Nullable
        public static Vec3 zoek(ServerLevel level, Vec3 thuis, Vec3 uit) {
            BlockPos midden = BlockPos.containing(thuis.add(uit.scale(UIT)));
            Vec3 beste = null;
            double besteD = Double.MAX_VALUE;
            for (int dx = -ROND; dx <= ROND; dx++) {
                for (int dz = -ROND; dz <= ROND; dz++) {
                    int x = midden.getX() + dx, z = midden.getZ() + dz;
                    if (!level.hasChunkAt(new BlockPos(x, midden.getY(), z))) {
                        continue;
                    }
                    // (looking down from the mooring, not asking the heightmap: what hangs above the jetty does not count)
                    BlockPos.MutableBlockPos kijk = new BlockPos.MutableBlockPos(x, net.minecraft.util.Mth.floor(thuis.y) - 1, z);
                    while (kijk.getY() > level.getMinY() && level.getBlockState(kijk).getCollisionShape(level, kijk).isEmpty()
                            && level.getFluidState(kijk).isEmpty()) {
                        kijk.move(0, -1, 0);
                    }
                    int y = kijk.getY() + 1;
                    Vec3 plek = new Vec3(x + 0.5, y, z + 0.5);
                    double d = dx * dx + dz * dz;
                    if (d < besteD && y <= thuis.y - LAGER && y > level.getMinY() + 1 && vrij(level, plek)) {
                        beste = plek;
                        besteD = d;
                    }
                }
            }
            return beste;
        }

        /** Can the basket stand here: something solid and dry under it, nothing in it. */
        public static boolean vrij(ServerLevel level, Vec3 plek) {
            BlockPos voet = BlockPos.containing(plek);
            if (!level.hasChunkAt(voet)) {
                return false;
            }
            BlockState onder = level.getBlockState(voet.below());
            if (onder.getCollisionShape(level, voet.below()).isEmpty() || !onder.getFluidState().isEmpty() || !level.getFluidState(voet).isEmpty()) {
                return false;
            }
            return level.noCollision(new AABB(plek.x - 0.7, plek.y + 0.05, plek.z - 0.7, plek.x + 0.7, plek.y + 2.6, plek.z + 0.7));
        }

        /** Is the curve from the mooring to the landing spot free of blocks (past the first steps beside the jetty)? */
        public static boolean wegVrij(ServerLevel level, Vec3 thuis, Vec3 uit, Vec3 landing) {
            for (int i = 6; i < 40; i++) {
                Vec3 p = punt(thuis, uit, landing, i / 40.0);
                if (!level.hasChunkAt(BlockPos.containing(p)) || !level.noCollision(new AABB(p.x - 0.7, p.y + 0.05, p.z - 0.7, p.x + 0.7, p.y + 2.6, p.z + 0.7))) {
                    return false;
                }
            }
            return true;
        }

        private Landing() {
        }
    }
}
