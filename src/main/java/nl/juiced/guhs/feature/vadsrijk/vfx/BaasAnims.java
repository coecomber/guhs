package nl.juiced.guhs.feature.vadsrijk.vfx;

import java.util.List;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

import net.minecraft.world.entity.Entity;

/**
 * The boss animation standard (roadmap 3.1) on top of every boss's own "beweeg"/"actie" controllers: one extra GeckoLib
 * controller {@code "vfx"} with the generic moves every boss model gets from tools/features/vadsrijk_t_vfx.py (merged into its
 * animation file, on its own bones: root/body/head/... whatever the model has):
 * <ul>
 *   <li>{@code vfx_windup}: crouch and tremble, HELD until the next animation (the telegraph time);</li>
 *   <li>{@code vfx_treffer}: the pop of the hit;</li>
 *   <li>{@code vfx_geraakt}: a small flinch (rate limited by {@link BaasVfx});</li>
 *   <li>{@code vfx_fase}: rear up + shake (phase change);</li>
 *   <li>{@code vfx_woede}: the soft-enrage tremble;</li>
 *   <li>{@code vfx_juich}: the cheer-up finale (sways and waves; plays over the plof).</li>
 * </ul>
 * Other names go to the boss's own {@code "actie"} controller (only works for names in its actieAnimaties()).
 */
public final class BaasAnims {
    public static final String CONTROLLER = "vfx";
    public static final List<String> STANDAARD = List.of("vfx_windup", "vfx_treffer", "vfx_geraakt", "vfx_fase", "vfx_woede", "vfx_juich");

    private BaasAnims() {
    }

    /** Hook for a boss's registerControllers (BazenBaas, Z2Baas; T-VFX): adds the "vfx" controller. */
    public static <T extends GeoEntity> void controllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<T> c = new AnimationController<>(CONTROLLER, 2, state -> PlayState.STOP);
        c.triggerableAnim("vfx_windup", RawAnimation.begin().thenPlayAndHold("vfx_windup"));
        for (String a : STANDAARD) {
            if (!a.equals("vfx_windup")) {
                c.triggerableAnim(a, RawAnimation.begin().thenPlay(a));
            }
        }
        controllers.add(c);
    }

    /** Plays an animation on a boss (server side: GeckoLib syncs it). Nothing for a boss without GeckoLib (it gets {@link Vfx#pose}). */
    public static void speel(Entity e, String anim) {
        if (e instanceof GeoEntity g && anim != null && !anim.isEmpty() && !(e instanceof nl.juiced.guhs.feature.vadsrijk.bazen.mika.MikaSpion)) {   // (the Mika uses the shared guh animations: none of ours)
            g.triggerAnim(anim.startsWith("vfx_") ? CONTROLLER : "actie", anim);
        }
    }
}
