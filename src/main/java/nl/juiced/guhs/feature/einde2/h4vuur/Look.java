package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;

/**
 * einde2 phase B, slice h4_vuur: how a player looks in chapter 4 (CONTRACT_E2_B 7.5), told to every client that sees them
 * (message {@code guhs:einde2_h4_vuur_look}): {@link #VERMOMD} = the Vuurnatie disguise over the skin (from the arrival
 * until the Dag van de Zwarte Zon), {@link #GLOED} = the glowing eyes of the Guhvatar-staat (the last round of the duel).
 * It is a render layer on the player (client.LookLaag), never guh clothes and never an item: nothing to lose.
 */
public final class Look {
    public static final byte GEEN = 0, VERMOMD = 1, GLOED = 2;

    /** Client: entity id of a player -> their look (absent = none). */
    public static final Map<Integer, Byte> CLIENT = new ConcurrentHashMap<>();

    public record Bericht(int speler, byte look) implements CustomPacketPayload {
        public static final Type<Bericht> TYPE = new Type<>(Guhs.id("einde2_h4_vuur_look"));
        public static final StreamCodec<FriendlyByteBuf, Bericht> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Bericht::speler, ByteBufCodecs.BYTE, Bericht::look, Bericht::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(Bericht b, IPayloadContext context) {
            if (b.look() == GEEN) {
                CLIENT.remove(b.speler());
            } else {
                CLIENT.put(b.speler(), b.look());
            }
        }
    }

    private Look() {
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Bericht.TYPE, Bericht.STREAM_CODEC, Bericht::handle);
    }

    /** The look of this player now. */
    public static byte van(ServerPlayer p) {
        return Duel.gloed(p) ? GLOED : E2H4Vuur.vermomd(p) ? VERMOMD : GEEN;
    }

    /** Tells the player and everyone near them how they look. */
    public static void sync(ServerPlayer p) {
        Bericht b = new Bericht(p.getId(), van(p));
        int bereik = p.getType().clientTrackingRange() * 16 + 16;
        for (ServerPlayer q : p.level().players()) {
            if (q == p || q.distanceToSqr(p) <= (double) bereik * bereik) {
                ModNetworking.sendTo(q, b);
            }
        }
    }

    static void opVolgen(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer kijker && event.getTarget() instanceof ServerPlayer doel) {
            byte look = van(doel);
            if (look != GEEN) {
                ModNetworking.sendTo(kijker, new Bericht(doel.getId(), look));
            }
        }
    }

    static void opLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sync(p);
        }
    }

    static void opDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sync(p);
        }
    }
}
