package nl.juiced.guhs.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;

/**
 * 1.1.0 (MC 26.1), gametests only: {@code GameTestHelper#makeMockServerPlayerInLevel} gives the mock player a
 * connection on a netty {@link EmbeddedChannel} that never negotiated any mod channel, and NeoForge 26.1 throws
 * ("Payload ... may not be sent to the client!") when a modded payload (GeckoLib animation triggers, our own packets)
 * is sent to it, which crashes the whole test server. Mock players never read packets, so modded payloads to such a
 * connection are dropped. Real players always have a socket channel and are not affected.
 */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class MockConnectionMixin {
    @Shadow
    @Final
    protected Connection connection;

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void guhs$geenModPakkettenNaarTestSpelers(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if (packet instanceof ClientboundCustomPayloadPacket custom && this.connection.channel() instanceof EmbeddedChannel
                && !"minecraft".equals(custom.payload().type().id().getNamespace())) {
            ci.cancel();
        }
    }
}
