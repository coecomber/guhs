package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import nl.juiced.guhs.registry.ModVillagers;

/**
 * 26.1: villager trades are datapack trade sets and NeoForge's VillagerTradesEvent is gone. The guh professions keep
 * their 1.0.0 trade lists in code (ModVillagers): when a guh-profession villager gets its trades for a level, two
 * random ones of that level are added, exactly like 1.21.1's VillagerTradesEvent listings did.
 */
@Mixin(Villager.class)
public abstract class VillagerMixin {
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void guhs$guhTrades(ServerLevel level, CallbackInfo ci) {
        ModVillagers.addTrades((Villager) (Object) this);
    }
}
