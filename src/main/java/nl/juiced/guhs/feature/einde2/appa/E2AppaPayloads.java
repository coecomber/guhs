package nl.juiced.guhs.feature.einde2.appa;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;

/**
 * einde2, slice appa: its one message, server to client: whether a player carries a Momo-guh on their right shoulder
 * (guhs:einde2_appa_momo_schouder; the client draws him there, client.E2AppaClient).
 */
public final class E2AppaPayloads {
    /** Client: the entity ids of the players with a Momo-guh on their shoulder. */
    public static final Set<Integer> CLIENT_SCHOUDERS = ConcurrentHashMap.newKeySet();

    public record MomoSchouder(int speler, boolean heeft) implements CustomPacketPayload {
        public static final Type<MomoSchouder> TYPE = new Type<>(Guhs.id("einde2_appa_momo_schouder"));
        public static final StreamCodec<FriendlyByteBuf, MomoSchouder> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MomoSchouder::speler, ByteBufCodecs.BOOL, MomoSchouder::heeft, MomoSchouder::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MomoSchouder p, IPayloadContext context) {
            if (p.heeft()) {
                CLIENT_SCHOUDERS.add(p.speler());
            } else {
                CLIENT_SCHOUDERS.remove(p.speler());
            }
        }
    }

    static MomoSchouder bericht(ServerPlayer player) {
        return new MomoSchouder(player.getId(), Momo.opSchouder(player));
    }

    /** Tells the player and everyone who sees them whether a Momo-guh sits on their shoulder. */
    static void sync(ServerPlayer player) {
        MomoSchouder bericht = bericht(player);
        int range = player.getType().clientTrackingRange() * 16 + 16;
        for (ServerPlayer p : player.level().players()) {
            if (p == player || p.distanceToSqr(player) <= (double) range * range) {
                ModNetworking.sendTo(p, bericht);
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(MomoSchouder.TYPE, MomoSchouder.STREAM_CODEC, MomoSchouder::handle);
    }

    private E2AppaPayloads() {
    }
}
