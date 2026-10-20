package nl.juiced.guhs.feature.safari.rit;

import java.util.ArrayList;
import java.util.List;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * The open safari truck (S3, design 1.7): it drives the route loop of the park and stands at every stop for a while
 * (hop on, hop off: a click gets you in, sneaking gets you out). A guide guh drives and tells a fact at every stop; a few
 * visitor guhs ride along looking out with their binoculars, but two seats always stay free for players; your own guhs
 * ride in the little trailer. Players can photograph from it (the camera just works). At a night safari it carries
 * lanterns. Model: geckolib safari_rit/safaritruck (phase-0 model + trailer, tools/features/safari_rit.py).
 */
public class TruckEntity extends SchemaRit {
    public static final int STOELEN = 7, GUH_STOELEN = 4;
    private static final EntityDataAccessor<Integer> DATA_BEZOEKERS = SynchedEntityData.defineId(TruckEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_NACHT = SynchedEntityData.defineId(TruckEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_STIL = SynchedEntityData.defineId(TruckEntity.class, EntityDataSerializers.BOOLEAN);

    /** The seats (model pixels: x, seat surface y, z; front = -z): 0 is the driver (the guide). */
    public static final double[][] ZIT = {{-10, 15, -20}, {10, 15, -20}, {-10, 15, -6}, {10, 15, -6}, {-10, 17, 8}, {10, 17, 8},
            {-10, 19, 21}, {10, 19, 21}};
    /** Which seats players take, in order (the visitors fill the others from the back). */
    public static final int[] SPELER_ZIT = {1, 2, 3, 5, 4, 6, 7};
    /** The trailer (guh seats). */
    public static final double[][] AANHANGER = {{-6, 10, 52}, {6, 10, 52}, {-6, 10, 62}, {6, 10, 62}};

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

    /** The visitor guhs it carries (2-4 by its number, fewer when players fill the seats; two always stay free). */
    public int basisBezoekers() {
        return 2 + Math.floorMod(k() * 7 + 3, 3);
    }

    /** The seats the visitors sit in now (not the players' seats). */
    public List<Integer> bezoekerStoelen() {
        int spelers = (int) spelers();
        List<Integer> vrij = new ArrayList<>();
        for (int i = SPELER_ZIT.length - 1; i >= spelers; i--) {
            vrij.add(SPELER_ZIT[i]);
        }
        return vrij.subList(0, Math.min(bezoekers(), vrij.size()));
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
            }
            vorigeHalte = s.halte();
            int b = (int) Math.min(basisBezoekers(), STOELEN - spelers());
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
                neemGuhsMee(p, GUH_STOELEN);
                p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.safari.rit.instappen"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (passenger instanceof GuhEntity) {
            return guhs() < GUH_STOELEN;
        }
        return passenger instanceof Player && spelers() < STOELEN;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        if (passenger instanceof GuhEntity) {
            int i = 0;
            for (Entity e : getPassengers()) {
                if (e == passenger) {
                    break;
                }
                if (e instanceof GuhEntity) {
                    i++;
                }
            }
            double[] z = AANHANGER[Math.min(i, AANHANGER.length - 1)];
            return stoel(z[0], z[1] - 4, z[2]);
        }
        int i = 0;
        for (Entity e : getPassengers()) {
            if (e == passenger) {
                break;
            }
            if (e instanceof Player) {
                i++;
            }
        }
        double[] z = ZIT[SPELER_ZIT[Math.min(i, SPELER_ZIT.length - 1)]];
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
