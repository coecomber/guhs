package nl.juiced.guhs.feature.blub;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.8.0: "snoes_geholpen", the player flag that opens the Blub-guhs. Wild Blubs are dream creatures: you only see them (and
 * can tame them) once you helped Snoes fall asleep. Saved with the player (GuhQuests.saved, survives death), synced to that
 * player's client ({@code guhs:blub_snoes}, at login and on every change).
 * <p>
 * For now {@code /guhs blub snoes aan|uit} sets it; later Snoes' dream story (ontwerp-guhs180-dromen.md) is the real trigger.
 */
public final class BlubSnoes {
    public static final String KEY = "guhs_snoes_geholpen";

    private BlubSnoes() {
    }

    /** Did this player help Snoes? (Server: the saved flag; client: the synced copy of the local player.) */
    public static boolean heeft(Player player) {
        if (player.level().isClientSide()) {
            return Client.geholpen;
        }
        return GuhQuests.saved(player).getBooleanOr(KEY, false);
    }

    public static void zet(ServerPlayer player, boolean ja) {
        GuhQuests.saved(player).putBoolean(KEY, ja);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        ModNetworking.sendTo(player, new Vlag(heeft(player)));
    }

    /**
     * May this player see (and click) this Blub? Tamed Blubs (of anyone): always. Wild ones: once you helped Snoes.
     * TODO(Guhdromenland): in the dream dimension every Blub is always visible ({@link #inDroom}).
     */
    public static boolean zichtbaar(BlubGuhEntity blub, Player player) {
        return blub.isTame() || inDroom(blub.level()) || heeft(player);
    }

    /** TODO(Guhdromenland): true in the dream dimension (it doesn't exist yet). */
    public static boolean inDroom(Level level) {
        return false;
    }

    /** The local player's flag on the client (set by the payload; nothing client-only in here, so the entity may read it). */
    public static final class Client {
        public static volatile boolean geholpen;

        private Client() {
        }
    }

    /** Server to client: the flag of this player. */
    public record Vlag(boolean geholpen) implements CustomPacketPayload {
        public static final Type<Vlag> TYPE = new Type<>(Guhs.id("blub_snoes"));
        public static final StreamCodec<FriendlyByteBuf, Vlag> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Vlag::geholpen, Vlag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Vlag p, IPayloadContext context) {
            Client.geholpen = p.geholpen();
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Vlag.TYPE, Vlag.STREAM_CODEC, Vlag::handle);
    }

    static void events() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                sync(sp);
            }
        });
    }
}
