package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import nl.juiced.guhs.feature.guhvillagers.GuhVillagersFeature;
import nl.juiced.guhs.registry.ModVillagers;

/**
 * 1.7.0: a guh villager summons an IJzerguhlem instead of an iron golem. Only the entity type handed to
 * SpawnUtil#trySpawnMob changes: the gossip/panic/sleep checks, the number of villagers that must agree, the spawn
 * attempts and the placement strategy stay exactly vanilla's (so iron farms with guh villagers work the same).
 */
@Mixin(Villager.class)
public abstract class GuhGolemMixin {
    @ModifyArg(method = "spawnGolemIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/SpawnUtil;trySpawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/util/SpawnUtil$Strategy;Z)Ljava/util/Optional;"), index = 0)
    private EntityType<?> guhs$ijzerguhlem(EntityType<?> type) {
        return ModVillagers.isGuhVillager((Villager) (Object) this) ? GuhVillagersFeature.IJZERGUHLEM.get() : type;
    }
}
