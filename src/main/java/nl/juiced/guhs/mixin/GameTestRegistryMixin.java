package nl.juiced.guhs.mixin;

import java.lang.reflect.Method;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.gametest.framework.GameTestRegistry;
import nl.juiced.guhs.gametest.GametestFilter;

/** The gametest filter (-Pgt=..., see {@link GametestFilter}): test methods that don't pass it are never registered. */
@Mixin(GameTestRegistry.class)
public abstract class GameTestRegistryMixin {
    @Inject(method = "register(Ljava/lang/reflect/Method;Ljava/util/Set;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void guhs$onlyTheChosenTests(Method testMethod, Set<String> allowedNamespaces, CallbackInfo ci) {
        if (!GametestFilter.allowed(testMethod)) {
            ci.cancel();
        }
    }
}
