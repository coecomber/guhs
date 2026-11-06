package nl.juiced.guhs.feature.vadsrijk.zichtbaar;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** U-ZICHTBAAR network: what a player wears in the virtual slots, as item ids in {@link Looks#VOLGORDE} ("" = nothing). */
public final class ZichtbaarPayloads {
    /** Server -> the player + everyone tracking them: the worn looks (all "" outside the Vadsrijk). */
    public record Uitrusting(UUID speler, List<String> looks) implements CustomPacketPayload {
        public static final Type<Uitrusting> TYPE = new Type<>(Guhs.id("vr_u_zichtbaar_uitrusting"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Uitrusting> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
                Uitrusting::speler, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), Uitrusting::looks, Uitrusting::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client. */
    public static Consumer<Uitrusting> ontvanger = u -> {
    };

    private ZichtbaarPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Uitrusting.TYPE, Uitrusting.STREAM_CODEC, (p, c) -> ontvanger.accept(p));
    }

    /** To one player (a gametest mock player has no channel: then nothing). */
    static void stuur(ServerPlayer naar, CustomPacketPayload s) {
        if (naar.connection == null) {
            return;
        }
        try {
            PacketDistributor.sendToPlayer(naar, s);
        } catch (RuntimeException e) {
            // (a mock player)
        }
    }

    /** To the player and everyone tracking them. */
    static void stuurRond(ServerPlayer van, CustomPacketPayload s) {
        try {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(van, s);
        } catch (RuntimeException e) {
            // (a mock player that is not in the chunk map)
        }
    }
}
