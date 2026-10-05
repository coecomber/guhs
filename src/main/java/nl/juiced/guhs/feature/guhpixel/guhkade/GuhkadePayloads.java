package nl.juiced.guhs.feature.guhpixel.guhkade;

import java.util.function.Consumer;

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
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;

/**
 * The Guhkade over the network:
 * <ul>
 *   <li>{@code guhs:guhkade_open} (to the client): the game screen of the cabinet at pos: which game, the seed of the next
 *   game, the top 5, your own best;</li>
 *   <li>{@code guhs:guhkade_start} (to the server): the game with that seed begins now;</li>
 *   <li>{@code guhs:guhkade_klaar} (to the server): the game is over: the input of every step (never a score: the server
 *   plays the input again);</li>
 *   <li>{@code guhs:guhkade_uitslag} (to the client): the score the server counted, your place, the top 5 and a new seed;</li>
 *   <li>{@code guhs:guhkade_stop} (to the server): the screen closed.</li>
 * </ul>
 */
public final class GuhkadePayloads {
    public static volatile Consumer<Open> opener = p -> { };
    public static volatile Consumer<Uitslag> uitslagOntvanger = p -> { };
    /** One game's input at most (two bits a step, ten minutes) plus a little room. */
    private static final int MAX_INVOER = Sim.MAX_STAPPEN / 4 + 64;

    public record Open(BlockPos pos, String spel, long seed, CompoundTag scherm, int best) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("guhkade_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Open::pos,
                ByteBufCodecs.STRING_UTF8, Open::spel, ByteBufCodecs.VAR_LONG, Open::seed, ByteBufCodecs.COMPOUND_TAG, Open::scherm,
                ByteBufCodecs.VAR_INT, Open::best, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Open p, IPayloadContext context) {
            opener.accept(p);
        }
    }

    public record Start(BlockPos pos, long seed) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(Guhs.id("guhkade_start"));
        public static final StreamCodec<FriendlyByteBuf, Start> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Start::pos,
                ByteBufCodecs.VAR_LONG, Start::seed, Start::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Start p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                Guhkade.start(player, p.pos(), p.seed());
            }
        }
    }

    public record Klaar(BlockPos pos, long seed, int stappen, byte[] invoer) implements CustomPacketPayload {
        public static final Type<Klaar> TYPE = new Type<>(Guhs.id("guhkade_klaar"));
        public static final StreamCodec<FriendlyByteBuf, Klaar> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Klaar::pos,
                ByteBufCodecs.VAR_LONG, Klaar::seed, ByteBufCodecs.VAR_INT, Klaar::stappen, ByteBufCodecs.byteArray(MAX_INVOER), Klaar::invoer, Klaar::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Klaar p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                Guhkade.klaar(player, p.pos(), p.seed(), p.stappen(), p.invoer());
            }
        }
    }

    public record Uitslag(BlockPos pos, int score, int plaats, boolean record, int verslagen, long seed, CompoundTag scherm, int best)
            implements CustomPacketPayload {
        public static final Type<Uitslag> TYPE = new Type<>(Guhs.id("guhkade_uitslag"));
        public static final StreamCodec<FriendlyByteBuf, Uitslag> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeBlockPos(p.pos());
            buf.writeVarInt(p.score());
            buf.writeVarInt(p.plaats());
            buf.writeBoolean(p.record());
            buf.writeVarInt(p.verslagen());
            buf.writeLong(p.seed());
            buf.writeNbt(p.scherm());
            buf.writeVarInt(p.best());
        }, buf -> new Uitslag(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readLong(),
                orEmpty(buf.readNbt()), buf.readVarInt()));

        private static CompoundTag orEmpty(CompoundTag t) {
            return t == null ? new CompoundTag() : t;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Uitslag p, IPayloadContext context) {
            uitslagOntvanger.accept(p);
        }
    }

    public record Stop(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Stop> TYPE = new Type<>(Guhs.id("guhkade_stop"));
        public static final StreamCodec<FriendlyByteBuf, Stop> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Stop::pos, Stop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Stop p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                Guhkade.stop(player, p.pos());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Start.TYPE, Start.STREAM_CODEC, Start::handle);
        registrar.playToServer(Klaar.TYPE, Klaar.STREAM_CODEC, Klaar::handle);
        registrar.playToClient(Uitslag.TYPE, Uitslag.STREAM_CODEC, Uitslag::handle);
        registrar.playToServer(Stop.TYPE, Stop.STREAM_CODEC, Stop::handle);
    }

    private GuhkadePayloads() {
    }
}
