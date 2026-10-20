package nl.juiced.guhs.feature.safari.rit;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import net.minecraft.network.chat.Component;
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
 * A gondola of the kabelbaan IJskust - Bamboebergen (S3): two of them shuttle along the cable, one at each end, and
 * wait a while in every station. Get in from the station deck (a click); you only get out in a station (sneaking under
 * way does nothing: a long way down). Model: geckolib safari_rit/gondel (tools/features/safari_rit.py).
 */
public class GondelEntity extends SchemaRit {
    public static final int PLAATSEN = 4, GUH_PLAATSEN = 2;
    private static final EntityDataAccessor<Integer> DATA_STATION = SynchedEntityData.defineId(GondelEntity.class, EntityDataSerializers.INT);

    public GondelEntity(EntityType<? extends GondelEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATION, 0);
    }

    /** The station it waits in (0 = IJskust, 1 = Bamboebergen), -1 under way. */
    public int station() {
        return entityData.get(DATA_STATION);
    }

    @Override
    protected void plaats(long t, boolean snap) {
        RitData d = RitData.get();
        if (d.kabelA == null || d.kabelB == null) {
            return;
        }
        double[] g = RitSchema.gondel(t, k(), d.kabelA.distanceTo(d.kabelB));
        Vec3 p = d.kabelA.lerp(d.kabelB, g[0]);
        Vec3 r = d.kabelB.subtract(d.kabelA);
        float yaw = (float) Math.toDegrees(Math.atan2(-r.x, r.z));
        zet(p, yaw, true);
        if (!level().isClientSide() && (int) g[1] != station()) {
            entityData.set(DATA_STATION, (int) g[1]);
            if (g[1] >= 0) {
                for (Entity e : getPassengers()) {
                    if (e instanceof ServerPlayer sp) {
                        sp.sendOverlayMessage(Component.translatable("gui.guhs.safari.kabel.aangekomen",
                                Component.translatable(g[1] == 0 ? "gui.guhs.safari.zone.ijskust" : "gui.guhs.safari.zone.bamboe")));
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer p) {
            if (station() < 0) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.safari.kabel.onderweg"));
            } else if (spelers() >= PLAATSEN) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.safari.rit.vol"));
            } else if (p.startRiding(this)) {
                neemGuhsMee(p, GUH_PLAATSEN);
                p.sendOverlayMessage(Component.translatable("gui.guhs.safari.kabel.instappen"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (passenger instanceof GuhEntity) {
            return guhs() < GUH_PLAATSEN;
        }
        return passenger instanceof Player && spelers() < PLAATSEN;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int i = Math.max(0, getPassengers().indexOf(passenger));
        double x = (i % 2 == 0) ? -5 : 5, z = (i / 2 % 2 == 0) ? -5 : 5;
        return stoel(x, 1, z);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    private static final RawAnimation SCHOMMEL = RawAnimation.begin().thenLoop("animation.gondel.schommel");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("schommel", 5, state -> station() >= 0 ? PlayState.STOP : state.setAndContinue(SCHOMMEL)));
    }
}
