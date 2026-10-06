package nl.juiced.guhs.mixin;

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.verhaal.Sluiers;

/**
 * bbq2 (ring-kern, CONTRACT_130 13.9): a sound or a level event (a door, a splash) at a spot inside a Guhdalfs sluier is
 * not sent to a player for whom that sluier is closed. {@code PlayerList#broadcast} is how the server sends such things to
 * everybody near a spot; only in a dimension that has a sluier at all, and only for those two packets, the loop is done
 * here with that one extra question ({@link Sluiers#magHoren}). (Sounds of an entity need nothing: the game of a player who
 * was never told about the entity drops them.)
 */
@Mixin(PlayerList.class)
public abstract class RingSluierGeluidMixin {
    @Shadow
    @Final
    private List<ServerPlayer> players;

    @Inject(method = "broadcast(Lnet/minecraft/world/entity/player/Player;DDDDLnet/minecraft/resources/ResourceKey;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"), cancellable = true)
    private void guhs$nietDoorDeSluier(@Nullable Player except, double x, double y, double z, double range, ResourceKey<Level> dimension, Packet<?> packet,
                                       CallbackInfo ci) {
        if (!(packet instanceof ClientboundSoundPacket || packet instanceof ClientboundLevelEventPacket) || !Sluiers.heeftZones(dimension)) {
            return;
        }
        for (int i = 0; i < this.players.size(); i++) {
            ServerPlayer player = this.players.get(i);
            if (player != except && player.level().dimension() == dimension) {
                double xd = x - player.getX(), yd = y - player.getY(), zd = z - player.getZ();
                if (xd * xd + yd * yd + zd * zd < range * range && Sluiers.magHoren(player, player.level(), x, y, z)) {
                    player.connection.send(packet);
                }
            }
        }
        ci.cancel();
    }
}
