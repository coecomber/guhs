package nl.juiced.guhs.feature.einde2.h2water;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;

/**
 * einde2 h2_water: the two messages of the chapter to the client.
 * <ul>
 *   <li>{@link Toestand}: how the world looks for this player: {@code E2H2Water.GEWOON}, {@code GRIJS} (the moon fish is
 *       gone: the colours drain away) or {@code STORMACHTIG} (the blizzard on the snow plain). Sent on a change only.</li>
 *   <li>{@link Blanco}: Koh took this player's face for a while: a blank face over their head (sent to them and to the
 *       players around them).</li>
 * </ul>
 * The client keeps both in the static fields of {@link Client} (common code: no client class is named here).
 */
public final class E2H2WaterPayloads {
    public record Toestand(int soort) implements CustomPacketPayload {
        public static final Type<Toestand> TYPE = new Type<>(Guhs.id("einde2_h2_water_toestand"));
        public static final StreamCodec<FriendlyByteBuf, Toestand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Toestand::soort, Toestand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Toestand payload, IPayloadContext context) {
            Client.toestand = payload.soort();
        }
    }

    public record Blanco(UUID speler, int ticks) implements CustomPacketPayload {
        public static final Type<Blanco> TYPE = new Type<>(Guhs.id("einde2_h2_water_blanco"));
        public static final StreamCodec<FriendlyByteBuf, Blanco> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Blanco::speler,
                ByteBufCodecs.VAR_INT, Blanco::ticks, Blanco::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Blanco payload, IPayloadContext context) {
            var level = context.player().level();
            Client.BLANCO.put(payload.speler(), level.getGameTime() + payload.ticks());
        }
    }

    /** What the client knows (only read and written on the client). */
    public static final class Client {
        /** The state of the local player's world ({@code E2H2Water.GEWOON} ...). */
        public static volatile int toestand;
        /** Players without a face: until which game time. */
        public static final Map<UUID, Long> BLANCO = new ConcurrentHashMap<>();

        public static boolean blanco(UUID speler, long nu) {
            Long tot = BLANCO.get(speler);
            if (tot == null) {
                return false;
            }
            if (nu > tot) {
                BLANCO.remove(speler);
                return false;
            }
            return true;
        }

        /** (logout) forget everything. */
        public static void wis() {
            toestand = 0;
            BLANCO.clear();
        }

        private Client() {
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Toestand.TYPE, Toestand.STREAM_CODEC, Toestand::handle);
        registrar.playToClient(Blanco.TYPE, Blanco.STREAM_CODEC, Blanco::handle);
    }

    /** Sends to one player (never to a game test's mock player). */
    static void stuur(ServerPlayer p, CustomPacketPayload payload) {
        if (!(p instanceof FakePlayer) && p.connection != null) {
            ModNetworking.sendTo(p, payload);
        }
    }

    /** Koh took this player's face: tell them and everybody within 48 blocks. */
    static void blanco(ServerPlayer p, int ticks) {
        Blanco b = new Blanco(p.getUUID(), ticks);
        for (ServerPlayer q : p.level().players()) {
            if (q.distanceToSqr(p) < 48 * 48) {
                stuur(q, b);
            }
        }
    }

    private E2H2WaterPayloads() {
    }
}
