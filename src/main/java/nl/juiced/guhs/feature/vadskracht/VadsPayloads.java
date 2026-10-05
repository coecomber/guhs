package nl.juiced.guhs.feature.vadskracht;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The hover readout over the network:
 * <ul>
 *   <li>{@code guhs:vadskracht_kijk} (to the server): the player's crosshair is on this vadskracht block (asked at most every
 *       10 ticks by {@code client.VadsHover});</li>
 *   <li>{@code guhs:vadskracht_stand} (to the client): the lines of {@link VadsUitlezing} for that block.</li>
 * </ul>
 */
public final class VadsPayloads {
    /** How far away a block may be to be read (well past any reach). */
    private static final double BEREIK = 32;
    /** A player gets an answer at most once per this many ticks for the same block. */
    private static final int WACHT = 4;
    /** Client handler (set by client.VadsHover). */
    public static volatile Consumer<Stand> ontvanger = p -> {
    };
    private static final Map<ServerPlayer, long[]> LAATST = new WeakHashMap<>();

    public record Kijk(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Kijk> TYPE = new Type<>(Guhs.id("vadskracht_kijk"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Kijk> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Kijk::pos, Kijk::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kijk p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                antwoord(sp, p.pos());
            }
        }
    }

    public record Stand(BlockPos pos, List<Component> regels) implements CustomPacketPayload {
        public static final Type<Stand> TYPE = new Type<>(Guhs.id("vadskracht_stand"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Stand> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Stand::pos,
                ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(32)), Stand::regels, Stand::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Stand p, IPayloadContext context) {
            ontvanger.accept(p);
        }
    }

    /** What this player reads on this block right now (null: too far, not loaded, not a vadskracht block, or asked too fast). */
    @javax.annotation.Nullable
    public static Stand stand(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        if (pos.distToCenterSqr(player.position()) > BEREIK * BEREIK || !level.isLoaded(pos) || !level.getBlockState(pos).is(VadsKracht.TOON)) {
            return null;
        }
        long nu = level.getGameTime();
        long[] laatst = LAATST.computeIfAbsent(player, p -> new long[] {Long.MIN_VALUE, 0});
        if (laatst[1] == pos.asLong() && nu - laatst[0] < WACHT && nu >= laatst[0]) {
            return null;
        }
        laatst[0] = nu;
        laatst[1] = pos.asLong();
        List<Component> regels = VadsKracht.regels(level, pos);
        return new Stand(pos, regels.size() > 32 ? new ArrayList<>(regels.subList(0, 32)) : regels);
    }

    private static void antwoord(ServerPlayer player, BlockPos pos) {
        Stand stand = stand(player, pos);
        if (stand != null) {
            ModNetworking.sendTo(player, stand);
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Kijk.TYPE, Kijk.STREAM_CODEC, Kijk::handle);
        registrar.playToClient(Stand.TYPE, Stand.STREAM_CODEC, Stand::handle);
    }

    private VadsPayloads() {
    }
}
