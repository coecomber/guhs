package nl.juiced.guhs.feature.guhpixel.bioscoop;

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

/**
 * The Guhbioscoop over the network: {@code guhs:guhbioscoop_open} (to the client: open / refresh the projector screen
 * with {@link Bioscoop#stand}) and {@code guhs:guhbioscoop_doe} (to the server: play a film or stop). The film itself needs
 * no packets: the projector's block entity syncs "which film since when".
 */
public final class BioscoopPayloads {
    public static final int SPEEL = 0, STOP = 1;
    /** Set by client.BioscoopClient; the server never calls it. */
    public static volatile Consumer<Open> opener = p -> { };

    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("guhbioscoop_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            opener.accept(p);
        }
    }

    public record Doe(BlockPos pos, int actie, String film) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("guhbioscoop_doe"));
        public static final StreamCodec<FriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Doe::pos,
                ByteBufCodecs.VAR_INT, Doe::actie, ByteBufCodecs.stringUtf8(64), Doe::film, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Doe p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                Bioscoop.doe(sp, p.pos(), p.actie(), p.film());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Doe.TYPE, Doe.STREAM_CODEC, Doe::handle);
    }

    private BioscoopPayloads() {
    }
}
