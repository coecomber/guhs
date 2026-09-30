package nl.juiced.guhs.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhSleeEntity;

/** The sled panel (client -> server): start, stop, speed 1-3, or turn around. Only the one riding it may. */
public record SledControlPayload(int action) implements CustomPacketPayload {
    public static final int START = 0, STOP = 1, REVERSE = 2, SPEED = 10; // SPEED + 1..3

    public static final Type<SledControlPayload> TYPE = new Type<>(Guhs.id("sled_control"));
    public static final StreamCodec<ByteBuf, SledControlPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(SledControlPayload::new, SledControlPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SledControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.getVehicle() instanceof GuhSleeEntity sled)) {
            return;
        }
        switch (payload.action()) {
            case START -> sled.setRunning(true);
            case STOP -> sled.setRunning(false);
            case REVERSE -> sled.reverse();
            default -> sled.setSpeed(payload.action() - SPEED);
        }
    }
}
