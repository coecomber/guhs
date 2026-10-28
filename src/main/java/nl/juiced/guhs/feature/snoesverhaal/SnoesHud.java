package nl.juiced.guhs.feature.snoesverhaal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.dromen.DroomReis;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.dromen.DroomVoortgang.Stukje;
import nl.juiced.guhs.network.ModNetworking;

/**
 * Playtest: the story's progress is always in view, top left (client: SnoesHudLaag), like the safari quest tracker: the
 * phase, which pieces are found (a bit per {@link Stukje}, in order) and whether the player is dreaming. Sent when it
 * changes (checked once a second) and on login.
 */
public final class SnoesHud {
    private static final Map<UUID, Stand> LAATST = new ConcurrentHashMap<>();

    private SnoesHud() {
    }

    /** What the HUD shows: the phase (SnoesVerhaal.Fase ordinal), the pieces (bits) and whether the player is in the dream. */
    public record Stand(int fase, int stukjes, boolean droomt) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("snoes_hud"));
        public static final StreamCodec<FriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Stand::fase,
                ByteBufCodecs.VAR_INT, Stand::stukjes, ByteBufCodecs.BOOL, Stand::droomt, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand s, IPayloadContext context) {
            Client.stand = s;
        }
    }

    /** The local player's story state on the client (nothing client-only in here). */
    public static final class Client {
        public static volatile Stand stand = new Stand(0, 0, false);

        private Client() {
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
    }

    /** This player's state now. */
    static Stand stand(ServerPlayer p) {
        int bits = 0;
        for (Stukje s : Stukje.values()) {
            if (DroomVoortgang.heeft(p, s)) {
                bits |= 1 << s.ordinal();
            }
        }
        return new Stand(SnoesVerhaal.fase(p).ordinal(), bits, DroomReis.inDroom(p));
    }

    /** Sends the state when it changed (force: always). */
    static void sync(ServerPlayer p, boolean force) {
        Stand s = stand(p);
        if (force || !s.equals(LAATST.get(p.getUUID()))) {
            LAATST.put(p.getUUID(), s);
            ModNetworking.sendTo(p, s);
        }
    }

    /** Once a second (SnoesVerhaal.tick). */
    static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            sync(p, false);
        }
    }

    static void uitgelogd(ServerPlayer p) {
        LAATST.remove(p.getUUID());
    }
}
