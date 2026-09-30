package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.Guhs;

/**
 * 1.1.0 (MC 26.1): {@code Entity#startRiding} on the server refuses every vehicle whose entity type is not saved
 * ({@code EntityType.Builder#noSave()}). Guhs has several such vehicles that only exist while a game or ride lasts
 * (race guh, kart, guh seat, Balto sled, surf board, zwembandje, ...); in 1.0.0 you could ride them and they were never
 * written to the world. This lets guhs entity types pass that one check, so they stay rideable and unsaved as before.
 */
@Mixin(Entity.class)
public abstract class EntityRidingMixin {
    @WrapOperation(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean guhs$rideUnsavedGuhsVehicles(EntityType<?> type, Operation<Boolean> original) {
        return original.call(type) || Guhs.MODID.equals(EntityType.getKey(type).getNamespace());
    }
}
