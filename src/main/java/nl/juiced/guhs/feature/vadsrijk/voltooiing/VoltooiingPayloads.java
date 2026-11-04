package nl.juiced.guhs.feature.vadsrijk.voltooiing;

import java.util.function.Consumer;

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
 * I-VOLTOOIING network: the book data ({@link Boek}, see {@link Voltooiing#schermData}), a milestone toast ({@link Toast}) and the
 * "open the book" request ({@link Open}: the key B or the tab in the mount/pet screens).
 */
public final class VoltooiingPayloads {
    /** Server -> client: open or refresh the book. */
    public record Boek(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Boek> TYPE = new Type<>(Guhs.id("vr_voltooiing_boek"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Boek> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Boek::data,
                Boek::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: "Zwamzoet 50 % verkend!" (a little toast in the corner). */
    public record Toast(String zoneKey, int procent) implements CustomPacketPayload {
        public static final Type<Toast> TYPE = new Type<>(Guhs.id("vr_voltooiing_toast"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Toast> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Toast::zoneKey,
                ByteBufCodecs.VAR_INT, Toast::procent, Toast::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: please send me the book. */
    public record Open(String zone) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("vr_voltooiing_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Open::zone,
                Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client. */
    public static Consumer<Boek> boekOntvanger = b -> {
    };
    public static Consumer<Toast> toastOntvanger = t -> {
    };

    private VoltooiingPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Boek.TYPE, Boek.STREAM_CODEC, (p, c) -> boekOntvanger.accept(p));
        r.playToClient(Toast.TYPE, Toast.STREAM_CODEC, (p, c) -> toastOntvanger.accept(p));
        r.playToServer(Open.TYPE, Open.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                VoltooiingRegistry.open(sp);
            }
        });
    }

    static void stuur(ServerPlayer p, CustomPacketPayload s) {
        PacketDistributor.sendToPlayer(p, s);
    }
}
