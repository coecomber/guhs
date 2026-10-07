package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.verhaal.Sluiers;

/**
 * bbq2 phase 3 (PHASE3 R06, the rest of CONTRACT_130 13.9): particles the server sends from a spot inside a Guhdalfs
 * sluier are not sent to a player for whom that sluier is closed, like the creatures ({@link RingZichtMixin}) and the
 * sounds ({@link RingSluierGeluidMixin}) in there. Every {@code ServerLevel#sendParticles} ends in this one private
 * method per player; only in a dimension that has a sluier at all the one extra question is asked
 * ({@link Sluiers#magHoren}: a spot, a player; spectators and creative operators see everything).
 */
@Mixin(ServerLevel.class)
public abstract class RingSluierDeeltjesMixin {
    @Inject(method = "sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z", at = @At("HEAD"), cancellable = true)
    private void guhs$nietDoorDeSluier(ServerPlayer player, boolean ver, double x, double y, double z, Packet<?> packet, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (Sluiers.heeftZones(level.dimension()) && !Sluiers.magHoren(player, level, x, y, z)) {
            cir.setReturnValue(false);
        }
    }
}
