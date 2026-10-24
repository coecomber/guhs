package nl.juiced.guhs.mixin;

import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.GolemSensor;
import nl.juiced.guhs.feature.guhvillagers.GuhVillagersFeature;

/**
 * 1.7.0: vanilla's golem sensor only counts entities of the type minecraft:iron_golem. An IJzerguhlem in sight counts
 * too, exactly like an iron golem (else guh villagers would keep summoning new ones).
 */
@Mixin(GolemSensor.class)
public abstract class GuhGolemSensorMixin {
    @Inject(method = "checkForNearbyGolem", at = @At("TAIL"))
    private static void guhs$ijzerguhlem(LivingEntity body, CallbackInfo ci) {
        Optional<List<LivingEntity>> seen = body.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
        if (seen.isPresent() && seen.get().stream().anyMatch(e -> e.getType() == GuhVillagersFeature.IJZERGUHLEM.get())) {
            GolemSensor.golemDetected(body);
        }
    }
}
