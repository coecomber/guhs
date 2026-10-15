package nl.juiced.guhs.feature.park.plek;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * 1.4.2 (Het Knabbelpark, de speeltuin): the invisible seat you sit on on a swing ({@link SpeelBlokken.Schommel}) or a
 * seesaw ({@link SpeelBlokken.Wip}). The server moves it every tick, the client glides after it (an interpolation of two
 * ticks), and the rider goes along:
 * <ul>
 *   <li>swing: a pendulum on ropes of {@link #TOUW} blocks under the bar two blocks above the swing block, towards FACING
 *       and back, slowly swinging up to {@link #SCHOMMEL_GRADEN} degrees; the block shows the seat at the nearest angle;</li>
 *   <li>seesaw: its end ({@code kant} +1 = the end towards FACING) goes up and down with the game time, so both ends of a
 *       seesaw always move against each other; the block shows which end is up.</li>
 * </ul>
 * Gone as soon as nobody sits on it (or its block is gone); then the block is put back at rest.
 */
public class ParkZitje extends Entity {
    public enum Soort { SCHOMMEL, WIP }

    /** The rope length of a swing (blocks, from the bar to the seat) and its largest angle. */
    public static final double TOUW = 1.625, SCHOMMEL_GRADEN = 42;
    /** Ticks of one swing (there and back) and of one seesaw rock; ticks to swing up to full height. */
    public static final int SCHOMMEL_PERIODE = 56, WIP_PERIODE = 48, AANLOOP = 60;
    /** The seesaw: the seat height at rest (blocks above the block) and how far it goes up and down. */
    public static final double WIP_ZIT = 0.62, WIP_SLAG = 0.42;
    /** Where the block's angle flips (the seesaw: the share of the swing where the plank is drawn tilted). */
    public static final double WIP_KANTEL = 0.3;

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 2);
    private BlockPos anker = BlockPos.ZERO;
    private Soort soort = Soort.SCHOMMEL;
    private int kant;
    private long begin;

    public ParkZitje(EntityType<? extends ParkZitje> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // --- making one ----------------------------------------------------------------------------------------------------

    /** The seat on this anchor (a swing: kant 0; a seesaw: its end), or null. */
    @Nullable
    public static ParkZitje op(ServerLevel level, BlockPos anker, int kant) {
        for (ParkZitje z : SpeelBlokken.zitjes(level, anker)) {
            if (z.kant == kant) {
                return z;
            }
        }
        return null;
    }

    /** A new seat on this swing or seesaw end, with the player on it (server). */
    @Nullable
    public static ParkZitje zet(ServerLevel level, BlockPos anker, Soort soort, int kant, Player player) {
        ParkZitje z = SpeelBlokken.nieuwZitje(level);
        if (z == null) {
            return null;
        }
        z.anker = anker.immutable();
        z.soort = soort;
        z.kant = kant;
        z.begin = level.getGameTime();
        Vec3 p = z.plek(level.getBlockState(anker), 0);
        z.snapTo(p.x, p.y, p.z, z.kijk(level.getBlockState(anker)), 0f);
        level.addFreshEntity(z);
        player.startRiding(z);
        if (player instanceof ServerPlayer sp) {
            GuhAdvancements.grant(sp, soort == Soort.SCHOMMEL ? "parkplek_schommel" : "parkplek_wip");
        }
        return z;
    }

    public BlockPos anker() {
        return anker;
    }

    public Soort soort() {
        return soort;
    }

    public int kant() {
        return kant;
    }

    // --- moving --------------------------------------------------------------------------------------------------------

    /** The swing's angle now (radians, positive = towards FACING), {@code t} ticks after it started. */
    public static double hoek(long t) {
        double amp = Math.toRadians(SCHOMMEL_GRADEN) * Math.min(1.0, t / (double) AANLOOP);
        return amp * Math.sin(t * 2 * Math.PI / SCHOMMEL_PERIODE);
    }

    /** The seesaw's swing now (-1..1, positive = the end towards FACING is up). */
    public static double wip(long gameTime) {
        return Math.sin(gameTime * 2 * Math.PI / WIP_PERIODE);
    }

    /** The block state that goes with this swing angle. */
    public static int stand(double hoek) {
        int s = (int) Math.round(Math.toDegrees(hoek) / SpeelBlokken.STAND_GRADEN) + SpeelBlokken.STAND_RECHT;
        return Math.max(0, Math.min(6, s));
    }

    /** The block state that goes with this seesaw swing. */
    public static SpeelBlokken.Kant kantel(double wip) {
        return wip > WIP_KANTEL ? SpeelBlokken.Kant.A : wip < -WIP_KANTEL ? SpeelBlokken.Kant.B : SpeelBlokken.Kant.VLAK;
    }

    private Direction richting(BlockState state) {
        return state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    /** Where the seat is now ({@code t}: ticks since it started). */
    Vec3 plek(BlockState state, long t) {
        Direction f = richting(state);
        Vec3 midden = Vec3.atBottomCenterOf(anker);
        if (soort == Soort.SCHOMMEL) {
            double h = hoek(t);
            double uit = TOUW * Math.sin(h);
            return new Vec3(midden.x + f.getStepX() * uit, anker.getY() + 2.0 - TOUW * Math.cos(h), midden.z + f.getStepZ() * uit);
        }
        double y = anker.getY() + WIP_ZIT + kant * WIP_SLAG * wip(level().getGameTime());
        return new Vec3(midden.x + f.getStepX() * kant * SpeelBlokken.Wip.ARM, y, midden.z + f.getStepZ() * kant * SpeelBlokken.Wip.ARM);
    }

    /** Which way the rider looks: forward on a swing, to the middle on a seesaw. */
    float kijk(BlockState state) {
        Direction f = richting(state);
        return (soort == Soort.SCHOMMEL || kant < 0 ? f : f.getOpposite()).toYRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolation.interpolate();
            return;
        }
        BlockState state = level().getBlockState(anker);
        boolean blok = soort == Soort.SCHOMMEL ? state.getBlock() instanceof SpeelBlokken.Schommel : state.getBlock() instanceof SpeelBlokken.Wip;
        if (!isVehicle() || !blok) {
            klaar();
            return;
        }
        long t = level().getGameTime() - begin;
        Vec3 p = plek(state, t);
        setPos(p.x, p.y, p.z);
        setYRot(kijk(state));
        BlockState nieuw = soort == Soort.SCHOMMEL ? state.setValue(SpeelBlokken.STAND, stand(hoek(t)))
                : state.setValue(SpeelBlokken.KANT, kantel(wip(level().getGameTime())));
        if (nieuw != state) {
            level().setBlock(anker, nieuw, 2);
        }
    }

    /** Nobody on it any more: the block at rest (unless the other end of a seesaw is still taken), the seat gone. */
    private void klaar() {
        ejectPassengers();
        BlockState state = level().getBlockState(anker);
        if (level() instanceof ServerLevel server) {
            boolean ander = SpeelBlokken.zitjes(server, anker).stream().anyMatch(z -> z != this && z.isVehicle());
            if (state.getBlock() instanceof SpeelBlokken.Schommel && state.getValue(SpeelBlokken.STAND) != SpeelBlokken.STAND_RECHT) {
                level().setBlock(anker, state.setValue(SpeelBlokken.STAND, SpeelBlokken.STAND_RECHT), 2);
            } else if (state.getBlock() instanceof SpeelBlokken.Wip && !ander && state.getValue(SpeelBlokken.KANT) != SpeelBlokken.Kant.VLAK) {
                level().setBlock(anker, state.setValue(SpeelBlokken.KANT, SpeelBlokken.Kant.VLAK), 2);
            }
        }
        discard();
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return Vec3.ZERO;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        passenger.setYBodyRot(getYRot());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockState state = level().getBlockState(anker);
        Direction f = richting(state);
        BlockPos uit = soort == Soort.SCHOMMEL ? anker.relative(f.getOpposite()) : anker.relative(f, kant).relative(f.getClockWise());
        return Vec3.atBottomCenterOf(uit);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        anker = BlockPos.of(tag.getLongOr("Anker", 0L));
        soort = tag.getIntOr("Soort", 0) == 1 ? Soort.WIP : Soort.SCHOMMEL;
        kant = tag.getIntOr("Kant", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        tag.putLong("Anker", anker.asLong());
        tag.putInt("Soort", soort.ordinal());
        tag.putInt("Kant", kant);
    }
}
