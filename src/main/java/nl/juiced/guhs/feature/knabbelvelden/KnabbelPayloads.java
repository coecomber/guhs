package nl.juiced.guhs.feature.knabbelvelden;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;

/**
 * The Knabbelportaal's messages: guhs:knabbelvelden_open (server -> client: the big screen, opened or updated, see
 * {@link Pensioen#data}), guhs:knabbelvelden_actie (client -> server: a button: send / bring home these guhs, or go),
 * guhs:knabbelvelden_vraag (server -> client: "Mee naar huis?" for a ghost the owner sneak-clicked) and
 * guhs:knabbelvelden_mee (client -> server: yes).
 */
public final class KnabbelPayloads {
    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("knabbelvelden_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.knabbelvelden.client.KnabbelveldenClient.open(payload.data());
        }
    }

    public record Actie(long portaal, int soort, List<UUID> guhs, boolean bevestigd) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("knabbelvelden_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, Actie::portaal,
                ByteBufCodecs.VAR_INT, Actie::soort, UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(512)), Actie::guhs,
                ByteBufCodecs.BOOL, Actie::bevestigd, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Actie payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer p) {
                Pensioen.actie(p, BlockPos.of(payload.portaal()), payload.soort(), payload.guhs(), payload.bevestigd());
            }
        }
    }

    public record Vraag(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Vraag> TYPE = new Type<>(Guhs.id("knabbelvelden_vraag"));
        public static final StreamCodec<FriendlyByteBuf, Vraag> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Vraag::data, Vraag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Vraag payload, IPayloadContext context) {
            nl.juiced.guhs.feature.knabbelvelden.client.KnabbelveldenClient.vraag(payload.data());
        }
    }

    public record MeeNaarHuis(int entity) implements CustomPacketPayload {
        public static final Type<MeeNaarHuis> TYPE = new Type<>(Guhs.id("knabbelvelden_mee"));
        public static final StreamCodec<FriendlyByteBuf, MeeNaarHuis> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, MeeNaarHuis::entity,
                MeeNaarHuis::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MeeNaarHuis payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer p) {
                Geesten.meeNaarHuis(p, payload.entity());
            }
        }
    }

    static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Actie.TYPE, Actie.STREAM_CODEC, Actie::handle);
        registrar.playToClient(Vraag.TYPE, Vraag.STREAM_CODEC, Vraag::handle);
        registrar.playToServer(MeeNaarHuis.TYPE, MeeNaarHuis.STREAM_CODEC, MeeNaarHuis::handle);
    }

    private KnabbelPayloads() {
    }
}
