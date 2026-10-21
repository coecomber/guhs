package nl.juiced.guhs.feature.safari.rit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * The open safari truck (S3, design 1.7; made longer in round 3): it drives the route loop of the park and stands at every
 * stop for a while (hop on, hop off: a click gets you in, sneaking gets you out). A guide guh drives and tells a fact (and
 * asks a quiz question, {@link SafariQuiz}) at every stop; behind the guide are three roomy rows of two fixed seats. A few
 * visitor guhs ride along looking out with their binoculars, but {@link #VRIJ} seats always stay free for players; your
 * own guhs walk and run along beside the truck (they are put next to it again when they fall more than
 * {@link #BIJ_MAX} blocks behind). Riders cannot get hurt (see {@link Ritten}). It opens the
 * {@link SafariPoortBlock safaripoorten} on its way. Model: geckolib safari_rit/safaritruck (the phase-0 model stretched,
 * tools/features/safari_rit.py).
 */
public class TruckEntity extends SchemaRit {
    /** Seats for players and visitors (the guide's not counted); at least VRIJ of them stay free for players. */
    public static final int STOELEN = 6, VRIJ = 2;
    /** Own guhs that fall further behind than this are put next to the truck again. */
    public static final double BIJ_MAX = 24;
    private static final EntityDataAccessor<Integer> DATA_BEZOEKERS = SynchedEntityData.defineId(TruckEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_NACHT = SynchedEntityData.defineId(TruckEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_STIL = SynchedEntityData.defineId(TruckEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * The seats (model pixels: x, seat surface y, z; front = -z): 0 is the driver (the guide), then three rows of two
     * (tools/features/safari_rit.py TRUCK_STOELEN has the same numbers).
     */
    public static final double[][] ZIT = {{-11.5, 15, -17.5}, {-11.5, 16, 1.5}, {11.5, 16, 1.5}, {-11.5, 18, 21.5}, {11.5, 18, 21.5},
            {-11.5, 20, 41.5}, {11.5, 20, 41.5}};
    /** Which seats players take, in order: from the front (the visitors fill the others from the back). */
    public static final int[] SPELER_ZIT = {1, 2, 3, 4, 5, 6};

    private int vorigeHalte = -1;
    /** Server: how many loops it has done (the guide picks another fact every loop). */
    int rondes;

    public TruckEntity(EntityType<? extends TruckEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BEZOEKERS, 2);
        builder.define(DATA_NACHT, false);
        builder.define(DATA_STIL, false);
    }

    public int bezoekers() {
        return entityData.get(DATA_BEZOEKERS);
    }

    public boolean nacht() {
        return entityData.get(DATA_NACHT);
    }

    public boolean staatStil() {
        return entityData.get(DATA_STIL);
    }

    public void zetNacht(boolean nacht) {
        if (nacht != nacht()) {
            entityData.set(DATA_NACHT, nacht);
        }
    }

    /** The visitor guhs it carries when nobody rides (2-4 by its number). */
    public int basisBezoekers() {
        return 2 + Math.floorMod(k() * 7 + 3, 3);
    }

    /** How many visitors ride along with this many players: never so many that fewer than {@link #VRIJ} seats stay free. */
    public static int bezoekers(int basis, int spelers) {
        return Math.max(0, Math.min(basis, STOELEN - VRIJ - spelers));
    }

    /** The seat (index in {@link #ZIT}) of the i-th player aboard. */
    public static int spelerStoel(int i) {
        return SPELER_ZIT[Mth.clamp(i, 0, SPELER_ZIT.length - 1)];
    }

    /** The seats the visitors sit in with this many players aboard: from the back, never a player's seat. */
    public static List<Integer> bezoekerStoelen(int spelers, int bezoekers) {
        List<Integer> vrij = new ArrayList<>();
        for (int i = SPELER_ZIT.length - 1; i >= spelers && vrij.size() < bezoekers; i--) {
            vrij.add(SPELER_ZIT[i]);
        }
        return vrij;
    }

    /** The seats the visitors sit in now. */
    public List<Integer> bezoekerStoelen() {
        return bezoekerStoelen((int) spelers(), bezoekers());
    }

    @Override
    protected void plaats(long t, boolean snap) {
        RitData d = RitData.get();
        if (d.route == null) {
            return;
        }
        RitSchema.Stand s = RitSchema.truck(t, k(), n(), d.route.lengte(), Ritten.halteAfstanden());
        zet(d.route.op(s.afstand()), d.route.richting(s.afstand(), getYRot()), snap);
        if (!level().isClientSide()) {
            if (s.staatStil() != staatStil()) {
                entityData.set(DATA_STIL, s.staatStil());
            }
            if (s.staatStil() && s.halte() != vorigeHalte && !snap) {
                if (s.halte() == 0) {
                    rondes++;
                }
                Ritten.aanDeHalte(this, d.haltes.get(s.halte()));
                level().playSound(null, this, nl.juiced.guhs.feature.safari.spullen.SafariGeluid.TRUCK_TOETER.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.9f, 1.0f);
            }
            vorigeHalte = s.halte();
            int b = bezoekers(basisBezoekers(), (int) spelers());
            if (b != bezoekers()) {
                entityData.set(DATA_BEZOEKERS, b);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer p) {
            if (spelers() >= STOELEN) {
                p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.safari.rit.vol"));
            } else if (p.startRiding(this)) {
                p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.safari.rit.instappen"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && spelers() < STOELEN;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 10 == 0) {
            begeleid(sl);
        }
    }

    /**
     * The riders' own guhs walk (run, when it drives) along beside the truck, in pairs left and right; one that fell
     * more than {@link #BIJ_MAX} behind (or got stuck) is put next to it again.
     */
    private void begeleid(ServerLevel level) {
        int n = 0;
        for (Entity e : getPassengers()) {
            if (!(e instanceof ServerPlayer p)) {
                continue;
            }
            UUID id = p.getUUID();
            for (GuhEntity g : level.getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(64, 24, 64),
                    g -> g.isTame() && id.equals(g.getOwnerUUID()) && !g.isOrderedToSit() && !g.isPassenger() && g.isAlive())) {
                double kant = n % 2 == 0 ? -44 : 44;
                Vec3 plek = position().add(stoel(kant, 0, -8 + (n / 2) * 18.0));
                n++;
                if (g.distanceToSqr(plek) > BIJ_MAX * BIJ_MAX) {
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(plek.x), Mth.floor(plek.z));
                    g.getNavigation().stop();
                    g.teleportTo(plek.x, Math.max(y, Mth.floor(plek.y)), plek.z);
                } else {
                    g.getNavigation().moveTo(plek.x, plek.y, plek.z, staatStil() ? 1.0 : 1.7);
                }
            }
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int i = 0;
        for (Entity e : getPassengers()) {
            if (e == passenger) {
                break;
            }
            if (e instanceof Player) {
                i++;
            }
        }
        double[] z = ZIT[spelerStoel(i)];
        return stoel(z[0], z[1] - 8, z[2]);
    }

    // --- GeckoLib ---------------------------------------------------------------------------------------------------------------

    private static final RawAnimation RIJDEN = RawAnimation.begin().thenLoop("animation.safaritruck.rijden");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("wielen", 3, state -> staatStil() ? com.geckolib.animation.object.PlayState.STOP
                : state.setAndContinue(RIJDEN)));
    }
}
