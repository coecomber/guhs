package nl.juiced.guhs.feature.safari.rit;

import java.util.UUID;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.safari.SafariDier;

/**
 * The corner part of a {@link SafariPoortBlock}: draws the gate (GeckoLib, the doors swing by the OPEN state) and runs
 * it on the server, every {@link #ELKE} ticks:
 * <ul>
 *   <li>a truck that comes towards the gate (within {@link #TRUCK_VOOR} blocks) or is driving through it holds it
 *       open, and so do the own guhs of the players in a truck while they walk through;</li>
 *   <li>a gate opened by hand shuts itself after {@link #OPEN_MAX} ticks (when nothing holds it);</li>
 *   <li>a park animal or race ostrich that somehow stands in the gateway is put back on its own side.</li>
 * </ul>
 */
public class SafariPoortBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int ELKE = 4, OPEN_MAX = 200;
    /** How far ahead a truck opens the gate (4.4 blocks a second: the doors are open well before it gets there). */
    public static final double TRUCK_VOOR = 18, TRUCK_DOOR = 7, GUH_DOOR = 5;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Server: open by hand until (game time); held open by a truck / guhs until. */
    private long openTot, houdTot;
    /** Client: has it been open since it was loaded (else a shut gate just stands, without swinging shut first). */
    private boolean gezienOpen;

    public SafariPoortBlockEntity(BlockPos pos, BlockState state) {
        super(SafariRitten.POORT_BE.get(), pos, state);
    }

    public Direction.Axis as() {
        return getBlockState().getValue(SafariPoortBlock.AS);
    }

    public boolean open() {
        return getBlockState().getValue(SafariPoortBlock.OPEN);
    }

    /** The middle of the gateway (between part 1 and 2, at the bottom). */
    public Vec3 midden() {
        BlockPos p = getBlockPos();
        return as() == Direction.Axis.X ? new Vec3(p.getX() + 2.0, p.getY(), p.getZ() + 0.5) : new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 2.0);
    }

    /** A click: open (for {@link #OPEN_MAX} ticks) or shut (unless a truck is coming through). */
    void klik(ServerLevel level) {
        long t = level.getGameTime();
        if (open()) {
            openTot = 0;
            if (t >= houdTot) {
                SafariPoortBlock.zetOpen(level, getBlockPos(), false);
            }
        } else {
            openTot = t + OPEN_MAX;
            SafariPoortBlock.zetOpen(level, getBlockPos(), true);
        }
    }

    /** Test/admin: open by hand now. */
    public void openMetDeHand() {
        if (level instanceof ServerLevel sl && !open()) {
            klik(sl);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SafariPoortBlockEntity be) {
        long t = level.getGameTime();
        if (!(level instanceof ServerLevel sl) || Math.floorMod(t + pos.asLong(), ELKE) != 0) {
            return;
        }
        if (be.iemandKomtEraan(sl)) {
            be.houdTot = t + ELKE * 3L;
        }
        boolean moet = t < be.houdTot || t < be.openTot;
        if (moet != be.open()) {
            SafariPoortBlock.zetOpen(sl, pos, moet);
        }
        if (be.open()) {
            be.houdDierenTegen(sl);
        }
    }

    /**
     * Does a truck want to go through (coming towards the gate within {@link #TRUCK_VOOR}, or still in it), or are the
     * own guhs of a truck's riders walking through?
     */
    boolean iemandKomtEraan(ServerLevel level) {
        Vec3 m = midden();
        AABB rond = new AABB(m, m).inflate(TRUCK_VOOR, 6, TRUCK_VOOR);
        for (TruckEntity truck : level.getEntitiesOfClass(TruckEntity.class, rond)) {
            if (truckWilDoor(truck.position(), truck.getYRot(), m)) {
                return true;
            }
        }
        for (GuhEntity g : level.getEntitiesOfClass(GuhEntity.class, new AABB(m, m).inflate(GUH_DOOR, 4, GUH_DOOR))) {
            UUID baas = g.getOwnerUUID();
            Player p = baas == null ? null : level.getPlayerByUUID(baas);
            if (p != null && p.getVehicle() instanceof TruckEntity) {
                return true;
            }
        }
        return false;
    }

    /** Pure: a truck at pos heading yaw wants this gate open (it drives towards it, or its long body is still in it). */
    public static boolean truckWilDoor(Vec3 pos, float yaw, Vec3 poort) {
        Vec3 naar = poort.subtract(pos);
        double d = naar.horizontalDistance();
        if (d < TRUCK_DOOR) {
            return true;
        }
        if (d > TRUCK_VOOR || Math.abs(naar.y) > 6) {
            return false;
        }
        double vx = -Mth.sin(yaw * Mth.DEG_TO_RAD), vz = Mth.cos(yaw * Mth.DEG_TO_RAD);
        return (naar.x * vx + naar.z * vz) / d > 0.5;
    }

    /** The hard rule: no park animal or race ostrich stands in the open gateway; it goes back to the side it came from. */
    private void houdDierenTegen(ServerLevel level) {
        BlockPos p = getBlockPos();
        boolean langsX = as() == Direction.Axis.X;
        AABB gat = langsX ? new AABB(p.getX(), p.getY(), p.getZ() - 0.4, p.getX() + 4, p.getY() + 5, p.getZ() + 1.4)
                : new AABB(p.getX() - 0.4, p.getY(), p.getZ(), p.getX() + 1.4, p.getY() + 5, p.getZ() + 4);
        double vlak = langsX ? p.getZ() + 0.5 : p.getX() + 0.5;
        for (Entity e : level.getEntitiesOfClass(Entity.class, gat, e -> e instanceof SafariDier || e instanceof RaceStruisvogelEntity)) {
            double c = langsX ? e.getZ() : e.getX();
            double kant = c >= vlak ? 1 : -1;
            if (Math.abs(c - vlak) < 0.05) {
                Vec3 v = e.getDeltaMovement();
                kant = -Math.signum(langsX ? v.z : v.x);
                kant = kant == 0 ? 1 : kant;
            }
            double nieuw = vlak + kant * (0.9 + e.getBbWidth() / 2);
            if (langsX) {
                e.teleportTo(e.getX(), e.getY(), nieuw);
            } else {
                e.teleportTo(nieuw, e.getY(), e.getZ());
            }
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------------

    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("animation.safaripoort.open");
    private static final RawAnimation DICHT = RawAnimation.begin().thenPlayAndHold("animation.safaripoort.dicht");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("deuren", 0, state -> {
            if (open()) {
                gezienOpen = true;
            } else if (!gezienOpen) {
                return com.geckolib.animation.object.PlayState.STOP;   // shut since it was loaded: the rest pose, no swing
            }
            return state.setAndContinue(open() ? OPEN : DICHT);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
