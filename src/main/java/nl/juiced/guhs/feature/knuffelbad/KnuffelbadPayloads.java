package nl.juiced.guhs.feature.knuffelbad;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * The Knuffelbad's messages: Badmeester Bubbel's screen (knuffelbad_open / knuffelbad_action), the rider's game telling
 * the server where it is on the slide (knuffelbad_stuur, every tick) and the ride panel (knuffelbad_hud).
 */
public final class KnuffelbadPayloads {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Actie.TYPE, Actie.STREAM_CODEC, Actie::handle);
        registrar.playToServer(Stuur.TYPE, Stuur.STREAM_CODEC, Stuur::handle);
        registrar.playToClient(Hud.TYPE, Hud.STREAM_CODEC, Hud::handle);
    }

    /** Sends to a player, if their game knows the message (fake players in tests don't). */
    static void naar(ServerPlayer player, CustomPacketPayload payload) {
        if (!(player instanceof net.neoforged.neoforge.common.util.FakePlayer) && player.connection != null && player.connection.hasChannel(payload)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** (Client) where the rider's own ride is now. */
    static void stuur(int ring, float tau, float lat) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new Stuur(ring, tau, lat));
    }

    /** Server -> client: open Badmeester Bubbel's screen. */
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("knuffelbad_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.knuffelbad.client.KnuffelbadClient.open(payload);
        }
    }

    /** Client -> server: a button on his screen (Badmeester.WINKEL, TIP, WASSEN). */
    public record Actie(int npcId, int actie) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("knuffelbad_action"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Actie::npcId, ByteBufCodecs.VAR_INT, Actie::actie, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Actie payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.level().getEntity(payload.npcId()) instanceof GuhNpcEntity npc) {
                Badmeester.actie(npc, player, payload.actie());
            }
        }
    }

    /** Client -> server: the rider's own ride is at tau (ticks) with this lateral. */
    public record Stuur(int ring, float tau, float lat) implements CustomPacketPayload {
        public static final Type<Stuur> TYPE = new Type<>(Guhs.id("knuffelbad_stuur"));
        public static final StreamCodec<FriendlyByteBuf, Stuur> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Stuur::ring, ByteBufCodecs.FLOAT, Stuur::tau, ByteBufCodecs.FLOAT, Stuur::lat, Stuur::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stuur payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof ZwembandjeEntity ring && ring.getId() == payload.ring()) {
                GlijRit rit = GlijRit.van(player);
                if (rit != null && ring.getUUID().equals(rit.bandje()) && Float.isFinite(payload.tau()) && Float.isFinite(payload.lat())) {
                    rit.meld(player, payload.tau(), payload.lat());
                }
            }
        }
    }

    /** Server -> client: the ride panel (score, ducks, combo; "Actief" false: gone). */
    public record Hud(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Hud> TYPE = new Type<>(Guhs.id("knuffelbad_hud"));
        public static final StreamCodec<FriendlyByteBuf, Hud> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Hud::data, Hud::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Hud payload, IPayloadContext context) {
            nl.juiced.guhs.feature.knuffelbad.client.KnuffelbadClient.hud(payload.data());
        }
    }

    private KnuffelbadPayloads() {
    }
}
