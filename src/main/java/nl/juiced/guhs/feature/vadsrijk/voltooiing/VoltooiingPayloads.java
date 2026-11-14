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
 * "open the book" request ({@link Open}: the key B or the tab in the mount/pet screens). 1.10.3: the campfire travel screen
 * ({@link Reis}, {@link ReisKies}, {@link ReisKlaar}; a {@link Toast} with percent {@link KampvuurReizen#TOAST_VRIJ} = "travel unlocked").
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

    /** 1.10.3 Kampvuurreizen. Server -> client: open or refresh the campfire travel screen ({@link KampvuurReizen#schermData}). */
    public record Reis(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Reis> TYPE = new Type<>(Guhs.id("vr_kampvuur_reis"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Reis> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Reis::data,
                Reis::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 1.10.3. Client -> server: travel to this campfire (a {@link KampvuurReizen.Vuur#id()}); the server checks everything. */
    public record ReisKies(String id) implements CustomPacketPayload {
        public static final Type<ReisKies> TYPE = new Type<>(Guhs.id("vr_kampvuur_reis_kies"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ReisKies> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ReisKies::id,
                ReisKies::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 1.10.3. Server -> client: the trip happened: close the travel screen and fade in from pink over {@code ticks}. */
    public record ReisKlaar(int ticks) implements CustomPacketPayload {
        public static final Type<ReisKlaar> TYPE = new Type<>(Guhs.id("vr_kampvuur_reis_klaar"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ReisKlaar> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ReisKlaar::ticks,
                ReisKlaar::new);

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
    public static Consumer<Reis> reisOntvanger = r -> {
    };
    public static Consumer<ReisKlaar> reisKlaarOntvanger = r -> {
    };

    private VoltooiingPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Boek.TYPE, Boek.STREAM_CODEC, (p, c) -> boekOntvanger.accept(p));
        r.playToClient(Toast.TYPE, Toast.STREAM_CODEC, (p, c) -> toastOntvanger.accept(p));
        r.playToClient(Reis.TYPE, Reis.STREAM_CODEC, (p, c) -> reisOntvanger.accept(p));
        r.playToClient(ReisKlaar.TYPE, ReisKlaar.STREAM_CODEC, (p, c) -> reisKlaarOntvanger.accept(p));
        r.playToServer(ReisKies.TYPE, ReisKies.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                KampvuurReizen.opKies(sp, p.id());
            }
        });
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
