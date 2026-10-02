package nl.juiced.guhs.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SledControlPayload.TYPE, SledControlPayload.STREAM_CODEC, SledControlPayload::handle);
        registrar.playToServer(GuhActionPayload.TYPE, GuhActionPayload.STREAM_CODEC, GuhActionPayload::handle);
        registrar.playToServer(BankActionPayload.TYPE, BankActionPayload.STREAM_CODEC, BankActionPayload::handle);
        registrar.playToServer(BankJeiPayload.TYPE, BankJeiPayload.STREAM_CODEC, BankJeiPayload::handle);   // 1.2.5: JEI "+"
        registrar.playToServer(DrinkKaasSausPayload.TYPE, DrinkKaasSausPayload.STREAM_CODEC, DrinkKaasSausPayload::handle);
        registrar.playToClient(BankContentsPayload.TYPE, BankContentsPayload.STREAM_CODEC, BankContentsPayload::handle);
        MaagPayloads.register(registrar);
        nl.juiced.guhs.feature.Features.payloads(registrar);
    }

    private ModNetworking() {
    }

    /** Sends to a player, if their client knows the message (fake players in tests don't). */
    public static void sendTo(net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (!(player instanceof net.neoforged.neoforge.common.util.FakePlayer) && player.connection != null && player.connection.hasChannel(payload)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
