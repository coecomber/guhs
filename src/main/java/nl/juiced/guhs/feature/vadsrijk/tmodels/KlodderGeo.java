package nl.juiced.guhs.feature.vadsrijk.tmodels;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.world.entity.Entity;

/**
 * T-MODELS: the GeckoLib look of kern's slimes, which are plain {@code VadsrijkMob}s (no GeoEntity) and used the vanilla slime
 * model. A {@link GeoReplacedEntity} gives them a GeckoLib model without touching the entity classes:
 * <ul>
 *   <li>{@link #REUS}: the Grauwe Klodderreus (model vadsrijk/tmodels/grauwe_klodderreus). Main controller: idle, sprong (in the
 *   air), land (just landed), rol (rolling), blij (after the plof). Trigger controller "actie": spuug, brul, aanloop
 *   ({@link #speel}, synced from the server).</li>
 *   <li>{@link #KLEIN}: the proefklodder and the kloddertjes he spits (the zone-1 klodder model): idle, hop, blij.</li>
 * </ul>
 * The client fills the data tickets in {@code client.KlodderGeoRenderer#addRenderData}.
 */
public final class KlodderGeo implements GeoReplacedEntity {
    public static final KlodderGeo REUS = new KlodderGeo(true);
    public static final KlodderGeo KLEIN = new KlodderGeo(false);

    public static final DataTicket<Boolean> GEPLOFD = DataTicket.create("guhs_tm_geplofd", Boolean.class);
    public static final DataTicket<Boolean> LUCHT = DataTicket.create("guhs_tm_lucht", Boolean.class);
    public static final DataTicket<Boolean> LAND = DataTicket.create("guhs_tm_land", Boolean.class);
    public static final DataTicket<Boolean> ROLT = DataTicket.create("guhs_tm_rolt", Boolean.class);

    /** The trigger animations of the Klodderreus (controller "actie"). */
    public static final String[] ACTIES = {"spuug", "brul", "aanloop"};

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SPRONG = RawAnimation.begin().thenLoop("sprong");
    private static final RawAnimation LANDT = RawAnimation.begin().thenPlayAndHold("land");
    private static final RawAnimation ROL = RawAnimation.begin().thenLoop("rol");
    private static final RawAnimation BLIJ = RawAnimation.begin().thenLoop("blij");
    private static final RawAnimation HOP = RawAnimation.begin().thenLoop("hop");

    private final boolean reus;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private KlodderGeo(boolean reus) {
        this.reus = reus;
    }

    /** Common setup: the giant's trigger animations are synced (server -> client). */
    public static void init() {
        SingletonGeoAnimatable.registerSyncedAnimatable(REUS);
    }

    /** Server side: play a trigger animation ({@link #ACTIES}) on a Klodderreus for everyone who sees him. */
    public static void speel(Entity reus, String actie) {
        if (!reus.level().isClientSide()) {
            REUS.triggerAnim(reus, "actie", actie);
        }
    }

    public boolean reus() {
        return reus;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<KlodderGeo>("main", 3, test -> {
            if (test.getDataOrDefault(GEPLOFD, false)) {
                return test.setAndContinue(BLIJ);
            }
            boolean lucht = test.getDataOrDefault(LUCHT, false);
            if (!reus) {
                return test.setAndContinue(lucht || test.isMoving() ? HOP : IDLE);
            }
            if (lucht) {
                return test.setAndContinue(SPRONG);
            }
            if (test.getDataOrDefault(LAND, false)) {
                return test.setAndContinue(LANDT);
            }
            return test.setAndContinue(test.getDataOrDefault(ROLT, false) ? ROL : IDLE);
        }));
        if (reus) {
            AnimationController<KlodderGeo> actie = new AnimationController<>("actie", 0, test -> PlayState.STOP);
            for (String a : ACTIES) {
                actie.triggerableAnim(a, "aanloop".equals(a) ? RawAnimation.begin().thenPlay(a).thenPlay(a).thenPlay(a)
                        : RawAnimation.begin().thenPlay(a));
            }
            controllers.add(actie);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
