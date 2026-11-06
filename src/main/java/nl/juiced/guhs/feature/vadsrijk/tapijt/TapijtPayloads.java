package nl.juiced.guhs.feature.vadsrijk.tapijt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * U-TAPIJT network: the carpet's tuning numbers ({@link TapijtBalans}) to the client, because the riding client moves the carpet
 * (prediction) and must use the server's numbers. Sent on join and whenever a number changes ({@code /guhs vadsrijk tapijt tune}).
 */
public final class TapijtPayloads {
    public record Balans(CompoundTag waarden) implements CustomPacketPayload {
        public static final Type<Balans> TYPE = new Type<>(Guhs.id("vr_tapijt_balans"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Balans> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Balans::waarden,
                Balans::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private TapijtPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Balans.TYPE, Balans.STREAM_CODEC, (p, c) -> TapijtBalans.vanTag(p.waarden()));
    }

    static void stuur(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new Balans(TapijtBalans.alsTag()));
    }
}
