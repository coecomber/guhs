package nl.juiced.guhs.feature.snuffelsteiger;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.snuffel.BewonerEntity;
import nl.juiced.guhs.feature.snuffel.Hondvorm;
import nl.juiced.guhs.feature.snuffel.SnuffelHond;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * Who lives at a steigerhuisje (entity {@code guhs:steiger_bewoner}): a dog of Het Snuffeleiland ({@link BewonerEntity}:
 * the approved models, the kern's renderer) with a ROLE of the dock instead of a role of the island:
 * <ul>
 *   <li>{@link #PUP}: the sick little brother or sister, Kleine Wiebel. Drawn as the puppy of whoever looks at it (the
 *   viewer's own breed and coat: it is THEIR family), lying in the sickbed;</li>
 *   <li>{@link #BUUR}: Buurvrouw Mandje, who watches over it (a golden retriever);</li>
 *   <li>{@link #KAPITEIN}: Kapitein Zoutsnoet at the end of the pier (the kern's named resident, with his cap and coat).</li>
 * </ul>
 * A click goes to {@link SteigerVerhaal#klik}; the kern's island roles never see these dogs ({@link #sleutel}). One
 * animation more than the kern's dogs have: {@code lig} (lying down, breathing: written into the puppies' animation files
 * by tools/features/snuffel_steiger.py), for the sickbed and for the collapse in the feast's cutscene, where this entity
 * type is the puppy's actor.
 */
public class SteigerBewoner extends BewonerEntity {
    public static final String PUP = "pup", BUUR = "buur", KAPITEIN = "kapitein";
    /** The cutscene animation (and the pose of the sick puppy): lying down. */
    public static final String LIG = "lig";
    private static final EntityDataAccessor<String> DATA_ROL = SynchedEntityData.defineId(SteigerBewoner.class, EntityDataSerializers.STRING);
    private static final RawAnimation LIGT = RawAnimation.begin().thenLoop(LIG);

    public SteigerBewoner(EntityType<? extends SteigerBewoner> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ROL, "");
    }

    /** {@link #PUP}, {@link #BUUR}, {@link #KAPITEIN}, or "" (an actor in a scene). */
    public String rol() {
        return entityData.get(DATA_ROL);
    }

    /** Gives this dog its role and the look that goes with it. */
    public void zetRol(String rol) {
        entityData.set(DATA_ROL, rol);
        switch (rol) {
            case PUP -> {
                zetHond("shiba", "rood", true);
                zetAlsSpeler(true);
                setCustomName(Component.translatable("entity.guhs.steiger_bewoner.pup"));
            }
            case BUUR -> {
                zetHond("golden", "rood", false);
                setCustomName(Component.translatable("entity.guhs.steiger_bewoner.buur"));
            }
            case KAPITEIN -> zetBewoner("kapitein");
            default -> {
            }
        }
    }

    /** Lying down: the sick puppy always, an actor when its scene says so. */
    public boolean ligt() {
        return PUP.equals(rol()) || (level().isClientSide() && LIG.equals(Cutscenes.animatie(this)));
    }

    /** Never one of the island's keys: the island's roles (the harbour captain's among them) are not for the dock. */
    @Override
    public String sleutel() {
        return "steiger_" + rol();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            SteigerVerhaal.klik(this, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isNoGravity() {
        return PUP.equals(rol()) || super.isNoGravity();   // (the puppy lies ON its bed, wherever the bed's blocks are)
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("Rol", rol());
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        String rol = in.getStringOr("Rol", "");
        if (!rol.isEmpty()) {
            zetRol(rol);
        }
    }

    /** The kern's own choice of animation ({@link SnuffelHond#registerControllers}), with lying down before everything else. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SteigerBewoner>("houding", 4, state -> {
            if (ligt()) {
                return state.setAndContinue(LIGT);
            }
            String scene = level().isClientSide() ? Cutscenes.animatie(this) : "";
            if (!scene.isEmpty()) {
                for (String naam : ANIMATIES) {
                    if (naam.equals(scene)) {
                        return state.setAndContinue(RawAnimation.begin().thenLoop(naam));
                    }
                }
            }
            boolean beweegt = loopt != null ? loopt : state.isMoving();
            int h = houding();
            String a;
            if (blafTot >= tickCount) {
                a = "blaf";
            } else if (beweegt) {
                a = "walk";
            } else if ((h & Hondvorm.ZIT) != 0) {
                a = "zit";
            } else if ((h & Hondvorm.KWISPELT) != 0) {
                a = "kwispel";
            } else {
                a = "idle";
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop(a));
        }));
    }
}
