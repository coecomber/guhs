package nl.juiced.guhs.feature.guhrio;

import net.minecraft.core.BlockPos;
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
 * Super Guhrio's messages. To the player's game: you are in a level now ({@link Start}: the lanes) / not any more
 * ({@link Stop}), the panel's numbers twice a second ({@link Staat}; it is also the heartbeat: a game that hears nothing
 * for a few seconds lets go of the lane by itself), a piece changed for you ({@link Stuk}: a coin taken, a ?-block
 * empty), something happened ({@link Moment}: back at the flag, into a pipe, out of it, the level done). To the server:
 * what your game saw you do ({@link Actie}: your head bumped a block, you touched a coin, you ducked on a pipe, you
 * landed on a Guhmba...); the server checks it and decides.
 */
public final class GuhrioPayloads {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Start.TYPE, Start.STREAM_CODEC, Start::handle);
        registrar.playToClient(Stop.TYPE, Stop.STREAM_CODEC, Stop::handle);
        registrar.playToClient(Staat.TYPE, Staat.STREAM_CODEC, Staat::handle);
        registrar.playToClient(Stuk.TYPE, Stuk.STREAM_CODEC, Stuk::handle);
        registrar.playToClient(Moment.TYPE, Moment.STREAM_CODEC, Moment::handle);
        registrar.playToServer(Actie.TYPE, Actie.STREAM_CODEC, Actie::handle);
    }

    /** Sends to a player, if their client knows the message (fake players in tests don't). */
    static void send(ServerPlayer player, CustomPacketPayload payload) {
        nl.juiced.guhs.network.ModNetworking.sendTo(player, payload);
    }

    /** Server -> client: you are in this level (GuhrioLevel.Geplaatst.naarTag, plus Baan: the lane you are on). */
    public record Start(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(Guhs.id("guhrio_start"));
        public static final StreamCodec<FriendlyByteBuf, Start> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Start::data, Start::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Start payload, IPayloadContext context) {
            nl.juiced.guhs.feature.guhrio.client.GuhrioClient.start(payload.data());
        }
    }

    /** Server -> client: the level is over for you (reden: GuhrioSpel.Einde's ordinal). */
    public record Stop(int reden) implements CustomPacketPayload {
        public static final Type<Stop> TYPE = new Type<>(Guhs.id("guhrio_stop"));
        public static final StreamCodec<FriendlyByteBuf, Stop> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Stop::reden, Stop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stop payload, IPayloadContext context) {
            nl.juiced.guhs.feature.guhrio.client.GuhrioClient.stop(payload.reden());
        }
    }

    /**
     * Server -> client: the panel (coins of this run, the coins in your pocket, the time, your power-up), the lane you are
     * on, your switch channels (bits), the big vadsmunten of this level you have (bits), the Guhshi that carries you (its
     * entity id; 0: none, -1: not made yet) - and the heartbeat.
     */
    public record Staat(int munten, int totaal, int ticks, int kracht, int baan, int kanalen, int vads, int guhshi) implements CustomPacketPayload {
        public static final Type<Staat> TYPE = new Type<>(Guhs.id("guhrio_staat"));
        public static final StreamCodec<FriendlyByteBuf, Staat> STREAM_CODEC = StreamCodec.of((buf, s) -> {
            buf.writeVarInt(s.munten);
            buf.writeVarInt(s.totaal);
            buf.writeVarInt(s.ticks);
            buf.writeVarInt(s.kracht);
            buf.writeVarInt(s.baan);
            buf.writeVarInt(s.kanalen);
            buf.writeVarInt(s.vads);
            buf.writeVarInt(s.guhshi);
        }, buf -> new Staat(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Staat payload, IPayloadContext context) {
            nl.juiced.guhs.feature.guhrio.client.GuhrioClient.staat(payload);
        }
    }

    /** Server -> client: this piece is in this state for you now (0 as built; a coin 1 taken, a ?-block 1 empty, a brick 1 broken). */
    public record Stuk(BlockPos pos, int staat) implements CustomPacketPayload {
        public static final Type<Stuk> TYPE = new Type<>(Guhs.id("guhrio_stuk"));
        public static final StreamCodec<FriendlyByteBuf, Stuk> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Stuk::pos, ByteBufCodecs.VAR_INT, Stuk::staat, Stuk::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stuk payload, IPayloadContext context) {
            nl.juiced.guhs.feature.guhrio.client.GuhrioClient.stuk(payload.pos(), payload.staat());
        }
    }

    /** Server -> client: something happened to you (soort: the constants below; pos and getal depend on it). */
    public record Moment(int soort, BlockPos pos, int getal) implements CustomPacketPayload {
        /** Back at your flag (getal: 0 fell, 1 bumped into something). */
        public static final int TERUG = 0;
        /** You go down into the pipe at pos (getal: ticks until you come out). */
        public static final int PIJP_IN = 1;
        /** You come up out of the pipe at pos (getal: the lane you are on now). */
        public static final int PIJP_UIT = 2;
        /** The flagpole at pos: done (getal: your time in ticks). */
        public static final int KLAAR = 3;
        /** A new flag at pos is yours. */
        public static final int VLAG = 4;
        /** You lost your power-up (getal 0) or Guhshi (getal 1), and are safe for a moment. */
        public static final int KRIMP = 5;
        /** Landed on a creature (getal: its entity id): bounce (the server saw it; your own game bounced already). */
        public static final int STUITER = 6;
        /** You stepped through a door and stand in the door at pos (getal: the lane you are on now). */
        public static final int DEUR = 7;
        /** The big vadsmunt at pos is yours (getal: how many of this level you have now). */
        public static final int VADSMUNT = 8;
        /** Guhshi's tongue shot out (getal: how far in tenths of a block, negative = back along the lane). */
        public static final int TONG = 9;

        public static final Type<Moment> TYPE = new Type<>(Guhs.id("guhrio_moment"));
        public static final StreamCodec<FriendlyByteBuf, Moment> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Moment::soort, BlockPos.STREAM_CODEC, Moment::pos, ByteBufCodecs.VAR_INT, Moment::getal, Moment::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Moment payload, IPayloadContext context) {
            nl.juiced.guhs.feature.guhrio.client.GuhrioClient.moment(payload);
        }
    }

    /** Client -> server: what your game saw you do (soort: the constants below; pos or an entity id). */
    public record Actie(int soort, BlockPos pos, int wezen) implements CustomPacketPayload {
        /** Your head bumped the block at pos from below. */
        public static final int BOTS = 0;
        /** You touch the piece at pos (a coin). */
        public static final int RAAK = 1;
        /** You duck (S) standing on pos (a pipe). */
        public static final int DUIK = 2;
        /** You press W at pos (a door). */
        public static final int DEUR = 3;
        /** You landed on the creature {@code wezen}. */
        public static final int STAMP = 4;
        /** The creature {@code wezen} touched you from the side. */
        public static final int GERAAKT = 5;
        /** "You say I am in a level, but my game let go of it": send the level again. */
        public static final int WEER = 6;
        /** The action key: throw a knabbel (Vuurpeper) or Guhshi's tongue ({@code wezen}: +1 further along the lane, -1 back). */
        public static final int GOOI = 7;
        /** You want out of the level (back to its entrance). */
        public static final int STOP = 8;
        /** You stand on the block at pos (a switch). */
        public static final int STAP = 9;

        public static final Type<Actie> TYPE = new Type<>(Guhs.id("guhrio_actie"));
        public static final StreamCodec<FriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Actie::soort, BlockPos.STREAM_CODEC, Actie::pos, ByteBufCodecs.VAR_INT, Actie::wezen, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Actie payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                GuhrioSpel.actie(player, payload.soort(), payload.pos(), payload.wezen());
            }
        }
    }

    private GuhrioPayloads() {
    }
}
