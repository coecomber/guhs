package nl.juiced.guhs.feature.verhaal;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * bbq2 (verhaal engine): the messages of the cutscenes, the narrator cards and Guhdalfs sluier.
 * <ul>
 *   <li>{@code guhs:verhaal_cutscene} (to the client): play this scene at this anchor;</li>
 *   <li>{@code guhs:verhaal_kaart} (to the client): show this narrator card, or a scene as a picture book;</li>
 *   <li>{@code guhs:verhaal_stop} (to the client): what you are watching ends now (the lock was broken off);</li>
 *   <li>{@code guhs:verhaal_klaar} (to the server): the scene or card is over;</li>
 *   <li>{@code guhs:verhaal_herbekijk} (to the server): the Guhdex button "watch again";</li>
 *   <li>{@code guhs:verhaal_sluiers} (to the client): the smoke walls near you.</li>
 * </ul>
 * The client handlers are set by client.VerhaalClient.
 */
public final class VerhaalPayloads {
    public static final String SCENE = "scene", KAART = "kaart", BOEK = "boek";

    public static volatile Consumer<Speel> speelOntvanger = p -> {
    };
    public static volatile Consumer<Kaart> kaartOntvanger = p -> {
    };
    public static volatile Consumer<Stop> stopOntvanger = p -> {
    };
    public static volatile Consumer<Rook> rookOntvanger = p -> {
    };

    public record Speel(String id, BlockPos anker, int draai) implements CustomPacketPayload {
        public static final Type<Speel> TYPE = new Type<>(Guhs.id("verhaal_cutscene"));
        public static final StreamCodec<FriendlyByteBuf, Speel> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Speel::id, BlockPos.STREAM_CODEC, Speel::anker, ByteBufCodecs.VAR_INT, Speel::draai, Speel::new);

        public Rotation rotatie() {
            return Rotation.values()[Math.floorMod(draai, Rotation.values().length)];
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Speel p, IPayloadContext context) {
            context.enqueueWork(() -> speelOntvanger.accept(p));
        }
    }

    /** soort: {@link #KAART} (id = a narrator card) or {@link #BOEK} (id = a scene, shown as a picture book). */
    public record Kaart(String soort, String id) implements CustomPacketPayload {
        public static final Type<Kaart> TYPE = new Type<>(Guhs.id("verhaal_kaart"));
        public static final StreamCodec<FriendlyByteBuf, Kaart> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Kaart::soort, ByteBufCodecs.STRING_UTF8, Kaart::id, Kaart::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kaart p, IPayloadContext context) {
            context.enqueueWork(() -> kaartOntvanger.accept(p));
        }
    }

    public record Stop(String soort, String id) implements CustomPacketPayload {
        public static final Type<Stop> TYPE = new Type<>(Guhs.id("verhaal_stop"));
        public static final StreamCodec<FriendlyByteBuf, Stop> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Stop::soort, ByteBufCodecs.STRING_UTF8, Stop::id, Stop::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stop p, IPayloadContext context) {
            context.enqueueWork(() -> stopOntvanger.accept(p));
        }
    }

    public record Klaar(String soort, String id) implements CustomPacketPayload {
        public static final Type<Klaar> TYPE = new Type<>(Guhs.id("verhaal_klaar"));
        public static final StreamCodec<FriendlyByteBuf, Klaar> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Klaar::soort, ByteBufCodecs.STRING_UTF8, Klaar::id, Klaar::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Klaar p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> Vast.klaar(sp, p.soort(), p.id()));
            }
        }
    }

    /** soort: {@link #SCENE} or {@link #KAART}. */
    public record Herbekijk(String soort, String id) implements CustomPacketPayload {
        public static final Type<Herbekijk> TYPE = new Type<>(Guhs.id("verhaal_herbekijk"));
        public static final StreamCodec<FriendlyByteBuf, Herbekijk> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Herbekijk::soort, ByteBufCodecs.STRING_UTF8, Herbekijk::id, Herbekijk::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Herbekijk p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> {
                    if (KAART.equals(p.soort())) {
                        Verteller.herbekijk(sp, p.id());
                    } else {
                        Cutscenes.herbekijk(sp, p.id());
                    }
                });
            }
        }
    }

    /** The smoke walls of this player near them: "Zones" [{S (structure), X0, Y0, Z0, X1, Y1, Z1}]. */
    public record Rook(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Rook> TYPE = new Type<>(Guhs.id("verhaal_sluiers"));
        public static final StreamCodec<FriendlyByteBuf, Rook> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Rook::data, Rook::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Rook p, IPayloadContext context) {
            context.enqueueWork(() -> rookOntvanger.accept(p));
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Speel.TYPE, Speel.STREAM_CODEC, Speel::handle);
        registrar.playToClient(Kaart.TYPE, Kaart.STREAM_CODEC, Kaart::handle);
        registrar.playToClient(Stop.TYPE, Stop.STREAM_CODEC, Stop::handle);
        registrar.playToClient(Rook.TYPE, Rook.STREAM_CODEC, Rook::handle);
        registrar.playToServer(Klaar.TYPE, Klaar.STREAM_CODEC, Klaar::handle);
        registrar.playToServer(Herbekijk.TYPE, Herbekijk.STREAM_CODEC, Herbekijk::handle);
    }

    private VerhaalPayloads() {
    }
}
