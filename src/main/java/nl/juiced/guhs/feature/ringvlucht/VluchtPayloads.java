package nl.juiced.guhs.feature.ringvlucht;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * 1.4.2 (rookguh): guhs:ringvlucht_open (server -> client: the Rookguh's screen with the places this player may fly to) and
 * guhs:ringvlucht_kies (the place picked; the server checks everything again: {@link Vlucht#kies}).
 */
public final class VluchtPayloads {
    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Kies.TYPE, Kies.STREAM_CODEC, Kies::handle);
    }

    /** data: "Haltes" = a list of {Id, Naam (lang key), Wereld (lang key), Volgende}, in the order to show. */
    public record Open(int rookguh, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("ringvlucht_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::rookguh, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.ringvlucht.client.RingVluchtClient.open(payload.rookguh(), payload.data());
        }
    }

    public record Kies(int rookguh, String halte) implements CustomPacketPayload {
        public static final Type<Kies> TYPE = new Type<>(Guhs.id("ringvlucht_kies"));
        public static final StreamCodec<FriendlyByteBuf, Kies> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Kies::rookguh, ByteBufCodecs.STRING_UTF8, Kies::halte, Kies::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kies payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer p && p.level().getEntity(payload.rookguh()) instanceof VluchtRookguhEntity rookguh) {
                Vlucht.kies(p, rookguh, payload.halte());
            }
        }
    }

    private VluchtPayloads() {
    }
}
