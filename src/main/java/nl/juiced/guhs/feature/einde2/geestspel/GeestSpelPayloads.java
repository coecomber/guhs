package nl.juiced.guhs.feature.einde2.geestspel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * einde2 phase B, slice geest_spel: its messages.
 * <ul>
 *   <li>{@link Sfeer} (server to client): the mood of the Geestenwereld now (spirit tide: deep blue mist; moon night:
 *       silver light), for the fog colour;</li>
 *   <li>{@link Ritme} (server to client): Piandao-guh's duel runs: a click (attack or use) is a parry, not a swing;</li>
 *   <li>{@link Pareer} (client to server): the player clicked during the duel;</li>
 *   <li>{@link Spiegel} (server to client): what a mirror pond shows (a building's id, or yourself glowing).</li>
 * </ul>
 * The client's state lives in the static fields here (no client class is touched by the common side).
 */
public final class GeestSpelPayloads {
    /** Client: the mood of the Geestenwereld, and since when (client game time) a mirror pond shows something. */
    public static volatile boolean CLIENT_TIJ, CLIENT_MAAN, CLIENT_RITME;
    public static volatile String CLIENT_SPIEGEL = "";
    public static volatile boolean CLIENT_SPIEGEL_ZELF;
    public static volatile long CLIENT_SPIEGEL_SINDS = -10000;

    private GeestSpelPayloads() {
    }

    public record Sfeer(boolean tij, boolean maan) implements CustomPacketPayload {
        public static final Type<Sfeer> TYPE = new Type<>(Guhs.id("einde2_geest_spel_sfeer"));
        public static final StreamCodec<FriendlyByteBuf, Sfeer> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Sfeer::tij, ByteBufCodecs.BOOL,
                Sfeer::maan, Sfeer::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Sfeer p, IPayloadContext context) {
            CLIENT_TIJ = p.tij();
            CLIENT_MAAN = p.maan();
        }
    }

    public record Ritme(boolean aan) implements CustomPacketPayload {
        public static final Type<Ritme> TYPE = new Type<>(Guhs.id("einde2_geest_spel_ritme"));
        public static final StreamCodec<FriendlyByteBuf, Ritme> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Ritme::aan, Ritme::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Ritme p, IPayloadContext context) {
            CLIENT_RITME = p.aan();
        }
    }

    public record Pareer() implements CustomPacketPayload {
        public static final Type<Pareer> TYPE = new Type<>(Guhs.id("einde2_geest_spel_pareer"));
        public static final StreamCodec<FriendlyByteBuf, Pareer> STREAM_CODEC = StreamCodec.unit(new Pareer());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Pareer p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                context.enqueueWork(() -> Proeven.pareer(sp));
            }
        }
    }

    public record Spiegel(String gebouw, boolean zelf) implements CustomPacketPayload {
        public static final Type<Spiegel> TYPE = new Type<>(Guhs.id("einde2_geest_spel_spiegel"));
        public static final StreamCodec<FriendlyByteBuf, Spiegel> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Spiegel::gebouw,
                ByteBufCodecs.BOOL, Spiegel::zelf, Spiegel::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Spiegel p, IPayloadContext context) {
            CLIENT_SPIEGEL = p.gebouw();
            CLIENT_SPIEGEL_ZELF = p.zelf();
            CLIENT_SPIEGEL_SINDS = System.currentTimeMillis();
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Sfeer.TYPE, Sfeer.STREAM_CODEC, Sfeer::handle);
        registrar.playToClient(Ritme.TYPE, Ritme.STREAM_CODEC, Ritme::handle);
        registrar.playToServer(Pareer.TYPE, Pareer.STREAM_CODEC, Pareer::handle);
        registrar.playToClient(Spiegel.TYPE, Spiegel.STREAM_CODEC, Spiegel::handle);
    }
}
